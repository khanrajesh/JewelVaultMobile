package com.velox.jewelvault.utils

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.velox.jewelvault.data.roomdb.entity.ItemEntity
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter

fun exportItemListInBackground(
    context: Context,
    fileName: String,
    itemList: List<ItemEntity>,
    headers: List<String>,
    format: ExportFormat,
    onSuccess: () -> Unit,
    onFailure: (Throwable) -> Unit
) {
    ioScope {
        try {
            val mimeType = when (format) {
                ExportFormat.CSV -> "text/csv"
                ExportFormat.XLS -> "application/vnd.ms-excel"
                ExportFormat.XLSX -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(
                    MediaStore.Downloads.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + "/JewelVault/ItemExport"
                )
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw IOException("Unable to create file in MediaStore")

            resolver.openOutputStream(uri)?.use { outputStream ->
                when (format) {
                    ExportFormat.CSV -> {
                        val writer = BufferedWriter(OutputStreamWriter(outputStream))
                        writer.write(headers.joinToString(","))
                        writer.newLine()
                        itemList.forEachIndexed { index, item ->
                            val values = listOf(
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
                                "Extra"
                            )
                            writer.write(values.joinToString(","))
                            writer.newLine()
                        }
                        writer.flush()
                    }

                    ExportFormat.XLS, ExportFormat.XLSX -> {
                        val workbook = if (format == ExportFormat.XLS) HSSFWorkbook() else XSSFWorkbook()
                        val sheet = workbook.createSheet("Inventory")

                        val headerRow = sheet.createRow(0)
                        headers.forEachIndexed { col, title ->
                            headerRow.createCell(col).setCellValue(title)
                        }

                        itemList.forEachIndexed { rowIndex, item ->
                            val row = sheet.createRow(rowIndex + 1)
                            val values = listOf(
                                (rowIndex + 1).toString(),
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
                                "Extra"
                            )
                            values.forEachIndexed { col, value ->
                                row.createCell(col).setCellValue(value)
                            }
                        }

                        workbook.write(outputStream)
                        workbook.close()
                    }
                }
            }

            withMain {
                onSuccess()
            }
        } catch (e: Exception) {
            withMain {
                onFailure(e)
            }
        }
    }
}
