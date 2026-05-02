# =============================================================================
# Device Masker — Consumer ProGuard Rules (:common module)
# libxposed API 101 edition.
#
# Merged into :app's R8 run automatically by AGP.
# Protects shared models, generators, AIDL interface, enums, and keys.
# =============================================================================

# =============================================================================
# ANNOTATIONS — Required for kotlinx.serialization at runtime
# =============================================================================
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes AnnotationDefault
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes Exceptions

# =============================================================================
# AIDL INTERFACE — IDeviceMaskerService and its generated Stub/Proxy
#
# AIDL compiler outputs IDeviceMaskerService in the package declared in the
# .aidl file: com.astrixforge.devicemasker (no .common.aidl sub-package).
# All members are accessed via JNI/Parcel reflection at runtime.
# =============================================================================
-keep interface com.astrixforge.devicemasker.IDeviceMaskerService { *; }
-keep class com.astrixforge.devicemasker.IDeviceMaskerService$Stub { *; }
-keep class com.astrixforge.devicemasker.IDeviceMaskerService$Stub$Proxy { *; }
-keepclassmembers class com.astrixforge.devicemasker.IDeviceMaskerService$** {
    *;
}

# =============================================================================
# SHARED MODELS — @Serializable data classes, JsonConfig, AppConfig, SpoofGroup
# DeviceProfilePreset enriched with buildTime, tacPrefixes, etc. — all fields kept.
# =============================================================================
-keepclassmembers class com.astrixforge.devicemasker.common.** {
    <init>(...);
    *;
}

# Serializer companion objects (covers both default and named companions)
-keepclassmembers class com.astrixforge.devicemasker.common.**$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

# Generated $$serializer inner classes (serialization descriptor)
-keep class com.astrixforge.devicemasker.common.**$$serializer { *; }

# =============================================================================
# ENUMS — SpoofType (24 values), SpoofCategory, CorrelationGroup, etc.
# values() and valueOf() are called by serialization and hook-side SpoofType lookup.
# =============================================================================
-keepclassmembers enum com.astrixforge.devicemasker.common.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    <fields>;
}

# =============================================================================
# KOTLIN OBJECT SINGLETONS
# SharedPrefsKeys, NetworkTypeMapper, IMEIGenerator, etc.
# R8 full mode can strip the INSTANCE field from objects that appear unreferenced
# from the app perspective — even if they're called reflectively from the hook process.
# =============================================================================
-keepclassmembers class com.astrixforge.devicemasker.common.** {
    public static final ** INSTANCE;
}
-keepclassmembers class com.astrixforge.devicemasker.common.** {
    public static final *** INSTANCE;
}

# =============================================================================
# DATA CLASSES — Keep component() and copy() methods
# Used by UI ViewModels and the JSON serializer.
# =============================================================================
-keepclassmembers class com.astrixforge.devicemasker.common.** {
    public ** component*();
    public ** copy(...);
}

# =============================================================================
# GENERATORS — IMEI, IMSI, MAC, ICCID, Serial, etc.
# Called by DeviceHooker and other hookers at hook time — must not be renamed.
# IMEIGenerator.generateForPreset() and generateWithTac() are new entry points.
# NetworkTypeMapper.getForMccMnc() is called from DeviceHooker.
# =============================================================================
-keep class com.astrixforge.devicemasker.common.generators.** { *; }
-keepclassmembers class com.astrixforge.devicemasker.common.generators.** {
    public *;
    public static *;
}

# NetworkTypeMapper — new in API 101 migration
-keep class com.astrixforge.devicemasker.common.NetworkTypeMapper { *; }
-keepclassmembers class com.astrixforge.devicemasker.common.NetworkTypeMapper {
    public static *;
}

# DeviceProfilePreset.PRESETS, findById() and groupedByManufacturer() are called from UI
-keep class com.astrixforge.devicemasker.common.DeviceProfilePreset { *; }
-keepclassmembers class com.astrixforge.devicemasker.common.DeviceProfilePreset {
    *;
}
-keepclassmembers class com.astrixforge.devicemasker.common.DeviceProfilePreset$Companion {
    public *;
}

# =============================================================================
# MODELS sub-package (SIMConfig, LocationConfig, Carrier)
# =============================================================================
-keep class com.astrixforge.devicemasker.common.models.** { *; }

# =============================================================================
# SHARED PREFS KEYS — Single source of truth; accessed by :app and :xposed
# Must not be renamed — hook process uses the same generated key strings.
# =============================================================================
-keep class com.astrixforge.devicemasker.common.SharedPrefsKeys { *; }
-keepclassmembers class com.astrixforge.devicemasker.common.SharedPrefsKeys {
    public static *;
}

# =============================================================================
# KOTLIN SPECIFICS
# =============================================================================
-keepattributes *KotlinMetadata*
-dontwarn kotlin.reflect.jvm.internal.**
