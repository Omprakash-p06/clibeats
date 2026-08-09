package com.clibeats.data.cache;

import android.content.Context;
import com.clibeats.data.local.dao.CacheIndexDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class CacheManager_Factory implements Factory<CacheManager> {
  private final Provider<Context> contextProvider;

  private final Provider<CacheIndexDao> cacheIndexDaoProvider;

  public CacheManager_Factory(Provider<Context> contextProvider,
      Provider<CacheIndexDao> cacheIndexDaoProvider) {
    this.contextProvider = contextProvider;
    this.cacheIndexDaoProvider = cacheIndexDaoProvider;
  }

  @Override
  public CacheManager get() {
    return newInstance(contextProvider.get(), cacheIndexDaoProvider.get());
  }

  public static CacheManager_Factory create(Provider<Context> contextProvider,
      Provider<CacheIndexDao> cacheIndexDaoProvider) {
    return new CacheManager_Factory(contextProvider, cacheIndexDaoProvider);
  }

  public static CacheManager newInstance(Context context, CacheIndexDao cacheIndexDao) {
    return new CacheManager(context, cacheIndexDao);
  }
}
