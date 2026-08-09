package com.amj_pos.data.repository

import com.amj_pos.data.local.dao.ProductDao
import com.amj_pos.data.local.entities.Product
import com.amj_pos.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow

class ProductRepositoryImpl(private val productDao: ProductDao) : ProductRepository {
    override fun getAllProducts(): Flow<List<Product>> = productDao.getAllProducts()

    override suspend fun getProductByBarcode(barcode: String): Product? = 
        productDao.getProductByBarcode(barcode)

    override suspend fun getProductById(id: Long): Product? = 
        productDao.getProductById(id)

    override suspend fun upsertProduct(product: Product) {
        productDao.insertProduct(product)
    }

    override suspend fun deleteProduct(product: Product) {
        productDao.deleteProduct(product)
    }

    override suspend fun addBulkStock(productId: Long, bulkQuantity: Int) {
        val product = productDao.getProductById(productId) ?: return
        val piecesToAdd = bulkQuantity * product.piecesPerBulk
        val updatedProduct = product.copy(
            currentStockInPieces = product.currentStockInPieces + piecesToAdd,
            updatedAt = System.currentTimeMillis()
        )
        productDao.updateProduct(updatedProduct)
    }
}
