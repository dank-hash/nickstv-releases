package com.nickstv.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickstv.app.NicksTVApp
import com.nickstv.app.data.local.ContinueWatchingEntity
import com.nickstv.app.data.local.WatchlistEntity
import com.nickstv.app.model.*
import com.nickstv.app.repository.MediaRepository
import com.nickstv.app.repository.RealDebridRepository
import com.nickstv.app.repository.WatchlistRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(
    private val mediaRepo: MediaRepository = NicksTVApp.instance.mediaRepository,
    private val watchlistRepo: WatchlistRepository = NicksTVApp.instance.watchlistRepository
) : ViewModel() {

    private val _homeState = MutableStateFlow<UiState<HomeResponse>>(UiState.Loading)
    val homeState: StateFlow<UiState<HomeResponse>> = _homeState.asStateFlow()

    val watchlist: StateFlow<List<WatchlistEntity>> = watchlistRepo.watchlist
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val continueWatching: StateFlow<List<ContinueWatchingEntity>> = watchlistRepo.continueWatching
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadHome()
    }

    fun loadHome() {
        viewModelScope.launch {
            mediaRepo.getHome().collect { _homeState.value = it }
        }
    }
}

class SearchViewModel(
    private val mediaRepo: MediaRepository = NicksTVApp.instance.mediaRepository
) : ViewModel() {

    private val _searchState = MutableStateFlow<UiState<SearchResponse>>(UiState.Empty)
    val searchState: StateFlow<UiState<SearchResponse>> = _searchState.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    init {
        viewModelScope.launch {
            _query
                .debounce(400)
                .filter { it.length >= 2 }
                .distinctUntilChanged()
                .collect { q -> search(q) }
        }
    }

    fun updateQuery(q: String) { _query.value = q }

    fun search(query: String, type: String = "all", page: Int = 1) {
        if (query.isBlank()) { _searchState.value = UiState.Empty; return }
        viewModelScope.launch {
            mediaRepo.search(query, type, page).collect { _searchState.value = it }
        }
    }

    fun clearSearch() {
        _searchState.value = UiState.Empty
        _query.value = ""
    }
}

class DetailViewModel(
    private val mediaRepo: MediaRepository = NicksTVApp.instance.mediaRepository,
    private val watchlistRepo: WatchlistRepository = NicksTVApp.instance.watchlistRepository,
    private val rdRepo: RealDebridRepository = NicksTVApp.instance.realDebridRepository
) : ViewModel() {

    private val _detailState = MutableStateFlow<UiState<DetailResponse>>(UiState.Loading)
    val detailState: StateFlow<UiState<DetailResponse>> = _detailState.asStateFlow()

    private val _selectedSeason = MutableStateFlow(1)
    val selectedSeason: StateFlow<Int> = _selectedSeason.asStateFlow()

    private val _episodeState = MutableStateFlow<UiState<DetailResponse>>(UiState.Empty)
    val episodeState: StateFlow<UiState<DetailResponse>> = _episodeState.asStateFlow()

    // RD sources loaded in background on detail page
    private val _rdSources = MutableStateFlow<List<SourceEntry>>(emptyList())
    val rdSources: StateFlow<List<SourceEntry>> = _rdSources.asStateFlow()

    private val _rdLoading = MutableStateFlow(false)
    val rdLoading: StateFlow<Boolean> = _rdLoading.asStateFlow()

    private val _imdbId = MutableStateFlow<String?>(null)
    val imdbId: StateFlow<String?> = _imdbId.asStateFlow()

    val hasRdToken: Boolean get() = rdRepo.hasToken

    private var _currentId: String = "0"
    private var _currentType: String = "movie"

    fun load(id: String, type: String) {
        _currentId = id
        _currentType = type
        _rdSources.value = emptyList()
        viewModelScope.launch {
            mediaRepo.getMeta(id, type).collect { state ->
                _detailState.value = state
                // Once meta loads, resolve IMDB ID and kick off RD scan
                if (state is UiState.Success && _imdbId.value == null) {
                    resolveAndLoadRd(id, type)
                }
            }
        }
    }

    private fun resolveAndLoadRd(tmdbId: String, type: String) {
        if (!rdRepo.hasToken) return
        viewModelScope.launch {
            val imdb = rdRepo.resolveImdbId(tmdbId, type)
            _imdbId.value = imdb
            if (imdb != null) {
                _rdLoading.value = true
                rdRepo.loadRdSources(
                    imdbId = imdb,
                    mediaType = type,
                    season = if (type == "tv") _selectedSeason.value else null,
                    episode = null, // detail page loads all available; episode filter happens in player
                    onProgress = { partial -> _rdSources.value = partial }
                )
                _rdLoading.value = false
            }
        }
    }

    fun selectSeason(season: Int) {
        _selectedSeason.value = season
        viewModelScope.launch {
            mediaRepo.getMeta(_currentId, "tv", season).collect { _episodeState.value = it }
        }
    }

    fun isSaved(id: String, type: String): Flow<Boolean> = watchlistRepo.isSaved(id, type)

    fun toggleWatchlist(item: WatchlistEntity) = viewModelScope.launch {
        watchlistRepo.toggle(item)
    }

    fun markStarted(id: String, type: String, title: String, poster: String, backdrop: String,
                    year: String, season: Int? = null, episode: Int? = null, episodeTitle: String? = null) {
        viewModelScope.launch {
            watchlistRepo.saveContinueWatching(
                ContinueWatchingEntity(id, type, title, poster, backdrop, year, season, episode, episodeTitle)
            )
        }
    }
}

class BrowseViewModel(
    private val mediaRepo: MediaRepository = NicksTVApp.instance.mediaRepository
) : ViewModel() {

    private val _browseState = MutableStateFlow<UiState<DiscoverResponse>>(UiState.Loading)
    val browseState: StateFlow<UiState<DiscoverResponse>> = _browseState.asStateFlow()

    private val _allResults = MutableStateFlow<List<MediaItem>>(emptyList())
    val allResults: StateFlow<List<MediaItem>> = _allResults.asStateFlow()

    private var currentPage = 1
    private var totalPages = 1
    private var isLoadingMore = false

    private val _selectedType = MutableStateFlow("movie")
    val selectedType: StateFlow<String> = _selectedType.asStateFlow()

    private val _selectedGenre = MutableStateFlow<String?>(null)
    val selectedGenre: StateFlow<String?> = _selectedGenre.asStateFlow()

    private val _selectedYear = MutableStateFlow<Int?>(null)
    val selectedYear: StateFlow<Int?> = _selectedYear.asStateFlow()

    private val _selectedSort = MutableStateFlow<String?>(null)
    val selectedSort: StateFlow<String?> = _selectedSort.asStateFlow()

    init { discover() }

    fun setType(type: String) { _selectedType.value = type; resetAndDiscover() }
    fun setGenre(genre: String?) { _selectedGenre.value = genre; resetAndDiscover() }
    fun setYear(year: Int?) { _selectedYear.value = year; resetAndDiscover() }
    fun setSort(sort: String?) { _selectedSort.value = sort; resetAndDiscover() }

    private fun resetAndDiscover() {
        currentPage = 1
        totalPages = 1
        _allResults.value = emptyList()
        discover()
    }

    fun discover(loadMore: Boolean = false) {
        if (loadMore) {
            if (isLoadingMore || currentPage >= totalPages) return
            currentPage++
        }
        isLoadingMore = true
        viewModelScope.launch {
            mediaRepo.discover(
                type = _selectedType.value,
                genre = _selectedGenre.value,
                year = _selectedYear.value,
                sort = _selectedSort.value,
                page = currentPage
            ).collect { state ->
                _browseState.value = state
                if (state is UiState.Success) {
                    totalPages = state.data.totalPages ?: 1
                    val newItems = state.data.results ?: emptyList()
                    _allResults.value = if (loadMore) _allResults.value + newItems else newItems
                }
                isLoadingMore = false
            }
        }
    }
}

class WatchlistViewModel(
    private val watchlistRepo: WatchlistRepository = NicksTVApp.instance.watchlistRepository
) : ViewModel() {

    val watchlist: StateFlow<List<WatchlistEntity>> = watchlistRepo.watchlist
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun remove(id: String, type: String) = viewModelScope.launch {
        watchlistRepo.removeFromWatchlist(id, type)
    }
}

class PlayerViewModel(
    private val mediaRepo: MediaRepository = NicksTVApp.instance.mediaRepository,
    private val watchlistRepo: WatchlistRepository = NicksTVApp.instance.watchlistRepository,
    private val rdRepo: RealDebridRepository = NicksTVApp.instance.realDebridRepository
) : ViewModel() {

    private val knownProviders = listOf(
        "super_barbarian", "bowler", "miner", "headhunter",
        "valkyrie", "hog_rider", "lavahound"
    )

    private val _streamState = MutableStateFlow<UiState<StreamResponse>>(UiState.Loading)
    val streamState: StateFlow<UiState<StreamResponse>> = _streamState.asStateFlow()

    private val _metaState = MutableStateFlow<UiState<DetailResponse>>(UiState.Loading)
    val metaState: StateFlow<UiState<DetailResponse>> = _metaState.asStateFlow()

    private val _allSources = MutableStateFlow<List<SourceEntry>>(emptyList())
    val allSources: StateFlow<List<SourceEntry>> = _allSources.asStateFlow()

    private val _sourcesLoading = MutableStateFlow(false)
    val sourcesLoading: StateFlow<Boolean> = _sourcesLoading.asStateFlow()

    fun load(type: String, id: String, season: Int? = null, episode: Int? = null) {
        viewModelScope.launch {
            mediaRepo.getMeta(id, type).collect { _metaState.value = it }
        }
        viewModelScope.launch {
            mediaRepo.getStream(type, id, null, season, episode).collect { state ->
                _streamState.value = state
                if (state is UiState.Success) {
                    loadAllSources(type, id, season, episode)
                }
            }
        }
    }

    fun loadAllSources(type: String, id: String, season: Int? = null, episode: Int? = null) {
        if (_sourcesLoading.value) return
        _sourcesLoading.value = true
        viewModelScope.launch {
            val entries = mutableListOf<SourceEntry>()

            // ── FrameXTV providers ────────────────────────────────────────────
            val fxtJobs = knownProviders.map { provider ->
                launch {
                    try {
                        mediaRepo.getStream(type, id, provider, season, episode).collect { state ->
                            if (state is UiState.Success) {
                                val sources = state.data.sources ?: return@collect
                                val provName = state.data.provider ?: provider
                                sources.forEach { src ->
                                    val url = src.url ?: return@forEach
                                    if (url.isBlank()) return@forEach
                                    synchronized(entries) {
                                        entries.add(SourceEntry(
                                            providerName = provName.replace("_", " ")
                                                .split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } },
                                            serverLabel = src.server ?: provName,
                                            quality = src.quality ?: "Unknown",
                                            url = url
                                        ))
                                    }
                                    // Emit partial update so UI shows sources as they arrive
                                    val seen = mutableSetOf<String>()
                                    _allSources.value = entries.filter { seen.add(it.url) }
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
            fxtJobs.forEach { it.join() }

            // ── Real-Debrid sources ───────────────────────────────────────────
            if (rdRepo.hasToken) {
                val imdbId = rdRepo.resolveImdbId(id, type)
                if (imdbId != null) {
                    rdRepo.loadRdSources(
                        imdbId = imdbId,
                        mediaType = type,
                        season = season,
                        episode = episode,
                        onProgress = { rdPartial ->
                            synchronized(entries) {
                                // Merge RD sources in — replace existing RD entries
                                val nonRd = entries.filter { it.providerName != "Real-Debrid" }
                                entries.clear()
                                entries.addAll(nonRd)
                                entries.addAll(rdPartial)
                            }
                            val seen = mutableSetOf<String>()
                            _allSources.value = entries.filter { seen.add(it.url) }
                        }
                    )
                }
            }

            val seen = mutableSetOf<String>()
            _allSources.value = entries.filter { seen.add(it.url) }
            _sourcesLoading.value = false
        }
    }

    fun markContinueWatching(
        id: String, type: String, title: String, poster: String, backdrop: String,
        year: String, season: Int? = null, episode: Int? = null, episodeTitle: String? = null
    ) = viewModelScope.launch {
        watchlistRepo.saveContinueWatching(
            ContinueWatchingEntity(id, type, title, poster, backdrop, year, season, episode, episodeTitle)
        )
    }
}
