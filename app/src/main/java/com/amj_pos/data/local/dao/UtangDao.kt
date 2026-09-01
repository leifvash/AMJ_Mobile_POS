package com.amj_pos.data.local.dao

import androidx.room.*
import com.amj_pos.data.local.entities.Customer
import com.amj_pos.data.local.entities.UtangRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface UtangDao {
    @Insert
    suspend fun insertCustomer(customer: Customer): Long

    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :customerId")
    suspend fun getCustomerById(customerId: Long): Customer?

    @Insert
    suspend fun insertUtangRecord(record: UtangRecord)

    /**
     * Calculates total outstanding "utang" across all customers.
     * Sums up all amounts in the utang_records table.
     */
    @Query("SELECT SUM(amount) FROM utang_records")
    fun getTotalOutstandingUtang(): Flow<Double?>

    /**
     * Calculates outstanding "utang" for a specific customer.
     */
    @Query("SELECT SUM(amount) FROM utang_records WHERE customerId = :customerId")
    fun getCustomerBalance(customerId: Long): Flow<Double?>

    @Query("SELECT * FROM utang_records WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getUtangHistory(customerId: Long): Flow<List<UtangRecord>>

    @Query("DELETE FROM utang_records WHERE customerId = :customerId")
    suspend fun deleteUtangRecordsByCustomerId(customerId: Long)

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    @Query("DELETE FROM customers")
    suspend fun deleteAllCustomers()

    @Query("DELETE FROM utang_records")
    suspend fun deleteAllUtangRecords()
}
