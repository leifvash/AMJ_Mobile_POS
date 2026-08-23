package com.amj_pos.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_adjustments",
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["productId"])]
)
data class StockAdjustment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val adjustmentAmount: Double, // can be negative (spoilage) or positive (restock)
    val reason: String, // "Restock", "Spoilage", "Consumption", "Correction"
    val timestamp: Long = System.currentTimeMillis(),
    val branchName: String
)
