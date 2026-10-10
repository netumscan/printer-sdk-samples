// SPDX-License-Identifier: MIT
pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        exclusiveContent {
            forRepository { maven { url = uri("https://repo.maven.apache.org/maven2") } }
            filter { includeModule("com.netumscan", "printer-sdk-android") }
        }
        google()
        mavenCentral()
    }
}
rootProject.name = "printer-sdk-samples"
include(":quick-start", ":full-demo")
