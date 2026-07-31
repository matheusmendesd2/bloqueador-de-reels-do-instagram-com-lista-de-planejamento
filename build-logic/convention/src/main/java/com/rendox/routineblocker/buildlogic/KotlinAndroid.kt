package com.rendox.routineblocker.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    commonExtension.apply {
        compileSdk = 34

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_17
            // java.time e usado no bloqueador e o minSdk e 24, entao o desugaring
            // precisa estar ligado de verdade - nao basta declarar a dependencia.
            isCoreLibraryDesugaringEnabled = true
        }
    }

    dependencies {
        add("coreLibraryDesugaring", libs.findLibrary("android.tools.desugar").get())
    }
}

internal fun configureKotlinAndroid(
    extension: KotlinAndroidProjectExtension,
) {
    extension.apply {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}
