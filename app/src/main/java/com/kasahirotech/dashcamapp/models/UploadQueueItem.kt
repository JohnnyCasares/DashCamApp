package com.kasahirotech.dashcamapp.models

import java.util.UUID

/**
 * Represents an item in the upload queue
 */
data class UploadQueueItem(
    val id: String = UUID.randomUUID().toString(),
    val videoPath: String,
    val fileName: String,
    val fileSize: Long,
    val addedTime: Long,
    val priority: UploadPriority,
    val status: UploadStatus,
    val progress: Float = 0f,
    val cloudFileId: String? = null,
    val retryCount: Int = 0,
    val lastError: String? = null
)
