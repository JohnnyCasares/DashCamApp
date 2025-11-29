package com.kasahirotech.dashcamapp.screens

import android.os.Bundle
import android.util.Log
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kasahirotech.dashcamapp.databinding.ActivityCameraConfigBinding
import com.kasahirotech.dashcamapp.models.CameraInfo
import com.kasahirotech.dashcamapp.service.CameraEnumerator
import com.kasahirotech.dashcamapp.service.PreferenceManager
import com.kasahirotech.dashcamapp.service.ZoomController
import kotlinx.coroutines.launch

/**
 * Activity for configuring camera selection and zoom settings.
 * Provides a live preview with camera selection list and pinch-to-zoom functionality.
 */
class CameraConfigActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCameraConfigBinding
    private val cameraEnumerator = CameraEnumerator()
    private val zoomController = ZoomController()
    private var cameraList: List<CameraInfo> = emptyList()
    private var selectedCameraId: String = ""
    private var scaleGestureDetector: ScaleGestureDetector? = null
    private var zoomIndicatorHideRunnable: Runnable? = null
    private var cameraListAdapter: CameraListAdapter? = null
    
    companion object {
        private const val TAG = "CameraConfigActivity"
        private const val ZOOM_INDICATOR_HIDE_DELAY = 3000L
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set up action bar
        supportActionBar?.title = getString(com.kasahirotech.dashcamapp.R.string.camera_config_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Set content descriptions for accessibility
        binding.previewView.contentDescription = getString(com.kasahirotech.dashcamapp.R.string.camera_preview_description)
        binding.zoomIndicator.contentDescription = getString(com.kasahirotech.dashcamapp.R.string.zoom_indicator_description)
        
        // Check if recording is in progress (prevent camera config during recording)
        if (isRecordingInProgress()) {
            Toast.makeText(
                this,
                getString(com.kasahirotech.dashcamapp.R.string.camera_config_recording_in_progress),
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }
        
        // Initialize gesture detector for pinch-to-zoom
        setupGestureDetector()
        
        // Load camera list and initialize UI
        loadCameraList()
        
        // Set up button listeners
        binding.btnSave.setOnClickListener {
            saveConfiguration()
        }
        
        binding.btnCancel.setOnClickListener {
            finish()
        }
    }
    
    /**
     * Checks if recording is currently in progress.
     * Camera configuration should not be changed during recording.
     */
    private fun isRecordingInProgress(): Boolean {
        // Check if RecordingService is active
        return try {
            com.kasahirotech.dashcamapp.service.RecordingService.isRecording()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking recording status", e)
            false
        }
    }
    
    /**
     * Sets up the scale gesture detector for pinch-to-zoom.
     */
    private fun setupGestureDetector() {
        scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scaleFactor = detector.scaleFactor
                zoomController.handlePinchGesture(scaleFactor)
                updateZoomIndicator()
                return true
            }
        })
        
        binding.previewView.setOnTouchListener { _, event ->
            scaleGestureDetector?.onTouchEvent(event)
            true
        }
    }
    
    /**
     * Loads the list of available cameras.
     */
    private fun loadCameraList() {
        lifecycleScope.launch {
            try {
                cameraList = cameraEnumerator.getCameraList(this@CameraConfigActivity)
                
                if (cameraList.isEmpty()) {
                    Toast.makeText(
                        this@CameraConfigActivity,
                        getString(com.kasahirotech.dashcamapp.R.string.camera_config_no_cameras),
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                    return@launch
                }
                
                // Load saved camera ID or use default
                selectedCameraId = PreferenceManager.getSelectedCameraId(this@CameraConfigActivity)
                if (selectedCameraId.isEmpty()) {
                    selectedCameraId = cameraEnumerator.getDefaultCameraId()
                }
                
                // Verify selected camera exists in list
                if (cameraList.none { it.id == selectedCameraId }) {
                    selectedCameraId = cameraList.first().id
                }
                
                // Set up RecyclerView (will be implemented in next task)
                setupCameraList()
                
                // Start camera preview
                startCameraPreview(selectedCameraId)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading camera list", e)
                Toast.makeText(
                    this@CameraConfigActivity,
                    getString(com.kasahirotech.dashcamapp.R.string.camera_config_error_loading),
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }
    
    /**
     * Sets up the camera list RecyclerView with adapter.
     */
    private fun setupCameraList() {
        binding.cameraListRecyclerView.layoutManager = LinearLayoutManager(this)
        
        cameraListAdapter = CameraListAdapter(
            cameras = cameraList,
            selectedCameraId = selectedCameraId,
            onCameraSelected = { camera ->
                onCameraSelected(camera)
            }
        )
        
        binding.cameraListRecyclerView.adapter = cameraListAdapter
    }
    
    /**
     * Handles camera selection from the list.
     */
    private fun onCameraSelected(camera: CameraInfo) {
        if (camera.id == selectedCameraId) {
            return // Already selected
        }
        
        // Update selected camera ID
        selectedCameraId = camera.id
        
        // Update adapter
        cameraListAdapter?.updateSelectedCamera(selectedCameraId)
        
        // Show message if camera doesn't support zoom
        if (!camera.supportsZoom) {
            Toast.makeText(
                this,
                getString(com.kasahirotech.dashcamapp.R.string.camera_config_no_zoom_support),
                Toast.LENGTH_SHORT
            ).show()
        }
        
        // Restart camera preview with new camera
        startCameraPreview(selectedCameraId)
        
        Log.d(TAG, "Camera selected: ${camera.displayName} (${camera.id})")
    }
    
    /**
     * Starts the camera preview for the specified camera ID.
     */
    private fun startCameraPreview(cameraId: String) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                
                // Build preview
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }
                
                // Build camera selector
                val cameraSelector = buildCameraSelectorForId(cameraId)
                
                // Unbind all use cases
                cameraProvider.unbindAll()
                
                // Bind camera to lifecycle
                val camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview
                )
                
                // Initialize zoom controller
                zoomController.setCamera(camera.cameraControl, camera.cameraInfo)
                
                // Load and apply saved zoom ratio
                val savedZoom = PreferenceManager.getZoomRatio(this, cameraId)
                if (savedZoom != 1.0f) {
                    zoomController.setZoomRatio(savedZoom)
                }
                
                // Update zoom indicator
                updateZoomIndicator()
                
                Log.d(TAG, "Camera preview started for ID: $cameraId")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error starting camera preview", e)
                Toast.makeText(
                    this,
                    getString(com.kasahirotech.dashcamapp.R.string.camera_config_error_loading),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }
    
    /**
     * Builds a CameraSelector for a specific camera ID.
     */
    private fun buildCameraSelectorForId(cameraId: String): CameraSelector {
        return try {
            if (cameraId == "0") {
                CameraSelector.DEFAULT_BACK_CAMERA
            } else {
                CameraSelector.Builder()
                    .addCameraFilter { cameraInfos ->
                        cameraInfos.filterIndexed { index, _ -> index.toString() == cameraId }
                    }
                    .build()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error building camera selector", e)
            CameraSelector.DEFAULT_BACK_CAMERA
        }
    }
    
    /**
     * Updates the zoom indicator with the current zoom ratio.
     */
    private fun updateZoomIndicator() {
        val zoomRatio = zoomController.getZoomRatio()
        binding.zoomIndicator.text = String.format("%.1fx", zoomRatio)
        binding.zoomIndicator.visibility = View.VISIBLE
        
        // Cancel previous hide runnable
        zoomIndicatorHideRunnable?.let {
            binding.zoomIndicator.removeCallbacks(it)
        }
        
        // Schedule hide after delay
        zoomIndicatorHideRunnable = Runnable {
            binding.zoomIndicator.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    binding.zoomIndicator.visibility = View.GONE
                    binding.zoomIndicator.alpha = 1f
                }
                .start()
        }
        
        binding.zoomIndicator.postDelayed(zoomIndicatorHideRunnable!!, ZOOM_INDICATOR_HIDE_DELAY)
    }
    
    /**
     * Saves the camera and zoom configuration.
     */
    private fun saveConfiguration() {
        try {
            // Save selected camera ID
            PreferenceManager.setSelectedCameraId(this, selectedCameraId)
            
            // Save zoom ratio for selected camera
            val currentZoom = zoomController.getZoomRatio()
            PreferenceManager.setZoomRatio(this, selectedCameraId, currentZoom)
            
            Toast.makeText(this, getString(com.kasahirotech.dashcamapp.R.string.camera_config_saved), Toast.LENGTH_SHORT).show()
            finish()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error saving configuration", e)
            Toast.makeText(this, getString(com.kasahirotech.dashcamapp.R.string.camera_config_error), Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
