# DUALBEAT — DEVICE COMPATIBILITY & HARDWARE SPECIFICATION

**Primary Target Device:** Samsung Galaxy A24 (SM-A245F)  
**Chipset:** MediaTek Helio G99 (6nm, Octa-core: 2x 2.2 GHz Cortex-A76 & 6x 2.0 GHz Cortex-A55)  
**GPU:** Mali-G57 MC2  
**Display:** 6.5" Super AMOLED, 90Hz, 1080 x 2340 pixels (~396 ppi)  
**Bluetooth Version:** Bluetooth 5.3, A2DP, LE  
**Wi-Fi Version:** Wi-Fi 802.11 a/b/g/n/ac, dual-band, Wi-Fi Direct  
**Operating System:** Android 13/14/15/16 with Samsung One UI  

---

## 1. Samsung Galaxy A24 Hardware Characteristics

### A. Display & AMOLED Mode
- The Galaxy A24 features a Super AMOLED display capable of true black pixel shutoff.
- DualBeat's default **AMOLED theme** uses `#000000` background to minimize battery consumption and take advantage of the 90Hz refresh rate.

### B. Bluetooth 5.3 & Audio Routing
- Bluetooth 5.3 supports high data throughput and lower connection latency.
- **Samsung One UI Dual Audio:** Supports connecting two Bluetooth audio sinks simultaneously through Samsung's system audio framework.
  - *Location:* Quick Panel → Media Output → Check both Bluetooth devices.
  - *App Visibility:* DualBeat detects when two active A2DP outputs are registered with `AudioManager` and reports `Samsung Dual Audio is ACTIVE`.

### C. MediaTek Helio G99 Codec Support
- Hardware video decoding: H.264 (AVC), H.265 (HEVC), VP9 up to 1080p @ 60fps.
- DualBeat's Media3 configuration falls back to software decoders when encountering uncommon formats (e.g., AV1 4K, 10-bit VP9) to ensure the application never crashes.

---

## 2. General Android Compatibility Matrix

| Android Version | API Level | Compatibility | Notes |
| :--- | :---: | :---: | :--- |
| **Android 8.0 - 8.1 (Oreo)** | 26–27 | Supported (minSdk) | Classic permissions, background playback limits. |
| **Android 9.0 (Pie)** | 28 | Supported | AudioFocus ducking improvements. |
| **Android 10 (Q)** | 29 | Supported | `AudioPlaybackCaptureConfiguration` available. |
| **Android 11 (R)** | 30 | Supported | Scoped storage enforcement. |
| **Android 12 - 12L (S)** | 31–32 | Supported | `BLUETOOTH_CONNECT` & `BLUETOOTH_SCAN` permissions. |
| **Android 13 (Tiramisu)** | 33 | Supported | `READ_MEDIA_AUDIO`, `READ_MEDIA_VIDEO`, `POST_NOTIFICATIONS`. |
| **Android 14 (Upside Down Cake)** | 34 | Supported | Foreground service type enforcement. |
| **Android 15 (Vanilla Ice Cream)** | 35 | Supported (targetSdk) | Edge-to-edge layout enforcement. |
| **Android 16 (Baklava / One UI 8.5)**| 36 | Fully Supported | Modernized system audio routing & predictive back. |
