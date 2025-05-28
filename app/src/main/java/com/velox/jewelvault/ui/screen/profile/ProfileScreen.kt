package com.velox.jewelvault.ui.screen.profile

import android.widget.Toast
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velox.jewelvault.ui.components.CusOutlinedTextField
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.ui.components.bounceClick
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.mainScope

@Composable
fun ProfileScreen(profileViewModel: ProfileViewModel) {
    val baseViewModel = LocalBaseViewModel.current
    val isEditable = remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect (true){
        profileViewModel.getStoreData()
    }

    Box(
        Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopStart
    ) {
        Column(Modifier.fillMaxSize()) {
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(4f)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
                    .padding(10.dp)
            ) {
                Row(
                    Modifier
                        .height(70.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.weight(0.5f))
                    BasicTextField(
                        value = profileViewModel.shopName.text,
                        onValueChange = {
                            profileViewModel.shopName.onTextChanged(it)
                        },

                        textStyle = TextStyle(
                            fontSize = 36.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(Modifier.weight(1f))
                    if (!isEditable.value)
                        Icon(
                            Icons.Default.Edit,
                            null,
                            modifier = Modifier.bounceClick { isEditable.value = true })
                }

                Spacer(Modifier.height(50.dp))

                Row(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(Modifier.weight(1f)) {
                        CusOutlinedTextField(
                            modifier = Modifier
                                .padding(vertical = 5.dp)
                                .fillMaxWidth(),
                            state = profileViewModel.propName,
                            placeholderText = "Proprietor",
                            keyboardType = KeyboardType.Text,
                            readOnly = !isEditable.value
                        )
                        CusOutlinedTextField(
                            modifier = Modifier
                                .padding(vertical = 5.dp)
                                .fillMaxWidth(),
                            state = profileViewModel.userEmail,
                            placeholderText = "Email",
                            keyboardType = KeyboardType.Email,
                            readOnly = !isEditable.value
                        )
                        CusOutlinedTextField(
                            modifier = Modifier
                                .padding(vertical = 5.dp)
                                .fillMaxWidth(),
                            state = profileViewModel.userMobile,
                            placeholderText = "Mobile No",
                            keyboardType = KeyboardType.Phone,
                            readOnly = !isEditable.value
                        )

                        CusOutlinedTextField(
                            modifier = Modifier
                                .padding(vertical = 5.dp)
                                .fillMaxWidth(),
                            state = profileViewModel.address,
                            placeholderText = "Address",
                            keyboardType = KeyboardType.Text,
                            readOnly = !isEditable.value,
                            maxLines = 3
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        CusOutlinedTextField(
                            modifier = Modifier
                                .padding(vertical = 5.dp)
                                .fillMaxWidth(),
                            state = profileViewModel.registrationNo,
                            placeholderText = "Registration No",
                            keyboardType = KeyboardType.Text,
                            readOnly = !isEditable.value
                        )
                        CusOutlinedTextField(
                            modifier = Modifier
                                .padding(vertical = 5.dp)
                                .fillMaxWidth(),
                            state = profileViewModel.gstinNo,
                            placeholderText = "GSTIN No",
                            keyboardType = KeyboardType.Text,
                            readOnly = !isEditable.value
                        )
                        CusOutlinedTextField(
                            modifier = Modifier
                                .padding(vertical = 5.dp)
                                .fillMaxWidth(),
                            state = profileViewModel.panNumber,
                            placeholderText = "PAN No",
                            keyboardType = KeyboardType.Text,
                            readOnly = !isEditable.value
                        )
                    }
                }

                if (isEditable.value)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            "Cancel", Modifier
                                .clickable {
                                    isEditable.value = !isEditable.value
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
                            "Done", Modifier
                                .clickable {

                                    profileViewModel.saveStoreData(onSuccess = {
                                        mainScope {
                                            Toast.makeText(
                                                context,
                                                "Store Details updated. Thank you!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                        onFailure = {
                                            mainScope {
                                                Toast.makeText(
                                                    context,
                                                    "Unable to update Store Details.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }

                                    )

                                    isEditable.value = !isEditable.value
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

        Box(
            Modifier
                .size(250.dp)
                .offset(x = 50.dp)
                .shadow(2.dp, RoundedCornerShape(18.dp), spotColor = Color.LightGray)
                .background(Color.White, RoundedCornerShape(18.dp))
                .padding(5.dp)
        ) {

        }
    }
}