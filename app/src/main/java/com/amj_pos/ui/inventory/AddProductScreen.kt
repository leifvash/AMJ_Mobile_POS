package com.amj_pos.ui.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    viewModel: AddProductViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Product") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Product Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = uiState.barcode,
                    onValueChange = viewModel::onBarcodeChange,
                    label = { Text("Barcode") },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = viewModel::scanBarcode) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan")
                }
            }

            Text("Tingoting Logic (Bulk to Piece)", style = MaterialTheme.typography.titleSmall)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = uiState.bulkCostPrice,
                    onValueChange = viewModel::onBulkCostPriceChange,
                    label = { Text("Bulk Cost (₱)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = uiState.piecesPerBulk,
                    onValueChange = viewModel::onPiecesPerBulkChange,
                    label = { Text("Pieces per Bulk") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = uiState.pieceRetailPrice,
                onValueChange = viewModel::onPieceRetailPriceChange,
                label = { Text("Retail Price per Piece (₱)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.initialStockPieces,
                onValueChange = viewModel::onInitialStockChange,
                label = { Text("Initial Stock (Pieces)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = viewModel::saveProduct,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Product")
            }
        }
    }
}
