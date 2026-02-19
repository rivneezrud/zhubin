/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.changepassword

sealed class ChangePasswordException(
    message: String? = null,
) : Exception(message) {
    data class InvalidCurrentPassword(
        val matrixError: String?,
    ) : ChangePasswordException(matrixError)

    data class WeakPassword(
        val matrixError: String?,
    ) : ChangePasswordException(matrixError)

    data class Unauthorized(
        val matrixError: String?,
    ) : ChangePasswordException(matrixError)

    data class ApiError(
        val statusCode: Int,
        val matrixError: String?,
    ) : ChangePasswordException(matrixError)

    data object MissingSession : ChangePasswordException()
}
