package com.example.stremiompvplayer.utils

import com.example.stremiompvplayer.models.EPGProgram
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

object EPGParser {

    private val dateFormat = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US)

    suspend fun parseEPG(url: String, onProgress: ((String) -> Unit)? = null): List<EPGProgram> {
        val programs = mutableListOf<EPGProgram>()

        try {
            onProgress?.invoke("Downloading EPG data...")
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            onProgress?.invoke("Parsing XML...")
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(BufferedReader(InputStreamReader(connection.inputStream)))

            var eventType = parser.eventType
            var currentChannelId: String? = null
            var currentTitle: String? = null
            var currentDesc: String? = null
            var currentStart: Long? = null
            var currentStop: Long? = null
            var currentCategory: String? = null
            var currentIcon: String? = null
            var programCount = 0

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "programme" -> {
                                currentChannelId = parser.getAttributeValue(null, "channel")
                                val startStr = parser.getAttributeValue(null, "start")
                                val stopStr = parser.getAttributeValue(null, "stop")

                                currentStart = parseDate(startStr)
                                currentStop = parseDate(stopStr)
                            }
                            "title" -> {
                                currentTitle = parser.nextText()
                            }
                            "desc" -> {
                                currentDesc = parser.nextText()
                            }
                            "category" -> {
                                currentCategory = parser.nextText()
                            }
                            "icon" -> {
                                currentIcon = parser.getAttributeValue(null, "src")
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "programme") {
                            // Save program
                            if (currentChannelId != null && currentTitle != null && currentStart != null && currentStop != null) {
                                programs.add(
                                    EPGProgram(
                                        channelId = currentChannelId,
                                        title = currentTitle,
                                        description = currentDesc,
                                        startTime = currentStart,
                                        endTime = currentStop,
                                        category = currentCategory,
                                        icon = currentIcon
                                    )
                                )
                                programCount++
                                if (programCount % 100 == 0) {
                                    onProgress?.invoke("Parsed $programCount programs...")
                                }
                            }

                            // Reset for next program
                            currentChannelId = null
                            currentTitle = null
                            currentDesc = null
                            currentStart = null
                            currentStop = null
                            currentCategory = null
                            currentIcon = null
                        }
                    }
                }
                eventType = parser.next()
            }

            connection.disconnect()
            onProgress?.invoke("Completed: Parsed ${programs.size} programs")
        } catch (e: Exception) {
            onProgress?.invoke("Error: ${e.message}")
            e.printStackTrace()
        }

        return programs
    }

    private fun parseDate(dateStr: String?): Long? {
        if (dateStr.isNullOrEmpty()) return null

        return try {
            // XMLTV format: 20250101120000 +0000
            dateFormat.parse(dateStr)?.time
        } catch (e: Exception) {
            try {
                // Try without timezone if format doesn't match
                val simpleDateFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
                simpleDateFormat.parse(dateStr)?.time
            } catch (e2: Exception) {
                null
            }
        }
    }

    fun getCurrentProgram(programs: List<EPGProgram>, currentTime: Long = System.currentTimeMillis()): EPGProgram? {
        return programs.firstOrNull { program ->
            currentTime >= program.startTime && currentTime < program.endTime
        }
    }

    fun getUpcomingPrograms(programs: List<EPGProgram>, currentTime: Long = System.currentTimeMillis(), limit: Int = 5): List<EPGProgram> {
        return programs.filter { it.startTime > currentTime }
            .sortedBy { it.startTime }
            .take(limit)
    }
}
