# DUALBEAT — DEVICE COMPATIBILITY & HARDWARE SPECIFICATION

**Primary Target Device:** Samsung Galaxy A24 (SM-A245F)  
**Chipset:** MediaTek Helio G99 (6nm, Octa-core: 2x 2.2 GHz Cortex-A76 & 6x 2.0 GHz Cortex-A55)  
**GPU:** Mali-G57 MC2  
**Display:** 6.5" Super AMOLED, 90Hz, 1080 x 2340 pixels (~396 ppi)  
**Bluetooth Version:** Bluetooth 5.3, A2DP, LE  
**Wi-Fi Version:** Wi-Fi 802.11 a/b/g/n/ac, dual-band, Wi-Fi Direct  
**Operating System:** Android 14/15/16 with Samsung One UI  
**minSdk:** 26 (Android 8.0 Oreo)  
**targetSdk:** 35 (Android 15 Vanilla Ice Cream / Android 16 Preview)  

---

## 1. Samsung Galaxy A24 Hardware Characteristics

### A. Display & AMOLED Mode
- The Galaxy A24 features a Super AMOLED display capable of true black pixel shutoff.
- DualBeat's default **AMOLED theme** uses `#000000` background to minimize battery consumption and take full advantage of the 90Hz refresh rate.
- Status bar and navigation bar conform to modern edge-to-edge system insets.

### B. Bluetooth 5.3 & Multi-Audio Reality
- Bluetooth 5.3 hardware supports stable wireless connectivity with lower power drain.
- **Samsung One UI Dual Audio:** Supports connecting two Bluetooth audio sinks simultaneously through Samsung's system-level proprietary framework.
  - *Setup Procedure:* Quick Panel → Media Output → Select both connected Bluetooth devices.
  - *Engineering Truth:* Third-party Android SDK applications have **no public API** to force dual A2DP stream splitting or query whether the Samsung audio HAL is actively duplicating packets.
  - *DualBeat Behavior:* DualBeat enumerates connected audio endpoints and classifies multiple Bluetooth devices as `SYSTEM_MANAGED`. It never shows a false "ACTIVE" badge without physical verification, and provides a one-tap shortcut to system Bluetooth settings.

### C. MediaTek Helio G99 Codec Support
- Hardware video decoding: H.264 (AVC), H.265 (HEVC), VP9 up to 1080p @ 60fps.
- DualBeat's Media3 configuration falls back to software decoders when encountering uncommon formats (e.g., AV1 4K, 10-bit VP9) to ensure uninterrupted playback without crashes.

---

## 2. General Android Compatibility Matrix

| Android Version | API Level | Compatibility | Notes |
| :--- | :---: | :---: | :--- |
| **Android 8.0 - 8.1 (Oreo)** | 26–27 | Supported (minSdk) | Classic permissions, background playback limits. |
| **Android 9.0 (Pie)** | 28 | Supported | AudioFocus ducking improvements. |
| **Android 10 (Q)** | 29 | Supported | `AudioPlaybackCaptureConfiguration` available; Scoped Storage introduced. |
| **Android 11 (R)** | 30 | Supported | Scoped storage enforcement; MediaStore query optimizations. |
| **Android 12 - 12L (S)** | 31–32 | Supported | Runtime `BLUETOOTH_CONNECT` & `BLUETOOTH_SCAN` permissions. |
| **Android 13 (Tiramisu)** | 33 | Supported | Contextual `READ_MEDIA_AUDIO`, `READ_MEDIA_VIDEO`, `POST_NOTIFICATIONS`. |
| **Android 14 (Upside Down Cake)** | 34 | Supported | Foreground service type enforcement (`mediaPlayback`, `mediaProjection`). |
| **Android 15 (Vanilla Ice Cream)** | 35 | Supported (targetSdk) | Edge-to-edge layout enforcement, modern predictive back animation. |
| **Android 16 (Baklava / One UI 8.5)**| 36 | Fully Supported | Modernized system audio routing & predictive back gestures. |
