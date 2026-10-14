package com.nickstv.app.repository

import android.content.Context
import android.content.SharedPreferences
import com.nickstv.app.NicksTVApp
import com.nickstv.app.api.EztvTorrent
import com.nickstv.app.api.YtsMovie
import com.nickstv.app.model.SourceEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class RealDebridRepository(
    context: Context = NicksTVApp.instance
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("nickstv_rd", Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val KEY_TOKEN = "rd_api_token"
        private const val RD_BASE = "https://api.real-debrid.com/rest/1.0"
        private const val YTS_BASE = "https://yts.mx/api/v2"
        private const val EZTV_BASE = "https://eztv.re/api"
    }

    // ── Token storage ─────────────────────────────────────────────────────────

    var apiToken: String
        get() = prefs.getString(KEY_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TOKEN, value.trim()).apply()

    val hasToken: Boolean get() = apiToken.isNotBlank()

    private fun bearer() = "Bearer ${apiToken}"

    // ── RD user validation ────────────────────────────────────────────────────

    suspend fun validateToken(token: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$RD_BASE/user")
                .header("Authorization", "Bearer $token")
                .build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext Pair(false, "HTTP ${resp.code}")
            val body = resp.body?.string() ?: return@withContext Pair(false, "Empty response")
            val json = JSONObject(body)
            val username = json.optString("username", "")
            val premium = json.optInt("premium", 0)
            if (username.isBlank()) return@withContext Pair(false, "Invalid token")
            Pair(true, if (premium > 0) username else "$username (free — premium required for streams)")
        } catch (e: Exception) {
            Pair(false, e.message ?: "Network error")
        }
    }

    // ── Unrestrict a direct link or magnet ────────────────────────────────────

    private suspend fun unrestrictLink(link: String): String? = withContext(Dispatchers.IO) {
        try {
            val body = FormBody.Builder().add("link", link).build()
            val req = Request.Builder()
                .url("$RD_BASE/unrestrict/link")
                .header("Authorization", bearer())
                .post(body)
                .build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext null
            val json = JSONObject(resp.body?.string() ?: return@withContext null)
            json.optString("download").takeIf { it.isNotBlank() }
        } catch (e: Exception) { null }
    }

    // ── Add magnet to RD, wait for it to be cached, get links ─────────────────

    private suspend fun magnetToLinks(magnet: String): List<String> = withContext(Dispatchers.IO) {
        try {
            // 1. Add magnet
            val addBody = FormBody.Builder().add("magnet", magnet).build()
            val addReq = Request.Builder()
                .url("$RD_BASE/torrents/addMagnet")
                .header("Authorization", bearer())
                .post(addBody)
                .build()
            val addResp = client.newCall(addReq).execute()
            if (!addResp.isSuccessful) return@withContext emptyList()
            val addJson = JSONObject(addResp.body?.string() ?: return@withContext emptyList())
            val torrentId = addJson.optString("id").takeIf { it.isNotBlank() }
                ?: return@withContext emptyList()

            // 2. Select all files
            val selBody = FormBody.Builder().add("files", "all").build()
            val selReq = Request.Builder()
                .url("$RD_BASE/torrents/selectFiles/$torrentId")
                .header("Authorization", bearer())
                .post(selBody)
                .build()
            client.newCall(selReq).execute()

            // 3. Poll for status — max 25s
            repeat(5) { attempt ->
                Thread.sleep(if (attempt == 0) 2000L else 5000L)
                val infoReq = Request.Builder()
                    .url("$RD_BASE/torrents/info/$torrentId")
                    .header("Authorization", bearer())
                    .build()
                val infoResp = client.newCall(infoReq).execute()
                if (!infoResp.isSuccessful) return@repeat
                val infoJson = JSONObject(infoResp.body?.string() ?: return@repeat)
                val status = infoJson.optString("status")
                if (status == "downloaded") {
                    val linksArr = infoJson.optJSONArray("links") ?: return@repeat
                    val links = (0 until linksArr.length()).map { linksArr.getString(it) }
                    if (links.isNotEmpty()) return@withContext links
                }
            }
            emptyList()
        } catch (e: Exception) { emptyList() }
    }

    // ── YTS: fetch movie torrents by IMDB ID ──────────────────────────────────

    suspend fun fetchYtsTorrents(imdbId: String): List<Pair<YtsMovie, com.nickstv.app.api.YtsTorrent>> =
        withContext(Dispatchers.IO) {
            try {
                val url = "$YTS_BASE/list_movies.json?query_term=${imdbId}&limit=1"
                val req = Request.Builder().url(url).build()
                val resp = client.newCall(req).execute()
                if (!resp.isSuccessful) return@withContext emptyList()
                val json = JSONObject(resp.body?.string() ?: return@withContext emptyList())
                val data = json.optJSONObject("data") ?: return@withContext emptyList()
                val movies = data.optJSONArray("movies") ?: return@withContext emptyList()
                val results = mutableListOf<Pair<YtsMovie, com.nickstv.app.api.YtsTorrent>>()
                for (i in 0 until movies.length()) {
                    val m = movies.getJSONObject(i)
                    val title = m.optString("title")
                    val imdb = m.optString("imdb_code")
                    val torrentsArr = m.optJSONArray("torrents") ?: continue
                    val movie = YtsMovie(
                        id = m.optInt("id"),
                        title = title,
                        imdbCode = imdb,
                        year = m.optInt("year"),
                        torrents = null
                    )
                    for (j in 0 until torrentsArr.length()) {
                        val t = torrentsArr.getJSONObject(j)
                        val hash = t.optString("hash")
                        if (hash.isBlank()) continue
                        val torrent = com.nickstv.app.api.YtsTorrent(
                            url = t.optString("url"),
                            hash = hash,
                            quality = t.optString("quality"),
                            type = t.optString("type"),
                            videoCodec = t.optString("video_codec"),
                            seeds = t.optInt("seeds"),
                            peers = t.optInt("peers"),
                            size = t.optString("size"),
                            sizeBytes = t.optLong("size_bytes")
                        )
                        results.add(Pair(movie, torrent))
                    }
                }
                // Sort by quality desc then seeds desc
                results.sortWith(compareByDescending<Pair<YtsMovie, com.nickstv.app.api.YtsTorrent>> {
                    when (it.second.quality) { "2160p" -> 4; "1080p" -> 3; "720p" -> 2; else -> 1 }
                }.thenByDescending { it.second.seeds ?: 0 })
                results
            } catch (e: Exception) { emptyList() }
        }

    // ── EZTV: fetch TV torrents by IMDB ID ────────────────────────────────────

    suspend fun fetchEztvTorrents(imdbId: String, season: Int?, episode: Int?): List<EztvTorrent> =
        withContext(Dispatchers.IO) {
            try {
                // EZTV uses numeric IMDB ID
                val numericId = imdbId.removePrefix("tt").toIntOrNull()
                    ?: return@withContext emptyList()
                val url = "$EZTV_BASE/get-torrents?imdb_id=$numericId&limit=50"
                val req = Request.Builder().url(url).build()
                val resp = client.newCall(req).execute()
                if (!resp.isSuccessful) return@withContext emptyList()
                val json = JSONObject(resp.body?.string() ?: return@withContext emptyList())
                val arr = json.optJSONArray("torrents") ?: return@withContext emptyList()
                val results = mutableListOf<EztvTorrent>()
                for (i in 0 until arr.length()) {
                    val t = arr.getJSONObject(i)
                    val magnet = t.optString("magnet_url").takeIf { it.isNotBlank() } ?: continue
                    val s = t.optString("season").toIntOrNull() ?: 0
                    val e = t.optString("episode").toIntOrNull() ?: 0
                    // Filter to matching season/episode if provided
                    if (season != null && s != season) continue
                    if (episode != null && e != episode) continue
                    results.add(
                        EztvTorrent(
                            id = t.optLong("id"),
                            title = t.optString("title"),
                            season = t.optString("season"),
                            episode = t.optString("episode"),
                            magnetUrl = magnet,
                            sizeBytes = t.optLong("size_bytes"),
                            seeds = t.optInt("seeds"),
                            peers = t.optInt("peers"),
                            quality = extractQuality(t.optString("title"))
                        )
                    )
                }
                // Sort by quality desc then seeds desc
                results.sortWith(compareByDescending<EztvTorrent> {
                    when (it.quality) { "2160p" -> 4; "1080p" -> 3; "720p" -> 2; else -> 1 }
                }.thenByDescending { it.seeds ?: 0 })
                results.take(10)
            } catch (e: Exception) { emptyList() }
        }

    private fun extractQuality(title: String): String {
        val t = title.uppercase()
        return when {
            t.contains("2160P") || t.contains("4K") || t.contains("UHD") -> "2160p"
            t.contains("1080P") -> "1080p"
            t.contains("720P") -> "720p"
            t.contains("480P") -> "480p"
            else -> "Unknown"
        }
    }

    // ── Main: load all RD sources for a title ─────────────────────────────────
    // Returns a list of SourceEntry ready to merge into the server picker

    suspend fun loadRdSources(
        imdbId: String,
        mediaType: String,
        season: Int? = null,
        episode: Int? = null,
        onProgress: (List<SourceEntry>) -> Unit = {}
    ): List<SourceEntry> {
        if (!hasToken) return emptyList()
        return try {
            coroutineScope {
                val entries = mutableListOf<SourceEntry>()

                if (mediaType == "movie") {
                    val torrents = fetchYtsTorrents(imdbId)
                    // Unrestrict in parallel (max 4)
                    val jobs = torrents.take(6).map { (movie, torrent) ->
                        async {
                            val magnet = buildMagnetForYts(torrent, movie.title ?: "")
                            if (magnet.isBlank()) return@async
                            // Try direct unrestrict of YTS torrent URL first (faster)
                            val directUrl = torrent.url?.takeIf { it.isNotBlank() }
                                ?.let { unrestrictLink(it) }
                            val streamUrl = directUrl
                                ?: run {
                                    val links = magnetToLinks(magnet)
                                    links.firstOrNull()?.let { unrestrictLink(it) }
                                }
                            if (streamUrl != null) {
                                synchronized(entries) {
                                    entries.add(SourceEntry(
                                        providerName = "Real-Debrid",
                                        serverLabel = "RD · ${torrent.quality ?: "?"} · ${torrent.type?.uppercase() ?: "WEB"}",
                                        quality = torrent.quality ?: "Unknown",
                                        url = streamUrl
                                    ))
                                }
                                onProgress(entries.toList())
                            }
                        }
                    }
                    jobs.forEach { it.await() }
                } else {
                    // TV via EZTV
                    val torrents = fetchEztvTorrents(imdbId, season, episode)
                    val jobs = torrents.take(6).map { torrent ->
                        async {
                            val magnet = torrent.magnetUrl ?: return@async
                            val links = magnetToLinks(magnet)
                            val streamUrl = links.firstOrNull()?.let { unrestrictLink(it) }
                            if (streamUrl != null) {
                                synchronized(entries) {
                                    entries.add(SourceEntry(
                                        providerName = "Real-Debrid",
                                        serverLabel = "RD · ${torrent.quality ?: "?"} · S${torrent.season}E${torrent.episode}",
                                        quality = torrent.quality ?: "Unknown",
                                        url = streamUrl
                                    ))
                                }
                                onProgress(entries.toList())
                            }
                        }
                    }
                    jobs.forEach { it.await() }
                }
                entries.toList()
            }
        } catch (e: Exception) { emptyList() }
    }

    private fun buildMagnetForYts(torrent: com.nickstv.app.api.YtsTorrent, title: String): String {
        val hash = torrent.hash?.takeIf { it.isNotBlank() } ?: return ""
        return "magnet:?xt=urn:btih:$hash" +
            "&dn=${title.replace(" ", "+")}" +
            "&tr=udp%3A%2F%2Fopen.demonii.com%3A1337%2Fannounce" +
            "&tr=udp%3A%2F%2Ftracker.openbittorrent.com%3A80" +
            "&tr=udp%3A%2F%2Ftracker.coppersurfer.tk%3A6969" +
            "&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337%2Fannounce"
    }

    // ── IMDB ID lookup via FrameXTV /api/details ──────────────────────────────

    suspend fun resolveImdbId(tmdbId: String, mediaType: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val url = "https://api.framextv.tech/api/details?id=$tmdbId&type=$mediaType"
                val req = Request.Builder().url(url)
                    .header("User-Agent", "NicksTV/1.5 Android")
                    .build()
                val resp = client.newCall(req).execute()
                if (!resp.isSuccessful) return@withContext null
                val json = JSONObject(resp.body?.string() ?: return@withContext null)
                // FrameXTV /api/details returns imdb_id or external_ids.imdb_id
                json.optString("imdb_id").takeIf { it.startsWith("tt") }
                    ?: json.optJSONObject("external_ids")?.optString("imdb_id")?.takeIf { it.startsWith("tt") }
                    // fallback: try TMDB directly
                    ?: fetchImdbFromTmdb(tmdbId, mediaType)
            } catch (e: Exception) { null }
        }

    private suspend fun fetchImdbFromTmdb(tmdbId: String, mediaType: String): String? =
        withContext(Dispatchers.IO) {
            try {
                // Use TMDB's public external IDs endpoint (no key needed for basic lookup via 3rd party)
                // Try omdbapi by title as last resort - but first try a direct TMDB call
                val type = if (mediaType == "tv") "tv" else "movie"
                val url = "https://api.themoviedb.org/3/$type/$tmdbId/external_ids?api_key=4ef0d7355d9ffb5151e987764708ce96"
                val req = Request.Builder().url(url).build()
                val resp = client.newCall(req).execute()
                if (!resp.isSuccessful) return@withContext null
                val json = JSONObject(resp.body?.string() ?: return@withContext null)
                json.optString("imdb_id").takeIf { it.startsWith("tt") }
            } catch (e: Exception) { null }
        }
}
