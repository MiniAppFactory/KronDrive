"""Arac sprite hatti: referans cizim + maske -> oyunun iki katmanli sprite'lari.

## Neden iki katman

Oyunda 10 boya var ve oyuncu istedigi govdeyi istedigi boyayla surebiliyor.
Tek bir hazir PNG kullansaydik her govde icin 10 dosya gerekirdi (7 govde x 10
boya = 70 dosya). Bunun yerine her govde IKI katmana ayriliyor:

  * ``_body``   — boyanabilir alan, GRI TONLAMALI. Sadece isik/golge bilgisi
                  tasir. Calisma aninda secili boyayla CARPILIR
                  (``BlendMode.Modulate``), boylece tek dosyadan 10 boya cikar.
  * ``_detail`` — cam, lastik, far, stop lambasi, serit. Renkli ve oldugu gibi
                  ustune cizilir; boyadan etkilenmez.

Ayrimi proje sahibinin urettigi ``*_mask.png`` dosyalari soyluyor: govde MAGENTA
isaretli, geri kalan siyah. Maskeler kayipli sikistirmadan gectigi icin temiz
iki renkli DEGIL (bir maskede 100 binden fazla farkli renk var); bu yuzden esik
degil YUMUSAK bir agirlik kullaniyoruz (asagida ``chroma``). Sert esik kenarlara
merdiven yapiyordu.

## Hizalama sozlesmesi (bozulursa carpisma kutusu kayar)

Vektor cizim uzayinda arac kutusu x -20..20, y -2..74'tur (``CarCatalog``).
Sprite'lar tam bu orana (40:76) uretilir ve icine ORANI BOZULMADAN sigdirilir.
Cizici sprite'i ayni kutuya oturttugu icin gorsel ile hitbox ortusmeye devam
eder — sprite'a gecerken carpisma davranisi DEGISMEZ.

Cikti ``drawable-nodpi`` altina yazilir: Android'in yogunluga gore on-olcekleme
yapmasini istemiyoruz, cizici zaten hedef boyutu kendisi veriyor.

Kullanim:  py tools/build_car_sprites.py
"""

from __future__ import annotations

import os
import sys
from PIL import Image, ImageChops

# --- Yollar ---------------------------------------------------------------

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REFS = os.path.join(ROOT, "incoming", "car_refs")
OUT = os.path.join(ROOT, "source", "app", "src", "main", "res", "drawable-nodpi")

# --- Hedef kutu -----------------------------------------------------------
#
# CarCatalog'un cizim kutusu 40 x 76 birim; sprite ayni orani korur (240/456 =
# 40/76 tam olarak) ki cizici tek bir dikdortgene oturtabilsin.
#
# Cozunurluk aracin ekranda EN BUYUK gorundugu yerden turetildi: garajdaki
# secili arac karti 96x118 dp kutuda, 6 dp dolgu ile ~106 dp yuksekliginde.
# En yogun ekranda (xxxhdpi, density 4) bu 424 px eder; oyun sahnesinde arac
# 60.8 dp = 243 px. 456 px hepsinin ustunde kaliyor ve pay birakiyor.
# Daha buyugu APK'ya bosuna yer yazar: 608 px denendi, 861 KB tutuyordu.

TARGET_W = 240
TARGET_H = 456

# Maske magenta esigi: chroma = (R+B)/2 - G. Tam magenta'da 255, siyahta 0.
# 90'a bolup doyuruyoruz — kayipli sikistirmanin bulanik kenarinda yumusak bir
# gecis birakir, govde/detay sinirinda merdiven olusmaz.
CHROMA_SPAN = 90

# Maske alfasi bunun altindaysa piksel aracin disindadir.
MASK_ALPHA_FLOOR = 64

# Govde parlakligi normalize edilirken tepe olarak alinan yuzdelik. Referans
# cizimlerde birkac spekuler piksel 255'e vuruyor; onlara gore normalize
# edersek govdenin tamami kararir.
BODY_PEAK_PERCENTILE = 0.98

# Referans cizimlerin govde tonu birbirinden COK farkli: beyaz station wagon'un
# ortanca parlakligi 238, siyah kas arabasininki 41. Carpim boyamada bu fark
# dogrudan "boyayi goremiyorum"a donusuyor — siyah referans hangi boyayi
# secersen sec neredeyse siyah kaliyor. Bu yuzden her govdenin ORTANCASI ayni
# hedefe cekiliyor; boylece 10 boya her govdede ayni gucte okunuyor.
BODY_MID_TARGET = 205

# ...ama duzeltme gamma ile yapiliyor ve gamma TABANLANIYOR. Siyah referansta
# ham oran 6x'e cikiyor ve o kadar gerdirmek kayipli sikistirmanin gurultusunu
# de 6x buyutuyordu (govdede lekeler). 0.40 tabani "elinden geleni yap, ama
# gurultuyu buyutme" siniri.
BODY_GAMMA_FLOOR = 0.40
BODY_GAMMA_CEIL = 1.60

# WebP kalitesi. Govde gri tonlamali — renk kanallari ayni oldugu icin daha
# agresif sikistirilabilir; detay katmani cam/far/serit tasidigindan yuksek
# kalir. Alfa kanali her ikisinde de kenar kalitesini belirliyor.
WEBP_QUALITY_BODY = 82
WEBP_QUALITY_DETAIL = 88

# --- Govde -> referans dosyasi eslesmesi ----------------------------------
#
# Anahtarlar CarCatalog'daki SHAPE_* kimlikleridir; cizici dosyayi bu adla
# arar (car_<id>_body / car_<id>_detail). Yeni bir govde eklenirse buraya bir
# satir eklemek yeterli.

MAPPING = {
    "hatchback": "01_sehir",
    "race_sedan": "02_yaris_sedan",
    "kus_slx": "Kuş SLX",
    "mountain_goat": "DağKeçisi",
    "muscle": "03_kas_arabasi",
    "muscle_67": "Boğa67",
    "supercar": "04_super_araba",
    # Trafik araci: oyuncunun alamadigi sabit govde. Sprite'a gecmesi ayrica
    # KAZANC — sahnede ayni anda onlarca trafik araci var ve her biri vektorde
    # ~20 parca cizdiriyordu.
    "traffic": "08_trafik_arac",
}


def body_weight(mask: Image.Image) -> Image.Image:
    """Maskeden 0..255 'burasi boyanabilir govde' agirligi uretir."""
    mr, mg, mb, ma = mask.split()
    # (R + B) / 2 — once yariya bolup topluyoruz ki 255'te tasma olmasin.
    avg_rb = ImageChops.add(mr.point(lambda i: i // 2), mb.point(lambda i: i // 2))
    chroma = ImageChops.subtract(avg_rb, mg)  # negatifleri 0'a kirpar
    w = chroma.point(lambda i: min(255, i * 255 // CHROMA_SPAN))
    inside = ma.point(lambda i: 255 if i > MASK_ALPHA_FLOOR else 0)
    return ImageChops.multiply(w, inside)


def _percentile(value: Image.Image, solid: Image.Image, p: float) -> int:
    hist = value.histogram(mask=solid)
    total = sum(hist)
    if total == 0:
        return 255
    acc = 0
    for level, count in enumerate(hist):
        acc += count
        if acc >= total * p:
            return max(level, 1)
    return 255


def normalize_body(value: Image.Image, alpha: Image.Image) -> tuple[Image.Image, int, float]:
    """Govde parlakligini butun govdelerde ortak bir araliga tasir.

    Iki asama:

      1. **Tepe gerdirme.** p98 -> 255. Referans cizimlerin govde tonu 255'e
         kadar gitmiyor; boyle yapilmazsa carpim sonrasi arac secili boyadan
         belirgin KOYU cikar.
      2. **Ortanca esitleme (gamma).** Her govdenin ortancasi
         [BODY_MID_TARGET]'a cekilir. Lineer olcek yerine gamma kullaniliyor
         cunku lineer olcek koyu referanslarda tepeleri kirpip govdeyi duz bir
         renk lekesine cevirtiyordu; gamma 0 ve 255'i sabit birakip aradaki
         dagilimi tasiyor, hacim hissi kaliyor.

    Doner: (duzeltilmis kanal, ham ortanca, uygulanan gamma) — rapor icin.
    """
    solid = alpha.point(lambda i: 255 if i > 200 else 0)
    peak = _percentile(value, solid, BODY_PEAK_PERCENTILE)
    stretched = value.point(lambda i: min(255, i * 255 // peak))

    mid = _percentile(stretched, solid, 0.50)
    if mid <= 1 or mid >= 254:
        return stretched, mid, 1.0
    import math

    gamma = math.log(BODY_MID_TARGET / 255.0) / math.log(mid / 255.0)
    gamma = min(BODY_GAMMA_CEIL, max(BODY_GAMMA_FLOOR, gamma))
    lut = [min(255, round(255.0 * (i / 255.0) ** gamma)) for i in range(256)]
    lut[0] = 0
    return stretched.point(lut), mid, gamma


def build(shape_id: str, stem: str) -> tuple[int, int, int, float]:
    base_path = os.path.join(REFS, stem + ".png")
    mask_path = os.path.join(REFS, stem + "_mask.png")
    for p in (base_path, mask_path):
        if not os.path.exists(p):
            raise FileNotFoundError(p)

    base = Image.open(base_path).convert("RGBA")
    mask = Image.open(mask_path).convert("RGBA")
    if mask.size != base.size:
        mask = mask.resize(base.size, Image.LANCZOS)

    r, g, b, a = base.split()
    w = body_weight(mask)

    body_alpha = ImageChops.multiply(a, w)
    detail_alpha = ImageChops.multiply(a, ImageChops.invert(w))

    # Parlaklik olcusu olarak LUMINANS DEGIL max(R,G,B) kullaniliyor: kirmizi
    # bir govdenin luminansi dusuktur ve golge araligi eziliyordu. max kanal,
    # doygun boyalarda isik bilgisini oldugu gibi tasir.
    value = ImageChops.lighter(ImageChops.lighter(r, g), b)
    value, raw_mid, gamma = normalize_body(value, body_alpha)

    # RGB'yi hicbir yerde sifirlamiyoruz (yalnizca alfa degisiyor): olcekleme
    # sirasinda kenar pikselleri komsularindan renk cekiyor, siyaha cekerse
    # aracin cevresinde koyu bir hale olusuyor.
    body = Image.merge("RGBA", (value, value, value, body_alpha))
    detail = Image.merge("RGBA", (r, g, b, detail_alpha))

    # Iki katman AYNI kutudan kirpilir; ayri kirpilirsa ustuste binmezler.
    bbox = a.point(lambda i: 255 if i > 8 else 0).getbbox()
    if bbox is None:
        raise ValueError("bos referans: " + stem)
    body = body.crop(bbox)
    detail = detail.crop(bbox)

    # Orani bozmadan sigdir + ortala.
    cw, ch = body.size
    scale = min(TARGET_W / cw, TARGET_H / ch)
    nw, nh = max(1, round(cw * scale)), max(1, round(ch * scale))
    ox, oy = (TARGET_W - nw) // 2, (TARGET_H - nh) // 2

    written = []
    layers = (("body", body, WEBP_QUALITY_BODY), ("detail", detail, WEBP_QUALITY_DETAIL))
    for name, layer, quality in layers:
        scaled = layer.resize((nw, nh), Image.LANCZOS)
        canvas = Image.new("RGBA", (TARGET_W, TARGET_H), (0, 0, 0, 0))
        canvas.paste(scaled, (ox, oy))
        out_path = os.path.join(OUT, f"car_{shape_id}_{name}.webp")
        canvas.save(out_path, "WEBP", quality=quality, method=6)
        written.append(os.path.getsize(out_path))
    return written[0], written[1], raw_mid, gamma


def main() -> int:
    os.makedirs(OUT, exist_ok=True)
    total = 0
    for shape_id, stem in MAPPING.items():
        body_bytes, detail_bytes, raw_mid, gamma = build(shape_id, stem)
        total += body_bytes + detail_bytes
        print(
            "%-14s <- %-16s body %5.1f KB  detail %5.1f KB  ortanca %3d -> gamma %.2f"
            % (shape_id, stem, body_bytes / 1024, detail_bytes / 1024, raw_mid, gamma)
        )
    print("-" * 58)
    print("%d govde, toplam %.1f KB" % (len(MAPPING), total / 1024))
    return 0


if __name__ == "__main__":
    sys.exit(main())
