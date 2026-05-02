// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    // ksp plugin removed — only used by YukiHookAPI KSP processor, now replaced by libxposed API
    // 100
    alias(libs.plugins.spotless)
    idea
}

// Shared configuration for all modules
ext {
    set("compileSdk", 36)
    set("minSdk", 26)
    set("targetSdk", 36)
}

spotless {
    kotlin {
        target("app/**/*.kt", "common/**/*.kt", "xposed/**/*.kt")
        targetExclude("**/build/**/*.kt")
        targetExclude("**/memory-bank/**")
        targetExclude("**/openspec/**")
        targetExclude("**/scripts/**")
        targetExclude(".agents/**")
        targetExclude(".agents/**/*.kt")
        targetExclude("**/.agents/**")
        targetExclude(".claude/**")
        targetExclude(".claude/**/*.kt")
        targetExclude("**/.claude/**")
        targetExclude("**/docs/**")
        ktfmt("0.54").kotlinlangStyle()
        trimTrailingWhitespace()
        indentWithSpaces(4)
        endWithNewline()
    }
    kotlinGradle {
        target(
            "*.gradle.kts",
            "app/**/*.gradle.kts",
            "common/**/*.gradle.kts",
            "xposed/**/*.gradle.kts",
        )
        targetExclude("**/build/**/*.gradle.kts")
        targetExclude("**/memory-bank/**")
        targetExclude("**/openspec/**")
        targetExclude("**/scripts/**")
        targetExclude(".agents/**")
        targetExclude(".agents/**/*.gradle.kts")
        targetExclude("**/.agents/**")
        targetExclude(".claude/**")
        targetExclude(".claude/**/*.gradle.kts")
        targetExclude("**/.claude/**")
        targetExclude("**/docs/**")
        ktfmt("0.54").kotlinlangStyle()
    }
}

idea {
    module {
        excludeDirs.addAll(
            files("memory-bank", "openspec", "scripts", ".agents", ".claude", "docs")
        )
    }
}
