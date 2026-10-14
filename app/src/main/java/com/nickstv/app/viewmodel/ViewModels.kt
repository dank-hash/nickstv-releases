package com.nickstv.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickstv.app.NicksTVApp
import com.nickstv.app.data.local.ContinueWatchingEntity
import com.nickstv.app.data.local.WatchlistEntity
import com.nickstv.app.model.*
import com.nickstv.app.repository.MediaRepository
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
    private val watchlistRepo: WatchlistRepository = NicksTVApp.instance.watchlistRepository
) : ViewModel() {

    private val _detailState = MutableStateFlow<UiState<DetailResponse>>(UiState.Loading)
    val detailState: StateFlow<UiState<DetailResponse>> = _detailState.asStateFlow()

    private val _selectedSeason = MutableStateFlow(1)
    val selectedSeason: StateFlow<Int> = _selectedSeason.asStateFlow()

    private val _episodeState = MutableStateFlow<UiState<DetailResponse>>(UiState.Empty)
    val episodeState: StateFlow<UiState<DetailResponse>> = _episodeState.asStateFlow()

    private var _currentId: String = "0"
    private var _currentType: String = "movie"

    fun load(id: String, type: String) {
        _currentId = id
        _currentType = type
        viewModelScope.launch {
            mediaRepo.getMeta(id, type).collect { _detailState.value = it }
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
    private val watchlistRepo: WatchlistRepository = NicksTVApp.instance.watchlistRepository
) : ViewModel() {

    private val _streamState = MutableStateFlow<UiState<StreamResponse>>(UiState.Loading)
    val streamState: StateFlow<UiState<StreamResponse>> = _streamState.asStateFlow()

    private val _metaState = MutableStateFlow<UiState<DetailResponse>>(UiState.Loading)
    val metaState: StateFlow<UiState<DetailResponse>> = _metaState.asStateFlow()

    fun load(type: String, id: String, season: Int? = null, episode: Int? = null) {
        viewModelScope.launch {
            mediaRepo.getMeta(id, type).collect { _metaState.value = it }
        }
        viewModelScope.launch {
            mediaRepo.getStream(type, id, null, season, episode).collect { _streamState.value = it }
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
