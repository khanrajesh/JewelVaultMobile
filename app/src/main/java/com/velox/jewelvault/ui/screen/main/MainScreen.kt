package com.velox.jewelvault.ui.screen.main

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.Help
import androidx.compose.material.icons.automirrored.twotone.ReceiptLong
import androidx.compose.material.icons.twotone.AssuredWorkload
import androidx.compose.material.icons.twotone.Dashboard
import androidx.compose.material.icons.twotone.People
import androidx.compose.material.icons.twotone.PictureAsPdf
import androidx.compose.material.icons.twotone.Print
import androidx.compose.material.icons.twotone.RuleFolder
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material.icons.twotone.Warehouse
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.velox.jewelvault.ui.components.BluetoothToggleIcon
import com.velox.jewelvault.ui.components.InputIconState
import com.velox.jewelvault.ui.components.TabDrawerValue
import com.velox.jewelvault.ui.components.TabNavigationDrawer
import com.velox.jewelvault.ui.components.rememberTabDrawerState
import com.velox.jewelvault.ui.nav.SubAppNavigation
import com.velox.jewelvault.ui.nav.SubScreens
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.LocalNavController
import com.velox.jewelvault.utils.isLandscape
import com.velox.jewelvault.utils.log
import java.io.File


// Function to show file manager dialog with options
fun showFileManagerDialog(
    context: android.content.Context,
    navController: NavHostController,
    inputIconStates: List<InputIconState>
) {
    try {
        val jewelVaultFolder = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "JewelVault"
        )

        // Create the folder if it doesn't exist
        if (!jewelVaultFolder.exists()) {
            jewelVaultFolder.mkdirs()
        }

        val folderPath = jewelVaultFolder.absolutePath
        android.util.Log.d("FileManager", "JewelVault folder path: $folderPath")

        // Show a simple dialog with the path and options
        val alertDialog = android.app.AlertDialog.Builder(context)
        alertDialog.setTitle("JewelVault Files")
        alertDialog.setMessage("JewelVault folder location:\n$folderPath\n\nChoose an option:")

        alertDialog.setPositiveButton("Open File Manager") { _, _ ->
            openFileManager(context, navController, inputIconStates)
        }

        alertDialog.setNeutralButton("Copy Path") { _, _ ->
            val clipboard =
                context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("JewelVault Path", folderPath)
            clipboard.setPrimaryClip(clip)
            android.widget.Toast.makeText(
                context,
                "Path copied to clipboard",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }

        alertDialog.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        alertDialog.show()

    } catch (e: Exception) {
        android.util.Log.e("FileManager", "Error showing file manager dialog: ${e.message}")
    }
}

// Function to open file manager and navigate to JewelVault folder
fun openFileManager(
    context: android.content.Context,
    navController: NavHostController,
    inputIconStates: List<InputIconState>
) {
    try {
        val jewelVaultFolder = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "JewelVault"
        )

        // Create the folder if it doesn't exist
        if (!jewelVaultFolder.exists()) {
            jewelVaultFolder.mkdirs()
        }

        android.util.Log.d(
            "FileManager",
            "JewelVault folder path: ${jewelVaultFolder.absolutePath}"
        )
        android.util.Log.d("FileManager", "JewelVault folder exists: ${jewelVaultFolder.exists()}")

        // Preferred: open system picker with initial location
        val initialUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Uri.parse("content://com.android.externalstorage.documents/document/primary:Download")
        } else {
            null
        }

        val pickerIntent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
            initialUri?.let {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, it)
            }
        }

        val fallbackIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(pickerIntent)
        } catch (e: Exception) {
            android.util.Log.d("FileManager", "Document tree picker failed: ${e.message}")
            context.startActivity(Intent.createChooser(fallbackIntent, "Select a file"))
        }

        // Update selection state to dashboard
        inputIconStates.forEach { it.selected = false }
        inputIconStates.find { it.text == "Dashboard" }?.selected = true

        // Ensure user returns to dashboard when they come back
        navController.navigate(SubScreens.Dashboard.route) {
            popUpTo(SubScreens.Dashboard.route) {
                inclusive = true
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("FileManager", "Error opening file manager: ${e.message}")
    }
}

@Composable
fun MainScreen() {
    val baseViewModel = LocalBaseViewModel.current
    val context = LocalContext.current
    val subNavController = rememberNavController()


    val inputIconStates = mutableListOf(
        InputIconState(
            "Dashboard", Icons.TwoTone.Dashboard, selected = true
        ) {
            subNavController.navigate(SubScreens.Dashboard.route) {
                popUpTo(SubScreens.Dashboard.route) {
                    inclusive = true
                }
            }
        },
        InputIconState(
            "Inventory", Icons.TwoTone.Warehouse
        ) {
            subNavController.navigate(SubScreens.Inventory.route) {
                popUpTo(SubScreens.Dashboard.route) {
                    inclusive = true
                }
            }
        },
        InputIconState(
            "Customers", Icons.TwoTone.People
        ) {
            subNavController.navigate(SubScreens.Customers.route) {
                popUpTo(SubScreens.Dashboard.route) {
                    inclusive = true
                }
            }
        },
        InputIconState(
            "Ledger", Icons.AutoMirrored.TwoTone.ReceiptLong
        ) {
            subNavController.navigate(SubScreens.OrderAndPurchase.route) {
                popUpTo(SubScreens.Dashboard.route) {
                    inclusive = true
                }
            }
        },
        InputIconState(
            "Audit", Icons.TwoTone.AssuredWorkload
        ) {
            subNavController.navigate(SubScreens.Audit.route) {
                popUpTo(SubScreens.Dashboard.route) {
                    inclusive = true
                }
            }
        },
        InputIconState(
            "File Manager", Icons.TwoTone.RuleFolder
        ) {
            // This will be handled after inputIconStates is created
        },
        InputIconState(
            "Device", Icons.TwoTone.Print
        ) {
            subNavController.navigate(SubScreens.BluetoothScanConnect.route) {
                popUpTo(SubScreens.Dashboard.route) {
                    inclusive = true
                }
            }
        },
        InputIconState(
            "PDF Templates", Icons.TwoTone.PictureAsPdf
        ) {
            subNavController.navigate(SubScreens.PdfTemplateList.route) {
                popUpTo(SubScreens.Dashboard.route) {
                    inclusive = true
                }
            }
        },
        InputIconState(
            "Setting", Icons.TwoTone.Settings
        ) {
            subNavController.navigate(SubScreens.Setting.route) {
                popUpTo(SubScreens.Dashboard.route) {
                    inclusive = true
                }
            }
        },
        InputIconState(
            "Guide", Icons.AutoMirrored.TwoTone.Help
        ) {
            baseViewModel.currentScreenHeading = "Guide & Feedback"
            subNavController.navigate(SubScreens.Guide.route) {
                popUpTo(SubScreens.Dashboard.route) { inclusive = false }
                launchSingleTop = true
            }
        },
    )

    // Update the File Manager click handler after inputIconStates is created
    val fileManagerIndex = inputIconStates.indexOfFirst { it.text == "File Manager" }
    if (fileManagerIndex != -1) {
        inputIconStates[fileManagerIndex].onClick = {
            showFileManagerDialog(context, subNavController, inputIconStates)
        }
    }

    LaunchedEffect(true) {
        if (baseViewModel.metalRates.isNotEmpty()) {
            baseViewModel.refreshMetalRates(context = context)
        }

    }

    LaunchedEffect(
        baseViewModel.pendingNotificationRoute.value, baseViewModel.pendingNotificationArg.value
    ) {
        val targetRoute = baseViewModel.pendingNotificationRoute.value
        if (!targetRoute.isNullOrBlank()) {
            val arg = baseViewModel.pendingNotificationArg.value
            val destination = if (arg.isNullOrBlank()) targetRoute else "$targetRoute/$arg"
            runCatching {
                subNavController.navigate(destination) {
                    launchSingleTop = true
                }
            }.onFailure {
                log("MainScreen: Failed to navigate from notification: ${it.message}")
            }
            baseViewModel.clearPendingNotificationNavigation()
        }
    }

    val drawerState = rememberTabDrawerState(TabDrawerValue.Closed)
    val navHost = LocalNavController.current

    val currentUser = baseViewModel.dataStoreManager.getCurrentLoginUser()

    TabNavigationDrawer(
        modifier = Modifier,
        drawerState = drawerState,
        inputIconStates = inputIconStates,
        content = {
            SubAppNavigation(
                subNavController,
                navHost,
                baseViewModel,
                startDestination = SubScreens.Dashboard.route,

                )
        },
        onProfileClick = {
            subNavController.navigate("${SubScreens.Profile.route}/${false}") {
                popUpTo(SubScreens.Dashboard.route) {
                    inclusive = true
                }
            }
        },
        currentUser = currentUser
    )
}





