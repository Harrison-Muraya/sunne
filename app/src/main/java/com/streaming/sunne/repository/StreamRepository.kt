package com.streaming.sunne.repository

import com.streaming.sunne.data.model.Stream
import com.streaming.sunne.data.model.StreamCategory
//import com.streaming.sunne.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StreamRepository {
//    private val apiService = RetrofitClient.apiService
//
//    /**
//     * Fetch all streams from the backend
//     */
//    suspend fun getStreams(): Result<List<Stream>> = withContext(Dispatchers.IO) {
//        try {
//            val response = apiService.getStreams()
//            if (response.isSuccessful && response.body() != null) {
//                Result.success(response.body()!!)
//            } else {
//                Result.failure(Exception("Failed to fetch streams: ${response.message()}"))
//            }
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
    }

    /**
     * Fetch a specific stream by ID
     */
//    suspend fun getStreamById(id: Int): Result<Stream> = withContext(Dispatchers.IO) {
//        try {
//            val response = apiService.getStreamById(id)
//            if (response.isSuccessful && response.body() != null) {
//                Result.success(response.body()!!)
//            } else {
//                Result.failure(Exception("Failed to fetch stream: ${response.message()}"))
//            }
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }

    /**
     * Fetch and group streams by category
     * Does the network call and grouping in a single IO operation
     */
//    suspend fun getStreamsByCategory(): Result<List<StreamCategory>> = withContext(Dispatchers.IO) {
//        try {
//            val response = apiService.getStreams()
//            if (response.isSuccessful && response.body() != null) {
//                val streams = response.body()!!
//
//                // Group streams by category
//                val categories = streams
//                    .groupBy { it.category }
//                    .map { (category, streamList) ->
//                        StreamCategory(category, streamList)
//                    }
//                    .sortedBy { it.category } // Optional: sort alphabetically
//
//                Result.success(categories)
//            } else {
//                Result.failure(Exception("Failed to fetch streams: ${response.message()}"))
//            }
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }
//}