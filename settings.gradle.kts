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
        jcenter()
        maven { setUrl("https://jitpack.io") }
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "NovelDokusha"
include(":app")
include(":features:local_database")
include(":scraper")
include(":strings")
include(":core")
include(":networking")
include(":features:epub_parser")
include(":features:text_translator:translator")
include(":features:text_translator:domain")
include(":features:text_translator:translator_nop")
