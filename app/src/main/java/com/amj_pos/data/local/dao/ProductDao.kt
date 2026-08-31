package com.amj_pos.data.local.dao

import androidx.room.*
import com.amj_pos.data.local.entities.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE isArchived = 0 AND (:branchName = '' OR branchName = :branchName) ORDER BY name ASC")
    fun getAllProducts(branchName: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE isArchived = 0 AND (:branchName = '' OR branchName = :branchName) AND (name LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%') ORDER BY name ASC")
    fun searchProducts(branchName: String, query: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): Product?

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Long): Product?

    @Query("SELECT * FROM products WHERE isArchived = 0 AND (:branchName = '' OR branchName = :branchName) AND currentStock < :threshold ORDER BY currentStock ASC")
    fun getLowStockProducts(branchName: String, threshold: Int): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Update
    suspend fun updateProduct(product: Product)

    @Query("UPDATE products SET isArchived = 1 WHERE id = :productId")
    suspend fun archiveProduct(productId: Long)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("UPDATE products SET currentStock = currentStock - :quantity WHERE id = :productId")
    suspend fun reduceStock(productId: Long, quantity: Double)

    @Query("UPDATE products SET currentStock = currentStock + :quantity WHERE id = :productId")
    suspend fun restoreStock(productId: Long, quantity: Double)

    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()
}
