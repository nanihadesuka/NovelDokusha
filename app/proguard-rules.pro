# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keep public class * extends androidx.lifecycle.ViewModel { *; }

################################################################################
# Source classes — keep all source implementations and their metadata.
# Sources are instantiated reflectively via Scraper.kt and their properties
# (id, nameStrId, baseUrl, catalogUrl, language, iconUrl) are accessed at
# runtime by name. Without these keep rules, R8 could rename fields and break
# source discovery / catalog listing.
################################################################################
-keep class my.noveldokusha.scraper.sources.** { *; }
-keep class my.noveldokusha.scraper.databases.** { *; }
-keep class my.noveldokusha.scraper.Scraper { *; }
-keep class my.noveldokusha.scraper.SourceInterface { *; }
-keep class my.noveldokusha.scraper.SourceInterface$* { *; }
-keep class my.noveldokusha.scraper.domain.** { *; }

################################################################################
# Networking interceptors — keep the Cloudflare bypass interceptor and its
# collaborators. The WebView-based bypass creates WebViews by class name and
# invokes CookieManager methods reflectively in some OEM ROMs.
################################################################################
-keep class my.noveldokusha.network.interceptors.** { *; }
-keep class my.noveldokusha.network.ScraperNetworkClient { *; }
-keep class my.noveldokusha.network.NetworkClient { *; }
-keep class my.noveldokusha.network.ScraperCookieJar { *; }

################################################################################
# Lua engine (luaj-jse) — keep all luaj classes. The Lua interpreter uses
# reflection and ServiceLoader to register its ScriptEngineFactory, and R8
# strips these if not explicitly kept.
#
# javax.script.* and org.apache.bcel.* are NOT available on Android — luaj
# references them for its JSR-223 ScriptEngine integration, but we use luaj
# directly via JsePlatform (not through javax.script). The -dontwarn rules
# suppress R8's "missing class" errors for these unused APIs.
################################################################################
-keep class org.luaj.** { *; }
-keep class my.noveldokusha.lua_engine.** { *; }
-keep class my.noveldokusha.scraper.sources.LuaSourceAdapter { *; }
-keep class my.noveldokusha.scraper.sources.RemoteSourceLoader { *; }
-keep class my.noveldokusha.scraper.sources.Wtrlab { *; }
-dontwarn javax.script.**
-dontwarn org.apache.bcel.**

################################################################################
# Cloudflare / WebView cookie bridge — CookieManager is accessed at runtime.
################################################################################
-keep class android.webkit.CookieManager { *; }
-keepclassmembers class android.webkit.WebView {
    public *;
}

################################################################################
# Custom exceptions — keep message constructors so error reporting works.
################################################################################
-keep class my.noveldokusha.core.domain.*Exception { *; }




#### Kotlin Serialization
# Keep `Companion` object fields of serializable classes.
# This avoids serializer lookup through `getDeclaredClasses` as done for named companion objects.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Keep `serializer()` on companion objects (both default and named) of serializable classes.
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep `INSTANCE.serializer()` of serializable objects.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# @Serializable and @Polymorphic are used at runtime for polymorphic serialization.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

-dontwarn org.slf4j.impl.StaticLoggerBinder

-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int d(...);
    public static int v(...);
    public static int i(...);
}

-assumenosideeffects class timber.log.Timber* {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}