package com.neorefraction.htm1_aiassistant.camera

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.util.Log
import android.view.Surface
import android.widget.Toast

object CameraUtils {

    fun prepareSurface(surfaceTexture: SurfaceTexture): Surface? {
        return try {
            surfaceTexture.setDefaultBufferSize(1920, 1080)
            Surface(surfaceTexture)
        } catch (e: Exception) {
            Log.e("Camera", "Error preparando Surface", e)
            null
        }
    }

    fun buildPreviewRequest(camera: CameraDevice?, surface: Surface): CaptureRequest? {
        return try {
            camera?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)?.apply {
                addTarget(surface)
            }?.build()
        } catch (e: CameraAccessException) {
            Log.e("Camera", "Error construyendo request", e)
            null
        }
    }

    fun createPreviewSession(
        camera: CameraDevice?,
        surface: Surface,
        request: CaptureRequest,
        context: Context
    ) {
        camera?.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                try {
                    session.setRepeatingRequest(request, null, null)
                } catch (e: CameraAccessException) {
                    Log.e("Camera", "Error iniciando preview", e)
                }
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                Toast.makeText(context, "Error al configurar la cámara", Toast.LENGTH_SHORT).show()
            }
        }, null)
    }
}