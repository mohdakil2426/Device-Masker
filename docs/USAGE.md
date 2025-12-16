# 📖 Device Masker Usage Guide

This guide provides a detailed walkthrough of using Device Masker to protect your privacy.

---

## 🚀 First Launch

### Step 1: Verify Module Status

When you first open Device Masker, the **Home** screen shows:

1. **Module Status** - Should show "ACTIVE" if LSPosed is working
2. **Protected Apps** - Number of apps with spoofing enabled
3. **Masked Identifiers** - Number of active spoof values

> ⚠️ If module shows "INACTIVE", ensure:
> - LSPosed is installed and working
> - Device Masker is enabled in LSPosed Manager
> - Device was rebooted after enabling

### Step 2: Select Target Apps

Navigate to **Apps** tab:

1. Browse or search for apps
2. Toggle the checkbox to enable spoofing
3. Use filter chips to show User/System/Enabled apps
4. Use "Select All" or "Clear All" for bulk actions

**Recommended apps to protect:**
- Social media (Instagram, Facebook, TikTok)
- Messaging (WhatsApp, Telegram)
- Games (especially ones with anti-cheat)
- Shopping apps
- Any app that tracks you

**Apps to AVOID:**
- ❌ Device Masker itself
- ❌ System Settings
- ❌ LSPosed Manager
- ❌ Magisk app

---

## ⚙️ Configuring Spoof Values

### Spoof Settings Screen

Navigate to **Spoof** tab to see all spoof categories:

#### 📱 Device Identifiers
| Value | Description | Format |
|-------|-------------|--------|
| IMEI | Mobile equipment identifier | 15 digits (Luhn valid) |
| MEID | CDMA device identifier | 14 hex chars |
| Serial | Hardware serial number | 8-16 alphanumeric |
| Android ID | Unique app identifier | 16 hex chars |

#### 📶 Network Identifiers
| Value | Description | Format |
|-------|-------------|--------|
| WiFi MAC | WiFi hardware address | XX:XX:XX:XX:XX:XX |
| Bluetooth MAC | BT hardware address | XX:XX:XX:XX:XX:XX |
| SSID | Connected network name | Text |
| BSSID | Router MAC address | XX:XX:XX:XX:XX:XX |

#### 📣 Advertising IDs
| Value | Description | Format |
|-------|-------------|--------|
| Advertising ID | Google Ad tracking ID | UUID |
| GSF ID | Google Services ID | 16 hex chars |
| Media DRM ID | Widevine device ID | 16 hex chars |

#### 🏗️ System Properties
| Value | Description | Example |
|-------|-------------|---------|
| Manufacturer | Device maker | "Google" |
| Model | Device model | "Pixel 9 Pro" |
| Brand | Device brand | "google" |
| Fingerprint | Full build string | google/husky/... |

### Actions Per Value

Each value card has three action buttons:

1. **🔄 Regenerate** - Generate new random value
2. **✏️ Edit** - Enter custom value manually
3. **📋 Copy** - Copy value to clipboard

### Regenerate All

From the **Home** screen, tap "Regenerate All" to randomize all values at once. Useful when you want a completely new identity.

---

## 👤 Profile Management

### Why Profiles?

Profiles let you maintain different identities:
- **Default** - Your standard spoofed identity
- **Banking** - Conservative settings for banking apps
- **Gaming** - Aggressive spoofing for games
- **Testing** - Specific values for debugging

### Creating a Profile

1. Go to **Profiles** tab
2. Tap the **+** button
3. Enter profile name and description
4. Tap "Create"
5. New profile will use generated defaults

### Editing a Profile

1. Tap the **(i)** button on a profile card
2. Modify name or description
3. Tap "Save"

### Setting Default Profile

1. Tap the menu on a profile card
2. Select "Set as Default"
3. This profile applies to all apps without specific assignment

### Assigning Profile to Apps

1. Go to **Apps** tab
2. Tap on an app (not the checkbox)
3. Select which profile to use
4. Changes apply immediately

---

## 🔍 Diagnostics

### Accessing Diagnostics

1. Go to **Settings** tab
2. Tap "Diagnostics" in Advanced section

### What Diagnostics Shows

1. **Module Status** - XPosed active/inactive
2. **Detected vs Spoofed Values** - Compare real and fake values
3. **Anti-Detection Tests** - Verify hiding is working

### Anti-Detection Tests

| Test | What It Checks |
|------|----------------|
| Stack Trace | Xposed classes hidden from traces |
| Class Loading | Class.forName blocks Xposed classes |
| /proc/maps | LSPosed libraries hidden |
| Package Check | Xposed packages hidden from PM |

All tests should show "PASSED" (green) when working correctly.

---

## ⚙️ Settings

### Appearance
- **AMOLED Dark Mode** - Pure black background for OLED
- **Dynamic Colors** - Material You colors (Android 12+)

### Advanced
- **Debug Logging** - Enable verbose logs for troubleshooting
- **Diagnostics** - View spoofing effectiveness

### About
- Version information
- Module info

---

## 💡 Tips & Best Practices

### For Maximum Privacy

1. **Enable for all apps** you use regularly
2. **Regenerate values** periodically (weekly)
3. **Use different profiles** for different purposes
4. **Combine with Shamiko** for root hiding

### For Banking Apps

Banking apps are sensitive. Recommended setup:

1. Create a dedicated "Banking" profile
2. Use realistic values (don't use obvious fakes)
3. Keep same values consistent (don't regenerate)
4. Ensure Play Integrity passes with PIF
5. Test with small transactions first

### For Games

Games often have aggressive anti-cheat:

1. Use unique profile per game
2. Spoof device model to common devices
3. Regenerate between game sessions if banned

### For App Development/Testing

1. Enable Debug Logging
2. Use custom values you can recognize
3. Check LSPosed logs for hook triggers

---

## ❓ FAQ

### Q: Values aren't changing in target app?
A: Force close the app, clear app data if safe, and reopen.

### Q: Module shows inactive but LSPosed is working?
A: Remove the app from LSPosed scope (don't hook yourself).

### Q: How do I know it's working?
A: Use the Diagnostics screen, or install a device info app.

### Q: Will this break my banking app?
A: Maybe. Banking apps detect root, SafetyNet, and fingerprinting. You need all three protections. See companion modules.

### Q: Is this detectable?
A: The anti-detection layer hides most traces. However, sophisticated apps may still detect through advanced methods.

### Q: Can I get banned?
A: Using spoofing may violate app ToS. Use at your own risk.

---

## 📞 Getting Help

If you encounter issues:

1. Check the **Troubleshooting** section in README
2. Enable Debug Logging and check LSPosed logs
3. Open an issue on GitHub with:
   - Device model and Android version
   - LSPosed version
   - Steps to reproduce
   - Log excerpts

---

Made with ❤️ for your privacy
