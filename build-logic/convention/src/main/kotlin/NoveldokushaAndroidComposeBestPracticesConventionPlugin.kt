
import com.android.build.api.dsl.CommonExtension
import my.noveldoksuha.convention.plugin.implementation
import my.noveldoksuha.convention.plugin.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class NoveldokushaAndroidComposeBestPracticesConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            val extension = extensions.getByType<CommonExtension>()

            extension.apply {
                buildFeatures.compose = true

                dependencies {
                    implementation(libs.findLibrary("compose-androidx-ui").get())
                    implementation(libs.findLibrary("compose-androidx-ui-tooling").get())
                }

                // For Robolectric
                testOptions.unitTests.isIncludeAndroidResources = true
            }

        }
    }
}
