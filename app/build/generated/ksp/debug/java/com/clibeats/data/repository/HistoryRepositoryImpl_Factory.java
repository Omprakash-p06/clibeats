package com.clibeats.data.repository;

import com.clibeats.data.local.dao.HistoryDao;
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
public final class HistoryRepositoryImpl_Factory implements Factory<HistoryRepositoryImpl> {
  private final Provider<HistoryDao> historyDaoProvider;

  public HistoryRepositoryImpl_Factory(Provider<HistoryDao> historyDaoProvider) {
    this.historyDaoProvider = historyDaoProvider;
  }

  @Override
  public HistoryRepositoryImpl get() {
    return newInstance(historyDaoProvider.get());
  }

  public static HistoryRepositoryImpl_Factory create(Provider<HistoryDao> historyDaoProvider) {
    return new HistoryRepositoryImpl_Factory(historyDaoProvider);
  }

  public static HistoryRepositoryImpl newInstance(HistoryDao historyDao) {
    return new HistoryRepositoryImpl(historyDao);
  }
}
