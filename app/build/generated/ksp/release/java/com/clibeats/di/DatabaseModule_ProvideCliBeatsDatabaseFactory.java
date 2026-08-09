package com.clibeats.di;

import android.content.Context;
import com.clibeats.data.local.CliBeatsDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class DatabaseModule_ProvideCliBeatsDatabaseFactory implements Factory<CliBeatsDatabase> {
  private final Provider<Context> contextProvider;

  public DatabaseModule_ProvideCliBeatsDatabaseFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public CliBeatsDatabase get() {
    return provideCliBeatsDatabase(contextProvider.get());
  }

  public static DatabaseModule_ProvideCliBeatsDatabaseFactory create(
      Provider<Context> contextProvider) {
    return new DatabaseModule_ProvideCliBeatsDatabaseFactory(contextProvider);
  }

  public static CliBeatsDatabase provideCliBeatsDatabase(Context context) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideCliBeatsDatabase(context));
  }
}
