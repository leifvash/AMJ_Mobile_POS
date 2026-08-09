package com.amj_pos.data.repository

import androidx.room.withTransaction
import com.amj_pos.data.local.db.AppDatabase
import com.amj_pos.data.local.entities.Transaction
import com.amj_pos.data.local.entities.TransactionItem
import com.amj_pos.data.local.entities.DailyStat
import com.amj_pos.data.local.entities.UtangRecord
import com.amj_pos.data.local.entities.UtangType
import com.amj_pos.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRepositoryImpl(
    private val database: AppDatabase
) : TransactionRepository {

    private val transactionDao = database.transactionDao()
    private val productDao = database.productDao()
    private val utangDao = database.utangDao()

    override fun getDailyProfit(): Flow<Double> = 
        transactionDao.getDailyProfit().map { it ?: 0.0 }

    override fun getDailyStats(days: Int): Flow<List<DailyStat>> {
        val since = System.currentTimeMillis() - (days.toLong() * 24 * 60 * 60 * 1000)
        return transactionDao.getDailyStats(since)
    }

    override fun getAllTransactions(): Flow<List<Transaction>> = 
        transactionDao.getAllTransactions()

    override fun getTransactionsByDate(dateStr: String): Flow<List<Transaction>> =
        transactionDao.getTransactionsByDate(dateStr)

    override fun getTotalSalesByDate(dateStr: String): Flow<Double> =
        transactionDao.getTotalSalesByDate(dateStr).map { it ?: 0.0 }

    override suspend fun getItemsForTransaction(transactionId: Long): List<TransactionItem> = 
        transactionDao.getItemsForTransaction(transactionId)

    override suspend fun executeSale(
        transaction: Transaction,
        items: List<TransactionItem>,
        isUtang: Boolean
    ) {
        database.withTransaction {
            // 1. Insert Transaction
            val transactionId = transactionDao.insertTransaction(transaction)
            
            // 2. Insert Items (mapping them to the new transactionId)
            val itemsWithId = items.map { it.copy(transactionId = transactionId) }
            transactionDao.insertTransactionItems(itemsWithId)
            
            // 3. Update Stock
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
        }
    }

    override suspend fun deleteAllTransactions() {
        database.withTransaction {
            transactionDao.deleteAllTransactions()
            transactionDao.deleteAllTransactionItems()
        }
    }
}
