package com.example.gestionsolicitudes.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gestionsolicitudes.R
import com.example.gestionsolicitudes.model.ServiceRequest

class ServiceRequestAdapter(
    private val items: MutableList<ServiceRequest>,
    private val onDeleteClick: (position: Int) -> Unit
) : RecyclerView.Adapter<ServiceRequestAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvClient: TextView = itemView.findViewById(R.id.tvClient)
        val tvTypeDate: TextView = itemView.findViewById(R.id.tvTypeDate)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_service_request, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvClient.text = "Cliente: ${item.clientName}"
        holder.tvTypeDate.text = "${item.serviceType} • ${item.date}"
        holder.tvDescription.text = item.description

        holder.btnDelete.setOnClickListener {
            onDeleteClick(position)
        }
    }

    override fun getItemCount(): Int = items.size

    fun removeAt(position: Int) {
        if (position in 0 until items.size) {
            items.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, items.size)
        }
    }
}