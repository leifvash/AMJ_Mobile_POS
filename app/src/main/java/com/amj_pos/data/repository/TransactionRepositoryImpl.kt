package com.amj_pos.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.room.withTransaction
import androidx.work.*
import com.amj_pos.data.local.db.AppDatabase
import com.amj_pos.data.local.entities.Transaction
import com.amj_pos.data.local.entities.TransactionItem
import com.amj_pos.data.local.entities.DailyStat
import com.amj_pos.data.local.entities.UtangRecord
import com.amj_pos.data.local.entities.UtangType
import com.amj_pos.domain.repository.TransactionRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class TransactionRepositoryImpl(
    private val database: AppDatabase,
    private val firestore: FirebaseFirestore,
    private val workManager: WorkManager
) : TransactionRepository {

    private val transactionDao = database.transactionDao()
    private val productDao = database.productDao()
    private val utangDao = database.utangDao()

    override fun getDailySales(branchName: String): Flow<Double> =
        transactionDao.getDailySalesByBranch(branchName).map { it ?: 0.0 }

    override fun getDailyStats(days: Int): Flow<List<DailyStat>> {
        val since = System.currentTimeMillis() - (days.toLong() * 24 * 60 * 60 * 1000)
        return transactionDao.getDailyStats(since)
    }

    override fun getAllTransactions(): Flow<List<Transaction>> = 
        transactionDao.getAllTransactions()

    override fun getTransactionsPaged(branchName: String): Flow<PagingData<Transaction>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = {
                if (branchName.isEmpty()) transactionDao.getAllTransactionsPaging()
                else transactionDao.getTransactionsByBranchPaging(branchName)
            }
        ).flow
    }

    override fun getTransactionsByDate(dateStr: String, branchName: String): Flow<List<Transaction>> =
        transactionDao.getTransactionsByDateAndBranch(dateStr, branchName)

    override fun getTotalSalesByDate(dateStr: String, branchName: String): Flow<Double> =
        transactionDao.getTotalSalesByDateAndBranch(dateStr, branchName).map { it ?: 0.0 }

    override suspend fun getItemsForTransaction(transactionId: Long): List<TransactionItem> = 
        transactionDao.getItemsForTransaction(transactionId)

    override fun getTopSellingProducts(limit: Int): Flow<List<TransactionItem>> =
        transactionDao.getTopSellingProducts(limit)

    override fun getBranchPerformance(): Flow<List<com.amj_pos.data.local.entities.BranchPerformance>> =
        transactionDao.getBranchPerformance()

    override suspend fun executeSale(
        transaction: Transaction,
        items: List<TransactionItem>,
        isUtang: Boolean
    ) {
        database.withTransaction {
            // 1. Insert Transaction Locally
            val transactionId = transactionDao.insertTransaction(transaction)
            
            // 2. Insert Items (mapping them to the new transactionId)
            val itemsWithId = items.map { it.copy(transactionId = transactionId) }
            transactionDao.insertTransactionItems(itemsWithId)
            
            // 3. Update Stock Locally
            items.forEach { item ->
                productDao.reduceStock(item.productId, item.quantity)
            }
            
            // 4. Handle Utang (Credit)
            if (isUtang && transaction.customerId != null) {
                val utangRecord = UtangRecord(
                    customerId = transaction.customerId,
                    amount = transaction.totalAmount,
                    type = UtangType.CREDIT,
                    note = "POS Sale #${transactionId}"
                )
                utangDao.insertUtangRecord(utangRecord)
            }

            // 5. Sync to Firestore (Async)
            syncTransactionToFirestore(transaction.copy(id = transactionId), itemsWithId)
        }
    }

    private fun syncTransactionToFirestore(transaction: Transaction, items: List<TransactionItem>) {
        val data = workDataOf("transactionId" to transaction.id)
        val request = OneTimeWorkRequestBuilder<com.amj_pos.data.sync.TransactionSyncWorker>()
            .setInputData(data)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()
        
        workManager.enqueueUniqueWork("sync_transaction_${transaction.remoteId}", ExistingWorkPolicy.REPLACE, request)
    }

    override suspend fun syncTransactionsFromFirestore() {
        try {
            val snapshot = firestore.collection("transactions")
                .whereGreaterThan("timestamp", System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)) // Last 30 days
                .get()
                .await()

            database.withTransaction {
                snapshot.documents.forEach { doc ->
                    val remoteId = doc.getString("remoteId") ?: doc.id
                    val timestamp = doc.getLong("timestamp") ?: return@forEach
                    val totalAmount = doc.getDouble("totalAmount") ?: 0.0
                    val isUtang = doc.getBoolean("isUtang") ?: false
                    val customerId = doc.getLong("customerId")
                    val paymentMethodStr = doc.getString("paymentMethod") ?: "CASH"
                    val branchName = doc.getString("branchName") ?: ""

                    val transaction = Transaction(
                        remoteId = remoteId,
                        timestamp = timestamp,
                        totalAmount = totalAmount,
                        isUtang = isUtang,
                        customerId = customerId,
                        paymentMethod = com.amj_pos.data.local.entities.PaymentMethod.valueOf(paymentMethodStr),
                        branchName = branchName
                    )

                    // Insert or ignore if exists (Conflict on remoteId index)
                    val transactionId = transactionDao.insertTransaction(transaction)
                    
                    // If inserted (not a duplicate), insert items
                    if (transactionId != -1L) {
                        val itemsData = doc.get("items") as? List<Map<String, Any>>
                        val items = itemsData?.map { itemMap ->
                            com.amj_pos.data.local.entities.TransactionItem(
                                transactionId = transactionId,
                                productId = (itemMap["productId"] as? Number)?.toLong() ?: 0L,
                                productName = itemMap["productName"] as? String ?: "",
                                quantity = (itemMap["quantity"] as? Number)?.toDouble() ?: 0.0,
                                sellPrice = (itemMap["sellPrice"] as? Number)?.toDouble() ?: 0.0,
                                unitType = itemMap["unitType"] as? String ?: "Piece"
                            )
                        }
                        if (items != null) {
                            transactionDao.insertTransactionItems(items)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun voidTransaction(transaction: Transaction) {
        database.withTransaction {
            // 1. Get items to restore stock
            val items = transactionDao.getItemsForTransaction(transaction.id)
            
            // 2. Restore stock for each item
            items.forEach { item ->
                productDao.restoreStock(item.productId, item.quantity)
            }
            
            // 3. Delete from local DB
            transactionDao.deleteTransactionById(transaction.id)
            
            // 4. Delete from Firestore
            firestore.collection("transactions")
                .document(transaction.remoteId)
                .delete()
        }
    }

    override suspend fun deleteAllTransactions() {
        database.withTransaction {
            transactionDao.deleteAllTransactions()
            transactionDao.deleteAllTransactionItems()
        }
    }
}
