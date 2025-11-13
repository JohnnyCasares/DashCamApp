package com.kasahirotech.dashcamapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.kasahirotech.dashcamapp.databinding.ActivityMainBinding
import com.kasahirotech.dashcamapp.screens.Gallery
import com.kasahirotech.dashcamapp.service.Camera
import com.kasahirotech.dashcamapp.service.PermissionHandler
import com.kasahirotech.dashcamapp.settings.Settings

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var camera: Camera
    private lateinit var permissionHandler: PermissionHandler

    private enum class PermissionRequestContext {
        STARTUP,
        RECORD,
        GALLERY,
        NONE
    }

    private var currentPermissionContext = PermissionRequestContext.NONE

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        //Handle permissions granted/rejected
        var permissionGranted = true
        permissions.entries.forEach {
            if (it.value == false)
                permissionGranted = false
        }
        
        if (permissionGranted) {
            // Handle context-specific actions when permissions are granted
            when (currentPermissionContext) {
                PermissionRequestContext.RECORD -> {
                    // Initialize camera if not already started, then start recording
                    if (!camera.isCameraStarted()) {
                        camera.startCamera()
                    }
                    camera.captureVideo()
                }
                PermissionRequestContext.GALLERY -> {
                    // Open Gallery activity
                    Intent(this, Gallery::class.java).also {
                        this.startActivity(it)
                    }
                }
                PermissionRequestContext.STARTUP -> {
                    // Initialize camera for startup
                    camera.startCamera()
                }
                PermissionRequestContext.NONE -> {
                    // No specific action needed
                }
            }
        } else {
            // Show context-specific error messages when permissions are denied
            val errorMessage = when (currentPermissionContext) {
                PermissionRequestContext.RECORD -> 
                    "Camera and microphone permissions are required to record videos"
                PermissionRequestContext.GALLERY -> 
                    "Media access permission is required to view videos"
                PermissionRequestContext.STARTUP -> 
                    "Permission request denied"
                PermissionRequestContext.NONE -> 
                    "Permission request denied"
            }
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
        }
        
        // Reset context to NONE after handling
        currentPermissionContext = PermissionRequestContext.NONE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        permissionHandler = PermissionHandler(this)
        camera = Camera(
            activity = this,
            binding = binding,
            surfaceProvider = binding.viewFinder.surfaceProvider,
        )
        setContentView(binding.root)

        this.permissionCheck()


        binding.imgBtnGallery.setOnClickListener {
            val galleryPermissions = permissionHandler.getGalleryPermissions()
            if (galleryPermissions.isEmpty() || permissionHandler.arePermissionsGranted(galleryPermissions)) {
                // No permissions needed (API 29-32) or permissions are granted, open Gallery
                Intent(this, Gallery::class.java).also {
                    this.startActivity(it)
                }
            } else {
                // Permissions not granted (API 33+), request them
                currentPermissionContext = PermissionRequestContext.GALLERY
                activityResultLauncher.launch(galleryPermissions)
            }
        }

        binding.btnSettings.setOnClickListener {
            Intent(this, Settings::class.java).also {
                this.startActivity(it)
            }
        }

        binding.btnRecordAndStop.setOnClickListener {
            val recordingPermissions = permissionHandler.getRecordingPermissions()
            if (permissionHandler.arePermissionsGranted(recordingPermissions)) {
                // Permissions are granted, proceed with recording
                camera.captureVideo()
            } else {
                // Permissions not granted, request them
                currentPermissionContext = PermissionRequestContext.RECORD
                activityResultLauncher.launch(recordingPermissions)
            }
        }
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(PermissionHandler.REQUIRED_PERMISSIONS)
    }

    private fun permissionCheck() {
        //Request camera permissions
        if (permissionHandler.allPermissionsGranted()) {
            //Toast.makeText(baseContext, "Permission request allowed", Toast.LENGTH_SHORT)
            camera.startCamera()
        } else {
            currentPermissionContext = PermissionRequestContext.STARTUP
            requestPermissions()
//            permissionCheck()
        }
    }
}