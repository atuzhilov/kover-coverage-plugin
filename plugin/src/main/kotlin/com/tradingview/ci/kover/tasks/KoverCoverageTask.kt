package com.tradingview.ci.kover.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import javax.xml.parsers.DocumentBuilderFactory

abstract class KoverCoverageTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val reportFile: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun run() {
        val xml = reportFile.get().asFile
        val doc = DocumentBuilderFactory.newInstance().apply {
            isValidating = false
            isNamespaceAware = false
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        }.newDocumentBuilder().parse(xml)

        val counters = doc.documentElement.childNodes
        var lineMissed = 0L
        var lineCovered = 0L
        var branchMissed = 0L
        var branchCovered = 0L

        for (i in 0 until counters.length) {
            val node = counters.item(i)
            if (node.nodeName != "counter") continue
            val attrs = node.attributes
            when (attrs.getNamedItem("type")?.nodeValue) {
                "LINE" -> {
                    lineMissed = attrs.getNamedItem("missed")?.nodeValue?.toLongOrNull() ?: 0L
                    lineCovered = attrs.getNamedItem("covered")?.nodeValue?.toLongOrNull() ?: 0L
                }
                "BRANCH" -> {
                    branchMissed = attrs.getNamedItem("missed")?.nodeValue?.toLongOrNull() ?: 0L
                    branchCovered = attrs.getNamedItem("covered")?.nodeValue?.toLongOrNull() ?: 0L
                }
            }
        }

        val lineTotal = lineMissed + lineCovered
        val branchTotal = branchMissed + branchCovered

        val linePct = if (lineTotal > 0) lineCovered * 100.0 / lineTotal else 0.0
        val branchPct = if (branchTotal > 0) branchCovered * 100.0 / branchTotal else 0.0

        val out = outputFile.get().asFile
        out.parentFile?.mkdirs()
        out.writeText(
            buildString {
                appendLine("Line coverage:   ${"%.2f".format(linePct)}% ($lineCovered/$lineTotal)")
                appendLine("Branch coverage: ${"%.2f".format(branchPct)}% ($branchCovered/$branchTotal)")
            }
        )

        logger.lifecycle("Coverage written to ${out.absolutePath}")
    }
}

