/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

rootProject.name = "Zhubin"

// برای رزولوشن پلاگین‌ها مثل com.android.application
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // اختیاری ولی معمول در پروژه‌های جدید اندروید
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
    }

    versionCatalogs {
        create("libs") {
            // اگر در ریشه همین پروژه است، احتمالاً بهتر است:
            // from(files("gradle/libs.versions.toml"))
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
