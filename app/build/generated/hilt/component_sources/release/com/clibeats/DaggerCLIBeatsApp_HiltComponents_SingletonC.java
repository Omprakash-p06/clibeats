package com.clibeats;

import android.app.Activity;
import android.app.Service;
import android.content.SharedPreferences;
import android.view.View;
import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import androidx.media3.common.AudioAttributes;
import androidx.media3.exoplayer.ExoPlayer;
import com.clibeats.data.cache.CacheManager;
import com.clibeats.data.gateway.GatewayMusicProvider;
import com.clibeats.data.gateway.api.GatewayApi;
import com.clibeats.data.local.CliBeatsDatabase;
import com.clibeats.data.local.dao.CacheIndexDao;
import com.clibeats.data.local.dao.PlaylistDao;
import com.clibeats.data.local.dao.SongDao;
import com.clibeats.data.preferences.AppPreferences;
import com.clibeats.data.repository.PlaybackRepositoryImpl;
import com.clibeats.data.repository.PlaylistRepositoryImpl;
import com.clibeats.data.repository.SongRepositoryImpl;
import com.clibeats.di.CacheModule_ProvideCacheManagerFactory;
import com.clibeats.di.DatabaseModule_ProvideCacheIndexDaoFactory;
import com.clibeats.di.DatabaseModule_ProvideCliBeatsDatabaseFactory;
import com.clibeats.di.DatabaseModule_ProvidePlaylistDaoFactory;
import com.clibeats.di.DatabaseModule_ProvideSongDaoFactory;
import com.clibeats.di.NetworkModule_ProvideGatewayApiFactory;
import com.clibeats.di.NetworkModule_ProvideGatewayOkHttpClientFactory;
import com.clibeats.di.NetworkModule_ProvideGatewayRetrofitFactory;
import com.clibeats.di.NetworkModule_ProvideJsonFactory;
import com.clibeats.di.PlaybackModule_ProvideAudioAttributesFactory;
import com.clibeats.di.PlaybackModule_ProvideExoPlayerFactory;
import com.clibeats.di.StorageModule_ProvideDataStoreFactory;
import com.clibeats.di.StorageModule_ProvideSecurePrefsFactory;
import com.clibeats.domain.playback.QueueManager;
import com.clibeats.playback.PlayerAdapter;
import com.clibeats.playback.service.PlaybackService;
import com.clibeats.playback.service.PlaybackService_MembersInjector;
import com.clibeats.presentation.library.LibraryViewModel;
import com.clibeats.presentation.library.LibraryViewModel_HiltModules;
import com.clibeats.presentation.player.PlayerViewModel;
import com.clibeats.presentation.player.PlayerViewModel_HiltModules;
import com.clibeats.presentation.playlist.PlaylistViewModel;
import com.clibeats.presentation.playlist.PlaylistViewModel_HiltModules;
import com.clibeats.presentation.queue.QueueViewModel;
import com.clibeats.presentation.queue.QueueViewModel_HiltModules;
import com.clibeats.presentation.search.SearchViewModel;
import com.clibeats.presentation.search.SearchViewModel_HiltModules;
import com.clibeats.presentation.settings.SettingsViewModel;
import com.clibeats.presentation.settings.SettingsViewModel_HiltModules;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.IdentifierNameString;
import dagger.internal.KeepFieldType;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;
import kotlinx.serialization.json.Json;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

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
public final class DaggerCLIBeatsApp_HiltComponents_SingletonC {
  private DaggerCLIBeatsApp_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public CLIBeatsApp_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements CLIBeatsApp_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public CLIBeatsApp_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements CLIBeatsApp_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public CLIBeatsApp_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements CLIBeatsApp_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public CLIBeatsApp_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements CLIBeatsApp_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public CLIBeatsApp_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements CLIBeatsApp_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public CLIBeatsApp_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements CLIBeatsApp_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public CLIBeatsApp_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements CLIBeatsApp_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public CLIBeatsApp_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends CLIBeatsApp_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends CLIBeatsApp_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends CLIBeatsApp_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends CLIBeatsApp_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity mainActivity) {
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(ImmutableMap.<String, Boolean>builderWithExpectedSize(6).put(LazyClassKeyProvider.com_clibeats_presentation_library_LibraryViewModel, LibraryViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_clibeats_presentation_player_PlayerViewModel, PlayerViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_clibeats_presentation_playlist_PlaylistViewModel, PlaylistViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_clibeats_presentation_queue_QueueViewModel, QueueViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_clibeats_presentation_search_SearchViewModel, SearchViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_clibeats_presentation_settings_SettingsViewModel, SettingsViewModel_HiltModules.KeyModule.provide()).build());
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_clibeats_presentation_search_SearchViewModel = "com.clibeats.presentation.search.SearchViewModel";

      static String com_clibeats_presentation_player_PlayerViewModel = "com.clibeats.presentation.player.PlayerViewModel";

      static String com_clibeats_presentation_library_LibraryViewModel = "com.clibeats.presentation.library.LibraryViewModel";

      static String com_clibeats_presentation_playlist_PlaylistViewModel = "com.clibeats.presentation.playlist.PlaylistViewModel";

      static String com_clibeats_presentation_queue_QueueViewModel = "com.clibeats.presentation.queue.QueueViewModel";

      static String com_clibeats_presentation_settings_SettingsViewModel = "com.clibeats.presentation.settings.SettingsViewModel";

      @KeepFieldType
      SearchViewModel com_clibeats_presentation_search_SearchViewModel2;

      @KeepFieldType
      PlayerViewModel com_clibeats_presentation_player_PlayerViewModel2;

      @KeepFieldType
      LibraryViewModel com_clibeats_presentation_library_LibraryViewModel2;

      @KeepFieldType
      PlaylistViewModel com_clibeats_presentation_playlist_PlaylistViewModel2;

      @KeepFieldType
      QueueViewModel com_clibeats_presentation_queue_QueueViewModel2;

      @KeepFieldType
      SettingsViewModel com_clibeats_presentation_settings_SettingsViewModel2;
    }
  }

  private static final class ViewModelCImpl extends CLIBeatsApp_HiltComponents.ViewModelC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<LibraryViewModel> libraryViewModelProvider;

    private Provider<PlayerViewModel> playerViewModelProvider;

    private Provider<PlaylistViewModel> playlistViewModelProvider;

    private Provider<QueueViewModel> queueViewModelProvider;

    private Provider<SearchViewModel> searchViewModelProvider;

    private Provider<SettingsViewModel> settingsViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;

      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.libraryViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.playerViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.playlistViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.queueViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.searchViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.settingsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(ImmutableMap.<String, javax.inject.Provider<ViewModel>>builderWithExpectedSize(6).put(LazyClassKeyProvider.com_clibeats_presentation_library_LibraryViewModel, ((Provider) libraryViewModelProvider)).put(LazyClassKeyProvider.com_clibeats_presentation_player_PlayerViewModel, ((Provider) playerViewModelProvider)).put(LazyClassKeyProvider.com_clibeats_presentation_playlist_PlaylistViewModel, ((Provider) playlistViewModelProvider)).put(LazyClassKeyProvider.com_clibeats_presentation_queue_QueueViewModel, ((Provider) queueViewModelProvider)).put(LazyClassKeyProvider.com_clibeats_presentation_search_SearchViewModel, ((Provider) searchViewModelProvider)).put(LazyClassKeyProvider.com_clibeats_presentation_settings_SettingsViewModel, ((Provider) settingsViewModelProvider)).build());
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return ImmutableMap.<Class<?>, Object>of();
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_clibeats_presentation_library_LibraryViewModel = "com.clibeats.presentation.library.LibraryViewModel";

      static String com_clibeats_presentation_search_SearchViewModel = "com.clibeats.presentation.search.SearchViewModel";

      static String com_clibeats_presentation_queue_QueueViewModel = "com.clibeats.presentation.queue.QueueViewModel";

      static String com_clibeats_presentation_playlist_PlaylistViewModel = "com.clibeats.presentation.playlist.PlaylistViewModel";

      static String com_clibeats_presentation_player_PlayerViewModel = "com.clibeats.presentation.player.PlayerViewModel";

      static String com_clibeats_presentation_settings_SettingsViewModel = "com.clibeats.presentation.settings.SettingsViewModel";

      @KeepFieldType
      LibraryViewModel com_clibeats_presentation_library_LibraryViewModel2;

      @KeepFieldType
      SearchViewModel com_clibeats_presentation_search_SearchViewModel2;

      @KeepFieldType
      QueueViewModel com_clibeats_presentation_queue_QueueViewModel2;

      @KeepFieldType
      PlaylistViewModel com_clibeats_presentation_playlist_PlaylistViewModel2;

      @KeepFieldType
      PlayerViewModel com_clibeats_presentation_player_PlayerViewModel2;

      @KeepFieldType
      SettingsViewModel com_clibeats_presentation_settings_SettingsViewModel2;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.clibeats.presentation.library.LibraryViewModel 
          return (T) new LibraryViewModel(singletonCImpl.songRepositoryImplProvider.get(), singletonCImpl.playbackRepositoryImplProvider.get());

          case 1: // com.clibeats.presentation.player.PlayerViewModel 
          return (T) new PlayerViewModel(singletonCImpl.playbackRepositoryImplProvider.get());

          case 2: // com.clibeats.presentation.playlist.PlaylistViewModel 
          return (T) new PlaylistViewModel(singletonCImpl.playlistRepositoryImplProvider.get(), singletonCImpl.playbackRepositoryImplProvider.get());

          case 3: // com.clibeats.presentation.queue.QueueViewModel 
          return (T) new QueueViewModel(singletonCImpl.playbackRepositoryImplProvider.get());

          case 4: // com.clibeats.presentation.search.SearchViewModel 
          return (T) new SearchViewModel(singletonCImpl.gatewayMusicProvider.get());

          case 5: // com.clibeats.presentation.settings.SettingsViewModel 
          return (T) new SettingsViewModel(singletonCImpl.appPreferencesProvider.get(), singletonCImpl.provideCacheManagerProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends CLIBeatsApp_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends CLIBeatsApp_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }

    @Override
    public void injectPlaybackService(PlaybackService playbackService) {
      injectPlaybackService2(playbackService);
    }

    private PlaybackService injectPlaybackService2(PlaybackService instance) {
      PlaybackService_MembersInjector.injectPlayer(instance, singletonCImpl.provideExoPlayerProvider.get());
      return instance;
    }
  }

  private static final class SingletonCImpl extends CLIBeatsApp_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<CliBeatsDatabase> provideCliBeatsDatabaseProvider;

    private Provider<SongDao> provideSongDaoProvider;

    private Provider<SongRepositoryImpl> songRepositoryImplProvider;

    private Provider<AudioAttributes> provideAudioAttributesProvider;

    private Provider<ExoPlayer> provideExoPlayerProvider;

    private Provider<CacheIndexDao> provideCacheIndexDaoProvider;

    private Provider<CacheManager> provideCacheManagerProvider;

    private Provider<PlayerAdapter> playerAdapterProvider;

    private Provider<Json> provideJsonProvider;

    private Provider<OkHttpClient> provideGatewayOkHttpClientProvider;

    private Provider<Retrofit> provideGatewayRetrofitProvider;

    private Provider<GatewayApi> provideGatewayApiProvider;

    private Provider<GatewayMusicProvider> gatewayMusicProvider;

    private Provider<QueueManager> queueManagerProvider;

    private Provider<PlaybackRepositoryImpl> playbackRepositoryImplProvider;

    private Provider<PlaylistDao> providePlaylistDaoProvider;

    private Provider<PlaylistRepositoryImpl> playlistRepositoryImplProvider;

    private Provider<DataStore<Preferences>> provideDataStoreProvider;

    private Provider<SharedPreferences> provideSecurePrefsProvider;

    private Provider<AppPreferences> appPreferencesProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.provideCliBeatsDatabaseProvider = DoubleCheck.provider(new SwitchingProvider<CliBeatsDatabase>(singletonCImpl, 2));
      this.provideSongDaoProvider = DoubleCheck.provider(new SwitchingProvider<SongDao>(singletonCImpl, 1));
      this.songRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<SongRepositoryImpl>(singletonCImpl, 0));
      this.provideAudioAttributesProvider = DoubleCheck.provider(new SwitchingProvider<AudioAttributes>(singletonCImpl, 6));
      this.provideExoPlayerProvider = DoubleCheck.provider(new SwitchingProvider<ExoPlayer>(singletonCImpl, 5));
      this.provideCacheIndexDaoProvider = DoubleCheck.provider(new SwitchingProvider<CacheIndexDao>(singletonCImpl, 8));
      this.provideCacheManagerProvider = DoubleCheck.provider(new SwitchingProvider<CacheManager>(singletonCImpl, 7));
      this.playerAdapterProvider = DoubleCheck.provider(new SwitchingProvider<PlayerAdapter>(singletonCImpl, 4));
      this.provideJsonProvider = DoubleCheck.provider(new SwitchingProvider<Json>(singletonCImpl, 12));
      this.provideGatewayOkHttpClientProvider = DoubleCheck.provider(new SwitchingProvider<OkHttpClient>(singletonCImpl, 13));
      this.provideGatewayRetrofitProvider = DoubleCheck.provider(new SwitchingProvider<Retrofit>(singletonCImpl, 11));
      this.provideGatewayApiProvider = DoubleCheck.provider(new SwitchingProvider<GatewayApi>(singletonCImpl, 10));
      this.gatewayMusicProvider = DoubleCheck.provider(new SwitchingProvider<GatewayMusicProvider>(singletonCImpl, 9));
      this.queueManagerProvider = DoubleCheck.provider(new SwitchingProvider<QueueManager>(singletonCImpl, 14));
      this.playbackRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<PlaybackRepositoryImpl>(singletonCImpl, 3));
      this.providePlaylistDaoProvider = DoubleCheck.provider(new SwitchingProvider<PlaylistDao>(singletonCImpl, 16));
      this.playlistRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<PlaylistRepositoryImpl>(singletonCImpl, 15));
      this.provideDataStoreProvider = DoubleCheck.provider(new SwitchingProvider<DataStore<Preferences>>(singletonCImpl, 18));
      this.provideSecurePrefsProvider = DoubleCheck.provider(new SwitchingProvider<SharedPreferences>(singletonCImpl, 19));
      this.appPreferencesProvider = DoubleCheck.provider(new SwitchingProvider<AppPreferences>(singletonCImpl, 17));
    }

    @Override
    public void injectCLIBeatsApp(CLIBeatsApp cLIBeatsApp) {
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return ImmutableSet.<Boolean>of();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.clibeats.data.repository.SongRepositoryImpl 
          return (T) new SongRepositoryImpl(singletonCImpl.provideSongDaoProvider.get());

          case 1: // com.clibeats.data.local.dao.SongDao 
          return (T) DatabaseModule_ProvideSongDaoFactory.provideSongDao(singletonCImpl.provideCliBeatsDatabaseProvider.get());

          case 2: // com.clibeats.data.local.CliBeatsDatabase 
          return (T) DatabaseModule_ProvideCliBeatsDatabaseFactory.provideCliBeatsDatabase(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 3: // com.clibeats.data.repository.PlaybackRepositoryImpl 
          return (T) new PlaybackRepositoryImpl(singletonCImpl.playerAdapterProvider.get(), singletonCImpl.gatewayMusicProvider.get(), singletonCImpl.queueManagerProvider.get());

          case 4: // com.clibeats.playback.PlayerAdapter 
          return (T) new PlayerAdapter(singletonCImpl.provideExoPlayerProvider.get(), singletonCImpl.provideCacheManagerProvider.get());

          case 5: // androidx.media3.exoplayer.ExoPlayer 
          return (T) PlaybackModule_ProvideExoPlayerFactory.provideExoPlayer(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.provideAudioAttributesProvider.get());

          case 6: // androidx.media3.common.AudioAttributes 
          return (T) PlaybackModule_ProvideAudioAttributesFactory.provideAudioAttributes();

          case 7: // com.clibeats.data.cache.CacheManager 
          return (T) CacheModule_ProvideCacheManagerFactory.provideCacheManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.provideCacheIndexDaoProvider.get());

          case 8: // com.clibeats.data.local.dao.CacheIndexDao 
          return (T) DatabaseModule_ProvideCacheIndexDaoFactory.provideCacheIndexDao(singletonCImpl.provideCliBeatsDatabaseProvider.get());

          case 9: // com.clibeats.data.gateway.GatewayMusicProvider 
          return (T) new GatewayMusicProvider(singletonCImpl.provideGatewayApiProvider.get());

          case 10: // com.clibeats.data.gateway.api.GatewayApi 
          return (T) NetworkModule_ProvideGatewayApiFactory.provideGatewayApi(singletonCImpl.provideGatewayRetrofitProvider.get());

          case 11: // @javax.inject.Named("gateway") retrofit2.Retrofit 
          return (T) NetworkModule_ProvideGatewayRetrofitFactory.provideGatewayRetrofit(singletonCImpl.provideJsonProvider.get(), singletonCImpl.provideGatewayOkHttpClientProvider.get());

          case 12: // kotlinx.serialization.json.Json 
          return (T) NetworkModule_ProvideJsonFactory.provideJson();

          case 13: // @javax.inject.Named("gateway") okhttp3.OkHttpClient 
          return (T) NetworkModule_ProvideGatewayOkHttpClientFactory.provideGatewayOkHttpClient();

          case 14: // com.clibeats.domain.playback.QueueManager 
          return (T) new QueueManager();

          case 15: // com.clibeats.data.repository.PlaylistRepositoryImpl 
          return (T) new PlaylistRepositoryImpl(singletonCImpl.providePlaylistDaoProvider.get());

          case 16: // com.clibeats.data.local.dao.PlaylistDao 
          return (T) DatabaseModule_ProvidePlaylistDaoFactory.providePlaylistDao(singletonCImpl.provideCliBeatsDatabaseProvider.get());

          case 17: // com.clibeats.data.preferences.AppPreferences 
          return (T) new AppPreferences(singletonCImpl.provideDataStoreProvider.get(), singletonCImpl.provideSecurePrefsProvider.get());

          case 18: // androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> 
          return (T) StorageModule_ProvideDataStoreFactory.provideDataStore(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 19: // android.content.SharedPreferences 
          return (T) StorageModule_ProvideSecurePrefsFactory.provideSecurePrefs(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
