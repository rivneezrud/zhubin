package io.element.android.libraries.savedgifs.test

import android.net.Uri
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.savedgifs.api.SavedGif
import io.element.android.libraries.savedgifs.api.SavedGifsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeSavedGifsStore : SavedGifsStore {
    private val gifs = MutableStateFlow<List<SavedGif>>(emptyList())
    var saveGifResult: Result<SavedGif>? = null
    var recentGifResult: Result<SavedGif>? = null
    var deleteGifResult: Result<Unit> = Result.success(Unit)

    override fun savedGifs(sessionId: SessionId): Flow<List<SavedGif>> = gifs.map { list ->
        list.filter { it.sessionId == sessionId.value }
    }

    override fun recentGifs(sessionId: SessionId, limit: Int): Flow<List<SavedGif>> = gifs.map { list ->
        list.filter { it.sessionId == sessionId.value }
            .sortedByDescending { it.lastUsedAtMillis ?: Long.MIN_VALUE }
            .take(limit)
    }

    override suspend fun saveGif(
        sessionId: SessionId,
        uri: Uri,
        filename: String,
        mimeType: String,
        fileSize: Long?,
    ): Result<SavedGif> = saveGifResult ?: createGif(sessionId, uri, filename, mimeType, fileSize, lastUsedAtMillis = null)

    override suspend fun markGifAsRecentlyUsed(
        sessionId: SessionId,
        uri: Uri,
        filename: String,
        mimeType: String,
        fileSize: Long?,
    ): Result<SavedGif> = recentGifResult ?: createGif(sessionId, uri, filename, mimeType, fileSize, lastUsedAtMillis = System.currentTimeMillis())

    override suspend fun deleteGif(sessionId: SessionId, gifId: Long): Result<Unit> {
        if (deleteGifResult.isSuccess) {
            gifs.value = gifs.value.filterNot { it.sessionId == sessionId.value && it.id == gifId }
        }
        return deleteGifResult
    }

    private fun createGif(
        sessionId: SessionId,
        uri: Uri,
        filename: String,
        mimeType: String,
        fileSize: Long?,
        lastUsedAtMillis: Long?,
    ): Result<SavedGif> {
        val gif = SavedGif(
            id = (gifs.value.maxOfOrNull { it.id } ?: 0L) + 1L,
            sessionId = sessionId.value,
            filename = filename,
            mimeType = mimeType,
            fileSize = fileSize ?: 0L,
            uri = uri,
            createdAtMillis = System.currentTimeMillis(),
            lastUsedAtMillis = lastUsedAtMillis,
        )
        gifs.value = gifs.value.filterNot { it.sessionId == sessionId.value && it.uri == uri } + gif
        return Result.success(gif)
    }
}
