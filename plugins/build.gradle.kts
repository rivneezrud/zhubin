/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
buildscript {
    repositories {
        maven(url = "https://plugins.gradle.org/m2/")
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    dependencies {
        // Resolve the module (not a raw JAR file) so runtime transitive dependencies
        // such as kotlin-gradle-plugin are also available on the script classpath.
        classpath("org.gradle.kotlin:gradle-kotlin-dsl-plugins:6.4.1")
    }
}

apply(plugin = "org.gradle.kotlin.kotlin-dsl")
apply(plugin = "org.gradle.kotlin.kotlin-dsl.precompiled-script-plugins")

dependencies {
    add("implementation", libs.android.gradle.plugin)
    add("implementation", libs.kotlin.gradle.plugin)
    add("implementation", libs.kover.gradle.plugin)
    add("implementation", platform(libs.google.firebase.bom))
    add("implementation", libs.firebase.appdistribution.gradle)
    add("implementation", files(libs.javaClass.superclass.protectionDomain.codeSource.location))
    add("implementation", libs.autonomousapps.dependencyanalysis.plugin)
    add("implementation", libs.metro.gradle.plugin)
    add("implementation", libs.ksp.gradle.plugin)
    add("implementation", libs.compose.compiler.plugin)
}
