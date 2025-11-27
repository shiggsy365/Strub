package com.example.stremiompvplayer.utils

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.stremiompvplayer.workers.TraktSyncWorker
import java.util.concurrent.TimeUnit

/**
 * Utility class to schedule and manage background Trakt synchronization
 */
object TraktSyncScheduler {
    private const val TAG = "TraktSyncScheduler"

    /**
     * Schedule periodic background sync based on user preferences
     */
    fun schedulePeriodicSync(context: Context) {
        val prefsManager = SharedPreferencesManager.getInstance(context)

        // Cancel if background sync is disabled
        if (!prefsManager.isBackgroundSyncEnabled()) {
            cancelPeriodicSync(context)
            return
        }

        val intervalMillis = prefsManager.getBackgroundSyncInterval()
        val wifiOnly = prefsManager.isSyncOnWifiOnly()

        // Convert milliseconds to hours (minimum 15 minutes for WorkManager)
        val intervalHours = (intervalMillis / (60 * 60 * 1000)).coerceAtLeast(1)

        // Build constraints
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true) // Don't sync if battery is low
            .build()

        // Build periodic work request
        val syncWorkRequest = PeriodicWorkRequestBuilder<TraktSyncWorker>(
            intervalHours,
            TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .addTag(TraktSyncWorker.WORK_NAME)
            .build()

        // Schedule work (replace existing if already scheduled)
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            TraktSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            syncWorkRequest
        )

        Log.i(TAG, "Scheduled periodic Trakt sync every $intervalHours hours (Wi-Fi only: $wifiOnly)")
    }

    /**
     * Cancel periodic background sync
     */
    fun cancelPeriodicSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(TraktSyncWorker.WORK_NAME)
        Log.i(TAG, "Cancelled periodic Trakt sync")
    }

    /**
     * Check if periodic sync is scheduled
     */
    fun isPeriodicSyncScheduled(context: Context): Boolean {
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(TraktSyncWorker.WORK_NAME)
            .get()
        return workInfos.isNotEmpty()
    }
}
