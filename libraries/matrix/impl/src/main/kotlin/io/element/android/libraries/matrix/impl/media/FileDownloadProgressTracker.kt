/*
 * Copyright (c) 2026 Zhubin
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.media

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class FileDownloadProgressTracker(
    private val destinationFile: File,
    private val expectedContentLength: Long,
    private val coroutineScope: CoroutineScope,
    private val pollIntervalMillis: Long = 200L,
) {
    data class Progress(
        val bytesTransferred: Long,
        val contentLength: Long,
    )

    private val _progress = MutableStateFlow(
        Progress(
            bytesTransferred = 0L,
            contentLength = expectedContentLength,
        )
    )
    val progress: StateFlow<Progress> = _progress

    private var pollingJob: Job? = null

    fun start() {
        if (pollingJob != null) return

        val existingBytes = resolveTrackedFile()?.length() ?: 0L
        if (existingBytes >= expectedContentLength) {
            _progress.value = Progress(
                bytesTransferred = expectedContentLength,
                contentLength = expectedContentLength,
            )
            return
        }

        pollingJob = coroutineScope.launch {
            var lastBytes = -1L
            while (isActive) {
                val trackedFile = resolveTrackedFile()
                val currentBytes = trackedFile?.length() ?: 0L
                if (currentBytes != lastBytes) {
                    lastBytes = currentBytes
                    _progress.value = Progress(
                        bytesTransferred = currentBytes.coerceAtMost(expectedContentLength),
                        contentLength = expectedContentLength,
                    )
                }
                if (currentBytes >= expectedContentLength) break
                delay(pollIntervalMillis)
            }
        }
    }

    suspend fun stop() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun resolveTrackedFile(): File? {
        if (destinationFile.exists()) return destinationFile

        val parent = destinationFile.parentFile ?: return null
        val files = parent.listFiles().orEmpty().filter { it.isFile }
        if (files.isEmpty()) return null

        return files.firstOrNull { it.name == destinationFile.name }
            ?: files.firstOrNull { it.name.startsWith(destinationFile.nameWithoutExtension) }
            ?: files.maxByOrNull { it.lastModified() }
    }
}
