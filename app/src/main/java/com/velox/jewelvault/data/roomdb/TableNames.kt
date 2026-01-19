package com.velox.jewelvault.data.roomdb

/**
 * Centralized object containing all database table names.
 * This ensures consistency across entities and DAOs and makes it easy to change table names.
 */
object TableNames {
    
    // Core entities
    const val USERS = "users"
    const val USER_ADDITIONAL_INFO = "user_additional_info"
    const val STORE = "store"
    const val CATEGORY = "category"
    const val SUB_CATEGORY = "sub_category"
    const val ITEM = "item"
    
    // Customer related tables
    const val CUSTOMER = "customer"
    const val CUSTOMER_KHATA_BOOK = "customer_khata_book"
    const val CUSTOMER_TRANSACTION = "customer_transaction"
    
    // Order related tables
    const val ORDER = "order"
    const val ORDER_ITEM = "order_item"
    const val EXCHANGE_ITEM = "exchange_item"
    
    // Purchase related tables
    const val FIRM = "firm"
    const val SELLER = "seller"
    const val PURCHASE_ORDER = "purchase_order"
    const val PURCHASE_ORDER_ITEM = "purchase_order_item"
    const val METAL_EXCHANGE = "metal_exchange"
    
    // Printer related tables
    const val PRINTER = "printer"

    // Label designer related tables
    const val LABEL_TEMPLATE = "label_template"
    const val LABEL_ELEMENT = "label_element"

    // PDF template designer related tables
    const val PDF_TEMPLATE = "pdf_template"
    const val PDF_ELEMENT = "pdf_element"
}
