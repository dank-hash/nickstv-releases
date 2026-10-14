package com.nickstv.app.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

// ── Real-Debrid API models ────────────────────────────────────────────────────

data class RdUserResponse(
    val id: Int?,
    val username: String?,
    val premium: Int?  // > 0 = premium account
)

data class RdUnrestrictRequest(
    val link: String,
    val password: String? = null
)

data class RdUnrestrictResponse(
    val id: String?,
    val filename: String?,
    val mimeType: String?,
    val filesize: Long?,
    val link: String?,      // original link
    val host: String?,
    val chunks: Int?,
    val download: String?,  // the actual streamable URL
    val streamable: Int?
)

data class RdTorrentAddResponse(
    val id: String?,
    val uri: String?
)

data class RdTorrentInfo(
    val id: String?,
    val filename: String?,
    val status: String?,  // "downloaded", "waiting_files_selection", etc.
    val links: List<String>?
)

// ── Real-Debrid Retrofit interface ────────────────────────────────────────────

interface RealDebridApiService {

    @GET("user")
    suspend fun getUser(
        @Header("Authorization") bearer: String
    ): Response<RdUserResponse>

    @FormUrlEncoded
    @POST("unrestrict/link")
    suspend fun unrestrictLink(
        @Header("Authorization") bearer: String,
        @Field("link") link: String
    ): Response<RdUnrestrictResponse>

    @FormUrlEncoded
    @POST("torrents/addMagnet")
    suspend fun addMagnet(
        @Header("Authorization") bearer: String,
        @Field("magnet") magnet: String
    ): Response<RdTorrentAddResponse>

    @GET("torrents/info/{id}")
    suspend fun getTorrentInfo(
        @Header("Authorization") bearer: String,
        @Path("id") id: String
    ): Response<RdTorrentInfo>
}
