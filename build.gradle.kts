// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
	repositories {
		google()
		jcenter()
		mavenCentral()
	}
	dependencies {
		classpath("com.android.tools.build:gradle:4.1.3")
		classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.21")
		
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