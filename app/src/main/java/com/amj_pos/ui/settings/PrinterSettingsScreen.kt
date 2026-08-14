package com.amj_pos.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amj_pos.domain.printer.PrinterStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterSettingsScreen(
    viewModel: PrinterSettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Printer Settings") },
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
            // Current Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when(uiState.connectionStatus) {
                        PrinterStatus.CONNECTED -> Color(0xFFE8F5E9)
                        PrinterStatus.ERROR -> Color(0xFFFFEBEE)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Print,
                        contentDescription = null,
                        tint = if (uiState.connectionStatus == PrinterStatus.CONNECTED) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = when(uiState.connectionStatus) {
                                PrinterStatus.CONNECTED -> "Connected to ${uiState.connectedDeviceName}"
                                PrinterStatus.CONNECTING -> "Connecting..."
                                PrinterStatus.DISCONNECTED -> "Not Connected"
                                PrinterStatus.ERROR -> "Connection Error"
                            },
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (uiState.connectionStatus == PrinterStatus.CONNECTED) "Ready to print receipts" else "Please select a device below",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Test Tools
            Text("Hardware Test Tools", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.printTestPage() },
                    modifier = Modifier.weight(1f),
                    enabled = uiState.connectionStatus == PrinterStatus.CONNECTED
                ) {
                    Text("Test Print")
                }
                OutlinedButton(
                    onClick = { viewModel.disconnect() },
                    modifier = Modifier.weight(1f),
                    enabled = uiState.connectionStatus == PrinterStatus.CONNECTED
                ) {
                    Text("Disconnect")
                }
            }

            HorizontalDivider()

            // Device List
            Text("Paired Bluetooth Devices", style = MaterialTheme.typography.titleMedium)
            if (uiState.pairedDevices.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No paired devices found. Pair your JK-5801H in phone settings first.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.pairedDevices) { device ->
                        ListItem(
                            headlineContent = { Text(device.name) },
                            supportingContent = { Text(device.address) },
                            leadingContent = { Icon(Icons.Default.Bluetooth, contentDescription = null) },
                            modifier = Modifier.clickable { viewModel.connectToDevice(device.address) },
                            trailingContent = {
                                if (uiState.connectedDeviceName == device.name) {
                                    Icon(Icons.Default.Print, contentDescription = "Active", tint = Color(0xFF2E7D32))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
