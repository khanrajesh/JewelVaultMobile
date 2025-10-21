package com.velox.jewelvault.data

import android.annotation.SuppressLint
import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.velox.jewelvault.ui.components.CalculatorScreen
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.toCustomFormat
import com.velox.jewelvault.utils.ioScope
import com.velox.jewelvault.utils.toCustomFormatDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime


@Composable
fun MetalRatesTicker(
    modifier: Modifier = Modifier,
    textColor: Color = Color.Black,
    backgroundColor: Color = Color.Transparent,
) {
    val showEditDialog = remember { mutableStateOf(false) }
    val baseViewModel = LocalBaseViewModel.current
    val infiniteTransition = rememberInfiniteTransition()
    val animatedOffsetX by infiniteTransition.animateFloat(
        initialValue = 1.5f, targetValue = -1.5f, animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // Combine all metal rates into a single string
    val tickerText = baseViewModel.metalRates.groupBy { it.source }.map { (_, rates) ->
            val dateTime =
                rates.first().updatedDate // assuming all rates for a source have the same date
//            val sourceText = "$source (Fetched on $dateTime) :"
            val sourceText = "(Fetched on $dateTime) :"
            val ratesText = rates.joinToString(separator = "  -•-  ") { rate ->
                "${rate.metal} ${rate.caratOrPurity}: ₹${rate.price}"
            }
            "$sourceText $ratesText"
        }.joinToString(separator = "   •   ")

    Box(
        modifier = modifier
            .padding(horizontal = 10.dp)
            .height(50.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        showEditDialog.value = true
                    })
            }) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            if (baseViewModel.metalRatesLoading.value) {
                CircularProgressIndicator(Modifier.size(20.dp), color = Color.Black)
                Spacer(Modifier.width(10.dp))
            }

            Text(
                text = tickerText,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                modifier = Modifier.offset(x = animatedOffsetX.dp * 1000) // smooth move right to left
            )
        }

        // Left fade gradient (start of scroll)
        if (backgroundColor != Color.Transparent) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(40.dp)
                    .height(20.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                backgroundColor,
                                backgroundColor.copy(alpha = 0.8f),
                                backgroundColor.copy(alpha = 0.0f)
                            )
                        )
                    )
            )
            // Right fade gradient (end of scroll)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(40.dp)
                    .height(20.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                backgroundColor.copy(alpha = 0.0f),
                                backgroundColor.copy(alpha = 0.8f),
                                backgroundColor
                            )
                        )
                    )
            )

        }

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
    LocalContext.current
    val viewModel = LocalBaseViewModel.current
    val editedRates = remember { mutableStateListOf(*viewModel.metalRates.toTypedArray()) }

    Dialog(
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        ),
        onDismissRequest = {},
        content = {
            Column(
                Modifier
                    .padding(vertical = 150.dp, horizontal = 200.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .fillMaxSize()
                    .padding(10.dp)
            ) {
                Text("Edit Metal Rates", fontWeight = FontWeight.Black)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.weight(1f)) {
                    CalculatorScreen(
                        Modifier
                            .weight(0.6f)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)
                            )
                            .padding(10.dp)
                    )
                    Spacer(Modifier.width(10.dp))



                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                        itemsIndexed(editedRates) { index, metalRate ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${metalRate.metal}, ${metalRate.caratOrPurity}",
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(5.dp))
                                TextField(
                                    modifier = Modifier.weight(2f),
                                    value = metalRate.price,
                                    singleLine = true,
                                    onValueChange = {
                                        try {
                                            if (metalRate.metal == "Gold" && metalRate.caratOrPurity == "24K") {
                                                updateConjugateMetalPrice(
                                                    editedRates,
                                                    it.toDouble()
                                                )
                                            } else if (metalRate.metal != "Gold") {
                                                editedRates[index] = metalRate.copy(price = it)
                                            } else {
                                                viewModel.snackBarState =
                                                    "Currently you can only edit 24K Gold and Silver"
                                            }
                                        } catch (e: Exception) {
                                            viewModel.snackBarState = "Invalid input"
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                        }
                    }


                }

                Row(Modifier.fillMaxWidth()) {
                    Spacer(Modifier.weight(1f))

                    TextButton(onClick = { showDialog.value = false }) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(10.dp))
                    TextButton(onClick = {
                        editedRates.forEachIndexed { index, metalRate ->
                            editedRates[index] =
                                metalRate.copy(updatedDate = LocalDateTime.now().toCustomFormat())
                        }
                        viewModel.metalRates.clear()
                        viewModel.metalRates.addAll(editedRates)
                        // Persist 3 fields when user updates from dialog:
                        // 1) GOLD 24K (per gram) as Double
                        // 2) SILVER 1 Kg as Double (compute from 1 g if needed)
                        // 3) METAL_FETCH_DATE as yyyy-MM-dd
                        ioScope {
                            try {
                                val todayIso = java.time.LocalDate.now().toString()
                                val gold24k = editedRates.firstOrNull { it.metal == "Gold" && it.caratOrPurity.equals("24K", true) }?.price?.replace(",", "")?.toDoubleOrNull()

                                // Compute silver 1 Kg from edited rates if provided; do not overwrite if user didn't change
                                val computedSilverKg = computeSilverKgFromRates(editedRates)

                                if (gold24k != null) {
                                    viewModel.dataStoreManager.setValue(DataStoreManager.METAL_GOLD_24K, gold24k)
                                }
                                if (computedSilverKg != null) {
                                    viewModel.dataStoreManager.setValue(DataStoreManager.METAL_SILVER_KG, computedSilverKg)
                                }
                                viewModel.dataStoreManager.setValue(DataStoreManager.METAL_FETCH_DATE, todayIso)
                            } catch (_: Exception) { }
                        }
                        showDialog.value = false

                    }) {
                        Text("Save")
                    }

                }

            }
        },

        )
}

@SuppressLint("DefaultLocale")
fun updateConjugateMetalPrice(
    editedRates: SnapshotStateList<MetalRate>, value24k: Double
) {
    val gold100 = (100 / 99.9) * value24k
//  val gold100 = value24k
    val updatedList = editedRates.map { metalRate ->
        if (metalRate.metal == "Gold") {
            val updatedPrice = when (metalRate.caratOrPurity) {
                "24K", "999" -> value24k
                "22K", "916" -> gold100 * 0.916
                "20K", "833" -> gold100 * 0.833
                "18K", "750" -> gold100 * 0.750
                else -> null
            }
            if (updatedPrice != null) {
                metalRate.copy(price = String.format("%.0f", updatedPrice))
            } else {
                metalRate
            }
        } else {
            metalRate
        }
    }
    editedRates.clear()
    editedRates.addAll(updatedList)
}

data class MetalRate(
    val source: String,      // Example: "GoodReturns", "KDBullion"
    val metal: String,       // "Gold" or "Silver"
    val caratOrPurity: String, // Example: "22K", "24K", "999", "Coin 10gm" etc.
    val price: String,       // Example: "₹9025"
    val updatedDate: String  // Example: "2025-04-27" (today's date)
)

private fun computeSilverKgFromRates(rates: List<MetalRate>): Double? {
    // Priority:
    //  - Explicit per-kg (/kg, per kg)
    //  - Explicit per-gram (/g, /gm, /gram, /grams)
    //  - 1 Kg / 1000 g
    //  - 100 g, 10 g, 1 g
    fun String.n(): Double? = this.replace("₹", "").replace(",", "").trim().toDoubleOrNull()
    val silverRates = rates.filter { it.metal.equals("Silver", true) }

    val perKgPattern = Regex("(?i)(/|per)\\s*kg\\b")
    val oneKgPattern = Regex("(?i)\\b1\\s*kg\\b|\\b1000\\s*g(?:m|ms|ram|rams)?\\b")


    // /kg or per kg
    val perKg = silverRates.firstOrNull { perKgPattern.containsMatchIn(it.caratOrPurity.lowercase()) }?.price?.n()
    if (perKg != null) return perKg

    // 1 Kg or 1000 g
    val kg = silverRates.firstOrNull { oneKgPattern.containsMatchIn(it.caratOrPurity) }?.price?.n()
   return kg

}

suspend fun fetchAllMetalRates(
    state: String,
    context: Context,
    metalRatesLoading: MutableState<Boolean>,
    dataStoreManager: DataStoreManager
): List<MetalRate> = withContext(Dispatchers.IO) {


    val storeDate = dataStoreManager.getValue(DataStoreManager.METAL_FETCH_DATE).first()

    // yyyy-MM-dd
    val todayDate = LocalDate.now().toString()

    if (todayDate != storeDate) {
        metalRates(metalRatesLoading, state, context, dataStoreManager)
    } else {

        val gold24k = dataStoreManager.getValue(DataStoreManager.METAL_GOLD_24K).first()
        val silverKg = dataStoreManager.getValue(DataStoreManager.METAL_SILVER_KG).first()
        val updated = LocalDateTime.now().toCustomFormat()

        val out = mutableListOf<MetalRate>()
        if (gold24k != null) {
            val gold100 = (100 / 99.9) * gold24k
            out.add(MetalRate("Cache", "Gold", "24K", String.format("%.0f", gold24k), updated))
            out.add(MetalRate("Cache", "Gold", "22K", String.format("%.0f", gold100 * 0.916), updated))
            out.add(MetalRate("Cache", "Gold", "18K", String.format("%.0f", gold100 * 0.750), updated))
        }
        if (silverKg != null) {
            out.add(MetalRate("Cache", "Silver", "1 Kg", String.format("%.0f", silverKg), updated))
            out.add(MetalRate("Cache", "Silver", "1 g", String.format("%.0f", silverKg / 1000.0), updated))
        }
        metalRatesLoading.value = false
        return@withContext out
    }


}

suspend fun metalRates(
    metalRatesLoading: MutableState<Boolean>,
    state: String,
    context: Context,
    dataStoreManager: DataStoreManager,
): MutableList<MetalRate> {
    val todayDate = LocalDate.now().toString()
    val combinedRates = mutableListOf<MetalRate>()
    metalRatesLoading.value = true

    // Fetch gold rates from GoodReturns with separate error handling
    try {
//        val goldRates = fetchGoldPricesGoodReturns(state, context)
//        combinedRates.addAll(goldRates)
    } catch (e: Exception) {
        // Handle error specifically for gold rates, add an error item
        combinedRates.add(
            MetalRate(
                source = "GoodReturns",
                metal = "Gold",
                caratOrPurity = "Error",
                price = e.localizedMessage ?: "Unknown error",
                updatedDate = LocalDateTime.now().toCustomFormat()
            )
        )
    }

    // Fetch gold rates from Angel One with separate error handling
    try {
        val angelOneGoldRates = fetchGoldPricesAngelOne(state, context)
        combinedRates.addAll(angelOneGoldRates)
    } catch (e: Exception) {
        // Handle error specifically for Angel One gold rates, add an error item
        combinedRates.add(
            MetalRate(
                source = "AngelOne",
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
    // Persist today's 24K gold (per gram) and Silver 1 Kg into DataStore
    try {
        val gold24k = combinedRates.firstOrNull {
            it.metal == "Gold" && it.caratOrPurity.equals(
                "24K",
                true
            )
        }?.price?.replace(",", "")?.toDoubleOrNull()
        val silverKg = computeSilverKgFromRates(combinedRates)

        if (gold24k != null) {
            dataStoreManager.setValue(DataStoreManager.METAL_GOLD_24K, gold24k)
        }
        if (silverKg != null) {
            dataStoreManager.setValue(DataStoreManager.METAL_SILVER_KG, silverKg)
        }
        dataStoreManager.setValue(DataStoreManager.METAL_FETCH_DATE, todayDate)
    } catch (_: Exception) {
    }

    metalRatesLoading.value = false
    try {
        val todayIso = LocalDate.now().toString()
        val gold24k = combinedRates.firstOrNull {
            it.metal == "Gold" && it.caratOrPurity.equals(
                "24K",
                true
            )
        }?.price?.replace(",", "")?.toDoubleOrNull()

        fun isSilver1Kg(label: String): Boolean =
            label.contains("1kg", true) || label.contains("1 kg", true) || label.contains(
                "1000 g",
                true
            )

        fun isSilver1g(label: String): Boolean =
            label.contains("1 g", true) || label.contains("1g", true)

        val silverKg = computeSilverKgFromRates(combinedRates)

        if (gold24k != null) {
            dataStoreManager.setValue(DataStoreManager.METAL_GOLD_24K, gold24k)
        }
        if (silverKg != null) {
            dataStoreManager.setValue(DataStoreManager.METAL_SILVER_KG, silverKg)
        }
        dataStoreManager.setValue(DataStoreManager.METAL_FETCH_DATE, todayIso)
        // Also ensure display always includes Silver 1 Kg and 1 g entries
        val updatedTs = LocalDateTime.now().toCustomFormat()
        if (silverKg != null) {
            val hasKg = combinedRates.any {
                it.metal == "Silver" && it.caratOrPurity.contains(
                    "1 Kg",
                    true
                )
            }
            val hasG = combinedRates.any {
                it.metal == "Silver" && (it.caratOrPurity.contains(
                    "1 g",
                    true
                ) || it.caratOrPurity.contains("1g", true))
            }
            if (!hasKg) {
                combinedRates.add(
                    MetalRate(
                        source = "Computed",
                        metal = "Silver",
                        caratOrPurity = "1 Kg",
                        price = String.format("%.0f", silverKg),
                        updatedDate = updatedTs
                    )
                )
            }
            if (!hasG) {
                combinedRates.add(
                    MetalRate(
                        source = "Computed",
                        metal = "Silver",
                        caratOrPurity = "1 g",
                        price = String.format("%.0f", silverKg / 1000.0),
                        updatedDate = updatedTs
                    )
                )
            }
        }
    } catch (_: Exception) {
    }
    // Return the combined list of rates, even if some fetches failed
    return combinedRates
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
                    "GoodReturns", "Gold", "Error", "No data", todayDate
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

suspend fun fetchGoldPricesAngelOne(state: String, context: Context): List<MetalRate> =
    withContext(Dispatchers.IO) {
        val url = "https://www.angelone.in/gold-rates-today"

        try {
            val doc = Jsoup.connect(url).userAgent("Mozilla/5.0").get()
            val prices = mutableListOf<MetalRate>()
            val todayDate = LocalDateTime.now().toCustomFormat()

            // Look for table rows containing gold prices
            val tableRows = doc.select("tr.MuiTableRow-root")

            for (row in tableRows) {
                val cells = row.select("td.MuiTableCell-root")

                if (cells.size >= 4) {
                    val gramText = cells[0].text().trim()
                    val price24kText = cells[1].select("div").text().trim()
                    val price22kText = cells[2].select("div").text().trim()
                    val price18kText = cells[3].select("div").text().trim()

                    // Check if this row contains 1 gm data
                    if (gramText.contains("1 gm", ignoreCase = true)) {
                        // Add 24K gold price
                        if (price24kText.isNotEmpty()) {
                            prices.add(
                                MetalRate(
                                    source = "AngelOne",
                                    metal = "Gold",
                                    caratOrPurity = "24K",
                                    price = price24kText.replace("₹", "").replace(",", ""),
                                    updatedDate = todayDate
                                )
                            )
                        }

                        // Add 22K gold price
                        if (price22kText.isNotEmpty()) {
                            prices.add(
                                MetalRate(
                                    source = "AngelOne",
                                    metal = "Gold",
                                    caratOrPurity = "22K",
                                    price = price22kText.replace("₹", "").replace(",", ""),
                                    updatedDate = todayDate
                                )
                            )
                        }

                        // Add 18K gold price
                        if (price18kText.isNotEmpty()) {
                            prices.add(
                                MetalRate(
                                    source = "AngelOne",
                                    metal = "Gold",
                                    caratOrPurity = "18K",
                                    price = price18kText.replace("₹", "").replace(",", ""),
                                    updatedDate = todayDate
                                )
                            )
                        }
                    }
                }
            }

            // If no table rows found, try alternative selectors
            if (prices.isEmpty()) {
                // Try to find the main price display elements
                doc.selectFirst("div:contains(24K Gold)")
                doc.selectFirst("div:contains(22K Gold)")
                doc.selectFirst("div:contains(18K Gold)")

                // Look for price patterns in the document
                val pricePattern = Regex("₹[0-9,]+")
                val allText = doc.text()
                val priceMatches = pricePattern.findAll(allText).map { it.value }.toList()

                if (priceMatches.size >= 3) {
                    prices.add(
                        MetalRate(
                            source = "AngelOne",
                            metal = "Gold",
                            caratOrPurity = "24K",
                            price = priceMatches[0].replace("₹", "").replace(",", ""),
                            updatedDate = todayDate
                        )
                    )
                    prices.add(
                        MetalRate(
                            source = "AngelOne",
                            metal = "Gold",
                            caratOrPurity = "22K",
                            price = priceMatches[1].replace("₹", "").replace(",", ""),
                            updatedDate = todayDate
                        )
                    )
                    prices.add(
                        MetalRate(
                            source = "AngelOne",
                            metal = "Gold",
                            caratOrPurity = "18K",
                            price = priceMatches[2].replace("₹", "").replace(",", ""),
                            updatedDate = todayDate
                        )
                    )
                }
            }

            if (prices.isEmpty()) {
                listOf(
                    MetalRate(
                        "AngelOne", "Gold", "Error", "No data found", todayDate
                    )
                )
            } else {
                prices
            }

        } catch (e: Exception) {
            listOf(
                MetalRate(
                    "AngelOne",
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
                    "GoodReturns", "Silver", "Error", "No data", todayDate
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

        saveToRootStorage(context, doc, "KdBullion") { message ->
            Log.d("KDBullionFetcher", message)
        }
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
    // Use environment variables or secure storage for credentials
    val registerJson = JSONObject().apply {
        put("Name", "JewelVaultApp")
        put("FirmName", "JewelVault")
        put("City", "India")
        put("ContactNo", "00000000000") // Use actual business contact
        put("ClientId", 4)
    }

    val registerBody = JSONObject().apply {
        put("ClientDetails", JSONArray().apply {
            put(registerJson)
        })
    }

    val registerRequestBody =
        registerBody.toString().toRequestBody("application/json".toMediaTypeOrNull())

    val registerRequest = Request.Builder().url(registerUrl).post(registerRequestBody).build()

    try {
        Log.d("RegisterAndFetch", "Sending registration request to $registerUrl")
        val registerResponse = client.newCall(registerRequest).execute()
        if (!registerResponse.isSuccessful) {
            val errorResponse = registerResponse.body?.string() ?: "Unknown error"
            Log.e(
                "RegisterAndFetch", "Registration failed: ${registerResponse.code}, $errorResponse"
            )
            return@withContext "Registration failed: ${registerResponse.code}, $errorResponse"
        }
        Log.d(
            "RegisterAndFetch", "Registration successful, response code: ${registerResponse.code}"
        )
    } catch (e: Exception) {
        Log.e("RegisterAndFetch", "Error during registration: ${e.localizedMessage}")
        return@withContext "Error during registration: ${e.localizedMessage}"
    }

    // Step 2: Fetch rates (This can stay the same as the previous example)
    val fetchUrl = "http://kdbullion.in/WebService/WebService.asmx/GetRateByClient"
    val fetchRequest = Request.Builder().url(fetchUrl)
        .post("".toRequestBody("application/json".toMediaTypeOrNull())).build()

    try {
        Log.d("RegisterAndFetch", "Sending request to fetch rates from $fetchUrl")
        val fetchResponse = client.newCall(fetchRequest).execute()
        if (!fetchResponse.isSuccessful) {
            val errorResponse = fetchResponse.body?.string() ?: "Unknown error"
            Log.e(
                "RegisterAndFetch", "Fetching rates failed: ${fetchResponse.code}, $errorResponse"
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
                    "KDBullion", "Unknown", "Error", "No data", todayDate
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

fun saveToRootStorage(
    context: Context,
    doc: org.jsoup.nodes.Document,
    fileName: String,
    onMessage: ((String) -> Unit)? = null
) {
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

        // Show message through callback if provided
        onMessage?.invoke("File saved at: ${file.absolutePath}")

    } catch (e: IOException) {
        Log.e("GoldPriceFetcher", "Error saving document to file", e)
        onMessage?.invoke("Failed to save file: ${e.message}")
    }
}