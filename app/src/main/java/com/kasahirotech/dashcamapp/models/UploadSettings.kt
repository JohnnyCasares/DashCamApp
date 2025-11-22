package com.kasahirotech.dashcamapp.models

/**
 * Represents user preferences for upload behavior
 */
data class UploadSettings(
    val autoUploadEnabled: Boolean = false,
    val wifiOnlyMode: Boolean = true,
    val minimumBatteryLevel: Int = 20,
    val maxRetryAttempts: Int = 3,
    val deleteLocalAfterUpload: Boolean = false
)
