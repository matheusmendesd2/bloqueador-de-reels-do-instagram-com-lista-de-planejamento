plugins {
    id("routineblocker.android.library")
    id("routineblocker.android.library.compose")
}
android {
    namespace = "com.rendox.routinetracker.core.ui"
}
dependencies {
    implementation(project(":core:model"))
    implementation(libs.jetbrains.kotlinx.datetime)
    implementation(libs.kizitonwose.calendar.compose)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.android.material)
}
