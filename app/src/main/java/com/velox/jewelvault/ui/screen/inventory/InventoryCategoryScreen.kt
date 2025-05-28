package com.velox.jewelvault.ui.screen.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.velox.jewelvault.data.roomdb.dto.CatSubCatDto
import com.velox.jewelvault.data.roomdb.entity.SubCategoryEntity
import com.velox.jewelvault.ui.components.CusOutlinedTextField
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.ui.components.bounceClick
import com.velox.jewelvault.ui.nav.SubScreens
import com.velox.jewelvault.utils.LocalSubNavController


sealed class CatType(val type: String) {
    data object Category : CatType("Category")
    data object SubCategory : CatType("Sub Category")
}

@Composable
fun InventoryCategoryScreen(inventoryViewModel: InventoryViewModel) {
    LaunchedEffect(true) {
        inventoryViewModel.init()
    }
    LandscapeInventoryScreen(inventoryViewModel)
}

@Composable
fun LandscapeInventoryScreen(inventoryViewModel: InventoryViewModel) {
    val text = remember { InputFieldState("") }
    val height = LocalConfiguration.current.screenHeightDp / 3
    val showAddCatDialog = remember { mutableStateOf(false) }
    val addCatType = remember { mutableStateOf("") }
    val selectedCatName = remember { mutableStateOf<String?>(null) }
    val selectedCatId = remember { mutableStateOf<Int?>(null) }

    val subNavController = LocalSubNavController.current

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 18.dp))
            .padding(top = 5.dp, start = 5.dp)
    ) {
        LazyVerticalGrid(modifier = Modifier.fillMaxSize(), columns = GridCells.Fixed(2)) {

            items(inventoryViewModel.catSubCatDto) {
                CategoryItem(height, it, addSubCatClick = { showOption ->

                    addCatType.value = CatType.SubCategory.type
                    selectedCatName.value = it.catName
                    selectedCatId.value = it.catId
                    showOption.value = false
                    showAddCatDialog.value = true

                })
            }

            item {
                Column(
                    Modifier
                        .clickable {
                            subNavController.navigate(SubScreens.InventoryFilter.route)
                        }
                        .padding(end = 5.dp, bottom = 5.dp)
                        .height(height.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(10.dp)
                ) {


                    Spacer(Modifier.weight(1f))
                    Row {
                        Spacer(Modifier.weight(1f))
                        Text(
                            "Add", Modifier
                                .clickable {
                                    addCatType.value = CatType.Category.type
                                    showAddCatDialog.value = true
                                }
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(16.dp),
                                )
                                .padding(10.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

        }

        if (showAddCatDialog.value) {
            Dialog(onDismissRequest = {
                showAddCatDialog.value = !showAddCatDialog.value
            }) {
                Column(
                    Modifier
                        .width(500.dp)
                        .wrapContentHeight()
                        .background(
                            MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(18.dp)
                        )
                        .padding(10.dp)
                ) {
                    Text("Add ${addCatType.value}", fontWeight = FontWeight.SemiBold)
                    CusOutlinedTextField(
                        modifier = Modifier
                            .padding(vertical = 5.dp)
                            .fillMaxWidth(),
                        state = text,
                        placeholderText = "",
                        keyboardType = KeyboardType.Text,
                    )
                    Row {
                        Text(
                            "Cancel", Modifier
                                .clickable {
                                    text.clear
                                    selectedCatName.value = null
                                    selectedCatId.value = null
                                    showAddCatDialog.value = !showAddCatDialog.value
                                }
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(16.dp),
                                )
                                .padding(10.dp),
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.weight(1f))

                        Text(
                            "Add", Modifier
                                .clickable {
//                                    inventoryViewModel.catSubCatDto.clear()
                                    if (addCatType.value == CatType.Category.type) {
                                        inventoryViewModel.addCategory(text.text)
                                    } else {
                                        if (selectedCatId.value != null && selectedCatName.value != null) {
                                            inventoryViewModel.addSubCategory(
                                                subCatName = text.text,
                                                catName = selectedCatName.value!!,
                                                catId = selectedCatId.value!!
                                            )
                                        }
                                    }

                                    selectedCatName.value = null
                                    selectedCatId.value = null
                                    text.clear
                                    showAddCatDialog.value = !showAddCatDialog.value
                                }
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(16.dp),
                                )
                                .padding(10.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryItem(
    height: Int,
    catSubCatDto: CatSubCatDto,
    addSubCatClick: (MutableState<Boolean>) -> Unit,
) {

    val showOption = remember { mutableStateOf(false) }
    Box(
        Modifier
            .clickable {
                showOption.value = false
            }
            .padding(end = 5.dp, bottom = 5.dp)
            .height(height.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(10.dp),
        contentAlignment = Alignment.TopEnd
    ) {

        Column(Modifier) {
            Row {
                Column {
                    Text(
                        catSubCatDto.catName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp
                    )

                    Text(
                        """
                Id: ${catSubCatDto.catId}, Gs wt: ${catSubCatDto.gsWt}gm
            """.trimIndent()
                    )

                }
                Spacer(Modifier.weight(1f))

                Icon(Icons.Default.MoreVert, null, modifier = Modifier.clickable {
                    showOption.value = !showOption.value
                })

            }



            Spacer(Modifier.height(10.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(3)
            ) {
                items(catSubCatDto.subCategoryList) {
                    SubCategoryItem(it)
                }
            }
        }

        if (showOption.value)
            Box(
                Modifier
                    .offset(y = 40.dp)
                    .wrapContentHeight()
                    .wrapContentWidth()
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(5.dp)
            ) {
                Text("Add Sub Category",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clickable {
                        addSubCatClick(showOption)
                    })
            }
    }
}

@Composable
fun SubCategoryItem(subCategoryEntity: SubCategoryEntity) {
    val subNav = LocalSubNavController.current
    Column(
        Modifier
            .bounceClick {
                ///{catId}/{catName}/{subCatId}/{subCatName}
                subNav.navigate("${SubScreens.InventoryItem.route}/${subCategoryEntity.catId}/${subCategoryEntity.catName}/${subCategoryEntity.subCatId}/${subCategoryEntity.subCatName}")
            }
            .padding(3.dp)
            .wrapContentHeight()
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(16.dp)
            )
            .padding(5.dp)
    ) {
        Text(
            subCategoryEntity.subCatName, fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        Text("Qty: ${subCategoryEntity.quantity}")
        Text("Gs Wt: ${subCategoryEntity.gsWt}gm")
    }
}