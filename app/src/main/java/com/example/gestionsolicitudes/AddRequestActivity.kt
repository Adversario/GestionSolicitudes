package com.example.gestionsolicitudes

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
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
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

class AddRequestActivity : AppCompatActivity() {

    private lateinit var tvTitleAdd: TextView
    private lateinit var etClientName: TextInputEditText
    private lateinit var spServiceType: Spinner

    // "Otro"
    private lateinit var tilOtherService: TextInputLayout
    private lateinit var etOtherService: TextInputEditText

    private lateinit var etDate: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var btnSave: Button

    private lateinit var dao: ServiceRequestDao

    private var editingId: Long? = null
    private var isEditMode: Boolean = false

    companion object {
        const val TAG_UI = "UI_LAYER"
        const val TAG_DB = "DB_LAYER"
        const val TAG_ERR = "ERROR_HANDLER"
        const val EXTRA_ID = "extra_id"

        private const val SERVICE_OTHER = "Otro"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_request)

        tvTitleAdd = findViewById(R.id.tvTitleAdd)
        etClientName = findViewById(R.id.etClientName)
        spServiceType = findViewById(R.id.spServiceType)

        tilOtherService = findViewById(R.id.tilOtherService)
        etOtherService = findViewById(R.id.etOtherService)

        etDate = findViewById(R.id.etDate)
        etDescription = findViewById(R.id.etDescription)
        btnSave = findViewById(R.id.btnSave)

        dao = AppDatabase.getInstance(this).serviceRequestDao()

        setupSpinner()
        setupDatePicker()

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
        val items = resources.getStringArray(R.array.service_types).toList()
        val adapter = ArrayAdapter(this, R.layout.spinner_item, items)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spServiceType.adapter = adapter
    }

    private fun setupDatePicker() {
        // XML tiene etDate con focusable=false y clickable=true
        // Aquí conectamos el click para abrir calendario
        etDate.setOnClickListener {
            showDatePicker()
        }
        // Accesibilidad
        etDate.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) showDatePicker()
        }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()

        // Si ya hay una fecha escrita dd/MM/yyyy, la usamos como punto de partida
        val currentText = etDate.text?.toString()?.trim().orEmpty()
        val parts = currentText.split("/")
        if (parts.size == 3) {
            val d = parts[0].toIntOrNull()
            val m = parts[1].toIntOrNull()
            val y = parts[2].toIntOrNull()
            if (d != null && m != null && y != null) {
                cal.set(Calendar.DAY_OF_MONTH, d)
                cal.set(Calendar.MONTH, m - 1)
                cal.set(Calendar.YEAR, y)
            }
        }

        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, y, m, d ->
            val formatted = String.format(Locale.getDefault(), "%02d/%02d/%04d", d, (m + 1), y)
            etDate.setText(formatted)
            etDate.error = null
            Log.d(TAG_UI, "Fecha seleccionada=$formatted")
        }, year, month, day).show()
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
                var found = false
                for (i in 0 until spinnerAdapter.count) {
                    if (spinnerAdapter.getItem(i).toString() == request.serviceType) {
                        spServiceType.setSelection(i)
                        found = true
                        break
                    }
                }

                // Si NO está en el array → usar "Otro" y rellenar
                if (!found) {
                    val otherIndex = (0 until spinnerAdapter.count)
                        .firstOrNull { spinnerAdapter.getItem(it).toString() == SERVICE_OTHER }

                    if (otherIndex != null) {
                        spServiceType.setSelection(otherIndex)
                        tilOtherService.visibility = View.VISIBLE
                        etOtherService.setText(request.serviceType)
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
            val clientName = etClientName.text?.toString()?.trim().orEmpty()
            val selectedService = spServiceType.selectedItem?.toString().orEmpty()
            val otherService = etOtherService.text?.toString()?.trim().orEmpty()
            val date = etDate.text?.toString()?.trim().orEmpty()
            val description = etDescription.text?.toString()?.trim().orEmpty()

            val finalServiceType =
                if (selectedService == SERVICE_OTHER) otherService else selectedService

            validateInputs(clientName, selectedService, otherService, date, description)

            Log.i(TAG_UI, "Guardar presionado. ModoEditar=$isEditMode serviceType=$finalServiceType")

            lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        if (isEditMode) {
                            Log.d(TAG_DB, "Room UPDATE id=$editingId")
                            dao.update(
                                ServiceRequestEntity(
                                    id = editingId!!,
                                    clientName = clientName,
                                    serviceType = finalServiceType,
                                    date = date,
                                    description = description
                                )
                            )
                        } else {
                            Log.d(TAG_DB, "Room INSERT nuevo")
                            dao.insert(
                                ServiceRequestEntity(
                                    clientName = clientName,
                                    serviceType = finalServiceType,
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

    private fun validateInputs(
        clientName: String,
        selectedService: String,
        otherService: String,
        date: String,
        description: String
    ) {
        var ok = true

        if (clientName.isBlank()) {
            etClientName.error = "Ingresa el nombre"
            ok = false
        }

        if (selectedService == SERVICE_OTHER) {
            if (otherService.isBlank()) {
                tilOtherService.error = "Especifica el servicio"
                ok = false
            } else {
                tilOtherService.error = null
            }
        } else {
            tilOtherService.error = null
        }

        if (date.isBlank()) {
            etDate.error = "Selecciona una fecha"
            ok = false
        }

        if (description.isBlank()) {
            etDescription.error = "Ingresa la descripción"
            ok = false
        }

        if (!ok) throw IllegalArgumentException("Completa los campos obligatorios")
    }
}