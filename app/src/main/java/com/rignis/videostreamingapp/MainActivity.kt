package com.rignis.videostreamingapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileDescriptorOutputOptions
import androidx.camera.video.OutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import com.rignis.videostreamingapp.databinding.ActivityMainBinding
import com.rignis.videostreamingapp.ui.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

typealias LumaListener = (luma: Double) -> Unit

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding

    private val viewModel by viewModels<MainViewModel>()

    private var imageCapture: ImageCapture? = null

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    private lateinit var cameraExecutor: ExecutorService

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    )
    { permissions ->
        // Handle Permission granted/rejected
        var permissionGranted = true
        permissions.entries.forEach {
            if (it.key in REQUIRED_PERMISSIONS && !it.value)
                permissionGranted = false
        }
        if (!permissionGranted) {
            Toast.makeText(
                baseContext,
                "Permission request denied",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            startCamera()
        }
    }

    private val activityResultFolderSelectLauncher = registerForActivityResult(
        object : ActivityResultContracts.OpenDocumentTree() {
            override fun createIntent(context: Context, input: Uri?): Intent {
                val result = super.createIntent(context, input)
                result.flags =
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                return result
            }
        }
    ) { uri: Uri? ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.onStorageFolderSelected(uri.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        // Set up the listeners for take photo and video capture buttons
        viewBinding.imageCaptureButton.setOnClickListener { onCapturePhotoClick() }
        viewBinding.videoCaptureButton.setOnClickListener { captureVideo() }

        cameraExecutor = Executors.newSingleThreadExecutor()
        observeViewModel()
    }

    private fun createFileName(): String {
        val df = SimpleDateFormat.getInstance()
        return df.format(Date())
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.state.collectLatest {
                Log.i("amittest", "New state = $it")
            }
        }
    }

    private fun captureAndSaveImage(outputStream: OutputStream) {
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(outputStream)
            .build()
        imageCapture?.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun
                        onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            }
        )
    }

    private fun recordAndSaveVideo(outputStream: ParcelFileDescriptor) {
        recording = videoCapture!!.output.prepareRecording(
            this,
            FileDescriptorOutputOptions.Builder(outputStream).build()
        ).apply {
            if (PermissionChecker.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.RECORD_AUDIO
                ) == PermissionChecker.PERMISSION_GRANTED
            ) {
                withAudioEnabled()
            }
        }.start(ContextCompat.getMainExecutor(this@MainActivity)) { recordEvent ->
            when (recordEvent) {
                is VideoRecordEvent.Resume -> {

                }

                is VideoRecordEvent.Status -> {

                }

                is VideoRecordEvent.Pause -> {

                }

                is VideoRecordEvent.Start -> {
                    viewBinding.videoCaptureButton.apply {
                        text = getString(R.string.stop_capture)
                        isEnabled = true
                    }
                }

                is VideoRecordEvent.Finalize -> {
                    if (!recordEvent.hasError()) {
                        val msg = "Video capture succeeded: " +
                                "${recordEvent.outputResults.outputUri}"
                        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT)
                            .show()
                        Log.d(TAG, msg)
                    } else {
                        recording?.close()
                        recording = null
                        Log.e(
                            TAG, "Video capture ends with error: " +
                                    "${recordEvent.error}"
                        )
                    }
                    viewBinding.videoCaptureButton.apply {
                        text = getString(R.string.start_capture)
                        isEnabled = true
                    }
                }
            }
        }
    }


    private fun onCapturePhotoClick() {
        lifecycleScope.launch {
            val job = launch(start = CoroutineStart.LAZY) {
                val selectedDir = viewModel.getUserSelectedDir()
                if (selectedDir.isNullOrEmpty()) {
                    selectFolder()
                } else {
                    val doc = DocumentFile.fromTreeUri(this@MainActivity, Uri.parse(selectedDir))
                    val file = doc?.createFile("image/jpeg", "${createFileName()}.jpeg")
                    val writeStream = contentResolver.openOutputStream(file?.uri!!)
                    captureAndSaveImage(writeStream!!)
                }
            }
            withResumed {
                job.start()
            }
        }
    }

    private fun captureVideo() {
        val videoCapture = this.videoCapture ?: return
        viewBinding.videoCaptureButton.isEnabled = false
        recording?.let {
            it.stop()
            recording = null
            return
        }
        lifecycleScope.launch {
            val selectedDir = viewModel.getUserSelectedDir()
            if (selectedDir.isNullOrEmpty()) {
                selectFolder()
            } else {
                val doc = DocumentFile.fromTreeUri(this@MainActivity, Uri.parse(selectedDir))
                val file = doc?.createFile("video/mp4", "${createFileName()}.mp4")
                val writeStream = contentResolver.openFileDescriptor(file?.uri!!, "w")
                recordAndSaveVideo(writeStream!!)
            }
        }

    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }
            imageCapture = ImageCapture.Builder().build()

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, videoCapture
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))

    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun selectFolder() {
        activityResultFolderSelectLauncher.launch(null)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}