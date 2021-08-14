import java.util.Properties

plugins {
	id("com.android.application")
	id("kotlin-android")
	id("kotlin-kapt")
}

android {
	
	compileSdk = 30
	
	val localPropertiesFile = file("../local.properties")
	val isSignBuild = localPropertiesFile.exists()
	
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_1_8
		targetCompatibility = JavaVersion.VERSION_1_8
	}
	
	kotlinOptions {
		jvmTarget = JavaVersion.VERSION_1_8.toString()
		useIR = true
	}
	
	composeOptions {
		kotlinCompilerExtensionVersion = "1.0.1"
		kotlinCompilerVersion = "1.5.10"
	}
	
	defaultConfig {
		applicationId = "my.noveldokusha"
		minSdk = 26
		targetSdk = 30
		versionCode = 1
		versionName = "1.0"
		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}
	
	if (isSignBuild) signingConfigs {
		create("release") {
			val properties = Properties().apply {
				load(localPropertiesFile.inputStream())
			}
			storeFile = file(properties.getProperty("storeFile"))
			storePassword = properties.getProperty("storePassword")
			keyAlias = properties.getProperty("keyAlias")
			keyPassword = properties.getProperty("keyPassword")
		}
	}
	
	buildTypes {
		
		if (isSignBuild) all {
			signingConfig = signingConfigs["release"]
		}
		
		named("debug") {
			postprocessing {
				proguardFile("proguard-rules.pro")
				isRemoveUnusedCode = true
				isObfuscate = false
				isOptimizeCode = true
				isRemoveUnusedResources = true
			}
		}
		
		named("release") {
			postprocessing {
				proguardFile("proguard-rules.pro")
				isRemoveUnusedCode = true
				isObfuscate = false
				isOptimizeCode = true
				isRemoveUnusedResources = true
			}
		}
	}
	
	buildFeatures {
		viewBinding = true
		compose = true
	}
}

dependencies {
	
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")
	
	implementation("androidx.appcompat:appcompat:1.3.1")
	
	// Room components
	implementation("androidx.room:room-runtime:2.3.0")
	implementation("androidx.room:room-ktx:2.3.0")
	kapt("androidx.room:room-compiler:2.3.0")
	androidTestImplementation("androidx.room:room-testing:2.3.0")
	
	// Lifecycle components
	implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.3.1")
	implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.3.1")
	implementation("androidx.lifecycle:lifecycle-common-java8:2.3.1")
	implementation("androidx.coordinatorlayout:coordinatorlayout:1.1.0")
	
	// UI
	implementation("androidx.constraintlayout:constraintlayout:2.1.0")
	implementation("com.google.android.material:material:1.4.0")
	
	implementation("com.google.code.gson:gson:2.8.7")
	
	implementation("androidx.recyclerview:recyclerview:1.2.1")
	implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
	
	
	implementation(fileTree("libs") { include("*.jar") })
	implementation("org.jetbrains.kotlin:kotlin-stdlib:1.5.21")
	implementation("androidx.core:core-ktx:1.6.0")
	implementation("androidx.navigation:navigation-fragment-ktx:2.3.5")
	implementation("androidx.navigation:navigation-ui-ktx:2.3.5")
	implementation("org.jsoup:jsoup:1.14.1")
	
	implementation("org.jetbrains.kotlin:kotlin-script-runtime:1.5.30-RC")
	
	implementation("com.afollestad.material-dialogs:core:3.2.1")
	
	// Glide
	implementation("com.github.bumptech.glide:glide:4.12.0")
	kapt("com.github.bumptech.glide:compiler:4.12.0")
	
	implementation("com.chimbori.crux:crux:3.0.1")
	implementation("net.dankito.readability4j:readability4j:1.0.6")
	
	implementation("com.l4digital.fastscroll:fastscroll:2.0.1")
	
	// Jetpack compose
	
	implementation("androidx.compose.ui:ui:1.0.1")
	// Tooling support (Previews, etc.)
	implementation("androidx.compose.ui:ui-tooling:1.0.1")
	// Foundation (Border, Background, Box, Image, Scroll, shapes, animations, etc.)
	implementation("androidx.compose.foundation:foundation:1.0.1")
	// Material Design
	implementation("androidx.compose.material:material:1.0.1")
	// Material design icons
	implementation("androidx.compose.material:material-icons-core:1.0.1")
	implementation("androidx.compose.material:material-icons-extended:1.0.1")
	// Integration with activities
	implementation("androidx.activity:activity-compose:1.3.1")
	// Integration with ViewModels
	implementation("androidx.lifecycle:lifecycle-viewmodel-compose:1.0.0-alpha07")
	// Integration with observables
	implementation("androidx.compose.runtime:runtime-livedata:1.0.1")
	implementation("androidx.compose.runtime:runtime-rxjava2:1.0.1")
	// UI Tests
	androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.0.1")
}