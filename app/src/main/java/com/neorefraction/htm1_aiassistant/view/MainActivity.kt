package com.neorefraction.htm1_aiassistant.view

// Camera required imports
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.neorefraction.htm1_aiassistant.R
import com.neorefraction.htm1_aiassistant.viewmodel.ViewModel

/* Constants */

// Dictation constants
const val DICTATION_REQUEST_CODE = 100
const val DICTATION_ACTION = "com.realwear.keyboard.intent.action.DICTATION"
const val DICTATION_PACKAGE = "com.realwear.wearhf.intent.extra.SOURCE_PACKAGE"

// Text to Speech Intents
const val ACTION_TTS = "com.realwear.wearhf.intent.action.TTS"
const val EXTRA_TEXT = "text_to_speak"
const val EXTRA_ID = "tts_id"
const val EXTRA_PAUSE = "pause_speech_recognizer"
const val TTS_REQUEST_CODE = 34

// Speech
const val SPEECH_ACTION: String = "com.realwear.wearhf.intent.action.SPEECH_EVENT"

// Permissions
const val PERMISSIONS_REQUEST_CODE = 400

class MainActivity : AppCompatActivity() {

    // UI Components
    private lateinit var textureView: TextureView

    private val viewModel: ViewModel by viewModels()

    // Camera
    private lateinit var cameraManager: CameraManager
    private var _camera: CameraDevice? = null

    private val speechReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val command = intent.getStringExtra("command")
            when (command) {
                "Hola Gepeto" -> {
                    startDictation()
                }
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set activity layout
        this.setContentView(R.layout.activity_main)
        this.setupFullScreenUI()

        // Request application permissions
        this.requestPermissions(
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
            PERMISSIONS_REQUEST_CODE)

        // Get the camera Service to manage camera
        this.cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager

        // Setup texture view listener
        this.setupTextureSurfaceListener()

        // Setup camera ViewModels Observers
        this.observeViewModels()

        // Adds a callback for user commands over RealWear device
        val intentFilter = IntentFilter(SPEECH_ACTION)
        registerReceiver(speechReceiver, intentFilter)
    }

    /**
     * Setup the camera ViewModel elements to be observed
     */
    private fun observeViewModels() {
        this.viewModel.isCameraReady.observe(this) { isCameraReady ->
            if (isCameraReady)
                openCamera()
            else
                closeCamera()
        }
    }

    /**
     * Setup Layout to support full screen
     */
    private fun setupFullScreenUI() {

        // enables full screen size
        enableEdgeToEdge()

        // Applies auto hiding for screen Insets
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // Applies padding when Insets are shown on the window
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    /**
     * Setup texture view listeners to react on TextureView lifecycle
     */
    private fun setupTextureSurfaceListener() {
        this.textureView = findViewById(R.id.tvCamera)
        this.textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                viewModel.setSurfaceAvailability(true)
            }
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                viewModel.setSurfaceAvailability(false)
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
        }
    }

    /**
     * Links a hardware camera to the application
     */
    private fun openCamera() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            return

        val cameraId = this.cameraManager.cameraIdList.firstOrNull() ?: return

        this.cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                _camera = camera
                startPreview()
            }

            override fun onDisconnected(camera: CameraDevice) {
                camera.close()
            }

            override fun onError(camera: CameraDevice, error: Int) {
                camera.close()
                _camera = null
            }
        }, null)
    }

    /**
     * Close camera
     */
    private fun closeCamera() {
        this._camera?.close()
        this._camera = null
    }

    /**
     * Start the camera image preview
     */
    private fun startPreview() {
        val surfaceTexture = textureView.surfaceTexture ?: return
        surfaceTexture.setDefaultBufferSize(1920, 1080)
        val surface = Surface(surfaceTexture)

        val previewRequestBuilder = this._camera?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)?.apply {
            addTarget(surface)
        }

        _camera?.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                try {
                    session.setRepeatingRequest(previewRequestBuilder!!.build(), null, null)
                } catch (e: CameraAccessException) {
                    e.printStackTrace()
                }
            }
            override fun onConfigureFailed(session: CameraCaptureSession) {
                Toast.makeText(applicationContext, "Configuración fallida", Toast.LENGTH_SHORT).show()
            }
        }, null)
    }

    /**
     * Uses RealWear Dictation service to transcript user input
     */
    private fun startDictation() {
        val intent = Intent(DICTATION_ACTION)
        intent.putExtra(DICTATION_PACKAGE, packageName)
        startActivityForResult(intent, DICTATION_REQUEST_CODE)
    }

    /**
     * Callback to catch RealWear services results
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Dictation service
        if (requestCode == DICTATION_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val texto = data?.getStringExtra("result") ?: ""
            Log.i("JOHNNY", "Transcripción: $texto")
            // TODO: Send query to AI service
        }
    }

    /**
     * Set permissions for ViewModel
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE && grantResults.size >= 2) {
            viewModel.setCameraPermission(grantResults[0] == PackageManager.PERMISSION_GRANTED)
        }
    }

    override fun onPause() {
        super.onPause()
        // Make sure we release the microphone on pause
        //this.microphoneViewModel.releaseMicrophone()
    }
}