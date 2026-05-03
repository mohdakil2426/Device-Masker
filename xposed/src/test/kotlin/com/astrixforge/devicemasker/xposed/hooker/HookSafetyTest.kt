package com.astrixforge.devicemasker.xposed.hooker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HookSafetyTest {

    @Test
    fun `webview user agent replacement handles android model segment without regex init crash`() {
        val original =
            "Mozilla/5.0 (Linux; Android 16; sdk_gphone64_x86_64 Build/BP22.250325.007) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/135.0.7049.111 Mobile Safari/537.36"

        val spoofed = WebViewHooker.modifyUserAgent(original, "Pixel 9 Pro")

        assertEquals(
            "Mozilla/5.0 (Linux; Android 16; Pixel 9 Pro Build/BP22.250325.007) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/135.0.7049.111 Mobile Safari/537.36",
            spoofed,
        )
    }

    @Test
    fun `webview user agent replacement passes through unexpected formats`() {
        val original = "Mozilla/5.0 AppleWebKit/537.36"

        assertEquals(original, WebViewHooker.modifyUserAgent(original, "Pixel 9 Pro"))
        assertEquals(original, WebViewHooker.modifyUserAgent(original, ""))
    }

    @Test
    fun `webview user agent replacement handles android segment without build token`() {
        val original = "Mozilla/5.0 (Linux; Android 16; sdk_gphone64_x86_64) AppleWebKit/537.36"

        assertEquals(
            "Mozilla/5.0 (Linux; Android 16; Pixel 9 Pro) AppleWebKit/537.36",
            WebViewHooker.modifyUserAgent(original, "Pixel 9 Pro"),
        )
    }

    @Test
    fun `anti detect class lookup does not hide app startup and platform classes`() {
        assertFalse(AntiDetectHooker.shouldHideClassForTest("androidx.work.WorkManagerInitializer"))
        assertFalse(
            AntiDetectHooker.shouldHideClassForTest("androidx.startup.InitializationProvider")
        )
        assertFalse(AntiDetectHooker.shouldHideClassForTest("java.lang.String"))
        assertFalse(AntiDetectHooker.shouldHideClassForTest("kotlin.collections.CollectionsKt"))
        assertFalse(
            AntiDetectHooker.shouldHideClassForTest(
                "com.google.android.gms.common.GoogleApiAvailability"
            )
        )
    }

    @Test
    fun `anti detect class lookup still hides explicit hook framework classes`() {
        assertTrue(AntiDetectHooker.shouldHideClassForTest("de.robv.android.xposed.XposedBridge"))
        assertTrue(AntiDetectHooker.shouldHideClassForTest("io.github.lsposed.lspd.LSPosedService"))
        assertTrue(AntiDetectHooker.shouldHideClassForTest("io.github.libxposed.api.XposedModule"))
    }

    @Test
    fun `hooker files use safe hook or structured diagnostics`() {
        val hookerDir = File("src/main/kotlin/com/astrixforge/devicemasker/xposed/hooker")
        val hookerFiles =
            hookerDir.listFiles { file ->
                file.isFile && file.name.endsWith("Hooker.kt") && file.name != "BaseSpoofHooker.kt"
            }.orEmpty()

        assertTrue("Expected hooker files", hookerFiles.isNotEmpty())
        hookerFiles.forEach { file ->
            val text = file.readText()
            assertTrue(
                "${file.name} must use safeHook or structured diagnostics",
                text.contains("safeHook(") ||
                    text.contains("XposedDiagnosticEventSink") ||
                    text.contains("DualLog."),
            )
        }
    }
}
