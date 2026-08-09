package com.amj_pos.ui.utang

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amj_pos.data.local.entities.UtangRecord
import com.amj_pos.data.local.entities.UtangType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    customerName: String,
    viewModel: CustomerDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(customerName) },
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
        ) {
            // Summary Header
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Current Balance", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "₱${uiState.balance}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.balance > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                "History",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.history.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No records yet.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.history) { record ->
                        UtangRecordItem(record)
                    }
                }
            }
        }
    }
}

@Composable
fun UtangRecordItem(record: UtangRecord) {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val dateStr = sdf.format(Date(record.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (record.type == UtangType.CREDIT) "Credit Purchase" else "Payment Received",
                    fontWeight = FontWeight.Bold
                )
                Text(dateStr, style = MaterialTheme.typography.bodySmall)
                record.note?.let {
                    Text("Note: $it", style = MaterialTheme.typography.labelSmall)
                }
            }
            Text(
                text = "${if (record.type == UtangType.CREDIT) "+" else "-"} ₱${Math.abs(record.amount)}",
                fontWeight = FontWeight.ExtraBold,
                color = if (record.type == UtangType.CREDIT) Color(0xFFD32F2F) else Color(0xFF388E3C)
            )
        }
    }
}
