package com.velox.jewelvault.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.velox.jewelvault.utils.isLandscape

@Composable
fun RowOrColumn(
    rowModifier: Modifier = Modifier,
    columnModifier: Modifier = Modifier,
    rotate: Boolean = false,
    content: @Composable (isLandScape: Boolean) -> Unit
) {
    val isLandscape = isLandscape()
    if (isLandscape && !rotate) {
        Row(rowModifier) { content(true) }
    } else {
        Column(columnModifier) { content(false) }
    }
}

@Composable
fun WidthThenHeightSpacer(gap: Dp = 5.dp, rotate: Boolean = false) {
    if (isLandscape() && !rotate) {
        Spacer(Modifier.width(gap))
    } else {
        Spacer(Modifier.height(gap))
    }
}