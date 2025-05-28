package com.velox.jewelvault.ui.screen.purchase

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.velox.jewelvault.data.MetalRatesTicker
import com.velox.jewelvault.ui.components.CusOutlinedTextField
import com.velox.jewelvault.ui.components.bounceClick
import com.velox.jewelvault.utils.LocalBaseViewModel
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
//                .verticalScroll(scrollState)
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
            PurchaseItemDetails(viewModel)
        }
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
                    maxLines = 1, keyboardType = KeyboardType.Phone,
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
                Text(
                    "GSTIN/PAN : ", textAlign = TextAlign.Center
                )

                Spacer(Modifier.width(5.dp))
                CusOutlinedTextField(
                    viewModel.firmGstin,
                    placeholderText = "GSTIN/PAN ID",
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun PurchaseItemDetails(viewModel: PurchaseViewModel) {
    LazyColumn(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(5.dp)
    ) {

        item {

        }

        item {
            Column {
                Text("Add Item")
                Row {
                    CusOutlinedTextField(
                        viewModel.addCat,
                        placeholderText = "Bill No",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(5.dp))
                    CusOutlinedTextField(
                        viewModel.addCat,
                        placeholderText = "Category",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(5.dp))
                    CusOutlinedTextField(
                        viewModel.addSubCat,
                        placeholderText = "Sub Category Name",
                        modifier = Modifier.weight(1f)
                    )
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
                        viewModel.addGsWt,
                        placeholderText = "Gross Weight",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(5.dp))
                    CusOutlinedTextField(
                        viewModel.addPurity,
                        placeholderText = "Purity",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(5.dp))
                    CusOutlinedTextField(
                        viewModel.addFnWt,
                        placeholderText = "Fine Weight",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(5.dp))
                    CusOutlinedTextField(
                        viewModel.addWastage,
                        placeholderText = "Wastage",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(5.dp))
                    CusOutlinedTextField(
                        viewModel.addExtraChargeDes,
                        placeholderText = "Extra Charge Des",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(5.dp))
                    CusOutlinedTextField(
                        viewModel.addExtraCharge,
                        placeholderText = "Extra Charge",
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(Modifier.weight(1f))
                    Text(
                        "Clear", Modifier
                            .bounceClick {

                            }
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(16.dp),
                            )
                            .padding(10.dp), fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Add", Modifier
                            .bounceClick {

                            }
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(16.dp),
                            )
                            .padding(10.dp), fontWeight = FontWeight.Bold
                    )
                }

            }

        }
    }
}
