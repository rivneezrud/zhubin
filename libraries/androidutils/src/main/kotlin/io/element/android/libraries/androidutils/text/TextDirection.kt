/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.androidutils.text

enum class MessageTextDirection {
    Ltr,
    Rtl,
}

fun detectTextDirection(text: CharSequence?): MessageTextDirection? {
    val sanitizedText = text
        ?.toString()
        ?.takeIf { it.isNotBlank() }
        ?.removeUrls()
        ?: return null

    val meaningfulLetters = sanitizedText.meaningfulLetterCodePoints()
    if (meaningfulLetters.isEmpty()) {
        return MessageTextDirection.Ltr
    }

    val rtlLetters = meaningfulLetters.count { codePoint ->
        when (Character.getDirectionality(codePoint)) {
            Character.DIRECTIONALITY_RIGHT_TO_LEFT,
            Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC,
            -> true
            else -> false
        }
    }

    return if (rtlLetters.toFloat() / meaningfulLetters.size >= RTL_THRESHOLD) {
        MessageTextDirection.Rtl
    } else {
        MessageTextDirection.Ltr
    }
}

private fun String.meaningfulLetterCodePoints(): List<Int> {
    val codePoints = mutableListOf<Int>()
    var index = 0
    while (index < length) {
        val codePoint = codePointAt(index)
        if (Character.isLetter(codePoint) && !codePoint.isEmojiCodePoint()) {
            codePoints += codePoint
        }
        index += Character.charCount(codePoint)
    }
    return codePoints
}

private fun Int.isEmojiCodePoint(): Boolean {
    return this in EMOJI_MISC_SYMBOLS_START..EMOJI_MISC_SYMBOLS_END ||
        this in EMOJI_SUPPLEMENTARY_START..EMOJI_SUPPLEMENTARY_END
}

private fun String.removeUrls(): String {
    return URL_REGEX.replace(this, " ")
}

private const val RTL_THRESHOLD = 0.5f
private const val EMOJI_MISC_SYMBOLS_START = 0x2600
private const val EMOJI_MISC_SYMBOLS_END = 0x27BF
private const val EMOJI_SUPPLEMENTARY_START = 0x1F000
private const val EMOJI_SUPPLEMENTARY_END = 0x1FFFF

private val URL_REGEX = Regex("""(?i)\b((?:https?://|www\.)\S+)""")
