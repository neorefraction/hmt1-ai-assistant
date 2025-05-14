package com.neorefraction.htm1_aiassistant

import android.content.Context
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
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData

const val CAMERA_PERMISSION_REQUEST_CODE = 100

class MainActivity : AppCompatActivity() {
    // Texture View
    private lateinit var textureView: TextureView

    // Camera
    private lateinit var cameraService: CameraManager
    private var cameraDevice: CameraDevice? = null
    private lateinit var cameraId: String

    // Observables
    private val _isPermissionGranted = MutableLiveData<Boolean>()
    private val _isSurfaceAvailable = MutableLiveData<Boolean>()
    private val _isCameraReady = MediatorLiveData<Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        // Inicializa la referencia de TextureView después de inflar el layout
        textureView = findViewById(R.id.tvCamera)

        // Set Android Camera service as camera manager
        cameraService = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        // Implements TextureView surface listener interface
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            // Callback for surface when is ready to be used
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture, width: Int, height: Int) {
                _isSurfaceAvailable.value = true
            }

            // Callback for surface when size changes
            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture, width: Int, height: Int) {}

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {} // Not used
        }

        _isCameraReady.observe(this) { ready ->
            if (ready == true) {
                openCamera()
            }
        }
    }

    // Callback for Android Permissions once the user response the permissions request
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) { // Permissions for camera
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            _isPermissionGranted.value = granted
            if(!granted) {
                // Si el permiso no fue concedido, mostrar un mensaje al usuario
                Toast.makeText(this, "Permission denied! Unable to use the camera.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openCamera() {
        try {
            // Obtener el ID de la cámara trasera (o frontal según lo que desees)
            cameraId = cameraService.cameraIdList[0]  // Usamos la cámara 0 (generalmente trasera)

            // Abrir la cámara
            if (checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                cameraService.openCamera(cameraId, object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        cameraDevice = camera
                        startPreview(camera)
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        cameraDevice?.close()
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        cameraDevice?.close()
                        cameraDevice = null
                    }
                }, null)
            } else {
                Toast.makeText(this, "There are no camera permissions", Toast.LENGTH_SHORT).show()
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
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

    private fun updateCameraReadyState() {
        val permission = _isPermissionGranted.value ?: false
        val surface = _isSurfaceAvailable.value ?: false
        _isCameraReady.value = permission && surface
    }
}