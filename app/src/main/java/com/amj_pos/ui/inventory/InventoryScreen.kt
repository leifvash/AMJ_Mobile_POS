package com.amj_pos.ui.inventory

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
    onNavigateToAddProduct: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    var productToRestock by remember { mutableStateOf<Product?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddProduct) {
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
                            onDelete = { viewModel.deleteProduct(product) },
                            onRestock = { productToRestock = product }
                        )
                    }
                }
            }
        }
    }

    productToRestock?.let { product ->
        RestockDialog(
            productName = product.name,
            onDismiss = { productToRestock = null },
            onConfirm = { bulkQty ->
                viewModel.addBulkStock(product.id, bulkQty)
                productToRestock = null
            }
        )
    }
}

@Composable
fun ProductItem(
    product: Product,
    onDelete: () -> Unit,
    onRestock: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
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
                Text("Stock: ${product.currentStockInPieces} pieces", style = MaterialTheme.typography.bodyMedium)
                Text("Price: ₱${product.pieceRetailPrice}", style = MaterialTheme.typography.bodySmall)
            }
            Row {
                TextButton(onClick = onRestock) {
                    Text("Restock")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun RestockDialog(
    productName: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var bulkQty by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restock $productName") },
        text = {
            Column {
                Text("How many BULK units (e.g. boxes) to add?")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = bulkQty,
                    onValueChange = { bulkQty = it },
                    label = { Text("Bulk Quantity") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { 
                bulkQty.toIntOrNull()?.let { onConfirm(it) }
            }) {
                Text("Add Stock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
