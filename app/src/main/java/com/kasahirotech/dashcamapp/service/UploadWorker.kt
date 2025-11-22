package com.kasahirotech.dashcamapp.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Background worker for processing Google Drive upload queue
 * Runs periodically to upload queued videos when conditions are met
 */
class UploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "UploadWorker"
        const val WORK_NAME = "upload_worker"
    }
    
    override suspend fun doWork(): Result {
        Log.d(TAG, "Upload worker started")
        
        return try {
            // Check if upload conditions are met
            if (!UploadQueueManager.canUpload(applicationContext)) {
                Log.d(TAG, "Upload conditions not met, will retry later")
                return Result.retry()
            }
            
            // Process the upload queue
            UploadQueueManager.processQueue(applicationContext)
            
            Log.d(TAG, "Upload worker completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Upload worker failed", e)
            Result.retry()
        }
    }
}
