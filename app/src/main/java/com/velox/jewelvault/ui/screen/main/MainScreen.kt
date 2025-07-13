package com.velox.jewelvault.ui.screen.main

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
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

@Composable
fun MainScreen() {
    val baseViewModel = LocalBaseViewModel.current
    val context = LocalContext.current
    val subNavController = rememberNavController()

    LaunchedEffect(true) {
        if (baseViewModel.metalRates.isNotEmpty()) {
            baseViewModel.refreshMetalRates(context = context)
        }
    }
    val inputIconStates = listOf(
        InputIconState(
            "Dashboard", Icons.Default.Dashboard
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
            "Profile", Icons.Default.Person
        ) {
            subNavController.navigate("${SubScreens.Profile.route}/${false}") {
                popUpTo(SubScreens.Dashboard.route) {
                    inclusive = true
                }
            }
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

    if (isLandscape()) {
        LandscapeDashboardScreen(inputIconStates, subNavController)
    } else {
        PortraitDashboardScreen(inputIconStates)
    }
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
            TopAppBar(title = { Text("Jewel Vault") }, navigationIcon = {
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

    TabNavigationDrawer(drawerState = drawerState, content = {
        SubAppNavigation(
            subNavController, navHost, baseViewModel, startDestination = SubScreens.Dashboard.route
        )
    }, drawerContent = {
        LazyColumn {
            items(inputIconStates) { item ->
                Row(Modifier
                    .clickable {
                        item.onClick.invoke()
                    }
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    item.icon?.let { icon ->
                        if (icon is androidx.compose.ui.graphics.vector.ImageVector) {
                            Icon(
                                imageVector = icon,
                                contentDescription = item.text,
                                Modifier
                                    .padding(start = 5.dp)
                                    .size(30.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Image(
                                painter = painterResource(icon as Int),
                                contentDescription = item.text,
                                Modifier
                                    .padding(start = 5.dp)
                                    .size(30.dp)
                            )
                        }
                    }
                    if (drawerState.isOpen) {
                        Spacer(Modifier.width(10.dp))
                        Text(item.text, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    })
}




