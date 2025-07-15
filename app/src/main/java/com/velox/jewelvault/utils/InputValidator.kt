package com.velox.jewelvault.utils

import java.util.regex.Pattern

object InputValidator {
    
    // Phone number validation (Indian format)
    fun isValidPhoneNumber(phone: String): Boolean {
        val phonePattern = Pattern.compile("^[+]?[0-9]{10,13}$")
        return phonePattern.matcher(phone.trim()).matches()
    }
    
    // Email validation
    fun isValidEmail(email: String): Boolean {
        if (email.isBlank()) return false // Required field
        val emailPattern = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        )
        return emailPattern.matcher(email.trim()).matches()
    }
    
    // PIN validation (4-6 digits)
    fun isValidPin(pin: String): Boolean {
        val pinPattern = Pattern.compile("^[0-9]{4,6}$")
        return pinPattern.matcher(pin.trim()).matches()
    }
    
    // Numeric validation
    fun isValidNumeric(value: String): Boolean {
        return value.trim().matches(Regex("^\\d*\\.?\\d*$"))
    }
    
    // Text sanitization (remove special characters that could cause issues)
    fun sanitizeText(text: String): String {
        return text.trim()
            .replace(Regex("[<>\"']"), "") // Remove potential XSS characters
            .replace(Regex("\\s+"), " ") // Normalize whitespace
    }
    
    // GSTIN validation
    fun isValidGSTIN(gstin: String): Boolean {
        if (gstin.isBlank()) return false // Required field
        val gstinPattern = Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$")
        return gstinPattern.matcher(gstin.trim().uppercase()).matches()
    }
    
    // PAN validation
    fun isValidPAN(pan: String): Boolean {
        if (pan.isBlank()) return false // Required field
        val panPattern = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]{1}$")
        return panPattern.matcher(pan.trim().uppercase()).matches()
    }
    
    // HUID validation (Hallmark Unique ID)
    fun isValidHUID(huid: String): Boolean {
        if (huid.isBlank()) return true // Optional field
        val huidPattern = Pattern.compile("^[A-Z0-9]{6}$")
        return huidPattern.matcher(huid.trim().uppercase()).matches()
    }
    
    // Amount validation (positive decimal)
    fun isValidAmount(amount: String): Boolean {
        return amount.trim().matches(Regex("^\\d+\\.?\\d{0,2}$")) && 
               amount.trim().toDoubleOrNull()?.let { it > 0 } == true
    }
    
    // Quantity validation (positive integer)
    fun isValidQuantity(quantity: String): Boolean {
        return quantity.trim().matches(Regex("^\\d+$")) && 
               quantity.trim().toIntOrNull()?.let { it > 0 } == true
    }
    
    // Weight validation (positive decimal)
    fun isValidWeight(weight: String): Boolean {
        return weight.trim().matches(Regex("^\\d+\\.?\\d{0,3}$")) && 
               weight.trim().toDoubleOrNull()?.let { it > 0 } == true
    }
} 