/*
 * Copyright (c) 2025 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.changepassword

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.element.android.libraries.androidutils.json.JsonProvider
import io.element.android.libraries.matrix.test.FakeMatrixClient
import io.element.android.libraries.matrix.test.A_SESSION_ID
import io.element.android.libraries.sessionstorage.test.InMemorySessionStore
import io.element.android.libraries.sessionstorage.test.aSessionData
import io.element.android.tests.testutils.testCoroutineDispatchers
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.Response

class DefaultChangePasswordUseCaseTest {
    @Test
    fun `invoke - submit password change request with current session data`() = runTest {
        val api = FakeChangePasswordApi(
            responses = mutableListOf(
                Response.success(Unit)
            )
        )
        val sessionStore = InMemorySessionStore(
            initialList = listOf(
                aSessionData(
                    sessionId = A_SESSION_ID.value,
                    accessToken = "access-token",
                ).copy(homeserverUrl = "https://matrix.example.org")
            )
        )
        val useCase = createUseCase(
            api = api,
            sessionStore = sessionStore,
        )

        val result = useCase(
            currentPassword = "CurrentPassword!123",
            newPassword = "NewPassword!123",
        )

        assertThat(result.isSuccess).isTrue()
        assertThat(api.requests).hasSize(1)
        val request = api.requests.single()
        assertThat(request.authorization).isEqualTo("Bearer access-token")
        assertThat(request.body.new_password).isEqualTo("NewPassword!123")
        assertThat(request.body.logout_devices).isTrue()
        assertThat(request.body.auth).isNull()
    }

    @Test
    fun `invoke - retries with interactive auth session when first call returns 401`() = runTest {
        val api = FakeChangePasswordApi(
            responses = mutableListOf(
                matrixError(
                    statusCode = 401,
                    body = """
                        {
                          "errcode": "M_UNAUTHORIZED",
                          "error": "Additional authentication required",
                          "session": "uiaa-session-123"
                        }
                    """.trimIndent()
                ),
                Response.success(Unit),
            )
        )
        val sessionStore = InMemorySessionStore(
            initialList = listOf(
                aSessionData(
                    sessionId = A_SESSION_ID.value,
                    accessToken = "access-token",
                ).copy(homeserverUrl = "https://matrix.example.org")
            )
        )
        val useCase = createUseCase(
            api = api,
            sessionStore = sessionStore,
        )

        val result = useCase(
            currentPassword = "CurrentPassword!123",
            newPassword = "NewPassword!123",
        )

        assertThat(result.isSuccess).isTrue()
        assertThat(api.requests).hasSize(2)
        assertThat(api.requests[0].body.auth).isNull()
        assertThat(api.requests[1].body.auth?.type).isEqualTo("m.login.password")
        assertThat(api.requests[1].body.auth?.identifier?.type).isEqualTo("m.id.user")
        assertThat(api.requests[1].body.auth?.identifier?.user).isEqualTo(A_SESSION_ID.value)
        assertThat(api.requests[1].body.auth?.password).isEqualTo("CurrentPassword!123")
        assertThat(api.requests[1].body.auth?.session).isEqualTo("uiaa-session-123")
    }

    @Test
    fun `invoke - maps forbidden errors to invalid current password`() = runTest {
        val api = FakeChangePasswordApi(
            responses = mutableListOf(
                matrixError(
                    statusCode = 401,
                    body = """
                        {
                          "errcode": "M_UNAUTHORIZED",
                          "error": "Additional authentication required",
                          "session": "uiaa-session-123"
                        }
                    """.trimIndent()
                ),
                matrixError(
                    statusCode = 403,
                    body = """
                        {
                          "errcode": "M_FORBIDDEN",
                          "error": "Invalid password"
                        }
                    """.trimIndent()
                )
            )
        )
        val sessionStore = InMemorySessionStore(
            initialList = listOf(
                aSessionData(sessionId = A_SESSION_ID.value).copy(homeserverUrl = "https://matrix.example.org")
            )
        )
        val useCase = createUseCase(
            api = api,
            sessionStore = sessionStore,
        )

        val result = useCase(
            currentPassword = "bad-password",
            newPassword = "NewPassword!123",
        )

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(ChangePasswordException.InvalidCurrentPassword::class.java)
    }

    @Test
    fun `invoke - maps weak password errors`() = runTest {
        val api = FakeChangePasswordApi(
            responses = mutableListOf(
                matrixError(
                    statusCode = 401,
                    body = """
                        {
                          "errcode": "M_UNAUTHORIZED",
                          "error": "Additional authentication required",
                          "session": "uiaa-session-123"
                        }
                    """.trimIndent()
                ),
                matrixError(
                    statusCode = 400,
                    body = """
                        {
                          "errcode": "M_WEAK_PASSWORD",
                          "error": "Password too weak"
                        }
                    """.trimIndent()
                )
            )
        )
        val sessionStore = InMemorySessionStore(
            initialList = listOf(
                aSessionData(sessionId = A_SESSION_ID.value).copy(homeserverUrl = "https://matrix.example.org")
            )
        )
        val useCase = createUseCase(
            api = api,
            sessionStore = sessionStore,
        )

        val result = useCase(
            currentPassword = "CurrentPassword!123",
            newPassword = "weak",
        )

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(ChangePasswordException.WeakPassword::class.java)
    }

    @Test
    fun `invoke - retries with legacy auth payload when UIA keeps returning 401`() = runTest {
        val api = FakeChangePasswordApi(
            responses = mutableListOf(
                matrixError(
                    statusCode = 401,
                    body = """
                        {
                          "session": "uiaa-session-123"
                        }
                    """.trimIndent()
                ),
                matrixError(
                    statusCode = 401,
                    body = """
                        {
                          "session": "uiaa-session-456"
                        }
                    """.trimIndent()
                ),
                Response.success(Unit),
            )
        )
        val sessionStore = InMemorySessionStore(
            initialList = listOf(
                aSessionData(
                    sessionId = A_SESSION_ID.value,
                    accessToken = "access-token",
                ).copy(homeserverUrl = "https://matrix.example.org")
            )
        )
        val useCase = createUseCase(
            api = api,
            sessionStore = sessionStore,
        )

        val result = useCase(
            currentPassword = "CurrentPassword!123",
            newPassword = "NewPassword!123",
        )

        assertThat(result.isSuccess).isTrue()
        assertThat(api.requests).hasSize(2)
        assertThat(api.requests[0].body.auth).isNull()
        assertThat(api.requests[1].body.auth?.session).isEqualTo("uiaa-session-123")
        assertThat(api.legacyRequests).hasSize(1)
        val legacyRequest = api.legacyRequests.single()
        assertThat(legacyRequest.authorization).isEqualTo("Bearer access-token")
        assertThat(legacyRequest.body.auth.type).isEqualTo("m.login.password")
        assertThat(legacyRequest.body.auth.identifier).isEqualTo(A_SESSION_ID.value)
        assertThat(legacyRequest.body.auth.user).isEqualTo(
            A_SESSION_ID.value.removePrefix("@").substringBefore(":")
        )
        assertThat(legacyRequest.body.auth.password).isEqualTo("CurrentPassword!123")
        assertThat(legacyRequest.body.auth.session).isEqualTo("uiaa-session-456")
    }

    @Test
    fun `invoke - fails when no active session data exists`() = runTest {
        val useCase = createUseCase(
            api = FakeChangePasswordApi(
                responses = mutableListOf(Response.success(Unit))
            ),
            sessionStore = InMemorySessionStore(initialList = emptyList()),
        )

        val result = useCase(
            currentPassword = "CurrentPassword!123",
            newPassword = "NewPassword!123",
        )

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isEqualTo(ChangePasswordException.MissingSession)
    }

    private fun TestScope.createUseCase(
        api: ChangePasswordApi,
        sessionStore: InMemorySessionStore,
    ): DefaultChangePasswordUseCase {
        return DefaultChangePasswordUseCase(
            matrixClient = FakeMatrixClient(sessionId = A_SESSION_ID),
            sessionStore = sessionStore,
            secureRetrofitFactory = fakeSecureRetrofitFactory(api),
            coroutineDispatchers = testCoroutineDispatchers(),
            jsonProvider = JsonProvider {
                Json {
                    ignoreUnknownKeys = true
                }
            },
        )
    }
}

private fun fakeSecureRetrofitFactory(api: ChangePasswordApi): SecureRetrofitFactory {
    val secureRetrofitFactory = mockk<SecureRetrofitFactory>()
    val retrofit = mockk<Retrofit>()
    every { secureRetrofitFactory.create(any()) } returns retrofit
    every { retrofit.create(ChangePasswordApi::class.java) } returns api
    return secureRetrofitFactory
}

private data class RecordedPasswordRequest(
    val authorization: String,
    val body: ChangePasswordRequest,
)

private data class RecordedLegacyPasswordRequest(
    val authorization: String,
    val body: ChangePasswordLegacyRequest,
)

private class FakeChangePasswordApi(
    val responses: MutableList<Response<Unit>>,
) : ChangePasswordApi {
    val requests = mutableListOf<RecordedPasswordRequest>()
    val legacyRequests = mutableListOf<RecordedLegacyPasswordRequest>()

    override suspend fun changePassword(
        authorization: String,
        body: ChangePasswordRequest,
    ): Response<Unit> {
        requests += RecordedPasswordRequest(authorization, body)
        return responses.removeFirst()
    }

    override suspend fun changePasswordLegacy(
        authorization: String,
        body: ChangePasswordLegacyRequest,
    ): Response<Unit> {
        legacyRequests += RecordedLegacyPasswordRequest(authorization, body)
        return responses.removeFirst()
    }
}

private fun matrixError(
    statusCode: Int,
    body: String,
): Response<Unit> {
    return Response.error(
        statusCode,
        body.toResponseBody("application/json".toMediaType())
    )
}
