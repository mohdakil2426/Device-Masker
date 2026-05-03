package com.astrixforge.devicemasker.service.diagnostics

import com.astrixforge.devicemasker.common.diagnostics.DiagnosticRedactor
import com.astrixforge.devicemasker.common.diagnostics.RedactionMode
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class SupportBundleMode {
    BASIC,
    FULL,
    ROOT_MAXIMUM,
}

class SupportBundleBuilder(
    private val appEvents: List<String>,
    private val xposedEvents: List<String>,
    private val serviceEvents: List<String>,
    private val snapshots: Map<String, String> = emptyMap(),
    private val rootArtifactsDir: File? = null,
) {
    fun build(outputDir: File, mode: SupportBundleMode, redactionMode: RedactionMode): File {
        outputDir.mkdirs()
        val file = File(outputDir, "devicemasker_support_${timestamp()}.zip")
        val redactor = DiagnosticRedactor(redactionMode)
        ZipOutputStream(file.outputStream().buffered()).use { zip ->
            zip.writeText(
                "manifest.json",
                """{"mode":"$mode","redaction":"$redactionMode","createdAt":"${timestamp()}"}""",
            )
            zip.writeText(
                "README_REPRO.md",
                "Reproduce the issue, then attach this local support bundle.\n",
            )
            zip.writeText("app/app_events.jsonl", appEvents.joinToString("\n").redact(redactor))
            zip.writeText(
                "xposed/xposed_events.jsonl",
                xposedEvents.joinToString("\n").redact(redactor),
            )
            zip.writeText(
                "diagnostics/service_events.jsonl",
                serviceEvents.joinToString("\n").redact(redactor),
            )

            if (mode == SupportBundleMode.FULL || mode == SupportBundleMode.ROOT_MAXIMUM) {
                snapshots.forEach { (name, content) ->
                    val prefix =
                        when {
                            name.startsWith("config") || name.startsWith("remote") -> "config"
                            name.startsWith("scope") -> "scope"
                            else -> "diagnostics"
                        }
                    zip.writeText("$prefix/$name", content.redact(redactor))
                }
            }

            if (mode == SupportBundleMode.ROOT_MAXIMUM) {
                rootArtifactsDir
                    ?.walkTopDown()
                    ?.filter { it.isFile }
                    ?.forEach { artifact ->
                        val relative = artifact.relativeTo(rootArtifactsDir).invariantSeparatorsPath
                        zip.writeText("root/$relative", artifact.readText().redact(redactor))
                    }
            }
        }
        return file
    }

    private fun ZipOutputStream.writeText(path: String, content: String) {
        putNextEntry(ZipEntry(path))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun String.redact(redactor: DiagnosticRedactor): String = redactor.redactMessage(this)

    private fun timestamp(): String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
}
