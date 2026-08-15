"""ONCE/SONRA arac cizimi karsilastirmasi.

Sol blok  = 2026-08-14 tarihli ESKI geometri (bu dosyaya kopyalandi, koddan
            silindigi icin burada donmus halde duruyor).
Sag blok  = kron_art.py icindeki YENI geometri (CarCatalog.kt'nin aynasi).

Her sekil iki olcekte: oyun olcegi (~32 px genislik, CAR_WIDTH_PX = 40*0.80)
ve ayni cizimin 4x buyutulmus hali (detay denetimi icin).

    py car_compare.py
"""

import os

from PIL import Image, ImageDraw

import kron_art as K

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                   '..', 'previews')

BG = (0x3A, 0x40, 0x48)        # asfalt (drawTrack ROAD)
PANEL = (0x10, 0x14, 0x1A)
LINE = (0xEA, 0xF1, 0xFB)

GAME_W = 32                    # GameConfig.CAR_WIDTH_PX = 40 * 0.80
ZOOM = 4

# ---------------------------------------------------------------------------
# ESKI geometri (2026-08-14 CarCatalog.kt + GameRenderer.drawObstacleCar)
# ---------------------------------------------------------------------------

OLD_SHAPES = {
    'hatchback': [
        ('box', 'BODY_SHADE', -18.0, 66.0, 36.0, 8.0, 0.0),
        ('box', 'BODY_SHADE', -16.0, 4.0, 32.0, 6.0, 0.0),
        ('box', 'TIRE', -20.0, 14.0, 8.0, 18.0, 0.0),
        ('box', 'TIRE', 12.0, 14.0, 8.0, 18.0, 0.0),
        ('box', 'TIRE', -20.0, 50.0, 8.0, 18.0, 0.0),
        ('box', 'TIRE', 12.0, 50.0, 8.0, 18.0, 0.0),
        ('box', 'BODY', -10.0, 8.0, 20.0, 62.0, 8.0),
        ('box', 'BODY', -6.0, -2.0, 12.0, 18.0, 6.0),
        ('box', 'GLASS', -7.0, 23.0, 14.0, 18.0, 6.0),
        ('box', 'BODY', -14.0, 29.0, 28.0, 8.0, 0.0),
        ('disc', 'DRIVER', 0.0, 29.0, 5.0),
        ('box', 'ACCENT', -4.0, 11.0, 8.0, 34.0, 0.0),
    ],
    'race_sedan': [
        ('box', 'BODY_SHADE', -17.0, 2.0, 34.0, 5.0, 0.0),
        ('box', 'TIRE', -20.0, 13.0, 8.0, 19.0, 0.0),
        ('box', 'TIRE', 12.0, 13.0, 8.0, 19.0, 0.0),
        ('box', 'TIRE', -20.0, 49.0, 8.0, 19.0, 0.0),
        ('box', 'TIRE', 12.0, 49.0, 8.0, 19.0, 0.0),
        ('box', 'BODY', -11.0, 6.0, 22.0, 64.0, 7.0),
        ('box', 'BODY', -7.0, -2.0, 14.0, 14.0, 5.0),
        ('box', 'ACCENT', -6.0, 8.0, 4.0, 42.0, 0.0),
        ('box', 'ACCENT', 2.0, 8.0, 4.0, 42.0, 0.0),
        ('box', 'GLASS', -8.0, 24.0, 16.0, 16.0, 5.0),
        ('box', 'BODY', -15.0, 28.0, 30.0, 8.0, 0.0),
        ('disc', 'DRIVER', 0.0, 30.0, 5.0),
        ('box', 'BODY_SHADE', -14.0, 62.0, 28.0, 8.0, 0.0),
        ('box', 'ACCENT', -13.0, 58.0, 26.0, 4.0, 2.0),
    ],
    'muscle': [
        ('box', 'BODY_SHADE', -18.0, 66.0, 36.0, 8.0, 0.0),
        ('box', 'BODY_SHADE', -15.0, 3.0, 30.0, 6.0, 0.0),
        ('box', 'TIRE', -20.0, 12.0, 8.0, 20.0, 0.0),
        ('box', 'TIRE', 12.0, 12.0, 8.0, 20.0, 0.0),
        ('box', 'TIRE', -20.0, 48.0, 8.0, 22.0, 0.0),
        ('box', 'TIRE', 12.0, 48.0, 8.0, 22.0, 0.0),
        ('box', 'BODY', -13.0, 6.0, 26.0, 62.0, 4.0),
        ('box', 'BODY', -9.0, -2.0, 18.0, 12.0, 3.0),
        ('box', 'TRIM', -16.0, 42.0, 3.0, 20.0, 1.5),
        ('box', 'TRIM', 13.0, 42.0, 3.0, 20.0, 1.5),
        ('box', 'ACCENT', -7.0, 4.0, 4.0, 60.0, 0.0),
        ('box', 'ACCENT', 3.0, 4.0, 4.0, 60.0, 0.0),
        ('box', 'BODY_SHADE', -3.0, 12.0, 6.0, 8.0, 2.0),
        ('box', 'GLASS', -9.0, 26.0, 18.0, 14.0, 3.0),
        ('box', 'BODY', -14.0, 30.0, 28.0, 7.0, 0.0),
        ('disc', 'DRIVER', 0.0, 31.0, 5.0),
    ],
    'supercar': [
        ('box', 'BODY_SHADE', -16.0, 2.0, 32.0, 4.0, 0.0),
        ('box', 'TIRE', -20.0, 14.0, 8.0, 17.0, 0.0),
        ('box', 'TIRE', 12.0, 14.0, 8.0, 17.0, 0.0),
        ('box', 'TIRE', -20.0, 50.0, 8.0, 20.0, 0.0),
        ('box', 'TIRE', 12.0, 50.0, 8.0, 20.0, 0.0),
        ('wedge', 'BODY', [(-8.0, -2.0), (8.0, -2.0), (12.0, 26.0), (-12.0, 26.0)]),
        ('box', 'BODY', -12.0, 24.0, 24.0, 22.0, 3.0),
        ('wedge', 'BODY', [(-12.0, 44.0), (12.0, 44.0), (14.0, 68.0), (-14.0, 68.0)]),
        ('box', 'BODY_SHADE', -13.0, 44.0, 4.0, 12.0, 1.0),
        ('box', 'BODY_SHADE', 9.0, 44.0, 4.0, 12.0, 1.0),
        ('box', 'ACCENT', -2.0, 0.0, 4.0, 60.0, 0.0),
        ('box', 'GLASS', -8.0, 25.0, 16.0, 14.0, 5.0),
        ('box', 'BODY', -14.0, 30.0, 28.0, 7.0, 0.0),
        ('disc', 'DRIVER', 0.0, 31.0, 5.0),
        ('box', 'BODY_SHADE', -17.0, 64.0, 34.0, 6.0, 0.0),
        ('box', 'ACCENT', -15.0, 60.0, 30.0, 4.0, 2.0),
    ],
    # GameRenderer.drawObstacleCar (eski)
    'traffic': [
        ('box', 'BODY_SHADE', -18.0, 66.0, 36.0, 8.0, 0.0),
        ('box', 'BODY_SHADE', -16.0, 4.0, 32.0, 6.0, 0.0),
        ('box', 'TIRE', -20.0, 14.0, 8.0, 18.0, 0.0),
        ('box', 'TIRE', 12.0, 14.0, 8.0, 18.0, 0.0),
        ('box', 'TIRE', -20.0, 50.0, 8.0, 18.0, 0.0),
        ('box', 'TIRE', 12.0, 50.0, 8.0, 18.0, 0.0),
        ('box', 'BODY', -10.0, 8.0, 20.0, 62.0, 8.0),
        ('box', 'BODY', -6.0, -2.0, 12.0, 18.0, 6.0),
        ('box', 'GLASS', -7.0, 23.0, 14.0, 18.0, 6.0),
        ('box', 'BODY', -14.0, 29.0, 28.0, 8.0, 0.0),
        ('disc', 'DRIVER', 0.0, 29.0, 5.0),
        ('box', 'ACCENT', -4.0, 11.0, 8.0, 34.0, 0.0),
        ('box', 'TAIL', -15.0, 60.0, 9.0, 5.0, 2.0),
        ('box', 'TAIL', 6.0, 60.0, 9.0, 5.0, 2.0),
    ],
}


def old_car_layer(palette, unit=16, pad=6, flame=False, shape='hatchback'):
    """Eski cizici: duz renk, gradyan/alfa yok, alev keskin ucgen."""
    y1 = K.CAR_Y1 + (K.FLAME_GAP + K.FLAME_OL if flame else 0.0)
    x0, y0 = K.CAR_X0 - pad, K.CAR_Y0 - pad
    w = int(round((K.CAR_X1 + pad - x0) * unit))
    h = int(round((y1 + pad - y0) * unit))
    img = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    def px(x, y):
        return ((x - x0) * unit, (y - y0) * unit)

    for part in OLD_SHAPES[shape]:
        if part[0] == 'box':
            _, paint, left, top, pw, ph, corner = part
            col = palette[paint] + (255,)
            a, b = px(left, top), px(left + pw, top + ph)
            box = [a[0], a[1], b[0] - 1, b[1] - 1]
            if corner > 0:
                r = min(corner * unit, (b[0] - a[0]) / 2.0, (b[1] - a[1]) / 2.0)
                d.rounded_rectangle(box, radius=r, fill=col)
            else:
                d.rectangle(box, fill=col)
        elif part[0] == 'disc':
            _, paint, cx, cy, r = part
            col = palette[paint] + (255,)
            a, b = px(cx - r, cy - r), px(cx + r, cy + r)
            d.ellipse([a[0], a[1], b[0] - 1, b[1] - 1], fill=col)
        else:
            _, paint, pts = part
            d.polygon([px(x, y) for x, y in pts], fill=palette[paint] + (255,))

    if flame:
        start = K.CAR_Y1 + K.FLAME_GAP
        d.polygon([px(-K.FLAME_OW, start), px(K.FLAME_OW, start),
                   px(0.0, start + K.FLAME_OL)], fill=K.FLAME_OUTER + (255,))
        d.polygon([px(-K.FLAME_IW, start), px(K.FLAME_IW, start),
                   px(0.0, start + K.FLAME_IL)], fill=K.FLAME_INNER + (255,))
    return img


# ---------------------------------------------------------------------------
# Yerlesim
# ---------------------------------------------------------------------------

CAR_UNITS = K.CAR_X1 - K.CAR_X0     # 40 birim


def render(shape, palette, new, flame, target_body_px):
    """Araci, gövdesi target_body_px genis olacak sekilde uret."""
    pad = 6.0
    ss = 6 if target_body_px < 80 else 3
    unit = (target_body_px / CAR_UNITS) * ss
    if new:
        img, _, _, _ = K.car_layer(palette, unit=unit, pad=pad, flame=flame,
                                   shape=shape, phase=1.37)
    else:
        img = old_car_layer(palette, unit=unit, pad=pad, flame=flame,
                            shape=shape)
    w, h = img.size
    return img.resize((max(1, round(w / ss)), max(1, round(h / ss))),
                      Image.LANCZOS)


def main():
    os.makedirs(OUT, exist_ok=True)
    from PIL import ImageFont
    try:
        font = ImageFont.truetype('arialbd.ttf', 20)
        small = ImageFont.truetype('arial.ttf', 15)
    except OSError:
        font = small = ImageFont.load_default()

    rows = [
        ('Sehir (hatchback)', 'hatchback', K.PLAYER_PALETTE, True),
        ('Yaris Sedan', 'race_sedan', K.PLAYER_PALETTE, False),
        ('Kas Arabasi', 'muscle', K.PLAYER_PALETTE, False),
        ('Super Araba', 'supercar', K.PLAYER_PALETTE, False),
        ('Trafik (sari)', 'traffic', K.traffic_palette(0), False),
        ('Trafik (camgobegi)', 'traffic', K.traffic_palette(1), False),
    ]

    # Once tum gorseller uretilir, satir yuksekligi ONLARDAN turetilir —
    # boylece hicbir arac hucreden tasip kirpilmaz.
    grid = []
    for label, shape, palette, flame in rows:
        cells = []
        for new in (False, True):
            cells.append((render(shape, palette, new, flame, GAME_W),
                          render(shape, palette, new, flame, GAME_W * ZOOM)))
        grid.append((label, cells))

    pad_x, gap = 34, 26
    col_w = max(g.size[0] + gap + z.size[0] for _, cs in grid for g, z in cs) + 60
    W = pad_x * 2 + col_w * 2
    row_h = [max(z.size[1] for _, z in cs) + 62 for _, cs in grid]
    H = 76 + sum(row_h)
    canvas = Image.new('RGB', (W, H), PANEL)
    d = ImageDraw.Draw(canvas)

    d.text((pad_x, 24), 'ONCE  (2026-08-14 geometrisi)', font=font, fill=LINE)
    d.text((pad_x + col_w, 24), 'SONRA (2026-08-15)', font=font,
           fill=(0x56, 0xE9, 0xFF))

    top = 70
    for ri, (label, cells) in enumerate(grid):
        h = row_h[ri]
        d.rectangle([pad_x - 14, top, W - pad_x + 14, top + h - 12], fill=BG)
        d.text((pad_x - 6, top + 6), label, font=small, fill=LINE)
        base = top + h - 26
        for ci, (g, z) in enumerate(cells):
            cx = pad_x + ci * col_w
            canvas.paste(g, (cx + 14, base - g.size[1]), g)
            canvas.paste(z, (cx + 14 + g.size[0] + gap, base - z.size[1]), z)
        d.text((pad_x + 14, base + 4), f'sol: {GAME_W}px (oyun olcegi)   '
               f'sag: {GAME_W * ZOOM}px (4x)', font=small,
               fill=(0xB0, 0xBE, 0xD0))
        top += h

    path = os.path.abspath(os.path.join(OUT, 'car_before_after.png'))
    canvas.save(path)
    print('yazildi:', path, canvas.size)


if __name__ == '__main__':
    main()
