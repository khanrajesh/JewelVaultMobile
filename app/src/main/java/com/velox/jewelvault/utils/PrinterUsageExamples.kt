package com.velox.jewelvault.utils

import android.content.Context
import com.velox.jewelvault.data.roomdb.entity.ItemEntity
import com.velox.jewelvault.data.bluetooth.LabelSizes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Usage examples for the Global Printer Manager
 * These examples show how to use the printer functions from anywhere in the app
 */

/**
 * Example 1: Print HTML content
 * This can be called from any Activity, Fragment, or ViewModel
 */
fun printHtmlExample(context: Context) {
    val htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
            <style>
                body { font-family: Arial, sans-serif; margin: 10px; }
                .header { text-align: center; font-weight: bold; }
            </style>
        </head>
        <body>
            <div class="header">Sample Print</div>
            <p>This is a test print from JewelVault Mobile App</p>
            <p>Date: ${System.currentTimeMillis()}</p>
        </body>
        </html>
    """.trimIndent()
    
    // Print using PrintUtils (recommended)
    PrintUtils.printHtmlToBluetoothPrinter(
        context = context,
        htmlContent = htmlContent,
        onComplete = {
            // Print successful
            log("Print completed successfully")
        },
        onError = { error ->
            // Print failed
            log("Print failed: $error")
        }
    )
}

/**
 * Example 2: Print item details
 * This replaces the direct print functionality
 */
fun printItemExample(context: Context, item: ItemEntity) {
    // Print item details using Bluetooth printer
    PrintUtils.printItemToBluetoothPrinter(
        context = context,
        item = item,
        onComplete = {
            // Print successful
            log("Item printed successfully")
        },
        onError = { error ->
            // Print failed
            log("Item print failed: $error")
        }
    )
}

/**
 * Example 3: Print thermal label
 * This replaces the thermal label print functionality
 */
fun printThermalLabelExample(context: Context, item: ItemEntity) {
    // Print thermal label using Bluetooth printer
    PrintUtils.printThermalLabelToBluetoothPrinter(
        context = context,
        item = item,
        storeLogoBase64 = null, // Optional store logo
        onComplete = {
            // Print successful
            log("Thermal label printed successfully")
        },
        onError = { error ->
            // Print failed
            log("Thermal label print failed: $error")
        }
    )
}

/**
 * Example 4: Check printer connection before printing
 * This shows how to check if printer is connected before attempting to print
 */
fun checkAndPrintExample(context: Context) {
    CoroutineScope(Dispatchers.IO).launch {
        // Check if printer is connected
        if (PrinterUtils.isConnected()) {
            val printerName = PrinterUtils.getConnectedPrinterName()
            log("Printer connected: $printerName")
            
            // Print something
            PrinterUtils.printTextAsync(
                text = "Hello from JewelVault!",
                onSuccess = {
                    log("Print successful")
                },
                onError = { error ->
                    log("Print failed: $error")
                }
            )
        } else {
            log("No printer connected. Please connect to a Bluetooth printer first.")
        }
    }
}

/**
 * Example 5: Print with custom label size
 * This shows how to print with different label sizes
 */
fun printWithCustomSizeExample(context: Context) {
    val htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
            <style>
                body { font-family: Arial, sans-serif; margin: 5px; font-size: 10px; }
                .header { text-align: center; font-weight: bold; font-size: 12px; }
            </style>
        </head>
        <body>
            <div class="header">Small Label</div>
            <p>This is a small label print</p>
        </body>
        </html>
    """.trimIndent()
    
    // Print with custom label size (3x2 inches)
    PrintUtils.printHtmlToBluetoothPrinter(
        context = context,
        htmlContent = htmlContent,
        onComplete = {
            log("Small label printed successfully")
        },
        onError = { error ->
            log("Small label print failed: $error")
        }
    )
}

/**
 * Example 6: Print from ViewModel
 * This shows how to use the printer from a ViewModel
 */
class ExampleViewModel {
    
    fun printFromViewModel(context: Context, item: ItemEntity) {
        // Check printer connection
        CoroutineScope(Dispatchers.IO).launch {
            if (PrinterUtils.isConnected()) {
                // Print item details
                PrinterUtils.printHtmlAsync(
                    htmlContent = generateItemHtml(item),
                    onSuccess = {
                        log("Item printed from ViewModel")
                    },
                    onError = { error ->
                        log("Print failed from ViewModel: $error")
                    }
                )
            } else {
                log("No printer connected in ViewModel")
            }
        }
    }
    
    private fun generateItemHtml(item: ItemEntity): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; margin: 10px; }
                    .header { text-align: center; font-weight: bold; }
                    .item-info { margin: 5px 0; }
                </style>
            </head>
            <body>
                <div class="header">Item Details</div>
                <div class="item-info">ID: ${item.itemId}</div>
                <div class="item-info">Name: ${item.itemAddName}</div>
                <div class="item-info">Weight: ${item.gsWt.to3FString()} gm</div>
                <div class="item-info">Purity: ${item.purity}</div>
            </body>
            </html>
        """.trimIndent()
    }
}

/**
 * Example 7: Print from Activity
 * This shows how to use the printer from an Activity
 */
class ExampleActivity {
    
    fun printFromActivity(context: Context) {
        // Simple text print
        PrintUtils.printHtmlToBluetoothPrinter(
            context = context,
            htmlContent = "<html><body><h1>Hello from Activity!</h1></body></html>",
            onComplete = {
                // Show success message
                log("Print completed from Activity")
            },
            onError = { error ->
                // Show error message
                log("Print failed from Activity: $error")
            }
        )
    }
}

/**
 * Example 8: Print from Fragment
 * This shows how to use the printer from a Fragment
 */
class ExampleFragment {
    
    fun printFromFragment(context: Context, item: ItemEntity) {
        // Print thermal label
        PrintUtils.printThermalLabelToBluetoothPrinter(
            context = context,
            item = item,
            onComplete = {
                // Update UI or show success message
                log("Thermal label printed from Fragment")
            },
            onError = { error ->
                // Show error dialog or toast
                log("Thermal label print failed from Fragment: $error")
            }
        )
    }
}

/**
 * Example 9: Batch printing
 * This shows how to print multiple items
 */
fun batchPrintExample(context: Context, items: List<ItemEntity>) {
    items.forEachIndexed { index, item ->
        // Add delay between prints to avoid overwhelming the printer
        CoroutineScope(Dispatchers.IO).launch {
            kotlinx.coroutines.delay(index * 1000L) // 1 second delay between prints
            
            PrintUtils.printItemToBluetoothPrinter(
                context = context,
                item = item,
                onComplete = {
                    log("Item ${index + 1} printed successfully")
                },
                onError = { error ->
                    log("Item ${index + 1} print failed: $error")
                }
            )
        }
    }
}

/**
 * Example 10: Error handling
 * This shows comprehensive error handling
 */
fun printWithErrorHandlingExample(context: Context, item: ItemEntity) {
    PrintUtils.printItemToBluetoothPrinter(
        context = context,
        item = item,
        onComplete = {
            // Success - update UI, show success message, etc.
            log("Print completed successfully")
            // You can update UI here or show a success toast
        },
        onError = { error ->
            // Error - handle different error types
            when {
                error.contains("not connected") -> {
                    // Show dialog to connect printer
                    log("Please connect to a Bluetooth printer first")
                }
                error.contains("Print failed") -> {
                    // Show retry option
                    log("Print failed, please try again")
                }
                else -> {
                    // Show generic error message
                    log("Print error: $error")
                }
            }
        }
    )
}
