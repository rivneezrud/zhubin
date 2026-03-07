/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.changepassword

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import dev.zacsweers.metro.Inject
import io.element.android.features.preferences.impl.R
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.architecture.runCatchingUpdatingState
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarDispatcher
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarMessage
import io.element.android.libraries.designsystem.utils.snackbar.collectSnackbarMessageAsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Inject
class ChangePasswordPresenter(
    private val changePasswordUseCase: ChangePasswordUseCase,
    private val snackbarDispatcher: SnackbarDispatcher,
) : Presenter<ChangePasswordState> {
    @Composable
    override fun present(): ChangePasswordState {
        val coroutineScope = rememberCoroutineScope()
        val updateAction: MutableState<AsyncAction<Unit>> = remember {
            mutableStateOf(AsyncAction.Uninitialized)
        }
        val formState: MutableState<ChangePasswordFormState> = remember {
            mutableStateOf(ChangePasswordFormState.Default)
        }
        val snackbarMessage by snackbarDispatcher.collectSnackbarMessageAsState()

        fun updateFormState(update: ChangePasswordFormState.() -> ChangePasswordFormState) {
            formState.value = update(formState.value)
            if (updateAction.value.isFailure()) {
                updateAction.value = AsyncAction.Uninitialized
            }
        }

        fun handleEvent(event: ChangePasswordEvents) {
            when (event) {
                is ChangePasswordEvents.SetCurrentPassword -> updateFormState {
                    copy(currentPassword = event.password)
                }
                is ChangePasswordEvents.SetNewPassword -> updateFormState {
                    copy(newPassword = event.password)
                }
                is ChangePasswordEvents.SetConfirmNewPassword -> updateFormState {
                    copy(confirmNewPassword = event.password)
                }
                ChangePasswordEvents.Submit -> {
                    val currentFormState = formState.value
                    if (currentFormState.canSubmit(updateAction.value)) {
                        coroutineScope.submit(
                            currentFormState = currentFormState,
                            updateAction = updateAction,
                            formState = formState,
                        )
                    }
                }
            }
        }

        return ChangePasswordState(
            formState = formState.value,
            updateAction = updateAction.value,
            snackbarMessage = snackbarMessage,
            eventSink = ::handleEvent,
        )
    }

    private fun CoroutineScope.submit(
        currentFormState: ChangePasswordFormState,
        updateAction: MutableState<AsyncAction<Unit>>,
        formState: MutableState<ChangePasswordFormState>,
    ) = launch {
        suspend {
            changePasswordUseCase(
                currentPassword = currentFormState.currentPassword,
                newPassword = currentFormState.newPassword,
                logoutDevices = true,
                logoutCurrentSessionOnSuccess = false,
            ).getOrThrow()
        }.runCatchingUpdatingState(state = updateAction)
            .onSuccess {
                snackbarDispatcher.post(
                    SnackbarMessage(R.string.screen_change_password_success)
                )
                updateAction.value = AsyncAction.Uninitialized
            }

        // Clear sensitive data from memory-backed state after the request completes.
        formState.value = ChangePasswordFormState.Default
    }

    private fun ChangePasswordFormState.canSubmit(updateAction: AsyncAction<Unit>): Boolean {
        val hasMinimumLength = newPassword.length >= ChangePasswordState.MIN_PASSWORD_LENGTH
        val strengthScore = listOf(
            newPassword.any(Char::isLowerCase),
            newPassword.any(Char::isUpperCase),
            newPassword.any(Char::isDigit),
            newPassword.any { it.isLetterOrDigit().not() },
        ).count { it }
        val hasStrongEnoughComposition = strengthScore >= 3
        return updateAction.isLoading().not() &&
            currentPassword.isNotEmpty() &&
            hasMinimumLength &&
            hasStrongEnoughComposition &&
            confirmNewPassword.isNotEmpty() &&
            confirmNewPassword == newPassword
    }
}
