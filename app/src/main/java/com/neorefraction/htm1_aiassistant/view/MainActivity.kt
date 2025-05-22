package com.neorefraction.htm1_aiassistant.view


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.TextureView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.neorefraction.htm1_aiassistant.R
import com.neorefraction.htm1_aiassistant.ai.AiClient
import com.neorefraction.htm1_aiassistant.ai.AiParser
import com.neorefraction.htm1_aiassistant.camera.CameraController
import com.neorefraction.htm1_aiassistant.dictation.DictationManager
import com.neorefraction.htm1_aiassistant.tts.TextToSpeechManager
import com.neorefraction.htm1_aiassistant.viewmodel.ViewModel
import kotlinx.coroutines.launch



// Speech
const val SPEECH_ACTION: String = "com.realwear.wearhf.intent.action.SPEECH_EVENT"
// Permissions
const val PERMISSIONS_REQUEST_CODE = 400

class MainActivity : AppCompatActivity() {

    private lateinit var textureView: TextureView
    private val viewModel: ViewModel by viewModels()

    private lateinit var cameraController: CameraController
    private val aiClient = AiClient()
    private val tts = TextToSpeechManager()
    private val dictation = DictationManager()

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupFullScreenUI()

        requestPermissions(
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
            PERMISSIONS_REQUEST_CODE
        )

        textureView = findViewById(R.id.tvCamera)
        cameraController = CameraController(this, textureView)

        setupTextureSurfaceListener()
        observeViewModels()

        registerReceiver(dictation.receiver(::onCommandReceived), IntentFilter(SPEECH_ACTION))
    }

    private fun onCommandReceived(command: String?) {
        if (command == "Test") {
            dictation.start(this)
            this.viewModel.setPhoto(cameraController.launchCameraRawPhoto())
        }
    }

    private fun observeViewModels() {
        viewModel.isCameraReady.observe(this) { ready ->
            if (ready) cameraController.open() else cameraController.close()
        }

        viewModel.bothResultsReady.observe(this) { (text, image) ->
            lifecycleScope.launch {
                try {
                    val response = aiClient.fetchResponse(text, "data:image/jpeg;base64,$image")
                    Log.i("IMAGEN JOHNNY", image)
                    Log.e("MENSAJE JOHNNY", response.toString())
                    val parsed = AiParser.extractContent(response)
                    Log.e("MENSAJE JOHNNY", parsed.toString())
                    tts.speak(this@MainActivity, parsed ?: "Error al enviar mensaje")
                } catch (e: Exception) {
                    tts.speak(this@MainActivity, "Error al enviar mensaje")
                    Log.e("MENSAJE JOHNNY", e.stackTraceToString())
                }
            }
        }
    }

    private fun setupFullScreenUI() {
        enableEdgeToEdge()
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(sysBars.left, sysBars.top, sysBars.right, sysBars.bottom)
            insets
        }
    }

    private fun setupTextureSurfaceListener() {
        textureView.surfaceTextureListener = cameraController.surfaceListener(viewModel)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (dictation.isResult(requestCode, resultCode)) {
            val result = dictation.extractText(data)
            if (result.isNullOrBlank()) {
                tts.speak(this, "Mensaje vacío detectado")
            } else {
                viewModel.setDictationResult("Que ves?")
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (requestCode == PERMISSIONS_REQUEST_CODE && results.size >= 2) {
            viewModel.setCameraPermission(results[0] == PackageManager.PERMISSION_GRANTED)
        }
    }
}