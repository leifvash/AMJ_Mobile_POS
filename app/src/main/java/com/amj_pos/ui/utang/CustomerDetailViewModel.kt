package com.amj_pos.ui.utang

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amj_pos.data.local.entities.UtangRecord
import com.amj_pos.domain.repository.UtangRepository
import kotlinx.coroutines.flow.*

data class CustomerDetailUiState(
    val history: List<UtangRecord> = emptyList(),
    val balance: Double = 0.0,
    val isLoading: Boolean = false
)

class CustomerDetailViewModel(
    private val utangRepository: UtangRepository,
    private val customerId: Long
) : ViewModel() {

    val uiState: StateFlow<CustomerDetailUiState> = combine(
        utangRepository.getUtangHistory(customerId),
        utangRepository.getCustomerBalance(customerId)
    ) { history, balance ->
        CustomerDetailUiState(
            history = history,
            balance = balance,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CustomerDetailUiState(isLoading = true)
    )
}
