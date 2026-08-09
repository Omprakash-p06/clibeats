package com.clibeats.data.repository;

import com.clibeats.domain.playback.QueueManager;
import com.clibeats.domain.provider.MusicProvider;
import com.clibeats.playback.PlayerAdapter;
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
public final class PlaybackRepositoryImpl_Factory implements Factory<PlaybackRepositoryImpl> {
  private final Provider<PlayerAdapter> playerAdapterProvider;

  private final Provider<MusicProvider> musicProvider;

  private final Provider<QueueManager> queueManagerProvider;

  public PlaybackRepositoryImpl_Factory(Provider<PlayerAdapter> playerAdapterProvider,
      Provider<MusicProvider> musicProvider, Provider<QueueManager> queueManagerProvider) {
    this.playerAdapterProvider = playerAdapterProvider;
    this.musicProvider = musicProvider;
    this.queueManagerProvider = queueManagerProvider;
  }

  @Override
  public PlaybackRepositoryImpl get() {
    return newInstance(playerAdapterProvider.get(), musicProvider.get(), queueManagerProvider.get());
  }

  public static PlaybackRepositoryImpl_Factory create(Provider<PlayerAdapter> playerAdapterProvider,
      Provider<MusicProvider> musicProvider, Provider<QueueManager> queueManagerProvider) {
    return new PlaybackRepositoryImpl_Factory(playerAdapterProvider, musicProvider, queueManagerProvider);
  }

  public static PlaybackRepositoryImpl newInstance(PlayerAdapter playerAdapter,
      MusicProvider musicProvider, QueueManager queueManager) {
    return new PlaybackRepositoryImpl(playerAdapter, musicProvider, queueManager);
  }
}
