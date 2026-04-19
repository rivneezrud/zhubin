/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

import org.gradle.api.JavaVersion
import org.gradle.jvm.toolchain.JavaLanguageVersion

/**
 * Version codes are quite sensitive, because there is a mix between bundle and APKs.
 * Max versionCode allowed by the PlayStore (for information):
 * 2_100_000_000
 *
 * Also note that the versionCode is multiplied by 10 in app/build.gradle.kts:
 * ```
 * output.versionCode.set((output.versionCode.orNull ?: 0) * 10 + abiCode)
 * ```
 *
 * Zhubin uses a simple SemVer-like scheme. Update the values below to change the
 * app version name and code.
 */

/**
 * Major version. Value must be in [0,99].
 */
private const val versionMajor = 1

/**
 * Minor version. Value must be in [0,99].
 */ 
private const val versionMinor = 1

/**
 * Patch version. Value must be in [0,99].
 */
private const val versionPatch = 2

object Versions {
    /**
     * Base version code that will be set in the Android Manifest.
     * The value will be modified at build time to add the ABI code when APK are build.
     * AAB will have a ABI code of 0.
     * See comment above for the calculation method.
     */
    const val VERSION_CODE = versionMajor * 10_000 + versionMinor * 100 + versionPatch
    val VERSION_NAME = "$versionMajor.$versionMinor.$versionPatch"

    /**
     * Compile SDK version. Must be updated when a new Android version is released.
     * When updating COMPILE_SDK, please also update BUILD_TOOLS_VERSION.
     */
    const val COMPILE_SDK = 36

    /**
     * Build tools version. Must be kept in sync with COMPILE_SDK.
     * The value is used by the release script.
     */
    @Suppress("unused")
    private const val BUILD_TOOLS_VERSION = "36.0.0"

    /**
     * Target SDK version. Should be kept up to date with COMPILE_SDK.
     */
    const val TARGET_SDK = 36

    /**
     * Minimum SDK version for FOSS builds.
     */
    private const val MIN_SDK_FOSS = 24

    /**
     * Minimum SDK version for Enterprise builds.
     */
    private const val MIN_SDK_ENTERPRISE = 33

    /**
     * minSdkVersion that will be set in the Android Manifest.
     */
    val minSdk = if (isEnterpriseBuild) MIN_SDK_ENTERPRISE else MIN_SDK_FOSS

    /**
     * Java version used for compilation.
     * Update this value when you want to use a newer Java version.
     */
    private const val JAVA_VERSION = 21

    val javaVersion: JavaVersion = JavaVersion.toVersion(JAVA_VERSION)
    val javaLanguageVersion: JavaLanguageVersion = JavaLanguageVersion.of(JAVA_VERSION)

    // Perform some checks on the values to avoid releasing with bad values
    init {
        require(versionMajor in 0..99) { "versionMajor must be in [0,99]" }
        require(versionMinor in 0..99) { "versionMinor must be in [0,99]" }
        require(versionPatch in 0..99) { "versionPatch must be in [0,99]" }
        require(BUILD_TOOLS_VERSION.startsWith(COMPILE_SDK.toString())) { "When updating COMPILE_SDK, please also update BUILD_TOOLS_VERSION" }
    }
}
