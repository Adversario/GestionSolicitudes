package com.example.gestionsolicitudes

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.example.gestionsolicitudes.data.AppDatabase
import com.example.gestionsolicitudes.data.ServiceRequestDao
import com.example.gestionsolicitudes.data.ServiceRequestEntity
import com.example.gestionsolicitudes.data.remote.RetrofitClient
import com.example.gestionsolicitudes.leaks.LeakHolder
import com.example.gestionsolicitudes.repository.ServiceRequestRepository
import com.example.gestionsolicitudes.ui.ServiceRequestAdapter
import com.example.gestionsolicitudes.viewmodel.ServiceRequestViewModel
import com.example.gestionsolicitudes.viewmodel.ServiceRequestViewModelFactory
import com.example.gestionsolicitudes.viewmodel.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var rvRequests: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var btnAdd: Button
    private lateinit var btnSync: Button
    private lateinit var btnToggleError: Button

    private lateinit var loadingOverlay: View
    private lateinit var lottieLoading: LottieAnimationView

    private lateinit var dao: ServiceRequestDao
    private lateinit var viewModel: ServiceRequestViewModel

    private val requests = mutableListOf<ServiceRequestEntity>()
    private lateinit var adapter: ServiceRequestAdapter

    // Para que el loading se alcance a ver
    private var loadingShownAt: Long = 0L
    private val minLoadingMs = 900L

    companion object {
        const val TAG_UI = "UI_LAYER"
        const val TAG_ERR = "ERROR_HANDLER"
        const val TAG_MEM = "MEMORY_FIX"
    }

    private val addRequestLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.loadLocal()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvRequests = findViewById(R.id.rvRequests)
        tvEmpty = findViewById(R.id.tvEmpty)
        btnAdd = findViewById(R.id.btnAdd)
        btnSync = findViewById(R.id.btnSync)
        btnToggleError = findViewById(R.id.btnToggleError)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        lottieLoading = findViewById(R.id.lottieLoading)

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

        // Inicialmente modo error OFF
        RetrofitClient.FORCE_ERROR = false
        updateToggleText()

        // ViewModel + Observer
        val repository = ServiceRequestRepository(dao) { RetrofitClient.api }
        val factory = ServiceRequestViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[ServiceRequestViewModel::class.java]

        viewModel.state.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    Log.d(TAG_UI, "UI: Loading")
                    showLoading(true)
                }

                is UiState.Success -> {
                    Log.d(TAG_UI, "UI: Success items=${state.data.size}")
                    showLoading(false)
                    adapter.setItems(state.data)
                    tvEmpty.visibility = if (state.data.isEmpty()) View.VISIBLE else View.GONE
                }

                is UiState.Error -> {
                    Log.e(TAG_ERR, "UI: Error -> ${state.message}")
                    showLoading(false)
                    tvEmpty.visibility = View.VISIBLE
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnAdd.setOnClickListener {
            val intent = Intent(this, AddRequestActivity::class.java)
            addRequestLauncher.launch(intent)
        }

        btnSync.setOnClickListener {
            Log.i(TAG_UI, "Usuario presiona Sincronizar (FORCE_ERROR=${RetrofitClient.FORCE_ERROR})")
            viewModel.syncFromApi()
        }

        btnToggleError.setOnClickListener {
            RetrofitClient.FORCE_ERROR = !RetrofitClient.FORCE_ERROR
            updateToggleText()
            Toast.makeText(
                this,
                "Modo Error: " + (if (RetrofitClient.FORCE_ERROR) "ON" else "OFF"),
                Toast.LENGTH_SHORT
            ).show()
        }

        // Carga inicial desde Room
        viewModel.loadLocal()
    }

    private fun updateToggleText() {
        btnToggleError.text = "Modo Error: " + (if (RetrofitClient.FORCE_ERROR) "ON" else "OFF")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (LeakHolder.leakedActivity === this) {
            LeakHolder.leakedActivity = null
            Log.d(TAG_MEM, "LeakHolder.leakedActivity limpiado en onDestroy()")
        }
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            loadingShownAt = SystemClock.elapsedRealtime()
            loadingOverlay.visibility = View.VISIBLE
            lottieLoading.playAnimation()
        } else {
            // Garantiza que se vea un mínimo de tiempo
            val elapsed = SystemClock.elapsedRealtime() - loadingShownAt
            val remaining = minLoadingMs - elapsed

            lifecycleScope.launch {
                if (remaining > 0) delay(remaining)
                lottieLoading.cancelAnimation()
                loadingOverlay.visibility = View.GONE
            }
        }
    }

    private fun showDeleteDialog(item: ServiceRequestEntity) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_delete_title))
            .setMessage(getString(R.string.dialog_delete_msg))
            .setPositiveButton(getString(R.string.dialog_yes)) { _, _ ->
                viewModel.delete(item)
            }
            .setNegativeButton(getString(R.string.dialog_no), null)
            .show()
    }
}