package com.example.stremiompvplayer.models

import com.squareup.moshi.Json
import java.io.Serializable
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

// --- API RESPONSE MODELS ---

data class StreamResponse(
    val streams: List<Stream>
)

data class Stream(
    val name: String?,
    val description: String?,
    val infoHash: String?,
    val url: String?,
    val ytId: String?,
    val behaviorHints: BehaviorHints?,

    // AIOStreams Rich Metadata Fields
    val proxied: Boolean? = false,
    val library: Boolean? = false,
    val service: ServiceInfo?,
    @Json(name = "type") val streamType: String?,
    val resolution: String?,
    val size: Long?,
    val seeders: Int?,
    val addon: String?
) : Serializable {

    val title: String
        get() = name ?: "Unknown Stream"

    // FIX: Re-implementing formattedTitle based on the provided regex-like syntax
    val formattedTitle: String
        get() {
            val parts = mutableListOf<String>()
            val sbIconsAndType = StringBuilder()

            // Defaulting nullable booleans to false for checks
            val isCached = service?.cached ?: false
            val isProxied = proxied ?: false
            val isLibrary = library ?: false
            val seedCount = seeders ?: 0
            val type = streamType ?: ""

            // --- Part 1: Icons, Type, and Seeders ---

            // Icons
            if (isProxied) sbIconsAndType.append("🔒")
            if (isLibrary) sbIconsAndType.append("📚")

            // Cached Status
            if (isCached) sbIconsAndType.append("⚡") else sbIconsAndType.append("⏳")

            // Service/Type String
            val typeStr = when (type) {
                "debrid" -> "DB"
                "usenet" -> "UN"
                "p2p" -> "P2P"
                "http" -> "Web"
                "youtube" -> "YT"
                "live" -> "Live"
                else -> ""
            }
            sbIconsAndType.append(typeStr)

            // Seeders Conditional: (not cached OR type is p2p) AND seeders > 0
            if ((!isCached || type == "p2p") && seedCount > 0) {
                // Ensure a space separation only if preceding characters exist
                if (sbIconsAndType.isNotEmpty()) {
                    sbIconsAndType.append(" ")
                }
                sbIconsAndType.append("🌱$seedCount")
            }

            if (sbIconsAndType.isNotBlank()) {
                parts.add(sbIconsAndType.toString().trim())
            }

            // --- Part 2: Resolution ---
            val res = when (resolution) {
                "2160p" -> "4K"
                "1440p" -> "2K"
                "1080p" -> "FHD"
                "720p" -> "HD"
                "576p", "480p", "360p", "240p", "144p" -> "SD"
                else -> resolution ?: ""
            }
            if (res.isNotEmpty()) parts.add(res)

            // --- Part 3: Size ---
            if ((size ?: 0) > 0) {
                parts.add(formatBytes(size!!))
            }

            // --- Part 4: Addon Name ---
            val addonName = addon ?: name?.substringBefore(" | ") ?: ""
            if (addonName.isNotEmpty()) {
                parts.add(addonName)
            }


            // Join parts using " | "
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

data class ServiceInfo(
    val id: String?,
    val name: String?,
    val shortName: String?,
    val cached: Boolean?
) : Serializable

data class BehaviorHints(
    val notWebReady: Boolean? = false
) : Serializable

data class MetaResponse(val meta: Meta)
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
    val title: String,
    val released: String?,
    val thumbnail: String?,
    val number: Int?,
    val season: Int?,
    val overview: String? = null
) : Serializable

data class MetaItem(
    val id: String,
    val type: String,
    val name: String,
    val poster: String?,
    val background: String?,
    val description: String?
) : Serializable

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
    data class ExtraProp(val name: String, val isRequired: Boolean? = false) : Serializable
}

data class CatalogResponse(val metas: List<MetaItem>) : Serializable