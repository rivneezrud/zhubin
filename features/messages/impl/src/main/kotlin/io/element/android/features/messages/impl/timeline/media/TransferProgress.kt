/*
 * Copyright (c) 2026 Zhubin
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.media

data class TransferProgress(
    val bytesTransferred: Long,
    val contentLength: Long,
) {
    val fraction: Float
        get() = if (contentLength <= 0L) 0f else (bytesTransferred.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)

    val percentage: Int
        get() = (fraction * 100f).toInt()
}
