package com.velox.jewelvault.data.model.pdf

object PdfTemplateType {
    const val INVOICE = "INVOICE"
    const val DRAFT_INVOICE = "DRAFT_INVOICE"
    const val RECEIPT = "RECEIPT"
    const val KHATA_BOOK = "KHATA_BOOK"

    val all = listOf(INVOICE, DRAFT_INVOICE, RECEIPT, KHATA_BOOK)

    fun displayName(type: String): String = when (type) {
        INVOICE -> "Invoice"
        DRAFT_INVOICE -> "Draft Invoice"
        RECEIPT -> "Receipt"
        KHATA_BOOK -> "Khata Book"
        else -> type
    }
}

object PdfTemplateStatus {
    const val DRAFT = "DRAFT"
    const val PUBLISHED = "PUBLISHED"
}
