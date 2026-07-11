# Performance Notes

This document captures known performance characteristics, bottlenecks, and
the findings from the v2.2.9 and v3.0.0 audit passes. It is intended for
contributors who want to make the app faster or avoid re-introducing
regressions.

## v3.0.0 Audit Fixes

### ReaderSession leak on Activity destroy (CRITICAL — fixed)
`ReaderActivity.onDestroy()` now calls `viewModel.onCloseManually()` when `isFinishing` is true. Previously, navigating up via the toolbar (not back button) leaked the entire ReaderSession — TTS engine, foreground service, 5 coroutines, translation providers — until process death.

### ForegroundServiceDidNotStartInTimeException crash (CRITICAL — fixed)
`NarratorMediaControlsService.onCreate()` now always calls `startForeground()` with a placeholder notification before checking for a null session. Previously, a race between `ReaderSession.close()` and `startForegroundService()` could skip `startForeground()`, causing the system to crash the app within 5 seconds.

### LuaEngine blocking Default threads (HIGH — fixed)
`runBlocking { }` in LuaEngine's `http_get`/`http_post`/`http_get_batch` changed to `runBlocking(Dispatchers.IO) { }`. Previously, Lua HTTP calls blocked `Dispatchers.Default` threads (sized `max(2, CPU cores)`), causing thread-pool starvation during global-source-search over 30+ Lua sources.

### ReaderChaptersLoader SupervisorJob not cancelled (HIGH — fixed)
`ReaderSession.close()` now calls `(coroutineContext[Job])?.cancel()` on the chapters loader in addition to `cancelChildren()`. Previously, the loader's `SupervisorJob` remained active after close, allowing orphan coroutines to launch.

### AppPreferences CoroutineScope leak (MEDIUM — fixed)
`toFlow()` no longer allocates a `CoroutineScope(Dispatchers.Default)` per call. The `MutableStateFlow.value` is set directly from the SharedPreferences listener (thread-safe). Previously, ~40 preferences each leaked a `SupervisorJob` that lived forever.

### AppPreferences thread-unsafe listener set (MEDIUM — fixed)
`preferencesChangeListeners` changed from `mutableSetOf()` to `Collections.synchronizedSet()`. Previously, concurrent modification between `onSubscription` and `onCompletion` could corrupt the set.

### Gemini default model mismatch (HIGH — fixed)
`TranslationManagerGemini.getApiEndpoint()` now defaults to `"gemini-2.5-flash"` (matching `AppPreferences`). Previously, it defaulted to `"gemini-2.5-flash-lite"`, silently using a cheaper, lower-quality model.

## v2.2.9 Audit Fixes
(300ms worst case, the CF interceptor's poll interval).

## Cursor lifecycle

**Fixed in v2.2.9**: `Cursor.asSequence()` in
`core/CursorExtensions.kt` now closes the cursor in a `finally` block
inside the sequence builder.

**Before**: the cursor was only closed when the consumer iterated the
sequence to completion. Operators like `take(n)`, `first()`, or an
exception in a downstream `map` would abandon the sequence early and
leak the cursor. `AppLocalSources` recursively walks the SAF tree with
these sequences, so a deep directory scan that was cancelled by the user
navigating away could leak dozens of open cursors.

**After**: the cursor is always closed, even on early exit.

## OkHttp Response body leaks

**Fixed in v2.2.9**: `DownloaderRepository.bookChapter()` now uses
`.use { }` when following redirects to discover a source's real URL.

**Before**: the redirect-following `networkClient.call(request,
followRedirects = true)` returned a `Response` that was read for its
`.request.url` but never closed. Each call leaked a connection back to
the pool unreturned, eventually forcing OkHttp to open new connections
for every subsequent request.

**After**: `.use { it.request.url.toString() }` guarantees the Response
is closed even if reading the URL throws.

## Reader session scope

**Fixed in v2.2.9**: `ReaderSession.close()` now calls `scope.cancel()`
instead of `scope.coroutineContext.cancelChildren()`.

**Before**: `cancelChildren()` only cancelled running child coroutines —
the scope's `Job` itself remained active. Any coroutine launched on the
scope after `close()` (e.g. a stray `snapshotFlow.collect` from a
notification update) would still run, holding references to the closed
session's state.

**After**: `scope.cancel()` fully cancels the scope. Any subsequent
launch on the cancelled scope throws `CancellationException` immediately,
making post-close launches fail fast.

## WebView lifecycle

**Fixed in v2.2.9**: `WebViewActivity` now overrides `onDestroy()` to
call `webView.stopLoading()`, `webView.removeAllViews()`, and
`webView.destroy()`.

**Before**: the WebView was created in `onCreate()` and never explicitly
destroyed. Android's WebKit renderer thread outlives the Activity,
holding the Activity's context via the WebViewClient. Each "open in
webview" leaked the entire Activity view hierarchy until GC — which on
low-RAM devices could be many seconds.

**After**: the WebView is torn down in `onDestroy()`, releasing the
renderer thread and the Activity context.

## TimoTxt translation batching

**Fixed in v2.2.9**: `TimoTxtTranslate` now uses `BATCH_CHAR_LIMIT = 4500`
(was 2000). Testing confirmed Google Translate's free `client=gtx`
endpoint accepts POST bodies of 5000+ characters and returns a full
translation in a single request.

**Impact**: a typical 2000-character Chinese chapter previously required
1 batch; now the same chapter is 1 batch. Longer chapters (4000-5000
chars) previously required 2-3 batches; now 1. Each batch avoids a
network round-trip + the 300ms inter-batch delay, saving ~600-900ms per
long chapter.

## Catalog title translation

**Fixed in v2.2.9**: `TimoTxtTranslate.getCatalogList()` and
`getCatalogSearch()` now use `translateBatchTitles()` (joins 15 titles
with `|||`, one API call) instead of calling `translateText()` per
title (18 separate API calls per catalog page).

**Impact**: catalog page load dropped from ~18 API calls × (1-3 retries
× 1.5s backoff) = up to 60s+ on a flaky network, to ~1-2 API calls =
~1-2s. If the API fails, the catalog still loads — Chinese titles are
shown instead (fast-fail mode with `TITLE_TRANSLATE_MAX_RETRIES = 1`).

## Gemini API key guard

**Fixed in v2.2.9**: `TimoTxtGemini` now checks `geminiApiKey.isBlank()`
before every translation call. Without a key, all translation is skipped
and Chinese text is shown. Previously, calling
`geminiClient.translateText()` with an empty key would fail after the
full 14-second rate-limit timeout, blocking the catalog or info page
for that entire duration.

## Known remaining bottlenecks

These are known but not yet fixed — they're acceptable for v2.2.9 but
worth revisiting:

### 1. Cloudflare bypass WebView overhead

The first challenged request on a fresh session pays the full WebView
solve cost (2-20 seconds). Subsequent requests to the same domain reuse
the cached `cf_clearance` cookie and pay no cost. There's no way to
avoid this without a server-side proxy.

### 2. R8 minification build time

`./gradlew assembleFullRelease` takes 5-8 minutes on a 2-core CI runner,
mostly in the `:app:minifyFullReleaseWithR8` task. R8 performs
whole-program optimization across all 29 modules. On a 4+ core machine
it drops to 2-4 minutes. Debug builds skip R8 entirely and take 1-2
minutes.

### 3. Lint vital analysis OOM

On machines with <4GB RAM, `:app:lintVitalAnalyzeRelease` can OOM-kill
the Gradle daemon. The release workflow runs lint, but local builds can
skip it with `-x lintVitalAnalyzeRelease -x lintAnalyzeFullRelease`.

### 4. Room write contention

`ChapterBodyRepository.insertWithTitle()` runs inside a Room transaction
that writes to both `ChapterBody` and `Chapter` tables. Under heavy
chapter preloading (the reader loads the next chapter in advance), these
transactions can block each other. The reader uses `Dispatchers.Default`
for fetching and `Dispatchers.IO` for DB writes, which helps, but a
write-ahead logging (WAL) mode would be better. Room enables WAL by
default on API 16+, so this is already the case — but the transaction
serializes writes within a single connection.

### 5. Image loading

Coil is configured to share the OkHttp client (so CF cookies apply to
image loads). Glide is also on the classpath (used by the landscapist
Compose Glide integration). Two image libraries is wasteful — a future
cleanup should pick one. Coil is the better choice for Compose-first
apps.

## Profiling tips

- **Network**: enable `HttpLoggingInterceptor` (debug builds only) via
  `AppInternalState.isDebugMode`. Logs go to Timber.
- **DB**: use Android Studio's Database Inspector to watch Room queries
  in real time.
- **Coroutines**: `kotlinx.coroutines.debug` is not enabled; if you need
  to debug a hung coroutine, add `-Dkotlinx.coroutines.debug=on` to the
  JVM args.
- **Memory**: Android Studio's Memory Profiler. Watch for
  `android.webkit.WebView` instances lingering after Activity destroy —
  the v2.2.9 fix above should prevent this.
- **R8**: `./gradlew assembleFullRelease --scan` for a full build scan.
