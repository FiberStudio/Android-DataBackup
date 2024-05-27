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
        maven("https://jitpack.io")
    }
}

rootProject.name = "DataBackup"
include(":app")
include(":core:common")
include(":core:service")
include(":core:ui")
include(":core:model")
include(":core:database")
include(":core:data")
include(":core:datastore")
include(":core:util")
include(":core:hiddenapi")
include(":core:rootservice")
include(":core:network")
include(":core:provider")
include(":feature:crash")
include(":feature:setup")
include(":feature:main:dashboard")
include(":feature:main:restore")
include(":feature:main:cloud")
include(":feature:main:settings")
include(":feature:main:packages")
include(":feature:main:directory")
include(":feature:flavor:foss")
include(":feature:flavor:premium")
include(":feature:flavor:alpha")
