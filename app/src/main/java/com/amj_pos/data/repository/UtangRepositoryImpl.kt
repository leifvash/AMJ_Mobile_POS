package com.amj_pos.data.repository

import com.amj_pos.data.local.dao.UtangDao
import com.amj_pos.data.local.entities.Customer
import com.amj_pos.data.local.entities.UtangRecord
import com.amj_pos.data.local.entities.UtangType
import com.amj_pos.domain.repository.UtangRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UtangRepositoryImpl(private val utangDao: UtangDao) : UtangRepository {
    override fun getTotalOutstandingUtang(): Flow<Double> = 
        utangDao.getTotalOutstandingUtang().map { it ?: 0.0 }

    override fun getCustomers(): Flow<List<Customer>> = utangDao.getAllCustomers()

    override fun getCustomerBalance(customerId: Long): Flow<Double> = 
        utangDao.getCustomerBalance(customerId).map { it ?: 0.0 }

    override fun getUtangHistory(customerId: Long): Flow<List<UtangRecord>> = 
        utangDao.getUtangHistory(customerId)

    override suspend fun addCustomer(customer: Customer): Long = 
        utangDao.insertCustomer(customer)

    override suspend fun recordPayment(customerId: Long, amount: Double, note: String?) {
        // Payment reduces debt, so we record it as a negative value if the balance is positive
        // But usually, amount passed is positive, so we store it as negative in ledger
        val record = UtangRecord(
            customerId = customerId,
            amount = -kotlin.math.abs(amount),
            type = UtangType.PAYMENT,
            note = note
        )
        utangDao.insertUtangRecord(record)
    }

    override suspend fun recordCredit(customerId: Long, amount: Double, note: String?) {
        val record = UtangRecord(
            customerId = customerId,
            amount = kotlin.math.abs(amount),
            type = UtangType.CREDIT,
            note = note
        )
        utangDao.insertUtangRecord(record)
    }
}
