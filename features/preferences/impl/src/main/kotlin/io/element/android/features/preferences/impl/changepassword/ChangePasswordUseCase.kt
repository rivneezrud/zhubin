/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.changepassword

import dev.zacsweers.metro.ContributesBinding
import io.element.android.libraries.androidutils.json.JsonProvider
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.sessionstorage.api.SessionStore
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.Response
import timber.log.Timber

interface ChangePasswordUseCase {
    suspend operator fun invoke(
        currentPassword: String,
        newPassword: String,
        logoutDevices: Boolean = true,
        logoutCurrentSessionOnSuccess: Boolean = false,
    ): Result<Unit>
}

@ContributesBinding(SessionScope::class)
class DefaultChangePasswordUseCase internal constructor(
    private val matrixClient: MatrixClient,
    private val sessionStore: SessionStore,
    private val secureRetrofitFactory: SecureRetrofitFactory,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val jsonProvider: JsonProvider,
) : ChangePasswordUseCase {
    override suspend fun invoke(
        currentPassword: String,
        newPassword: String,
        logoutDevices: Boolean,
        logoutCurrentSessionOnSuccess: Boolean,
    ): Result<Unit> = withContext(coroutineDispatchers.io) {
        val sessionData = sessionStore.getSession(matrixClient.sessionId.value)
            ?: return@withContext Result.failure(ChangePasswordException.MissingSession)
        val api = secureRetrofitFactory.create(sessionData.homeserverUrl)
            .create(ChangePasswordApi::class.java)
        val authorization = "Bearer ${sessionData.accessToken}"
        val fullUserId = sessionData.userId
        val localPart = fullUserId.removePrefix("@").substringBefore(":")
        val authTemplate = ChangePasswordAuth(
            type = MATRIX_LOGIN_TYPE_PASSWORD,
            identifier = ChangePasswordIdentifier(
                type = MATRIX_IDENTIFIER_TYPE_USER,
                user = fullUserId,
            ),
            password = currentPassword,
        )

        val request = ChangePasswordRequest(
            new_password = newPassword,
            logout_devices = logoutDevices,
            auth = null,
        )

        val firstResponse = api.changePassword(
            authorization = authorization,
            body = request,
        )

        if (firstResponse.isSuccessful) {
            return@withContext maybeLogoutCurrentSession(logoutCurrentSessionOnSuccess)
        }

        val firstError = parseMatrixError(firstResponse)
        if (firstResponse.isSuccessful.not()) {
            Timber.w(
                "Change password first attempt failed: http=%d, errcode=%s, hasSession=%s, error=%s",
                firstResponse.code(),
                firstError?.errcode,
                firstError?.session != null,
                firstError?.error,
            )
        }
        if (firstResponse.code() != HTTP_STATUS_UNAUTHORIZED || firstError?.session == null) {
            return@withContext Result.failure(
                mapErrorToException(
                    statusCode = firstResponse.code(),
                    matrixError = firstError,
                )
            )
        }

        val retryResponse = api.changePassword(
            authorization = authorization,
            body = request.copy(auth = authTemplate.copy(session = firstError.session)),
        )
        if (retryResponse.isSuccessful) {
            return@withContext maybeLogoutCurrentSession(logoutCurrentSessionOnSuccess)
        }
        val retryError = parseMatrixError(retryResponse)
        if (retryResponse.isSuccessful.not()) {
            Timber.w(
                "Change password retry failed: http=%d, errcode=%s, hasSession=%s, error=%s",
                retryResponse.code(),
                retryError?.errcode,
                retryError?.session != null,
                retryError?.error,
            )
        }
        if (retryResponse.code() == HTTP_STATUS_UNAUTHORIZED && retryError?.session != null) {
            val passwordCandidates = buildCurrentPasswordCandidates(currentPassword)
            if (passwordCandidates.size > 1) {
                var session = retryError.session
                passwordCandidates.drop(1).forEach { candidatePassword ->
                    Timber.w("Retrying change password with normalized current password candidate")
                    val normalizedRetryResponse = api.changePassword(
                        authorization = authorization,
                        body = request.copy(
                            auth = authTemplate.copy(
                                password = candidatePassword,
                                session = session,
                            )
                        ),
                    )
                    if (normalizedRetryResponse.isSuccessful) {
                        return@withContext maybeLogoutCurrentSession(logoutCurrentSessionOnSuccess)
                    }
                    val normalizedRetryError = parseMatrixError(normalizedRetryResponse)
                    if (normalizedRetryResponse.isSuccessful.not()) {
                        Timber.w(
                            "Change password normalized retry failed: http=%d, errcode=%s, hasSession=%s, error=%s",
                            normalizedRetryResponse.code(),
                            normalizedRetryError?.errcode,
                            normalizedRetryError?.session != null,
                            normalizedRetryError?.error,
                        )
                    }
                    if (normalizedRetryResponse.code() != HTTP_STATUS_UNAUTHORIZED || normalizedRetryError?.session == null) {
                        return@withContext Result.failure(
                            mapErrorToException(
                                statusCode = normalizedRetryResponse.code(),
                                matrixError = normalizedRetryError,
                            )
                        )
                    }
                    session = normalizedRetryError.session
                }
            }

            val legacyResponse = api.changePasswordLegacy(
                authorization = authorization,
                body = ChangePasswordLegacyRequest(
                    new_password = newPassword,
                    logout_devices = logoutDevices,
                    auth = ChangePasswordLegacyAuth(
                        type = MATRIX_LOGIN_TYPE_PASSWORD,
                        identifier = fullUserId,
                        user = localPart,
                        password = currentPassword,
                        session = retryError.session,
                    )
                ),
            )
            if (legacyResponse.isSuccessful) {
                return@withContext maybeLogoutCurrentSession(logoutCurrentSessionOnSuccess)
            }
            val legacyError = parseMatrixError(legacyResponse)
            if (legacyResponse.isSuccessful.not()) {
                Timber.w(
                    "Change password legacy retry failed: http=%d, errcode=%s, hasSession=%s, error=%s",
                    legacyResponse.code(),
                    legacyError?.errcode,
                    legacyError?.session != null,
                    legacyError?.error,
                )
            }
            return@withContext Result.failure(
                mapErrorToException(
                    statusCode = legacyResponse.code(),
                    matrixError = legacyError,
                )
            )
        }

        Result.failure(
            mapErrorToException(
                statusCode = retryResponse.code(),
                matrixError = retryError,
            )
        )
    }

    private suspend fun maybeLogoutCurrentSession(logoutCurrentSessionOnSuccess: Boolean): Result<Unit> {
        if (!logoutCurrentSessionOnSuccess) return Result.success(Unit)
        return runCatching {
            matrixClient.logout(
                userInitiated = true,
                ignoreSdkError = false,
            )
        }
    }

    private fun mapErrorToException(
        statusCode: Int,
        matrixError: MatrixErrorResponse?,
    ): ChangePasswordException {
        return when (matrixError?.errcode) {
            MATRIX_ERROR_FORBIDDEN -> ChangePasswordException.InvalidCurrentPassword(matrixError.error)
            MATRIX_ERROR_WEAK_PASSWORD -> ChangePasswordException.WeakPassword(matrixError.error)
            MATRIX_ERROR_UNAUTHORIZED -> ChangePasswordException.Unauthorized(matrixError.error)
            else -> if (statusCode == HTTP_STATUS_UNAUTHORIZED) {
                ChangePasswordException.Unauthorized(matrixError?.error)
            } else {
                ChangePasswordException.ApiError(statusCode, matrixError?.error)
            }
        }
    }

    private fun parseMatrixError(response: Response<Unit>): MatrixErrorResponse? {
        val body = response.errorBody()?.string().orEmpty()
        if (body.isEmpty()) return null
        val json = jsonProvider()
        return runCatching {
            json.decodeFromString(MatrixErrorResponse.serializer(), body)
        }.recoverCatching {
            // Fallback for malformed JSON responses. Try to extract only the fields we need.
            val root = json.parseToJsonElement(body).jsonObject
            MatrixErrorResponse(
                errcode = root["errcode"]?.jsonPrimitive?.contentOrNull,
                error = root["error"]?.jsonPrimitive?.contentOrNull,
                session = root["session"]?.jsonPrimitive?.contentOrNull,
            )
        }.getOrNull()
    }

    private fun buildCurrentPasswordCandidates(password: String): List<String> {
        val normalizedDigits = password.normalizeCommonLocalizedDigits()
        val trimmed = password.trim()
        val trimmedNormalized = normalizedDigits.trim()
        return listOf(password, normalizedDigits, trimmed, trimmedNormalized).distinct()
    }

    private fun String.normalizeCommonLocalizedDigits(): String {
        if (isEmpty()) return this
        var changed = false
        val normalized = buildString(length) {
            for (char in this@normalizeCommonLocalizedDigits) {
                val mapped = when (char) {
                    in '\u0660'..'\u0669' -> '0' + (char.code - '\u0660'.code)
                    in '\u06F0'..'\u06F9' -> '0' + (char.code - '\u06F0'.code)
                    else -> char
                }
                if (mapped != char) changed = true
                append(mapped)
            }
        }
        return if (changed) normalized else this
    }

    private companion object {
        private const val MATRIX_ERROR_FORBIDDEN = "M_FORBIDDEN"
        private const val MATRIX_ERROR_WEAK_PASSWORD = "M_WEAK_PASSWORD"
        private const val MATRIX_ERROR_UNAUTHORIZED = "M_UNAUTHORIZED"
        private const val MATRIX_LOGIN_TYPE_PASSWORD = "m.login.password"
        private const val MATRIX_IDENTIFIER_TYPE_USER = "m.id.user"
        private const val HTTP_STATUS_UNAUTHORIZED = 401
    }
}
