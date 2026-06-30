plugins {
    id("routineblocker.android.library")
    id("routineblocker.android.koin")
}
android {
    namespace = "com.rendox.routinetracker.core.domain"
}
dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:database"))
    implementation(project(":core:model"))
    implementation(project(":core:logic"))
    implementation(libs.jetbrains.kotlinx.coroutines.core)
    implementation(libs.jetbrains.kotlinx.datetime)
}
