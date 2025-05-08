package com.velox.jewelvault.ui.screen.inventory

import android.content.Context
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.dao.ItemDao
import com.velox.jewelvault.data.roomdb.entity.ItemEntity
import com.velox.jewelvault.utils.ioScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val appDatabase: AppDatabase,
    dataStore: DataStore<Preferences>,
    context: Context
) : ViewModel() {

    val itemList: SnapshotStateList<ItemEntity> = SnapshotStateList()

    init {
        getAllItems()
    }

    fun getAllItems() {
        viewModelScope.launch {
            appDatabase.itemDao().getAll().collect { items ->
                // Update the SnapshotStateList with the fetched items
                itemList.clear() // Clear the list before adding new data
                itemList.addAll(items) // Add all items to the SnapshotStateList
            }
        }
    }

    fun safeInsertItem(
        item: ItemEntity,
        onSuccess: (ItemEntity, Long) -> Unit,  // Pass item and new itemId
        onFailure: (Throwable) -> Unit
    ) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // Insert the item into the database and get the auto-generated itemId
                    val newItemId = appDatabase.itemDao().insert(item)

                    // If you want to return the item with its new itemId (auto-incremented)
                    val insertedItem = item.copy(itemId = newItemId.toInt())  // Cast Long to Int

                    // Call the success callback with the new item and itemId
                    onSuccess(insertedItem, newItemId)
                }
            } catch (e: Exception) {
                // Call the failure callback if an error occurred
                onFailure(e)
            }
        }
    }


}