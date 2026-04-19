package io.element.android.libraries.savedgifs.api

import android.net.Uri

data class SavedGif(
    val id: Long,
    val sessionId: String,
    val filename: String,
    val mimeType: String,
    val fileSize: Long,
    val uri: Uri,
    val createdAtMillis: Long,
    val lastUsedAtMillis: Long?,
)
