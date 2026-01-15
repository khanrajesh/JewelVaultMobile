package com.velox.jewelvault.ui.components

import android.content.Context
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.twotone.KeyboardArrowRight
import androidx.compose.material.icons.twotone.AccountBalance
import androidx.compose.material.icons.twotone.Dashboard
import androidx.compose.material.icons.twotone.Inventory
import androidx.compose.material.icons.twotone.Menu
import androidx.compose.material.icons.twotone.People
import androidx.compose.material.icons.twotone.Person
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material.icons.twotone.Update
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.velox.jewelvault.BaseViewModel
import com.velox.jewelvault.data.MetalRatesTicker
import com.velox.jewelvault.data.roomdb.entity.users.UsersEntity
import com.velox.jewelvault.ui.theme.ZenFontFamily
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.VaultPreview
import com.velox.jewelvault.utils.ioScope
import com.velox.jewelvault.utils.isLandscape
import com.velox.jewelvault.utils.log
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
            }, initialIcon = when (index) {
                0 -> Icons.TwoTone.Dashboard
                1 -> Icons.TwoTone.Inventory
                2 -> Icons.TwoTone.People
                3 -> Icons.TwoTone.AccountBalance
                4 -> Icons.TwoTone.Person
                else -> Icons.TwoTone.Settings
            }, initialOnClick = {
                println("Item ${index + 1} clicked")
            })
    }

    val drawerState = rememberTabDrawerState(TabDrawerValue.Closed)

    TabNavigationDrawer(drawerState = drawerState, content = {
        Text("Main Content")
    }, inputIconStates = inputIconStates)
}

@Composable
fun DrawerItem(
    item: InputIconState, drawerState: TabDrawerState, onClick: () -> Unit
) {
    val isSelected = item.selected
    val containerColor =
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val contentColor =
        if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable { onClick() }) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = contentColor.copy(alpha = 0.12f), shape = CircleShape
                    ), contentAlignment = Alignment.Center
            ) {
                when (val icon = item.icon) {
                    is ImageVector -> {
                        Icon(
                            imageVector = icon,
                            contentDescription = item.text,
                            modifier = Modifier.size(24.dp),
                            tint = contentColor
                        )
                    }

                    is Int -> {
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = item.text,
                            modifier = Modifier.size(24.dp),
                            tint = contentColor
                        )
                    }
                }
            }

            if (drawerState.isOpen) {
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = contentColor
                )
            }
        }
    }
}

enum class TabDrawerValue {
    Closed, Open
}

@Composable
fun rememberTabDrawerState(
    initialValue: TabDrawerValue, confirmStateChange: (TabDrawerValue) -> Boolean = { true }
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
    initialValue: TabDrawerValue, val confirmStateChange: (TabDrawerValue) -> Boolean = { true }
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
            Saver(save = { it.currentValue }, restore = { TabDrawerState(it, confirmStateChange) })
    }
}

@Composable
fun TabNavigationDrawer(
    modifier: Modifier = Modifier,
    drawerState: TabDrawerState = rememberTabDrawerState(TabDrawerValue.Closed),
    inputIconStates: List<InputIconState>,
    onProfileClick: () -> Unit = {},
    currentUser: UsersEntity? = null,
    content: @Composable () -> Unit
) {
    rememberCoroutineScope()
    LocalDensity.current
    val baseViewModel = LocalBaseViewModel.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        baseViewModel.loadStoreName()
        baseViewModel.loadStoreImage()
    }

    if (isLandscape()) {
        LandscapeNavView(
            drawerState,
            modifier,
            onProfileClick,
            baseViewModel,
            context,
            inputIconStates,
            currentUser,
            content,

            )
    } else {
        PortraitNavView(
            drawerState,
            modifier,
            onProfileClick,
            baseViewModel,
            context,
            inputIconStates,
            currentUser,
            content
        )
    }


}

@Composable
private fun MenuHeaderCard(
    modifier: Modifier = Modifier,
    onProfileClick: () -> Unit,
    baseViewModel: BaseViewModel,
    currentUser: UsersEntity?
) {
    val store = baseViewModel.storeData.value
    val proprietorName = store?.proprietor?.takeIf { it.isNotBlank() }
    val proprietorPhone = store?.phone?.takeIf { it.isNotBlank() }

    Surface(
        modifier = modifier.bounceClick(onProfileClick),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top
        ) {
            ProfileImage(size = 56.dp, onProfileClick = onProfileClick)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Proprietor",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = proprietorName ?: "—",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (proprietorPhone != null) {
                    Text(
                        text = proprietorPhone,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                    )
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "User",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                if (currentUser != null) {
                    Text(
                        text = currentUser.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currentUser.mobileNo,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = currentUser.role.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                } else {
                    Text(
                        text = "—",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PortraitNavView(
    drawerState: TabDrawerState,
    modifier: Modifier,
    onProfileClick: () -> Unit,
    baseViewModel: BaseViewModel,
    context: Context,
    inputIconStates: List<InputIconState>,
    currentUser: UsersEntity? = null,
    content: @Composable (() -> Unit)
) {
    val bottomNavItems = inputIconStates.filter { item ->
        item.text.equals("Dashboard", ignoreCase = true) || item.text.equals(
            "Inventory", ignoreCase = true
        ) || item.text.equals("Customers", ignoreCase = true) || item.text.equals(
            "Customer", ignoreCase = true
        ) || item.text.equals("Ledger", ignoreCase = true) || item.text.equals(
            "Order&Purchase", ignoreCase = true
        ) || item.text.equals(
            "Order & Purchase", ignoreCase = true
        ) || item.text.equals(
            "OrderAndPurchase", ignoreCase = true
        ) || item.text.equals("Order And Purchase", ignoreCase = true)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .background(MaterialTheme.colorScheme.primary),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (drawerState.isOpen) drawerState.close() else drawerState.open()
                }) {
                    Icon(
                        imageVector = Icons.TwoTone.Menu,
                        contentDescription = "Menu",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Box(contentAlignment = Alignment.TopStart) {
                    Text(
                        text = (baseViewModel.storeName.value
                        ?: "Jewel Vault").substringBefore(" "),
                        fontSize = 22.sp,
                        fontFamily = ZenFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .bounceClick {
                                ioScope.launch {
                                    baseViewModel.refreshOnlineMetalRates(context = context)
                                }
                            }
                            .offset(y = (-8).dp))

                    // Current Screen Heading
                    if (baseViewModel.currentScreenHeading.isNotEmpty()) {
                        Text(
                            text = baseViewModel.currentScreenHeading,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.offset(y = (17).dp)
                        )
                    }
                }
                Spacer(Modifier.width(5.dp))
                MetalRatesTicker(
                    Modifier
                        .height(30.dp)
                        .weight(1f),
                    backgroundColor = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(5.dp))
                BluetoothToggleIcon(
                    Modifier.size(25.dp)
                )
                Spacer(Modifier.width(5.dp))

            }
            val onBottomNavItemSelected: (InputIconState) -> Unit = { selectedItem ->
                inputIconStates.forEach { it.selected = false }
                selectedItem.selected = true
                selectedItem.onClick.invoke()
            }

            Box(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 42.dp)
                ) {
                    content()
                }

                BottomNavBubbleBar(
                    items = bottomNavItems,
                    onItemSelected = onBottomNavItemSelected,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }


        }

        if (drawerState.isOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f))
                    .clickable { drawerState.close() })

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.8f)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .align(Alignment.CenterStart)
                    .padding(vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = baseViewModel.storeName.value ?: "Jewel Vault",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { drawerState.close() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.TwoTone.KeyboardArrowLeft,
                            contentDescription = "Close menu",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                MenuHeaderCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp), onProfileClick = {
                        drawerState.close()
                        onProfileClick()
                    }, baseViewModel = baseViewModel, currentUser = currentUser
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 4.dp),
                ) {
                    items(inputIconStates) { item ->
                        DrawerItem(item, drawerState) {
                            inputIconStates.forEach { it.selected = false }
                            item.selected = true
                            item.onClick.invoke()
                            drawerState.close()
                        }
                    }
                    item {
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "v${baseViewModel.remoteConfigManager.getCurrentAppVersionName()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

private fun bottomNavLabel(item: InputIconState): String = when {
    item.text.equals("ledger", ignoreCase = true) -> "Order & Purchase"
    item.text.equals("order&purchase", ignoreCase = true) -> "Order & Purchase"
    item.text.equals("order & purchase", ignoreCase = true) -> "Order & Purchase"
    item.text.equals("orderandpurchase", ignoreCase = true) -> "Order & Purchase"
    item.text.equals("order and purchase", ignoreCase = true) -> "Order & Purchase"
    else -> item.text
}

@Composable
private fun BottomNavIcon(
    icon: Any?,
    contentDescription: String,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    when (icon) {
        is ImageVector -> Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = modifier.size(size),
            tint = tint
        )

        is Int -> Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            modifier = modifier.size(size),
            tint = tint
        )
    }
}

@Composable
private fun BottomNavBubbleBar(
    items: List<InputIconState>,
    onItemSelected: (InputIconState) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = item.selected
                Column(modifier = Modifier
                    .weight(1f)
                    .bounceClick { onItemSelected(item) }
                    .padding(vertical = 2.dp)
                    .animateContentSize(),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    val bubbleSize = if (isSelected) 44.dp else 40.dp
                    val bubbleBorderWidth = 4.dp
                    val bubbleColor = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    Box(
                        modifier = Modifier
                            .size(bubbleSize)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bubbleBorderWidth)
                                .clip(CircleShape)
                                .background(bubbleColor),
                            contentAlignment = Alignment.Center
                        ) {
                            BottomNavIcon(
                                icon = item.icon,
                                contentDescription = item.text,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                size = 22.dp
                            )
                        }
                    }
                    if (isSelected) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = bottomNavLabel(item),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun LandscapeNavView(
    drawerState: TabDrawerState,
    modifier: Modifier,
    onProfileClick: () -> Unit,
    baseViewModel: BaseViewModel,
    context: Context,
    inputIcons: List<InputIconState>,
    currentUser: UsersEntity? = null,
    content: @Composable (() -> Unit)
) {
    val isDrawerOpen = drawerState.isOpen
    val width = if (isDrawerOpen) 200.dp else 60.dp

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
                ProfileImage(size = 44.dp, onProfileClick = onProfileClick)
                Spacer(Modifier.height(6.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(inputIcons) { item ->
                        Column(
                            horizontalAlignment = if (drawerState.isOpen) Alignment.Start else Alignment.CenterHorizontally,
                            modifier = Modifier
                                .bounceClick {
                                    inputIcons.forEach { it.selected = false }
                                    item.selected = true
                                    item.onClick.invoke()
                                }
                                .fillMaxWidth()) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = if (drawerState.isOpen) Arrangement.Start else Arrangement.Center
                            ) {
                                item.icon?.let { icon ->
                                    if (icon is ImageVector) {
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
            }


            Column(
                modifier = Modifier, horizontalAlignment = Alignment.Start
            ) {

                Row(modifier = Modifier
                    .clickable {
                        drawerState.currentValue =
                            if (drawerState.isOpen) TabDrawerValue.Closed else TabDrawerValue.Open
                    }
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.End) {
                    // Version info and update notification
                    Icon(
                        if (drawerState.isOpen) Icons.AutoMirrored.TwoTone.KeyboardArrowLeft
                        else Icons.AutoMirrored.TwoTone.KeyboardArrowRight,
                        contentDescription = if (drawerState.isOpen) "Close drawer" else "Open drawer",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Text(
                    text = "v${baseViewModel.remoteConfigManager.getCurrentAppVersionName()}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Normal,
                    lineHeight = 10.sp
                )

                // Show update notification if available
                if (baseViewModel.updateInfo.value != null) {
                    // Note: We can't call suspend function here, so we'll show based on updateInfo
                    val currentVersion = baseViewModel.remoteConfigManager.getCurrentAppVersion()
                    val latestVersion =
                        baseViewModel.updateInfo.value?.latestVersionCode ?: currentVersion
                    if (latestVersion > currentVersion) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(bottom = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.TwoTone.Update,
                                contentDescription = "Update Available",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "Update",
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(
                                    alpha = 0.8f
                                ),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }


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
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.width(20.dp))
                    Column {
                        Text(
                            text = baseViewModel.storeName.value ?: "Jewel Vault",
                            fontSize = 22.sp,
                            fontFamily = ZenFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .bounceClick {
                                    ioScope.launch {
                                        baseViewModel.refreshOnlineMetalRates(context = context)
                                    }
                                }
                                .padding(top = 5.dp))

                        // Current Screen Heading
                        if (baseViewModel.currentScreenHeading.isNotEmpty()) {
                            Text(
                                text = baseViewModel.currentScreenHeading,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .padding(start = 10.dp)
                                    .offset(y = (-7).dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(20.dp))
                    MetalRatesTicker(
                        Modifier
                            .height(30.dp)
                            .weight(1f),
                        backgroundColor = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(
                        modifier = Modifier
                    ) {

                        BluetoothToggleIcon()
                        Spacer(Modifier.width(5.dp))
                        Text(
                            buildAnnotatedString {
                                withStyle(
                                    style = SpanStyle(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = MaterialTheme.typography.labelSmall.fontSize,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    append(currentUser?.name?.uppercase())
                                }
                                append("\n")
                                withStyle(
                                    style = SpanStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                ) {
                                    append(currentUser?.role?.uppercase())
                                }
                            },
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.End
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Spacer(Modifier.width(20.dp))
                }
                Box(Modifier.fillMaxSize()) {
                    content()
                }
            }
        }
    }
}

class InputIconState(
    initialText: String = "", initialIcon: Any? = null, // Can be ImageVector or Int resource
    selected: Boolean = false, initialOnClick: () -> Unit = {}
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
fun ProfileImage(size: Dp = 60.dp, onProfileClick: () -> Unit) {
    val baseViewModel = LocalBaseViewModel.current
    val context = LocalContext.current


    LaunchedEffect(Unit) {
        baseViewModel.loadStoreImage()
    }


    Box(modifier = Modifier
        .clickable {
            onProfileClick()
        }
        .size(size), contentAlignment = Alignment.Center) {
        // Prefer remote URL if present; fall back to local cached file
        val imageData = when {
            !baseViewModel.storeImage.value.isNullOrBlank() -> {
                log("TabNavigationDrawer: Using storeImage: ${baseViewModel.storeImage.value}")
                baseViewModel.storeImage.value
            }

            baseViewModel.hasLocalLogo() -> {
                log("TabNavigationDrawer: Using local logo: ${baseViewModel.getLogoUri()}")
                baseViewModel.getLogoUri()
            }

            else -> {
                log("TabNavigationDrawer: No image data available")
                null
            }
        }

        if (imageData != null) {
            log("TabNavigationDrawer: ImageData type: ${imageData::class.simpleName}, value: $imageData")

            // Create ImageRequest with proper configuration and logging
            val imageRequest = ImageRequest.Builder(context).data(imageData).crossfade(true)
                .error(android.R.drawable.ic_menu_gallery)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .listener(onError = { request, result ->
                    log("TabNavigationDrawer: Coil load error for ${request.data}: ${result.throwable.message}")
                }, onSuccess = { request, _ ->
                    log("TabNavigationDrawer: Coil load success for ${request.data}")
                }).build()

            // Show profile image if available
            Image(
                painter = rememberAsyncImagePainter(imageRequest),
                contentDescription = "Profile Image",
                modifier = Modifier
                    .fillMaxSize()
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
                    .fillMaxSize()
                    .background(
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.1f),
                        shape = CircleShape
                    )
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f),
                        shape = CircleShape
                    ), contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.TwoTone.Person,
                    contentDescription = "Profile",
                    modifier = Modifier.fillMaxSize(),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
