package com.clibeats.data.gateway;

import com.clibeats.data.gateway.api.GatewayApi;
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
public final class GatewayMusicProvider_Factory implements Factory<GatewayMusicProvider> {
  private final Provider<GatewayApi> apiProvider;

  public GatewayMusicProvider_Factory(Provider<GatewayApi> apiProvider) {
    this.apiProvider = apiProvider;
  }

  @Override
  public GatewayMusicProvider get() {
    return newInstance(apiProvider.get());
  }

  public static GatewayMusicProvider_Factory create(Provider<GatewayApi> apiProvider) {
    return new GatewayMusicProvider_Factory(apiProvider);
  }

  public static GatewayMusicProvider newInstance(GatewayApi api) {
    return new GatewayMusicProvider(api);
  }
}
