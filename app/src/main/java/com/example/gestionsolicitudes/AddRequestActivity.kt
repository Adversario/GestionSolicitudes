package com.example.gestionsolicitudes

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.gestionsolicitudes.data.AppDatabase
import com.example.gestionsolicitudes.data.ServiceRequestDao
import com.example.gestionsolicitudes.data.ServiceRequestEntity
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddRequestActivity : AppCompatActivity() {

    private lateinit var tvTitleAdd: TextView
    private lateinit var etClientName: TextInputEditText
    private lateinit var spServiceType: Spinner
    private lateinit var etDate: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var btnSave: Button

    private lateinit var dao: ServiceRequestDao

    private var editingId: Long? = null
    private var isEditMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_request)

        tvTitleAdd = findViewById(R.id.tvTitleAdd)
        etClientName = findViewById(R.id.etClientName)
        spServiceType = findViewById(R.id.spServiceType)
        etDate = findViewById(R.id.etDate)
        etDescription = findViewById(R.id.etDescription)
        btnSave = findViewById(R.id.btnSave)

        dao = AppDatabase.getInstance(this).serviceRequestDao()

        setupSpinner()

        // Modo Editar si viene un id
        editingId = intent.getLongExtra(EXTRA_ID, -1L).takeIf { it != -1L }
        isEditMode = editingId != null

        if (isEditMode) {
            tvTitleAdd.text = getString(R.string.title_edit_request)
            loadRequestForEdit(editingId!!)
        } else {
            tvTitleAdd.text = getString(R.string.title_add_request)
        }

        btnSave.setOnClickListener {
            onSave()
        }
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.service_types,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spServiceType.adapter = adapter
    }

    private fun loadRequestForEdit(id: Long) {
        lifecycleScope.launch {
            val request = withContext(Dispatchers.IO) {
                dao.getById(id)
            }

            if (request == null) {
                Toast.makeText(this@AddRequestActivity, "No se encontró la solicitud", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            etClientName.setText(request.clientName)
            etDate.setText(request.date)
            etDescription.setText(request.description)

            // Seleccionar el tipo en Spinner
            val spinnerAdapter = spServiceType.adapter
            for (i in 0 until spinnerAdapter.count) {
                if (spinnerAdapter.getItem(i).toString() == request.serviceType) {
                    spServiceType.setSelection(i)
                    break
                }
            }
        }
    }

    private fun onSave() {
        val clientName = etClientName.text?.toString()?.trim().orEmpty()
        val serviceType = spServiceType.selectedItem?.toString().orEmpty()
        val date = etDate.text?.toString()?.trim().orEmpty()
        val description = etDescription.text?.toString()?.trim().orEmpty()

        // Validación mínima
        var ok = true
        if (clientName.isBlank()) { etClientName.error = "Ingresa el nombre"; ok = false }
        if (date.isBlank()) { etDate.error = "Ingresa la fecha"; ok = false }
        if (description.isBlank()) { etDescription.error = "Ingresa la descripción"; ok = false }
        if (!ok) return

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                if (isEditMode) {
                    dao.update(
                        ServiceRequestEntity(
                            id = editingId!!,
                            clientName = clientName,
                            serviceType = serviceType,
                            date = date,
                            description = description
                        )
                    )
                } else {
                    dao.insert(
                        ServiceRequestEntity(
                            clientName = clientName,
                            serviceType = serviceType,
                            date = date,
                            description = description
                        )
                    )
                }
            }

            // Toast requerido en formulario
            if (isEditMode) {
                Toast.makeText(this@AddRequestActivity, getString(R.string.toast_updated), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@AddRequestActivity, getString(R.string.toast_saved), Toast.LENGTH_SHORT).show()
            }

            setResult(
                Activity.RESULT_OK,
                Intent().putExtra(EXTRA_WAS_EDIT, isEditMode)
            )
            finish()
        }
    }

    companion object {
        const val EXTRA_ID = "extra_id"
        const val EXTRA_WAS_EDIT = "extra_was_edit"
    }
}