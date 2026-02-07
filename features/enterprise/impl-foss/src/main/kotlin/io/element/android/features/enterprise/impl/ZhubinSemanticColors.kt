/*
 * Copyright (c) 2026 Zhubin
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.enterprise.impl

import io.element.android.compound.annotations.CoreColorToken
import io.element.android.compound.colors.SemanticColorsLightDark
import io.element.android.compound.tokens.generated.SemanticColors
import io.element.android.compound.tokens.generated.compoundColorsDark
import io.element.android.compound.tokens.generated.compoundColorsLight
import io.element.android.compound.tokens.generated.internal.DarkColorTokens
import io.element.android.compound.tokens.generated.internal.LightColorTokens

@OptIn(CoreColorToken::class)
internal object ZhubinSemanticColors {
    val light: SemanticColors = compoundColorsLight.copy(
        bgAccentHovered = LightColorTokens.colorBlue1000,
        bgAccentPressed = LightColorTokens.colorBlue1100,
        bgAccentRest = LightColorTokens.colorBlue900,
        bgAccentSelected = LightColorTokens.colorAlphaBlue300,
        bgBadgeAccent = LightColorTokens.colorAlphaBlue400,
        borderAccentSubtle = LightColorTokens.colorBlue700,
        gradientActionStop1 = LightColorTokens.colorBlue500,
        gradientActionStop2 = LightColorTokens.colorBlue700,
        gradientActionStop3 = LightColorTokens.colorBlue900,
        gradientActionStop4 = LightColorTokens.colorBlue1100,
        gradientSubtleStop1 = LightColorTokens.colorAlphaBlue500,
        gradientSubtleStop2 = LightColorTokens.colorAlphaBlue400,
        gradientSubtleStop3 = LightColorTokens.colorAlphaBlue300,
        gradientSubtleStop4 = LightColorTokens.colorAlphaBlue200,
        gradientSubtleStop5 = LightColorTokens.colorAlphaBlue100,
        iconAccentPrimary = LightColorTokens.colorBlue900,
        iconAccentTertiary = LightColorTokens.colorBlue800,
        textActionAccent = LightColorTokens.colorBlue900,
        textBadgeAccent = LightColorTokens.colorBlue1100,
    )

    val dark: SemanticColors = compoundColorsDark.copy(
        bgAccentHovered = DarkColorTokens.colorBlue1000,
        bgAccentPressed = DarkColorTokens.colorBlue1100,
        bgAccentRest = DarkColorTokens.colorBlue900,
        bgAccentSelected = DarkColorTokens.colorAlphaBlue300,
        bgBadgeAccent = DarkColorTokens.colorAlphaBlue500,
        borderAccentSubtle = DarkColorTokens.colorBlue700,
        gradientActionStop1 = DarkColorTokens.colorBlue1100,
        gradientActionStop2 = DarkColorTokens.colorBlue900,
        gradientActionStop3 = DarkColorTokens.colorBlue700,
        gradientActionStop4 = DarkColorTokens.colorBlue500,
        gradientSubtleStop1 = DarkColorTokens.colorAlphaBlue500,
        gradientSubtleStop2 = DarkColorTokens.colorAlphaBlue400,
        gradientSubtleStop3 = DarkColorTokens.colorAlphaBlue300,
        gradientSubtleStop4 = DarkColorTokens.colorAlphaBlue200,
        gradientSubtleStop5 = DarkColorTokens.colorAlphaBlue100,
        iconAccentPrimary = DarkColorTokens.colorBlue900,
        iconAccentTertiary = DarkColorTokens.colorBlue800,
        textActionAccent = DarkColorTokens.colorBlue900,
        textBadgeAccent = DarkColorTokens.colorBlue1100,
    )

    val lightDark: SemanticColorsLightDark = SemanticColorsLightDark(
        light = light,
        dark = dark,
    )
}
