// SPDX-License-Identifier: MIT
plugins { id("com.android.application") }

android {
    namespace = "com.netumscan.printersdk.samples.quickstart"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.netumscan.printersdk.samples.quickstart"
        minSdk = 21
        targetSdk = 35
        versionCode = 2
        versionName = "0.3.0-preview.16"
        testInstrumentationRunner = "com.netumscan.printersdk.samples.runtime.QuickStartRuntimeGate"
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
dependencies { implementation("com.netumscan:printer-sdk-android:0.3.0-preview.16") }

tasks.register("copyResolvedSdk") {
    doLast {
        val sdk = configurations["releaseRuntimeClasspath"].resolvedConfiguration.resolvedArtifacts.single {
            it.moduleVersion.id.group == "com.netumscan" && it.name == "printer-sdk-android"
        }
        require(sdk.moduleVersion.id.version == "0.3.0-preview.16")
        sdk.file.copyTo(layout.buildDirectory.file("verification/resolved-sdk.aar").get().asFile, overwrite = true)
    }
}
