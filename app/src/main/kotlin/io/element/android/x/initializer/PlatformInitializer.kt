/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.x.initializer

import android.content.Context
import android.system.Os
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.preference.PreferenceManager
import androidx.startup.Initializer
import com.google.firebase.messaging.FirebaseMessaging
import io.element.android.features.rageshake.api.logs.createWriteToFilesConfiguration
import io.element.android.libraries.architecture.bindings
import io.element.android.libraries.featureflag.api.FeatureFlags
import io.element.android.libraries.matrix.api.tracing.TracingConfiguration
import io.element.android.x.di.AppBindings
import io.element.android.x.BuildConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import timber.log.Timber

private const val ELEMENT_X_TARGET = "elementx"

class PlatformInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        // Set Persian as default language
        try {
            val persianLocale = LocaleListCompat.forLanguageTags("fa")
            AppCompatDelegate.setApplicationLocales(persianLocale)
        } catch (t: Throwable) {
            // Can't use Timber yet, it's not initialized
        }

        val appBindings = context.bindings<AppBindings>()
        val tracingService = appBindings.tracingService()
        val platformService = appBindings.platformService()
        val bugReporter = appBindings.bugReporter()
        Timber.plant(tracingService.createTimberTree(ELEMENT_X_TARGET))
        
        Timber.i("Persian language set as default")
        
        val preferencesStore = appBindings.preferencesStore()
        val featureFlagService = appBindings.featureFlagService()
        val logLevel = runBlocking { preferencesStore.getTracingLogLevelFlow().first() }
        val tracingConfiguration = TracingConfiguration(
            writesToLogcat = runBlocking { featureFlagService.isFeatureEnabled(FeatureFlags.PrintLogsToLogcat) },
            writesToFilesConfiguration = bugReporter.createWriteToFilesConfiguration(),
            logLevel = logLevel,
            extraTargets = listOf(ELEMENT_X_TARGET),
            traceLogPacks = runBlocking { preferencesStore.getTracingLogPacksFlow().first() },
            sdkSentryDsn = appBindings.sentrySdkDsn()?.value?.takeIf { it.isNotBlank() },
        )
        bugReporter.setCurrentTracingLogLevel(logLevel.name)
        platformService.init(tracingConfiguration)
        // Also set env variable for rust back trace
        Os.setenv("RUST_BACKTRACE", "1", true)
        
        // Manual Firebase initialization AFTER Timber is set up.
        // Uses BuildConfig values populated in `app/build.gradle.kts` (kept in sync with google-services.json).
        // Use reflection to handle FOSS builds without Firebase
        try {
            initializeFirebase(context)
        } catch (t: Throwable) {
            Timber.w(t, "Firebase initialization skipped (not available in this build)")
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = mutableListOf()
}

private fun initializeFirebase(context: Context) {
    try {
        // Use reflection to load Firebase classes (not available in FOSS builds)
        val firebaseAppClass = Class.forName("com.google.firebase.FirebaseApp")
        val firebaseOptionsClass = Class.forName("com.google.firebase.FirebaseOptions")
        
        val getAppsMethod = firebaseAppClass.getMethod("getApps", Context::class.java)
        val initializeAppMethod = firebaseAppClass.getMethod("initializeApp", Context::class.java, firebaseOptionsClass)
        
        @Suppress("UNCHECKED_CAST")
        val existing = getAppsMethod.invoke(null, context) as List<Any>?
        
        if (existing?.isEmpty() != false) {
            Timber.i("Firebase not yet initialized, initializing with custom BuildConfig values")
        } else {
            Timber.i("Firebase already initialized (%d apps), deleting default app to reinitialize with custom values", existing?.size ?: 0)
            // Delete the default app if it exists so we can reinitialize with our custom values
            try {
                val getAppMethod = firebaseAppClass.getMethod("getInstance")
                val defaultApp = getAppMethod.invoke(null)
                if (defaultApp != null) {
                    firebaseAppClass.getMethod("delete").invoke(defaultApp)
                    Timber.i("Deleted default Firebase app to reinitialize")
                }
            } catch (e: Exception) {
                Timber.w(e, "Could not delete default Firebase app")
            }
        }
        
        // Build Firebase options with our custom BuildConfig values
        val builderClass = Class.forName("com.google.firebase.FirebaseOptions\$Builder")
        val builder = builderClass.getDeclaredConstructor().newInstance()
        
        builderClass.getMethod("setProjectId", String::class.java)
            .invoke(builder, BuildConfig.FIREBASE_PROJECT_ID)
        builderClass.getMethod("setApplicationId", String::class.java)
            .invoke(builder, BuildConfig.FIREBASE_APP_ID)
        builderClass.getMethod("setApiKey", String::class.java)
            .invoke(builder, BuildConfig.FIREBASE_API_KEY)
        builderClass.getMethod("setStorageBucket", String::class.java)
            .invoke(builder, BuildConfig.FIREBASE_STORAGE_BUCKET)
        
        val buildMethod = builderClass.getMethod("build")
        val options = buildMethod.invoke(builder)
        
        initializeAppMethod.invoke(null, context, options)
        Timber.i("Firebase initialized with custom BuildConfig values (Project ID: %s, App ID: %s)", BuildConfig.FIREBASE_PROJECT_ID, BuildConfig.FIREBASE_APP_ID)
        
        // Request FCM token REGARDLESS of whether we just initialized Firebase or it was already initialized
        Timber.i("About to request FirebaseMessaging.getInstance()")
        try {
            val messaging = FirebaseMessaging.getInstance()
            Timber.i("FirebaseMessaging.getInstance() succeeded, now calling .token")
            
            messaging.token.addOnCompleteListener { task ->
                Timber.i("[TOKEN_CALLBACK_INVOKED] FCM token callback fired. Success: ${task.isSuccessful}")
                
                try {
                    if (task.isSuccessful) {
                        val token = task.result
                        Timber.i("[TOKEN_SUCCESS] Token obtained: %s", token?.substring(0, Math.min(20, token?.length ?: 0)) + "...")
                        
                        Timber.i("[TOKEN_PREFS] Getting default SharedPreferences")
                        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                        Timber.i("[TOKEN_PREFS] Got SharedPreferences, now editing")
                        
                        val editor = prefs.edit()
                        Timber.i("[TOKEN_PREFS] Created editor, putting string with key=FCM_TOKEN")
                        editor.putString("FCM_TOKEN", token)
                        Timber.i("[TOKEN_PREFS] String put, now calling apply()")
                        editor.apply()
                        Timber.i("[TOKEN_STORED] FCM token successfully stored in SharedPreferences")
                    } else {
                        Timber.w(task.exception, "[TOKEN_FAILED] Task failed with exception")
                    }
                } catch (t: Throwable) {
                    Timber.e(t, "[TOKEN_ERROR] Exception in callback handler")
                }
            }
            
            Timber.i("[TOKEN_REQUEST] Token request added, callback is async - will fire when result is available")
        } catch (t: Throwable) {
            Timber.e(t, "[TOKEN_INIT_ERROR] Could not request FCM token - exception during getInstance() or addOnCompleteListener()")
        }
    } catch (e: ClassNotFoundException) {
        Timber.i("Firebase not available in this build variant")
    }
}
