"""Aday C -> uygulama ikonu (launcher + magaza).

Sahibin karari: C (perspektif yol + kerb + far konisi + kirmizi arac) hem
telefon ikonu hem magaza ikonu olacak. TEK kompozisyon, uc ciktiya dagitiliyor:

  1) legacy mipmap  : mipmap-*/ic_launcher.png ve ic_launcher_round.png
                      (minSdk 24 -> API 24/25 adaptive ikonu tanimaz)
  2) adaptive       : drawable/ic_launcher_background.xml  (zemin: gece
                      gradyani + asfalt + kerb + serit + ufuk + coin)
                      drawable/ic_launcher_foreground.png  (on plan: far
                      konisi + arac; govde 66 dp guvenli dairenin icinde)
  3) magaza ikonu   : docs/play_store_assets/play-store-icon-512.png

Adaptive katman dosya ADLARI ve TURLERI degismedi: arka plan yine vector XML,
on plan yine PNG; mipmap-anydpi-v26/*.xml'e dokunulmadi.

Calistirma:  py gen_launcher.py
"""

import os
import shutil
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import kron_art as K
import gen_icons as G

TOOLS = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.abspath(os.path.join(TOOLS, '..'))
RES = os.path.abspath(os.path.join(
    TOOLS, '..', '..', '..', 'source', 'app', 'src', 'main', 'res'))

MIPMAPS = [('mdpi', 48), ('hdpi', 72), ('xhdpi', 96), ('xxhdpi', 144), ('xxxhdpi', 192)]

MASTER = 1024          # her sey bu boydan LANCZOS ile kuculur
FG_PX = 432            # 108 dp @ xxxhdpi — mevcut foreground ile ayni boy
VIEWPORT = 108.0       # adaptive ikon vektor viewport'u


# ---------------------------------------------------------------------------
# 1) legacy mipmap + magaza ikonu
# ---------------------------------------------------------------------------

def write_mipmaps(master):
    made = []
    for name, px in MIPMAPS:
        d = os.path.join(RES, 'mipmap-' + name)
        sq = master.resize((px, px), Image.LANCZOS)
        p1 = os.path.join(d, 'ic_launcher.png')
        sq.save(p1)
        made.append(p1)
        # round: API 24/25 yuvarlak yuvali launcher'lar bunu ister
        p2 = os.path.join(d, 'ic_launcher_round.png')
        G.round_mask(sq).save(p2)
        made.append(p2)
    return made


def _backup_once(path, suffix='_old'):
    """Ilk uretimden onceki dosyayi bir kez saklar.

    Bilerek "bir kez": yedek, C'ye gecmeden ONCEKI ikonu tutmali. Her
    calistirmada uzerine yazsaydik ikinci calistirmada yedek de C olurdu ve
    geri donulecek nokta kaybolurdu.
    """
    root, ext = os.path.splitext(path)
    backup = root + suffix + ext
    if os.path.exists(path) and not os.path.exists(backup):
        shutil.copy2(path, backup)
    return backup


def write_store_icon(master):
    path = os.path.join(ASSETS, 'play-store-icon-512.png')
    backup = _backup_once(path)
    icon = master.resize((512, 512), Image.LANCZOS).convert('RGBA')
    # Play kurali: 512x512, 32 bit, SAYDAMLIK YOK.
    assert min(icon.split()[3].getdata()) == 255, 'saydam piksel var'
    icon.save(path)
    return path, backup


def write_candidate_c(master):
    """Aday sayfasindaki C dosyasi da secilen kompozisyonla ayni kalsin."""
    path = os.path.join(ASSETS, 'icon_512_c.png')
    backup = _backup_once(path)
    master.resize((512, 512), Image.LANCZOS).convert('RGBA').save(path)
    return path, backup


def write_app_icon_png(master):
    """res/drawable-nodpi/app_icon.png — ILK ACILISTAKI DIL EKRANI bunu kullanir.

    Neden ayri bir PNG: dil ekrani `painterResource` ile yukluyor ve o
    `mipmap-anydpi-v26/ic_launcher.xml` (adaptive-icon XML) ile calismiyor —
    yuklemeye kalkinca cokuyor. Yani ikon her degistiginde bu dosya da
    tazelenmeli, yoksa dil ekrani eski ikonu gosterir.
    """
    path = os.path.join(RES, 'drawable-nodpi', 'app_icon.png')
    master.resize((192, 192), Image.LANCZOS).convert('RGBA').save(path)
    return path


# ---------------------------------------------------------------------------
# 2) adaptive on plan (PNG)
# ---------------------------------------------------------------------------

def write_foreground():
    fg = G.c_subject(MASTER, center=True).resize((FG_PX, FG_PX), Image.LANCZOS)
    path = os.path.join(RES, 'drawable', 'ic_launcher_foreground.png')
    fg.save(path)
    return path, fg


# ---------------------------------------------------------------------------
# 3) adaptive arka plan (vector XML) — C'nin zemini ile AYNI geometri
# ---------------------------------------------------------------------------

def _p(t, u):
    """C'nin yol noktasi, 108'lik vector viewport'unda."""
    x, y = G.c_road_pt(t, u, VIEWPORT)
    return x, y


def _poly(points):
    head = 'M%.2f,%.2f' % points[0]
    return head + ''.join('L%.2f,%.2f' % q for q in points[1:]) + 'Z'


def _hex(rgb, alpha=255):
    return '#%02X%02X%02X%02X' % ((alpha,) + tuple(rgb))


def background_vector():
    road = _poly([_p(0, -1), _p(0, 1), _p(1, 1), _p(1, -1)])

    kerb = {-1: [], 1: []}          # -1 kirmizi blok, 1 beyaz blok
    for i in range(G.C_STEPS):
        t0, t1 = i / G.C_STEPS, (i + 1) / G.C_STEPS
        k0 = 8.0 * (0.18 + 0.82 * t0) / G._road_half(t0)
        k1 = 8.0 * (0.18 + 0.82 * t1) / G._road_half(t1)
        for side in (-1, 1):
            quad = [_p(t0, side), _p(t0, side * (1 + k0)),
                    _p(t1, side * (1 + k1)), _p(t1, side)]
            kerb[-1 if i % 2 == 0 else 1].append(_poly(quad))

    lanes = []
    for u in (-1.0 / 3.0, 1.0 / 3.0):
        for i in range(0, G.C_STEPS, 2):
            t0, t1 = i / G.C_STEPS, (i + 0.95) / G.C_STEPS
            h0, h1 = 2.0 / G._road_half(t0), 2.0 / G._road_half(t1)
            lanes.append(_poly([_p(t0, u - h0), _p(t0, u + h0),
                                _p(t1, u + h1), _p(t1, u - h1)]))

    hx, hy = _p(0, 0)                       # ufuk (kacis noktasi)
    ck = VIEWPORT / 512.0
    # Coin konumu gen_icons'ta TEK YERDE tanimli; burada tekrar yazilmaz.
    cx, cy = _p(G.C_COIN_T, G.C_COIN_U)
    cr = (G.C_COIN_PX / 2.0) * ck

    return """<?xml version="1.0" encoding="utf-8"?>
<!-- URETILDI: docs/play_store_assets/tools/gen_launcher.py — elle duzenleme.
     Aday C'nin zemin katmani: gece gradyani + asfalt + kerb + serit + ufuk
     parlamasi + coin. Geometri gen_icons.c_road_pt ile birebir ayni; 512'lik
     ikon 108'lik viewport'a olceklendi. Renkler koddan (kron_art.py). -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">

    <!-- Gece gokyuzu (RoadTheme.NIGHT dikey gradyani) -->
    <path android:pathData="M0,0h108v108h-108z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:type="linear"
                android:startX="0" android:startY="0"
                android:endX="0" android:endY="108"
                android:startColor="{sky_top}"
                android:endColor="{sky_bot}" />
        </aapt:attr>
    </path>

    <!-- Ufuktan gelen soluk sehir parlamasi -->
    <path android:pathData="M0,0h108v108h-108z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:type="radial"
                android:centerX="54" android:centerY="12"
                android:gradientRadius="58"
                android:startColor="{halo_in}"
                android:endColor="{halo_out}" />
        </aapt:attr>
    </path>

    <!-- Asfalt -->
    <path android:fillColor="{road}" android:pathData="{road_path}" />

    <!-- Kerb bloklari (drawTrack: kirmizi/beyaz almasik) -->
    <path android:fillColor="{kerb_red}" android:pathData="{kerb_red_path}" />
    <path android:fillColor="{line}" android:pathData="{kerb_white_path}" />

    <!-- Serit ayirici kesik cizgiler (LANE_COUNT = 3) -->
    <path android:fillColor="{line_soft}" android:pathData="{lane_path}" />

    <!-- Ufuk isigi — yolun ucu isikla kapanir, kadraj derinlesir -->
    <path android:pathData="M0,0h108v108h-108z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:type="radial"
                android:centerX="{hx:.2f}" android:centerY="{hy:.2f}"
                android:gradientRadius="26"
                android:startColor="{hz_in}"
                android:endColor="{hz_out}" />
        </aapt:attr>
    </path>

    <!-- Coin (drawCoin renkleri) -->
    <path android:fillColor="{coin}"
        android:pathData="M{cx_l:.2f},{cy:.2f}a{cr:.2f},{cr:.2f} 0 1,0 {cd:.2f},0a{cr:.2f},{cr:.2f} 0 1,0 -{cd:.2f},0Z" />
    <path android:strokeColor="{coin_ring}" android:strokeWidth="{ring_w:.2f}"
        android:pathData="M{cx_l:.2f},{cy:.2f}a{cr:.2f},{cr:.2f} 0 1,0 {cd:.2f},0a{cr:.2f},{cr:.2f} 0 1,0 -{cd:.2f},0Z" />
    <path android:fillColor="{coin_shine}"
        android:pathData="M{sx0:.2f},{sy0:.2f}h{sw:.2f}v{sh:.2f}h-{sw:.2f}z" />
</vector>
""".format(
        sky_top=_hex(K.NIGHT_TOP), sky_bot=_hex(K.NIGHT_BOT),
        halo_in=_hex((0x16, 0x9D, 0xFF), 46), halo_out=_hex((0x16, 0x9D, 0xFF), 0),
        road=_hex(K.ROAD), road_path=road,
        kerb_red=_hex(K.KERB_RED), kerb_red_path=''.join(kerb[-1]),
        line=_hex(K.ROAD_LINE), kerb_white_path=''.join(kerb[1]),
        line_soft=_hex(K.ROAD_LINE, 235), lane_path=''.join(lanes),
        hx=hx, hy=hy,
        hz_in=_hex(K.BLUE_BRIGHT, 86), hz_out=_hex(K.BLUE_BRIGHT, 0),
        coin=_hex(K.COIN), coin_ring=_hex(K.COIN_RING), coin_shine=_hex(K.COIN_SHINE),
        cx=cx, cy=cy, cx_l=cx - cr, cd=2 * cr, cr=cr, ring_w=cr * 0.30,
        sx0=cx - cr * 0.20, sy0=cy - cr * 0.60, sw=cr * 0.40, sh=cr * 1.20,
    )


def background_vector_preview(size=432):
    """Vector XML'in TARIF ETTIGI seyi Pillow ile cizer.

    c_ground() rasteri gaussian blur kullaniyor; vector'de blur yok, yerine
    radial gradyan var. Bu fonksiyon vector'un gercek ciktisini taklit eder ki
    "Android'de ne gorunecek" gozle dogrulanabilsin (adb/emulator yok).
    """
    k = size / VIEWPORT
    img = K.vertical_gradient((size, size), K.NIGHT_TOP, K.NIGHT_BOT).convert('RGBA')

    def radial(cx, cy, radius, rgb, a0):
        lay = Image.new('RGBA', (size, size), rgb + (0,))
        alpha = Image.new('L', (size, size))
        px = alpha.load()
        r2 = (radius * k) ** 2
        for y in range(size):
            dy2 = (y - cy * k) ** 2
            for x in range(size):
                d2 = (x - cx * k) ** 2 + dy2
                px[x, y] = 0 if d2 >= r2 else int(a0 * (1.0 - (d2 ** 0.5) / (radius * k)))
        lay.putalpha(alpha)
        img.alpha_composite(lay)

    radial(54, 12, 58, (0x16, 0x9D, 0xFF), 46)

    ss = 2
    lay = Image.new('RGBA', (size * ss, size * ss), (0, 0, 0, 0))
    from PIL import ImageDraw
    d = ImageDraw.Draw(lay)

    def sp(t, u):
        x, y = _p(t, u)
        return (x * k * ss, y * k * ss)

    d.polygon([sp(0, -1), sp(0, 1), sp(1, 1), sp(1, -1)], fill=K.ROAD + (255,))
    for i in range(G.C_STEPS):
        t0, t1 = i / G.C_STEPS, (i + 1) / G.C_STEPS
        k0 = 8.0 * (0.18 + 0.82 * t0) / G._road_half(t0)
        k1 = 8.0 * (0.18 + 0.82 * t1) / G._road_half(t1)
        col = (K.KERB_RED if i % 2 == 0 else K.ROAD_LINE) + (255,)
        for side in (-1, 1):
            d.polygon([sp(t0, side), sp(t0, side * (1 + k0)),
                       sp(t1, side * (1 + k1)), sp(t1, side)], fill=col)
    for u in (-1.0 / 3.0, 1.0 / 3.0):
        for i in range(0, G.C_STEPS, 2):
            t0, t1 = i / G.C_STEPS, (i + 0.95) / G.C_STEPS
            h0, h1 = 2.0 / G._road_half(t0), 2.0 / G._road_half(t1)
            d.polygon([sp(t0, u - h0), sp(t0, u + h0),
                       sp(t1, u + h1), sp(t1, u - h1)], fill=K.ROAD_LINE + (235,))
    img.alpha_composite(lay.resize((size, size), Image.LANCZOS))

    hx, hy = _p(0, 0)
    radial(hx, hy, 26, K.BLUE_BRIGHT, 86)

    ck = VIEWPORT / 512.0
    cx, cy = _p(G.C_COIN_T, G.C_COIN_U)
    cr = (G.C_COIN_PX / 2.0) * ck
    cl = Image.new('RGBA', (size * ss, size * ss), (0, 0, 0, 0))
    dc = ImageDraw.Draw(cl)
    box = [(cx - cr) * k * ss, (cy - cr) * k * ss, (cx + cr) * k * ss, (cy + cr) * k * ss]
    dc.ellipse(box, fill=K.COIN + (255,))
    dc.ellipse(box, outline=K.COIN_RING + (255,), width=max(1, int(cr * 0.30 * k * ss)))
    dc.rectangle([(cx - cr * 0.20) * k * ss, (cy - cr * 0.60) * k * ss,
                  (cx + cr * 0.20) * k * ss, (cy + cr * 0.60) * k * ss],
                 fill=K.COIN_SHINE + (255,))
    img.alpha_composite(cl.resize((size, size), Image.LANCZOS))
    return img


def write_adaptive_proof():
    """Kanit sayfasi: arka plan (vector), on plan (PNG), birlesik ve maskeli."""
    from PIL import ImageDraw
    size = 432
    bg = background_vector_preview(size)
    fg = G.c_subject(MASTER, center=True).resize((size, size), Image.LANCZOS)
    full = bg.copy()
    full.alpha_composite(fg)

    keep = int(round(size * 72.0 / 108.0))
    off = (size - keep) // 2
    masked = G.round_mask(full.crop((off, off, off + keep, off + keep)))

    # guvenli alan halkasi ciz
    guide = full.copy()
    gd = ImageDraw.Draw(guide)
    gd.ellipse([off, off, off + keep, off + keep], outline=(0x56, 0xE9, 0xFF, 220), width=3)
    gd.rectangle([off, off, off + keep, off + keep], outline=(0xFF, 0xD4, 0x3D, 120), width=1)

    checker = Image.new('RGBA', (size, size), (0x2B, 0x2B, 0x2B, 255))
    fg_on_check = checker.copy()
    fg_on_check.alpha_composite(fg)
    mask_on_check = checker.copy()
    mask_on_check.alpha_composite(masked)

    pad = 20
    sheet = Image.new('RGBA', (4 * size + 5 * pad, size + 2 * pad + 28),
                      (0x1A, 0x1A, 0x1A, 255))
    labels = ['1. arka plan (vector XML)', '2. on plan (PNG, seffaf)',
              '3. birlesik + 72dp maske alani', '4. MASKE SONRASI (launcher)']
    for i, im in enumerate([bg, fg_on_check, guide, mask_on_check]):
        x = pad + i * (size + pad)
        sheet.alpha_composite(im, (x, pad))
        ImageDraw.Draw(sheet).text((x, pad + size + 8), labels[i], fill=(0xEA, 0xF1, 0xFB))
    path = os.path.join(ASSETS, 'icon_adaptive_layers.png')
    sheet.convert('RGB').save(path)
    return path


def write_background():
    path = os.path.join(RES, 'drawable', 'ic_launcher_background.xml')
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write(background_vector())
    return path


# ---------------------------------------------------------------------------

def main():
    master = G.icon_c(MASTER).convert('RGBA')
    made = write_mipmaps(master)
    store, store_backup = write_store_icon(master)
    cand, cand_backup = write_candidate_c(master)
    app_icon = write_app_icon_png(master)
    fg, _ = write_foreground()
    bg = write_background()

    # Kontrol sayfasi: 48 px okunabilirlik + adaptive maske sonrasi
    sheet = os.path.join(ASSETS, 'icon_48_readability.png')
    G.c_readability().convert('RGB').save(sheet)
    proof = write_adaptive_proof()

    for p in made + [fg, bg, app_icon, store, cand, sheet, proof]:
        print('%-72s %s' % (os.path.relpath(p, os.path.join(TOOLS, '..', '..', '..')),
                            os.path.getsize(p)))
    for b in (store_backup, cand_backup):
        print('yedek: %s (%s)' % (b, 'var' if os.path.exists(b) else 'YOK'))


if __name__ == '__main__':
    main()
