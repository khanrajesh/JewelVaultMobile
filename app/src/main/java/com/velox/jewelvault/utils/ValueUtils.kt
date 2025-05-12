package com.velox.jewelvault.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale


val mainScope = CoroutineScope(Dispatchers.Main)
fun mainScope(func: suspend () -> Unit) {
    CoroutineScope(Dispatchers.Main).launch { func() }
}

val ioScope = CoroutineScope(Dispatchers.IO)
fun ioScope(func: suspend () -> Unit) {
    CoroutineScope(Dispatchers.IO).launch { func() }
}

suspend fun <T> withIo(block: suspend () -> T): T {
    return withContext(Dispatchers.IO) {
        block()
    }
}





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

    inline fun <reified T> T.log(message: String) {
        val tag = T::class.java.simpleName
        android.util.Log.d(tag, message)
    }

    fun Double.roundTo3Decimal(): Double {
        return BigDecimal(this).setScale(3, RoundingMode.HALF_UP).toDouble()
    }