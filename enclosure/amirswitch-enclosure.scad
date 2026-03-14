/*
 * AmirSwitch - Smart Plug Enclosure
 * Parametric 3D-printable enclosure for ESP32 + Relay + HLK-PM01
 *
 * Print Settings:
 *   Material: PETG (preferred) or PLA
 *   Layer height: 0.2mm
 *   Infill: 30%
 *   Walls: 3 perimeters
 *   Supports: None needed
 *
 * To export STL:
 *   1. Open this file in OpenSCAD
 *   2. Press F5 to preview
 *   3. Press F6 to render
 *   4. File → Export as STL
 *   5. Export bottom and top separately by commenting/uncommenting
 *       the show_top / show_bottom variables below
 */

// ============================================================
// DISPLAY CONTROL — Toggle which part to show/export
// ============================================================
show_bottom = true;
show_top = true;
exploded_view = true;  // Set false for assembled view
explode_distance = 30; // mm gap in exploded view

// ============================================================
// ENCLOSURE DIMENSIONS
// ============================================================
// Outer box
box_length = 110;   // mm (X)
box_width = 68;     // mm (Y)
box_height = 50;    // mm (Z) — total height (bottom + top)
wall = 2.5;         // mm wall thickness
bottom_height = 32; // mm — height of bottom part
top_height = box_height - bottom_height;

// Corner radius
corner_r = 4;

// ============================================================
// COMPONENT DIMENSIONS (approximate, measure yours!)
// ============================================================
// ESP32 DevKit V1 (30-pin)
esp_length = 51;
esp_width = 28;
esp_height = 10;    // with pins
esp_x = 6;          // offset from inner left wall
esp_y = 5;          // offset from inner front wall

// Relay module (1-channel with optocoupler)
relay_length = 43;
relay_width = 17;
relay_height = 18;
relay_x = 6;
relay_y = 38;

// HLK-PM01 power supply
hlk_length = 34;
hlk_width = 20;
hlk_height = 15;
hlk_x = 65;
hlk_y = 10;

// ============================================================
// CUTOUT DIMENSIONS
// ============================================================
// Input power cord hole (left side)
cord_hole_dia = 12;
cord_hole_z = 12;   // center height from bottom

// Output socket cutout (right side) — Type H panel mount
socket_cutout_w = 42;
socket_cutout_h = 42;

// USB access (front side, for ESP32 programming)
usb_cutout_w = 12;
usb_cutout_h = 8;
usb_cutout_z = 6;

// Ventilation slots
vent_slot_w = 2;
vent_slot_h = 12;
vent_slot_spacing = 6;
vent_count = 5;

// ============================================================
// SCREW MOUNTS
// ============================================================
screw_dia = 3.2;     // M3 screw clearance
screw_head_dia = 6;
screw_post_dia = 7;
screw_post_height = bottom_height - wall;

// Screw positions (from outer corners)
screw_inset = 8;

// ============================================================
// STANDOFFS for components
// ============================================================
standoff_dia = 5;
standoff_height = 4;
standoff_hole_dia = 2; // for M2 screws

// ============================================================
// MODULES
// ============================================================

// Rounded rectangle
module rounded_rect(l, w, h, r) {
    hull() {
        translate([r, r, 0]) cylinder(h=h, r=r, $fn=30);
        translate([l-r, r, 0]) cylinder(h=h, r=r, $fn=30);
        translate([r, w-r, 0]) cylinder(h=h, r=r, $fn=30);
        translate([l-r, w-r, 0]) cylinder(h=h, r=r, $fn=30);
    }
}

// Screw post
module screw_post(h, outer_d, inner_d) {
    difference() {
        cylinder(h=h, d=outer_d, $fn=20);
        translate([0, 0, -0.1])
            cylinder(h=h+0.2, d=inner_d, $fn=20);
    }
}

// Component standoff
module standoff(h, outer_d, hole_d) {
    difference() {
        cylinder(h=h, d=outer_d, $fn=20);
        translate([0, 0, -0.1])
            cylinder(h=h+0.2, d=hole_d, $fn=20);
    }
}

// Ventilation slots
module vent_slots(count, slot_w, slot_h, spacing, wall_t) {
    for (i = [0:count-1]) {
        translate([i * spacing, 0, 0])
            cube([slot_w, wall_t + 0.2, slot_h]);
    }
}

// ============================================================
// BOTTOM HALF
// ============================================================
module bottom() {
    difference() {
        // Outer shell
        rounded_rect(box_length, box_width, bottom_height, corner_r);

        // Hollow inside
        translate([wall, wall, wall])
            rounded_rect(box_length - 2*wall, box_width - 2*wall,
                         bottom_height, corner_r - wall/2);

        // === CUTOUTS ===

        // Input power cord hole (left side)
        translate([-0.1, box_width/2, cord_hole_z])
            rotate([0, 90, 0])
                cylinder(d=cord_hole_dia, h=wall+0.2, $fn=30);

        // Output socket cutout (right side)
        translate([box_length - wall - 0.1,
                   (box_width - socket_cutout_w) / 2,
                   2])
            cube([wall + 0.2, socket_cutout_w, socket_cutout_h]);

        // USB access cutout (front side)
        translate([esp_x + wall + (esp_length - usb_cutout_w) / 2,
                   -0.1,
                   usb_cutout_z + wall])
            cube([usb_cutout_w, wall + 0.2, usb_cutout_h]);

        // Ventilation slots (back side)
        total_vent_width = vent_count * vent_slot_w + (vent_count - 1) * (vent_slot_spacing - vent_slot_w);
        translate([(box_length - total_vent_width) / 2,
                   box_width - wall - 0.1,
                   bottom_height - vent_slot_h - 5])
            vent_slots(vent_count, vent_slot_w, vent_slot_h, vent_slot_spacing, wall);

        // Ventilation slots (top of bottom, for heat from HLK-PM01)
        // (These will be on the inner floor near the HLK module)

        // Screw holes through bottom corners
        translate([screw_inset, screw_inset, -0.1])
            cylinder(d=screw_dia, h=wall+0.2, $fn=20);
        translate([box_length-screw_inset, screw_inset, -0.1])
            cylinder(d=screw_dia, h=wall+0.2, $fn=20);
        translate([screw_inset, box_width-screw_inset, -0.1])
            cylinder(d=screw_dia, h=wall+0.2, $fn=20);
        translate([box_length-screw_inset, box_width-screw_inset, -0.1])
            cylinder(d=screw_dia, h=wall+0.2, $fn=20);
    }

    // === INTERNAL FEATURES ===

    // Screw posts (4 corners)
    translate([screw_inset, screw_inset, wall])
        screw_post(screw_post_height, screw_post_dia, screw_dia);
    translate([box_length-screw_inset, screw_inset, wall])
        screw_post(screw_post_height, screw_post_dia, screw_dia);
    translate([screw_inset, box_width-screw_inset, wall])
        screw_post(screw_post_height, screw_post_dia, screw_dia);
    translate([box_length-screw_inset, box_width-screw_inset, wall])
        screw_post(screw_post_height, screw_post_dia, screw_dia);

    // ESP32 standoffs (4 corners of ESP32 footprint)
    translate([wall + esp_x, wall + esp_y, wall])
        standoff(standoff_height, standoff_dia, standoff_hole_dia);
    translate([wall + esp_x + esp_length, wall + esp_y, wall])
        standoff(standoff_height, standoff_dia, standoff_hole_dia);
    translate([wall + esp_x, wall + esp_y + esp_width, wall])
        standoff(standoff_height, standoff_dia, standoff_hole_dia);
    translate([wall + esp_x + esp_length, wall + esp_y + esp_width, wall])
        standoff(standoff_height, standoff_dia, standoff_hole_dia);

    // Relay standoffs (2 mounting holes)
    translate([wall + relay_x, wall + relay_y, wall])
        standoff(standoff_height, standoff_dia, standoff_hole_dia);
    translate([wall + relay_x + relay_length, wall + relay_y, wall])
        standoff(standoff_height, standoff_dia, standoff_hole_dia);

    // HLK-PM01 retaining walls (3 sides, friction fit)
    translate([wall + hlk_x, wall + hlk_y, wall])
        cube([2, hlk_width, hlk_height]);
    translate([wall + hlk_x + hlk_length, wall + hlk_y, wall])
        cube([2, hlk_width, hlk_height]);
    translate([wall + hlk_x, wall + hlk_y, wall])
        cube([hlk_length, 2, hlk_height]);

    // Divider wall between mains side (HLK/relay) and ESP32
    // This adds safety by separating high voltage from low voltage
    translate([wall + 58, wall, wall])
        cube([2, box_width - 2*wall, bottom_height * 0.5]);
}

// ============================================================
// TOP HALF (LID)
// ============================================================
module top() {
    lip = 1.5;  // inner lip for alignment
    lip_h = 3;

    difference() {
        union() {
            // Outer shell
            rounded_rect(box_length, box_width, top_height, corner_r);

            // Inner lip for alignment (fits inside bottom)
            translate([wall + 0.3, wall + 0.3, top_height - 0.1])
                rounded_rect(box_length - 2*wall - 0.6,
                             box_width - 2*wall - 0.6,
                             lip_h, corner_r - wall/2);
        }

        // Hollow inside
        translate([wall, wall, wall])
            rounded_rect(box_length - 2*wall, box_width - 2*wall,
                         top_height + lip_h, corner_r - wall/2);

        // Screw holes
        translate([screw_inset, screw_inset, -0.1])
            cylinder(d=screw_dia, h=top_height + 0.2, $fn=20);
        translate([box_length-screw_inset, screw_inset, -0.1])
            cylinder(d=screw_dia, h=top_height + 0.2, $fn=20);
        translate([screw_inset, box_width-screw_inset, -0.1])
            cylinder(d=screw_dia, h=top_height + 0.2, $fn=20);
        translate([box_length-screw_inset, box_width-screw_inset, -0.1])
            cylinder(d=screw_dia, h=top_height + 0.2, $fn=20);

        // Screw head countersink
        translate([screw_inset, screw_inset, -0.1])
            cylinder(d=screw_head_dia, h=2, $fn=20);
        translate([box_length-screw_inset, screw_inset, -0.1])
            cylinder(d=screw_head_dia, h=2, $fn=20);
        translate([screw_inset, box_width-screw_inset, -0.1])
            cylinder(d=screw_head_dia, h=2, $fn=20);
        translate([box_length-screw_inset, box_width-screw_inset, -0.1])
            cylinder(d=screw_head_dia, h=2, $fn=20);

        // Top ventilation slots
        total_vent_width = vent_count * vent_slot_w + (vent_count - 1) * (vent_slot_spacing - vent_slot_w);
        translate([(box_length - total_vent_width) / 2,
                   (box_width - total_vent_width) / 2,
                   -0.1])
            for (i = [0:vent_count-1]) {
                translate([i * vent_slot_spacing, 0, 0])
                    cube([vent_slot_w, box_width/2, wall + 0.2]);
            }

        // Label text (debossed)
        translate([box_length/2, box_width/2 + 12, 0.5])
            linear_extrude(1)
                text("AmirSwitch", size=7, halign="center", valign="center",
                     font="Liberation Sans:style=Bold");

        translate([box_length/2, box_width/2 + 2, 0.5])
            linear_extrude(1)
                text("Smart Plug v1.0", size=4, halign="center", valign="center",
                     font="Liberation Sans");
    }
}

// ============================================================
// ASSEMBLY
// ============================================================
if (show_bottom) {
    color("SteelBlue", 0.9)
        bottom();
}

if (show_top) {
    tz = exploded_view ? bottom_height + explode_distance : bottom_height;
    color("LightSteelBlue", 0.8)
        translate([0, 0, tz])
            mirror([0, 0, 1])
                top();
}

// ============================================================
// COMPONENT GHOSTS (for visualization only)
// ============================================================
if (show_bottom) {
    // ESP32 ghost
    color("green", 0.3)
        translate([wall + esp_x, wall + esp_y, wall + standoff_height])
            cube([esp_length, esp_width, esp_height]);

    // Relay ghost
    color("blue", 0.3)
        translate([wall + relay_x, wall + relay_y, wall + standoff_height])
            cube([relay_length, relay_width, relay_height]);

    // HLK-PM01 ghost
    color("red", 0.3)
        translate([wall + hlk_x, wall + hlk_y, wall])
            cube([hlk_length, hlk_width, hlk_height]);
}
