"""512x512 magaza ikonu adaylari (A / B / C) + 48 px okunabilirlik sayfasi.

Cikti: docs/play_store_assets/icon_512_{a,b,c}.png  (RGB, saydamlik YOK)
       docs/play_store_assets/icon_48_readability.png
"""

import os
import sys

from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import kron_art as K

OUT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
S = 512
CENTER = S / 2.0
SAFE_R = S * 0.66 / 2.0          # adaptive maske guvenli dairesi (169 px)


# ---------------------------------------------------------------------------
# ortak parcalar
# ---------------------------------------------------------------------------

def night_bg():
    bg = K.vertical_gradient((S, S), K.NIGHT_TOP, K.NIGHT_BOT).convert('RGBA')
    # Ufuktan gelen soluk sehir parlamasi (NIGHT temasindaki sehir isiklari hissi)
    halo = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(halo)
    d.ellipse([-120, -190, S + 120, 210], fill=(0x16, 0x9D, 0xFF, 46))
    bg.alpha_composite(halo.filter(ImageFilter.GaussianBlur(60)))
    return bg


def player_car(body_h, flame=False, tilt=0.58, squash=0.62):
    """3/4 aciya yatirilmis oyuncu araci.

    body_h = govdenin (76 birimlik arac kutusu) cikti yuksekligi. Olcek
    buradan turetilir; boylece kare tuvale sigdigi ONCEDEN bilinir.
    """
    pad = 3.0
    ppu = body_h / (76.0 * squash)            # piksel / arac birimi (yatay)
    layer_w = (40.0 + 2 * pad) * ppu
    car, _, _, _ = K.car_layer(K.PLAYER_PALETTE, unit=16, pad=pad, flame=flame)
    w, h = car.size
    ss = 2
    ow = int(layer_w * ss)
    oh = int(round(ow * (h / w) * squash))
    inset = ow * (1.0 - tilt) / 2.0
    out = K.perspective(car, (ow, oh), [(inset, 0), (ow - inset, 0), (ow, oh), (0, oh)])
    out = out.resize((int(layer_w), max(1, oh // ss)), Image.LANCZOS)
    # NOT (2026-08-15): burada eskiden bir "egzoz isigi" halesi vardi. Alev o
    # zaman iki KESKIN ucgendi ve gövdeden kopuk duruyordu; hale onu gövdeye
    # baglamak icin eklenmisti. Alev artik kendi halosunu tasiyor (bkz.
    # kron_art._flame_layer), ikisi ust uste binince asfalta tasan buyuk mavi
    # bir leke olusuyordu — bu yuzden kaldirildi.
    return out


def lightning(h, color=K.ACCENT_YELLOW):
    """Marka simsegi (mevcut ikondaki ogenin sadelestirilmis hali)."""
    ss = 4
    pts = [(0.62, 0.0), (0.06, 0.52), (0.42, 0.52), (0.20, 1.0),
           (1.00, 0.40), (0.56, 0.40), (0.92, 0.0)]
    w = int(h * 0.66)
    img = Image.new('RGBA', (w * ss, h * ss), (0, 0, 0, 0))
    ImageDraw.Draw(img).polygon([(x * w * ss, y * h * ss) for x, y in pts],
                                fill=color + (255,))
    return img.resize((w, h), Image.LANCZOS)


def add_shadow(base, layer, xy, radius=14, dy=10, opacity=0.5):
    sh, off = K.drop_shadow(layer, radius=radius, offset=(0, dy), opacity=opacity)
    K.paste(base, sh, (xy[0] + off[0], xy[1] + off[1]))
    K.paste(base, layer, xy)


# ---------------------------------------------------------------------------
# A — kadraji dolduran arac + kacan camgobegi seritler
# ---------------------------------------------------------------------------

def icon_a():
    img = night_bg()

    streaks = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(streaks)
    for side in (-1, 1):
        bx = CENTER + side * 206          # tabandaki merkez
        tx = CENTER + side * 62           # tepedeki merkez (kacis noktasina dogru)
        d.polygon([(bx - 34, S + 30), (bx + 34, S + 30), (tx + 5, 60), (tx - 5, 60)],
                  fill=K.BLUE_BRIGHT + (170,))
    # Ustte soner: cizgi degil, hiz izi olsun
    fade = Image.linear_gradient('L').resize((S, S)).point(lambda v: min(255, int(v * 1.35)))
    streaks.putalpha(Image.composite(streaks.split()[3], Image.new('L', (S, S), 0), fade))
    img.alpha_composite(K.glow(streaks, 30, 1.0))
    img.alpha_composite(streaks.filter(ImageFilter.GaussianBlur(2)))

    car = player_car(body_h=348, flame=False, tilt=0.56, squash=0.60)
    x = int(CENTER - car.width / 2)
    y = int(CENTER - car.height / 2 + 10)
    add_shadow(img, car, (x, y), radius=18, dy=14, opacity=0.55)

    bolt = lightning(120)
    K.paste(img, K.glow(bolt, 16, 0.8), (S - bolt.width - 30, 24))
    K.paste(img, bolt, (S - bolt.width - 34, 22))
    return img.convert('RGB')


# ---------------------------------------------------------------------------
# B — ayni arac, mevcut sari halka korunarak (marka surekliligi)
# ---------------------------------------------------------------------------

def icon_b():
    img = night_bg()

    streaks = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(streaks)
    for side in (-1, 1):
        bx = CENTER + side * 118
        tx = CENTER + side * 44
        d.polygon([(bx - 20, 470), (bx + 20, 470), (tx + 5, 150), (tx - 5, 150)],
                  fill=K.BLUE_BRIGHT + (120,))
    img.alpha_composite(K.glow(streaks, 22, 0.8))
    img.alpha_composite(streaks.filter(ImageFilter.GaussianBlur(2)))

    # Halka: mevcut ikondaki olcuye yakin (yaricap ~0.39*512, kalinlik ~22)
    ring = Image.new('RGBA', (S * 2, S * 2), (0, 0, 0, 0))
    rd = ImageDraw.Draw(ring)
    r = 200 * 2
    rd.ellipse([S - r, S - r, S + r, S + r], outline=K.ACCENT_YELLOW + (255,), width=44)
    ring = ring.resize((S, S), Image.LANCZOS)
    img.alpha_composite(K.glow(ring, 18, 0.55))
    img.alpha_composite(ring)

    car = player_car(body_h=286, flame=False, tilt=0.56, squash=0.60)
    x = int(CENTER - car.width / 2)
    y = int(CENTER - car.height / 2 + 6)
    add_shadow(img, car, (x, y), radius=16, dy=12, opacity=0.6)

    bolt = lightning(96)
    K.paste(img, K.glow(bolt, 14, 0.8), (S - bolt.width - 46, 40))
    K.paste(img, bolt, (S - bolt.width - 50, 38))
    return img.convert('RGB')


# ---------------------------------------------------------------------------
# C — secilen ikon: perspektif yol + arac (48 px'te figur-zemin en guclusu)
#
# C artik iki ayri katman olarak da uretilebiliyor cunku adaptive launcher
# ikonu zemini ve on plani ayri drawable ister:
#   c_ground()  -> gece gradyani + yol + kerb + serit + ufuk parlamasi + coin
#   c_subject() -> far konisi + oyuncu araci (seffaf zeminde)
# Duz (tek parca) ikon bu ikisinin ust uste binmesidir; boylece magaza ikonu,
# legacy mipmap ve adaptive katmanlar TEK kompozisyondan turuyor.
# ---------------------------------------------------------------------------

# C'nin geometrisi 512'lik tuvale gore yazildi; k ile her boya olceklenir.
C_TOP_Y, C_BOT_Y = 108.0, 552.0
C_TOP_HALF, C_BOT_HALF = 46.0, 268.0
C_STEPS = 14

# Coin yerlesimi TEK YERDE: hem raster zemin (c_ground) hem de adaptive arka
# planin vector XML'i (gen_launcher.background_vector) bunu okur. Ikisinde ayri
# ayri yazildiginda duz ikon ile launcher ikonu birbirinden kaymisti.
# t=0.58 / u=-0.78: aracin sol ONUNDE bos asfalt (t=0.19 ufka cok yakindi ve
# genisleyen yeni gövde coin'i tamamen ortuyordu).
C_COIN_T, C_COIN_U, C_COIN_PX = 0.58, -0.78, 52.0


def _road_half(t, top_half=C_TOP_HALF, bot_half=C_BOT_HALF):
    return top_half + (bot_half - top_half) * (t ** 1.55)


def c_road_pt(t, u, size=S):
    """t: 0 ufuk .. 1 on; u: -1 sol kenar .. +1 sag kenar. Donen: piksel."""
    k = size / 512.0
    y = C_TOP_Y + (C_BOT_Y - C_TOP_Y) * t
    return ((256.0 + u * _road_half(t)) * k, y * k)


def c_ground(size=S, coin=False):
    # coin VARSAYILAN OLARAK KAPALI (sahibi, 2026-08-15): adaptive maske
    # ikonu daireye kirpinca coin'in yalnizca bir dilimi kaliyor ve
    # ikonun sol alt kosesinde anlamsiz SARI BIR CIZGI gibi gorunuyordu.
    """C'nin zemin katmani: gece gokyuzu + asfalt + kerb + serit + ufuk."""
    k = size / 512.0
    img = K.vertical_gradient((size, size), K.NIGHT_TOP, K.NIGHT_BOT).convert('RGBA')
    halo = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    ImageDraw.Draw(halo).ellipse([-120 * k, -190 * k, size + 120 * k, 210 * k],
                                 fill=(0x16, 0x9D, 0xFF, 46))
    img.alpha_composite(halo.filter(ImageFilter.GaussianBlur(int(60 * k) + 1)))

    ss = 2
    road = Image.new('RGBA', (size * ss, size * ss), (0, 0, 0, 0))
    d = ImageDraw.Draw(road)

    def sp(t, u):
        x, y = c_road_pt(t, u, size)
        return (x * ss, y * ss)

    d.polygon([sp(0, -1), sp(0, 1), sp(1, 1), sp(1, -1)], fill=K.ROAD + (255,))

    # Kerb bloklari (drawTrack: 8 px genislik, kirmizi/beyaz almasik).
    # 48 px'te blok sayisi az olmali, yoksa kenar gurultuye doner.
    for i in range(C_STEPS):
        t0, t1 = i / C_STEPS, (i + 1) / C_STEPS
        col = (K.KERB_RED if i % 2 == 0 else K.ROAD_LINE) + (255,)
        for side in (-1, 1):
            k0 = 8.0 * (0.18 + 0.82 * t0) / _road_half(t0)
            k1 = 8.0 * (0.18 + 0.82 * t1) / _road_half(t1)
            d.polygon([sp(t0, side), sp(t0, side * (1 + k0)),
                       sp(t1, side * (1 + k1)), sp(t1, side)], fill=col)

    # Kesik serit cizgileri (LANE_COUNT = 3 -> u = -1/3 ve +1/3)
    for u in (-1.0 / 3.0, 1.0 / 3.0):
        for i in range(0, C_STEPS, 2):
            t0, t1 = i / C_STEPS, (i + 0.95) / C_STEPS
            hw0 = 2.0 / _road_half(t0)
            hw1 = 2.0 / _road_half(t1)
            d.polygon([sp(t0, u - hw0), sp(t0, u + hw0),
                       sp(t1, u + hw1), sp(t1, u - hw1)],
                      fill=K.ROAD_LINE + (235,))
    img.alpha_composite(road.resize((size, size), Image.LANCZOS))

    # Ufuk parlamasi — yolun ucu isikla kapanir, kadraj derinlesir
    hz = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    cx = size / 2.0
    ty = C_TOP_Y * k
    ImageDraw.Draw(hz).ellipse([cx - 118 * k, ty - 44 * k, cx + 118 * k, ty + 26 * k],
                               fill=K.BLUE_BRIGHT + (86,))
    img.alpha_composite(hz.filter(ImageFilter.GaussianBlur(int(34 * k) + 1)))

    if coin:
        c = K.coin(max(6, int(round(C_COIN_PX * k))))
        cx, cy = c_road_pt(C_COIN_T, C_COIN_U, size)
        pos = (cx - c.width / 2, cy - c.height / 2)
        K.paste(img, K.glow(c, max(2, int(12 * k)), 0.7), pos)
        K.paste(img, c, pos)
    return img


def c_subject(size=S, center=False, body_frac=None):
    """C'nin on plan katmani: far konisi + oyuncu araci (seffaf zemin).

    center=False : duz ikondaki yerlesim (arac alt kenara oturur).
    center=True  : adaptive on plan — arac GOVDESI tuvalin tam ortasina gelir,
                   boylece 66% maske dairesi tekerlekleri kesmez.
    body_frac    : arac govdesinin tuval yuksekligine orani. Adaptive'de 0.42;
                   govde kutusu 40x76 birim -> ekranda 189x215 px, kosegen yari
                   capi 143 px, guvenli dairenin yaricapi 170 px. Tekerlekler
                   (en dis kose) daireye 27 px kala kaliyor, kirpilmiyorlar.
    """
    if body_frac is None:
        body_frac = 0.42 if center else 0.492
    k = size / 512.0
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    car = player_car(body_h=body_frac * size, flame=True, tilt=0.56, squash=0.60)
    x = int(size / 2.0 - car.width / 2)
    if center:
        # car_layer y araligi: -5 .. 99 birim (pad 3 + alev 22). Govde -2..74
        # yani katmanin %39.4'unde ortalanir; onu tuval merkezine tasi.
        y = int(size / 2.0 - 0.394 * car.height)
    else:
        y = int(size - car.height - 10 * k)

    # Far konisi (drawNightHeadlights: sicak FFFAD2, aracin ONUNE dogru acilir).
    # 48 px'te ikonun en guclu isik sekli bu — gece yarisi hemen okunuyor.
    cx = size / 2.0
    nose_y = y + 6 * k
    cone = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    ImageDraw.Draw(cone).polygon(
        [(cx - 118 * k, nose_y + 8 * k), (cx - 30 * k, nose_y - 150 * k),
         (cx + 30 * k, nose_y - 150 * k), (cx + 118 * k, nose_y + 8 * k)],
        fill=(0xFF, 0xFA, 0xD2, 62))
    img.alpha_composite(cone.filter(ImageFilter.GaussianBlur(int(20 * k) + 1)))

    add_shadow(img, car, (x, y), radius=int(18 * k) + 1, dy=12 * k, opacity=0.6)
    return img


def icon_c(size=S):
    img = c_ground(size)
    img.alpha_composite(c_subject(size))
    return img.convert('RGB')


# ---------------------------------------------------------------------------

def round_mask(img, inset=0.0):
    """Daire maskesi (adaptive/round launcher kirpmasi). inset: kenar payi orani."""
    s = img.size[0]
    ss = 4
    m = Image.new('L', (s * ss, s * ss), 0)
    pad = inset * s * ss
    ImageDraw.Draw(m).ellipse([pad, pad, s * ss - 1 - pad, s * ss - 1 - pad], fill=255)
    m = m.resize((s, s), Image.LANCZOS)
    out = img.convert('RGBA').copy()
    out.putalpha(m)
    return out


def readability_sheet(columns, checker=(0x2B, 0x2B, 0x2B)):
    """columns: [(etiket, 512'lik RGBA gorsel)]. Her sutun: gercek 48 px + 4x buyutme."""
    cols = len(columns)
    cw, pad = 216, 24
    top = 24
    sheet = Image.new('RGBA', (cols * (cw + pad) + pad, top + 48 + 3 * pad + 192 + 34),
                      checker + (255,))
    d = ImageDraw.Draw(sheet)
    for i, (label, im) in enumerate(columns):
        ic = im.convert('RGBA').resize((48, 48), Image.LANCZOS)
        x = pad + i * (cw + pad)
        sheet.alpha_composite(ic, (x + (cw - 48) // 2, top))
        sheet.alpha_composite(ic.resize((192, 192), Image.NEAREST), (x + 12, top + pad + 48))
        for j, line in enumerate(label.split('\n')):
            d.text((x + 12, top + pad + 48 + 192 + 8 + j * 13), line, fill=(255, 255, 255))
    d.text((pad, 6), 'ust sira: GERCEK 48 px  |  alt: ayni piksellerin 4x buyutmesi',
           fill=(0xA8, 0xB4, 0xC4))
    return sheet


def adaptive_preview(size=S):
    """Launcher'in gercekten gosterdigi sey: 108 dp katmanin ORTA %66.7'si,
    daire maske ile kirpilmis ve ikon yuvasina buyutulmus hali."""
    layer = c_ground(size)
    layer.alpha_composite(c_subject(size, center=True))
    keep = int(round(size * 72.0 / 108.0))          # maske capi = 72 dp
    off = (size - keep) // 2
    return round_mask(layer.crop((off, off, off + keep, off + keep)).resize(
        (size, size), Image.LANCZOS))


def c_readability():
    """C'nin son hali: duz, yuvarlak maskeli ve adaptive maske sonrasi."""
    flat = icon_c().convert('RGBA')
    return readability_sheet([
        ('C - duz (magaza ikonu / legacy mipmap)', flat),
        ('C - daire maske (ic_launcher_round.png)', round_mask(flat)),
        ('C - ADAPTIVE, maske sonrasi (API 26+)\n(orta %66.7 kirpma + daire)',
         adaptive_preview()),
    ])


def main():
    jobs = [('a', icon_a), ('b', icon_b), ('c', icon_c)]
    for name, fn in jobs:
        path = os.path.join(OUT, 'icon_512_%s.png' % name)
        img = fn()
        assert img.size == (S, S) and img.mode == 'RGB'
        # Play: 32 bit PNG, saydamlik YOK -> RGBA ama alfa her pikselde 255.
        img = img.convert('RGBA')
        assert min(img.split()[3].getdata()) == 255, 'saydam piksel var'
        img.save(path)
        print('%-8s %s' % (name.upper(), path))
    sheet = os.path.join(OUT, 'icon_48_readability.png')
    c_readability().convert('RGB').save(sheet)
    print('sheet    ' + sheet)


if __name__ == '__main__':
    main()
