# Build Guide

This document captures everything you need to build NovelDokusha locally,
including environment setup, common errors and their fixes, signing
precautions, and CI integration notes. It was written after the v2.2.9
build cycle and reflects the actual state of the project.

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Environment Setup](#2-environment-setup)
3. [Building the APK](#3-building-the-apk)
4. [Signing the APK (v1 + v2)](#4-signing-the-apk-v1--v2)
5. [Verifying the Build](#5-verifying-the-build)
6. [Common Build Errors and Fixes](#6-common-build-errors-and-fixes)
7. [Precautions and Gotchas](#7-precautions-and-gotchas)
8. [CI / GitHub Actions](#8-ci--github-actions)
9. [Quick Reference](#9-quick-reference)

---

## 1. Prerequisites

| Component | Required Version | Why |
|-----------|------------------|-----|
| JDK | **17** (Temurin recommended) | AGP 8.2.2 requires JDK 17. JDK 21 will fail with cryptic errors. |
| Android SDK | Platform 34 + build-tools 34.0.0 | `compileSdk = 34`, `targetSdk = 34` (set in `build-logic/.../AppConfig.kt`). |
| Gradle | 8.2 (wrapper included) | The `gradle-wrapper.properties` pins Gradle 8.2. Do not upgrade without testing — AGP 8.2.2 is not compatible with Gradle 9. |
| Kotlin | 1.9.23 (via Gradle plugin) | Pinned in `gradle/libs.versions.toml`. |
| Android `minSdk` | 26 | Set in `AppConfig.kt`. |
| RAM | 4 GB+ free | The Kotlin compiler daemon uses `-Xmx3g`. With less RAM, builds will OOM. |
| Disk | 5 GB+ free | Gradle cache + Android SDK + build outputs. |

### Why JDK 17, not 21?

AGP 8.2.x is officially built and tested against JDK 17. Running it on JDK 21
causes:

- `Unsupported class file major version 65` errors during R8 minification.
- Random Kotlin compiler daemon crashes (no error message, just exit 137).
- `java.lang.NoSuchMethodError` from AGP internal classes.

If your system has JDK 21 as default (common on Debian 13 / Ubuntu 24.04),
install Temurin 17 and point `JAVA_HOME` at it explicitly:

```bash
# Install Temurin 17 (Linux x64)
wget https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.13%2B11/OpenJDK17U-jdk_x64_linux_hotspot_17.0.13_11.tar.gz
tar -xzf OpenJDK17U-jdk_x64_linux_hotspot_17.0.13_11.tar.gz -C $HOME/jdk17 --strip-components=1
export JAVA_HOME=$HOME/jdk17
export PATH=$JAVA_HOME/bin:$PATH
java -version  # should print "17.0.13"
```

---

## 2. Environment Setup

### 2.1 Install Android SDK (command-line only)

If you don't have Android Studio, install the SDK via `cmdline-tools`:

```bash
mkdir -p $HOME/android-sdk/cmdline-tools
cd $HOME/android-sdk/cmdline-tools
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip cmdline-tools-linux-11076708_latest.zip
mv cmdline-tools latest

export ANDROID_HOME=$HOME/android-sdk
export ANDROID_SDK_ROOT=$HOME/android-sdk
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH

# Accept licenses (important — without this, components won't install)
yes | sdkmanager --licenses

# Install required components
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

### 2.2 Create the keystore

You only need to do this once. The keystore is reused for every build.

```bash
mkdir -p $HOME/keystore
keytool -genkeypair \
  -keystore $HOME/keystore/noveldokusha-release.jks \
  -storetype JKS \
  -keyalg RSA \
  -keysize 2048 \
  -validity 36500 \
  -alias noveldokusha \
  -storepass 'your-store-password' \
  -keypass 'your-key-password' \
  -dname "CN=NovelDokusha, OU=App, O=NovelDokusha, L=Local, ST=Local, C=US"
```

> **Back up this keystore file.** If you lose it, you will never be able to
> publish an update to the same applicationId on Google Play, and users who
> installed the previous version won't be able to upgrade — Android refuses
> to update an app whose signing key has changed.

### 2.3 Create `local.properties`

Create a file named `local.properties` in the project root (it is
`.gitignore`d — never commit it):

```properties
sdk.dir=/home/your-user/android-sdk

# Signing config (read by app/build.gradle.kts)
storeFile=/home/your-user/keystore/noveldokusha-release.jks
storePassword=your-store-password
keyAlias=noveldokusha
keyPassword=your-key-password
```

The `app/build.gradle.kts` file reads this via `defaultSigningConfigData`
and wires it into the release `signingConfig`.

---

## 3. Building the APK

### 3.1 Clean build (first time or after dependency changes)

```bash
cd NovelDokusha
export JAVA_HOME=$HOME/jdk17
export PATH=$JAVA_HOME/bin:$PATH
export ANDROID_HOME=$HOME/android-sdk
export ANDROID_SDK_ROOT=$HOME/android-sdk

./gradlew assembleFullRelease --no-daemon --no-configuration-cache --console=plain
```

**Expected time**: 8-12 minutes for a clean build. Subsequent incremental
builds take 1-3 minutes.

**Output**: `app/build/outputs/apk/full/release/NovelDokusha_v<version>-full-release.apk`

### 3.2 Build flavors

| Flavor | Translation backend | Build command |
|--------|---------------------|---------------|
| `full` | MLKit + Gemini AI | `./gradlew assembleFullRelease` |
| `foss` | No translation (pure FOSS) | `./gradlew assembleFossRelease` |

The `full` flavor is the default and recommended build.

### 3.3 Build flags explained

| Flag | Purpose |
|------|---------|
| `--no-daemon` | Prevents the Gradle daemon from lingering after the build. Use this in CI and when running multiple builds in sequence — the daemon eats 3+ GB RAM and is the #1 cause of OOM kills. |
| `--no-configuration-cache` | AGP 8.2 has known incompatibilities with Gradle's configuration cache. Leaving this off causes random `org.gradle.cache.CacheOpenException` failures. |
| `--console=plain` | Removes ANSI color codes and progress bar animation. Required for clean log files in CI; optional locally. |
| `--stacktrace` | Adds full stack traces to error output. Useful when debugging build failures. |

### 3.4 Common build variations

```bash
# Debug build (faster, no R8, signed with debug key)
./gradlew assembleFullDebug

# Release build (R8 minification + resource shrinking)
./gradlew assembleFullRelease

# Build all 4 variants (foss/full × debug/release)
./gradlew assemble

# Run unit tests only
./gradlew test

# Run a single source's tests
./gradlew :scraper:testRelease
```

---

## 4. Signing the APK (v1 + v2)

### 4.1 Why both v1 and v2?

This is **the most important lesson from the v2.2.x build cycle**:

- **v2 (APK Signature Scheme v2)** is the modern scheme, required for
  Android 7+ and enforced on Play Store.
- **v1 (JAR signing)** is the legacy scheme. AGP 8.2 **defaults to v2-only**
  when `minSdk >= 24`, because Android 7+ "doesn't need" v1.

**The problem**: Many Android TV devices and some phone OEM package
installers (especially older Xiaomi, HiSense, and generic Chinese TV
boxes) **still require v1 signing**. A v2-only APK can fail to install on
these devices in a way that **leaves the OS package installer service in
a broken state**, after which **all** subsequent APK installs fail — even
APKs from other developers that previously worked fine.

**The fix**: Always sign with **both v1 and v2**. This costs a few hundred
KB of extra APK size (the v1 MANIFEST.MF + .SF + .RSA files) but is
universally compatible.

### 4.2 The signing config in `app/build.gradle.kts`

```kotlin
signingConfigs {
    if (hasDefaultSigningConfigData) create("default") {
        storeFile = file(defaultSigningConfigData.getProperty("storeFile"))
        storePassword = defaultSigningConfigData.getProperty("storePassword")
        keyAlias = defaultSigningConfigData.getProperty("keyAlias")
        keyPassword = defaultSigningConfigData.getProperty("keyPassword")
        // Force-enable BOTH v1 and v2 signing.
        enableV1Signing = true
        enableV2Signing = true
        enableV3Signing = false
        enableV4Signing = false
    }
}
```

> **Note**: As of AGP 8.2, the `enableV1Signing` flag in `signingConfigs`
> is sometimes ignored by the build pipeline. To guarantee v1+v2 signing,
> **re-sign the APK manually with `apksigner` after the build** (see 4.3).

### 4.3 Manual re-sign with `apksigner` (recommended)

This is the foolproof method. It strips whatever signature Gradle produced
and re-signs with explicit v1+v2 control:

```bash
APK=app/build/outputs/apk/full/release/NovelDokusha_v2.2.9-full-release.apk
KEYSTORE=$HOME/keystore/noveldokusha-release.jks
ALIAS=noveldokusha
PASS='your-store-password'

BUILD_TOOLS=$HOME/android-sdk/build-tools/34.0.0

# 1. Strip the existing signature
cp "$APK" /tmp/unsigned.apk
(cd /tmp && zip -d unsigned.apk 'META-INF/*')

# 2. Zipalign (required before v2 signing — v2 is alignment-sensitive)
$BUILD_TOOLS/zipalign -f 4 /tmp/unsigned.apk /tmp/aligned.apk

# 3. Sign with v1 + v2
$BUILD_TOOLS/apksigner sign \
  --ks "$KEYSTORE" \
  --ks-key-alias "$ALIAS" \
  --ks-pass "pass:$PASS" \
  --key-pass "pass:$PASS" \
  --v1-signing-enabled true \
  --v2-signing-enabled true \
  --v3-signing-enabled false \
  --v4-signing-enabled false \
  --min-sdk-version 23 \
  --out /tmp/signed.apk \
  /tmp/aligned.apk

# 4. Move the signed APK back
mv /tmp/signed.apk "$APK"
rm /tmp/unsigned.apk /tmp/aligned.apk
```

### 4.4 Verifying the signature

```bash
$BUILD_TOOLS/apksigner verify --verbose --min-sdk-version 23 "$APK"
```

You should see:

```
Verifies
Verified using v1 scheme (JAR signing): true
Verified using v2 scheme (APK Signature Scheme v2): true
Verified using v3 scheme (APK Signature Scheme v3): false
...
```

> **Critical**: Always pass `--min-sdk-version 23` (or lower) when
> verifying. Without it, `apksigner` will skip v1 verification and report
> `false` for v1 even when the v1 signature is valid — because it considers
> v1 "unnecessary" when v2 is present and minSdk >= 24. This is a known
> trap that wastes hours of debugging.

You can also cross-check with `jarsigner`:

```bash
$JAVA_HOME/bin/jarsigner -verify "$APK"
# Should print: "jar verified."
```

---

## 5. Verifying the Build

After building, always check the APK metadata:

```bash
# Application ID, version code, version name
$BUILD_TOOLS/aapt dump badging "$APK" | grep -E "package:|application-label:"

# Expected output:
# package: name='com.paras.noveldokusha' versionCode='22' versionName='2.2.9' ...
# application-label:'NovelDokusha'
```

### Key fields to verify

| Field | Expected value | Where it's set |
|-------|----------------|----------------|
| `package` (applicationId) | `com.paras.noveldokusha` | `app/build.gradle.kts` line 46 |
| `versionCode` | Integer, increments every release | `app/build.gradle.kts` line 47 |
| `versionName` | `X.Y.Z` string | `app/build.gradle.kts` line 48 |
| v1 signature | `true` | apksigner verify output |
| v2 signature | `true` | apksigner verify output |
| minSdk | 26 | `build-logic/.../AppConfig.kt` |
| targetSdk | 34 | `build-logic/.../AppConfig.kt` |

---

## 6. Common Build Errors and Fixes

These are errors that actually occurred during the v2.2.x build cycle.
Each one is documented with the root cause and fix.

### 6.1 `Unresolved reference: readLightNovelDomainChange_1_today`

```
e: Migrations.kt:22:34 Unresolved reference: readLightNovelDomainChange_1_today
```

**Root cause**: Extension functions defined in
`migrations/Migration_readLightNovelDomain.kt` (package
`my.noveldokusha.feature.local_database.migrations`) are referenced from
`Migrations.kt` (package `my.noveldokusha.feature.local_database`). Kotlin
requires explicit imports for extension functions across packages.

**Fix**: Add explicit imports at the top of `Migrations.kt`:

```kotlin
import my.noveldokusha.feature.local_database.migrations._1stKissNovelDomainChange_1_org
import my.noveldokusha.feature.local_database.migrations.readLightNovelDomainChange_1_today
import my.noveldokusha.feature.local_database.migrations.readLightNovelDomainChange_2_meme
import my.noveldokusha.feature.local_database.migrations.wuxiaDomainChange_blog_to_click
```

### 6.2 `Expecting an element` (KSP parser error)

```
e: TimoTxt.kt:149:17 Expecting an element
```

**Root cause**: A multi-line `!=` expression where `!= null` starts a new
line confuses the KSP parser:

```kotlin
// BROKEN — parser fails on the line break before `!=`
val hasNextPage = doc.selectFirst("a.next, ...")
    != null || doc.select(".pagination li, ...").let { ... }
```

**Fix**: Wrap the `!= null` check on the same line as the call, and
parenthesize each side of `||`:

```kotlin
val hasNextPage = (doc.selectFirst("a.next, ...") != null) ||
    doc.select(".pagination li, ...").let { ... }
```

### 6.3 `A 'return' expression required in a function with a block body`

```
e: NovelCool.kt:227:5 A 'return' expression required in a function with a block body ('{...}')
```

**Root cause**: A function declared with `= { ... }` body that has
multi-line control flow where the trailing expression is ambiguous.

**Fix**: Add an explicit `return` keyword:

```kotlin
// Before (ambiguous)
val isLast = ...
PagedList(
    list = books,
    index = index,
    isLastPage = isLast || books.isEmpty()
)

// After (explicit)
val isLast = ...
return PagedList(
    list = books,
    index = index,
    isLastPage = isLast || books.isEmpty()
)
```

### 6.4 `Unresolved reference: MenuAnchorType`

```
e: SettingsGemini.kt:18:35 Unresolved reference: MenuAnchorType
e: SettingsGemini.kt:127:41 Too many arguments for public abstract fun Modifier.menuAnchor(): Modifier
```

**Root cause**: `MenuAnchorType` was added in Material3 v1.3.0, but the
project pins Material3 v1.2.1. The `menuAnchor(MenuAnchorType.PrimaryNotEditable)`
overload doesn't exist in v1.2.1.

**Fix**: Remove the import and use the no-argument overload:

```kotlin
// Before
import androidx.compose.material3.MenuAnchorType
// ...
modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)

// After
// (remove the import)
modifier = Modifier.menuAnchor()
```

### 6.5 `RemoveWorkManagerInitializer` lint error

```
AndroidManifest.xml:11: Error: Remove androidx.work.WorkManagerInitializer
from your AndroidManifest.xml when using on-demand initialization.
```

**Root cause**: The `App` class implements
`androidx.work.Configuration.Provider`, which means WorkManager should be
initialized on-demand by the app, not by the default
`androidx.startup.InitializationProvider`. The lint check enforces this.

**Fix**: Add a `tools:node="remove"` provider block to `AndroidManifest.xml`:

```xml
<manifest xmlns:android="..."
          xmlns:tools="http://schemas.android.com/tools">
    <application ...>
        <provider
            android:name="androidx.startup.InitializationProvider"
            android:authorities="${applicationId}.androidx-startup"
            android:exported="false"
            tools:node="merge">
            <meta-data
                android:name="androidx.work.WorkManagerInitializer"
                android:value="androidx.startup"
                tools:node="remove" />
        </provider>
    </application>
</manifest>
```

### 6.6 Build hangs / "context deadline exceeded"

**Symptoms**: The build runs for 10+ minutes with no output, then times
out. Process list shows a Gradle daemon at 99% CPU.

**Root cause**: The Kotlin compiler daemon (`KotlinCompileDaemon`) gets
stuck. Common triggers:
- Out of memory (the daemon uses `-Xmx3g`; if the system has <4 GB free,
  it thrashes).
- Stale daemon from a previous interrupted build.
- JDK version mismatch (running on JDK 21 instead of 17).

**Fix**:

```bash
# Kill all stale Gradle/Kotlin processes
pkill -9 -f "gradle"
pkill -9 -f "KotlinCompileDaemon"
pkill -9 -f "GradleDaemon"
sleep 3

# Verify JAVA_HOME points to JDK 17
echo $JAVA_HOME
java -version

# Re-run the build with --no-daemon
./gradlew assembleFullRelease --no-daemon --no-configuration-cache --console=plain
```

### 6.7 R8 minification step takes 5+ minutes

**Symptoms**: Build log shows `> Task :app:minifyFullReleaseWithR8` and
appears stuck for several minutes.

**Root cause**: This is normal. R8 performs whole-program optimization
across all 29 modules + dependencies. On a 2-core CI runner, it can take
5-8 minutes. On a 4+ core machine, 2-4 minutes.

**Fix**: Don't interrupt. Wait it out. If you need faster builds during
development, use the debug variant (`assembleFullDebug`), which skips R8.

### 6.8 `apksigner` reports v1 = false even after manual signing

**Symptoms**: You ran `apksigner sign --v1-signing-enabled true ...` but
verification still shows `Verified using v1 scheme (JAR signing): false`.

**Root cause**: By default, `apksigner verify` skips v1 verification when
the APK's `minSdk >= 24` and v2 is present — it considers v1 redundant.

**Fix**: Pass `--min-sdk-version 23` to the verify command:

```bash
apksigner verify --verbose --min-sdk-version 23 "$APK"
```

You can also confirm v1 is present by inspecting the APK:

```bash
unzip -l "$APK" | grep META-INF
# Should show: MANIFEST.MF, CERT.SF (or NOVELDOK.SF), CERT.RSA (or NOVELDOK.RSA)
```

---

## 7. Precautions and Gotchas

### 7.1 Never change the applicationId after publishing

**⚠️ This rule has been violated multiple times by automated edits and
environment resets. Read it twice.**

The `applicationId` (`com.paras.noveldokusha`) is the app's identity on
the device and on Google Play. Changing it makes Android treat the new
build as a completely different app — users can't upgrade, and data from
the old app is orphaned.

The original upstream project used `applicationId = "my.noveldokusha"`.
The v2.2.x release line is published under `com.paras.noveldokusha`.
**Do not revert to `my.noveldokusha`**, even if:

- A tool / IDE / formatter offers to "fix" it back to match the `namespace`.
- An environment reset wipes your working tree and the file reverts to
  upstream's `my.noveldokusha`.
- You're rebasing / merging from upstream and the merge brings back
  `my.noveldokusha`.

The correct value is **always**:
```kotlin
applicationId = "com.paras.noveldokusha"
```

The `namespace` (used internally for `R` and `BuildConfig` generation)
remains `my.noveldokusha` — these are two different fields and the
mismatch is intentional. See §7.4.

Both release workflows (`release_foss.yml`, `release_full.yml`) include
a **fail-fast check** that aborts the build if `applicationId` is not
`com.paras.noveldokusha`. This is your safety net — but you should still
verify it locally before pushing, because the fail-fast wastes ~1 minute
of CI time per failure.

If you must change `applicationId` (e.g., rebranding), you have two options:
1. **Migration build**: Ship a final version of the old app that exports
   its database to a known location, then have the new app import it on
   first launch.
2. **Fresh start**: Just publish the new applicationId and accept that
   users lose their library.

### 7.2 Never lose the keystore

The keystore file (`noveldokusha-release.jks`) is irreplaceable. If you
lose it:
- You cannot publish updates to the same applicationId on Google Play.
- Users who installed a previous version cannot upgrade — Android refuses
  to install an APK signed with a different key.
- The only recovery is Google Play's "App Signing" service, which lets
  you reset the key (but only if you enrolled in Play App Signing before
  losing the key, and only for Play Store distribution — sideloaded users
  are out of luck).

**Back up the keystore to at least two locations** (e.g., a password
manager attachment + an encrypted USB drive + a cloud storage bucket).

### 7.2.1 Never ship a release APK without v1 (JAR) signing

**⚠️ This rule was violated once. It broke users' package installers on
Android TV and Android phones. Read it twice.**

A release APK **must** be signed with **both** v1 (JAR) and v2 (APK
Signature Scheme v2). v2-only APKs install fine on stock Pixel Android
but **break the package installer on many TV / OEM Android forks**:

- Android TV (multiple vendors)
- MIUI (Xiaomi)
- EMUI (Huawei)
- Some AOSP-derived TV box firmware

When a v2-only APK is sideloaded on these devices, the package installer
may report "parsing error" / "corrupt APK" / "App not installed" — and
worse, in some firmware versions, the installer itself gets into a bad
state and **fails to install other APKs afterward**, requiring a device
reboot or factory reset to recover. This is the "package installer
breakage" reported by users after the v2.2.8 release.

The root cause: **AGP 8.2 skips v1 signing by default when `minSdk >= 24`**,
producing v2-only APKs. The `enableV1Signing = true` flag in
`app/build.gradle.kts` does NOT override this — AGP 8.2 ignores it.

The fix: **always re-sign release APKs manually with `apksigner`**:

```bash
apksigner sign \
  --ks <keystore> --ks-key-alias <alias> \
  --ks-pass pass:<pass> --key-pass pass:<pass> \
  --v1-signing-enabled true \
  --v2-signing-enabled true \
  --v3-signing-enabled false \
  --v4-signing-enabled false \
  --min-sdk-version 23 \
  --out <signed.apk> <aligned.apk>
```

Then verify with:
```bash
apksigner verify --verbose --min-sdk-version 23 <apk>
# MUST show:
#   Verified using v1 scheme (JAR signing): true
#   Verified using v2 scheme (APK Signature Scheme v2): true
```

(The `--min-sdk-version 23` flag on `verify` is critical — without it,
`apksigner` skips v1 verification when v2 is present and `minSdk >= 24`,
producing a false-negative `v1 = false`.)

Both release workflows in this repo (`release_foss.yml`, `release_full.yml`)
do the manual re-sign automatically and **fail-fast** if v1 signing is
missing. See §8.3.

**Never** ship a release APK that you haven't verified has v1 signing.
This is the single highest-risk step in the release process.

### 7.3 Never commit `local.properties`

`local.properties` contains the keystore password. It is in `.gitignore`
for a reason. If you accidentally commit it:
1. Rotate the keystore password immediately (`keytool -storepasswd`).
2. Remove the file from git history (`git filter-repo` or BFG Repo-Cleaner).
3. Force-push the cleaned history.

### 7.4 The `namespace` vs `applicationId` distinction

| Field | Value | Where | Purpose |
|-------|-------|-------|---------|
| `applicationId` | `com.paras.noveldokusha` | `app/build.gradle.kts` | The app's identity on the device and Play Store. |
| `namespace` | `my.noveldokusha` | `app/build.gradle.kts` | Internal package for generated `R` and `BuildConfig` classes. |

These can differ (and in this project, they do). The `namespace` is a
build-time artifact and never visible to users. Don't change `namespace`
without a strong reason — it would require renaming every `import
my.noveldokusha.R` across the codebase.

### 7.5 The `localPropertiesFilePath` trick

`app/build.gradle.kts` supports a `localPropertiesFilePath` project
property:

```bash
./gradlew assembleFullRelease -PlocalPropertiesFilePath=ci.properties
```

This lets CI use a different properties file than local development
(useful for injecting secrets without touching the default
`local.properties`). The CI file follows the same format.

### 7.6 Build cache invalidation

If you see bizarre errors that don't make sense (e.g., "method not found"
on a class that definitely has that method), the Gradle build cache may
be stale. Nuke it:

```bash
./gradlew clean
rm -rf ~/.gradle/caches/transforms-3/
rm -rf ~/.gradle/caches/8.2/
```

Then re-run the build.

### 7.7 Don't upgrade Kotlin / AGP / Gradle without testing

The project is pinned to:
- Kotlin 1.9.23
- AGP 8.2.2
- Gradle 8.2

Upgrading any of these will trigger cascading breakage:
- Kotlin 2.x changes the K2 compiler frontend, which breaks KSP plugins.
- AGP 8.3+ requires Gradle 8.4+, which changes task graph semantics.
- Gradle 9.0 removes deprecated APIs that AGP 8.2 uses.

If you must upgrade, do it one minor version at a time and run the full
test suite after each step.

### 7.8 Cloudflare-related sources may need rework

The TimoTxt sources use `translate.goog` as a Cloudflare-bypassing proxy.
If `translate.goog` changes its URL scheme, the sources will break. The
relevant code is in:
- `scraper/.../sources/TimoTxtTranslate.kt`
- `scraper/.../sources/TimoTxtGemini.kt`
- `scraper/.../sources/TimoTxt.kt` (base)

Look for the `toTranslateUrl()` function and the
`_x_tr_sl=zh-CN&_x_tr_tl=en&_x_tr_hl=en&_x_tr_pto=wapp` query params.

---

## 8. CI / GitHub Actions

The project ships **four workflows**, one per flavor × build-type combination.
This is intentional — it keeps each workflow small, makes failures easy to
attribute, and lets you trigger only the build you actually need.

| Workflow | File | Trigger | Outputs |
|----------|------|---------|---------|
| Debug FOSS | `.github/workflows/debug_foss.yml` | push, PR, manual | Debug FOSS APK artifact (versionCode = 1000 + commit count) |
| Debug Full | `.github/workflows/debug_full.yml` | manual only | Debug Full APK artifact |
| Release FOSS | `.github/workflows/release_foss.yml` | manual only | Signed release FOSS APK + GitHub Release (`vX.Y.Z-foss` tag) |
| Release Full | `.github/workflows/release_full.yml` | manual only | Signed release Full APK + GitHub Release (`vX.Y.Z-full` tag) |

### 8.1 Why four workflows?

- **Debug FOSS auto-runs on every commit** so broken merges are caught
  immediately. It bumps `versionCode` to `1000 + commit-count` so each
  commit produces a unique APK that can be sideloaded over the previous
  one without "package conflict" errors.
- **Debug Full is manual** because it pulls in the Gemini / Google
  Translate codepaths and takes longer. Trigger it only when you actually
  need to test translator behavior.
- **Release FOSS / Full are manual** because release builds require the
  signing keystore (which lives in GitHub Secrets) and create a public
  GitHub Release.

### 8.2 Required GitHub Secrets (for release signing)

Set these in your repo's Settings → Secrets and variables → Actions:

| Secret | Value |
|--------|-------|
| `SIGNING_KEY` | Base64-encoded keystore file: `base64 -w0 noveldokusha-release.jks` |
| `KEY_STORE_PASSWORD` | Keystore password |
| `ALIAS` | Key alias (e.g., `noveldokusha`) |
| `KEY_PASSWORD` | Key password |

If any of these are missing, the release workflow will fail-fast with a
clear error message at the "Setup APK signing keystore" step.

### 8.3 Fail-fast verifications built into every release workflow

Every release workflow runs three verification steps **after** signing.
If any of them fail, the workflow aborts and **no GitHub Release is
created**. This is what prevents broken APKs from reaching users.

1. **v1 (JAR) signing present** — `apksigner verify --verbose
   --min-sdk-version 23` must show `Verified using v1 scheme (JAR
   signing): true`. If this fails, the workflow aborts with an error
   explaining that TV/OEM package installers will break.
2. **v2 (APK Signature Scheme v2) signing present** — same verify command
   must show `Verified using v2 scheme (APK Signature Scheme v2): true`.
3. **applicationId == `com.paras.noveldokusha`** — `aapt dump badging`
   must show `package: name='com.paras.noveldokusha'`. If this fails,
   the workflow aborts with an error explaining that the applicationId
   has been reverted.

There is also a **pre-build** fail-fast check that greps
`app/build.gradle.kts` for `applicationId = "com.paras.noveldokusha"`
before any build step runs — so you don't waste 20 minutes of CI time
building an APK that will be rejected anyway.

### 8.4 The v1+v2 re-sign step

The release workflows run the Gradle build, then **re-sign the APK with
`apksigner`** to guarantee v1+v2 signing. The Gradle `enableV1Signing`
flag is unreliable in AGP 8.2 (it silently produces v2-only APKs when
`minSdk >= 24`), so the manual re-sign is mandatory.

The exact commands are in the workflow files (`.github/workflows/release_*.yml`,
the "Re-sign APK with v1 + v2 signature" step). See §4.3 of this document
for the equivalent local commands.

### 8.5 The "Could not find or load main class GradleWrapperMain" CI error

If CI fails with:

```
Error: Could not find or load main class org.gradle.wrapper.GradleWrapperMain
Caused by: java.lang.ClassNotFoundException: org.gradle.wrapper.GradleWrapperMain
```

…then `gradle/wrapper/gradle-wrapper.jar` is missing or empty on the
runner. Causes and fixes:

1. **The jar was accidentally gitignored or removed.** Verify locally:
   ```bash
   git ls-files gradle/wrapper/gradle-wrapper.jar
   # Must print: gradle/wrapper/gradle-wrapper.jar
   ```
   If it prints nothing, the jar isn't tracked — re-add it:
   ```bash
   git add gradle/wrapper/gradle-wrapper.jar
   git commit -m "Re-add gradle-wrapper.jar"
   git push
   ```

2. **The jar was committed as a text file and got mangled by line-ending
   conversion.** Add a `.gitattributes` entry:
   ```
   gradle/wrapper/gradle-wrapper.jar binary
   ```

3. **The workflow runs `./gradlew` from the wrong directory.** All four
   workflows in this project use `actions/checkout@v4` followed by
   `chmod +x ./gradlew` and a `test -s gradle/wrapper/gradle-wrapper.jar`
   guard, so they fail with a clear error message before invoking
   `./gradlew` if the jar is missing.

All four workflows in this repo include the guard:
```yaml
- name: Verify gradle wrapper is present
  run: |
    chmod +x ./gradlew
    if [ ! -s gradle/wrapper/gradle-wrapper.jar ]; then
      echo "::error::gradle/wrapper/gradle-wrapper.jar is missing or empty."
      exit 1
    fi
```

### 8.6 CI runner requirements

- **OS**: Ubuntu 22.04 or later (`ubuntu-latest` works).
- **RAM**: 7 GB+ (GitHub-hosted runners have 7 GB, which is enough but
  tight — the build will OOM on smaller runners).
- **Disk**: 10 GB+ free (Android SDK + Gradle cache + build outputs).

### 8.7 CI cache strategy

Each workflow caches `~/.gradle/caches` and `~/.gradle/wrapper` under a
workflow-specific key (e.g. `gradle-release-full-...`). The key includes
the hash of `**/*.gradle*`, `**/gradle-wrapper.properties`, and
`**/libs.versions.toml`, so changing any of those invalidates the cache
automatically.

The `actions/setup-java` action also caches Gradle dependencies under
its own key.

Don't manually clear the cache unless you see stale-cache errors (e.g.,
"method not found" on a class that definitely has that method).

---

## 9. Quick Reference

### One-shot build script

Save this as `build.sh` in the project root:

```bash
#!/bin/bash
set -e

export JAVA_HOME=${JAVA_HOME:-$HOME/jdk17}
export PATH=$JAVA_HOME/bin:$PATH
export ANDROID_HOME=${ANDROID_HOME:-$HOME/android-sdk}
export ANDROID_SDK_ROOT=$ANDROID_HOME

# Build
./gradlew assembleFullRelease --no-daemon --no-configuration-cache --console=plain

# Re-sign with v1+v2
APK=$(ls app/build/outputs/apk/full/release/*.apk | head -1)
KEYSTORE=${KEYSTORE:-$HOME/keystore/noveldokusha-release.jks}
ALIAS=${ALIAS:-noveldokusha}
PASS=${KEY_PASS:-password}
BUILD_TOOLS=$ANDROID_HOME/build-tools/34.0.0

cp "$APK" /tmp/unsigned.apk
(cd /tmp && zip -d unsigned.apk 'META-INF/*' > /dev/null)
$BUILD_TOOLS/zipalign -f 4 /tmp/unsigned.apk /tmp/aligned.apk
$BUILD_TOOLS/apksigner sign \
  --ks "$KEYSTORE" --ks-key-alias "$ALIAS" \
  --ks-pass "pass:$PASS" --key-pass "pass:$PASS" \
  --v1-signing-enabled true --v2-signing-enabled true \
  --v3-signing-enabled false --v4-signing-enabled false \
  --min-sdk-version 23 \
  --out "$APK" /tmp/aligned.apk
rm /tmp/unsigned.apk /tmp/aligned.apk

# Verify
$BUILD_TOOLS/apksigner verify --verbose --min-sdk-version 23 "$APK" | head -5
$BUILD_TOOLS/aapt dump badging "$APK" | grep -E "package:|versionName"

echo ""
echo "Built: $APK"
```

### File locations cheat sheet

| What | Where |
|------|-------|
| Application ID | `app/build.gradle.kts` → `applicationId` |
| Version | `app/build.gradle.kts` → `versionCode` / `versionName` |
| SDK versions | `build-logic/convention/.../AppConfig.kt` |
| Signing config | `app/build.gradle.kts` → `signingConfigs` block |
| Keystore path | `local.properties` → `storeFile` |
| Source registry | `scraper/.../Scraper.kt` → `sourcesList` |
| Cloudflare bypass | `networking/.../interceptors/CloudfareVerificationInterceptor.kt` |
| TimoTxt translate proxy | `scraper/.../sources/TimoTxtTranslate.kt` → `toTranslateUrl()` |
| WorkManager init removal | `app/src/main/AndroidManifest.xml` → `<provider>` block |
| ProGuard keep rules | `app/proguard-rules.pro` |

### Version bump checklist

When releasing a new version:

1. [ ] Increment `versionCode` in `app/build.gradle.kts`
2. [ ] Update `versionName` in `app/build.gradle.kts`
3. [ ] Run `./gradlew assembleFullRelease`
4. [ ] Re-sign with `apksigner` (v1 + v2)
5. [ ] Verify with `apksigner verify --verbose --min-sdk-version 23`
6. [ ] Verify `applicationId` with `aapt dump badging`
7. [ ] Test install on a real device (especially Android TV)
8. [ ] Update `docs/SOURCES.md` if sources changed
9. [ ] Update `README.md` feature list if applicable
10. [ ] Tag the git commit: `git tag v2.2.9 && git push --tags`
