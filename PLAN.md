# PLAN.md — ParasDokusha Improvement Plan

> Created after full analysis of ParasDokusha + NoveLA fork + HnDK0 external-sources.
> Last updated: v3.0.0
> Read this BEFORE making any edits.

## Implementation Status

### Phase 1: Critical Bug Fixes ✅ DONE
1. Fix Wtrlab — change "web" to "ai" mode ✓
2. Fix external Lua source baseUrl — use parseBaseUrlFromLua ✓
3. Fix Response.toDocument() to set base URI ✓

### Phase 2: Remove MLKit + Port 4 Translation Providers ✅ DONE
- Deleted MLKit from full flavor ✓
- Ported TranslationManagerComposite + 4 providers ✓
- Ported SupportedLanguages (226 codes) ✓
- Ported 5 prompt presets ✓
- New AppPreferences for all provider settings ✓

### Phase 3: ChapterTranslation DB Caching ✅ DONE
- ChapterTranslation entity + DAO ✓
- Room v9 → v10 with migration ✓
- LocalDatabaseModule provides DAO ✓

### Phase 4: CPS Calibration ✅ DONE
- EMA-smoothed CPS calibration ported to ReaderTextToSpeech ✓
- chapterWordCount, estimatedWpm, estimatedTotalSeconds, estimatedRemainingSeconds ✓

### Phase 4: Audit Fixes ✅ DONE (3 CRITICAL + 5 HIGH + 10 MEDIUM)
- C3: ReaderActivity.onDestroy closes session ✓
- C2: NarratorMediaControlsService always calls startForeground ✓
- H1: LuaEngine runBlocking uses Dispatchers.IO ✓
- H2: ReaderChaptersLoader Job cancelled on close ✓
- H4: Gemini default model fixed ✓
- M1: AppPreferences toFlow no longer leaks CoroutineScope ✓
- M2: preferencesChangeListeners is thread-safe ✓
- M9: Migration SQL removed IF NOT EXISTS ✓
- M10: Removed redundant @Transaction from DAO ✓

## Deferred (Phase 5+ — future sessions)

### Phase 5: Fix remaining audit issues
- C1: Replace blocking execute() with suspend call() in all 4 translation providers
- M6: Google PA key invalidation on auth failure
- H5: OpenAI retry policy decoupled from key count
- H3: TimoTxtGemini reads API key lazily instead of at construction

### Phase 6: UI improvements
- Port floating TTS mini-player from NoveLA (FloatingTtsService + TtsMiniPlayer + FloatingTtsOverlay)
- Port parallel translation display (XML layout + adapter + preferences)
- Port settings UI for translation provider selection
- Port per-novel prompt system
- Port lock-screen media session with cover art

### Phase 7: Lua source improvements
- Port FilterableCatalog interface
- Port Configurable settings support
- Port cf_options Cloudflare per-source configuration
- Port source cache JSON for instant UI rendering

## Decisions
1. Don't upgrade Kotlin/AGP/Gradle — stay on 1.9.23 / 8.2.2
2. Don't copy NoveLA's UI — only port specific features as additions
3. Keep built-in Kotlin sources — Lua sources are ADDITIONAL
4. MLKit removed in v3.0.0 ✓
5. Upload to tmpfiles.org with max expiry (172800 seconds)
