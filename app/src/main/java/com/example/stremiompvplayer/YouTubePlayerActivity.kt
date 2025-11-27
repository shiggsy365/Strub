package com.example.stremiompvplayer

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.example.stremiompvplayer.databinding.ActivityYoutubePlayerBinding

class YouTubePlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityYoutubePlayerBinding
    private var youtubeKey: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityYoutubePlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Hide system UI for immersive experience
        hideSystemUi()

        youtubeKey = intent.getStringExtra("YOUTUBE_KEY")
        val title = intent.getStringExtra("TITLE") ?: "Trailer"

        if (youtubeKey == null) {
            finish()
            return
        }

        binding.trailerTitle.text = title
        setupWebView()
    }

    private fun setupWebView() {
        val webView = binding.webView
        val webSettings: WebSettings = webView.settings

        // Enable JavaScript (required for YouTube player)
        webSettings.javaScriptEnabled = true

        // Enable DOM storage
        webSettings.domStorageEnabled = true

        // Enable media playback
        webSettings.mediaPlaybackRequiresUserGesture = false

        // Set cache mode
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT

        // Configure WebView clients
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()

        // Load YouTube IFrame Player
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body, html {
                        margin: 0;
                        padding: 0;
                        width: 100%;
                        height: 100%;
                        background-color: #000;
                        overflow: hidden;
                    }
                    #player {
                        position: absolute;
                        top: 0;
                        left: 0;
                        width: 100%;
                        height: 100%;
                    }
                </style>
            </head>
            <body>
                <div id="player"></div>
                <script>
                    var tag = document.createElement('script');
                    tag.src = "https://www.youtube.com/iframe_api";
                    var firstScriptTag = document.getElementsByTagName('script')[0];
                    firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

                    var player;
                    function onYouTubeIframeAPIReady() {
                        player = new YT.Player('player', {
                            height: '100%',
                            width: '100%',
                            videoId: '$youtubeKey',
                            playerVars: {
                                'autoplay': 1,
                                'playsinline': 1,
                                'controls': 1,
                                'rel': 0,
                                'modestbranding': 1,
                                'fs': 1
                            },
                            events: {
                                'onReady': onPlayerReady,
                                'onStateChange': onPlayerStateChange
                            }
                        });
                    }

                    function onPlayerReady(event) {
                        event.target.playVideo();
                    }

                    function onPlayerStateChange(event) {
                        // YT.PlayerState.ENDED = 0
                        if (event.data == 0) {
                            // Video ended, notify Android to finish activity
                            Android.onVideoEnded();
                        }
                    }
                </script>
            </body>
            </html>
        """.trimIndent()

        // Add JavaScript interface to handle video end
        webView.addJavascriptInterface(object {
            @android.webkit.JavascriptInterface
            fun onVideoEnded() {
                runOnUiThread {
                    finish()
                }
            }
        }, "Android")

        webView.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
    }

    private fun hideSystemUi() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        )
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onDestroy() {
        binding.webView.destroy()
        super.onDestroy()
    }
}
