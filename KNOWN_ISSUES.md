# DUALBEAT — KNOWN ISSUES & PLATFORM CONSTRAINTS

**Document Version:** 1.0.0  
**Updated:** November 2026  
**Auditor:** DualBeat Core Engineering Team  

---

## 1. Multi-Bluetooth Audio Limitations (Standard Android vs. One UI)

- **Issue:** Third-party Android apps cannot simultaneously force A2DP packet replication to two arbitrary Bluetooth MAC addresses using public Android SDK APIs (`AudioManager`, `AudioTrack`).
- **Reason:** The Android Bluetooth stack (Fluoride/GD) and Audio HAL restrict third-party applications to a single active A2DP audio route.
- **Samsung One UI Solution:** Samsung Galaxy A24 provides "Dual Audio" in its system Quick Settings. Users with two connected Bluetooth speakers must enable both devices in **Quick Panel → Media Output**.
- **Alternative:** DualBeat provides **Wi-Fi Multi-Speaker (Cast)** to stream synchronized audio across multiple devices without Bluetooth limitations.

---

## 2. System Audio Capture Restrictions (DRM & App Policies)

- **Issue:** Audio from certain streaming applications (e.g. Netflix, Spotify) cannot be captured using `AudioPlaybackCaptureConfiguration`.
- **Reason:** Android security policies strictly forbid capturing audio from applications that declare `android:allowAudioPlaybackCapture="false"` in their manifest or use DRM-protected media (`FLAG_SECURE`).
- **Status:** By design; platform security cannot be bypassed. DualBeat displays an explanatory notice when non-capturable audio is encountered.

---

## 3. Battery Optimization & Background Execution

- **Issue:** On some Android vendor skins (including aggressive battery modes), background services may be paused if the user minimizes the app.
- **Solution:** DualBeat runs `DualBeatMediaSessionService` and `DualBeatCastService` as high-priority foreground services with ongoing notifications and wake locks. Users are advised to set DualBeat battery usage to **Unrestricted** in Android App Info if they experience stutter during extended background sessions.

---

## 4. Subtitle Encoding Auto-Detection

- **Issue:** External subtitle files (`.srt`) encoded in legacy ANSI or non-UTF8 encodings may occasionally display corrupted characters.
- **Solution:** Standardize subtitles to UTF-8 before loading. Media3 ExoPlayer provides native UTF-8 and WebVTT support.
