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
    @Json(name = "type") val streamType: String?, // e.g. "debrid", "p2p"
    val resolution: String?,
    val size: Long?,
    val seeders: Int?,

    // FIXED: Defined as String to match API response
    val addon: String?
) : Serializable {

    // Fallback title if formatting fails or data is missing
    val title: String
        get() = name ?: "Unknown Stream"

    // Custom Formatter based on your specific regex/template
    val formattedTitle: String
        get() {
            val parts = mutableListOf<String>()

            // PART 1: Icons & Service Name
            val part1 = StringBuilder()
            if (proxied == true) part1.append("🔒")
            if (library == true) part1.append("📚")

            // Cache Status
            val isCached = service?.cached == true
            if (isCached) part1.append("⚡") else part1.append("⏳")

            // Service Name / Type
            when (streamType) {
                "debrid", "usenet" -> part1.append(service?.shortName ?: "")
                "p2p" -> part1.append("P2P")
                "http" -> part1.append("Web")
                "youtube" -> part1.append("YT")
                "live" -> part1.append("Live")
            }

            // Seeders (if not cached OR p2p, and seeders > 0)
            if ((!isCached || streamType == "p2p") && (seeders ?: 0) > 0) {
                part1.append(" 🌱 $seeders")
            }
            parts.add(part1.toString())

            // PART 2: Resolution
            val resString = when (resolution) {
                "2160p" -> "4K Ultra HD"
                "1440p" -> "2K Quad HD"
                "1080p" -> "Full HD"
                "720p" -> "HD"
                "576p" -> "SD (PAL)"
                "480p" -> "SD (NTSC)"
                "360p" -> "Camcorder"
                "240p" -> "Webcam"
                "144p" -> "Low Quality"
                else -> resolution // Fallback to raw string (e.g. "Unknown")
            }
            if (!resString.isNullOrEmpty()) {
                parts.add(resString)
            }

            // PART 3: Size
            if ((size ?: 0) > 0) {
                parts.add(formatBytes(size!!))
            }

            // PART 4: Addon Name (FIXED: Use string directly)
            if (!addon.isNullOrEmpty()) {
                parts.add(addon)
            }

            // Join with Pipes
            return parts.filter { it.isNotEmpty() }.joinToString(" | ")
        }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(bytes / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
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

// --- META MODELS ---
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
    val season: Int?
) : Serializable

// --- CATALOG MODELS ---
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