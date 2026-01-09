package com.velox.jewelvault.ui.components

import android.content.Context
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.Help
import androidx.compose.material.icons.automirrored.twotone.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.twotone.KeyboardArrowRight
import androidx.compose.material.icons.twotone.AccountBalance
import androidx.compose.material.icons.twotone.Dashboard
import androidx.compose.material.icons.twotone.Inventory
import androidx.compose.material.icons.twotone.People
import androidx.compose.material.icons.twotone.Person
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material.icons.twotone.Update
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.velox.jewelvault.BaseViewModel
import com.velox.jewelvault.data.MetalRatesTicker
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
    Row(Modifier
        .clickable { onClick() }
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        // Icon with better styling
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.1f),
                    shape = CircleShape
                ), contentAlignment = Alignment.Center
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
    onGuideClick: () -> Unit = {},
    notifierContent: @Composable () -> Unit = {},
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
            onGuideClick,
            baseViewModel,
            context,
            inputIconStates,
            notifierContent,
            content,

            )
    } else {

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .background(MaterialTheme.colorScheme.primary),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(5.dp))
                Box(contentAlignment = Alignment.TopStart) {
                    Text(
                        text = (baseViewModel.storeName.value ?: "Jewel Vault").substringBefore(" "),
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
                            .offset(y = (-8).dp)
//                            .padding(top = 5.dp)
                    )

                    // Current Screen Heading
                    if (baseViewModel.currentScreenHeading.isNotEmpty()) {
                        Text(
                            text = baseViewModel.currentScreenHeading,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier
                                .offset(y = (17).dp)
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
                notifierContent()
                Spacer(Modifier.width(5.dp))

            }
            Box(Modifier.weight(1f)) {
                content()
            }
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(MaterialTheme.colorScheme.primary),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                item{
                    Box(Modifier.size(38.dp)) {
                        ProfileImage { onProfileClick() }
                    }
                }
                items(inputIconStates) { item ->
                    item.icon?.let { icon ->
                        Box(Modifier
                            .clickable {
                                inputIconStates.forEach { it.selected = false }
                                item.selected = true
                                item.onClick.invoke()
                            }
                            .background(
                                color = if (item.selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(3.dp)
                            .size(32.dp)) {
                            if (icon is ImageVector) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = item.text,
                                    Modifier.fillMaxSize(),
                                    tint = if (item.selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Image(
                                    painter = painterResource(icon as Int),
                                    contentDescription = item.text,
                                    Modifier.fillMaxSize()
                                )
                            }
                        }
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
    onGuideClick: () -> Unit,
    baseViewModel: BaseViewModel,
    context: Context,
    inputIcons: List<InputIconState>,
    notifierContent: @Composable (() -> Unit),
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
                ProfileImage(onProfileClick=onProfileClick)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(inputIcons) { item ->
                        Column(
                            horizontalAlignment = if (drawerState.isOpen) Alignment.Start else Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(Modifier
                                .clickable {
                                    inputIcons.forEach { it.selected = false }
                                    item.selected = true
                                    item.onClick.invoke()
                                }
                                .fillMaxWidth()
                                .padding(vertical = 4.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = if (drawerState.isOpen) Arrangement.Start else Arrangement.Center) {
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
                    .fillMaxWidth()
                    .clickable { onGuideClick() }
                    .padding(vertical = 6.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (isDrawerOpen) Arrangement.Start else Arrangement.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.TwoTone.Help,
                        contentDescription = "Help & Guide",
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    if (drawerState.isOpen) {
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Guide",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

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
                    Spacer(Modifier.width(20.dp))
                    notifierContent()
                    Spacer(Modifier.width(10.dp))
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
