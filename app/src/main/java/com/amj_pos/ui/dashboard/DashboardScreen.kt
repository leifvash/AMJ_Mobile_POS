package com.amj_pos.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToCheckout: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToUtang: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAnalytics: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AMJ Sari-Sari POS", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SummaryCard(
                    label = "Kada-araw na Kita",
                    amount = uiState.dailyProfit,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToAnalytics() }
                )
                SummaryCard(
                    label = "Total na Utang",
                    amount = uiState.totalUtang,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToUtang() }
                )
            }

            if (uiState.lowStockProducts.isNotEmpty()) {
                LowStockAlertSection(products = uiState.lowStockProducts, onAction = onNavigateToInventory)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Quick Actions", style = MaterialTheme.typography.titleMedium)

            // Grid Actions - Using Column/Row instead of LazyVerticalGrid inside a scrollable column
            DashboardActionGrid(
                onNavigateToCheckout = onNavigateToCheckout,
                onNavigateToInventory = onNavigateToInventory,
                onNavigateToUtang = onNavigateToUtang,
                onNavigateToTransactions = onNavigateToTransactions
            )
        }
    }
}

@Composable
fun LowStockAlertSection(products: List<com.amj_pos.data.local.entities.Product>, onAction: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Inventory, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Restock Needed!", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(8.dp))
            products.take(3).forEach { product ->
                Text("• ${product.name} (${product.currentStockInPieces} pieces left)", style = MaterialTheme.typography.bodySmall)
            }
            if (products.size > 3) {
                Text("...and ${products.size - 3} more", style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onAction, modifier = Modifier.align(Alignment.End)) {
                Text("Fix Now")
            }
        }
    }
}

@Composable
fun DashboardActionGrid(
    onNavigateToCheckout: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToUtang: () -> Unit,
    onNavigateToTransactions: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ActionCard(label = "Checkout", icon = Icons.Default.QrCodeScanner, onClick = onNavigateToCheckout, modifier = Modifier.weight(1f))
            ActionCard(label = "Inventory", icon = Icons.Default.Inventory, onClick = onNavigateToInventory, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ActionCard(label = "Utang Ledger", icon = Icons.Default.People, onClick = onNavigateToUtang, modifier = Modifier.weight(1f))
            ActionCard(label = "Transactions", icon = Icons.Default.Receipt, onClick = onNavigateToTransactions, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun SummaryCard(
    label: String,
    amount: Double,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "PH"))
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        modifier = modifier.height(120.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(
                text = currencyFormatter.format(amount),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionCard(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        elevation = CardDefaults.elevatedCardElevation(),
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label)
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
