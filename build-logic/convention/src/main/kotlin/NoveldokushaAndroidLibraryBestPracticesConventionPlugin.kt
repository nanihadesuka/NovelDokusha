import com.android.build.gradle.LibraryExtension
import my.noveldoksuha.convention.plugin.appConfig
import my.noveldoksuha.convention.plugin.applyHilt
import my.noveldoksuha.convention.plugin.configureAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class NoveldokushaAndroidLibraryBestPracticesConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {

        with(target) {
            with(pluginManager) {
                apply("com.android.library")
                apply("org.jetbrains.kotlin.android")
            }

            applyHilt()

            extensions.configure<LibraryExtension> {
                configureAndroid(this)
                defaultConfig.targetSdk = appConfig.TARGET_SDK
                // The resource prefix is derived from the module name,
                // so resources inside ":core:module1" must be prefixed with "core_module1_"
                resourcePrefix = path
                    .split("""\W""".toRegex()).drop(1).distinct()
                    .joinToString(separator = "_")
                    .lowercase() + "_"

                buildTypes {
                    release {
                        isMinifyEnabled = false
                    }
                }
            }
        }
    }
}
