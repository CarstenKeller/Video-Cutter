plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.carstenkeller.videocutter"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.carstenkeller.videocutter"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-mvp"
    }

    // Fester, ins Repo eingecheckter Debug-Keystore statt des von AGP pro
    // Maschine automatisch generierten ~/.android/debug.keystore. Ohne das
    // würde jeder CI-Build (frischer Runner) mit einem anderen Schlüssel
    // signiert, und Android verweigert dann die Installation einer neuen
    // Version über eine bereits installierte ("App nicht installiert").
    signingConfigs {
        getByName("debug") {
            storeFile = file("../keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.01.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")

    // Media3: ExoPlayer für die Vorschau, Transformer für den Export.
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")
    implementation("androidx.media3:media3-transformer:1.5.1")
    implementation("androidx.media3:media3-common:1.5.1")
    implementation("androidx.media3:media3-effect:1.5.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
