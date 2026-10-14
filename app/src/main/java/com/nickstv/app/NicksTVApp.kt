package com.nickstv.app

import android.app.Application
import androidx.room.Room
import com.nickstv.app.data.local.NicksTVDatabase
import com.nickstv.app.data.remote.NetworkModule
import com.nickstv.app.repository.MediaRepository
import com.nickstv.app.repository.UpdateRepository
import com.nickstv.app.repository.WatchlistRepository

class NicksTVApp : Application() {

    lateinit var database: NicksTVDatabase
        private set

    lateinit var mediaRepository: MediaRepository
        private set

    lateinit var watchlistRepository: WatchlistRepository
        private set

    lateinit var updateRepository: UpdateRepository
        private set

    override fun onCreate() {
        super.onCreate()

        NetworkModule.init(cacheDir)

        database = Room.databaseBuilder(
            applicationContext,
            NicksTVDatabase::class.java,
            "nickstv.db"
        )
            .fallbackToDestructiveMigration()
            .build()

        mediaRepository = MediaRepository()
        watchlistRepository = WatchlistRepository(
            database.watchlistDao(),
            database.continueWatchingDao()
        )
        updateRepository = UpdateRepository()

        instance = this
    }

    companion object {
        lateinit var instance: NicksTVApp
            private set
    }
}
