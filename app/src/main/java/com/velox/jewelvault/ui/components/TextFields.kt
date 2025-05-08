package com.velox.jewelvault.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class InputFieldState(
    initValue: String = "",
    textState: MutableState<String>? = null
) {
    private val _text = textState ?: mutableStateOf(initValue)
    var text: String
        get() = _text.value
        set(value) {
            _text.value = value
        }

    var error by mutableStateOf("")

    fun onTextChanged(newText: String) {
        text = newText
        if (error.isNotEmpty()) {
            error = ""
        }
    }

    fun asState(): State<String> = _text
}

@Composable
fun CusOutlinedTextField(
    state: InputFieldState,
    modifier: Modifier = Modifier,
    placeholderText: String,
    dropdownItems: List<String> = emptyList(),
    onDropdownItemSelected: ((String) -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    leadingIcon: ImageVector? = null,
    onLeadingIconClick: (() -> Unit)? = null,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = OutlinedTextFieldDefaults.shape,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors()
) {

    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clickable(
                enabled = dropdownItems.isNotEmpty(),
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                if (dropdownItems.isNotEmpty()) {
                    expanded = !expanded
                }
            }
    ) {
        Column(Modifier.fillMaxWidth()) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth()
                    .onFocusChanged {
                        if (dropdownItems.isNotEmpty()) {
                            expanded = it.isFocused
                        }
                    },
                value = state.text,
                onValueChange = {
                    state.onTextChanged(it)
                },
                maxLines = maxLines,
                placeholder = { Text("Enter $placeholderText") },
                label = { Text(placeholderText) },
                isError = state.error.isNotEmpty(),
                enabled = enabled,
                readOnly = readOnly || dropdownItems.isNotEmpty(),
                prefix = prefix,
                suffix = suffix,
                keyboardActions = keyboardActions,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                leadingIcon = leadingIcon?.let {
                    {
                        if (onLeadingIconClick != null) {
                            Icon(
                                imageVector = it,
                                contentDescription = "Leading Icon",
                                modifier = Modifier
                                    .clickable { onLeadingIconClick() }
                                    .padding(8.dp)
                            )
                        } else {
                            Icon(imageVector = it, contentDescription = "Leading Icon")
                        }
                    }
                },
                trailingIcon = {
                    when {
                        dropdownItems.isNotEmpty() -> {
                            Icon(
                                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (expanded) "Collapse dropdown" else "Expand dropdown",
                                modifier = Modifier
                                    .clickable { expanded = !expanded }
                                    .padding(8.dp)
                            )
                        }

                        trailingIcon != null -> {
                            if (onTrailingIconClick != null) {
                                Icon(
                                    imageVector = trailingIcon,
                                    contentDescription = "Trailing Icon",
                                    modifier = Modifier
                                        .clickable { onTrailingIconClick() }
                                        .padding(8.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = trailingIcon,
                                    contentDescription = "Trailing Icon"
                                )
                            }
                        }

                        else -> null
                    }
                },
                visualTransformation = visualTransformation,
                interactionSource = interactionSource,
                textStyle = textStyle,
                shape = shape,
                colors = colors
            )

            // Dropdown suggestions (full list, no "Select" placeholder)
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .width(200.dp)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                dropdownItems
                    .filter { it.isNotBlank() }
                    .forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item) },
                            onClick = {
                                state.onTextChanged(item)
                                onDropdownItemSelected?.invoke(item)
                                expanded = false
                            }
                        )
                    }
            }

            // Error text
            if (state.error.isNotEmpty()) {
                Text(
                    text = state.error,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                )
            }
        }
    }
}
