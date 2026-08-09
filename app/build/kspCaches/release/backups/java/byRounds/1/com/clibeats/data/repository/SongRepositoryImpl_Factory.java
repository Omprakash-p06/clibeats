package com.clibeats.data.repository;

import com.clibeats.data.local.dao.SongDao;
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
public final class SongRepositoryImpl_Factory implements Factory<SongRepositoryImpl> {
  private final Provider<SongDao> songDaoProvider;

  public SongRepositoryImpl_Factory(Provider<SongDao> songDaoProvider) {
    this.songDaoProvider = songDaoProvider;
  }

  @Override
  public SongRepositoryImpl get() {
    return newInstance(songDaoProvider.get());
  }

  public static SongRepositoryImpl_Factory create(Provider<SongDao> songDaoProvider) {
    return new SongRepositoryImpl_Factory(songDaoProvider);
  }

  public static SongRepositoryImpl newInstance(SongDao songDao) {
    return new SongRepositoryImpl(songDao);
  }
}
