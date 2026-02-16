package com.example.gestionsolicitudes.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestionsolicitudes.data.ServiceRequestEntity
import com.example.gestionsolicitudes.repository.ServiceRequestRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ServiceRequestViewModel(
    private val repository: ServiceRequestRepository
) : ViewModel() {

    private val _state = MutableLiveData<UiState<List<ServiceRequestEntity>>>()
    val state: LiveData<UiState<List<ServiceRequestEntity>>> = _state

    companion object {
        // Solo para que Lottie se aprecie en video/capturas, no bloquea la UI
        private const val MIN_LOADING_MS = 1200L
    }

    fun loadLocal() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            try {
                // opcional: hace más visible el overlay incluso en lecturas rápidas de Room
                delay(300)
                val local = repository.getLocalRequests()
                _state.value = UiState.Success(local)
            } catch (e: Exception) {
                _state.value = UiState.Error("Error cargando datos locales: ${e.message}")
            }
        }
    }

    fun syncFromApi() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            try {
                // tiempo mínimo para que se vea Lottie en la demo
                delay(MIN_LOADING_MS)

                val synced = repository.syncFromApi()
                _state.value = UiState.Success(synced)

            } catch (e: Exception) {
                _state.value = UiState.Error("Error sincronizando API: ${e.message}")
            }
        }
    }

    fun delete(item: ServiceRequestEntity) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            try {
                repository.delete(item)
                val local = repository.getLocalRequests()
                _state.value = UiState.Success(local)
            } catch (e: Exception) {
                _state.value = UiState.Error("Error eliminando: ${e.message}")
            }
        }
    }
}