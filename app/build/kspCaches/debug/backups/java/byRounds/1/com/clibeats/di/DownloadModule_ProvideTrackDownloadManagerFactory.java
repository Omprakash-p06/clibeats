package com.clibeats.di;

import com.clibeats.data.cache.CacheManager;
import com.clibeats.data.download.TrackDownloadManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DownloadModule_ProvideTrackDownloadManagerFactory implements Factory<TrackDownloadManager> {
  private final Provider<OkHttpClient> okHttpClientProvider;

  private final Provider<CacheManager> cacheManagerProvider;

  public DownloadModule_ProvideTrackDownloadManagerFactory(
      Provider<OkHttpClient> okHttpClientProvider, Provider<CacheManager> cacheManagerProvider) {
    this.okHttpClientProvider = okHttpClientProvider;
    this.cacheManagerProvider = cacheManagerProvider;
  }

  @Override
  public TrackDownloadManager get() {
    return provideTrackDownloadManager(okHttpClientProvider.get(), cacheManagerProvider.get());
  }

  public static DownloadModule_ProvideTrackDownloadManagerFactory create(
      Provider<OkHttpClient> okHttpClientProvider, Provider<CacheManager> cacheManagerProvider) {
    return new DownloadModule_ProvideTrackDownloadManagerFactory(okHttpClientProvider, cacheManagerProvider);
  }

  public static TrackDownloadManager provideTrackDownloadManager(OkHttpClient okHttpClient,
      CacheManager cacheManager) {
    return Preconditions.checkNotNullFromProvides(DownloadModule.INSTANCE.provideTrackDownloadManager(okHttpClient, cacheManager));
  }
}
