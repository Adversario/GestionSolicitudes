package com.example.gestionsolicitudes.model

data class ServiceRequest(
    val clientName: String,
    val serviceType: String,
    val date: String,
    val description: String
)