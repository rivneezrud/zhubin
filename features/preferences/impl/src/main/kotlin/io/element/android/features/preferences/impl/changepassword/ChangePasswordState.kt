/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.changepassword

import android.os.Parcelable
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarMessage
import kotlinx.parcelize.Parcelize

data class ChangePasswordState(
    val formState: ChangePasswordFormState,
    val updateAction: AsyncAction<Unit>,
    val snackbarMessage: SnackbarMessage?,
    val eventSink: (ChangePasswordEvents) -> Unit,
) {
    val hasMinimumLength: Boolean
        get() = formState.newPassword.length >= MIN_PASSWORD_LENGTH

    val hasLowercaseCharacter: Boolean
        get() = formState.newPassword.any(Char::isLowerCase)

    val hasUppercaseCharacter: Boolean
        get() = formState.newPassword.any(Char::isUpperCase)

    val hasDigitCharacter: Boolean
        get() = formState.newPassword.any(Char::isDigit)

    val hasSpecialCharacter: Boolean
        get() = formState.newPassword.any { it.isLetterOrDigit().not() }

    val hasStrongEnoughComposition: Boolean
        get() = listOf(
            hasLowercaseCharacter,
            hasUppercaseCharacter,
            hasDigitCharacter,
            hasSpecialCharacter,
        ).count { it } >= 3

    val isNewPasswordValid: Boolean
        get() = hasMinimumLength && hasStrongEnoughComposition

    val doesConfirmationMatch: Boolean
        get() = formState.confirmNewPassword.isNotEmpty() &&
            formState.confirmNewPassword == formState.newPassword

    val submitEnabled: Boolean
        get() = updateAction.isLoading().not() &&
            formState.currentPassword.isNotEmpty() &&
            isNewPasswordValid &&
            doesConfirmationMatch

    companion object {
        const val MIN_PASSWORD_LENGTH = 12
    }
}

@Parcelize
data class ChangePasswordFormState(
    val currentPassword: String,
    val newPassword: String,
    val confirmNewPassword: String,
) : Parcelable {
    companion object {
        val Default = ChangePasswordFormState(
            currentPassword = "",
            newPassword = "",
            confirmNewPassword = "",
        )
    }
}
