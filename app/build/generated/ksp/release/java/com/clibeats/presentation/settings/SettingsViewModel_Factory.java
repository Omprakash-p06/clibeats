package com.clibeats.presentation.settings;

import com.clibeats.data.cache.CacheManager;
import com.clibeats.data.preferences.AppPreferences;
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<AppPreferences> appPreferencesProvider;

  private final Provider<CacheManager> cacheManagerProvider;

  public SettingsViewModel_Factory(Provider<AppPreferences> appPreferencesProvider,
      Provider<CacheManager> cacheManagerProvider) {
    this.appPreferencesProvider = appPreferencesProvider;
    this.cacheManagerProvider = cacheManagerProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(appPreferencesProvider.get(), cacheManagerProvider.get());
  }

  public static SettingsViewModel_Factory create(Provider<AppPreferences> appPreferencesProvider,
      Provider<CacheManager> cacheManagerProvider) {
    return new SettingsViewModel_Factory(appPreferencesProvider, cacheManagerProvider);
  }

  public static SettingsViewModel newInstance(AppPreferences appPreferences,
      CacheManager cacheManager) {
    return new SettingsViewModel(appPreferences, cacheManager);
  }
}
