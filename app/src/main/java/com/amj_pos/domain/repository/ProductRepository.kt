package com.amj_pos.domain.repository

import com.amj_pos.data.local.entities.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getAllProducts(): Flow<List<Product>>
    suspend fun getProductByBarcode(barcode: String): Product?
    suspend fun getProductById(id: Long): Product?
    suspend fun upsertProduct(product: Product)
    suspend fun deleteProduct(product: Product)
    
    /**
     * Converts bulk units (boxes/packs) to pieces and adds to inventory.
     */
    suspend fun addBulkStock(productId: Long, bulkQuantity: Int)
}
