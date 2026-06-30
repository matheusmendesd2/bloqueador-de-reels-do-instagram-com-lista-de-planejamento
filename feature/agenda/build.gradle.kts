plugins {
    id("routineblocker.android.feature")
    id("routineblocker.android.library.compose")
    id("routineblocker.android.koin")
}
android {
    namespace = "com.rendox.routinetracker.feature.agenda"
    buildFeatures {
        viewBinding = true
    }
}
dependencies {
    implementation(project(":core:data"))
    implementation(libs.kizitonwose.calendar.compose)
    implementation(libs.jetbrains.kotlinx.datetime)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.compose.ui.viewbinding)
}
