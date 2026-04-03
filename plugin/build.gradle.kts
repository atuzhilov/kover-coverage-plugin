plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    alias(libs.plugins.publish.plugin)
}

group = libs.plugins.kover.coverage.get().pluginId

gradlePlugin {
    plugins.register("kover-coverage") {
        id = libs.plugins.kover.coverage.get().pluginId
        version = libs.versions.kover.coverage.get()
        implementationClass = "com.tradingview.ci.kover.KoverCoveragePlugin"
        displayName = "Kover Coverage Reporter"
        description = "Registers per-module and summary Kover coverage tasks"
    }
}

val koverVersion: String = libs.versions.kover.plugin.get()

dependencies {
    implementation(gradleApi())
    compileOnly("org.jetbrains.kotlinx:kover-gradle-plugin:$koverVersion")
}