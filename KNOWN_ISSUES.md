# DUALBEAT — KNOWN ISSUES, PLATFORM LIMITATIONS & REPRODUCTION STEPS

**Document Version:** 2.0.0 (Post-Correction Edition)  
**Updated:** November 2026  
**Auditor:** DualBeat Core Engineering Team  

---

## 1. Multi-Bluetooth Audio Output (Standard Android vs. Samsung One UI)

### Issue Description
Third-party Android applications cannot command the Android Bluetooth stack to split or duplicate A2DP audio packets simultaneously to two arbitrary Bluetooth MAC addresses using public Android SDK APIs (`AudioManager`, `AudioTrack`, `MediaRouter`).

### Root Cause
1. Standard Android OS Bluetooth architecture (Fluoride/GD) routes media audio (`STREAM_MUSIC`) exclusively to a single active Bluetooth A2DP endpoint at a time.
2. While Samsung Galaxy A24 (SM-A245F) supports Bluetooth 5.3 and features "Dual Audio" in One UI, this functionality is implemented entirely in Samsung's private system daemon (`com.samsung.android.app.soundalive` / AudioService) and requires system-level signature permissions.
3. No public Android API exists to activate or query whether Dual Audio is actively splitting PCM streams.

### Reproduction Steps
1. Pair two independent Bluetooth speakers or headphones (e.g. Speaker A and Speaker B) in Android Settings.
2. Open DualBeat Speakers screen. Both devices are listed as paired.
3. Start audio playback in DualBeat.
4. Observe audio playing only from the single primary Bluetooth device selected by the Android audio HAL.

### Mitigation & Workaround
- **System-Level Fix:** Swipe down the notification shade, expand Samsung's **Quick Panel**, tap **Media Output**, and select both connected Bluetooth devices.
- **DualBeat Implementation:** DualBeat accurately classifies this state as `SYSTEM_MANAGED` instead of reporting a fake "ACTIVE" status, and provides an immediate button to launch `Settings.ACTION_BLUETOOTH_SETTINGS`.
- **Platform-Independent Solution:** Users can use **DualBeat Cast (Wi-Fi Multi-Speaker)** to stream synchronized audio across multiple devices over local Wi-Fi without Bluetooth restrictions.

---

## 2. System Audio Capture Restrictions (DRM & Application Opt-Out)

### Issue Description
Audio from certain third-party applications (such as Netflix, Spotify, or banking apps) cannot be captured using `AudioPlaybackCaptureConfiguration` in `SystemAudioCaptureService`.

### Root Cause
1. `AudioPlaybackCaptureConfiguration` (introduced in Android 10) enforces strict platform privacy rules.
2. Applications that declare `android:allowAudioPlaybackCapture="false"` in their `AndroidManifest.xml` are legally and technically exempt from recording.
3. Any media stream rendered using DRM protection or `FLAG_SECURE` surfaces is silenced at the OS HAL level during capture.

### Reproduction Steps
1. Launch DualBeat and activate "Broadcast System Audio" in the Speakers screen.
2. Grant the Android MediaProjection consent dialog.
3. Open a DRM-protected streaming app or an app that opts out of capture.
4. Observe captured audio stream output is muted or silent.

### Mitigation & Workaround
- This limitation is strictly enforced by the Android platform security sandbox and cannot be bypassed by any non-rooted third-party application.
- DualBeat handles this gracefully without throwing uncaught exceptions, and notifies the user of the platform restriction.

---

## 3. Wi-Fi Multi-Speaker (DualBeat Cast) Network Jitter on Crowded 2.4GHz Networks

### Issue Description
On congested 2.4GHz Wi-Fi networks with high packet loss or severe latency fluctuations, receiver devices may experience occasional audio buffer underruns.

### Root Cause
DualBeat Cast streams raw 16-bit stereo PCM audio (176.4 KB/s) over local TCP. When Wi-Fi packet latency exceeds the jitter buffer threshold (400ms), the non-blocking receiver queue empties before new packets arrive.

### Reproduction Steps
1. Connect transmitter and receiver phones to a congested 2.4GHz Wi-Fi router under heavy local network traffic.
2. Start Wi-Fi Cast transmitter and receiver modes.
3. Introduce substantial radio interference or distance from the router.
4. Small audio micro-stutters may occur as the jitter buffer re-aligns.

### Mitigation & Workaround
- DualBeat's `ConcurrentLinkedQueue` bounded jitter buffer prunes stale packets and re-synchronizes when packet timestamps drift.
- Connect devices to 5GHz Wi-Fi or a local mobile hotspot for minimum latency (< 25ms) and stutter-free multi-speaker streaming.

---

## 4. Battery Optimization / Background Throttling on Aggressive OEM Skins

### Issue Description
On aggressive battery optimization profiles (e.g. Extreme Power Saving or aggressive background app limits), the operating system may suspend the Wi-Fi streaming socket when the app remains backgrounded with the screen turned off for prolonged durations.

### Reproduction Steps
1. Enable aggressive power saving mode in system settings.
2. Start Wi-Fi Cast broadcast and turn off the device screen.
3. Leave the device idle for 30+ minutes.
4. Android Doze mode may throttle CPU or suspend Wi-Fi wake locks.

### Mitigation & Workaround
- DualBeat holds a `PARTIAL_WAKE_LOCK` and runs `DualBeatMediaSessionService` / `DualBeatCastService` as high-priority foreground services with ongoing notifications.
- Users are advised to set DualBeat battery usage to **Unrestricted** in Android App Info if they intend to run continuous multi-hour Wi-Fi broadcast sessions.
