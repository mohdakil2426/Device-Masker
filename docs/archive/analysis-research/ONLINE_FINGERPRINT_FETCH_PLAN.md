# Online Fingerprint Fetching Implementation Plan

**Date**: December 20, 2025  
**Feature**: Fetch fresh device fingerprints from Google Developer site with local fallback  
**Approach**: Same as PlayIntegrityFix but in Kotlin

---

## Overview

Add background online fetching of device fingerprints from Google's developer pages (like PlayIntegrityFix does) with automatic fallback to embedded presets if network fails.

**Key Points**:
- ✅ **No UI changes** - works transparently in background
- ✅ **Fallback to local** - if fetch fails, use embedded presets
- ✅ **Caching** - cache fetched data to avoid repeated network calls
- ✅ **Async/Non-blocking** - uses Kotlin coroutines
- ✅ **Privacy-aware** - user can disable in settings

---

## Architecture

```
User selects device in UI
         ↓
DeviceProfilePreset.findById("pixel_8_pro")
         ↓
    Is cached?
    /        \
  YES        NO
   ↓          ↓
Return   Fetch online
cached   (background)
         |     |
      Success Fail
         ↓     ↓
      Cache  Fallback
      result to local
         ↓      ↓
      Return Return
      fresh  embedded
      data   preset
```

---

## File Structure

```
common/
├── src/main/kotlin/.../common/
│   ├── DeviceProfilePreset.kt              # Existing (modified)
│   ├── network/                            # NEW folder
│   │   ├── FingerprintFetcher.kt          # Main fetcher
│   │   ├── GoogleDevPageScraper.kt        # HTML parsing
│   │   ├── OTAMetadataExtractor.kt        # Extract from OTA files
│   │   └── FetchResult.kt                 # Sealed class for results
│   └── cache/                              # NEW folder
│       ├── FingerprintCache.kt            # In-memory + file cache
│       └── CacheEntry.kt                  # Cache data model

app/
├── src/main/kotlin/.../
│   ├── data/
│   │   └── preferences/
│   │       └── AppPreferences.kt          # Add online fetch toggle
```

---

## Dependencies to Add

### libs.versions.toml

```toml
[versions]
ktor = "3.0.3"                    # HTTP client
jsoup = "1.18.3"                  # HTML parsing
kotlinx-datetime = "0.6.1"        # Date handling

[libraries]
# Networking
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-android = { module = "io.ktor:ktor-client-android", version.ref = "ktor" }
ktor-client-logging = { module = "io.ktor:ktor-client-logging", version.ref = "ktor" }

# HTML Parsing
jsoup = { module = "org.jsoup:jsoup", version.ref = "jsoup" }

# DateTime
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinx-datetime" }
```

### common/build.gradle.kts

```kotlin
dependencies {
    // Existing dependencies...
    
    // Network fetching
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.logging)
    
    // HTML parsing
    implementation(libs.jsoup)
    
    // DateTime
    implementation(libs.kotlinx.datetime)
}
```

---

## Implementation Details

### 1. FetchResult.kt (Sealed Class)

**Location**: `common/src/main/kotlin/.../common/network/FetchResult.kt`

```kotlin
package com.astrixforge.devicemasker.common.network

import com.astrixforge.devicemasker.common.DeviceProfilePreset

/**
 * Result of fetching device fingerprint online.
 */
sealed class FetchResult {
    /**
     * Successfully fetched fresh fingerprint.
     */
    data class Success(
        val preset: DeviceProfilePreset,
        val source: Source = Source.ONLINE
    ) : FetchResult()
    
    /**
     * Fetch failed, using cached data.
     */
    data class Cached(
        val preset: DeviceProfilePreset,
        val cacheAge: kotlin.time.Duration
    ) : FetchResult()
    
    /**
     * Fetch failed, using embedded fallback.
     */
    data class Fallback(
        val preset: DeviceProfilePreset,
        val reason: String
    ) : FetchResult()
    
    /**
     * Complete failure (network + cache + fallback all failed).
     */
    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : FetchResult()
    
    enum class Source {
        ONLINE,    // Fetched from network
        CACHE,     // Retrieved from cache
        EMBEDDED   // Local fallback preset
    }
}
```

---

### 2. GoogleDevPageScraper.kt (HTML Scraping)

**Location**: `common/src/main/kotlin/.../common/network/GoogleDevPageScraper.kt`

```kotlin
package com.astrixforge.devicemasker.common.network

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Scrapes Google's Android developer pages to get latest device info.
 * 
 * Mimics PlayIntegrityFix's approach:
 * 1. Fetch https://developer.android.com/about/versions
 * 2. Extract latest beta page URL
 * 3. Get OTA download links
 */
class GoogleDevPageScraper(private val httpClient: HttpClient) {
    
    companion object {
        private const val VERSIONS_URL = "https://developer.android.com/about/versions"
        private const val TIMEOUT_MS = 10_000L
    }
    
    /**
     * Fetches list of available Pixel devices from Google's beta page.
     * 
     * @return List of device models and their product names
     */
    suspend fun fetchPixelDeviceList(): Result<List<PixelDevice>> = runCatching {
        // Step 1: Get versions page
        val versionsHtml = httpClient.get(VERSIONS_URL) {
            timeout {
                requestTimeoutMillis = TIMEOUT_MS
            }
        }.bodyAsText()
        
        // Step 2: Parse to find latest beta URL
        val betaUrl = extractLatestBetaUrl(versionsHtml)
            ?: throw IllegalStateException("Could not find beta URL")
        
        // Step 3: Get beta page
        val betaHtml = httpClient.get(betaUrl) {
            timeout {
                requestTimeoutMillis = TIMEOUT_MS
            }
        }.bodyAsText()
        
        // Step 4: Find OTA page link
        val otaUrl = extractOtaPageUrl(betaHtml)
            ?: throw IllegalStateException("Could not find OTA page URL")
        
        // Step 5: Get OTA page
        val otaHtml = httpClient.get(otaUrl) {
            timeout {
                requestTimeoutMillis = TIMEOUT_MS
            }
        }.bodyAsText()
        
        // Step 6: Extract device list
        parseDeviceList(otaHtml)
    }
    
    /**
     * Fetches fingerprint and security patch for a specific device.
     * 
     * @param productName Product name (e.g., "panther_beta")
     * @return Device fingerprint data
     */
    suspend fun fetchDeviceFingerprint(productName: String): Result<FingerprintData> = runCatching {
        // Get OTA download link for this product
        val otaDownloadUrl = getOtaDownloadUrl(productName)
            ?: throw IllegalStateException("OTA URL not found for $productName")
        
        // Download first 15 lines of OTA zip as strings (like PlayIntegrityFix)
        val metadata = httpClient.get(otaDownloadUrl) {
            timeout {
                requestTimeoutMillis = TIMEOUT_MS
            }
            // Only download first few KB (metadata is at start of zip)
            headers {
                append("Range", "bytes=0-16384")  // First 16KB should contain metadata
            }
        }.readBytes()
        
        // Extract fingerprint and security patch
        extractFingerprintFromOta(metadata)
    }
    
    private fun extractLatestBetaUrl(html: String): String? {
        val doc: Document = Jsoup.parse(html)
        
        // Find links matching pattern: /about/versions/[number]
        return doc.select("a[href*='/about/versions/']")
            .map { it.attr("abs:href") }
            .filter { it.matches(Regex(".*versions/\\d+.*")) }
            .sortedDescending()
            .firstOrNull()
    }
    
    private fun extractOtaPageUrl(html: String): String? {
        val doc: Document = Jsoup.parse(html)
        
        // Find link to OTA download page (contains "download-ota")
        return doc.select("a[href*='download-ota']")
            .firstOrNull()
            ?.attr("abs:href")
    }
    
    private fun parseDeviceList(html: String): List<PixelDevice> {
        val doc: Document = Jsoup.parse(html)
        val devices = mutableListOf<PixelDevice>()
        
        // Extract from table rows with id attribute
        doc.select("tr[id]").forEach { row ->
            val productName = row.id() + "_beta"  // e.g., "panther_beta"
            val model = row.select("td").firstOrNull()?.text()
            
            if (model != null && model.isNotBlank()) {
                devices.add(PixelDevice(model, productName))
            }
        }
        
        return devices
    }
    
    private suspend fun getOtaDownloadUrl(productName: String): String? {
        // Implementation: fetch OTA page and extract download URL for specific product
        // Similar to parseDeviceList but extract href attribute
        return null // Placeholder
    }
    
    private fun extractFingerprintFromOta(otaBytes: ByteArray): FingerprintData {
        // Convert bytes to string (OTA metadata is text)
        val metadata = String(otaBytes, Charsets.UTF_8)
        
        // Extract build fingerprint (line starts with "post-build=")
        val fingerprint = metadata.lines()
            .firstOrNull { it.startsWith("post-build=") }
            ?.substringAfter("post-build=")
            ?.trim()
            ?: throw IllegalStateException("Fingerprint not found in OTA metadata")
        
        // Extract security patch (line starts with "security-patch-level=")
        val securityPatch = metadata.lines()
            .firstOrNull { it.startsWith("security-patch-level=") }
            ?.substringAfter("security-patch-level=")
            ?.trim()
            ?: throw IllegalStateException("Security patch not found in OTA metadata")
        
        return FingerprintData(fingerprint, securityPatch)
    }
}

/**
 * Pixel device info from Google's page.
 */
data class PixelDevice(
    val model: String,        // e.g., "Pixel 7"
    val productName: String   // e.g., "panther_beta"
)

/**
 * Fingerprint data extracted from OTA.
 */
data class FingerprintData(
    val fingerprint: String,       // Full build fingerprint
    val securityPatch: String      // Security patch date (YYYY-MM-DD)
)
```

---

### 3. FingerprintCache.kt (Caching Layer)

**Location**: `common/src/main/kotlin/.../common/cache/FingerprintCache.kt`

```kotlin
package com.astrixforge.devicemasker.common.cache

import com.astrixforge.devicemasker.common.DeviceProfilePreset
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/**
 * In-memory cache for fetched device fingerprints.
 * 
 * Cache expires after 7 days to ensure freshness.
 */
class FingerprintCache {
    
    companion object {
        private val CACHE_DURATION = 7.days
    }
    
    private val cache = mutableMapOf<String, CacheEntry>()
    
    /**
     * Gets cached preset if available and not expired.
     * 
     * @param presetId Preset ID (e.g., "pixel_8_pro")
     * @return Cached preset or null if not found/expired
     */
    fun get(presetId: String): DeviceProfilePreset? {
        val entry = cache[presetId] ?: return null
        
        // Check if expired
        val age = Clock.System.now() - entry.timestamp
        if (age > CACHE_DURATION) {
            cache.remove(presetId)
            return null
        }
        
        return entry.preset
    }
    
    /**
     * Caches a preset.
     * 
     * @param presetId Preset ID
     * @param preset The preset to cache
     */
    fun put(presetId: String, preset: DeviceProfilePreset) {
        cache[presetId] = CacheEntry(
            preset = preset,
            timestamp = Clock.System.now()
        )
    }
    
    /**
     * Gets age of cached entry.
     * 
     * @param presetId Preset ID
     * @return Age of cache entry or null if not cached
     */
    fun getCacheAge(presetId: String): Duration? {
        val entry = cache[presetId] ?: return null
        return Clock.System.now() - entry.timestamp
    }
    
    /**
     * Clears all cached entries.
     */
    fun clear() {
        cache.clear()
    }
}

/**
 * Cache entry with timestamp.
 */
data class CacheEntry(
    val preset: DeviceProfilePreset,
    val timestamp: Instant
)
```

---

### 4. FingerprintFet cher.kt (Main Fetcher)

**Location**: `common/src/main/kotlin/.../common/network/FingerprintFetcher.kt`

```kotlin
package com.astrixforge.devicemasker.common.network

import com.astrixforge.devicemasker.common.DeviceProfilePreset
import com.astrixforge.devicemasker.common.cache.FingerprintCache
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.logging.*
import kotlinx.coroutines.*

/**
 * Fetches fresh device fingerprints from Google with fallback to local presets.
 * 
 * Strategy:
 * 1. Check cache first
 * 2. If not cached, try online fetch
 * 3. If fetch fails, use embedded preset
 */
class FingerprintFetcher {
    
    private val httpClient = HttpClient(Android) {
        install(Logging) {
            level = LogLevel.INFO
        }
        engine {
            connectTimeout = 10_000
            socketTimeout = 10_000
        }
    }
    
    private val scraper = GoogleDevPageScraper(httpClient)
    private val cache = FingerprintCache()
    
    /**
     * Fetches fingerprint for a device preset.
     * 
     * @param presetId Preset ID (e.g., "pixel_8_pro")
     * @param forceRefresh Force online fetch even if cached
     * @return FetchResult with preset data
     */
    suspend fun fetchFingerprint(
        presetId: String,
        forceRefresh: Boolean = false
    ): FetchResult = withContext(Dispatchers.IO) {
        
        // Step 1: Check cache (unless force refresh)
        if (!forceRefresh) {
            cache.get(presetId)?.let { cachedPreset ->
                val cacheAge = cache.getCacheAge(presetId)!!
                return@withContext FetchResult.Cached(cachedPreset, cacheAge)
            }
        }
        
        // Step 2: Try online fetch
        val fetchResult = tryOnlineFetch(presetId)
        if (fetchResult is FetchResult.Success) {
            // Cache successful fetch
            cache.put(presetId, fetchResult.preset)
            return@withContext fetchResult
        }
        
        // Step 3: Fallback to embedded preset
        val embeddedPreset = DeviceProfilePreset.findById(presetId)
        if (embeddedPreset != null) {
            return@withContext FetchResult.Fallback(
                preset = embeddedPreset,
                reason = "Online fetch failed, using embedded preset"
            )
        }
        
        // Step 4: Complete failure
        FetchResult.Error("No preset found for ID: $presetId")
    }
    
    /**
     * Attempts to fetch fingerprint online.
     */
    private suspend fun tryOnlineFetch(presetId: String): FetchResult {
        return try {
            // Map preset ID to product name
            val productName = mapPresetIdToProductName(presetId)
                ?: return FetchResult.Error("Unknown preset ID: $presetId")
            
            // Fetch fingerprint data
            val fingerprintData = scraper.fetchDeviceFingerprint(productName)
                .getOrThrow()
            
            // Create updated preset
            val embeddedPreset = DeviceProfilePreset.findById(presetId)!!
            val freshPreset = embeddedPreset.copy(
                fingerprint = fingerprintData.fingerprint,
                securityPatch = fingerprintData.securityPatch
            )
            
            FetchResult.Success(freshPreset, FetchResult.Source.ONLINE)
            
        } catch (e: Exception) {
            FetchResult.Error("Fetch failed: ${e.message}", e)
        }
    }
    
    /**
     * Maps our preset IDs to Google's product names.
     */
    private fun mapPresetIdToProductName(presetId: String): String? {
        return when (presetId) {
            "pixel_8_pro" -> "husky_beta"
            "pixel_7" -> "panther_beta"
            "pixel_7_pro" -> "cheetah_beta"
            "pixel_6" -> "oriole_beta"
            "pixel_6_pro" -> "raven_beta"
            // Add more mappings as needed
            else -> null
        }
    }
    
    /**
     * Cleanup resources.
     */
    fun close() {
        httpClient.close()
    }
}
```

---

### 5. Update DeviceProfilePreset.kt

**Location**: `common/src/main/kotlin/.../common/DeviceProfilePreset.kt`

**Add to companion object**:

```kotlin
companion object {
    // Existing PRESETS list...
    
    /**
     * Fingerprint fetcher instance (lazy init).
     */
    private val fetcher by lazy { FingerprintFetcher() }
    
    /**
     * Finds preset by ID with optional online refresh.
     * 
     * @param id Preset ID
     * @param fetchOnline Whether to fetch fresh data online
     * @return Preset or null if not found
     */
    suspend fun findByIdWithFetch(
        id: String,
        fetchOnline: Boolean = true
    ): DeviceProfilePreset? {
        if (!fetchOnline) {
            return findById(id)  // Use existing method
        }
        
        // Try online fetch with fallback
        return when (val result = fetcher.fetchFingerprint(id)) {
            is FetchResult.Success -> result.preset
            is FetchResult.Cached -> result.preset
            is FetchResult.Fallback -> result.preset
            is FetchResult.Error -> findById(id)  // Final fallback to embedded
        }
    }
    
    /**
     * Existing findById() method - no changes needed
     */
    fun findById(id: String): DeviceProfilePreset? {
        return PRESETS.find { it.id == id }
    }
}
```

---

## Integration Points

### 1. SpoofRepository.kt

**Update generateValue()**:

```kotlin
fun generateValue(type: SpoofType): String {
    return when (type) {
        // ... other types
        
        SpoofType.DEVICE_PROFILE -> {
            // Note: Online fetching happens in UI layer when user selects device
            // Here we just return a random preset ID
            DeviceProfilePreset.PRESETS.random().id
        }
        
        // ... other types
    }
}
```

### 2. ProfileDetailScreen.kt (UI Integration)

**No UI changes, just data loading**:

```kotlin
@Composable
fun ProfileDetailScreen(...) {
    // When device profile changes, fetch online in background
    LaunchedEffect(selectedPresetId) {
        if (onlineFetchEnabled) {
            // Fetch in background, update when ready
            val freshPreset = DeviceProfilePreset.findByIdWithFetch(
                id = selectedPresetId,
                fetchOnline = true
            )
            
            // Update profile with fresh data
            if (freshPreset != null) {
                onUpdateDeviceProfile(freshPreset)
            }
        }
    }
}
```

---

## Settings Integration

### AppPreferences.kt

**Add preference for online fetching**:

```kotlin
class AppPreferences(private val dataStore: DataStore<Preferences>) {
    
    companion object {
        private val ONLINE_FETCH_ENABLED = booleanPreferencesKey("online_fetch_enabled")
    }
    
    /**
     * Whether to fetch fingerprints online.
     */
    val onlineFetchEnabled: Flow<Boolean> = dataStore.data
        .map { it[ONLINE_FETCH_ENABLED] ?: false }  // Default: disabled
    
    suspend fun setOnlineFetchEnabled(enabled: Boolean) {
        dataStore.edit { it[ONLINE_FETCH_ENABLED] = enabled }
    }
}
```

### SettingsScreen.kt

**Add toggle (minimal UI change)**:

```kotlin
@Composable
fun SettingsScreen(...) {
    // Existing settings...
    
    // Add this setting
    SettingItem(
        title = "Online Device Fetch",
        description = "Fetch latest device fingerprints from Google (requires internet)",
        trailing = {
            Switch(
                checked = onlineFetchEnabled,
                onCheckedChange = { onToggleOnlineFetch(it) }
            )
        }
    )
}
```

---

## Privacy Considerations

1. **Default OFF** - Online fetching disabled by default
2. **User control** - Toggle in Settings
3. **No tracking** - Only fetches from developer.android.com
4. **Fallback always** - Works offline with embedded presets
5. **Transparent** - Show source in logs (Online/Cached/Embedded)

---

## Testing Strategy

### Unit Tests

```kotlin
class FingerprintFetcherTest {
    @Test
    fun `fallback to local when network fails`() = runTest {
        val fetcher = FingerprintFetcher()
        // Simulate network failure
        val result = fetcher.fetchFingerprint("pixel_8_pro")
        
        assertTrue(result is FetchResult.Fallback)
        assertNotNull((result as FetchResult.Fallback).preset)
    }
    
    @Test
    fun `cache is used for repeated requests`() = runTest {
        val fetcher = FingerprintFetcher()
        val result1 = fetcher.fetchFingerprint("pixel_8_pro")
        val result2 = fetcher.fetchFingerprint("pixel_8_pro")
        
        assertTrue(result2 is FetchResult.Cached)
    }
}
```

---

## Performance Optimization

1. **Lazy fetch** - Only fetch when user selects device
2. **7-day cache** - Avoid repeated network calls
3. **Timeout 10s** - Fast fail if network slow
4. **Background thread** - Non-blocking UI
5. **Partial download** - Only first 16KB of OTA file

---

## Error Handling

```kotlin
try {
    // Fetch online
} catch (e: ConnectTimeoutException) {
    // Network timeout → Fallback
} catch (e: UnknownHostException) {
    // No internet → Fallback
} catch (e: Exception) {
    // Any error → Fallback
}
```

Always falls back gracefully to local presets.

---

## Implementation Checklist

### Phase 1: Dependencies
- [ ] Add Ktor dependencies to libs.versions.toml
- [ ] Add Jsoup dependency
- [ ] Add kotlinx-datetime dependency
- [ ] Sync Gradle

### Phase 2: Core Classes
- [ ] Create FetchResult.kt
- [ ] Create CacheEntry.kt  
- [ ] Create FingerprintCache.kt
- [ ] Create GoogleDevPageScraper.kt
- [ ] Create FingerprintFetcher.kt

### Phase 3: Integration
- [ ] Update DeviceProfilePreset.kt
- [ ] Add AppPreferences toggle
- [ ] Add Settings toggle UI
- [ ] Update ProfileDetailScreen fetch logic

### Phase 4: Testing
- [ ] Unit tests for cache
- [ ] Unit tests for fallback
- [ ] Manual testing with/without network
- [ ] Verify fallback works

### Phase 5: Build & Verify
- [ ] Build all modules
- [ ] Test on device
- [ ] Verify no crashes on network failure

---

## Summary

**What's Added**:
- ✅ Online fingerprint fetching from Google
- ✅ 7-day caching layer
- ✅ Automatic fallback to local presets
- ✅ Background async fetching
- ✅ User toggle in Settings

**What's NOT Changed**:
- ❌ No new screens
- ❌ No UI redesign
- ❌ Existing presets still work
- ❌ Everything works offline

**Dependencies**: +3 (Ktor, Jsoup, kotlinx-datetime)  
**Files Created**: 6 new files  
**Files Modified**: 3 existing files  
**Estimated Effort**: 4-6 hours

This implements the same approach as PlayIntegrityFix but in **Kotlin** with proper **error handling**, **caching**, and **fallback strategy**! 🚀
