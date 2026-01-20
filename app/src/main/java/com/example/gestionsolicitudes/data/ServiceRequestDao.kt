package com.example.gestionsolicitudes.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ServiceRequestDao {

    @Query("SELECT * FROM service_requests ORDER BY id DESC")
    suspend fun getAll(): List<ServiceRequestEntity>

    @Query("SELECT * FROM service_requests WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ServiceRequestEntity?

    @Insert
    suspend fun insert(request: ServiceRequestEntity): Long

    @Update
    suspend fun update(request: ServiceRequestEntity)

    @Delete
    suspend fun delete(request: ServiceRequestEntity)
}