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
include(":tooling:backup_restore")
include(":tooling:backup_create")
include(":tooling:epub_importer")

include(":features:reader")
include(":features:chaptersList")
include(":features:globalSourceSearch")

include(":navigation")
include(":features:databaseExplorer")
include(":features:sourceExplorer")
include(":features:catalogExplorer")
include(":features:settings")
include(":features:libraryExplorer")
