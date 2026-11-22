import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

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
        // CI passes -PversionName=1.2.3 -PversionCode=10203 from the git tag (major*10000 + minor*100 + patch).
        versionCode = providers.gradleProperty("versionCode").orElse("20400").get().toInt()
        versionName = providers.gradleProperty("versionName").orElse("2.4.0").get()

        // Optional free Jamendo API key (developer.jamendo.com). Empty by default.
        val jamendoClientId = providers.gradleProperty("JAMENDO_CLIENT_ID").orElse("").get()
        buildConfigField("String", "JAMENDO_CLIENT_ID", "\"${jamendoClientId.replace("\"", "\\\"")}\"")
    }

    // Release signing from env (set by .github/workflows/release.yml). A release APK signed with
    // the debug key cannot update an installed release in place (Android rejects the different
    // certificate), so missing secrets are a hard error. Local test builds can opt out explicitly
    // with -PallowDebugSigning=true; that APK must never be published.
    val allowDebugSigning = providers.gradleProperty("allowDebugSigning").orNull?.toBoolean() == true
    val releaseKeystore = System.getenv("KEYSTORE_FILE")?.let { file(it) }?.takeIf { it.exists() }
    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig =
                signingConfigs.findByName("release")
                    ?: if (allowDebugSigning) signingConfigs.getByName("debug") else null
        }
    }

    // Fail only when a release artifact is actually built, so debug/test tasks still work on a
    // machine without the release keystore.
    tasks.matching {
        it.name == "assembleRelease" || it.name == "bundleRelease" || it.name == "packageRelease"
    }.configureEach {
        doFirst {
            if (signingConfigs.findByName("release") == null && !allowDebugSigning) {
                throw GradleException(
                    "Release builds require KEYSTORE_FILE/KEYSTORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD " +
                        "so the APK can update existing installs. For a throwaway local build, pass " +
                        "-PallowDebugSigning=true (never publish that APK).",
                )
            }
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
    val localProperties = Properties()
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use { localProperties.load(it) }
    }
    val defaultSdk =
        File(System.getProperty("user.home"), "Android/Sdk").takeIf { it.exists() }?.absolutePath
            ?: "C:\\Android\\Sdk"
    val androidHome =
        System.getenv("ANDROID_HOME")
            ?: System.getenv("ANDROID_SDK_ROOT")
            ?: localProperties.getProperty("sdk.dir")
            ?: defaultSdk
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
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.common)
    implementation(libs.media3.datasource.okhttp)
    implementation(libs.androidx.media)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.palette.ktx)
    implementation(libs.newpipe.extractor)
    implementation(libs.google.guava)
    implementation(libs.reorderable)
    detektPlugins(libs.detekt.formatting)
    debugImplementation(libs.compose.ui.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.google.truth)
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
