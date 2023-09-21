buildscript {
    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:8.1.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.21")
        classpath("org.jetbrains.compose:compose-gradle-plugin:1.5.1")
    }
}

plugins {
    kotlin("multiplatform") version "1.9.10"
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

kotlin {
    jvm()
}