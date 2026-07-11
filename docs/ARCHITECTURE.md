# Architecture

This document describes the module structure, dependency graph, and data
flow of ParasDokusha v3.0.0. It is intended for contributors and AI agents
who need to navigate the codebase.

## Module graph

ParasDokusha is a multi-module Gradle project (31 modules). The dependency
graph below shows the **intended** direction of dependencies (A → B means
A depends on B). Cycles are forbidden.

```
                          ┌─────────────────────┐
                          │         app         │  ← entry point, MainActivity, DI wiring
                          └──────────┬──────────┘
                                     │
        ┌────────────────────────────┼─────────────────────────────┐
        │                            │                             │
        ▼                            ▼                             ▼
  ┌──────────┐               ┌──────────────┐              ┌──────────────┐
  │ features │               │    data      │              │   tooling    │
  │  (9 sub) │               │ (repos)      │              │  (12 sub)    │
  └────┬─────┘               └──────┬───────┘              └──────┬───────┘
       │                            │                             │
       │   ┌────────────────────────┼────────────────────────┐    │
       │   │                        │                        │    │
       ▼   ▼                        ▼                        ▼    ▼
  ┌────────────┐  ┌──────────┐  ┌────────┐  ┌──────────┐  ┌────────────┐
  │  scraper   │  │networking│  │  core  │  │ coreui   │  │  strings   │
  │ (sources)  │  │ (okhttp) │  │ (prefs)│  │ (theme)  │  │ (i18n)     │
  └─────┬──────┘  └────┬─────┘  └────┬───┘  └────┬─────┘  └────────────┘
        │              │             │           │
        └──────────────┴─────────────┴───────────┘
                       (all depend on core)
```

### Module list

| Module | Purpose | Key classes |
|---|---|---|
| `app` | Application entry, MainActivity, DI wiring, signing config | `App`, `MainActivity`, `AppModule`, `AppNavigationRoutes` |
| `scraper` | Source registry + 30 source impls + 2 databases | `Scraper`, `SourceInterface`, `DatabaseInterface`, `sources/*`, `databases/*` |
| `networking` | OkHttp client + interceptor chain (UA, browser headers, decode, CF bypass) + cookie jar | `ScraperNetworkClient`, `interceptors/*`, `ScraperCookieJar`, `okhttpExtensions.kt` |
| `core` | Response/PagedList/LanguageCode, AppPreferences, domain exceptions, utils | `AppPreferences`, `Response`, `PagedList`, `LanguageCode`, `AppCoroutineScope`, `CursorExtensions` |
| `coreui` | Compose theme system (6 themes), reusable components, BaseViewModel/Activity | `Theme`, `ThemeProvider`, `BaseActivity`, `BaseViewModel`, components/* |
| `data` | Repositories that consume Scraper + Room + app prefs | `DownloaderRepository`, `ChapterBodyRepository`, `LibraryBooksRepository`, `BookChaptersRepository`, `AppRepository`, `ScraperRepository` |
| `strings` | i18n + non-translatable source/database display names | `strings.xml`, `strings-no-translatable.xml` |
| `navigation` | Navigation routes interface (implemented by `app`) | `NavigationRoutes` |
| `features/reader` | Reader Activity, ViewModel, session manager, TTS, live translation | `ReaderActivity`, `ReaderViewModel`, `ReaderSession`, `ReaderManager`, `services/NarratorMediaControls*` |
| `features/chaptersList` | Chapter list screen + bottom sheet | `ChaptersActivity`, `ChaptersViewModel` |
| `features/globalSourceSearch` | Search across all sources simultaneously | `GlobalSourceSearchActivity`, `GlobalSourceSearchViewModel` |
| `features/databaseExplorer` | Database search + book info | `DatabaseSearchActivity`, `DatabaseBookInfoActivity` |
| `features/sourceExplorer` | Source catalog browse | `SourceCatalogActivity`, `SourceCatalogViewModel` |
| `features/catalogExplorer` | Language filter + catalog list | `CatalogExplorerScreen`, `CatalogExplorerViewModel` |
| `features/settings` | Settings screen + sections | `SettingsScreen`, `SettingsViewModel`, `sections/*` |
| `features/libraryExplorer` | Library browse + filter + sort | `LibraryScreen`, `LibraryViewModel`, `LibraryPageViewModel` |
| `features/webview` | In-app WebView for "open in browser" | `WebViewActivity`, `WebViewScreen` |
| `tooling/local_database` | Room schema, DAOs, migrations | `AppDatabase`, `LibraryDao`, `ChapterDao`, `ChapterBodyDao`, `migrations/*` |
| `tooling/epub_parser` | EPUB parsing for local source + importer | `Epub`, `EpubParser`, `EpubCoverParser` |
| `tooling/epub_importer` | EPUB import service | `EpubImportService` |
| `tooling/text_to_speech` | Android TTS wrapper | `TextToSpeechManager` |
| `tooling/text_translator/domain` | Translation manager interface | `TranslationManager` |
| `tooling/text_translator/translator` | Full-flavor MLKit impl | `TranslationManagerMLKit`, `FullModule` |
| `tooling/text_translator/translator_nop` | FOSS-flavor no-op impl | `TranslationManagerEmpty`, `FossModule` |
| `tooling/backup_create` | Backup export service | `BackupDataService` |
| `tooling/backup_restore` | Backup import service | `RestoreDataService` |
| `tooling/application_workers` | WorkManager workers + factory | `LibraryUpdatesWorker`, `UpdatesCheckerWorker`, `AppWorkerFactory` |
| `tooling/local_source` | LocalSource impl (reads EPUBs via SAF) | `AppLocalSources`, `LocalSourcesDirectories` |
| `tooling/algorithms` | Pure-Kotlin algorithms (text splitter) | `DelimiterAwareTextSplitter` |

## Source interface contract

All sources implement `SourceInterface` (in `scraper/.../SourceInterface.kt`).

```kotlin
sealed interface SourceInterface {
    val id: String                    // unique, e.g. "royal_road"
    val nameStrId: Int                // R.string.source_name_<id>
    val baseUrl: String               // MUST end with "/"
    val isLocalSource: Boolean
    val requiresLogin: Boolean

    // Transform URL for OkHttp fetching (in-app reader)
    suspend fun transformChapterUrl(url: String): String = url

    // Transform URL for the in-app WebView browser
    suspend fun transformWebviewUrl(url: String): String = url

    suspend fun getChapterTitle(doc: Document): String?
    suspend fun getChapterText(doc: Document): String?

    interface Base : SourceInterface              // URL-only sources (Reddit, AT, Sousetsuka)
    interface Catalog : SourceInterface {         // sources with catalog/search
        val catalogUrl: String
        val language: LanguageCode?
        val iconUrl: Any
        suspend fun getBookCoverImageUrl(bookUrl: String): Response<String?>
        suspend fun getBookDescription(bookUrl: String): Response<String?>
        suspend fun getChapterList(bookUrl: String): Response<List<ChapterResult>>
        suspend fun getCatalogList(index: Int): Response<PagedList<BookResult>>
        suspend fun getCatalogSearch(index: Int, input: String): Response<PagedList<BookResult>>
    }
    interface Configurable {                     // sources with a settings UI
        @Composable fun ScreenConfig()
    }
}
```

### `transformChapterUrl` vs `transformWebviewUrl`

These two methods serve different purposes and **must not be conflated**:

| Method | Called by | Purpose | Default |
|---|---|---|---|
| `transformChapterUrl` | `DownloaderRepository.bookChapter()` | Convert stored URL to a URL that **OkHttp can fetch** to get HTML for the in-app reader | Returns URL unchanged |
| `transformWebviewUrl` | `ReaderViewModel.transformUrlForWeb()` (called from `ReaderActivity.onOpenChapterInWeb`) | Convert stored URL to a URL that **the in-app WebView browser can render** usefully | Returns URL unchanged |

For the TimoTxt sources:

| Source | Stored URL | `transformChapterUrl` (OkHttp) | `transformWebviewUrl` (browser) |
|---|---|---|---|
| `TimoTxt` | `https://www.timotxt.com/{id}/{ch}.html` | unchanged → fetch from timotxt.com | → `https://www-timotxt-com.translate.goog/{id}/{ch}.html?_x_tr_sl=zh-CN&_x_tr_tl=en&_x_tr_hl=en&_x_tr_pto=wapp` |
| `TimoTxtTranslate` | `https://www-timotxt-com.translate.goog/{id}/{ch}.html` | → `https://www.timotxt.com/{id}/{ch}.html` (strip params) | → add `_x_tr_*` params to the stored translate.goog URL |
| `TimoTxtGemini` | `https://www-timotxt-com-gemini.goog/{id}/{ch}.html` | → `https://www.timotxt.com/{id}/{ch}.html` | → `https://www-timotxt-com.translate.goog/{id}/{ch}.html?_x_tr_*` |

The `translate.goog` proxy returns **Chinese HTML** to OkHttp (which doesn't
run JavaScript) — the app then translates the extracted text via the Google
Translate API or Gemini API. In a WebView (which DOES run JavaScript), the
proxy's translation script runs and translates the page to English
in-place, so the user sees the translated page in the browser.

## Data flow: opening a chapter

```
User taps chapter
        │
        ▼
ReaderActivity starts with bookUrl + chapterUrl
        │
        ▼
ReaderManager.initiateOrGetSession(bookUrl, chapterUrl)
        │  (creates or reuses ReaderSession)
        ▼
ReaderSession → ReaderChaptersLoader.loadChapter(chapterUrl)
        │
        ▼
ChapterBodyRepository.fetchBody(chapterUrl)
        │
        ├─ cache hit (ChapterBody table) → return cached body
        │
        └─ cache miss → DownloaderRepository.bookChapter(chapterUrl)
                │
                ├─ Scraper.getCompatibleSource(chapterUrl)
                │      (matches by URL prefix; called BEFORE redirects
                │       so translate.goog URLs route to TimoTxtTranslate,
                │       not the plain TimoTxt source)
                │
                ├─ source.transformChapterUrl(chapterUrl)
                │      (e.g. translate.goog → timotxt.com for fetching)
                │
                ├─ networkClient.get(transformedUrl)
                │      (CF interceptors fire if challenged;
                │       BrowserHeaders → Decode → CloudFareVerification)
                │
                ├─ response.toDocument()  (Jsoup.parse(body.string()))
                │
                ├─ source.getChapterTitle(doc)
                ├─ source.getChapterText(doc)
                │      (TimoTxtTranslate translates via Google Translate API;
                │       TimoTxtGemini translates via Gemini API;
                │       TimoTxt returns raw Chinese)
                │
                └─ ChapterBodyRepository.insertWithTitle(body, title)
                        (Room transaction: cache body + update chapter title)
```

## OkHttp interceptor chain

Order matters. Defined in `ScraperNetworkClient`:

1. `HttpLoggingInterceptor` (debug only) — logs full request/response bodies
2. `UserAgentInterceptor` — sets a modern Android Chrome 120 / Pixel 7 UA
3. `BrowserHeadersInterceptor` — Tier 1 CF evasion: adds `Accept`,
   `Accept-Language`, `Sec-Fetch-*`, `Sec-CH-UA-*`, `Cache-Control`
4. `DecodeResponseInterceptor` — decompresses `gzip` and `br` response bodies
5. `CloudFareVerificationInterceptor` — Tier 2/3: detects challenges via
   `cf-mitigated` header, status 202/403/429/502/503 + `Server: cloudflare*`,
   or body markers; solves via headless WebView with Turnstile-click JS,
   force-re-navigation after 5s, IP-block fast-fail on `error=1020/1015`,
   up to 3 retry attempts

`ScraperCookieJar` bridges OkHttp ↔ `android.webkit.CookieManager`, so
WebView-baked `cf_clearance` cookies flow back to OkHttp automatically.

## Room database

Schema version 9. Three entities:

| Entity | Primary key | Purpose |
|---|---|---|
| `Book` | `url` | Library books + metadata (title, cover, description, lastReadChapter, inLibrary) |
| `Chapter` | `url` | Chapter metadata (title, bookUrl, position, read, lastReadPosition, lastReadOffset) |
| `ChapterBody` | `url` | Cached chapter text body |

Migrations live in `tooling/local_database/.../migrations/`. The
`websiteDomainChangeHelper` is used when a source's domain changes (e.g.
`wuxia.blog` → `wuxia.click`) — it `REPLACE`s the old domain with the new
one across `Book.url`, `Book.coverImageUrl`, `Chapter.url`, `Chapter.bookUrl`,
and `ChapterBody.url`.

## Hilt DI graph

`App` is `@HiltAndroidApp`. The DI graph is:

- `AppModule` (in `app`) — binds `App`, `AppInternalState`, `Toasty`,
  `NavigationRoutes`
- `NetworkingModule` (in `networking`) — binds `ScraperNetworkClient` as
  `NetworkClient` singleton
- `CoreModule` (in `core`) — provides `AppPreferences`, `AppCoroutineScope`
- `CoreUIModule` (in `coreui`) — provides `ThemeProvider`
- `LocalDatabaseModule` (in `tooling:local_database`) — provides `AppDatabase`
- `LocalSourceModule` (in `tooling:local_source`) — provides `LocalSource`
- `AppWorkersModule` (in `tooling:application_workers`) — provides
  `AppWorkerFactory`, `PeriodicWorkersInitializer`
- Per-flavor: `FullModule` (MLKit translator) or `FossModule` (no-op
  translator)
- `Scraper` is `@Singleton @Inject` — gets `NetworkClient`, `LocalSource`,
  `AppPreferences` injected; constructs all 30 sources + 2 databases

## WorkManager workers

Two periodic workers (defined in `tooling:application_workers`):

| Worker | Interval | Purpose |
|---|---|---|
| `LibraryUpdatesWorker` | Configurable (default 24h) | Checks each library book's source for new chapters; posts a notification if found |
| `UpdatesCheckerWorker` | Daily | Checks GitHub releases for app updates |

`App` implements `Configuration.Provider` so WorkManager is initialized
on-demand (the default `WorkManagerInitializer` is removed via
`tools:node="remove"` in `AndroidManifest.xml`).

## Adding a new source

1. Create a new file in `scraper/src/main/java/my/noveldokusha/scraper/sources/`
2. Implement `SourceInterface.Base` (URL-only) or `SourceInterface.Catalog`
   (with catalog/search)
3. Add a string resource `source_name_<id>` in
   `strings/src/main/res/values/strings-no-translatable.xml`
4. Register the source in `Scraper.sourcesList` (in `Scraper.kt`)
5. If the source needs URL transformation for fetching or webview,
   override `transformChapterUrl` and/or `transformWebviewUrl`
6. ProGuard keep rules in `app/proguard-rules.pro` already cover
   `my.noveldokusha.scraper.sources.**` via wildcard — no per-source entry
7. Add an instrumented test in
   `app/src/androidTest/.../SourcesCatalogTest.kt` calling
   `checkSourceCanBeOpened("Display Name")`

Source IDs MUST be unique (unit-tested in `ScraperTest.kt`). Base URLs
MUST end with `/` (also unit-tested).
