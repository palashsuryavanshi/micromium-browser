<div align="center">
<img width="160" height="160" alt="Micromium logo" src="icons/android-chrome-512x512.png" />
</div>

<h1 align="center">Micromium</h1>

<p align="center">A privacy-first Android browser with a built-in tracking-protection shield, live tab previews, and a clean Material 3 interface.</p>

## Features

### 🛡️ Privacy Shield
- Hardcoded ad blocking (DoubleClick, AdSense, Outbrain & ad exchanges)
- Cross-site tracker protection (analytics, pixels, session recording, fingerprinters)
- Tracking-parameter stripping (`utm_*`, `fbclid`, `gclid`, …)
- Third-party cookie blocking
- Cosmetic element hiding (collapses blank spaces left by blocked ads)
- Per-page blocked counters (ads, trackers, data saved) with a blocked-event log
- Per-site controls: clear site cookies & cache

### 🧭 Browsing
- Chromium-powered WebView with desktop-site toggle, zoom, and HTTPS indicator
- Smart omnibox: type a URL or a search query, with search suggestions UI
- Multiple search engines (DuckDuckGo, Google, Brave) plus custom engines you can add/remove
- Bookmarks and full browsing history, stored on-device (Room)
- Incognito tabs
- System-driven rotation (follows the device auto-rotate setting)

### 🗂️ Tabs
- Grid tab switcher with **live page-preview thumbnails** — every tab's WebView stays alive in the background
- Grey placeholder for tabs that haven't loaded a page yet
- Swipe-to-dismiss tabs, close-all, new-tab button, active-tab highlight

### 🎨 Appearance
- Dark and light themes with a neutral grey/blue accent system
- Toolbar at the top or bottom
- Animated dropdown menu, settings page transitions, and tab-switcher open/close

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

## Project structure
- `app/src/main/java/com/example/` — `MainActivity.kt`, ViewModel, data (Room), privacy engine
- `…/ui/components/` — omnibox, WebView, tab switcher, start page, shield sheet, settings, dialogs
- `…/ui/theme/` — color schemes and typography
- `icons/` — app icon set and web manifest
