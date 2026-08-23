package com.amj_pos.domain.repository

import com.amj_pos.data.local.entities.Product
import com.amj_pos.data.local.entities.StockAdjustment
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getAllProducts(): Flow<List<Product>>
    fun getLowStockProducts(threshold: Int): Flow<List<Product>>
    fun searchProducts(query: String): Flow<List<Product>>
    suspend fun getProductByBarcode(barcode: String): Product?
    suspend fun getProductById(id: Long): Product?
    suspend fun upsertProduct(product: Product)
    suspend fun archiveProduct(productId: Long)
    suspend fun deleteProduct(product: Product)

    // Stock Adjustments
    suspend fun adjustStock(adjustment: StockAdjustment)
    fun getStockAdjustments(productId: Long): Flow<List<StockAdjustment>>
    
    suspend fun deleteAllProducts()
    suspend fun syncFromFirestore()
}
