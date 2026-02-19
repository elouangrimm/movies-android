plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "xyz.elouan.movies"
    compileSdk = 34

    defaultConfig {
        applicationId = "xyz.elouan.movies"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // Inject OMDB API key from environment variable (set as GitHub secret)
        val omdbKey = System.getenv("OMDB_API_KEY") ?: ""
        buildConfigField("String", "OMDB_API_KEY", "\"$omdbKey\"")
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        // Release signing — populated via environment variables injected by CI secrets
        create("release") {
            val storeFile = System.getenv("STORE_FILE")
            val storePassword = System.getenv("STORE_PASSWORD")
            val keyAlias = System.getenv("KEY_ALIAS")
            val keyPassword = System.getenv("KEY_PASSWORD")
            if (storeFile != null && storePassword != null && keyAlias != null && keyPassword != null) {
                this.storeFile = file(storeFile)
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.coil)
    implementation(libs.kotlinx.coroutines.android)
}
