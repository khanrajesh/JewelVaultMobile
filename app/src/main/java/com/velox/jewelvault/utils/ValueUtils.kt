package com.velox.jewelvault.utils

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Timestamp
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

suspend fun <T> withMain(block: suspend () -> T): T {
    return withContext(Dispatchers.Main) {
        block()
    }
}

/**
 *   viewModelScope.launch {
 *         withContext(Dispatchers.IO) {
 *             block()
 *         }
 *     }
 * **/
fun ViewModel.ioLaunch(block: suspend () -> Unit) {
    viewModelScope.launch {
        withContext(Dispatchers.IO) {
            block()
        }
    }
}

fun getCurrentDate(): String {
    val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    return formatter.format(Date())
}

fun getCurrentTimestamp(): Timestamp {
    return Timestamp(System.currentTimeMillis())
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

fun LocalDateTime.toCustomFormatDate(): String {
    val formatter = DateTimeFormatter.ofPattern("dd-MM")
    return this.format(formatter)
}

inline fun <reified T> T.log(message: String) {
    val tag = T::class.java.simpleName
    Log.d(tag, message)
}

fun log(message: String, tag: String = "Log") {
    Log.d(tag, message)
}

fun Double.roundTo3Decimal(): Double {
    return BigDecimal(this).setScale(3, RoundingMode.HALF_UP).toDouble()
}

@SuppressLint("DefaultLocale")
fun Double.to3FString(): String{
   return try {
        String.format(Locale.US, "%.3f", this)
    }catch (_:Exception){
        "NULL"
    }
}

@SuppressLint("DefaultLocale")
fun Double.to2FString(): String{
    return try {
        String.format(Locale.US, "%.2f", this)
    }catch (_:Exception){
        "NULL"
    }
}

fun Double.to1FString() = String.format(Locale.US, "%.1f", this)

fun numberToWords(number: Int): String {
    if (number == 0) return "Zero"

    val units = arrayOf(
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
        "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen",
        "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    )
    val tens = arrayOf(
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    )

    fun convert(n: Int): String {
        return when {
            n < 20 -> units[n]
            n < 100 -> tens[n / 10] + if (n % 10 != 0) " " + units[n % 10] else ""
            n < 1000 -> units[n / 100] + " Hundred" + if (n % 100 != 0) " " + convert(n % 100) else ""
            n < 100000 -> convert(n / 1000) + " Thousand" + if (n % 1000 != 0) " " + convert(n % 1000) else ""
            n < 10000000 -> convert(n / 100000) + " Lakh" + if (n % 100000 != 0) " " + convert(n % 100000) else ""
            else -> convert(n / 10000000) + " Crore" + if (n % 10000000 != 0) " " + convert(n % 10000000) else ""
        }
    }

    return convert(number)
}

//fun convertNumberToWords(number: Int): String {
//    if (number == 0) return "Zero"
//
//    val ones = arrayOf("", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
//        "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
//        "Seventeen", "Eighteen", "Nineteen")
//    val tens = arrayOf("", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety")
//
//    fun convertHundreds(n: Int): String {
//        var result = ""
//        if (n >= 100) {
//            result += ones[n / 100] + " Hundred "
//        }
//        val remainder = n % 100
//        if (remainder >= 20) {
//            result += tens[remainder / 10] + " "
//            if (remainder % 10 != 0) {
//                result += ones[remainder % 10] + " "
//            }
//        } else if (remainder > 0) {
//            result += ones[remainder] + " "
//        }
//        return result.trim()
//    }
//
//    var result = ""
//    var num = number
//
//    if (num >= 10000000) { // Crores
//        result += convertHundreds(num / 10000000) + " Crore "
//        num %= 10000000
//    }
//    if (num >= 100000) { // Lakhs
//        result += convertHundreds(num / 100000) + " Lakh "
//        num %= 100000
//    }
//    if (num >= 1000) { // Thousands
//        result += convertHundreds(num / 1000) + " Thousand "
//        num %= 1000
//    }
//    if (num > 0) {
//        result += convertHundreds(num)
//    }
//
//    return result.trim()
//}

fun String.canBeInt(): Boolean {
    return this.toIntOrNull() != null
}

/**
 * Format a Double value as currency string with Indian Rupee symbol
 * Uses the same pattern as existing codebase (₹ + to2FString)
 * @param amount The amount to format
 * @return Formatted currency string (e.g., "₹1,234.56")
 */
fun formatCurrency(amount: Double): String {
    return "₹${amount.to3FString()}"
}

/**
 * Format an Int value as currency string with Indian Rupee symbol
 * @param amount The amount to format
 * @return Formatted currency string (e.g., "₹1,234.00")
 */
fun formatCurrency(amount: Int): String {
    return formatCurrency(amount.toDouble())
}

/**
 * Format a Long value as currency string with Indian Rupee symbol
 * @param amount The amount to format
 * @return Formatted currency string (e.g., "₹1,234.00")
 */
fun formatCurrency(amount: Long): String {
    return formatCurrency(amount.toDouble())
}

/**
 * Format a Timestamp as a readable date string
 * Uses the same pattern as existing codebase (dd-MMM-yyyy)
 * @param timestamp The timestamp to format
 * @return Formatted date string (e.g., "25-Dec-2024")
 */
fun formatDate(timestamp: java.sql.Timestamp): String {
    val formatter = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
    return formatter.format(timestamp)
}

/**
 * Format a Date as a readable date string
 * Uses the same pattern as existing codebase (dd-MMM-yyyy)
 * @param date The date to format
 * @return Formatted date string (e.g., "25-Dec-2024")
 */
fun formatDate(date: java.util.Date): String {
    val formatter = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
    return formatter.format(date)
}

/**
 * Format a Long timestamp as a readable date string
 * @param timestamp The timestamp in milliseconds
 * @return Formatted date string (e.g., "25-Dec-2024")
 */
fun formatDate(timestamp: Long): String {
    return formatDate(java.util.Date(timestamp))
}
