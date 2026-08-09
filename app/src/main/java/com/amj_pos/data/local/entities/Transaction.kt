package com.amj_pos.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val totalAmount: Double,
    val totalProfit: Double, // The "Kita" for this entire transaction
    val isUtang: Boolean = false,
    val customerId: Long? = null // Nullable if it's a guest walk-in
)
