plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val versionNameFromEnv: String = System.getenv("VERSION_NAME")
    ?: providers.exec {
        commandLine("git", "describe", "--tags", "--abbrev=0")
    }.standardOutput.asText.map { it.trim().removePrefix("v") }.getOrElse("1.0.0")

val versionCodeFromEnv: Int = System.getenv("VERSION_CODE")?.toIntOrNull()
    ?: providers.exec {
        commandLine("git", "tag", "--list", "v*")
    }.standardOutput.asText.map { text ->
        text.trim().lines().filter { it.isNotEmpty() }.size.coerceAtLeast(1)
    }.getOrElse(1)

android {
    namespace = "com.ethersense"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ethersense"
        minSdk = 26
        targetSdk = 35
        versionCode = versionCodeFromEnv
        versionName = versionNameFromEnv

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH")
            val keystorePassword = System.getenv("KEYSTORE_PASSWORD")
            val keyAliasEnv = System.getenv("KEY_ALIAS")
            val keyPasswordEnv = System.getenv("KEY_PASSWORD")

            if (keystorePath != null && keystorePassword != null && keyAliasEnv != null && keyPasswordEnv != null) {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                keyAlias = keyAliasEnv
                keyPassword = keyPasswordEnv
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "FULL"
            }
            val releaseSigningConfig = signingConfigs.findByName("release")
            if (releaseSigningConfig?.storeFile != null) {
                signingConfig = releaseSigningConfig
            }
        }
        debug {
            isMinifyEnabled = false
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
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.bundles.lifecycle)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.bundles.compose.debug)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Coroutines
    implementation(libs.bundles.coroutines)

    // Accompanist (Permissions)
    implementation(libs.accompanist.permissions)

    // DataStore
    implementation(libs.datastore.preferences)

    // OkHttp (Speed Test)
    implementation(libs.okhttp)

    // Testing
    testImplementation(libs.bundles.testing)
    androidTestImplementation(libs.bundles.androidTesting)
    androidTestImplementation(platform(libs.compose.bom))
}
