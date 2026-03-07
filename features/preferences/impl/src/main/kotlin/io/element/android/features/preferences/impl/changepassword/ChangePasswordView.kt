/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.changepassword

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.preferences.impl.R
import io.element.android.libraries.designsystem.components.button.BackButton
import io.element.android.libraries.designsystem.components.form.textFieldState
import io.element.android.libraries.designsystem.modifiers.onTabOrEnterKeyFocusNext
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextField
import io.element.android.libraries.designsystem.theme.components.TextFieldValidity
import io.element.android.libraries.designsystem.theme.components.TopAppBar
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarHost
import io.element.android.libraries.designsystem.utils.snackbar.rememberSnackbarHostState
import io.element.android.libraries.ui.strings.CommonStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordView(
    state: ChangePasswordState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = rememberSnackbarHostState(snackbarMessage = state.snackbarMessage)
    val isLoading by remember(state.updateAction) {
        derivedStateOf { state.updateAction.isLoading() }
    }
    val focusManager = LocalFocusManager.current

    var currentPasswordFieldState by textFieldState(stateValue = state.formState.currentPassword)
    var newPasswordFieldState by textFieldState(stateValue = state.formState.newPassword)
    var confirmNewPasswordFieldState by textFieldState(stateValue = state.formState.confirmNewPassword)

    var isCurrentPasswordVisible by remember { mutableStateOf(false) }
    var isNewPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    if (isLoading) {
        // Ensure all fields are obscured while a network request is in progress.
        isCurrentPasswordVisible = false
        isNewPasswordVisible = false
        isConfirmPasswordVisible = false
    }

    fun submit() {
        focusManager.clearFocus(force = true)
        state.eventSink(ChangePasswordEvents.Submit)
    }

    val confirmMismatch = state.formState.confirmNewPassword.isNotEmpty() && !state.doesConfirmationMatch
    val newPasswordSupportingText = when {
        state.formState.newPassword.isEmpty() -> stringResource(
            id = R.string.screen_change_password_min_length_hint,
            ChangePasswordState.MIN_PASSWORD_LENGTH
        )
        state.hasMinimumLength -> stringResource(
            id = R.string.screen_change_password_min_length_met,
            ChangePasswordState.MIN_PASSWORD_LENGTH
        )
        else -> stringResource(
            id = R.string.screen_change_password_min_length_hint,
            ChangePasswordState.MIN_PASSWORD_LENGTH
        )
    }

    val error = state.updateAction.errorOrNull()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                navigationIcon = { BackButton(onClick = onBackClick) },
                titleStr = stringResource(id = R.string.screen_change_password_title),
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .imePadding()
                .padding(padding)
                .consumeWindowInsets(padding)
                .verticalScroll(state = rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TextField(
                value = currentPasswordFieldState,
                label = stringResource(R.string.screen_change_password_current_password),
                placeholder = stringResource(CommonStrings.common_password),
                readOnly = isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .onTabOrEnterKeyFocusNext(focusManager)
                    .semantics { contentType = ContentType.Password },
                onValueChange = {
                    val sanitized = it.sanitize()
                    currentPasswordFieldState = sanitized
                    state.eventSink(ChangePasswordEvents.SetCurrentPassword(sanitized))
                },
                visualTransformation = if (isCurrentPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    PasswordVisibilityToggle(
                        isVisible = isCurrentPasswordVisible,
                        onClick = { isCurrentPasswordVisible = !isCurrentPasswordVisible }
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
                ),
                singleLine = true,
            )

            TextField(
                value = newPasswordFieldState,
                label = stringResource(R.string.screen_change_password_new_password),
                placeholder = stringResource(CommonStrings.common_password),
                readOnly = isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .onTabOrEnterKeyFocusNext(focusManager)
                    .semantics { contentType = ContentType.Password },
                onValueChange = {
                    val sanitized = it.sanitize()
                    newPasswordFieldState = sanitized
                    state.eventSink(ChangePasswordEvents.SetNewPassword(sanitized))
                },
                supportingText = newPasswordSupportingText,
                validity = when {
                    state.formState.newPassword.isEmpty() -> TextFieldValidity.None
                    state.isNewPasswordValid -> TextFieldValidity.Valid
                    else -> TextFieldValidity.Invalid
                },
                visualTransformation = if (isNewPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    PasswordVisibilityToggle(
                        isVisible = isNewPasswordVisible,
                        onClick = { isNewPasswordVisible = !isNewPasswordVisible }
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
                ),
                singleLine = true,
            )

            Text(
                text = stringResource(R.string.screen_change_password_strength_hint),
                style = ElementTheme.typography.fontBodySmRegular,
                color = when {
                    state.formState.newPassword.isEmpty() -> ElementTheme.colors.textSecondary
                    state.hasStrongEnoughComposition -> ElementTheme.colors.textSuccessPrimary
                    else -> ElementTheme.colors.textCriticalPrimary
                },
            )

            TextField(
                value = confirmNewPasswordFieldState,
                label = stringResource(R.string.screen_change_password_confirm_new_password),
                placeholder = stringResource(CommonStrings.common_password),
                readOnly = isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .onTabOrEnterKeyFocusNext(focusManager)
                    .semantics { contentType = ContentType.Password },
                onValueChange = {
                    val sanitized = it.sanitize()
                    confirmNewPasswordFieldState = sanitized
                    state.eventSink(ChangePasswordEvents.SetConfirmNewPassword(sanitized))
                },
                supportingText = if (confirmMismatch) {
                    stringResource(R.string.screen_change_password_confirm_mismatch)
                } else {
                    null
                },
                validity = when {
                    state.formState.confirmNewPassword.isEmpty() -> TextFieldValidity.None
                    confirmMismatch -> TextFieldValidity.Invalid
                    else -> TextFieldValidity.Valid
                },
                visualTransformation = if (isConfirmPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    PasswordVisibilityToggle(
                        isVisible = isConfirmPasswordVisible,
                        onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { submit() }
                ),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                text = stringResource(R.string.screen_change_password_update_action),
                showProgress = isLoading,
                enabled = state.submitEnabled,
                modifier = Modifier.fillMaxWidth(),
                onClick = ::submit,
            )

            if (error != null) {
                Text(
                    text = error.toUiMessage(),
                    style = ElementTheme.typography.fontBodySmRegular,
                    color = ElementTheme.colors.textCriticalPrimary,
                )
            }
        }
    }
}

@Composable
private fun PasswordVisibilityToggle(
    isVisible: Boolean,
    onClick: () -> Unit,
) {
    val image = if (isVisible) CompoundIcons.VisibilityOn() else CompoundIcons.VisibilityOff()
    val contentDescription = if (isVisible) {
        stringResource(CommonStrings.a11y_hide_password)
    } else {
        stringResource(CommonStrings.a11y_show_password)
    }
    Box(
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(imageVector = image, contentDescription = contentDescription)
    }
}

@Composable
private fun Throwable.toUiMessage(): String {
    return when (this) {
        is ChangePasswordException.InvalidCurrentPassword -> {
            stringResource(R.string.screen_change_password_error_invalid_current_password)
        }
        is ChangePasswordException.WeakPassword -> {
            matrixError ?: stringResource(R.string.screen_change_password_error_weak_password)
        }
        is ChangePasswordException.Unauthorized -> {
            stringResource(R.string.screen_change_password_error_unauthorized)
        }
        is ChangePasswordException.MissingSession -> {
            stringResource(R.string.screen_change_password_error_missing_session)
        }
        is ChangePasswordException.ApiError -> {
            matrixError ?: stringResource(CommonStrings.common_something_went_wrong_message)
        }
        else -> {
            stringResource(CommonStrings.common_something_went_wrong_message)
        }
    }
}

private fun String.sanitize(): String {
    return replace("\n", "")
}

@PreviewsDayNight
@Composable
internal fun ChangePasswordViewPreview(
    @PreviewParameter(ChangePasswordStateProvider::class) state: ChangePasswordState,
) = ElementPreview {
    ChangePasswordView(
        state = state,
        onBackClick = {},
    )
}
