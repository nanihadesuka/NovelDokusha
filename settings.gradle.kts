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
include(":scraper")
include(":strings")
include(":core")
include(":networking")
include(":coreui")
include(":data")

include(":tooling:local_database")
include(":tooling:epub_parser")
include(":tooling:text_translator:translator")
include(":tooling:text_translator:domain")
include(":tooling:text_translator:translator_nop")
include(":tooling:textToSpeech")

include(":features:reader")

include(":navigation")
