package com.kasahirotech.dashcamapp.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

// In service/PermissionHandler.kt

class PermissionHandler(private val context: Context) {

    fun allPermissionsGranted(): Boolean {
        // Loop through the permissions array required for the specific SDK version
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    companion object {
        // Define all possible permissions here
        const val CAMERA = Manifest.permission.CAMERA
        const val RECORD_AUDIO = Manifest.permission.RECORD_AUDIO
        const val WRITE_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE
        const val READ_MEDIA_VIDEO = Manifest.permission.READ_MEDIA_VIDEO

        // Use a function to get the correct permissions based on the device's SDK version
        val REQUIRED_PERMISSIONS: Array<String> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // For Android 13 (API 33) and above
                arrayOf(CAMERA, RECORD_AUDIO, READ_MEDIA_VIDEO)
            } else
                // For Android 10 (API 29) to 12 (API 32)
                // No storage permissions are needed because of Scoped Storage.
                arrayOf(CAMERA, RECORD_AUDIO)
    }
}
