package com.amj_pos.ui.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amj_pos.data.local.entities.Transaction
import com.amj_pos.data.local.entities.TransactionItem
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    viewModel: TransactionHistoryViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagedTransactions = viewModel.pagedTransactions.collectAsLazyPagingItems()
    var showDatePicker by remember { mutableStateOf(false) }
    var transactionToVoid by remember { mutableStateOf<Transaction?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Select Date")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Day Summary
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Date: ${uiState.selectedDate}", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (uiState.selectedBranch.isEmpty()) "All Branches" else "Branch: ${uiState.selectedBranch}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        
                        if (uiState.userRole == "owner") {
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                TextButton(onClick = { expanded = true }) {
                                    Text(if (uiState.selectedBranch.isEmpty()) "Filter: All" else "Filter: ${uiState.selectedBranch}")
                                }
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    DropdownMenuItem(
                                        text = { Text("All Branches") },
                                        onClick = {
                                            viewModel.onBranchSelected("")
                                            expanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Mambuaya") },
                                        onClick = {
                                            viewModel.onBranchSelected("Mambuaya")
                                            expanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Bayanga") },
                                        onClick = {
                                            viewModel.onBranchSelected("Bayanga")
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    Text(
                        "Total Sales: ₱${uiState.totalSales}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.transactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No transactions for this date.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        count = pagedTransactions.itemCount,
                        key = pagedTransactions.itemKey { it.id }
                    ) { index ->
                        val transaction = pagedTransactions[index]
                        if (transaction != null) {
                            TransactionItemCard(
                                transaction = transaction,
                                getItems = { viewModel.getTransactionItems(transaction.id) },
                                getCustomerName = { viewModel.getCustomerName(transaction.customerId) },
                                onVoid = { transactionToVoid = transaction }
                            )
                        }
                    }

                    if (pagedTransactions.loadState.append is LoadState.Loading) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }

    if (transactionToVoid != null) {
        AlertDialog(
            onDismissRequest = { transactionToVoid = null },
            title = { Text("Void Transaction") },
            text = { Text("Are you sure you want to void this transaction? This will delete the record and restore stock.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        transactionToVoid?.let { viewModel.voidTransaction(it) }
                        transactionToVoid = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Void")
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToVoid = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it))
                        viewModel.onDateSelected(date)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun TransactionItemCard(
    transaction: Transaction,
    getItems: suspend () -> List<TransactionItem>,
    getCustomerName: suspend () -> String?,
    onVoid: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var items by remember { mutableStateOf<List<TransactionItem>>(emptyList()) }
    var customerName by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(transaction.customerId) {
        if (transaction.isUtang) {
            customerName = getCustomerName()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                expanded = !expanded
                if (expanded && items.isEmpty()) {
                    // Load items when expanded
                    scope.launch {
                        items = getItems()
                    }
                }
            }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(transaction.timestamp))
                Text("Time: $time", fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("₱${transaction.totalAmount}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onVoid,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Void",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Text("Method: ${transaction.paymentMethod}", style = MaterialTheme.typography.bodySmall)
            customerName?.let {
                Text("Customer: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val whole = item.quantity.toInt()
                        val fraction = item.quantity - whole
                        val qtyStr = when {
                            fraction == 0.0 -> whole.toString()
                            fraction == 0.5 -> if (whole == 0) "1/2" else "$whole and 1/2"
                            else -> String.format(Locale.getDefault(), "%.1f", item.quantity)
                        }
                        Text("$qtyStr ${item.productName}", style = MaterialTheme.typography.bodySmall)
                        Text("₱${item.sellPrice}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                // Removed Profit line
            }
        }
    }
}
