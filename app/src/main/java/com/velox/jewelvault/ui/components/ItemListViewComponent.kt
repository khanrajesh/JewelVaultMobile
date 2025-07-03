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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velox.jewelvault.data.roomdb.entity.ItemEntity
import com.velox.jewelvault.utils.to2FString


@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
@Deprecated("Use TextListView instead.",
    replaceWith = ReplaceWith("TextListView",
        "com.velox.jewelvault.ui.components.TextListView"
        ))
fun ItemListViewComponent(
    itemHeaderList: List<String>,
    items: List<ItemEntity>,
    modifier: Modifier = Modifier,
    onItemLongClick: (ItemEntity) -> Unit
) {
    val scrollState = rememberScrollState()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columnWidth = 100.dp
        val itemsWithHeader = listOf<ItemEntity?>(null) + items

        Column(modifier = Modifier.horizontalScroll(scrollState)) {
            LazyColumn {
                itemsIndexed(itemsWithHeader) { index, item ->
                    val isHeader = index == 0
                    val values = if (isHeader) {
                        itemHeaderList
                    } else {
                        if (item != null) {
                            listOf(
                                "$index",
                                "${item.catName} (${item.catId})",
                                "${item.subCatName} (${item.subCatId})",
                                item.itemId.toString(),
                                item.itemAddName,
                                item.entryType,
                                item.quantity.toString(),
                                item.gsWt.to2FString(),
                                item.ntWt.to2FString(),
                                item.unit,
                                item.purity,
                                item.fnWt.to2FString(),
                                item.crgType,
                                item.crg.to2FString(),
                                item.othCrgDes,
                                item.othCrg.to2FString(),
                                (item.cgst + item.sgst + item.igst).to2FString(),
                                item.huid,
                                item.addDate.toString(),
                                item.addDesKey,
                                item.addDesValue,
                                "Extra value"
                            )
                        } else {
                            emptyList()
                        }
                    }

                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .combinedClickable(onClick = {}, onLongClick = {
                            if (!isHeader && item != null) {
                                onItemLongClick(item)
                            }
                        })) {
                        values.forEachIndexed { i, value ->
                            Box(
                                modifier = Modifier
                                    .width(columnWidth)
                                    .padding(horizontal = 2.dp)
                            ) {
                                Text(
                                    text = value,
                                    fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            if (i < values.size - 1) {
                                Text(
                                    "|",
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .width(2.dp)
                                        .padding(vertical = 8.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    if (isHeader) {
                        Spacer(
                            Modifier
                                .height(2.dp)
                                .fillMaxWidth()
                                .background(Color.LightGray)
                        )
                    }
                }
            }
        }
    }
}
