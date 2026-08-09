package com.amj_pos.data.local.dao

import androidx.room.*
import com.amj_pos.data.local.entities.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchProducts(query: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): Product?

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Long): Product?

    @Query("SELECT * FROM products WHERE currentStockInPieces < :threshold ORDER BY currentStockInPieces ASC")
    fun getLowStockProducts(threshold: Int): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("UPDATE products SET currentStockInPieces = currentStockInPieces - :quantity WHERE id = :productId")
    suspend fun reduceStock(productId: Long, quantity: Int)

    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()
}
