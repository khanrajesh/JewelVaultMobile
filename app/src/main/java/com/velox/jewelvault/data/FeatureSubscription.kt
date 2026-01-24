package com.velox.jewelvault.data

data class FeatureListState(
    val features: Map<String, Boolean> = emptyMap(),
    val lastUpdated: Long = 0L
)

data class SubscriptionState(
    val plan: String = "",
    val isActive: Boolean = false,
    val startAt: Long = 0L,
    val endAt: Long = 0L,
    val lastUpdated: Long = 0L
)

object FeatureDefaults {
    // TODO: Update this list to the exact feature keys you decided.
    val defaultFeatureKeys = listOf(
        "ble_device",
        "user_management",
        "khata_book",
        "draft_invoice",
        "audit",
        "inventory_import_item",
        "inventory_export_item",
        "quick_cam",
        "quick_cam_delete",
        "back_up_offline",
        "back_up_online",
        "custom_bill_offline",
        "custom_bill_online",
        "multi_store",
        "sync_enable"
    )

    fun defaultFeatureMap(): Map<String, Boolean> {
        return defaultFeatureKeys.associateWith { true }
    }
}
