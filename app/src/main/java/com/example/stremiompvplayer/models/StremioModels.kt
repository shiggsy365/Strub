package com.example.stremiompvplayer.models

import com.squareup.moshi.Json
import java.io.Serializable

data class StreamResponse(
    val streams: List<Stream>
)

data class Stream(
    val name: String?,
    val description: String?,
    val infoHash: String?,
    val url: String?, // Can be http, dln, or magnet
    val ytId: String?, // YouTube video ID
    val behaviorHints: BehaviorHints?
) : Serializable {
    val title: String
        get() = name ?: "Unknown Stream"
    val subtitle: String
        get() = description ?: infoHash ?: ytId ?: "No details"
}

data class BehaviorHints(
    val notWebReady: Boolean? = false
) : Serializable


data class MetaResponse(
    val meta: Meta
)

data class Meta(
    val id: String,
    val type: String,
    val name: String,
    val poster: String?,
    val background: String?,
    val description: String?,
    val videos: List<Video>?
) : Serializable

data class Video(
    val id: String,
    val title: String, // Episode title
    val released: String?,
    val thumbnail: String?,
    val number: Int?, // Episode number
    val season: Int? // Season number
) : Serializable

data class MetaItem(
    val id: String,
    val type: String, // "movie", "series", "tv" etc.
    val name: String,
    val poster: String?,
    val background: String?,
    val description: String?
) : Serializable

// NEW: Added models to parse the add-on manifest.json
data class Manifest(
    val id: String,
    val version: String,
    val name: String,
    val description: String?,
    val types: List<String>,
    val catalogs: List<Catalog>
) : Serializable

data class Catalog(
    val type: String,
    val id: String,
    val name: String,
    @Json(name = "extra") val extraProps: List<ExtraProp>?
) : Serializable {
    data class ExtraProp(
        val name: String,
        val isRequired: Boolean? = false
    ) : Serializable
}

// NEW: Added a response model for catalog requests (e.g., /catalog/movie/top.json)
data class CatalogResponse(
    val metas: List<MetaItem>
) : Serializable