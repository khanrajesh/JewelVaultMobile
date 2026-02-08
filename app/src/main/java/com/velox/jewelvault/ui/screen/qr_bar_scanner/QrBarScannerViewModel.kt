package com.velox.jewelvault.ui.screen.qr_bar_scanner

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velox.jewelvault.data.MetalRate
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.dto.ItemSelectedModel
import com.velox.jewelvault.data.DataStoreManager
import com.velox.jewelvault.utils.to3FString
import com.velox.jewelvault.utils.withIo
import com.velox.jewelvault.utils.CalculationUtils
import com.velox.jewelvault.utils.parseQrItemPayload
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QrBarScannerViewModel @Inject constructor(
    private val context: Context,
    private val appDatabase: AppDatabase,
    private val dataStoreManager: DataStoreManager,
) : ViewModel() {

    val selectedItemList = mutableStateListOf<Pair<String, ItemSelectedModel?>>()

    fun processScan(raw: String, metalRates: SnapshotStateList<MetalRate>) {
        viewModelScope.launch {
            val id = extractItemId(raw)
            checkAndAddList(id, metalRates)
        }
    }

    private fun extractItemId(raw: String): String {
        val parsed = parseQrItemPayload(raw)
        return parsed?.id ?: raw
    }

    private suspend fun checkAndAddList(
        id: String,
        metalRates: SnapshotStateList<MetalRate>
    ): String = withIo  {
        val existing = selectedItemList.find { it.first == id }?.second
        if (existing != null) {
            val existingTotal = (
                existing.price+
                existing.chargeAmount+
                existing.compCrg
            )
            return@withIo "Id: $id\nWt: ${existing.gsWt.to3FString()} (${existing.fnWt.to3FString()})\n(${existing.purity}) P: $${existingTotal.to3FString()}"
        }

        val item = getItemByIdSync(id)
        if (item == null) return@withIo "Id: $id\nItem Not Found"

        // Use CalculationUtils for all calculations
        val oneUnitPrice = CalculationUtils.metalUnitPrice(item.catName, metalRates.toList())
        if (oneUnitPrice == null) return@withIo "Id: $id\nPlease load metal price"

        val price = CalculationUtils.baseMetalPrice(item.fnWt, oneUnitPrice)
        val charge = CalculationUtils.makingCharge(
            chargeType = item.crgType,
            chargeRate = item.crg,
            basePrice = price,
            quantity = item.quantity,
            weight = item.ntWt
        )
        val tax = CalculationUtils.calculateTaxPerItem(
            basePrice = price,
            charge = charge+item.compCrg,
            cgstRate = item.cgst,
            sgstRate = item.sgst,
            igstRate = item.igst
        )
        val updatedItem = item.copy(price = price, chargeAmount = charge)
        selectedItemList.add(id to updatedItem)

        val total = (price+ charge+ item.compCrg+ tax)
        return@withIo "Id: $id\nWt: ${item.gsWt.to3FString()} (${item.fnWt.to3FString()})\n(${item.purity}) P: $${total.to3FString()}"
    }

    private suspend fun getItemByIdSync(itemId: String): ItemSelectedModel? = withIo {
        val entity = appDatabase.itemDao().getItemById(itemId)
        entity?.let { item ->
            ItemSelectedModel(
                itemId       = item.itemId,
                itemAddName  = item.itemAddName,
                catId        = item.catId,
                userId       = item.userId,
                storeId      = item.storeId,
                catName      = item.catName,
                subCatId     = item.subCatId,
                subCatName   = item.subCatName,
                entryType    = item.entryType,
                quantity     = item.quantity,
                gsWt         = item.gsWt,
                ntWt         = item.ntWt,
                fnWt         = item.fnWt,
                fnMetalPrice = 0.0,
                purity       = item.purity,
                crgType      = item.crgType,
                crg          = item.crg,
                compDes    = item.othCrgDes,
                compCrg       = item.othCrg,
                cgst         = item.cgst,
                sgst         = item.sgst,
                igst         = item.igst,
                huid         = item.huid,
                addDate      = item.addDate,
                addDesKey    = item.addDesKey,
                addDesValue  = item.addDesValue,
                sellerFirmId = item.sellerFirmId,
                purchaseOrderId = item.purchaseOrderId,
                purchaseItemId = item.purchaseItemId,
            )
        }
    }
}


//
