package com.velox.jewelvault.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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

    fun clear() {
        text = ""
    }

    fun asState(): State<String> = _text
}

@Composable
fun CusOutlinedTextField(
    state: InputFieldState,
    modifier: Modifier = Modifier,
    onTextChange:(String)->Unit={},
    placeholderText: String,
    dropdownItems: List<String> = emptyList(),
    onDropdownItemSelected: ((String) -> Unit)? = null,
    leadingIcon: ImageVector? = null,
    onLeadingIconClick: (() -> Unit)? = null,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false ,
    textStyle: TextStyle = LocalTextStyle.current,
    prefix:String = "",
    suffix:String = "",
    supportingText: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = OutlinedTextFieldDefaults.shape,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    validation: ((String) -> String?)? = null,
    isDatePicker: Boolean = false,
    initialDate: LocalDate = LocalDate.now(),
    onDateSelected: ((LocalDate) -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }


    fun showDatePicker() {
        android.app.DatePickerDialog(
            context,
            { _, year, month, day ->
                val pickedDate = LocalDate.of(year, month + 1, day)
                state.onTextChanged(pickedDate.format(formatter))
                onDateSelected?.invoke(pickedDate)
            },
            initialDate.year,
            initialDate.monthValue - 1,
            initialDate.dayOfMonth
        ).show()

    }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged {
                        if (dropdownItems.isNotEmpty()) {
                            expanded = it.isFocused
                        }
                    },
                value = state.text,
                onValueChange = {
                    if (keyboardType == KeyboardType.Number) {

                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*\$"))) {
                            state.onTextChanged(it)
                            validation?.let { validate ->
                                val errorMsg = validate(it)
                                state.error = errorMsg ?: ""
                                if (errorMsg != null) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        } else {
                            state.error = "Only numbers and one decimal point allowed"
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    } else {

                        state.onTextChanged(it)
                        validation?.let { validate ->
                            val errorMsg = validate(it)
                            state.error = errorMsg ?: ""
                            if (errorMsg != null) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }
                    }

                    onTextChange(it)
                },
                maxLines = maxLines,
                placeholder = { Text("Enter $placeholderText") },
                label = { Text(placeholderText) },
                isError = state.error.isNotEmpty(),
                enabled = enabled,
                readOnly = readOnly || dropdownItems.isNotEmpty() || isDatePicker,
                keyboardActions = keyboardActions,
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = imeAction
                ),
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
                        isDatePicker -> {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Pick Date",
                                modifier = Modifier
                                    .clickable { showDatePicker() }
                                    .padding(8.dp)
                            )
                        }
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
                                validation?.let { validate ->
                                    val errorMsg = validate(item)
                                    state.error = errorMsg ?: ""
                                    if (errorMsg != null) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                }
                                onDropdownItemSelected?.invoke(item)
                                expanded = false
                            }
                        )
                    }
            }

            // Error text
            if (state.error.isNotEmpty()) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
