package com.amj_pos.ui.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
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
    var showDatePicker by remember { mutableStateOf(false) }

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
                    Text("Date: ${uiState.selectedDate}", style = MaterialTheme.typography.titleMedium)
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
                    items(uiState.transactions) { transaction ->
                        TransactionItemCard(
                            transaction = transaction,
                            getItems = { viewModel.getTransactionItems(transaction.id) }
                        )
                    }
                }
            }
        }
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
    getItems: suspend () -> List<TransactionItem>
) {
    var expanded by remember { mutableStateOf(false) }
    var items by remember { mutableStateOf<List<TransactionItem>>(emptyList()) }
    val scope = rememberCoroutineScope()

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
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(transaction.timestamp))
                Text("Time: $time", fontWeight = FontWeight.Bold)
                Text("₱${transaction.totalAmount}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Text("Method: ${transaction.paymentMethod}", style = MaterialTheme.typography.bodySmall)
            
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${item.productName} x${item.quantity}", style = MaterialTheme.typography.bodySmall)
                        Text("₱${item.sellPricePerPiece * item.quantity}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Profit (Kita): ₱${transaction.totalProfit}", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
