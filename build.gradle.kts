// Pin the Kotlin toolchain used by AGP 9's built-in Kotlin support and by the
// Compose compiler / KSP plugins so that every Kotlin-based tool agrees on
// Kotlin 2.4.10 (the Compose compiler plugin must match the compiler version).
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.10")
        classpath("com.google.devtools.ksp:symbol-processing-gradle-plugin:2.3.10")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}