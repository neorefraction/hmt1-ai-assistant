package com.neorefraction.htm1_aiassistant.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.util.Base64
import android.view.TextureView
import com.neorefraction.htm1_aiassistant.viewmodel.ViewModel
import java.io.ByteArrayOutputStream


class CameraController(
    private val context: Context,
    private val textureView: TextureView
) {
    private var camera: CameraDevice? = null
    private val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    fun open() {
        val cameraId = manager.cameraIdList.firstOrNull() ?: return
        if (context.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return

        manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                this@CameraController.camera = camera
                startPreview()
            }

            override fun onDisconnected(camera: CameraDevice) = camera.close()
            override fun onError(camera: CameraDevice, error: Int) {
                camera.close()
                this@CameraController.camera = null
            }
        }, null)
    }

    fun close() {
        camera?.close()
        camera = null
    }

    fun surfaceListener(viewModel: ViewModel) = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
            viewModel.setSurfaceAvailability(true)
        }

        override fun onSurfaceTextureDestroyed(st: SurfaceTexture) = false.also {
            viewModel.setSurfaceAvailability(false)
        }

        override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
        override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {}
    }

    private fun startPreview() {
        val surfaceTexture = textureView.surfaceTexture ?: return
        CameraUtils.prepareSurface(surfaceTexture)?.let { surface ->
            val request = CameraUtils.buildPreviewRequest(camera, surface) ?: return
            CameraUtils.createPreviewSession(camera, surface, request, context)
        }
    }

    fun launchCameraRawPhoto(): String? {
        val bitmap = textureView.bitmap
        if (bitmap != null) {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            val byteArray = outputStream.toByteArray()
            val base64Image = Base64.encodeToString(byteArray, Base64.NO_WRAP)

            return base64Image
        } else {
            return null
        }
    }

}