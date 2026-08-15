# -*- coding: utf-8 -*-
"""
Kariyer haritasi (yaris pisti) mockup'i — 1080x2400.

Bu betik SUSLEME DEGIL, dogrulama aracidir: bu makinede adb/emulator yok, o
yuzden LevelMapScreen.kt'nin gercekten nasil gorunecegini ancak ayni geometriyi
ikinci bir yerde cizerek gorebiliyoruz.

Sayilar TrackLayout.kt ile BIREBIR ayni:
    centerX(y) = width/2 + amplitude * sin(PI * y / SEGMENT_HEIGHT)
    stopY(i)   = totalHeight - (i + 0.5) * SEGMENT_HEIGHT
Arac cizimi de CarCatalog.HATCHBACK + COLOR_KRON_RED parcalarindan uretiliyor.

Calistirma:  py tools/level_map_mock.py
"""
import math
import os
from PIL import Image, ImageDraw, ImageFont

# ---------------------------------------------------------------------------
# Ekran / olcek
# ---------------------------------------------------------------------------
W_DP, H_DP = 360.0, 800.0
DENSITY = 3.0          # 1080 / 360
SS = 2                 # kenar yumusatma icin asiri ornekleme
S = DENSITY * SS       # 1 dp kac piksel (asiri ornekli tuvalde)

OUT_W, OUT_H = int(W_DP * DENSITY), int(H_DP * DENSITY)   # 1080 x 2400

# ---------------------------------------------------------------------------
# TrackLayout.kt sabitleri
# ---------------------------------------------------------------------------
STOP_COUNT = 30
SEG = 140.0
ROAD_HALF = 44.0
KERB = 7.0
EDGE_MARGIN = 10.0
WEAVE_SLOPE = 1.0
KERB_BLOCK = 14.0
STOP_DIAMETER = 56.0
CAR_TRAIL = 0.42
CAR_W, CAR_H = 42.0, 82.0

HALF_KERB = ROAD_HALF + KERB
TOTAL_H = SEG * STOP_COUNT
AMP = min(WEAVE_SLOPE * SEG / math.pi, W_DP / 2 - HALF_KERB - EDGE_MARGIN)

# TrackSegmentArt.kt sabitleri
SHOULDER_SPREAD = 16.0
RIBBON_STEPS = 18
OVERHANG = 0.6

# LevelMapScreen.kt sabitleri
PANEL_EDGE = 8.0
PANEL_GAP = 12.0
PANEL_HALF_HEIGHT = 26.0

# ---------------------------------------------------------------------------
# KronColors
# ---------------------------------------------------------------------------
BG = (0x02, 0x09, 0x15)
BG_MID = (0x05, 0x14, 0x27)
BG_TOP = (0x0C, 0x2A, 0x52)
SURFACE = (0x0A, 0x1A, 0x33)
SURFACE_DEEP = (0x04, 0x0D, 0x1B)
ACCENT = (0xF5, 0xC1, 0x00)
ACCENT_BRIGHT = (0xFF, 0xD4, 0x3D)
TEXT_PRIMARY = (0xEA, 0xF1, 0xFB)
TEXT_SECONDARY = (0xB9, 0xC6, 0xD7)
TEXT_MUTED = (0x80, 0x92, 0xAB)
ROAD = (0x3A, 0x40, 0x48)
ROAD_LINE = (0xEF, 0xEF, 0xEF)
KERB_RED = (0xD6, 0x28, 0x28)
KERB_LIGHT = (0xDC, 0xE2, 0xE9)
OBJECTIVE_DONE = (0x3D, 0xDC, 0x84)
LOCKED = (0x1C, 0x27, 0x40)
SHOULDER = (0x04, 0x0D, 0x1B)
DARK_CHECKER = (0x10, 0x15, 0x1F)

FONT_DIR = "C:/Windows/Fonts/"


def font(sp, weight="regular"):
    name = {"regular": "arial.ttf", "bold": "arialbd.ttf", "black": "ariblk.ttf"}[weight]
    return ImageFont.truetype(os.path.join(FONT_DIR, name), int(round(sp * S)))


# ---------------------------------------------------------------------------
# Pist geometrisi (TrackLayout ile ayni formuller)
# ---------------------------------------------------------------------------
def center_x(y):
    return W_DP / 2 + AMP * math.sin(math.pi * y / SEG)


def slope_at(y):
    return AMP * (math.pi / SEG) * math.cos(math.pi * y / SEG)


def edge_point(y, offset):
    s = slope_at(y)
    length = math.hypot(1.0, s)
    return (center_x(y) + offset / length, y - offset * s / length)


def stop_y(level_index):
    return TOTAL_H - (level_index + 0.5) * SEG


def point_at(position):
    y = TOTAL_H - (position + 0.5) * SEG
    return (center_x(y), y)


def heading_at(position):
    y = TOTAL_H - (position + 0.5) * SEG
    return math.degrees(math.atan2(-slope_at(y), 1.0))


def segment_of_level(level_index):
    return STOP_COUNT - 1 - level_index


# ---------------------------------------------------------------------------
# Sahne durumu — mockup'ta gosterilen kayit
# ---------------------------------------------------------------------------
CURRENT_LEVEL = 5
EARNED = {1: 3, 2: 3, 3: 2, 4: 1}
GOALS = {  # id -> (tip, deger)
    1: ("time", 25), 2: ("time", 30), 3: ("time", 35), 4: ("time", 40),
    5: ("time", 45), 6: ("time", 45), 7: ("time", 50), 8: ("dist", 1200),
}


def goal_caption(level_id):
    return "HAYATTA KAL" if GOALS[level_id][0] == "time" else "MESAFE"


def goal_value(level_id):
    kind, value = GOALS[level_id]
    return "%d sn" % value if kind == "time" else "%d m" % value


# ---------------------------------------------------------------------------
# Cizim yardimcilari
# ---------------------------------------------------------------------------
def px(v):
    return v * S


def ribbon_polygon(y0, y1, near, far, y_to_layer):
    pts = []
    for i in range(RIBBON_STEPS + 1):
        y = y0 + (y1 - y0) * i / RIBBON_STEPS
        x, yy = edge_point(y, near)
        pts.append((px(x), y_to_layer(yy)))
    for i in range(RIBBON_STEPS, -1, -1):
        y = y0 + (y1 - y0) * i / RIBBON_STEPS
        x, yy = edge_point(y, far)
        pts.append((px(x), y_to_layer(yy)))
    return pts


def rgba(color, alpha=1.0):
    return (color[0], color[1], color[2], int(round(255 * alpha)))


# ---------------------------------------------------------------------------
# Arac — CarCatalog.HATCHBACK + COLOR_KRON_RED
# ---------------------------------------------------------------------------
BODY = (0xE1, 0x06, 0x00)
BODY_SHADE = (0x8E, 0x04, 0x00)
CAR_ACCENT = (0xFF, 0xFF, 0xFF)
GLASS = (0x1E, 0x2A, 0x47)
TIRE = (0x05, 0x05, 0x05)
DRIVER = (0xFF, 0xD3, 0x3D)
TAIL = (0xFF, 0x2D, 0x1F)
GLOSS = (0xFF, 0xFF, 0xFF)

BODY_ROLL = ("h", [(0.00, -0.52), (0.06, -0.24), (0.19, 0.30), (0.33, 0.13),
                   (0.52, 0.00), (0.72, -0.15), (0.88, -0.36), (1.00, -0.56)])
ROOF_ROLL = ("h", [(0.00, -0.26), (0.18, 0.20), (0.46, 0.07), (0.74, -0.08), (1.00, -0.32)])
NOSE_FADE = ("v", [(0.00, -0.38), (0.30, -0.08), (1.00, 0.02)])
REAR_FADE = ("v", [(0.00, -0.12), (0.38, -0.46), (1.00, -0.70)])
GLASS_SHEEN = ("v", [(0.00, 0.42), (0.28, 0.12), (0.62, -0.16), (1.00, 0.04)])
WHEEL_ROLL = ("h", [(0.00, 0.02), (0.26, 0.26), (0.58, 0.10), (1.00, 0.00)])
TAIL_GLOW = ("v", [(0.00, 0.32), (0.55, 0.00), (1.00, -0.26)])


def shifted(color, shift):
    if shift >= 0:
        return tuple(int(round(c + (255 - c) * shift)) for c in color)
    k = 1.0 + shift
    return tuple(int(round(c * k)) for c in color)


def _rear_face(top, half_top, half_bottom, tail_inset=1.6):
    bottom = 74.0
    height = bottom - top
    tail_half = half_bottom - 1.2
    strip_top = top + height * 0.30
    strip_h = height * 0.42
    return [
        ("wedge", BODY_SHADE, [(-half_top, top), (half_top, top),
                               (half_bottom, bottom), (-half_bottom, bottom)], REAR_FADE, 1.0),
        ("box", GLOSS, (-half_top + 0.4, top - 0.55, (half_top - 0.4) * 2, 1.0, 0.5), None, 0.55),
        ("box", TAIL, (-tail_half, strip_top, tail_half - tail_inset, strip_h, 0.9), TAIL_GLOW, 1.0),
        ("box", TAIL, (tail_inset, strip_top, tail_half - tail_inset, strip_h, 0.9), TAIL_GLOW, 1.0),
    ]


CAR_PARTS = [
    ("box", TIRE, (-20, 11, 7, 20, 2.4), WHEEL_ROLL, 1.0),
    ("box", TIRE, (13, 11, 7, 20, 2.4), WHEEL_ROLL, 1.0),
    ("box", TIRE, (-20, 47, 7, 21, 2.4), WHEEL_ROLL, 1.0),
    ("box", TIRE, (13, 47, 7, 21, 2.4), WHEEL_ROLL, 1.0),
    ("box", BODY, (-15.5, 16, 31, 56, 6), BODY_ROLL, 1.0),
    ("wedge", BODY, [(-9, -2), (9, -2), (15.5, 22), (-15.5, 22)], BODY_ROLL, 1.0),
    ("box", BODY_SHADE, (-8.4, -2, 16.8, 2.2, 1), NOSE_FADE, 1.0),
    ("box", CAR_ACCENT, (-2.8, 0.6, 5.6, 20.4, 1.2), None, 0.88),
    ("box", CAR_ACCENT, (-2.8, 47.6, 5.6, 20.4, 1.2), None, 0.88),
    ("box", GLASS, (-11.4, 21.5, 22.8, 10.2, 4), GLASS_SHEEN, 1.0),
    ("disc", DRIVER, (0, 26.4, 4), None, 1.0),
    ("box", BODY, (-12.4, 30.6, 24.8, 10.4, 3), ROOF_ROLL, 1.0),
    ("box", BODY_SHADE, (-12.2, 39.4, 24.4, 1.5, 0.7), None, 0.32),
    ("box", GLASS, (-11, 40.4, 22, 7, 3), GLASS_SHEEN, 1.0),
    ("box", GLOSS, (-14.3, 13, 1.5, 50, 0.75), None, 0.14),
] + _rear_face(69.0, 14.9, 13.8)

# CarPreview kutusu: arac kutusu + golge alani -> x -21..21, y -2..80
CAR_LEFT, CAR_TOP = -21.0, -2.0


def gradient_fill(size, bbox_px, axis, stops, base):
    """Parcanin kendi sinirlari boyunca uzanan ton gecisi (Brush ile ayni)."""
    x0, y0, x1, y1 = bbox_px
    span = max(1, int(round((x1 - x0) if axis == "h" else (y1 - y0))))
    ramp = Image.new("RGB", (span, 1) if axis == "h" else (1, span))
    pixels = ramp.load()
    for i in range(span):
        t = i / max(1, span - 1)
        # stops arasi dogrusal ara deger
        prev = stops[0]
        nxt = stops[-1]
        for k in range(len(stops) - 1):
            if stops[k][0] <= t <= stops[k + 1][0]:
                prev, nxt = stops[k], stops[k + 1]
                break
        denom = (nxt[0] - prev[0]) or 1.0
        f = (t - prev[0]) / denom
        shift = prev[1] + (nxt[1] - prev[1]) * f
        color = shifted(base, shift)
        if axis == "h":
            pixels[i, 0] = color
        else:
            pixels[0, i] = color
    full = ramp.resize((max(1, int(round(x1 - x0))), max(1, int(round(y1 - y0)))), Image.BILINEAR)
    canvas = Image.new("RGB", size, base)
    canvas.paste(full, (int(round(x0)), int(round(y0))))
    return canvas


def render_car():
    """Araci kendi kutusunda (42x82 dp) cizer; sonra dondurulup yapistirilir."""
    size = (int(round(CAR_W * S)), int(round(CAR_H * S)))
    img = Image.new("RGBA", size, (0, 0, 0, 0))

    def to_px(u, v):
        return ((u - CAR_LEFT) * S, (v - CAR_TOP) * S)

    # Golge (CarCatalog.SHADOW_*)
    shadow = Image.new("RGBA", size, (0, 0, 0, 0))
    ImageDraw.Draw(shadow).ellipse([to_px(-21, 12), to_px(21, 80)], fill=(0, 0, 0, 0x3D))
    img = Image.alpha_composite(img, shadow)

    for part in CAR_PARTS:
        kind, base, geom, grad, alpha = part
        mask = Image.new("L", size, 0)
        d = ImageDraw.Draw(mask)
        if kind == "box":
            left, top, w, h, corner = geom
            box = [to_px(left, top), to_px(left + w, top + h)]
            if corner > 0:
                d.rounded_rectangle(box, radius=corner * S, fill=255)
            else:
                d.rectangle(box, fill=255)
            bbox = (box[0][0], box[0][1], box[1][0], box[1][1])
        elif kind == "disc":
            cx, cy, r = geom
            box = [to_px(cx - r, cy - r), to_px(cx + r, cy + r)]
            d.ellipse(box, fill=255)
            bbox = (box[0][0], box[0][1], box[1][0], box[1][1])
        else:
            pts = [to_px(u, v) for u, v in geom]
            d.polygon(pts, fill=255)
            xs = [p[0] for p in pts]
            ys = [p[1] for p in pts]
            bbox = (min(xs), min(ys), max(xs), max(ys))

        if alpha < 1.0:
            mask = mask.point(lambda v: int(v * alpha))
        if grad is not None:
            fill_img = gradient_fill(size, bbox, grad[0], grad[1], base)
        else:
            fill_img = Image.new("RGB", size, base)
        img.paste(fill_img, (0, 0), mask)

    return img


# ---------------------------------------------------------------------------
# Ekran parcalari
# ---------------------------------------------------------------------------
def draw_background(img):
    d = ImageDraw.Draw(img)
    h = img.size[1]
    for y in range(h):
        t = y / (h - 1)
        if t < 0.5:
            f = t / 0.5
            c = tuple(int(BG_TOP[i] + (BG_MID[i] - BG_TOP[i]) * f) for i in range(3))
        else:
            f = (t - 0.5) / 0.5
            c = tuple(int(BG_MID[i] + (BG[i] - BG_MID[i]) * f) for i in range(3))
        d.line([(0, y), (img.size[0], y)], fill=c)


def draw_header(img):
    d = ImageDraw.Draw(img, "RGBA")
    # Geri butonu: 40 dp kutu, 14 dp kenar bosluk, 28 dp durum cubugu payi
    bx, by = 14.0, 38.0
    d.rounded_rectangle([px(bx), px(by), px(bx + 40), px(by + 40)], radius=px(14),
                        fill=SURFACE, outline=(255, 255, 255, 0x14), width=max(1, int(S)))
    # ok
    cx, cy = px(bx + 20), px(by + 20)
    d.line([(cx + px(6), cy - px(7)), (cx - px(5), cy), (cx + px(6), cy + px(7))],
           fill=ACCENT, width=int(px(2.4)), joint="curve")
    d.text((px(bx + 40 + 12), px(58)), "KARİYER", font=font(22, "regular"),
           fill=ACCENT, anchor="lm")


def draw_banner(img, top_dp):
    d = ImageDraw.Draw(img, "RGBA")
    d.rectangle([0, px(top_dp), px(W_DP), px(top_dp + 50)], fill=(0x08, 0x10, 0x1E))
    d.line([(0, px(top_dp)), (px(W_DP), px(top_dp))], fill=(255, 255, 255, 0x14), width=int(S))
    d.text((px(W_DP / 2), px(top_dp + 25)), "AdMob banner (320x50)", font=font(12),
           fill=TEXT_MUTED, anchor="mm")


def clip_text(d, text, fnt, max_w_dp):
    """Compose'daki `overflow = TextOverflow.Ellipsis` karsiligi."""
    limit = px(max_w_dp)
    if d.textlength(text, font=fnt) <= limit:
        return text
    while text and d.textlength(text + "…", font=fnt) > limit:
        text = text[:-1]
    return text + "…"


def draw_objective_dots(d, x, y, earned, total=3, dot=13.0, align_end=False):
    """ObjectiveDots ile ayni: dolu yesil + tik, bos ince cerceve. 4 dp aralik."""
    width = total * dot + (total - 1) * 4.0
    start = x - width if align_end else x
    for i in range(total):
        left = start + i * (dot + 4.0)
        box = [px(left), px(y), px(left + dot), px(y + dot)]
        if i < earned:
            d.ellipse(box, fill=OBJECTIVE_DONE)
            cx0, cy0 = px(left + dot * 0.26), px(y + dot * 0.52)
            cx1, cy1 = px(left + dot * 0.44), px(y + dot * 0.70)
            cx2, cy2 = px(left + dot * 0.76), px(y + dot * 0.32)
            d.line([(cx0, cy0), (cx1, cy1), (cx2, cy2)], fill=BG, width=int(px(1.5)), joint="curve")
        else:
            d.ellipse(box, outline=TEXT_MUTED, width=max(1, int(px(dot * 0.1))))


def draw_checkered_flag(d, x, y, w=18.0, h=16.0):
    pole = 1.6
    d.rectangle([px(x), px(y), px(x + pole), px(y + h)], fill=TEXT_SECONDARY)
    cols, rows = 3, 2
    cell = (w - pole) / cols
    for r in range(rows):
        for c in range(cols):
            color = TEXT_PRIMARY if (r + c) % 2 == 0 else DARK_CHECKER
            d.rectangle([px(x + pole + c * cell), px(y + r * cell),
                         px(x + pole + (c + 1) * cell), px(y + (r + 1) * cell)], fill=color)
    d.rectangle([px(x + pole), px(y), px(x + pole + cell * cols), px(y + cell * rows)],
                outline=(0, 0, 0, 0x33), width=max(1, int(S)))


def draw_lock(d, cx, cy, color):
    body_w, body_h = 13.0, 10.0
    d.rounded_rectangle([px(cx - body_w / 2), px(cy - body_h / 2 + 2),
                         px(cx + body_w / 2), px(cy + body_h / 2 + 2)],
                        radius=px(2), fill=color)
    d.arc([px(cx - 4.5), px(cy - 9), px(cx + 4.5), px(cy + 1)], 180, 360,
          fill=color, width=int(px(2)))


# ---------------------------------------------------------------------------
# Ana cizim
# ---------------------------------------------------------------------------
def build():
    img = Image.new("RGB", (int(W_DP * S), int(H_DP * S)), BG)
    draw_background(img)
    draw_header(img)

    content_top = 88.0                     # durum cubugu + baslik satiri
    content_bottom = H_DP - 12.0 - 50.0    # banner + gezinme payi
    content_h = content_bottom - content_top

    # LevelMapScreen: scrollToItem(segmentOfLevel(current-1) - 1)
    first_item = segment_of_level(CURRENT_LEVEL - 1) - 1
    scroll = first_item * SEG

    layer = Image.new("RGBA", (int(W_DP * S), int(content_h * S)), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer, "RGBA")

    def to_layer(y_dp):
        return (y_dp - scroll) * S

    first_seg = int(math.floor(scroll / SEG))
    last_seg = int(math.ceil((scroll + content_h) / SEG))

    for seg in range(max(0, first_seg), min(STOP_COUNT, last_seg + 1)):
        seg_top = seg * SEG
        # --- Asfalt ve kenar bandi
        d.polygon(ribbon_polygon(seg_top - OVERHANG, seg_top + SEG + OVERHANG,
                                 -(HALF_KERB + SHOULDER_SPREAD), HALF_KERB + SHOULDER_SPREAD,
                                 to_layer), fill=rgba(SHOULDER, 0.80))
        d.polygon(ribbon_polygon(seg_top - OVERHANG, seg_top + SEG + OVERHANG,
                                 -ROAD_HALF, ROAD_HALF, to_layer), fill=rgba(ROAD))
        # --- Kerb bloklari (blok indeksinin tek/ciftligi rengi belirler)
        blocks = int(SEG / KERB_BLOCK)
        for b in range(blocks):
            y0 = seg_top + b * KERB_BLOCK
            y1 = y0 + KERB_BLOCK + 0.3
            color = KERB_RED if b % 2 == 0 else KERB_LIGHT
            d.polygon(ribbon_polygon(y0, y1, -(ROAD_HALF + KERB), -ROAD_HALF, to_layer),
                      fill=rgba(color))
            d.polygon(ribbon_polygon(y0, y1, ROAD_HALF, ROAD_HALF + KERB, to_layer),
                      fill=rgba(color))
        # --- Orta serit cizgisi
        for b in range(0, blocks, 2):
            y0 = seg_top + b * KERB_BLOCK
            d.polygon(ribbon_polygon(y0, y0 + KERB_BLOCK * 0.55, -1.6, 1.6, to_layer),
                      fill=rgba(ROAD_LINE, 0.5))

    # --- Duraklar ve panolar
    for seg in range(max(0, first_seg), min(STOP_COUNT, last_seg + 1)):
        level_index = STOP_COUNT - 1 - seg
        level_id = level_index + 1
        if level_id not in GOALS:
            continue
        earned = EARNED.get(level_id, 0)
        locked = level_id > CURRENT_LEVEL
        is_current = level_id == CURRENT_LEVEL
        completed = earned > 0

        sy = stop_y(level_index)
        sx = center_x(sy)
        ly = to_layer(sy) / S            # dp cinsinden katman y'si
        panel_on_left = sx > W_DP / 2
        # Yol duragin ustunde/altinda panoya dogru kiviriliyor: sinir panonun
        # TUM yuksekliginden okunur (LevelMapScreen ile ayni hesap).
        side = -HALF_KERB if panel_on_left else HALF_KERB
        edges = [edge_point(sy + step * PANEL_HALF_HEIGHT / 2.0, side)[0]
                 for step in (-2, -1, 0, 1, 2)]
        road_bound = min(edges) if panel_on_left else max(edges)
        panel_x = PANEL_EDGE if panel_on_left else road_bound + PANEL_GAP
        panel_w = ((road_bound - PANEL_GAP - PANEL_EDGE) if panel_on_left
                   else (W_DP - road_bound - PANEL_GAP - PANEL_EDGE))

        dim = 0.45 if locked else 1.0

        # Pano
        panel_top = ly - PANEL_HALF_HEIGHT
        text_x = (panel_x + panel_w) if panel_on_left else panel_x
        anchor = "rt" if panel_on_left else "lt"
        d.text((px(text_x), px(panel_top)), clip_text(d, goal_caption(level_id), font(10), panel_w),
               font=font(10), fill=rgba(TEXT_MUTED, dim), anchor=anchor)
        d.text((px(text_x), px(panel_top + 13)),
               clip_text(d, goal_value(level_id), font(17, "black"), panel_w),
               font=font(17, "black"), fill=rgba(TEXT_SECONDARY if locked else TEXT_PRIMARY, dim),
               anchor=anchor)
        if not locked:
            draw_objective_dots(d, text_x, panel_top + 39, earned, align_end=panel_on_left)

        # Durak
        ring = ((255, 255, 255) if False else
                (TEXT_MUTED if locked else
                 (ACCENT_BRIGHT if is_current else
                  (OBJECTIVE_DONE if completed else ACCENT))))
        if locked:
            ring = (0x2A, 0x35, 0x4A)
        fill = LOCKED if locked else (ACCENT if is_current else SURFACE_DEEP)
        r = STOP_DIAMETER / 2
        d.ellipse([px(sx - r), px(ly - r), px(sx + r), px(ly + r)],
                  fill=rgba(ring, (0.34 if is_current else 0.16) * dim))
        d.ellipse([px(sx - r + 5), px(ly - r + 5), px(sx + r - 5), px(ly + r - 5)],
                  fill=rgba(fill, dim))
        d.ellipse([px(sx - r + 5), px(ly - r + 5), px(sx + r - 5), px(ly + r - 5)],
                  outline=rgba(ring, dim), width=int(px(3)))
        if locked:
            draw_lock(d, sx, ly, rgba(TEXT_MUTED, dim))
        else:
            d.text((px(sx), px(ly)), str(level_id), font=font(20, "black"),
                   fill=BG if is_current else TEXT_PRIMARY, anchor="mm")
        if completed:
            draw_checkered_flag(d, sx + 15, ly - 31)

    # --- Oyuncu araci
    position = (CURRENT_LEVEL - 1) - CAR_TRAIL
    cxp, cyp = point_at(position)
    car = render_car()
    car = car.rotate(-heading_at(position), resample=Image.BICUBIC, expand=True)
    layer.alpha_composite(
        car,
        (int(round(px(cxp) - car.size[0] / 2)),
         int(round(to_layer(cyp) - car.size[1] / 2)))
    )

    img.paste(layer, (0, int(px(content_top))), layer)
    draw_banner(img, content_bottom)

    return img.resize((OUT_W, OUT_H), Image.LANCZOS)


if __name__ == "__main__":
    out = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                       "..", "docs", "play_store_assets", "previews", "level_map_mock.png")
    out = os.path.normpath(out)
    os.makedirs(os.path.dirname(out), exist_ok=True)
    build().save(out)
    print("yazildi:", out, os.path.getsize(out), "bayt")
