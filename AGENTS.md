# AGENTS.md — Hand-off Guide for AI Agents

> This file is for AI agents (Claude, GPT, Gemini, local LLMs, etc.)
> working on the ParasDokusha codebase. It captures the non-obvious
> context that a human maintainer would know but an agent starting cold
> would not. Read this BEFORE making any changes.

## TL;DR

- **App**: Android web novel reader (Kotlin, Compose + XML hybrid, Hilt DI, Room v10, OkHttp, 30+ Gradle modules)
- **Version**: 3.0.1 (versionCode 31)
- **Application ID**: `com.paras.noveldokusha` (NEVER change — breaks updates)
- **Namespace**: `my.noveldokusha` (different from applicationId — intentional)
- **Display name**: ParasDokusha
- **Build**: JDK 17 (not 21!), AGP 8.2.2, Gradle 8.2, Kotlin 1.9.23 — pinned, don't upgrade without testing
- **Signing**: Release APKs MUST be re-signed with `apksigner` (v1+v2); AGP 8.2 silently drops v1
- **Translation**: 4 cloud providers (Google PA, Google Free, Gemini, OpenAI) — MLKit REMOVED in v3.0.0
- **Lua engine**: luaj-jse 3.0.1 for external source plugins from HnDK0/external-sources
- **Sources**: 28 built-in Kotlin sources + 24+ external Lua sources + 6 auto-translated zh sources + Wtrlab

## MANDATORY: Ponytail Workflow

**EVERY code change in this project MUST go through the ponytail skill first.**
No exceptions. This is non-negotiable.

### What this means:
1. **Before writing ANY code**: Read `~/.claude/skills/ponytail/SKILL.md`
2. **Follow the ladder**: Does it need to exist? → Already in codebase? → Stdlib? → Native? → Already-installed dep? → One line? → Minimum code
3. **Bug fix = root cause**: Grep every caller, fix at the shared function, not the symptom path
4. **No unrequested abstractions**: No interface with one impl, no factory for one product, no config for a value that never changes
5. **Deletion over addition**: Boring over clever. Fewest files possible. Shortest working diff wins.
6. **Mark simplifications**: `// ponytail: <ceiling>, <upgrade path>`

### Ponytail-audit before any dependency removal:
When auditing for unused dependencies, check BOTH:
- **Kotlin imports** (`grep -rn "com.google.android.material" src/`)
- **XML resource references** (`grep -rn "MaterialComponents\|colorOnPrimary\|colorSurface" src/main/res/`)
- **R.attr references** in Kotlin (`grep -rn "R\.attr\.color" src/`)

A dependency can be used via XML theme attributes even if NO Kotlin code imports it.
The `libs.material` removal bug (build failure due to missing `colorOnPrimary` etc.) was caused by
only checking Kotlin imports. **Always check XML resources too.**

### Ponytail skills installed at:
- `~/.claude/skills/ponytail/` — main skill (lazy senior dev)
- `~/.claude/skills/ponytail-audit/` — whole-repo audit for over-engineering
- `~/.claude/skills/ponytail-review/` — diff review for complexity
- `~/.claude/skills/ponytail-debt/` — harvest `ponytail:` comments into a ledger
- `~/.claude/skills/ponytail-gain/` — show measured impact scoreboard

## What's new in v3.0.0

### Phase 1: Critical Bug Fixes
- Wtrlab: `"translate":"web"` → `"translate":"ai"` (returns English instead of Chinese)
- Lua sources: `createAdapter()` now uses `parseBaseUrlFromLua()` (fixes all external sources resolving to wrong URL)
- `Response.toDocument()`: `Jsoup.parse(html)` → `Jsoup.parse(html, request.url.toString())` (fixes Wtrlab's `doc.location()` returning empty string)

### Phase 2: MLKit Removed + 4 Cloud Translation Providers
- Deleted MLKit dependency from `full` flavor — both flavors now use `translator_nop`
- Ported 4 providers from NoveLA fork:
  - `TranslationManagerGooglePA.kt` — auto-fetches API key from wtr-lab.com, HTML-chunk translation
  - `TranslationManagerGoogleFree.kt` — free Google Translate, no key needed
  - `TranslationManagerGemini.kt` — BLOCK_NONE safety, 5 prompt presets, numbered-list protocol
  - `TranslationManagerOpenAI.kt` — any OpenAI-compatible endpoint, multi-key rotation
- `TranslationManagerComposite.kt` — dispatcher routes to active provider
- `SupportedLanguages.kt` — 226 BCP-47 language codes
- `TranslationUtils.kt` — 5 prompt presets (MINIMAL, BALANCED, DETAILED, ADULT, DIRECT_ASIAN)
- New AppPreferences: `TRANSLATION_PROVIDER`, `OPENAI_API_KEY`, `OPENAI_BASE_URL`, `OPENAI_MODEL`, etc.

### Phase 3: ChapterTranslation DB Caching
- New Room entity `ChapterTranslation` (version 10, migration 9→10)
- Stores translated paragraphs as JSON array per (chapterUrl, sourceLang, targetLang)
- DAO with `getTranslations()`, `insertReplace()`, `deleteTranslationsByBookUrls()`, `getTranslatedTitlesFlow()`
- Registered in `AppDatabase.kt` and `LocalDatabaseModule.kt`

### Phase 4: CPS Calibration
- Ported EMA-smoothed CPS (characters-per-second) calibration from NoveLA
- `ReaderTextToSpeech.kt` now tracks `baseCharactersPerSecond` (starts at 13.0, dynamically calibrated)
- Exposes `chapterWordCount`, `chapterCharacterCount`, `estimatedWpm`, `estimatedTotalSeconds`, `estimatedRemainingSeconds`
- EMA formula: `0.2 * newCps + 0.8 * oldCps`, clamped to 3-40 cps

### Audit Fixes (3 CRITICAL + 5 HIGH + 10 MEDIUM)
- C3: `ReaderActivity.onDestroy()` now closes session when `isFinishing` (prevents TTS/service/scope leak)
- C2: `NarratorMediaControlsService.onCreate()` always calls `startForeground()` (prevents system crash)
- H1: LuaEngine `runBlocking` changed to `runBlocking(Dispatchers.IO)` (frees Default threads)
- H2: `ReaderSession.close()` now cancels `ReaderChaptersLoader`'s `Job` (not just children)
- H4: Gemini default model fixed to `"gemini-2.5-flash"` (was `"gemini-2.5-flash-lite"`)
- M1: AppPreferences `toFlow()` no longer leaks `CoroutineScope` per call
- M2: `preferencesChangeListeners` is now `Collections.synchronizedSet()`
- M9: Migration SQL removed `IF NOT EXISTS` (Room schema validation is authoritative)
- M10: Removed redundant `@Transaction` from DAO `@Insert` methods

## Where things live

```
NovelDokusha/
├── app/                          # Application entry, MainActivity, DI wiring, signing
├── scraper/                      # ⭐ Source registry + 30 source impls + 2 databases
│   └── src/main/java/my/noveldokusha/scraper/
│       ├── Scraper.kt            # Source registry — add new sources here
│       ├── SourceInterface.kt    # Sealed interface contract (includes transformWebviewUrl, displayName)
│       ├── sources/              # 30+ source impls + LuaSourceAdapter + LuaTranslatedSourceAdapter + Wtrlab
│       ├── sources/RemoteSourceLoader.kt  # Loads HnDK0 Lua plugins from GitHub
│       └── TextExtractor.kt      # HTML → plain text extractor
├── networking/                   # OkHttp client + interceptor chain
│   └── src/main/java/my/noveldokusha/network/
│       ├── NetworkClient.kt      # ScraperNetworkClient — the singleton client
│       ├── okhttpExtensions.kt   # Call.await() (cancellation-aware), Response.toDocument() (sets base URI)
│       ├── ScraperCookieJar.kt   # OkHttp ↔ WebView CookieManager bridge
│       └── interceptors/
│           ├── UserAgentInterceptor.kt       # Pixel 7 / Chrome 120 UA
│           ├── BrowserHeadersInterceptor.kt  # Sec-Fetch-*, Sec-CH-UA-*
│           ├── DecodeResponseInterceptor.kt  # gzip + br decompression
│           └── CloudfareVerificationInterceptor.kt  # WebView CF solver
├── tooling/
│   ├── lua_engine/               # ⭐ LuaJ interpreter + 30 API functions for external plugins
│   │   └── src/main/java/my/noveldokusha/lua_engine/LuaEngine.kt
│   ├── local_database/           # Room v10 schema, DAOs, migrations
│   │   └── .../tables/ChapterTranslation.kt  # NEW: translation caching entity
│   ├── text_translator/
│   │   ├── domain/               # TranslationManager interface + SupportedLanguages (226 codes)
│   │   └── translator_nop/       # ⭐ 4 cloud providers (Google PA, Google Free, Gemini, OpenAI)
│   │       └── .../TranslationManagerComposite.kt  # Provider dispatcher
│   └── ...                      # epub_parser, text_to_speech, backup_*, application_workers, etc.
├── core/                         # Response, PagedList, AppPreferences (with translation provider prefs)
├── coreui/                       # Compose theme, components, BaseViewModel
├── data/                         # Repositories (consume Scraper + Room)
├── features/                     # UI features (reader, chaptersList, settings, etc.)
│   └── reader/                   # ⭐ Reader Activity + session + TTS + live translation + CPS calibration
├── strings/                      # i18n + source display names
├── docs/                         # ⭐ Read these first
│   ├── ARCHITECTURE.md
│   ├── SOURCES.md
│   ├── PERFORMANCE.md
│   └── TROUBLESHOOTING.md
├── BUILD.md
├── AGENTS.md                     # ⭐ This file
├── PLAN.md                       # Improvement plan (Phases 1-4)
├── commit.txt                    # v3.0.0 changelog
└── README.md
```

## The TimoTxt traps (read if you touch TimoTxt)

Three TimoTxt sources share the same path structure on `timotxt.com` but use different stored URL hosts as routing keys. See [docs/SOURCES.md](docs/SOURCES.md) for the full `transformChapterUrl` vs `transformWebviewUrl` table.

**DO NOT conflate these two transforms.** `transformChapterUrl` is for OkHttp (returns Chinese HTML → app translates). `transformWebviewUrl` is for the browser (returns translate.goog proxy URL → JS translates in-place).

## The applicationId trap

`applicationId` = `com.paras.noveldokusha`. `namespace` = `my.noveldokusha`. **These intentionally differ.** Don't "fix" the mismatch. Both release workflows fail-fast if `applicationId != com.paras.noveldokusha`.

## The signing trap (v1 + v2)

AGP 8.2 defaults to v2-only signing when `minSdk >= 24`. v2-only APKs break TV/OEM package installers. **Always re-sign release APKs manually with `apksigner`** (v1+v2). See BUILD.md §4.

## The build environment

- **JDK**: 17 (Temurin). JDK 21 causes R8/Kotlin daemon crashes.
- **AGP**: 8.2.2. Don't upgrade — AGP 8.3+ requires Gradle 8.4+.
- **Gradle**: 8.2 (wrapper included). Don't upgrade to 9.x.
- **Kotlin**: 1.9.23. Don't upgrade to 2.x — K2 breaks KSP.
- **Material 3**: 1.2.1. Don't upgrade to 1.3+ without checking `MenuAnchorType`.

## Translation system (v3.0.0)

MLKit is **removed**. Both `full` and `foss` flavors use `translator_nop` which contains:

1. `TranslationManagerComposite` — reads `TRANSLATION_PROVIDER` pref, routes to active provider
2. `TranslationManagerGooglePA` — auto-fetches API key from wtr-lab.com (24h cache), HTML-chunk translation
3. `TranslationManagerGoogleFree` — free `translate.googleapis.com`, no key needed, cookie-seeding on 429
4. `TranslationManagerGemini` — user API key, BLOCK_NONE safety, 5 prompt presets, numbered-list protocol
5. `TranslationManagerOpenAI` — configurable baseUrl/model, multi-key rotation

Provider selection is via `AppPreferences.TRANSLATION_PROVIDER` (values: `GOOGLE_PA`, `GOOGLE_FREE`, `GEMINI`, `OPENAI`).

## Lua external sources

HnDK0's Lua plugins are loaded at runtime from `https://raw.githubusercontent.com/HnDK0/external-sources/refs/heads/main/`. The `RemoteSourceLoader` fetches `index.yaml` per language (en, zh, mtl), downloads `.lua` files, caches them locally, and creates `LuaSourceAdapter` instances.

For zh sources, a `LuaTranslatedSourceAdapter` is also created that wraps the Lua plugin and translates all text output via the Google Translate API (same engine as TimoTxtTranslate).

**wtrlab is excluded** from HnDK0's mtl sources — a native Kotlin `Wtrlab.kt` implementation is used instead (with local AES-256-GCM decryption and AI-translated mode).

## Common mistakes to avoid

1. Don't route all TimoTxt fetches through translate.goog — fetch from timotxt.com directly, use translate.goog only for webview.
2. Don't use `transformChapterUrl` for webview — use `transformWebviewUrl`.
3. Don't change `applicationId`.
4. Don't ship a release APK without v1 signing.
5. Don't upgrade Kotlin / AGP / Gradle / Material 3.
6. Don't use `GlobalScope` — use the injected `AppCoroutineScope`.
7. Don't call `response.body.string()` without closing the response on exception — use `response.use { }`.
8. Don't use `client.newCall(req).execute()` in translation providers — it blocks and doesn't propagate cancellation (known issue, will be fixed).
9. Don't forget to call `startForeground()` in `NarratorMediaControlsService.onCreate()` before any early return.
10. Don't forget to close `ReaderSession` in `ReaderActivity.onDestroy()` when `isFinishing` is true.

## When in doubt

- Read [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the big picture
- Read [docs/SOURCES.md](docs/SOURCES.md) for source-specific details
- Read [docs/PERFORMANCE.md](docs/PERFORMANCE.md) for performance pitfalls
- Read [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) for runtime issues
- Read [BUILD.md](BUILD.md) for build/sign/CI issues
- Read [PLAN.md](PLAN.md) for the improvement plan and what's done vs deferred
- Read [commit.txt](commit.txt) for the v3.0.0 changelog
