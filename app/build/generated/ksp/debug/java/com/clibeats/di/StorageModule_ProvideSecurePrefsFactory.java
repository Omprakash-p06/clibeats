package com.clibeats.di;

import android.content.Context;
import android.content.SharedPreferences;
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
public final class StorageModule_ProvideSecurePrefsFactory implements Factory<SharedPreferences> {
  private final Provider<Context> contextProvider;

  public StorageModule_ProvideSecurePrefsFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public SharedPreferences get() {
    return provideSecurePrefs(contextProvider.get());
  }

  public static StorageModule_ProvideSecurePrefsFactory create(Provider<Context> contextProvider) {
    return new StorageModule_ProvideSecurePrefsFactory(contextProvider);
  }

  public static SharedPreferences provideSecurePrefs(Context context) {
    return Preconditions.checkNotNullFromProvides(StorageModule.INSTANCE.provideSecurePrefs(context));
  }
}
