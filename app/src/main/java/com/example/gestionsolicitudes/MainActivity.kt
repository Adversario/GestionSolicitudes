package com.example.gestionsolicitudes

import android.app.Activity
import android.content.Intent
import android.os.Bundle
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var rvRequests: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var btnAdd: Button
    private lateinit var loadingOverlay: View

    private lateinit var dao: ServiceRequestDao

    private val requests = mutableListOf<ServiceRequestEntity>()
    private lateinit var adapter: ServiceRequestAdapter

    /**
     * Semana 3:
     * - true  => ANTES (freeze)
     * - false => DESPUÉS (coroutines correcto)
     *
     * Para capturas:
     * 1) pon true, saca capturas ANTES
     * 2) pon false, saca capturas DESPUÉS
     */
    private val PERFORMANCE_MODE_BEFORE = false

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
                val i = Intent(this, AddRequestActivity::class.java)
                i.putExtra(AddRequestActivity.EXTRA_ID, item.id)
                addRequestLauncher.launch(i)
            },
            onDeleteClick = { item ->
                showDeleteDialog(item)
            }
        )

        rvRequests.layoutManager = LinearLayoutManager(this)
        rvRequests.adapter = adapter

        btnAdd.setOnClickListener {
            val i = Intent(this, AddRequestActivity::class.java)
            addRequestLauncher.launch(i)
        }

        loadRequests()
    }

    private fun loadRequests() {
        if (PERFORMANCE_MODE_BEFORE) {
            loadRequests_BEFORE_freezeMainThread()
        } else {
            loadRequests_AFTER_coroutines()
        }
    }

    // -------------------------
    // ANTES (freeze intencional)
    // -------------------------
    private fun loadRequests_BEFORE_freezeMainThread() {
        showLoading(true)

        val list: List<ServiceRequestEntity> = runBlocking {
            withContext(Dispatchers.IO) { dao.getAll() }
        }

        val processed = heavyProcessing_BAD_onMainThread(list)

        adapter.setItems(processed)
        tvEmpty.visibility = if (processed.isEmpty()) View.VISIBLE else View.GONE

        showLoading(false)
    }

    private fun heavyProcessing_BAD_onMainThread(list: List<ServiceRequestEntity>): List<ServiceRequestEntity> {
        Thread.sleep(2000)

        var junk = 0L
        for (i in 1..8_000_000) {
            junk += (i % 3)
        }

        return list.map { item ->
            item.copy(description = item.description + " (prep:$junk)")
        }
    }

    // -------------------------
    // DESPUÉS (coroutines bien)
    // -------------------------
    private fun loadRequests_AFTER_coroutines() {
        showLoading(true)

        lifecycleScope.launch {
            // Todo lo pesado en background
            val processed = withContext(Dispatchers.IO) {
                val list = dao.getAll()
                heavyProcessing_GOOD_inBackground(list)
            }

            // Solo UI en Main
            adapter.setItems(processed)
            tvEmpty.visibility = if (processed.isEmpty()) View.VISIBLE else View.GONE

            showLoading(false)
        }
    }

    /**
     * MISMO procesamiento pesado, pero corriendo en background (Dispatchers.IO).
     * Mantiene evidencia comparable ANTES vs DESPUÉS.
     */
    private fun heavyProcessing_GOOD_inBackground(list: List<ServiceRequestEntity>): List<ServiceRequestEntity> {
        // Simulación de carga pesada sin bloquear UI
        Thread.sleep(2000)

        var junk = 0L
        for (i in 1..8_000_000) {
            junk += (i % 3)
        }

        return list.map { item ->
            item.copy(description = item.description + " (prep:$junk)")
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
                    withContext(Dispatchers.IO) {
                        dao.delete(item)
                    }
                    loadRequests()
                }
            }
            .setNegativeButton(getString(R.string.dialog_no), null)
            .show()
    }
}