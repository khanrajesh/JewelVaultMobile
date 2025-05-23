package com.velox.jewelvault.ui.screen.dashboard

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.dao.IndividualSellItem
import com.velox.jewelvault.utils.DataStoreManager
import com.velox.jewelvault.utils.withIo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.sql.Timestamp
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val appDatabase: AppDatabase,
    private val dataStoreManager: DataStoreManager,
    private val _loadingState: MutableState<Boolean>,
    private val _snackBarState: MutableState<String>
) : ViewModel() {

    val recentSellsItem: SnapshotStateList<IndividualSellItem> = SnapshotStateList()

    fun getRecentSellItem() {
        viewModelScope.launch {
            withIo {
                val end = Timestamp(System.currentTimeMillis())
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, -10)
                val start = Timestamp(calendar.timeInMillis)

                val list = appDatabase.orderDao().getIndividualSellItems(
                    start = start, end = end
                )

                if (list.isNotEmpty()) {
                    recentSellsItem.clear()
                    recentSellsItem.addAll(list)
                }
            }
        }
    }


}