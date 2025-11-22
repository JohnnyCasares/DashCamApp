package com.kasahirotech.dashcamapp.service

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import com.kasahirotech.dashcamapp.models.UploadPriority
import com.kasahirotech.dashcamapp.models.UploadQueueItem
import com.kasahirotech.dashcamapp.models.UploadStatus
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Manages the upload queue for Google Drive uploads
 * Handles queue persistence, status tracking, and queue operations
 */
object UploadQueueManager {
    private const val TAG = "UploadQueueManager"
    private const val QUEUE_FILE_NAME = "upload_queue.json"
    
    private val queueCache = mutableListOf<UploadQueueItem>()
    private var isQueueLoaded = false
    private var isProcessing = false
    private var isPaused = false
    
    // Status change listeners
    private val statusChangeListeners = mutableListOf<(UploadQueueItem) -> Unit>()
    
    /**
     * Get the queue file
     */
    private fun getQueueFile(context: Context): File {
        return File(context.filesDir, QUEUE_FILE_NAME)
    }
    
    /**
     * Load queue from persistent storage
     */
    private fun loadQueue(context: Context) {
        if (isQueueLoaded) return
        
        try {
            val queueFile = getQueueFile(context)
            if (!queueFile.exists()) {
                isQueueLoaded = true
                return
            }
            
            val jsonString = queueFile.readText()
            val jsonArray = JSONArray(jsonString)
            
            queueCache.clear()
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val item = UploadQueueItem(
                    id = jsonObject.getString("id"),
                    videoPath = jsonObject.getString("videoPath"),
                    fileName = jsonObject.getString("fileName"),
                    fileSize = jsonObject.getLong("fileSize"),
                    addedTime = jsonObject.getLong("addedTime"),
                    priority = UploadPriority.valueOf(jsonObject.getString("priority")),
                    status = UploadStatus.valueOf(jsonObject.getString("status")),
                    progress = jsonObject.optDouble("progress", 0.0).toFloat(),
                    cloudFileId = jsonObject.optString("cloudFileId", null),
                    retryCount = jsonObject.optInt("retryCount", 0),
                    lastError = jsonObject.optString("lastError", null)
                )
                queueCache.add(item)
            }
            
            isQueueLoaded = true
        } catch (e: Exception) {
            Log.e(TAG, "Error loading queue", e)
            queueCache.clear()
            isQueueLoaded = true
        }
    }
    
    /**
     * Save queue to persistent storage
     */
    private fun saveQueue(context: Context) {
        try {
            val jsonArray = JSONArray()
            for (item in queueCache) {
                val jsonObject = JSONObject().apply {
                    put("id", item.id)
                    put("videoPath", item.videoPath)
                    put("fileName", item.fileName)
                    put("fileSize", item.fileSize)
                    put("addedTime", item.addedTime)
                    put("priority", item.priority.name)
                    put("status", item.status.name)
                    put("progress", item.progress)
                    put("cloudFileId", item.cloudFileId ?: "")
                    put("retryCount", item.retryCount)
                    put("lastError", item.lastError ?: "")
                }
                jsonArray.put(jsonObject)
            }
            
            val queueFile = getQueueFile(context)
            queueFile.writeText(jsonArray.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error saving queue", e)
        }
    }
    
    /**
     * Add a video to the upload queue
     */
    fun addToQueue(
        context: Context,
        videoPath: String,
        priority: UploadPriority = UploadPriority.NORMAL
    ) {
        loadQueue(context)
        
        // Check if already in queue
        if (queueCache.any { it.videoPath == videoPath }) {
            Log.d(TAG, "Video already in queue: $videoPath")
            return
        }
        
        val file = File(videoPath)
        if (!file.exists()) {
            Log.e(TAG, "File does not exist: $videoPath")
            return
        }
        
        val item = UploadQueueItem(
            videoPath = videoPath,
            fileName = file.name,
            fileSize = file.length(),
            addedTime = System.currentTimeMillis(),
            priority = priority,
            status = UploadStatus.PENDING
        )
        
        queueCache.add(item)
        saveQueue(context)
        
        Log.d(TAG, "Added to queue: ${item.fileName}")
    }
    
    /**
     * Remove a video from the upload queue
     */
    fun removeFromQueue(context: Context, videoPath: String) {
        loadQueue(context)
        
        val removed = queueCache.removeAll { it.videoPath == videoPath }
        if (removed) {
            saveQueue(context)
            Log.d(TAG, "Removed from queue: $videoPath")
        }
    }
    
    /**
     * Get all items in the queue
     */
    fun getQueue(context: Context): List<UploadQueueItem> {
        loadQueue(context)
        return queueCache.toList()
    }
    
    /**
     * Clear all items from the queue
     */
    fun clearQueue(context: Context) {
        loadQueue(context)
        queueCache.clear()
        saveQueue(context)
        Log.d(TAG, "Queue cleared")
    }
    
    /**
     * Get upload status for a specific video
     */
    fun getUploadStatus(context: Context, videoPath: String): UploadStatus {
        loadQueue(context)
        return queueCache.find { it.videoPath == videoPath }?.status ?: UploadStatus.PENDING
    }
    
    /**
     * Update upload status for a specific video
     */
    fun updateUploadStatus(
        context: Context,
        videoPath: String,
        status: UploadStatus,
        progress: Float = 0f,
        cloudFileId: String? = null,
        error: String? = null
    ) {
        loadQueue(context)
        
        val index = queueCache.indexOfFirst { it.videoPath == videoPath }
        if (index != -1) {
            val item = queueCache[index]
            val updatedItem = item.copy(
                status = status,
                progress = progress,
                cloudFileId = cloudFileId ?: item.cloudFileId,
                lastError = error,
                retryCount = if (status == UploadStatus.FAILED) item.retryCount + 1 else item.retryCount
            )
            queueCache[index] = updatedItem
            saveQueue(context)
            
            // Notify listeners of status change
            notifyStatusChange(updatedItem)
            
            Log.d(TAG, "Updated status for ${item.fileName}: $status (${progress}%)")
        }
    }
    
    /**
     * Add a status change listener
     */
    fun addStatusChangeListener(listener: (UploadQueueItem) -> Unit) {
        statusChangeListeners.add(listener)
    }
    
    /**
     * Remove a status change listener
     */
    fun removeStatusChangeListener(listener: (UploadQueueItem) -> Unit) {
        statusChangeListeners.remove(listener)
    }
    
    /**
     * Notify all listeners of a status change
     */
    private fun notifyStatusChange(item: UploadQueueItem) {
        statusChangeListeners.forEach { listener ->
            try {
                listener(item)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying status change listener", e)
            }
        }
    }
    
    /**
     * Get queue item by video path
     */
    fun getQueueItem(context: Context, videoPath: String): UploadQueueItem? {
        loadQueue(context)
        return queueCache.find { it.videoPath == videoPath }
    }
    
    /**
     * Get all pending uploads
     */
    fun getPendingUploads(context: Context): List<UploadQueueItem> {
        loadQueue(context)
        return queueCache.filter { it.status == UploadStatus.PENDING }
            .sortedWith(compareByDescending<UploadQueueItem> { it.priority }.thenBy { it.addedTime })
    }
    
    /**
     * Get all failed uploads
     */
    fun getFailedUploads(context: Context): List<UploadQueueItem> {
        loadQueue(context)
        return queueCache.filter { it.status == UploadStatus.FAILED }
    }
    
    /**
     * Get all completed uploads
     */
    fun getCompletedUploads(context: Context): List<UploadQueueItem> {
        loadQueue(context)
        return queueCache.filter { it.status == UploadStatus.COMPLETED }
            .sortedByDescending { it.addedTime }
    }
    
    /**
     * Process the upload queue
     */
    suspend fun processQueue(context: Context) {
        if (isProcessing) {
            Log.d(TAG, "Queue processing already in progress")
            return
        }
        
        if (isPaused) {
            Log.d(TAG, "Queue processing is paused")
            return
        }
        
        if (!canUpload(context)) {
            Log.d(TAG, "Upload conditions not met")
            return
        }
        
        isProcessing = true
        
        try {
            val pendingUploads = getPendingUploads(context)
            Log.d(TAG, "Processing ${pendingUploads.size} pending uploads")
            
            for (item in pendingUploads) {
                if (isPaused) {
                    Log.d(TAG, "Queue processing paused")
                    break
                }
                
                if (!canUpload(context)) {
                    Log.d(TAG, "Upload conditions no longer met, pausing queue")
                    break
                }
                
                processUpload(context, item)
            }
        } finally {
            isProcessing = false
        }
    }
    
    /**
     * Process a single upload
     */
    private suspend fun processUpload(context: Context, item: UploadQueueItem) {
        val maxRetries = PreferenceManager.getMaxRetryAttempts(context)
        
        // Check if max retries exceeded
        if (item.retryCount >= maxRetries) {
            Log.d(TAG, "Max retries exceeded for ${item.fileName}")
            updateUploadStatus(
                context,
                item.videoPath,
                UploadStatus.FAILED,
                error = "Maximum retry attempts exceeded"
            )
            return
        }
        
        // Update status to uploading
        updateUploadStatus(context, item.videoPath, UploadStatus.UPLOADING, progress = 0f)
        
        try {
            // Perform upload
            val result = GoogleDriveManager.uploadFile(
                context,
                item.videoPath,
                item.fileName
            ) { progress ->
                updateUploadStatus(context, item.videoPath, UploadStatus.UPLOADING, progress = progress)
            }
            
            result.fold(
                onSuccess = { fileId ->
                    // Upload successful
                    updateUploadStatus(
                        context,
                        item.videoPath,
                        UploadStatus.COMPLETED,
                        progress = 100f,
                        cloudFileId = fileId
                    )
                    Log.d(TAG, "Upload completed: ${item.fileName}")
                    
                    // Delete local file if setting is enabled
                    if (PreferenceManager.isDeleteLocalAfterUpload(context)) {
                        try {
                            val file = File(item.videoPath)
                            if (file.exists() && file.delete()) {
                                Log.d(TAG, "Deleted local file: ${item.fileName}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error deleting local file", e)
                        }
                    }
                },
                onFailure = { error ->
                    // Upload failed
                    val errorMessage = error.message ?: "Unknown error"
                    Log.e(TAG, "Upload failed for ${item.fileName}: $errorMessage", error)
                    
                    // Implement exponential backoff
                    val backoffDelay = calculateBackoffDelay(item.retryCount)
                    delay(backoffDelay)
                    
                    updateUploadStatus(
                        context,
                        item.videoPath,
                        UploadStatus.FAILED,
                        error = errorMessage
                    )
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error processing upload for ${item.fileName}", e)
            updateUploadStatus(
                context,
                item.videoPath,
                UploadStatus.FAILED,
                error = e.message ?: "Unknown error"
            )
        }
    }
    
    /**
     * Calculate exponential backoff delay
     */
    private fun calculateBackoffDelay(retryCount: Int): Long {
        // Exponential backoff: 2^retryCount seconds, max 5 minutes
        val delaySeconds = minOf(Math.pow(2.0, retryCount.toDouble()).toLong(), 300L)
        return delaySeconds * 1000L
    }
    
    /**
     * Pause queue processing
     */
    fun pauseQueue(context: Context) {
        isPaused = true
        Log.d(TAG, "Queue paused")
    }
    
    /**
     * Resume queue processing
     */
    fun resumeQueue(context: Context) {
        isPaused = false
        Log.d(TAG, "Queue resumed")
    }
    
    /**
     * Check if queue is currently processing
     */
    fun isProcessing(): Boolean = isProcessing
    
    /**
     * Check if queue is paused
     */
    fun isPaused(): Boolean = isPaused
    
    /**
     * Check if upload conditions are met
     */
    fun canUpload(context: Context): Boolean {
        // Check if authenticated
        if (!GoogleDriveManager.isAuthenticated(context)) {
            Log.d(TAG, "Cannot upload: Not authenticated")
            return false
        }
        
        // Check WiFi-only mode
        if (PreferenceManager.isWifiOnlyMode(context) && !isWifiConnected(context)) {
            Log.d(TAG, "Cannot upload: WiFi-only mode enabled but not connected to WiFi")
            return false
        }
        
        // Check battery level
        val minBatteryLevel = PreferenceManager.getMinimumBatteryLevel(context)
        val currentBatteryLevel = getBatteryLevel(context)
        if (currentBatteryLevel < minBatteryLevel) {
            Log.d(TAG, "Cannot upload: Battery level ($currentBatteryLevel%) below minimum ($minBatteryLevel%)")
            return false
        }
        
        // Check if network is available
        if (!isNetworkAvailable(context)) {
            Log.d(TAG, "Cannot upload: No network connection")
            return false
        }
        
        return true
    }
    
    /**
     * Check if WiFi is connected
     */
    private fun isWifiConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.type == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected
        }
    }
    
    /**
     * Check if any network is available
     */
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected == true
        }
    }
    
    /**
     * Get current battery level percentage
     */
    private fun getBatteryLevel(context: Context): Int {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        
        return if (level >= 0 && scale > 0) {
            (level.toFloat() / scale.toFloat() * 100).toInt()
        } else {
            100 // Assume full battery if unable to read
        }
    }
}
