buildscript {
    dependencies {
        // AGP 9 has built-in Kotlin. Pin KGP so the Compose compiler plugin
        // and built-in Kotlin use the same current Kotlin release.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21")
    }
}

plugins {
    id("com.android.application") version "9.3.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
}
