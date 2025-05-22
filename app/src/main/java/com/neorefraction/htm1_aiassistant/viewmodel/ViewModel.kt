package com.neorefraction.htm1_aiassistant.viewmodel

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

import androidx.lifecycle.*

class ViewModel : ViewModel() {

    // Control de cámara
    private val _isPermissionGranted = MutableLiveData(false)
    private val _isSurfaceAvailable = MutableLiveData(false)

    // Nuevas LiveData
    private val _dictationResult = MutableLiveData<String?>()
    private val _photoBitmap = MutableLiveData<String?>()

    fun setCameraPermission(value: Boolean) {
        _isPermissionGranted.value = value
    }

    fun setSurfaceAvailability(value: Boolean) {
        _isSurfaceAvailable.value = value
    }

    fun setDictationResult(result: String?) {
        _dictationResult.value = result
    }

    fun setPhoto(image: String?) {
        _photoBitmap.value = image
    }

    val dictationResult: LiveData<String?> = _dictationResult
    val photoBitmap: LiveData<String?> = _photoBitmap

    val isCameraReady: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        var hasPermission = false
        var surfaceAvailable = false

        fun update() {
            value = hasPermission && surfaceAvailable
        }

        addSource(_isPermissionGranted) {
            hasPermission = it
            update()
        }

        addSource(_isSurfaceAvailable) {
            surfaceAvailable = it
            update()
        }
    }

    // MediatorLiveData que espera ambos resultados
    val bothResultsReady: LiveData<Pair<String, String>> = MediatorLiveData<Pair<String, String>>().apply {
        var currentText: String? = null
        var currentPhoto: String? = null

        fun update() {
            if (currentText != null && currentPhoto != null) {
                value = Pair(currentText!!, currentPhoto!!)
                // Reset para próxima vez (opcional, depende de tu lógica)
                _dictationResult.value = null
                _photoBitmap.value = null
            }
        }

        addSource(_dictationResult) {
            currentText = it
            update()
        }

        addSource(_photoBitmap) {
            currentPhoto = it
            update()
        }
    }
}
