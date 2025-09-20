package com.velox.jewelvault.ui.screen.profile

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.velox.jewelvault.ui.components.CusOutlinedTextField
import com.velox.jewelvault.ui.components.bounceClick
import com.velox.jewelvault.ui.nav.SubScreens
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.LocalSubNavController
import com.velox.jewelvault.utils.ioScope
import com.velox.jewelvault.utils.mainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(profileViewModel: ProfileViewModel, firstLaunch: Boolean) {

    profileViewModel.currentScreenHeadingState.value = "Profile"
    val baseViewModel = LocalBaseViewModel.current
    val isEditable = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val subNavController = LocalSubNavController.current
    val store: Triple<Flow<String>, Flow<String>, Flow<String>> = profileViewModel.dataStoreManager.getSelectedStoreInfo()

    BackHandler {
        subNavController.navigate(SubScreens.Dashboard.route) {
            popUpTo(SubScreens.Dashboard.route) {
                inclusive = true
            }
        }
    }
    
    LaunchedEffect(true) {
        if (firstLaunch){
            isEditable.value = true
        }
        val storeId = store.first.first()
        profileViewModel.getStoreData(storeId,firstLaunch)
    }

    Box(
        Modifier.fillMaxWidth(), contentAlignment = Alignment.TopStart
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
                    .background(
                        MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
                    .padding(10.dp)
            ) {
                Row(
                    Modifier.height(70.dp), verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.width(310.dp))
                    BasicTextField(
                        modifier = if (isEditable.value) {
                            Modifier
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(5.dp)
                                )
                                .padding(5.dp)
                        } else {
                            Modifier
                        },
                        value = profileViewModel.shopName.text,
                        onValueChange = {
                            profileViewModel.shopName.onTextChanged(it)
                        },
                        textStyle = TextStyle(
                            fontSize = 36.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                    )
                    Spacer(Modifier.weight(1f))
                    
                    // Sync button
                    val isLoading by profileViewModel.isLoading
                    if (!isEditable.value && !isLoading) {
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = "Sync from Cloud",
                            modifier = Modifier
                                .bounceClick {
                                    profileViewModel.syncFromFirestore(
                                        onSuccess = {
                                            // Success handled by ViewModel
                                        },
                                        onFailure = {
                                            // Failure handled by ViewModel
                                        }
                                    )
                                }
                                .padding(end = 8.dp)
                        )
                    }
                    
                    // Loading indicator
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    
                    if (!isEditable.value) Icon(Icons.Default.Edit,
                        null,
                        modifier = Modifier.bounceClick { isEditable.value = true })
                }

                Spacer(Modifier.height(50.dp))

                Row(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
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
                            readOnly = !isEditable.value,
                            validation = { input -> if (input.length != 10) "Please Enter Valid Number" else null }
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
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        CusOutlinedTextField(
                            modifier = Modifier
                                .padding(vertical = 5.dp)
                                .fillMaxWidth(),
                            state = profileViewModel.upiId,
                            placeholderText = "UPI ID (for QR payments)",
                            keyboardType = KeyboardType.Email,
                            readOnly = !isEditable.value
                        )

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .bounceClick{
                                        subNavController.navigate(SubScreens.UserManagement.route)
                                    }
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                    Text(
                                        text = "User Management",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )


                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Add, edit, or remove users from your store. Manage roles and permissions for different user types.",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    UserRoleChip("Admin", MaterialTheme.colorScheme.primary)
                                    UserRoleChip("Manager", MaterialTheme.colorScheme.secondary)
                                    UserRoleChip("Worker", MaterialTheme.colorScheme.tertiary)
                                    UserRoleChip("Salesperson", MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                    }


                }

                // User Management Card

                if (isEditable.value) Row(
                    Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Spacer(Modifier.weight(1f))
                    Text("Cancel", Modifier
                        .clickable {
                            ioScope {
                                isEditable.value = !isEditable.value
                                // Reset to original data when canceling
                                val storeId = store.first.first()
                                profileViewModel.getStoreData(storeId)
                            }
                        }
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(16.dp),
                        )
                        .padding(10.dp), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(50.dp))
                    Text("Done", Modifier
                        .clickable {
                            if (profileViewModel.shopName.text.isNotBlank() && profileViewModel.propName.text.isNotBlank() && profileViewModel.userEmail.text.isNotBlank() && profileViewModel.userMobile.text.isNotBlank() && profileViewModel.address.text.isNotBlank() && profileViewModel.registrationNo.text.isNotBlank() && profileViewModel.gstinNo.text.isNotBlank() && profileViewModel.panNumber.text.isNotBlank()) {
                                profileViewModel.saveStoreData(onSuccess = {
                                    // Refresh the store image in BaseViewModel
                                    baseViewModel.loadStoreImage()
                                    if (firstLaunch) {
                                        ioScope {
//                                            profileViewModel.initializeDefaultCategories()
//                                            delay(100)
                                            mainScope {
                                                subNavController.navigate(SubScreens.Dashboard.route) {
                                                    popUpTo(SubScreens.Dashboard.route) {
                                                        inclusive = true
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    profileViewModel.snackBarState.value =
                                        "Store Details updated successfully!"
                                }, onFailure = {
                                    // Error message is already set in ViewModel
                                })
                                isEditable.value = !isEditable.value
                            } else {
                                profileViewModel.snackBarState.value = "Please fill all the required fields."
                            }
                        }
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(16.dp),
                        )
                        .padding(10.dp), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(50.dp))
                }
            }
        }

        // Image upload box
        Box(
            Modifier
                .size(250.dp)
                .offset(x = 50.dp)
                .shadow(2.dp, RoundedCornerShape(18.dp), spotColor = Color.LightGray)
                .background(Color.White, RoundedCornerShape(18.dp))
                .padding(5.dp)
        ) {
            val imagePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                uri?.let { selectedUri ->
                    profileViewModel.setSelectedImageFile(selectedUri)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(enabled = isEditable.value) {
                        imagePickerLauncher.launch("image/*")
                    }
                    .border(
                        width = 2.dp,
                        color = if (isEditable.value) MaterialTheme.colorScheme.primary else Color.Gray,
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (profileViewModel.selectedImageUri.value.isNullOrBlank()) {
                    // Show placeholder when no image
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (isEditable.value) Icons.Default.Add else Icons.Outlined.Add,
                            contentDescription = "Add Store Image",
                            modifier = Modifier.size(48.dp),
                            tint = if (isEditable.value) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isEditable.value) "Add Store\nImage" else "Store Image",
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            color = if (isEditable.value) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                } else {
                    val imageData = if (baseViewModel.hasLocalLogo()) {
                        baseViewModel.getLogoUri()
                    } else {
                        profileViewModel.selectedImageUri.value
                    }
                    
                    Image(
                        painter = rememberAsyncImagePainter(
                            ImageRequest.Builder(context)
                                .data(imageData)
                                .build()
                        ),
                        contentDescription = "Store Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Show edit overlay when in edit mode
                    if (isEditable.value) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f))
                                .clickable {
                                    imagePickerLauncher.launch("image/*")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val isUploading by profileViewModel.isUploadingImage
                            if (isUploading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = Color.White,
                                    strokeWidth = 3.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Image",
                                    modifier = Modifier.size(32.dp),
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserRoleChip(role: String, color: Color) {
    Surface(
        modifier = Modifier.padding(horizontal = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f),
//        border = border(
//            width = 1.dp,
//            color = color,
//            shape = RoundedCornerShape(16.dp)
//        )
    ) {
        Text(
            text = role,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 12.sp,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}