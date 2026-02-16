package com.example.gestionsolicitudes

import android.app.Activity
import android.content.Intent
import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gestionsolicitudes.data.AppDatabase
import com.example.gestionsolicitudes.data.ServiceRequestDao
import com.example.gestionsolicitudes.data.ServiceRequestEntity
import com.example.gestionsolicitudes.ui.ServiceRequestAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var rvRequests: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var btnAdd: Button
    private lateinit var loadingOverlay: View

    private lateinit var dao: ServiceRequestDao

    private val requests = mutableListOf<ServiceRequestEntity>()
    private lateinit var adapter: ServiceRequestAdapter

    companion object {
        const val TAG_UI = "UI_LAYER"
        const val TAG_DATA = "DATA_LAYER"
        const val TAG_DB = "DB_LAYER"
        const val TAG_ERR = "ERROR_HANDLER"
    }

    private val addRequestLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                loadRequests()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvRequests = findViewById(R.id.rvRequests)
        tvEmpty = findViewById(R.id.tvEmpty)
        btnAdd = findViewById(R.id.btnAdd)
        loadingOverlay = findViewById(R.id.loadingOverlay)

        dao = AppDatabase.getInstance(this).serviceRequestDao()

        adapter = ServiceRequestAdapter(
            items = requests,
            onItemClick = { item ->
                val intent = Intent(this, AddRequestActivity::class.java)
                intent.putExtra(AddRequestActivity.EXTRA_ID, item.id)
                addRequestLauncher.launch(intent)
            },
            onDeleteClick = { item ->
                showDeleteDialog(item)
            }
        )

        rvRequests.layoutManager = LinearLayoutManager(this)
        rvRequests.adapter = adapter

        btnAdd.setOnClickListener {
            val intent = Intent(this, AddRequestActivity::class.java)
            addRequestLauncher.launch(intent)
        }

        loadRequests()
    }

    // ---------------------------
    // FLUJO CRÍTICO
    // ---------------------------
    private fun loadRequests() {

        Log.i(TAG_UI, "Inicio carga solicitudes")
        showLoading(true)

        lifecycleScope.launch {

            try {
                val processed = withContext(Dispatchers.IO) {

                    Log.d(TAG_DB, "Consultando Room dao.getAll()")
                    val list = dao.getAll()
                    Log.i(TAG_DB, "Registros obtenidos=${list.size}")

                    Log.d(TAG_DATA, "Procesando datos en background")
                    val result = heavyProcessing(list)

                    result
                }

                Log.d(TAG_UI, "Actualizando RecyclerView en Main")
                adapter.setItems(processed)
                tvEmpty.visibility = if (processed.isEmpty()) View.VISIBLE else View.GONE

            } catch (e: SQLiteException) {
                Log.e(TAG_ERR, "Error SQLite en carga de lista: ${e.message}", e)
                tvEmpty.visibility = View.VISIBLE

            } catch (e: Exception) {
                Log.e(TAG_ERR, "Error general en carga de lista: ${e.message}", e)
                tvEmpty.visibility = View.VISIBLE

            } finally {
                Log.i(TAG_UI, "Fin carga solicitudes")
                showLoading(false)
            }
        }
    }

    private fun heavyProcessing(list: List<ServiceRequestEntity>): List<ServiceRequestEntity> {
        // Simulación más “realista”: delay corto + procesamiento liviano
        Thread.sleep(250)

        // Evitar loop gigante: hacemos una operación pequeña proporcional a la lista
        val suffix = " (proc)"
        return list.map { item ->
            // Evita concatenar muchas veces strings grandes; solo una vez por item
            item.copy(description = item.description + suffix)
        }
    }

    private fun showLoading(show: Boolean) {
        loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showDeleteDialog(item: ServiceRequestEntity) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_delete_title))
            .setMessage(getString(R.string.dialog_delete_msg))
            .setPositiveButton(getString(R.string.dialog_yes)) { _, _ ->

                lifecycleScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            dao.delete(item)
                        }
                        Log.i(TAG_DB, "Registro eliminado id=${item.id}")
                        loadRequests()

                    } catch (e: SQLiteException) {
                        Log.e(TAG_ERR, "Error SQLite eliminando: ${e.message}", e)

                    } catch (e: Exception) {
                        Log.e(TAG_ERR, "Error general eliminando: ${e.message}", e)
                    }
                }
            }
            .setNegativeButton(getString(R.string.dialog_no), null)
            .show()
    }
}