# DUALBEAT — SOFTWARE ARCHITECTURE SPECIFICATION

**Project:** DualBeat  
**Architect:** Tesfa / DualBeat Core Engineering Team  
**Platform:** Native Android (100% Kotlin)  
**Target Hardware:** Samsung Galaxy A24 (SM-A245F) / Android 16 / One UI 8.5 (minSdk 26, targetSdk 35)  
**Revision:** Post-Correction Refactored Architecture (November 2026)  

---

## 1. Architectural Philosophy & Layered Decomposition

DualBeat adheres strictly to Clean Architecture combined with Model-View-ViewModel (MVVM) and Unidirectional Data Flow (UDF).

```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                       │
│    Jetpack Compose UI (Material 3) + StateFlow Observers    │
│    Screens: Home, Music, Video, Speakers, Sound, Split      │
│    Contextual Permission Rationales & HUD Overlays          │
└──────────────────────────────▲──────────────────────────────┘
                               │ Observes UI State / Dispatches Intents
┌──────────────────────────────┴──────────────────────────────┐
│                     VIEWMODEL LAYER                         │
│    HomeViewModel, MusicViewModel, VideoViewModel,           │
│    SpeakersViewModel, SoundStudioViewModel, SettingsVM      │
└──────────────────────────────▲──────────────────────────────┘
                               │ Interacts with Singletons & Controllers
┌──────────────────────────────┴──────────────────────────────┐
│             SINGLETON CORE CONTROLLER LAYER                 │
│  ┌─────────────────────────┐     ┌───────────────────────┐  │
│  │ MusicPlaybackController │     │ MultiBluetoothEngine  │  │
│  │ (MediaController Client)│     │ (Routing & Detection) │  │
│  └────────────┬────────────┘     └───────────────────────┘  │
│               │ Media3 Session Protocol                     │
│  ┌────────────▼───────────────────────────────────────────┐  │
│  │            DualBeatMediaSessionService                 │  │
│  │  (SOLE OWNER of ExoPlayer, MediaSession, AudioFocus)   │  │
│  └────────────────────────────────────────────────────────┘  │
└──────────────────────────────▲──────────────────────────────┘
                               │ Repositories, Databases, Drivers
┌──────────────────────────────┴──────────────────────────────┐
│             DATA & HARDWARE CONTROL LAYER                   │
│  ┌───────────────────────┐       ┌───────────────────────┐  │
│  │   LOCAL PERSISTENCE   │       │     WI-FI RECEIVER    │  │
│  │   • Room Database     │       │   • DualBeatCastSvr   │  │
│  │   • DataStore Prefs   │       │   • DualBeatCastCli   │  │
│  │   • Scoped MediaStore │       │   • Jitter Buffer     │  │
│  └───────────────────────┘       └───────────────────────┘  │
│  ┌───────────────────────┐       ┌───────────────────────┐  │
│  │   AUDIO & HARDWARE    │       │     SYSTEM CAPTURE    │  │
│  │   • AudioFX Engine    │       │   • MediaProjection   │  │
│  │   • BluetoothManager  │       │   • AudioRecord PCM   │  │
│  │   • Window/Audio Mgr  │       │   • Pipe to Cast Svr  │  │
│  └───────────────────────┘       └───────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. Corrected Unified Playback Architecture (Rule 2)

### Single Player Ownership Pattern
- **Service Ownership:** `DualBeatMediaSessionService` is the **exclusive owner** of the AndroidX Media3 `ExoPlayer` instance. It handles audio focus requests (`AUDIOFOCUS_GAIN`), audio becoming noisy (`ACTION_AUDIO_BECOMING_NOISY`), foreground service notifications, and system `MediaSession` lifecycle.
- **Client Binding:** `MusicPlaybackController` acts strictly as an asynchronous `MediaController` client. It establishes a `SessionToken` connection to `DualBeatMediaSessionService`.
- **Zero Audio Interruption on UI Lifecycle:** When activities or ViewModels are destroyed and recreated (e.g. during screen rotation, split-screen toggling, or navigation), the media playback service continues unaffected. Recreated ViewModels simply rebind to the active session via `MediaController`.
- **Dynamic AudioFX Binding:** `DualBeatApp` observes the active audio session ID from the single player and dynamically connects `SoundStudioEngine` effects without recreating effect audio sessions.

---

## 3. Bluetooth Routing & Samsung One UI Reality (Rule 3)

### Multi-Bluetooth Route Inspection
- **Hardware Topology:** Enumerate bonded Bluetooth devices via `BluetoothAdapter.bondedDevices` and determine connected A2DP profile state using `BluetoothProfile.A2DP.getConnectedDevices()`.
- **Active Sink Inspection:** Query `AudioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)` to inspect physically active audio routing endpoints.
- **Honest Status Classification:**
  - `0 connected A2DP devices`: -> `DualAudioDetectionStatus.NOT_CONNECTED`
  - `1 connected A2DP device`: -> `DualAudioDetectionStatus.SINGLE_OUTPUT_ACTIVE`
  - `2+ connected A2DP devices`: -> `DualAudioDetectionStatus.SYSTEM_MANAGED`
  - False-positive "ACTIVE" states are strictly forbidden. Android third-party apps cannot control or force dual A2DP packet replication via public SDK APIs.
  - Users are provided with an actionable deep-link button directly launching `Settings.ACTION_BLUETOOTH_SETTINGS` or Samsung One UI's **Media Output** panel.

---

## 4. DualBeat Wi-Fi Multi-Speaker (Cast) Pipeline (Rule 4)

### Capture & Streaming Topology
```
[Local Audio Playback / MediaProjection]
                   │
                   ▼ (16-bit Stereo PCM @ 44.1kHz)
   [SystemAudioCaptureService]
                   │
                   ▼ broadcastAudioPcm(buffer)
       [DualBeatCastServer] 
         (TCP Server : 8991)
                   │
         [Wi-Fi Local Subnet]
                   │
                   ▼ TCP Socket Stream
       [DualBeatCastClient]
                   │
                   ▼
     [ConcurrentLinkedQueue Jitter Buffer]
       • Sequence number validation
       • Packet size boundary check (<= 64KB)
       • Latency / PTS alignment
                   │
                   ▼ Non-blocking poll()
        [Android AudioTrack Output]
```

- **Jitter Buffer Architecture:** Replaced deprecated `Thread.sleep` hacks with a non-blocking `ConcurrentLinkedQueue` buffer bounded at 20 frames (400ms buffer). Oldest frames are safely pruned on network overflow, and packet sequence gaps are logged.
- **Protocol Framing:** Packets are framed with a 28-byte binary header containing Magic `0x44424554`, packet type, 64-bit sequence number, 64-bit presentation timestamp (PTS), and 32-bit payload size.

---

## 5. Video Player System Controls (Rule 6)

- **Compose AndroidView:** Hosts `androidx.media3.ui.PlayerView` in a lightweight Compose surface.
- **Brightness Drag:** Left 50% of the screen captures vertical swipe drag gestures. The calculated float value (0.01f to 1.0f) is applied directly to `activity.window.attributes.screenBrightness`.
- **Volume Drag:** Right 50% of the screen captures vertical swipe drag gestures. The calculated integer index is applied directly via `AudioManager.setStreamVolume(STREAM_MUSIC, volume, 0)`.
- **Horizontal Scrubbing Seek:** Drag gestures across the horizontal axis calculate relative seek offsets (`+/- deltaMs`) and dispatch seek updates via `ExoPlayer.seekTo()`.
- **HUD Feedback:** Gestures trigger reactive animated cards displaying real-time percentage indicators.

---

## 6. Scoped Storage & Contextual Permissions (Rule 8)

- **MediaStore Querying:** Android 10+ (API 29+) compatible. Uses `MediaStore.MediaColumns.RELATIVE_PATH`, `BUCKET_DISPLAY_NAME`, and `ContentUris.withAppendedId()`. Legacy `_DATA` file paths are eliminated.
- **Contextual Permission Architecture:**
  - Media permissions (`READ_MEDIA_AUDIO`, `READ_MEDIA_VIDEO` on API 33+; `READ_EXTERNAL_STORAGE` on API <= 32) are requested contextually within library screens (Home, Music, Video).
  - Bluetooth permissions (`BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN` on API 31+) are requested contextually within the Speakers screen.
  - In-app permission cards explain the specific reason for access and provide direct links to Android system application settings if permissions were permanently denied.
