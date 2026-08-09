package com.amj_pos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
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
import com.amj_pos.ui.settings.SettingsScreen
import com.amj_pos.ui.transactions.TransactionHistoryScreen
import com.amj_pos.ui.utang.CustomerDetailScreen
import com.amj_pos.ui.utang.UtangScreen
import com.amj_pos.ui.theme.AMJ_POSTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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
                                onNavigateToAnalytics = { navController.navigate("analytics") }
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
                                onNavigateToAddProduct = { navController.navigate("add_product") }
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
}
