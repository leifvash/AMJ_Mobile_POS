package com.amj_pos.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.amj_pos.data.local.entities.Transaction
import com.amj_pos.data.local.entities.TransactionItem
import com.amj_pos.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class TransactionHistoryUiState(
    val selectedDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
    val selectedBranch: String = "",
    val transactions: List<Transaction> = emptyList(),
    val totalSales: Double = 0.0,
    val isLoading: Boolean = false,
    val userRole: String = "employee"
)

class TransactionHistoryViewModel(
    private val transactionRepository: TransactionRepository,
    private val authRepository: com.amj_pos.domain.repository.AuthRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    )
    val selectedDate: StateFlow<String> = _selectedDate

    private val _selectedBranchFilter = MutableStateFlow("")
    val selectedBranchFilter: StateFlow<String> = _selectedBranchFilter

    private val _userRole = MutableStateFlow("employee")

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                val role = user?.role?.lowercase() ?: "employee"
                _userRole.value = role
                if (role == "employee") {
                    _selectedBranchFilter.value = user?.assigned_branch ?: ""
                } else {
                    // Owners default to all branches, or keep current filter
                }
            }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TransactionHistoryUiState> = combine(
        _selectedDate,
        _selectedBranchFilter,
        _userRole
    ) { date, branch, role ->
        Triple(date, branch, role)
    }.flatMapLatest { (date, branch, role) ->
        combine(
            transactionRepository.getTransactionsByDate(date, branch),
            transactionRepository.getTotalSalesByDate(date, branch)
        ) { transactions, total ->
            TransactionHistoryUiState(
                selectedDate = date,
                selectedBranch = branch,
                transactions = transactions,
                totalSales = total,
                isLoading = false,
                userRole = role
            )
        }
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionHistoryUiState(isLoading = true)
    )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val pagedTransactions: Flow<PagingData<Transaction>> = _selectedBranchFilter
        .flatMapLatest { branch ->
            transactionRepository.getTransactionsPaged(branch)
        }
        .cachedIn(viewModelScope)

    fun onDateSelected(date: String) {
        _selectedDate.value = date
    }

    fun onBranchSelected(branch: String) {
        if (_userRole.value == "owner") {
            _selectedBranchFilter.value = branch
        }
    }

    fun voidTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.voidTransaction(transaction)
        }
    }

    suspend fun getTransactionItems(transactionId: Long): List<TransactionItem> {
        return transactionRepository.getItemsForTransaction(transactionId)
    }
}
