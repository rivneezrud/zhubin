/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.changepassword

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarMessage
import io.element.android.libraries.ui.strings.CommonStrings

class ChangePasswordStateProvider : PreviewParameterProvider<ChangePasswordState> {
    override val values: Sequence<ChangePasswordState>
        get() = sequenceOf(
            aChangePasswordState(),
            aChangePasswordState(
                formState = ChangePasswordFormState(
                    currentPassword = "current-password",
                    newPassword = "NewPassword!123",
                    confirmNewPassword = "NewPassword!123",
                ),
            ),
            aChangePasswordState(
                formState = ChangePasswordFormState(
                    currentPassword = "current-password",
                    newPassword = "weak",
                    confirmNewPassword = "weaker",
                ),
                updateAction = AsyncAction.Failure(ChangePasswordException.InvalidCurrentPassword(null)),
            ),
            aChangePasswordState(
                updateAction = AsyncAction.Loading,
            ),
            aChangePasswordState(
                snackbarMessage = SnackbarMessage(CommonStrings.common_verification_complete),
            ),
        )
}

fun aChangePasswordState(
    formState: ChangePasswordFormState = ChangePasswordFormState.Default,
    updateAction: AsyncAction<Unit> = AsyncAction.Uninitialized,
    snackbarMessage: SnackbarMessage? = null,
    eventSink: (ChangePasswordEvents) -> Unit = {},
) = ChangePasswordState(
    formState = formState,
    updateAction = updateAction,
    snackbarMessage = snackbarMessage,
    eventSink = eventSink,
)
