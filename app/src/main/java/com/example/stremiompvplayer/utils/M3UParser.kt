package com.example.stremiompvplayer.utils

import com.example.stremiompvplayer.models.Channel
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object M3UParser {

    suspend fun parseM3U(url: String): List<Channel> {
        val channels = mutableListOf<Channel>()

        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                var line: String?
                var currentName: String? = null
                var currentLogo: String? = null
                var currentGroup: String? = null
                var currentTvgId: String? = null
                var channelIndex = 0

                while (reader.readLine().also { line = it } != null) {
                    val trimmedLine = line?.trim() ?: continue

                    if (trimmedLine.startsWith("#EXTINF:")) {
                        // Parse metadata line
                        currentName = extractName(trimmedLine)
                        currentLogo = extractAttribute(trimmedLine, "tvg-logo")
                        currentGroup = extractAttribute(trimmedLine, "group-title")
                        currentTvgId = extractAttribute(trimmedLine, "tvg-id")

                    } else if (!trimmedLine.startsWith("#") && trimmedLine.isNotEmpty()) {
                        // This is the URL line
                        val channelUrl = trimmedLine
                        val channelName = currentName ?: "Channel ${channelIndex + 1}"

                        channels.add(
                            Channel(
                                id = currentTvgId ?: "channel_$channelIndex",
                                name = channelName,
                                url = channelUrl,
                                logo = currentLogo,
                                group = currentGroup,
                                tvgId = currentTvgId
                            )
                        )

                        channelIndex++

                        // Reset for next channel
                        currentName = null
                        currentLogo = null
                        currentGroup = null
                        currentTvgId = null
                    }
                }
            }

            connection.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return channels
    }

    private fun extractName(line: String): String {
        // Extract name after the last comma
        val commaIndex = line.lastIndexOf(',')
        return if (commaIndex != -1 && commaIndex < line.length - 1) {
            line.substring(commaIndex + 1).trim()
        } else {
            "Unknown Channel"
        }
    }

    private fun extractAttribute(line: String, attributeName: String): String? {
        // Look for attribute="value" or attribute='value'
        val regex = """$attributeName="([^"]*)"""".toRegex()
        val match = regex.find(line)

        if (match != null) {
            return match.groupValues[1]
        }

        // Try single quotes
        val regexSingle = """$attributeName='([^']*)'""".toRegex()
        val matchSingle = regexSingle.find(line)

        return matchSingle?.groupValues?.get(1)
    }
}
