package com.clibeats.di;

import com.clibeats.data.local.CliBeatsDatabase;
import com.clibeats.data.local.dao.SongDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvideSongDaoFactory implements Factory<SongDao> {
  private final Provider<CliBeatsDatabase> dbProvider;

  public DatabaseModule_ProvideSongDaoFactory(Provider<CliBeatsDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public SongDao get() {
    return provideSongDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideSongDaoFactory create(Provider<CliBeatsDatabase> dbProvider) {
    return new DatabaseModule_ProvideSongDaoFactory(dbProvider);
  }

  public static SongDao provideSongDao(CliBeatsDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideSongDao(db));
  }
}
