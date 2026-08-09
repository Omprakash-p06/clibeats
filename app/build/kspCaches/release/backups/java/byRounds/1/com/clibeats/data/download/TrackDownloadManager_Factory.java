package com.clibeats.data.download;

import com.clibeats.data.cache.CacheManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
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
public final class TrackDownloadManager_Factory implements Factory<TrackDownloadManager> {
  private final Provider<OkHttpClient> okHttpClientProvider;

  private final Provider<CacheManager> cacheManagerProvider;

  public TrackDownloadManager_Factory(Provider<OkHttpClient> okHttpClientProvider,
      Provider<CacheManager> cacheManagerProvider) {
    this.okHttpClientProvider = okHttpClientProvider;
    this.cacheManagerProvider = cacheManagerProvider;
  }

  @Override
  public TrackDownloadManager get() {
    return newInstance(okHttpClientProvider.get(), cacheManagerProvider.get());
  }

  public static TrackDownloadManager_Factory create(Provider<OkHttpClient> okHttpClientProvider,
      Provider<CacheManager> cacheManagerProvider) {
    return new TrackDownloadManager_Factory(okHttpClientProvider, cacheManagerProvider);
  }

  public static TrackDownloadManager newInstance(OkHttpClient okHttpClient,
      CacheManager cacheManager) {
    return new TrackDownloadManager(okHttpClient, cacheManager);
  }
}
