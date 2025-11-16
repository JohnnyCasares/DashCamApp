package com.kasahirotech.dashcamapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.kasahirotech.dashcamapp.databinding.ActivityMainBinding
import com.kasahirotech.dashcamapp.screens.Gallery
import com.kasahirotech.dashcamapp.service.Camera
import com.kasahirotech.dashcamapp.service.DualCameraManager
import com.kasahirotech.dashcamapp.service.PermissionHandler
import com.kasahirotech.dashcamapp.service.PreferenceManager
import com.kasahirotech.dashcamapp.settings.Settings

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var camera: Camera
    private lateinit var dualCameraManager: DualCameraManager
    private lateinit var permissionHandler: PermissionHandler

    private var isDualCameraMode = false

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
        var permissionGranted = true
        permissions.entries.forEach {
            if (it.value == false)
                permissionGranted = false
        }
        
        if (permissionGranted) {
            when (currentPermissionContext) {
                PermissionRequestContext.RECORD -> {
                    if (isDualCameraMode) {
                        dualCameraManager.startDualRecording(PreferenceManager.isAudioRecordingEnabled(this))
                    } else {
                        camera.captureVideo()
                    }
                }
                PermissionRequestContext.GALLERY -> {
                    Intent(this, Gallery::class.java).also {
                        this.startActivity(it)
                    }
                }
                PermissionRequestContext.STARTUP -> {
                    initializeCameraMode()
                }
                PermissionRequestContext.NONE -> {}
            }
        } else {
            val errorMessage = when (currentPermissionContext) {
                PermissionRequestContext.RECORD -> "Camera and microphone permissions are required to record videos"
                PermissionRequestContext.GALLERY -> "Media access permission is required to view videos"
                PermissionRequestContext.STARTUP -> "Permission request denied"
                PermissionRequestContext.NONE -> "Permission request denied"
            }
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
        }
        
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
        dualCameraManager = DualCameraManager(
            activity = this,
            frontPreviewView = binding.viewFinderFront,
            backPreviewView = binding.viewFinderBack
        )
        setContentView(binding.root)

        this.permissionCheck()

        binding.imgBtnGallery.setOnClickListener {
            val galleryPermissions = permissionHandler.getGalleryPermissions()
            if (galleryPermissions.isEmpty() || permissionHandler.arePermissionsGranted(galleryPermissions)) {
                Intent(this, Gallery::class.java).also {
                    this.startActivity(it)
                }
            } else {
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
                if (isDualCameraMode) {
                    if (dualCameraManager.isRecording()) {
                        dualCameraManager.stopDualRecording()
                    } else {
                        dualCameraManager.startDualRecording(PreferenceManager.isAudioRecordingEnabled(this))
                    }
                } else {
                    camera.captureVideo()
                }
            } else {
                currentPermissionContext = PermissionRequestContext.RECORD
                activityResultLauncher.launch(recordingPermissions)
            }
        }
    }

    private fun initializeCameraMode() {
        isDualCameraMode = PreferenceManager.isDualCameraEnabled(this) && DualCameraManager.isDeviceCapable(this)
        
        if (isDualCameraMode) {
            binding.viewFinder.visibility = View.GONE
            binding.dualPreviewContainer.visibility = View.VISIBLE
            dualCameraManager.startDualCamera()
        } else {
            binding.viewFinder.visibility = View.VISIBLE
            binding.dualPreviewContainer.visibility = View.GONE
            camera.startCamera()
        }
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(PermissionHandler.REQUIRED_PERMISSIONS)
    }

    private fun permissionCheck() {
        if (permissionHandler.allPermissionsGranted()) {
            initializeCameraMode()
        } else {
            currentPermissionContext = PermissionRequestContext.STARTUP
            requestPermissions()
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (permissionHandler.allPermissionsGranted()) {
            initializeCameraMode()
        }
    }
}