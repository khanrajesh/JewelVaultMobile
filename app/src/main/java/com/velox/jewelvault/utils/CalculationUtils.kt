package com.velox.jewelvault.utils

import com.velox.jewelvault.data.MetalRate
import com.velox.jewelvault.data.roomdb.dto.ItemSelectedModel

/**
 * Utility class for all calculation operations in the JewelVault project.
 * Consolidates calculation logic that was previously scattered across multiple files.
 */
object CalculationUtils {

    /**
     * Calculate the unit price for a metal based on current metal rates
     * @param metalName The metal name (e.g., "Gold", "Silver")
     * @param metalRates List of current metal rates
     * @return Unit price per gram, or null if rate not found
     */
    fun metalUnitPrice(metalName: String, metalRates: List<MetalRate>): Double? {
        return when (metalName.trim().lowercase()) {
            "gold" -> {
                val price24k = metalRates.firstOrNull { 
                    it.metal == "Gold" && it.caratOrPurity == "24K" 
                }?.price?.toDoubleOrNull()
                
                if (price24k != null) {
                    // Gold calculation: (100 / 99.9) * price24k for 100% purity
                    (100.0 / 99.9) * price24k
                } else {
                    // Fallback to any gold rate
                    metalRates.firstOrNull { 
                        it.metal == "Gold" 
                    }?.price?.toDoubleOrNull()
                }
            }
            "silver" -> {
                metalRates.firstOrNull { 
                    it.metal == "Silver" && it.caratOrPurity == "Silver /g" 
                }?.price?.toDoubleOrNull()
                    ?: metalRates.firstOrNull { 
                        it.metal == "Silver" 
                    }?.price?.toDoubleOrNull()
            }
            else -> null
        }
    }

    /**
     * Calculate the base price for an item based on fine weight and metal unit price
     * @param fineWeight Fine weight in grams
     * @param unitPrice Unit price per gram
     * @return Base price before charges and taxes
     */
    fun basePrice(fineWeight: Double, unitPrice: Double): Double {
        return fineWeight * unitPrice
    }

    /**
     * Calculate making charges based on charge type and parameters
     * @param chargeType Type of charge (Percentage, Piece, PerGm)
     * @param chargeRate The charge rate value
     * @param basePrice Base price for percentage calculations
     * @param quantity Quantity for piece-based charges
     * @param weight Fine weight for per-gram charges
     * @return Calculated making charge amount
     */
    fun makingCharge(
        chargeType: String,
        chargeRate: Double,
        basePrice: Double = 0.0,
        quantity: Int = 1,
        weight: Double = 0.0
    ): Double {
        return when (chargeType) {
            ChargeType.Percentage.type -> basePrice * (chargeRate / 100.0)
            ChargeType.Piece.type -> chargeRate * quantity
            ChargeType.PerGm.type -> chargeRate * weight
            else -> 0.0
        }
    }

    /**
     * Calculate tax amount based on price and charge
     * @param basePrice Base price of the item
     * @param charge Making charge amount
     * @param cgstRate CGST rate percentage
     * @param sgstRate SGST rate percentage
     * @param igstRate IGST rate percentage
     * @return Total tax amount
     */
    fun calculateTax(
        basePrice: Double,
        charge: Double,
        cgstRate: Double = 0.0,
        sgstRate: Double = 0.0,
        igstRate: Double = 0.0
    ): Double {
        val taxableAmount = basePrice + charge
        val totalTaxRate = cgstRate + sgstRate + igstRate
        return taxableAmount * (totalTaxRate / 100.0)
    }

    /**
     * Calculate total price including all charges and taxes
     * @param basePrice Base price of the item
     * @param makingCharge Making charge amount
     * @param otherCharge Other charges amount
     * @param taxAmount Tax amount
     * @return Total price including all charges and taxes
     */
    fun totalPrice(
        basePrice: Double,
        makingCharge: Double,
        otherCharge: Double = 0.0,
        taxAmount: Double = 0.0
    ): Double {
        return basePrice + makingCharge + otherCharge + taxAmount
    }

    /**
     * Calculate fine weight from net weight using purity multiplier
     * @param netWeight Net weight in grams
     * @param purity Purity label (e.g., "916", "750")
     * @return Fine weight in grams
     */
    fun calculateFineWeight(netWeight: Double, purity: String): Double {
        val purityMultiplier = Purity.fromLabel(purity)?.multiplier ?: 1.0
        return netWeight * purityMultiplier
    }

    /**
     * Calculate summary totals for a list of items
     * @param items List of selected items
     * @return SummaryCalculationResult containing all totals
     */
    fun summaryTotals(items: List<ItemSelectedModel>): SummaryCalculationResult {
        if (items.isEmpty()) {
            return SummaryCalculationResult()
        }

        // Group items by metal type for weight summaries
        val groupedByMetal = items.groupBy { it.catName }
        
        val metalSummaries = groupedByMetal.map { (metalType, metalItems) ->
            MetalSummary(
                metalType = metalType,
                totalGrossWeight = metalItems.sumOf { it.gsWt },
                totalFineWeight = metalItems.sumOf { it.fnWt }
            )
        }

        // Calculate financial totals
        val totalBasePrice = items.sumOf { it.price }
        val totalMakingCharges = items.sumOf { it.chargeAmount }
        val totalOtherCharges = items.sumOf { it.othCrg }
        val totalTax = items.sumOf { it.tax }
        val totalPriceBeforeTax = totalBasePrice + totalMakingCharges + totalOtherCharges
        val grandTotal = totalPriceBeforeTax + totalTax

        return SummaryCalculationResult(
            metalSummaries = metalSummaries,
            totalBasePrice = totalBasePrice,
            totalMakingCharges = totalMakingCharges,
            totalOtherCharges = totalOtherCharges,
            totalTax = totalTax,
            totalPriceBeforeTax = totalPriceBeforeTax,
            grandTotal = grandTotal
        )
    }


    /**
     * Calculate price with separate charges view
     * @param item The item to calculate for
     * @param showSeparateCharges Whether to show charges separately
     * @return Price amount based on view mode
     */
    fun displayPrice(item: ItemSelectedModel, showSeparateCharges: Boolean): Double {
        return if (showSeparateCharges) {
            item.price
        } else {
            item.price + item.chargeAmount + item.othCrg
        }
    }

    /**
     * Validate metal rates availability
     * @param metalRates List of metal rates
     * @param requiredMetals List of required metals
     * @return Validation result with error message if any
     */
    fun validateMetalRates(metalRates: List<MetalRate>, requiredMetals: List<String> = listOf("Gold", "Silver")): MetalRatesValidationResult {
        if (metalRates.isEmpty()) {
            return MetalRatesValidationResult(
                isValid = false,
                errorMessage = "No metal rates available"
            )
        }

        val missingMetals = mutableListOf<String>()
        
        for (metal in requiredMetals) {
            val hasMetalRate = metalRates.any { 
                it.metal.equals(metal, ignoreCase = true) 
            }
            if (!hasMetalRate) {
                missingMetals.add(metal)
            }
        }

        return if (missingMetals.isEmpty()) {
            MetalRatesValidationResult(isValid = true)
        } else {
            MetalRatesValidationResult(
                isValid = false,
                errorMessage = "Missing rates for: ${missingMetals.joinToString(", ")}"
            )
        }
    }


    /**
     * Calculate total weight by metal type for exchanges
     * @param exchanges List of exchanges
     * @param metalType Metal type (e.g., "Gold", "Silver")
     * @return Total weight for the specified metal
     */
    fun calculateTotalExchangeWeightByMetal(
        exchanges: List<Any>,
        metalType: String
    ): Double {
        return exchanges.filter { 
            when (it) {
                is Map<*, *> -> it["catName"]?.toString()?.equals(metalType, ignoreCase = true) == true
                else -> false
            }
        }.sumOf { 
            when (it) {
                is Map<*, *> -> (it["fnWeight"] as? Number)?.toDouble() ?: 0.0
                else -> 0.0
            }
        }
    }
}

/**
 * Data class for summary calculation results
 */
data class SummaryCalculationResult(
    val metalSummaries: List<MetalSummary> = emptyList(),
    val totalBasePrice: Double = 0.0,
    val totalMakingCharges: Double = 0.0,
    val totalOtherCharges: Double = 0.0,
    val totalTax: Double = 0.0,
    val totalPriceBeforeTax: Double = 0.0,
    val grandTotal: Double = 0.0
)

/**
 * Data class for metal-specific summary
 */
data class MetalSummary(
    val metalType: String,
    val totalGrossWeight: Double,
    val totalFineWeight: Double
)

/**
 * Data class for metal rates validation result
 */
data class MetalRatesValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)
