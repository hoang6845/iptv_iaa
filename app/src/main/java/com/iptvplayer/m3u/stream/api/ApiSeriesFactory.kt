package com.iptvplayer.m3u.stream.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiSeriesFactory @Inject constructor() {

    fun create(baseUrl: String): SeriesApiService {
        return Retrofit.Builder()
            .baseUrl(baseUrl.ensureSlash())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SeriesApiService::class.java)
    }

    private fun String.ensureSlash(): String =
        if (endsWith("/")) this else "$this/"
}
