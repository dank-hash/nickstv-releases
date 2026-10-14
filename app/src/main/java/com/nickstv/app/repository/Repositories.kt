package com.nickstv.app.repository

import com.nickstv.app.data.local.*
import com.nickstv.app.data.remote.NetworkModule
import com.nickstv.app.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Response

// ── Safe API call helper ──────────────────────────────────────────────────────

private suspend fun <T> safeApiCall(call: suspend () -> Response<T>): UiState<T> {
    return try {
        val response = call()
        if (response.isSuccessful && response.body() != null) {
            UiState.Success(response.body()!!)
        } else {
            UiState.Error("HTTP ${response.code()}: ${response.message()}")
        }
    } catch (e: Exception) {
        UiState.Error(e.message ?: "Unknown network error", e)
    }
}

// ── Media Repository ─────────────────────────────────────────────────────────

class MediaRepository(
    private val api: com.nickstv.app.api.FrameXTVApiService = NetworkModule.frameXTVApi
) {

    fun getHome(): Flow<UiState<HomeResponse>> = flow {
        emit(UiState.Loading)
        emit(safeApiCall { api.getHome() })
    }

    fun getMeta(id: String, type: String, season: Int? = null, episode: Int? = null): Flow<UiState<DetailResponse>> = flow {
        emit(UiState.Loading)
        emit(safeApiCall { api.getMeta(id, type, season, episode) })
    }

    fun search(query: String, type: String = "all", page: Int = 1): Flow<UiState<SearchResponse>> = flow {
        emit(UiState.Loading)
        emit(safeApiCall { api.search(query, type, page) })
    }

    fun discover(
        type: String,
        category: String? = null,
        genre: String? = null,
        year: Int? = null,
        sort: String? = null,
        minRating: Float? = null,
        page: Int = 1
    ): Flow<UiState<DiscoverResponse>> = flow {
        emit(UiState.Loading)
        emit(safeApiCall { api.discover(type, category, genre, year, sort, minRating, page) })
    }

    fun getProviders(type: String = "movie_tv", subType: String? = null): Flow<UiState<ProvidersResponse>> = flow {
        emit(UiState.Loading)
        emit(safeApiCall { api.getProviders(type, subType) })
    }

    fun getStream(
        type: String,
        id: String,
        provider: String? = null,
        season: Int? = null,
        episode: Int? = null
    ): Flow<UiState<StreamResponse>> = flow {
        emit(UiState.Loading)
        emit(safeApiCall { api.getStream(type, id, provider, season, episode) })
    }

    fun getSubtitles(id: String, type: String, season: Int? = null, episode: Int? = null): Flow<UiState<List<Subtitle>>> = flow {
        emit(UiState.Loading)
        emit(safeApiCall { api.getSubtitles(id, type, season, episode) })
    }
}

// ── Watchlist Repository ──────────────────────────────────────────────────────

class WatchlistRepository(
    private val dao: WatchlistDao,
    private val continueDao: ContinueWatchingDao
) {

    val watchlist: Flow<List<WatchlistEntity>> = dao.getAllFlow()

    val continueWatching: Flow<List<ContinueWatchingEntity>> = continueDao.getAllFlow()

    fun isSaved(id: String, type: String): Flow<Boolean> = dao.isSavedFlow(id, type)

    suspend fun toggle(item: WatchlistEntity) {
        val existing = dao.getItem(item.id, item.mediaType)
        if (existing != null) dao.delete(existing) else dao.insert(item)
    }

    suspend fun addToWatchlist(item: WatchlistEntity) = dao.insert(item)

    suspend fun removeFromWatchlist(id: String, type: String) = dao.deleteById(id, type)

    suspend fun saveContinueWatching(item: ContinueWatchingEntity) = continueDao.insert(item)

    suspend fun getContinueWatchingItem(id: String, type: String) = continueDao.getItem(id, type)

    suspend fun removeContinueWatching(id: String, type: String) = continueDao.deleteById(id, type)
}

// ── GitHub / OTA Repository ───────────────────────────────────────────────────

class UpdateRepository(
    private val api: com.nickstv.app.api.GitHubApiService = NetworkModule.githubApi
) {

    suspend fun getLatestRelease(owner: String, repo: String, token: String): UiState<GitHubRelease> {
        return safeApiCall { api.getLatestRelease("token $token", owner, repo) }
    }

    suspend fun getAuthenticatedUser(token: String): UiState<GitHubUser> {
        return safeApiCall { api.getUser("token $token") }
    }
}
