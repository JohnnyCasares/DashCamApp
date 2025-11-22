package com.kasahirotech.dashcamapp.models

/**
 * Represents metadata for a file stored in cloud storage (Google Drive)
 */
data class CloudFileInfo(
    val id: String,
    val name: String,
    val size: Long,
    val mimeType: String,
    val createdTime: Long,
    val modifiedTime: Long,
    val webViewLink: String?
)
