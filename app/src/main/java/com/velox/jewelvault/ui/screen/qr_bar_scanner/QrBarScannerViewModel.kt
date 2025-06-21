package com.velox.jewelvault.ui.screen.qr_bar_scanner

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velox.jewelvault.data.MetalRate
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.dto.ItemSelectedModel
import com.velox.jewelvault.utils.ChargeType
import com.velox.jewelvault.utils.DataStoreManager
import com.velox.jewelvault.utils.to2FString
import com.velox.jewelvault.utils.withIo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class QrBarScannerViewModel @Inject constructor(
    private val context: Context,
    private val appDatabase: AppDatabase,
    private val dataStoreManager: DataStoreManager,
) : ViewModel() {

    val selectedItemList = mutableStateListOf<Pair<Int, ItemSelectedModel?>>()

    fun processScan(id: Int, metalRates: SnapshotStateList<MetalRate>) {
        viewModelScope.launch {
            checkAndAddList(id, metalRates)
        }
    }

    private suspend fun checkAndAddList(
        id: Int,
        metalRates: SnapshotStateList<MetalRate>
    ): String = withIo  {
        val existing = selectedItemList.find { it.first == id }?.second
        if (existing != null) {
            return@withIo "Id: $id\nWt: ${existing.gsWt.to2FString()} (${existing.fnWt.to2FString()})\n(${existing.purity}) P: $${(existing.price + existing.chargeAmount + existing.tax).to2FString()}"
        }

        val item = getItemByIdSync(id)
        if (item == null) return@withIo "Id: $id\nItem Not Found"

        val oneUnitPrice = when (item.catName.trim().lowercase()) {
            "gold" -> {
                val price24k = metalRates.firstOrNull {
                    it.metal == "Gold" && it.caratOrPurity == "24K"
                }?.price?.toDoubleOrNull()
                if (price24k != null) (100 / 99.9) * price24k else null
            }
            "silver" -> {
                metalRates.firstOrNull {
                    it.metal == "Silver" && it.caratOrPurity == "Silver /g"
                }?.price?.toDoubleOrNull()
            }
            else -> null
        }

        if (oneUnitPrice == null) return@withIo "Id: $id\nPlease load metal price"

        val price = oneUnitPrice * item.fnWt
        val charge = when (item.crgType) {
            ChargeType.Percentage.type -> price * (item.crg / 100)
            ChargeType.Piece.type -> item.crg * item.quantity
            ChargeType.PerGm.type -> item.crg * item.fnWt
            else -> 0.0
        }
        val tax = (price + charge) * ((item.cgst + item.igst + item.sgst) / 100)
        val updatedItem = item.copy(price = price, chargeAmount = charge, tax = tax)
        selectedItemList.add(id to updatedItem)

        return@withIo "Id: $id\nWt: ${item.gsWt.to2FString()} (${item.fnWt.to2FString()})\n(${item.purity}) P: $${(price + charge + tax).to2FString()}"
    }

    private suspend fun getItemByIdSync(itemId: Int): ItemSelectedModel? = withIo {
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
                othCrgDes    = item.othCrgDes,
                othCrg       = item.othCrg,
                cgst         = item.cgst,
                sgst         = item.sgst,
                igst         = item.igst,
                huid         = item.huid,
                addDate      = item.addDate,
                addDesKey    = item.addDesKey,
                addDesValue  = item.addDesValue
            )
        }
    }
}


//