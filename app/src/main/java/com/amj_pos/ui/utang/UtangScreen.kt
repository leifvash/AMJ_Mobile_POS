package com.amj_pos.ui.utang

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amj_pos.data.local.entities.Customer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UtangScreen(
    viewModel: UtangViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var customerToPay by remember { mutableStateOf<Customer?>(null) }
    var customerToDelete by remember { mutableStateOf<Customer?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Utang Ledger") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Customer")
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.customers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No customers found.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.customers) { customer ->
                    val balance by viewModel.getCustomerBalance(customer.id).collectAsState(initial = 0.0)
                    CustomerItem(
                        customer = customer,
                        balance = balance,
                        onPay = { customerToPay = customer },
                        onDelete = { customerToDelete = customer },
                        onClick = { onNavigateToDetail(customer.id, customer.name) }
                    )
                }
            }
        }
    }

    if (customerToDelete != null) {
        AlertDialog(
            onDismissRequest = { customerToDelete = null },
            title = { Text("Delete Customer") },
            text = { Text("Are you sure you want to delete ${customerToDelete?.name}? All their utang records will be permanently removed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        customerToDelete?.let { viewModel.deleteCustomer(it) }
                        customerToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { customerToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddDialog) {
        AddCustomerDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, phone ->
                viewModel.addCustomer(name, phone)
                showAddDialog = false
            }
        )
    }

    customerToPay?.let { customer ->
        PaymentDialog(
            customerName = customer.name,
            onDismiss = { customerToPay = null },
            onConfirm = { amount, note ->
                viewModel.recordPayment(customer.id, amount, note)
                customerToPay = null
            }
        )
    }
}

@Composable
fun CustomerItem(
    customer: Customer,
    balance: Double,
    onPay: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(customer.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                customer.phoneNumber?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("₱$balance", color = if (balance > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onPay, enabled = balance > 0) {
                    Text("Pay")
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Customer", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun PaymentDialog(
    customerName: String,
    onDismiss: () -> Unit,
    onConfirm: (Double, String?) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Payment for $customerName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Payment Amount (₱)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { 
                amount.toDoubleOrNull()?.let { onConfirm(it, note.ifBlank { null }) }
            }) {
                Text("Confirm Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddCustomerDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Customer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                TextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") })
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name, phone.ifBlank { null }) }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
