package com.tradingview.ci.kover.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class KoverModulesSummaryTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val inputFiles: ConfigurableFileCollection

    @get:Input
    abstract val moduleFilePrefix: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun run() {
        val prefix = moduleFilePrefix.get()
        val sb = StringBuilder()

        val sortedFiles = inputFiles.files.sortedBy { it.name }
        for (file in sortedFiles) {
            if (!file.exists()) return@run
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

        val out = outputFile.get().asFile
        out.parentFile?.mkdirs()
        out.writeText(sb.toString())

        logger.lifecycle("Coverage summary written to ${out.absolutePath}")
    }
}

