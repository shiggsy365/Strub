package com.example.stremiompvplayer

import android.net.Uri
import android.os.Bundle
import androidx.leanback.app.VideoSupportFragment
import androidx.leanback.app.VideoSupportFragmentGlueHost
import androidx.leanback.media.MediaPlayerAdapter
import androidx.leanback.media.PlaybackTransportControlGlue
import androidx.leanback.widget.PlaybackControlsRow

/**
 * Handles video playback with a customizable overlay using VideoSupportFragment.
 */
class PlaybackVideoFragment : VideoSupportFragment() {

    private lateinit var transportControlGlue: PlaybackTransportControlGlue<MediaPlayerAdapter>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val glueHost = VideoSupportFragmentGlueHost(this)
        val playerAdapter = MediaPlayerAdapter(activity)
        playerAdapter.setRepeatAction(PlaybackControlsRow.RepeatAction.INDEX_NONE)

        transportControlGlue = PlaybackTransportControlGlue(activity, playerAdapter)
        transportControlGlue.host = glueHost
        transportControlGlue.title = "Stremio MPV Player"
        transportControlGlue.subtitle = "Playing video..."
        transportControlGlue.playWhenPrepared()

        // Replace this with the actual video URL you intend to play
        val videoUrl = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        playerAdapter.setDataSource(Uri.parse(videoUrl))
    }

    override fun onPause() {
        super.onPause()
        transportControlGlue.pause()
    }
}