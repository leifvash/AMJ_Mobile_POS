package com.amj_pos.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amj_pos.data.local.entities.Transaction
import com.amj_pos.data.local.entities.TransactionItem
import com.amj_pos.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class TransactionHistoryUiState(
    val selectedDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
    val transactions: List<Transaction> = emptyList(),
    val totalSales: Double = 0.0,
    val isLoading: Boolean = false
)

class TransactionHistoryViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    )
    val selectedDate: StateFlow<String> = _selectedDate

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TransactionHistoryUiState> = _selectedDate
        .flatMapLatest { date ->
            combine(
                transactionRepository.getTransactionsByDate(date),
                transactionRepository.getTotalSalesByDate(date)
            ) { transactions, total ->
                TransactionHistoryUiState(
                    selectedDate = date,
                    transactions = transactions,
                    totalSales = total,
                    isLoading = false
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TransactionHistoryUiState(isLoading = true)
        )

    fun onDateSelected(date: String) {
        _selectedDate.value = date
    }

    suspend fun getTransactionItems(transactionId: Long): List<TransactionItem> {
        return transactionRepository.getItemsForTransaction(transactionId)
    }
}
