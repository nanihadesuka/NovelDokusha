pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "NovelDokusha"
include(":app")
include(":features:local_database")
include(":scraper")
include(":strings")
include(":core")
include(":networking")
