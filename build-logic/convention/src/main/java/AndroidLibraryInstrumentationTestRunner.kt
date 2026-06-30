import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidLibraryInstrumentationTestRunner : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
        }
    }
}
