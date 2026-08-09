# CLIBeats Security & Obfuscation Rules

# Keep Room Entities and DAOs
-keep class com.clibeats.data.local.entity.** { *; }
-keep class com.clibeats.data.local.dao.** { *; }

# Keep kotlinx.serialization DTOs
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}

# Keep Domain Models
-keep class com.clibeats.domain.model.** { *; }

# Suppress warnings from OkHttp / Retrofit
-dontwarn okhttp3.**
-dontwarn retrofit2.**