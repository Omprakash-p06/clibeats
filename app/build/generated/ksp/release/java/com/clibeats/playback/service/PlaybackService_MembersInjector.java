package com.clibeats.playback.service;

import androidx.media3.exoplayer.ExoPlayer;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class PlaybackService_MembersInjector implements MembersInjector<PlaybackService> {
  private final Provider<ExoPlayer> playerProvider;

  public PlaybackService_MembersInjector(Provider<ExoPlayer> playerProvider) {
    this.playerProvider = playerProvider;
  }

  public static MembersInjector<PlaybackService> create(Provider<ExoPlayer> playerProvider) {
    return new PlaybackService_MembersInjector(playerProvider);
  }

  @Override
  public void injectMembers(PlaybackService instance) {
    injectPlayer(instance, playerProvider.get());
  }

  @InjectedFieldSignature("com.clibeats.playback.service.PlaybackService.player")
  public static void injectPlayer(PlaybackService instance, ExoPlayer player) {
    instance.player = player;
  }
}
