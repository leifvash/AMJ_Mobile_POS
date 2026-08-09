package com.amj_pos.domain.repository

import com.amj_pos.data.local.entities.Transaction
import com.amj_pos.data.local.entities.TransactionItem
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getDailyProfit(): Flow<Double>
    fun getAllTransactions(): Flow<List<Transaction>>
    suspend fun getItemsForTransaction(transactionId: Long): List<TransactionItem>
    
    /**
     * Executes a sale. 
     * 1. Inserts Transaction record.
     * 2. Inserts TransactionItems.
     * 3. Updates Product stock.
     * 4. If it's an 'Utang' (credit), updates customer balance.
     */
    suspend fun executeSale(
        transaction: Transaction,
        items: List<TransactionItem>,
        isUtang: Boolean = false
    )
}
