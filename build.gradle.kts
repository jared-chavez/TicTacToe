// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // 1. Plugins de Android y Kotlin para los módulos
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    // 2. Plugin de Kotlin para el módulo Wear (importante para Compose)
    alias(libs.plugins.kotlin.compose) apply false
}

extra["compose_version"] = "1.6.1"