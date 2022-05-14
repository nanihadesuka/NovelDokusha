import java.util.Properties

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
}

android {

    val localPropertiesFile = file("../local.properties")
    val isSignBuild = localPropertiesFile.exists()

    compileSdk = 31

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.1.1"
    }

    defaultConfig {
        applicationId = "my.noveldokusha"
        minSdk = 26
        targetSdk = 31
        versionCode = 8
        versionName = "1.6.0"
        setProperty("archivesBaseName", "NovelDokusha_v$versionName")
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
                isRemoveUnusedCode = false
                isObfuscate = false
                isOptimizeCode = false
                isRemoveUnusedResources = false
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

    implementation(fileTree("libs") { include("*.jar") })

    // Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1-native-mt")
    implementation("org.jetbrains.kotlin:kotlin-script-runtime:1.6.20")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.21")

    // Room components
    implementation("androidx.room:room-runtime:2.4.2")
    implementation("androidx.room:room-ktx:2.4.2")
    kapt("androidx.room:room-compiler:2.4.2")
    androidTestImplementation("androidx.room:room-testing:2.4.2")

    // Lifecycle components
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.4.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.4.1")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.4.1")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")

    // Preferences
    implementation("androidx.preference:preference-ktx:1.2.0")

    // UI
    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("androidx.core:core-ktx:1.9.0-alpha03")
    implementation("androidx.navigation:navigation-fragment-ktx:2.4.2")
    implementation("androidx.navigation:navigation-ui-ktx:2.4.2")
    implementation("androidx.activity:activity-ktx:1.4.0")
    implementation("androidx.fragment:fragment-ktx:1.4.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.3")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("com.google.android.material:material:1.7.0-alpha01")
    implementation("com.l4digital.fastscroll:fastscroll:2.0.1")
    implementation("com.afollestad.material-dialogs:core:3.3.0")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.13.2")
    kapt("com.github.bumptech.glide:compiler:4.13.2")

    // Test
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")

    // GSON
    implementation("com.google.code.gson:gson:2.9.0")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")

    // Dependency injection
    implementation("com.google.dagger:hilt-android:2.42")
    kapt("com.google.dagger:hilt-compiler:2.42")

    // HTML text extractor
    implementation("com.chimbori.crux:crux:3.5.3")
    implementation("net.dankito.readability4j:readability4j:1.0.8")
    implementation("org.jsoup:jsoup:1.14.3")

    // Memory leak detector
    //debugImplementation("com.squareup.leakcanary:leakcanary-android:2.7")

    // Jetpack compose
    implementation("androidx.activity:activity-compose:1.4.0")
    implementation("androidx.compose.material:material:1.1.1")
    implementation("androidx.compose.animation:animation:1.1.1")
    implementation("androidx.compose.ui:ui-tooling:1.1.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.4.1")
    implementation("androidx.compose.runtime:runtime-livedata:1.1.1")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.0.0")
    implementation("androidx.compose.material:material-icons-extended:1.1.1")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.24.6-alpha")
    implementation("com.google.accompanist:accompanist-swiperefresh:0.20.2")
    implementation("com.google.accompanist:accompanist-insets:0.14.0")
    implementation("com.google.accompanist:accompanist-pager:0.20.2")
    implementation("com.google.accompanist:accompanist-pager-indicators:0.20.2")

    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.1.1")

    // Coil for jetpack compose
    implementation("io.coil-kt:coil-compose:2.0.0")


    // Compose collapsing toolbar
    implementation("me.onebone:toolbar-compose:2.3.3")

    // Compose scroll bar
    implementation("com.github.nanihadesuka:LazyColumnScrollbar:1.0.3")

}