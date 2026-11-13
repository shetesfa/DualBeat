# DUALBEAT — BUILD AND INSTALLATION GUIDE

**Application ID:** `com.tesfa.dualbeat`  
**Version:** 1.0.0 (Version Code: 1)  
**Target:** Android 16 / One UI 8.5  
**JDK:** OpenJDK 17  
**Build Tool:** Gradle with Android Gradle Plugin 8.7.2  

---

## 1. Prerequisites

1. **Java Development Kit (JDK):** OpenJDK 17 (e.g. Eclipse Temurin 17).
2. **Android SDK:** Installed with `platforms;android-35` or `android-36` and `build-tools;35.0.0`.
3. **Android Studio:** Ladybug / Meerkat (2024.2+) or standalone Gradle.
4. **Target Device:** Samsung Galaxy A24 (SM-A245F) or any Android device running Android 8.0+ (API 26+).

---

## 2. Building the Application

### A. Debug Build (Recommended for Development & Testing)
Open a terminal in the project root directory (`D:\mx app`):

```bash
# On Windows (PowerShell):
.\gradlew assembleDebug

# On Linux / macOS:
./gradlew assembleDebug
```

**Output APK Location:**
`app/build/outputs/apk/debug/app-debug.apk`

### B. Running Unit Tests
To execute all local unit tests (FormatUtils, Protocol, Sorting, Curves):

```bash
.\gradlew testDebugUnitTest
```

**Test Report Location:**
`app/build/reports/tests/testDebugUnitTest/index.html`

### C. Release Build
To build a release APK:

```bash
.\gradlew assembleRelease
```

**Output APK Location:**
`app/build/outputs/apk/release/app-release-unsigned.apk` (or signed with debug key if configured).

---

## 3. Installing on Samsung Galaxy A24

1. **Enable Developer Options on Device:**
   - Go to **Settings → About phone → Software information**.
   - Tap **Build number** 7 times until "Developer mode has been enabled" appears.
2. **Enable USB Debugging:**
   - Go to **Settings → Developer options**.
   - Turn on **USB debugging**.
3. **Connect Device via USB:**
   - Plug the phone into your PC with a high-quality USB-C cable.
   - Accept the "Allow USB debugging?" prompt on the phone screen.
4. **Install via ADB:**

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

5. **Launch Application:**
   - Launch DualBeat directly from the phone app drawer or via ADB:

```bash
adb shell am start -n com.tesfa.dualbeat.debug/com.tesfa.dualbeat.MainActivity
```

---

## 4. Troubleshooting Build Issues

- **JDK Version Incompatibility:** Ensure `JAVA_HOME` points to JDK 17. Run `java -version` to verify.
- **Gradle Daemon Memory:** Configured in `gradle.properties` (`-Xmx2048m`). Increase if building large media caches.
- **Offline Mode:** If building offline, ensure dependencies are cached in `~/.gradle/caches`.
