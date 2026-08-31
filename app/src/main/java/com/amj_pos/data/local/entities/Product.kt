package com.amj_pos.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a product in the store.
 * Supports "Tingoting" (bulk-to-piece splitting).
 */
@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val barcode: String?,
    val name: String,
    
    // Wholesale Logic
    val unitName: String = "Case", // e.g., "Case", "Pack", "Box"
    val piecesPerUnit: Int = 1,    // e.g., 12 pieces in a case
    val unitPrice: Double,         // Price for the whole unit
    
    // Inventory
    val currentStock: Double, // Total remaining units (e.g. 5.5 Cases)
    
    val category: String = "Uncategorized",
    val branchName: String = "", // Mambuaya or Bayanga
    val isArchived: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
