package com.amj_pos.ui.utang

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amj_pos.data.local.entities.Customer
import com.amj_pos.domain.repository.UtangRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UtangUiState(
    val customers: List<Customer> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class UtangViewModel(
    private val utangRepository: UtangRepository
) : ViewModel() {

    val uiState: StateFlow<UtangUiState> = utangRepository.getCustomers()
        .map { customers -> UtangUiState(customers = customers) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UtangUiState(isLoading = true)
        )

    fun addCustomer(name: String, phone: String?) {
        viewModelScope.launch {
            utangRepository.addCustomer(Customer(name = name, phoneNumber = phone))
        }
    }

    fun recordPayment(customerId: Long, amount: Double, note: String?) {
        viewModelScope.launch {
            utangRepository.recordPayment(customerId, amount, note)
        }
    }

    fun getCustomerBalance(customerId: Long): Flow<Double> {
        return utangRepository.getCustomerBalance(customerId)
    }
}
