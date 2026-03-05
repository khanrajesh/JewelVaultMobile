package com.velox.jewelvault.utils

/**
 * Compact QR payload for inventory items. Uses CSV with a version prefix:
 * JV1|id,catId,subCatId,gs,fn,nt,pur,mct,mc[,catName,subCatName]
 *
 * - Numeric fields are optional but should be parsable Double when present.
 * - catName/subCatName are optional tail fields to aid UI if ids are missing.
 * - Payload stays short enough for 10x10mm @203dpi QR with error correction L/M.
 */
data class QrItemPayload(
    val id: String,
    val catName: String?,
    val subCatName: String?,
    val gs: Double?,
    val fn: Double?,
    val nt: Double?,
    val purity: String?,
    val mcType: String?,
    val mc: Double?,
)

private const val QR_PAYLOAD_PREFIX = ""
private const val QR_PAYLOAD_PREFIX_V1 = "JV1|"
private val CONTROL_SUFFIX_REGEX = Regex("[\\r\\n\\u0000]+$")
private val CONTROL_PREFIX_REGEX = Regex("^[\\u0000\\u0002\\u0003]+")

/**
 * Normalize scanner output by trimming control prefixes/suffixes and whitespace.
 * Handles common HID scanner suffixes like CR/LF.
 */
fun normalizeScannedCode(raw: String): String {
    return raw
        .trim()
        .replace(CONTROL_PREFIX_REGEX, "")
        .replace(CONTROL_SUFFIX_REGEX, "")
        .trim()
}

/**
 * Extract a candidate item id from scanner output.
 * Prefers JV payload id when available, otherwise uses normalized raw value.
 */
fun extractScannedItemId(raw: String): String {
    val normalized = normalizeScannedCode(raw)
    if (normalized.isBlank()) return ""
    return parseQrItemPayload(normalized)?.id ?: normalized
}

/**
 * Serialize to versioned CSV. Empty/unknown values become blank segments to keep positions stable.
 */
fun QrItemPayload.toCsvString(): String {
    fun Double?.fmt(): String = this?.let { String.format("%.3f", it) } ?: ""
    val parts = listOf(
        id.trim(),
        catName.orEmpty().trim(),
        subCatName.orEmpty().trim(),
        gs.fmt(),
        fn.fmt(),
        nt.fmt(),
        purity.orEmpty().trim(),
        mcType.orEmpty().trim().lowercase(), // keep short codes (pc/pgm/pct)
        mc.fmt()
    )
    // Trim trailing empty fields to keep string short
    val trimmed = parts.toMutableList()
    while (trimmed.isNotEmpty() && trimmed.last().isEmpty()) {
        trimmed.removeAt(trimmed.lastIndex)
    }
    return QR_PAYLOAD_PREFIX + trimmed.joinToString(separator = ",")
}

/**
 * Attempt to parse a QR payload. Returns null for unknown formats or validation failures.
 */
fun parseQrItemPayload(raw: String): QrItemPayload? {
    val normalized = normalizeScannedCode(raw)
    val body = when {
        normalized.startsWith(QR_PAYLOAD_PREFIX_V1) -> normalized.removePrefix(QR_PAYLOAD_PREFIX_V1)
        else -> normalized
    }
    val tokens = body.split(',')
    if (tokens.isEmpty()) return null

    fun parseDouble(token: String): Double? =
        token.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()

    return try {
        QrItemPayload(
            id = tokens.getOrNull(0)?.trim().orEmpty(),
            catName = tokens.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() },
            subCatName = tokens.getOrNull(2)?.trim()?.takeIf { it.isNotEmpty() },
            gs = parseDouble(tokens.getOrNull(3) ?: ""),
            fn = parseDouble(tokens.getOrNull(4) ?: ""),
            nt = parseDouble(tokens.getOrNull(5) ?: ""),
            purity = tokens.getOrNull(6)?.trim()?.takeIf { it.isNotEmpty() },
            mcType = tokens.getOrNull(7)?.trim()?.takeIf { it.isNotEmpty() },
            mc = parseDouble(tokens.getOrNull(8) ?: "")
        ).takeIf { it.id.isNotEmpty() }
    } catch (_: Exception) {
        null
    }
}

/**
 * Build a payload from an ItemEntity. Keeps values minimal and consistent with the QR schema.
 */
fun com.velox.jewelvault.data.roomdb.entity.ItemEntity.toQrItemPayload(): QrItemPayload =
    QrItemPayload(
        id = itemId,
        catName = catName,
        subCatName = subCatName,
        gs = gsWt,
        fn = fnWt,
        nt = ntWt,
        purity = purity,
        mcType = crgType,
        mc = crg
    )
