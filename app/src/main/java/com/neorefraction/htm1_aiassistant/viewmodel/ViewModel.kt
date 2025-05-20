package com.neorefraction.htm1_aiassistant.viewmodel

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class ViewModel: ViewModel() {

    // Variable to check permissions
    private val _isPermissionGranted: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    private val _isSurfaceAvailable: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)


    fun setCameraPermission(value: Boolean) {
        this._isPermissionGranted.value = value
    }

    fun setSurfaceAvailability(value: Boolean) {
        this._isSurfaceAvailable.value = value
    }

    val isCameraReady: MediatorLiveData<Boolean> = MediatorLiveData<Boolean>(false).apply {
        var hasPermission: Boolean = false
        var surfaceAvailable: Boolean = false

        fun update() {
            this.value = hasPermission && surfaceAvailable
        }

        addSource(_isSurfaceAvailable) { isSurfaceAvailable ->
            surfaceAvailable = isSurfaceAvailable
            update()
        }

        addSource(_isPermissionGranted) { isPermissionGranted ->
            hasPermission = isPermissionGranted
            update()
        }
    }
}