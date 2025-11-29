package com.kasahirotech.dashcamapp.models

/**
 * Data class representing information about a physical camera on the device.
 * 
 * @property id Unique camera identifier from CameraX
 * @property displayName User-friendly name for the camera
 * @property lensFacing Camera lens facing direction (LENS_FACING_BACK or LENS_FACING_FRONT)
 * @property fieldOfView Field of view classification
 * @property minZoomRatio Minimum supported zoom ratio
 * @property maxZoomRatio Maximum supported zoom ratio
 * @property supportsZoom Whether the camera supports zoom operations
 */
data class CameraInfo(
    val id: String,
    val displayName: String,
    val lensFacing: Int,
    val fieldOfView: FieldOfViewType,
    val minZoomRatio: Float,
    val maxZoomRatio: Float,
    val supportsZoom: Boolean
)

/**
 * Enum representing the field of view type of a camera.
 */
enum class FieldOfViewType {
    ULTRA_WIDE,  // < 90 degrees
    WIDE,        // 90-120 degrees
    STANDARD,    // 120-180 degrees
    TELEPHOTO    // > 180 degrees (narrow field of view)
}
