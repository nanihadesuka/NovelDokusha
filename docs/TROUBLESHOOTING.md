# Troubleshooting

Common runtime issues and their fixes. For build-time issues, see
[BUILD.md §6](BUILD.md#6-common-build-errors-and-fixes).

## v3.0.0 Migration Notes

If upgrading from v2.2.9 or earlier:
- **MLKit is removed** — translations now use 4 cloud providers. No language model downloads needed.
- **Database auto-migrates** from v9 to v10 (adds ChapterTranslation table). No user action needed.
- **Translation provider defaults to Google PA** (Enhanced) which auto-fetches an API key from wtr-lab.com. No setup required.
- To switch providers: change `TRANSLATION_PROVIDER` in SharedPreferences (Settings UI for this is coming in a future update).

## Catalog is empty for a source

**Symptoms**: You open a source in Finder, the catalog page shows no
books (or shows a loading spinner forever).

**Causes & fixes**:

1. **Source's domain changed** — The site moved to a new URL. Check
   `docs/SOURCES.md` for the current domain. If you're a developer,
   update the source's `baseUrl` and `catalogUrl`, then add a Room
   migration (see `tooling/local_database/.../migrations/` for examples).

2. **Cloudflare challenge not solving** — The first request to a CF-
   protected source takes 2-20 seconds while the WebView solves the
   challenge. If it consistently fails:
   - Check your network — CF may have IP-blocked you (error 1020/1015).
     Try a different network (mobile data vs Wi-Fi).
   - Update Android System WebView via the Play Store — old WebView
     versions can't solve newer Turnstile challenges.
   - If you're on a custom ROM without Google Play, install
     [ Bromite](https://www.bromite.org/) or a current WebView provider.

3. **Source site is down** — Open the source's URL in a browser. If the
   site itself is down, the app can't help. Check
   `docs/SOURCES.md` → "Removed Sources" for known-dead domains.

4. **App version too old** — Source scrapers break when sites change
   their HTML structure. Update to the latest app version.

## Chapter text is in Chinese (not translated)

**Symptoms**: You're using `TimoTxt (Translate)` or `TimoTxt (Gemini)`
but chapters show raw Chinese text.

**Causes & fixes**:

1. **For TimoTxt (Translate)**: The Google Translate free API
   (`client=gtx`) may be rate-limiting you (HTTP 429). The app retries 3
   times with exponential backoff, then falls back to the original
   Chinese text. Wait a few minutes and try again, or switch to the
   `TimoTxt (Gemini)` source (different API, different rate limit).

2. **For TimoTxt (Gemini)**: No Gemini API key is configured. Go to
   Settings → Translation Models → Gemini API key and paste your key
   (get one at <https://aistudio.google.com/app/apikey>). Without a key,
   translation is skipped and Chinese is shown.

3. **Stale cache** — If you previously read this chapter with a
   different (broken) source, the cached body may be Chinese. The app
   has a stale-cache guard that detects Chinese text on translate.goog /
   gemini.goog URLs and re-fetches, but if the URL is plain timotxt.com
   the guard doesn't trigger. Long-press the chapter → "Re-download", or
   clear the app's cache from Settings → Data.

## "Open in WebView" shows "can't translate this page"

**Symptoms**: You tap the WebView icon in the reader, the browser opens
but shows a Google Translate error page instead of the chapter.

**Cause**: The stored chapter URL is missing the `_x_tr_*` query params
that the translate.goog proxy requires. This was a bug in v2.2.8 and
earlier where `transformWebviewUrl()` didn't exist and the reader used
`transformChapterUrl()` (which converts to timotxt.com, not the proxy).

**Fix**: Update to v2.2.9 or later. The reader now calls
`source.transformWebviewUrl(url)` which adds the params for all three
TimoTxt sources.

## Chapter list shows newest chapter first

**Symptoms**: Chapters are listed in reverse order (chapter 422 first,
chapter 1 last).

**Cause**: The timotxt.com `/dir` page has two `<ul>` lists inside
`.chaplist`:
1. `ul.flex` (12 links, newest first — "latest updates" sidebar)
2. `ul.flex.all` (all links, oldest first — complete list)

The scraper uses `.chaplist ul.all li a` to target the complete list,
which is already oldest-first. If the site changes its HTML structure
and the `.all` selector stops matching, the scraper falls back to
`.chaplist ul li a` which matches the sidebar (newest first).

**Fix**: Update the source's selector in
`scraper/.../sources/TimoTxt*.kt`. Report the issue with a screenshot
of the page's HTML.

## Reader crashes when opening a chapter

**Symptoms**: The reader opens, shows a loading spinner, then crashes.

**Causes & fixes**:

1. **Out of memory** — The reader loads multiple chapters in advance for
   infinite scroll. On devices with <2GB RAM, very long chapters can
   OOM. Try closing other apps, or reduce the reader's font size (larger
   fonts = more layout memory).

2. **Source returns invalid HTML** — If a source's `getChapterText()`
   throws (e.g. Jsoup can't parse the page), the reader shows an error
   dialog. Try "Re-download" or switch sources via the chapter list
   menu.

3. **Database corruption** — Rare, but if the app's Room database is
   corrupted (e.g. from a failed backup restore), chapter reads can
   crash. Clear app data from Android Settings → Apps → ParasDokusha →
   Storage → Clear data (this wipes your library — make sure you have a
   backup).

## Text-to-speech doesn't work

**Symptoms**: You tap the TTS button, nothing plays, or you get an
error.

**Causes & fixes**:

1. **No TTS engine installed** — Install one from the Play Store (e.g.
   Google Text-to-Speech, Samsung TTS). Go to Android Settings →
   Language & input → Text-to-speech to configure.

2. **No voices for the chapter's language** — If the chapter is in
   Chinese but you have no Chinese TTS voice, TTS won't play. Install
   the appropriate voice from your TTS engine's settings.

3. **Background playback stops when screen is off** — The app uses a
   foreground service for background playback. If you've disabled the
   "Reader narrator" notification channel, the service can't start.
   Re-enable it from Android Settings → Apps → ParasDokusha →
   Notifications.

4. **Media controls don't respond** — The notification media session
   requires Android 5.0+ (the app's minSdk is 26, so this is fine).
   If your phone's ROM has a custom notification shade that doesn't
   respect `MediaStyle`, the buttons may not appear. This is a ROM bug.

## Backup restore fails

**Symptoms**: You restore a backup file and get an error, or your
library is empty after restore.

**Causes & fixes**:

1. **Wrong file format** — The app expects a `.db` SQLite file created
   by the app's own backup feature. If you're trying to restore a file
   from a different app, it won't work.

2. **Database version mismatch** — If the backup was created with an
   older app version, the restore runs Room migrations. If a migration
   fails, the restore aborts. Update to the latest app version and try
   again — newer versions have all historical migrations.

3. **File access denied** — If the backup file is on removable storage
   that's been unmounted, or in a directory the app doesn't have SAF
   access to, the restore fails. Copy the file to internal storage
   (e.g. Downloads) and try again.

## Library auto-update doesn't find new chapters

**Symptoms**: You've added books to your library, but the app doesn't
notify you of new chapters even though the source site has them.

**Causes & fixes**:

1. **Auto-update disabled** — Go to Settings → Library auto-update and
   make sure it's enabled. Check the interval (default 24h).

2. **Source domain changed** — If the source moved to a new URL, the
   app's stored book URLs point to the dead domain. The migration
   should handle this, but if you skipped an app version the migration
   may not have run. Re-add the books from the new source.

3. **Cloudflare blocking the worker** — The library update worker runs
   in the background and may be killed by the OS before the CF
   challenge is solved. Try triggering a manual update from the
   Library screen's menu.

4. **Battery optimization** — Android's Doze mode can delay the worker
   significantly. Disable battery optimization for ParasDokusha from
   Android Settings → Apps → ParasDokusha → Battery.

## App won't install (APK parse error)

**Symptoms**: You sideload the APK and get "App not installed" or
"There was a problem parsing the package".

**Cause**: The APK was signed with v2-only signing. Many Android TV
devices and some phones require v1 (JAR) signing.

**Fix**: Use a v1+v2 signed APK. All official release builds from v2.2.9+
are signed with both schemes. If you're building yourself, follow
[BUILD.md §4](BUILD.md#4-signing-the-apk-v1--v2) and re-sign manually
with `apksigner`.

## Reporting bugs

If none of the above helps, open an issue on GitHub with:

1. App version (Settings → About)
2. Android version and device model
3. Source(s) affected
4. Steps to reproduce
5. Expected vs actual behavior
6. Logcat output (if you can capture it — `adb logcat` while
   reproducing the issue)
