package com.astrixforge.devicemasker.ui.screens.profiledetail.categories

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.astrixforge.devicemasker.common.DeviceProfilePreset
import com.astrixforge.devicemasker.common.SpoofProfile
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.ui.screens.profiledetail.items.IndependentSpoofItem

/**
 * Content layout for Device Hardware category.
 *
 * All 3 items are fully independent with their own Switch + Regenerate:
 * - Device Profile (shows preset name)
 * - IMEI
 * - Serial
 *
 * Long-press on values to copy.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DeviceHardwareCategoryContent(
    profile: SpoofProfile?,
    onToggle: (SpoofType, Boolean) -> Unit,
    onRegenerate: (SpoofType) -> Unit,
    @Suppress("UNUSED_PARAMETER")
    onRegenerateCategory: () -> Unit, // Not used - kept for interface compatibility
    onCopy: (String) -> Unit,
) {
    val deviceProfileEnabled = profile?.isTypeEnabled(SpoofType.DEVICE_PROFILE) ?: false
    val deviceProfileRawValue = profile?.getValue(SpoofType.DEVICE_PROFILE) ?: ""
    val deviceProfileDisplayValue = DeviceProfilePreset.findById(deviceProfileRawValue)?.name ?: deviceProfileRawValue
    val imeiEnabled = profile?.isTypeEnabled(SpoofType.IMEI) ?: false
    val imeiValue = profile?.getValue(SpoofType.IMEI) ?: ""
    val serialEnabled = profile?.isTypeEnabled(SpoofType.SERIAL) ?: false
    val serialValue = profile?.getValue(SpoofType.SERIAL) ?: ""

    // 1. Device Profile - independent (shows preset name instead of ID)
    IndependentSpoofItem(
        type = SpoofType.DEVICE_PROFILE,
        value = deviceProfileDisplayValue,
        isEnabled = deviceProfileEnabled,
        onToggle = { enabled -> onToggle(SpoofType.DEVICE_PROFILE, enabled) },
        onRegenerate = { onRegenerate(SpoofType.DEVICE_PROFILE) },
        onCopy = { onCopy(deviceProfileDisplayValue) },
        modifier = Modifier.fillMaxWidth()
    )

    // 2. IMEI - independent
    IndependentSpoofItem(
        type = SpoofType.IMEI,
        value = imeiValue,
        isEnabled = imeiEnabled,
        onToggle = { enabled -> onToggle(SpoofType.IMEI, enabled) },
        onRegenerate = { onRegenerate(SpoofType.IMEI) },
        onCopy = { onCopy(imeiValue) },
        modifier = Modifier.fillMaxWidth()
    )

    // 3. Serial - independent
    IndependentSpoofItem(
        type = SpoofType.SERIAL,
        value = serialValue,
        isEnabled = serialEnabled,
        onToggle = { enabled -> onToggle(SpoofType.SERIAL, enabled) },
        onRegenerate = { onRegenerate(SpoofType.SERIAL) },
        onCopy = { onCopy(serialValue) },
        modifier = Modifier.fillMaxWidth()
    )
}
