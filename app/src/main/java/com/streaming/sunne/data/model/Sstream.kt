//package com.streaming.sunne.data.model
//
//import com.google.gson.annotations.SerializedName
//
///**
// * Data model representing a stream/video content
// */
//data class StreamResponse(
//    val count: Int,
//    val next: String?,
//    val previous: String?,
//    val results: List<Stream>
//)
//
////data class Stream(
////    @SerializedName("id")
////    val id: Int,
////    @SerializedName("title")
////    val title: String,
////    @SerializedName("thumbnail")
////    val thumbnail: String,
////    @SerializedName("url")
////    val url: String,
////    @SerializedName("category")
////    val category: String,
////    @SerializedName("description")
////    val description: String? = null,
////    @SerializedName("duration")
////    val duration: String? = null,
////    @SerializedName("banner")
////    val banner: String? = null
////)
////
/////**
//// * Grouped streams by category
//// */
////data class StreamCategory(
////    val category: String,
////    val streams: List<Stream>
////)
//
//
//data class Stream(
//    @SerializedName("id")
//    val id: Int,
//    @SerializedName("title")
//    val title: String,
//    @SerializedName("thumbnail")
//    val thumbnail: String,
//    @SerializedName("url")
//    val url: String,
//    @SerializedName("category")
//    val category: String,
//    // Optional fields from detailed endpoint
//    @SerializedName("description")
//    val description: String? = null,
//    @SerializedName("banner")
//    val banner: String? = null,
//    @SerializedName("duration")
//    val duration: String? = null,
//    @SerializedName("rating")
//    val rating: Double? = null,
//    @SerializedName("release_year")
//    val release_year: Int? = null,
//    @SerializedName("director")
//    val director: String? = null,
//    @SerializedName("cast")
//    val cast: String? = null,
//    @SerializedName("language")
//    val language: String? = null,
//    @SerializedName("quality")
//    val quality: String? = null,
//    @SerializedName("is_featured")
//    val is_featured: Boolean? = null,
//    @SerializedName("is_live")
//    val is_live: Boolean? = null,
//    @SerializedName("view_count")
//    val view_count: Int? = null
//)
//
///**
// * Category grouping returned by /api/streams/by_category/
// */
//data class StreamCategory(
//    val category: String,
//    val streams: List<Stream>
//)