/*
 * AmirSwitch - Smart Plug Firmware
 * ESP32 + Firebase Realtime Database
 *
 * Wiring:
 *   ESP32 GPIO27  → Relay IN (signal)
 *   ESP32 VIN     ← HLK-PM01 5V out
 *   ESP32 GND     ← HLK-PM01 GND
 *   Relay VCC     ← HLK-PM01 5V out
 *   Relay GND     ← HLK-PM01 GND
 *   Relay COM     ← Mains LIVE in
 *   Relay NO      → Output socket LIVE
 *   Mains NEUTRAL → Output socket NEUTRAL (direct)
 *
 * SAFETY: Never work on this device while plugged into mains!
 */

#include <Arduino.h>
#include <WiFi.h>
#include <WiFiUdp.h>
#include <NTPClient.h>
#include <FirebaseESP32.h>
#include <ArduinoJson.h>
#include <time.h>

// ============================================================
// CONFIGURATION — Change these values before uploading!
// ============================================================
#define WIFI_SSID       "YOUR_WIFI_SSID"
#define WIFI_PASSWORD   "YOUR_WIFI_PASSWORD"

#define FIREBASE_HOST   "your-project-id.firebaseio.com"
#define FIREBASE_AUTH   "your-firebase-database-secret"

#define DEVICE_ID       "device_001"

// Hardware pins
#define RELAY_PIN       27
#define LED_PIN         2   // Built-in LED on most ESP32 boards

// Timing
#define HEARTBEAT_INTERVAL_MS   30000   // 30 seconds
#define SCHEDULE_CHECK_INTERVAL_MS 10000 // 10 seconds
#define WIFI_RETRY_DELAY_MS     5000
#define MAX_WIFI_RETRIES        20

// Timezone offset (Israel = UTC+2, or UTC+3 in summer)
#define UTC_OFFSET_SECONDS      7200    // UTC+2 (change to 10800 for summer time)

// ============================================================
// Global objects
// ============================================================
FirebaseData firebaseData;
FirebaseData streamData;
FirebaseConfig firebaseConfig;
FirebaseAuth firebaseAuth;

WiFiUDP ntpUDP;
NTPClient timeClient(ntpUDP, "pool.ntp.org", UTC_OFFSET_SECONDS, 60000);

// State
bool relayState = false;
bool deviceOnline = false;
unsigned long lastHeartbeat = 0;
unsigned long lastScheduleCheck = 0;

// Firebase paths
String devicePath = String("/devices/") + DEVICE_ID;
String statePath = devicePath + "/state";
String schedulesPath = devicePath + "/schedules";
String onlinePath = devicePath + "/online";
String lastSeenPath = devicePath + "/lastSeen";

// ============================================================
// Function declarations
// ============================================================
void setupWiFi();
void setupFirebase();
void setRelay(bool on);
void streamCallback(StreamData data);
void streamTimeoutCallback(bool timeout);
void sendHeartbeat();
void checkSchedules();
int getDayOfWeek();
void getTimeHHMM(int &hours, int &minutes);
bool isTimeInRange(int currentH, int currentM, int onH, int onM, int offH, int offM);

// ============================================================
// Setup
// ============================================================
void setup() {
    Serial.begin(115200);
    Serial.println();
    Serial.println("===================================");
    Serial.println("  AmirSwitch - Smart Plug v1.0");
    Serial.println("===================================");

    // Initialize pins
    pinMode(RELAY_PIN, OUTPUT);
    pinMode(LED_PIN, OUTPUT);
    digitalWrite(RELAY_PIN, LOW);   // Start with relay OFF
    digitalWrite(LED_PIN, LOW);

    // Connect to WiFi
    setupWiFi();

    // Initialize NTP time
    timeClient.begin();
    timeClient.update();
    Serial.print("Current time: ");
    Serial.println(timeClient.getFormattedTime());

    // Connect to Firebase
    setupFirebase();

    // Set initial state in Firebase
    Firebase.setBool(firebaseData, statePath, false);
    Firebase.setBool(firebaseData, onlinePath, true);
    Firebase.setInt(firebaseData, lastSeenPath, timeClient.getEpochTime());

    // Start streaming for real-time state changes
    if (!Firebase.beginStream(streamData, statePath)) {
        Serial.println("ERROR: Could not begin Firebase stream");
        Serial.println(streamData.errorReason());
    } else {
        Serial.println("Firebase stream started on: " + statePath);
    }
    Firebase.setStreamCallback(streamData, streamCallback, streamTimeoutCallback);

    Serial.println("Setup complete! Device is ready.");
    Serial.println("===================================");
}

// ============================================================
// Main loop
// ============================================================
void loop() {
    // Update NTP time
    timeClient.update();

    // Send heartbeat every 30 seconds
    unsigned long now = millis();
    if (now - lastHeartbeat >= HEARTBEAT_INTERVAL_MS) {
        sendHeartbeat();
        lastHeartbeat = now;
    }

    // Check schedules every 10 seconds
    if (now - lastScheduleCheck >= SCHEDULE_CHECK_INTERVAL_MS) {
        checkSchedules();
        lastScheduleCheck = now;
    }

    // Small delay to prevent watchdog issues
    delay(100);
}

// ============================================================
// WiFi Setup
// ============================================================
void setupWiFi() {
    Serial.print("Connecting to WiFi: ");
    Serial.println(WIFI_SSID);

    WiFi.mode(WIFI_STA);
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

    int retries = 0;
    while (WiFi.status() != WL_CONNECTED && retries < MAX_WIFI_RETRIES) {
        delay(WIFI_RETRY_DELAY_MS);
        Serial.print(".");
        retries++;
    }

    if (WiFi.status() == WL_CONNECTED) {
        Serial.println();
        Serial.println("WiFi connected!");
        Serial.print("IP address: ");
        Serial.println(WiFi.localIP());
        digitalWrite(LED_PIN, HIGH);  // LED on = connected
    } else {
        Serial.println();
        Serial.println("ERROR: WiFi connection failed! Restarting...");
        ESP.restart();
    }
}

// ============================================================
// Firebase Setup
// ============================================================
void setupFirebase() {
    Serial.println("Connecting to Firebase...");

    firebaseConfig.host = FIREBASE_HOST;
    firebaseConfig.signer.tokens.legacy_token = FIREBASE_AUTH;

    Firebase.begin(&firebaseConfig, &firebaseAuth);
    Firebase.reconnectWiFi(true);

    // Set read/write timeout
    firebaseData.setResponseSize(1024);

    Serial.println("Firebase connected!");
}

// ============================================================
// Relay Control
// ============================================================
void setRelay(bool on) {
    if (relayState != on) {
        relayState = on;
        digitalWrite(RELAY_PIN, on ? HIGH : LOW);
        Serial.print("Relay switched: ");
        Serial.println(on ? "ON" : "OFF");

        // Update state in Firebase
        Firebase.setBool(firebaseData, statePath, on);
    }
}

// ============================================================
// Firebase Stream Callback — Real-time state changes from app
// ============================================================
void streamCallback(StreamData data) {
    if (data.dataType() == "boolean") {
        bool newState = data.boolData();
        Serial.print("Firebase stream received: ");
        Serial.println(newState ? "ON" : "OFF");
        setRelay(newState);
    }
}

void streamTimeoutCallback(bool timeout) {
    if (timeout) {
        Serial.println("Firebase stream timeout — reconnecting...");
    }
}

// ============================================================
// Heartbeat — Report online status
// ============================================================
void sendHeartbeat() {
    // Check WiFi and reconnect if needed
    if (WiFi.status() != WL_CONNECTED) {
        Serial.println("WiFi disconnected! Reconnecting...");
        digitalWrite(LED_PIN, LOW);
        setupWiFi();
    }

    Firebase.setBool(firebaseData, onlinePath, true);
    Firebase.setInt(firebaseData, lastSeenPath, timeClient.getEpochTime());
}

// ============================================================
// Schedule Checker
// ============================================================
void checkSchedules() {
    // Get current time
    int currentH, currentM;
    getTimeHHMM(currentH, currentM);
    int currentDay = getDayOfWeek(); // 1=Mon ... 7=Sun

    // Read all schedules from Firebase
    if (!Firebase.getJSON(firebaseData, schedulesPath)) {
        // No schedules or error — that's OK
        return;
    }

    // Parse the JSON
    String jsonStr = firebaseData.jsonString();
    if (jsonStr.length() == 0 || jsonStr == "null") {
        return;
    }

    DynamicJsonDocument doc(4096);
    DeserializationError error = deserializeJson(doc, jsonStr);
    if (error) {
        Serial.print("JSON parse error: ");
        Serial.println(error.c_str());
        return;
    }

    // Check each schedule
    bool shouldBeOn = false;

    JsonObject root = doc.as<JsonObject>();
    for (JsonPair schedule : root) {
        JsonObject sched = schedule.value().as<JsonObject>();

        // Skip disabled schedules
        if (!sched["enabled"].as<bool>()) {
            continue;
        }

        // Check if today is in the schedule's days
        JsonArray days = sched["days"].as<JsonArray>();
        bool dayMatch = false;
        for (JsonVariant day : days) {
            if (day.as<int>() == currentDay) {
                dayMatch = true;
                break;
            }
        }
        if (!dayMatch) continue;

        // Parse on/off times
        String onTimeStr = sched["onTime"].as<String>();
        String offTimeStr = sched["offTime"].as<String>();

        int onH = onTimeStr.substring(0, 2).toInt();
        int onM = onTimeStr.substring(3, 5).toInt();
        int offH = offTimeStr.substring(0, 2).toInt();
        int offM = offTimeStr.substring(3, 5).toInt();

        // Check if current time is within the on-off window
        if (isTimeInRange(currentH, currentM, onH, onM, offH, offM)) {
            shouldBeOn = true;
            break;  // At least one schedule says ON
        }
    }

    // Apply schedule decision
    setRelay(shouldBeOn);
}

// ============================================================
// Time Helpers
// ============================================================
int getDayOfWeek() {
    // NTP getDay() returns 0=Sunday, 1=Monday, ... 6=Saturday
    // We want 1=Monday ... 7=Sunday
    int day = timeClient.getDay();
    if (day == 0) return 7;  // Sunday = 7
    return day;              // Mon=1, Tue=2, ... Sat=6
}

void getTimeHHMM(int &hours, int &minutes) {
    hours = timeClient.getHours();
    minutes = timeClient.getMinutes();
}

bool isTimeInRange(int currentH, int currentM, int onH, int onM, int offH, int offM) {
    int current = currentH * 60 + currentM;
    int onTime = onH * 60 + onM;
    int offTime = offH * 60 + offM;

    if (onTime <= offTime) {
        // Normal range (e.g., 07:00 - 22:00)
        return current >= onTime && current < offTime;
    } else {
        // Overnight range (e.g., 22:00 - 06:00)
        return current >= onTime || current < offTime;
    }
}
