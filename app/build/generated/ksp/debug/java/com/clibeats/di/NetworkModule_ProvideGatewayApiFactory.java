package com.clibeats.di;

import com.clibeats.data.gateway.api.GatewayApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import retrofit2.Retrofit;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("javax.inject.Named")
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
public final class NetworkModule_ProvideGatewayApiFactory implements Factory<GatewayApi> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideGatewayApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public GatewayApi get() {
    return provideGatewayApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideGatewayApiFactory create(Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideGatewayApiFactory(retrofitProvider);
  }

  public static GatewayApi provideGatewayApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideGatewayApi(retrofit));
  }
}
