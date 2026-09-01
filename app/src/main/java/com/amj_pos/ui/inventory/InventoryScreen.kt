package com.amj_pos.ui.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amj_pos.data.local.entities.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAddProduct: () -> Unit,
    onNavigateToProductDetail: (Long) -> Unit,
    userRole: String = "employee"
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    var showPasswordDialog by remember { mutableStateOf<Long?>(null) }
    var productToArchive by remember { mutableStateOf<Product?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Inventory")
                        Text(
                            text = if (uiState.selectedBranch.isEmpty()) "All Branches" else uiState.selectedBranch,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.userRole == "owner") {
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Filter Branch")
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                DropdownMenuItem(text = { Text("All Branches") }, onClick = { viewModel.onBranchSelected(""); expanded = false })
                                DropdownMenuItem(text = { Text("Mambuaya") }, onClick = { viewModel.onBranchSelected("Mambuaya"); expanded = false })
                                DropdownMenuItem(text = { Text("Bayanga") }, onClick = { viewModel.onBranchSelected("Bayanga"); expanded = false })
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                if (uiState.userRole == "owner") {
                    onNavigateToAddProduct()
                } else {
                    showPasswordDialog = -1L // Signal for Add Product
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Product")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                label = { Text("Search Products") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
            )

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.products.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (searchQuery.isBlank()) "No products in inventory." else "No products matching \"$searchQuery\"")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.products, key = { it.id }) { product ->
                        ProductItem(
                            product = product,
                            onDelete = { 
                                if (uiState.userRole == "owner") {
                                    productToArchive = product
                                }
                            },
                            onClick = { 
                                if (uiState.userRole == "owner") {
                                    onNavigateToProductDetail(product.id)
                                } else {
                                    showPasswordDialog = product.id
                                }
                            },
                            userRole = uiState.userRole
                        )
                    }
                }
            }
        }
    }

    if (productToArchive != null) {
        AlertDialog(
            onDismissRequest = { productToArchive = null },
            title = { Text("Archive Product") },
            text = { Text("Are you sure you want to archive ${productToArchive?.name}? It will be hidden from the active list but kept in history.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        productToArchive?.let { viewModel.archiveProduct(it.id) }
                        productToArchive = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Archive")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToArchive = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showPasswordDialog != null) {
        InventoryPasswordDialog(
            correctPassword = uiState.inventoryPassword,
            onDismiss = { showPasswordDialog = null },
            onConfirmed = {
                val targetId = showPasswordDialog!!
                showPasswordDialog = null
                if (targetId == -1L) onNavigateToAddProduct()
                else onNavigateToProductDetail(targetId)
            }
        )
    }
}

@Composable
fun InventoryPasswordDialog(
    correctPassword: String,
    onDismiss: () -> Unit,
    onConfirmed: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Inventory Access") },
        text = {
            Column {
                Text("Please enter the inventory password to proceed.")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; error = false },
                    label = { Text("Password") },
                    isError = error,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    singleLine = true
                )
                if (error) {
                    Text("Incorrect password", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (password == correctPassword) {
                    onConfirmed()
                } else {
                    error = true
                }
            }) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ProductItem(
    product: Product,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    userRole: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Category: ${product.category}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                Text("Stock: ${product.currentStock} ${product.unitName}s", style = MaterialTheme.typography.bodyMedium)
                Text("Price: P${product.unitPrice} per ${product.unitName}", style = MaterialTheme.typography.bodySmall)
            }
            if (userRole == "owner") {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
