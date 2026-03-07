/*
 * Copyright (c) 2025 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.changepassword

import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarDispatcher
import io.element.android.tests.testutils.WarmUpRule
import io.element.android.tests.testutils.lambda.lambdaRecorder
import io.element.android.tests.testutils.lambda.value
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ChangePasswordPresenterTest {
    @get:Rule
    val warmUpRule = WarmUpRule()

    @Test
    fun `present - initial state`() = runTest {
        createPresenter().test {
            val initialState = awaitItem()
            assertThat(initialState.formState).isEqualTo(ChangePasswordFormState.Default)
            assertThat(initialState.updateAction).isEqualTo(AsyncAction.Uninitialized)
            assertThat(initialState.submitEnabled).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `present - form validation`() = runTest {
        createPresenter().test {
            val initialState = awaitItem()
            initialState.eventSink(ChangePasswordEvents.SetCurrentPassword("current"))
            val currentPasswordState = awaitItem()
            assertThat(currentPasswordState.submitEnabled).isFalse()

            currentPasswordState.eventSink(ChangePasswordEvents.SetNewPassword("short"))
            val weakPasswordState = awaitItem()
            assertThat(weakPasswordState.isNewPasswordValid).isFalse()
            assertThat(weakPasswordState.submitEnabled).isFalse()

            weakPasswordState.eventSink(ChangePasswordEvents.SetNewPassword("AStrongPassword!123"))
            val strongPasswordState = awaitItem()
            assertThat(strongPasswordState.isNewPasswordValid).isTrue()
            assertThat(strongPasswordState.submitEnabled).isFalse()

            strongPasswordState.eventSink(ChangePasswordEvents.SetConfirmNewPassword("NotMatching!123"))
            val mismatchState = awaitItem()
            assertThat(mismatchState.doesConfirmationMatch).isFalse()
            assertThat(mismatchState.submitEnabled).isFalse()

            mismatchState.eventSink(ChangePasswordEvents.SetConfirmNewPassword("AStrongPassword!123"))
            val validState = awaitItem()
            assertThat(validState.submitEnabled).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `present - submit success clears passwords and posts snackbar`() = runTest {
        val recorder = lambdaRecorder<String, String, Boolean, Boolean, Result<Unit>> { _, _, _, _ ->
            Result.success(Unit)
        }
        createPresenter(
            changePasswordUseCase = FakeChangePasswordUseCase(recorder),
        ).test {
            val initialState = awaitItem()
            initialState.eventSink(ChangePasswordEvents.SetCurrentPassword("CurrentPassword!123"))
            val stateWithCurrentPassword = awaitItem()
            stateWithCurrentPassword.eventSink(ChangePasswordEvents.SetNewPassword("NewPassword!123"))
            val stateWithNewPassword = awaitItem()
            stateWithNewPassword.eventSink(ChangePasswordEvents.SetConfirmNewPassword("NewPassword!123"))
            val validState = awaitItem()

            validState.eventSink(ChangePasswordEvents.Submit)

            assertThat(awaitState { it.updateAction == AsyncAction.Loading }.updateAction).isEqualTo(AsyncAction.Loading)
            val resultState = awaitState {
                it.updateAction == AsyncAction.Uninitialized &&
                    it.formState == ChangePasswordFormState.Default &&
                    it.snackbarMessage != null
            }
            assertThat(resultState.snackbarMessage).isNotNull()
            recorder.assertions().isCalledOnce().with(
                value("CurrentPassword!123"),
                value("NewPassword!123"),
                value(true),
                value(false),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `present - submit failure clears passwords and renders error`() = runTest {
        val failure = IllegalStateException("Failed")
        val recorder = lambdaRecorder<String, String, Boolean, Boolean, Result<Unit>> { _, _, _, _ ->
            Result.failure(failure)
        }
        createPresenter(
            changePasswordUseCase = FakeChangePasswordUseCase(recorder),
        ).test {
            val initialState = awaitItem()
            initialState.eventSink(ChangePasswordEvents.SetCurrentPassword("CurrentPassword!123"))
            val stateWithCurrentPassword = awaitItem()
            stateWithCurrentPassword.eventSink(ChangePasswordEvents.SetNewPassword("NewPassword!123"))
            val stateWithNewPassword = awaitItem()
            stateWithNewPassword.eventSink(ChangePasswordEvents.SetConfirmNewPassword("NewPassword!123"))
            val validState = awaitItem()

            validState.eventSink(ChangePasswordEvents.Submit)

            assertThat(awaitState { it.updateAction == AsyncAction.Loading }.updateAction).isEqualTo(AsyncAction.Loading)
            val failureState = awaitState { it.updateAction == AsyncAction.Failure(failure) }
            assertThat(failureState.formState).isEqualTo(ChangePasswordFormState.Default)
            recorder.assertions().isCalledOnce().with(
                value("CurrentPassword!123"),
                value("NewPassword!123"),
                value(true),
                value(false),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createPresenter(
        changePasswordUseCase: ChangePasswordUseCase = FakeChangePasswordUseCase(),
    ) = moleculeFlow(RecompositionMode.Immediate) {
        ChangePasswordPresenter(
            changePasswordUseCase = changePasswordUseCase,
            snackbarDispatcher = SnackbarDispatcher(),
        ).present()
    }

    private suspend fun ReceiveTurbine<ChangePasswordState>.awaitState(predicate: (ChangePasswordState) -> Boolean): ChangePasswordState {
        while (true) {
            val state = awaitItem()
            if (predicate(state)) return state
        }
    }
}

private class FakeChangePasswordUseCase(
    private val callback: suspend (String, String, Boolean, Boolean) -> Result<Unit> = { _, _, _, _ ->
        Result.success(Unit)
    },
) : ChangePasswordUseCase {
    override suspend fun invoke(
        currentPassword: String,
        newPassword: String,
        logoutDevices: Boolean,
        logoutCurrentSessionOnSuccess: Boolean,
    ): Result<Unit> {
        return callback(currentPassword, newPassword, logoutDevices, logoutCurrentSessionOnSuccess)
    }
}
