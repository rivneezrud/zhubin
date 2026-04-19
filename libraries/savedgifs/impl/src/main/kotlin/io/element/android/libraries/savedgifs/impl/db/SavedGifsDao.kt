package io.element.android.libraries.savedgifs.impl.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedGifsDao {
    @Query("SELECT * FROM saved_gifs WHERE session_id = :sessionId ORDER BY created_at DESC")
    fun observeSaved(sessionId: String): Flow<List<SavedGifEntity>>

    @Query("SELECT * FROM saved_gifs WHERE session_id = :sessionId AND last_used_at IS NOT NULL ORDER BY last_used_at DESC LIMIT :limit")
    fun observeRecent(sessionId: String, limit: Int): Flow<List<SavedGifEntity>>

    @Query("SELECT * FROM saved_gifs WHERE session_id = :sessionId AND content_hash = :contentHash LIMIT 1")
    suspend fun getByContentHash(sessionId: String, contentHash: String): SavedGifEntity?

    @Query("SELECT * FROM saved_gifs WHERE session_id = :sessionId AND id = :gifId LIMIT 1")
    suspend fun getById(sessionId: String, gifId: Long): SavedGifEntity?

    @Insert
    suspend fun insert(entity: SavedGifEntity): Long

    @Query(
        """
        UPDATE saved_gifs
        SET filename = :filename,
            mime_type = :mimeType,
            file_size = :fileSize,
            local_path = :localPath,
            last_used_at = :lastUsedAt
        WHERE id = :id
        """
    )
    suspend fun updateMetadata(
        id: Long,
        filename: String,
        mimeType: String,
        fileSize: Long,
        localPath: String,
        lastUsedAt: Long?,
    )

    @Query("DELETE FROM saved_gifs WHERE session_id = :sessionId AND id = :gifId")
    suspend fun deleteById(sessionId: String, gifId: Long)
}
