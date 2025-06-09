package com.velox.jewelvault.ui.screen.purchase

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.velox.jewelvault.data.MetalRatesTicker
import com.velox.jewelvault.data.roomdb.dto.PurchaseItemInputDto
import com.velox.jewelvault.data.roomdb.dto.PurchaseMetalRateDto
import com.velox.jewelvault.data.roomdb.entity.SubCategoryEntity
import com.velox.jewelvault.ui.components.CusOutlinedTextField
import com.velox.jewelvault.ui.components.bounceClick
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.Purity
import com.velox.jewelvault.utils.VaultPreview
import com.velox.jewelvault.utils.ioScope
import com.velox.jewelvault.utils.rememberCurrentDateTime


@Composable
@VaultPreview
fun PurchaseScreenPreview() {
    PurchaseScreen(hiltViewModel<PurchaseViewModel>())
}


@Composable
fun PurchaseScreen(viewModel: PurchaseViewModel) {

    LaunchedEffect(true) {
        viewModel.getCategoryAndSubCategoryDetails()
    }

    LandscapePurchaseScreen(viewModel)
}

@Composable
fun LandscapePurchaseScreen(viewModel: PurchaseViewModel) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val baseViewModel = LocalBaseViewModel.current
    val currentDateTime = rememberCurrentDateTime()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)
                )
                .padding(5.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .padding(5.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    ioScope {
                        baseViewModel.refreshMetalRates(context = context)
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Refresh, contentDescription = "Refresh"
                    )
                }

                MetalRatesTicker(
                    Modifier
                        .height(50.dp)
                        .weight(1f)
                )
                Text(text = currentDateTime.value)
                Spacer(Modifier.width(10.dp))
                IconButton(onClick = {

                }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert, contentDescription = "Refresh"
                    )
                }
            }

            FirmDetails(viewModel)
            Spacer(Modifier.height(5.dp))
            PurchaseItemDetails(Modifier.weight(1f), viewModel)
            Spacer(Modifier.height(5.dp))
            DetailSection(Modifier, viewModel)

        }
    }
}

@Composable
fun DetailSection(modifier:Modifier, viewModel: PurchaseViewModel) {
    Row(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            """
            Purchase Fn.Wt: 123.00g,  Exchange Fn.Wt: 123.00g,  Settling Fn.Wt: 123.00g, Amt to: $123456789
        """.trimIndent(), modifier = Modifier.weight(1f)
        )

            Text("ADD TO REG", Modifier
                .bounceClick {

                }
                .background(
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(16.dp),
                )
                .padding(10.dp), fontWeight = FontWeight.Bold)

        }

}

@Composable
fun FirmDetails(viewModel: PurchaseViewModel) {
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(5.dp)
    ) {
        Text("Firm Details")
        Column {
            Row(Modifier) {
                CusOutlinedTextField(
                    viewModel.firmName,
                    placeholderText = "Firm Name",
                    modifier = Modifier.weight(2f)
                )
                Spacer(Modifier.width(5.dp))
                CusOutlinedTextField(viewModel.firmMobile,
                    placeholderText = "Firm Mobile No",
                    modifier = Modifier.weight(1f),
                    trailingIcon = Icons.Default.Search,
                    onTrailingIconClick = {
                        viewModel.getFirmByFirmMobile()
                    },
                    maxLines = 1,
                    keyboardType = KeyboardType.Phone,
                    validation = { input ->
                        when {
                            input.length != 10 -> "Please Enter Valid Number"
                            else -> null
                        }
                    })
            }

            Row(Modifier) {
                CusOutlinedTextField(
                    viewModel.sellerName,
                    placeholderText = "Seller Name",
                    modifier = Modifier.weight(2f)
                )
                Spacer(Modifier.width(5.dp))
                CusOutlinedTextField(viewModel.sellerMobile,
                    placeholderText = "Seller Mobile No",
                    modifier = Modifier.weight(1f),
                    trailingIcon = Icons.Default.Search,
                    onTrailingIconClick = {
                        viewModel.getFirmBySellerMobile()
                    },
                    maxLines = 1,
                    keyboardType = KeyboardType.Phone,
                    validation = { input ->
                        when {
                            input.length != 10 -> "Please Enter Valid Number"
                            else -> null
                        }
                    })
            }
            Spacer(Modifier.height(5.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                CusOutlinedTextField(
                    viewModel.firmAddress,
                    placeholderText = "Address",
                    modifier = Modifier.weight(1f),
                    maxLines = 3
                )
                Spacer(Modifier.width(5.dp))
                CusOutlinedTextField(
                    viewModel.firmGstin,
                    placeholderText = "GSTIN/PAN ID",
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                Spacer(Modifier.width(5.dp))
                CusOutlinedTextField(
                    viewModel.addBillNo, placeholderText = "Bill No", modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(5.dp))
                CusOutlinedTextField(
                    viewModel.addBillDate, placeholderText = "Date of Bill", modifier = Modifier.weight(1f),
                    isDatePicker = true,
                    onDateSelected = {
                        Toast.makeText(context, "$it", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
fun PurchaseItemDetails(modifier: Modifier, viewModel: PurchaseViewModel) {
    LazyColumn(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(5.dp)
    ) {

        item {
            PurchaseItemListComponent(viewModel.purchaseItemList)
        }

        item {
            MetalRateListComponent(viewModel.purchaseMetalRateDtos)
        }

        item {
            AddItemComponent(viewModel)
        }


        item {
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun MetalRateListComponent(rates: List<PurchaseMetalRateDto>) {
    Column(Modifier
        .fillMaxWidth()
        .padding(8.dp)) {
        Text("Metal Rates", style = MaterialTheme.typography.titleMedium)
        if (rates.isEmpty()){
            Text("Metal exchange info will be shown here!", modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }else{
            rates.forEach {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${it.metalName} (${it.categoryId})")
                        Text("₹${it.ratePerGram}/g")
                    }
                }
            }
        }



    }
}

@Composable
fun PurchaseItemListComponent(items: List<PurchaseItemInputDto>) {
    Column(Modifier
        .fillMaxWidth()
        .padding(8.dp)) {
        Text("Added Items", style = MaterialTheme.typography.titleMedium)

        if (items.isEmpty()){
            Text("Added items details will be shown here!", modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
                )
        }else{
            items.forEachIndexed { index, item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(Modifier.padding(8.dp)) {
                        Text("Item ${index + 1}: ${item.name}", fontWeight = FontWeight.Bold)
                        Text("GS Wt: ${item.gsWt}, FN Wt: ${item.fnWt}, Metal Rate: ₹${item.fnMetalRate}")
                        Text("GST: CGST ${item.cgst}%, SGST ${item.sgst}%, IGST ${item.igst}%")
                        if (item.extraChargeDes.isNotEmpty()) {
                            Text("Extra: ${item.extraChargeDes} - ₹${item.extraCharge}")
                        }
                    }
                }
            }
        }

    }
}


@Composable
private fun AddItemComponent(viewModel: PurchaseViewModel) {
    val subCategories = remember { mutableStateListOf<SubCategoryEntity>() }

    Column {
        Text("Add Item")
        Row {
            CusOutlinedTextField(modifier = Modifier.weight(1f),
                state = viewModel.addCat,
                placeholderText = "Category",
                dropdownItems = viewModel.catSubCatDto.map { it.catName },
                onDropdownItemSelected = { selected ->
                    viewModel.addCat.text = selected
                    // Set subcategories from the selected category
                    val selectedCat = viewModel.catSubCatDto.find { it.catName == selected }
                    subCategories.clear()
                    selectedCat?.subCategoryList?.let { subCategories.addAll(it) }
                    viewModel.addSubCat.text = ""
                })
            Spacer(Modifier.width(5.dp))
            CusOutlinedTextField(modifier = Modifier.weight(1f),
                state = viewModel.addSubCat,
                placeholderText = "Sub Category",
                dropdownItems = subCategories.map { it.subCatName },
                onDropdownItemSelected = { selected ->
                    viewModel.addSubCat.text = selected
                })
            Spacer(Modifier.width(5.dp))
            CusOutlinedTextField(
                viewModel.addMetalRate,
                placeholderText = "Fn Metal Rate",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(5.dp))
        Row {
            CusOutlinedTextField(
                viewModel.addGsWt, placeholderText = "Gross Weight", modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(5.dp))
            CusOutlinedTextField(
                viewModel.addPurity,
                placeholderText = "Purity",
                modifier = Modifier.weight(1f),
                dropdownItems = Purity.list(),
            )
            Spacer(Modifier.width(5.dp))
            CusOutlinedTextField(
                viewModel.addFnWt, placeholderText = "Fine Weight", modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(5.dp))
            CusOutlinedTextField(
                viewModel.addWastage, placeholderText = "Wastage", modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(5.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CusOutlinedTextField(
                viewModel.addCGst, placeholderText = "C.Gst", modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(5.dp))
            CusOutlinedTextField(
                viewModel.addIGst, placeholderText = "I.Gst", modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(5.dp))
            CusOutlinedTextField(
                viewModel.addSGst, placeholderText = "S.Gst", modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(5.dp))
            CusOutlinedTextField(
                viewModel.addExtraChargeDes,
                placeholderText = "E.Charge Des",
                modifier = Modifier.weight(1.5f)
            )
            Spacer(Modifier.width(5.dp))
            CusOutlinedTextField(
                viewModel.addExtraCharge,
                placeholderText = "Extra Charge",
                modifier = Modifier.weight(1f)
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
            Spacer(Modifier.width(8.dp))
            Text("Add", Modifier
                .bounceClick {
                    viewModel.addPurchaseItem()
                }
                .background(
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(16.dp),
                )
                .padding(10.dp), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Text("Metal Trade (Give)")
        Row(verticalAlignment = Alignment.CenterVertically) {
            CusOutlinedTextField(modifier = Modifier.weight(1f),
                state = viewModel.addExchangeCategory,
                placeholderText = "Category",
                dropdownItems = viewModel.catSubCatDto.map { it.catName },
                onDropdownItemSelected = { selected ->
                    viewModel.addExchangeCategory.text = selected
                })
            Spacer(Modifier.width(5.dp))
            CusOutlinedTextField(
                viewModel.addExchangeFnWeight,
                placeholderText = "Fine Weight",
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.weight(0.5f))
            Text("Clear", Modifier
                .bounceClick {

                }
                .background(
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(16.dp),
                )
                .padding(10.dp), fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text("Add", Modifier
                .bounceClick {

                }
                .background(
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(16.dp),
                )
                .padding(10.dp), fontWeight = FontWeight.Bold)
        }
    }
}

