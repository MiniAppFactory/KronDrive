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

Her govdenin bir OLCU SINIFI var (``VehicleClass.kt``): MOTOSIKLET 22x59,
BINEK 40x76, AGIR 48x202 birim. Sprite tuvali gövdenin SINIFININ oranini alir;
cizici sprite'i her zaman tam o dikdortgene oturtur.

Uzun sure "orani bozmadan icine sigdir" (``min(w_orani, h_orani)``) yeterli
sanildi. DEGIL: referansin orani sinifin oranindan farkliysa fark sessizce
SAYDAM BOSLUGA doner ve cizilen arac carpisma kutusundan DAR kalir — yani
oyuncu aracin yanindaki bosluga carpar. Olcum (2026-08-16): Boga 67'nin cizimi
27.73 dp genisligindeydi, kutu ise 28.16 dp; kutu her kenardan 0.21 dp
disari tasiyordu. 2026-08-13'te duzeltilen "havaya carptim" hatasinin
milimetrik hali.

Bu yuzden asagidaki olcek kurali GENISLIK ONCELIKLI: cizim once orani
bozulmadan kutuya sigdirilir, sonra gerekiyorsa carpisma kutusunu ORTECEK
kadar (yine oran bozulmadan) buyutulur; tasan yukseklik ustten ve alttan
esit kirpilir. Kirpma tamponun ucundan birkac pikseli alir, [MAX_TRIM_FRACTION]
kadarini gecerse hat DURUR — sessizce araci ezmek ya da tamponu kesmek yok.
Gecerlilik her kosuda [audit] ile YENIDEN ve HER GOVDE ICIN KENDI SINIFININ
kutusuna karsi olculur (bkz. dosyanin sonu).

Cikti ``drawable-nodpi`` altina yazilir: Android'in yogunluga gore on-olcekleme
yapmasini istemiyoruz, cizici zaten hedef boyutu kendisi veriyor.

Kullanim:
    py tools/build_car_sprites.py
    py tools/build_car_sprites.py --preview <klasor>   # gozle bakmak icin PNG
"""

from __future__ import annotations

import os
import sys
from PIL import Image, ImageChops

# --- Yollar ---------------------------------------------------------------

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REFS = os.path.join(ROOT, "incoming", "car_refs")
OUT = os.path.join(ROOT, "source", "app", "src", "main", "res", "drawable-nodpi")

# --- Doku yogunlugu -------------------------------------------------------
#
# Sinif bazina gecmeden once tuval tek bir cift sayiydi (240x456) ve bu iki
# sayinin arkasindaki KURAL yazili degildi: 240/40 = 6 ve 456/76 = 6, yani
# hattin ortuk bir "birim basina 6 piksel" sabiti vardi. Sinif basina tuvali
# elle yazmak yerine bu sabiti aciga cikariyoruz; boylece her sinif AYNI doku
# yogunlugunu alir ve sahnede motosiklet ile tir birbirine gore dogru
# keskinlikte gorunur (birinin pikselleri digerinden buyuk olmaz).
#
# 6 px/birim BINEK icin su sekilde secilmisti ve gecerliligi degismedi:
# aracin ekranda EN BUYUK gorundugu yer garajdaki secili arac karti
# (96x118 dp kutuda ~106 dp). En yogun ekranda (xxxhdpi, density 4) bu 424 px
# eder; oyun sahnesinde arac 60.8 dp = 243 px. 456 px hepsinin ustunde kalip
# pay birakiyor. Daha buyugu APK'ya bosuna yer yazar: 608 px denendi,
# 861 KB tutuyordu.

PX_PER_UNIT = 6.0

# --- Olcek sabitleri (GameConfig.kt'nin AYNASI) --------------------------
#
# Bu iki sayi Kotlin tarafinda [GameConfig] icinde yasiyor; burada yalnizca
# KOPYALARI var cunku betigin oyun kodunu okumasi yok. GameConfig'de biri
# degisirse burasi da degismeli — [audit] yanlis esikle olcer ve hata yakalamaz.

CAR_ART_SCALE = 0.80
HITBOX_SCALE = 0.88


class VehicleClass:
    """``game/VehicleClass.kt`` enum'unun Python aynasi.

    Kotlin tarafinda oranlar referans cizimlerin oranindan turetildi; burada
    ayni sayilari tekrar yaziyoruz ki hat, oyunun hangi kutuya cizecegini
    bilsin. Ikisi ayrilirsa [audit] yanlis kutuya karsi olcer — bu yuzden
    asagidaki uc satir Kotlin dosyasinin birebir kopyasi olmali.
    """

    def __init__(self, name: str, width_units: float, length_units: float):
        self.name = name
        self.width_units = width_units
        self.length_units = length_units

    # -- Tuval (kanvas px) --
    @property
    def canvas_w(self) -> int:
        return int(round(self.width_units * PX_PER_UNIT))

    @property
    def canvas_h(self) -> int:
        return int(round(self.length_units * PX_PER_UNIT))

    # -- Ekrandaki cizim (dp) --
    @property
    def art_w_dp(self) -> float:
        return self.width_units * CAR_ART_SCALE

    @property
    def art_h_dp(self) -> float:
        return self.length_units * CAR_ART_SCALE

    # -- Carpisma kutusu (dp) --
    @property
    def hitbox_w_dp(self) -> float:
        return self.art_w_dp * HITBOX_SCALE

    @property
    def hitbox_h_dp(self) -> float:
        return self.art_h_dp * HITBOX_SCALE

    @property
    def ratio(self) -> float:
        return self.width_units / self.length_units

    def dp_to_canvas_px(self, dp: float) -> float:
        """Oyun dp'si -> kanvas px. Cizici [canvas_w]'yi [art_w_dp]'ye esler."""
        return dp / self.art_w_dp * self.canvas_w

    def __repr__(self) -> str:  # rapor satirlari icin
        return self.name


# MOTOSIKLET 132x354, BINEK 240x456 (degismez), AGIR 288x1212 kanvas px.
MOTOSIKLET = VehicleClass("MOTOSIKLET", 22.0, 59.0)
BINEK = VehicleClass("BINEK", 40.0, 76.0)
AGIR = VehicleClass("AGIR", 48.0, 202.0)

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
# Bu esik SINIF ISI DEGIL, referans-sinif uyumu isidir: dogru sinifa konmus
# bir referansta kirpma neredeyse sifir cikar (olculdu 2026-08-17: motosiklet
# %0.0, tir %0.0). Sinira takilan referans yanlis sinifta demektir —
# 11_station_wagon (oran 0.434) BINEK'te %3.8 kirpma istiyor ve hat onu
# reddediyor; hala hicbir sinifa ait degil.
MAX_TRIM_FRACTION = 0.025

# Referans orani ile sinif orani arasindaki fark bunun ustundeyse RAPORLANIR.
#
# Neden yalnizca rapor, neden hata degil: oran uyusmazligi tek basina zararli
# degil, YONU zararli. Referans sinifindan DARSA cizim kutuyu ortmez ve oyuncu
# havaya carpar — bunu [audit] zaten olcup hatta durduruyor, hem de tahminle
# degil YAZILMIS DOSYADAN. Referans sinifindan GENISSE cizim tuvale sigmaz ya
# da kirpma gerektirir — onu da [MAX_TRIM_FRACTION] durduruyor.
#
# Sert bir oran kapisi bugun F1'i (referans orani 0.473, BINEK 0.526, fark
# %10.2) reddederdi; oysa F1 olcum sonucu BINEK'e OTURUYOR: sigdirma sonrasi
# cizim 28.80 dp, kutu 28.16 dp, kenar basina +0.32 dp pay — bugun yayinda
# olan Kas Arabasi'ndan (+0.25) daha iyi. Yani oran farki burada bir uyari
# isigidir, kilit degil (bkz. docs/VEHICLE_CLASSES.md §"F1 neden yeni sinif
# istemiyor").
RATIO_WARN_FRACTION = 0.02

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

# Sinifa gore kalite indirimi (ornek: {"AGIR": -20}). Kanca duruyor ama BUGUN
# BOS ve bos kalmasi bilincli bir karar:
#
# AGIR tuvali BINEK'in 3.19 kati alanda (288x1212 vs 240x456), yani tir
# "buyuk dosya" adayiydi. Olculdu (2026-08-17), varsayilan kalitede tir cifti
# 129.8 KB — alan basina otomobillerden ZATEN ucuz (ortalama otomobil 71 KB /
# 1.0x alan; oransal beklenti 227 KB olurdu). Kaliteyi dusurmenin getirisi
# olculdu ve KUCUK cikti:
#
#     kalite      toplam      kazanc    RMS fark (govde / detay)
#     82 / 88     129.8 KB       —          —
#     72 / 78     117.1 KB     -%10      3.14 / 2.16
#     62 / 68     112.4 KB     -%13      3.50 / 2.84
#     52 / 58     109.2 KB     -%16      3.91 / 3.04
#
# Cunku dosyanin agirligi renkte degil ALFA KANALINDA: tirin cevresi uzun ve
# ayrintili bir siluet. Kalite 30 puan dusurmek 20 KB kazandirip gorunur bir
# bozulma birakiyor — kotu takas. Bir sinif gercekten butceyi zorlarsa
# mekanizma burada duruyor; kullanilirsa RAKAMI raporla.
#
# Not: denetim (cizim >= kutu) bu indirimden SONRA, yazilmis dosyadan
# olculuyor — sikistirma alfayi yiyip araci daraltirsa hat yine durur.
# Olculdu: uc kalite kademesinde de tirin cizimi 38.00 x 161.07 dp kaldi.
QUALITY_DELTA_BY_CLASS: dict[str, int] = {}

# --- Govde -> (referans dosyasi, olcu sinifi) -----------------------------
#
# Anahtarlar CarCatalog'daki SHAPE_* kimlikleridir; cizici dosyayi bu adla
# arar (car_<id>_body / car_<id>_detail). Yeni bir govde eklenirse buraya bir
# satir eklemek yeterli — ama sinifini de yazmak ZORUNLU, cunku tuval ve
# denetim kutusu oradan geliyor.

MAPPING = {
    "hatchback": ("01_sehir", BINEK),
    "race_sedan": ("02_yaris_sedan", BINEK),
    "kus_slx": ("Kuş SLX", BINEK),
    "mountain_goat": ("DağKeçisi", BINEK),
    "muscle": ("03_kas_arabasi", BINEK),
    "muscle_67": ("Boğa67", BINEK),
    "supercar": ("04_super_araba", BINEK),
    "beety": ("beety", BINEK),
    # F1: referans orani 0.473, BINEK'in orani 0.526 — fark %10 ama YON
    # zararsiz. Olculdu (2026-08-17, yazilmis dosyadan): cizim 28.67 dp,
    # kutu 28.16 dp, kenar basina +0.25 dp pay. Yani F1 BINEK'e oturuyor ve
    # ayri bir sinif gerekmiyor (docs/VEHICLE_CLASSES.md §"F1 neden yeni
    # sinif istemiyor"; belge +0.32 ongormustu, dosyadan olculen 0.07 dp daha
    # dar — fark LANCZOS + kayipli alfa, bu yuzden olcum belgeden degil
    # DOSYADAN aliniyor). Kutuyu genisletmek yasak: oyuncunun kutusunu
    # yanlamasina buyutmek dogrudan "havaya carptim" uretir.
    "f1": ("10_f1", BINEK),
    # Trafik araci: oyuncunun alamadigi sabit govde. Sprite'a gecmesi ayrica
    # KAZANC — sahnede ayni anda onlarca trafik araci var ve her biri vektorde
    # ~20 parca cizdiriyordu.
    "traffic": ("08_trafik_arac", BINEK),
    # Motosiklet: kendi sinifi olmadan oyuna KESINLIKLE girmemeli. BINEK
    # kutusunda cizimi 22.80 dp, kutu 28.16 dp olurdu — kenar basina 2.68 dp
    # havaya carpma, otomobildekinin ~14 kati ve bu GORULUR.
    "motosiklet": ("07_motosiklet", MOTOSIKLET),
    # Tir: yalnizca trafik gövdesi olarak tasarlandi (oyuncuya verilmesi
    # 0.80 s'lik maruz kalma suresiyle oynanisi kilitliyor). Burasi sprite
    # uretiyor; kimin surdugune CarCatalog karar verir.
    "tir": ("09_tir", AGIR),
}

# Onizlemede kullanilan boyalar (CarCatalog'daki bodyArgb degerleri).
PREVIEW_COLORS = [
    ("kirmizi", (0xE1, 0x06, 0x00)),
    ("mavi", (0x1B, 0x3F, 0xD1)),
    ("sari", (0xF5, 0xC5, 0x18)),
    ("buzul", (0xED, 0xF1, 0xF5)),
    ("mor", (0x7B, 0x2F, 0xF7)),
]


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


def build(shape_id: str, stem: str, vclass: VehicleClass, boost: float = 1.0):
    """Bir govdeyi KENDI SINIFININ tuvaline uretir.

    Doner: (body baytlari, detail baytlari, ham ortanca, gamma, kirpma orani,
            referans orani).
    """
    target_w, target_h = vclass.canvas_w, vclass.canvas_h

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
    solid_h = solid[3] - solid[1]
    ref_ratio = solid_w / float(solid_h)

    cw, ch = body.size

    # Olcek iki alt sinirin buyugu:
    #   1) [fit]    — orani bozmadan SINIFIN kutusuna sigdir (eski, TEK basina
    #                 yetersiz olan davranis: dar referansta saydam bosluk),
    #   2) [needed] — SINIFIN carpisma kutusunu ORTMEYE yeten en kucuk olcek.
    # Genis referanslarda (1), dar/uzun referanslarda (2) belirleyici olur.
    # Iki durumda da TEK bir sayi ile olceklendigi icin oran korunur; arac
    # hicbir kosulda yatayda gerdirilmez.
    #
    # [boost] yakinsama dongusunun duzeltmesi: dosyadan geri okunan genislik
    # hedefin altinda kaldiysa cagiran 1'den buyuk bir deger verir
    # (bkz. [_build_until_covered]).
    hitbox_w_canvas = target_w * HITBOX_SCALE
    fit = min(target_w / cw, target_h / ch)
    needed = (hitbox_w_canvas + 2.0 * vclass.dp_to_canvas_px(HITBOX_EDGE_MARGIN_DP)) / solid_w
    scale = max(fit, needed) * boost

    nw, nh = max(1, round(cw * scale)), max(1, round(ch * scale))

    # Yatayda tasma yalnizca referansin TUYU dolu kismindan cok genisse olur
    # (orn. cevresinde parlama efekti). O durumda kirpmak gercek cizimi keser;
    # sessizce yapmak yerine duruyoruz.
    if nw > target_w:
        raise ValueError(
            "%s (%s): cizim yataya sigmiyor (%d > %d px). Referansin dolu "
            "govdesi tuylu kenarina gore cok dar — kenar efektini referanstan "
            "temizle." % (stem, vclass.name, nw, target_w)
        )

    # Dikey tasma ustten ve alttan ESIT kirpilir (arac kutuda ortali kalsin).
    trim = max(0, (nh - target_h) // 2)
    trim_fraction = trim / float(nh)
    if trim_fraction > MAX_TRIM_FRACTION:
        raise ValueError(
            "%s: carpisma kutusunu ortmek icin her ucundan %%%.1f kirpmak "
            "gerekiyor (sinir %%%.1f). Referansin en/boy orani %.3f, %s "
            "sinifininki %.3f — bu arac bu sinifa ait degil. Ya referans "
            "yeniden cizilmeli, ya baska bir sinifa konmali, ya da yeni bir "
            "sinif tanimlanmali (VehicleClass + CarCatalog isi)."
            % (stem, trim_fraction * 100.0, MAX_TRIM_FRACTION * 100.0,
               cw / float(ch), vclass.name, target_w / float(target_h))
        )

    ox = (target_w - nw) // 2
    oy = 0 if nh > target_h else (target_h - nh) // 2

    dq = QUALITY_DELTA_BY_CLASS.get(vclass.name, 0)
    written = []
    layers = (
        ("body", body, WEBP_QUALITY_BODY + dq),
        ("detail", detail, WEBP_QUALITY_DETAIL + dq),
    )
    for name, layer, quality in layers:
        scaled = layer.resize((nw, nh), Image.LANCZOS)
        if nh > target_h:
            top = (nh - target_h) // 2
            scaled = scaled.crop((0, top, nw, top + target_h))
        canvas = Image.new("RGBA", (target_w, target_h), (0, 0, 0, 0))
        canvas.paste(scaled, (ox, oy))
        out_path = os.path.join(OUT, f"car_{shape_id}_{name}.webp")
        canvas.save(out_path, "WEBP", quality=quality, method=6)
        written.append(os.path.getsize(out_path))
    return written[0], written[1], raw_mid, gamma, trim_fraction, ref_ratio


def audit(shape_id: str, vclass: VehicleClass) -> tuple[float, float]:
    """YAZILMIS dosyalari geri okuyup cizimin SINIFIN kutusunu ortup ortmedigini olcer.

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
        img = Image.open(path).convert("RGBA")
        if img.size != (vclass.canvas_w, vclass.canvas_h):
            raise ValueError(
                "%s: dosya %dx%d, %s sinifinin tuvali %dx%d — eski bir uretim "
                "kalmis olabilir." % (os.path.basename(path), img.size[0],
                                      img.size[1], vclass.name,
                                      vclass.canvas_w, vclass.canvas_h))
        layers.append(img.split()[3])
    combined = ImageChops.add(layers[0], layers[1])
    box = combined.point(lambda i: 255 if i >= SOLID_ALPHA else 0).getbbox()
    if box is None:
        raise ValueError("uretilen sprite bos: " + shape_id)
    w_px = (box[2] - box[0]) / float(vclass.canvas_w) * vclass.art_w_dp
    h_px = (box[3] - box[1]) / float(vclass.canvas_h) * vclass.art_h_dp
    return w_px, h_px


def _build_until_covered(shape_id: str, stem: str, vclass: VehicleClass):
    """Sprite'i uretir; DOSYADAN olculen genislik hedefin altindaysa yeniden uretir.

    Neden bir dongu: gereken olcegi referanstan hesaplamak yetmiyor. LANCZOS
    ile kucultup kayipli WebP'ye yazdiktan sonra kenardaki birkac piksel
    [SOLID_ALPHA] esiginin altina dusuyor ve arac hesaplanandan 0.1-0.2 dp dar
    cikiyor. Kayip her referansta ayni degil, o yuzden sabit bir telafi sayisi
    ya yetmez ya da gereksiz yere buyutur. Dongu her araca SADECE kendi
    kaybi kadar duzeltme uygular; hedefi tutan araca hic dokunmaz.
    """
    target_w = vclass.hitbox_w_dp + 2.0 * HITBOX_EDGE_MARGIN_DP
    boost = 1.0
    for attempt in range(1, MAX_FIT_ATTEMPTS + 1):
        stats = build(shape_id, stem, vclass, boost)
        w_px, h_px = audit(shape_id, vclass)
        if w_px >= target_w:
            return stats, w_px, h_px, attempt
        boost *= (target_w / w_px) * FIT_OVERSHOOT
    # Buraya dusmek "buyutmek ise yaramiyor" demektir; son olculen deger
    # oz-denetime gider ve orada net mesajla patlar.
    return stats, w_px, h_px, MAX_FIT_ATTEMPTS


def write_preview(shape_id: str, vclass: VehicleClass, out_dir: str) -> str:
    """Govde+detay katmanlarini birkac boyayla birlestirip PNG yazar.

    Oyunun yaptigi isin AYNISI: govde secili boyayla CARPILIR
    (``BlendMode.Modulate``), detay katmani ustune oldugu gibi cizilir. Amac
    "guzel gorunuyor mu" degil, gozle iki seyi dogrulamak: (a) boya govdeye
    gercekten islemis mi, (b) maske govde/detay sinirini dogru kesmis mi
    (lastigin boyanmasi ya da camin gri kalmasi burada gorulur).
    """
    body = Image.open(os.path.join(OUT, f"car_{shape_id}_body.webp")).convert("RGBA")
    detail = Image.open(os.path.join(OUT, f"car_{shape_id}_detail.webp")).convert("RGBA")
    w, h = body.size

    pad = 12
    tile_w = w + pad
    sheet = Image.new("RGBA", (tile_w * len(PREVIEW_COLORS) + pad, h + 2 * pad),
                      (24, 27, 33, 255))
    for i, (_, rgb) in enumerate(PREVIEW_COLORS):
        tint = Image.new("RGBA", (w, h), rgb + (255,))
        tinted = ImageChops.multiply(body.convert("RGB"), tint.convert("RGB"))
        tinted = Image.merge("RGBA", (*tinted.split(), body.split()[3]))
        frame = Image.alpha_composite(tinted, detail)
        sheet.alpha_composite(frame, (pad + i * tile_w, pad))

    os.makedirs(out_dir, exist_ok=True)
    path = os.path.join(out_dir, f"preview_{shape_id}_{vclass.name.lower()}.png")
    sheet.save(path, "PNG")
    return path


def main(argv: list[str]) -> int:
    preview_dir = None
    if "--preview" in argv:
        i = argv.index("--preview")
        if i + 1 >= len(argv):
            print("--preview bir klasor yolu ister")
            return 2
        preview_dir = argv[i + 1]

    os.makedirs(OUT, exist_ok=True)
    total = 0
    rows = []
    for shape_id, (stem, vclass) in MAPPING.items():
        stats, w_px, h_px, attempts = _build_until_covered(shape_id, stem, vclass)
        body_bytes, detail_bytes, raw_mid, gamma, trim, ref_ratio = stats
        total += body_bytes + detail_bytes
        notes = ""
        if trim > 0:
            notes += "  (uc basina %%%.1f kirpildi)" % (trim * 100.0)
        if attempts > 1:
            notes += "  (%d turda oturdu)" % attempts
        ratio_diff = abs(ref_ratio - vclass.ratio) / vclass.ratio
        if ratio_diff > RATIO_WARN_FRACTION:
            notes += "  (oran farki %%%.1f: ref %.3f / sinif %.3f)" % (
                ratio_diff * 100.0, ref_ratio, vclass.ratio)
        print(
            "%-14s %-10s %4dx%-4d <- %-16s body %5.1f KB  detail %5.1f KB  "
            "ortanca %3d -> gamma %.2f%s"
            % (shape_id, vclass.name, vclass.canvas_w, vclass.canvas_h, stem,
               body_bytes / 1024, detail_bytes / 1024, raw_mid, gamma, notes)
        )
        rows.append((shape_id, vclass, w_px, h_px))
    print("-" * 72)
    print("%d govde, toplam %.1f KB" % (len(MAPPING), total / 1024))

    # --- Oz-denetim -------------------------------------------------------
    #
    # Tek kural: CIZILEN arac KENDI SINIFININ carpisma kutusundan DAR OLAMAZ.
    # Dar kalirsa oyuncu aracin yanindaki bosluga carpar ("havaya carptim").
    # Ters yon (cizim kutudan genis) istenen durumdur — bu tur oyunlarda
    # affedici hissettirir, bkz. GameConfig.HITBOX_SCALE.
    #
    # Bu kontrol 2026-08-16'da eklendi: sigdirma kurali sessizce Boga 67'yi
    # kutudan 0.21 dp dar uretmisti ve hicbir yerde patlamamisti. 2026-08-17'de
    # sinif bazina cevrildi: artik her govde TEK bir kutuya degil, KENDI
    # sinifinin kutusuna karsi olculuyor — yoksa motosiklet BINEK kutusuyla
    # olculur ve "ihlal" diye yanlis alarm verir, ya da tersi.
    print()
    print("oz-denetim: cizim >= KENDI SINIFININ carpisma kutusu")
    print("%-14s %-10s %13s %19s   %s"
          % ("govde", "sinif", "kutu (dp)", "cizim (dp)", "pay (kenar basina)"))
    bad = []
    for shape_id, vclass, w_px, h_px in rows:
        hb_w, hb_h = vclass.hitbox_w_dp, vclass.hitbox_h_dp
        mx, my = (w_px - hb_w) / 2.0, (h_px - hb_h) / 2.0
        flag = "OK " if (mx >= 0 and my >= 0) else "IHLAL"
        print("  %s %-12s %-10s %6.2fx%-6.2f %8.2fx%-8.2f   x %+.2f  y %+.2f"
              % (flag, shape_id, vclass.name, hb_w, hb_h, w_px, h_px, mx, my))
        if mx < 0 or my < 0:
            bad.append((shape_id, vclass, mx, my))

    if bad:
        print()
        for shape_id, vclass, mx, my in bad:
            print(
                "HATA: %s (%s) carpisma kutusundan dar uretildi (x payi %+.2f "
                "dp, y payi %+.2f dp). Bu haliyle oyuncu aracin yanindaki "
                "BOSLUGA carpar. Referansin en/boy orani sinifin kutusuna "
                "uymuyor demektir; HITBOX_EDGE_MARGIN_DP / MAX_TRIM_FRACTION "
                "degerlerine, gövdenin SINIFINA ve referansin kendisine bak."
                % (shape_id, vclass.name, mx, my)
            )
        return 1

    if preview_dir:
        print()
        print("onizlemeler (%d boya ile carpilmis):" % len(PREVIEW_COLORS))
        for shape_id, vclass, _, _ in rows:
            print("  " + write_preview(shape_id, vclass, preview_dir))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
