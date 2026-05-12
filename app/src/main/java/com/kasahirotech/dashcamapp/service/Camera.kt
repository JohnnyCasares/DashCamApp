package com.kasahirotech.dashcamapp.service

import android.Manifest
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.kasahirotech.dashcamapp.MainActivity
import com.kasahirotech.dashcamapp.R
import com.kasahirotech.dashcamapp.databinding.ActivityMainBinding
import com.kasahirotech.dashcamapp.interfaces.CameraService
import java.io.File

/**
 * CameraX-based camera handler.
 *
 * Responsibilities:
 * - Render the live preview via the supplied [surfaceProvider]
 * - Record video to an explicit [File] (segment-based, for session management)
 * - Fire [onSegmentFinalized] when a recording segment is saved so the
 *   caller (MainActivity) can hand the file to [SessionManager]
 *
 * This class intentionally has NO knowledge of Camera2 / RecordingService.
 * All hand-off orchestration lives in MainActivity.
 */
class Camera(
    private val activity: AppCompatActivity,
    private val binding: ActivityMainBinding,
    private val surfaceProvider: Preview.SurfaceProvider,
) : CameraService {

    // ── CameraX internals ────────────────────────────────────────────────────
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var cameraStarted: Boolean = false

    // ── Dual-camera (unchanged from original) ────────────────────────────────
    private var dualCameraManager: DualCameraManager? = null
    private var isDualModeEnabled: Boolean = false

    // ── Camera selection / zoom ──────────────────────────────────────────────
    private var currentCameraId: String = "0"
    private var currentCameraInfo: com.kasahirotech.dashcamapp.models.CameraInfo? = null
    private val zoomController: ZoomController = ZoomController()
    private var cameraControl: androidx.camera.core.CameraControl? = null

    // ── Segment callback ─────────────────────────────────────────────────────
    /**
     * Called on the main thread when the current segment has been fully
     * written to disk. The [File] is guaranteed to exist with length > 0
     * on success; null is delivered on error.
     */
    var onSegmentFinalized: ((file: File?) -> Unit)? = null

    // ── CameraService interface ──────────────────────────────────────────────

    override fun startCamera() {
        val saved = PreferenceManager.getSelectedCameraId(activity)
        currentCameraId = if (saved.isNotEmpty()) saved else "0"
        startCameraWithId(currentCameraId)
    }

    /**
     * Legacy single-tap record / stop — kept for interface compliance.
     * In the hybrid session flow, use [startRecordingSegment] / [stopRecordingSegment] instead.
     */
    override fun captureVideo() {
        if (isDualModeEnabled && dualCameraManager != null) {
            captureDualVideo()
            return
        }
        if (recording != null) {
            stopRecordingSegment()
        } else {
            Log.w(TAG, "captureVideo() called but no output file specified — use startRecordingSegment()")
        }
    }

    override fun takePhoto() { /* reserved */ }

    override fun isCameraStarted(): Boolean = cameraStarted

    // ── Segment recording API ────────────────────────────────────────────────

    /**
     * Start recording a CameraX segment to [outputFile].
     * The preview remains visible during recording.
     *
     * @param outputFile Destination MP4 file (must be inside the session dir).
     * @param audioEnabled Whether to capture microphone audio.
     */
    fun startRecordingSegment(outputFile: File, audioEnabled: Boolean) {
        val vc = videoCapture ?: run {
            Log.e(TAG, "startRecordingSegment: videoCapture is null — was startCamera() called?")
            onSegmentFinalized?.invoke(null)
            return
        }
        if (recording != null) {
            Log.w(TAG, "startRecordingSegment: already recording, ignoring")
            return
        }

        val fileOutputOptions = FileOutputOptions.Builder(outputFile).build()

        recording = vc.output
            .prepareRecording(activity, fileOutputOptions)
            .apply {
                if (audioEnabled &&
                    PermissionChecker.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
                    == PermissionChecker.PERMISSION_GRANTED
                ) {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(activity)) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        Log.d(TAG, "CameraX segment started: ${outputFile.name}")
                        animateButtonToStop()
                        if (activity is MainActivity) activity.startTripLoggingIfEnabled()
                    }

                    is VideoRecordEvent.Finalize -> {
                        val ok = !event.hasError()
                        if (ok) {
                            Log.d(TAG, "CameraX segment finalized: ${outputFile.absolutePath} (${outputFile.length()} bytes)")
                            onSegmentFinalized?.invoke(outputFile)
                        } else {
                            Log.e(TAG, "CameraX segment error ${event.error}: ${event.cause?.message}")
                            onSegmentFinalized?.invoke(null)
                        }
                        // Do NOT reset button here — MainActivity controls the global stop state
                    }

                    else -> Unit
                }
            }
    }

    /**
     * Stops the current CameraX segment.
     * [onSegmentFinalized] will be called asynchronously once the file is flushed.
     */
    fun stopRecordingSegment() {
        val cur = recording
        if (cur == null) {
            Log.w(TAG, "stopRecordingSegment: nothing to stop")
            return
        }
        Log.d(TAG, "Stopping CameraX segment…")
        cur.stop()
        recording = null
    }

    /** @return true if a CameraX segment is currently being recorded */
    fun isRecordingSegment(): Boolean = recording != null

    // ── Camera mode initialization ───────────────────────────────────────────

    fun initializeCameraMode() {
        isDualModeEnabled = PreferenceManager.isDualCameraEnabled(activity) &&
                DualCameraManager.isDeviceCapable(activity)
        if (isDualModeEnabled) setupDualCameraMode() else setupSingleCameraMode()
    }

    private fun setupDualCameraMode() {
        try {
            val dualContainer = binding.root.findViewById<View>(R.id.dualPreviewContainer)
            val frontPreview = binding.root.findViewById<androidx.camera.view.PreviewView>(R.id.viewFinderFront)
            val backPreview = binding.root.findViewById<androidx.camera.view.PreviewView>(R.id.viewFinderBack)

            binding.viewFinder.visibility = View.GONE
            dualContainer.visibility = View.VISIBLE

            dualCameraManager = DualCameraManager(
                activity = activity,
                frontPreviewView = frontPreview,
                backPreviewView = backPreview
            )

            val success = dualCameraManager?.startDualCamera() ?: false
            if (!success) {
                Log.w(TAG, "Dual camera init failed, falling back to single")
                setupSingleCameraMode()
            } else {
                cameraStarted = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during dual camera setup", e)
            setupSingleCameraMode()
        }
    }

    private fun setupSingleCameraMode() {
        val dualContainer = binding.root.findViewById<View>(R.id.dualPreviewContainer)
        binding.viewFinder.visibility = View.VISIBLE
        dualContainer.visibility = View.GONE
        dualCameraManager?.stopDualCamera()
        dualCameraManager = null
        isDualModeEnabled = false
        startCamera()
    }

    // ── Dual-camera recording (unchanged logic) ──────────────────────────────

    private fun captureDualVideo() {
        val manager = dualCameraManager ?: return
        if (manager.isRecording()) {
            manager.stopDualRecording()
            animateButtonToRecord()
        } else {
            val audioEnabled = PreferenceManager.isAudioRecordingEnabled(activity)
            if (manager.startDualRecording(audioEnabled)) {
                animateButtonToStop()
            }
        }
    }

    // ── Camera selection ─────────────────────────────────────────────────────

    override fun switchCamera(cameraId: String): Boolean {
        return try {
            currentCameraId = cameraId
            PreferenceManager.setSelectedCameraId(activity, cameraId)
            startCameraWithId(cameraId)
            Log.d(TAG, "Switched to camera: $cameraId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error switching camera", e)
            false
        }
    }

    override fun getCurrentCameraId(): String = currentCameraId

    override fun getCameraInfo(): com.kasahirotech.dashcamapp.models.CameraInfo? = currentCameraInfo

    // ── Zoom ─────────────────────────────────────────────────────────────────

    override fun setZoomRatio(ratio: Float): Boolean {
        val success = zoomController.setZoomRatio(ratio)
        if (success) PreferenceManager.setZoomRatio(activity, currentCameraId, ratio)
        return success
    }

    override fun getZoomRatio(): Float = zoomController.getZoomRatio()

    override fun getZoomRange(): Pair<Float, Float> =
        Pair(zoomController.getMinZoomRatio(), zoomController.getMaxZoomRatio())

    // ── Internal camera startup ──────────────────────────────────────────────

    private fun startCameraWithId(cameraId: String) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)
        cameraProviderFuture.addListener({
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = surfaceProvider
            }
            imageCapture = ImageCapture.Builder().build()

            val selectedQuality = PreferenceManager.getVideoQuality(activity)
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(selectedQuality))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            val cameraSelector = buildCameraSelectorForId(cameraId)

            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    activity, cameraSelector, preview, videoCapture
                )

                cameraControl = camera.cameraControl
                zoomController.setCamera(camera.cameraControl, camera.cameraInfo)

                val savedZoom = PreferenceManager.getZoomRatio(activity, cameraId)
                if (savedZoom != 1.0f) zoomController.setZoomRatio(savedZoom)

                cameraStarted = true
                Log.d(TAG, "CameraX started: id=$cameraId zoom=$savedZoom")
            } catch (exc: Exception) {
                Log.e(TAG, "Failed to start CameraX for id=$cameraId", exc)
                cameraStarted = false
                if (cameraId != "0") {
                    Log.w(TAG, "Falling back to default camera")
                    currentCameraId = "0"
                    PreferenceManager.setSelectedCameraId(activity, "0")
                    startCameraWithId("0")
                }
            }
        }, ContextCompat.getMainExecutor(activity))
    }

    private fun buildCameraSelectorForId(cameraId: String): CameraSelector {
        return try {
            if (cameraId == "0") {
                CameraSelector.DEFAULT_BACK_CAMERA
            } else {
                CameraSelector.Builder()
                    .addCameraFilter { infos -> infos.filterIndexed { i, _ -> i.toString() == cameraId } }
                    .build()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error building camera selector for id=$cameraId", e)
            CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    // ── Button animation helpers (called from camera thread, on main executor) ─

    private fun animateButtonToStop() {
        val anim = AnimationUtils.loadAnimation(activity, R.anim.circle_to_square)
        anim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(a: Animation?) {}
            override fun onAnimationRepeat(a: Animation?) {}
            override fun onAnimationEnd(a: Animation?) {
                binding.btnRecordAndStop.setBackgroundResource(R.drawable.ic_stop_recording)
            }
        })
        binding.btnRecordAndStop.startAnimation(anim)
    }

    private fun animateButtonToRecord() {
        val anim = AnimationUtils.loadAnimation(activity, R.anim.square_to_circle)
        anim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(a: Animation?) {}
            override fun onAnimationRepeat(a: Animation?) {}
            override fun onAnimationEnd(a: Animation?) {
                binding.btnRecordAndStop.setBackgroundResource(R.drawable.bg_record_button)
            }
        })
        binding.btnRecordAndStop.startAnimation(anim)
    }

    companion object {
        private const val TAG = "Camera"
    }
}