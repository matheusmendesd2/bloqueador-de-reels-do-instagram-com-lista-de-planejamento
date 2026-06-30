import com.android.build.api.dsl.ApplicationExtension
import com.rendox.routineblocker.buildlogic.addAndroidTestDependencies
import com.rendox.routineblocker.buildlogic.addLocalTestDependencies
import com.rendox.routineblocker.buildlogic.configureBuildTypes
import com.rendox.routineblocker.buildlogic.configureKotlinAndroid
import com.rendox.routineblocker.buildlogic.configurePackaging
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("org.jetbrains.kotlin.android")
            }

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = 34
                configureBuildTypes(this)
                configurePackaging(this)
                addLocalTestDependencies(this)
                addAndroidTestDependencies(this)
            }

            extensions.getByType<KotlinAndroidProjectExtension>().apply {
                configureKotlinAndroid(this)
            }

            dependencies {
                add("testImplementation", project(":core:testcommon"))
            }
        }
    }
}
