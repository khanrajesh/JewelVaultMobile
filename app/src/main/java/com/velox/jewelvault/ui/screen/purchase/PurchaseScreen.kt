package com.velox.jewelvault.ui.screen.purchase

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Refresh
import androidx.compose.material.icons.twotone.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.velox.jewelvault.data.MetalRatesTicker
import com.velox.jewelvault.data.roomdb.dto.PurchaseItemInputDto
import com.velox.jewelvault.data.roomdb.dto.PurchaseMetalRateDto
import com.velox.jewelvault.data.roomdb.entity.category.SubCategoryEntity
import com.velox.jewelvault.ui.components.CusOutlinedTextField
import com.velox.jewelvault.ui.components.bounceClick
import com.velox.jewelvault.ui.components.baseBackground1
import com.velox.jewelvault.ui.components.baseBackground8
import com.velox.jewelvault.ui.components.baseBackground2
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.LocalNavController
import com.velox.jewelvault.utils.Purity
import com.velox.jewelvault.utils.VaultPreview
import com.velox.jewelvault.utils.ioScope
import com.velox.jewelvault.utils.isLandscape
import com.velox.jewelvault.utils.mainScope
import com.velox.jewelvault.utils.rememberCurrentDateTime
import com.velox.jewelvault.utils.to3FString


@Composable
@VaultPreview
fun PurchaseScreenPreview() {
    PurchaseScreen(hiltViewModel<PurchaseViewModel>())
}


@Composable
fun PurchaseScreen(viewModel: PurchaseViewModel) {

    viewModel.currentScreenHeadingState.value = "Purchase Details"

    LaunchedEffect(true) {
        viewModel.getCategoryAndSubCategoryDetails()
    }

    LandscapePurchaseScreen(viewModel)
}

@Composable
fun LandscapePurchaseScreen(viewModel: PurchaseViewModel) {
    val context = LocalContext.current
    val baseViewModel = LocalBaseViewModel.current
    val purchaseItemScrollState = remember { mutableStateOf("None") }
    val listState = rememberLazyListState()
    val previousIndex = remember { mutableIntStateOf(0) }
    val previousOffset = remember { mutableIntStateOf(0) }
    var upIgnoreCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }.collect { (index, offset) ->
            val isScrollingDown =
                index > previousIndex.intValue || (index == previousIndex.intValue && offset > previousOffset.intValue)

            val isScrollingUp =
                index < previousIndex.intValue || (index == previousIndex.intValue && offset < previousOffset.intValue)

            previousIndex.intValue = index
            previousOffset.intValue = offset

            when {
                isScrollingDown && purchaseItemScrollState.value != "Down" -> {
                    purchaseItemScrollState.value = "Down"
                    upIgnoreCount = 1
                    println("Scroll Down")
                }

                isScrollingUp && purchaseItemScrollState.value == "Down" -> {
                    if (upIgnoreCount > 0) {
                        upIgnoreCount--
                        println("Ignored Up (#${2 - upIgnoreCount})")
                    } else {
                        purchaseItemScrollState.value = "Up"
                        println("Scroll Up")
                    }
                }

                // Ignore idle or no movement
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .baseBackground8()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .baseBackground1()
                .padding(5.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .baseBackground1()
                    .padding(5.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    ioScope {
                        baseViewModel.refreshMetalRates(context = context)
                    }
                }) {
                    Icon(
                        imageVector = Icons.TwoTone.Refresh, contentDescription = "Refresh"
                    )
                }

                MetalRatesTicker(
                    Modifier
                        .height(40.dp)
                        .weight(1f)
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(vertical = 5.dp)
            ) {
                item { FirmDetails(viewModel, purchaseItemScrollState) }
                item { Spacer(Modifier.height(5.dp)) }
                item { PurchaseItemDetails(Modifier, viewModel) }
                item { Spacer(Modifier.height(5.dp)) }
                item { DetailSection(Modifier, viewModel) }
            }
        }
    }
}

@Composable
fun DetailSection(modifier: Modifier, viewModel: PurchaseViewModel) {

    val navController = LocalNavController.current
    // Use CalculationUtils for metal-specific calculations
    val goldItems = viewModel.purchaseItemList.filter { it.catName == "Gold" }
    val silverItems = viewModel.purchaseItemList.filter { it.catName == "Silver" }

    val purGoldFnWt = goldItems.sumOf { it.fnWt }
    val purGoldFnWtWastage = goldItems.sumOf { it.fnWt * (it.wastage / 100) }
    val purGoldExtraCharge = goldItems.sumOf { it.extraCharge }

    val purSilverFnWt = silverItems.sumOf { it.fnWt }
    val purSilverFnWtWastage = silverItems.sumOf { it.fnWt * (it.wastage / 100) }
    val purSilverExtraCharge = silverItems.sumOf { it.extraCharge }

    val exchangeGoldFnWt =
        viewModel.exchangeMetalRateDto.filter { it.catName == "Gold" }.sumOf { it.fnWt }
    val exchangeSilverFnWt =
        viewModel.exchangeMetalRateDto.filter { it.catName == "Silver" }.sumOf { it.fnWt }

    val goldFnWtRate = try {
        viewModel.purchaseItemList.filter { it.catName == "Gold" }[0].fnRatePerGm
    } catch (_: Exception) {
        1.0
    }
    val silverFnWtRate = try {
        viewModel.purchaseItemList.filter { it.catName == "Silver" }[0].fnRatePerGm
    } catch (_: Exception) {
        1.0
    }

    val goldGsWt = goldItems.sumOf { it.gsWt }
    val silverGsWt = silverItems.sumOf { it.gsWt }

    val goldText = """
           GOLD: Gs:${goldGsWt}g | Fn:${purGoldFnWt}g | Exc:${exchangeGoldFnWt.to3FString()}g | Wastage:${purGoldFnWtWastage.to3FString()}g | Extra:${purGoldExtraCharge.to3FString()}
           Settling: ${((purGoldFnWt + purGoldFnWtWastage) - exchangeGoldFnWt).to3FString()}g | Amount: ƒ,1${(((purGoldFnWt + purGoldFnWtWastage) - exchangeGoldFnWt) * goldFnWtRate).to3FString()}
        """
    val silverText ="""
                    SILVER: Gs:${silverGsWt}g | Fn:${purSilverFnWt}g | Exc:${exchangeSilverFnWt.to3FString()}g | Wastage:${purSilverFnWtWastage.to3FString()}g | Extra:${purSilverExtraCharge.to3FString()}
                    Settling: ${((purSilverFnWt + purSilverFnWtWastage) - exchangeSilverFnWt).to3FString()}g | Amount: ƒ,1${(((purSilverFnWt + purSilverFnWtWastage) - exchangeSilverFnWt) * silverFnWtRate).to3FString()}
        """
    val addToReg ={
        if (viewModel.purchaseItemList.isNotEmpty() && viewModel.addBillNo.text.isNotBlank() && viewModel.addBillDate.text.isNotBlank() && viewModel.firmName.text.isNotBlank() && viewModel.sellerName.text.isNotBlank() && viewModel.sellerMobile.text.isNotBlank() && viewModel.firmMobile.text.isNotBlank() && viewModel.purchaseItemList.none { it.fnRatePerGm == 0.0 || it.fnWt == 0.0 }) {
            viewModel.addToReg(onComplete = {
                viewModel.clearPurchaseInputs()
                viewModel.purchaseItemList.clear()
                viewModel.exchangeMetalRateDto.clear()
                viewModel.firmName.clear()
                viewModel.firmMobile.clear()
                viewModel.firmAddress.clear()
                viewModel.firmGstin.clear()
                viewModel.sellerName.clear()
                viewModel.sellerMobile.clear()
                viewModel.addBillNo.clear()
                viewModel.addBillDate.clear()
                viewModel.addExchangeCategory.clear()
                viewModel.addExchangeFnWeight.clear()
                viewModel.catSubCatDto.clear()
                viewModel.snackBarState.value = "Purchase Bill added"
                mainScope {
                    navController.popBackStack()
                }
            })
        } else {
            viewModel.snackBarState.value =
                "Please fill all firm details fields and make sure all fine rates are set"
        }
    }

    if (isLandscape()){
        Row(
            modifier
                .fillMaxWidth()
                .height(60.dp)
                .baseBackground2()
                .padding(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                goldText.trimIndent(), modifier = Modifier.weight(0.5f), style = TextStyle(
                    fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp
                )
            )
            Spacer(Modifier.weight(0.1f))
            Text(
                silverText.trimIndent(), modifier = Modifier.weight(0.5f), style = TextStyle(
                    fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp
                )
            )
            Spacer(Modifier.weight(0.1f))
            Text(
                "SAVE",
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(0.2f)
                    .bounceClick {
                        addToReg()
                    }
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(16.dp),
                    )
                    .padding(10.dp),
                fontWeight = FontWeight.Bold)
        }

    }else{
        Column(
            modifier
                .fillMaxWidth()
                .baseBackground2()
                .padding(5.dp),
        ) {

            Text(
                goldText.trimIndent(), modifier = Modifier, style = TextStyle(
                    fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp
                )
            )
            Spacer(Modifier.height(5.dp))
            Text(
                silverText.trimIndent(), modifier = Modifier, style = TextStyle(
                    fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp
                )
            )
            Spacer(Modifier.height(5.dp))

            Text(
                "SAVE",
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .bounceClick {
                        addToReg()
                    }
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(16.dp),
                    )
                    .padding(10.dp),
                fontWeight = FontWeight.Bold)
        }
    }

}


@Composable
fun FirmDetails(viewModel: PurchaseViewModel, purchaseItemScrollState: MutableState<String>) {
    val context = LocalContext.current
    val landscape = isLandscape()
    val isPortrait = !landscape

    Column(
        Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .baseBackground2()
            .padding(5.dp)
    ) {
        Text("Firm Details")
        Column(
            Modifier
                .wrapContentHeight()
                .fillMaxWidth()
        ) {
            if (landscape) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CusOutlinedTextField(
                        viewModel.firmName, placeholderText = "Firm Name", rowWeight = 2f
                    )
                    CusOutlinedTextField(
                        viewModel.firmMobile,
                        placeholderText = "Firm Mobile No",
                        rowWeight = 1f,
                        trailingIcon = Icons.TwoTone.Search,
                        onTrailingIconClick = {
                            viewModel.getFirmByFirmMobile()
                        },
                        maxLines = 1,
                        keyboardType = KeyboardType.Phone,
                        validation = { input -> if (input.length != 10) "Please Enter Valid Number" else null },
                        imeAction = ImeAction.Done,
                        keyboardActions = KeyboardActions {
                            viewModel.getFirmByFirmMobile()
                        })
                }
            } else {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    CusOutlinedTextField(
                        viewModel.firmName, placeholderText = "Firm Name", columnWeight = 1f
                    )
                    CusOutlinedTextField(
                        viewModel.firmMobile,
                        placeholderText = "Firm Mobile No",
                        columnWeight = 1f,
                        trailingIcon = Icons.TwoTone.Search,
                        onTrailingIconClick = {
                            viewModel.getFirmByFirmMobile()
                        },
                        maxLines = 1,
                        keyboardType = KeyboardType.Phone,
                        validation = { input -> if (input.length != 10) "Please Enter Valid Number" else null },
                        imeAction = ImeAction.Done,
                        keyboardActions = KeyboardActions {
                            viewModel.getFirmByFirmMobile()
                        })
                }
            }

            val scrolling = remember { mutableStateOf(true) }
            val check =
                (viewModel.firmMobile.text.isNotBlank() && viewModel.sellerMobile.text.isNotBlank() && viewModel.addBillNo.text.isNotBlank())

            if (isPortrait) {
                // In portrait always show extra section; no collapse/animation
                scrolling.value = true
                FirmDetailsExtra(viewModel, context)
            } else {
                LaunchedEffect(
                    purchaseItemScrollState.value
                ) {
                    scrolling.value = !(purchaseItemScrollState.value == "Down" && check)
                }

                AnimatedVisibility(
                    visible = scrolling.value,
                    enter = slideInVertically(initialOffsetY = { -it }),
                    exit = slideOutVertically(targetOffsetY = { -it })
                ) {
                    FirmDetailsExtra(viewModel, context)
                }
                if (!scrolling.value) {
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Spacer(Modifier.weight(1f))
                        Text("Show more", modifier = Modifier.bounceClick {
                            scrolling.value = true
                        })
                    }
                }
            }

        }
    }
}

@Composable
private fun FirmDetailsExtra(
    viewModel: PurchaseViewModel, context: Context
) {
    val landscape = isLandscape()

    Column {
        if (landscape) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CusOutlinedTextField(
                    viewModel.sellerName, placeholderText = "Seller Name", rowWeight = 2f
                )
                CusOutlinedTextField(
                    viewModel.sellerMobile,
                    placeholderText = "Seller Mobile No",
                    rowWeight = 1f,
                    trailingIcon = Icons.TwoTone.Search,
                    onTrailingIconClick = {
                        viewModel.getFirmBySellerMobile()
                    },
                    maxLines = 1,
                    keyboardType = KeyboardType.Phone,
                    validation = { input -> if (input.length != 10) "Please Enter Valid Number" else null },
                    imeAction = ImeAction.Done,
                    keyboardActions = KeyboardActions {
                        viewModel.getFirmBySellerMobile()
                    })
            }
        } else {
            Column(
                Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                CusOutlinedTextField(
                    viewModel.sellerName, placeholderText = "Seller Name", columnWeight = 1f
                )
                CusOutlinedTextField(
                    viewModel.sellerMobile,
                    placeholderText = "Seller Mobile No",
                    columnWeight = 1f,
                    trailingIcon = Icons.TwoTone.Search,
                    onTrailingIconClick = {
                        viewModel.getFirmBySellerMobile()
                    },
                    maxLines = 1,
                    keyboardType = KeyboardType.Phone,
                    validation = { input -> if (input.length != 10) "Please Enter Valid Number" else null },
                    imeAction = ImeAction.Done,
                    keyboardActions = KeyboardActions {
                        viewModel.getFirmBySellerMobile()
                    })
            }
        }
        Spacer(Modifier.height(5.dp))

        if (landscape) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CusOutlinedTextField(
                    viewModel.firmAddress, placeholderText = "Address", rowWeight = 1f, maxLines = 3
                )
                CusOutlinedTextField(
                    viewModel.firmGstin,
                    placeholderText = "GSTIN/PAN ID",
                    rowWeight = 1f,
                    maxLines = 1
                )
                CusOutlinedTextField(
                    viewModel.addBillNo, placeholderText = "Bill No", rowWeight = 1f
                )
                CusOutlinedTextField(
                    viewModel.addBillDate,
                    placeholderText = "Date of Bill",
                    rowWeight = 1f,
                    isDatePicker = true,
                    onDateSelected = {
                        viewModel.snackBarState.value = "$it"
                    })
            }
        } else {
            Column(
                Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                CusOutlinedTextField(
                    viewModel.firmAddress,
                    placeholderText = "Address",
                    columnWeight = 1f,
                    maxLines = 3
                )
                CusOutlinedTextField(
                    viewModel.firmGstin,
                    placeholderText = "GSTIN/PAN ID",
                    columnWeight = 1f,
                    maxLines = 1
                )
                CusOutlinedTextField(
                    viewModel.addBillNo, placeholderText = "Bill No", columnWeight = 1f
                )
                CusOutlinedTextField(
                    viewModel.addBillDate,
                    placeholderText = "Date of Bill",
                    columnWeight = 1f,
                    isDatePicker = true,
                    onDateSelected = {
                        viewModel.snackBarState.value = "$it"
                    })
            }
        }
        Spacer(Modifier.height(5.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CusOutlinedTextField(
                viewModel.addCGst,
                placeholderText = "C.Gst",
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number
            )
            Spacer(Modifier.width(5.dp))
            CusOutlinedTextField(
                viewModel.addIGst,
                placeholderText = "I.Gst",
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number
            )
            Spacer(Modifier.width(5.dp))
            CusOutlinedTextField(
                viewModel.addSGst,
                placeholderText = "S.Gst",
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number
            )
        }
    }
}

@Composable
fun PurchaseItemDetails(
    modifier: Modifier, viewModel: PurchaseViewModel
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .baseBackground2()
            .padding(5.dp)
    ) {
        PurchaseItemListComponent(viewModel.purchaseItemList)
        MetalRateListComponent(viewModel.exchangeMetalRateDto)
        AddItemComponent(viewModel)
        Spacer(Modifier.height(32.dp))
    }
}


@Composable
fun MetalRateListComponent(rates: List<PurchaseMetalRateDto>) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text("Metal Rates", style = MaterialTheme.typography.titleMedium)
        if (rates.isEmpty()) {
            Text(
                "Metal exchange info will be shown here!",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        } else {
            rates.forEach {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Text(
                        "${it.catName} (${it.catId}) -> ${it.fnWt} gm",
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PurchaseItemListComponent(items: List<PurchaseItemInputDto>) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text("Added Items", style = MaterialTheme.typography.titleMedium)

        if (items.isEmpty()) {
            Text(
                "Added items details will be shown here!",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        } else {
            items.forEachIndexed { index, item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(Modifier.padding(8.dp)) {
                        Text(
                            "Item ${index + 1}: ${item.name},  Cat/Sub Cat: ${item.catId}/${item.subCatId}",
                            fontWeight = FontWeight.Bold
                        )
                        Text("GS Wt: ${item.gsWt}, Purity: ${item.purity}, FN Wt: ${item.fnWt}")
                        if (item.extraChargeDes.isNotEmpty()) {
                            Text("Extra: ${item.extraChargeDes} - â‚¹${item.extraCharge}")
                        }
                    }
                }
            }
        }
    }
}


@SuppressLint("DefaultLocale")
@Composable
private fun AddItemComponent(viewModel: PurchaseViewModel) {
    val subCategories = remember { mutableStateListOf<SubCategoryEntity>() }
    val landscape = isLandscape()

    Column {
        Text("Add Item")
        // Category and subcategory
        if (landscape) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CusOutlinedTextField(
                    state = viewModel.addItemCat,
                    placeholderText = "Category",
                    rowWeight = 1f,
                    dropdownItems = viewModel.catSubCatDto.map { it.catName },
                    onDropdownItemSelected = { selected ->
                        viewModel.addItemCat.text = selected

                        val met =
                            viewModel.metalRates.filter { it.metal.lowercase() == selected.lowercase() }
                        if (met.isNotEmpty()) {
                            val price = met[0].price.toLongOrNull()
                            if (price != null) {
                                viewModel.addItemFineRatePerGm.text = "${price / 10}"
                            }
                        }

                        val selectedCat = viewModel.catSubCatDto.find { it.catName == selected }
                        subCategories.clear()
                        selectedCat?.subCategoryList?.let { subCategories.addAll(it) }
                        viewModel.addItemSubCat.text = ""
                    })
                CusOutlinedTextField(
                    state = viewModel.addItemSubCat,
                    placeholderText = "Sub Category",
                    rowWeight = 1f,
                    dropdownItems = subCategories.map { it.subCatName },
                    onDropdownItemSelected = { selected ->
                        viewModel.addItemSubCat.text = selected
                    })
            }
        } else {
            Column(
                Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                CusOutlinedTextField(
                    state = viewModel.addItemCat,
                    placeholderText = "Category",
                    columnWeight = 1f,
                    dropdownItems = viewModel.catSubCatDto.map { it.catName },
                    onDropdownItemSelected = { selected ->
                        viewModel.addItemCat.text = selected

                        val met =
                            viewModel.metalRates.filter { it.metal.lowercase() == selected.lowercase() }
                        if (met.isNotEmpty()) {
                            val price = met[0].price.toLongOrNull()
                            if (price != null) {
                                viewModel.addItemFineRatePerGm.text = "${price / 10}"
                            }
                        }

                        val selectedCat = viewModel.catSubCatDto.find { it.catName == selected }
                        subCategories.clear()
                        selectedCat?.subCategoryList?.let { subCategories.addAll(it) }
                        viewModel.addItemSubCat.text = ""
                    })
                CusOutlinedTextField(
                    state = viewModel.addItemSubCat,
                    placeholderText = "Sub Category",
                    columnWeight = 1f,
                    dropdownItems = subCategories.map { it.subCatName },
                    onDropdownItemSelected = { selected ->
                        viewModel.addItemSubCat.text = selected
                    })
            }
        }

        Spacer(Modifier.height(5.dp))

        // Weights and purity
        if (landscape) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CusOutlinedTextField(
                    viewModel.addItemGsWt,
                    placeholderText = "Gross Weight",
                    rowWeight = 1f,
                    keyboardType = KeyboardType.Number
                )
                CusOutlinedTextField(
                    viewModel.addItemNtWt,
                    placeholderText = "Nt Weight",
                    rowWeight = 1f,
                    keyboardType = KeyboardType.Number
                )
                CusOutlinedTextField(
                    viewModel.addItemPurity,
                    placeholderText = "Purity",
                    rowWeight = 1f,
                    dropdownItems = Purity.list(),
                    onDropdownItemSelected = { selected ->
                        if (viewModel.addItemGsWt.text.isNotBlank()) {
                            val ntWtValue = viewModel.addItemGsWt.text.toDoubleOrNull() ?: 0.0
                            val multiplier = Purity.fromLabel(selected)?.multiplier ?: 1.0
                            viewModel.addItemFnWt.text = (ntWtValue * multiplier).to3FString()
                        }
                        viewModel.addItemPurity.text = selected
                    })
                CusOutlinedTextField(
                    viewModel.addItemFnWt,
                    placeholderText = "Fine Weight",
                    rowWeight = 1f,
                    keyboardType = KeyboardType.Number
                )
                CusOutlinedTextField(
                    viewModel.addItemWastage,
                    placeholderText = "Wastage",
                    rowWeight = 1f,
                    keyboardType = KeyboardType.Number
                )
            }
        } else {
            Column(
                Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                CusOutlinedTextField(
                    viewModel.addItemGsWt,
                    placeholderText = "Gross Weight",
                    columnWeight = 1f,
                    keyboardType = KeyboardType.Number
                )
                CusOutlinedTextField(
                    viewModel.addItemNtWt,
                    placeholderText = "Nt Weight",
                    columnWeight = 1f,
                    keyboardType = KeyboardType.Number
                )
                CusOutlinedTextField(
                    viewModel.addItemPurity,
                    placeholderText = "Purity",
                    columnWeight = 1f,
                    dropdownItems = Purity.list(),
                    onDropdownItemSelected = { selected ->
                        if (viewModel.addItemGsWt.text.isNotBlank()) {
                            val ntWtValue = viewModel.addItemGsWt.text.toDoubleOrNull() ?: 0.0
                            val multiplier = Purity.fromLabel(selected)?.multiplier ?: 1.0
                            viewModel.addItemFnWt.text = (ntWtValue * multiplier).to3FString()
                        }
                        viewModel.addItemPurity.text = selected
                    })
                CusOutlinedTextField(
                    viewModel.addItemFnWt,
                    placeholderText = "Fine Weight",
                    columnWeight = 1f,
                    keyboardType = KeyboardType.Number
                )
                CusOutlinedTextField(
                    viewModel.addItemWastage,
                    placeholderText = "Wastage",
                    columnWeight = 1f,
                    keyboardType = KeyboardType.Number
                )
            }
        }

        Spacer(Modifier.height(5.dp))

        // Charges and actions
        if (landscape) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CusOutlinedTextField(
                    viewModel.addItemFineRatePerGm,
                    placeholderText = "Fn Rate Per Gm",
                    rowWeight = 1f,
                    keyboardType = KeyboardType.Number
                )
                CusOutlinedTextField(
                    viewModel.addItemExtraChargeDes,
                    placeholderText = "E.Charge Des",
                    rowWeight = 1.5f
                )
                CusOutlinedTextField(
                    viewModel.addItemExtraCharge,
                    placeholderText = "Extra Charge",
                    rowWeight = 1f,
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                )
                Spacer(Modifier.weight(0.5f))
                Text("Clear", Modifier
                    .bounceClick {
                        viewModel.clearPurchaseInputs()
                    }
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(16.dp),
                    )
                    .padding(10.dp), fontWeight = FontWeight.Bold)
                Text("Add", Modifier
                    .bounceClick {
                        if (viewModel.addItemFnWt.text.isNotBlank() && viewModel.addItemNtWt.text.isNotBlank() && viewModel.addItemGsWt.text.isNotBlank() && viewModel.addItemPurity.text.isNotBlank() && viewModel.addItemSubCat.text.isNotBlank() && viewModel.addItemCat.text.isNotBlank() && viewModel.addItemWastage.text.isNotBlank() && viewModel.addItemFineRatePerGm.text.isNotBlank()

                        ) {

                            viewModel.addPurchaseItem()
                        } else {
                            viewModel.snackBarState.value = "Please fill all items fields"
                        }
                    }
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(16.dp),
                    )
                    .padding(10.dp), fontWeight = FontWeight.Bold)
            }
        } else {
            Column(
                Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                CusOutlinedTextField(
                    viewModel.addItemFineRatePerGm,
                    placeholderText = "Fn Rate Per Gm",
                    columnWeight = 1f,
                    keyboardType = KeyboardType.Number
                )
                CusOutlinedTextField(
                    viewModel.addItemExtraChargeDes,
                    placeholderText = "E.Charge Des",
                    columnWeight = 1f
                )
                CusOutlinedTextField(
                    viewModel.addItemExtraCharge,
                    placeholderText = "Extra Charge",
                    columnWeight = 1f,
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Clear", Modifier
                        .bounceClick {
                            viewModel.clearPurchaseInputs()
                        }
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(16.dp),
                        )
                        .padding(10.dp), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text("Add", Modifier
                        .bounceClick {
                            if (viewModel.addItemFnWt.text.isNotBlank() && viewModel.addItemNtWt.text.isNotBlank() && viewModel.addItemGsWt.text.isNotBlank() && viewModel.addItemPurity.text.isNotBlank() && viewModel.addItemSubCat.text.isNotBlank() && viewModel.addItemCat.text.isNotBlank() && viewModel.addItemWastage.text.isNotBlank() && viewModel.addItemFineRatePerGm.text.isNotBlank()

                            ) {

                                viewModel.addPurchaseItem()
                            } else {
                                viewModel.snackBarState.value = "Please fill all items fields"
                            }
                        }
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(16.dp),
                        )
                        .padding(10.dp), fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("Metal Trade (Give)")

        if (landscape) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CusOutlinedTextField(
                    modifier = Modifier,
                    rowWeight = 1f,
                    state = viewModel.addExchangeCategory,
                    placeholderText = "Category",
                    dropdownItems = viewModel.catSubCatDto.map { it.catName },
                    onDropdownItemSelected = { selected ->
                        viewModel.addExchangeCategory.text = selected
                    })
                CusOutlinedTextField(
                    viewModel.addExchangeFnWeight,
                    placeholderText = "Fine Weight (gm)",
                    rowWeight = 1f,
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Clear", Modifier
                    .bounceClick {
                        viewModel.addExchangeCategory.clear()
                        viewModel.addExchangeFnWeight.clear()
                    }
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(16.dp),
                    )
                    .padding(10.dp), fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text("Add", Modifier
                    .bounceClick {
                        if (viewModel.addExchangeCategory.text.isNotBlank() && viewModel.addExchangeFnWeight.text.isNotBlank()) {
                            viewModel.setMetalRate()
                        } else {
                            viewModel.snackBarState.value = "Please fill all exchange metal fields"
                        }
                    }
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(16.dp),
                    )
                    .padding(10.dp), fontWeight = FontWeight.Bold)
            }
        } else {
            Column(
                Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                CusOutlinedTextField(
                    modifier = Modifier,
                    columnWeight = 1f,
                    state = viewModel.addExchangeCategory,
                    placeholderText = "Category",
                    dropdownItems = viewModel.catSubCatDto.map { it.catName },
                    onDropdownItemSelected = { selected ->
                        viewModel.addExchangeCategory.text = selected
                    })
                CusOutlinedTextField(
                    viewModel.addExchangeFnWeight,
                    placeholderText = "Fine Weight (gm)",
                    columnWeight = 1f,
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Clear", Modifier
                        .bounceClick {
                            viewModel.addExchangeCategory.clear()
                            viewModel.addExchangeFnWeight.clear()
                        }
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(16.dp),
                        )
                        .padding(10.dp), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text("Add", Modifier
                        .bounceClick {
                            if (viewModel.addExchangeCategory.text.isNotBlank() && viewModel.addExchangeFnWeight.text.isNotBlank()) {
                                viewModel.setMetalRate()
                            } else {
                                viewModel.snackBarState.value =
                                    "Please fill all exchange metal fields"
                            }
                        }
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(16.dp),
                        )
                        .padding(10.dp), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}









