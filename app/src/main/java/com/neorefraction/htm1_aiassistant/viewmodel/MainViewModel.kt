package com.neorefraction.htm1_aiassistant.viewmodel

import android.Manifest
import android.content.Context.CAMERA_SERVICE
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class MainViewModel {

    // ViewModel required attributes
    private var isPermissionAccepted: Boolean = false
    private var isSurfaceAvailable: Boolean = false
    private lateinit var cameraManager: CameraManager

    // Required LiveData
    private val _camera: MutableLiveData<CameraDevice?> = MutableLiveData<CameraDevice?>(null)
    val camera: LiveData<CameraDevice?> get() = this._camera

    private val _isAppReady: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    val isAppReady: LiveData<Boolean> get() = this._isAppReady

    fun setCameraManager(cameraManager: CameraManager) {
        this.cameraManager = cameraManager
    }

    fun setPermission(result: Int) {
        this.isPermissionAccepted = result == PackageManager.PERMISSION_GRANTED
        this._isAppReady.value = this.isPermissionAccepted && this.isSurfaceAvailable
    }

    fun setSurfaceAvailability(availability: Boolean) {
        this.isSurfaceAvailable = availability
        this._isAppReady.value = this.isPermissionAccepted && this.isSurfaceAvailable
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    fun openCamera() {
        try {
            cameraManager.openCamera(this.cameraManager.cameraIdList[0], object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    Log.w("JOHNNY", "LA CAMARA SE HA ABIERTO")
                    _camera.value = camera
                }

                override fun onDisconnected(camera: CameraDevice) {
                    _camera.value?.close()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.w("JOHNNY", "ERROR AL ABRIR LA CAMARA")
                    _camera.value?.close()
                    _camera.value = null
                }
            }, null)
        } catch (e: CameraAccessException) {
            throw IllegalStateException("There is no camera access") as Throwable
        }
    }
}