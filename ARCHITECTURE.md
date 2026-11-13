# DUALBEAT — SOFTWARE ARCHITECTURE SPECIFICATION

**Project:** DualBeat  
**Architect:** Tesfa / DualBeat Core Engineering Team  
**Platform:** Native Android (Kotlin)  
**Target:** Samsung Galaxy A24 (SM-A245F) / Android 16 / One UI 8.5  

---

## 1. Architectural Philosophy & Layered Decomposition

DualBeat adheres strictly to Clean Architecture combined with Model-View-ViewModel (MVVM) and unidirectional data flow (UDF).

```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                       │
│    Jetpack Compose UI (Material 3) + StateFlow Observers    │
│    Screens: Home, Music, Video, Speakers, Sound, Split      │
└──────────────────────────────▲──────────────────────────────┘
                               │ Observes UI State / Sends Intents
┌──────────────────────────────┴──────────────────────────────┐
│                     VIEWMODEL LAYER                         │
│    HomeViewModel, MusicViewModel, VideoViewModel,           │
│    SpeakersViewModel, SoundStudioViewModel, SettingsVM      │
└──────────────────────────────▲──────────────────────────────┘
                               │ Calls Use Cases / Repositories
┌──────────────────────────────┴──────────────────────────────┐
│                      DOMAIN LAYER                           │
│    Models: AudioTrack, VideoItem, MediaFolder, Playlist,    │
│            ReceiverNode, AudioEffectState, AudioRouteReport │
└──────────────────────────────▲──────────────────────────────┘
                               │ Queries / Persists / Controls
┌──────────────────────────────┴──────────────────────────────┐
│             DATA & HARDWARE CONTROL LAYER                   │
│  ┌───────────────────────┐       ┌───────────────────────┐  │
│  │   LOCAL PERSISTENCE   │       │   PLAYBACK ENGINES    │  │
│  │   • Room Database     │       │   • Media3 ExoPlayer  │  │
│  │   • DataStore Prefs   │       │   • MediaSessionSvc   │  │
│  │   • MediaStore Query  │       │   • VideoController   │  │
│  └───────────────────────┘       └───────────────────────┘  │
│  ┌───────────────────────┐       ┌───────────────────────┐  │
│  │   AUDIO & HARDWARE    │       │     WI-FI RECEIVER    │  │
│  │   • AudioFX Engine    │       │   • DualBeatCastSvr   │  │
│  │   • BluetoothManager  │       │   • DualBeatCastCli   │  │
│  │   • MultiBtEngine     │       │   • AudioTrack Sync   │  │
│  └───────────────────────┘       └───────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. Audio Playback & MediaSession Pipeline

1. **Playback Controller:** `MusicPlaybackController` encapsulates AndroidX Media3 `ExoPlayer` configured with `AUDIO_CONTENT_TYPE_MUSIC` and `USAGE_MEDIA`.
2. **Audio Focus Handling:** Audio focus is managed automatically by ExoPlayer with ducking and pause-on-transient-loss behavior.
3. **Audio Becoming Noisy:** Registered receiver detects wired headphone disconnect or Bluetooth link drop, pausing playback safely to prevent sudden loud speaker output.
4. **MediaSession Service:** `DualBeatMediaSessionService` runs as a foreground service with `FOREGROUND_SERVICE_MEDIA_PLAYBACK`. It coordinates lock-screen controls, Bluetooth AVRCP media keys, and system media notifications.
5. **Progress Polling:** A non-blocking coroutine loop (`Dispatchers.Main`) polls playback position every 500ms only while playing, minimizing CPU and battery consumption.

---

## 3. Advanced Sound Studio (AudioFX Engine)

- Attached directly to the active `audioSessionId` of the primary media player.
- **Graphic Equalizer:** Uses `android.media.audiofx.Equalizer`. Queries hardware band count, center frequencies, and decibel ranges. Applies presets or custom frequency curves.
- **Bass Boost:** Uses `android.media.audiofx.BassBoost` with hardware support validation (`isStrengthSupported`).
- **Virtualizer:** Uses `android.media.audiofx.Virtualizer` with 3D spatial field expansion.
- **Safety:** Audio effects are safely released on activity destroy or audio session change to avoid OpenSL/AAudio resource exhaustion.

---

## 4. Multi-Bluetooth & Audio Routing Reality on Samsung Galaxy A24

### Platform Boundary Analysis
- Standard Android public APIs (`AudioTrack`, `AudioRouting`, `AudioManager.setCommunicationDevice`) only allow setting a single preferred active audio communication/media route for media playback.
- **Samsung One UI Dual Audio:** Samsung implements a proprietary system-level feature called "Dual Audio" in One UI Quick Panel (Media Output) for Bluetooth 5.0+ devices. This operates in system-server space (`com.samsung.android.app.soundalive` / AudioService) and requires system signature permissions.
- **DualBeat Architecture Decision:** DualBeat detects and enumerates all connected Bluetooth audio devices, indicates whether the system currently routes to multiple sinks via system Dual Audio, and provides the **DualBeat Wi-Fi Multi-Speaker Receiver System** as the legitimate, cross-device multi-output solution.

---

## 5. DualBeat Wi-Fi Multi-Speaker (Cast) Protocol

### Discovery & Beaconing
- **UDP Port 8990:** DualBeat Cast Server broadcasts periodic discovery ping packets with magic header `0x44424554` (`DBET`).
- Receiver nodes on the same local subnet listen and respond with device name and unique node ID.

### Audio Streaming & Clock Synchronization
- **TCP Port 8991:** Control and audio streaming session.
- **Packet Format:**
  - `Magic Header` (4 bytes, `DBET`)
  - `Packet Type` (4 bytes, `TYPE_AUDIO_DATA`)
  - `Sequence Number` (8 bytes)
  - `Presentation Timestamp (PTS)` (8 bytes in milliseconds)
  - `Payload Size` (4 bytes)
  - `PCM Data` (N bytes, 16-bit stereo 44.1kHz)
- **Jitter Buffer:** Receiver node buffers packets up to the PTS target (`now + delta`), correcting network jitter and clock drift to deliver synchronized playback across multiple speakers.

---

## 6. Persistence & Scoped Storage Design

- **Database:** SQLite via AndroidX Room (`DualBeatDatabase`). Stores playlists, playlist items, user favorites, playback history, and saved receiver configurations.
- **Preferences:** Jetpack DataStore Preferences for theme selection, accent color, resume position, and gesture preferences.
- **Scoped Storage:** Scanned via `MediaStore.Audio.Media` and `MediaStore.Video.Media` without requiring full external storage permissions on Android 13+.
