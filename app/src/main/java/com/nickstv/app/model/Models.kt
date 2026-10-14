package com.nickstv.app.model

import com.google.gson.annotations.SerializedName

// ── Real FrameXTV API shapes ──────────────────────────────────────────────────
// The API returns id as String, rating as String, genres as List<String>,
// and image/poster/backdrop as fully-qualified URLs (no path building needed).

data class MediaItem(
    val id: String,                          // Always a String from the API
    val title: String?,
    val name: String?,
    val overview: String?,
    val image: String?,                      // Primary image (same as poster usually)
    val poster: String?,
    val backdrop: String?,
    val year: String?,
    val rating: String?,                     // Comes back as "7.7" string
    val type: String?,                        // "movie" or "tv"
    val genres: List<String>?,
    val success: Boolean? = null
) {
    val idInt: Int get() = id.toIntOrNull() ?: 0
    val displayTitle: String get() = title ?: name ?: "Unknown"
    val displayYear: String get() = year ?: ""
    val displayRating: Double get() = rating?.toDoubleOrNull() ?: 0.0
    val mediaType: String get() = type ?: "movie"
    val posterUrl: String get() = poster ?: image ?: ""
    val backdropUrl: String get() = backdrop ?: poster ?: image ?: ""
}

// ── Home response — rails wrapped under "rails" object ────────────────────────

data class HomeRail(
    val success: Boolean?,
    val page: Int?,
    @SerializedName("totalPages") val totalPages: Int?,
    @SerializedName("totalResults") val totalResults: Int?,
    val results: List<MediaItem>?
)

data class HomeHero(
    val id: String?,
    val title: String?,
    val type: String?,
    val rating: String?,
    val image: String?,
    val year: String?,
    val poster: String?,
    val backdrop: String?,
    val overview: String?
)

data class HomeRails(
    @SerializedName("trending_movies") val trendingMovies: HomeRail?,
    @SerializedName("popular_movies") val popularMovies: HomeRail?,
    @SerializedName("popular_tv") val popularTv: HomeRail?,
    @SerializedName("popular_anime") val popularAnime: HomeRail?,
    @SerializedName("new_releases") val newReleases: HomeRail?,
    @SerializedName("action_movies") val actionMovies: HomeRail?,
    @SerializedName("scifi_movies") val scifiMovies: HomeRail?,
    @SerializedName("thriller_movies") val thrillerMovies: HomeRail?,
    @SerializedName("comedy_movies") val comedyMovies: HomeRail?,
    @SerializedName("airing_anime") val airingAnime: HomeRail?
)

data class HomeResponse(
    val success: Boolean?,
    val timestamp: Long?,
    val hero: HomeHero?,
    val rails: HomeRails?
) {
    // Convenience accessors that flatten the rails
    val trendingMovies: List<MediaItem>? get() = rails?.trendingMovies?.results
    val popularMovies: List<MediaItem>? get() = rails?.popularMovies?.results
    val popularTv: List<MediaItem>? get() = rails?.popularTv?.results
    val popularAnime: List<MediaItem>? get() = rails?.popularAnime?.results
    val newReleases: List<MediaItem>? get() = rails?.newReleases?.results
    val actionMovies: List<MediaItem>? get() = rails?.actionMovies?.results
    val scifiMovies: List<MediaItem>? get() = rails?.scifiMovies?.results
    val thrillerMovies: List<MediaItem>? get() = rails?.thrillerMovies?.results
    val comedyMovies: List<MediaItem>? get() = rails?.comedyMovies?.results
    val airingAnime: List<MediaItem>? get() = rails?.airingAnime?.results

    fun heroAsMediaItem(): MediaItem? {
        val h = hero ?: return null
        return MediaItem(
            id = h.id ?: return null,
            title = h.title, name = null, overview = h.overview,
            image = h.image, poster = h.poster, backdrop = h.backdrop,
            year = h.year, rating = h.rating, type = h.type,
            genres = null, success = true
        )
    }
}

// ── Search / Discover response ────────────────────────────────────────────────

data class SearchResponse(
    val success: Boolean?,
    val page: Int?,
    @SerializedName("totalPages") val totalPages: Int?,
    @SerializedName("totalResults") val totalResults: Int?,
    val results: List<MediaItem>?
)

data class DiscoverResponse(
    val success: Boolean?,
    val page: Int?,
    @SerializedName("totalPages") val totalPages: Int?,
    @SerializedName("totalResults") val totalResults: Int?,
    val results: List<MediaItem>?
)

// ── Meta / Detail response ────────────────────────────────────────────────────
// /api/meta returns a minimal object — title, category string, backdrop, poster, year, genres (string list)

data class DetailResponse(
    val success: Boolean?,
    val id: String?,
    val title: String?,
    val name: String?,
    val overview: String?,
    val poster: String?,
    val backdrop: String?,
    val image: String?,
    val year: String?,
    val rating: String?,
    val category: String?,          // "MOVIE / Crime / Mystery / 2022"
    val genres: List<String>?,      // ["Crime","Mystery","Thriller"]
    val cast: List<CastMember>?,
    val seasons: List<Season>?,
    val episodes: List<Episode>?,
    val recommendations: List<MediaItem>?,
    val similar: List<MediaItem>?,
    val runtime: Int?,
    @SerializedName("number_of_seasons") val numberOfSeasons: Int?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("first_air_date") val firstAirDate: String?,
    val tagline: String?,
    val status: String?,
    val videos: List<Video>?
) {
    val displayTitle: String get() = title ?: name ?: "Unknown"
    val displayYear: String get() {
        val y = year ?: releaseDate?.take(4) ?: firstAirDate?.take(4)
        return y ?: ""
    }
    val displayRating: Double get() = rating?.toDoubleOrNull() ?: 0.0
    val posterUrl: String get() = poster ?: image ?: ""
    val backdropUrl: String get() = backdrop ?: poster ?: image ?: ""
    val mediaType: String get() {
        val cat = category?.uppercase() ?: ""
        return when {
            cat.contains("TV") || cat.contains("SERIES") -> "tv"
            name != null && title == null -> "tv"
            else -> "movie"
        }
    }
    // Parse genres from either the genres list or the category string
    val genreList: List<String> get() {
        if (!genres.isNullOrEmpty()) return genres
        val parts = category?.split("/")?.map { it.trim() } ?: return emptyList()
        return parts.drop(1).filter { it.isNotBlank() && it.all { c -> !c.isDigit() } }
    }
    fun toMediaItem(): MediaItem = MediaItem(
        id = id ?: "0", title = title, name = name, overview = overview,
        image = image, poster = posterUrl, backdrop = backdropUrl,
        year = displayYear, rating = rating, type = mediaType,
        genres = genreList, success = success
    )
}

data class CastMember(
    val id: String?,
    val name: String?,
    val character: String?,
    @SerializedName("profile_path") val profilePath: String?,
    val profile: String?,
    val image: String?
) {
    val photoUrl: String get() = profile ?: image ?: profilePath?.let { "https://image.tmdb.org/t/p/w185$it" } ?: ""
}

data class Season(
    val id: String?,
    val number: Int?,
    @SerializedName("season_number") val seasonNumber2: Int?,
    val name: String?,
    val overview: String?,
    val poster: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("episode_count") val episodeCount: Int?,
    @SerializedName("air_date") val airDate: String?
) {
    val seasonNumber: Int get() = number ?: seasonNumber2 ?: 0
    val displayName: String get() = name ?: "Season $seasonNumber"
    val posterUrl: String get() = poster ?: posterPath?.let { "https://image.tmdb.org/t/p/w185$it" } ?: ""
}

data class Episode(
    val id: String?,
    val number: Int?,
    @SerializedName("episode_number") val episodeNumber: Int?,
    val name: String?,
    val overview: String?,
    val still: String?,
    @SerializedName("still_path") val stillPath: String?,
    val runtime: Int?,
    @SerializedName("air_date") val airDate: String?,
    val rating: String?,
    @SerializedName("vote_average") val voteAverage: Double?
) {
    val epNumber: Int get() = number ?: episodeNumber ?: 0
    val displayName: String get() = name ?: "Episode $epNumber"
    val stillUrl: String get() = still ?: stillPath?.let { "https://image.tmdb.org/t/p/w300$it" } ?: ""
    val displayRating: Double get() = voteAverage ?: rating?.toDoubleOrNull() ?: 0.0
}

// ── Stream models ─────────────────────────────────────────────────────────────

data class ProvidersResponse(
    val success: Boolean?,
    val providers: List<Provider>?
)

data class Provider(
    val id: String,
    val name: String?,
    val tagline: String?,
    val audio: String?,
    val status: String?,
    val quality: String?
)

data class StreamResponse(
    val success: Boolean?,
    val sources: List<StreamSource>?,
    val subtitles: List<Subtitle>?,
    val provider: String?,
    val history: List<Any>?
)

data class StreamSource(
    val url: String?,
    val type: String?,
    val quality: String?,
    val server: String?,
    val headers: Map<String, String>?,
    val skipProxy: Boolean?
)

// Flat entry used by the server picker UI
data class SourceEntry(
    val providerName: String,
    val serverLabel: String,
    val quality: String,
    val url: String
)

data class Subtitle(
    val url: String?,
    val label: String?,
    val language: String?
)

data class Video(
    val id: String?,
    val key: String?,
    val name: String?,
    val site: String?,
    val type: String?,
    val official: Boolean?
)

// ── GitHub release models ─────────────────────────────────────────────────────

data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String?,
    val name: String?,
    val body: String?,
    @SerializedName("published_at") val publishedAt: String?,
    val assets: List<ReleaseAsset>?
)

data class ReleaseAsset(
    val name: String?,
    @SerializedName("browser_download_url") val downloadUrl: String?,
    val size: Long?
)

data class GitHubUser(
    val login: String?,
    val id: Int?
)

// ── UI state ──────────────────────────────────────────────────────────────────

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String, val cause: Throwable? = null) : UiState<Nothing>()
    object Empty : UiState<Nothing>()
}
