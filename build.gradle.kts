// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.spotless)
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**/*.kt")
        ktfmt("0.54").kotlinlangStyle()
        trimTrailingWhitespace()
        indentWithSpaces(4)
        endWithNewline()
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        ktfmt("0.54").kotlinlangStyle()
    }
}
