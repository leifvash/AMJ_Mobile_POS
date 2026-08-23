package com.amj_pos.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amj_pos.data.local.entities.DailyStat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sales Performance") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text("Daily Sales (Last 7 Days)", style = MaterialTheme.typography.titleMedium)
                
                SalesBarChart(
                    stats = uiState.dailyStats,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )

                HorizontalDivider()

                Text("Branch Performance", style = MaterialTheme.typography.titleMedium)
                uiState.branchPerformance.forEach { performance ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(performance.branchName)
                        Text("₱${performance.totalSales}", fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider()

                Text("Top 5 Selling Products", style = MaterialTheme.typography.titleMedium)
                uiState.topProducts.forEach { item ->
                    ListItem(
                        headlineContent = { Text(item.productName) },
                        supportingContent = { Text("${item.quantity} ${item.unitName}s sold") }
                    )
                }

                HorizontalDivider()

                Text("Daily Summary", style = MaterialTheme.typography.titleMedium)
                
                uiState.dailyStats.reversed().forEach { stat ->
                    DailyStatRow(stat)
                }
            }
        }
    }
}

@Composable
fun SalesBarChart(stats: List<DailyStat>, modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    
    if (stats.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Not enough data yet.", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    val maxSales = stats.maxOf { it.totalSales }.coerceAtLeast(100.0)

    Canvas(modifier = modifier.padding(bottom = 24.dp)) {
        val width = size.width
        val height = size.height
        val barWidth = width / (stats.size * 1.5f)
        val spacing = barWidth * 0.5f

        stats.forEachIndexed { index, stat ->
            val barHeight = (stat.totalSales / maxSales).toFloat() * height
            val x = index * (barWidth + spacing) + spacing
            
            // Draw Bar
            drawRect(
                color = if (stat.totalSales > 0) primaryColor else Color.Gray,
                topLeft = Offset(x, height - barHeight),
                size = Size(barWidth, barHeight)
            )

            // Draw Label (Date) - using nativeCanvas for simple text
            drawContext.canvas.nativeCanvas.apply {
                val label = stat.date.takeLast(5) // MM-DD
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 30f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                drawText(label, x + barWidth / 2, height + 40f, paint)
            }
        }
    }
}

@Composable
fun DailyStatRow(stat: DailyStat) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(stat.date, fontWeight = FontWeight.Bold)
                Text("Total Today:", style = MaterialTheme.typography.labelSmall)
            }
            Text("₱${stat.totalSales}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
        }
    }
}
