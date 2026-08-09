package com.amj_pos.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transaction_items",
    foreignKeys = [
        ForeignKey(
            entity = Transaction::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["transactionId"]),
        Index(value = ["productId"])
    ]
)
data class TransactionItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: Long,
    val productId: Long,
    val productName: String, // Denormalized for receipt history
    val quantity: Int,
    val sellPricePerPiece: Double,
    val costPricePerPiece: Double,
    val profit: Double // (sellPrice - costPrice) * quantity
)
