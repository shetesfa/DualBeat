# DUALBEAT — PERMISSIONS AND PRIVACY ARCHITECTURE

**Application Name:** DualBeat  
**Developer:** Tesfa (Founder & Developer)  
**Security Model:** 100% On-Device Local Processing (Zero Telemetry)  

---

## 1. Privacy Policy Summary

DualBeat operates under a strict **Zero-Data-Collection Principle**:
1. **No External Network Tracking:** DualBeat does not connect to analytics services, advertising networks, crash reporting servers, or external cloud databases.
2. **Local Storage Only:** All media metadata, user playlists, playback history, equalizer custom presets, and favorites are stored exclusively inside the application's private SQLite Room database (`dualbeat_database.db`) on the device.
3. **No Account Required:** Full playback, audio effects, and multi-speaker capabilities work without an account, login, or internet access.
4. **Local Network Communication Only:** The Wi-Fi Cast feature communicates strictly within the user's private local subnet (e.g. 192.168.x.x) using peer-to-peer UDP and TCP sockets. No audio is ever routed through external cloud relays.

---

## 2. Android Permissions Audit & Justification

| Permission | API Level | Protection Level | Technical Justification |
| :--- | :---: | :---: | :--- |
| `READ_MEDIA_AUDIO` | 33+ | Dangerous (User prompt) | Required to discover and index local music files in `MediaStore.Audio`. |
| `READ_MEDIA_VIDEO` | 33+ | Dangerous (User prompt) | Required to discover and index local video files in `MediaStore.Video`. |
| `READ_EXTERNAL_STORAGE` | <= 32 | Dangerous (User prompt) | Backward-compatible storage access for Android 12 and earlier devices. |
| `POST_NOTIFICATIONS` | 33+ | Dangerous (User prompt) | Required to display background playback controls and media progress on Android 13+. |
| `BLUETOOTH_CONNECT` | 31+ | Dangerous (User prompt) | Required to query bonded Bluetooth devices, speaker names, and connection states. |
| `BLUETOOTH_SCAN` | 31+ | Dangerous (`neverForLocation`) | Discovers nearby Bluetooth audio sinks without tracking the user's geographic location. |
| `INTERNET` | All | Normal | Used strictly for local Wi-Fi multi-speaker socket streaming on the LAN. |
| `ACCESS_NETWORK_STATE` | All | Normal | Checks whether the device is connected to a local Wi-Fi network before initiating Cast. |
| `ACCESS_WIFI_STATE` | All | Normal | Inspects local Wi-Fi link status to determine broadcast subnet. |
| `CHANGE_WIFI_MULTICAST_STATE` | All | Normal | Allows receiving UDP discovery beacon packets from remote receiver nodes. |
| `FOREGROUND_SERVICE` | 28+ | Normal | Keeps audio playback and Wi-Fi streaming alive when the screen is locked. |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | 34+ | Normal | Declares media playback foreground service type in compliance with Android 14+ rules. |
| `FOREGROUND_SERVICE_MEDIA_PROJECTION` | 34+ | Normal | Declares system audio capture foreground service type. |
| `RECORD_AUDIO` | All | Dangerous | Used by `AudioPlaybackCaptureConfiguration` to forward non-DRM media audio. |
| `WAKE_LOCK` | All | Normal | Prevents the CPU from going into deep sleep during active music playback. |
| `MODIFY_AUDIO_SETTINGS` | All | Normal | Allows the Equalizer and Sound Studio to apply frequency filters to the audio stream. |

---

## 3. Scoped Storage Compliance

- DualBeat fully respects Android Scoped Storage.
- It does **not** request broad `MANAGE_EXTERNAL_STORAGE` (`ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION`).
- Media items are referenced strictly via content URIs (`content://media/external/audio/media/123`), preserving application sandboxing and file security.
