package com.kasahirotech.dashcamapp.service

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Manages scheduling of background upload work
 */
object UploadScheduler {
    private const val TAG = "UploadScheduler"
    private const val UPLOAD_WORK_NAME = "periodic_upload_work"
    private const val REPEAT_INTERVAL_MINUTES = 30L
    
    /**
     * Schedule periodic upload work
     */
    fun schedulePeriodicUpload(context: Context) {
        try {
            val constraints = buildConstraints(context)
            
            val uploadWorkRequest = PeriodicWorkRequestBuilder<UploadWorker>(
                REPEAT_INTERVAL_MINUTES,
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UPLOAD_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                uploadWorkRequest
            )
            
            Log.d(TAG, "Periodic upload work scheduled")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling periodic upload work", e)
        }
    }
    
    /**
     * Cancel periodic upload work
     */
    fun cancelPeriodicUpload(context: Context) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(UPLOAD_WORK_NAME)
            Log.d(TAG, "Periodic upload work cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling periodic upload work", e)
        }
    }
    
    /**
     * Build work constraints based on user preferences
     */
    private fun buildConstraints(context: Context): Constraints {
        val networkType = if (PreferenceManager.isWifiOnlyMode(context)) {
            NetworkType.UNMETERED // WiFi or unlimited data
        } else {
            NetworkType.CONNECTED // Any network connection
        }
        
        return Constraints.Builder()
            .setRequiredNetworkType(networkType)
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()
    }
    
    /**
     * Trigger immediate upload work (one-time)
     */
    fun triggerImmediateUpload(context: Context) {
        try {
            val constraints = buildConstraints(context)
            
            val uploadWorkRequest = androidx.work.OneTimeWorkRequestBuilder<UploadWorker>()
                .setConstraints(constraints)
                .build()
            
            WorkManager.getInstance(context).enqueue(uploadWorkRequest)
            
            Log.d(TAG, "Immediate upload work triggered")
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering immediate upload work", e)
        }
    }
}
