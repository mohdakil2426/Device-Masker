# Product Context: Device Masker

## Problem

Android apps can collect many identifiers and signals to build a device fingerprint:
- Telephony identifiers such as IMEI, IMSI, ICCID, carrier, and phone number.
- Android and Google identifiers such as Android ID, GSF ID, Advertising ID, and Media DRM ID.
- Network identifiers such as Wi-Fi/Bluetooth MAC, SSID, and BSSID.
- System profile values such as model, manufacturer, build fingerprint, and serial.
- Locale, timezone, location, and sensor metadata.
- Installed package and stack trace evidence that a hook framework is present.

Users and researchers need a controlled way to present consistent alternate identities to selected apps without modifying APKs.

## Product Shape

Device Masker has two user-visible parts:

1. The Android app
   - Configure the global module switch.
   - Create and edit spoof groups.
   - Assign apps to groups.
   - Regenerate or customize identifier values.
   - See module/framework health and diagnostics.

2. The LSPosed module
   - Loads in scoped target app processes.
   - Reads configuration through libxposed RemotePreferences.
   - Applies spoof hooks only when the app and spoof type are enabled.
   - Hides selected LSPosed/module/package traces.
   - Reports diagnostics to the system_server service.

## Expected User Flow

1. Install the APK.
2. Enable Device Masker in LSPosed.
3. Ensure required scope is available (`android`, `system`, and selected target apps as applicable).
4. Open Device Masker.
5. Create or select a spoof group.
6. Assign target apps to that group.
7. Enable and configure specific spoof types.
8. Restart target apps if needed.
9. Verify with diagnostics and external identifier-checking apps.

## UX Principles

- Make current status clear: module switch, LSPosed service connection, protected app count, and configured identifiers.
- Avoid false health claims. The app can show service connection and diagnostics availability, but a target app is only proven hooked after hook registration or spoof events appear.
- Keep configuration changes immediate and recoverable.
- Prefer coherent group/persona configuration over one-off values that break correlation.
- Make development-state limitations visible rather than pretending the module is stable.

## Correct Behavior

For each target app and spoof type:
- If the module is disabled, return the original value.
- If the app is not enabled in `AppConfig`, return the original value.
- If the assigned group is missing or disabled, return the original value.
- If the spoof type is disabled, blank, missing, or malformed, return the original value.
- If a valid stored value exists and the type is enabled, return the stored spoofed value.

## Primary Risk To Avoid

Target apps can get stuck during startup if they see inconsistent or changing identifiers. Therefore hooks must not generate random values in target processes. Values should be generated and persisted in app-side configuration, then read by hooks as stable stored values.

