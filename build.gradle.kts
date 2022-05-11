// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
	repositories {
		google()
		jcenter()
		mavenCentral()
	}
	dependencies {
		classpath("com.android.tools.build:gradle:7.2.0")
		classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.10")
		classpath("com.google.dagger:hilt-android-gradle-plugin:2.40.1")

		// NOTE: Do not place your application dependencies here; they belong
		// in the individual module build.gradle.kts files
	}
}

allprojects {
	repositories {
		google()
		jcenter()
		maven { setUrl("https://jitpack.io") }
	}
}

tasks.register("clean", Delete::class) {
	delete(rootProject.buildDir)
}