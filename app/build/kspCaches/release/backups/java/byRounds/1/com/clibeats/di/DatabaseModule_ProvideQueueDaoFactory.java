package com.clibeats.di;

import com.clibeats.data.local.CliBeatsDatabase;
import com.clibeats.data.local.dao.QueueDao;
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
public final class DatabaseModule_ProvideQueueDaoFactory implements Factory<QueueDao> {
  private final Provider<CliBeatsDatabase> dbProvider;

  public DatabaseModule_ProvideQueueDaoFactory(Provider<CliBeatsDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public QueueDao get() {
    return provideQueueDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideQueueDaoFactory create(
      Provider<CliBeatsDatabase> dbProvider) {
    return new DatabaseModule_ProvideQueueDaoFactory(dbProvider);
  }

  public static QueueDao provideQueueDao(CliBeatsDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideQueueDao(db));
  }
}
