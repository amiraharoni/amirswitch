# AmirSwitch — DIY Smart Plug

A WiFi-connected smart plug that lets you control any 220V appliance from your Android phone — from anywhere in the world. Set schedules, toggle power remotely, and monitor device status in real time.

---

## How It Works

```
┌──────────┐       ┌──────────┐       ┌──────────────┐       ┌─────────────┐
│  Android  │──────▶│ HiveMQ   │──────▶│    ESP32      │──────▶│   Relay     │
│   App     │ WiFi/ │  MQTT    │ WiFi  │  (AmirSwitch │ GPIO  │  (switches  │
│           │◀──────│  Cloud   │◀──────│   firmware)  │◀──────│  220V AC)   │
└──────────┘  4G   └──────────┘       └──────────────┘       └─────────────┘
                                                                     │
                                                              ┌──────┴──────┐
                                                              │  Appliance  │
                                                              │ (lamp, fan, │
                                                              │  heater...) │
                                                              └─────────────┘
```

1. You tap ON/OFF in the app (or a schedule triggers)
2. The command goes to HiveMQ Cloud Serverless (MQTT broker)
3. The ESP32 receives it instantly via an MQTT subscription
4. The ESP32 toggles GPIO27 → the relay clicks → power flows (or stops)
5. Works from anywhere — home WiFi, mobile data, another country

---

## Project Structure

```
amirswitch/
├── firmware/                    ESP32 Firmware (PlatformIO / Arduino)
│   ├── platformio.ini           Build config + library dependencies
│   └── src/
│       └── main.cpp             Full firmware source code
│
├── enclosure/                   3D-Printable Enclosure (OpenSCAD)
│   └── amirswitch-enclosure.scad   Parametric enclosure design
│
├── app/                         Android App (Kotlin + Jetpack Compose)
│   ├── build.gradle.kts         App build config
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/amirswitch/
│           ├── MainActivity.kt          Navigation + entry point
│           ├── viewmodel/
│           │   └── DeviceViewModel.kt   State management
│           ├── data/
│           │   ├── MqttRepository.kt      MQTT operations
│           │   └── models/Schedule.kt     Schedule data model
│           └── ui/
│               ├── theme/Theme.kt          Material 3 theme
│               └── screens/
│                   ├── DeviceScreen.kt     Power toggle + status
│                   ├── SchedulesScreen.kt  Schedule management
│                   └── SettingsScreen.kt   Device configuration
│
├── preview/                     Visual preview (HTML mockup)
│   └── index.html
│
├── docs/                        Documentation
│   ├── README.md                This file
│   └── PURCHASING-LIST.md       Component shopping list with links
│
├── build.gradle.kts             Root Gradle build file
├── settings.gradle.kts          Gradle settings
└── gradle.properties            Gradle properties
```

---

## Hardware

### Components

| Component | Model | Purpose |
|-----------|-------|---------|
| Microcontroller | ESP32 DevKit V1 (30-pin) | WiFi-connected brain |
| Relay | 1-Channel 5V with Optocoupler | Switches 220V AC |
| Power Supply | Hi-Link HLK-PM01 | Converts 220V AC → 5V DC |
| Terminal Blocks | 2-pin screw terminals (×3) | Safe mains wire connections |
| Fuse | 2A 250V inline fuse holder | Overcurrent protection |
| Output Socket | Panel-mount Type H (Israel) | Where appliance plugs in |
| Jumper Wires | Dupont male-to-female | ESP32 ↔ Relay connections |

> See [PURCHASING-LIST.md](PURCHASING-LIST.md) for full details and buy links.

### Wiring Diagram

```
Wall Outlet (220V AC)
    │
    ├── LIVE ──→ Fuse ──→ Relay COM ──→ Relay NO ──→ Output Socket LIVE
    │
    ├── NEUTRAL ──→ (direct) ──→ Output Socket NEUTRAL
    │
    └── LIVE + NEUTRAL ──→ HLK-PM01 ──→ 5V DC ──→ ESP32 VIN + Relay VCC
                                         GND   ──→ ESP32 GND + Relay GND

    ESP32 GPIO27 ──→ Relay IN (signal pin)
```

### Enclosure

- **File:** `enclosure/amirswitch-enclosure.scad`
- **Software:** [OpenSCAD](https://openscad.org/) (free, open source)
- **Dimensions:** 110mm × 68mm × 50mm
- **Design:** Two-part box (bottom + lid), M3 screw closure
- **Features:**
  - Cutouts for power cord, Type H socket, micro-USB, ventilation
  - Internal standoffs for ESP32, relay, and HLK-PM01
  - Safety divider wall separating high-voltage from low-voltage side

#### Print Settings

| Setting | Value |
|---------|-------|
| Material | PETG (preferred) or PLA |
| Layer Height | 0.2mm |
| Infill | 30% |
| Walls | 3 perimeters |
| Supports | None needed |

#### Exporting STL

1. Open `amirswitch-enclosure.scad` in OpenSCAD
2. To export **bottom only**: set `show_top = false;` at the top of the file
3. Press F6 (Render), then File → Export as STL
4. Repeat with `show_bottom = false; show_top = true;` for the lid

---

## Firmware

### Prerequisites

- [VS Code](https://code.visualstudio.com/) with [PlatformIO extension](https://platformio.org/install/ide?install=vscode)
- Or [Arduino IDE](https://www.arduino.cc/en/software) with ESP32 board support

### Configuration

Edit `firmware/src/main.cpp` and replace these values:

```cpp
#define WIFI_SSID       "YOUR_WIFI_SSID"
#define WIFI_PASSWORD   "YOUR_WIFI_PASSWORD"
#define MQTT_HOST       "your-cluster.s1.eu.hivemq.cloud"
#define MQTT_PORT       8883
#define MQTT_USER       "your-mqtt-username"
#define MQTT_PASS       "your-mqtt-password"
#define DEVICE_ID       "device_001"
```

### Flashing

1. Connect ESP32 via USB
2. Open the `firmware/` folder in VS Code with PlatformIO
3. Click the Upload button (→) or run `pio run --target upload`
4. Open Serial Monitor (115200 baud) to verify:
   - WiFi connection
   - MQTT broker connection
   - Relay toggling

### Firmware Features

- **Real-time control:** Subscribes to MQTT topic for instant ON/OFF response
- **Scheduling:** Checks schedules every 10 seconds against NTP time
- **Heartbeat:** Reports online status via MQTT every 30 seconds
- **Auto-reconnect:** Reconnects WiFi and MQTT if connection drops
- **LWT (Last Will):** Broker automatically marks device offline on unexpected disconnect
- **Timezone support:** Receives UTC offset from app via schedule document
- **Overnight schedules:** Supports schedules that cross midnight (e.g., 22:00–06:00)
- **TLS:** Connects to HiveMQ Cloud Serverless over TLS 1.2 with ISRG Root X1 CA certificate

---

## Android App

### Prerequisites

- [Android Studio](https://developer.android.com/studio) (latest version)
- Android device or emulator (API 26+)

### MQTT Setup

1. Go to [HiveMQ Cloud Serverless](https://console.hivemq.cloud/)
2. Create a free cluster
3. Note your cluster URL (e.g., `your-cluster.s1.eu.hivemq.cloud`)
4. Create MQTT credentials (username + password)
5. Update the credentials in `MqttRepository.kt`

### Building the App

1. Open the project root in Android Studio
2. Wait for Gradle sync to complete
3. Run on device/emulator

### App Screens

#### 1. Device Screen (Main)
- Large animated power button (green = ON, gray = OFF)
- Device online/offline indicator with pulsing green dot
- Last seen timestamp when device is offline
- Quick access to Schedules and Settings

#### 2. Schedules Screen
- List of all schedules with:
  - Name
  - ON/OFF times
  - Active days (Mon–Sun chips)
  - Enable/disable toggle
- Add new schedule with + FAB button
- Edit schedule by tapping on it
- Delete with confirmation dialog
- Time picker (24-hour format)

#### 3. Settings Screen
- Device ID configuration (must match firmware `DEVICE_ID`)
- "How It Works" info card
- App version

### MQTT Topic Schema

| Topic | Direction | QoS | Retained | Payload |
|-------|-----------|-----|----------|---------|
| `amirswitch/{deviceId}/state` | ESP32 → App | 0 | Yes | `"true"` / `"false"` |
| `amirswitch/{deviceId}/state/set` | App → ESP32 | 1 | No | `"true"` / `"false"` |
| `amirswitch/{deviceId}/online` | ESP32 (LWT) | 1 | Yes | `"true"` / `"false"` |
| `amirswitch/{deviceId}/last_seen` | ESP32 → App | 0 | Yes | epoch integer string |
| `amirswitch/{deviceId}/schedules` | App → ESP32 | 1 | Yes | JSON schedule document |

### Schedule Document

The app publishes the full schedule document as a retained MQTT message. The ESP32 subscribes and receives it into RAM.

```json
{
  "version": 12,
  "updatedAt": 1712345678,
  "timezone": "Asia/Jerusalem",
  "utcOffsetSeconds": 7200,
  "schedules": [
    {
      "id": "a1b2c3d4",
      "name": "Morning Heater",
      "onTime": "07:00",
      "offTime": "08:30",
      "days": [1, 2, 3, 4, 5],
      "enabled": true
    }
  ]
}
```

**Day numbers:** 1 = Monday, 2 = Tuesday, ... 7 = Sunday

---

## Step-by-Step Build Guide

### Step 1 — Set Up HiveMQ Cloud Serverless
1. Create a free HiveMQ Cloud Serverless cluster
2. Create MQTT credentials (username + password)
3. Note your cluster URL and port (8883)

### Step 2 — Flash the ESP32
1. Install PlatformIO in VS Code
2. Edit WiFi and MQTT credentials in `main.cpp`
3. Flash firmware to ESP32
4. Verify via Serial Monitor that it connects to WiFi and MQTT

### Step 3 — Test Manually
1. Install [MQTTX](https://mqttx.app/) desktop client
2. Connect to your HiveMQ Cloud Serverless cluster
3. Publish `true` to `amirswitch/device_001/state/set` → hear relay click ON
4. Publish `false` → hear relay click OFF

### Step 4 — Build the Android App
1. Open project in Android Studio
2. Update MQTT credentials in `MqttRepository.kt`
3. Build and install on your phone
4. Toggle the power button → verify relay responds

### Step 5 — Print the Enclosure
1. Open `amirswitch-enclosure.scad` in OpenSCAD
2. Export bottom and top as separate STL files
3. Print with PETG, 0.2mm layer height, 30% infill

### Step 6 — Final Assembly
1. **UNPLUG everything from mains!**
2. Mount ESP32 on standoffs with M2 screws
3. Mount relay module on standoffs
4. Press-fit HLK-PM01 into retaining walls
5. Wire according to wiring diagram:
   - Mains Live → Fuse → Relay COM
   - Relay NO → Output socket Live
   - Mains Neutral → Output socket Neutral (direct)
   - HLK-PM01 AC in ← Mains Live + Neutral
   - HLK-PM01 5V out → ESP32 VIN + Relay VCC
   - HLK-PM01 GND → ESP32 GND + Relay GND
   - ESP32 GPIO27 → Relay IN
6. Close enclosure with M3 screws
7. Test with a lamp (low-power appliance first!)

---

## Safety

> **WARNING: This project involves 220V AC mains voltage, which can cause serious injury or death. Follow all safety guidelines carefully.**

- **NEVER** work on the device while it's plugged into mains
- The relay module **MUST** have an optocoupler (isolates ESP32 from mains)
- Use the HLK-PM01 (certified AC-DC converter) — do **NOT** build your own
- All 220V connections **MUST** use screw terminal blocks, never bare wires or solder
- The 3D-printed enclosure **MUST** fully cover all mains-voltage wiring when closed
- The 2A inline fuse provides overcurrent protection
- Maximum appliance load: 10A × 250V = **2,200W** (check your relay rating)
- The safety divider wall inside the enclosure separates high-voltage from low-voltage
- Use PETG filament for the enclosure (better heat resistance than PLA)
- If you're unsure about any electrical connection, **ask an electrician for help**

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| ESP32 doesn't connect to WiFi | Check SSID/password in firmware. Ensure 2.4GHz network (ESP32 doesn't support 5GHz) |
| ESP32 not found on USB | Try a different micro-USB cable (must be a data cable, not charge-only) |
| MQTT connection fails | Verify MQTT_HOST, MQTT_USER, and MQTT_PASS in firmware. Check HiveMQ Cloud Serverless console for cluster status |
| App shows "Device Offline" | Check ESP32 serial monitor. Verify WiFi connection. Check MQTT broker connectivity |
| Relay doesn't click | Verify wiring: GPIO27 → Relay IN, 5V → Relay VCC, GND → Relay GND |
| Schedule doesn't trigger | Check timezone offset in schedule document. Verify NTP time in serial monitor |
| App can't toggle device | Verify MQTT credentials in MqttRepository.kt. Check HiveMQ Cloud Serverless cluster is running |

---

## License

This is a personal DIY project. Use at your own risk. The author is not responsible for any damage, injury, or loss resulting from building or using this device.
