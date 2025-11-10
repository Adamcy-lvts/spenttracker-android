package com.example.spenttracker.util

/**
 * Phone Number Validator for Nigerian Phone Numbers
 * Supports various Nigerian mobile network formats
 */
object PhoneNumberValidator {
    
    // Nigerian mobile number pattern: 0[7-9]XXXXXXXXX (11 digits total)
    // Covers all mobile networks without maintaining specific prefix lists
    
    /**
     * Validates Nigerian phone number format
     * Supports formats: 08012345678, +2348012345678, 2348012345678
     */
    fun isValidNigerianPhoneNumber(phoneNumber: String): Boolean {
        if (phoneNumber.isBlank()) return false
        
        // Clean the phone number (remove spaces, dashes, etc.)
        val cleanedNumber = phoneNumber.replace(Regex("[\\s\\-()]"), "")
        
        return when {
            // Format: +2348012345678 (international with +)
            cleanedNumber.startsWith("+234") -> {
                val localPart = "0" + cleanedNumber.substring(4)
                validateLocalFormat(localPart)
            }
            
            // Format: 2348012345678 (international without +)
            cleanedNumber.startsWith("234") && cleanedNumber.length == 13 -> {
                val localPart = "0" + cleanedNumber.substring(3)
                validateLocalFormat(localPart)
            }
            
            // Format: 08012345678 (local format)
            cleanedNumber.startsWith("0") -> {
                validateLocalFormat(cleanedNumber)
            }
            
            // Format: 8012345678 (local without leading 0)
            cleanedNumber.length == 10 && cleanedNumber.all { it.isDigit() } -> {
                val localPart = "0$cleanedNumber"
                validateLocalFormat(localPart)
            }
            
            else -> false
        }
    }
    
    /**
     * Validates local format phone number (08012345678)
     */
    private fun validateLocalFormat(phoneNumber: String): Boolean {
        // Must be exactly 11 digits starting with 0
        if (phoneNumber.length != 11 || !phoneNumber.startsWith("0")) {
            return false
        }
        
        // Must contain only digits
        if (!phoneNumber.all { it.isDigit() }) {
            return false
        }
        
        // Second digit must be 7, 8, or 9 (Nigerian mobile number ranges)
        val secondDigit = phoneNumber[1]
        if (secondDigit !in '7'..'9') {
            return false
        }
        
        return true
    }
    
    /**
     * Formats phone number to standard Nigerian format (08012345678)
     */
    fun formatToNigerianLocal(phoneNumber: String): String? {
        if (!isValidNigerianPhoneNumber(phoneNumber)) return null
        
        val cleanedNumber = phoneNumber.replace(Regex("[\\s\\-()]"), "")
        
        return when {
            cleanedNumber.startsWith("+234") -> {
                "0" + cleanedNumber.substring(4)
            }
            cleanedNumber.startsWith("234") && cleanedNumber.length == 13 -> {
                "0" + cleanedNumber.substring(3)
            }
            cleanedNumber.startsWith("0") && cleanedNumber.length == 11 -> {
                cleanedNumber
            }
            cleanedNumber.length == 10 && cleanedNumber.all { it.isDigit() } -> {
                "0$cleanedNumber"
            }
            else -> null
        }
    }
    
    /**
     * Formats phone number to international format (+2348012345678)
     */
    fun formatToInternational(phoneNumber: String): String? {
        val localFormat = formatToNigerianLocal(phoneNumber) ?: return null
        return "+234" + localFormat.substring(1)
    }
    
    /**
     * Get phone number validation error message
     */
    fun getValidationErrorMessage(phoneNumber: String): String? {
        if (phoneNumber.isBlank()) return null
        
        val cleanedNumber = phoneNumber.replace(Regex("[\\s\\-()]"), "")
        
        return when {
            !cleanedNumber.all { it.isDigit() || it == '+' } -> 
                "Phone number can only contain digits and +"
                
            cleanedNumber.length < 10 -> 
                "Phone number is too short"
                
            cleanedNumber.length > 14 -> 
                "Phone number is too long"
                
            !isValidNigerianPhoneNumber(phoneNumber) -> 
                "Please enter a valid Nigerian phone number (e.g., 08012345678)"
                
            else -> null
        }
    }
    
    /**
     * Detect the network provider from phone number (simplified)
     */
    fun getNetworkProvider(phoneNumber: String): String? {
        val localFormat = formatToNigerianLocal(phoneNumber) ?: return null
        
        // Since we're using pattern-based validation, we can't reliably detect
        // specific networks without maintaining prefix lists
        return when (localFormat[1]) {
            '7' -> "Mobile Network (07xx)"
            '8' -> "Mobile Network (08xx)"
            '9' -> "Mobile Network (09xx)"
            else -> "Unknown"
        }
    }
}