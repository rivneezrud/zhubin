package io.element.android.libraries.savedgifs.impl

import android.content.Context
import android.net.Uri
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import io.element.android.libraries.core.hash.md5
import io.element.android.libraries.core.mimetype.MimeTypes
import io.element.android.libraries.core.mimetype.MimeTypes.normalizeMimeType
import io.element.android.libraries.di.annotations.ApplicationContext
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.savedgifs.api.SavedGif
import io.element.android.libraries.savedgifs.api.SavedGifsStore
import io.element.android.libraries.savedgifs.impl.db.SavedGifEntity
import io.element.android.libraries.savedgifs.impl.db.SavedGifsDao
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DefaultSavedGifsStore(
    @ApplicationContext private val context: Context,
    private val dispatchers: CoroutineDispatchers,
    private val savedGifsDao: SavedGifsDao,
) : SavedGifsStore {
    override fun savedGifs(sessionId: SessionId): Flow<List<SavedGif>> =
        savedGifsDao.observeSaved(sessionId.value).map { list -> list.map { it.toModel() } }

    override fun recentGifs(sessionId: SessionId, limit: Int): Flow<List<SavedGif>> =
        savedGifsDao.observeRecent(sessionId.value, limit).map { list -> list.map { it.toModel() } }

    override suspend fun saveGif(
        sessionId: SessionId,
        uri: Uri,
        filename: String,
        mimeType: String,
        fileSize: Long?,
    ): Result<SavedGif> = upsertGif(
        sessionId = sessionId,
        uri = uri,
        filename = filename,
        mimeType = mimeType,
        fileSize = fileSize,
        lastUsedAt = null,
    )

    override suspend fun markGifAsRecentlyUsed(
        sessionId: SessionId,
        uri: Uri,
        filename: String,
        mimeType: String,
        fileSize: Long?,
    ): Result<SavedGif> = upsertGif(
        sessionId = sessionId,
        uri = uri,
        filename = filename,
        mimeType = mimeType,
        fileSize = fileSize,
        lastUsedAt = System.currentTimeMillis(),
    )

    override suspend fun deleteGif(sessionId: SessionId, gifId: Long): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            val existing = savedGifsDao.getById(sessionId.value, gifId) ?: return@runCatching
            File(existing.localPath).takeIf { it.exists() }?.delete()
            savedGifsDao.deleteById(sessionId.value, gifId)
        }
    }

    private suspend fun upsertGif(
        sessionId: SessionId,
        uri: Uri,
        filename: String,
        mimeType: String,
        fileSize: Long?,
        lastUsedAt: Long?,
    ): Result<SavedGif> = withContext(dispatchers.io) {
        runCatching {
            val normalizedMimeType = mimeType.normalizeMimeType()
            require(normalizedMimeType == MimeTypes.Gif) { "Only GIF files can be saved" }

            val targetDirectory = File(context.filesDir, "saved_gifs/${sessionId.value.md5()}").apply { mkdirs() }
            val tempFile = File.createTempFile("saved_gif_", ".tmp", targetDirectory)
            val copied = copyToFile(uri = uri, target = tempFile)
            val targetFile = File(targetDirectory, "${copied.contentHash}.gif")
            if (targetFile.exists()) {
                tempFile.delete()
            } else if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            val safeFilename = filename.ensureGifExtension()
            val size = fileSize ?: copied.fileSize
            val existing = savedGifsDao.getByContentHash(sessionId.value, copied.contentHash)
            if (existing == null) {
                val entity = SavedGifEntity(
                    sessionId = sessionId.value,
                    filename = safeFilename,
                    mimeType = MimeTypes.Gif,
                    fileSize = size,
                    localPath = targetFile.absolutePath,
                    contentHash = copied.contentHash,
                    createdAt = System.currentTimeMillis(),
                    lastUsedAt = lastUsedAt,
                )
                val id = savedGifsDao.insert(entity)
                entity.copy(id = id).toModel()
            } else {
                savedGifsDao.updateMetadata(
                    id = existing.id,
                    filename = safeFilename,
                    mimeType = MimeTypes.Gif,
                    fileSize = size,
                    localPath = targetFile.absolutePath,
                    lastUsedAt = lastUsedAt ?: existing.lastUsedAt,
                )
                existing.copy(
                    filename = safeFilename,
                    mimeType = MimeTypes.Gif,
                    fileSize = size,
                    localPath = targetFile.absolutePath,
                    lastUsedAt = lastUsedAt ?: existing.lastUsedAt,
                ).toModel()
            }
        }
    }

    private fun copyToFile(uri: Uri, target: File): CopiedGif {
        val digest = MessageDigest.getInstance("SHA-256")
        var totalBytes = 0L
        context.contentResolver.openInputStream(uri).use { inputStream ->
            requireNotNull(inputStream) { "Unable to read GIF data from $uri" }
            FileOutputStream(target).use { outputStream ->
                copyAndDigest(inputStream, outputStream = outputStream, digest = digest) { copied ->
                    totalBytes += copied
                }
            }
        }
        return CopiedGif(
            contentHash = digest.digest().toHex(),
            fileSize = totalBytes,
        )
    }

    private fun copyAndDigest(
        inputStream: InputStream,
        outputStream: FileOutputStream,
        digest: MessageDigest,
        onBytesCopied: (Long) -> Unit,
    ) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = inputStream.read(buffer)
            if (count <= 0) return
            digest.update(buffer, 0, count)
            outputStream.write(buffer, 0, count)
            onBytesCopied(count.toLong())
        }
    }
}

private data class CopiedGif(
    val contentHash: String,
    val fileSize: Long,
)

private fun SavedGifEntity.toModel(): SavedGif = SavedGif(
    id = id,
    sessionId = sessionId,
    filename = filename,
    mimeType = mimeType,
    fileSize = fileSize,
    uri = Uri.fromFile(File(localPath)),
    createdAtMillis = createdAt,
    lastUsedAtMillis = lastUsedAt,
)

private fun String.ensureGifExtension(): String = if (endsWith(".gif", ignoreCase = true)) this else "$this.gif"

private fun ByteArray.toHex(): String = joinToString(separator = "") { byte ->
    "%02x".format(byte)
}
