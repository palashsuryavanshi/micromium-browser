<div align="center">
<img width="160" height="160" alt="Micromium logo" src="icons/android-chrome-512x512.png" />
</div>

<h1 align="center">Micromium</h1>

<p align="center">A privacy-first Android browser with a built-in tracking-protection shield, live tab previews, and a clean Material 3 interface.</p>

## Features

### 🛡️ Privacy Shield
- Plain-language protection verdict per page ("Blocked 12 hidden ads and trackers…")
- Ad blocking (DoubleClick, AdSense, Outbrain & ad exchanges)
- Cross-site tracker protection (analytics, pixels, session recording, fingerprinters)
- Tracking-link cleaning (`utm_*`, `fbclid`, `gclid`, …)
- Third-party cookie blocking
- Tidy-up of blank spaces left by blocked ads
- Per-page counters (ads stopped, trackers stopped, data saved) with a per-site activity log
- One-tap **Forget this site**: clears that site's cookies, storage, and history

### 🧭 Browsing
- Chromium-powered WebView with per-site desktop-mode memory, zoom, and HTTPS indicator
- Reader view for long articles
- Smart omnibox: type a URL or a search query, with search suggestions UI
- Multiple search engines (DuckDuckGo, Google, Brave) plus custom engines you can add/remove
- Bookmarks (with folders) and full searchable browsing history, stored on-device (Room)
- In-app downloads list with open/remove, plus system viewer fallback
- Share page via system share sheet or QR code
- Incognito tabs
- System-driven rotation (follows the device auto-rotate setting)

### 🗂️ Tabs
- Grid tab switcher with **live page-preview thumbnails** — every tab's WebView stays alive in the background
- Grey placeholder for tabs that haven't loaded a page yet
- Swipe-to-dismiss tabs, close-all, new-tab button, active-tab highlight

### 🎨 Appearance
- Dark and light themes with a neutral grey/blue accent system
- Toolbar at the top or bottom
- Spring-physics animations: dropdown menu open, sliding settings pages, staggered tab-grid entrance, tab-switcher open/close

### 🔐 Password manager (100% on-device, no account, no cloud)
- Device-password login plus system screen-lock unlock (PIN / pattern / fingerprint)
- Passwords encrypted at rest (Tink AES-256-GCM, keys in Android Keystore); the password itself is never stored
- Save, reveal, copy, and delete logins per site
- CSV import with auto-detected dialects: Chrome, Brave, Opera, Edge (`name,url,username,password`) and Firefox (`url,username,password,…`) — skips blanks and duplicates

### ⚙️ Settings
- Appearance (theme + toolbar position), Search Engine (defaults + custom), About
- Onboarding questionnaire that pre-configures shield defaults

## Tech stack
- Kotlin, Jetpack Compose (Material 3), Room, DataStore/Preferences-style repository, Gradle version catalogs

## Run locally

**Prerequisites:** [Android Studio](https://developer.android.com/studio) (its bundled JDK builds the project; no separate Java install needed).

1. Open Android Studio
2. Select **Open** and choose this project directory
3. Let Gradle sync finish
4. Run on an emulator or a physical device (debug build)

Command line (from the project root, using the Gradle wrapper JAR):

```bash
"C:\Program Files\Android\Android Studio\jbr\bin\java.exe" -jar gradle/wrapper/gradle-wrapper.jar assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Releases

Tagged `v*` pushes build and publish a signed release APK via [`.github/workflows/release.yml`](.github/workflows/release.yml):

1. Create an upload keystore once: `keytool -genkeypair -keystore my-upload-key.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000` (keep it out of git).
2. Add repo secrets: `KEYSTORE_BASE64` (base64 of the `.jks`), `STORE_PASSWORD`, `KEY_PASSWORD`.
3. Push a tag: `git tag v1.1.0 && git push origin v1.1.0` — the workflow derives `VERSION_NAME` from the tag and `VERSION_CODE` from the commit count, runs unit tests, and attaches `app-release.apk` to the GitHub Release.

Local signed build: set `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD` and run `assembleRelease`.

## Project structure
- `app/src/main/java/com/example/` — `MainActivity.kt`, ViewModel, data (Room), privacy engine
- `…/ui/components/` — omnibox, WebView, tab switcher, start page, shield sheet, settings, dialogs
- `…/ui/theme/` — color schemes and typography
- `icons/` — app icon set and web manifest
