package com.clibeats.di

import android.content.Context
import com.clibeats.data.cache.CacheManager
import com.clibeats.data.local.dao.CacheIndexDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CacheModule {
    @Provides
    @Singleton
    fun provideCacheManager(
        @ApplicationContext context: Context,
        cacheIndexDao: CacheIndexDao,
    ): CacheManager = CacheManager(context, cacheIndexDao)
}
