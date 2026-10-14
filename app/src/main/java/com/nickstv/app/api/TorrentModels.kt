package com.nickstv.app.api

import com.google.gson.annotations.SerializedName

// ── YTS movie torrent model ───────────────────────────────────────────────────

data class YtsMovie(
    val id: Int?,
    val title: String?,
    @SerializedName("imdb_code") val imdbCode: String?,
    val year: Int?,
    val torrents: List<YtsTorrent>?
)

data class YtsTorrent(
    val url: String?,
    val hash: String?,
    val quality: String?,  // "720p", "1080p", "2160p"
    val type: String?,     // "bluray", "web"
    @SerializedName("video_codec") val videoCodec: String?,
    val seeds: Int?,
    val peers: Int?,
    val size: String?,
    @SerializedName("size_bytes") val sizeBytes: Long?
) {
    // Build magnet link from hash
    val magnetUri: String get() = if (hash.isNullOrBlank()) "" else
        "magnet:?xt=urn:btih:$hash" +
        "&dn=${title?.replace(" ", "+") ?: "movie"}" +
        "&tr=udp%3A%2F%2Fopen.demonii.com%3A1337%2Fannounce" +
        "&tr=udp%3A%2F%2Ftracker.openbittorrent.com%3A80" +
        "&tr=udp%3A%2F%2Ftracker.coppersurfer.tk%3A6969" +
        "&tr=udp%3A%2F%2Fglotorrents.pw%3A6969%2Fannounce" +
        "&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337%2Fannounce"

    val title: String? get() = null  // placeholder; set externally
}

data class YtsData(
    @SerializedName("movie_count") val movieCount: Int?,
    val movies: List<YtsMovie>?
)

data class YtsResponse(
    val status: String?,
    val data: YtsData?
)

// ── EZTV TV torrent model ─────────────────────────────────────────────────────

data class EztvTorrent(
    val id: Long?,
    val title: String?,
    val season: String?,
    val episode: String?,
    @SerializedName("torrent_magnet_url") val magnetUrl: String?,
    @SerializedName("size_bytes") val sizeBytes: Long?,
    val seeds: Int?,
    val peers: Int?,
    @SerializedName("quality") val quality: String?
)

data class EztvResponse(
    val torrents: List<EztvTorrent>?,
    @SerializedName("torrents_count") val torrentsCount: Int?
)
