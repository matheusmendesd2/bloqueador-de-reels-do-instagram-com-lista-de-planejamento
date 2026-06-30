pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "RoutineBlocker"
include(":app")
include(":core:database")
include(":core:ui")
include(":core:logic")
include(":core:testcommon")
include(":core:data")
include(":core:model")
include(":core:domain")
include(":feature:routinedetails")
include(":feature:agenda")
include(":feature:addeditroutine")
include(":feature:shortsblocker")
