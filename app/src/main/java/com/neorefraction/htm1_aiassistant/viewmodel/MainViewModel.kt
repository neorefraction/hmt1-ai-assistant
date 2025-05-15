package com.neorefraction.htm1_aiassistant.viewmodel

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

const val CAMERA_PERMISSION_REQUEST_CODE = 100

class MainViewModel : ViewModel() {

    private val _isPermissionGranted = MutableLiveData<Boolean>(false)
    private val _isSurfaceAvailable = MutableLiveData<Boolean>(false)

    private val _camera = MutableLiveData<CameraDevice?>()
    val camera: LiveData<CameraDevice?> = _camera

    val isAppReady: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        var permissions = false
        var surface = false

        addSource(_isPermissionGranted) {
            permissions = it
            value = permissions && surface
        }

        addSource(_isSurfaceAvailable) {
            surface = it
            value = permissions && surface
        }
    }

    private lateinit var cameraManager: CameraManager

    fun setPermission(result: Int) {
        _isPermissionGranted.value = result == PackageManager.PERMISSION_GRANTED
    }

    fun setSurfaceAvailability(available: Boolean) {
        _isSurfaceAvailable.value = available
    }

    fun requestCameraPermissions(activity: Activity) {
        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
    }

    fun requestCameraService(getSystemService: (String) -> Any) {
        this.cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    fun openCamera(context: Context) {
        if (checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return

        val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return

        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                _camera.postValue(camera)
            }

            override fun onDisconnected(camera: CameraDevice) {
                camera.close()
            }

            override fun onError(camera: CameraDevice, error: Int) {
                camera.close()
                _camera.postValue(null)
            }
        }, null)
    }
}