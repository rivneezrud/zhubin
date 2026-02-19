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
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.Response

interface ChangePasswordUseCase {
    suspend operator fun invoke(
        currentPassword: String,
        newPassword: String,
        logoutDevices: Boolean = true,
        logoutCurrentSessionOnSuccess: Boolean = false,
    ): Result<Unit>
}

@ContributesBinding(SessionScope::class)
class DefaultChangePasswordUseCase(
    private val matrixClient: MatrixClient,
    private val sessionStore: SessionStore,
    private val changePasswordApiFactory: ChangePasswordApiFactory,
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
        val api = changePasswordApiFactory.create(sessionData.homeserverUrl)
        val authorization = "Bearer ${sessionData.accessToken}"

        val auth = ChangePasswordAuth(
            identifier = ChangePasswordIdentifier(user = matrixClient.sessionId.value),
            password = currentPassword,
        )
        val request = ChangePasswordRequest(
            new_password = newPassword,
            logout_devices = logoutDevices,
            auth = auth,
        )

        val firstResponse = api.changePassword(
            authorization = authorization,
            body = request,
        )

        if (firstResponse.isSuccessful) {
            return@withContext maybeLogoutCurrentSession(logoutCurrentSessionOnSuccess)
        }

        val firstError = parseMatrixError(firstResponse)
        if (firstResponse.code() == HTTP_STATUS_UNAUTHORIZED && firstError?.session != null) {
            val retryResponse = api.changePassword(
                authorization = authorization,
                body = request.copy(
                    auth = auth.copy(session = firstError.session)
                ),
            )
            if (retryResponse.isSuccessful) {
                return@withContext maybeLogoutCurrentSession(logoutCurrentSessionOnSuccess)
            }
            val retryError = parseMatrixError(retryResponse)
            return@withContext Result.failure(
                mapErrorToException(
                    statusCode = retryResponse.code(),
                    matrixError = retryError,
                )
            )
        }

        Result.failure(
            mapErrorToException(
                statusCode = firstResponse.code(),
                matrixError = firstError,
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

    private companion object {
        private const val MATRIX_ERROR_FORBIDDEN = "M_FORBIDDEN"
        private const val MATRIX_ERROR_WEAK_PASSWORD = "M_WEAK_PASSWORD"
        private const val MATRIX_ERROR_UNAUTHORIZED = "M_UNAUTHORIZED"
        private const val HTTP_STATUS_UNAUTHORIZED = 401
    }
}
