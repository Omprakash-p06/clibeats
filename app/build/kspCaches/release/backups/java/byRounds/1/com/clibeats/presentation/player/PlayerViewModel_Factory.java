package com.clibeats.presentation.player;

import com.clibeats.domain.repository.PlaybackRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class PlayerViewModel_Factory implements Factory<PlayerViewModel> {
  private final Provider<PlaybackRepository> playbackRepositoryProvider;

  public PlayerViewModel_Factory(Provider<PlaybackRepository> playbackRepositoryProvider) {
    this.playbackRepositoryProvider = playbackRepositoryProvider;
  }

  @Override
  public PlayerViewModel get() {
    return newInstance(playbackRepositoryProvider.get());
  }

  public static PlayerViewModel_Factory create(
      Provider<PlaybackRepository> playbackRepositoryProvider) {
    return new PlayerViewModel_Factory(playbackRepositoryProvider);
  }

  public static PlayerViewModel newInstance(PlaybackRepository playbackRepository) {
    return new PlayerViewModel(playbackRepository);
  }
}
