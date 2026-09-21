import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Release signing is configured from environment variables (used by GitHub Actions)
// or from a local keystore.properties file. If neither is present, the release
// build falls back to the debug signing key so local builds still work.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) load(keystorePropsFile.inputStream())
}

fun cfg(env: String, prop: String): String? =
    System.getenv(env) ?: keystoreProps.getProperty(prop)

val releaseStoreFile = cfg("KEYSTORE_FILE", "storeFile")
val hasReleaseSigning = releaseStoreFile != null && file(releaseStoreFile).exists()

android {
    namespace = "com.yogev.youtubeupdater"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.yogev.youtubeupdater"
        minSdk = 26
        targetSdk = 31
        versionCode = 7
        versionName = "1.0.6"
        vectorDrawables { useSupportLibrary = true }

        // Optional embedded read-only GitHub token to raise the API rate limit.
        // Provided at build time via -PgithubToken=... or the GITHUB_DEFAULT_TOKEN
        // env var; never committed. A token set in-app Settings takes precedence.
        val embeddedToken = (project.findProperty("githubToken") as String?)
            ?: System.getenv("EMBEDDED_GH_TOKEN")
            ?: ""
        buildConfigField("String", "DEFAULT_GITHUB_TOKEN", "\"$embeddedToken\"")
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = cfg("KEYSTORE_PASSWORD", "storePassword")
                keyAlias = cfg("KEY_ALIAS", "keyAlias")
                keyPassword = cfg("KEY_PASSWORD", "keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        // targetSdk 31 is intentional (sideloaded app, avoids the POST_NOTIFICATIONS
        // runtime permission on API 33+). This is only a Google Play requirement.
        disable += "ExpiredTargetSdkVersion"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")

    val composeBom = platform("androidx.compose:compose-bom:2024.09.02")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.work:work-runtime-ktx:2.9.1")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
