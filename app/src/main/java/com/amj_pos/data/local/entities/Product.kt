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
    
    // Cost calculation (Bulk)
    val bulkCostPrice: Double, // Price paid to supplier for the bulk unit (e.g., box)
    val piecesPerBulk: Int,    // How many individual items are in one bulk unit
    
    // Retail calculation (Piece)
    val pieceRetailPrice: Double, // Selling price per individual piece
    
    // Inventory
    val currentStockInPieces: Int, // Total remaining pieces in stock
    
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Calculates the cost price per individual piece.
     * This is used to calculate "Kita" (Net Profit) per sale.
     */
    val costPricePerPiece: Double
        get() = if (piecesPerBulk > 0) bulkCostPrice / piecesPerBulk else 0.0
}
