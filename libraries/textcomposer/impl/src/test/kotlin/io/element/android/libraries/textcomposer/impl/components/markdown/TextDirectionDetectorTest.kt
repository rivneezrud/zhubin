/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.textcomposer.impl.components.markdown

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.androidutils.text.MessageTextDirection
import io.element.android.libraries.androidutils.text.detectTextDirection
import org.junit.Test

class TextDirectionDetectorTest {
    @Test
    fun `returns null for empty or whitespace only text`() {
        assertThat(detectTextDirection("")).isNull()
        assertThat(detectTextDirection("   ")).isNull()
        assertThat(detectTextDirection(null)).isNull()
    }

    @Test
    fun `detects rtl from first meaningful arabic script character`() {
        assertThat(detectTextDirection(" سلام")).isEqualTo(MessageTextDirection.Rtl)
        assertThat(detectTextDirection("\n\tچت")).isEqualTo(MessageTextDirection.Rtl)
        assertThat(detectTextDirection("🔴🔴 ایران موافقت کرد")).isEqualTo(MessageTextDirection.Rtl)
    }

    @Test
    fun `detects ltr from latin and numeric characters`() {
        assertThat(detectTextDirection(" hello")).isEqualTo(MessageTextDirection.Ltr)
        assertThat(detectTextDirection(" 123test")).isEqualTo(MessageTextDirection.Ltr)
        assertThat(detectTextDirection("123 https://example.org !!! Hello")).isEqualTo(MessageTextDirection.Ltr)
    }
}
