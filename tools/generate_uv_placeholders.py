#!/usr/bin/env python3
"""
Generate UV placeholder textures for hoverbike parts.
Shows only the UV-mapped pixels, rest is transparent.
"""

from PIL import Image, ImageDraw
import os
import math

# Output directory
OUTPUT_DIR = "../src/main/resources/assets/apica/textures/entity/hoverbee"

# Colors for different parts (RGBA)
COLORS = [
    (255, 100, 100, 255),   # Red
    (100, 255, 100, 255),   # Green
    (100, 100, 255, 255),   # Blue
    (255, 255, 100, 255),   # Yellow
    (255, 100, 255, 255),   # Magenta
    (100, 255, 255, 255),   # Cyan
]

def get_box_uv_regions(u, v, width, height, depth):
    """
    Calculate UV regions for a Minecraft box.
    Returns list of (x, y, w, h) tuples for each face region.

    Minecraft box UV layout:
              depth    width    depth    width
            +--------+--------+--------+--------+
      depth |        |  TOP   |        | BOTTOM |
            +--------+--------+--------+--------+
     height |  LEFT  | FRONT  | RIGHT  |  BACK  |
            +--------+--------+--------+--------+
    """
    # Round dimensions to integers for pixel coords
    w = int(math.ceil(width))
    h = int(math.ceil(height))
    d = int(math.ceil(depth))

    regions = []

    # Top face (at V, from U+depth to U+depth+width)
    regions.append((u + d, v, w, d))

    # Bottom face (at V, from U+depth+width to U+depth+2*width)
    regions.append((u + d + w, v, w, d))

    # Left face (at V+depth, from U to U+depth)
    regions.append((u, v + d, d, h))

    # Front face (at V+depth, from U+depth to U+depth+width)
    regions.append((u + d, v + d, w, h))

    # Right face (at V+depth, from U+depth+width to U+depth+width+depth)
    regions.append((u + d + w, v + d, d, h))

    # Back face (at V+depth, from U+depth+width+depth to U+2*depth+2*width)
    regions.append((u + d + w + d, v + d, w, h))

    return regions

def create_placeholder_texture(tex_width, tex_height, cubes, filename):
    """
    Create a placeholder texture showing UV regions.
    cubes: list of (u, v, width, height, depth) tuples
    """
    img = Image.new('RGBA', (tex_width, tex_height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    color_idx = 0
    for cube in cubes:
        u, v, w, h, d = cube
        regions = get_box_uv_regions(u, v, w, h, d)
        color = COLORS[color_idx % len(COLORS)]

        for region in regions:
            rx, ry, rw, rh = region
            # Draw filled rectangle
            draw.rectangle([rx, ry, rx + rw - 1, ry + rh - 1], fill=color)

        color_idx += 1

    # Save
    filepath = os.path.join(OUTPUT_DIR, filename)
    os.makedirs(os.path.dirname(filepath), exist_ok=True)
    img.save(filepath)
    print(f"Created: {filepath}")

def main():
    os.chdir(os.path.dirname(os.path.abspath(__file__)))

    # === SADDLE A (32x16) ===
    # seat: 6x1x5 at (0,0)
    # backrest: 5x1x1 at (0,6)
    # back_bar: 7x2x2 at (0,8)
    create_placeholder_texture(32, 16, [
        (0, 0, 6, 1, 5),   # seat
        (0, 6, 5, 1, 1),   # backrest
        (0, 8, 7, 2, 2),   # back_bar
    ], "hoverbee_saddle_a.png")

    # === SADDLE B (32x16) ===
    # seat: 6x1x5 at (0,0)
    # backrest: 5x1x1 at (0,6)
    # cube_left/right: 1x2x2.15 at (0,8)
    create_placeholder_texture(32, 16, [
        (0, 0, 6, 1, 5),   # seat
        (0, 6, 5, 1, 1),   # backrest
        (0, 8, 1, 2, 3),   # cube (rounded up)
    ], "hoverbee_saddle_b.png")

    # === SADDLE C (32x32) ===
    # seat: 6x1x5 at (0,0)
    # backrest: 5x1x1 at (0,6)
    # main_cube: 3x3x1 at (0,8)
    # secondary_cube: 2x2x1 at (0,12)
    # bar_horizontal: 3.5x1x0.5 at (0,15)
    # bar_vertical: 1x3.5x0.5 at (8,15)
    create_placeholder_texture(32, 32, [
        (0, 0, 6, 1, 5),     # seat
        (0, 6, 5, 1, 1),     # backrest
        (0, 8, 3, 3, 1),     # main_cube
        (0, 12, 2, 2, 1),    # secondary_cube
        (0, 15, 4, 1, 1),    # bar_horizontal (rounded)
        (8, 15, 1, 4, 1),    # bar_vertical (rounded)
    ], "hoverbee_saddle_c.png")

    # === WING PROTECTOR A/B/C (32x16) - same geometry ===
    # protector_right: 6x1x5 at (0,0)
    # protector_left: 6x1x5 at (0,6)
    for variant in ['a', 'b', 'c']:
        create_placeholder_texture(32, 16, [
            (0, 0, 6, 1, 5),   # protector_right
            (0, 6, 6, 1, 5),   # protector_left
        ], f"hoverbee_wing_protector_{variant}.png")

    # === CONTROL A (16x16) ===
    # control: 1x3x3 at (0,0)
    create_placeholder_texture(16, 16, [
        (0, 0, 1, 3, 3),   # control cube
    ], "hoverbee_control_a.png")

    # === CONTROL B (16x16) ===
    # cube_front/back: 1x2x1.15 at (0,0)
    # connector: 1x2x2 at (0,4)
    create_placeholder_texture(16, 16, [
        (0, 0, 1, 2, 2),   # cube (rounded)
        (0, 4, 1, 2, 2),   # connector
    ], "hoverbee_control_b.png")

    # === CONTROL C (16x16) ===
    # main_cube: 1x3x3 at (0,0)
    # secondary_cube: 1x2x2 at (0,4)
    # bar_horizontal: 0.5x1x3.5 at (0,8)
    # bar_vertical: 0.5x3.5x1 at (0,10)
    create_placeholder_texture(16, 16, [
        (0, 0, 1, 3, 3),   # main_cube
        (0, 4, 1, 2, 2),   # secondary_cube
        (0, 8, 1, 1, 4),   # bar_horizontal (rounded)
        (0, 10, 1, 4, 1),  # bar_vertical (rounded)
    ], "hoverbee_control_c.png")

    # === CONNECTOR PLACEHOLDER (simple 8x8) ===
    img = Image.new('RGBA', (8, 8), (255, 0, 255, 255))  # Magenta
    filepath = os.path.join(OUTPUT_DIR, "hoverbee_connector_placeholder.png")
    img.save(filepath)
    print(f"Created: {filepath}")

    print("\nDone! All placeholder textures generated.")

if __name__ == "__main__":
    main()
