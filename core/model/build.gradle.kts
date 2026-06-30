plugins {
    id("routineblocker.android.library")
    id("routineblocker.android.library.compose")
}
android {
    namespace = "com.rendox.routinetracker.core.model"
}
dependencies {
    implementation(libs.jetbrains.kotlinx.datetime)
}
