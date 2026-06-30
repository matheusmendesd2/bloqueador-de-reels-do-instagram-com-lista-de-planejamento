plugins {
    id("routineblocker.android.library")
}
android {
    namespace = "com.rendox.routinetracker.core.testcommon"
}
dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:domain"))
}
