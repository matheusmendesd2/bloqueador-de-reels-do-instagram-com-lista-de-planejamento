plugins {
    id("routineblocker.android.library")
    id("routineblocker.android.koin")
    alias(libs.plugins.app.cash.sqldelight)
}
android {
    namespace = "com.rendox.routinetracker.core.database"
    defaultConfig {
        testInstrumentationRunner = "com.rendox.routinetracker.core.testcommon.InstrumentationTestRunner"
    }
}
dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:logic"))
    implementation(libs.jetbrains.kotlinx.datetime)
    implementation(libs.jetbrains.kotlinx.coroutines.core)
    implementation(libs.app.cash.sqldelight.android.driver)
    implementation(libs.app.cash.sqldelight.coroutines.extensions.jvm)
    implementation(libs.app.cash.sqldelight.primitive.adapters)
    implementation(libs.app.cash.sqldelight.sqlite.driver)
}
sqldelight {
    databases {
        create("RoutineTrackerDatabase") {
            packageName.set("com.rendox.routinetracker.core.database")
        }
    }
}
