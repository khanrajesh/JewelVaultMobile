package com.velox.jewelvault.ui.screen

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.velox.jewelvault.R
import com.velox.jewelvault.ui.components.InputIconState
import com.velox.jewelvault.ui.components.QrBarScannerPage
import com.velox.jewelvault.ui.components.TabDrawerValue
import com.velox.jewelvault.ui.components.TabNavigationDrawer
import com.velox.jewelvault.ui.components.rememberTabDrawerState
import com.velox.jewelvault.ui.nav.AppNavigation
import com.velox.jewelvault.ui.nav.Screens
import com.velox.jewelvault.ui.nav.SubAppNavigation
import com.velox.jewelvault.ui.nav.SubScreens
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.LocalNavController
import com.velox.jewelvault.utils.VaultPreview
import com.velox.jewelvault.utils.isLandscape
import kotlinx.coroutines.launch

@Composable
@VaultPreview
fun DashboardScreenPreview(){
    DashboardScreen()
}

@Composable
fun DashboardScreen(){
    val baseViewModel = LocalBaseViewModel.current
    val context = LocalContext.current

    val showBarcodeScanPage = remember { mutableStateOf(false) }

    LaunchedEffect(true) {
        if (baseViewModel.metalRates.isNotEmpty()){
            baseViewModel.refreshMetalRates(context = context)
        }
    }
    val inputIconStates = List(5) { index ->
        InputIconState(
            initialText = "Item ${index + 1}",
            initialIcon = when (index) {
                0 -> R.drawable.dashboard_tr // Replace with your actual icon resource IDs
                1 -> R.drawable.category_tr
                2 -> R.drawable.trading_ts
                3 -> R.drawable.settings_rr
                else -> R.drawable.logo_1
            },
            initialOnClick = {
                println("Item ${index + 1} clicked")
            }
        )
    }

        if (isLandscape()){
            LandscapeDashboardScreen(inputIconStates)
        }else{
            PortraitDashboardScreen(inputIconStates)
        }

}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PortraitDashboardScreen(inputIconStates: List<InputIconState>) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.primary,
            ) {
                Text("Drawer Item 1", modifier = Modifier.padding(16.dp))
                Text("Drawer Item 2", modifier = Modifier.padding(16.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Jewel Vault") },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                drawerState.open() // Open drawer on button click
                            }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        ) { innerPadding ->
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
private fun LandscapeDashboardScreen(inputIconStates: List<InputIconState>) {
  val drawerState = rememberTabDrawerState(TabDrawerValue.Closed)
    val navHost = LocalNavController.current
    val baseViewModel = LocalBaseViewModel.current
    val subNavController = rememberNavController()


    TabNavigationDrawer(
        drawerState = drawerState,
        content = {
            SubAppNavigation(
                subNavController,
                navHost,
                baseViewModel,
                startDestination = SubScreens.Main.route
            )
        },
        drawerContent = {
            LazyColumn() {
                items(inputIconStates) { item ->
                    Row(
                        Modifier
                            .clickable {
                                item.onClick()
                            }
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item.icon?.let {
                            Image(
                                painter = painterResource(it),
                                contentDescription = null,
                                Modifier.size(30.dp)
                            )
                        }
                        if (drawerState.isOpen) {
                            Spacer(Modifier.width(10.dp))
                            Text(item.text)
                        }
                    }
                }
            }
        }
    )
}




