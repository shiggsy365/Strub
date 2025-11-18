package com.example.stremiompvplayer

import android.os.Bundle
// FIX: Change from android.app.Activity to AppCompatActivity
import androidx.appcompat.app.AppCompatActivity

/**
 * This activity shows an error condition in the browse screens.
 * FIX: Extends AppCompatActivity to get supportFragmentManager
 */
class BrowseErrorActivity : AppCompatActivity() {

    private lateinit var mErrorFragment: ErrorFragment
    // ... existing code ...
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browse_error)

        mErrorFragment = ErrorFragment()
        // FIX: Add a container ID. I'm assuming 'R.id.fragment_container'
        // You MUST have a <FrameLayout android:id="@+id/fragment_container" ... />
        // in your 'activity_browse_error.xml' layout file for this to work.
        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container, mErrorFragment, "ErrorFragment")
            .commit()
    }
}