package com.amj_pos.ui.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    viewModel: ProductDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showAdjustDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(uiState.name) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Wholesale Details", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = uiState.unitName,
                onValueChange = viewModel::onUnitNameChange,
                label = { Text("Unit Name (e.g., Case, Pack)") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = uiState.unitPrice,
                    onValueChange = viewModel::onUnitPriceChange,
                    label = { Text("Unit Price (₱)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = uiState.piecesPerUnit,
                    onValueChange = viewModel::onPiecesPerUnitChange,
                    label = { Text("Pieces per Unit") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = uiState.stock,
                onValueChange = viewModel::onStockChange,
                label = { Text("Current Stock (Total ${uiState.unitName}s)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            
            Button(
                onClick = { showAdjustDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Adjust Stock")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Adjustment History", style = MaterialTheme.typography.titleSmall)
            
            uiState.adjustments.forEach { adjustment ->
                AdjustmentItem(adjustment)
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = viewModel::updateProduct,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Changes")
            }
        }
    }
    
    if (showAdjustDialog) {
        StockAdjustmentDialog(
            unitName = uiState.unitName,
            onDismiss = { showAdjustDialog = false },
            onConfirm = { amount, reason ->
                viewModel.adjustStock(amount, reason)
                showAdjustDialog = false
            }
        )
    }
}

@Composable
fun AdjustmentItem(adjustment: com.amj_pos.data.local.entities.StockAdjustment) {
    val date = java.text.SimpleDateFormat("MMM dd, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(adjustment.timestamp))
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(adjustment.reason, fontWeight = FontWeight.Bold)
                Text(date, style = MaterialTheme.typography.labelSmall)
            }
            Text(
                text = "${if (adjustment.adjustmentAmount > 0) "+" else ""}${adjustment.adjustmentAmount}",
                color = if (adjustment.adjustmentAmount > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun StockAdjustmentDialog(
    unitName: String,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("Restock") }
    var isAddition by remember { mutableStateOf(true) }
    val reasons = listOf("Restock", "Spoilage", "Consumption", "Correction")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adjust Stock") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Add/Subtract Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { isAddition = true },
                        modifier = Modifier.weight(1f),
                        colors = if (isAddition) ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)) 
                                 else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text("Add (+)", color = if (isAddition) Color.White else Color(0xFF2E7D32))
                    }
                    Button(
                        onClick = { isAddition = false },
                        modifier = Modifier.weight(1f),
                        colors = if (!isAddition) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) 
                                 else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text("Subtract (-)", color = if (!isAddition) Color.White else MaterialTheme.colorScheme.error)
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (${if (isAddition) "to add" else "to subtract"})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    placeholder = { Text("e.g., 2 or 0.5") },
                    modifier = Modifier.fillMaxWidth(),
                    suffix = { Text(unitName) }
                )
                
                Text("Reason:", style = MaterialTheme.typography.labelLarge)
                reasons.forEach { r ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { reason = r },
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        RadioButton(selected = reason == r, onClick = { reason = r })
                        Text(r)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                amount.toDoubleOrNull()?.let { 
                    val finalAmount = if (isAddition) it else -it
                    onConfirm(finalAmount, reason) 
                }
            }) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
