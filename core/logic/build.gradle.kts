plugins {
    id("routineblocker.android.library")
}
android {
    namespace = "com.rendox.routinetracker.core.logic"
}
dependencies {
    implementation(project(":core:model"))
    implementation(libs.jetbrains.kotlinx.datetime)
}
