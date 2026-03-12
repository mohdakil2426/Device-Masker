# =============================================================================
# Device Masker — Consumer ProGuard Rules (:common module)
# Automatically merged into :app's R8 run when :common is consumed.
# Protects shared models, generators, AIDL interface, and enums.
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
# AIDL INTERFACE — IDeviceMaskerService and generated Stub/Proxy
# The AIDL compiler generates IDeviceMaskerService.Stub and .Proxy.
# Keep all members so Binder can call them via JNI / reflection.
# =============================================================================
-keep interface com.astrixforge.devicemasker.common.aidl.IDeviceMaskerService { *; }
-keep class com.astrixforge.devicemasker.common.aidl.IDeviceMaskerService$Stub { *; }
-keep class com.astrixforge.devicemasker.common.aidl.IDeviceMaskerService$Stub$Proxy { *; }
-keepclassmembers class com.astrixforge.devicemasker.common.aidl.** {
    *;
}

# =============================================================================
# SHARED MODELS — @Serializable data classes & JsonConfig
# =============================================================================
-keepclassmembers class com.astrixforge.devicemasker.common.** {
    <init>(...);
    *;
}

# Keep serializer companion objects (covers both default and named companions)
-keepclassmembers class com.astrixforge.devicemasker.common.**$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep generated $$serializer inner classes
-keep class com.astrixforge.devicemasker.common.**$$serializer { *; }

# =============================================================================
# ENUMS — SpoofType, SpoofCategory, CorrelationGroup, etc.
# values() and valueOf() are called by serialization and hook lookup.
# =============================================================================
-keepclassmembers enum com.astrixforge.devicemasker.common.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    <fields>;
}

# =============================================================================
# KOTLIN OBJECT SINGLETONS (e.g. SharedPrefsKeys, Constants, generators)
# R8 full mode can strip the INSTANCE field from singletons that appear
# unreferenced, breaking reflective access and Xposed hook patterns.
# =============================================================================
-keepclassmembers class com.astrixforge.devicemasker.common.** {
    public static final ** INSTANCE;
}

# Serializable objects need their INSTANCE kept explicitly
-keepclassmembers class com.astrixforge.devicemasker.common.** {
    public static final *** INSTANCE;
}

# =============================================================================
# DATA CLASSES — Keep component() methods for destructuring & copy()
# =============================================================================
-keepclassmembers class com.astrixforge.devicemasker.common.** {
    public ** component*();
    public ** copy(...);
}

# =============================================================================
# GENERATORS — Value generation logic accessed by hookers
# =============================================================================
-keep class com.astrixforge.devicemasker.common.generators.** { *; }
-keepclassmembers class com.astrixforge.devicemasker.common.generators.** {
    public static *;
}

# =============================================================================
# MODELS (SIMConfig, LocationConfig, Carrier, DeviceHardwareConfig)
# =============================================================================
-keep class com.astrixforge.devicemasker.common.models.** { *; }

# =============================================================================
# SHARED PREFS KEYS — Accessed by both :app and :xposed; must not be renamed
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
