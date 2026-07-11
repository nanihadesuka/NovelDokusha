# ParasDokusha

> Android web novel reader — focused on simplicity and reading immersion.
> Search a large catalog, open your pick, and just enjoy.

**Current version**: 3.0.0 (versionCode 30)
**Application ID**: `com.paras.noveldokusha`
**Display name**: ParasDokusha
**Min Android**: 8.0 (API 26)
**Target Android**: 14 (API 34)

> The `applicationId` (`com.paras.noveldokusha`) and the `namespace`
> (`my.noveldokusha`) intentionally differ. The applicationId is the app's
> identity on the device and on the Play Store; the namespace is the
> internal package for generated `R` and `BuildConfig` classes. Don't
> change either — see [BUILD.md §7](BUILD.md#7-precautions-and-gotchas).

## Features

- **28 built-in sources** across 3 languages + 1 local EPUB source
- **24+ external Lua sources** loaded from [HnDK0/external-sources](https://github.com/HnDK0/external-sources) at runtime (English, Chinese, MTL)
- **6 auto-translated Chinese sources** (zh sources wrapped with Google Translate API, shown as "SourceName (Translate)" under MTL)
- **2 databases** for cross-source novel search (Novel Updates, Baka-Updates)
- **WTR-LAB source** — native Kotlin implementation with local AES-256-GCM decryption and AI-translated English text
- **Local source** — read EPUB files from device storage via SAF
- **Backup & restore** — full library/database export and import
- **Light, dark, grey, black, and launcher themes** — Material 3
- **Reader**
  - Infinite vertical scroll across chapters
  - Custom font family, font size, selectable text, keep-screen-on, full-screen
  - Live translation with **4 cloud providers** (no MLKit — faster, smaller APK):
    - Google Translate Enhanced (auto-fetches API key, HTML-chunk translation)
    - Google Translate Free (no key needed)
    - Google Gemini (BLOCK_NONE safety, 5 prompt presets, per-novel prompts)
    - OpenAI-compatible (OpenAI, OpenRouter, Mistral, DeepSeek, Ollama — multi-key rotation)
  - **Dynamic CPS calibration** — EMA-smoothed reading time estimation per chapter
  - **ChapterTranslation DB caching** — translations cached in Room, survive app restarts
  - Text-to-speech with background playback, voice/pitch/speed control,
    saved voice presets, media-style notification with play/pause/skip
- **Multi-tier Cloudflare evasion** — browser-headers interceptor + headless
  WebView with Turnstile-click, force-re-navigation, and IP-block fast-fail
- **TimoTxt translation pipeline** — three TimoTxt sources share the same
  path structure on `timotxt.com`:
  - `TimoTxt` — raw Chinese
  - `TimoTxtTranslate` — Chinese → English via Google Translate free API
  - `TimoTxtGemini` — Chinese → English via Google Gemini AI
  - "Open in WebView" opens the translate.goog proxy URL so the browser
    shows the JS-translated page

## Build

See [BUILD.md](BUILD.md) for the complete build guide. Quick start:

```bash
# Prerequisites
export JAVA_HOME=$HOME/jdk17        # JDK 17 (not 21!)
export ANDROID_HOME=$HOME/android-sdk
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/build-tools/34.0.0:$PATH

# Create local.properties with keystore info (see BUILD.md §2.3)

# Build release APK (FOSS flavor — both flavors now use cloud translation)
./gradlew assembleFossRelease --no-daemon --no-configuration-cache --console=plain

# The unsigned APK appears at:
# app/build/outputs/apk/foss/release/ParasDokusha_v3.0.0-foss-release-unsigned.apk
```

### Build flavors

| Flavor | Translation backend | Notes |
|--------|--------------------|-------|
| `full` | 4 cloud providers (Google PA, Google Free, Gemini, OpenAI) | Same as FOSS now — MLKit removed |
| `foss` | 4 cloud providers (Google PA, Google Free, Gemini, OpenAI) | **Recommended.** No proprietary dependencies |

> **v3.0.0 change**: MLKit has been **removed**. Both flavors now use the
> same cloud-based translation providers. The `full` flavor is kept for
> backward compatibility but is functionally identical to `foss`.

### Signing

**Critical**: The release APK must be signed with **both v1 (JAR) and v2
(APK Signature Scheme v2)** schemes. See [BUILD.md §4](BUILD.md#4-signing-the-apk-v1--v2).

### CI / GitHub Actions

Five workflows live under `.github/workflows/`:

| Workflow | Trigger | Purpose |
|----------|---------|---------|
| `debug_foss.yml` | push, PR, manual | Debug FOSS APK, `versionCode = 1000 + commit-count` (smoke test) |
| `debug_full.yml` | manual | Debug Full APK |
| `release_foss.yml` | manual | Builds, **v1+v2 signs**, verifies, publishes FOSS release |
| `release_full.yml` | manual | Builds, **v1+v2 signs**, verifies, publishes Full release |
| `release_self_signed.yml` | manual | Builds **both** flavors, auto-generates keystore, no secrets needed |

## Sources

See [docs/SOURCES.md](docs/SOURCES.md) for the full list of supported
sources, their URLs, and the Cloudflare bypass architecture.

### Built-in English sources (22)
Light Novel Translations, Read Novel Full, Royal Road, Novel Updates,
Reddit, AT, Wuxia, Novel Fire, Novel Phoenix, Novel Cool, Lnori, Wuxia
Box, Sousetsuka, Box Novel, NovelHall, Wuxia World, Saikai, Light Novel
World, Meio Novel, More Novel, NovelKu, Wb Novel

### Built-in Indonesian sources (3)
Indonesia Webnovel, Baca Lightnovel, Sakura Novel

### Built-in Chinese sources (3)
TimoTxt (raw Chinese), TimoTxt (Translated via Google Translate API),
TimoTxt (Gemini AI translation)

### Built-in MTL source (1)
WTR-LAB (AI-translated Chinese novels with local AES-256-GCM decryption)

### External Lua sources (loaded from HnDK0/external-sources at runtime)
- **English (HnDK0)**: AllNovel, FreeWebNovel, NoBadNovel, Novel Arrow, NovelBuddy, NovelFire, NovelFull, NovelHall, NovelNice, Novel Phoenix, ReadNovelFull, Royal Road, ScribbleHub, WuxiaWorld.site
- **Chinese (HnDK0)**: Novel543, PiaoTia, Quanben5, 69shuba, TTKan, TWKan
- **Chinese (Translate)**: Same 6 zh sources auto-translated via Google Translate API
- **MTL (HnDK0)**: Sonic MTL (wtrlab excluded — native Kotlin implementation used instead)

### Local source
Read local EPUB files from device storage.

## Screenshots

| Library | Finder |
|:---:|:---:|
| ![](screenshots/library.png) | ![](screenshots/finder.png) |
| **Book info** | **Book chapters** |
| ![](screenshots/book_info.png) | ![](screenshots/book_chapers.png) |
| **Reader** | **Database search** |
| ![](screenshots/reader.png) | ![](screenshots/database_search.png) |
| **Global search** | |
| ![](screenshots/global_search.png) | |

## Tech stack

- Kotlin 1.9.23, AGP 8.2.2, Gradle 8.2 (wrapper included)
- Jetpack Compose + XML views (hybrid migration)
- Material 3 (v1.2.1)
- Coroutines + LiveData
- Room v10 (SQLite) for storage — includes ChapterTranslation entity for translation caching
- Jsoup (HTML parsing)
- OkHttp 5 (networking) with custom interceptor chain
- Coil + Glide (image loading)
- Gson + Moshi + kotlinx.serialization
- **4 cloud translation providers** (Google PA, Google Free, Gemini, OpenAI-compatible) — MLKit removed in v3.0.0
- LuaJ 3.0.1 (Lua interpreter for external source plugins)
- Android TTS + MediaSession for background narration
- Hilt (dependency injection)
- WorkManager for library updates + app update checker

## Documentation

| Doc | Purpose |
|-----|---------|
| [BUILD.md](BUILD.md) | Complete build guide, common errors, signing, CI |
| [docs/SOURCES.md](docs/SOURCES.md) | All supported sources + Cloudflare bypass architecture |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Module graph, data flow, source interface contracts |
| [docs/PERFORMANCE.md](docs/PERFORMANCE.md) | Performance notes, known bottlenecks, audit findings |
| [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) | Common runtime issues and fixes |
| [AGENTS.md](AGENTS.md) | Hand-off doc for AI agents working on this repo |
| [PLAN.md](PLAN.md) | Improvement plan (Phases 1-4) |
| [commit.txt](commit.txt) | v3.0.0 changelog — what changed from v2.2.9 |

## License

Copyright © 2023, [nani](https://github.com/nanihadesuka). Released under
[GPL-3](LICENSE) FOSS.
