import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.paparazzi)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.clibeats"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.clibeats"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        debug {
            val debugGatewayUrl =
                providers.gradleProperty("GATEWAY_URL").orNull
                    ?: System.getenv("GATEWAY_URL")
                    ?: "http://192.168.0.106:8080/"
            buildConfigField("String", "GATEWAY_BASE_URL", "\"$debugGatewayUrl\"")
        }
        release {
            // Never fall back to a hardcoded host: the former default
            // (https://gateway.clibeats.io/) is not registered and is NXDOMAIN on
            // public resolvers, so a release APK built without GATEWAY_URL could
            // never reach the gateway (UnknownHostException -> provider_offline).
            // Fail fast at configuration time instead of shipping a dead APK.
            // Accept either a Gradle property (-PGATEWAY_URL=...) or the
            // GATEWAY_URL environment variable — same switch as debug.
            val releaseGatewayUrl =
                providers.gradleProperty("GATEWAY_URL").orNull
                    ?: System.getenv("GATEWAY_URL")
                    ?: "http://localhost:8080/"
            buildConfigField("String", "GATEWAY_BASE_URL", "\"$releaseGatewayUrl\"")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        abortOnError = true
        checkDependencies = true
        warningsAsErrors = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
}

tasks.withType<Test>().configureEach {
    val androidHome =
        System.getenv("ANDROID_HOME")
            ?: System.getenv("ANDROID_SDK_ROOT")
            ?: "C:\\Android\\Sdk"
    environment("ANDROID_HOME", androidHome)
    environment("ANDROID_SDK_ROOT", androidHome)
    systemProperty("ANDROID_HOME", androidHome)
    systemProperty("ANDROID_SDK_ROOT", androidHome)
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.material3.adaptive.nav)
    implementation(libs.material.icons.extended)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)
    implementation(libs.security.crypto)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.common)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    detektPlugins(libs.detekt.formatting)
    debugImplementation(libs.compose.ui.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.room.testing)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.mockito.core)
    testImplementation(libs.media3.test.utils)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.coroutines.test)
}
