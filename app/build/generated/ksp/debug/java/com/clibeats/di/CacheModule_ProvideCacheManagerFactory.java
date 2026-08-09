package com.clibeats.di;

import android.content.Context;
import com.clibeats.data.cache.CacheManager;
import com.clibeats.data.local.dao.CacheIndexDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class CacheModule_ProvideCacheManagerFactory implements Factory<CacheManager> {
  private final Provider<Context> contextProvider;

  private final Provider<CacheIndexDao> cacheIndexDaoProvider;

  public CacheModule_ProvideCacheManagerFactory(Provider<Context> contextProvider,
      Provider<CacheIndexDao> cacheIndexDaoProvider) {
    this.contextProvider = contextProvider;
    this.cacheIndexDaoProvider = cacheIndexDaoProvider;
  }

  @Override
  public CacheManager get() {
    return provideCacheManager(contextProvider.get(), cacheIndexDaoProvider.get());
  }

  public static CacheModule_ProvideCacheManagerFactory create(Provider<Context> contextProvider,
      Provider<CacheIndexDao> cacheIndexDaoProvider) {
    return new CacheModule_ProvideCacheManagerFactory(contextProvider, cacheIndexDaoProvider);
  }

  public static CacheManager provideCacheManager(Context context, CacheIndexDao cacheIndexDao) {
    return Preconditions.checkNotNullFromProvides(CacheModule.INSTANCE.provideCacheManager(context, cacheIndexDao));
  }
}
