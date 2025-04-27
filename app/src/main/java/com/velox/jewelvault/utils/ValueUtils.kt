package com.velox.jewelvault.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale


val mainScope = CoroutineScope(Dispatchers.Main)
val ioScope = CoroutineScope(Dispatchers.IO)

@Composable
fun rememberCurrentDateTime(): State<String> {
    val dateTimeState = remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val formatter = SimpleDateFormat("dd-MM-yyyy hh:mm:ss a", Locale.getDefault())
            dateTimeState.value = formatter.format(Date())
            delay(1000L)
        }
    }

    return dateTimeState
}

fun LocalDateTime.toCustomFormat(): String {
    val formatter = DateTimeFormatter.ofPattern("dd-MM hh:mm a")
    return this.format(formatter)
}