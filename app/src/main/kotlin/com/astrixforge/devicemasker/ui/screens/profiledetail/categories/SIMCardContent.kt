package com.astrixforge.devicemasker.ui.screens.profiledetail.categories

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.astrixforge.devicemasker.common.SpoofProfile
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.common.models.Carrier
import com.astrixforge.devicemasker.common.models.Country
import com.astrixforge.devicemasker.ui.components.dialog.CountryPickerDialog
import com.astrixforge.devicemasker.ui.components.expressive.ExpressiveCard
import com.astrixforge.devicemasker.ui.components.expressive.ExpressiveOutlinedCard
import com.astrixforge.devicemasker.ui.screens.profiledetail.items.IndependentSpoofItem
import com.astrixforge.devicemasker.ui.screens.profiledetail.items.ReadOnlyValueRow

/**
 * Content layout for SIM Card category with carrier-driven flow.
 *
 * UI Structure:
 * 1. Carrier Selection Card - Country picker + Carrier dropdown
 * 2. Locked Values Section - Read-only derived values (no switch/regenerate)
 * 3. Regeneratable Values Section - Phone, IMSI, ICCID with regenerate buttons
 *
 * Long-press on any value to copy.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SIMCardCategoryContent(
    profile: SpoofProfile?,
    onToggle: (SpoofType, Boolean) -> Unit,
    onRegenerate: (SpoofType) -> Unit,
    onCarrierChange: (Carrier) -> Unit,
    onCopy: (String) -> Unit,
) {
    // State for country picker dialog
    var showCountryPicker by remember { mutableStateOf(false) }

    // Get current carrier from profile
    val currentCarrierMccMnc = profile?.selectedCarrierMccMnc
    val currentCarrier = remember(currentCarrierMccMnc) {
        currentCarrierMccMnc?.let { Carrier.getByMccMnc(it) }
    }

    // Selected country (from current carrier or default)
    var selectedCountryIso by remember(currentCarrier) {
        mutableStateOf(currentCarrier?.countryIso ?: "IN")
    }

    // Get carriers filtered by selected country
    val carriersForCountry = remember(selectedCountryIso) {
        Carrier.getByCountry(selectedCountryIso)
    }

    // Values from profile
    val phoneEnabled = profile?.isTypeEnabled(SpoofType.PHONE_NUMBER) ?: false
    val phoneValue = profile?.getValue(SpoofType.PHONE_NUMBER) ?: ""
    val imsiEnabled = profile?.isTypeEnabled(SpoofType.IMSI) ?: false
    val imsiValue = profile?.getValue(SpoofType.IMSI) ?: ""
    val iccidEnabled = profile?.isTypeEnabled(SpoofType.ICCID) ?: false
    val iccidValue = profile?.getValue(SpoofType.ICCID) ?: ""

    // Locked/derived values
    val simCountryValue = profile?.getValue(SpoofType.SIM_COUNTRY_ISO) ?: ""
    val networkCountryValue = profile?.getValue(SpoofType.NETWORK_COUNTRY_ISO) ?: ""
    val mccMncValue = profile?.getValue(SpoofType.CARRIER_MCC_MNC) ?: ""
    val carrierNameValue = profile?.getValue(SpoofType.CARRIER_NAME) ?: ""
    val simOperatorValue = profile?.getValue(SpoofType.SIM_OPERATOR_NAME) ?: ""
    val networkOperatorValue = profile?.getValue(SpoofType.NETWORK_OPERATOR) ?: ""

    // Country picker dialog
    if (showCountryPicker) {
        CountryPickerDialog(
            selectedCountryIso = selectedCountryIso,
            onCountrySelected = { country ->
                selectedCountryIso = country.iso
                showCountryPicker = false
                // Auto-select first carrier from new country
                val newCarriers = Carrier.getByCountry(country.iso)
                if (newCarriers.isNotEmpty()) {
                    onCarrierChange(newCarriers.first())
                }
            },
            onDismiss = { showCountryPicker = false }
        )
    }

    // ═══════════════════════════════════════════════════════════
    // 1. CARRIER SELECTION CARD
    // ═══════════════════════════════════════════════════════════
    ExpressiveCard(
        onClick = { /* Selection action feedback */ },
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Country picker row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Country",
                    style = MaterialTheme.typography.bodyMedium,
                )

                // Country button - opens dialog
                val selectedCountry = Country.getByIso(selectedCountryIso)
                ExpressiveOutlinedCard(
                    onClick = { showCountryPicker = true },
                    modifier = Modifier.width(200.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Centered: Flag + Country name
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${selectedCountry?.emoji ?: "🌍"} ${selectedCountry?.name ?: selectedCountryIso}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        // Right arrow indicator
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Carrier dropdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Carrier",
                    style = MaterialTheme.typography.bodyMedium,
                )

                var carrierDropdownExpanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = carrierDropdownExpanded,
                    onExpandedChange = { carrierDropdownExpanded = it },
                    modifier = Modifier.width(200.dp),
                ) {
                    // Rounded container matching Country picker style
                    ExpressiveOutlinedCard(
                        onClick = { carrierDropdownExpanded = true },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = currentCarrier?.displayName ?: "Select carrier",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = carrierDropdownExpanded)
                        }
                    }

                    ExposedDropdownMenu(
                        expanded = carrierDropdownExpanded,
                        onDismissRequest = { carrierDropdownExpanded = false },
                    ) {
                        carriersForCountry.forEach { carrier ->
                            DropdownMenuItem(
                                text = { Text(carrier.displayName) },
                                onClick = {
                                    onCarrierChange(carrier)
                                    carrierDropdownExpanded = false
                                },
                                leadingIcon = if (carrier.mccMnc == currentCarrierMccMnc) {
                                    { Icon(Icons.Filled.Check, null) }
                                } else null,
                            )
                        }
                    }
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 2. LOCKED VALUES (derived from carrier, no switch/regenerate)
    // ═══════════════════════════════════════════════════════════
    ExpressiveCard(
        onClick = { /* Info action feedback */ },
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Carrier Info",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ReadOnlyValueRow(label = "SIM Country", value = simCountryValue, onCopy = { onCopy(simCountryValue) })
            ReadOnlyValueRow(label = "Network Country", value = networkCountryValue, onCopy = { onCopy(networkCountryValue) })
            ReadOnlyValueRow(label = "MCC/MNC", value = mccMncValue, onCopy = { onCopy(mccMncValue) })
            ReadOnlyValueRow(label = "Carrier Name", value = carrierNameValue, onCopy = { onCopy(carrierNameValue) })
            ReadOnlyValueRow(label = "SIM Operator", value = simOperatorValue, onCopy = { onCopy(simOperatorValue) })
            ReadOnlyValueRow(label = "Network Operator", value = networkOperatorValue, onCopy = { onCopy(networkOperatorValue) })
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 3. REGENERATABLE VALUES (Phone, IMSI, ICCID)
    // ═══════════════════════════════════════════════════════════

    // Phone Number
    IndependentSpoofItem(
        type = SpoofType.PHONE_NUMBER,
        value = phoneValue,
        isEnabled = phoneEnabled,
        onToggle = { enabled -> onToggle(SpoofType.PHONE_NUMBER, enabled) },
        onRegenerate = { onRegenerate(SpoofType.PHONE_NUMBER) },
        onCopy = { onCopy(phoneValue) },
        modifier = Modifier.fillMaxWidth()
    )

    // IMSI
    IndependentSpoofItem(
        type = SpoofType.IMSI,
        value = imsiValue,
        isEnabled = imsiEnabled,
        onToggle = { enabled -> onToggle(SpoofType.IMSI, enabled) },
        onRegenerate = { onRegenerate(SpoofType.IMSI) },
        onCopy = { onCopy(imsiValue) },
        modifier = Modifier.fillMaxWidth()
    )

    // ICCID
    IndependentSpoofItem(
        type = SpoofType.ICCID,
        value = iccidValue,
        isEnabled = iccidEnabled,
        onToggle = { enabled -> onToggle(SpoofType.ICCID, enabled) },
        onRegenerate = { onRegenerate(SpoofType.ICCID) },
        onCopy = { onCopy(iccidValue) },
        modifier = Modifier.fillMaxWidth()
    )
}
