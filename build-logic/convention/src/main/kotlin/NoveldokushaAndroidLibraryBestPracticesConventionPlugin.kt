import com.android.build.api.dsl.LibraryExtension
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
            }

            applyHilt()

            extensions.configure<LibraryExtension> {
                configureAndroid(this)
                // Libraries have no targetSdk of their own since AGP 9, it only applies to tests and lint
                testOptions.targetSdk = appConfig.TARGET_SDK
                lint.targetSdk = appConfig.TARGET_SDK
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
