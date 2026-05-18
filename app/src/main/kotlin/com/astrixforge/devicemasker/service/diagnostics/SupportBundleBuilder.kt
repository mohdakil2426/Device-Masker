package com.astrixforge.devicemasker.service.diagnostics

import com.astrixforge.devicemasker.common.diagnostics.DiagnosticRedactor
import com.astrixforge.devicemasker.common.diagnostics.RedactionMode
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class SupportBundleBuilder(
    private val appEvents: List<String>,
    private val xposedEvents: List<String>,
    private val snapshots: Map<String, String> = emptyMap(),
    private val rootArtifactsDir: File? = null,
) {
    fun build(outputDir: File, redactionMode: RedactionMode): File {
        outputDir.mkdirs()
        val file = File(outputDir, "devicemasker_support_${timestamp()}.zip")
        val redactor = DiagnosticRedactor(redactionMode)
        ZipOutputStream(file.outputStream().buffered()).use { zip ->
            zip.writeText(
                "manifest.json",
                """{"mode":"MAXIMUM","redaction":"$redactionMode","createdAt":"${timestamp()}"}""",
            )
            zip.writeText(
                "README_REPRO.md",
                "Reproduce the issue, then attach this local support bundle.\n",
            )
            zip.writeJsonl("app/app_events.jsonl", appEvents, redactor)
            zip.writeJsonl("xposed/xposed_events.jsonl", xposedEvents, redactor)

            snapshots.forEach { (name, content) ->
                val prefix =
                    when {
                        name.startsWith("config") || name.startsWith("remote") -> "config"
                        name.startsWith("scope") -> "scope"
                        else -> "diagnostics"
                    }
                zip.writeText("$prefix/$name", content.redact(redactor))
            }

            rootArtifactsDir
                ?.walkTopDown()
                ?.filter { it.isFile }
                ?.forEach { artifact ->
                    val relative = artifact.relativeTo(rootArtifactsDir).invariantSeparatorsPath
                    zip.writeRedactedFile("root/$relative", artifact, redactor)
                }
        }
        return file
    }

    private fun ZipOutputStream.writeText(path: String, content: String) {
        putNextEntry(ZipEntry(path))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun ZipOutputStream.writeJsonl(
        path: String,
        lines: List<String>,
        redactor: DiagnosticRedactor,
    ) {
        putNextEntry(ZipEntry(path))
        lines.forEach { line ->
            write(line.redact(redactor).toByteArray(Charsets.UTF_8))
            write('\n'.code)
        }
        closeEntry()
    }

    private fun ZipOutputStream.writeRedactedFile(
        path: String,
        file: File,
        redactor: DiagnosticRedactor,
    ) {
        putNextEntry(ZipEntry(path))
        file.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.forEach { line ->
                write(line.redact(redactor).toByteArray(Charsets.UTF_8))
                write('\n'.code)
            }
        }
        closeEntry()
    }

    private fun String.redact(redactor: DiagnosticRedactor): String = redactor.redactMessage(this)

    private fun timestamp(): String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
}
