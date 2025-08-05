package com.velox.jewelvault.ui.components

import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.velox.jewelvault.R
import com.velox.jewelvault.data.MetalRatesTicker
import com.velox.jewelvault.ui.theme.ZenFontFamily
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.VaultPreview
import com.velox.jewelvault.utils.ioScope
import com.velox.jewelvault.ui.components.OptionalUpdateDialog
import kotlinx.coroutines.launch

@Composable
@VaultPreview
fun TabNavigationDrawerPreview() {
    val inputIconStates = List(6) { index ->
        InputIconState(
            initialText = when (index) {
                0 -> "Dashboard"
                1 -> "Inventory"
                2 -> "Customers"
                3 -> "Ledger"
                4 -> "Profile"
                else -> "Settings"
            },
            initialIcon = when (index) {
                0 -> Icons.Default.Dashboard
                1 -> Icons.Default.Inventory
                2 -> Icons.Default.People
                3 -> Icons.Default.AccountBalance
                4 -> Icons.Default.Person
                else -> Icons.Default.Settings
            },
            initialOnClick = {
                println("Item ${index + 1} clicked")
            }
        )
    }

    val drawerState = rememberTabDrawerState(TabDrawerValue.Closed)

    TabNavigationDrawer(
        drawerState = drawerState,
        content = {
            Text("Main Content")
        }, drawerContent = {
            LazyColumn(
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                items(inputIconStates) { item ->
                    DrawerItem(
                        item = item,
                        drawerState = drawerState,
                        onClick = { item.onClick() }
                    )
                }
            }
        }
    )
}

@Composable
fun DrawerItem(
    item: InputIconState,
    drawerState: TabDrawerState,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .clickable { onClick() }
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon with better styling
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.1f),
                    shape = androidx.compose.foundation.shape.CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon as ImageVector,
                contentDescription = item.text,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
        
        if (drawerState.isOpen) {
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = item.text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

enum class TabDrawerValue {
    Closed,
    Open
}

@Composable
fun rememberTabDrawerState(
    initialValue: TabDrawerValue,
    confirmStateChange: (TabDrawerValue) -> Boolean = { true }
): TabDrawerState {
    return rememberSaveable(saver = TabDrawerState.Saver(confirmStateChange)) {
        TabDrawerState(initialValue, confirmStateChange)
    }
}

private val AnimationSpec = TweenSpec<Float>(durationMillis = 256)
private val DrawerVelocityThreshold = 400.dp
private val MinimumDrawerWidth = 240.dp

@Suppress("NotCloseable")
@Stable
class TabDrawerState(
    initialValue: TabDrawerValue,
    val confirmStateChange: (TabDrawerValue) -> Boolean = { true }
) {
    var currentValue by mutableStateOf(initialValue)

    val isOpen: Boolean get() = currentValue == TabDrawerValue.Open
    val isClosed: Boolean get() = currentValue == TabDrawerValue.Closed

    fun open() {
        if (confirmStateChange(TabDrawerValue.Open)) currentValue = TabDrawerValue.Open
    }

    fun close() {
        if (confirmStateChange(TabDrawerValue.Closed)) currentValue = TabDrawerValue.Closed
    }

    fun requireOffset(): Float = if (isOpen) 0f else -MinimumDrawerWidth.value

    companion object {
        fun Saver(confirmStateChange: (TabDrawerValue) -> Boolean): Saver<TabDrawerState, TabDrawerValue> =
            Saver(
                save = { it.currentValue },
                restore = { TabDrawerState(it, confirmStateChange) }
            )
    }
}

@Composable
fun TabNavigationDrawer(
    modifier: Modifier = Modifier,
    drawerState: TabDrawerState = rememberTabDrawerState(TabDrawerValue.Closed),
    notifierContent: @Composable () -> Unit = {},
    drawerContent: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val baseViewModel = LocalBaseViewModel.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        baseViewModel.loadStoreName()
    }


    val width = if (drawerState.isOpen) 280.dp else 60.dp
    Row {
        Row(modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxHeight()
                    .width(width)
                    .background(
                        color = MaterialTheme.colorScheme.primary
                    )
                    .padding(8.dp)
            ) {

                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    ProfileImage(drawerState)
                    drawerContent()
                }

                Row(
                    modifier = Modifier
                        .clickable {
                            drawerState.currentValue =
                                if (drawerState.isOpen) TabDrawerValue.Closed else TabDrawerValue.Open
                        }
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Icon(
                        if (drawerState.isOpen) Icons.AutoMirrored.Filled.KeyboardArrowLeft
                        else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = if (drawerState.isOpen) "Close drawer" else "Open drawer",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Column(Modifier.fillMaxSize()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .background(
                            color = MaterialTheme.colorScheme.primary
                        )
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth().wrapContentHeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(Modifier.width(20.dp))
                        Text(
                            text = baseViewModel.storeName.value ?:"Jewel Vault",
                            fontSize = 22.sp,
                            fontFamily = ZenFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.bounceClick {
                                ioScope.launch {
                                    baseViewModel.refreshMetalRates(context = context)
                                }
                            }.padding(10.dp)
                        )
                        Spacer(Modifier.width(20.dp))
                        MetalRatesTicker(Modifier.height(50.dp).weight(1f), backgroundColor = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(20.dp))
                        notifierContent()
                        Spacer(Modifier.width(10.dp))
                        
                        // Version info and update notification
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "v${baseViewModel.remoteConfigManager.getCurrentAppVersionName()}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Normal
                            )
                            
                            // Show update notification if available
                            if (baseViewModel.updateInfo.value != null) {
                                // Note: We can't call suspend function here, so we'll show based on updateInfo
                                val currentVersion = baseViewModel.remoteConfigManager.getCurrentAppVersion()
                                val latestVersion = baseViewModel.updateInfo.value?.latestVersionCode ?: currentVersion
                                if (latestVersion > currentVersion) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Update,
                                        contentDescription = "Update Available",
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = "Update",
                                        fontSize = 8.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        }
                    }
                    Box(Modifier.fillMaxSize()) {
                        content()
                    }
                }
            }
        }
    }
}

class InputIconState(
    initialText: String = "",
    initialIcon: Any? = null, // Can be ImageVector or Int resource
    selected: Boolean = false,
    initialOnClick: () -> Unit = {}
) {
    var text by mutableStateOf(initialText)
    var icon by mutableStateOf(initialIcon)
    var selected by mutableStateOf(selected)
    var onClick by mutableStateOf(initialOnClick)

    fun onTextChanged(newText: String) {
        text = newText
    }
}

@Composable
private fun TabScrim(
    open: Boolean,
    onClose: () -> Unit,
    fraction: () -> Float,
    color: Color
) {
    val closeDrawer = "Closed"
    val dismissDrawer = if (open) {
        Modifier
            .pointerInput(onClose) { detectTapGestures { onClose() } }
            .semantics(mergeDescendants = true) {
                contentDescription = closeDrawer
                onClick { onClose(); true }
            }
    } else {
        Modifier
    }

    Canvas(
        Modifier
            .fillMaxSize()
            .then(dismissDrawer)
    ) {
        drawRect(color, alpha = fraction())
    }
}

fun calculateFraction(a: Float, b: Float, pos: Float) =
    ((pos - a) / (b - a)).coerceIn(0f, 1f)

@Composable
fun ProfileImage(drawerState: TabDrawerState) {
    val baseViewModel = LocalBaseViewModel.current
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        baseViewModel.loadStoreImage()
    }

    
    Box(
        modifier = Modifier
            .size(60.dp)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        val imageUri = baseViewModel.storeImage.value
        
        if (!imageUri.isNullOrBlank()) {
            // Show profile image if available
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(context)
                        .data(imageUri)
                        .build()
                ),
                contentDescription = "Profile Image",
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f),
                        shape = CircleShape
                    ),
                contentScale = ContentScale.Crop
            )
        } else {
            // Show default profile icon if no image
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.1f),
                        shape = CircleShape
                    )
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}




