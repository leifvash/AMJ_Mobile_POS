package com.amj_pos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.amj_pos.ui.ViewModelFactory
import com.amj_pos.ui.analytics.AnalyticsScreen
import com.amj_pos.ui.checkout.CheckoutScreen
import com.amj_pos.ui.dashboard.DashboardScreen
import com.amj_pos.ui.inventory.AddProductScreen
import com.amj_pos.ui.inventory.InventoryScreen
import com.amj_pos.ui.inventory.ProductDetailScreen
import com.amj_pos.ui.settings.PrinterSettingsScreen
import com.amj_pos.ui.settings.SettingsScreen
import com.amj_pos.ui.transactions.TransactionHistoryScreen
import com.amj_pos.ui.utang.CustomerDetailScreen
import com.amj_pos.ui.utang.UtangScreen
import com.amj_pos.ui.theme.AMJ_POSTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle results if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkAndRequestPermissions()

        val appContainer = (application as AMJApplication).container
        val viewModelFactory = ViewModelFactory(appContainer)

        setContent {
            AMJ_POSTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    NavHost(navController = navController, startDestination = "dashboard") {
                        composable("dashboard") {
                            DashboardScreen(
                                viewModel = viewModel(factory = viewModelFactory),
                                onNavigateToCheckout = { navController.navigate("checkout") },
                                onNavigateToInventory = { navController.navigate("inventory") },
                                onNavigateToUtang = { navController.navigate("utang") },
                                onNavigateToTransactions = { navController.navigate("transactions") },
                                onNavigateToSettings = { navController.navigate("settings") },
                                onNavigateToAnalytics = { navController.navigate("analytics") },
                                onNavigateToPrinterSettings = { navController.navigate("printer_settings") }
                            )
                        }
                        composable("printer_settings") {
                            PrinterSettingsScreen(
                                viewModel = viewModel(factory = viewModelFactory),
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("analytics") {
                            AnalyticsScreen(
                                viewModel = viewModel(factory = viewModelFactory),
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel(factory = viewModelFactory),
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("transactions") {
                            TransactionHistoryScreen(
                                viewModel = viewModel(factory = viewModelFactory),
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("checkout") {
                            CheckoutScreen(
                                viewModel = viewModel(factory = viewModelFactory),
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("inventory") {
                            InventoryScreen(
                                viewModel = viewModel(factory = viewModelFactory),
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToAddProduct = { navController.navigate("add_product") },
                                onNavigateToProductDetail = { id -> navController.navigate("product_detail/$id") }
                            )
                        }
                        composable("product_detail/{productId}") { backStackEntry ->
                            val productId = backStackEntry.arguments?.getString("productId")?.toLongOrNull() ?: 0L
                            ProductDetailScreen(
                                viewModel = viewModel(factory = ViewModelFactory(appContainer, productId)),
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("add_product") {
                            AddProductScreen(
                                viewModel = viewModel(factory = viewModelFactory),
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("utang") {
                            UtangScreen(
                                viewModel = viewModel(factory = viewModelFactory),
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToDetail = { id, name -> 
                                    navController.navigate("customer_detail/$id/$name")
                                }
                            )
                        }
                        composable("customer_detail/{customerId}/{customerName}") { backStackEntry ->
                            val customerId = backStackEntry.arguments?.getString("customerId")?.toLongOrNull() ?: 0L
                            val customerName = backStackEntry.arguments?.getString("customerName") ?: ""
                            CustomerDetailScreen(
                                customerName = customerName,
                                viewModel = viewModel(factory = ViewModelFactory(appContainer, customerId)),
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }
}
