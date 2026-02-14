/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.pushproviders.firebase

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import io.element.android.libraries.core.log.logger.LoggerTag
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.pushproviders.api.Config
import io.element.android.libraries.pushproviders.api.Distributor
import io.element.android.libraries.pushproviders.api.PushProvider
import io.element.android.libraries.pushproviders.api.PusherSubscriber
import timber.log.Timber

private val loggerTag = LoggerTag("FirebasePushProvider", LoggerTag.PushLoggerTag)

@ContributesIntoSet(AppScope::class)
class FirebasePushProvider(
    private val firebaseStore: FirebaseStore,
    private val pusherSubscriber: PusherSubscriber,
    private val isPlayServiceAvailable: IsPlayServiceAvailable,
    private val firebaseTokenRotator: FirebaseTokenRotator,
    private val firebaseTokenGetter: FirebaseTokenGetter,
    private val firebaseGatewayProvider: FirebaseGatewayProvider,
) : PushProvider {
    override val index = FirebaseConfig.INDEX
    override val name = FirebaseConfig.NAME
    override val supportMultipleDistributors = false

    override fun getDistributors(): List<Distributor> {
        return listOfNotNull(
            firebaseDistributor.takeIf { isPlayServiceAvailable.isAvailable() }
        )
    }

    override suspend fun registerWith(matrixClient: MatrixClient, distributor: Distributor): Result<Unit> {
        var pushKey = firebaseStore.getFcmToken()
        
        // If no token is stored, fetch a fresh one from Firebase
        if (pushKey == null) {
            try {
                pushKey = firebaseTokenGetter.get()
                Timber.tag(loggerTag.value).d("Fetched fresh Firebase token")
            } catch (e: Exception) {
                Timber.tag(loggerTag.value).e(e, "Failed to fetch Firebase token")
                return Result.failure(
                    IllegalStateException(
                        "Unable to register pusher, Firebase token could not be generated."
                    )
                )
            }
        }
        
        return pusherSubscriber.registerPusher(
            matrixClient = matrixClient,
            pushKey = pushKey,
            gateway = firebaseGatewayProvider.getFirebaseGateway(),
        )
    }

    override suspend fun getCurrentDistributorValue(sessionId: SessionId): String = firebaseDistributor.value

    override suspend fun getCurrentDistributor(sessionId: SessionId) = firebaseDistributor

    override suspend fun unregister(matrixClient: MatrixClient): Result<Unit> {
        val pushKey = firebaseStore.getFcmToken()
        return if (pushKey == null) {
            Timber.tag(loggerTag.value).w("Unable to unregister pusher, Firebase token is not known.")
            Result.success(Unit)
        } else {
            pusherSubscriber.unregisterPusher(matrixClient, pushKey, firebaseGatewayProvider.getFirebaseGateway())
        }
    }

    /**
     * Nothing to clean up here.
     */
    override suspend fun onSessionDeleted(sessionId: SessionId) = Unit

    override suspend fun getPushConfig(sessionId: SessionId): Config? {
        return firebaseStore.getFcmToken()?.let { fcmToken ->
            Config(
                url = firebaseGatewayProvider.getFirebaseGateway(),
                pushKey = fcmToken
            )
        }
    }

    override fun canRotateToken(): Boolean = true

    override suspend fun rotateToken(): Result<Unit> {
        return firebaseTokenRotator.rotate()
    }

    companion object {
        private val firebaseDistributor = Distributor("Firebase", "Firebase")
    }
    
    /**
     * Initialize Firebase token generation at app startup.
     * This ensures a token is fetched from Firebase even before the user logs in.
     */
    suspend fun initializeToken() {
        if (firebaseStore.getFcmToken() == null && isPlayServiceAvailable.isAvailable()) {
            try {
                firebaseTokenGetter.get()
                Timber.tag(loggerTag.value).d("Firebase token initialized successfully")
            } catch (e: Exception) {
                Timber.tag(loggerTag.value).w(e, "Failed to initialize Firebase token at startup")
                // Don't fail the app startup, token will be fetched when needed
            }
        }
    }
}
