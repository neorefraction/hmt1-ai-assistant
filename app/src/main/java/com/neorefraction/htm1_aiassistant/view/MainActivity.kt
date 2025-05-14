package com.neorefraction.htm1_aiassistant.view

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.os.Bundle
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

// Camera required imports
import android.hardware.camera2.CameraDevice  // New API (hardware.Camera is deprecated)
import android.hardware.camera2.CameraManager
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.neorefraction.htm1_aiassistant.R
import com.neorefraction.htm1_aiassistant.viewmodel.MainViewModel

const val CAMERA_PERMISSION_REQUEST_CODE = 100

class MainActivity : AppCompatActivity() {
    // ViewModel
    private val viewModel: MainViewModel = MainViewModel()

    // Texture View
    private lateinit var textureView: TextureView

    // CameraService
    private lateinit var cameraManager: CameraManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Request Permissions
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        this.cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        // Init Activity
        this.viewModel.setCameraManager(this.cameraManager)
        initObservables()
        this.initLayout()
        this.initComponents()


    }

    // Callback for Android Permissions once the user response the permissions request
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(grantResults.isNotEmpty()) this.viewModel.setPermission(grantResults[0])
    }

    private fun initObservables() {
        viewModel.isAppReady.observe(this) {
            if(checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                this.viewModel.openCamera()
            }
        }
        viewModel.camera.observe(this) {
            startPreview(this.viewModel.camera.value!!)
        }
    }

    private fun initLayout() {
        // Sets Activity layout
        setContentView(R.layout.activity_main)

        // Set layout to full screen
        enableEdgeToEdge()

        // Set Auto hide for Windows Insets (power, wi-fi, time, ...)
        WindowInsetsControllerCompat(window, window.decorView).apply {  // Window object is like JS DOM but represents al UI space for each activity
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // Listener to apply padding into the main layout when Insets are applied on the window
        ViewCompat.setOnApplyWindowInsetsListener(findViewById<LinearLayout>(R.id.main)) { v, insets -> // Uses ViewCompat to ensure compatibility with legacy devices and avoid crashes in run time
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars()) // Retrieves systemBars from insets
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets // Return value ignored
        }
    }

    private fun initComponents() {
        // Inicializa la referencia de TextureView después de inflar el layout
        textureView = findViewById(R.id.tvCamera)

        // Implements TextureView surface listener interface
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            // Callback for surface when is ready to be used
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                viewModel.setSurfaceAvailability(true)
            }

            // Callback for surface when size changes
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                viewModel.setSurfaceAvailability(false)
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {} // Not used
        }
    }

    private fun startPreview(cameraDevice: CameraDevice) {
        val surfaceTexture = textureView.surfaceTexture
        surfaceTexture?.setDefaultBufferSize(1920, 1080)  // Tamaño de la vista previa
        val surface = Surface(surfaceTexture)

        val previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        previewRequestBuilder.addTarget(surface)

        val cameraCaptureSessionCallback = object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                try {
                    // Inicia la vista previa de la cámara
                    session.setRepeatingRequest(previewRequestBuilder.build(), null, null)
                } catch (e: CameraAccessException) {
                    e.printStackTrace()
                }
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                Toast.makeText(applicationContext, "Configuración fallida", Toast.LENGTH_SHORT).show()
            }
        }

        // Crear una sesión de captura
        cameraDevice.createCaptureSession(listOf(surface), cameraCaptureSessionCallback, null)
    }
}