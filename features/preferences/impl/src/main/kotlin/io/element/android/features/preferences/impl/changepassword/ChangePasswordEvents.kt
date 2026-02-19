/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.changepassword

sealed interface ChangePasswordEvents {
    data class SetCurrentPassword(val password: String) : ChangePasswordEvents
    data class SetNewPassword(val password: String) : ChangePasswordEvents
    data class SetConfirmNewPassword(val password: String) : ChangePasswordEvents
    data object Submit : ChangePasswordEvents
    data object ClearActionState : ChangePasswordEvents
}
