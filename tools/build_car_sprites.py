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
Sprite'lar tam bu orana (40:76) uretilir; cizici sprite'i her zaman TAM bu
dikdortgene oturtur.

Uzun sure "orani bozmadan icine sigdir" (``min(w_orani, h_orani)``) yeterli
sanildi. DEGIL: referansin orani 40:76'dan farkliysa fark sessizce SAYDAM
BOSLUGA doner ve cizilen arac carpisma kutusundan DAR kalir — yani oyuncu
aracin yanindaki bosluga carpar. Olcum (2026-08-16): Boga 67'nin cizimi
27.73 dp genisligindeydi, kutu ise 28.16 dp; kutu her kenardan 0.21 dp
disari tasiyordu. 2026-08-13'te duzeltilen "havaya carptim" hatasinin
milimetrik hali.

Bu yuzden asagidaki olcek kurali GENISLIK ONCELIKLI: cizim once orani
bozulmadan kutuya sigdirilir, sonra gerekiyorsa carpisma kutusunu ORTECEK
kadar (yine oran bozulmadan) buyutulur; tasan yukseklik ustten ve alttan
esit kirpilir. Kirpma tamponun ucundan birkac pikseli alir, [MAX_TRIM_FRACTION]
kadarini gecerse hat DURUR — sessizce araci ezmek ya da tamponu kesmek yok.
Gecerlilik her kosuda [audit] ile yeniden olculur (bkz. dosyanin sonu).

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

# --- Carpisma kutusu (GameConfig.kt'nin AYNASI) --------------------------
#
# Bu dort sayi Kotlin tarafinda [GameConfig] icinde yasiyor; burada yalnizca
# KOPYALARI var cunku betigin oyun kodunu okumasi yok. GameConfig'de biri
# degisirse burasi da degismeli — [audit] yanlis esikle olcer ve hata yakalamaz.
#
#   CAR_ART_SCALE=0.80, cizim 40 x 76 birim  -> 32.00 x 60.80 px
#   HITBOX_SCALE=0.88                        -> 28.16 x 53.50 px

CAR_ART_SCALE = 0.80
CAR_WIDTH_PX = 40.0 * CAR_ART_SCALE
CAR_HEIGHT_PX = (74.0 - (-2.0)) * CAR_ART_SCALE
HITBOX_SCALE = 0.88

# Kanvas pikseli cinsinden carpisma kutusu genisligi: cizici 240 px'i
# CAR_WIDTH_PX'e esliyor, yani oran dogrudan olceklenebilir.
HITBOX_W_CANVAS_PX = TARGET_W * HITBOX_SCALE

# Carpisma kutusu cizimin bu kadar ICINDE kalsin (kenar basina, dp).
#
# Neden sifir degil: kutu ile cizim kenari tam ustuste gelirse hattaki her
# yuvarlama ihlale doner. Hedef DOSYADA olculur, niyette degil — LANCZOS'un
# kenari yumusatmasi ve kayipli WebP alfasi birlikte kenar basina 0.5-1.5
# kanvas px yiyor (olculdu, 2026-08-16). Bu kayip sabit degil, referanstan
# referansa degisiyor; o yuzden telafi tek bir "fudge" sayisi degil, asagidaki
# yakinsama dongusu ([_build_until_covered]).
HITBOX_EDGE_MARGIN_DP = 0.15

# Yakinsama: hedefi tutturamayan sprite en fazla bu kadar kez yeniden uretilir.
# Her tur olceklemeyi olculen eksik kadar (+ kucuk bir emniyet) buyutur.
# Pratikte 1-2 tur yetiyor; yetmiyorsa sorun oran uyusmazligidir ve zaten
# [MAX_TRIM_FRACTION] ya da oz-denetim durduruyor.
MAX_FIT_ATTEMPTS = 4
FIT_OVERSHOOT = 1.003

# Genisligi tutturmak icin uc basina kesilebilecek EN BUYUK oran.
#
# Referanslarin ucundaki satirlar golge degil GERCEK TAMPON (Boga 67'nin son
# satirinda 400+ opak piksel var), yani kirpma cizimden et goturur. Aracin
# boyunun %2.5'i (60.8 dp'de ~1.5 dp) tamponun yuvarlatilmis dudagi kadardir;
# bunun otesinde duz bir kesik olarak okunmaya baslar.
#
# Bugunku referanslarda gerceklesen kirpma: Boga 67 %1.7, Dag Kecisi %1.1,
# kas arabasi %0.2, kalan bes govde %0. Sinira takilanlar BEKLEYEN referanslar:
# station wagon %3.8, motosiklet %10.3, tir %24.9 — bunlar 40:76 kutusuna ait
# DEGIL, hat onlari reddediyor (denendi, 2026-08-16).
MAX_TRIM_FRACTION = 0.025

# "Dolu piksel" esigi. Olcum de olcek karari da bu esikle yapilir ki hattin
# hedefi ile denetimin olctugu sey ayni sey olsun. Tuylu kenar (alfa 8..200)
# ekranda araci ortmuyor, o yuzden genislik hesabina KATILMAZ.
SOLID_ALPHA = 200

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


def _dp_to_canvas_px(dp: float) -> float:
    """Oyun px'i -> kanvas px. Cizici 240 kanvas px'i CAR_WIDTH_PX'e esler."""
    return dp / CAR_WIDTH_PX * TARGET_W


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


def build(shape_id: str, stem: str, boost: float = 1.0) -> tuple[int, int, int, float, float]:
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
    # Esik burada DUSUK (alfa > 8): tuylu kenar cizimin parcasi, kirpilirsa
    # aracin cevresinde sert bir sinir olusur.
    bbox = a.point(lambda i: 255 if i > 8 else 0).getbbox()
    if bbox is None:
        raise ValueError("bos referans: " + stem)
    body = body.crop(bbox)
    detail = detail.crop(bbox)

    # Olcek kararini veren genislik ise DOLU genisliktir (bkz. [SOLID_ALPHA]):
    # kutuyu ortmesi gereken sey tuy degil, gercekten opak boyadir.
    solid = a.crop(bbox).point(lambda i: 255 if i >= SOLID_ALPHA else 0).getbbox()
    if solid is None:
        raise ValueError("referansta opak piksel yok: " + stem)
    solid_w = solid[2] - solid[0]

    cw, ch = body.size

    # Olcek iki alt sinirin buyugu:
    #   1) [fit]    — orani bozmadan kutuya sigdir (eski, TEK basina yetersiz
    #                 olan davranis: dar referansta saydam bosluk birakiyordu),
    #   2) [needed] — carpisma kutusunu ORTMEYE yeten en kucuk olcek.
    # Genis referanslarda (1), dar/uzun referanslarda (2) belirleyici olur.
    # Iki durumda da TEK bir sayi ile olceklendigi icin oran korunur; arac
    # hicbir kosulda yatayda gerdirilmez.
    #
    # [boost] yakinsama dongusunun duzeltmesi: dosyadan geri okunan genislik
    # hedefin altinda kaldiysa cagiran 1'den buyuk bir deger verir
    # (bkz. [_build_until_covered]).
    fit = min(TARGET_W / cw, TARGET_H / ch)
    needed = (HITBOX_W_CANVAS_PX + 2.0 * _dp_to_canvas_px(HITBOX_EDGE_MARGIN_DP)) / solid_w
    scale = max(fit, needed) * boost

    nw, nh = max(1, round(cw * scale)), max(1, round(ch * scale))

    # Yatayda tasma yalnizca referansin TUYU dolu kismindan cok genisse olur
    # (orn. cevresinde parlama efekti). O durumda kirpmak gercek cizimi keser;
    # sessizce yapmak yerine duruyoruz.
    if nw > TARGET_W:
        raise ValueError(
            "%s: cizim yataya sigmiyor (%d > %d px). Referansin dolu govdesi "
            "tuylu kenarina gore cok dar — kenar efektini referanstan temizle."
            % (stem, nw, TARGET_W)
        )

    # Dikey tasma ustten ve alttan ESIT kirpilir (arac kutuda ortali kalsin).
    trim = max(0, (nh - TARGET_H) // 2)
    trim_fraction = trim / float(nh)
    if trim_fraction > MAX_TRIM_FRACTION:
        raise ValueError(
            "%s: carpisma kutusunu ortmek icin her ucundan %%%.1f kirpmak "
            "gerekiyor (sinir %%%.1f). Referansin en/boy orani %.3f, kutununki "
            "%.3f — bu arac 40:76 kutusuna ait degil. Ya referans yeniden "
            "cizilmeli ya da aracin kendi cizim/carpisma kutusu tanimlanmali "
            "(GameConfig + CarCatalog isi)."
            % (stem, trim_fraction * 100.0, MAX_TRIM_FRACTION * 100.0,
               cw / float(ch), TARGET_W / float(TARGET_H))
        )

    ox = (TARGET_W - nw) // 2
    oy = 0 if nh > TARGET_H else (TARGET_H - nh) // 2

    written = []
    layers = (("body", body, WEBP_QUALITY_BODY), ("detail", detail, WEBP_QUALITY_DETAIL))
    for name, layer, quality in layers:
        scaled = layer.resize((nw, nh), Image.LANCZOS)
        if nh > TARGET_H:
            top = (nh - TARGET_H) // 2
            scaled = scaled.crop((0, top, nw, top + TARGET_H))
        canvas = Image.new("RGBA", (TARGET_W, TARGET_H), (0, 0, 0, 0))
        canvas.paste(scaled, (ox, oy))
        out_path = os.path.join(OUT, f"car_{shape_id}_{name}.webp")
        canvas.save(out_path, "WEBP", quality=quality, method=6)
        written.append(os.path.getsize(out_path))
    return written[0], written[1], raw_mid, gamma, trim_fraction


def audit(shape_id: str) -> tuple[float, float]:
    """YAZILMIS dosyalari geri okuyup cizimin carpisma kutusunu ortup ortmedigini olcer.

    Neden dosyadan geri okuyor: hattaki niyet degil, APK'ya giren SONUC onemli.
    Yuvarlama ve kayipli WebP alfasi araya girdikten sonraki gercek genislik bu.

    Neden iki katmanin alfasi TOPLANIYOR: aracin gorunur siluetini ikisi
    BIRLIKTE olusturuyor ve en genis yer cogu araçta lastiktir — lastik
    ``_detail`` katmaninda. Tek basina ``_body`` olculseydi arac oldugundan dar
    gorunurdu (Boga 67: govde 199 px, silüet 208 px). Toplam ayni zamanda
    govde/detay sinirindaki yari saydam pikselleri de dogru topluyor
    (agirliklar w ve 1-w oldugu icin alfalari `a`ya geri toplanir).

    Doner: (dp cinsinden genislik, dp cinsinden yukseklik).
    """
    layers = []
    for name in ("body", "detail"):
        path = os.path.join(OUT, f"car_{shape_id}_{name}.webp")
        layers.append(Image.open(path).convert("RGBA").split()[3])
    combined = ImageChops.add(layers[0], layers[1])
    box = combined.point(lambda i: 255 if i >= SOLID_ALPHA else 0).getbbox()
    if box is None:
        raise ValueError("uretilen sprite bos: " + shape_id)
    w_px = (box[2] - box[0]) / float(TARGET_W) * CAR_WIDTH_PX
    h_px = (box[3] - box[1]) / float(TARGET_H) * CAR_HEIGHT_PX
    return w_px, h_px


def _build_until_covered(shape_id: str, stem: str):
    """Sprite'i uretir; DOSYADAN olculen genislik hedefin altindaysa yeniden uretir.

    Neden bir dongu: gereken olcegi referanstan hesaplamak yetmiyor. LANCZOS
    ile kucultup kayipli WebP'ye yazdiktan sonra kenardaki birkac piksel
    [SOLID_ALPHA] esiginin altina dusuyor ve arac hesaplanandan 0.1-0.2 dp dar
    cikiyor. Kayip her referansta ayni degil, o yuzden sabit bir telafi sayisi
    ya yetmez ya da gereksiz yere buyutur. Dongu her araca SADECE kendi
    kaybi kadar duzeltme uygular; hedefi tutan araca hic dokunmaz.
    """
    target_w = CAR_WIDTH_PX * HITBOX_SCALE + 2.0 * HITBOX_EDGE_MARGIN_DP
    boost = 1.0
    for attempt in range(1, MAX_FIT_ATTEMPTS + 1):
        stats = build(shape_id, stem, boost)
        w_px, h_px = audit(shape_id)
        if w_px >= target_w:
            return stats, w_px, h_px, attempt
        boost *= (target_w / w_px) * FIT_OVERSHOOT
    # Buraya dusmek "buyutmek ise yaramiyor" demektir; son olculen deger
    # oz-denetime gider ve orada net mesajla patlar.
    return stats, w_px, h_px, MAX_FIT_ATTEMPTS


def main() -> int:
    os.makedirs(OUT, exist_ok=True)
    total = 0
    rows = []
    for shape_id, stem in MAPPING.items():
        stats, w_px, h_px, attempts = _build_until_covered(shape_id, stem)
        body_bytes, detail_bytes, raw_mid, gamma, trim = stats
        total += body_bytes + detail_bytes
        notes = ""
        if trim > 0:
            notes += "  (uc basina %%%.1f kirpildi)" % (trim * 100.0)
        if attempts > 1:
            notes += "  (%d turda oturdu)" % attempts
        print(
            "%-14s <- %-16s body %5.1f KB  detail %5.1f KB  ortanca %3d -> gamma %.2f%s"
            % (shape_id, stem, body_bytes / 1024, detail_bytes / 1024, raw_mid, gamma, notes)
        )
        rows.append((shape_id, w_px, h_px))
    print("-" * 58)
    print("%d govde, toplam %.1f KB" % (len(MAPPING), total / 1024))

    # --- Oz-denetim -------------------------------------------------------
    #
    # Tek kural: CIZILEN arac carpisma kutusundan DAR OLAMAZ. Dar kalirsa
    # oyuncu aracin yanindaki bosluga carpar ("havaya carptim"). Ters yon
    # (cizim kutudan genis) istenen durumdur — bu tur oyunlarda affedici
    # hissettirir, bkz. GameConfig.HITBOX_SCALE.
    #
    # Bu kontrol 2026-08-16'da eklendi: sigdirma kurali sessizce Boga 67'yi
    # kutudan 0.21 dp dar uretmisti ve hicbir yerde patlamamisti.
    hb_w = CAR_WIDTH_PX * HITBOX_SCALE
    hb_h = CAR_HEIGHT_PX * HITBOX_SCALE
    print()
    print("oz-denetim: cizim >= carpisma kutusu (%.2f x %.2f px)" % (hb_w, hb_h))
    print("%-14s %9s %9s   %s" % ("govde", "genislik", "yukseklik", "pay (kenar basina)"))
    bad = []
    for shape_id, w_px, h_px in rows:
        mx, my = (w_px - hb_w) / 2.0, (h_px - hb_h) / 2.0
        flag = "OK " if (mx >= 0 and my >= 0) else "IHLAL"
        print("  %s %-12s %9.2f %9.2f   x %+.2f  y %+.2f"
              % (flag, shape_id, w_px, h_px, mx, my))
        if mx < 0 or my < 0:
            bad.append((shape_id, mx, my))

    if bad:
        print()
        for shape_id, mx, my in bad:
            print(
                "HATA: %s carpisma kutusundan dar uretildi (x payi %+.2f dp, "
                "y payi %+.2f dp). Bu haliyle oyuncu aracin yanindaki BOSLUGA "
                "carpar. Referansin en/boy orani kutuya uymuyor demektir; "
                "HITBOX_EDGE_MARGIN_DP / MAX_TRIM_FRACTION degerlerine ve "
                "referansin kendisine bak." % (shape_id, mx, my)
            )
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
