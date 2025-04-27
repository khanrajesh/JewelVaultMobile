package com.velox.jewelvault.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.twotone.Add
import androidx.compose.material.icons.twotone.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velox.jewelvault.data.MetalRatesTicker
import com.velox.jewelvault.ui.components.CusOutlinedTextField
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.ui.components.bounceClick
import com.velox.jewelvault.utils.LocalNavController
import com.velox.jewelvault.utils.VaultPreview
import com.velox.jewelvault.ui.nav.Screens
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.isLandscape
import com.velox.jewelvault.utils.rememberCurrentDateTime
import kotlinx.coroutines.launch

@Composable
@VaultPreview
fun SellInvoiceScreenPreview() {
    SellInvoiceScreen()
}

@Composable
fun SellInvoiceScreen() {
    if (isLandscape()) {
        SellInvoiceLandscape()
    } else {
        SellInvoicePortrait()
    }
}

@Composable
fun SellInvoicePortrait() {
    Text("Sell invoice Landscape view")
}

@Composable
fun SellInvoiceLandscape() {
    val context = LocalContext.current
    val navHost = LocalNavController.current
    val baseViewModel = LocalBaseViewModel.current
    val currentDateTime = rememberCurrentDateTime()
    val coroutineScope = rememberCoroutineScope()
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(5.dp)
    ) {

        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                .padding(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                coroutineScope.launch {
                    baseViewModel.refreshMetalRates(context = context)
               }
            }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh"
                )
            }

            MetalRatesTicker( Modifier.height(50.dp).weight(1f))
            Text(text = currentDateTime.value)
        }

        Spacer(Modifier.height(5.dp))
        Row(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .weight(2.5f)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .padding(5.dp)
            ) {
                CustomerDetails()

                Spacer(Modifier.height(5.dp))

//                LazyColumn(
//                    Modifier
//                        .weight(1f)
//                        .fillMaxWidth()
//                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
//                        .padding(3.dp)
//                ) {
//
//
//                }

                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {

                        Column(
                            Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                .padding(5.dp)
                        ) {

                            Column(
                                Modifier
                                    .padding(2.dp)
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                            ) {
                                Row(Modifier.fillMaxWidth()) {
                                    Text("S.No", fontWeight = FontWeight.Black, modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center)
                                    Text("Id", fontWeight = FontWeight.Black, modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center)
                                    Text(
                                        "Item",
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.weight(2f), textAlign = TextAlign.Center
                                    )
                                    Text("HSSN", fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                    Text("Gs Wt.", fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                    Text("Fine Wt.", fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                    Text("Metal", fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                    Text("M.Chr", fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                    Text("BSI No", fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)

                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                                }
                                Spacer(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(MaterialTheme.colorScheme.outline)
                                )
                            }
                            LazyColumn(
                                Modifier
                                    .weight(1f)
                            ) {

                                items(5) {it->
                                    Column(
                                        Modifier
                                            .padding(2.dp)
                                            .fillMaxWidth()
                                            .wrapContentHeight()
                                    ) {
                                        Row(Modifier.fillMaxWidth()) {
                                            Text(
                                                "${it+1}. ",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.DarkGray,
                                                modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center
                                            )
                                            Text(
                                                "${it+1}. ",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.DarkGray,
                                                modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center
                                            )
                                            Text(
                                                "Box Chain",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.DarkGray,
                                                modifier = Modifier.weight(2f), textAlign = TextAlign.Center
                                            )
                                            Text(
                                                "1234567890",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.DarkGray,
                                                modifier = Modifier.weight(1f), textAlign = TextAlign.Center
                                            )
                                            Text(
                                                "2.5g",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.DarkGray,
                                                modifier = Modifier.weight(1f), textAlign = TextAlign.Center
                                            )
                                            Text(
                                                "2.45g",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.DarkGray,
                                                modifier = Modifier.weight(1f), textAlign = TextAlign.Center
                                            )
                                            Text(
                                                "Gold/22k",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.DarkGray,
                                                modifier = Modifier.weight(1f), textAlign = TextAlign.Center
                                            )
                                            Text(
                                                "16%",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.DarkGray,
                                                modifier = Modifier.weight(1f), textAlign = TextAlign.Center
                                            )

                                            Text(
                                                "45554846465",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.DarkGray,
                                                modifier = Modifier.weight(1f), textAlign = TextAlign.Center
                                            )

                                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                                        }
                                        Spacer(
                                            Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .background(MaterialTheme.colorScheme.outline)
                                        )
                                    }
                                }
                            }
                        }

                }


                Spacer(Modifier.height(5.dp))

                Row(
                    Modifier
                        .height(50.dp)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                        .padding(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Add Different Item",
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    Row(
                        Modifier
                            .fillMaxHeight()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(10.dp)
                            )
                            .padding(3.dp)
                    ) {
                        Icon(
                            Icons.TwoTone.DateRange, null,

                            modifier = Modifier
                                .fillMaxHeight()
                                .aspectRatio(1f)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(10.dp)
                                )
                        )
                        Spacer(Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center // this centers the text vertically and horizontally
                        ) {
                            BasicTextField(
                                value = "123456789",
                                onValueChange = { },
                                textStyle = TextStyle(
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center // <-- text will be centered horizontally
                                ),
                                modifier = Modifier
                                    .padding(horizontal = 8.dp) // optional inner padding
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Box (  modifier = Modifier
                            .fillMaxHeight()
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(10.dp)
                            ),
                            contentAlignment = Alignment.Center
                            ){
                            Row {
                                Spacer(Modifier.width(5.dp))
                                Text("Add Item")
                                Spacer(Modifier.width(5.dp))
                                Icon(
                                    Icons.TwoTone.Add,
                                    null,

                                    )
                                Spacer(Modifier.width(5.dp))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.width(5.dp))
            Column(
                Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .padding(5.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .padding(3.dp)

            ) {

                Spacer(Modifier.weight(1f))

                Box(modifier = Modifier
                    .bounceClick {
                        navHost.navigate(Screens.SellPreview.route)
                    }
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                    .padding(10.dp), contentAlignment = Alignment.Center) {
                    Text("Proceed", textAlign = TextAlign.Center)
                }

            }
        }
    }
}



@Composable
fun CustomerDetails() {
    /*
    Name
    Mobile No
    Address

    * */
    val name = remember { InputFieldState() }
    val mobileNo = remember { InputFieldState() }
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(5.dp)
    ) {
        Text("Customer Details")

        Column {

            Row(Modifier) {
                CusOutlinedTextField(name, placeholderText = "Name", modifier = Modifier.weight(2f))
                Spacer(Modifier.width(5.dp))
                CusOutlinedTextField(name,
                    placeholderText = "Mobile No",
                    modifier = Modifier.weight(1f),
                    trailingIcon = Icons.Default.Search,
                    onTrailingIconClick = {
                        Toast.makeText(context, "Search by customer number.", Toast.LENGTH_SHORT)
                            .show()
                    },
                    maxLines = 1
                )
            }
            Spacer(Modifier.height(5.dp))
            CusOutlinedTextField(
                name,
                placeholderText = "Address",
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1
            )
            Spacer(Modifier.height(5.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "GSTIN/PAN Details : ",
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.width(5.dp))
                CusOutlinedTextField(
                    name,
                    placeholderText = "GSTIN/PAN ID",
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
            }
        }
    }
}




