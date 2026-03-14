# AmirSwitch — Purchasing List

All components needed to build the AmirSwitch smart plug device.

---

## Summary

| # | Component | Qty | Est. Price |
|---|-----------|-----|------------|
| 1 | ESP32 DevKit V1 (30-pin) | 1 | $5–8 |
| 2 | 1-Channel 5V Relay Module (with optocoupler) | 1 | $1–3 |
| 3 | Hi-Link HLK-PM01 AC-DC Power Supply (220V → 5V) | 1 | $2–4 |
| 4 | 2-Pin Screw Terminal Blocks | 3 | $1 |
| 5 | Inline Fuse Holder + 2A Fuse (250V) | 1 | $1 |
| 6 | Panel-Mount Power Socket Type H (Israel) | 1 | $3–5 |
| 7 | Dupont Jumper Wires (male-to-female) | 1 pack | $1 |
| 8 | Standard 220V Power Cord with exposed leads | 1 | $2 |
| 9 | Micro-USB Cable (data capable) | 1 | $1 |
| 10 | M3 Screws + Nuts (8mm length) | 4 | $1 |
| | **Total** | | **~$18–27** |

---

## Detailed Purchase Links

### 1. ESP32 DevKit V1 (30-pin)

The brain of the device. WiFi + Bluetooth enabled microcontroller.

| Store | Link |
|-------|------|
| AliExpress (search) | https://www.aliexpress.com/w/wholesale-esp32-devkit-v1.html |
| AliExpress (specific listing) | https://www.aliexpress.com/item/1005005562548949.html |
| Amazon | https://www.amazon.com/ESP32-WROOM-32-Development-ESP-32S-Bluetooth-forArduino/dp/B08PCPJ12M |

> **What to look for:** Make sure it says "ESP32-WROOM-32" or "ESP32 DevKit V1" with 30 pins (15 per side). Comes with micro-USB connector for programming.

---

### 2. 1-Channel 5V Relay Module with Optocoupler

Switches the 220V AC mains power on/off. The optocoupler provides electrical isolation between the ESP32 (low voltage) and the mains (high voltage).

| Store | Link |
|-------|------|
| AliExpress (search) | https://www.aliexpress.com/w/wholesale-1-channel-5v-relay-module-for-arduino.html |
| AliExpress (specific listing) | https://www.aliexpress.com/item/1005006408997728.html |

> **What to look for:** Must say "with optocoupler" or "optoisolator". Look for the SRD-05VDC-SL-C relay. The board should have 3 low-voltage pins (VCC, GND, IN) and 3 high-voltage screw terminals (COM, NO, NC).

---

### 3. Hi-Link HLK-PM01 AC-DC Power Supply

Converts 220V AC mains to 5V DC to power the ESP32 and relay module. This is a certified, encapsulated module — much safer than building your own power supply.

| Store | Link |
|-------|------|
| AliExpress | https://www.aliexpress.com/item/33022245121.html |
| Amazon | https://www.amazon.com/HLK-PM01-Step-Down-Supply-Intelligent-Household/dp/B01B7GGL6C |

> **What to look for:** "HLK-PM01" specifically. Input: 100–240V AC. Output: 5V DC, 600mA. The green encapsulated module with 4 pins (AC in × 2, DC out × 2).

---

### 4. 2-Pin Screw Terminal Blocks (×3)

Used for safely connecting the mains wires (Live in, Live out, Neutral passthrough).

| Store | Link |
|-------|------|
| AliExpress (search) | https://www.aliexpress.com/w/wholesale-pcb-screw-terminal-block-connector.html |
| AliExpress (specific listing) | https://www.aliexpress.com/item/4000867583795.html |

> **What to look for:** 2-pin, 5mm pitch, PCB mount screw terminal blocks rated for at least 10A. Buy a pack of 10 (they're very cheap).

---

### 5. Inline Fuse Holder + 2A Fuse (250V)

Overcurrent protection. If something goes wrong, the fuse blows before any damage occurs.

| Store | Link |
|-------|------|
| AliExpress (search) | https://www.aliexpress.com/w/wholesale-2a-fuse-holder.html |
| AliExpress (specific listing) | https://www.aliexpress.com/item/32675535725.html |

> **What to look for:** Inline fuse holder for 5×20mm glass fuses + a 2A 250V glass fuse. Buy a few spare fuses.

---

### 6. Panel-Mount Power Socket — Type H (Israel)

The output socket where you plug your appliance. This mounts on the side of the enclosure.

| Store | Link |
|-------|------|
| Wise Electric (Israel) | https://wise-electric.co.il/product-category/plugs-sockets-adapters/socket-for-panel/israeli-socket-for-the-panel/ |
| Roi Hi-Tech (Israel) | https://roihitech.com/products/israeli-socket-type-h |
| International Config | https://internationalconfig.com/icc6.asp?item=77110-S |

> **What to look for:** "Type H" or "SI-32" standard Israeli socket. Must be panel-mount style (not wall-mount). Rated 16A 250V. Check the mounting dimensions to match the enclosure cutout (the OpenSCAD file uses a 42×42mm cutout — adjust if your socket is different).

---

### 7. Dupont Jumper Wires (male-to-female)

Connect the ESP32 pins to the relay module signal pins.

| Store | Link |
|-------|------|
| AliExpress (search) | https://www.aliexpress.com/w/wholesale-dupont-jumper-wire.html |
| AliExpress (specific listing) | https://www.aliexpress.com/item/32891879068.html |
| Amazon | https://www.amazon.com/HiLetgo-5x40pcs-Breadboard-Assortment-Arduino/dp/B077X99KX1 |

> **What to look for:** Male-to-female dupont wires. You only need 3 wires (VCC, GND, Signal) but they come in packs of 40. Get a mixed pack (M-F, M-M, F-F) — useful for other projects too.

---

### 8. Standard 220V Power Cord with Exposed Leads

The input cable — plugs into the wall on one end, connects to the terminal blocks on the other.

> **Where to buy:** Any local hardware/electrical store in Israel. Ask for a "כבל חשמל עם תקע" (power cable with plug). You need one with 3 wires exposed on the cut end (Live, Neutral, Earth).

---

### 9. Micro-USB Cable

For programming the ESP32. You likely already have one.

> **Important:** Must be a **data** cable, not a charge-only cable. If your ESP32 doesn't show up when plugged in, try a different cable.

---

### 10. M3 Screws + Nuts

For securing the enclosure lid to the bottom.

> **Where to buy:** Any hardware store or AliExpress. You need 4× M3 screws (8mm length) and 4× M3 nuts.

---

## Optional but Recommended

| Component | Purpose | Price |
|-----------|---------|-------|
| Breadboard | For prototyping before final assembly | $2 |
| Multimeter | For testing connections and continuity | $10–15 |
| Heat shrink tubing | For insulating wire connections | $2 |
| Hot glue gun | For securing components inside enclosure | $5 |
| Wire (14–16 AWG) | For internal mains connections | $2 |

---

## Notes

- All prices are approximate and may vary by seller/shipping
- AliExpress is cheapest but shipping takes 2–4 weeks
- Amazon is faster but more expensive
- The Type H socket is best purchased locally in Israel
- Buy a few extras of cheap components (terminal blocks, fuses) as spares
