# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AmirSwitch is a DIY smart plug system with three components:
- **Android app** (`app/`) — Kotlin/Jetpack Compose app for controlling the smart plug
- **Firmware** (`firmware/`) — ESP32 Arduino firmware (PlatformIO)
- **Enclosure** (`enclosure/`) — OpenSCAD 3D-printable enclosure design

All three components communicate through **HiveMQ Cloud Serverless (MQTT)**. The app publishes commands and schedules, the ESP32 subscribes and reacts in real time.

## Build Commands

### Android App
```bash
./gradlew assembleDebug          # Build debug APK
./gradlew installDebug           # Build and install on connected device
./gradlew build                  # Full build with checks
```

### Firmware
```bash
cd firmware
pio run                          # Compile firmware
pio run -t upload                # Compile and flash to ESP32
pio device monitor               # Serial monitor (115200 baud)
```
Before uploading, edit `firmware/src/main.cpp` to set `WIFI_SSID`, `WIFI_PASSWORD`, `MQTT_HOST`, `MQTT_PORT`, `MQTT_USER`, `MQTT_PASS`, and `DEVICE_ID`.

### Enclosure
Open `enclosure/amirswitch-enclosure.scad` in OpenSCAD. Toggle `show_top`/`show_bottom` to export each half as STL separately.

## Architecture

### Data Flow
```
Android App  <-->  HiveMQ Cloud MQTT  <-->  ESP32 Firmware
```
- App publishes to `amirswitch/{deviceId}/state/set` to toggle the relay
- ESP32 subscribes to `state/set`, toggles relay, publishes confirmed state to `state` (retained)
- App publishes full schedule document to `amirswitch/{deviceId}/schedules` (retained)
- ESP32 subscribes to `schedules`, receives into RAM, evaluates locally every 10 seconds
- ESP32 uses LWT to set `online` to `"false"` on unexpected disconnect
- ESP32 publishes heartbeats to `last_seen` every 30 seconds

### MQTT Topic Schema
| Topic | Direction | QoS | Retained | Payload |
|-------|-----------|-----|----------|---------|
| `amirswitch/{deviceId}/state` | ESP32 -> App | 0 | Yes | `"true"` / `"false"` |
| `amirswitch/{deviceId}/state/set` | App -> ESP32 | 1 | No | `"true"` / `"false"` |
| `amirswitch/{deviceId}/online` | ESP32 (LWT) | 1 | Yes | `"true"` / `"false"` |
| `amirswitch/{deviceId}/last_seen` | ESP32 -> App | 0 | Yes | epoch integer string |
| `amirswitch/{deviceId}/schedules` | App -> ESP32 | 1 | Yes | JSON schedule document |

### Schedule Document Payload
```json
{
  "version": 12,
  "updatedAt": 1712345678,
  "timezone": "Asia/Jerusalem",
  "utcOffsetSeconds": 7200,
  "schedules": [
    {"id": "uuid", "name": "Work hours", "onTime": "08:00", "offTime": "18:00", "days": [1,2,3,4,5], "enabled": true}
  ]
}
```

### Android App Structure (MVVM)
- `MainActivity` -> NavHost with three routes: `device`, `schedules`, `settings`
- `DeviceViewModel` — single shared ViewModel; holds all UI state as `StateFlow`s
- `MqttRepository` — all MQTT operations; uses HiveMQ MQTT Client with auto-reconnect
- `Schedule` model — days use 1=Monday..7=Sunday; times in "HH:mm" format

### Firmware Key Details
- GPIO27 = relay pin, GPIO2 = status LED
- Relay control is idempotent — `setRelay()` only acts on state changes
- Schedule evaluation: any active schedule matching current day+time wins (OR logic)
- Supports overnight time ranges (e.g., 22:00-06:00)
- Timezone received from schedule document's `utcOffsetSeconds` field

## Tech Stack
- **App**: Kotlin, Jetpack Compose, Material 3, Navigation Compose, HiveMQ MQTT Client 1.3.3, compileSdk 36, minSdk 26, JVM target 21
- **Firmware**: ESP32 (Arduino framework), PlatformIO, PubSubClient, ArduinoJson v6, NTPClient, WiFiClientSecure (TLS with ISRG Root X1 CA)
- **Cloud**: HiveMQ Cloud Serverless (free tier MQTT broker)
- **Enclosure**: OpenSCAD (parametric design)
