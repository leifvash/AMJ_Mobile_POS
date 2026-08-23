package com.amj_pos.domain.repository

import androidx.paging.PagingData
import com.amj_pos.data.local.entities.Transaction
import com.amj_pos.data.local.entities.TransactionItem
import com.amj_pos.data.local.entities.DailyStat
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getDailySales(branchName: String = ""): Flow<Double>
    fun getDailyStats(days: Int): Flow<List<DailyStat>>
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getTransactionsPaged(branchName: String = ""): Flow<PagingData<Transaction>>
    fun getTransactionsByDate(dateStr: String, branchName: String = ""): Flow<List<Transaction>>
    fun getTotalSalesByDate(dateStr: String, branchName: String = ""): Flow<Double>
    suspend fun getItemsForTransaction(transactionId: Long): List<TransactionItem>
    fun getTopSellingProducts(limit: Int): Flow<List<TransactionItem>>
    fun getBranchPerformance(): Flow<List<com.amj_pos.data.local.entities.BranchPerformance>>
    
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

    suspend fun deleteAllTransactions()

    suspend fun voidTransaction(transaction: Transaction)

    suspend fun syncTransactionsFromFirestore()
}
