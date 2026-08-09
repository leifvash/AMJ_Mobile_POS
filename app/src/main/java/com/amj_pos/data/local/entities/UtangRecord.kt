package com.amj_pos.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "utang_records",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["customerId"])]
)
data class UtangRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val amount: Double, // Positive for new debt, negative for payments
    val type: UtangType,
    val note: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class UtangType {
    CREDIT,   // Increased debt
    PAYMENT   // Partial or full payment
}
