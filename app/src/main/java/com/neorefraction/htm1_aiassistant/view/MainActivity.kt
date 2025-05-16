package com.neorefraction.htm1_aiassistant.view

// Camera required imports
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
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
import com.neorefraction.htm1_aiassistant.viewmodel.MainViewModel

private const val ACTION_TTS = "com.realwear.wearhf.intent.action.TTS"

private const val EXTRA_TEXT = "text_to_speak"
private const val EXTRA_ID = "tts_id"
private const val EXTRA_PAUSE = "pause_speech_recognizer"

private const val TTS_REQUEST_CODE = 34

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var textureView: TextureView

    private val speechReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val command = intent.getStringExtra("command")
            when (command) {
                "GEPETO" -> {
                    Log.i("JOHNNY", "En que puedo ayudarte?")
                    textToSpeech("En que puedo ayudarte?")
                }
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel.requestCameraService(::getSystemService)
        viewModel.requestCameraPermissions(this)

        setupFullScreenUI()
        setupCameraSurfaceListener()
        observeViewModel()

        val intentFilter = IntentFilter("com.realwear.wearhf.intent.action.SPEECH_EVENT")
        registerReceiver(speechReceiver, intentFilter)
    }

    private fun observeViewModel() {
        viewModel.isAppReady.observe(this) { isReady ->
            if (isReady) viewModel.openCamera(this)
        }

        viewModel.camera.observe(this) { camera ->
            camera?.let { startPreview(it) }
        }
    }

    private fun setupFullScreenUI() {
        enableEdgeToEdge()
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupCameraSurfaceListener() {
        textureView = findViewById(R.id.tvCamera)
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                viewModel.setSurfaceAvailability(true)
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                viewModel.setSurfaceAvailability(false)
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
    }

    private fun startPreview(cameraDevice: CameraDevice) {
        val surfaceTexture = textureView.surfaceTexture ?: return
        surfaceTexture.setDefaultBufferSize(1920, 1080)
        val surface = Surface(surfaceTexture)

        val previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
            addTarget(surface)
        }

        cameraDevice.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                try {
                    session.setRepeatingRequest(previewRequestBuilder.build(), null, null)
                } catch (e: CameraAccessException) {
                    e.printStackTrace()
                }
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                Toast.makeText(applicationContext, "Configuración fallida", Toast.LENGTH_SHORT).show()
            }
        }, null)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty()) {
            viewModel.setPermission(grantResults[0])
        }
    }

    private fun textToSpeech(text: String) {

        val intent = Intent(ACTION_TTS)
        intent.putExtra(EXTRA_TEXT, text)
        intent.putExtra(EXTRA_ID, TTS_REQUEST_CODE)
        intent.putExtra(EXTRA_PAUSE, false)
        sendBroadcast(intent)
    }
}