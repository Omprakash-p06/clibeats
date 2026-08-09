package com.clibeats.presentation.playlist;

import com.clibeats.domain.repository.PlaybackRepository;
import com.clibeats.domain.repository.PlaylistRepository;
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
public final class PlaylistViewModel_Factory implements Factory<PlaylistViewModel> {
  private final Provider<PlaylistRepository> playlistRepositoryProvider;

  private final Provider<PlaybackRepository> playbackRepositoryProvider;

  public PlaylistViewModel_Factory(Provider<PlaylistRepository> playlistRepositoryProvider,
      Provider<PlaybackRepository> playbackRepositoryProvider) {
    this.playlistRepositoryProvider = playlistRepositoryProvider;
    this.playbackRepositoryProvider = playbackRepositoryProvider;
  }

  @Override
  public PlaylistViewModel get() {
    return newInstance(playlistRepositoryProvider.get(), playbackRepositoryProvider.get());
  }

  public static PlaylistViewModel_Factory create(
      Provider<PlaylistRepository> playlistRepositoryProvider,
      Provider<PlaybackRepository> playbackRepositoryProvider) {
    return new PlaylistViewModel_Factory(playlistRepositoryProvider, playbackRepositoryProvider);
  }

  public static PlaylistViewModel newInstance(PlaylistRepository playlistRepository,
      PlaybackRepository playbackRepository) {
    return new PlaylistViewModel(playlistRepository, playbackRepository);
  }
}
