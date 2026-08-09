package com.clibeats.presentation.queue;

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
public final class QueueViewModel_Factory implements Factory<QueueViewModel> {
  private final Provider<PlaybackRepository> playbackRepositoryProvider;

  public QueueViewModel_Factory(Provider<PlaybackRepository> playbackRepositoryProvider) {
    this.playbackRepositoryProvider = playbackRepositoryProvider;
  }

  @Override
  public QueueViewModel get() {
    return newInstance(playbackRepositoryProvider.get());
  }

  public static QueueViewModel_Factory create(
      Provider<PlaybackRepository> playbackRepositoryProvider) {
    return new QueueViewModel_Factory(playbackRepositoryProvider);
  }

  public static QueueViewModel newInstance(PlaybackRepository playbackRepository) {
    return new QueueViewModel(playbackRepository);
  }
}
