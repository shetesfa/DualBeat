# DUALBEAT — Advanced Android Media Player & Multi-Output Audio Platform

**Developer / Founder:** Tesfa  
**Category:** Native Android Media Player, Equalizer & Multi-Output Audio Distribution  
**Primary Target Device:** Samsung Galaxy A24 (SM-A245F)  
**Target Operating System:** Android 16 / One UI 8.5 (minSdk 26, targetSdk 35)  
**Development Language:** 100% Kotlin  
**UI Framework:** Jetpack Compose + Material 3 (AMOLED & Dynamic Themes)  
**Audio Architecture:** AndroidX Media3 (ExoPlayer), Android AudioFX, DualBeat Cast Engine  

---

## 🎵 Product Vision
> **"One Library. Powerful Playback. Smarter Sound."**

DualBeat is a native Android media player engineered from the ground up for high-performance audio/video playback, advanced sound sculpting, and flexible audio distribution across physical speakers and local Wi-Fi devices.

---

## ⚡ Key Highlights

### 1. Unified Local Media Library
- Automatic indexing of on-device music (`audio/*`) and videos (`video/*`) via scoped Android `MediaStore`.
- Intuitive Folder Browser with directory hierarchy, file size calculation, and track counts.
- Multi-criteria real-time sorting (by Title, Date Added, Date Modified, Duration, File Size, Artist, Album).
- Fast local search with instant substring matching across metadata and file paths.
- Local Favorites and Playlists backed by Android Room database.

### 2. High-Fidelity Music Player
- Powered by **AndroidX Media3** with a dedicated background `MediaSessionService`.
- Persistent **Mini-Player** with tap-to-expand modal bottom sheet.
- System lock-screen and notification controls with media art and playback progress.
- Headset unplugged protection (`AUDIO_BECOMING_NOISY`), audio focus ducking, and resume on interruption.
- Queue management with Shuffle and 3-stage Repeat (Off / All / One).

### 3. Gesture-Driven Video Player
- Seamless video rendering using Jetpack Compose and AndroidX Media3 `PlayerView`.
- Configurable touch gestures:
  - Vertical swipe on left side: Screen brightness.
  - Vertical swipe on right side: Volume.
  - Horizontal swipe: Video timeline scrubbing.
  - Double-tap left/right: 10-second instant seek.
  - Single tap: Toggle UI controls.
- Aspect ratio switcher (Fit, Fill, Zoom, 16:9, 4:3) and Speed control (0.5x to 2.0x).
- Screen Lock toggle to prevent accidental touches during playback.

### 4. Advanced Sound Studio (Equalizer & Effects)
- Genuine integration with Android platform `android.media.audiofx` attached directly to the active audio session ID.
- Multi-band graphic equalizer with exact decibel level control (-15dB to +15dB).
- Studio Presets: *Normal, Rock, Pop, Classical, Jazz, Electronic, Vocal, Bass Boost, Custom*.
- Hardware-accelerated Bass Boost slider (0–100%) and 3D Virtualizer spatial effect.

### 5. Speaker Control Center & Multi-Output Engine
- **Bluetooth Device Management:** Enumerate bonded and connected Bluetooth speakers and identify the active system audio sink.
- **Samsung One UI Dual Audio Analysis:** Deep inspection of Samsung Galaxy A24 firmware capabilities, distinguishing standard Android single A2DP routing from Samsung One UI Quick Panel Dual Audio.
- **Wi-Fi Multi-Speaker (DualBeat Cast):** Cross-device synchronized audio distribution over local Wi-Fi. Stream to other phones or receiver devices with timestamped packets and jitter buffers.

### 6. Adaptive Split View & Multitasking
- Dual-pane layout designed for phones, foldable devices, and tablets in portrait and landscape orientations.
- Multitask seamlessly: Music + Equalizer, Speakers + Sound Studio.

### 7. Developer Profile
- Dedicated profile for founder and developer **Tesfa** located at **Settings → About DualBeat → Developer Profile**.
- Includes a dedicated 3:4 portrait card, application version information, and privacy assurances.

---

## 📂 Project Architecture

```
app/src/main/java/com/tesfa/dualbeat/
├── DualBeatApp.kt                  # Application class, dependency container
├── MainActivity.kt                 # Launcher activity, permission orchestration
├── core/
│   ├── theme/                      # Color palettes, Typography, AMOLED & Light Themes
│   └── util/                       # FormatUtils (duration, file size, timestamps)
├── data/
│   ├── local/                      # Room Database, Entities (Playlists, Favorites, History), DAOs
│   ├── preferences/                # Jetpack DataStore Preferences Repository
│   └── repository/                 # MediaRepository, PlaylistRepository
├── domain/
│   └── model/                      # AudioTrack, VideoItem, MediaFolder, ReceiverNode, AudioEffects
├── playback/
│   ├── player/                     # MusicPlaybackController, VideoPlaybackController
│   └── service/                    # DualBeatMediaSessionService (Media3 Foreground Service)
├── audio/
│   ├── effects/                    # SoundStudioEngine (Equalizer, BassBoost, Virtualizer)
│   └── capture/                    # SystemAudioCaptureService (MediaProjection / AudioPlaybackCapture)
├── bluetooth/
│   ├── BluetoothDeviceManager.kt   # Bonded devices, connection state, active route
│   └── MultiBluetoothEngine.kt     # Samsung A24 HAL analysis, Dual Audio evaluation
├── receiver/
│   ├── protocol/                   # DualBeatCastProtocol (Packet headers, PTS synchronization)
│   ├── DualBeatCastServer.kt       # Wi-Fi transmitter & UDP beacon broadcaster
│   ├── DualBeatCastClient.kt       # Wi-Fi receiver node with jitter buffer & AudioTrack
│   └── service/                    # DualBeatCastService (Foreground Wi-Fi streaming service)
└── ui/
    ├── navigation/                 # Screen definitions & DualBeatNavGraph
    ├── components/                 # MiniPlayer, MediaCards, DeviceCard, EqualizerSliders
    ├── home/                       # HomeScreen & HomeViewModel
    ├── music/                      # MusicScreen, MusicViewModel, FullMusicPlayerBottomSheet
    ├── video/                      # VideoScreen, VideoPlayerScreen, VideoViewModel
    ├── speakers/                   # SpeakersScreen & SpeakersViewModel
    ├── sound/                      # SoundStudioScreen & SoundStudioViewModel
    ├── split/                      # AdaptiveSplitViewScreen
    └── settings/                   # SettingsScreen, AboutScreen, DeveloperProfileScreen
```

---

## 🔒 Privacy & Permissions
DualBeat is built on a strict zero-telemetry policy. The app does not connect to external servers or collect analytics. Refer to `PERMISSIONS_AND_PRIVACY.md` for a complete audit of declared Android permissions.

---

## 📜 Copyright
© 2026 DualBeat. Developed by **Tesfa**. All rights reserved.
