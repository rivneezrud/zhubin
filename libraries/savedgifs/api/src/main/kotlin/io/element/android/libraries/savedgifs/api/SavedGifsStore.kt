package io.element.android.libraries.savedgifs.api

import android.net.Uri
import io.element.android.libraries.matrix.api.core.SessionId
import kotlinx.coroutines.flow.Flow

interface SavedGifsStore {
    fun savedGifs(sessionId: SessionId): Flow<List<SavedGif>>
    fun recentGifs(sessionId: SessionId, limit: Int = DEFAULT_RECENT_LIMIT): Flow<List<SavedGif>>
    suspend fun saveGif(
        sessionId: SessionId,
        uri: Uri,
        filename: String,
        mimeType: String,
        fileSize: Long?,
    ): Result<SavedGif>

    suspend fun markGifAsRecentlyUsed(
        sessionId: SessionId,
        uri: Uri,
        filename: String,
        mimeType: String,
        fileSize: Long?,
    ): Result<SavedGif>

    suspend fun deleteGif(sessionId: SessionId, gifId: Long): Result<Unit>

    companion object {
        const val DEFAULT_RECENT_LIMIT = 24
    }
}
