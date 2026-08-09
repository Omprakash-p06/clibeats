package com.clibeats.presentation.search;

import com.clibeats.domain.provider.MusicProvider;
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
public final class SearchViewModel_Factory implements Factory<SearchViewModel> {
  private final Provider<MusicProvider> musicProvider;

  public SearchViewModel_Factory(Provider<MusicProvider> musicProvider) {
    this.musicProvider = musicProvider;
  }

  @Override
  public SearchViewModel get() {
    return newInstance(musicProvider.get());
  }

  public static SearchViewModel_Factory create(Provider<MusicProvider> musicProvider) {
    return new SearchViewModel_Factory(musicProvider);
  }

  public static SearchViewModel newInstance(MusicProvider musicProvider) {
    return new SearchViewModel(musicProvider);
  }
}
