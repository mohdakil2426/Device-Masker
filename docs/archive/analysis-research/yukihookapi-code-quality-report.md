# YukiHookAPI Code Quality Research Report

**Date**: December 25, 2025  
**Status**: Research Complete  
**Focus**: Improve code quality, robustness, reduce code

---

## Executive Summary

After comprehensive research of YukiHookAPI 1.3.1 documentation, LSPosed best practices, and analysis of our current codebase, I've identified **5 key improvements** that can make our Xposed code:

- **More robust** (fewer edge-case failures)
- **Cleaner** (less duplicate code)
- **Smaller** (~20-30% reduction)
- **Safer** (no breaking changes)

---

## 🔍 Current Codebase Analysis

### Statistics

| File | Lines | runCatching | Could Optimize |
|------|-------|-------------|----------------|
| DeviceHooker.kt | 492 | 19 | ✅ High |
| NetworkHooker.kt | 175 | 2 | ✅ Done |
| AntiDetectHooker.kt | 277 | 6 | ✅ Medium |
| SensorHooker.kt | 147 | 1 | ⚠️ Low |
| LocationHooker.kt | 203 | 5 | ✅ Medium |
| SystemHooker.kt | 171 | 3 | ⚠️ Low |
| AdvertisingHooker.kt | 150 | 5 | ✅ Medium |
| WebViewHooker.kt | 88 | 0 | ⚠️ Low |
| **Total** | **~1700** | **41** | |

---

## ✅ Improvement #1: Use `.optional()` Instead of `runCatching`

### Problem
```kotlin
// Current pattern (verbose)
runCatching {
    method {
        name = "getSubscriberId"
        param(IntType)
    }.hook {
        replaceAny { cachedImsi }
    }
}
```

### YukiHookAPI Built-in Solution
```kotlin
// Use .optional() - native YukiHookAPI feature
method {
    name = "getSubscriberId"
    param(IntType)
}.optional().hook {
    replaceAny { cachedImsi }
}
```

### Benefits
| Before | After |
|--------|-------|
| 6 lines | 5 lines |
| Manual error handling | Built-in graceful failure |
| Inconsistent logging | Consistent YukiHookAPI logs |

### Where to Apply
- **DeviceHooker.kt**: 19 `runCatching` → `.optional()`
- **AntiDetectHooker.kt**: 6 `runCatching` → `.optional()`
- **LocationHooker.kt**: 5 `runCatching` → `.optional()`
- **AdvertisingHooker.kt**: 5 `runCatching` → `.optional()`

**Estimated reduction**: ~50 lines removed

---

## ✅ Improvement #2: Use `.allMethods()` for Overloaded Methods

### Problem
```kotlin
// Current: Hook each overload separately (3 hooks for same method)
method { name = "getDeviceId"; emptyParam() }.hook { replaceAny { cachedImei } }
method { name = "getDeviceId"; param(IntType) }.hook { replaceAny { cachedImei } }
```

### Better Pattern
```kotlin
// New: Hook ALL overloads at once
allMethods { name = "getDeviceId" }.hookAll {
    replaceAny { cachedImei }
}
```

### Where to Apply
| Method | Current Hooks | Can Combine |
|--------|--------------|-------------|
| `getDeviceId` | 2 | ✅ |
| `getImei` | 2 | ✅ |
| `getSubscriberId` | 2 | ✅ |
| `getSimSerialNumber` | 2 | ✅ |
| `getSimCountryIso` | 2 | ✅ |
| `getNetworkCountryIso` | 2 | ✅ |
| `getSimOperatorName` | 2 | ✅ |
| `getNetworkOperator` | 2 | ✅ |
| `getSimOperator` | 2 | ✅ |

**Estimated reduction**: ~60 lines (18 hooks → 9 hooks)

---

## ✅ Improvement #3: Use `.ignoredHookClassNotFoundFailure()` for Optional Classes

### Problem
```kotlin
// Current: Nullable class reference
private val subscriptionInfoClass by lazy { 
    "android.telephony.SubscriptionInfo".toClassOrNull() 
}

// Then null check everywhere
subscriptionInfoClass?.apply { 
    // hooks 
}
```

### Better Pattern
```kotlin
// Let YukiHookAPI handle it silently
private val subscriptionInfoClass by lazy { 
    "android.telephony.SubscriptionInfo".toClass()
        .ignoredHookClassNotFoundFailure()
}

// No null checks needed - hooks just silently skip if class missing
subscriptionInfoClass.apply { 
    // hooks 
}
```

### Where to Apply
- All `toClassOrNull()` usages (~15 occurrences)

---

## ✅ Improvement #4: Create Base Hooker for Common Logic

### Current Pattern (Repeated in Every Hooker)
```kotlin
object DeviceHooker : YukiBaseHooker() {
    private const val TAG = "DeviceHooker"
    
    private fun getSpoofValue(type: SpoofType, fallback: () -> String): String {
        return PrefsHelper.getSpoofValue(prefs, packageName, type, fallback)
    }
    
    override fun onHook() {
        DualLog.debug(TAG, "Starting hooks for: $packageName")
        // ...
        HookMetrics.recordSuccess(TAG, "initialization")
    }
}
```

### Better: Abstract Base Class
```kotlin
/**
 * Base hooker with common functionality.
 */
abstract class BaseSpoofHooker(private val tag: String) : YukiBaseHooker() {
    
    /** Get spoof value with fallback. */
    protected fun getSpoofValue(type: SpoofType, fallback: () -> String): String {
        return PrefsHelper.getSpoofValue(prefs, packageName, type, fallback)
    }
    
    /** Log hook start. */
    protected fun logStart() {
        DualLog.debug(tag, "Starting hooks for: $packageName")
    }
    
    /** Record success. */
    protected fun recordSuccess() {
        HookMetrics.recordSuccess(tag, "initialization")
    }
    
    /** Safe hook wrapper. */
    protected inline fun safeHook(block: () -> Unit) {
        runCatching { block() }.onFailure { 
            DualLog.warn(tag, "Hook failed", it) 
        }
    }
}
```

### Simplified Child Hooker
```kotlin
object DeviceHooker : BaseSpoofHooker("DeviceHooker") {
    
    override fun onHook() {
        logStart()
        hookTelephonyManager()
        hookBuildClass()
        recordSuccess()
    }
    
    private fun hookTelephonyManager() {
        val cachedImei = getSpoofValue(SpoofType.IMEI) { generateImei() }
        // hooks...
    }
}
```

**Estimated reduction**: ~100 lines (removes repeated code from 8 hookers)

---

## ✅ Improvement #5: Consolidate Generator Functions

### Problem
Each hooker has its own generator functions, some duplicated:

```kotlin
// DeviceHooker.kt
private fun generateImei(): String { ... }
private fun generateSerial(): String { ... }

// NetworkHooker.kt  
private fun generateMac(): String { ... }

// LocationHooker.kt
private fun generateLatitude(): Double { ... }
```

### Better: Centralized Generator Utility
```kotlin
// xposed/utils/ValueGenerators.kt
object ValueGenerators {
    
    fun imei(): String { 
        // Luhn-valid IMEI
    }
    
    fun mac(): String { 
        // Valid MAC with unicast bit
    }
    
    fun androidId(): String {
        // 16-char hex
    }
    
    fun serial(): String {
        // 8-16 alphanumeric
    }
    
    fun imsi(): String {
        // 15 digits
    }
}
```

Then hookers just call:
```kotlin
val cachedImei = getSpoofValue(SpoofType.IMEI) { ValueGenerators.imei() }
```

**Estimated reduction**: ~80 lines (consolidate ~10 generator functions)

---

## 📋 Implementation Plan

### Phase 1: Low Risk, High Impact (Recommended)

| Task | Files | Effort | Risk | Impact |
|------|-------|--------|------|--------|
| Replace `runCatching` with `.optional()` | All hookers | 🟢 Easy | 🟢 Low | High |
| Create ValueGenerators utility | New file | 🟢 Easy | 🟢 Low | Medium |

### Phase 2: Medium Risk, High Impact

| Task | Files | Effort | Risk | Impact |
|------|-------|--------|------|--------|
| Use `allMethods().hookAll()` | DeviceHooker | 🟡 Medium | 🟡 Medium | High |
| Create BaseSpoofHooker | All hookers | 🟡 Medium | 🟡 Medium | High |

### Phase 3: Higher Risk (Future)

| Task | Files | Effort | Risk | Impact |
|------|-------|--------|------|--------|
| Replace `toClassOrNull()` with `ignoredHookClassNotFoundFailure()` | All | 🔴 Hard | 🔴 High | Medium |

---

## 🎯 Quick Wins - Immediate Improvements

### 1. Replace runCatching with .optional()

**Before:**
```kotlin
runCatching {
    method { name = "getImei"; param(IntType) }.hook {
        replaceAny { cachedImei }
    }
}
```

**After:**
```kotlin
method { name = "getImei"; param(IntType) }.optional().hook {
    replaceAny { cachedImei }
}
```

### 2. Combine Overloaded Method Hooks

**Before:**
```kotlin
method { name = "getDeviceId"; emptyParam() }.hook { replaceAny { cachedImei } }
method { name = "getDeviceId"; param(IntType) }.hook { replaceAny { cachedImei } }
```

**After:**
```kotlin
allMethods { name = "getDeviceId" }.hookAll { replaceAny { cachedImei } }
```

---

## 📊 Expected Results

| Metric | Before | After | Improvement |
|--------|--------|-------|-------------|
| Total Lines | ~1,700 | ~1,200 | -30% |
| runCatching blocks | 41 | 0 | -100% |
| Generator functions | ~10 scattered | 1 centralized | Cleaner |
| Repeated code | ~150 lines | 0 | -100% |
| Hook registrations | ~80 | ~50 | -37% |

---

## ⚠️ Important Notes

1. **Test on device after each change** - Xposed hooks are hard to unit test
2. **Keep runCatching for non-hook operations** - Only replace for method hooks
3. **Don't change working hooks without testing** - "If it ain't broke..."
4. **Start with DeviceHooker** - It has the most opportunities for optimization

---

## 🔗 References

- [YukiHookAPI .optional() docs](https://highcapable.github.io/YukiHookAPI/en/api/public/com/highcapable/yukihookapi/hook/core/finder/members/MethodFinder/#optional-method)
- [YukiHookAPI allMethods() docs](https://highcapable.github.io/YukiHookAPI/en/guide/quick-start/#finding-all-methods)
- [YukiHookAPI ignoredHookClassNotFoundFailure](https://highcapable.github.io/YukiHookAPI/en/api/public/com/highcapable/yukihookapi/hook/core/YukiHookCreator/#ignoredhookclassnotfoundfailure-method)

---

**Would you like me to implement Phase 1 changes now?**
