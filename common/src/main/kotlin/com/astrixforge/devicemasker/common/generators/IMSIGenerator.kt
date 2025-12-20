package com.astrixforge.devicemasker.common.generators

import java.security.SecureRandom

/**
 * IMSI (International Mobile Subscriber Identity) Generator.
 *
 * IMSI Format (15 digits):
 * - MCC (Mobile Country Code): 3 digits
 * - MNC (Mobile Network Code): 2-3 digits  
 * - MSIN (Mobile Subscription Identification Number): 9-10 digits
 *
 * This generator uses realistic MCC/MNC combinations from major carriers worldwide.
 */
object IMSIGenerator {

    /**
     * Secure random instance for cryptographic-quality randomness.
     */
    private val secureRandom = SecureRandom()

    /**
     * Valid MCC+MNC combinations from major carriers worldwide.
     * Format: "MCCMNC" (5-6 digits total)
     */
    private val VALID_MCC_MNC = listOf(
        // United States
        "310260", // T-Mobile US
        "311480", // Verizon US
        "310410", // AT&T US  
        "310120", // Sprint
        "310004", // Verizon
        "310028", // AT&T Mobility
        
        // Canada
        "302720", // Rogers
        "302220", // Telus
        "302610", // Bell
        
        // United Kingdom
        "234150", // Vodafone UK
        "234100", // EE (Everything Everywhere)
        "234200", // O2 UK
        "234303", // Three UK
        
        // Germany
        "262010", // T-Mobile Germany
        "262020", // Vodafone Germany
        "262030", // O2 Germany
        
        // France
        "208010", // SFR
        "208200", // Orange France
        "208030", // F-Bouygues
        
        // India
        "404450", // Airtel
        "404400", // Vodafone India
        "404900", // Reliance Jio
        
        // China
        "460000", // China Mobile
        "460010", // China Unicom
        "460030", // China Telecom
        
        // Japan
        "440100", // NTT DoCoMo
        "440200", // SoftBank
        "440300", // KDDI
        
        // Australia
        "505010", // Telstra
        "505020", // Optus
        "505030", // Vodafone AU
    )

    /**
     * Generates a valid 15-digit IMSI number with realistic MCC/MNC.
     *
     * @return A valid IMSI string (15 digits)
     */
    fun generate(): String {
        // Select a random valid MCC+MNC
        val mccMnc = VALID_MCC_MNC[secureRandom.nextInt(VALID_MCC_MNC.size)]
        
        // Calculate MSIN length to complete 15 digits
        val msinLength = 15 - mccMnc.length
        
        // Generate MSIN (remaining digits)
        val msin = buildString {
            repeat(msinLength) {
                append(secureRandom.nextInt(10))
            }
        }
        
        return mccMnc + msin
    }
}
