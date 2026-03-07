/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.changepassword

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

internal interface ChangePasswordApi {
    @POST("/_matrix/client/v3/account/password")
    suspend fun changePassword(
        @Header("Authorization") authorization: String,
        @Body body: ChangePasswordRequest,
    ): Response<Unit>

    @POST("/_matrix/client/v3/account/password")
    suspend fun changePasswordLegacy(
        @Header("Authorization") authorization: String,
        @Body body: ChangePasswordLegacyRequest,
    ): Response<Unit>
}

@Serializable
internal data class ChangePasswordRequest(
    val new_password: String,
    val logout_devices: Boolean,
    val auth: ChangePasswordAuth? = null,
)

@Serializable
internal data class ChangePasswordAuth(
    val type: String,
    val identifier: ChangePasswordIdentifier,
    val password: String,
    val session: String? = null,
)

@Serializable
internal data class ChangePasswordLegacyRequest(
    val new_password: String,
    val logout_devices: Boolean,
    val auth: ChangePasswordLegacyAuth,
)

@Serializable
internal data class ChangePasswordLegacyAuth(
    val type: String,
    val identifier: String,
    val user: String? = null,
    val password: String,
    val session: String? = null,
)

@Serializable
internal data class ChangePasswordIdentifier(
    val type: String,
    val user: String,
)

@Serializable
internal data class MatrixErrorResponse(
    val errcode: String? = null,
    val error: String? = null,
    val session: String? = null,
)
