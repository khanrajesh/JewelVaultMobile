package com.velox.jewelvault.data

import android.annotation.SuppressLint
import android.content.Context
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Cloud
import androidx.compose.material.icons.twotone.CloudOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.velox.jewelvault.data.remort.RepositoryImpl
import com.velox.jewelvault.data.remort.model.MetalRatesResponseDto
import com.velox.jewelvault.ui.components.CalculatorScreen
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.handler.handleFlowKtor
import com.velox.jewelvault.utils.ioScope
import com.velox.jewelvault.utils.isLandscape
import com.velox.jewelvault.utils.toCustomFormat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime


@Composable
fun MetalRatesTicker(
    modifier: Modifier = Modifier,
    textColor: Color = Color.Black,
) {
    val showEditDialog = remember { mutableStateOf(false) }
    val baseViewModel = LocalBaseViewModel.current
    val hasApi = baseViewModel.metalRates.any { rate ->
        !rate.source.trim().equals("cache", ignoreCase = true)
    }
    val latestTime = baseViewModel.metalRates.firstOrNull()?.updatedDate.orEmpty()
    val infiniteTransition = rememberInfiniteTransition()
    val animatedOffsetX by infiniteTransition.animateFloat(
        initialValue = 1.5f, targetValue = -1.5f, animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // Build scrolling content with single source icon (cloud = API present, cloud off = cached/local)
    Box(
        modifier = modifier
            .padding(horizontal = 10.dp)
            .height(50.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        Log.d(
                            "MetalRates",
                            "MetalRatesTicker: long press detected, opening edit dialog (rates=${baseViewModel.metalRates.size})"
                        )
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

            val ratesText = baseViewModel.metalRates.joinToString("   •   ") {
                "${it.metal} ${it.caratOrPurity}: ₹${it.price}"
            }

            Row(
                modifier = Modifier.offset(x = animatedOffsetX.dp * 1000),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (hasApi) Icons.TwoTone.Cloud else Icons.TwoTone.CloudOff,
                    contentDescription = if (hasApi) "API rates ($latestTime)" else "Cached rates",
                    tint = if (hasApi) Color.Black else Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = ratesText,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                )
            }
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
    Log.d("MetalRates", "EditMetalRatesDialog: opened")
    LocalContext.current
    val viewModel = LocalBaseViewModel.current
    val editedRates = remember { mutableStateListOf(*viewModel.metalRates.toTypedArray()) }

    Dialog(
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        ),
        onDismissRequest = {
            Log.d("MetalRates", "EditMetalRatesDialog: dismissed")
            showDialog.value = false
        },
        content = {
            val dialogScroll = rememberScrollState()
            Column(
                Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.9f)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .verticalScroll(dialogScroll)
                    .padding(16.dp)
            ) {
                Text("Edit Metal Rates", fontWeight = FontWeight.Black)
                Spacer(Modifier.height(10.dp))
                if (isLandscape()) {
                    Row(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        CalculatorScreen(
                            Modifier
                                .weight(0.6f)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(10.dp)
                        )
                        Spacer(Modifier.width(10.dp))



                        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                            itemsIndexed(editedRates) { index, metalRate ->
                                val isGold24 = metalRate.metal.equals(
                                    "Gold",
                                    true
                                ) && metalRate.caratOrPurity.equals("24K", true)
                                val isSilverKg = metalRate.metal.equals(
                                    "Silver",
                                    true
                                ) && metalRate.caratOrPurity.contains("1 Kg", true)
                                val isEditable = isGold24 || isSilverKg
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
                                        enabled = isEditable,
                                        onValueChange = {
                                            try {
                                                when {
                                                    isGold24 -> updateConjugateMetalPrice(
                                                        editedRates,
                                                        it.toDouble()
                                                    )

                                                    isSilverKg -> updateSilverPrices(
                                                        editedRates,
                                                        it.toDouble()
                                                    )

                                                    else -> viewModel.snackBarState =
                                                        "Only Gold 24K and Silver 1 Kg are editable"
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

                } else {
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                            itemsIndexed(editedRates) { index, metalRate ->
                                val isGold24 = metalRate.metal.equals(
                                    "Gold",
                                    true
                                ) && metalRate.caratOrPurity.equals("24K", true)
                                val isSilverKg = metalRate.metal.equals(
                                    "Silver",
                                    true
                                ) && metalRate.caratOrPurity.contains("1 Kg", true)
                                val isEditable = isGold24 || isSilverKg
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
                                        enabled = isEditable,
                                        onValueChange = {
                                            try {
                                                when {
                                                    isGold24 -> updateConjugateMetalPrice(
                                                        editedRates,
                                                        it.toDouble()
                                                    )

                                                    isSilverKg -> updateSilverPrices(
                                                        editedRates,
                                                        it.toDouble()
                                                    )

                                                    else -> viewModel.snackBarState =
                                                        "Only Gold 24K and Silver 1 Kg are editable"
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

                        Spacer(Modifier.width(10.dp))
                        CalculatorScreen(
                            Modifier
                                .weight(0.6f)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(10.dp)
                        )

                    }

                }

                Row(Modifier.fillMaxWidth()) {
                    Spacer(Modifier.weight(1f))

                    TextButton(onClick = { showDialog.value = false }) {
                        Log.d("MetalRates", "EditMetalRatesDialog: cancel clicked")
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(10.dp))
                    TextButton(onClick = {
                        Log.d("MetalRates", "EditMetalRatesDialog: save clicked")
                        editedRates.forEachIndexed { index, metalRate ->
                            editedRates[index] =
                                metalRate.copy(updatedDate = LocalDateTime.now().toCustomFormat())
                        }
                        viewModel.metalRates.clear()
                        viewModel.metalRates.addAll(editedRates)
                        Log.d(
                            "MetalRates",
                            "EditMetalRatesDialog: rates updated in viewModel -> ${viewModel.metalRates.size} items"
                        )
                        // Persist 3 fields when user updates from dialog:
                        // 1) GOLD 24K (per gram) as Double
                        // 2) SILVER 1 Kg as Double (compute from 1 g if needed)
                        // 3) METAL_FETCH_DATE as yyyy-MM-dd
                        ioScope {
                            try {
                                val todayIso = LocalDate.now().toString()
                                val gold24k = editedRates.firstOrNull {
                                    it.metal == "Gold" && it.caratOrPurity.equals(
                                        "24K", true
                                    )
                                }?.price?.replace(",", "")?.toDoubleOrNull()

                                // Compute silver 1 Kg from edited rates if provided; do not overwrite if user didn't change
                                val computedSilverKg = computeSilverKgFromRates(editedRates)

                                if (gold24k != null) {
                                    viewModel.dataStoreManager.setValue(
                                        DataStoreManager.METAL_GOLD_24K, gold24k
                                    )
                                }
                                if (computedSilverKg != null) {
                                    viewModel.dataStoreManager.setValue(
                                        DataStoreManager.METAL_SILVER_KG, computedSilverKg
                                    )
                                }
                                viewModel.dataStoreManager.setValue(
                                    DataStoreManager.METAL_FETCH_DATE, todayIso
                                )
                                Log.d(
                                    "MetalRates",
                                    "EditMetalRatesDialog: persisted gold24k=$gold24k, silverKg=$computedSilverKg, date=$todayIso"
                                )
                            } catch (_: Exception) {
                            }
                        }
                        showDialog.value = false
                        Log.d("MetalRates", "EditMetalRatesDialog: closed after save")
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

@SuppressLint("DefaultLocale")
fun updateSilverPrices(
    editedRates: SnapshotStateList<MetalRate>, valueKg: Double
) {
    val updatedList = editedRates.map { metalRate ->
        if (metalRate.metal.equals("Silver", true)) {
            val updatedPrice = when {
                metalRate.caratOrPurity.contains("1 Kg", true) -> valueKg
                metalRate.caratOrPurity.contains(
                    "1 g",
                    true
                ) || metalRate.caratOrPurity.contains("1g", true) -> valueKg / 1000.0

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
    val perKg =
        silverRates.firstOrNull { perKgPattern.containsMatchIn(it.caratOrPurity.lowercase()) }?.price?.n()
    if (perKg != null) return perKg

    // 1 Kg or 1000 g
    val kg = silverRates.firstOrNull { oneKgPattern.containsMatchIn(it.caratOrPurity) }?.price?.n()
    return kg

}

suspend fun fetchAllMetalRates(
    _context: Context,
    metalRatesLoading: MutableState<Boolean>,
    dataStoreManager: DataStoreManager,
    repository: RepositoryImpl
): List<MetalRate> = withContext(Dispatchers.IO) {

    val storeDate = dataStoreManager.getValue(DataStoreManager.METAL_FETCH_DATE).first()
    val todayDate = LocalDate.now().toString() // yyyy-MM-dd
    val gold24kCached = dataStoreManager.getValue(DataStoreManager.METAL_GOLD_24K).first()
    val silverKgCached = dataStoreManager.getValue(DataStoreManager.METAL_SILVER_KG).first()

    val hasCompleteCache = gold24kCached != null && silverKgCached != null

    if (todayDate != storeDate || !hasCompleteCache) {
        metalRates(metalRatesLoading, _context, dataStoreManager, repository)
    } else {
        val updated = LocalDateTime.now().toCustomFormat()

        val out = mutableListOf<MetalRate>()
        val gold100 = (100 / 99.9) * (gold24kCached ?: 0.0)
        out.add(MetalRate("Cache", "Gold", "24K", String.format("%.0f", gold24kCached), updated))
        out.add(MetalRate("Cache", "Gold", "22K", String.format("%.0f", gold100 * 0.916), updated))
        out.add(MetalRate("Cache", "Gold", "18K", String.format("%.0f", gold100 * 0.750), updated))

        out.add(
            MetalRate(
                "Cache", "Silver", "1 Kg", String.format("%.0f", silverKgCached), updated
            )
        )
        out.add(
            MetalRate(
                "Cache",
                "Silver",
                "1 g",
                String.format("%.0f", (silverKgCached ?: 0.0) / 1000.0),
                updated
            )
        )

        metalRatesLoading.value = false
        return@withContext out
    }
}

suspend fun metalRates(
    metalRatesLoading: MutableState<Boolean>,
    _context: Context,
    dataStoreManager: DataStoreManager,
    repository: RepositoryImpl,
): MutableList<MetalRate> {
    val combinedRates = mutableListOf<MetalRate>()
    metalRatesLoading.value = true
    var apiSucceeded = false

    val completion = CompletableDeferred<Unit>()
    handleFlowKtor(
        flow = repository.requestMetalRate(),
        onLoading = { isLoading -> metalRatesLoading.value = isLoading },
        onFailure = { message, _, _ ->
            val updatedTs = LocalDateTime.now().toCustomFormat()
            val gold24kCached = withContext(Dispatchers.IO) {
                dataStoreManager.getValue(DataStoreManager.METAL_GOLD_24K).first()
            }
            val silverKgCached = withContext(Dispatchers.IO) {
                dataStoreManager.getValue(DataStoreManager.METAL_SILVER_KG).first()
            }

            if (gold24kCached != null && silverKgCached != null) {
                combinedRates.clear()
                val gold100 = (100 / 99.9) * gold24kCached
                combinedRates.add(
                    MetalRate(
                        source = "Cache",
                        metal = "Gold",
                        caratOrPurity = "24K",
                        price = String.format("%.0f", gold24kCached),
                        updatedDate = updatedTs
                    )
                )
                combinedRates.add(
                    MetalRate(
                        source = "Cache",
                        metal = "Gold",
                        caratOrPurity = "22K",
                        price = String.format("%.0f", gold100 * 0.916),
                        updatedDate = updatedTs
                    )
                )
                combinedRates.add(
                    MetalRate(
                        source = "Cache",
                        metal = "Gold",
                        caratOrPurity = "18K",
                        price = String.format("%.0f", gold100 * 0.750),
                        updatedDate = updatedTs
                    )
                )
                combinedRates.add(
                    MetalRate(
                        source = "Cache",
                        metal = "Silver",
                        caratOrPurity = "1 Kg",
                        price = String.format("%.0f", silverKgCached),
                        updatedDate = updatedTs
                    )
                )
                combinedRates.add(
                    MetalRate(
                        source = "Cache",
                        metal = "Silver",
                        caratOrPurity = "1 g",
                        price = String.format("%.0f", silverKgCached / 1000.0),
                        updatedDate = updatedTs
                    )
                )
            } else {
                combinedRates.clear()
                val zeroPrice = "0000"
                combinedRates.add(
                    MetalRate(
                        source = "Cache",
                        metal = "Gold",
                        caratOrPurity = "24K",
                        price = zeroPrice,
                        updatedDate = updatedTs
                    )
                )
                combinedRates.add(
                    MetalRate(
                        source = "Cache",
                        metal = "Gold",
                        caratOrPurity = "22K",
                        price = zeroPrice,
                        updatedDate = updatedTs
                    )
                )
                combinedRates.add(
                    MetalRate(
                        source = "Cache",
                        metal = "Gold",
                        caratOrPurity = "18K",
                        price = zeroPrice,
                        updatedDate = updatedTs
                    )
                )
                combinedRates.add(
                    MetalRate(
                        source = "Cache",
                        metal = "Silver",
                        caratOrPurity = "1 Kg",
                        price = zeroPrice,
                        updatedDate = updatedTs
                    )
                )
                combinedRates.add(
                    MetalRate(
                        source = "Cache",
                        metal = "Silver",
                        caratOrPurity = "1 g",
                        price = zeroPrice,
                        updatedDate = updatedTs
                    )
                )
            }
            if (!completion.isCompleted) completion.complete(Unit)
        },
        onSuccess = { data ->
            apiSucceeded = true
            combinedRates.addAll(mapMetalRatesResponse(data))
            if (!completion.isCompleted) completion.complete(Unit)
        })
    completion.await()

    try {
        val todayIso = LocalDate.now().toString()
        val gold24k = combinedRates.firstOrNull {
            it.metal == "Gold" && it.caratOrPurity.equals("24K", true)
        }?.price?.replace(",", "")?.toDoubleOrNull()
        val silverKg = computeSilverKgFromRates(combinedRates)

        if (apiSucceeded) {
            if (gold24k != null) {
                dataStoreManager.setValue(DataStoreManager.METAL_GOLD_24K, gold24k)
            }
            if (silverKg != null) {
                dataStoreManager.setValue(DataStoreManager.METAL_SILVER_KG, silverKg)
            }
            dataStoreManager.setValue(DataStoreManager.METAL_FETCH_DATE, todayIso)
        }

        val updatedTs = LocalDateTime.now().toCustomFormat()
        if (silverKg != null) {
            val hasKg = combinedRates.any {
                it.metal == "Silver" && it.caratOrPurity.contains(
                    "1 Kg", true
                )
            }
            val hasG = combinedRates.any {
                it.metal == "Silver" && (it.caratOrPurity.contains(
                    "1 g", true
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

    metalRatesLoading.value = false
    return combinedRates
}


private fun mapMetalRatesResponse(response: MetalRatesResponseDto?): List<MetalRate> {
    val data = response?.data ?: return emptyList()
    val output = mutableListOf<MetalRate>()
    val updatedDate = LocalDateTime.now().toCustomFormat()

    data.rates.forEach { rate ->
        val price = rate.price
        val unitGm = rate.unitGm
        if (price == null || unitGm == null || unitGm <= 0) return@forEach

        when (rate.metal.lowercase()) {
            "gold" -> {
                val perGram = price / unitGm
                val gold100 = (100 / 99.9) * perGram
                output.add(
                    MetalRate(
                        "API", "Gold", "24K", String.format("%.0f", perGram), updatedDate
                    )
                )
                output.add(
                    MetalRate(
                        "API", "Gold", "22K", String.format("%.0f", gold100 * 0.916), updatedDate
                    )
                )
                output.add(
                    MetalRate(
                        "API", "Gold", "18K", String.format("%.0f", gold100 * 0.750), updatedDate
                    )
                )
            }

            "silver" -> {
                val pricePerKg = price * (1000.0 / unitGm)
                output.add(
                    MetalRate(
                        "API", "Silver", "1 Kg", String.format("%.0f", pricePerKg), updatedDate
                    )
                )
                output.add(
                    MetalRate(
                        "API",
                        "Silver",
                        "1 g",
                        String.format("%.0f", pricePerKg / 1000.0),
                        updatedDate
                    )
                )
            }
        }
    }

    return output
}
