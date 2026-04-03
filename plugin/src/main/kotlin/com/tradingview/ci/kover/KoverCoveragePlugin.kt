package com.tradingview.ci.kover

import com.tradingview.ci.kover.tasks.KoverCoverageTask
import com.tradingview.ci.kover.tasks.KoverModulesSummaryTask
import com.tradingview.ci.kover.utils.toSafeModuleSegment
import kotlinx.kover.gradle.plugin.dsl.tasks.KoverXmlReport
import org.gradle.api.Plugin
import org.gradle.api.Project

class KoverCoveragePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val ext = project.extensions.create(
            "koverCoverage",
            KoverCoverageExtension::class.java
        ).apply {
            taskPrefix.convention("koverModuleCoverage_")
            filePrefix.convention("coverage_")
            excludedProjects.convention(emptySet())
        }

        project.afterEvaluate {
            registerTasks(project, ext)
        }
    }

    private fun registerTasks(project: Project, ext: KoverCoverageExtension) {
        val excluded = ext.excludedProjects.get()
        val taskPrefix = ext.taskPrefix.get()
        val filePrefix = ext.filePrefix.get()

        val coverageProjects = project.subprojects
            .map { it.path }
            .filter { it !in excluded }
            .sorted()

        val perModuleProviders = coverageProjects.mapNotNull { projectPath ->
            val koverXmlTasks = project.project(projectPath).tasks
                .withType(KoverXmlReport::class.java)
            if (koverXmlTasks.isEmpty()) return@mapNotNull null

            val safe = projectPath.toSafeModuleSegment()
            val fileName = filePrefix + safe + ".txt"

            project.tasks.register(
                taskPrefix + safe,
                KoverCoverageTask::class.java
            ) {
                val debugTasks = koverXmlTasks.matching { it.variantName == "debug" }
                val selected = if (debugTasks.isNotEmpty())
                    debugTasks.first() else koverXmlTasks.first()

                reportFile.set(
                    koverXmlTasks.named(selected.name).map { task ->
                        project.layout.projectDirectory.file(
                            task.outputs.files.singleFile.absolutePath
                        )
                    }
                )
                outputFile.set(project.layout.buildDirectory.file(fileName))
            }
        }

        project.tasks.register(
            "koverAllModulesCoverageReport",
            KoverModulesSummaryTask::class.java
        ) {
            perModuleProviders.forEach { provider ->
                inputFiles.from(provider.flatMap { it.outputFile })
            }
            moduleFilePrefix.set(filePrefix)
            outputFile.set(
                project.layout.buildDirectory.file("coverageAllModulesSummary.txt")
            )
        }
    }
}

