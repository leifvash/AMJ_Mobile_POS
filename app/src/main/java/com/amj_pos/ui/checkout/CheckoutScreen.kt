package com.amj_pos.ui.checkout

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.amj_pos.R
import com.amj_pos.data.local.entities.PaymentMethod
import com.amj_pos.data.local.entities.TransactionItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    viewModel: CheckoutViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val manualSearchQuery by viewModel.manualSearchQuery.collectAsState()
    
    var showPaymentSheet by remember { mutableStateOf(false) }
    var showCustomerSheet by remember { mutableStateOf(false) }
    var showManualAddSheet by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val text = data?.get(0) ?: ""
            viewModel.onVoiceResult(text)
            showManualAddSheet = true // Open search sheet with the voice text
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Checkout") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say product name...")
                        }
                        voiceLauncher.launch(intent)
                    }) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice Search")
                    }
                    IconButton(onClick = { showManualAddSheet = true }) {
                        Icon(Icons.Default.Search, contentDescription = "Manual Add")
                    }
                    IconButton(onClick = viewModel::scanBarcode) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan")
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
        ) {
            if (uiState.items.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("Scan a product to start.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.items) { item ->
                        CartItem(
                            item = item,
                            onIncrement = { viewModel.updateQuantity(item.productId, 1) },
                            onDecrement = { viewModel.updateQuantity(item.productId, -1) },
                            onRemove = { viewModel.removeItemFromCart(item.productId) }
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Amount:", style = MaterialTheme.typography.titleMedium)
                    Text("Method: ${uiState.paymentMethod}", style = MaterialTheme.typography.bodySmall)
                    if (uiState.paymentMethod == PaymentMethod.UTANG) {
                        Text(
                            text = uiState.selectedCustomer?.let { "Customer: ${it.name}" } ?: "Select Customer",
                            color = if (uiState.selectedCustomer == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.clickable { showCustomerSheet = true }
                        )
                    }
                }
                Text("₱${uiState.totalAmount}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showPaymentSheet = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Change Method")
                }
                
                Button(
                    onClick = { 
                        if (uiState.paymentMethod == PaymentMethod.QRPH) {
                            showQrDialog = true
                        } else {
                            viewModel.completeSale()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = uiState.items.isNotEmpty()
                ) {
                    Text(if (uiState.paymentMethod == PaymentMethod.QRPH) "Show QR" else "Confirm Sale")
                }
            }
        }
    }

    if (showQrDialog) {
        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            modifier = Modifier.fillMaxWidth(),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = { Text("Scan to Pay (GCash QR)", modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = R.drawable.static_gcash_qr),
                        contentDescription = "GCash QR Code",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .padding(8.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Total: ₱${uiState.totalAmount}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.completeSale()
                        showQrDialog = false
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("DONE - Payment Received", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showQrDialog = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showPaymentSheet) {
        ModalBottomSheet(onDismissRequest = { showPaymentSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Select Payment Method", style = MaterialTheme.typography.titleLarge)
                PaymentMethod.entries.forEach { method ->
                    ListItem(
                        headlineContent = { Text(method.name) },
                        modifier = Modifier.clickable {
                            viewModel.onPaymentMethodChange(method)
                            showPaymentSheet = false
                        }
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showCustomerSheet) {
        ModalBottomSheet(onDismissRequest = { showCustomerSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Select Customer for Utang", style = MaterialTheme.typography.titleLarge)
                if (uiState.customers.isEmpty()) {
                    Text("No customers found. Please add them in the Utang Ledger.")
                } else {
                    uiState.customers.forEach { customer ->
                        ListItem(
                            headlineContent = { Text(customer.name) },
                            modifier = Modifier.clickable {
                                viewModel.onCustomerSelected(customer)
                                showCustomerSheet = false
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showManualAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { 
                showManualAddSheet = false 
                viewModel.onManualSearchQueryChange("")
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Manual Product Selection", style = MaterialTheme.typography.titleLarge)
                
                OutlinedTextField(
                    value = manualSearchQuery,
                    onValueChange = viewModel::onManualSearchQueryChange,
                    label = { Text("Search Inventory") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.searchProducts) { product ->
                        ListItem(
                            headlineContent = { Text(product.name) },
                            supportingContent = { Text("Stock: ${product.currentStockInPieces} | ₱${product.pieceRetailPrice}") },
                            trailingContent = {
                                Button(
                                    onClick = { 
                                        viewModel.addItemToCart(product)
                                        showManualAddSheet = false
                                        viewModel.onManualSearchQueryChange("")
                                    },
                                    enabled = product.currentStockInPieces > 0
                                ) {
                                    Text("Add")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CartItem(
    item: TransactionItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.productName, fontWeight = FontWeight.Bold)
                Text("₱${item.sellPricePerPiece} per piece")
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onDecrement) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease")
                }
                Text("${item.quantity}", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onIncrement) {
                    Icon(Icons.Default.Add, contentDescription = "Increase")
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text("₱${item.sellPricePerPiece * item.quantity}", fontWeight = FontWeight.Bold)
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
