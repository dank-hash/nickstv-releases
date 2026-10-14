package com.nickstv.app.api

import com.nickstv.app.model.*
import retrofit2.Response
import retrofit2.http.*

interface FrameXTVApiService {

    // ── Home ──────────────────────────────────────────────────────────────────

    @GET("api/home")
    suspend fun getHome(): Response<HomeResponse>

    // ── Search ────────────────────────────────────────────────────────────────

    @GET("api/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("type") type: String = "all",
        @Query("page") page: Int = 1
    ): Response<SearchResponse>

    // ── Metadata ──────────────────────────────────────────────────────────────

    @GET("api/meta")
    suspend fun getMeta(
        @Query("id") id: String,
        @Query("type") type: String,
        @Query("season") season: Int? = null,
        @Query("episode") episode: Int? = null
    ): Response<DetailResponse>

    // ── Discover ──────────────────────────────────────────────────────────────

    @GET("api/discover")
    suspend fun discover(
        @Query("type") type: String,
        @Query("category") category: String? = null,
        @Query("genre") genre: String? = null,
        @Query("year") year: Int? = null,
        @Query("sort") sort: String? = null,
        @Query("min_rating") minRating: Float? = null,
        @Query("page") page: Int = 1
    ): Response<DiscoverResponse>

    // ── Subtitles ─────────────────────────────────────────────────────────────

    @GET("api/subtitles")
    suspend fun getSubtitles(
        @Query("id") id: String,
        @Query("type") type: String,
        @Query("season") season: Int? = null,
        @Query("episode") episode: Int? = null
    ): Response<List<Subtitle>>

    // ── Providers ─────────────────────────────────────────────────────────────

    @GET("api/providers")
    suspend fun getProviders(
        @Query("type") type: String = "movie_tv",
        @Query("sub_type") subType: String? = null
    ): Response<ProvidersResponse>

    // ── Stream ────────────────────────────────────────────────────────────────

    @GET("api/stream")
    suspend fun getStream(
        @Query("type") type: String,
        @Query("id") id: String,
        @Query("provider") provider: String? = null,
        @Query("season") season: Int? = null,
        @Query("episode") episode: Int? = null,
        @Query("seed") seed: String? = null,
        @Query("force_refresh") forceRefresh: Boolean? = null
    ): Response<StreamResponse>

    @GET("api/stream/check")
    suspend fun checkStream(
        @Query("id") id: String,
        @Query("type") type: String,
        @Query("provider") provider: String,
        @Query("season") season: Int? = null,
        @Query("episode") episode: Int? = null
    ): Response<Map<String, Any>>
}

interface GitHubApiService {

    @GET("user")
    suspend fun getUser(@Header("Authorization") token: String): Response<GitHubUser>

    // No auth required for public repos — token param is nullable/optional
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<GitHubRelease>

    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestReleaseAuth(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<GitHubRelease>
}
