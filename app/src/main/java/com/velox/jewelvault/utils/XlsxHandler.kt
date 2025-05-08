package com.velox.jewelvault.utils

import android.content.Context
import android.widget.Toast
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

fun exportToExcel(context: Context, fileName: String, data: List<List<String>>) {
    val workbook = XSSFWorkbook()
    val sheet = workbook.createSheet("Sheet1")

    data.forEachIndexed { rowIndex, row ->
        val excelRow = sheet.createRow(rowIndex)
        row.forEachIndexed { cellIndex, cell ->
            val cellItem = excelRow.createCell(cellIndex)
            cellItem.setCellValue(cell)
        }
    }

    val file = File(context.getExternalFilesDir(null), "$fileName.xlsx")
    FileOutputStream(file).use { output ->
        workbook.write(output)
        workbook.close()
    }

    Toast.makeText(context, "Excel file saved to: ${file.absolutePath}", Toast.LENGTH_LONG).show()
}

fun importFromExcel(context: Context, fileName: String): List<List<String>> {
    val file = File(context.getExternalFilesDir(null), "$fileName.xlsx")
    val result = mutableListOf<List<String>>()

    FileInputStream(file).use { inputStream ->
        val workbook = XSSFWorkbook(inputStream)
        val sheet = workbook.getSheetAt(0)
        for (row in sheet) {
            val rowData = mutableListOf<String>()
            for (cell in row) {
                rowData.add(cell.toString())
            }
            result.add(rowData)
        }
        workbook.close()
    }

    return result
}
