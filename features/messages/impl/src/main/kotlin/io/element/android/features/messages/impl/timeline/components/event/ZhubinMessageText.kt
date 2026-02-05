/*
 * Copyright (c) 2026 Zhubin
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.components.event

import android.text.Spanned
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.android.toAnnotatedString
import androidx.compose.ui.unit.em
import io.element.android.features.messages.impl.R
import io.element.android.features.messages.impl.timeline.components.IRAN_FLAG_INLINE_ID
import io.element.android.features.messages.impl.timeline.components.IRAN_FLAG_UNICODE
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.wysiwyg.link.Link
import kotlinx.collections.immutable.persistentMapOf

@Composable
internal fun ZhubinMessageText(
    text: CharSequence,
    onLinkClick: (Link) -> Unit,
    onLinkLongClick: (Link) -> Unit,
    modifier: Modifier = Modifier,
    onTextLayout: (TextLayoutResult) -> Unit = {},
) {
    val annotated = remember(text) { text.toAnnotatedStringCompat() }
    val annotatedWithInline = remember(annotated) { annotated.replaceIranFlagWithInlineContent() }
    val inlineContent = remember {
        persistentMapOf(
            IRAN_FLAG_INLINE_ID to androidx.compose.foundation.text.InlineTextContent(
                placeholder = Placeholder(
                    width = 1.em,
                    height = 1.em,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_flag_iran_lionsun),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        )
    }

    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val tapModifier = Modifier.pointerInput(annotatedWithInline) {
        detectTapGestures(
            onTap = { position ->
                val link = annotatedWithInline.linkAtPosition(position, layoutResult)
                if (link != null) onLinkClick(link)
            },
            onLongPress = { position ->
                val link = annotatedWithInline.linkAtPosition(position, layoutResult)
                if (link != null) onLinkLongClick(link)
            }
        )
    }

    Text(
        text = annotatedWithInline,
        inlineContent = inlineContent,
        modifier = modifier.then(tapModifier),
        onTextLayout = { layout ->
            layoutResult = layout
            onTextLayout(layout)
        },
    )
}

private fun CharSequence.toAnnotatedStringCompat(): AnnotatedString {
    return when (this) {
        is AnnotatedString -> this
        is Spanned -> this.toAnnotatedString()
        else -> AnnotatedString(this.toString())
    }
}

private fun AnnotatedString.replaceIranFlagWithInlineContent(): AnnotatedString {
    if (!text.contains(IRAN_FLAG_UNICODE)) return this
    val builder = AnnotatedString.Builder()
    var index = 0
    while (index < text.length) {
        val next = text.indexOf(IRAN_FLAG_UNICODE, startIndex = index)
        if (next < 0) {
            builder.append(subSequence(index, text.length))
            break
        }
        if (next > index) {
            builder.append(subSequence(index, next))
        }
        builder.appendInlineContent(IRAN_FLAG_INLINE_ID, IRAN_FLAG_UNICODE)
        index = next + IRAN_FLAG_UNICODE.length
    }
    return builder.toAnnotatedString()
}

private fun AnnotatedString.linkAtPosition(
    position: androidx.compose.ui.geometry.Offset,
    layoutResult: TextLayoutResult?,
): Link? {
    val layout = layoutResult ?: return null
    val offset = layout.getOffsetForPosition(position)
    val urlRange = getUrlAnnotations(start = offset, end = offset).firstOrNull()
    if (urlRange != null) {
        val linkText = text.substring(urlRange.start, urlRange.end)
        return Link(urlRange.item.url, linkText)
    }
    val stringRange = getStringAnnotations(start = offset, end = offset)
        .firstOrNull { it.tag.equals("URL", ignoreCase = true) || it.tag.equals("LINK", ignoreCase = true) }
        ?: return null
    val linkText = text.substring(stringRange.start, stringRange.end)
    return Link(stringRange.item, linkText)
}
