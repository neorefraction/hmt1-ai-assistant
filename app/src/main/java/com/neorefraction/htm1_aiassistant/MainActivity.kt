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
import android.view.WindowInsets
import android.widget.Toast

const val CAMERA_PERMISSION_REQUEST_CODE = 100

class MainActivity : AppCompatActivity() {
    // Texture View
    private lateinit var textureView: TextureView

    // Camera
    private lateinit var cameraService: CameraManager
    private var cameraDevice: CameraDevice? = null
    private lateinit var cameraId: String

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

        // Verifies that the TextureView is ready to display camera image
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture, width: Int, height: Int) {
                openCamera()
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture, width: Int, height: Int) {}

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
    }

    // Método que se llama después de que el usuario haya respondido a la solicitud de permisos
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            // Verificar si el permiso fue concedido
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Si el permiso es concedido, continuar con la funcionalidad de la cámara
                openCamera()
            } else {
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
                // Si no se tienen permisos, muestra un mensaje
                Toast.makeText(this, "No se tienen permisos para usar la cámara", Toast.LENGTH_SHORT).show()
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
}