package com.streaming.sunne.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data model representing a stream/video content
 */
data class StreamResponse(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<Stream>
)

data class Stream(
    @SerializedName("id")
    val id: Int,
    @SerializedName("title")
    val title: String,
    @SerializedName("thumbnail")
    val thumbnail: String,
    @SerializedName("url")
    val url: String,
    @SerializedName("category")
    val category: String,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("duration")
    val duration: String? = null,
    @SerializedName("banner")
    val banner: String? = null
)

/**
 * Grouped streams by category
 */
data class StreamCategory(
    val category: String,
    val streams: List<Stream>
)