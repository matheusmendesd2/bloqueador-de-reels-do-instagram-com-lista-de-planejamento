plugins {
    id("routineblocker.android.application")
    id("routineblocker.android.application.compose")
    id("routineblocker.android.koin")
}

android {
    namespace = "com.rendox.routineblocker"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.rendox.routineblocker"
        minSdk = 24
        multiDexEnabled = true
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    compileOptions {
        // java.time e usado no bloqueador e o minSdk e 24, entao o desugaring precisa
        // estar habilitado onde o dexing final acontece (somente no modulo :app).
        isCoreLibraryDesugaringEnabled = true
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:database"))
    implementation(project(":core:model"))
    implementation(project(":core:ui"))
    implementation(project(":core:domain"))
    implementation(project(":core:logic"))
    implementation(project(":feature:addeditroutine"))
    implementation(project(":feature:agenda"))
    implementation(project(":feature:routinedetails"))
    implementation(project(":feature:shortsblocker"))

    coreLibraryDesugaring(libs.android.tools.desugar)

    implementation(libs.jetbrains.kotlinx.datetime)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.dataStore.core)
    implementation(libs.androidx.dataStore.preferences)
    implementation(libs.timber.logging)
}
