package io.element.android.libraries.savedgifs.impl.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SavedGifEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class SavedGifsDatabase : RoomDatabase() {
    abstract fun savedGifsDao(): SavedGifsDao
}
