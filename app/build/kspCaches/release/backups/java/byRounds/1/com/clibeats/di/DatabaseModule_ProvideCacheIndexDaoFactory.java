package com.clibeats.di;

import com.clibeats.data.local.CliBeatsDatabase;
import com.clibeats.data.local.dao.CacheIndexDao;
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
public final class DatabaseModule_ProvideCacheIndexDaoFactory implements Factory<CacheIndexDao> {
  private final Provider<CliBeatsDatabase> dbProvider;

  public DatabaseModule_ProvideCacheIndexDaoFactory(Provider<CliBeatsDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public CacheIndexDao get() {
    return provideCacheIndexDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideCacheIndexDaoFactory create(
      Provider<CliBeatsDatabase> dbProvider) {
    return new DatabaseModule_ProvideCacheIndexDaoFactory(dbProvider);
  }

  public static CacheIndexDao provideCacheIndexDao(CliBeatsDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideCacheIndexDao(db));
  }
}
