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
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var rvRequests: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var btnAdd: Button

    private lateinit var dao: ServiceRequestDao

    private val requests = mutableListOf<ServiceRequestEntity>()
    private lateinit var adapter: ServiceRequestAdapter

    private val addRequestLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                loadRequestsFromDb()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvRequests = findViewById(R.id.rvRequests)
        tvEmpty = findViewById(R.id.tvEmpty)
        btnAdd = findViewById(R.id.btnAdd)

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

        loadRequestsFromDb()
    }

    private fun loadRequestsFromDb() {
        lifecycleScope.launch {
            val list = withContext(Dispatchers.IO) {
                dao.getAll()
            }
            adapter.setItems(list)
            tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
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
                    loadRequestsFromDb()
                }
            }
            .setNegativeButton(getString(R.string.dialog_no), null)
            .show()
    }
}