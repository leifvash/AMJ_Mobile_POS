package com.amj_pos.data.repository

import androidx.room.withTransaction
import androidx.work.*
import com.amj_pos.data.local.dao.ProductDao
import com.amj_pos.data.local.dao.StockAdjustmentDao
import com.amj_pos.data.local.db.AppDatabase
import com.amj_pos.data.local.entities.Product
import com.amj_pos.data.local.entities.StockAdjustment
import com.amj_pos.domain.repository.ProductRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class ProductRepositoryImpl(
    private val database: AppDatabase,
    private val firestore: FirebaseFirestore,
    private val workManager: WorkManager
) : ProductRepository {
    private val productDao = database.productDao()
    private val stockAdjustmentDao = database.stockAdjustmentDao()

    override fun getAllProducts(branchName: String): Flow<List<Product>> = productDao.getAllProducts(branchName)
    
    override fun getLowStockProducts(threshold: Int, branchName: String): Flow<List<Product>> = productDao.getLowStockProducts(branchName, threshold)

    override fun searchProducts(query: String, branchName: String): Flow<List<Product>> = productDao.searchProducts(branchName, query)

    override suspend fun getProductByBarcode(barcode: String): Product? = 
        productDao.getProductByBarcode(barcode)

    override suspend fun getProductById(id: Long): Product? = 
        productDao.getProductById(id)

    override suspend fun upsertProduct(product: Product) {
        val id = productDao.insertProduct(product)
        val productWithId = if (product.id == 0L) product.copy(id = id) else product
        syncProductToFirestore(productWithId)
    }

    override suspend fun archiveProduct(productId: Long) {
        productDao.archiveProduct(productId)
        firestore.collection("products").document(productId.toString()).update("isArchived", true)
    }

    override suspend fun deleteProduct(product: Product) {
        productDao.deleteProduct(product)
        firestore.collection("products").document(product.id.toString()).delete()
    }

    override suspend fun adjustStock(adjustment: StockAdjustment) {
        database.withTransaction {
            stockAdjustmentDao.insertAdjustment(adjustment)
            productDao.restoreStock(adjustment.productId, adjustment.adjustmentAmount)
        }
        // Sync product to firestore
        val product = productDao.getProductById(adjustment.productId)
        if (product != null) syncProductToFirestore(product)
    }

    override fun getStockAdjustments(productId: Long): Flow<List<StockAdjustment>> = 
        stockAdjustmentDao.getAdjustmentsForProduct(productId)

    private fun syncProductToFirestore(product: Product) {
        val data = workDataOf("productId" to product.id)
        val request = OneTimeWorkRequestBuilder<com.amj_pos.data.sync.ProductSyncWorker>()
            .setInputData(data)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()
        
        workManager.enqueueUniqueWork("sync_product_${product.id}", ExistingWorkPolicy.REPLACE, request)
    }

    override suspend fun deleteAllProducts() {
        productDao.deleteAllProducts()
    }

    override suspend fun syncFromFirestore() {
        try {
            val snapshot = firestore.collection("products").get().await()
            val remoteProducts = snapshot.toObjects(Product::class.java)
            remoteProducts.forEach { product ->
                productDao.insertProduct(product)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
