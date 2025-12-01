package com.velox.jewelvault.ui.screen.inventory

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.twotone.MoreVert
import androidx.compose.material.icons.twotone.Refresh
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.velox.jewelvault.data.roomdb.dto.CatSubCatDto
import com.velox.jewelvault.data.roomdb.entity.category.SubCategoryEntity
import com.velox.jewelvault.ui.components.CusOutlinedTextField
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.ui.components.bounceClick
import com.velox.jewelvault.ui.nav.SubScreens
import com.velox.jewelvault.utils.LocalSubNavController
import com.velox.jewelvault.utils.to3FString


sealed class CatType(val type: String) {
    data object Category : CatType("Category")
    data object SubCategory : CatType("Sub Category")
}

@Composable
fun InventoryCategoryScreen(inventoryViewModel: InventoryViewModel) {

    val subNavController = LocalSubNavController.current
    inventoryViewModel.currentScreenHeadingState.value = "Inventory"

    BackHandler {
        subNavController.navigate(SubScreens.Dashboard.route){
            popUpTo(SubScreens.Dashboard.route) {
                inclusive = true
            }
        }
    }


    LaunchedEffect(true) {
        inventoryViewModel.getCategoryAndSubCategoryDetails()
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
    val selectedCatId = remember { mutableStateOf<String?>(null) }
    val showDeleteCatDialog = remember { mutableStateOf(false) }
    val categoryToDelete = remember { mutableStateOf<CatSubCatDto?>(null) }
    val adminPin = remember { InputFieldState("") }
    val showSubCatInfoDialog = remember { mutableStateOf(false) }
    val selectedSubCategory = remember { mutableStateOf<SubCategoryEntity?>(null) }
    val showEditSubCatDialog = remember { mutableStateOf(false) }
    val showDeleteSubCatDialog = remember { mutableStateOf(false) }
    val subCatName = remember { InputFieldState("") }
    val subCatAdminPin = remember { InputFieldState("") }

    val subNavController = LocalSubNavController.current

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 18.dp))
            .padding(top = 5.dp, start = 5.dp)
    ) {
        LazyVerticalGrid(modifier = Modifier.fillMaxSize(), columns = GridCells.Fixed(2)) {

            items(inventoryViewModel.catSubCatDto) {
                CategoryItem(
                    height = height, 
                    catSubCatDto = it, 
                    addSubCatClick = { showOption ->
                        addCatType.value = CatType.SubCategory.type
                        selectedCatName.value = it.catName
                        selectedCatId.value = it.catId
                        showOption.value = false
                        showAddCatDialog.value = true
                    },
                    deleteCategoryClick = { category ->
                        categoryToDelete.value = category
                        showDeleteCatDialog.value = true
                    },
                    subCategoryLongClick = { subCategory ->
                        selectedSubCategory.value = subCategory
                        showSubCatInfoDialog.value = true
                    }
                )
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
                    // Inventory Summary Section
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Row {
                            Text(
                                "Inventory Summary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(Modifier.weight(1f))

                            Icon(Icons.TwoTone.Refresh,"",
                                modifier = Modifier.bounceClick{
                                inventoryViewModel.updateCatAndSubQtyAndWt()
                            },
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Summary Statistics
                        Row {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${inventoryViewModel.inventorySummary.value.totalItems}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Total Items",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${inventoryViewModel.inventorySummary.value.totalCategories}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Categories",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${inventoryViewModel.inventorySummary.value.totalGrossWeight.to3FString()}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Gross Wt (gm)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${inventoryViewModel.inventorySummary.value.recentItemsAdded}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Recent (7d)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Add Category Button at bottom
                    Row {
                        Spacer(Modifier.weight(1f))
                        Text(
                            "Import Item",
                            Modifier
                                .clickable {
                                    subNavController.navigate(SubScreens.ImportItems.route)
                                }
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(16.dp),
                                )
                                .padding(10.dp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Scan & Add",
                            Modifier
                                .clickable {
                                    subNavController.navigate(SubScreens.ScanAddItem.route)
                                }
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(16.dp),
                                )
                                .padding(10.dp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Add Category", 
                            Modifier
                                .clickable {
                                    addCatType.value = CatType.Category.type
                                    showAddCatDialog.value = true
                                }
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(16.dp),
                                )
                                .padding(10.dp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
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
                                    text.clear()
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
                                    val name = text.text.trim().let {
                                        if (it.isNotEmpty()) it.substring(0, 1)
                                            .uppercase() + it.substring(1).lowercase() else it
                                    }
                                    if (addCatType.value == CatType.Category.type) {
                                        inventoryViewModel.addCategory(name)
                                    } else {
                                        if (selectedCatId.value != null && selectedCatName.value != null) {
                                            inventoryViewModel.addSubCategory(
                                                subCatName = name,
                                                catName = selectedCatName.value!!,
                                                catId = selectedCatId.value!!
                                            )
                                        }
                                    }

                                    selectedCatName.value = null
                                    selectedCatId.value = null
                                    text.clear()
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

        // Delete Category Confirmation Dialog
        if (showDeleteCatDialog.value && categoryToDelete.value != null) {
            Dialog(onDismissRequest = {
                showDeleteCatDialog.value = false
                categoryToDelete.value = null
                adminPin.clear()
            }) {
                Column(
                    Modifier
                        .width(500.dp)
                        .wrapContentHeight()
                        .background(
                            MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(18.dp)
                        )
                        .padding(20.dp)
                ) {
                    Text(
                        "Delete Category",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        "Are you sure you want to delete the category '${categoryToDelete.value?.catName}'?",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    Text(
                        "This will also delete all subcategories and items in this category. This action cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        "Enter Admin PIN to confirm:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    CusOutlinedTextField(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .fillMaxWidth(),
                        state = adminPin,
                        placeholderText = "Enter Admin PIN",
                        keyboardType = KeyboardType.NumberPassword,
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row {
                        Text(
                            "Cancel", 
                            Modifier
                                .clickable {
                                    adminPin.clear()
                                    categoryToDelete.value = null
                                    showDeleteCatDialog.value = false
                                }
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(16.dp),
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.weight(1f))

                        Text(
                            "Delete", 
                            Modifier
                                .clickable {
                                    val category = categoryToDelete.value
                                    if (category != null && adminPin.text.isNotEmpty()) {
                                        inventoryViewModel.deleteCategoryWithPin(
                                            category = category,
                                            adminPin = adminPin.text,
                                            onSuccess = {
                                                adminPin.clear()
                                                categoryToDelete.value = null
                                                showDeleteCatDialog.value = false
                                            },
                                            onFailure = { error ->
                                                // Error message will be shown via snackbar
                                            }
                                        )
                                    }
                                }
                                .background(
                                    MaterialTheme.colorScheme.error,
                                    RoundedCornerShape(16.dp),
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                }
            }
        }

        // SubCategory Info Dialog
        if (showSubCatInfoDialog.value && selectedSubCategory.value != null) {
            Dialog(onDismissRequest = {
                showSubCatInfoDialog.value = false
                selectedSubCategory.value = null
            }) {
                Column(
                    Modifier
                        .width(500.dp)
                        .wrapContentHeight()
                        .background(
                            MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(18.dp)
                        )
                        .padding(20.dp)
                ) {
                    Text(
                        "SubCategory Information",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val subCategory = selectedSubCategory.value!!
                    Text("Name: ${subCategory.subCatName}")
                    Text("Category: ${subCategory.catName}")
                    Text("Quantity: ${subCategory.quantity}")
                    Text("Gross Weight: ${subCategory.gsWt}gm")
                    Text("Fine Weight: ${subCategory.fnWt}gm")
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row {
                        Text(
                            "Edit", 
                            Modifier
                                .clickable {
                                    subCatName.text = subCategory.subCatName
                                    showSubCatInfoDialog.value = false
                                    showEditSubCatDialog.value = true
                                }
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(16.dp),
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )

                        Spacer(Modifier.weight(1f))

                        Text(
                            "Delete", 
                            Modifier
                                .clickable {
                                    showSubCatInfoDialog.value = false
                                    showDeleteSubCatDialog.value = true
                                }
                                .background(
                                    MaterialTheme.colorScheme.error,
                                    RoundedCornerShape(16.dp),
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                }
            }
        }

        // Edit SubCategory Dialog
        if (showEditSubCatDialog.value && selectedSubCategory.value != null) {
            Dialog(onDismissRequest = {
                showEditSubCatDialog.value = false
                selectedSubCategory.value = null
                subCatName.clear()
            }) {
                Column(
                    Modifier
                        .width(500.dp)
                        .wrapContentHeight()
                        .background(
                            MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(18.dp)
                        )
                        .padding(20.dp)
                ) {
                    Text(
                        "Edit SubCategory Name",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    CusOutlinedTextField(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .fillMaxWidth(),
                        state = subCatName,
                        placeholderText = "Enter new subcategory name",
                        keyboardType = KeyboardType.Text,
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row {
                        Text(
                            "Cancel", 
                            Modifier
                                .clickable {
                                    subCatName.clear()
                                    selectedSubCategory.value = null
                                    showEditSubCatDialog.value = false
                                }
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(16.dp),
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.weight(1f))

                        Text(
                            "Update", 
                            Modifier
                                .clickable {
                                    val subCategory = selectedSubCategory.value
                                    val newName = subCatName.text.trim()
                                    if (subCategory != null && newName.isNotEmpty()) {
                                        inventoryViewModel.updateSubCategoryName(
                                            subCategory = subCategory,
                                            newName = newName,
                                            onSuccess = {
                                                subCatName.clear()
                                                selectedSubCategory.value = null
                                                showEditSubCatDialog.value = false
                                            },
                                            onFailure = { error ->
                                                // Error message will be shown via snackbar
                                            }
                                        )
                                    }
                                }
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(16.dp),
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        // Delete SubCategory Dialog
        if (showDeleteSubCatDialog.value && selectedSubCategory.value != null) {
            Dialog(onDismissRequest = {
                showDeleteSubCatDialog.value = false
                selectedSubCategory.value = null
                subCatAdminPin.clear()
            }) {
                Column(
                    Modifier
                        .width(500.dp)
                        .wrapContentHeight()
                        .background(
                            MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(18.dp)
                        )
                        .padding(20.dp)
                ) {
                    Text(
                        "Delete SubCategory",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val subCategory = selectedSubCategory.value!!
                    Text(
                        "Are you sure you want to delete the subcategory '${subCategory.subCatName}'?",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    Text(
                        "This will also delete all items in this subcategory. This action cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        "Enter Admin PIN to confirm:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    CusOutlinedTextField(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .fillMaxWidth(),
                        state = subCatAdminPin,
                        placeholderText = "Enter Admin PIN",
                        keyboardType = KeyboardType.NumberPassword,
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row {
                        Text(
                            "Cancel", 
                            Modifier
                                .clickable {
                                    subCatAdminPin.clear()
                                    selectedSubCategory.value = null
                                    showDeleteSubCatDialog.value = false
                                }
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(16.dp),
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.weight(1f))

                        Text(
                            "Delete", 
                            Modifier
                                .clickable {
                                    val subCategory = selectedSubCategory.value
                                    if (subCategory != null && subCatAdminPin.text.isNotEmpty()) {
                                        inventoryViewModel.deleteSubCategoryWithPin(
                                            subCategory = subCategory,
                                            adminPin = subCatAdminPin.text,
                                            onSuccess = {
                                                subCatAdminPin.clear()
                                                selectedSubCategory.value = null
                                                showDeleteSubCatDialog.value = false
                                            },
                                            onFailure = { error ->
                                                // Error message will be shown via snackbar
                                            }
                                        )
                                    }
                                }
                                .background(
                                    MaterialTheme.colorScheme.error,
                                    RoundedCornerShape(16.dp),
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onError
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
    deleteCategoryClick: (CatSubCatDto) -> Unit,
    subCategoryLongClick: (SubCategoryEntity) -> Unit,
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

                Icon(Icons.TwoTone.MoreVert, null, modifier = Modifier.clickable {
                    showOption.value = !showOption.value
                })

            }



            Spacer(Modifier.height(10.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(3)
            ) {
                items(catSubCatDto.subCategoryList) {
                    SubCategoryItem(
                        subCategoryEntity = it,
                        onLongClick = { subCategoryLongClick(it) }
                    )
                }
            }
        }

        if (showOption.value)
            Box(
                Modifier
                    .offset(y = 40.dp)
                    .wrapContentHeight()
                    .wrapContentWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                    .padding(8.dp)
            ) {
                Column {
                    Text("Add Sub Category",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clickable {
                            addSubCatClick(showOption)
                        })
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Delete Category",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.clickable {
                            showOption.value = false
                            deleteCategoryClick(catSubCatDto)
                        })
                }
            }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SubCategoryItem(
    subCategoryEntity: SubCategoryEntity,
    onLongClick: () -> Unit
) {
    val subNav = LocalSubNavController.current
    Column(
        Modifier
            .combinedClickable(
                onClick = {
                    ///{catId}/{catName}/{subCatId}/{subCatName}
                    subNav.navigate("${SubScreens.InventoryItem.route}/${subCategoryEntity.catId}/${subCategoryEntity.catName}/${subCategoryEntity.subCatId}/${subCategoryEntity.subCatName}")
                },
                onLongClick = onLongClick
            )
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
        Text("Gs Wt: ${subCategoryEntity.gsWt.to3FString()}gm")
    }
}
