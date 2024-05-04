import com.android.build.api.dsl.ApplicationExtension
import my.noveldoksuha.convention.plugin.appConfig
import my.noveldoksuha.convention.plugin.applyHilt
import my.noveldoksuha.convention.plugin.configureAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class NoveldokushaAndroidApplicationBestPracticesConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {

        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("org.jetbrains.kotlin.android")
            }
            applyHilt()

            extensions.configure<ApplicationExtension> {
                configureAndroid(this)
                defaultConfig.targetSdk = appConfig.targetSdk
            }
        }
    }

}
