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
    val body = when {
        raw.startsWith(QR_PAYLOAD_PREFIX_V1) -> raw.removePrefix(QR_PAYLOAD_PREFIX_V1)
        raw.startsWith(QR_PAYLOAD_PREFIX) -> raw.removePrefix(QR_PAYLOAD_PREFIX)
        else -> raw
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
