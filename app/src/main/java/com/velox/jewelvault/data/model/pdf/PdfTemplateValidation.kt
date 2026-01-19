package com.velox.jewelvault.data.model.pdf

import com.velox.jewelvault.data.roomdb.entity.pdf.PdfElementEntity
import org.json.JSONArray
import org.json.JSONObject

object PdfTemplateValidation {
    private val invoiceBindings = listOf(
        "storeInfo.name",
        "storeInfo.address",
        "storeInfo.proprietor",
        "storeInfo.phone",
        "storeInfo.email",
        "storeInfo.registrationNo",
        "storeInfo.gstinNo",
        "storeInfo.panNo",
        "customerInfo.name",
        "customerInfo.mobileNo",
        "customerInfo.address",
        "invoiceMeta.invoiceNumber",
        "invoiceMeta.date",
        "invoiceMeta.time",
        "invoiceMeta.salesMan",
        "invoiceMeta.documentType",
        "goldRate",
        "silverRate",
        "items.serialNumber",
        "items.productDescription",
        "items.quantitySet",
        "items.grossWeightGms",
        "items.netWeightGms",
        "items.ratePerGm",
        "items.makingAmount",
        "items.purityPercent",
        "items.metalType",
        "items.metalPrice",
        "items.totalAmount",
        "paymentSummary.subTotal",
        "paymentSummary.gstAmount",
        "paymentSummary.gstLabel",
        "paymentSummary.discount",
        "paymentSummary.cardCharges",
        "paymentSummary.totalAmountBeforeOldExchange",
        "paymentSummary.oldExchange",
        "paymentSummary.roundOff",
        "paymentSummary.netAmountPayable",
        "paymentSummary.amountInWords",
        "paymentReceived.cashLabel1",
        "paymentReceived.cashAmount1",
        "declarationPoints",
        "thankYouMessage",
        "customerSignature",
        "ownerSignature"
    )

    private val receiptBindings = listOf(
        "storeInfo.name",
        "storeInfo.address",
        "customerInfo.name",
        "customerInfo.mobileNo",
        "invoiceMeta.invoiceNumber",
        "invoiceMeta.date",
        "paymentSummary.netAmountPayable",
        "paymentReceived.cashAmount1"
    )

    private val khataBindings = listOf(
        "customerInfo.name",
        "customerInfo.mobileNo",
        "invoiceMeta.invoiceNumber",
        "invoiceMeta.date",
        "paymentSummary.netAmountPayable",
        "paymentReceived.cashAmount1"
    )

    fun requiredBindings(templateType: String): List<String> = when (templateType) {
        PdfTemplateType.INVOICE, PdfTemplateType.DRAFT_INVOICE -> invoiceBindings
        PdfTemplateType.RECEIPT -> receiptBindings
        PdfTemplateType.KHATA_BOOK -> khataBindings
        else -> emptyList()
    }

    fun missingBindings(templateType: String, elements: List<PdfElementEntity>): List<String> {
        val collected = elements.flatMap { element ->
            val bindings = mutableSetOf<String>()
            element.dataBinding?.trim()?.takeIf { it.isNotEmpty() }?.let { bindings.add(it) }
            bindings.addAll(extractBindingsFromProperties(element.properties))
            bindings
        }.toSet()
        return requiredBindings(templateType).filterNot { it in collected }
    }

    private fun extractBindingsFromProperties(properties: String): Set<String> {
        if (properties.isBlank()) return emptySet()
        return runCatching {
            val root = JSONObject(properties)
            val bindings = mutableSetOf<String>()
            root.optString("binding").takeIf { it.isNotBlank() }?.let { bindings.add(it) }
            collectArrayBindings(root.optJSONArray("bindings"), bindings)
            val columns = root.optJSONArray("columns")
            if (columns != null) {
                for (i in 0 until columns.length()) {
                    val col = columns.optJSONObject(i) ?: continue
                    col.optString("binding").takeIf { it.isNotBlank() }?.let { bindings.add(it) }
                    collectArrayBindings(col.optJSONArray("bindings"), bindings)
                }
            }
            bindings
        }.getOrDefault(emptySet())
    }

    private fun collectArrayBindings(array: JSONArray?, bindings: MutableSet<String>) {
        if (array == null) return
        for (i in 0 until array.length()) {
            val value = array.optString(i)
            if (value.isNotBlank()) {
                bindings.add(value)
            }
        }
    }
}
