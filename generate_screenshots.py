"""
Athar App Store Screenshot Generator
Produces polished App Store / Google Play screenshots with the Athar logo
centered inside a clean device frame.

Output: screenshots/ directory
Sizes generated:
  - iPhone 6.7"  : 1290 x 2796  (App Store required)
  - iPhone 6.5"  : 1284 x 2778
  - iPad 12.9"   : 2048 x 2732
  - Android phone: 1080 x 1920  (Play Store)
"""

import os
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter, ImageFont

# ── Paths ─────────────────────────────────────────────────────────────────────
ROOT       = Path(__file__).parent
LOGO_PATH  = ROOT / "app/src/main/res/drawable/athar_logo.png"
OUT_DIR    = ROOT / "screenshots"
OUT_DIR.mkdir(exist_ok=True)

# ── Brand colours ─────────────────────────────────────────────────────────────
NAVY     = (31,  60,  91)       # #1F3C5B
GOLD     = (201, 162,  77)      # #C9A24D
WHITE    = (255, 255, 255)
SKY_BLUE = (234, 242, 251)      # #EAF2FB

# ── Screen sizes ──────────────────────────────────────────────────────────────
SIZES = {
    "iphone_67":  (1290, 2796),
    "iphone_65":  (1284, 2778),
    "ipad_129":   (2048, 2732),
    "android":    (1080, 1920),
}

# ── Device frame config (as fraction of canvas) ───────────────────────────────
FRAME_PADDING   = 0.06   # gap between canvas edge and phone frame outer edge
CORNER_RADIUS_F = 0.10   # corner radius as fraction of frame width
STATUS_BAR_H_F  = 0.05   # status-bar height as fraction of frame height
HOME_BAR_H_F    = 0.03   # home indicator as fraction of frame height
SCREEN_INSET_F  = 0.015  # inner screen padding fraction (frame wall thickness)

# Logo occupies this fraction of the usable screen area (width-driven)
LOGO_WIDTH_FRACTION = 0.55   # logo width = 55 % of screen width
LOGO_MAX_HEIGHT_F   = 0.35   # never taller than 35 % of screen height


def _gradient_background(size: tuple[int, int]) -> Image.Image:
    """Vertical gradient from NAVY (top) to a lighter navy (bottom)."""
    w, h = size
    img = Image.new("RGB", size)
    draw = ImageDraw.Draw(img)
    top    = NAVY
    bottom = (20, 40, 65)
    for y in range(h):
        t = y / h
        r = int(top[0] + (bottom[0] - top[0]) * t)
        g = int(top[1] + (bottom[1] - top[1]) * t)
        b = int(top[2] + (bottom[2] - top[2]) * t)
        draw.line([(0, y), (w, y)], fill=(r, g, b))
    return img


def _rounded_rect_mask(size: tuple[int, int], radius: int) -> Image.Image:
    """RGBA mask with rounded corners."""
    w, h = size
    mask = Image.new("L", (w, h), 0)
    draw = ImageDraw.Draw(mask)
    draw.rounded_rectangle([(0, 0), (w - 1, h - 1)], radius=radius, fill=255)
    return mask


def _draw_subtle_gold_dots(draw: ImageDraw.ImageDraw, w: int, h: int) -> None:
    """Scatter a few small gold accent circles in the background corners."""
    import random
    random.seed(42)
    regions = [
        (0,        0,        w // 3, h // 3),
        (2*w//3,   0,        w,      h // 3),
        (0,        2*h//3,   w//3,   h),
        (2*w//3,   2*h//3,   w,      h),
    ]
    for rx0, ry0, rx1, ry1 in regions:
        for _ in range(6):
            cx = random.randint(rx0, rx1)
            cy = random.randint(ry0, ry1)
            r  = random.randint(4, 16) * (w // 1080)
            alpha_val = random.randint(25, 70)
            # draw.ellipse is opaque, so we fake alpha by blending manually
            draw.ellipse(
                [(cx - r, cy - r), (cx + r, cy + r)],
                fill=(*GOLD, alpha_val),
            )


def _draw_gold_divider(draw: ImageDraw.ImageDraw,
                       cx: int, y: int, width: int, scale: float) -> None:
    """Thin horizontal gold rule with dots at each end."""
    half = int(width * 0.28)
    lw   = max(2, int(3 * scale))
    dot  = max(4, int(8 * scale))
    draw.line([(cx - half, y), (cx + half, y)],
              fill=(*GOLD, 220), width=lw)
    for ex in (cx - half, cx + half):
        draw.ellipse([(ex - dot, y - dot), (ex + dot, y + dot)],
                     fill=(*GOLD, 220))


def generate_screenshot(name: str, canvas_size: tuple[int, int]) -> Path:
    w, h   = canvas_size
    scale  = w / 1080          # normalise to 1080-wide baseline

    # ── 1. Background ─────────────────────────────────────────────────────────
    canvas = _gradient_background(canvas_size).convert("RGBA")
    draw   = ImageDraw.Draw(canvas, "RGBA")
    _draw_subtle_gold_dots(draw, w, h)

    # ── 2. Device frame dimensions ────────────────────────────────────────────
    pad        = int(FRAME_PADDING * w)
    frame_w    = w - 2 * pad
    frame_h    = int(frame_w * (h / w) * 1.85)   # keep phone aspect ≈ real
    frame_h    = min(frame_h, h - 2 * pad)
    frame_x    = (w - frame_w) // 2
    frame_y    = (h - frame_h) // 2
    corner_r   = int(CORNER_RADIUS_F * frame_w)
    wall       = max(8, int(SCREEN_INSET_F * frame_w))

    # ── 3. Phone body (dark navy with gold outline) ───────────────────────────
    body_col  = (18, 32, 52)
    border_w  = max(3, int(6 * scale))

    # outer glow / shadow
    shadow = Image.new("RGBA", canvas_size, (0, 0, 0, 0))
    sdraw  = ImageDraw.Draw(shadow)
    for offset_px in range(max(1, int(18 * scale)), 0, -1):
        alpha = int(80 * (offset_px / (18 * scale)))
        sdraw.rounded_rectangle(
            [(frame_x - offset_px, frame_y - offset_px),
             (frame_x + frame_w + offset_px, frame_y + frame_h + offset_px)],
            radius=corner_r + offset_px,
            fill=(0, 0, 0, alpha),
        )
    shadow = shadow.filter(ImageFilter.GaussianBlur(radius=int(12 * scale)))
    canvas = Image.alpha_composite(canvas, shadow)
    draw   = ImageDraw.Draw(canvas, "RGBA")

    # phone body
    draw.rounded_rectangle(
        [(frame_x, frame_y), (frame_x + frame_w, frame_y + frame_h)],
        radius=corner_r, fill=(*body_col, 255),
    )
    # gold border
    draw.rounded_rectangle(
        [(frame_x, frame_y), (frame_x + frame_w, frame_y + frame_h)],
        radius=corner_r, outline=(*GOLD, 200), width=border_w,
    )

    # ── 4. Screen area (white card inside frame) ───────────────────────────────
    sx  = frame_x + wall
    sy  = frame_y + wall
    sw  = frame_w - 2 * wall
    sh  = frame_h - 2 * wall
    scr = int(corner_r * 0.72)    # screen corner radius (slightly less)

    draw.rounded_rectangle(
        [(sx, sy), (sx + sw, sy + sh)],
        radius=scr, fill=(*WHITE, 255),
    )

    # ── 5. Status bar (dark strip at top of screen) ────────────────────────────
    sb_h = int(STATUS_BAR_H_F * sh)
    draw.rounded_rectangle(
        [(sx, sy), (sx + sw, sy + sb_h)],
        radius=scr, fill=(*body_col, 255),
    )
    # tiny camera notch
    notch_w = int(sw * 0.22)
    notch_h = int(sb_h * 0.55)
    nx = sx + (sw - notch_w) // 2
    ny = sy
    draw.rounded_rectangle(
        [(nx, ny), (nx + notch_w, ny + notch_h)],
        radius=notch_h // 2, fill=(*body_col, 255),
    )

    # ── 6. Home indicator bar at bottom of screen ─────────────────────────────
    hb_h  = int(HOME_BAR_H_F * sh)
    hb_w  = int(sw * 0.30)
    hbx   = sx + (sw - hb_w) // 2
    hby   = sy + sh - hb_h - int(6 * scale)
    hb_r  = hb_h // 2
    draw.rounded_rectangle(
        [(hbx, hby), (hbx + hb_w, hby + hb_h)],
        radius=hb_r, fill=(*body_col, 200),
    )

    # ── 7. Usable content area on screen ──────────────────────────────────────
    content_top    = sy + sb_h + int(12 * scale)
    content_bottom = hby - int(12 * scale)
    content_h      = content_bottom - content_top
    content_cx     = sx + sw // 2

    # ── 8. Logo ────────────────────────────────────────────────────────────────
    logo_raw = Image.open(LOGO_PATH).convert("RGBA")

    # Target logo width
    target_logo_w = int(sw * LOGO_WIDTH_FRACTION)
    # Scale proportionally, then clamp by max height
    ratio         = target_logo_w / logo_raw.width
    target_logo_h = int(logo_raw.height * ratio)
    max_logo_h    = int(content_h * LOGO_MAX_HEIGHT_F)
    if target_logo_h > max_logo_h:
        ratio         = max_logo_h / logo_raw.height
        target_logo_w = int(logo_raw.width * ratio)
        target_logo_h = max_logo_h

    logo = logo_raw.resize(
        (target_logo_w, target_logo_h),
        resample=Image.LANCZOS,
    )

    # Center logo in the upper 55 % of the content area
    logo_section_h = int(content_h * 0.55)
    logo_x = content_cx - target_logo_w // 2
    logo_y = content_top + (logo_section_h - target_logo_h) // 2

    # Composite onto white screen (logo has RGBA)
    canvas.paste(logo, (logo_x, logo_y), logo)
    draw = ImageDraw.Draw(canvas, "RGBA")

    # ── 9. Gold divider below logo ────────────────────────────────────────────
    divider_y = content_top + logo_section_h + int(10 * scale)
    _draw_gold_divider(draw, content_cx, divider_y, sw, scale)

    # ── 10. App name text (simple, no custom font dependency) ─────────────────
    text_y = divider_y + int(30 * scale)
    try:
        # Try to use a system font; fall back gracefully
        font_size_large = max(24, int(52 * scale))
        font_size_small = max(14, int(30 * scale))
        try:
            font_large = ImageFont.truetype("arial.ttf", font_size_large)
            font_small = ImageFont.truetype("arial.ttf", font_size_small)
        except OSError:
            font_large = ImageFont.truetype(
                "C:/Windows/Fonts/arial.ttf", font_size_large)
            font_small = ImageFont.truetype(
                "C:/Windows/Fonts/arial.ttf", font_size_small)

        # App name
        app_name = "Athar"
        bb = draw.textbbox((0, 0), app_name, font=font_large)
        tw = bb[2] - bb[0]
        draw.text(
            (content_cx - tw // 2, text_y),
            app_name,
            font=font_large,
            fill=(*NAVY, 255),
        )
        text_y += (bb[3] - bb[1]) + int(14 * scale)

        # Tagline
        tagline = "Accessibility Mapping"
        bb2 = draw.textbbox((0, 0), tagline, font=font_small)
        tw2 = bb2[2] - bb2[0]
        draw.text(
            (content_cx - tw2 // 2, text_y),
            tagline,
            font=font_small,
            fill=(*GOLD, 230),
        )
    except Exception:
        pass   # silently skip text if no font available

    # ── 11. Save ───────────────────────────────────────────────────────────────
    out_path = OUT_DIR / f"athar_screenshot_{name}.png"
    canvas.convert("RGB").save(out_path, "PNG", optimize=True)
    print(f"  OK  {out_path.name}  ({w}x{h})")
    return out_path


def main() -> None:
    print("Generating Athar App Store screenshots...\n")
    for name, size in SIZES.items():
        generate_screenshot(name, size)
    print(f"\nDone - saved to {OUT_DIR}")


if __name__ == "__main__":
    main()
