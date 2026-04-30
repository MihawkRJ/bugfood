package com.bugfood.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bugfood.database.DeliveryEntity
import com.bugfood.databinding.ItemDeliveryBinding
import java.text.SimpleDateFormat
import java.util.*

class DeliveryAdapter(
    private val onDelete: (DeliveryEntity) -> Unit,
    private val onEditCode: (DeliveryEntity) -> Unit
) : ListAdapter<DeliveryEntity, DeliveryAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<DeliveryEntity>() {
            override fun areItemsTheSame(a: DeliveryEntity, b: DeliveryEntity) = a.id == b.id
            override fun areContentsTheSame(a: DeliveryEntity, b: DeliveryEntity) = a == b
        }
        private val DATE_FMT = SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR"))
    }

    inner class ViewHolder(private val binding: ItemDeliveryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entity: DeliveryEntity) {
            binding.apply {
                tvCustomerName.text = entity.customerName
                tvPickupCode.text = "🔑 Coleta: ${entity.pickupCode}"
                tvDeliveryCode.text = if (entity.deliveryCode.isNotBlank())
                    "📦 Entrega: ${entity.deliveryCode}" else "📦 Entrega: —"
                tvUsageCount.text = "⚡ Auto-fill: ${entity.usageCount}x"
                tvDate.text = DATE_FMT.format(Date(entity.lastUsedAt))

                btnDelete.setOnClickListener { onDelete(entity) }
                btnEditCode.setOnClickListener { onEditCode(entity) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDeliveryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
