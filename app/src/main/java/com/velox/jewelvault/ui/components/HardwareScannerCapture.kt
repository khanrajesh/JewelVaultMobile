package com.velox.jewelvault.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.velox.jewelvault.utils.normalizeScannedCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ScanInputMode(val label: String) {
    CAMERA("Camera"),
    HARDWARE("Hardware"),
    AUTO("Auto")
}

@Composable
fun ScanInputModeSelector(
    mode: ScanInputMode,
    onModeChange: (ScanInputMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ScanInputMode.entries.forEach { option ->
            FilterChip(
                selected = mode == option,
                onClick = { onModeChange(option) },
                label = { Text(option.label) }
            )
        }
    }
}

@Composable
fun HardwareScannerCapture(
    enabled: Boolean,
    onScanned: (String) -> Unit,
    modifier: Modifier = Modifier,
    duplicateWindowMs: Long = 450L
) {
    var buffer by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    var lastScan by remember { mutableStateOf<Pair<String, Long>?>(null) }

    fun emit(raw: String) {
        val normalized = normalizeScannedCode(raw)
        if (normalized.isBlank()) return
        val now = System.currentTimeMillis()
        val previous = lastScan
        if (previous != null && previous.first == normalized && now - previous.second <= duplicateWindowMs) {
            return
        }
        lastScan = normalized to now
        onScanned(normalized)
    }

    fun flushBuffer() {
        if (buffer.isNotBlank()) {
            emit(buffer)
            buffer = ""
        }
    }

    LaunchedEffect(enabled) {
        if (enabled) {
            delay(120)
            runCatching { focusRequester.requestFocus() }
            keyboardController?.hide()
        } else {
            buffer = ""
        }
    }

    BasicTextField(
        value = buffer,
        onValueChange = { incoming ->
            if (!enabled) return@BasicTextField
            val hasTerminator = incoming.contains('\n') || incoming.contains('\r')
            if (!hasTerminator) {
                buffer = incoming.take(256)
                return@BasicTextField
            }

            val tokens = incoming.split('\n', '\r')
            tokens.dropLast(1).forEach { token ->
                if (token.isNotBlank()) emit(token)
            }
            buffer = tokens.lastOrNull().orEmpty().take(256)
        },
        modifier = modifier
            .size(1.dp)
            .alpha(0f)
            .focusRequester(focusRequester)
            .onFocusChanged { state ->
                if (enabled && !state.isFocused) {
                    scope.launch {
                        delay(80)
                        runCatching { focusRequester.requestFocus() }
                        keyboardController?.hide()
                    }
                }
            },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Ascii,
            imeAction = ImeAction.Done
        ),
        singleLine = true
    )

    LaunchedEffect(buffer, enabled) {
        if (!enabled || buffer.isBlank()) return@LaunchedEffect
        delay(250)
        if (enabled && buffer.isNotBlank()) {
            flushBuffer()
        }
    }
}
