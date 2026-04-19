/*
 * Copyright (c) 2026 Zhubin
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.androidutils.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TextDirectionTest {
    @Test
    fun `returns null for empty or whitespace only text`() {
        assertThat(detectTextDirection("")).isNull()
        assertThat(detectTextDirection("   ")).isNull()
        assertThat(detectTextDirection(null)).isNull()
    }

    @Test
    fun `detects rtl when emoji prefix is followed by persian text`() {
        assertThat(detectTextDirection("🔴🔴 ایران موافقت کرد")).isEqualTo(MessageTextDirection.Rtl)
    }

    @Test
    fun `ignores urls punctuation and numbers when detecting dominant direction`() {
        val text = "123 https://example.org !!! سلام hello"

        assertThat(detectTextDirection(text)).isEqualTo(MessageTextDirection.Rtl)
    }

    @Test
    fun `defaults to ltr when no meaningful letters remain`() {
        assertThat(detectTextDirection("🔴 123 https://example.org ...")).isEqualTo(MessageTextDirection.Ltr)
    }
}
