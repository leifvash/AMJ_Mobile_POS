package com.amj_pos.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amj_pos.data.local.entities.Product
import com.amj_pos.domain.repository.AuthRepository
import com.amj_pos.domain.repository.ProductRepository
import com.amj_pos.domain.scanner.BarcodeScanner
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class InventoryUiState(
    val products: List<Product> = emptyList(),
    val selectedBranch: String = "",
    val isLoading: Boolean = false,
    val userRole: String = "employee",
    val inventoryPassword: String = "",
    val error: String? = null
)

class InventoryViewModel(
    private val productRepository: ProductRepository,
    private val authRepository: AuthRepository,
    private val barcodeScanner: BarcodeScanner
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedBranchFilter = MutableStateFlow("")
    val selectedBranchFilter: StateFlow<String> = _selectedBranchFilter

    private val _userRole = MutableStateFlow("employee")

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _userRole.value = user?.role?.lowercase() ?: "employee"
            }
        }
        
        viewModelScope.launch {
            authRepository.selectedBranch.collect { branch ->
                if (_userRole.value == "employee") {
                    _selectedBranchFilter.value = branch ?: ""
                }
            }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<InventoryUiState> = combine(
        _searchQuery,
        _selectedBranchFilter,
        _userRole,
        authRepository.getInventoryPassword()
    ) { query, branch, role, password ->
        Quad(query, branch, role, password)
    }.flatMapLatest { quad ->
        val (query, branch, role, password) = quad
        val productsFlow = if (query.isBlank()) {
            productRepository.getAllProducts(branchName = branch)
        } else {
            productRepository.searchProducts(query = query, branchName = branch)
        }

        productsFlow.map { products ->
            InventoryUiState(
                products = products,
                selectedBranch = branch,
                userRole = role,
                inventoryPassword = password,
                isLoading = false
            )
        }
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InventoryUiState(isLoading = true)
    )

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onBranchSelected(branch: String) {
        if (_userRole.value == "owner") {
            _selectedBranchFilter.value = branch
        }
    }

    private data class Quad<T1, T2, T3, T4>(val first: T1, val second: T2, val third: T3, val fourth: T4)

    fun seedMockData() {
        viewModelScope.launch {
            val mockProducts = listOf(
                Product(name = "Kopiko Brown Sachet", barcode = "123456", unitName = "Pack", piecesPerUnit = 12, unitPrice = 100.0, currentStock = 4.0),
                Product(name = "Lucky Me! Pancit Canton", barcode = "789012", unitName = "Box", piecesPerUnit = 50, unitPrice = 600.0, currentStock = 2.0),
                Product(name = "Coca-Cola 290ml", barcode = "345678", unitName = "Case", piecesPerUnit = 12, unitPrice = 240.0, currentStock = 2.0),
                Product(name = "Silver Swan Soy Sauce", barcode = "901234", unitName = "Pack", piecesPerUnit = 10, unitPrice = 150.0, currentStock = 2.0)
            )
            mockProducts.forEach { productRepository.upsertProduct(it) }
        }
    }

    fun archiveProduct(productId: Long) {
        viewModelScope.launch {
            productRepository.archiveProduct(productId)
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            productRepository.deleteProduct(product)
        }
    }
}
