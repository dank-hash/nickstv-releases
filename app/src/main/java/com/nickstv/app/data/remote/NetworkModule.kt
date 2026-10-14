package com.nickstv.app.data.remote

import com.nickstv.app.api.FrameXTVApiService
import com.nickstv.app.api.GitHubApiService
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

object NetworkModule {

    private const val FRAME_BASE_URL = "https://api.framextv.tech/"
    private const val GITHUB_BASE_URL = "https://api.github.com/"
    private const val CACHE_SIZE_BYTES = 50L * 1024 * 1024 // 50 MB

    private var cacheDir: File? = null

    fun init(cacheDirectory: File) {
        cacheDir = File(cacheDirectory, "http_cache")
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .retryOnConnectionFailure(true)

        cacheDir?.let { dir ->
            runCatching {
                builder.cache(Cache(dir, CACHE_SIZE_BYTES))
            }
        }

        builder.build()
    }

    private val frameXTVRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(FRAME_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val githubRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(GITHUB_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val frameXTVApi: FrameXTVApiService by lazy {
        frameXTVRetrofit.create(FrameXTVApiService::class.java)
    }

    val githubApi: GitHubApiService by lazy {
        githubRetrofit.create(GitHubApiService::class.java)
    }
}
