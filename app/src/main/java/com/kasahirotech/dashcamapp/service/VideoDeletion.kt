package com.kasahirotech.dashcamapp.service

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.kasahirotech.dashcamapp.interfaces.DeletionResult
import com.kasahirotech.dashcamapp.interfaces.VideoDeletionService
import com.kasahirotech.dashcamapp.models.AppVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementation of VideoDeletionService for deleting videos from MediaStore
 */
class VideoDeletion : VideoDeletionService {

    override suspend fun deleteVideos(context: Context, videos: List<AppVideo>): DeletionResult {
        return withContext(Dispatchers.IO) {
            val failedVideos = mutableListOf<AppVideo>()
            var deletedCount = 0

            for (video in videos) {
                try {
                    val deleted = deleteVideo(context, video)
                    if (deleted) {
                        deletedCount++
                    } else {
                        failedVideos.add(video)
                    }
                } catch (e: Exception) {
                    failedVideos.add(video)
                }
            }

            val success = failedVideos.isEmpty()
            val errorMessage = if (!success) {
                "Failed to delete ${failedVideos.size} video(s)"
            } else null

            DeletionResult(
                success = success,
                deletedCount = deletedCount,
                failedVideos = failedVideos,
                errorMessage = errorMessage
            )
        }
    }

    private fun deleteVideo(context: Context, video: AppVideo): Boolean {
        return try {
            val rowsDeleted = context.contentResolver.delete(
                video.uri,
                null,
                null
            )
            rowsDeleted > 0
        } catch (e: SecurityException) {
            // On Android 10+, we might need to request permission via createDeleteRequest
            // For now, return false to indicate failure
            false
        }
    }

    override fun hasDeletePermission(context: Context): Boolean {
        // On Android 10+ (API 29+), apps can delete their own media files without special permissions
        // For files created by other apps, the system will show a permission dialog
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }
}
