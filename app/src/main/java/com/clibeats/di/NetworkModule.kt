@file:Suppress("ForbiddenImport")

package com.clibeats.di

import android.content.Context
import com.clibeats.BuildConfig
import com.clibeats.data.network.NetworkMonitor
import com.clibeats.data.provider.api.AudiusApi
import com.clibeats.data.provider.api.InnerTubeApi
import com.clibeats.data.provider.api.InnerTubeHeaderInterceptor
import com.clibeats.data.provider.api.InternetArchiveApi
import com.clibeats.data.provider.api.JamendoApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

private const val INTERNET_ARCHIVE_BASE_URL = "https://archive.org/"
private const val JAMENDO_BASE_URL = "https://api.jamendo.com/v3.0/"
private const val INNERTUBE_BASE_URL = "https://music.youtube.com/youtubei/v1/"
private const val READ_TIMEOUT_SECONDS = 20L
private const val CONNECT_TIMEOUT_SECONDS = 15L

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideJson(): Json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder =
            OkHttpClient.Builder()
                .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                },
            )
        }
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(AudiusApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideAudiusApi(retrofit: Retrofit): AudiusApi = retrofit.create(AudiusApi::class.java)

    @Provides
    @Singleton
    fun provideInternetArchiveApi(
        okHttpClient: OkHttpClient,
        json: Json,
    ): InternetArchiveApi =
        Retrofit.Builder()
            .baseUrl(INTERNET_ARCHIVE_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(InternetArchiveApi::class.java)

    @Provides
    @Singleton
    fun provideJamendoApi(
        okHttpClient: OkHttpClient,
        json: Json,
    ): JamendoApi =
        Retrofit.Builder()
            .baseUrl(JAMENDO_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(JamendoApi::class.java)

    @Provides
    @Singleton
    fun provideInnerTubeHeaderInterceptor(): InnerTubeHeaderInterceptor = InnerTubeHeaderInterceptor()

    @Provides
    @Singleton
    @Named(INNERTUBE_OKHTTP_QUALIFIER)
    fun provideInnerTubeOkHttpClient(innerTubeHeaderInterceptor: InnerTubeHeaderInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(innerTubeHeaderInterceptor)
            .build()

    @Provides
    @Singleton
    fun provideInnerTubeApi(
        @Named(INNERTUBE_OKHTTP_QUALIFIER) innerTubeOkHttpClient: OkHttpClient,
        json: Json,
    ): InnerTubeApi =
        Retrofit.Builder()
            .baseUrl(INNERTUBE_BASE_URL)
            .client(innerTubeOkHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(InnerTubeApi::class.java)

    const val INNERTUBE_OKHTTP_QUALIFIER = "innertube_okhttp_client"

    @Provides
    @Singleton
    fun provideNetworkMonitor(
        @ApplicationContext context: Context,
    ): NetworkMonitor = NetworkMonitor(context)
}
