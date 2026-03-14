/*
 * AmirSwitch - Smart Plug Firmware
 * ESP32 + HiveMQ Cloud MQTT
 *
 * Wiring:
 *   ESP32 GPIO27  -> Relay IN (signal)
 *   ESP32 VIN     <- HLK-PM01 5V out
 *   ESP32 GND     <- HLK-PM01 GND
 *   Relay VCC     <- HLK-PM01 5V out
 *   Relay GND     <- HLK-PM01 GND
 *   Relay COM     <- Mains LIVE in
 *   Relay NO      -> Output socket LIVE
 *   Mains NEUTRAL -> Output socket NEUTRAL (direct)
 *
 * SAFETY: Never work on this device while plugged into mains!
 */

#include <Arduino.h>
#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <WiFiUdp.h>
#include <NTPClient.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>

// ============================================================
// CONFIGURATION — Change these values before uploading!
// ============================================================
#define WIFI_SSID       "Aharoni 2.4"
#ifndef WIFI_PASSWORD
#define WIFI_PASSWORD   "YOUR_WIFI_PASSWORD"
#endif

#define MQTT_HOST       "0742ead081124a0fa2248931ff6032a1.s1.eu.hivemq.cloud"
#define MQTT_PORT       8883
#define MQTT_USER       "amirswitch"
#ifndef MQTT_PASS
#define MQTT_PASS       "your-mqtt-password"
#endif

#define DEVICE_ID       "device_001"

// Hardware pins
#define RELAY_PIN       27
#define LED_PIN         2   // Built-in LED on most ESP32 boards

// Timing
#define HEARTBEAT_INTERVAL_MS   30000   // 30 seconds
#define SCHEDULE_CHECK_INTERVAL_MS 10000 // 10 seconds
#define WIFI_RETRY_DELAY_MS     5000
#define MAX_WIFI_RETRIES        20
#define MQTT_RECONNECT_DELAY_MS 5000

// Default timezone offset (Israel = UTC+2), updated from schedule doc
#define DEFAULT_UTC_OFFSET_SECONDS 7200

// ============================================================
// ISRG Root X1 CA certificate (Let's Encrypt / HiveMQ Cloud)
// ============================================================
static const char ca_cert[] PROGMEM = R"EOF(
-----BEGIN CERTIFICATE-----
MIIFazCCA1OgAwIBAgIRAIIQz7DSQONZRGPgu2OCiwAwDQYJKoZIhvcNAQELBQAw
TzELMAkGA1UEBhMCVVMxKTAnBgNVBAoTIEludGVybmV0IFNlY3VyaXR5IFJlc2Vh
cmNoIEdyb3VwMRUwEwYDVQQDEwxJU1JHIFJvb3QgWDEwHhcNMTUwNjA0MTEwNDM4
WhcNMzUwNjA0MTEwNDM4WjBPMQswCQYDVQQGEwJVUzEpMCcGA1UEChMgSW50ZXJu
ZXQgU2VjdXJpdHkgUmVzZWFyY2ggR3JvdXAxFTATBgNVBAMTDElTUkcgUm9vdCBY
MTCCAiIwDQYJKoZIhvcNAQEBBQADggIPADCCAgoCggIBAK3oJHP0FDfzm54rVygc
h77ct984kIxuPOZXoHj3dcKi/vVqbvYATyjb3miGbESTtrFj/RQSa78f0uoxmyF+
0TM8ukj13Xnfs7j/EvEhmkvBioZxaUpmZmyPfjxwv60pIgbz5MDmgK7iS4+3mX6U
A5/TR5d8mUgjU+g4rk8Kb4Mu0UlXjIB0ttov0DiNewNwIRt18jA8+o+u3dpjq+sW
T8KOEUt+zwvo/7V3LvSye0rgTBIlDHCNAymg4VMk7BPZ7hm/ELNKjD+Jo2FR3qyH
B5T0Y3HsLuJvW5iB4YlcNHlsdu87kGJ55tukmi8mxdAQ4Q7e2RCOFvu396j3x+UC
B3FaSYAk7YkMXm5Uu0dtJv0JixiXaPb8HIJBqHmJIFtQ+UFj0gPBfCTM5BG2YhZ/
/Ww6ggIaSCEdEVB4xdFO0FrAIBnIFSQSKr0hMB+Z9IqTwOHpjQWi17EMT8qFpBz
QVTTaJsjHBqyJMjkFpsb/FpnWkOyLHatbkS/LGTh1coY82GUdocXB2KYULMMG4GD
n/Fh43j4DHHSU4fqDl/UpAMBOlGXRgz3Bj+3hVL2MFNHK+YvYqxgio4QME3Inby
j9IJOuqxQhDB3AO0hltS7oGCsJiQMB3Wt7GSGYcfx5JNoI0fSnGIVJm4U+8VTIW5
GMHNxjkjFU+MFXmGbNJeFRexhiNIeXGiwDaF3LBHYm3TTJkGjMTtJCP2L+FFCr/O
fpQlAgMBAAGjQjBAMA4GA1UdDwEB/wQEAwIBBjAPBgNVHRMBAf8EBTADAQH/MB0G
A1UdDgQWBBR5tFnm+7bl5AFzgAiIyBpY9umbbTANBgkqhkiG9w0BAQsFAAOCAgEA
Xnc0F2MSQPF8BVulRgel+Cf36VtBCk/m+QxLPm2NlR0YCOUF1yd4u+nABEH/Pqj6
s/pSz7sSqhLI6kNmHev8WAaxLuIT5A1mcFJRH2dPMMuiLBkJBiDflOoWBGeEF7GG
N2Td2XJFERPEtLMOxXyvn2ZRWS8B+ANJwJFHYpQIzifQ/VnalNzaGQxARxlLQ6NG
bGYknMJITkdJxI8hUgbJr1PAjkkEL7VRg+aRf2DhgEqhYL9DmqRAyuiP7VxbYqjz
VdA9yCjkSFI/rkWz+VREZnmiLlhMcshCPwKfYGCBRzohJMgbLWNP2IFDqCgny6cE
C6lJriW+sPDQ8JZJhfAj0ylW6bFmse7mXdLNJWIGdLOwIiHb7B0njwuqGr2JKVPZ
3LJqgVFp0Fl5GABuakbVCDBhely/XzPEJJmlkB2dFLrBF8n3HfCtZKtswSXi7GkD
m3n7Y7Z6kM2bWM3Gp8TkyLmFQxJqrpLhhNDmLSdamcWIHnWOCaLMj5mS/MFWO5e5
RPDTfLPmR1BIZTGA2II8K9R9xvMDwGDxFRUFj6TNxWHpcXSITVDsOLBTr7Q7bXk3
gFHDgSjPAqcxmC+7uPNmD+14MC/MVNCO6affiTSEqpnw8g3RCkXyH4jBnVfCT0u/
rDDC+nnQR5T6OJvYAKHF+w6s+sDQrv5AT5V+z/J/fGU=
-----END CERTIFICATE-----
)EOF";

// ============================================================
// MQTT Topics
// ============================================================
static const String PREFIX = String("amirswitch/") + DEVICE_ID;
static const String TOPIC_STATE       = PREFIX + "/state";
static const String TOPIC_STATE_SET   = PREFIX + "/state/set";
static const String TOPIC_ONLINE      = PREFIX + "/online";
static const String TOPIC_LAST_SEEN   = PREFIX + "/last_seen";
static const String TOPIC_SCHEDULES   = PREFIX + "/schedules";

// ============================================================
// Global objects
// ============================================================
WiFiClientSecure espClient;
PubSubClient mqttClient(espClient);

WiFiUDP ntpUDP;
NTPClient timeClient(ntpUDP, "pool.ntp.org",
                     DEFAULT_UTC_OFFSET_SECONDS, 60000);

// State
bool relayState = false;
unsigned long lastHeartbeat = 0;
unsigned long lastScheduleCheck = 0;
unsigned long lastMqttReconnect = 0;

// In-memory schedules from MQTT
DynamicJsonDocument schedulesDoc(4096);
int schedulesVersion = -1;
long utcOffsetSeconds = DEFAULT_UTC_OFFSET_SECONDS;

// ============================================================
// Function declarations
// ============================================================
void setupWiFi();
void setupMqtt();
void reconnectMqtt();
void mqttCallback(char* topic, byte* payload, unsigned int length);
void setRelay(bool on);
void publishState();
void sendHeartbeat();
void checkSchedules();
int getDayOfWeek();
void getTimeHHMM(int &hours, int &minutes);
bool isTimeInRange(
    int currentH, int currentM,
    int onH, int onM,
    int offH, int offM
);

// ============================================================
// Setup
// ============================================================
void setup() {
    Serial.begin(115200);
    Serial.println();
    Serial.println("===================================");
    Serial.println("  AmirSwitch - Smart Plug v2.0");
    Serial.println("===================================");

    pinMode(RELAY_PIN, OUTPUT);
    pinMode(LED_PIN, OUTPUT);
    digitalWrite(RELAY_PIN, LOW);
    digitalWrite(LED_PIN, LOW);

    setupWiFi();

    timeClient.begin();
    timeClient.update();
    Serial.print("Current time: ");
    Serial.println(timeClient.getFormattedTime());

    setupMqtt();

    Serial.println("Setup complete! Device is ready.");
    Serial.println("===================================");
}

// ============================================================
// Main loop
// ============================================================
void loop() {
    timeClient.update();

    if (!mqttClient.connected()) {
        unsigned long now = millis();
        if (now - lastMqttReconnect >= MQTT_RECONNECT_DELAY_MS) {
            lastMqttReconnect = now;
            reconnectMqtt();
        }
    }
    mqttClient.loop();

    unsigned long now = millis();
    if (now - lastHeartbeat >= HEARTBEAT_INTERVAL_MS) {
        sendHeartbeat();
        lastHeartbeat = now;
    }

    if (now - lastScheduleCheck >= SCHEDULE_CHECK_INTERVAL_MS) {
        checkSchedules();
        lastScheduleCheck = now;
    }

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
    while (WiFi.status() != WL_CONNECTED
           && retries < MAX_WIFI_RETRIES) {
        delay(WIFI_RETRY_DELAY_MS);
        Serial.print(".");
        retries++;
    }

    if (WiFi.status() == WL_CONNECTED) {
        Serial.println();
        Serial.println("WiFi connected!");
        Serial.print("IP address: ");
        Serial.println(WiFi.localIP());
        digitalWrite(LED_PIN, HIGH);
    } else {
        Serial.println();
        Serial.println("ERROR: WiFi connection failed! Restarting...");
        ESP.restart();
    }
}

// ============================================================
// MQTT Setup
// ============================================================
void setupMqtt() {
    Serial.println("Configuring MQTT...");

    espClient.setCACert(ca_cert);
    mqttClient.setServer(MQTT_HOST, MQTT_PORT);
    mqttClient.setBufferSize(2048);
    mqttClient.setCallback(mqttCallback);

    reconnectMqtt();
}

void reconnectMqtt() {
    if (WiFi.status() != WL_CONNECTED) {
        Serial.println("WiFi disconnected! Reconnecting WiFi...");
        digitalWrite(LED_PIN, LOW);
        setupWiFi();
    }

    Serial.print("Connecting to MQTT broker...");

    bool connected = mqttClient.connect(
        DEVICE_ID,
        MQTT_USER,
        MQTT_PASS,
        TOPIC_ONLINE.c_str(),
        1,      // QoS 1 for LWT
        true,   // retained
        "false"  // LWT payload: mark offline on disconnect
    );

    if (connected) {
        Serial.println(" connected!");

        // Publish online status
        mqttClient.publish(
            TOPIC_ONLINE.c_str(), "true", true
        );

        // Publish current relay state
        publishState();

        // Subscribe to command and schedule topics
        mqttClient.subscribe(TOPIC_STATE_SET.c_str(), 1);
        mqttClient.subscribe(TOPIC_SCHEDULES.c_str(), 1);

        Serial.println("Subscribed to: " + TOPIC_STATE_SET);
        Serial.println("Subscribed to: " + TOPIC_SCHEDULES);
    } else {
        Serial.print(" failed, rc=");
        Serial.println(mqttClient.state());
    }
}

// ============================================================
// MQTT Callback — Incoming messages
// ============================================================
void mqttCallback(
    char* topic, byte* payload, unsigned int length
) {
    String topicStr(topic);
    String payloadStr;
    payloadStr.reserve(length);
    for (unsigned int i = 0; i < length; i++) {
        payloadStr += (char)payload[i];
    }

    Serial.print("MQTT received [");
    Serial.print(topicStr);
    Serial.print("]: ");
    Serial.println(payloadStr);

    if (topicStr == TOPIC_STATE_SET) {
        bool newState = (payloadStr == "true");
        setRelay(newState);
        publishState();
    } else if (topicStr == TOPIC_SCHEDULES) {
        if (payloadStr.length() == 0) return;

        DynamicJsonDocument doc(4096);
        DeserializationError err = deserializeJson(doc, payloadStr);
        if (err) {
            Serial.print("Schedule JSON parse error: ");
            Serial.println(err.c_str());
            return;
        }

        int version = doc["version"] | -1;
        if (version <= schedulesVersion) {
            Serial.println("Schedule version unchanged, skipping");
            return;
        }

        schedulesVersion = version;
        schedulesDoc = doc;

        // Update timezone offset from document
        const char* tz = doc["timezone"];
        if (tz) {
            long offset = doc["utcOffsetSeconds"] | -1;
            if (offset >= 0) {
                utcOffsetSeconds = offset;
                timeClient.setTimeOffset(utcOffsetSeconds);
                Serial.print("Timezone offset updated: ");
                Serial.println(utcOffsetSeconds);
            }
        }

        Serial.print("Schedules updated, version=");
        Serial.println(schedulesVersion);
    }
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
    }
}

void publishState() {
    mqttClient.publish(
        TOPIC_STATE.c_str(),
        relayState ? "true" : "false",
        true  // retained
    );
}

// ============================================================
// Heartbeat — Report online status
// ============================================================
void sendHeartbeat() {
    if (WiFi.status() != WL_CONNECTED) {
        Serial.println("WiFi disconnected! Reconnecting...");
        digitalWrite(LED_PIN, LOW);
        setupWiFi();
    }

    if (mqttClient.connected()) {
        String epoch = String(timeClient.getEpochTime());
        mqttClient.publish(
            TOPIC_LAST_SEEN.c_str(),
            epoch.c_str(),
            true  // retained
        );
    }
}

// ============================================================
// Schedule Checker
// ============================================================
void checkSchedules() {
    if (schedulesVersion < 0) return;

    int currentH, currentM;
    getTimeHHMM(currentH, currentM);
    int currentDay = getDayOfWeek();

    JsonArray schedules = schedulesDoc["schedules"];
    if (schedules.isNull()) return;

    bool shouldBeOn = false;

    for (JsonObject sched : schedules) {
        if (!sched["enabled"].as<bool>()) continue;

        JsonArray days = sched["days"];
        bool dayMatch = false;
        for (JsonVariant day : days) {
            if (day.as<int>() == currentDay) {
                dayMatch = true;
                break;
            }
        }
        if (!dayMatch) continue;

        String onTimeStr = sched["onTime"].as<String>();
        String offTimeStr = sched["offTime"].as<String>();

        int onH = onTimeStr.substring(0, 2).toInt();
        int onM = onTimeStr.substring(3, 5).toInt();
        int offH = offTimeStr.substring(0, 2).toInt();
        int offM = offTimeStr.substring(3, 5).toInt();

        if (isTimeInRange(currentH, currentM,
                          onH, onM, offH, offM)) {
            shouldBeOn = true;
            break;
        }
    }

    if (relayState != shouldBeOn) {
        setRelay(shouldBeOn);
        publishState();
    }
}

// ============================================================
// Time Helpers
// ============================================================
int getDayOfWeek() {
    // NTP getDay() returns 0=Sunday, 1=Monday, ... 6=Saturday
    // We want 1=Monday ... 7=Sunday
    int day = timeClient.getDay();
    if (day == 0) return 7;
    return day;
}

void getTimeHHMM(int &hours, int &minutes) {
    hours = timeClient.getHours();
    minutes = timeClient.getMinutes();
}

bool isTimeInRange(
    int currentH, int currentM,
    int onH, int onM,
    int offH, int offM
) {
    int current = currentH * 60 + currentM;
    int onTime = onH * 60 + onM;
    int offTime = offH * 60 + offM;

    if (onTime <= offTime) {
        return current >= onTime && current < offTime;
    } else {
        return current >= onTime || current < offTime;
    }
}
