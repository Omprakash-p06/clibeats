package com.clibeats.telemetry;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class TimberTelemetryTracker_Factory implements Factory<TimberTelemetryTracker> {
  @Override
  public TimberTelemetryTracker get() {
    return newInstance();
  }

  public static TimberTelemetryTracker_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static TimberTelemetryTracker newInstance() {
    return new TimberTelemetryTracker();
  }

  private static final class InstanceHolder {
    private static final TimberTelemetryTracker_Factory INSTANCE = new TimberTelemetryTracker_Factory();
  }
}
