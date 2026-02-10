package com.streaming.sunne.data.network

import com.streaming.sunne.data.model.Stream
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit API service interface
 * Define all API endpoints here
 */
interface ApiService {
    /**
     * Get all available streams
     */
    @GET("api/streams")
    suspend fun getStreams(): Response<List<Stream>>

    /**
     * Get details of a specific stream by ID
     */
    @GET("api/stream/{id}")
    suspend fun getStreamById(@Path("id") id: Int): Response<Stream>
}