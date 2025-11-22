package com.kasahirotech.dashcamapp.models

/**
 * Represents cloud storage quota information
 */
data class StorageQuota(
    val limit: Long,
    val usage: Long,
    val usageInDrive: Long
) {
    val availableSpace: Long
        get() = limit - usage
    
    val usagePercentage: Float
        get() = if (limit > 0) (usage.toFloat() / limit.toFloat()) * 100 else 0f
}
