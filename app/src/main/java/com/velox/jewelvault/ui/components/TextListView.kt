package com.velox.jewelvault.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun TextListView(
    headerList: List<String>,
    items: List<List<String>>,
    modifier: Modifier = Modifier,
    maxColumnWidth: Dp = 200.dp,
    onItemClick: (List<String>) -> Unit,
    onItemLongClick: (List<String>) -> Unit,
) {

    Column {
        val scrollState = rememberScrollState()
        val textMeasurer = rememberTextMeasurer()
        val density = LocalDensity.current

        // Calculate optimal column widths
        val columnWidths = remember(headerList, items, maxColumnWidth) {
            val minColumnWidth = 100.dp
            val numColumns = headerList.size
            val allRows = listOf(headerList) + items

            (0 until numColumns).map { columnIndex ->
                val maxTextWidth = allRows.mapIndexedNotNull { rowIndex, row ->
                    if (columnIndex < row.size) {
                        val text = row[columnIndex]
                        val isHeaderRow = rowIndex == 0
                        val textStyle = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = if (isHeaderRow) FontWeight.Bold else FontWeight.Normal
                        )
                        val textLayoutResult = textMeasurer.measure(text, textStyle)
                        with(density) { textLayoutResult.size.width.toDp() }
                    } else {
                        null
                    }
                }.maxOrNull() ?: 0.dp

                // Add padding (8.dp horizontal padding total)
                val calculatedWidth = maxTextWidth + 16.dp

                // Ensure width is between min and max
                val finalWidth = min(max(calculatedWidth, minColumnWidth), maxColumnWidth)
                println("Column $columnIndex: maxTextWidth=$maxTextWidth, calculatedWidth=$calculatedWidth, finalWidth=$finalWidth")
                finalWidth
            }
        }

        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val itemsWithHeader = listOf<List<String>?>(null) + items
            val calculatedTotalWidth =
                columnWidths.fold(0.dp) { acc, width -> acc + width } + (columnWidths.size - 1).dp
            val screenWidth = maxWidth

            // If calculated width is less than screen width, distribute the extra space
            val finalColumnWidths = if (calculatedTotalWidth < screenWidth) {
                val extraSpace = screenWidth - calculatedTotalWidth
                val extraPerColumn = extraSpace / columnWidths.size
                columnWidths.map { it + extraPerColumn }
            } else {
                columnWidths
            }

            val totalWidth =
                if (calculatedTotalWidth < screenWidth) screenWidth else calculatedTotalWidth

            Column(modifier = Modifier.horizontalScroll(scrollState)) {
                LazyColumn {
                    itemsIndexed(itemsWithHeader) { index, item ->
                        val isHeader = index == 0
                        val values = if (isHeader) {
                            headerList
                        } else {
                            item ?: emptyList()
                        }

                        Column {
                            Row(
                                modifier = Modifier
                                    .width(totalWidth)
                                    .height(48.dp) // Fixed height so vertical dividers work
                                    .combinedClickable(onClick = {
                                        if (!isHeader && item != null) {
                                            onItemClick(item)
                                        }
                                    }, onLongClick = {
                                        if (!isHeader && item != null) {
                                            onItemLongClick(item)
                                        }
                                    }), verticalAlignment = Alignment.CenterVertically
                            ) {
                                values.forEachIndexed { i, value ->
                                    Box(
                                        modifier = Modifier
                                            .width(if (i < finalColumnWidths.size) finalColumnWidths[i] else 100.dp)
                                            .fillMaxHeight()
                                            .padding(horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = value,
                                            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth(),
                                            maxLines = if (isHeader) 2 else Int.MAX_VALUE,
                                            softWrap = true,
                                            style = LocalTextStyle.current.copy(
                                                lineHeight = 14.sp
                                            )
                                        )
                                    }

                                    // Add vertical divider between columns
                                    if (i < values.size - 1) {
                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .fillMaxHeight()
                                                .background(Color.Gray)
                                        )
                                    }
                                }
                            }

                            // Add horizontal divider after each row
                            Spacer(modifier = Modifier.height(1.dp))
                            Box(
                                modifier = Modifier
                                    .height(if (isHeader) 3.dp else 1.dp)
                                    .width(totalWidth)
                                    .background(if (isHeader) Color.Black else Color.Gray)
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                        }
                    }
                }
            }
        }
    }
}