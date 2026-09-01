package com.amj_pos.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amj_pos.domain.printer.PrinterStatus
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
    onNavigateToAnalytics: () -> Unit,
    onNavigateToPrinterSettings: () -> Unit,
    onNavigateToRegistration: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Auto-connect on Dashboard Load if not connected
    LaunchedEffect(uiState.printerStatus) {
        if (uiState.printerStatus == PrinterStatus.DISCONNECTED) {
            viewModel.connectPrinter()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("AMJ Sari-Sari POS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (uiState.selectedBranch.isEmpty()) "Select Branch" else uiState.selectedBranch,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (uiState.selectedBranch.isEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            if (uiState.userRole == "owner") {
                                var showBranchSwitcher by remember { mutableStateOf(false) }
                                IconButton(
                                    onClick = { showBranchSwitcher = true },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = "Switch Branch", modifier = Modifier.size(16.dp))
                                }
                                if (showBranchSwitcher) {
                                    DropdownMenu(expanded = showBranchSwitcher, onDismissRequest = { showBranchSwitcher = false }) {
                                        DropdownMenuItem(
                                            text = { Text("All Branches") },
                                            onClick = { 
                                                viewModel.switchBranch("")
                                                showBranchSwitcher = false 
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Mambuaya") },
                                            onClick = { 
                                                viewModel.switchBranch("Mambuaya")
                                                showBranchSwitcher = false 
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Bayanga") },
                                            onClick = { 
                                                viewModel.switchBranch("Bayanga")
                                                showBranchSwitcher = false 
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                actions = {
                    if (uiState.userRole == "owner") {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    } else {
                        IconButton(onClick = { viewModel.logout() }) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                        }
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
            // Printer Status Bar
            PrinterStatusBar(
                status = uiState.printerStatus,
                onConnect = { viewModel.connectPrinter() },
                onSettings = onNavigateToPrinterSettings
            )

            if (uiState.userName.isNotBlank()) {
                Text(
                    text = "Hello, ${uiState.userName}!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Summary Cards
            SummaryCard(
                label = "Today's Total Sales",
                amount = uiState.dailySales,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToAnalytics() }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryCard(
                    label = "Cash",
                    amount = uiState.cashSales,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    label = "GCash",
                    amount = uiState.gcashSales,
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    label = "Today's Utang",
                    amount = uiState.dailyUtangSales,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )
            }

            SummaryCard(
                label = "Overall Outstanding Utang",
                amount = uiState.totalUtang,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToUtang() }
            )

            if (uiState.lowStockProducts.isNotEmpty()) {
                LowStockAlertSection(products = uiState.lowStockProducts, onAction = onNavigateToInventory)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Quick Actions", style = MaterialTheme.typography.titleMedium)

            // Grid Actions - Using Column/Row instead of LazyVerticalGrid inside a scrollable column
            DashboardActionGrid(
                userRole = uiState.userRole,
                onNavigateToCheckout = onNavigateToCheckout,
                onNavigateToInventory = onNavigateToInventory,
                onNavigateToUtang = onNavigateToUtang,
                onNavigateToTransactions = onNavigateToTransactions,
                onNavigateToRegistration = onNavigateToRegistration
            )
        }
    }
}

@Composable
fun LowStockAlertSection(products: List<com.amj_pos.data.local.entities.Product>, onAction: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

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
            
            val displayList = if (expanded) products else products.take(2)
            
            displayList.forEach { product ->
                Text("• ${product.name} (${product.currentStock} ${product.unitName}s left)", style = MaterialTheme.typography.bodySmall)
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (products.size > 2) {
                    TextButton(onClick = { expanded = !expanded }) {
                        Text(if (expanded) "View Less" else "View More (${products.size - 2})")
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                
                TextButton(onClick = onAction) {
                    Text("Fix Now")
                }
            }
        }
    }
}

@Composable
fun DashboardActionGrid(
    userRole: String,
    onNavigateToCheckout: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToUtang: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToRegistration: () -> Unit
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
        if (userRole == "owner") {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ActionCard(label = "Add Employee", icon = Icons.Default.PersonAdd, onClick = onNavigateToRegistration, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.weight(1f))
            }
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
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(
                text = currencyFormatter.format(amount),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
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

@Composable
fun PrinterStatusBar(status: PrinterStatus, onConnect: () -> Unit, onSettings: () -> Unit) {
    val backgroundColor = when (status) {
        PrinterStatus.CONNECTED -> Color(0xFFE8F5E9)
        PrinterStatus.CONNECTING -> Color(0xFFFFF3E0)
        PrinterStatus.DISCONNECTED -> Color(0xFFF5F5F5)
        PrinterStatus.ERROR -> Color(0xFFFFEBEE)
    }
    
    val contentColor = when (status) {
        PrinterStatus.CONNECTED -> Color(0xFF2E7D32)
        PrinterStatus.CONNECTING -> Color(0xFFEF6C00)
        PrinterStatus.DISCONNECTED -> Color(0xFF757575)
        PrinterStatus.ERROR -> Color(0xFFC62828)
    }

    val statusText = when (status) {
        PrinterStatus.CONNECTED -> "Printer Connected"
        PrinterStatus.CONNECTING -> "Connecting to Printer..."
        PrinterStatus.DISCONNECTED -> "Printer Disconnected"
        PrinterStatus.ERROR -> "Printer Error (Check BT)"
    }

    Surface(
        color = backgroundColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSettings() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (status == PrinterStatus.CONNECTED) Icons.Default.Inventory else Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(statusText, style = MaterialTheme.typography.labelLarge)
            }
            
            if (status != PrinterStatus.CONNECTED && status != PrinterStatus.CONNECTING) {
                TextButton(onClick = onConnect) {
                    Text("Connect", style = MaterialTheme.typography.labelSmall)
                }
            } else {
                Text("Settings >", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
