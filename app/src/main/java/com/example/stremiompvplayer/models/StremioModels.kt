package com.example.stremiompvplayer.models

import com.squareup.moshi.Json
import java.io.Serializable
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

data class StreamResponse(val streams: List<Stream>)

data class Stream(
    val name: String?, val description: String?, val infoHash: String?, val url: String?, val ytId: String?, val behaviorHints: BehaviorHints?,
    val proxied: Boolean? = false, val library: Boolean? = false, val service: ServiceInfo?,
    @Json(name = "type") val streamType: String?, val resolution: String?, val size: Long?, val seeders: Int?, val addon: String?,
    val parsedFile: ParsedFile?
) : Serializable {
    val title: String get() = name ?: "Unknown Stream"
    val rawTitle: String get() = name ?: "Unknown Stream"
    private val resolutionRegex = "(?i)(2160p|4k|1080p|720p|480p|360p)".toRegex()
    private val cachedRegex = "(?i)\\[(RD\\+|AD\\+|PM\\+|DL\\+)\\]|\\b(cached)\\b".toRegex()
    val formattedTitle: String get() {
        val parts = mutableListOf<String>()
        val sbIconsAndType = StringBuilder()
        val isCached = (streamType == "debrid") || (service?.cached == true) || (name != null && cachedRegex.containsMatchIn(name)) || (description != null && cachedRegex.containsMatchIn(description))
        val rawRes = parsedFile?.resolution ?: resolution ?: run { val match = resolutionRegex.find(name ?: "") ?: resolutionRegex.find(description ?: ""); match?.value }
        val isProxied = proxied ?: false
        val isLibrary = library ?: false
        val seedCount = seeders ?: 0
        val type = streamType ?: ""
        if (isProxied) sbIconsAndType.append("🌐")
        if (isLibrary) sbIconsAndType.append("📚")
        if (isCached) sbIconsAndType.append("⚡") else sbIconsAndType.append("⏳")
        val typeStr = when (type) { "debrid" -> "DB"; "usenet" -> "UN"; "p2p" -> "P2P"; "http" -> "Web"; "youtube" -> "YT"; "live" -> "Live"; else -> if (isCached) "DB" else "P2P" }
        sbIconsAndType.append(typeStr)
        if ((!isCached || type == "p2p") && seedCount > 0) { if (sbIconsAndType.isNotEmpty()) sbIconsAndType.append(" "); sbIconsAndType.append("👤$seedCount") }
        if (sbIconsAndType.isNotBlank()) parts.add(sbIconsAndType.toString().trim())
        val resDisplay = when (rawRes?.lowercase()) { "2160p", "4k" -> "4K"; "1440p" -> "2K"; "1080p" -> "1080p"; "720p" -> "720p"; "576p", "480p", "360p", "240p", "144p" -> "SD"; else -> rawRes ?: "" }
        if (resDisplay.isNotEmpty()) parts.add(resDisplay)
        if ((size ?: 0) > 0) parts.add(formatBytes(size!!))
        val addonName = addon ?: name?.replace(resolutionRegex, "")?.replace(cachedRegex, "")?.trim() ?: ""
        // Filter out very short addon names (like "PTP") and meaningless strings
        if (addonName.isNotEmpty() && addonName.length > 3 && !addonName.matches(Regex("^[A-Z]{2,4}$"))) {
            parts.add(if (addonName.length > 20) addonName.take(17) + "..." else addonName)
        }
        return parts.filter { it.isNotEmpty() }.joinToString(" | ")
    }
    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
        val unitIndex = if (digitGroups < units.size) digitGroups else units.size - 1
        return DecimalFormat("#,##0.#").format(bytes / 1024.0.pow(digitGroups.toDouble())) + " " + units[unitIndex]
    }
}

data class ParsedFile(val title: String?, val year: String?, val resolution: String?, val edition: String?, val visualTags: List<String>?, val audioTags: List<String>?, val languages: List<String>?, val seasonPack: Boolean?) : Serializable
data class ServiceInfo(val id: String?, val name: String?, val shortName: String?, val cached: Boolean?) : Serializable
data class BehaviorHints(val notWebReady: Boolean? = false) : Serializable
data class MetaResponse(val meta: Meta)
data class Meta(val id: String, val type: String, val name: String, val poster: String?, val background: String?, val description: String?, val videos: List<Video>?, val rating: String? = null) : Serializable
data class Video(val id: String, val title: String, val released: String?, val thumbnail: String?, val number: Int?, val season: Int?, val overview: String? = null, val rating: String? = null) : Serializable

data class MetaItem(
    val id: String,
    val type: String,
    val name: String,
    val poster: String?,
    val background: String?,
    val description: String?,
    val releaseDate: String? = null,
    val rating: String? = null,
    val popularity: Double? = null,  // For sorting search/similar/actor results
    var isWatched: Boolean = false,
    var progress: Long = 0,
    var duration: Long = 0,
    // NEW: Landscape Flag
    var isLandscape: Boolean = false,
    // Genre fields for genre items
    val genreId: Int? = null,
    val genreType: String? = null,
    // Genres for movies/series (JSON array stored as string, same format as CollectedItem)
    val genres: String? = null
) : Serializable

data class Manifest(val id: String, val version: String, val name: String, val description: String?, val types: List<String>, val catalogs: List<Catalog>) : Serializable
data class Catalog(val type: String, val id: String, val name: String, @Json(name = "extra") val extraProps: List<ExtraProp>?) : Serializable { data class ExtraProp(val name: String, val isRequired: Boolean? = false) : Serializable }
data class CatalogResponse(val metas: List<MetaItem>) : Serializable

// Subtitle models for AIOStreams
data class SubtitleResponse(val subtitles: List<Subtitle>) : Serializable

data class Subtitle(
    val id: String,
    val url: String,
    val lang: String,
    @Json(name = "sub_id") val subId: Int? = null,
    @Json(name = "ai_translated") val aiTranslated: Boolean? = null,
    @Json(name = "from_trusted") val fromTrusted: Boolean? = null,
    @Json(name = "uploader_id") val uploaderId: Int? = null,
    @Json(name = "lang_code") val langCode: String? = null,
    val title: String? = null,
    val moviehash: String? = null,
    @Json(name = "SubEncoding") val subEncoding: String? = null,
    val m: String? = null,  // Additional field from AIOStreams
    val g: String? = null   // Additional field from AIOStreams
) : Serializable {
    val formattedTitle: String get() = if (!title.isNullOrEmpty()) "AIO - $title" else "AIO - Subtitle"
}