package com.tradingview.ci.kover

import org.gradle.api.provider.SetProperty
import org.gradle.api.provider.Property

interface KoverCoverageExtension {
    val excludedProjects: SetProperty<String>
    val taskPrefix: Property<String>
    val filePrefix: Property<String>
}

