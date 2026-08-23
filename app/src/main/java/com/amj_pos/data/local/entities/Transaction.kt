package com.amj_pos.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["remoteId"], unique = true)]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteId: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val totalAmount: Double,
    val isUtang: Boolean = false,
    val customerId: Long? = null, // Nullable if it's a guest walk-in
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val branchName: String = ""
)

enum class PaymentMethod {
    CASH,
    QRPH,
    UTANG
}
