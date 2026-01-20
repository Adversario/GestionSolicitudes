package com.example.gestionsolicitudes.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "service_requests")
data class ServiceRequestEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val clientName: String,
    val serviceType: String,
    val date: String,
    val description: String
)