plugins {
    id("routineblocker.android.feature")
    id("routineblocker.android.library.compose")
    id("routineblocker.android.koin")
}
android {
    namespace = "com.rendox.routineblocker.feature.shortsblocker"
}
dependencies {
    implementation(libs.androidx.dataStore.core)
    implementation(libs.androidx.dataStore.preferences)
    implementation(libs.timber.logging)
    implementation(libs.androidx.compose.material.icons.extended)
}
