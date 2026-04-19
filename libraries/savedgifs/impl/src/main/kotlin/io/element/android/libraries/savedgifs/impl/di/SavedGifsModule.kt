package io.element.android.libraries.savedgifs.impl.di

import android.content.Context
import androidx.room.Room
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import io.element.android.libraries.di.annotations.ApplicationContext
import io.element.android.libraries.savedgifs.impl.db.SavedGifsDao
import io.element.android.libraries.savedgifs.impl.db.SavedGifsDatabase

@ContributesTo(AppScope::class)
interface SavedGifsModule {
    companion object {
        @Provides
        fun provideSavedGifsDatabase(@ApplicationContext context: Context): SavedGifsDatabase = Room.databaseBuilder(
            context,
            SavedGifsDatabase::class.java,
            "saved_gifs.db",
        ).build()

        @Provides
        fun provideSavedGifsDao(database: SavedGifsDatabase): SavedGifsDao = database.savedGifsDao()
    }
}
