"""Kron Drive magaza gorselleri — ortak cizim katmani.

TUM renkler ve geometri KODDAN alindi, uydurma yok:
  * arac parcalari  : game/CarCatalog.kt  -> SHAPES (7 govde) + TRAFFIC
  * arac boyalari   : game/CarCatalog.kt  -> COLOR_KRON_RED (DEFAULT_COLOR_ID)
  * ortak renkler   : game/CarCatalog.kt  -> GLASS/TIRE/TRIM/DRIVER/TAIL/GLOSS
  * gradyanlar      : game/CarCatalog.kt  -> BODY_ROLL/ROOF_ROLL/REAR_FADE/...
  * boost alevi     : game/CarCatalog.kt  -> FLAME_* sabitleri
  * trafik araci    : game/CarCatalog.kt  -> TRAFFIC + trafficStyle()
  * trafik renkleri : game/GameEngine.kt  -> OBSTACLE_COLORS
  * gece temasi     : ui/game/GameRenderer.kt -> RoadTheme.NIGHT
  * yol/kerb/serit  : ui/game/GameRenderer.kt -> drawTrack
  * marka paleti    : ui/theme/Color.kt    -> KronColors

Arac koordinat uzayi prototipin ham uzayi: x -20..20, y -2..74 (burun yukari).

## Perspektif sozlesmesi (2026-08-15)

Kamera aracin USTUNDE ve biraz ARKASINDADIR. Bunun cizime iki zorunlu sonucu:

  1. Gordugumuz yuzeyin neredeyse tamami TAVAN/KAPUT duzlemidir.
  2. Aracin ARKA yuzu (tampon + stop lambalari) tavanla ayni duzlemde DEGIL,
     ona dik duran ve bize dogru KISALARAK yansiyan ince bir seritdir. Bu
     yuzden arka yuz gövde boyunun ~%7'sini kaplar, asagi dogru kararir ve
     tavanla arasinda ince bir kirilma cizgisi vardir.

Sahibin 2026-08-15 elestirisi tam olarak buydu: "stop lambasi ve tampon
o kadar tavan ile ayni paralelde olmamali".
"""

import math

from PIL import Image, ImageDraw, ImageFilter

# ---------------------------------------------------------------------------
# Palet (koddan birebir)
# ---------------------------------------------------------------------------

BODY        = (0xE1, 0x06, 0x00)   # CarColorDef.kron_red bodyArgb
BODY_SHADE  = (0x8E, 0x04, 0x00)   # ...shadeArgb
ACCENT      = (0xFF, 0xFF, 0xFF)   # ...accentArgb
GLASS       = (0x1E, 0x2A, 0x47)   # CarCatalog.GLASS_ARGB
TIRE        = (0x05, 0x05, 0x05)   # CarCatalog.TIRE_ARGB
TRIM        = (0xB9, 0xC6, 0xD7)   # CarCatalog.TRIM_ARGB
DRIVER      = (0xFF, 0xD3, 0x3D)   # CarCatalog.DRIVER_ARGB
TAIL        = (0xFF, 0x2D, 0x1F)   # CarCatalog.TAIL_ARGB
GLOSS       = (0xFF, 0xFF, 0xFF)   # CarCatalog.GLOSS_ARGB

FLAME_CORE  = (0xFF, 0xF6, 0xE0)   # CarCatalog.FLAME_CORE_ARGB
FLAME_INNER = (0xFF, 0xD3, 0x3D)   # CarCatalog.FLAME_INNER_ARGB
FLAME_OUTER = (0x29, 0xB6, 0xFF)   # CarCatalog.FLAME_OUTER_ARGB
FLAME_GAP, FLAME_OL, FLAME_OW, FLAME_IL, FLAME_IW = 6.0, 16.0, 5.0, 10.0, 3.0
FLAME_HALO_RADIUS = 13.0           # CarCatalog.FLAME_HALO_RADIUS

# Trafik araci (CarCatalog.trafficStyle) — govde disi sabitleri
OBST_SHADE  = (0x0A, 0x0D, 0x11)
OBST_DRIVER = (0x8E, 0xCA, 0xE6)
OBSTACLE_COLORS = [                # GameEngine.OBSTACLE_COLORS
    (0xFF, 0xD6, 0x0A),
    (0x00, 0xC2, 0xFF),
    (0xFF, 0xFF, 0xFF),
    (0xFF, 0x7B, 0x00),
]

# Gece temasi / yol (GameRenderer)
NIGHT_TOP   = (0x07, 0x11, 0x1F)
NIGHT_BOT   = (0x0F, 0x21, 0x38)
ROAD        = (0x3A, 0x40, 0x48)
KERB_RED    = (0xD6, 0x28, 0x28)
ROAD_LINE   = (0xEF, 0xEF, 0xEF)
CITY_WARM   = (0xFF, 0xD5, 0x4F)
CITY_COOL   = (0x90, 0xCA, 0xF9)

# Marka (KronColors)
ACCENT_YELLOW = (0xF5, 0xC1, 0x00)
ACCENT_BRIGHT = (0xFF, 0xD4, 0x3D)
BLUE_BRIGHT   = (0x56, 0xE9, 0xFF)
COIN          = (0xF6, 0xC0, 0x00)
COIN_RING     = (0x8F, 0x6A, 0x00)
COIN_SHINE    = (0xFF, 0xF0, 0xA8)
TEXT_PRIMARY  = (0xEA, 0xF1, 0xFB)

# Arac cizim kutusu (CarCatalog: ART_LEFT..ART_RIGHT / ART_TOP..ART_BOTTOM)
CAR_X0, CAR_X1 = -20.0, 20.0
CAR_Y0, CAR_Y1 = -2.0, 74.0

# ---------------------------------------------------------------------------
# Gradyanlar — CarCatalog.kt icindeki sabitlerin BIREBIR aynisi
#
# ('h'|'v', [(konum 0..1, ton kaydirma -1..+1)])
#   +1 = tam beyaza, -1 = tam siyaha. Kaydirma parcanin KENDI renginin
#   uzerine uygulanir; boylece 9 boyanin hepsinde ayni hacim hissi olusur.
# ---------------------------------------------------------------------------

BODY_ROLL = ('h', [(0.00, -0.52), (0.06, -0.24), (0.19, 0.30), (0.33, 0.13),
                   (0.52, 0.00), (0.72, -0.15), (0.88, -0.36), (1.00, -0.56)])
ROOF_ROLL = ('h', [(0.00, -0.26), (0.18, 0.20), (0.46, 0.07), (0.74, -0.08),
                   (1.00, -0.32)])
NOSE_FADE = ('v', [(0.00, -0.38), (0.30, -0.08), (1.00, 0.02)])
REAR_FADE = ('v', [(0.00, -0.12), (0.38, -0.46), (1.00, -0.70)])
GLASS_SHEEN = ('v', [(0.00, 0.42), (0.28, 0.12), (0.62, -0.16), (1.00, 0.04)])
WHEEL_ROLL = ('h', [(0.00, 0.02), (0.26, 0.26), (0.58, 0.10), (1.00, 0.00)])
TAIL_GLOW = ('v', [(0.00, 0.32), (0.55, 0.00), (1.00, -0.26)])

# ---------------------------------------------------------------------------
# Govde sekilleri — CarCatalog.kt ile BIREBIR ayni sira/koordinat
#   ('box',   paint, left, top, w, h, corner, grad, alpha)
#   ('disc',  paint, cx, cy, r, grad, alpha)
#   ('wedge', paint, [(x, y), ...], grad, alpha)
# ---------------------------------------------------------------------------


def _b(paint, left, top, w, h, corner=0.0, grad=None, alpha=1.0):
    return ('box', paint, left, top, w, h, corner, grad, alpha)


def _d(paint, cx, cy, r, grad=None, alpha=1.0):
    return ('disc', paint, cx, cy, r, grad, alpha)


def _w(paint, pts, grad=None, alpha=1.0):
    return ('wedge', paint, pts, grad, alpha)


# --- ortak arka yuz kalibi --------------------------------------------------
#
# top     : arka yuzun basladigi y (tavanin bittigi yer)
# halfTop : o noktadaki gövde yari genisligi
# halfBot : 74'teki yari genislik (perspektifte hafif daralir)


def _rear_face(top, half_top, half_bottom, tail_half=None, tail_inset=1.6):
    """Kisalmis arka yuz + kirilma cizgisi + ince stop seritleri."""
    bottom = CAR_Y1
    height = bottom - top
    tail_half = tail_half if tail_half is not None else half_bottom - 1.2
    strip_top = top + height * 0.30
    strip_h = height * 0.42
    return [
        _w('BODY_SHADE', [(-half_top, top), (half_top, top),
                          (half_bottom, bottom), (-half_bottom, bottom)],
           REAR_FADE),
        # Tavan ile arka yuzun kirilma cizgisi: isik burada kirilir, gozun
        # "iki ayri duzlem" okumasini saglayan tek detay bu.
        _b('GLOSS', -half_top + 0.4, top - 0.55, (half_top - 0.4) * 2, 1.0,
           0.5, None, 0.55),
        _b('TAIL', -tail_half, strip_top, tail_half - tail_inset, strip_h,
           0.9, TAIL_GLOW),
        _b('TAIL', tail_inset, strip_top, tail_half - tail_inset, strip_h,
           0.9, TAIL_GLOW),
    ]


HATCHBACK = [
    # --- tekerlekler (gövdenin ALTINDA; camurluktan ~2 birim tasarlar) -----
    _b('TIRE', -20.0, 11.0, 7.0, 20.0, 2.4, WHEEL_ROLL),
    _b('TIRE', 13.0, 11.0, 7.0, 20.0, 2.4, WHEEL_ROLL),
    _b('TIRE', -20.0, 47.0, 7.0, 21.0, 2.4, WHEEL_ROLL),
    _b('TIRE', 13.0, 47.0, 7.0, 21.0, 2.4, WHEEL_ROLL),
    # --- gövde ust yuzeyi ---------------------------------------------------
    _b('BODY', -15.5, 16.0, 31.0, 56.0, 6.0, BODY_ROLL),
    _w('BODY', [(-9.0, -2.0), (9.0, -2.0), (15.5, 22.0), (-15.5, 22.0)],
       BODY_ROLL),
    _b('BODY_SHADE', -8.4, -2.0, 16.8, 2.2, 1.0, NOSE_FADE),   # on lip
    # --- seritler: kaput ve bagaj uzerinde, CAMI KESMEZ --------------------
    _b('ACCENT', -2.8, 0.6, 5.6, 20.4, 1.2, None, 0.88),
    _b('ACCENT', -2.8, 47.6, 5.6, 20.4, 1.2, None, 0.88),
    # --- kokpit: on cam / TAVAN (omuzlardan icerde) / arka cam -------------
    _b('GLASS', -11.4, 21.5, 22.8, 10.2, 4.0, GLASS_SHEEN),
    _d('DRIVER', 0.0, 26.4, 4.0),
    _b('BODY', -12.4, 30.6, 24.8, 10.4, 3.0, ROOF_ROLL),
    _b('BODY_SHADE', -12.2, 39.4, 24.4, 1.5, 0.7, None, 0.32),  # tavan/arka temas
    _b('GLASS', -11.0, 40.4, 22.0, 7.0, 3.0, GLASS_SHEEN),
    # --- spekuler vurgu: sol omuzda ince uzun parlama ----------------------
    _b('GLOSS', -14.3, 13.0, 1.5, 50.0, 0.75, None, 0.14),
] + _rear_face(top=69.0, half_top=14.9, half_bottom=13.8)

# 1980'ler Turk sedani (CarCatalog.KUS_SLX). Kimlik: kose yaricapi 1.6 (en
# kare govde), KISA dik on cam + UZUN duz tavan, tam boy krom yan serit,
# dikdortgen farlar. Tamamen kozmetik.
KUS_SLX = [
    _b('TIRE', -20.0, 12.0, 7.0, 19.0, 1.4, WHEEL_ROLL),
    _b('TIRE', 13.0, 12.0, 7.0, 19.0, 1.4, WHEEL_ROLL),
    _b('TIRE', -20.0, 47.0, 7.0, 20.0, 1.4, WHEEL_ROLL),
    _b('TIRE', 13.0, 47.0, 7.0, 20.0, 1.4, WHEEL_ROLL),
    _b('BODY', -16.0, 10.0, 32.0, 60.0, 1.6, BODY_ROLL),
    _w('BODY', [(-14.5, -2.0), (14.5, -2.0), (16.0, 12.0), (-16.0, 12.0)],
       BODY_ROLL),
    _b('BODY_SHADE', -14.0, -2.0, 28.0, 2.2, 0.6, NOSE_FADE),
    # ince yatay izgara + krom cubuk
    _b('BODY_SHADE', -9.5, 0.6, 19.0, 2.6, 0.5, NOSE_FADE),
    _b('TRIM', -9.5, 1.6, 19.0, 0.7, 0.35, None, 0.9),
    # dikdortgen farlar: TRIM cerceve + GLOSS lens (32 px'de iki parlak nokta)
    _b('TRIM', -13.8, 0.4, 4.0, 3.0, 0.5, None, 0.95),
    _b('TRIM', 9.8, 0.4, 4.0, 3.0, 0.5, None, 0.95),
    _b('GLOSS', -13.4, 0.8, 3.2, 2.2, 0.4, None, 0.85),
    _b('GLOSS', 10.2, 0.8, 3.2, 2.2, 0.4, None, 0.85),
    # tam boy krom yan seritler
    _b('TRIM', -16.0, 12.0, 1.3, 52.0, 0.5, None, 0.88),
    _b('TRIM', 14.7, 12.0, 1.3, 52.0, 0.5, None, 0.88),
    # garnitur bantlari (kaput + bagaj), yatay
    _b('ACCENT', -8.0, 5.4, 16.0, 1.4, 0.6, None, 0.8),
    _b('ACCENT', -9.0, 55.0, 18.0, 1.6, 0.7, None, 0.8),
    _b('GLOSS', -14.4, 12.0, 1.3, 50.0, 0.65, None, 0.14),
    _b('GLASS', -12.6, 21.8, 25.2, 8.2, 1.6, GLASS_SHEEN),
    _d('DRIVER', 0.0, 25.8, 3.8),
    _b('BODY', -13.4, 30.0, 26.8, 12.0, 1.2, ROOF_ROLL),
    _b('BODY_SHADE', -13.2, 40.6, 26.4, 1.4, 0.6, None, 0.32),
    _b('GLASS', -12.2, 41.8, 24.4, 6.4, 1.4, GLASS_SHEEN),
] + _rear_face(top=69.0, half_top=15.6, half_bottom=14.6)

RACE_SEDAN = [
    _b('TIRE', -20.0, 10.0, 7.0, 21.0, 2.2, WHEEL_ROLL),
    _b('TIRE', 13.0, 10.0, 7.0, 21.0, 2.2, WHEEL_ROLL),
    _b('TIRE', -20.0, 46.0, 7.0, 22.0, 2.2, WHEEL_ROLL),
    _b('TIRE', 13.0, 46.0, 7.0, 22.0, 2.2, WHEEL_ROLL),
    _b('BODY', -16.0, 14.0, 32.0, 56.0, 5.0, BODY_ROLL),
    _w('BODY', [(-8.0, -2.0), (8.0, -2.0), (16.0, 20.0), (-16.0, 20.0)],
       BODY_ROLL),
    _b('BODY_SHADE', -9.0, -2.0, 18.0, 2.4, 1.0, NOSE_FADE),
    # ikiz serit — yarisci kimligi
    _b('ACCENT', -5.0, 0.6, 3.4, 19.4, 0.9, None, 0.88),
    _b('ACCENT', 1.6, 0.6, 3.4, 19.4, 0.9, None, 0.88),
    _b('ACCENT', -5.0, 46.6, 3.4, 11.0, 0.9, None, 0.88),
    _b('ACCENT', 1.6, 46.6, 3.4, 11.0, 0.9, None, 0.88),
    _b('GLASS', -11.9, 20.0, 23.8, 10.4, 4.0, GLASS_SHEEN),
    _d('DRIVER', 0.0, 25.0, 4.0),
    _b('BODY', -12.8, 29.8, 25.6, 10.6, 3.0, ROOF_ROLL),
    _b('BODY_SHADE', -12.6, 38.9, 25.2, 1.5, 0.7, None, 0.32),
    _b('GLASS', -11.4, 39.9, 22.8, 6.6, 3.0, GLASS_SHEEN),
    _b('GLOSS', -14.8, 12.0, 1.5, 50.0, 0.75, None, 0.14),
    # arka kanat: gövdenin USTUNDE durur -> altina ince golge duser
    _b('BODY_SHADE', -17.4, 62.6, 34.8, 3.0, 1.0, None, 0.5),
    _b('BODY_SHADE', -17.0, 58.4, 34.0, 4.6, 1.6, ROOF_ROLL),
    _b('ACCENT', -16.6, 58.6, 33.2, 1.2, 0.6, None, 0.6),
] + _rear_face(top=68.4, half_top=15.2, half_bottom=14.1)

MUSCLE = [
    _b('TIRE', -20.0, 9.0, 7.0, 22.0, 1.8, WHEEL_ROLL),
    _b('TIRE', 13.0, 9.0, 7.0, 22.0, 1.8, WHEEL_ROLL),
    _b('TIRE', -20.0, 45.0, 7.0, 24.0, 1.8, WHEEL_ROLL),
    _b('TIRE', 13.0, 45.0, 7.0, 24.0, 1.8, WHEEL_ROLL),
    # kaskati kose: kas arabasi kimligi kose yariçapindan okunur
    _b('BODY', -16.6, 12.0, 33.2, 58.0, 2.5, BODY_ROLL),
    _w('BODY', [(-12.0, -2.0), (12.0, -2.0), (16.6, 16.0), (-16.6, 16.0)],
       BODY_ROLL),
    _b('BODY_SHADE', -11.4, -2.0, 22.8, 2.6, 0.8, NOSE_FADE),
    # yan egzozlar (kisa, silindirik gölgeli — beyaz bar gibi durmasin)
    _b('TRIM', -18.3, 46.0, 2.1, 15.0, 0.9, WHEEL_ROLL, 0.85),
    _b('TRIM', 16.2, 46.0, 2.1, 15.0, 0.9, WHEEL_ROLL, 0.85),
    # kaput hava girisi
    _b('BODY_SHADE', -4.2, 10.0, 8.4, 9.0, 1.6, NOSE_FADE),
    _b('GLOSS', -4.0, 10.2, 8.0, 0.9, 0.4, None, 0.4),
    _b('ACCENT', -6.6, 0.0, 3.6, 8.6, 0.7, None, 0.88),
    _b('ACCENT', 3.0, 0.0, 3.6, 8.6, 0.7, None, 0.88),
    _b('ACCENT', -6.6, 47.4, 3.6, 19.0, 0.7, None, 0.88),
    _b('ACCENT', 3.0, 47.4, 3.6, 19.0, 0.7, None, 0.88),
    _b('GLASS', -12.5, 22.4, 25.0, 9.6, 2.5, GLASS_SHEEN),
    _d('DRIVER', 0.0, 27.2, 4.0),
    _b('BODY', -13.4, 31.2, 26.8, 10.0, 2.0, ROOF_ROLL),
    _b('BODY_SHADE', -13.2, 40.2, 26.4, 1.5, 0.7, None, 0.32),
    _b('GLASS', -12.0, 41.2, 24.0, 6.2, 2.0, GLASS_SHEEN),
    _b('GLOSS', -15.4, 11.0, 1.6, 52.0, 0.75, None, 0.14),
] + _rear_face(top=68.8, half_top=16.2, half_bottom=15.0)

# Station wagon (CarCatalog.MOUNTAIN_GOAT). Kimlik: 22 birimlik UZUN tavan,
# tavan bagaj raylari (ACCENT) + capraz cubuklar (TRIM), arka yan camlar,
# genis bagaj kapagi cami. Beyaz boyayla (COLOR_GLACIER) tasarlandi.
MOUNTAIN_GOAT = [
    _b('TIRE', -20.0, 11.0, 7.0, 21.0, 2.0, WHEEL_ROLL),
    _b('TIRE', 13.0, 11.0, 7.0, 21.0, 2.0, WHEEL_ROLL),
    _b('TIRE', -20.0, 46.0, 7.0, 22.0, 2.0, WHEEL_ROLL),
    _b('TIRE', 13.0, 46.0, 7.0, 22.0, 2.0, WHEEL_ROLL),
    _b('BODY', -16.2, 12.0, 32.4, 58.0, 2.2, BODY_ROLL),
    _w('BODY', [(-13.0, -2.0), (13.0, -2.0), (16.2, 14.0), (-16.2, 14.0)],
       BODY_ROLL),
    _b('BODY_SHADE', -12.4, -2.0, 24.8, 2.4, 0.8, NOSE_FADE),
    # izgara tamponun HEMEN arkasinda (beyaz kaputun ortasinda yuzmesin)
    _b('BODY_SHADE', -8.0, 1.0, 16.0, 3.6, 0.9, NOSE_FADE),
    # vurgu camlardan ONCE: ortada camlarin altinda kalir
    _b('GLOSS', -14.8, 14.0, 1.5, 52.0, 0.75, None, 0.14),
    _b('GLASS', -12.4, 20.6, 24.8, 9.6, 2.4, GLASS_SHEEN),
    _d('DRIVER', 0.0, 25.2, 4.0),
    # UZUN tavan — SW okunmasini saglayan ana kutle
    _b('BODY', -13.2, 30.0, 26.4, 22.0, 2.0, ROOF_ROLL),
    # tavan bagaji: iki boyuna ray + iki capraz cubuk
    # raylar TAM opak (32 px'deki tek okunur imza), capraz cubuklar sonuk
    _b('ACCENT', -10.6, 30.6, 2.0, 20.4, 0.8),
    _b('ACCENT', 8.6, 30.6, 2.0, 20.4, 0.8),
    _b('TRIM', -10.6, 34.0, 21.2, 0.9, 0.4, None, 0.5),
    _b('TRIM', -10.6, 47.4, 21.2, 0.9, 0.4, None, 0.5),
    # arka yan camlar (tavanin YANINDA, omuz duzleminde)
    _b('GLASS', -15.6, 34.0, 2.2, 14.0, 0.9, GLASS_SHEEN),
    _b('GLASS', 13.4, 34.0, 2.2, 14.0, 0.9, GLASS_SHEEN),
    _b('BODY_SHADE', -13.0, 51.4, 26.0, 1.4, 0.6, None, 0.32),
    _b('GLASS', -12.6, 52.6, 25.2, 8.4, 1.8, GLASS_SHEEN),
    _b('ACCENT', -10.0, 63.6, 20.0, 2.0, 0.8, None, 0.85),
] + _rear_face(top=68.8, half_top=15.8, half_bottom=14.8)

# Ikinci kas arabasi (CarCatalog.MUSCLE_67). 60'lar/70'ler Amerikan kas
# arabasi DILI — hicbir gercek modelin kopyasi degil, marka adi gecmez.
# Mevcut MUSCLE'dan ayrildigi yerler: tek KALIN orta serit (MUSCLE'da iki
# ince), kola-sisesi bel (MUSCLE duz yanli), uzun kaput/kisa bagaj,
# fastback arka cam, dortlu yuvarlak far, kosellere itilmis stop lambalari
# + ortada iki krom egzoz. Tamamen kozmetik.
MUSCLE_67 = [
    _b('TIRE', -20.0, 10.0, 7.0, 21.0, 2.0, WHEEL_ROLL),
    _b('TIRE', 13.0, 10.0, 7.0, 21.0, 2.0, WHEEL_ROLL),
    _b('TIRE', -20.0, 44.0, 7.0, 25.0, 2.0, WHEEL_ROLL),
    _b('TIRE', 13.0, 44.0, 7.0, 25.0, 2.0, WHEEL_ROLL),
    # gövde TEK poligon: omuz 33.2 -> bel 29.6 -> kalca 33.8
    _w('BODY', [(-14.8, -2.0), (14.8, -2.0), (16.6, 14.0), (14.8, 30.0),
                (14.8, 40.0), (16.9, 56.0), (16.9, 70.0), (-16.9, 70.0),
                (-16.9, 56.0), (-14.8, 40.0), (-14.8, 30.0), (-16.6, 14.0)],
       BODY_ROLL),
    _b('BODY_SHADE', -13.8, -2.0, 27.6, 2.4, 0.8, NOSE_FADE),
    # kararmis izgara bandi (neredeyse tam genislik)
    _b('BODY_SHADE', -12.8, 0.6, 25.6, 4.2, 0.8, NOSE_FADE),
    # dortlu yuvarlak far: TRIM cerceve + GLOSS lens
    _d('TRIM', -11.0, 2.7, 1.6),
    _d('TRIM', -7.0, 2.7, 1.6),
    _d('TRIM', 7.0, 2.7, 1.6),
    _d('TRIM', 11.0, 2.7, 1.6),
    _d('GLOSS', -11.0, 2.7, 1.05),
    _d('GLOSS', -7.0, 2.7, 1.05),
    _d('GLOSS', 7.0, 2.7, 1.05),
    _d('GLOSS', 11.0, 2.7, 1.05),
    # KALIN orta serit (kaput) + uzerinde iki agizli kaput cikintisi
    _b('ACCENT', -4.7, 5.6, 9.4, 20.4, 1.2, None, 0.9),
    _b('GLOSS', -4.4, 9.4, 8.8, 0.9, 0.4, None, 0.45),
    _b('BODY_SHADE', -4.0, 10.0, 3.6, 8.6, 0.9, NOSE_FADE),
    _b('BODY_SHADE', 0.4, 10.0, 3.6, 8.6, 0.9, NOSE_FADE),
    # omuz vurgusu BELE gore hizali (-14.4): kola sisesi kenar y'ye gore
    # gezdigi icin kenara yapistirilirsa belde govdenin disina tasiyor
    _b('GLOSS', -14.4, 16.0, 1.5, 46.0, 0.75, None, 0.14),
    # kokpit ARKADA: kaput 28 birim, bagaj 15.1
    _b('GLASS', -12.4, 26.0, 24.8, 8.6, 2.6, GLASS_SHEEN),
    _d('DRIVER', 0.0, 30.2, 4.0),
    _b('BODY', -13.2, 34.6, 26.4, 10.2, 2.2, ROOF_ROLL),
    _b('ACCENT', -4.7, 34.9, 9.4, 9.6, 1.0, None, 0.9),
    _b('BODY_SHADE', -13.0, 43.6, 26.0, 1.5, 0.7, None, 0.32),
    # fastback: arka cam tavandan UZUN
    _b('GLASS', -12.2, 44.9, 24.4, 8.6, 2.2, GLASS_SHEEN),
    _b('ACCENT', -4.7, 53.8, 9.4, 13.4, 1.2, None, 0.9),
    # ordek kuyrugu bagaj dudagi (kanat DEGIL)
    _b('BODY_SHADE', -16.6, 66.6, 33.2, 2.0, 0.8, None, 0.38),
] + _rear_face(top=68.6, half_top=16.9, half_bottom=15.7, tail_inset=6.2) + [
    # ortada iki krom egzoz ucu (lambalar kosellere itildigi icin yer var)
    _b('TRIM', -4.6, 70.4, 3.6, 2.4, 1.1, WHEEL_ROLL, 0.85),
    _b('TRIM', 1.0, 70.4, 3.6, 2.4, 1.1, WHEEL_ROLL, 0.85),
]

SUPERCAR = [
    _b('TIRE', -20.0, 12.0, 7.0, 19.0, 2.6, WHEEL_ROLL),
    _b('TIRE', 13.0, 12.0, 7.0, 19.0, 2.6, WHEEL_ROLL),
    _b('TIRE', -20.0, 46.0, 7.0, 23.0, 2.6, WHEEL_ROLL),
    _b('TIRE', 13.0, 46.0, 7.0, 23.0, 2.6, WHEEL_ROLL),
    # kama burun -> daralan bel -> genisleyen arka omuz
    _w('BODY', [(-6.5, -2.0), (6.5, -2.0), (14.0, 26.0), (-14.0, 26.0)],
       BODY_ROLL),
    _b('BODY', -14.0, 24.0, 28.0, 22.0, 3.0, BODY_ROLL),
    _w('BODY', [(-14.0, 44.0), (14.0, 44.0), (16.8, 70.0), (-16.8, 70.0)],
       BODY_ROLL),
    _b('BODY_SHADE', -6.0, -2.0, 12.0, 2.0, 0.8, NOSE_FADE),
    # yan hava girisleri (genisleyen arkanin girintisi)
    _b('BODY_SHADE', -14.8, 47.0, 3.0, 13.0, 1.2, NOSE_FADE),
    _b('BODY_SHADE', 11.8, 47.0, 3.0, 13.0, 1.2, NOSE_FADE),
    _b('ACCENT', -2.2, 0.0, 4.4, 21.0, 0.9, None, 0.88),
    _b('ACCENT', -2.2, 46.6, 4.4, 10.0, 0.9, None, 0.88),
    _b('GLASS', -10.9, 21.6, 21.8, 10.0, 4.0, GLASS_SHEEN),
    _d('DRIVER', 0.0, 26.4, 4.0),
    _b('BODY', -11.6, 30.8, 23.2, 10.0, 3.0, ROOF_ROLL),
    _b('BODY_SHADE', -11.4, 39.8, 22.8, 1.5, 0.7, None, 0.32),
    _b('GLASS', -11.6, 40.8, 23.2, 6.0, 3.0, GLASS_SHEEN),
    _b('GLOSS', -12.9, 14.0, 1.5, 48.0, 0.75, None, 0.14),
    # buyuk spoiler: kanat + altinda golge (beyaz blok DEGIL)
    _b('BODY_SHADE', -17.8, 62.2, 35.6, 3.4, 1.0, None, 0.5),
    _b('BODY_SHADE', -17.6, 57.4, 35.2, 5.0, 1.8, ROOF_ROLL),
    _b('ACCENT', -17.2, 57.6, 34.4, 1.3, 0.6, None, 0.6),
] + _rear_face(top=69.4, half_top=16.8, half_bottom=15.6)

# Trafik araci: silueti bilerek DAHA KUTU (kose yaricapi kucuk, burun kunt)
# ki oyuncu araci ile 60 Hz'de karismasin. Palet farki da korunuyor:
# sari/camgobegi/beyaz/turuncu govde + mavi surucu.
TRAFFIC = [
    _b('TIRE', -20.0, 12.0, 7.0, 20.0, 1.6, WHEEL_ROLL),
    _b('TIRE', 13.0, 12.0, 7.0, 20.0, 1.6, WHEEL_ROLL),
    _b('TIRE', -20.0, 47.0, 7.0, 21.0, 1.6, WHEEL_ROLL),
    _b('TIRE', 13.0, 47.0, 7.0, 21.0, 1.6, WHEEL_ROLL),
    _b('BODY', -15.8, 12.0, 31.6, 57.0, 2.0, BODY_ROLL),
    _w('BODY', [(-13.0, -2.0), (13.0, -2.0), (15.8, 14.0), (-15.8, 14.0)],
       BODY_ROLL),
    _b('BODY_SHADE', -12.4, -2.0, 24.8, 2.4, 0.8, NOSE_FADE),
    _b('GLASS', -12.4, 21.0, 24.8, 10.0, 2.5, GLASS_SHEEN),
    _d('DRIVER', 0.0, 25.8, 4.0),
    _b('BODY', -12.8, 30.2, 25.6, 10.4, 2.0, ROOF_ROLL),
    _b('BODY_SHADE', -12.6, 39.2, 25.2, 1.5, 0.7, None, 0.32),
    _b('GLASS', -11.8, 40.2, 23.6, 6.4, 2.0, GLASS_SHEEN),
    _b('ACCENT', -3.0, 1.0, 6.0, 18.0, 1.0, None, 0.7),
    _b('GLOSS', -14.4, 14.0, 1.5, 50.0, 0.75, None, 0.14),
] + _rear_face(top=68.6, half_top=15.5, half_bottom=14.3)

SHAPES = {
    'hatchback': HATCHBACK,
    'kus_slx': KUS_SLX,
    'race_sedan': RACE_SEDAN,
    'muscle': MUSCLE,
    'mountain_goat': MOUNTAIN_GOAT,
    'muscle_67': MUSCLE_67,
    'supercar': SUPERCAR,
    'traffic': TRAFFIC,
}

PLAYER_PALETTE = {
    'BODY': BODY, 'BODY_SHADE': BODY_SHADE, 'ACCENT': ACCENT,
    'GLASS': GLASS, 'TIRE': TIRE, 'TRIM': TRIM, 'DRIVER': DRIVER,
    'TAIL': TAIL, 'GLOSS': GLOSS,
}

# CarCatalog.COLOR_GLACIER — Dag Kecisi'nin tasarlandigi boya. Trafik beyazi
# TAM beyaz (FFFFFF), bu KIRIK beyaz (EDF1F5) + belirgin gri golge.
GLACIER = (0xED, 0xF1, 0xF5)
GLACIER_SHADE = (0x98, 0xA2, 0xAE)
GLACIER_ACCENT = (0x2A, 0x33, 0x40)

GLACIER_PALETTE = dict(PLAYER_PALETTE,
                       BODY=GLACIER, BODY_SHADE=GLACIER_SHADE,
                       ACCENT=GLACIER_ACCENT)


def traffic_palette(index):
    """CarCatalog.trafficStyle: govde trafik rengi, shade 0A0D11, surucu 8ECAE6."""
    return {
        'BODY': OBSTACLE_COLORS[index], 'BODY_SHADE': OBST_SHADE,
        'ACCENT': ACCENT, 'GLASS': GLASS, 'TIRE': TIRE, 'TRIM': TRIM,
        'DRIVER': OBST_DRIVER, 'TAIL': TAIL, 'GLOSS': GLOSS,
    }


# ---------------------------------------------------------------------------
# Cizim yardimcilari
# ---------------------------------------------------------------------------

def _shift(col, s):
    """Ton kaydirma: +1 tam beyaz, -1 tam siyah (CarArtwork.shifted ile ayni)."""
    if s >= 0.0:
        return tuple(int(round(c + (255 - c) * s)) for c in col)
    k = 1.0 + s
    return tuple(int(round(c * k)) for c in col)


def _stop_color(col, stops, p):
    if p <= stops[0][0]:
        return _shift(col, stops[0][1])
    for i in range(len(stops) - 1):
        p0, s0 = stops[i]
        p1, s1 = stops[i + 1]
        if p <= p1:
            t = 0.0 if p1 <= p0 else (p - p0) / (p1 - p0)
            return _shift(col, s0 + (s1 - s0) * t)
    return _shift(col, stops[-1][1])


def _grad_patch(size, col, grad):
    """Parcanin bbox'i kadar gradyan yamasi uret."""
    w, h = size
    axis, stops = grad
    img = Image.new('RGB', (max(1, w), max(1, h)))
    d = ImageDraw.Draw(img)
    if axis == 'h':
        for x in range(max(1, w)):
            c = _stop_color(col, stops, x / max(1, w - 1))
            d.line([(x, 0), (x, h)], fill=c)
    else:
        for y in range(max(1, h)):
            c = _stop_color(col, stops, y / max(1, h - 1))
            d.line([(0, y), (w, y)], fill=c)
    return img


def _draw_part(img, part, palette, px):
    """Tek parcayi (maske + gradyan + alfa) katmana isler."""
    kind = part[0]
    paint = part[1]
    col = palette[paint]
    if kind == 'box':
        _, _, left, top, pw, ph, corner, grad, alpha = part
        a, b = px(left, top), px(left + pw, top + ph)
        shape = ('rr', [a[0], a[1], b[0] - 1, b[1] - 1],
                 min(corner * (b[0] - a[0]) / max(pw, 1e-6),
                     (b[0] - a[0]) / 2.0, (b[1] - a[1]) / 2.0))
        bbox = (a[0], a[1], b[0], b[1])
    elif kind == 'disc':
        _, _, cx, cy, r, grad, alpha = part
        a, b = px(cx - r, cy - r), px(cx + r, cy + r)
        shape = ('el', [a[0], a[1], b[0] - 1, b[1] - 1], 0)
        bbox = (a[0], a[1], b[0], b[1])
    else:
        _, _, pts, grad, alpha = part
        p = [px(x, y) for x, y in pts]
        shape = ('pg', p, 0)
        xs = [q[0] for q in p]
        ys = [q[1] for q in p]
        bbox = (min(xs), min(ys), max(xs), max(ys))

    mask = Image.new('L', img.size, 0)
    md = ImageDraw.Draw(mask)
    lvl = int(round(255 * max(0.0, min(1.0, alpha))))
    if shape[0] == 'rr':
        md.rounded_rectangle(shape[1], radius=shape[2], fill=lvl)
    elif shape[0] == 'el':
        md.ellipse(shape[1], fill=lvl)
    else:
        md.polygon(shape[1], fill=lvl)

    if grad is None:
        layer = Image.new('RGBA', img.size, col + (255,))
    else:
        x0, y0, x1, y1 = (int(math.floor(bbox[0])), int(math.floor(bbox[1])),
                          int(math.ceil(bbox[2])), int(math.ceil(bbox[3])))
        patch = _grad_patch((max(1, x1 - x0), max(1, y1 - y0)), col, grad)
        layer = Image.new('RGBA', img.size, col + (255,))
        layer.paste(patch, (x0, y0))
    layer.putalpha(mask)
    img.alpha_composite(layer)


# ---------------------------------------------------------------------------
# Boost alevi — yumusak, katmanli, sicaktan soguga
# ---------------------------------------------------------------------------

def _flame_layer(size, px, unit, start, phase=0.0):
    """CarArtwork.drawCarBoostFlame ile ayni katman semasi.

    1) halo (radyal, camgobegi)  2) dis pluma  3) ic pluma  4) sicak cekirdek
    Titresim: iki farkli frekansta sinus -> mekanik tekrar hissi olusmasin.
    """
    flick = 1.0 + 0.13 * math.sin(phase * 17.0) + 0.07 * math.sin(phase * 27.3)
    wob = 1.0 + 0.09 * math.sin(phase * 21.0 + 1.7)
    out_len = FLAME_OL * flick
    out_w = FLAME_OW * wob
    in_len = FLAME_IL * (1.0 + 0.10 * math.sin(phase * 23.0 + 0.6))
    in_w = FLAME_IW * wob

    layer = Image.new('RGBA', size, (0, 0, 0, 0))

    # 1) halo — radyal sonum (yumusak kenar bu katmandan gelir)
    halo = Image.new('RGBA', size, (0, 0, 0, 0))
    hd = ImageDraw.Draw(halo)
    steps = 16
    for i in range(steps, 0, -1):
        t = i / steps
        r = FLAME_HALO_RADIUS * t
        a = int(round(64 * (1.0 - t) ** 1.7))
        c = px(0.0, start + FLAME_HALO_RADIUS * 0.42)
        rr = r * unit
        hd.ellipse([c[0] - rr * 0.78, c[1] - rr, c[0] + rr * 0.78, c[1] + rr],
                   fill=FLAME_OUTER + (a,))
    layer.alpha_composite(halo)

    def plume(hw, length, color, alpha0, samples=26):
        """Kok genis/opak, uc sivri/saydam bir pluma. Dilim dilim cizilir."""
        pl = Image.new('RGBA', size, (0, 0, 0, 0))
        pd = ImageDraw.Draw(pl)
        for i in range(samples):
            t0 = i / samples
            t1 = (i + 1) / samples
            w0 = hw * (1.0 - t0 ** 1.5)
            w1 = hw * (1.0 - t1 ** 1.5)
            y0 = start + length * t0
            y1 = start + length * t1
            a = int(round(alpha0 * (1.0 - t0) ** 1.25))
            pd.polygon([px(-w0, y0), px(w0, y0), px(w1, y1), px(-w1, y1)],
                       fill=color + (a,))
        return pl

    layer.alpha_composite(plume(out_w, out_len, FLAME_OUTER, 215))
    layer.alpha_composite(plume(in_w, in_len, FLAME_INNER, 235))
    layer.alpha_composite(plume(in_w * 0.52, in_len * 0.62, FLAME_CORE, 250))
    return layer.filter(ImageFilter.GaussianBlur(max(0.6, unit * 0.5)))


# ---------------------------------------------------------------------------
# Arac katmani
# ---------------------------------------------------------------------------

def car_layer(palette, unit=16, pad=6, flame=False, shape='hatchback',
              phase=0.0):
    """Araci kendi RGBA katmaninda ciz. Donen: (image, x0, y0, unit).

    x0/y0 = katmandaki (0,0) pikselinin arac uzayindaki karsiligi.
    shape : SHAPES anahtari ('hatchback' | 'race_sedan' | 'muscle' |
            'supercar' | 'traffic')
    """
    parts = SHAPES[shape] if isinstance(shape, str) else shape
    y1 = CAR_Y1 + (FLAME_GAP + FLAME_OL if flame else 0.0)
    x0, y0 = CAR_X0 - pad, CAR_Y0 - pad
    w = int(round((CAR_X1 + pad - x0) * unit))
    h = int(round((y1 + pad - y0) * unit))
    img = Image.new('RGBA', (w, h), (0, 0, 0, 0))

    def px(x, y):
        return ((x - x0) * unit, (y - y0) * unit)

    if flame:
        # Alev KOKU tamponun hemen dibinde baslar (FLAME_GAP bir bosluk degil,
        # alevin toplam uzanma payi). Kopuk bir ucgen yerine egzozdan cikan
        # bir pluma gibi okunmasi icin.
        img.alpha_composite(
            _flame_layer((w, h), px, unit, CAR_Y1 + 0.8, phase))

    for part in parts:
        _draw_part(img, part, palette, px)
    return img, x0, y0, unit


# --- perspektif -------------------------------------------------------------

def _solve(a, b):
    n = len(b)
    m = [row[:] + [b[i]] for i, row in enumerate(a)]
    for c in range(n):
        p = max(range(c, n), key=lambda r: abs(m[r][c]))
        m[c], m[p] = m[p], m[c]
        pv = m[c][c]
        if abs(pv) < 1e-12:
            raise ValueError('tekil matris')
        m[c] = [v / pv for v in m[c]]
        for r in range(n):
            if r != c and m[r][c] != 0.0:
                f = m[r][c]
                m[r] = [v - f * w for v, w in zip(m[r], m[c])]
    return [m[i][n] for i in range(n)]


def perspective(img, out_size, dest_quad):
    """img'i out_size tuvaline, koseleri dest_quad olacak sekilde tasir.

    dest_quad = [(x,y) sol-ust, sag-ust, sag-alt, sol-alt] cikti pikselinde.
    Ust kenari dar verirsen kamera yukaridan/arkadan bakiyormus gibi olur
    (3/4 ust aci) — oyundaki geometri DEGISMEZ, sadece izlenme acisi doner.
    """
    w, h = img.size
    src = [(0, 0), (w, 0), (w, h), (0, h)]
    a, b = [], []
    for (dx, dy), (sx, sy) in zip(dest_quad, src):
        a.append([dx, dy, 1, 0, 0, 0, -dx * sx, -dy * sx]); b.append(sx)
        a.append([0, 0, 0, dx, dy, 1, -dx * sy, -dy * sy]); b.append(sy)
    coeffs = _solve(a, b)
    return img.transform(out_size, Image.PERSPECTIVE, coeffs, Image.BICUBIC)


def tilted_car(palette, target_w, tilt=0.62, squash=0.74, unit=16,
               flame=False, ss=2, shape='hatchback'):
    """Araci 3/4 ust aciya yatirip verilen genislige olceklenmis olarak dondurur.

    tilt   : ust kenarin genislik orani (1.0 = duz tepeden bakis)
    squash : perspektifin toplam yukseklik kisalmasi
    """
    car, _, _, _ = car_layer(palette, unit=unit, flame=flame, shape=shape)
    w, h = car.size
    ow = int(target_w * ss)
    oh = int(round(ow * (h / w) * squash))
    inset = ow * (1.0 - tilt) / 2.0
    quad = [(inset, 0), (ow - inset, 0), (ow, oh), (0, oh)]
    out = perspective(car, (ow, oh), quad)
    return out.resize((int(target_w), max(1, oh // ss)), Image.LANCZOS)


# --- zemin / efekt ----------------------------------------------------------

def vertical_gradient(size, top, bottom):
    w, h = size
    img = Image.new('RGB', (w, h))
    d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / max(1, h - 1)
        d.line([(0, y), (w, y)],
               fill=tuple(int(round(top[i] + (bottom[i] - top[i]) * t)) for i in range(3)))
    return img


def glow(layer, radius, strength=1.0):
    g = layer.filter(ImageFilter.GaussianBlur(radius))
    if strength != 1.0:
        alpha = g.split()[3].point(lambda v: min(255, int(v * strength)))
        g.putalpha(alpha)
    return g


def drop_shadow(layer, radius=10, offset=(0, 8), opacity=0.55):
    a = layer.split()[3].filter(ImageFilter.GaussianBlur(radius))
    a = a.point(lambda v: int(v * opacity))
    sh = Image.new('RGBA', layer.size, (0, 0, 0, 0))
    sh.putalpha(a)
    return sh, offset


def paste(base, layer, xy):
    base.alpha_composite(layer, (int(xy[0]), int(xy[1])))


def coin(size):
    """drawCoin ile ayni renk semasi: dolgu F6C000, halka 8F6A00, isik FFF0A8."""
    ss = 4
    s = size * ss
    img = Image.new('RGBA', (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse([0, 0, s - 1, s - 1], fill=COIN + (255,))
    d.ellipse([0, 0, s - 1, s - 1], outline=COIN_RING + (255,), width=max(1, int(s * 0.15)))
    d.rectangle([s * 0.4, s * 0.2, s * 0.6, s * 0.8], fill=COIN_SHINE + (255,))
    return img.resize((size, size), Image.LANCZOS)
