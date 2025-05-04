package com.velox.jewelvault.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


import androidx.compose.runtime.*

class InputFieldState(
    initValue: String = "",
    textState: MutableState<String>? = null
) {
    // Use external MutableState if provided; else use internal state
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
    keyboardType: KeyboardType = KeyboardType.Text,
    leadingIcon: ImageVector? = null,
    onLeadingIconClick: (() -> Unit)? = null,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    prefix: @Composable() (() -> Unit)? = null,
    suffix: @Composable() (() -> Unit)? = null,
    supportingText: @Composable() (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int. MAX_VALUE,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = OutlinedTextFieldDefaults.shape,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors()
) {

    Column(modifier) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.text,
            maxLines = maxLines,
            onValueChange =state::onTextChanged,
            placeholder = { Text("Enter $placeholderText") },
            label = { Text(placeholderText) },
            isError = state.error.isNotEmpty(),
            enabled = enabled,
            readOnly = readOnly,
            prefix=prefix,
            keyboardActions = keyboardActions,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType
            ),
            leadingIcon = if (leadingIcon != null) {
                {
                    if (onLeadingIconClick != null) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = "Leading Icon",
                            modifier = Modifier.bounceClick { onLeadingIconClick() }
                        )
                    } else {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = "Leading Icon"
                        )
                    }
                }
            } else null,
            trailingIcon = if (trailingIcon != null) {
                {
                    if (onTrailingIconClick != null) {
                        Icon(
                            imageVector = trailingIcon,
                            contentDescription = "Trailing Icon",
                            modifier = Modifier.bounceClick { onTrailingIconClick() }
                        )
                    } else {
                        Icon(
                            imageVector = trailingIcon,
                            contentDescription = "Trailing Icon"
                        )
                    }
                }
            } else null,

        )


        if (state.error.isNotEmpty()) {
            Text(
                text = state.error,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

