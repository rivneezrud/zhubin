/*
 * Copyright (c) 2026 Zhubin
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.media

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.element.android.libraries.di.RoomScope
import io.element.android.libraries.di.annotations.AppCoroutineScope
import io.element.android.libraries.matrix.api.media.MediaSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@SingleIn(RoomScope::class)
@Inject
class MediaProgressPresenter(
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope,
) {
    private val downloadProgress = MutableStateFlow<Map<String, TransferProgress>>(emptyMap())

    fun progressFor(mediaSource: MediaSource): StateFlow<TransferProgress?> {
        val key = mediaSource.transferKey()
        return downloadProgress
            .map { it[key] }
            .stateIn(
                scope = appCoroutineScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = downloadProgress.value[key],
            )
    }

    fun onDownloadProgress(
        key: String,
        bytesTransferred: Long,
        contentLength: Long,
    ) {
        if (contentLength <= 0L) return

        val next = TransferProgress(
            bytesTransferred = bytesTransferred,
            contentLength = contentLength,
        )

        downloadProgress.value = buildMap {
            putAll(downloadProgress.value)
            if (bytesTransferred >= contentLength) {
                remove(key)
            } else {
                put(key, next)
            }
        }
    }

    fun clear(key: String) {
        downloadProgress.value = buildMap {
            putAll(downloadProgress.value)
            remove(key)
        }
    }

    fun clear(mediaSource: MediaSource) {
        clear(mediaSource.transferKey())
    }
}

fun MediaSource.transferKey(): String = json?.let { "$url|$it" } ?: url
