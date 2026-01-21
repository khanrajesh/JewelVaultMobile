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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.AddAPhoto
import androidx.compose.material.icons.twotone.Edit
import androidx.compose.material.icons.twotone.Storefront
import androidx.compose.material.icons.twotone.Sync
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.velox.jewelvault.ui.components.CusOutlinedTextField
import com.velox.jewelvault.ui.components.CusOutlinedTextFieldInternal
import com.velox.jewelvault.ui.components.bounceClick
import com.velox.jewelvault.ui.nav.SubScreens
import com.velox.jewelvault.utils.InputValidator
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.LocalSubNavController
import com.velox.jewelvault.utils.isLandscape
import com.velox.jewelvault.utils.ioScope
import com.velox.jewelvault.utils.log
import com.velox.jewelvault.utils.mainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

@Composable
fun ProfileScreen(profileViewModel: ProfileViewModel, firstLaunch: Boolean) {

    profileViewModel.currentScreenHeadingState.value = "Profile"
    val baseViewModel = LocalBaseViewModel.current
    val isEditable = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val subNavController = LocalSubNavController.current
    val scrollState = rememberScrollState()
    val configuration = LocalConfiguration.current
    val isLandscape = isLandscape()
    val imageSize =
        if (isLandscape) 220.dp else (configuration.screenWidthDp.dp * 0.4f).coerceIn(120.dp, 180.dp)
    val isLoading by profileViewModel.isLoading
    val userManagementCardModifier =
        if (isEditable.value) Modifier else Modifier.bounceClick {
            subNavController.navigate(SubScreens.UserManagement.route)
        }
    val shopNameValidation: (String) -> String? = { input ->
        val normalized = InputValidator.sanitizeText(input)
        when {
            normalized.isBlank() -> "Store name is required"
            normalized.length < 3 -> "Store name must be at least 3 characters"
            else -> null
        }
    }
    val proprietorValidation: (String) -> String? = { input ->
        val normalized = InputValidator.sanitizeText(input)
        when {
            normalized.isBlank() -> "Proprietor name is required"
            normalized.length < 2 -> "Proprietor name must be at least 2 characters"
            else -> null
        }
    }
    val emailValidation: (String) -> String? = { input ->
        val trimmed = input.trim()
        when {
            trimmed.isBlank() -> "Email address is required"
            !InputValidator.isValidEmail(trimmed) -> "Enter a valid email address"
            else -> null
        }
    }
    val phoneValidation: (String) -> String? = { input ->
        val digits = input.filter { char -> char.isDigit() }
        when {
            digits.isBlank() -> "Mobile number is required"
            digits.length != 10 || !InputValidator.isValidPhoneNumber(digits) -> "Enter a valid 10-digit mobile number"
            else -> null
        }
    }
    val addressValidation: (String) -> String? = { input ->
        val normalized = InputValidator.sanitizeText(input)
        when {
            normalized.isBlank() -> "Store address is required"
            normalized.length < 10 -> "Please enter a complete address (min 10 characters)"
            else -> null
        }
    }
    val registrationValidation: (String) -> String? = { input ->
        val normalized = InputValidator.sanitizeText(input).uppercase()
        when {
            normalized.isBlank() -> "Registration number is required"
            else -> null
        }
    }
    val gstinValidation: (String) -> String? = { input ->
        val normalized = input.trim().uppercase()
        when {
            normalized.isBlank() -> "GSTIN number is required"
            !InputValidator.isValidGSTIN(normalized) -> "Enter a valid GSTIN (15 characters, e.g. 22AAAAA0000A1Z5)"
            else -> null
        }
    }
    val panValidation: (String) -> String? = { input ->
        val normalized = input.trim().uppercase()
        when {
            normalized.isBlank() -> "PAN number is required"
            !InputValidator.isValidPAN(normalized) -> "Enter a valid PAN (format ABCDE1234F)"
            else -> null
        }
    }
    val upiValidation: (String) -> String? = { input ->
        val trimmed = input.trim()
        when {
            trimmed.isBlank() -> "UPI ID is required"
            !InputValidator.isValidUpiId(trimmed) -> "Enter a valid UPI ID (name@bank)"
            else -> null
        }
    }
    val store: Triple<Flow<String>, Flow<String>, Flow<String>> = profileViewModel.dataStoreManager.getSelectedStoreInfo()
    LaunchedEffect(Unit) {
        // Ensure store data and image are loaded on entry
        val storeId = store.first.first()
        profileViewModel.getStoreData(storeId, isFirstLaunch = firstLaunch)
        baseViewModel.loadStoreImage()
    }
    
    // Watch for changes in store image and refresh if needed
    LaunchedEffect(baseViewModel.storeImage.value, baseViewModel.localLogoUri.value) {
        // This will trigger recomposition when store image changes
    }

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
        Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
                    .padding(10.dp)
            ) {
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    if (!isEditable.value && !isLoading) {
                        Icon(
                            Icons.TwoTone.Sync,
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
                                .padding(end = 12.dp)
                        )
                    }
                    if (!isEditable.value) {
                        Icon(
                            Icons.TwoTone.Edit,
                            contentDescription = "Edit Profile",
                            modifier = Modifier.bounceClick { isEditable.value = true }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        Modifier
                            .size(imageSize)
                            .shadow(2.dp, CircleShape, spotColor = Color.LightGray)
                            .background(Color.White, CircleShape)
                            .padding(6.dp)
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
                                .clip(CircleShape)
                                .clickable(enabled = isEditable.value) {
                                    imagePickerLauncher.launch("image/*")
                                }
                                .border(
                                    width = 2.dp,
                                    color = if (isEditable.value) MaterialTheme.colorScheme.primary else Color.Gray,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (profileViewModel.selectedImageUri.value.isNullOrBlank() && baseViewModel.getLogoUri() == null && (baseViewModel.storeImage.value.isNullOrBlank())) {
                                // Show placeholder when no image
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = if (isEditable.value) Icons.TwoTone.AddAPhoto else Icons.TwoTone.Storefront,
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
                                val imageData = when {
                                    // If we have a newly selected image file, show it immediately
                                    profileViewModel.selectedImageFileUri.value != null -> {
                                        log("ProfileScreen: Using selectedImageFileUri: ${profileViewModel.selectedImageFileUri.value}")
                                        profileViewModel.selectedImageFileUri.value
                                    }
                                    // Prefer remote URL if present
                                    !profileViewModel.selectedImageUri.value.isNullOrBlank() -> {
                                        log("ProfileScreen: Using selectedImageUri: ${profileViewModel.selectedImageUri.value}")
                                        profileViewModel.selectedImageUri.value
                                    }
                                    !baseViewModel.storeImage.value.isNullOrBlank() -> {
                                        log("ProfileScreen: Using storeImage: ${baseViewModel.storeImage.value}")
                                        baseViewModel.storeImage.value
                                    }
                                    // Fallback to local logo file
                                    baseViewModel.hasLocalLogo() -> {
                                        log("ProfileScreen: Using local logo: ${baseViewModel.getLogoUri()}")
                                        baseViewModel.getLogoUri()
                                    }
                                    else -> {
                                        log("ProfileScreen: No image data available")
                                        null
                                    }
                                }
                                
                                if (imageData != null) {
                                    log("ProfileScreen: ImageData type: ${imageData::class.simpleName}, value: $imageData")
                                    
                                    // Create ImageRequest with proper configuration
                                    val imageRequest = ImageRequest.Builder(context)
                                        .data(imageData)
                                        .crossfade(true)
                                        .error(android.R.drawable.ic_menu_gallery)
                                        .placeholder(android.R.drawable.ic_menu_gallery)
                                        .listener(
                                            onError = { request, result ->
                                                log("ProfileScreen: Coil load error for ${request.data}: ${result.throwable.message}")
                                            },
                                            onSuccess = { request, _ ->
                                                log("ProfileScreen: Coil load success for ${request.data}")
                                            }
                                        )
                                        .build()
                                    
                                    Image(
                                        painter = rememberAsyncImagePainter(imageRequest),
                                        contentDescription = "Store Image",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                
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
                                                imageVector = Icons.TwoTone.Edit,
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

                Spacer(Modifier.height(16.dp))

                Row(
                    Modifier
                    .fillMaxWidth()
                    .heightIn(min = 70.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val shopNameTextStyle = TextStyle(
                        fontSize = 36.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (isEditable.value) {
                            CusOutlinedTextFieldInternal(
                                modifier = Modifier
                                    .padding(vertical = 5.dp)
                                    .fillMaxWidth(),
                                state = profileViewModel.shopName,
                                placeholderText = "Store Name",
                                keyboardType = KeyboardType.Text,
                                singleLine = true,
                                textStyle = shopNameTextStyle,
                                onTextChange = {
                                    profileViewModel.shopName.text = InputValidator.sanitizeText(it)
                                },
                                validation = shopNameValidation
                            )
                        } else {
                            Text(
                                text = profileViewModel.shopName.text,
                                modifier = Modifier.fillMaxWidth(),
                                style = shopNameTextStyle
                            )
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                if (isLandscape) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(Modifier.weight(1f)) {
                            CusOutlinedTextField(
                                modifier = Modifier
                                    .padding(vertical = 5.dp)
                                    .fillMaxWidth(),
                                state = profileViewModel.propName,
                                placeholderText = "Proprietor",
                                keyboardType = KeyboardType.Text,
                                readOnly = !isEditable.value,
                                onTextChange = {
                                    profileViewModel.propName.text = InputValidator.sanitizeText(it)
                                },
                                validation = proprietorValidation
                            )
                            CusOutlinedTextField(
                                modifier = Modifier
                                    .padding(vertical = 5.dp)
                                    .fillMaxWidth(),
                                state = profileViewModel.userEmail,
                                placeholderText = "Email",
                                keyboardType = KeyboardType.Email,
                                readOnly = !isEditable.value,
                                onTextChange = {
                                    profileViewModel.userEmail.text = it.trim()
                                },
                                validation = emailValidation
                            )
                            CusOutlinedTextField(
                                modifier = Modifier
                                    .padding(vertical = 5.dp)
                                    .fillMaxWidth(),
                                state = profileViewModel.userMobile,
                                placeholderText = "Mobile No",
                                keyboardType = KeyboardType.Phone,
                                readOnly = !isEditable.value,
                                onTextChange = { input ->
                                    val digits = input.filter { it.isDigit() }.take(10)
                                    profileViewModel.userMobile.text = digits
                                },
                                validation = phoneValidation
                            )

                            CusOutlinedTextField(
                                modifier = Modifier
                                    .padding(vertical = 5.dp)
                                    .fillMaxWidth(),
                                state = profileViewModel.address,
                                placeholderText = "Address",
                                keyboardType = KeyboardType.Text,
                                readOnly = !isEditable.value,
                                maxLines = 3,
                                onTextChange = {
                                    profileViewModel.address.text = it
                                },
                                validation = addressValidation
                            )
                            CusOutlinedTextField(
                                modifier = Modifier
                                    .padding(vertical = 5.dp)
                                    .fillMaxWidth(),
                                state = profileViewModel.registrationNo,
                                placeholderText = "Registration No",
                                keyboardType = KeyboardType.Text,
                                readOnly = !isEditable.value,
                                onTextChange = { input ->
                                    profileViewModel.registrationNo.text = input.uppercase()
                                },
                                validation = registrationValidation
                            )
                            CusOutlinedTextField(
                                modifier = Modifier
                                    .padding(vertical = 5.dp)
                                    .fillMaxWidth(),
                                state = profileViewModel.gstinNo,
                                placeholderText = "GSTIN No",
                                keyboardType = KeyboardType.Text,
                                readOnly = !isEditable.value,
                                onTextChange = { input ->
                                    profileViewModel.gstinNo.text = input.uppercase()
                                },
                                validation = gstinValidation
                            )
                            CusOutlinedTextField(
                                modifier = Modifier
                                    .padding(vertical = 5.dp)
                                    .fillMaxWidth(),
                                state = profileViewModel.panNumber,
                                placeholderText = "PAN No",
                                keyboardType = KeyboardType.Text,
                                readOnly = !isEditable.value,
                                onTextChange = { input ->
                                    profileViewModel.panNumber.text = input.uppercase()
                                },
                                validation = panValidation
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
                                readOnly = !isEditable.value,
                                onTextChange = {
                                    profileViewModel.upiId.text = it.trim()
                                },
                                validation = upiValidation
                            )

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Column(
                                    modifier = userManagementCardModifier
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
                } else {
                    Column(
                        Modifier.fillMaxWidth()
                    ) {
                        Column {
                            CusOutlinedTextField(
                                modifier = Modifier
                                    .padding(vertical = 5.dp)
                                    .fillMaxWidth(),
                                state = profileViewModel.propName,
                                placeholderText = "Proprietor",
                                keyboardType = KeyboardType.Text,
                                readOnly = !isEditable.value,
                                onTextChange = {
                                    profileViewModel.propName.text = InputValidator.sanitizeText(it)
                                },
                                validation = proprietorValidation
                            )
                            CusOutlinedTextField(
                                modifier = Modifier
                                    .padding(vertical = 5.dp)
                                    .fillMaxWidth(),
                                state = profileViewModel.userEmail,
                                placeholderText = "Email",
                                keyboardType = KeyboardType.Email,
                                readOnly = !isEditable.value,
                                onTextChange = {
                                    profileViewModel.userEmail.text = it.trim()
                                },
                                validation = emailValidation
                            )
                            CusOutlinedTextField(
                                modifier = Modifier
                                    .padding(vertical = 5.dp)
                                    .fillMaxWidth(),
                                state = profileViewModel.userMobile,
                                placeholderText = "Mobile No",
                                keyboardType = KeyboardType.Phone,
                                readOnly = !isEditable.value,
                                onTextChange = { input ->
                                    val digits = input.filter { it.isDigit() }.take(10)
                                    profileViewModel.userMobile.text = digits
                                },
                                validation = phoneValidation
                            )

                            CusOutlinedTextField(
                                modifier = Modifier
                                    .padding(vertical = 5.dp)
                                    .fillMaxWidth(),
                                state = profileViewModel.address,
                                placeholderText = "Address",
                                keyboardType = KeyboardType.Text,
                                readOnly = !isEditable.value,
                                maxLines = 3,
                                onTextChange = {
                                    profileViewModel.address.text = it
                                },
                                validation = addressValidation
                            )
                            CusOutlinedTextField(
                                modifier = Modifier
                                    .padding(vertical = 5.dp)
                                    .fillMaxWidth(),
                                state = profileViewModel.registrationNo,
                                placeholderText = "Registration No",
                                keyboardType = KeyboardType.Text,
                                readOnly = !isEditable.value,
                                onTextChange = { input ->
                                    profileViewModel.registrationNo.text = input.uppercase()
                                },
                                validation = registrationValidation
                            )
                            CusOutlinedTextField(
                                modifier = Modifier
                                    .padding(vertical = 5.dp)
                                    .fillMaxWidth(),
                                state = profileViewModel.gstinNo,
                                placeholderText = "GSTIN No",
                                keyboardType = KeyboardType.Text,
                                readOnly = !isEditable.value,
                                onTextChange = { input ->
                                    profileViewModel.gstinNo.text = input.uppercase()
                                },
                                validation = gstinValidation
                            )
                            CusOutlinedTextField(
                                modifier = Modifier
                                    .padding(vertical = 5.dp)
                                    .fillMaxWidth(),
                                state = profileViewModel.panNumber,
                                placeholderText = "PAN No",
                                keyboardType = KeyboardType.Text,
                                readOnly = !isEditable.value,
                                onTextChange = { input ->
                                    profileViewModel.panNumber.text = input.uppercase()
                                },
                                validation = panValidation
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        Column {
                            CusOutlinedTextField(
                                modifier = Modifier
                                    .padding(vertical = 5.dp)
                                    .fillMaxWidth(),
                                state = profileViewModel.upiId,
                                placeholderText = "UPI ID (for QR payments)",
                                keyboardType = KeyboardType.Email,
                                readOnly = !isEditable.value,
                                onTextChange = {
                                    profileViewModel.upiId.text = it.trim()
                                },
                                validation = upiValidation
                            )

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Column(
                                    modifier = userManagementCardModifier
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
                }

                Spacer(Modifier.height(16.dp))

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
                                profileViewModel.clearFieldErrors()
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
                            profileViewModel.saveStoreData(
                                onSuccess = {
                                    baseViewModel.loadStoreImage()
                                    if (firstLaunch) {
                                        ioScope {
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
                                    isEditable.value = false
                                },
                                onFailure = {
                                    // Validation and error messages handled in ViewModel
                                },
                                onImageUpdated = {
                                    baseViewModel.loadStoreImage()
                                }
                            )
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
