package com.amj_pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.paging.PagingSource
import com.amj_pos.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Insert
    suspend fun insertTransactionItems(items: List<TransactionItem>)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE branchName = :branchName ORDER BY timestamp DESC")
    fun getTransactionsByBranchPaging(branchName: String): PagingSource<Int, Transaction>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsPaging(): PagingSource<Int, Transaction>

    @Query("SELECT * FROM transactions WHERE date(timestamp / 1000, 'unixepoch', 'localtime') = :dateStr AND (:branchName = '' OR branchName = :branchName) ORDER BY timestamp DESC")
    fun getTransactionsByDateAndBranch(dateStr: String, branchName: String): Flow<List<Transaction>>

    @Query("SELECT SUM(totalAmount) FROM transactions WHERE date(timestamp / 1000, 'unixepoch', 'localtime') = :dateStr AND (:branchName = '' OR branchName = :branchName)")
    fun getTotalSalesByDateAndBranch(dateStr: String, branchName: String): Flow<Double?>

    @Query("SELECT SUM(totalAmount) FROM transactions WHERE date(timestamp / 1000, 'unixepoch', 'localtime') = date('now', 'localtime') AND (:branchName = '' OR branchName = :branchName)")
    fun getDailySalesByBranch(branchName: String): Flow<Double?>

    @Query("""
        SELECT date(timestamp / 1000, 'unixepoch', 'localtime') as date, 
               SUM(totalAmount) as totalSales 
        FROM transactions 
        WHERE timestamp >= :since 
        GROUP BY date 
        ORDER BY date ASC
    """)
    fun getDailyStats(since: Long): Flow<List<DailyStat>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): Transaction?

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId")
    suspend fun getItemsForTransaction(transactionId: Long): List<TransactionItem>

    @Query("""
        SELECT productId, productName, SUM(quantity) as quantity, sellPrice, unitName, unitType, 0 as id, 0 as transactionId
        FROM transaction_items 
        GROUP BY productId 
        ORDER BY quantity DESC 
        LIMIT :limit
    """)
    fun getTopSellingProducts(limit: Int): Flow<List<TransactionItem>>

    @Query("SELECT branchName, SUM(totalAmount) as totalSales FROM transactions GROUP BY branchName")
    fun getBranchPerformance(): Flow<List<BranchPerformance>>

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteTransactionById(transactionId: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("DELETE FROM transaction_items")
    suspend fun deleteAllTransactionItems()
}
