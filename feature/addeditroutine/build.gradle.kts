plugins {
    id("routineblocker.android.feature")
    id("routineblocker.android.library.compose")
    id("routineblocker.android.koin")
}
android {
    namespace = "com.rendox.routinetracker.feature.addeditroutine"
}
dependencies {
    implementation(project(":core:data"))
    implementation(libs.kizitonwose.calendar.compose)
    implementation(libs.jetbrains.kotlinx.datetime)
}
