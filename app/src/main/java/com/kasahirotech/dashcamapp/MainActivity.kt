package com.kasahirotech.dashcamapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.kasahirotech.dashcamapp.databinding.ActivityMainBinding
import com.kasahirotech.dashcamapp.service.Camera
import com.kasahirotech.dashcamapp.service.Storage
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
            if (it.key in Storage.REQUIRED_PERMISSIONS && it.value == false)
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

            context = this,
            binding = binding,
            lifecycleOwner = this,
            surfaceProvider = binding.viewFinder.surfaceProvider,
            contentResolver = contentResolver
        )
        setContentView(binding.root)

        //Request camera permissions
        if (Storage(this).allPermissionsGranted()) {
            //Toast.makeText(baseContext, "Permission request allowed", Toast.LENGTH_SHORT)
             camera.startCamera()
        } else {
            requestPermissions()
        }


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
        activityResultLauncher.launch(Storage.REQUIRED_PERMISSIONS)
    }
}