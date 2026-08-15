"""1024x500 feature graphic.

Sol %40: gercek marka logosu (res/drawable-nodpi/kron_logo.png) + KRON DRIVE
yazisi. Sag %60: gece otoyolu, uc seritte hacimli yaris otomobilleri.

TEK PERSPEKTIF MODELI (elle konumlandirma YOK)
----------------------------------------------
Duz bir yolun tum paralel dogrulari ekranda TEK bir kacis noktasinda (VP)
birlesir; bu yuzden yolun yari genisligi ekran y'sinde DOGRUSALDIR:

    s(y)    = (y - VP_y) / (Y_NEAR - VP_y)      # 0 = ufuk, 1 = yakin kenar
    half(y) = HALF_NEAR * s(y)
    cx(y)   = VP_x + (X_NEAR - VP_x) * s(y)

Bir arac yalnizca (serit indeksi, derinlik) ile yerlestirilir; konumu, olcegi
ve donus acisi bu modelden TURETILIR:

    konum = o seridin VP'den cikan isini uzerinde
    olcek = serit genisliginin sabit orani  -> derinligin fonksiyonu
    aci   = o noktadan VP'ye bakan yonun acisi (burun VP'ye donuk)

Alev/golge/far araca AIT katmanda cizilir, dolayisiyla ayni donusumden gecer;
"alev araçtan kopmus ve asagi bakiyor" hatasi bu yuzden yapisal olarak imkansiz.

Derinlik ornekleme: z dunya mesafesi, s = 1/(1+z). Kerb bloklari ve serit
cizgileri z ekseninde ESIT araliklarla dizilir; ekranda dogru sikismayi
kendiliginden verir.
"""

import math
import os
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageFont

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import kron_art as K
from kron_car3d import realistic_car

TOOLS = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.abspath(os.path.join(TOOLS, '..'))
LOGO = os.path.abspath(os.path.join(
    TOOLS, '..', '..', '..', 'source', 'app', 'src', 'main', 'res',
    'drawable-nodpi', 'kron_logo.png'))

W, H = 1024, 500
SS = 2                                    # supersampling

FONT_BLACK = r'C:\Windows\Fonts\ariblk.ttf'
FONT_BOLD = r'C:\Windows\Fonts\arialbd.ttf'

# --- perspektif modeli ------------------------------------------------------
VP = (838.0, 46.0)                        # kacis noktasi
Y_NEAR = 660.0                            # yakin referans satiri (tuvalin altinda)
X_NEAR = 742.0                            # yol merkezinin o satirdaki x'i
HALF_NEAR = 470.0                         # yolun o satirdaki yari genisligi

LANE_U = (-2.0 / 3.0, 0.0, 2.0 / 3.0)     # LANE_COUNT = 3, serit merkezleri
KERB_FRAC = 0.052                         # kerb genisligi / yol yari genisligi
DASH_FRAC = 0.014                         # serit cizgisi yari genisligi / half
CAR_LANE_RATIO = 0.52                     # arac govdesi / serit genisligi


def s_of_y(y):
    return (y - VP[1]) / (Y_NEAR - VP[1])


def y_of_s(s):
    return VP[1] + (Y_NEAR - VP[1]) * s


def s_of_z(z):
    """Dunya mesafesi -> ekran olcegi. z=0 yakin kenar, z->sonsuz ufuk."""
    return 1.0 / (1.0 + z)


def half_of_s(s):
    return HALF_NEAR * s


def cx_of_s(s):
    return VP[0] + (X_NEAR - VP[0]) * s


def road_pt(s, u):
    """Yolun (derinlik s, yanal u) noktasi. u=-1 sol kenar, +1 sag kenar."""
    return (cx_of_s(s) + u * half_of_s(s), y_of_s(s))


def lane_angle_deg(s, u):
    """O noktadan VP'ye bakan yonun, ekranda YUKARI'dan saat yonundeki acisi."""
    x, y = road_pt(s, u)
    return math.degrees(math.atan2(VP[0] - x, y - VP[1]))


# ---------------------------------------------------------------------------
# zemin
# ---------------------------------------------------------------------------

def background():
    img = K.vertical_gradient((W, H), K.NIGHT_TOP, K.NIGHT_BOT).convert('RGBA')
    # NIGHT temasindaki uzak sehir isiklari (drawSideBackgrounds)
    lights = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(lights)
    for i in range(30):
        x = (i * 71) % W
        y = 18 + (i * 29) % 96
        col = K.CITY_WARM if i % 4 == 0 else K.CITY_COOL
        d.rectangle([x, y, x + 3, y + 8], fill=col + (120,))
    img.alpha_composite(K.glow(lights, 5, 1.0))
    img.alpha_composite(lights)
    # NIGHT'taki egik camgobegi kenar cizgileri
    lines = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    dl = ImageDraw.Draw(lines)
    for y in range(-60, H + 120, 105):
        dl.line([(0, y), (360, y + 40)], fill=K.BLUE_BRIGHT + (46,), width=2)
    img.alpha_composite(lines)
    return img


def road(img):
    layer = Image.new('RGBA', (W * SS, H * SS), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)

    def sp(s, u):
        x, y = road_pt(s, u)
        return (x * SS, y * SS)

    # Asfalt: apeksi VP olan ucgen — yol tuvalin alt/sag kenarindan tasar.
    s_bot = s_of_y(float(H))
    d.polygon([(VP[0] * SS, VP[1] * SS), sp(s_bot, -1.0), sp(s_bot, 1.0)],
              fill=K.ROAD + (255,))

    # Kerb bloklari (drawTrack: kirmizi/beyaz almasik). z'de esit araliklarla.
    dz = 0.085
    for i in range(140):
        s0, s1 = s_of_z(i * dz), s_of_z((i + 1) * dz)
        if y_of_s(s0) < VP[1] + 1:
            break
        col = (K.KERB_RED if i % 2 == 0 else K.ROAD_LINE) + (255,)
        for side in (-1.0, 1.0):
            d.polygon([sp(s0, side), sp(s0, side * (1 + KERB_FRAC)),
                       sp(s1, side * (1 + KERB_FRAC)), sp(s1, side)], fill=col)

    # Serit ayirici kesik cizgiler (LANE_COUNT = 3 -> u = -1/3 ve +1/3)
    dz = 0.20
    for u in (-1.0 / 3.0, 1.0 / 3.0):
        for i in range(140):
            s0, s1 = s_of_z(i * dz), s_of_z(i * dz + dz * 0.55)
            if y_of_s(s1) < VP[1] + 1:
                break
            d.polygon([sp(s0, u - DASH_FRAC), sp(s0, u + DASH_FRAC),
                       sp(s1, u + DASH_FRAC), sp(s1, u - DASH_FRAC)],
                      fill=K.ROAD_LINE + (235,))
    img.alpha_composite(layer.resize((W, H), Image.LANCZOS))

    # Ufuk isigi — yolun ucu isikla kapanir
    hz = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(hz).ellipse([VP[0] - 150, VP[1] - 70, VP[0] + 150, VP[1] + 60],
                               fill=K.BLUE_BRIGHT + (92,))
    img.alpha_composite(hz.filter(ImageFilter.GaussianBlur(38)))
    return img


# ---------------------------------------------------------------------------
# arac yerlestirme — konum/olcek/aci TAMAMEN modelden
# ---------------------------------------------------------------------------

def place_car(img, s, lane, base, flame=False):
    """Araci (serit, derinlik s) ile yerlestirir. Elle x/y/aci/olcek YOK."""
    u = LANE_U[lane]
    half = half_of_s(s)
    body_w = (2.0 * half / 3.0) * CAR_LANE_RATIO      # olcek = derinligin fonksiyonu
    car, bcy = realistic_car(body_w, base, flame=flame)

    # Govde merkezini goruntunun merkezine tasi ki rotate(expand) etrafinda donsun.
    pad_top = max(0.0, car.height - 2 * bcy)
    pad_bot = max(0.0, 2 * bcy - car.height)
    cen = Image.new('RGBA', (car.width, int(round(car.height + pad_top + pad_bot))),
                    (0, 0, 0, 0))
    cen.alpha_composite(car, (0, int(round(pad_top))))

    ang = lane_angle_deg(s, u)                         # aci = isinin acisi
    rot = cen.rotate(-ang, resample=Image.BICUBIC, expand=True)

    cx, cy = road_pt(s, u)                             # konum = isinin uzeri
    img.alpha_composite(rot, (int(round(cx - rot.width / 2.0)),
                              int(round(cy - rot.height / 2.0))))
    return {'lane': lane, 's': s, 'y': cy, 'x': cx, 'body_w': body_w, 'angle': ang}


# ---------------------------------------------------------------------------
# marka blogu: GERCEK logo + yazi
# ---------------------------------------------------------------------------

def wordmark(img):
    d = ImageDraw.Draw(img)
    f1 = ImageFont.truetype(FONT_BLACK, 88)
    f2 = ImageFont.truetype(FONT_BOLD, 21)
    x = 104
    y_logo, logo_px = 48, 158
    y1, y2 = 218, 308
    y_rule = 416
    y_tag = 436

    text_w = max(f1.getlength('KRON'), f1.getlength('DRIVE'))

    # Okunurluk icin metnin arkasina yumusak koyu perde
    veil = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(veil).ellipse([x - 150, y_logo - 90, x + text_w + 130, y_tag + 130],
                                 fill=(0x02, 0x09, 0x15, 186))
    img.alpha_composite(veil.filter(ImageFilter.GaussianBlur(60)))

    # GERCEK logo — projedeki kron_logo.png, uretilmiyor, saydamligi korunuyor
    logo = Image.open(LOGO).convert('RGBA')
    assert logo.size == (512, 512), 'kron_logo.png beklenen boyutta degil'
    logo = logo.resize((logo_px, logo_px), Image.LANCZOS)
    # sari halkanin etrafina hafif isik: gece zeminde logo yuzmesin
    img.alpha_composite(K.glow(logo, 22, 0.45),
                        (int(x + (text_w - logo_px) / 2), y_logo))
    img.alpha_composite(logo, (int(x + (text_w - logo_px) / 2), y_logo))

    for yy, line in ((y1, 'KRON'), (y2, 'DRIVE')):
        d.text((x, yy), line, font=f1, fill=K.ACCENT_YELLOW)

    d.rectangle([x + 3, y_rule, x + 74, y_rule + 6], fill=K.BLUE_BRIGHT)
    d.text((x + 4, y_tag), 'DRIVE  ·  DODGE  ·  SURVIVE', font=f2, fill=K.TEXT_PRIMARY)
    # NOT: "KRON"un sagindaki ayri simsek KALDIRILDI — logo zaten simsegi
    # iceriyor, ikisi birlikte tekrar ediyordu.
    return img, x + text_w


# ---------------------------------------------------------------------------

def main():
    img = background()
    img = road(img)

    # (serit, derinlik) — uzaktan yakina cizilir ki yakindaki ustte kalsin.
    placed = [
        place_car(img, 0.205, 2, K.OBSTACLE_COLORS[3]),                  # turuncu, uzak
        place_car(img, 0.462, 0, K.OBSTACLE_COLORS[0]),                  # sari, sol serit
        place_car(img, 0.560, 2, K.OBSTACLE_COLORS[1]),                  # camgobegi, sag
        place_car(img, 0.544, 1, K.BODY, flame=True),                    # oyuncu, orta
    ]
    img, text_right = wordmark(img)

    out = img.convert('RGB')
    assert out.size == (W, H)
    path = os.path.join(OUT, 'feature_graphic_1024x500.png')
    out.save(path)

    # --- kendi kendini denetleme: kerb/serit ihlali ve olcek sirasi --------
    print('%-10s %-6s %-8s %-9s %-8s %s' %
          ('serit', 's', 'govde', 'aci', 'y', 'seride sigma payi (px)'))
    for p in placed:
        half = half_of_s(p['s'])
        lane_half = half / 3.0
        # aracin yanal yari genisligi: tekerlekler govdenin +-23.6/20 katina cikar
        car_half = p['body_w'] / 2.0 * (24.8 / 20.0)
        print('%-10d %-6.3f %-8.1f %-9.2f %-8.1f %.1f' %
              (p['lane'], p['s'], p['body_w'], p['angle'], p['y'],
               lane_half - car_half))
        assert car_half < lane_half, 'arac serit sinirini asiyor'
    angles = sorted(round(p['angle'], 2) for p in placed)
    assert len(set(angles)) == len(angles), 'farkli seritlerde ayni aci'
    assert text_right < road_pt(s_of_y(float(H)), -1.0)[0], 'yazi yola giriyor'
    print(path)


if __name__ == '__main__':
    main()
