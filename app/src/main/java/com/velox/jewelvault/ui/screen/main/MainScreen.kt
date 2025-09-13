package com.velox.jewelvault.ui.screen.main

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import android.content.Intent
import android.net.Uri
import android.os.Environment
import java.io.File
import com.velox.jewelvault.R
import com.velox.jewelvault.ui.components.InputIconState
import com.velox.jewelvault.ui.components.TabDrawerValue
import com.velox.jewelvault.ui.components.TabNavigationDrawer
import com.velox.jewelvault.ui.components.rememberTabDrawerState
import com.velox.jewelvault.ui.nav.SubAppNavigation
import com.velox.jewelvault.ui.nav.SubScreens
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.LocalNavController
import com.velox.jewelvault.utils.VaultPreview
import com.velox.jewelvault.utils.isLandscape
import kotlinx.coroutines.launch


@Composable
@VaultPreview
fun MainScreenPreview() {
    MainScreen()
}

// Function to show file manager dialog with options
fun showFileManagerDialog(context: android.content.Context, navController: NavHostController, inputIconStates: List<InputIconState>) {
    try {
        val jewelVaultFolder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "JewelVault")
        
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
            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("JewelVault Path", folderPath)
            clipboard.setPrimaryClip(clip)
            android.widget.Toast.makeText(context, "Path copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
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
fun openFileManager(context: android.content.Context, navController: NavHostController, inputIconStates: List<InputIconState>) {
    try {
        val jewelVaultFolder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "JewelVault")
        
        // Create the folder if it doesn't exist
        if (!jewelVaultFolder.exists()) {
            jewelVaultFolder.mkdirs()
        }
        
        android.util.Log.d("FileManager", "JewelVault folder path: ${jewelVaultFolder.absolutePath}")
        android.util.Log.d("FileManager", "JewelVault folder exists: ${jewelVaultFolder.exists()}")
        
        // Method 1: Try to open specific folder with file:// URI
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val uri = Uri.fromFile(jewelVaultFolder)
            intent.setDataAndType(uri, "resource/folder")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            android.util.Log.d("FileManager", "Trying file URI: $uri")
            context.startActivity(intent)
            
            // Update selection state to dashboard
            inputIconStates.forEach { it.selected = false }
            inputIconStates.find { it.text == "Dashboard" }?.selected = true
            
            // Ensure user returns to dashboard when they come back
            navController.navigate(SubScreens.Dashboard.route) {
                popUpTo(SubScreens.Dashboard.route) {
                    inclusive = true
                }
            }
            return
        } catch (e: Exception) {
            android.util.Log.d("FileManager", "File URI method failed: ${e.message}")
        }
        
        // Method 2: Try with content:// URI for DocumentsUI
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            // Try different URI formats
            val uri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload%2FJewelVault")
            intent.setData(uri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            android.util.Log.d("FileManager", "Trying content URI: $uri")
            context.startActivity(intent)
            
            // Update selection state to dashboard
            inputIconStates.forEach { it.selected = false }
            inputIconStates.find { it.text == "Dashboard" }?.selected = true
            
            // Ensure user returns to dashboard when they come back
            navController.navigate(SubScreens.Dashboard.route) {
                popUpTo(SubScreens.Dashboard.route) {
                    inclusive = true
                }
            }
            return
        } catch (e: Exception) {
            android.util.Log.d("FileManager", "Content URI method failed: ${e.message}")
        }
        
        // Method 3: Try with different content URI format
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val uri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3ADownload%2FJewelVault")
            intent.setData(uri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            android.util.Log.d("FileManager", "Trying tree URI: $uri")
            context.startActivity(intent)
            
            // Update selection state to dashboard
            inputIconStates.forEach { it.selected = false }
            inputIconStates.find { it.text == "Dashboard" }?.selected = true
            
            // Ensure user returns to dashboard when they come back
            navController.navigate(SubScreens.Dashboard.route) {
                popUpTo(SubScreens.Dashboard.route) {
                    inclusive = true
                }
            }
            return
        } catch (e: Exception) {
            android.util.Log.d("FileManager", "Tree URI method failed: ${e.message}")
        }
        
        // Method 4: Try to open Downloads folder as fallback
        try {
            val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val intent = Intent(Intent.ACTION_VIEW)
            val uri = Uri.fromFile(downloadsFolder)
            intent.setDataAndType(uri, "resource/folder")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            android.util.Log.d("FileManager", "Trying Downloads folder: $uri")
            context.startActivity(intent)
            
            // Update selection state to dashboard
            inputIconStates.forEach { it.selected = false }
            inputIconStates.find { it.text == "Dashboard" }?.selected = true
            
            // Ensure user returns to dashboard when they come back
            navController.navigate(SubScreens.Dashboard.route) {
                popUpTo(SubScreens.Dashboard.route) {
                    inclusive = true
                }
            }
            return
        } catch (e: Exception) {
            android.util.Log.d("FileManager", "Downloads folder method failed: ${e.message}")
        }
        
        // Method 5: Try with generic file manager
        try {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            android.util.Log.d("FileManager", "Trying generic file manager")
            context.startActivity(Intent.createChooser(intent, "Open File Manager"))
            
            // Update selection state to dashboard
            inputIconStates.forEach { it.selected = false }
            inputIconStates.find { it.text == "Dashboard" }?.selected = true
            
            // Ensure user returns to dashboard when they come back
            navController.navigate(SubScreens.Dashboard.route) {
                popUpTo(SubScreens.Dashboard.route) {
                    inclusive = true
                }
            }
            return
        } catch (e: Exception) {
            android.util.Log.d("FileManager", "Generic file manager method failed: ${e.message}")
        }
        
        // If all methods fail, show error
        android.util.Log.e("FileManager", "All file manager methods failed")
        
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
            "Dashboard", Icons.Default.Dashboard,
            selected = true
        ) {
            subNavController.navigate(SubScreens.Dashboard.route) {
                popUpTo(SubScreens.Dashboard.route) {
                    inclusive = true
                }
            }
        },
        InputIconState(
            "Inventory", Icons.Default.Inventory
        ) {
            subNavController.navigate(SubScreens.Inventory.route) {
                popUpTo(SubScreens.Dashboard.route) {
                    inclusive = true
                }
            }
        },
        InputIconState(
            "Customers", Icons.Default.People
        ) {
            subNavController.navigate(SubScreens.Customers.route) {
                popUpTo(SubScreens.Dashboard.route) {
                    inclusive = true
                }
            }
        },
        InputIconState(
            "Ledger", Icons.Default.AccountBalance
        ) {
            subNavController.navigate(SubScreens.OrderAndPurchase.route) {
                popUpTo(SubScreens.Dashboard.route) {
                    inclusive = true
                }
            }
        },
        InputIconState(
            "Audit", Icons.Default.QrCodeScanner
        ) {
            subNavController.navigate(SubScreens.Audit.route) {
                popUpTo(SubScreens.Dashboard.route) {
                    inclusive = true
                }
            }
        },
        InputIconState(
            "File Manager", Icons.Default.Folder
        ) {
            // This will be handled after inputIconStates is created
        },
        InputIconState(
            "Setting", Icons.Default.Settings
        ) {
            subNavController.navigate(SubScreens.Setting.route) {
                popUpTo(SubScreens.Dashboard.route) {
                    inclusive = true
                }
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

//    if (isLandscape()) {
        LandscapeDashboardScreen(inputIconStates, subNavController)
//    } else {
//        PortraitDashboardScreen(inputIconStates)
//    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PortraitDashboardScreen(inputIconStates: List<InputIconState>) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()



    ModalNavigationDrawer(drawerState = drawerState, drawerContent = {
        ModalDrawerSheet(
            drawerContainerColor = MaterialTheme.colorScheme.primary,
        ) {
            Text("Drawer Item 1", modifier = Modifier.padding(16.dp))
            Text("Drawer Item 2", modifier = Modifier.padding(16.dp))
        }
    }) {
        Scaffold(topBar = {
            TopAppBar(
                title = { Text("") }, navigationIcon = {
                    IconButton(onClick = {
                        scope.launch {
                            drawerState.open() // Open drawer on button click
                        }
                    }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }, colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                Text("Main content goes here")
            }
        }
    }
}

@SuppressLint("SuspiciousIndentation")
@Composable
private fun LandscapeDashboardScreen(
    inputIconStates: List<InputIconState>, subNavController: NavHostController
) {
    val drawerState = rememberTabDrawerState(TabDrawerValue.Closed)
    val navHost = LocalNavController.current
    val baseViewModel = LocalBaseViewModel.current

    val currentUser = baseViewModel.dataStoreManager.getCurrentLoginUser()

    TabNavigationDrawer(
        drawerState = drawerState,
        content = {
            SubAppNavigation(
                subNavController,
                navHost,
                baseViewModel,
                startDestination = SubScreens.Dashboard.route
            )
        },
        onProfileClick = {
            subNavController.navigate("${SubScreens.Profile.route}/${false}") {
                popUpTo(SubScreens.Dashboard.route) {
                    inclusive = true
                }
            }
        },
        drawerContent = {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(inputIconStates) { item ->
                    Column(
                        horizontalAlignment = if (drawerState.isOpen) Alignment.Start else Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier
                                .clickable {
                                    inputIconStates.forEach { it.selected = false }
                                    item.selected = true
                                    item.onClick.invoke()
                                }
                                .fillMaxWidth()
                                .padding( vertical = 4.dp, horizontal = 4.dp ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = if (drawerState.isOpen) Arrangement.Start else Arrangement.Center
                        ) {
                            item.icon?.let { icon ->
                                if (icon is androidx.compose.ui.graphics.vector.ImageVector) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = item.text,
                                        Modifier.size(28.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(icon as Int),
                                        contentDescription = item.text,
                                        Modifier.size(32.dp)
                                    )
                                }
                            }
                            if (drawerState.isOpen) {
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    item.text,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }

                        if (item.selected) {
                            baseViewModel.currentScreenHeading = item.text

                            Spacer(
                                Modifier
                                    .fillMaxWidth(0.8f)
                                    .height(4.dp)
                                    .background(
                                        MaterialTheme.colorScheme.onPrimary,
                                        RoundedCornerShape(2.dp)
                                    )
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        },
        notifierContent = {
            Column(
                modifier = Modifier
            ) {
                Text(
                    buildAnnotatedString {

                        withStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = MaterialTheme.typography.labelLarge.fontSize,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            append(currentUser.name.uppercase())
                        }
                        append("\n")
                        withStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            append(currentUser.role.uppercase())
                        }


                    },
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.End
                )
            }
        })
}




