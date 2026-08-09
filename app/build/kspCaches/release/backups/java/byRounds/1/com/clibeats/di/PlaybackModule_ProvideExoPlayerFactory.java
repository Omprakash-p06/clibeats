package com.clibeats.di;

import android.content.Context;
import androidx.media3.common.AudioAttributes;
import androidx.media3.exoplayer.ExoPlayer;
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
public final class PlaybackModule_ProvideExoPlayerFactory implements Factory<ExoPlayer> {
  private final Provider<Context> contextProvider;

  private final Provider<AudioAttributes> audioAttributesProvider;

  public PlaybackModule_ProvideExoPlayerFactory(Provider<Context> contextProvider,
      Provider<AudioAttributes> audioAttributesProvider) {
    this.contextProvider = contextProvider;
    this.audioAttributesProvider = audioAttributesProvider;
  }

  @Override
  public ExoPlayer get() {
    return provideExoPlayer(contextProvider.get(), audioAttributesProvider.get());
  }

  public static PlaybackModule_ProvideExoPlayerFactory create(Provider<Context> contextProvider,
      Provider<AudioAttributes> audioAttributesProvider) {
    return new PlaybackModule_ProvideExoPlayerFactory(contextProvider, audioAttributesProvider);
  }

  public static ExoPlayer provideExoPlayer(Context context, AudioAttributes audioAttributes) {
    return Preconditions.checkNotNullFromProvides(PlaybackModule.INSTANCE.provideExoPlayer(context, audioAttributes));
  }
}
