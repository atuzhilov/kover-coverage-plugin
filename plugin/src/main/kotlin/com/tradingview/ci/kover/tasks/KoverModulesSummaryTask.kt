package com.tradingview.ci.kover.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class KoverModulesSummaryTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional
    abstract val coverageDir: DirectoryProperty

    @get:Input
    abstract val moduleFilePrefix: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun run() {
        val prefix = moduleFilePrefix.get()
        val dir = coverageDir.get().asFile

        val coverageFiles = dir.listFiles { file ->
            file.isFile && file.name.startsWith(prefix) && file.extension == "txt"
        }?.sortedBy { it.name } ?: emptyList()

        val sb = StringBuilder()

        for (file in coverageFiles) {
            val moduleName = file.nameWithoutExtension
                .removePrefix(prefix)
                .replace("_dash_", "-")
                .replace("__", "\u0000")
                .replace('_', ':')
                .replace("\u0000", "_")

            sb.appendLine("=== $moduleName ===")
            sb.appendLine(file.readText().trimEnd())
            sb.appendLine()
        }

        if (coverageFiles.isEmpty()) {
            sb.appendLine("No per-module coverage files found in ${dir.absolutePath}")
        }

        val out = outputFile.get().asFile
        out.parentFile?.mkdirs()
        out.writeText(sb.toString())

        logger.lifecycle(
            "Coverage summary: ${coverageFiles.size} modules written to ${out.absolutePath}"
        )
    }
}
