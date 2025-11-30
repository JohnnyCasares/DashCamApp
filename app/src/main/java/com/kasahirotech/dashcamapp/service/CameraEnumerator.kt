package com.kasahirotech.dashcamapp.service

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import com.kasahirotech.dashcamapp.interfaces.CameraEnumeratorService
import com.kasahirotech.dashcamapp.models.CameraInfo
import com.kasahirotech.dashcamapp.models.FieldOfViewType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Implementation of CameraEnumeratorService for discovering and providing information
 * about available cameras on the device using CameraX.
 */
class CameraEnumerator : CameraEnumeratorService {
    
    companion object {
        private const val TAG = "CameraEnumerator"
        private const val DEFAULT_CAMERA_ID = "0"
    }
    
    /**
     * Gets a list of all available cameras on the device.
     * 
     * @param context Application or Activity context
     * @return List of CameraInfo objects representing available cameras
     */
    override suspend fun getCameraList(context: Context): List<CameraInfo> = withContext(Dispatchers.Main) {
        try {
            // First, log Camera2 API information for debugging
            logCamera2Info(context)
            
            val cameraProvider = suspendCoroutine<ProcessCameraProvider> { continuation ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    continuation.resume(cameraProviderFuture.get())
                }, androidx.core.content.ContextCompat.getMainExecutor(context))
            }
            
            val cameraInfoList = mutableListOf<CameraInfo>()
            val availableCameraInfos = cameraProvider.availableCameraInfos
            
            Log.d(TAG, "Found ${availableCameraInfos.size} CameraX cameras")
            
            availableCameraInfos.forEachIndexed { index, cameraInfo ->
                try {
                    val lensFacing = getLensFacing(cameraInfo)
                    val zoomState = cameraInfo.zoomState.value
                    val minZoom = zoomState?.minZoomRatio ?: 1.0f
                    val maxZoom = zoomState?.maxZoomRatio ?: 1.0f
                    val supportsZoom = maxZoom > minZoom
                    
                    // Generate camera ID from index
                    val cameraId = index.toString()
                    
                    // Determine field of view type based on zoom capabilities
                    val fovType = determineFieldOfViewType(minZoom, maxZoom, lensFacing)
                    
                    // Generate display name
                    val displayName = generateDisplayName(lensFacing, fovType, index, availableCameraInfos.size)
                    
                    val info = CameraInfo(
                        id = cameraId,
                        displayName = displayName,
                        lensFacing = lensFacing,
                        fieldOfView = fovType,
                        minZoomRatio = minZoom,
                        maxZoomRatio = maxZoom,
                        supportsZoom = supportsZoom
                    )
                    
                    cameraInfoList.add(info)
                    Log.d(TAG, "Camera $cameraId: $displayName, zoom: ${minZoom}x-${maxZoom}x, FOV: $fovType")
                    
                    // Log additional camera characteristics for debugging
                    try {
                        val intrinsicZoomRatio = cameraInfo.intrinsicZoomRatio
                        Log.d(TAG, "  Intrinsic zoom ratio: $intrinsicZoomRatio")
                    } catch (e: Exception) {
                        Log.d(TAG, "  Intrinsic zoom ratio not available")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing camera at index $index", e)
                }
            }
            
            cameraInfoList
        } catch (e: Exception) {
            Log.e(TAG, "Error enumerating cameras", e)
            emptyList()
        }
    }
    
    /**
     * Logs Camera2 API information for debugging physical camera capabilities.
     * This helps understand what cameras are available at the hardware level.
     */
    private fun logCamera2Info(context: Context) {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraIds = cameraManager.cameraIdList
            
            Log.d(TAG, "=== Camera2 API Info ===")
            Log.d(TAG, "Found ${cameraIds.size} Camera2 cameras")
            
            cameraIds.forEach { cameraId ->
                try {
                    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                    val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    val facingStr = when (facing) {
                        CameraCharacteristics.LENS_FACING_BACK -> "BACK"
                        CameraCharacteristics.LENS_FACING_FRONT -> "FRONT"
                        CameraCharacteristics.LENS_FACING_EXTERNAL -> "EXTERNAL"
                        else -> "UNKNOWN"
                    }
                    
                    // Get zoom ratio range if available (API 30+)
                    val zoomRatioRange = try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                            characteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        null
                    }
                    
                    // Get physical camera IDs if this is a logical camera
                    val physicalCameraIds = try {
                        characteristics.physicalCameraIds
                    } catch (e: Exception) {
                        emptySet()
                    }
                    
                    Log.d(TAG, "Camera2 ID: $cameraId, Facing: $facingStr")
                    if (zoomRatioRange != null) {
                        Log.d(TAG, "  Zoom range: ${zoomRatioRange.lower}x - ${zoomRatioRange.upper}x")
                    }
                    if (physicalCameraIds.isNotEmpty()) {
                        Log.d(TAG, "  Physical cameras: ${physicalCameraIds.joinToString()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading Camera2 characteristics for ID: $cameraId", e)
                }
            }
            Log.d(TAG, "=== End Camera2 Info ===")
        } catch (e: Exception) {
            Log.e(TAG, "Error accessing Camera2 API", e)
        }
    }
    
    /**
     * Gets information about a specific camera by ID.
     * 
     * @param context Application or Activity context
     * @param cameraId Unique camera identifier
     * @return CameraInfo object if camera exists, null otherwise
     */
    override suspend fun getCameraInfo(context: Context, cameraId: String): CameraInfo? {
        return try {
            val cameraList = getCameraList(context)
            cameraList.find { it.id == cameraId }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting camera info for ID: $cameraId", e)
            null
        }
    }
    
    /**
     * Gets the default camera ID (typically the back camera).
     * 
     * @return Default camera ID string
     */
    override fun getDefaultCameraId(): String {
        return DEFAULT_CAMERA_ID
    }
    
    /**
     * Determines the lens facing direction from CameraInfo.
          */
    private fun getLensFacing(cameraInfo: androidx.camera.core.CameraInfo): Int {
        return try {
            // Try to determine lens facing from camera selector
            when {
                hasLensFacing(cameraInfo, CameraSelector.LENS_FACING_BACK) -> CameraSelector.LENS_FACING_BACK
                hasLensFacing(cameraInfo, CameraSelector.LENS_FACING_FRONT) -> CameraSelector.LENS_FACING_FRONT
                else -> CameraSelector.LENS_FACING_BACK // Default to back
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error determining lens facing, defaulting to back", e)
            CameraSelector.LENS_FACING_BACK
        }
    }
    
    /**
     * Checks if a camera has a specific lens facing.
     */
    private fun hasLensFacing(cameraInfo: androidx.camera.core.CameraInfo, lensFacing: Int): Boolean {
        return try {
            val selector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()
            selector.filter(listOf(cameraInfo)).isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Determines the field of view type based on zoom capabilities.
     * This is a heuristic approach since CameraX doesn't directly expose FOV.
     */
    private fun determineFieldOfViewType(minZoom: Float, maxZoom: Float, lensFacing: Int): FieldOfViewType {
        return when {
            // Ultra-wide cameras typically have min zoom < 1.0 (includes 0.6x, 0.7x cameras)
            minZoom < 0.95f -> FieldOfViewType.ULTRA_WIDE
            // Telephoto cameras typically have min zoom > 1.0
            minZoom > 1.5f -> FieldOfViewType.TELEPHOTO
            // Wide cameras have good zoom range
            maxZoom > 8.0f -> FieldOfViewType.WIDE
            // Standard cameras
            else -> FieldOfViewType.STANDARD
        }
    }
    
    /**
     * Generates a user-friendly display name for a camera.
     */
    private fun generateDisplayName(
        lensFacing: Int,
        fovType: FieldOfViewType,
        index: Int,
        totalCameras: Int
    ): String {
        val facingStr = if (lensFacing == CameraSelector.LENS_FACING_FRONT) "Front" else "Back"
        
        val fovStr = when (fovType) {
            FieldOfViewType.ULTRA_WIDE -> "Ultra-Wide"
            FieldOfViewType.WIDE -> "Wide"
            FieldOfViewType.STANDARD -> ""
            FieldOfViewType.TELEPHOTO -> "Telephoto"
        }
        
        // If there are multiple cameras with same facing, add index
        val indexStr = if (totalCameras > 2) " ($index)" else ""
        
        return if (fovStr.isNotEmpty()) {
            "$facingStr $fovStr$indexStr"
        } else {
            "$facingStr$indexStr"
        }
    }
}
