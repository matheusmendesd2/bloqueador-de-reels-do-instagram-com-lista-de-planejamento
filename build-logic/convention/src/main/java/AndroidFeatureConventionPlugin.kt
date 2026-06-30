import com.android.build.gradle.LibraryExtension
import com.rendox.routineblocker.buildlogic.addAndroidTestDependencies
import com.rendox.routineblocker.buildlogic.addLocalTestDependencies
import com.rendox.routineblocker.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply {
                apply("routineblocker.android.library")
            }
            extensions.configure<LibraryExtension> {
                addLocalTestDependencies(this)
                addAndroidTestDependencies(this)

                defaultConfig {
                    testInstrumentationRunner =
                        "com.rendox.routinetracker.core.testcommon.InstrumentationTestRunner"
                }
            }

            dependencies {
                add("testImplementation", project(":core:testcommon"))

                add("implementation", project(":core:ui"))
                add("implementation", project(":core:model"))
                add("implementation", project(":core:domain"))
                add("implementation", project(":core:logic"))

                add("implementation", libs.findLibrary("androidx-lifecycle-runtime-ktx").get())
                add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
                add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
                add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-ktx").get())
                add("implementation", libs.findLibrary("androidx-navigation").get())
            }
        }
    }
}
