package com.tradingview.ci.kover.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class KoverModulesSummaryTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    @get:Optional
    abstract val inputFiles: ConfigurableFileCollection

    @get:Input
    abstract val moduleFilePrefix: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val prefix = moduleFilePrefix.get()

        val files = inputFiles.files
            .filter { it.exists() && it.isFile }
            .sortedBy { it.name }

        val rows = files.mapNotNull { file ->
            parseLineCoverage(file)?.let { percent ->
                val moduleName = file.nameWithoutExtension
                    .removePrefix(prefix)
                    .replace("_dash_", "-")
                    .replace("__", "\u0000")
                    .replace('_', ':')
                    .replace("\u0000", "_")
                Row(moduleName = moduleName, percent = percent)
            }
        }

        val table = buildString {
            appendLine("Kover coverage summary by module")
            appendLine("Sources: ${rows.size} module file(s)")
            appendLine()

            val wModule = maxOf("module".length, rows.maxOfOrNull { it.moduleName.length } ?: 0)
            val header = "%-${wModule}s | %8s".format("module", "coverage")
            appendLine(header)
            appendLine("-".repeat(header.length))

            rows.sortedByDescending { it.percent }.forEach { row ->
                appendLine("%-${wModule}s | %7.2f%%".format(row.moduleName, row.percent))
            }
            appendLine()

            if (rows.isNotEmpty()) {
                appendLine("Average (unweighted): ${"%.2f".format(rows.map(Row::percent).average())}%")
            }
        }

        val out = outputFile.get().asFile
        out.parentFile?.mkdirs()
        out.writeText(table)
        logger.lifecycle("Wrote Kover modules summary to: ${out.absolutePath}")
    }

    private fun parseLineCoverage(file: File): Double? {
        for (line in file.readLines()) {
            LINE_COVERAGE_REGEX.find(line)?.let {
                return it.groupValues[1].toDoubleOrNull()
            }
            TOTAL_COVERAGE_REGEX.find(line)?.let {
                return it.groupValues[1].toDoubleOrNull()
            }
        }
        return null
    }

    private data class Row(val moduleName: String, val percent: Double)

    private companion object {
        val LINE_COVERAGE_REGEX = """Line coverage:\s+([0-9]+(?:\.[0-9]+)?)%""".toRegex()
        val TOTAL_COVERAGE_REGEX = """Total Code Coverage:\s*([0-9]+(?:\.[0-9]+)?)%""".toRegex()
    }
}
