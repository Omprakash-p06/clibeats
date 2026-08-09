package com.clibeats.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import kotlinx.serialization.json.Json;
import okhttp3.OkHttpClient;
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
public final class NetworkModule_ProvideGatewayRetrofitFactory implements Factory<Retrofit> {
  private final Provider<Json> jsonProvider;

  private final Provider<OkHttpClient> okHttpClientProvider;

  public NetworkModule_ProvideGatewayRetrofitFactory(Provider<Json> jsonProvider,
      Provider<OkHttpClient> okHttpClientProvider) {
    this.jsonProvider = jsonProvider;
    this.okHttpClientProvider = okHttpClientProvider;
  }

  @Override
  public Retrofit get() {
    return provideGatewayRetrofit(jsonProvider.get(), okHttpClientProvider.get());
  }

  public static NetworkModule_ProvideGatewayRetrofitFactory create(Provider<Json> jsonProvider,
      Provider<OkHttpClient> okHttpClientProvider) {
    return new NetworkModule_ProvideGatewayRetrofitFactory(jsonProvider, okHttpClientProvider);
  }

  public static Retrofit provideGatewayRetrofit(Json json, OkHttpClient okHttpClient) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideGatewayRetrofit(json, okHttpClient));
  }
}
