package com.velox.jewelvault.ui.screen.store_selection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.velox.jewelvault.data.roomdb.entity.StoreEntity
import com.velox.jewelvault.ui.components.baseBackground8
import com.velox.jewelvault.ui.components.goldAnimationBackground
import com.velox.jewelvault.ui.nav.Screens
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.LocalNavController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StoreSelectionScreen() {
    val navHost = LocalNavController.current
    val baseViewModel = LocalBaseViewModel.current
    val viewModel = hiltViewModel<StoreSelectionViewModel>()

    val hasNavigated = remember { mutableStateOf(false) }
    val isLoading = viewModel.isLoading.value
    val isSelecting = viewModel.isSelecting.value
    val hasNoStores = viewModel.hasNoStores.value
    val requiresManualSelection = viewModel.requiresManualSelection.value
    val navigateToStartLoading = viewModel.navigateToStartLoading.value
    val blockedInfo = viewModel.blockedDeviceInfo.value
    val stores = viewModel.stores

    LaunchedEffect(Unit) {
        viewModel.loadStores()
    }

    LaunchedEffect(viewModel.errorMessage.value) {
        val message = viewModel.errorMessage.value ?: return@LaunchedEffect
        baseViewModel.snackBarState = message
        viewModel.consumeError()
    }

    LaunchedEffect(navigateToStartLoading, hasNoStores) {
        if (hasNavigated.value) return@LaunchedEffect
        if (!navigateToStartLoading && !hasNoStores) return@LaunchedEffect

        hasNavigated.value = true
        viewModel.consumeNavigation()
        navHost.navigate(Screens.StartLoading.route) {
            popUpTo(Screens.StoreSelection.route) {
                inclusive = true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .baseBackground8()
            .goldAnimationBackground()
            .padding(16.dp)
    ) {
        when {
            isLoading && stores.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Loading stores...")
                }
            }

            !requiresManualSelection -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Continuing with your selected store...")
                }
            }

            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Select Store",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Choose the store you want to use on this device.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = stores,
                            key = { it.storeId }
                        ) { store ->
                            StoreCard(
                                store = store,
                                enabled = !isSelecting,
                                onClick = { viewModel.selectStore(store) }
                            )
                        }
                    }
                }
            }
        }

        if (isSelecting && requiresManualSelection) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }

    if (blockedInfo != null) {
        val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
        val lastLoginText = if (blockedInfo.lastLoginAt > 0L) {
            dateFormat.format(Date(blockedInfo.lastLoginAt))
        } else {
            "Unknown"
        }
        AlertDialog(
            onDismissRequest = { viewModel.dismissBlockedDeviceDialog() },
            title = { Text("Session Active on Another Device") },
            text = {
                Column {
                    Text("This store is already active on another device.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Device: ${blockedInfo.manufacturer} ${blockedInfo.model}")
                    Text("Last login: $lastLoginText")
                }
            },
            confirmButton = {
                com.velox.jewelvault.ui.components.AppButton(
                    onClick = { viewModel.dismissBlockedDeviceDialog() }
                ) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun StoreCard(
    store: StoreEntity,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = store.name.ifBlank { "Unnamed Store" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (store.phone.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Phone: ${store.phone}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Store ID: ${store.storeId}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
