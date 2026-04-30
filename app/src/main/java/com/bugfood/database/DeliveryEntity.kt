package com.bugfood.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "deliveries",
    indices = [Index(value = ["customerName"], unique = false)]
)
data class DeliveryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Nome do cliente/pedido detectado no iFood */
    val customerName: String,

    /** Código de coleta (pickup code) */
    val pickupCode: String,

    /** Código de entrega (delivery code) – preenchido após entrega */
    val deliveryCode: String = "",

    /** Timestamp da primeira captura */
    val createdAt: Long = System.currentTimeMillis(),

    /** Última vez que foi usado */
    val lastUsedAt: Long = System.currentTimeMillis(),

    /** Número de vezes que foi auto-preenchido */
    val usageCount: Int = 0,

    /** Nome do pacote do app (sempre iFood Entregadores) */
    val sourceApp: String = "br.com.iFood.deliveryApp"
)
