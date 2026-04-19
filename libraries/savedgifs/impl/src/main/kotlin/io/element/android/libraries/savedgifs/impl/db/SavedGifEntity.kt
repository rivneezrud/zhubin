package io.element.android.libraries.savedgifs.impl.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "saved_gifs",
    indices = [
        Index(value = ["session_id", "content_hash"], unique = true),
        Index(value = ["session_id", "created_at"]),
        Index(value = ["session_id", "last_used_at"]),
    ],
)
data class SavedGifEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    @ColumnInfo(name = "filename")
    val filename: String,
    @ColumnInfo(name = "mime_type")
    val mimeType: String,
    @ColumnInfo(name = "file_size")
    val fileSize: Long,
    @ColumnInfo(name = "local_path")
    val localPath: String,
    @ColumnInfo(name = "content_hash")
    val contentHash: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "last_used_at")
    val lastUsedAt: Long?,
)
