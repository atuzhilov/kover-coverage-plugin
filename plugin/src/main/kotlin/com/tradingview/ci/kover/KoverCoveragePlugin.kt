package com.tradingview.ci.kover

import com.tradingview.ci.kover.tasks.KoverCoverageTask
import com.tradingview.ci.kover.tasks.KoverModulesSummaryTask
import com.tradingview.ci.kover.utils.toSafeModuleSegment
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

class KoverCoveragePlugin : Plugin<Project> {

    private companion object {
        const val KOVER_XML_REPORT_CLASS = "kotlinx.kover.gradle.plugin.dsl.tasks.KoverXmlReport"
        const val DEBUG_VARIANT = "debug"
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

        project.gradle.projectsEvaluated {
            registerTasks(project, ext)
        }
    }

    private fun registerTasks(project: Project, ext: KoverCoverageExtension) {
        val koverReportClass = resolveKoverReportClass(project) ?: run {
            project.logger.warn("Kover plugin not found on project classpath, skipping coverage tasks")
            return
        }

        val excluded = ext.excludedProjects.get()
        val taskPrefix = ext.taskPrefix.get()
        val filePrefix = ext.filePrefix.get()

        val coverageProjects = project.subprojects
            .map { it.path }
            .filter { it !in excluded }
            .sorted()

        val perModuleProviders = coverageProjects.mapNotNull { projectPath ->
            val subproject = project.project(projectPath)

            val koverXmlTasks = subproject.tasks.matching { koverReportClass.isInstance(it) }
            if (koverXmlTasks.isEmpty()) return@mapNotNull null

            val selectedTask = selectDebugTask(koverXmlTasks) ?: return@mapNotNull null

            val safe = projectPath.toSafeModuleSegment()
            val fileName = filePrefix + safe + ".txt"

            project.tasks.register(
                taskPrefix + safe,
                KoverCoverageTask::class.java
            ) {
                reportFile.set(
                    subproject.tasks.named(selectedTask.name).map { task ->
                        project.layout.projectDirectory.file(
                            task.outputs.files.singleFile.absolutePath
                        )
                    }
                )
                outputFile.set(project.layout.buildDirectory.file(fileName))
            }
        }

        val summaryTask = project.tasks.register(
            "koverAllModulesCoverageReport",
            KoverModulesSummaryTask::class.java
        ) {
            mustRunAfter(perModuleProviders)
            coverageDir.set(project.layout.buildDirectory)
            moduleFilePrefix.set(filePrefix)
            outputFile.set(
                project.layout.buildDirectory.file("coverageAllModulesSummary.txt")
            )
        }

        project.tasks.register("koverAllModuleCoverage") {
            dependsOn(perModuleProviders)
            finalizedBy(summaryTask)
        }
    }

    private fun resolveKoverReportClass(project: Project): Class<*>? {
        val classLoaders = (sequenceOf(project) + project.subprojects.asSequence())
            .flatMap { it.plugins.asSequence() }
            .map { it::class.java.classLoader }
            .distinct()

        return classLoaders.firstNotNullOfOrNull { classLoader ->
            try {
                classLoader.loadClass(KOVER_XML_REPORT_CLASS)
            } catch (_: ClassNotFoundException) {
                null
            }
        }
    }

    private fun selectDebugTask(tasks: Iterable<Task>): Task? {
        val debugTask = tasks.firstOrNull { task ->
            try {
                task::class.java.getMethod("getVariantName").invoke(task) == DEBUG_VARIANT
            } catch (_: ReflectiveOperationException) {
                false
            }
        }
        return debugTask ?: tasks.firstOrNull()
    }
}
