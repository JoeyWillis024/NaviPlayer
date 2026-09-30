# NaviPlayer 🎵

A custom, native Android music client crafted specifically for **[Navidrome](https://www.navidrome.org/)** and Subsonic-compatible servers.

Built with **Kotlin 2.x**, **Jetpack Compose (Material 3)**, and **AndroidX Media3**, NaviPlayer combines audiophile playback quality with modern Android system integrations like Android Auto, lockscreen playback controls, and offline downloads.

---

## ✨ Features

- **🚀 Modern Native Stack:** Built 100% with Kotlin, Jetpack Compose, Material 3 with dynamic wallpaper/artwork color theming, and AndroidX Media3 (`ExoPlayer` + `MediaLibraryService`).
- **🎧 Adaptive Quality & Transcoding:**
  - **WiFi:** Direct streaming (Lossless FLAC, MP3, AAC) without server transcoding.
  - **Cellular:** Automatic on-the-fly server transcoding (Opus / MP3 at 192/320 kbps) to preserve mobile data.
- **📥 Offline Downloads & Smart Cache:**
  - Automatic 1GB LRU playback cache for seamless scrubbing and replay.
  - One-tap offline downloading for songs, albums, and playlists indexed in a local **Room** database.
- **🚗 Android Auto & System Integration:**
  - In-car dashboard library browsing (Favorites, Recently Played, Playlists) via `MediaLibrarySession`.
  - Android notification with seekable progress bar, album art, and lockscreen controls.
  - Bluetooth auto-pause and resume (`AUDIOFOCUS` & `ACTION_AUDIO_BECOMING_NOISY`).
  - Home Screen widget built with **Jetpack Glance**.
- **🎛️ In-App Equalizer & Bass Boost:**
  - Built-in 5-band equalizer with presets (Flat, Rock, Pop, Jazz, Electronic, Vocal) and custom band adjustment.
  - Hardware-accelerated bass boost slider.
- **⏱️ Sleep Timer:**
  - Set 15, 30, 45, or 60 minute timers with smooth audio fade-out over the final 30 seconds.
- **📊 Scrobbling:**
  - Automatic Navidrome server scrobbling (`/rest/scrobble`).
  - Broadcast intents for third-party scrobblers like **Pano Scrobbler** and **Simple Last.fm Scrobbler**.
- **🌐 Multi-Server & Reverse Proxy Support:**
  - Manage multiple Navidrome servers with instant switching.
  - Subsonic MD5 token+salt authentication (plain passwords are never sent).
  - Custom HTTP header support for Cloudflare Access (`CF-Access-Client-Id`/`Secret`), Authelia, or Basic Auth.

---

## 🏛️ Architecture

NaviPlayer follows **Clean Architecture** with **Unidirectional Data Flow (MVI)**:

```
app/src/main/java/com/antiwilly/naviplayer/
├── MainActivity.kt               # Entrypoint with theme and edge-to-edge
├── NaviApplication.kt            # Hilt application and WorkManager setup
├── core/
│   ├── model/                    # Domain models (Song, Album, Artist, ServerProfile)
│   ├── network/                  # Retrofit, SubsonicAuthInterceptor, DTOs
│   ├── database/                 # Room database, DAOs (Downloads, Cache, Profiles)
│   ├── datastore/                # User preferences and equalizer settings
│   ├── playback/                 # NaviMediaService (Media3), AudioEffects, Scrobbler
│   └── data/
│       ├── repository/           # Music, Server, Download, Playback repositories
│       └── worker/               # DownloadTrackWorker (WorkManager)
├── feature/
│   ├── home/                     # Home feed (Recent, Most Played, Discovery)
│   ├── library/                  # Artists, Albums, and Playlists tabs
│   ├── album/                    # Album detail view and tracklist
│   ├── artist/                   # Artist detail view and discography
│   ├── playlist/                 # Playlist detail view
│   ├── search/                   # Real-time instant search
│   ├── downloads/                # Downloaded offline music manager
│   ├── player/                   # Fullscreen Now Playing, Queue, Scrubber
│   ├── equalizer/                # In-app equalizer modal bottom sheet
│   ├── sleeptimer/               # Sleep timer modal bottom sheet
│   ├── settings/                 # Server profiles and streaming quality rules
│   └── widget/                   # Glance Home Screen widget
└── ui/
    ├── NaviApp.kt                # Root Scaffold, NavHost, and MiniPlayer
    ├── components/               # SongListItem, AlbumCard, MiniPlayer
    └── theme/                    # Material 3 Theme, Typography, Palette Extractor
```

---

## 🛠️ Building & Running

### Prerequisites
- **Android Studio Ladybug (2024.2+)** or newer
- **JDK 17+**
- **Android SDK Platform 35** (minSdk: 26, targetSdk: 35)

### Running in Android Studio
1. Open Android Studio and select **Open** -> choose `/home/antiwilly/Downloads/GRAV`.
2. Allow Gradle sync to complete (Gradle will download dependencies specified in `gradle/libs.versions.toml`).
3. Connect an Android device or start an Android Virtual Device (AVD).
4. Click **Run** (`Shift + F10`).

### Command Line Build
```bash
./gradlew assembleDebug
```
The APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

### Running Unit Tests
```bash
./gradlew testDebugUnitTest
```

---

## 🔒 Connecting to Navidrome

1. Open NaviPlayer and navigate to the **Settings** tab.
2. Tap **Add Server**:
   - **Server Name:** E.g., `Home Server`
   - **Base URL:** E.g., `https://music.yourdomain.com` or `http://192.168.1.100:4533`
   - **Username:** Your Navidrome username
   - **Password:** Your Navidrome password
   - *(Optional)* **Reverse Proxy Header:** If behind Cloudflare Access or Authelia, enter header name (e.g. `CF-Access-Client-Id`) and value.
3. Tap **Test Connection** to verify connectivity with Navidrome.
4. Tap **Save & Connect**.
# NaviPlayer
