package com.velox.jewelvault.data

import android.content.Context
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.toCustomFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime


@Composable
fun MetalRatesTicker(modifier: Modifier = Modifier) {
    val showEditDialog = remember { mutableStateOf(false) }
    val baseViewModel = LocalBaseViewModel.current
    val infiniteTransition = rememberInfiniteTransition()
    val animatedOffsetX by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = -1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // Combine all metal rates into a single string
    val tickerText = baseViewModel.metalRates
        .groupBy { it.source }
        .map { (source, rates) ->
            val dateTime =
                rates.first().updatedDate // assuming all rates for a source have the same date
            val sourceText = "$source (Fetched on $dateTime) :"
            val ratesText = rates.joinToString(separator = "  -•-  ") { rate ->
                "${rate.metal} ${rate.caratOrPurity}: ₹${rate.price}"
            }
            "$sourceText $ratesText"
        }
        .joinToString(separator = "   •   ")

    Row(
        modifier = modifier
            .padding(horizontal = 10.dp)
            .height(50.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        showEditDialog.value = true
                    }
                )
            }
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        if (baseViewModel.metalRatesLoading.value) {
            CircularProgressIndicator(Modifier.size(20.dp), color = Color.Black)
            Spacer(Modifier.width(10.dp))
        }

        Text(
            text = tickerText,
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            maxLines = 1,
            modifier = Modifier
                .offset(x = animatedOffsetX.dp * 1000) // smooth move right to left
        )
    }

    if (showEditDialog.value && baseViewModel.metalRates.isNotEmpty()) {
        EditMetalRatesDialog(showEditDialog)
    } else {
        showEditDialog.value = false
    }
}

// Composable for Edit Metal Rates
@Composable
fun EditMetalRatesDialog(
    showDialog: MutableState<Boolean>
) {
    val viewModel = LocalBaseViewModel.current
    val editedRates = remember { mutableStateListOf(*viewModel.metalRates.toTypedArray()) }

    AlertDialog(
        onDismissRequest = { /* Prevent dismissing by tapping outside */ },
        title = { Text("Edit Metal Rates") },
        text = {
            LazyColumn {
                itemsIndexed(editedRates) { index, metalRate ->
                    Row {
                        Text(
                            text = "${metalRate.metal}, ${metalRate.caratOrPurity}",
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(5.dp))
                        TextField(
                            modifier = Modifier.weight(1f),
                            value = metalRate.price,
                            onValueChange = { editedRates[index] = metalRate.copy(price = it) },
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                editedRates.forEachIndexed { index, metalRate ->
                    editedRates[index] =
                        metalRate.copy(updatedDate = LocalDateTime.now().toCustomFormat())
                }
                viewModel.metalRates.clear()
                viewModel.metalRates.addAll(editedRates)
                showDialog.value = false
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = { showDialog.value = false }) {
                Text("Cancel")
            }
        }
    )
}

data class MetalRate(
    val source: String,      // Example: "GoodReturns", "KDBullion"
    val metal: String,       // "Gold" or "Silver"
    val caratOrPurity: String, // Example: "22K", "24K", "999", "Coin 10gm" etc.
    val price: String,       // Example: "₹9025"
    val updatedDate: String  // Example: "2025-04-27" (today's date)
)

suspend fun fetchAllMetalRates(
    state: String,
    context: Context,
    metalRatesLoading: MutableState<Boolean>
): List<MetalRate> = withContext(Dispatchers.IO) {
    val combinedRates = mutableListOf<MetalRate>()
    metalRatesLoading.value = true
    // Fetch gold rates with separate error handling
    try {
        val goldRates = fetchGoldPricesGoodReturns(state, context)
        combinedRates.addAll(goldRates)
    } catch (e: Exception) {
        // Handle error specifically for gold rates, add an error item
        combinedRates.add(
            MetalRate(
                source = "GoldFetcher",
                metal = "Gold",
                caratOrPurity = "Error",
                price = e.localizedMessage ?: "Unknown error",
                updatedDate = LocalDateTime.now().toCustomFormat()
            )
        )
    }

    // Fetch silver rates with separate error handling
    try {
        val silverRates = fetchSilverPricesGoodReturns(state, context)
        combinedRates.addAll(silverRates)
    } catch (e: Exception) {
        // Handle error specifically for silver rates, add an error item
        combinedRates.add(
            MetalRate(
                source = "SilverFetcher",
                metal = "Silver",
                caratOrPurity = "Error",
                price = e.localizedMessage ?: "Unknown error",
                updatedDate = LocalDateTime.now().toCustomFormat()
            )
        )
    }

    // You can also add separate error handling for the KDBullion API (if needed)
    try {
        // val kdbRates = fetchPricesKDBullion(context)
        // combinedRates.addAll(kdbRates)
    } catch (e: Exception) {
        // Handle error for KDBullion rates
        combinedRates.add(
            MetalRate(
                source = "KDBullionFetcher",
                metal = "KDBullion",
                caratOrPurity = "Error",
                price = e.localizedMessage ?: "Unknown error",
                updatedDate = LocalDateTime.now().toCustomFormat()
            )
        )
    }
    metalRatesLoading.value = false

    // Return the combined list of rates, even if some fetches failed
    return@withContext combinedRates
}


suspend fun fetchGoldPricesGoodReturns(state: String, context: Context): List<MetalRate> =
    withContext(
        Dispatchers.IO
    ) {
        val formattedState = state.lowercase().replace(" ", "-")
        val url = "https://www.goodreturns.in/gold-rates/$formattedState.html"

        try {
            val doc = Jsoup.connect(url).userAgent("Mozilla/5.0").get()
            val prices = mutableListOf<MetalRate>()
            val containers = doc.select(".gold-each-container")
            val todayDate = LocalDateTime.now().toCustomFormat()

            for (container in containers) {
                val typeElement = container.selectFirst(".gold-top .gold-common-head")
                val priceElement = container.selectFirst(".gold-bottom .gold-common-head")

                if (typeElement != null && priceElement != null) {
                    val typeText = typeElement.text()
                    val priceText = priceElement.text()

                    val karat = when {
                        typeText.contains("24K", ignoreCase = true) -> "24K"
                        typeText.contains("22K", ignoreCase = true) -> "22K"
                        typeText.contains("18K", ignoreCase = true) -> "18K"
                        else -> "Unknown"
                    }

                    if (karat != "Unknown") {
                        prices.add(
                            MetalRate(
                                source = "GoodReturns",
                                metal = "Gold",
                                caratOrPurity = karat,
                                price = priceText.replace("₹", "").replace(",", ""),
                                updatedDate = todayDate
                            )
                        )
                    }
                }
            }

            if (prices.isEmpty()) listOf(
                MetalRate(
                    "GoodReturns",
                    "Gold",
                    "Error",
                    "No data",
                    todayDate
                )
            )
            else prices

        } catch (e: Exception) {
            listOf(
                MetalRate(
                    "GoodReturns",
                    "Gold",
                    "Error",
                    e.localizedMessage ?: "Unknown error",
                    LocalDateTime.now().toCustomFormat()
                )
            )
        }
    }

suspend fun fetchSilverPricesGoodReturns(state: String, context: Context): List<MetalRate> =
    withContext(Dispatchers.IO) {
        val formattedState = state.lowercase().replace(" ", "-")
        val url = "https://www.goodreturns.in/silver-rates/$formattedState.html"

        try {
            val doc = Jsoup.connect(url).userAgent("Mozilla/5.0").get()
            val prices = mutableListOf<MetalRate>()
            val containers = doc.select(".gold-each-container")
            val todayDate = LocalDateTime.now().toCustomFormat()

            for (container in containers) {
                val typeElement = container.selectFirst(".gold-top .gold-common-head")
                val priceElement = container.selectFirst(".gold-bottom .gold-common-head")

                if (typeElement != null && priceElement != null) {
                    val typeText = typeElement.text()
                    val priceText = priceElement.text()

                    prices.add(
                        MetalRate(
                            source = "GoodReturns",
                            metal = "Silver",
                            caratOrPurity = typeText,
                            price = priceText.replace("₹", "").replace(",", ""),
                            updatedDate = todayDate
                        )
                    )
                }
            }

            if (prices.isEmpty()) listOf(
                MetalRate(
                    "GoodReturns",
                    "Silver",
                    "Error",
                    "No data",
                    todayDate
                )
            )
            else prices

        } catch (e: Exception) {
            listOf(
                MetalRate(
                    "GoodReturns",
                    "Silver",
                    "Error",
                    e.localizedMessage ?: "Unknown error",
                    LocalDateTime.now().toCustomFormat()
                )
            )
        }
    }

suspend fun fetchPricesKDBullion(context: Context): List<MetalRate> = withContext(Dispatchers.IO) {
    val url = "http://kdbullion.in/"

    try {
        Log.d("KDBullionFetcher", "Connecting to $url...")
        val doc = Jsoup.connect(url).userAgent("Mozilla/5.0").get()
        Log.d("KDBullionFetcher", "Fetched document.")

        // Debug: Save or print the whole HTML
        Log.d("KDBullionFetcher", "Document HTML: ${doc.outerHtml()}")

        saveToRootStorage(context, doc, "KdBullion")
        val prices = mutableListOf<MetalRate>()
        val rows = doc.select("#divProduct tr.product-cover-number")
        Log.d("KDBullionFetcher", "Found ${rows.size} rows.")

        val todayDate = LocalDateTime.now().toCustomFormat()

        for (row in rows) {
            val columns = row.select("td")
            Log.d("KDBullionFetcher", "Row has ${columns.size} columns.")

            if (columns.size >= 3) {
                val productName = columns[0].text().trim()
                val sellPriceElement = columns[2].selectFirst("span")
                val sellPrice = sellPriceElement?.text()?.trim() ?: ""

                Log.d("KDBullionFetcher", "Product: $productName, Sell Price: $sellPrice")

                if (sellPrice.isNotEmpty()) {
                    val metalType = when {
                        productName.contains("GOLD", ignoreCase = true) -> "Gold"
                        productName.contains("SILVER", ignoreCase = true) -> "Silver"
                        else -> "Unknown"
                    }

                    if (metalType != "Unknown") {
                        prices.add(
                            MetalRate(
                                source = "KDBullion",
                                metal = metalType,
                                caratOrPurity = productName,
                                price = sellPrice,
                                updatedDate = todayDate
                            )
                        )
                    }
                }
            }
        }

        if (prices.isEmpty()) {
            Log.d("KDBullionFetcher", "No valid prices found. Returning error MetalRate.")
            listOf(MetalRate("KDBullion", "Unknown", "Error", "No data", todayDate))
        } else {
            Log.d("KDBullionFetcher", "Fetched prices: $prices")
            prices
        }

    } catch (e: Exception) {
        Log.e("KDBullionFetcher", "Error fetching prices", e)
        listOf(
            MetalRate(
                "KDBullion",
                "Unknown",
                "Error",
                e.localizedMessage ?: "Unknown error",
                LocalDateTime.now().toCustomFormat()
            )
        )
    }
}


suspend fun registerAndFetchRates(): String = withContext(Dispatchers.IO) {
    val client = OkHttpClient()

    // Step 1: Register
    val registerUrl = "http://kdbullion.in/WebService/WebService.asmx/InsertOtr"
    val registerJson = JSONObject().apply {
        put("Name", "Rajesh")
        put("FirmName", "")
        put("City", "")
        put("ContactNo", "82606636334")
        put("ClientId", 4)
    }

    val registerBody = JSONObject().apply {
        put("ClientDetails", JSONArray().apply {
            put(registerJson)
        })
    }

    val registerRequestBody =
        registerBody.toString().toRequestBody("application/json".toMediaTypeOrNull())

    val registerRequest = Request.Builder()
        .url(registerUrl)
        .post(registerRequestBody)
        .build()

    try {
        Log.d("RegisterAndFetch", "Sending registration request to $registerUrl")
        val registerResponse = client.newCall(registerRequest).execute()
        if (!registerResponse.isSuccessful) {
            val errorResponse = registerResponse.body?.string() ?: "Unknown error"
            Log.e(
                "RegisterAndFetch",
                "Registration failed: ${registerResponse.code}, $errorResponse"
            )
            return@withContext "Registration failed: ${registerResponse.code}, $errorResponse"
        }
        Log.d(
            "RegisterAndFetch",
            "Registration successful, response code: ${registerResponse.code}"
        )
    } catch (e: Exception) {
        Log.e("RegisterAndFetch", "Error during registration: ${e.localizedMessage}")
        return@withContext "Error during registration: ${e.localizedMessage}"
    }

    // Step 2: Fetch rates (This can stay the same as the previous example)
    val fetchUrl = "http://kdbullion.in/WebService/WebService.asmx/GetRateByClient"
    val fetchRequest = Request.Builder()
        .url(fetchUrl)
        .post("".toRequestBody("application/json".toMediaTypeOrNull()))
        .build()

    try {
        Log.d("RegisterAndFetch", "Sending request to fetch rates from $fetchUrl")
        val fetchResponse = client.newCall(fetchRequest).execute()
        if (!fetchResponse.isSuccessful) {
            val errorResponse = fetchResponse.body?.string() ?: "Unknown error"
            Log.e(
                "RegisterAndFetch",
                "Fetching rates failed: ${fetchResponse.code}, $errorResponse"
            )
            return@withContext "Fetching rates failed: ${fetchResponse.code}, $errorResponse"
        }

        val responseBody = fetchResponse.body?.string() ?: "No data"
        Log.d("RegisterAndFetch", "Fetched rates successfully: $responseBody")

        // Parse the response (assuming it's a JSON string)
        try {
            val jsonResponse = JSONObject(responseBody)
            // Assuming jsonResponse contains a field "rates" with rate data
            val rates = jsonResponse.optJSONArray("rates")?.let { array ->
                val rateList = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    rateList.add(array.getString(i))
                }
                rateList.joinToString(", ")
            } ?: "No rates available"

            Log.d("RegisterAndFetch", "Parsed rates: $rates")
            return@withContext rates
        } catch (jsonException: Exception) {
            Log.e(
                "RegisterAndFetch",
                "Error parsing rates response: ${jsonException.localizedMessage}"
            )
            return@withContext "Error parsing rates response: ${jsonException.localizedMessage}"
        }
    } catch (e: Exception) {
        Log.e("RegisterAndFetch", "Error during fetching rates: ${e.localizedMessage}")
        return@withContext "Error during fetching rates: ${e.localizedMessage}"
    }
}


suspend fun fetchPricesKDBullionrr(context: Context): List<MetalRate> =
    withContext(Dispatchers.IO) {
        val url = "http://kdbullion.in/"

        try {
            val doc = Jsoup.connect(url).userAgent("Mozilla/5.0").get()
            val prices = mutableListOf<MetalRate>()
            val rows = doc.select("#divProduct tr.product-cover-number")
            val todayDate = LocalDateTime.now().toCustomFormat()

            for (row in rows) {
                val columns = row.select("td")
                if (columns.size >= 3) {
                    val productName = columns[0].text().trim()
                    val sellPriceElement = columns[2].selectFirst("span")
                    val sellPrice = sellPriceElement?.text()?.trim() ?: ""

                    if (sellPrice.isNotEmpty()) {
                        val metalType = when {
                            productName.contains("GOLD", ignoreCase = true) -> "Gold"
                            productName.contains("SILVER", ignoreCase = true) -> "Silver"
                            else -> "Unknown"
                        }

                        if (metalType != "Unknown") {
                            prices.add(
                                MetalRate(
                                    source = "KDBullion",
                                    metal = metalType,
                                    caratOrPurity = productName,
                                    price = sellPrice,
                                    updatedDate = todayDate
                                )
                            )
                        }
                    }
                }
            }

            if (prices.isEmpty()) listOf(
                MetalRate(
                    "KDBullion",
                    "Unknown",
                    "Error",
                    "No data",
                    todayDate
                )
            )
            else prices

        } catch (e: Exception) {
            listOf(
                MetalRate(
                    "KDBullion",
                    "Unknown",
                    "Error",
                    e.localizedMessage ?: "Unknown error",
                    LocalDateTime.now().toCustomFormat()
                )
            )
        }
    }

fun saveToRootStorage(context: Context, doc: org.jsoup.nodes.Document, fileName: String) {
    try {
        val rootPath = Environment.getExternalStorageDirectory()
        val folder = File(rootPath, "JewelVault")

        if (!folder.exists()) {
            folder.mkdirs()
        }

        val file = File(folder, fileName)

        FileOutputStream(file).use { outputStream ->
            val htmlContent = doc.html()
            outputStream.write(htmlContent.toByteArray())
            Log.d("GoldPriceFetcher", "File saved at: ${file.absolutePath}")
        }

        // ✅ Show toast on main thread
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, "File saved at:\n${file.absolutePath}", Toast.LENGTH_LONG)
                .show()
        }

    } catch (e: IOException) {
        Log.e("GoldPriceFetcher", "Error saving document to file", e)
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, "Failed to save file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}