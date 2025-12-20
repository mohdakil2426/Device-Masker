package com.astrixforge.devicemasker.common.generators

import java.security.SecureRandom

/**
 * ICCID (Integrated Circuit Card Identifier) Generator.
 *
 * ICCID Format (19-20 digits with Luhn checksum):
 * - Major Industry Identifier: 2 digits (89 for telecom)
 * - Country Code: 1-3 digits
 * - Issuer Identifier: 1-4 digits
 * - Account Identification: 6-12 digits
 * - Check Digit: 1 digit (Luhn algorithm)
 *
 * Standard format: 89 (telecom) + CC + Issuer + Account + Luhn
 */
object ICCIDGenerator {

    /**
     * Secure random instance for cryptographic-quality randomness.
     */
    private val secureRandom = SecureRandom()

    /**
     * Generates a valid ICCID with Luhn checksum.
     * Typical format: 89 + country (2) + issuer (2) + account (12) + check (1) = 19 digits
     *
     * @return A valid ICCID string (19 digits)
     */
    fun generate(): String {
        // Major Industry Identifier for telecom
        val mii = "89"
        
        // Country code (2 digits) - using common values
        val countryCode = String.format("%02d", secureRandom.nextInt(100))
        
        // Issuer identifier (2 digits)
        val issuer = String.format("%02d", secureRandom.nextInt(100))
        
        // Account identification (12 digits)
        val account = buildString {
            repeat(12) {
                append(secureRandom.nextInt(10))
            }
        }
        
        // Base ICCID without check digit (18 digits)
        val base = mii + countryCode + issuer + account
        
        // Calculate and append Luhn check digit
        val checkDigit = calculateLuhnCheckDigit(base)
        
        return base + checkDigit
    }

    /**
     * Calculates the Luhn check digit for an ICCID.
     *
     * @param partial The ICCID without check digit (18 digits)
     * @return The single check digit (0-9)
     */
    private fun calculateLuhnCheckDigit(partial: String): Int {
        var sum = 0
        
        // Process digits from right to left
        for (i in partial.indices) {
            var digit = partial[i].digitToInt()
            
            // Double every second digit from the right (odd positions from right)
            if ((partial.length - i) % 2 == 0) {
                digit *= 2
                if (digit > 9) {
                    digit -= 9
                }
            }
            
            sum += digit
        }
        
        return (10 - (sum % 10)) % 10
    }
}
