package com.example.stremiompvplayer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class TestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create a simple UI programmatically
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(100, 100, 100, 100)
            setBackgroundColor(android.graphics.Color.BLACK)
        }

        val title = TextView(this).apply {
            text = "Stremio Player - Test Screen"
            textSize = 32f
            setTextColor(android.graphics.Color.WHITE)
            setPadding(0, 0, 0, 50)
        }

        val message = TextView(this).apply {
            text = "✅ App is working!\n\nThe APK built successfully and the app can launch.\n\nNow we need to debug why the main UI isn't loading."
            textSize = 20f
            setTextColor(android.graphics.Color.WHITE)
            setPadding(0, 0, 0, 50)
        }

        val mainButton = Button(this).apply {
            text = "Try Main Activity"
            textSize = 18f
            setOnClickListener {
                try {
                    startActivity(Intent(this@TestActivity, MainActivity::class.java))
                } catch (e: Exception) {
                    message.text = "Error launching MainActivity:\n${e.message}"
                }
            }
        }

        val userButton = Button(this).apply {
            text = "Try User Selection"
            textSize = 18f
            setOnClickListener {
                try {
                    startActivity(Intent(this@TestActivity, UserSelectionActivity::class.java))
                } catch (e: Exception) {
                    message.text = "Error launching UserSelection:\n${e.message}"
                }
            }
        }

        layout.addView(title)
        layout.addView(message)
        layout.addView(mainButton)
        layout.addView(userButton)

        setContentView(layout)
    }
}