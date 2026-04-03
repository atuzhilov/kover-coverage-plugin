package com.tradingview.ci.kover

import com.tradingview.ci.kover.tasks.KoverCoverageTask
import com.tradingview.ci.kover.tasks.KoverModulesSummaryTask
import com.tradingview.ci.kover.utils.toSafeModuleSegment
import org.gradle.api.Plugin
import org.gradle.api.Project

class KoverCoveragePlugin : Plugin<Project> {

    private companion object {
        const val KOVER_XML_DEBUG_TASK = "koverXmlReportDebug"
        const val LIFECYCLE_TASK_NAME = "koverAllModuleCoverage"
        const val SUMMARY_TASK_NAME = "koverAllModulesCoverageReport"
    }

    override fun apply(project: Project) {
        val ext = project.extensions.create(
            "koverCoverage",
            KoverCoverageExtension::class.java
        ).apply {
            taskPrefix.convention("koverModuleCoverage_")
            filePrefix.convention("coverage_")
            excludedProjects.convention(emptySet())
        }

        val taskPrefix = ext.taskPrefix.get()
        val filePrefix = ext.filePrefix.get()

        val summaryTask = project.tasks.register(
            SUMMARY_TASK_NAME,
            KoverModulesSummaryTask::class.java
        ) {
            coverageDir.set(project.layout.buildDirectory)
            moduleFilePrefix.set(filePrefix)
            outputFile.set(
                project.layout.buildDirectory.file("coverageAllModulesSummary.txt")
            )
        }

        val lifecycleTask = project.tasks.register(LIFECYCLE_TASK_NAME) {
            finalizedBy(summaryTask)
        }

        project.subprojects.forEach { subproject ->
            val safe = subproject.path.toSafeModuleSegment()
            val perModuleTaskName = taskPrefix + safe
            val fileName = filePrefix + safe + ".txt"

            val perModuleTask = project.tasks.register(
                perModuleTaskName,
                KoverCoverageTask::class.java
            ) {

                if (subproject.path in ext.excludedProjects.get()) {
                    enabled = false
                    return@register
                }

                val koverXmlTasks = subproject.tasks.matching {
                    it.name == KOVER_XML_DEBUG_TASK
                }
                if (koverXmlTasks.isEmpty()) {
                    enabled = false
                    return@register
                }

                reportFile.set(
                    subproject.tasks.named(KOVER_XML_DEBUG_TASK).map { task ->
                        project.layout.projectDirectory.file(
                            task.outputs.files.singleFile.absolutePath
                        )
                    }
                )
                outputFile.set(project.layout.buildDirectory.file(fileName))
            }

            lifecycleTask.configure { dependsOn(perModuleTask) }
        }
    }
}
