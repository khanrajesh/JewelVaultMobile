package com.velox.jewelvault.ui.screen.user_management

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Add
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.Edit
import androidx.compose.material.icons.twotone.Person
import androidx.compose.material.icons.twotone.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.velox.jewelvault.data.roomdb.entity.users.UsersEntity
import com.velox.jewelvault.ui.components.CusOutlinedTextField
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.ui.components.bounceClick
import com.velox.jewelvault.utils.log

@Composable
fun UserManagementScreen(userManagementViewModel: UserManagementViewModel = hiltViewModel()) {
    val users by userManagementViewModel.users.collectAsState()
    val operationSuccess by userManagementViewModel.operationSuccess.collectAsState()
    var showAddEditDialog by remember { mutableStateOf(false) }
    var selectedUserForEdit by remember { mutableStateOf<UsersEntity?>(null) }


    // Clear form when operation is successful
    LaunchedEffect(operationSuccess) {
        if (operationSuccess) {
            userManagementViewModel.clearForm()
            userManagementViewModel._operationSuccess.value = false
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 18.dp))
            .padding(5.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "User Management",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(start = 10.dp)
            )
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = { userManagementViewModel.syncUsersFromFirestore() }
            ) {
                Icon(Icons.TwoTone.Sync, contentDescription = "Sync from Cloud")
            }
            IconButton(
                onClick = {
                    selectedUserForEdit = null
                    userManagementViewModel.clearForm()
                    showAddEditDialog = true
                }
            ) {
                Icon(Icons.TwoTone.Add, contentDescription = "Add User")
            }
        }

        LazyColumn(
            Modifier.fillMaxWidth()
        ) {
            // Users Grid
            item {
                Text(
                    text = "Users (${users.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 10.dp, bottom = 8.dp)
                )
            }

            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 600.dp)
                ) {
                    items(users) { user ->
                        UserCard(
                            modifier = Modifier,
                            user = user,
                            onCardClick = {
                                selectedUserForEdit = user
                                userManagementViewModel.getUser(user.userId)
                                showAddEditDialog = true
                            },
                            onEdit = {
                                selectedUserForEdit = user
                                userManagementViewModel.getUser(user.userId)
                                showAddEditDialog = true
                            },
                            onDelete = {
                                userManagementViewModel.deleteUser(user.userId)
                            }
                        )
                    }
                }
            }
        }

        // Add/Edit User Dialog
        if (showAddEditDialog) {
            AddEditUserDialog(
                viewModel = userManagementViewModel,
                userToEdit = selectedUserForEdit,
                onDismiss = {
                    showAddEditDialog = false
                    selectedUserForEdit = null
                    userManagementViewModel.clearForm()
                },
                onSave = {
                    if (selectedUserForEdit != null) {
                        userManagementViewModel.updateUser()
                    } else {
                        userManagementViewModel.addUser()
                    }
                    showAddEditDialog = false
                    selectedUserForEdit = null
                }
            )
        }
    }
}


@Composable
fun RoleDropdown(
    state: InputFieldState,
    onRoleSelected: (String) -> Unit
) {
    val roles = listOf("Manager", "Worker", "Salesperson")

    Box(modifier = Modifier.fillMaxWidth()) {
        CusOutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            state = state,
            placeholderText = "Select Role *",
            readOnly = true,
            dropdownItems = roles,
            onDropdownItemSelected = { role ->
                state.text = role
                onRoleSelected(role)
            }
        )
    }
}

@Composable
fun UserCard(
    modifier: Modifier,
    user: UsersEntity,
    onCardClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isDefaultUser = user.role.lowercase() == "admin"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(12.dp)
            )
            .padding(4.dp)
            .clickable { onCardClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // User Icon and Info
            Column(
                modifier = Modifier
                    .weight(1f)

            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.TwoTone.Person,
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(10.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    Column {

                        Text(
                            text = user.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Text(
                            text = user.mobileNo,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 1
                        )
                    }

                }


                Text(
                    text = user.role,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!isDefaultUser) {
                    // Edit Button with text and background
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .clickable(
                                onClick = onEdit,
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.TwoTone.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Edit",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Delete Button with text and background
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                            .clickable(
                                onClick = onDelete,
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.TwoTone.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Delete",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditUserDialog(
    viewModel: UserManagementViewModel,
    userToEdit: UsersEntity?,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val isEditing = userToEdit != null
    val isDefaultUser = userToEdit?.let { viewModel.isDefaultUser(it) } ?: false
    val isAdmin = userToEdit?.role?.lowercase() == "admin"
    var isEditMode by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "User Details" else "Add New User",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Basic Information
                Text(
                    text = "Basic Information",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                CusOutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    state = viewModel.userName,
                    placeholderText = "Full Name *",
                    keyboardType = KeyboardType.Text,
                    readOnly = isEditing && !isEditMode
                )

                Spacer(modifier = Modifier.height(8.dp))

                CusOutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    state = viewModel.userMobile,
                    placeholderText = "Mobile Number *",
                    keyboardType = KeyboardType.Phone,
                    readOnly = isEditing && !isEditMode
                )

                Spacer(modifier = Modifier.height(8.dp))

                CusOutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    state = viewModel.userEmail,
                    placeholderText = "Email Address (Optional)",
                    keyboardType = KeyboardType.Email,
                    readOnly = isEditing && !isEditMode
                )

                Spacer(modifier = Modifier.height(8.dp))

                CusOutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    state = viewModel.userPin,
                    placeholderText = if (isEditing) "Enter new PIN (4-6 digits) *" else "PIN (4-6 digits) *",
                    keyboardType = KeyboardType.Number,
                    visualTransformation = PasswordVisualTransformation(),
                    readOnly = isEditing && !isEditMode
                )

                if (isEditing && !isEditMode) {
                    Text(
                        text = "Note: PIN will be updated when you save changes",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Role Selection (only for non-default users)
                if (!isDefaultUser) {
                    if (isEditing && !isEditMode) {
                        // Show role as read-only text
                        Text(
                            text = "Role: ${viewModel.userRole.text}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    } else {
                        RoleDropdown(
                            state = viewModel.userRole,
                            onRoleSelected = { }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Additional Information (only for non-default users)
                    Text(
                        text = "Additional Information",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    CusOutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        state = viewModel.userAadhaar,
                        placeholderText = "Aadhaar Number",
                        keyboardType = KeyboardType.Number,
                        readOnly = isEditing && !isEditMode
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    CusOutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        state = viewModel.userAddress,
                        placeholderText = "Address",
                        keyboardType = KeyboardType.Text,
                        readOnly = isEditing && !isEditMode
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.emergencyContactPerson,
                            placeholderText = "Emergency Contact Person",
                            keyboardType = KeyboardType.Text,
                            readOnly = isEditing && !isEditMode
                        )
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.emergencyContactNumber,
                            placeholderText = "Emergency Contact Number",
                            keyboardType = KeyboardType.Phone,
                            readOnly = isEditing && !isEditMode
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.governmentIdType,
                            placeholderText = "Government ID Type",
                            keyboardType = KeyboardType.Text,
                            readOnly = isEditing && !isEditMode
                        )
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.governmentIdNumber,
                            placeholderText = "Government ID Number",
                            keyboardType = KeyboardType.Text,
                            readOnly = isEditing && !isEditMode
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.dateOfBirth,
                            placeholderText = "Date of Birth",
                            keyboardType = KeyboardType.Text,
                            readOnly = isEditing && !isEditMode
                        )
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.bloodGroup,
                            placeholderText = "Blood Group",
                            keyboardType = KeyboardType.Text,
                            readOnly = isEditing && !isEditMode
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (isEditing && !isEditMode && !isAdmin) {
                // Show Edit button for non-admin users
                TextButton(
                    onClick = { isEditMode = true },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Edit")
                }
            } else {
                // Show Save/Update button
                TextButton(
                    onClick = onSave,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (isEditing) "Update" else "Add User")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(if (isEditMode) "Cancel" else "Close")
            }
        }
    )
} 