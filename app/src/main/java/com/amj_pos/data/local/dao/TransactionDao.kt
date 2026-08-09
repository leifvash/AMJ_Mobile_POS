package com.amj_pos.data.local.dao

import androidx.room.*
import com.amj_pos.data.local.entities.Transaction
import com.amj_pos.data.local.entities.TransactionItem
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert
    suspend fun insertTransaction(transaction: Transaction): Long

    @Insert
    suspend fun insertTransactionItems(items: List<TransactionItem>)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    /**
     * Calculates daily net profit ("Kita") for the current date.
     * Timestamp is stored in milliseconds.
     */
    @Query("""
        SELECT SUM(totalProfit) FROM transactions 
        WHERE date(timestamp / 1000, 'unixepoch', 'localtime') = date('now', 'localtime')
    """)
    fun getDailyProfit(): Flow<Double?>

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId")
    suspend fun getItemsForTransaction(transactionId: Long): List<TransactionItem>
}
