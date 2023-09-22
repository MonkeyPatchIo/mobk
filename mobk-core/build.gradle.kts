plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
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
            baseName = "mobk-core"
        }

    }
    sourceSets {
        val commonMain by getting{
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation("org.jetbrains.kotlinx:atomicfu:0.21.0")
            }

        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val iosMain by getting
        val iosTest by getting
    }
}

android {
    namespace = "io.monkeypatch.mobk.core"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
}



publishing {
    repositories {
        maven{
            url = uri("https://api.bintray.com/maven/alexandre-delattre/MonkeyPatchLibs/mobk-core/;publish=1")
            credentials {
                username = project.findProperty("bintrayUsername") as String? ?: ""
                password = project.findProperty("bintrayPassword") as String? ?: ""
            }
        }
    }
}