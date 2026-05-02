package com.astrixforge.devicemasker

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseBuildSafetyTest {

    @Test
    fun `release build keeps xposed hooker bytecode unminified`() {
        val buildFile = projectFile("app/build.gradle.kts").readText()

        assertTrue(
            "Release R8 minification must stay disabled until libxposed hooker lambdas are live-validated.",
            Regex("release\\s*\\{[\\s\\S]*isMinifyEnabled\\s*=\\s*false").containsMatchIn(buildFile),
        )
        assertTrue(
            "Resource shrinking requires minification and must stay disabled with xposed hookers unminified.",
            buildFile.contains("isShrinkResources = false"),
        )
    }

    @Test
    fun `xposed target process does not look up custom diagnostics service`() {
        val entryFile =
            projectFile("xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/XposedEntry.kt")
                .readText()

        assertTrue(
            "Target-process diagnostics must not use ServiceManager; SELinux denies this on user builds.",
            !entryFile.contains("Class.forName(\"android.os.ServiceManager\")"),
        )
    }

    @Test
    fun `anti detect class lookup hooks are reentrant safe`() {
        val hookerFile =
            projectFile(
                    "xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/hooker/AntiDetectHooker.kt"
                )
                .readText()

        assertTrue(
            "Class lookup hooks must guard reentry; otherwise target startup can ANR while hook registration loads classes.",
            hookerFile.contains("classLookupHookActive"),
        )
        assertTrue(
            "Class lookup hooks must reset their guard in finally blocks.",
            hookerFile.contains("classLookupHookActive.set(false)"),
        )
        assertTrue(
            "Global class lookup hooks must stay disabled until they are proven target-startup safe.",
            !hookerFile.contains("hookClassForName(cl, xi)") &&
                !hookerFile.contains("hookClassLoaderLoadClass(cl, xi)"),
        )
        assertTrue(
            "Bootstrap classes should be resolved directly instead of through the target ClassLoader after class-loading hooks are active.",
            hookerFile.contains("ClassLoader::class.java") &&
                hookerFile.contains("Class::class.java"),
        )
        assertTrue(
            "The class lookup hook must avoid Kotlin collection helpers in the intercepted class-loading path.",
            !hookerFile.substringAfter("private fun shouldHideClass").contains(".any"),
        )
    }

    private fun projectFile(path: String): File {
        val normalized = path.replace("/", File.separator)
        return generateSequence(File("").absoluteFile) { it.parentFile }
            .map { File(it, normalized) }
            .first { it.exists() }
    }
}
