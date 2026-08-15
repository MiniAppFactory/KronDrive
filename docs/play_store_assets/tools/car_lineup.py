"""Garajdaki YEDI govdenin yan yana denetim goruntusu.

Her govde iki olcekte cizilir:
  * oyun olcegi ~32 px genislik (GameConfig.CAR_WIDTH_PX = 40 * 0.80)
  * ayni cizimin 4x buyutulmus hali (detay denetimi icin)

Alt bloklar iki ayri "karisiyor mu" kontrolu:
  1. IKI KAS ARABASI yan yana (Kas Arabasi vs Boga 67) — 2026-08-15'te
     eklenen ikinci kas arabasinin mevcut olandan siluetle ayrildigini
     dogrulamak icin. Beklenen fark: tek KALIN orta serit vs iki ince
     serit, kola-sisesi bel vs duz yan, arkada kokpit, fastback cam.
  2. Beyaz Dag Kecisi ile trafikteki BEYAZ engel araci. Oyuncu beyazi
     bilerek kirik (EDF1F5), trafik beyazi tam beyaz (FFFFFF).

Geometri kron_art.py'den, o da CarCatalog.kt'nin aynasi.

    py car_lineup.py
"""

import os

from PIL import Image, ImageDraw, ImageFont

import kron_art as K

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'previews')

BG = (0x3A, 0x40, 0x48)        # asfalt (drawTrack ROAD)
PANEL = (0x10, 0x14, 0x1A)
LINE = (0xEA, 0xF1, 0xFB)
DIM = (0xB0, 0xBE, 0xD0)
NEW = (0x56, 0xE9, 0xFF)

GAME_W = 32
ZOOM = 4
CAR_UNITS = K.CAR_X1 - K.CAR_X0

# (etiket, sekil, palet, yeni mi) — sira CarCatalog.shapes ile ayni
ROWS = [
    ('Sehir  0 coin / lv1', 'hatchback', K.PLAYER_PALETTE, False),
    ('Yaris Sedan  900 / lv2', 'race_sedan', K.PLAYER_PALETTE, False),
    ('Kus SLX  1500 / lv2', 'kus_slx', K.PLAYER_PALETTE, False),
    ('Dag Kecisi  1500 / lv2', 'mountain_goat', K.GLACIER_PALETTE, False),
    ('Kas Arabasi  1800 / lv4', 'muscle', K.PLAYER_PALETTE, False),
    ('Boga 67  2400 / lv5  << YENI', 'muscle_67', K.PLAYER_PALETTE, True),
    ('Super Araba  3200 / lv6', 'supercar', K.PLAYER_PALETTE, False),
]

# 1) Iki kas arabasi karisiyor mu?
MUSCLE_CHECK = [
    ('Kas Arabasi: 2 ince serit, duz yan',
     'muscle', K.PLAYER_PALETTE),
    ('Boga 67: 1 kalin serit, bel daralir',
     'muscle_67', K.PLAYER_PALETTE),
]

# 2) Beyaz oyuncu araci vs beyaz trafik araci
WHITE_CHECK = [
    ('Dag Kecisi (oyuncu beyazi EDF1F5)', 'mountain_goat', K.GLACIER_PALETTE),
    ('Trafik (tam beyaz FFFFFF)', 'traffic', K.traffic_palette(2)),
]


def render(shape, palette, target_body_px):
    """Araci, govdesi target_body_px genis olacak sekilde uret."""
    ss = 6 if target_body_px < 80 else 3
    unit = (target_body_px / CAR_UNITS) * ss
    img, _, _, _ = K.car_layer(palette, unit=unit, pad=6.0, shape=shape)
    w, h = img.size
    return img.resize((max(1, round(w / ss)), max(1, round(h / ss))),
                      Image.LANCZOS)


def main():
    os.makedirs(OUT, exist_ok=True)
    try:
        title = ImageFont.truetype('arialbd.ttf', 22)
        label = ImageFont.truetype('arialbd.ttf', 16)
        small = ImageFont.truetype('arial.ttf', 14)
    except OSError:
        title = label = small = ImageFont.load_default()

    cells = [(lb, render(sh, pal, GAME_W), render(sh, pal, GAME_W * ZOOM), new)
             for lb, sh, pal, new in ROWS]

    def prep(rows):
        return [(lb, render(sh, pal, GAME_W), render(sh, pal, GAME_W * ZOOM))
                for lb, sh, pal in rows]

    muscles = prep(MUSCLE_CHECK)
    whites = prep(WHITE_CHECK)

    gap = 26
    col_w = max(g.size[0] + gap + z.size[0] for _, g, z, _ in cells) + 56
    pad_x = 30
    W = pad_x * 2 + col_w * 2
    row_h = max(z.size[1] for _, _, z, _ in cells) + 58
    rows_n = (len(cells) + 1) // 2
    chk_h = max(z.size[1] for _, _, z in muscles + whites) + 62
    H = 88 + rows_n * row_h + 2 * (52 + chk_h)

    canvas = Image.new('RGB', (W, H), PANEL)
    d = ImageDraw.Draw(canvas)
    d.text((pad_x, 20), 'KRON DRIVE — garajdaki yedi govde', font=title,
           fill=LINE)
    d.text((pad_x, 50), 'sol: 32 px (oyun olcegi)    sag: 128 px (4x)    '
                        'hepsi ayni kutuda: x -20..20, y -2..74',
           font=small, fill=DIM)

    top = 84
    for i, (lb, g, z, new) in enumerate(cells):
        r, c = divmod(i, 2)
        x = pad_x + c * col_w
        y = top + r * row_h
        d.rectangle([x - 12, y, x + col_w - 18, y + row_h - 14], fill=BG)
        d.text((x - 4, y + 6), lb, font=label, fill=NEW if new else LINE)
        base = y + row_h - 26
        canvas.paste(g, (x + 12, base - g.size[1]), g)
        canvas.paste(z, (x + 12 + g.size[0] + gap, base - z.size[1]), z)

    y = top + rows_n * row_h + 16

    def block(heading, rows):
        nonlocal y
        d.text((pad_x, y), heading, font=label, fill=(0xF5, 0xC1, 0x00))
        y += 30
        for i, (lb, g, z) in enumerate(rows):
            x = pad_x + i * col_w
            d.rectangle([x - 12, y, x + col_w - 18, y + chk_h - 14], fill=BG)
            d.text((x - 4, y + 6), lb, font=small, fill=LINE)
            base = y + chk_h - 26
            canvas.paste(g, (x + 12, base - g.size[1]), g)
            canvas.paste(z, (x + 12 + g.size[0] + gap, base - z.size[1]), z)
        y += chk_h + 22

    block('KARISIYOR MU? iki kas arabasi yan yana', muscles)
    block('KARISIYOR MU? beyaz oyuncu araci vs beyaz trafik', whites)

    path = os.path.abspath(os.path.join(OUT, 'car_lineup_7.png'))
    canvas.save(path)
    print('yazildi:', path, canvas.size)


if __name__ == '__main__':
    main()
