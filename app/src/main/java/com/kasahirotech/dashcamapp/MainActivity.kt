package com.kasahirotech.dashcamapp

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.kasahirotech.dashcamapp.databinding.ActivityMainBinding
import com.kasahirotech.dashcamapp.screens.Gallery
import com.kasahirotech.dashcamapp.service.Camera
import com.kasahirotech.dashcamapp.service.PermissionHandler
import com.kasahirotech.dashcamapp.settings.Settings

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var camera: Camera

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        //Handle permissions granted/rejected
        var permissionGranted = true
        permissions.entries.forEach {
            if (it.key in PermissionHandler.REQUIRED_PERMISSIONS && it.value == false)
                permissionGranted = false
        }
        if (!permissionGranted) {
            Toast.makeText(this, "Permission request denied", Toast.LENGTH_SHORT)
        } else {
            Toast.makeText(this, "Permission request allowed", Toast.LENGTH_SHORT)
            //startCamera()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        camera = Camera(
            activity = this,
            binding = binding,
            surfaceProvider = binding.viewFinder.surfaceProvider,
        )
        setContentView(binding.root)

        this.permissionCheck()


//        binding.imgBtnGallery.setOnClickListener {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
//            }
//
//            Intent(this, Gallery::class.java).also {
//                this.startActivity(it)
//            }
//        }

        binding.btnSettings.setOnClickListener {
            Intent(this, Settings::class.java).also {
                this.startActivity(it)
            }
        }

        binding.btnRecordAndStop.setOnClickListener {
            camera.captureVideo()
        }
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(PermissionHandler.REQUIRED_PERMISSIONS)
    }

    private fun permissionCheck() {
        //Request camera permissions
        if (PermissionHandler(this).allPermissionsGranted()) {
            //Toast.makeText(baseContext, "Permission request allowed", Toast.LENGTH_SHORT)
            camera.startCamera()
        } else {
            requestPermissions()
            permissionCheck()
        }
    }
}