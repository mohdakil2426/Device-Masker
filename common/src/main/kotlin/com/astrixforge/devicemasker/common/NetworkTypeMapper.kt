package com.astrixforge.devicemasker.common

/**
 * Maps carrier MCC/MNC codes to expected network type constants.
 *
 * Spoofing the carrier name/MCC/MNC without also spoofing the network type is detectable: fraud
 * SDKs cross-reference the carrier against the expected technology. For example, Jio India (404-40)
 * on a 2025 device should return `NETWORK_TYPE_NR` (5G), not HSPA+ (15).
 *
 * The mapping uses the first 3 digits of MCC/MNC as a broad carrier prefix rather than exact PLMN
 * matches, providing good coverage without an exhaustive database.
 *
 * ## Network type constants (from `TelephonyManager`):
 * ```
 * NETWORK_TYPE_LTE = 13
 * NETWORK_TYPE_NR  = 20
 * ```
 *
 * @see [spec: network-type-mapping/spec.md]
 */
object NetworkTypeMapper {

    // TelephonyManager.NETWORK_TYPE_LTE = 13 (avoids depending on android framework in :common)
    const val NETWORK_TYPE_LTE: Int = 13

    // TelephonyManager.NETWORK_TYPE_NR = 20
    const val NETWORK_TYPE_NR: Int = 20

    /**
     * Maps carrying MCC/MNC to the expected network type constant.
     *
     * Supports both compact format `"40440"` (5-digit) and separated format `"404"+"40"`.
     *
     * Returns [NETWORK_TYPE_NR] for carriers known to have nationwide 5G coverage in 2025+. Returns
     * [NETWORK_TYPE_LTE] as a safe default for all others.
     *
     * @param mccMnc Combined MCC+MNC string (5 or 6 digits, no separator), e.g. `"31026"`,
     *   `"40440"`
     * @return `NETWORK_TYPE_NR` (20) or `NETWORK_TYPE_LTE` (13)
     */
    fun getForMccMnc(mccMnc: String): Int {
        if (mccMnc.length < 5) return NETWORK_TYPE_LTE

        // Use first 5 digits for matching (covers both 5- and 6-digit PLMNs)
        val plmn = mccMnc.take(5)
        val mcc = plmn.take(3)

        return when {
            // ── United States (MCC 310-316) — all major US carriers support 5G NR ────────
            mcc in US_MCC_RANGE -> NETWORK_TYPE_NR

            // ── United Kingdom ────────────────────────────────────────────────────────────
            // EE (23430), O2 (23410), Vodafone (23415), Three (23420) — all 5G capable
            plmn.startsWith("234") -> NETWORK_TYPE_NR

            // ── India ─────────────────────────────────────────────────────────────────────
            // Jio (40440/40430), Airtel (40410/40455), Vi (40420) — all launched 5G NR 2022+
            plmn.startsWith("404") || plmn.startsWith("405") -> NETWORK_TYPE_NR

            // ── China ─────────────────────────────────────────────────────────────────────
            // China Mobile (46000/46007), China Unicom (46001), China Telecom (46011) — 5G
            plmn.startsWith("460") -> NETWORK_TYPE_NR

            // ── European Union flagship carriers ──────────────────────────────────────────
            // DE (Telekom 26201, Vodafone 26202, O2 26207)
            plmn.startsWith("262") -> NETWORK_TYPE_NR
            // FR (Orange 20801, SFR 20810, Bouygues 20820, Free 20815)
            plmn.startsWith("208") -> NETWORK_TYPE_NR
            // IT (TIM 22201, Vodafone 22210, WindTre 22288)
            plmn.startsWith("222") -> NETWORK_TYPE_NR
            // ES (Movistar 21407, Vodafone 21401, Orange 21403)
            plmn.startsWith("214") -> NETWORK_TYPE_NR
            // NL (KPN 20408, T-Mobile NL 20416, Vodafone NL 20404)
            plmn.startsWith("204") -> NETWORK_TYPE_NR

            // ── East Asia ────────────────────────────────────────────────────────────────
            // South Korea (450xx) — SKT/KT/LG U+ all have nationwide 5G since 2019
            mcc == "450" -> NETWORK_TYPE_NR
            // Japan (440xx) — NTT Docomo/Softbank/au — full 5G coverage
            mcc == "440" -> NETWORK_TYPE_NR
            // Taiwan (466xx)
            mcc == "466" -> NETWORK_TYPE_NR
            // Hong Kong (454xx)
            mcc == "454" -> NETWORK_TYPE_NR

            // ── Middle East ───────────────────────────────────────────────────────────────
            // UAE (42402/42403/42401) — du/Etisalat have 5G since 2019
            plmn.startsWith("424") -> NETWORK_TYPE_NR
            // Saudi Arabia (42001/42007) — STC/Mobily 5G
            plmn.startsWith("420") -> NETWORK_TYPE_NR

            // ── SE Asia ───────────────────────────────────────────────────────────────────
            // Singapore (52505/52503/52501) — major carriers have 5G
            plmn.startsWith("525") -> NETWORK_TYPE_NR

            // ── Oceania ───────────────────────────────────────────────────────────────────
            // Australia (50501 Telstra, 50502 Optus, 50589 Vodafone AU) — 5G
            plmn.startsWith("505") -> NETWORK_TYPE_NR

            // ── Default: LTE (safe fallback for all other carriers) ───────────────────────
            else -> NETWORK_TYPE_LTE
        }
    }

    /** @return The network type string name — for logging / diagnostics. */
    fun getNetworkTypeName(networkType: Int): String =
        when (networkType) {
            NETWORK_TYPE_NR -> "NR (5G)"
            NETWORK_TYPE_LTE -> "LTE (4G)"
            else -> "UNKNOWN ($networkType)"
        }

    // ── US MCC ranges ─────────────────────────────────────────────────────────────────────────
    // MCC 310..316 are all allocated to the United States
    private val US_MCC_RANGE by lazy { (310..316).map { it.toString() }.toSet() }
}
