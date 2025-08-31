package com.velox.jewelvault.ui.screen.setting

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.velox.jewelvault.ui.nav.Screens
import com.velox.jewelvault.ui.nav.SubScreens
import com.velox.jewelvault.ui.screen.dashboard.DashboardViewModel
import com.velox.jewelvault.utils.LocalNavController
import com.velox.jewelvault.utils.LocalSubNavController
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.ui.components.OptionalUpdateDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen() {
    val subNavController = LocalSubNavController.current
    val mainNavController = LocalNavController.current
    val baseViewModel = LocalBaseViewModel.current
    val context = LocalContext.current

    baseViewModel.currentScreenHeading = "Settings"

    BackHandler {
        subNavController.navigate(SubScreens.Dashboard.route) {
            popUpTo(SubScreens.Dashboard.route) {
                inclusive = true
            }
        }
    }

    // Handle data wipe completion
    LaunchedEffect(baseViewModel.snackBarState) {
        if (baseViewModel.snackBarState.contains("All data wiped successfully")) {
            // Navigate to login screen after successful data wipe
            mainNavController.navigate(Screens.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }
    
    // Handle settings updates
    LaunchedEffect(baseViewModel.snackBarState) {
        if (baseViewModel.snackBarState.isNotEmpty() &&
            !baseViewModel.snackBarState.contains("All data wiped successfully")) {
            // Clear the message after 3 seconds
            kotlinx.coroutines.delay(3000)
            baseViewModel.snackBarState = ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 18.dp))
            .padding(5.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Settings Content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Network Settings
                item {
                    SettingsSectionHeader("Network & Connectivity", Icons.Default.Wifi)
                }
                
                item {
                    SettingsSwitchItem(
                        title = "Continuous Network Check",
                        subtitle = "Monitor internet connection continuously",
                        checked = baseViewModel.continuousNetworkCheck.value,
                        onCheckedChange = { baseViewModel.updateSetting("continuous_network_check", it) }
                    )
                }
                
                item {
                    SettingsSwitchItem(
                        title = "Network Speed Monitoring",
                        subtitle = "Check internet speed periodically",
                        checked = baseViewModel.networkSpeedMonitoring.value,
                        onCheckedChange = { baseViewModel.updateSetting("network_speed_monitoring", it) }
                    )
                }
                
                item {
                    SettingsSwitchItem(
                        title = "Auto-refresh Metal Rates",
                        subtitle = "Automatically refresh metal prices",
                        checked = baseViewModel.autoRefreshMetalRates.value,
                        onCheckedChange = { baseViewModel.updateSetting("auto_refresh_metal_rates", it) }
                    )
                }

                // Business Settings
                item {
                    SettingsSectionHeader("Business Settings", Icons.Default.Business)
                }
                
                item {
                    SettingsTextInputItem(
                        title = "Default CGST (%)",
                        subtitle = "Central Goods and Services Tax",
                        value = baseViewModel.defaultCgst.value,
                        onValueChange = { baseViewModel.updateSetting("default_cgst", it) }
                    )
                }
                
                item {
                    SettingsTextInputItem(
                        title = "Default SGST (%)",
                        subtitle = "State Goods and Services Tax",
                        value = baseViewModel.defaultSgst.value,
                        onValueChange = { baseViewModel.updateSetting("default_sgst", it) }
                    )
                }
                
                item {
                    SettingsTextInputItem(
                        title = "Default IGST (%)",
                        subtitle = "Integrated Goods and Services Tax",
                        value = baseViewModel.defaultIgst.value,
                        onValueChange = { baseViewModel.updateSetting("default_igst", it) }
                    )
                }

                // Security Settings
                item {
                    SettingsSectionHeader("Security", Icons.Default.Security)
                }
                
                item {
                    SettingsSliderItem(
                        title = "Session Timeout",
                        subtitle = "${baseViewModel.sessionTimeoutMinutes.value} minutes",
                        value = baseViewModel.sessionTimeoutMinutes.value.toFloat(),
                        onValueChange = { baseViewModel.updateSetting("session_timeout_minutes", it.toInt()) },
                        valueRange = 5f..120f,
                        steps = 23
                    )
                }
                
                item {
                    SettingsSwitchItem(
                        title = "Auto-logout on Inactivity",
                        subtitle = "Automatically logout when inactive",
                        checked = baseViewModel.autoLogoutInactivity.value,
                        onCheckedChange = { baseViewModel.updateSetting("auto_logout_inactivity", it) }
                    )
                }
                
                item {
                    SettingsSwitchItem(
                        title = "Biometric Authentication",
                        subtitle = "Use fingerprint or face unlock",
                        checked = baseViewModel.biometricAuth.value,
                        onCheckedChange = { baseViewModel.updateSetting("biometric_auth", it) }
                    )
                }

                // Data Management
                item {
                    SettingsSectionHeader("Data Management", Icons.Default.Settings)
                }
                item {
                    SettingsActionItem(
                        title = "Export App Data",
                        subtitle = "Export all data to xlsx file",
                        icon = Icons.Default.CloudCircle,
                        onClick = {
                            subNavController.navigate(SubScreens.BackUpSetting.route)
                        }
                    )
                }

                item {
                    SettingsActionItem(
                        title = "Reset App Preferences",
                        subtitle = "Reset all settings to default",
                        icon = Icons.Default.Refresh,
                        onClick = {
                            baseViewModel.resetAppPreferences()
                        }
                    )
                }
                
                item {
                    SettingsActionItem(
                        title = "Wipe All Data",
                        subtitle = "Delete all data and return to login",
                        icon = Icons.Default.DeleteForever,
                        onClick = { baseViewModel.showDataWipeConfirmation.value = true },
                        isDestructive = true
                    )
                }

                // About & Support
                item {
                    SettingsSectionHeader("About & Support", Icons.Default.Info)
                }
                
                item {
                    SettingsInfoItem(
                        title = "App Version",
                        subtitle = baseViewModel.getAppVersion(context)
                    )
                }
                
                item {
                    SettingsActionItem(
                        title = "Check for Updates",
                        subtitle = "Check for app updates",
                        icon = Icons.Default.Refresh,
                        onClick = { 
                            baseViewModel.checkForUpdates(context, forceCheck = true)
                        }
                    )
                }
            }
        }
    }

    // Data Wipe Confirmation Dialog
    if (baseViewModel.showDataWipeConfirmation.value) {
        AlertDialog(
            onDismissRequest = { baseViewModel.showDataWipeConfirmation.value = false },
            title = { Text("Confirm Data Wipe") },
            text = { Text("This action will permanently delete all your data including:\n\n• All inventory items\n• Customer information\n• Order history\n• Store settings\n• User data\n\nThis action cannot be undone. Are you sure you want to continue?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        baseViewModel.showDataWipeConfirmation.value = false
                        baseViewModel.initiateDataWipe()
                    }
                ) {
                    Text("Yes, Wipe All Data", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { baseViewModel.showDataWipeConfirmation.value = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // PIN Verification Dialog
    if (baseViewModel.showPinVerificationDialog.value) {
        PinVerificationDialog(
            onPinSubmit = { pin -> baseViewModel.verifyPinForWipe(pin) },
            onDismiss = { baseViewModel.showPinVerificationDialog.value = false }
        )
    }

    // OTP Verification Dialog
    if (baseViewModel.showOtpVerificationDialog.value) {
        OtpVerificationDialog(
            onOtpSubmit = { otp -> baseViewModel.verifyOtpForWipe(otp) },
            onDismiss = { baseViewModel.showOtpVerificationDialog.value = false },
            isLoading = baseViewModel.isWipeInProgress.value
        )
    }
    
    // Show update dialogs if needed
    if (baseViewModel.showUpdateDialog.value) {
        baseViewModel.updateInfo.value?.let { updateInfo ->
            OptionalUpdateDialog(
                updateInfo = updateInfo,
                onUpdateClick = { baseViewModel.onUpdateClick(context) },
                onDismiss = { baseViewModel.dismissUpdateDialog() },
                onBackupClick = { baseViewModel.onUpdateClick(context) }
            )
        }
    }
    
    if (baseViewModel.showForceUpdateDialog.value) {
        baseViewModel.updateInfo.value?.let { updateInfo ->
            com.velox.jewelvault.ui.components.ForceUpdateDialog(
                updateInfo = updateInfo,
                onUpdateClick = { baseViewModel.onUpdateClick(context) }
            )
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    Row(
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
fun SettingsSliderItem(
    title: String,
    subtitle: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun SettingsTextInputItem(
    title: String,
    subtitle: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                singleLine = true
            )
        }
    }
}

@Composable
fun SettingsActionItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDestructive) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (isDestructive) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingsInfoItem(
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PinVerificationDialog(
    onPinSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Enter PIN",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Please enter your PIN to continue",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = pin,
                    onValueChange = { 
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            pin = it
                            showError = false
                        }
                    },
                    label = { Text("PIN") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                    ),
                    singleLine = true,
                    isError = showError,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (showError) {
                    Text(
                        text = "PIN is required",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            if (pin.isNotEmpty()) {
                                onPinSubmit(pin)
                            } else {
                                showError = true
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Continue")
                    }
                }
            }
        }
    }
}

@Composable
fun OtpVerificationDialog(
    onOtpSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean
) {
    var otp by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    
    Dialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = !isLoading, dismissOnClickOutside = !isLoading)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Sms,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = if (isLoading) "Wiping Data..." else "Enter OTP",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = if (isLoading) "Please wait while we wipe all data" else "Please enter the OTP sent to your phone",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                if (!isLoading) {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    OutlinedTextField(
                        value = otp,
                        onValueChange = { 
                            if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                otp = it
                                showError = false
                            }
                        },
                        label = { Text("OTP") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        singleLine = true,
                        isError = showError,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (showError) {
                        Text(
                            text = "OTP is required",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        
                        Button(
                            onClick = {
                                if (otp.isNotEmpty()) {
                                    onOtpSubmit(otp)
                                } else {
                                    showError = true
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Verify")
                        }
                    }
                }
            }
        }
    }
}