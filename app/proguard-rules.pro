# CLIBeats Security & Obfuscation Rules

# Keep Room Entities and DAOs
-keep class com.clibeats.data.local.entity.** { *; }
-keep class com.clibeats.data.local.dao.** { *; }

# Keep Gateway DTOs & kotlinx.serialization DTOs
-keep class com.clibeats.data.gateway.dto.** { *; }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}

# Keep Domain Models
-keep class com.clibeats.domain.model.** { *; }

# Keep Media3 ExoPlayer components
-keep class androidx.media3.** { *; }

# Suppress warnings from OkHttp / Retrofit
-dontwarn okhttp3.**
-dontwarn retrofit2.**