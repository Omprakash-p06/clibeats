package com.clibeats.playback;

import androidx.media3.exoplayer.ExoPlayer;
import com.clibeats.data.cache.CacheManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class PlayerAdapter_Factory implements Factory<PlayerAdapter> {
  private final Provider<ExoPlayer> playerProvider;

  private final Provider<CacheManager> cacheManagerProvider;

  public PlayerAdapter_Factory(Provider<ExoPlayer> playerProvider,
      Provider<CacheManager> cacheManagerProvider) {
    this.playerProvider = playerProvider;
    this.cacheManagerProvider = cacheManagerProvider;
  }

  @Override
  public PlayerAdapter get() {
    return newInstance(playerProvider.get(), cacheManagerProvider.get());
  }

  public static PlayerAdapter_Factory create(Provider<ExoPlayer> playerProvider,
      Provider<CacheManager> cacheManagerProvider) {
    return new PlayerAdapter_Factory(playerProvider, cacheManagerProvider);
  }

  public static PlayerAdapter newInstance(ExoPlayer player, CacheManager cacheManager) {
    return new PlayerAdapter(player, cacheManager);
  }
}
