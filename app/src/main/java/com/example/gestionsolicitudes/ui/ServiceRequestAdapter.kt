package com.example.gestionsolicitudes.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gestionsolicitudes.R
import com.example.gestionsolicitudes.data.ServiceRequestEntity

class ServiceRequestAdapter(
    private val items: MutableList<ServiceRequestEntity>,
    private val onItemClick: (item: ServiceRequestEntity) -> Unit,
    private val onDeleteClick: (item: ServiceRequestEntity) -> Unit
) : RecyclerView.Adapter<ServiceRequestAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvClient: TextView = itemView.findViewById(R.id.tvClient)
        val tvTypeDate: TextView = itemView.findViewById(R.id.tvTypeDate)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        val root: View = itemView
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

        holder.root.setOnClickListener {
            onItemClick(item)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun setItems(newItems: List<ServiceRequestEntity>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}