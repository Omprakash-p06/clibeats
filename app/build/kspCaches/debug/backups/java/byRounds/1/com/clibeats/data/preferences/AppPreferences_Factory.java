package com.clibeats.data.preferences;

import android.content.SharedPreferences;
import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
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
public final class AppPreferences_Factory implements Factory<AppPreferences> {
  private final Provider<DataStore<Preferences>> dataStoreProvider;

  private final Provider<SharedPreferences> securePrefsProvider;

  public AppPreferences_Factory(Provider<DataStore<Preferences>> dataStoreProvider,
      Provider<SharedPreferences> securePrefsProvider) {
    this.dataStoreProvider = dataStoreProvider;
    this.securePrefsProvider = securePrefsProvider;
  }

  @Override
  public AppPreferences get() {
    return newInstance(dataStoreProvider.get(), securePrefsProvider.get());
  }

  public static AppPreferences_Factory create(Provider<DataStore<Preferences>> dataStoreProvider,
      Provider<SharedPreferences> securePrefsProvider) {
    return new AppPreferences_Factory(dataStoreProvider, securePrefsProvider);
  }

  public static AppPreferences newInstance(DataStore<Preferences> dataStore,
      SharedPreferences securePrefs) {
    return new AppPreferences(dataStore, securePrefs);
  }
}
