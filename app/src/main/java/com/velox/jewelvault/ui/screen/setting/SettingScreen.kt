package com.velox.jewelvault.ui.screen.setting

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.velox.jewelvault.ui.nav.Screens
import com.velox.jewelvault.ui.nav.SubScreens
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
    LaunchedEffect(baseViewModel.wipeCompleted.value) {
        if (baseViewModel.wipeCompleted.value) {
            mainNavController.navigate(Screens.Login.route) {
                popUpTo(0) { inclusive = true }
            }
            baseViewModel.wipeCompleted.value = false
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

            // Settings Content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Network Settings
                item {
                    SettingsSectionHeader("Network & Connectivity", Icons.TwoTone.Wifi)
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
                    SettingsSectionHeader("Business Settings", Icons.TwoTone.Business)
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
                    SettingsSectionHeader("Security", Icons.TwoTone.Security)
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

                // Subscription
                item {
                    SettingsSectionHeader("Subscription", Icons.TwoTone.Star)
                }
                item {
                    SettingsActionItem(
                        title = "Subscription Details",
                        subtitle = "View your active plan and features",
                        icon = Icons.TwoTone.Star,
                        onClick = { subNavController.navigate(SubScreens.SubscriptionDetails.route) }
                    )
                }

                // Permissions
                item {
                    SettingsSectionHeader("Permissions", Icons.TwoTone.Lock)
                }

                item {
                    SettingsActionItem(
                        title = "Manage App Permissions",
                        subtitle = "Enable or disable camera, storage, notifications, and more",
                        icon = Icons.TwoTone.Lock,
                        onClick = { subNavController.navigate(SubScreens.Permissions.route) }
                    )
                }

                // Data Management
                item {
                    SettingsSectionHeader("Data Management", Icons.TwoTone.Settings)
                }
                item {
                    SettingsActionItem(
                        title = "Sync & Export App Data",
                        subtitle = "Sync & Export all data to xlsx file",
                        icon = Icons.TwoTone.CloudCircle,
                        onClick = {
                            subNavController.navigate(SubScreens.BackUpSetting.route)
                        }
                    )
                }

                item {
                    SettingsActionItem(
                        title = "Share Logs",
                        subtitle = "Share all app logs",
                        icon = Icons.TwoTone.Share,
                        onClick = { baseViewModel.shareLogs(context) }
                    )
                }

                item {
                    SettingsActionItem(
                        title = "Reset App Preferences",
                        subtitle = "Reset all settings to default",
                        icon = Icons.TwoTone.Refresh,
                        onClick = {
                            baseViewModel.resetAppPreferences()
                        }
                    )
                }
                
                item {
                    SettingsActionItem(
                        title = "Wipe All Data",
                        subtitle = "Delete all data and return to login",
                        icon = Icons.TwoTone.DeleteForever,
                        onClick = { baseViewModel.showDataWipeConfirmation.value = true },
                        isDestructive = true
                    )
                }

                // Legal & Support
                item {
                    SettingsSectionHeader("Legal & Support", Icons.TwoTone.Info)
                }
                
                item {
                    SettingsActionItem(
                        title = "Privacy Policy",
                        subtitle = "View our privacy policy",
                        icon = Icons.TwoTone.Security,
                        onClick = {
                            subNavController.navigate(SubScreens.PrivacyPolicy.route)
                        }
                    )
                }
                
                item {
                    SettingsActionItem(
                        title = "Terms & Conditions",
                        subtitle = "View terms and conditions",
                        icon = Icons.TwoTone.Description,
                        onClick = {
                            subNavController.navigate(SubScreens.TermsAndConditions.route)
                        }
                    )
                }
                
                // About & Support
                item {
                    SettingsSectionHeader("About & Support", Icons.TwoTone.Info)
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
                        icon = Icons.TwoTone.Refresh,
                        onClick = { 
                            baseViewModel.checkForUpdates(context, forceCheck = true)
                        }
                    )
                }
            }
        }
    }

    // Data Wipe Confirmation Dialog logic...
    if (baseViewModel.showDataWipeConfirmation.value) {
        AlertDialog(
            onDismissRequest = { baseViewModel.showDataWipeConfirmation.value = false },
            title = { Text("Wipe All Data?") },
            text = { Text("This will permanently delete all your data and logout. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        baseViewModel.showDataWipeConfirmation.value = false
                        baseViewModel.initiateDataWipe()
                    }
                ) {
                    Text("WIPE DATA", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { baseViewModel.showDataWipeConfirmation.value = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    if (baseViewModel.showPinVerificationDialog.value) {
        var pin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { baseViewModel.showPinVerificationDialog.value = false },
            title = { Text("Enter PIN") },
            text = {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4) pin = it },
                    label = { Text("4-Digit PIN") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = { baseViewModel.verifyPinForWipe(pin) }) {
                    Text("Verify")
                }
            }
        )
    }

    if (baseViewModel.showOtpVerificationDialog.value) {
        var otp by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { baseViewModel.showOtpVerificationDialog.value = false },
            title = { Text("Verify OTP") },
            text = {
                Column {
                    Text("Enter the OTP sent to your registered mobile number.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = otp,
                        onValueChange = { if (it.length <= 6) otp = it },
                        label = { Text("6-Digit OTP") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = { baseViewModel.verifyOtpForWipe(otp) }) {
                    Text("Confirm Wipe")
                }
            },
            dismissButton = {
                TextButton(onClick = { baseViewModel.resendOtpForWipe() }) {
                    Text("Resend OTP")
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun SettingsActionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDestructive) MaterialTheme.colorScheme.error.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.TwoTone.ChevronRight,
                contentDescription = null,
                tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
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
fun SettingsTextInputItem(
    title: String,
    subtitle: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var textValue by remember { mutableStateOf(value) }

    Surface(
        onClick = { isEditing = true },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (isEditing) {
        AlertDialog(
            onDismissRequest = { isEditing = false },
            title = { Text(title) },
            text = {
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onValueChange(textValue)
                    isEditing = false
                }) {
                    Text("SAVE")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    textValue = value
                    isEditing = false
                }) {
                    Text("CANCEL")
                }
            }
        )
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
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps
            )
        }
    }
}

@Composable
fun SettingsInfoItem(
    title: String,
    subtitle: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
