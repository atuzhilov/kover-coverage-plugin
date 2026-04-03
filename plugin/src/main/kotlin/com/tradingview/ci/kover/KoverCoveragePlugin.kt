package com.tradingview.ci.kover

import com.tradingview.ci.kover.tasks.KoverCoverageTask
import com.tradingview.ci.kover.tasks.KoverModulesSummaryTask
import com.tradingview.ci.kover.utils.toSafeModuleSegment
import org.gradle.api.Plugin
import org.gradle.api.Project

class KoverCoveragePlugin : Plugin<Project> {

    private companion object {
        const val KOVER_XML_TASK_PREFIX = "koverXmlReport"
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

        project.tasks.register(SUMMARY_TASK_NAME, KoverModulesSummaryTask::class.java)
        project.tasks.register(LIFECYCLE_TASK_NAME)

        project.gradle.projectsEvaluated {
            val perModuleProviders = discoverAndRegisterPerModuleTasks(project, ext)

            val lifecycleTask = project.tasks.getByName(LIFECYCLE_TASK_NAME)
            perModuleProviders.forEach { provider ->
                lifecycleTask.dependsOn(provider.get())
            }
            lifecycleTask.finalizedBy(SUMMARY_TASK_NAME)

            project.logger.lifecycle(
                "Kover: lifecycle task deps count = ${lifecycleTask.dependsOn.size}"
            )

            val summaryTask = project.tasks.getByName(SUMMARY_TASK_NAME) as KoverModulesSummaryTask
            summaryTask.mustRunAfter(perModuleProviders)
            summaryTask.coverageDir.set(project.layout.buildDirectory)
            summaryTask.moduleFilePrefix.set(ext.filePrefix.get())
            summaryTask.outputFile.set(
                project.layout.buildDirectory.file("coverageAllModulesSummary.txt")
            )

            project.logger.lifecycle(
                "Kover coverage: registered ${perModuleProviders.size} per-module tasks"
            )
        }
    }

    private fun discoverAndRegisterPerModuleTasks(
        project: Project,
        ext: KoverCoverageExtension,
    ) = project.subprojects
        .map { it.path }
        .filter { it !in ext.excludedProjects.get() }
        .sorted()
        .mapNotNull { projectPath ->
            val subproject = project.project(projectPath)

            val koverXmlTaskName = subproject.tasks.names
                .filter { it.startsWith(KOVER_XML_TASK_PREFIX) }
                .let { names ->
                    names.find { it == KOVER_XML_DEBUG_TASK } ?: names.firstOrNull()
                } ?: return@mapNotNull null

            val safe = projectPath.toSafeModuleSegment()
            val fileName = ext.filePrefix.get() + safe + ".txt"

            project.tasks.register(
                ext.taskPrefix.get() + safe,
                KoverCoverageTask::class.java
            ) {
                reportFile.set(
                    subproject.tasks.named(koverXmlTaskName).map { task ->
                        project.layout.projectDirectory.file(
                            task.outputs.files.singleFile.absolutePath
                        )
                    }
                )
                outputFile.set(project.layout.buildDirectory.file(fileName))
            }
        }
}
