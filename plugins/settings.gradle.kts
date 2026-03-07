/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

rootProject.name = "Zhubin"

pluginManagement {
    repositories {
        // Direct Maven endpoint for Gradle plugins. Keeping this explicit helps in
        // environments where marker lookup via the plugin portal alias can fail.
        maven(url = "https://plugins.gradle.org/m2/")
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
    }

    versionCatalogs {
        create("libs") {
            // from(files("gradle/libs.versions.toml"))
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
