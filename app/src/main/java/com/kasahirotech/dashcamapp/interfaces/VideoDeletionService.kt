package com.kasahirotech.dashcamapp.interfaces

import android.content.Context
import com.kasahirotech.dashcamapp.models.AppVideo

/**
 * Service interface for deleting videos from MediaStore and file system
 */
interface VideoDeletionService {
    /**
     * Deletes videos from MediaStore and file system
     * @param context Android context for ContentResolver access
     * @param videos List of videos to delete
     * @return Result indicating success or failure with error details
     */
    suspend fun deleteVideos(context: Context, videos: List<AppVideo>): DeletionResult

    /**
     * Checks if the app has permission to delete media files
     * @param context Android context
     * @return true if permission is granted
     */
    fun hasDeletePermission(context: Context): Boolean
}

/**
 * Result of a video deletion operation
 */
data class DeletionResult(
    val success: Boolean,
    val deletedCount: Int,
    val failedVideos: List<AppVideo> = emptyList(),
    val errorMessage: String? = null
)
