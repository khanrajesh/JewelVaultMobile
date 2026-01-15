package com.velox.jewelvault.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.velox.jewelvault.utils.isLandscape

@Composable
fun RowOrColumn(
    rowModifier: Modifier = Modifier,
    columnModifier: Modifier = Modifier,
    rotate: Boolean = false,
    horizontalArrangement: Arrangement.Horizontal? = null,
    verticalAlignment: Alignment.Vertical? = null,
    content: @Composable (isLandScape: Boolean) -> Unit
) {
    val isLandscapeMode = isLandscape()
    // Fallback to sensible defaults when caller does not provide arrangements/alignments
    val rowArrangement = horizontalArrangement ?: Arrangement.Start
    val rowAlignment: Alignment.Vertical = verticalAlignment ?: Alignment.CenterVertically

    if (isLandscapeMode && !rotate) {
        Row(
            rowModifier,
            horizontalArrangement = rowArrangement,
            verticalAlignment = rowAlignment
        ) { content(true) }
    } else {
        Column(columnModifier) { content(false) }
    }
}

@Composable
fun WidthThenHeightSpacer(gap: Dp = 5.dp, rotate: Boolean = false) {
    val isLandscapeMode = isLandscape()
    if (isLandscapeMode && !rotate) {
        Spacer(Modifier.width(gap))
    } else {
        Spacer(Modifier.height(gap))
    }
}
