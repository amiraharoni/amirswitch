# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AmirSwitch is a DIY smart plug system with three components:
- **Android app** (`app/`) — Kotlin/Jetpack Compose app for controlling the smart plug
- **Firmware** (`firmware/`) — ESP32 Arduino firmware (PlatformIO)
- **Enclosure** (`enclosure/`) — OpenSCAD 3D-printable enclosure design

All three components communicate through **Firebase Realtime Database**. The app writes state/schedule changes, the ESP32 streams state changes and periodically reads schedules.

## Build Commands

### Android App
```bash
./gradlew assembleDebug          # Build debug APK
./gradlew installDebug           # Build and install on connected device
./gradlew build                  # Full build with checks
```
Requires `app/google-services.json` from Firebase Console (not checked in).

### Firmware
```bash
cd firmware
pio run                          # Compile firmware
pio run -t upload                # Compile and flash to ESP32
pio device monitor               # Serial monitor (115200 baud)
```
Before uploading, edit `firmware/src/main.cpp` to set `WIFI_SSID`, `WIFI_PASSWORD`, `FIREBASE_HOST`, `FIREBASE_AUTH`, and `DEVICE_ID`.

### Enclosure
Open `enclosure/amirswitch-enclosure.scad` in OpenSCAD. Toggle `show_top`/`show_bottom` to export each half as STL separately.

## Architecture

### Data Flow
```
Android App  ←→  Firebase Realtime Database  ←→  ESP32 Firmware
```
- App writes to `/devices/{deviceId}/state` (boolean) to toggle the relay
- ESP32 uses Firebase streaming to react to state changes in real time
- ESP32 reads `/devices/{deviceId}/schedules` every 10 seconds and sets the relay accordingly
- ESP32 writes heartbeats (`online`, `lastSeen`) every 30 seconds

### Android App Structure (MVVM)
- `MainActivity` → NavHost with three routes: `device`, `schedules`, `settings`
- `DeviceViewModel` — single shared ViewModel; holds all UI state as `StateFlow`s
- `FirebaseRepository` — all Firebase operations; uses anonymous auth and `callbackFlow` for real-time observation
- `Schedule` model — days use 1=Monday..7=Sunday; times in "HH:mm" format

### Firebase Database Schema
```
/devices/{deviceId}/
  state: boolean          # relay on/off
  online: boolean         # heartbeat status
  lastSeen: long          # epoch timestamp
  schedules/
    {pushId}/
      name: string
      onTime: "HH:mm"
      offTime: "HH:mm"
      days: [1-7]         # 1=Mon, 7=Sun
      enabled: boolean
```

### Firmware Key Details
- GPIO27 = relay pin, GPIO2 = status LED
- Relay control is idempotent — `setRelay()` only acts on state changes
- Schedule evaluation: any active schedule matching current day+time wins (OR logic)
- Supports overnight time ranges (e.g., 22:00–06:00)
- Timezone hardcoded to UTC+2 (Israel); change `UTC_OFFSET_SECONDS` for summer time

## Tech Stack
- **App**: Kotlin, Jetpack Compose, Material 3, Navigation Compose, Firebase Auth + Realtime Database, compileSdk 34, minSdk 26, JVM target 17
- **Firmware**: ESP32 (Arduino framework), PlatformIO, FirebaseESP32, ArduinoJson v7, NTPClient
- **Enclosure**: OpenSCAD (parametric design)
