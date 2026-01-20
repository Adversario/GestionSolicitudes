package com.example.gestionsolicitudes

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class AddRequestActivity : AppCompatActivity() {

    private lateinit var etClientName: TextInputEditText
    private lateinit var spServiceType: Spinner
    private lateinit var etDate: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var btnSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_request)

        etClientName = findViewById(R.id.etClientName)
        spServiceType = findViewById(R.id.spServiceType)
        etDate = findViewById(R.id.etDate)
        etDescription = findViewById(R.id.etDescription)
        btnSave = findViewById(R.id.btnSave)

        setupSpinner()

        btnSave.setOnClickListener {
            val clientName = etClientName.text?.toString()?.trim().orEmpty()
            val serviceType = spServiceType.selectedItem?.toString().orEmpty()
            val date = etDate.text?.toString()?.trim().orEmpty()
            val description = etDescription.text?.toString()?.trim().orEmpty()

            if (clientName.isBlank() || date.isBlank() || description.isBlank()) {
                if (clientName.isBlank()) etClientName.error = "Ingresa el nombre"
                if (date.isBlank()) etDate.error = "Ingresa la fecha"
                if (description.isBlank()) etDescription.error = "Ingresa la descripción"
                return@setOnClickListener
            }

            val data = Intent().apply {
                putExtra(EXTRA_CLIENT, clientName)
                putExtra(EXTRA_TYPE, serviceType)
                putExtra(EXTRA_DATE, date)
                putExtra(EXTRA_DESC, description)
            }

            setResult(Activity.RESULT_OK, data)
            finish()
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

    companion object {
        const val EXTRA_CLIENT = "extra_client"
        const val EXTRA_TYPE = "extra_type"
        const val EXTRA_DATE = "extra_date"
        const val EXTRA_DESC = "extra_desc"
    }
}