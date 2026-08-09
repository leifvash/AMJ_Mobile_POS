package com.amj_pos.data.repository

import androidx.room.withTransaction
import com.amj_pos.data.local.db.AppDatabase
import com.amj_pos.data.local.entities.Transaction
import com.amj_pos.data.local.entities.TransactionItem
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

    override fun getAllTransactions(): Flow<List<Transaction>> = 
        transactionDao.getAllTransactions()

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
}
