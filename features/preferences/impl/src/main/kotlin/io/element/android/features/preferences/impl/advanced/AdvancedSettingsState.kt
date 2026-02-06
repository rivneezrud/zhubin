/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.advanced

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import androidx.core.os.LocaleListCompat
import io.element.android.features.preferences.impl.R
import io.element.android.libraries.designsystem.components.preferences.DropdownOption
import io.element.android.libraries.preferences.api.store.VideoCompressionPreset
import io.element.android.libraries.ui.strings.CommonStrings

data class AdvancedSettingsState(
    val isDeveloperModeEnabled: Boolean,
    val isSharePresenceEnabled: Boolean,
    val mediaOptimizationState: MediaOptimizationState?,
    val theme: ThemeOption,
    val appLanguage: AppLanguageOption,
    val mediaPreviewConfigState: MediaPreviewConfigState,
    val eventSink: (AdvancedSettingsEvents) -> Unit
)

sealed interface MediaOptimizationState {
    data class AllMedia(val isEnabled: Boolean) : MediaOptimizationState
    data class Split(
        val compressImages: Boolean,
        val videoPreset: VideoCompressionPreset,
    ) : MediaOptimizationState

    val shouldCompressImages: Boolean get() = when (this) {
        is AllMedia -> isEnabled
        is Split -> compressImages
    }
}

enum class ThemeOption : DropdownOption {
    System {
        @Composable
        @ReadOnlyComposable
        override fun getText(): String = stringResource(CommonStrings.common_system)
    },
    Dark {
        @Composable
        @ReadOnlyComposable
        override fun getText(): String = stringResource(CommonStrings.common_dark)
    },
    Light {
        @Composable
        @ReadOnlyComposable
        override fun getText(): String = stringResource(CommonStrings.common_light)
    }
}

enum class AppLanguageOption(
    private val languageTag: String?,
) : DropdownOption {
    System(null) {
        @Composable
        @ReadOnlyComposable
        override fun getText(): String = stringResource(CommonStrings.common_system)
    },
    English("en") {
        @Composable
        @ReadOnlyComposable
        override fun getText(): String = stringResource(R.string.screen_advanced_settings_app_language_english)
    },
    Persian("fa") {
        @Composable
        @ReadOnlyComposable
        override fun getText(): String = stringResource(R.string.screen_advanced_settings_app_language_persian)
    };

    fun toLocaleList(): LocaleListCompat {
        return if (languageTag == null) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageTag)
        }
    }

    companion object {
        fun fromLocales(locales: LocaleListCompat): AppLanguageOption {
            if (locales.isEmpty) return System
            val tag = locales[0]?.toLanguageTag()?.lowercase() ?: return System
            return when {
                tag.startsWith("fa") -> Persian
                tag.startsWith("en") -> English
                else -> System
            }
        }
    }
}
