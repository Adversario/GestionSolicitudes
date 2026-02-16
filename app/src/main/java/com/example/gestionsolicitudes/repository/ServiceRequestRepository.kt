package com.example.gestionsolicitudes.repository

import android.util.Log
import com.example.gestionsolicitudes.data.ServiceRequestDao
import com.example.gestionsolicitudes.data.ServiceRequestEntity
import com.example.gestionsolicitudes.data.remote.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ServiceRequestRepository(
    private val dao: ServiceRequestDao,
    private val apiProvider: () -> ApiService
) {

    companion object {
        const val TAG_DATA = "DATA_LAYER"
        const val TAG_DB = "DB_LAYER"
        const val TAG_ERR = "ERROR_HANDLER"
    }

    suspend fun getLocalRequests(): List<ServiceRequestEntity> = withContext(Dispatchers.IO) {
        Log.d(TAG_DB, "Room: getAll()")
        dao.getAll()
    }

    suspend fun syncFromApi(): List<ServiceRequestEntity> = withContext(Dispatchers.IO) {
        // delay intencional para que se vea Lottie en video (comentar si no se usa)
        kotlinx.coroutines.delay(1200)
        Log.i(TAG_DATA, "API: getPosts() iniciando (provider dinámico)")
        val posts = apiProvider().getPosts()
        Log.i(TAG_DATA, "API: posts recibidos=${posts.size}")

        val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        val mapped = posts.take(10).map { p ->
            ServiceRequestEntity(
                clientName = "Cliente API #${p.userId}",
                serviceType = "API",
                date = today,
                description = "${p.title}\n${p.body}"
            )
        }

        Log.d(TAG_DB, "Room: deleteByType('API')")
        dao.deleteByType("API")

        Log.d(TAG_DB, "Room: insertAll(mapped size=${mapped.size})")
        dao.insertAll(mapped)

        Log.d(TAG_DB, "Room: getAll() post-sync")
        dao.getAll()
    }

    suspend fun delete(item: ServiceRequestEntity) = withContext(Dispatchers.IO) {
        Log.d(TAG_DB, "Room: delete(id=${item.id})")
        dao.delete(item)
    }
}