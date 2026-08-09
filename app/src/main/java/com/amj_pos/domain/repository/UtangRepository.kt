package com.amj_pos.domain.repository

import com.amj_pos.data.local.entities.Customer
import com.amj_pos.data.local.entities.UtangRecord
import kotlinx.coroutines.flow.Flow

interface UtangRepository {
    fun getTotalOutstandingUtang(): Flow<Double>
    fun getCustomers(): Flow<List<Customer>>
    fun getCustomerBalance(customerId: Long): Flow<Double>
    fun getUtangHistory(customerId: Long): Flow<List<UtangRecord>>
    
    suspend fun addCustomer(customer: Customer): Long
    suspend fun recordPayment(customerId: Long, amount: Double, note: String?)
    suspend fun recordCredit(customerId: Long, amount: Double, note: String?)
}
