/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.changepassword

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.element.android.libraries.androidutils.json.JsonProvider
import io.element.android.libraries.core.uri.ensureTrailingSlash
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.network.interceptors.UserAgentInterceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

interface ChangePasswordApiFactory {
    fun create(baseUrl: String): ChangePasswordApi
}

@ContributesBinding(SessionScope::class)
class DefaultChangePasswordApiFactory(
    private val secureRetrofitFactory: SecureRetrofitFactory,
) : ChangePasswordApiFactory {
    override fun create(baseUrl: String): ChangePasswordApi {
        return secureRetrofitFactory.create(baseUrl)
            .create(ChangePasswordApi::class.java)
    }
}

@Inject
class SecureRetrofitFactory(
    private val userAgentInterceptor: UserAgentInterceptor,
    private val jsonProvider: JsonProvider,
) {
    // Keep this client isolated from dynamic HTTP body logging to ensure passwords are never logged.
    private val secureClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(userAgentInterceptor)
            .build()
    }

    fun create(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl.ensureTrailingSlash())
            .addConverterFactory(jsonProvider().asConverterFactory("application/json".toMediaType()))
            .callFactory { request -> secureClient.newCall(request) }
            .build()
    }
}
