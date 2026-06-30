import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

group = "com.rendox.routineblocker.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.sqldelight.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplicationCompose") {
            id = "routineblocker.android.application.compose"
            implementationClass = "AndroidApplicationComposeConventionPlugin"
        }
        register("androidApplication") {
            id = "routineblocker.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "routineblocker.android.library.compose"
            implementationClass = "AndroidLibraryComposeConventionPlugin"
        }
        register("androidLibrary") {
            id = "routineblocker.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidFeature") {
            id = "routineblocker.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("jvmLibrary") {
            id = "routineblocker.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("androidKoin") {
            id = "routineblocker.android.koin"
            implementationClass = "AndroidKoinConventionPlugin"
        }
        register("instrumentationTestRunner") {
            id = "routineblocker.android.library.instrumentationtestrunner"
            implementationClass = "AndroidLibraryInstrumentationTestRunner"
        }
        register("lint") {
            id = "routineblocker.lint"
            implementationClass = "StaticAnalysisConventionPlugin"
        }
    }
}
