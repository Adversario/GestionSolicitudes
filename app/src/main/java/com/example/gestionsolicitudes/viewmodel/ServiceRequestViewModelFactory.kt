package com.example.gestionsolicitudes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gestionsolicitudes.repository.ServiceRequestRepository

class ServiceRequestViewModelFactory(
    private val repository: ServiceRequestRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ServiceRequestViewModel::class.java)) {
            return ServiceRequestViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}