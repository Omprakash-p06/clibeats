package com.clibeats.presentation.library;

import com.clibeats.domain.repository.PlaybackRepository;
import com.clibeats.domain.repository.SongRepository;
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
public final class LibraryViewModel_Factory implements Factory<LibraryViewModel> {
  private final Provider<SongRepository> songRepositoryProvider;

  private final Provider<PlaybackRepository> playbackRepositoryProvider;

  public LibraryViewModel_Factory(Provider<SongRepository> songRepositoryProvider,
      Provider<PlaybackRepository> playbackRepositoryProvider) {
    this.songRepositoryProvider = songRepositoryProvider;
    this.playbackRepositoryProvider = playbackRepositoryProvider;
  }

  @Override
  public LibraryViewModel get() {
    return newInstance(songRepositoryProvider.get(), playbackRepositoryProvider.get());
  }

  public static LibraryViewModel_Factory create(Provider<SongRepository> songRepositoryProvider,
      Provider<PlaybackRepository> playbackRepositoryProvider) {
    return new LibraryViewModel_Factory(songRepositoryProvider, playbackRepositoryProvider);
  }

  public static LibraryViewModel newInstance(SongRepository songRepository,
      PlaybackRepository playbackRepository) {
    return new LibraryViewModel(songRepository, playbackRepository);
  }
}
