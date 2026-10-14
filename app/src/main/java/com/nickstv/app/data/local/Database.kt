package com.nickstv.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val id: String,              // String ID from API
    val mediaType: String,
    val title: String,
    val posterUrl: String,
    val backdropUrl: String,
    val year: String,
    val rating: Double,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "continue_watching")
data class ContinueWatchingEntity(
    @PrimaryKey val id: String,              // String ID from API
    val mediaType: String,
    val title: String,
    val posterUrl: String,
    val backdropUrl: String,
    val year: String,
    val season: Int?,
    val episode: Int?,
    val episodeTitle: String?,
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
    fun getAllFlow(): Flow<List<WatchlistEntity>>

    @Query("SELECT * FROM watchlist WHERE id = :id AND mediaType = :type LIMIT 1")
    suspend fun getItem(id: String, type: String): WatchlistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WatchlistEntity)

    @Delete
    suspend fun delete(item: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE id = :id AND mediaType = :type")
    suspend fun deleteById(id: String, type: String)

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE id = :id AND mediaType = :type)")
    fun isSavedFlow(id: String, type: String): Flow<Boolean>
}

@Dao
interface ContinueWatchingDao {
    @Query("SELECT * FROM continue_watching ORDER BY updatedAt DESC LIMIT 20")
    fun getAllFlow(): Flow<List<ContinueWatchingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ContinueWatchingEntity)

    @Query("DELETE FROM continue_watching WHERE id = :id AND mediaType = :type")
    suspend fun deleteById(id: String, type: String)

    @Query("SELECT * FROM continue_watching WHERE id = :id AND mediaType = :type LIMIT 1")
    suspend fun getItem(id: String, type: String): ContinueWatchingEntity?
}

@Database(
    entities = [WatchlistEntity::class, ContinueWatchingEntity::class],
    version = 2,
    exportSchema = false
)
abstract class NicksTVDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao
    abstract fun continueWatchingDao(): ContinueWatchingDao
}
