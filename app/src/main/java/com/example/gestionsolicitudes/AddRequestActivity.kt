package com.example.gestionsolicitudes

import android.app.Activity
import android.content.Intent
import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.util.Log
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

    companion object {
        // ✅ SOLO 1 companion object (arregla el error)
        const val TAG_UI = "UI_LAYER"
        const val TAG_DB = "DB_LAYER"
        const val TAG_ERR = "ERROR_HANDLER"

        // ✅ EXTRA_ID existe aquí (arregla el Unresolved reference)
        const val EXTRA_ID = "extra_id"
    }

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

        editingId = intent.getLongExtra(EXTRA_ID, -1L).takeIf { it != -1L }
        isEditMode = editingId != null

        if (isEditMode) {
            tvTitleAdd.text = getString(R.string.title_edit_request)
            Log.i(TAG_UI, "Modo EDITAR iniciado id=$editingId")
            loadRequestForEdit(editingId!!)
        } else {
            tvTitleAdd.text = getString(R.string.title_add_request)
            Log.i(TAG_UI, "Modo CREAR iniciado")
        }

        btnSave.setOnClickListener { onSave() }
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
            try {
                val request = withContext(Dispatchers.IO) {
                    Log.d(TAG_DB, "Room SELECT getById($id)")
                    dao.getById(id)
                }

                if (request == null) {
                    Log.w(TAG_ERR, "No se encontró solicitud para editar id=$id")
                    Toast.makeText(this@AddRequestActivity, "No se encontró la solicitud", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                etClientName.setText(request.clientName)
                etDate.setText(request.date)
                etDescription.setText(request.description)

                // Seleccionar tipo en spinner
                val spinnerAdapter = spServiceType.adapter
                for (i in 0 until spinnerAdapter.count) {
                    if (spinnerAdapter.getItem(i).toString() == request.serviceType) {
                        spServiceType.setSelection(i)
                        break
                    }
                }

            } catch (e: SQLiteException) {
                Log.e(TAG_ERR, "Error SQLite cargando solicitud id=$id: ${e.message}", e)
                Toast.makeText(this@AddRequestActivity, "Error de base de datos", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Log.e(TAG_ERR, "Error general cargando solicitud id=$id: ${e.message}", e)
                Toast.makeText(this@AddRequestActivity, "Error inesperado", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun onSave() {
        try {
            // VALIDACIÓN (usuario)
            val clientName = etClientName.text?.toString()?.trim().orEmpty()
            val serviceType = spServiceType.selectedItem?.toString().orEmpty()
            val date = etDate.text?.toString()?.trim().orEmpty()
            val description = etDescription.text?.toString()?.trim().orEmpty()

            validateInputs(clientName, date, description)

            Log.i(TAG_UI, "Guardar presionado. ModoEditar=$isEditMode")

            lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        if (isEditMode) {
                            Log.d(TAG_DB, "Room UPDATE id=$editingId")
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
                            Log.d(TAG_DB, "Room INSERT nuevo")
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

                    Toast.makeText(
                        this@AddRequestActivity,
                        if (isEditMode) getString(R.string.toast_updated) else getString(R.string.toast_saved),
                        Toast.LENGTH_SHORT
                    ).show()

                    setResult(Activity.RESULT_OK, Intent())
                    finish()

                } catch (e: SQLiteException) {
                    Log.e(TAG_ERR, "Error SQLite guardando: ${e.message}", e)
                    Toast.makeText(this@AddRequestActivity, "Error de base de datos al guardar", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e(TAG_ERR, "Error general guardando: ${e.message}", e)
                    Toast.makeText(this@AddRequestActivity, "Error inesperado al guardar", Toast.LENGTH_SHORT).show()
                }
            }

        } catch (e: IllegalArgumentException) {
            Log.w(TAG_ERR, "Validación fallida: ${e.message}")
            Toast.makeText(this, e.message ?: "Datos inválidos", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG_ERR, "Error general en onSave: ${e.message}", e)
            Toast.makeText(this, "Error inesperado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateInputs(clientName: String, date: String, description: String) {
        var ok = true
        if (clientName.isBlank()) {
            etClientName.error = "Ingresa el nombre"
            ok = false
        }
        if (date.isBlank()) {
            etDate.error = "Ingresa la fecha"
            ok = false
        }
        if (description.isBlank()) {
            etDescription.error = "Ingresa la descripción"
            ok = false
        }
        if (!ok) throw IllegalArgumentException("Completa los campos obligatorios")
    }
}