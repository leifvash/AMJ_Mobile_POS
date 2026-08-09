package com.amj_pos.data.local.dao

import androidx.room.*
import com.amj_pos.data.local.entities.Transaction
import com.amj_pos.data.local.entities.TransactionItem
import com.amj_pos.data.local.entities.DailyStat
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert
    suspend fun insertTransaction(transaction: Transaction): Long

    @Insert
    suspend fun insertTransactionItems(items: List<TransactionItem>)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE date(timestamp / 1000, 'unixepoch', 'localtime') = :dateStr ORDER BY timestamp DESC")
    fun getTransactionsByDate(dateStr: String): Flow<List<Transaction>>

    @Query("SELECT SUM(totalAmount) FROM transactions WHERE date(timestamp / 1000, 'unixepoch', 'localtime') = :dateStr")
    fun getTotalSalesByDate(dateStr: String): Flow<Double?>

    @Query("""
        SELECT date(timestamp / 1000, 'unixepoch', 'localtime') as date, 
               SUM(totalAmount) as totalSales, 
               SUM(totalProfit) as totalProfit 
        FROM transactions 
        WHERE timestamp >= :since 
        GROUP BY date 
        ORDER BY date ASC
    """)
    fun getDailyStats(since: Long): Flow<List<DailyStat>>

    /**
     * Calculates daily net profit ("Kita") for the current date.
     * Timestamp is stored in milliseconds.
     */
    @Query("SELECT SUM(totalProfit) FROM transactions WHERE date(timestamp / 1000, 'unixepoch', 'localtime') = date('now', 'localtime')")
    fun getDailyProfit(): Flow<Double?>

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId")
    suspend fun getItemsForTransaction(transactionId: Long): List<TransactionItem>

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("DELETE FROM transaction_items")
    suspend fun deleteAllTransactionItems()
}
