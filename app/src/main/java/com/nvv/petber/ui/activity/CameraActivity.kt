package com.nvv.petber.ui.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nvv.petber.R
import com.nvv.petber.databinding.ActivityCameraBinding
import com.nvv.petber.utils.ext.gone
import com.nvv.petber.utils.ext.toast
import com.nvv.petber.utils.ext.visible
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCameraBinding
    private lateinit var cameraExecutor: ExecutorService
    private var camera: Camera? = null

    // CameraX use cases
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null

    // State
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var flashMode = ImageCapture.FLASH_MODE_OFF
    private var isVideoMode = false
    private var isRecording = false
    private var isCameraReady = false
    private var isCapturing = false

    // Timer
    private var recordingStartTime = 0L
    private val timerHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            val elapsed = SystemClock.elapsedRealtime() - recordingStartTime
            val seconds = (elapsed / 1000).toInt()
            val minutes = seconds / 60
            binding.tvTimer.text =
                String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds % 60)
            timerHandler.postDelayed(this, 1000)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.all { it.value }
        if (allGranted) {
            startCamera()
        } else {
            val anyDeniedForever = results.entries.any { (permission, granted) ->
                !granted && !shouldShowRequestPermissionRationale(permission)
            }
            if (anyDeniedForever) {
                showPermanentlyDeniedDialog()
            } else {
                showPermissionDeniedView()
            }
        }
    }

    companion object {
        const val EXTRA_RESULT_URI = "extra_result_uri"
        const val EXTRA_IS_VIDEO = "extra_is_video"
        const val EXTRA_MODE = "extra_camera_mode" // "photo" or "video" or "all"
        const val MODE_PHOTO = "photo"
        const val MODE_VIDEO = "video"
        const val MODE_ALL = "all"
        private const val TAG = "CameraActivity"

        fun createIntent(
            context: android.content.Context,
            mode: String = MODE_ALL
        ) = Intent(context, CameraActivity::class.java).apply {
            putExtra(EXTRA_MODE, mode)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Check device has camera
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            toast(getString(R.string.no_camera_found))
            finish()
            return
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        val startMode = intent.getStringExtra(EXTRA_MODE) ?: MODE_ALL
        isVideoMode = startMode == MODE_VIDEO

        setupModeToggle(startMode)
        setupListeners()
        checkAndRequestPermissions()

    }

    private fun checkAndRequestPermissions() {
        val allPermissions = mutableListOf(Manifest.permission.CAMERA)
        allPermissions.add(Manifest.permission.RECORD_AUDIO)

        val missing = allPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            startCamera()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun showPermissionDeniedView() {
        binding.previewView.visible()
        binding.controlsContainer.gone()
        binding.permissionView.visible()
        binding.tvPermissionMessage.text = getString(R.string.camera_permission_required)
    }

    private fun showPermanentlyDeniedDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.permission_required))
            .setMessage(getString(R.string.camera_permission_permanently_denied))
            .setPositiveButton(getString(R.string.go_to_settings)) { _, _ ->
                openAppSettings()
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun openAppSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        })
    }

    // ──────────────────────────────────────────
    // Camera setup
    // ──────────────────────────────────────────

    private fun startCamera() {
        binding.permissionView.gone()
        binding.previewView.visible()
        binding.controlsContainer.visible()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            } catch (e: Exception) {
                Log.e(TAG, "Camera provider init failed: ${e.message}")
                toast(getString(R.string.camera_init_failed))
                finish()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val provider = cameraProvider ?: return

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        preview = Preview.Builder().build().also {
            it.surfaceProvider = binding.previewView.surfaceProvider
        }

        try {
            provider.unbindAll()

            camera = if (isVideoMode) {
                bindVideoUseCase(provider, cameraSelector)
            } else {
                bindPhotoUseCase(provider, cameraSelector)
            }
            isCameraReady = true
        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed: ${e.message}")
            isCameraReady = false
        }
    }

    private fun bindPhotoUseCase(
        provider: ProcessCameraProvider,
        selector: CameraSelector
    ): Camera {
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(flashMode)
            .build()
        return provider.bindToLifecycle(this, selector, preview!!, imageCapture!!)
    }

    private fun bindVideoUseCase(
        provider: ProcessCameraProvider,
        selector: CameraSelector
    ): Camera {
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
            .build()
        videoCapture = VideoCapture.withOutput(recorder)
        return provider.bindToLifecycle(this, selector, preview!!, videoCapture!!)
    }

    // ──────────────────────────────────────────
    // UI setup
    // ──────────────────────────────────────────

    private fun setupModeToggle(startMode: String) {
        when (startMode) {
            MODE_PHOTO -> {
                binding.modeToggle.gone()
                isVideoMode = false
            }

            MODE_VIDEO -> {
                binding.modeToggle.gone()
                isVideoMode = true
                updateShutterIcon()
            }

            else -> {
                binding.modeToggle.visible()
            }
        }
    }

    private fun setupListeners() {
        binding.btnClose.setOnClickListener { finish() }

        binding.btnGrantPermission.setOnClickListener {
            checkAndRequestPermissions()
        }

        binding.btnFlipCamera.setOnClickListener {
            if (!isCameraReady || isRecording) return@setOnClickListener

            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                CameraSelector.LENS_FACING_FRONT
            else
                CameraSelector.LENS_FACING_BACK

            flashMode = ImageCapture.FLASH_MODE_OFF
            updateFlashIcon()

            bindCameraUseCases()
        }

        binding.btnFlash.setOnClickListener {
            if (isVideoMode) {
                val isTorchOn = camera?.cameraInfo?.torchState?.value == TorchState.ON
                camera?.cameraControl?.enableTorch(!isTorchOn)
                binding.btnFlash.setImageResource(
                    if (isTorchOn) R.drawable.ic_flash_off else R.drawable.ic_flash_on
                )
            } else {
                flashMode = when (flashMode) {
                    ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                    ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                    else -> ImageCapture.FLASH_MODE_OFF
                }
                imageCapture?.flashMode = flashMode
                updateFlashIcon()
            }
        }

        binding.btnShutter.setOnClickListener {
            if (!isCameraReady) return@setOnClickListener
            if (isVideoMode) toggleVideoRecording()
            else takePhoto()
        }

        binding.btnModePhoto.setOnClickListener {
            if (isRecording) return@setOnClickListener
            if (isVideoMode) {
                isVideoMode = false
                updateModeUI()
                bindCameraUseCases()
            }
        }

        binding.btnModeVideo.setOnClickListener {
            if (isRecording) return@setOnClickListener
            if (!isVideoMode) {
                isVideoMode = true
                updateModeUI()
                bindCameraUseCases()
            }
        }
    }

    private fun updateModeUI() {
        updateShutterIcon()
        if (isVideoMode) {
            binding.btnModeVideo.setTextColor(getColor(android.R.color.white))
            binding.btnModeVideo.setTypeface(null, android.graphics.Typeface.BOLD)
            binding.btnModeVideo.setBackgroundColor(getColor(android.R.color.white))
            binding.btnModeVideo.setTextColor(getColor(android.R.color.black))
            binding.btnModePhoto.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            binding.btnModePhoto.setTextColor(getColor(android.R.color.white))
            binding.btnModePhoto.setTypeface(null, android.graphics.Typeface.NORMAL)
        } else {
            binding.btnModePhoto.setBackgroundColor(getColor(android.R.color.white))
            binding.btnModePhoto.setTextColor(getColor(android.R.color.black))
            binding.btnModePhoto.setTypeface(null, android.graphics.Typeface.BOLD)
            binding.btnModeVideo.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            binding.btnModeVideo.setTextColor(getColor(android.R.color.white))
            binding.btnModeVideo.setTypeface(null, android.graphics.Typeface.NORMAL)
        }
    }

    private fun updateShutterIcon() {
        binding.btnShutter.setImageResource(
            if (isVideoMode) R.drawable.ic_shutter_video
            else R.drawable.ic_shutter_photo
        )
    }

    private fun updateFlashIcon() {
        val icon = when (flashMode) {
            ImageCapture.FLASH_MODE_ON -> R.drawable.ic_flash_on
            ImageCapture.FLASH_MODE_AUTO -> R.drawable.ic_flash_auto
            else -> R.drawable.ic_flash_off
        }
        binding.btnFlash.setImageResource(icon)
    }

    // ──────────────────────────────────────────
    // Take photo
    // ──────────────────────────────────────────

    private fun takePhoto() {
        if (isCapturing) return
        isCapturing = true

        val capture = imageCapture ?: run {
            isCapturing = false
            return
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/PetBer")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    isCapturing = false
                    val uri = output.savedUri ?: return
                    returnResult(uri, isVideo = false)
                }

                override fun onError(exception: ImageCaptureException) {
                    isCapturing = false
                    Log.e(TAG, "Photo capture failed: ${exception.message}")
                    val msg = when (exception.imageCaptureError) {
                        ImageCapture.ERROR_CAMERA_CLOSED ->
                            getString(R.string.error_camera_closed)

                        else -> getString(R.string.error_capture_failed)
                    }
                    toast(msg)
                }
            }
        )
    }

    // ──────────────────────────────────────────
    // Video recording
    // ──────────────────────────────────────────

    private fun toggleVideoRecording() {
        if (isRecording) stopRecording() else startRecording()
    }

    private fun startRecording() {
        val capture = videoCapture ?: return

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "VID_${System.currentTimeMillis()}")
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/PetBer")
            }
        }

        val mediaStoreOutput = MediaStoreOutputOptions.Builder(
            contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()

        // Check RECORD_AUDIO permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            toast(getString(R.string.microphone_permission_required))
            return
        }

        activeRecording = capture.output
            .prepareRecording(this, mediaStoreOutput)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(this)) { event ->
                handleVideoRecordEvent(event)
            }

        isRecording = true
        updateRecordingUI(true)
        startTimer()
    }

    private fun stopRecording() {
        activeRecording?.stop()
        activeRecording = null
    }

    private fun handleVideoRecordEvent(event: VideoRecordEvent) {
        when (event) {
            is VideoRecordEvent.Start -> {
                // recording started
            }

            is VideoRecordEvent.Finalize -> {
                isRecording = false
                stopTimer()
                updateRecordingUI(false)

                if (event.hasError()) {
                    val msg = when (event.error) {
                        VideoRecordEvent.Finalize.ERROR_INSUFFICIENT_STORAGE ->
                            getString(R.string.error_insufficient_storage)

                        VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE ->
                            getString(R.string.error_camera_closed)

                        else -> getString(R.string.error_capture_failed)
                    }
                    Log.e(TAG, "Video recording error: ${event.error}")
                    toast(msg)
                } else {
                    val uri = event.outputResults.outputUri
                    returnResult(uri, isVideo = true)
                }
            }

            else -> {}
        }
    }

    private fun updateRecordingUI(recording: Boolean) {
        binding.btnShutter.setImageResource(
            if (recording) R.drawable.ic_shutter_stop
            else R.drawable.ic_shutter_video
        )
        binding.btnFlipCamera.isEnabled = !recording
        binding.btnModePhoto.isEnabled = !recording
        binding.btnModeVideo.isEnabled = !recording
        binding.tvTimer.visibility = if (recording) View.VISIBLE else View.GONE
    }

    // ──────────────────────────────────────────
    // Timer
    // ──────────────────────────────────────────

    private fun startTimer() {
        recordingStartTime = SystemClock.elapsedRealtime()
        timerHandler.post(timerRunnable)
    }

    @SuppressLint("SetTextI18n")
    private fun stopTimer() {
        timerHandler.removeCallbacks(timerRunnable)
        binding.tvTimer.text = "00:00"
    }

    // ──────────────────────────────────────────
    // Return result
    // ──────────────────────────────────────────

    private fun returnResult(uri: Uri, isVideo: Boolean) {
        val result = Intent().apply {
            putExtra(EXTRA_RESULT_URI, uri.toString())
            putExtra(EXTRA_IS_VIDEO, isVideo)
        }
        setResult(RESULT_OK, result)
        finish()
    }

    // ──────────────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────────────

    override fun onResume() {
        super.onResume()
        // Re-check permission khi user quay lại từ Settings
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED && !isCameraReady
        ) {
            startCamera()
        }
    }

    override fun onPause() {
        super.onPause()
        if (isRecording) {
            stopRecording()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        timerHandler.removeCallbacks(timerRunnable)
        cameraProvider?.unbindAll()
    }
}