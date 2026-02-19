# movies-android

Lightweight native Android app for watching movies and TV shows from multiple embed sources — no popups, no ads breaking playback.

## How it works

The app uses Android's `WebView` to load embed player URLs, but wraps them in a strict containment layer:

- **`shouldOverrideUrlLoading`** — any top-frame navigation to a domain not on the allowlist is silently dropped. Ad redirects vanish without the embed player noticing.
- **`onCreateWindow`** → returns `false` — all `window.open()` / `target="_blank"` popups are dead on arrival. The embed site never gets an error; the popup simply never appears.
- **`onJsAlert/Confirm/Prompt`** — all JavaScript dialogs are auto-dismissed silently.

The embed player loads and plays exactly as it would in a browser. It just happens to be inside a very quiet cage.

## Features

- Search movies and TV shows via OMDb
- Multiple embed sources with one-tap switching
- Season & episode picker for TV series
- Recently watched list (up to 20 items, stored locally)
- Full-screen landscape player, tap to toggle controls
- Zero permissions beyond `INTERNET`

## Building in GitHub Actions (recommended)

1. Push this repo to GitHub
2. **Settings → Secrets and variables → Actions → New repository secret:**
   - `OMDB_API_KEY` — your free key from [omdbapi.com/apikey.aspx](https://www.omdbapi.com/apikey.aspx)
3. Push to `main` or trigger **Actions → Build APK → Run workflow**
4. Download the APK from the **Artifacts** section of the run
5. Install on Android (enable *Install unknown apps* for your file manager)

## Release signing (optional)

To get a signed, optimised release APK:

1. Generate a keystore:
   ```bash
   keytool -genkey -v -keystore release.jks -alias movies -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Base64-encode it: `base64 -w 0 release.jks`
3. Add repository **secrets**: `KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
4. Add a repository **variable** (not secret): `HAS_SIGNING = true`
5. The `build-release` job runs automatically on every push.

## Local build

```bash
# One-time: generate the Gradle wrapper (requires Gradle installed)
gradle wrapper --gradle-version 8.6

# Build
./gradlew assembleDebug
# APK → app/build/outputs/apk/debug/app-debug.apk
```

## Adding / reordering sources

Edit `EMBED_SOURCES` in [app/src/main/java/xyz/elouan/movies/data/Models.kt](app/src/main/java/xyz/elouan/movies/data/Models.kt). The order controls both the button order and which source loads first.
