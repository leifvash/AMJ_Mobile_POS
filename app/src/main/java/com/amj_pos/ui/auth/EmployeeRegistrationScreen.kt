package com.amj_pos.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeRegistrationScreen(
    viewModel: EmployeeRegistrationViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }

    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            snackbarHostState.showSnackbar("Employee registered successfully!")
            viewModel.resetSuccess()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Register Employee") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Create Account for Staff", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.username,
                onValueChange = viewModel::onUsernameChange,
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Assign Branch", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.onBranchChange("Mambuaya") },
                    modifier = Modifier.weight(1f),
                    colors = if (uiState.branchId == "Mambuaya") 
                        ButtonDefaults.buttonColors() 
                    else 
                        ButtonDefaults.outlinedButtonColors(),
                    border = if (uiState.branchId != "Mambuaya") 
                        androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) 
                    else null
                ) {
                    Text("Mambuaya", color = if (uiState.branchId == "Mambuaya") 
                        MaterialTheme.colorScheme.onPrimary 
                    else 
                        MaterialTheme.colorScheme.primary)
                }
                
                Button(
                    onClick = { viewModel.onBranchChange("Bayanga") },
                    modifier = Modifier.weight(1f),
                    colors = if (uiState.branchId == "Bayanga") 
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary) 
                    else 
                        ButtonDefaults.outlinedButtonColors(),
                    border = if (uiState.branchId != "Bayanga") 
                        androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary) 
                    else null
                ) {
                    Text("Bayanga", color = if (uiState.branchId == "Bayanga") 
                        MaterialTheme.colorScheme.onSecondary 
                    else 
                        MaterialTheme.colorScheme.secondary)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = viewModel::register,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Create Employee Account")
                }
            }
        }
    }
}
