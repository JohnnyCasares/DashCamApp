package com.kasahirotech.dashcamapp.models

/**
 * Represents the current status of a video upload
 */
enum class UploadStatus {
    PENDING,
    UPLOADING,
    COMPLETED,
    FAILED,
    PAUSED,
    CANCELLED
}
