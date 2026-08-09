package com.clibeats.data.repository;

import com.clibeats.data.local.dao.PlaylistDao;
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
public final class PlaylistRepositoryImpl_Factory implements Factory<PlaylistRepositoryImpl> {
  private final Provider<PlaylistDao> playlistDaoProvider;

  public PlaylistRepositoryImpl_Factory(Provider<PlaylistDao> playlistDaoProvider) {
    this.playlistDaoProvider = playlistDaoProvider;
  }

  @Override
  public PlaylistRepositoryImpl get() {
    return newInstance(playlistDaoProvider.get());
  }

  public static PlaylistRepositoryImpl_Factory create(Provider<PlaylistDao> playlistDaoProvider) {
    return new PlaylistRepositoryImpl_Factory(playlistDaoProvider);
  }

  public static PlaylistRepositoryImpl newInstance(PlaylistDao playlistDao) {
    return new PlaylistRepositoryImpl(playlistDao);
  }
}
