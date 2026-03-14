Import("env")
import os

env_file = os.path.join(env["PROJECT_DIR"], "..", ".env")
if os.path.isfile(env_file):
    with open(env_file) as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, value = line.split("=", 1)
            key, value = key.strip(), value.strip()
            if key in ("MQTT_PASS", "WIFI_PASSWORD"):
                env.Append(CPPDEFINES=[(key, env.StringifyMacro(value))])
