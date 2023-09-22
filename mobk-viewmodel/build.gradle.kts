plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("maven-publish")
}


group = "io.monkeypatch"
version = "0.0.11"

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
    targetHierarchy.default()

    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }

        publishLibraryVariants("release")
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "mobk-viewmodel"

            export("dev.icerock.moko:mvvm-core:0.16.1")
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":mobk-core"))
                api("dev.icerock.moko:mvvm-core:0.16.1")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

android {
    namespace = "io.monkeypatch.mobk.viewmodel"
    compileSdk = 33
    defaultConfig {
        minSdk = 24
    }
}