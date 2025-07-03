package com.velox.jewelvault.ui.screen.inventory

import android.app.DatePickerDialog
import android.widget.DatePicker
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.velox.jewelvault.data.roomdb.entity.SubCategoryEntity
import com.velox.jewelvault.ui.components.CusOutlinedTextField
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.ui.components.ItemListViewComponent
import com.velox.jewelvault.utils.ChargeType
import com.velox.jewelvault.utils.EntryType
import com.velox.jewelvault.utils.ExportFormat
import com.velox.jewelvault.utils.Purity
import com.velox.jewelvault.utils.export.ExportWorker
import com.velox.jewelvault.utils.export.enqueueExportWorker
import com.velox.jewelvault.utils.export.exportItemListInBackground
import com.velox.jewelvault.utils.mainScope
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


@Composable
fun InventoryFilterScreen(viewModel: InventoryViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val entryType = remember { InputFieldState() }
    val purity = remember { InputFieldState() }
    val chargeType = remember { InputFieldState() }
    val categoryState = remember { InputFieldState() }
    val subCategoryState = remember { InputFieldState() }
    val startDate = remember { InputFieldState() }
    val endDate = remember { InputFieldState() }

    // Keep track of subcategories for selected category
    val subCategories = remember { mutableStateListOf<SubCategoryEntity>() }

    val calendar = remember { Calendar.getInstance() }
    val dateFormat = remember { SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()) }

    fun showDatePicker(onDateSelected: (String) -> Unit) {
        val datePickerDialog = DatePickerDialog(
            context,
            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                calendar.set(year, month, dayOfMonth)
                onDateSelected(dateFormat.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    LaunchedEffect(true) {
//        viewModel.getCategoryAndSubCategoryDetails()
        viewModel.filterItems()
    }

    Column(Modifier.fillMaxSize().background(
        MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 18.dp)
    )) {

        // Top row: Clear + Export buttons
        Row {
            Spacer(Modifier.weight(1f))

            Button(onClick = {
                entryType.text = ""
                purity.text = ""
                chargeType.text = ""
                categoryState.text = ""
                subCategoryState.text = ""
                startDate.text = ""
                endDate.text = ""
                subCategories.clear()
//                viewModel.clearFilters()
            }) {
                Text("Clear")
            }

            Spacer(Modifier.width(8.dp))


            Button(onClick = {

                val rows = viewModel.itemList.mapIndexed { index, item ->
                    listOf(
                        (index + 1).toString(),
                        item.catName,
                        item.subCatName,
                        item.itemId.toString(),
                        item.itemAddName,
                        item.entryType,
                        item.quantity.toString(),
                        item.gsWt.toString(),
                        item.ntWt.toString(),
                        item.unit,
                        item.purity,
                        item.fnWt.toString(),
                        item.crgType,
                        item.crg.toString(),
                        item.othCrgDes,
                        item.othCrg.toString(),
                        (item.cgst + item.sgst + item.igst).toString(),
                        item.huid,
                        item.addDate.toString(),
                        item.addDesKey,
                        item.addDesValue,
                        "Extra"
                    )
                }
                val fileName = "ItemExport_${System.currentTimeMillis()}.xlsx"

                enqueueExportWorker(context,lifecycleOwner, fileName, viewModel.itemHeaderList, rows)
            }) {
                Text("Export")
            }

        }

        // Filters: Category, Subcategory, Entry Type
        Row {
            CusOutlinedTextField(
                modifier = Modifier.weight(1f),
                state = categoryState,
                placeholderText = "Category",
                dropdownItems = viewModel.catSubCatDto.map { it.catName },
                onDropdownItemSelected = { selected ->
                    categoryState.text = selected
                    val selectedCat = viewModel.catSubCatDto.find { it.catName == selected }
                    subCategories.clear()
                    selectedCat?.subCategoryList?.let { subCategories.addAll(it) }
                    subCategoryState.text = ""
                }
            )

            Spacer(Modifier.width(5.dp))

            CusOutlinedTextField(
                modifier = Modifier.weight(1f),
                state = subCategoryState,
                placeholderText = "Sub Category",
                dropdownItems = subCategories.map { it.subCatName },
                onDropdownItemSelected = { selected ->
                    subCategoryState.text = selected
                }
            )

            Spacer(Modifier.width(5.dp))

            CusOutlinedTextField(
                modifier = Modifier.weight(1f),
                state = entryType,
                placeholderText = "Entry Type",
                dropdownItems = EntryType.list(),
                onDropdownItemSelected = { selected ->
                    entryType.text = selected
                }
            )
            Spacer(Modifier.width(5.dp))

        }

        // Filters: Purity, ChargeType, Start/End Dates
        Row {
            CusOutlinedTextField(
                modifier = Modifier.weight(1f),
                state = purity,
                placeholderText = "Purity",
                dropdownItems = Purity.list(),
                onDropdownItemSelected = { selected ->
                    purity.text = selected
                }
            )

            Spacer(Modifier.width(5.dp))

            CusOutlinedTextField(
                modifier = Modifier.weight(1f),
                state = chargeType,
                placeholderText = "Charge Type",
                dropdownItems = ChargeType.list(),
                onDropdownItemSelected = { selected ->
                    chargeType.text = selected
                }
            )

            Spacer(Modifier.width(5.dp))

            CusOutlinedTextField(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        showDatePicker { selectedDate ->
                            startDate.text = selectedDate
                        }
                    },   readOnly = true,
                state = startDate,
                placeholderText = "Start Date (dd-MM-yyyy)",
            )

            Spacer(Modifier.width(5.dp))

            CusOutlinedTextField(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        showDatePicker { selectedDate ->
                            endDate.text = selectedDate
                        }
                    },
                readOnly = true,
                state = endDate,
                placeholderText = "End Date (dd-MM-yyyy)",
            )
            Spacer(Modifier.width(5.dp))

        }

        // Item list
        ItemListViewComponent(
            itemHeaderList = viewModel.itemHeaderList,
            items = viewModel.itemList,
            onItemLongClick = {
                mainScope {
                    Toast.makeText(context, "Long click", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}
