package com.velox.jewelvault.ui.components

import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velox.jewelvault.R
import com.velox.jewelvault.data.MetalRatesTicker
import com.velox.jewelvault.ui.theme.ZenFontFamily
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.VaultPreview
import com.velox.jewelvault.utils.ioScope
import kotlinx.coroutines.launch

@Composable
@VaultPreview
fun TabNavigationDrawerPreview() {
    val inputIconStates = List(5) { index ->
        InputIconState(
            initialText = "Item ${index + 1}",
            initialIcon = when (index) {
                0 -> R.drawable.logo_1 // Replace with your actual icon resource IDs
                1 -> R.drawable.logo_1
                2 -> R.drawable.logo_1
                3 -> R.drawable.logo_1
                else -> R.drawable.logo_1
            },
            initialOnClick = {
                // Define onClick behavior for each item
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
            ) {

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
                                Modifier.size(40.dp)
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
@OptIn(ExperimentalMaterial3Api::class)
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
    drawerContent: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val baseViewModel = LocalBaseViewModel.current
    val context = LocalContext.current

    val width = if (drawerState.isOpen) 200.dp else 60.dp
    Row {
        Row(modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxHeight()
                    .width(width)
                    .background(
                        color = MaterialTheme.colorScheme.primary
                    )
                    .padding(5.dp)
            ) {

                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(75.dp))
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
                        contentDescription = "",
                        modifier = Modifier.size(40.dp)
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
                            text = "Jewel Vault",
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
                        MetalRatesTicker(Modifier.height(50.dp).weight(1f))
                        Spacer(Modifier.width(20.dp))

                        Icon(Icons.Outlined.Notifications, null, modifier = Modifier.size(25.dp))
                        Spacer(Modifier.width(10.dp))
                        Icon(Icons.Default.MoreVert, null, modifier = Modifier.size(25.dp))
                        Spacer(Modifier.width(10.dp))
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
    initialIcon: Int? = null,
    initialOnClick: () -> Unit = {}
) {
    var text by mutableStateOf(initialText)
    var icon by mutableStateOf(initialIcon)
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




