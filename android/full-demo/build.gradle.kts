// SPDX-License-Identifier: MIT
plugins { id("com.android.application") }

android {
    namespace = "com.netumscan.printersdk.demo"
    compileSdk = 35
    defaultConfig {
        // Preserve the existing Demo installation identity. Public SDK imports use netumscan.
        applicationId = "com.netum.printersdk.demo"
        minSdk = 21
        targetSdk = 35
        versionCode = 2
        versionName = "0.3.0-preview.2"
        testInstrumentationRunner = "com.netumscan.printersdk.samples.runtime.RuntimeGate"
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64") }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
dependencies { implementation("com.netumscan:printer-sdk-android:0.3.0-preview.2") }

tasks.register("copyResolvedSdk") {
    doLast {
        val sdk = configurations["releaseRuntimeClasspath"].resolvedConfiguration.resolvedArtifacts.single {
            it.moduleVersion.id.group == "com.netumscan" && it.name == "printer-sdk-android"
        }
        require(sdk.moduleVersion.id.version == "0.3.0-preview.2")
        sdk.file.copyTo(layout.buildDirectory.file("verification/resolved-sdk.aar").get().asFile, overwrite = true)
    }
}
