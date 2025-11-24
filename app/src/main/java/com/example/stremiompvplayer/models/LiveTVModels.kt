package com.example.stremiompvplayer.models

data class Channel(
    val id: String,
    val name: String,
    val url: String,
    val logo: String? = null,
    val group: String? = null,
    val tvgId: String? = null
)

data class EPGProgram(
    val channelId: String,
    val title: String,
    val description: String? = null,
    val startTime: Long,
    val endTime: Long,
    val category: String? = null,
    val icon: String? = null
)

data class ChannelWithPrograms(
    val channel: Channel,
    val currentProgram: EPGProgram? = null,
    val nextPrograms: List<EPGProgram> = emptyList()
)

data class TVGuideEntry(
    val channel: Channel,
    val programs: List<EPGProgram>
)
