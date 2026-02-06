// Top-level build file
plugins {
    id("com.android.application") version "8.8.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.23" apply false
    id("com.google.devtools.ksp") version "1.9.23-1.0.20" apply false
}

// In your /build.gradle.kts file
buildscript {
    dependencies {
        val nav_version = "2.9.5" // Use the latest stable version
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:$nav_version")
    }
}

tasks.register("clean", Delete::class) {
    delete(layout.buildDirectory)
}
