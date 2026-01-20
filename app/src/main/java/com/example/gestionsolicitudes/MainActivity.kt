package com.example.gestionsolicitudes

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gestionsolicitudes.model.ServiceRequest
import com.example.gestionsolicitudes.ui.ServiceRequestAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

class MainActivity : AppCompatActivity() {

    private lateinit var rvRequests: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var btnAdd: Button

    private val requests = mutableListOf<ServiceRequest>()
    private lateinit var adapter: ServiceRequestAdapter

    private val addRequestLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data ?: return@registerForActivityResult

                val client = data.getStringExtra(AddRequestActivity.EXTRA_CLIENT).orEmpty()
                val type = data.getStringExtra(AddRequestActivity.EXTRA_TYPE).orEmpty()
                val date = data.getStringExtra(AddRequestActivity.EXTRA_DATE).orEmpty()
                val desc = data.getStringExtra(AddRequestActivity.EXTRA_DESC).orEmpty()

                // Agregar a memoria
                requests.add(
                    ServiceRequest(
                        clientName = client,
                        serviceType = type,
                        date = date,
                        description = desc
                    )
                )
                adapter.notifyItemInserted(requests.size - 1)
                updateEmptyState()

                Toast.makeText(this, getString(R.string.toast_saved), Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvRequests = findViewById(R.id.rvRequests)
        tvEmpty = findViewById(R.id.tvEmpty)
        btnAdd = findViewById(R.id.btnAdd)

        requests.clear()
        requests.add(
            ServiceRequest(
                clientName = "Juan Pérez",
                serviceType = "Electricidad",
                date = "10/03/2026",
                description = "No funciona el enchufe del living."
            )
        )

        adapter = ServiceRequestAdapter(requests) { position ->
            showDeleteDialog(position)
        }

        rvRequests.layoutManager = LinearLayoutManager(this)
        rvRequests.adapter = adapter

        btnAdd.setOnClickListener {
            val i = Intent(this, AddRequestActivity::class.java)
            addRequestLauncher.launch(i)
        }

        updateEmptyState()
    }

    private fun updateEmptyState() {
        tvEmpty.visibility = if (requests.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showDeleteDialog(position: Int) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_delete_title))
            .setMessage(getString(R.string.dialog_delete_msg))
            .setPositiveButton(getString(R.string.dialog_yes)) { _, _ ->
                adapter.removeAt(position)
                updateEmptyState()
            }
            .setNegativeButton(getString(R.string.dialog_no), null)
            .show()
    }
}