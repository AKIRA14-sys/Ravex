# AKIRA RAVEX — COMPLETE AI GAMING COMMAND CENTER

![AKIRA RAVEX Sharingan Reference](app/src/main/res/drawable/ravex_sharingan_ref.jpg)

**AKIRA RAVEX** is a high-performance, native Android gaming command center and companion platform built in **Kotlin** with **Jetpack Compose**. It provides a landscape-first **Sharingan UI** gaming interface, **GameForge** installed game launcher, persistent draggable **Floating RAVEX HUD Bubble Overlay**, multi-provider **AI Provider Center** (Groq, OpenRouter, Google Gemini, Custom OpenAI), 200+ visual preset **Crosshair Engine**, **Smart Lag Detection**, **AI Camera** setup reviewer, **AI Voice Commands**, **Gaming Session Coach**, **AI Troubleshooter**, **Smart Network Diagnosis**, **Phone Health Telemetry**, **Thermal Guard**, and local crash diagnostics (**RavexGuard**).

---

## 🌟 Key Features & Architecture

1. **Landscape Sharingan UI & GameForge Launcher**:
   - Primary gaming interface optimized for horizontal/landscape mobile orientation (`sensorLandscape`).
   - Animated rotating 3-tomoe Sharingan Eye centerpiece with pulsing crimson radial glow and top telemetry header.
   - **GameForge Launcher**: Detects real installed Android applications using `PackageManager` with genuine icons and launch intents. Includes Game Mode switcher (`BALANCED`, `PERFORMANCE`, `ULTRA`) and per-game target crosshair assignment.

2. **Persistent Floating RAVEX HUD Overlay**:
   - Draggable floating Wolf bubble using Android `SYSTEM_ALERT_WINDOW` (`Settings.canDrawOverlays`).
   - Tap-to-expand compact floating panel providing quick navigation to all 11 HUD modules (**Copilot**, **Smart Lag Diagnosis**, **Game Profiles**, **Crosshair Assistant**, **AI Camera**, **Voice Commands**, **Session Coach**, **AI Troubleshooter**, **Network Diagnosis**, **Phone Health**, **HUD Customizer**).

3. **AI Provider Center (Groq, OpenRouter, Gemini, Custom OpenAI)**:
   - Hardware-encrypted key storage backed by **Android KeyStore** and `EncryptedSharedPreferences` (`RavexSecurity`).
   - **Automatic Model Discovery**: Live model catalog retrieval from official provider endpoints (`api.groq.com`, `openrouter.ai`, `generativelanguage.googleapis.com`), JSON parsing, offline caching, search filtering, and test connection checks.
   - **Unified AI Request Router**: Supports default and feature-specific provider/model selection, vision model routing, request timeouts, and fallback routing.

4. **RAVEX AI Gaming Copilot**:
   - Conversational assistant with context injection (live free RAM %, battery temp °C, game mode, active target game).
   - Local chat history storage, clipboard copy, retry, and suggested prompts.

5. **Smart Lag Detection & Network Diagnosis**:
   - Honest telemetry analysis (RAM pressure %, battery temperature °C, thermal throttle level).
   - Real ping probes (`8.8.8.8`) estimating min/avg/max latency and jitter variation.
   - Evidence-based AI explanation summary.

6. **AI Camera Setup Reviewer**:
   - CameraX integration with live preview, photo capture/review, explicit user consent privacy gate, Base64 encoding, and vision model query for ergonomics, desk setup, lighting, or photographed graphics menus.

7. **AI Voice Commands & Session Coach**:
   - Push-to-talk speech recognition (`SpeechRecognizer`), voice navigation commands, confirmation feedback, and text fallback.
   - **Session Coach**: Session duration timer, hydration/break alerts, session history, and AI post-match coaching feedback.

8. **Smart Crosshair Engine (200+ Presets)**:
   - Over **200+ distinct visual crosshair designs** across Tactical FPS, Cyberpunk, Minimal, Dynamic, and Custom categories.
   - Canvas-based customizer for shape, color, outline, size, stroke, gap, opacity, center dot, animated pulse, and rotation.
   - Strictly visual canvas overlay (no aimbot, auto-aim, game memory modification, or anti-cheat violation).

9. **Hardware Safety & Phone Health**:
   - Pure read-only telemetry for RAM, refresh rate, battery temperature, voltage, and thermal status.
   - **Hardware Safety**: Does NOT overclock, undervolt, modify kernel parameters, or alter device hardware.

---

## 🛠️ Tech Stack

- **Language**: Kotlin 1.9.22
- **UI Framework**: Jetpack Compose (Material 3 & Custom Canvas Drawing)
- **Target SDK**: 34 (Android 14) / Min SDK: 26
- **Build System**: Gradle 8.8 with Kotlin DSL
- **Key Libraries**: CameraX (`androidx.camera`), Security Crypto (`EncryptedSharedPreferences`), OkHttp, Gson, Coroutines Android.

---

## 📁 Compiled APK Binary
The compiled debug APK binary is committed directly in the repository at:
`apk/AKIRA-RAVEX-debug.apk` (~13MB)

### Rebuilding Debug APK
```bash
./gradlew assembleDebug --no-daemon
```

### Running Unit Tests
```bash
./gradlew test --no-daemon
```

---

## 🔒 Privacy & Policy Notice
- **Visual Overlay Only**: RAVEX Crosshair is a visual canvas overlay. It does NOT automatically aim, track targets, detect enemy heads, modify game memory, or bypass game anti-cheat systems.
- **Read-Only Telemetry**: RAVEX Phone Health is a monitoring readout system. It does NOT overclock, undervolt, modify kernel settings, or alter device hardware.
- **Secure Credentials**: API keys are encrypted on-device via Android KeyStore and are never logged, transmitted to third-party tracking services, or committed to Git.
