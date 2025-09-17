package com.velox.jewelvault.utils

import com.velox.jewelvault.data.roomdb.entity.category.CategoryEntity
import com.velox.jewelvault.data.roomdb.entity.category.SubCategoryEntity
import kotlin.math.abs

object InputCorrectionUtils {
    
    data class CorrectionResult<T>(
        val original: String,
        val corrected: T?,
        val suggestion: String?,
        val confidence: Float // 0.0 to 1.0
    )
    
    // Common purity format variations and their corrections
    private val purityCorrections = mapOf(
        // Gold variations
        "24k" to "1000",
        "24K" to "1000", 
        "24 karat" to "1000",
        "24 karats" to "1000",
        "fine gold" to "1000",
        "pure gold" to "1000",
        
        "22k" to "916",
        "22K" to "916",
        "22 karat" to "916",
        "22 karats" to "916",
        
        "18k" to "750",
        "18K" to "750",
        "18 karat" to "750",
        "18 karats" to "750",
        
        "14k" to "585",
        "14K" to "585",
        "14 karat" to "585",
        "14 karats" to "585",
        
        // Silver variations
        "sterling" to "925S",
        "sterling silver" to "925S",
        "sterling s" to "925S",
        "silver" to "925S",
        
        "fine silver" to "999S",
        "pure silver" to "999S",
        
        // Common typos
        "999.5" to "999.5",
        "999,5" to "999.5",
        "999 5" to "999.5",
        "9995" to "999.5",
        
        "916.0" to "916",
        "750.0" to "750",
        "585.0" to "585"
    )
    
    // Common making charge type variations
    private val mcTypeCorrections = mapOf(
        "percent" to "%",
        "percentage" to "%",
        "pct" to "%",
        "pc" to "piece",
        "pieces" to "piece",
        "per piece" to "piece",
        "per gm" to "/gm",
        "per gram" to "/gm",
        "per g" to "/gm",
        "gram" to "/gm",
        "gm" to "/gm"
    )
    
    // Common entry type variations
    private val entryTypeCorrections = mapOf(
        "pieces" to "piece",
        "pcs" to "piece",
        "pc" to "piece",
        "lots" to "Lot",
        "lot" to "Lot"
    )
    
    /**
     * Corrects purity input with suggestions
     */
    fun correctPurity(input: String): CorrectionResult<String> {
        val trimmed = input.trim()
        
        // Check if already valid
        if (Purity.fromLabel(trimmed) != null) {
            return CorrectionResult(trimmed, null, null, 1.0f)
        }
        
        // Check for exact matches in corrections
        val exactMatch = purityCorrections[trimmed.lowercase()]
        if (exactMatch != null) {
            return CorrectionResult(
                input, 
                exactMatch, 
                "Did you mean '$exactMatch'?",
                0.9f
            )
        }
        
        // Find closest match using fuzzy matching
        val closestMatch = findClosestPurity(trimmed)
        if (closestMatch != null) {
            return CorrectionResult(
                input,
                closestMatch,
                "Did you mean '$closestMatch'?",
                0.7f
            )
        }
        
        return CorrectionResult(
            input,
            null,
            "Invalid purity. Valid options: ${Purity.list().take(10).joinToString(", ")}...",
            0.0f
        )
    }
    
    /**
     * Corrects making charge type input
     */
    fun correctMcType(input: String): CorrectionResult<String> {
        val trimmed = input.trim()
        
        // Check if already valid
        if (ChargeType.list().contains(trimmed)) {
            return CorrectionResult(trimmed, null, null, 1.0f)
        }
        
        // Check for exact matches in corrections
        val exactMatch = mcTypeCorrections[trimmed.lowercase()]
        if (exactMatch != null) {
            return CorrectionResult(
                input,
                exactMatch,
                "Did you mean '$exactMatch'?",
                0.9f
            )
        }
        
        return CorrectionResult(
            input,
            null,
            "Invalid making charge type. Valid options: ${ChargeType.list().joinToString(", ")}",
            0.0f
        )
    }
    
    /**
     * Corrects entry type input
     */
    fun correctEntryType(input: String): CorrectionResult<String> {
        val trimmed = input.trim()
        
        // Check if already valid
        if (EntryType.list().contains(trimmed)) {
            return CorrectionResult(trimmed, null, null, 1.0f)
        }
        
        // Check for exact matches in corrections
        val exactMatch = entryTypeCorrections[trimmed.lowercase()]
        if (exactMatch != null) {
            return CorrectionResult(
                input,
                exactMatch,
                "Did you mean '$exactMatch'?",
                0.9f
            )
        }
        
        return CorrectionResult(
            input,
            null,
            "Invalid entry type. Valid options: ${EntryType.list().joinToString(", ")}",
            0.0f
        )
    }
    
    /**
     * Finds closest category match using fuzzy matching
     */
    fun findClosestCategory(
        input: String, 
        categories: List<CategoryEntity>
    ): CorrectionResult<CategoryEntity>? {
        val trimmed = input.trim()
        
        // Exact match (case insensitive)
        val exactMatch = categories.find { 
            it.catName.equals(trimmed, ignoreCase = true) 
        }
        if (exactMatch != null) {
            return CorrectionResult(trimmed, exactMatch, null, 1.0f)
        }
        
        // Fuzzy matching
        val bestMatch = categories.maxByOrNull { category ->
            calculateSimilarity(trimmed.lowercase(), category.catName.lowercase())
        }
        
        if (bestMatch != null) {
            val similarity = calculateSimilarity(trimmed.lowercase(), bestMatch.catName.lowercase())
            if (similarity > 0.6f) { // Only suggest if similarity is above 60%
                return CorrectionResult<CategoryEntity>(
                    trimmed,
                    bestMatch,
                    "Did you mean '${bestMatch.catName}'?",
                    similarity
                )
            }
        }
        
        return null
    }
    
    /**
     * Finds closest subcategory match using fuzzy matching
     */
    fun findClosestSubCategory(
        input: String, 
        subCategories: List<SubCategoryEntity>,
        categoryId: String? = null
    ): CorrectionResult<SubCategoryEntity>? {
        val trimmed = input.trim()
        
        // Filter by category if provided
        val filteredSubCategories = if (categoryId != null) {
            subCategories.filter { it.catId == categoryId }
        } else {
            subCategories
        }
        
        // Exact match (case insensitive)
        val exactMatch = filteredSubCategories.find { 
            it.subCatName.equals(trimmed, ignoreCase = true) 
        }
        if (exactMatch != null) {
            return CorrectionResult<SubCategoryEntity>(trimmed, exactMatch, null, 1.0f)
        }
        
        // Fuzzy matching
        val bestMatch = filteredSubCategories.maxByOrNull { subCategory ->
            calculateSimilarity(trimmed.lowercase(), subCategory.subCatName.lowercase())
        }
        
        if (bestMatch != null) {
            val similarity = calculateSimilarity(trimmed.lowercase(), bestMatch.subCatName.lowercase())
            if (similarity > 0.6f) { // Only suggest if similarity is above 60%
                return CorrectionResult<SubCategoryEntity>(
                    trimmed,
                    bestMatch,
                    "Did you mean '${bestMatch.subCatName}'?",
                    similarity
                )
            }
        }
        
        return null
    }
    
    /**
     * Standardizes weight input format
     */
    fun standardizeWeight(input: String): CorrectionResult<String> {
        val trimmed = input.trim()
        
        // Remove common suffixes
        val cleaned = trimmed.replace(Regex("[gG]\\s*$"), "")
        
        // Handle comma as decimal separator
        val normalized = cleaned.replace(",", ".")
        
        // Validate format
        val weight = normalized.toDoubleOrNull()
        if (weight != null && weight >= 0) {
            val formatted = String.format("%.3f", weight)
            return if (formatted != trimmed) {
                CorrectionResult(
                    input,
                    formatted,
                    "Formatted as: $formatted",
                    0.8f
                )
            } else {
                CorrectionResult(trimmed, null, null, 1.0f)
            }
        }
        
        return CorrectionResult(
            input,
            null,
            "Invalid weight format. Use decimal numbers (e.g., 10.500)",
            0.0f
        )
    }
    
    /**
     * Standardizes quantity input
     */
    fun standardizeQuantity(input: String): CorrectionResult<Int> {
        val trimmed = input.trim()
        
        // Remove common suffixes
        val cleaned = trimmed.replace(Regex("[pP][cC][sS]?$"), "")
            .replace(Regex("[pP][iI][eE][cC][eE][sS]?$"), "")
            .replace(Regex("[nN][oO][sS]?$"), "")
        
        val quantity = cleaned.toIntOrNull()
        if (quantity != null && quantity > 0) {
            return CorrectionResult<Int>(
                input,
                quantity,
                if (cleaned != trimmed) "Parsed as: $quantity" else null,
                0.9f
            )
        }
        
        return CorrectionResult<Int>(
            input,
            null,
            "Invalid quantity. Use positive integers (e.g., 1, 2, 10)",
            0.0f
        )
    }
    
    /**
     * Finds closest purity match using fuzzy matching
     */
    private fun findClosestPurity(input: String): String? {
        val validPurities = Purity.list()
        val bestMatch = validPurities.maxByOrNull { purity ->
            calculateSimilarity(input.lowercase(), purity.lowercase())
        }
        
        return if (bestMatch != null && calculateSimilarity(input.lowercase(), bestMatch.lowercase()) > 0.7f) {
            bestMatch
        } else {
            null
        }
    }
    
    /**
     * Calculates similarity between two strings using Levenshtein distance
     */
    private fun calculateSimilarity(s1: String, s2: String): Float {
        val distance = levenshteinDistance(s1, s2)
        val maxLength = maxOf(s1.length, s2.length)
        return if (maxLength == 0) 1.0f else (maxLength - distance).toFloat() / maxLength
    }
    
    /**
     * Calculates Levenshtein distance between two strings
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val matrix = Array(s1.length + 1) { IntArray(s2.length + 1) }
        
        for (i in 0..s1.length) matrix[i][0] = i
        for (j in 0..s2.length) matrix[0][j] = j
        
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                matrix[i][j] = minOf(
                    matrix[i - 1][j] + 1,      // deletion
                    matrix[i][j - 1] + 1,      // insertion
                    matrix[i - 1][j - 1] + cost // substitution
                )
            }
        }
        
        return matrix[s1.length][s2.length]
    }
}
