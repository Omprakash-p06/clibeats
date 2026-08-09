package com.clibeats.di;

import androidx.media3.common.AudioAttributes;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class PlaybackModule_ProvideAudioAttributesFactory implements Factory<AudioAttributes> {
  @Override
  public AudioAttributes get() {
    return provideAudioAttributes();
  }

  public static PlaybackModule_ProvideAudioAttributesFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static AudioAttributes provideAudioAttributes() {
    return Preconditions.checkNotNullFromProvides(PlaybackModule.INSTANCE.provideAudioAttributes());
  }

  private static final class InstanceHolder {
    private static final PlaybackModule_ProvideAudioAttributesFactory INSTANCE = new PlaybackModule_ProvideAudioAttributesFactory();
  }
}
