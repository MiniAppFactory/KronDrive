"""Gercekci (hacimli) yaris otomobili — YALNIZCA feature graphic icin.

Neden ayri dosya: `kron_art.py` oyunun KODUNDAN alinan geometriyi tasir ve
oradaki her sayinin bir karsiligi vardir. Buradaki arac ise bilincli bir
sapmadir — sahibin karari: "Feature'i illa oyunun kendi asseti olan araba
kullanman sart degil, gercekci araba kullanabilirsin." Oyun ici gorunum
DEGISMEZ; bu cizim yalnizca magaza vitrinine girer.

Hicbir stok/hazir gorsel kullanilmiyor, her sekil burada uretiliyor
(prototipteki "pngtree" filigranli arac lisans riski yuzunden reddedilmisti).

Marka bagi: ana govde KRON kirmizisi #E10600 (CarColorDef.kron_red).

Yerel uzay (burun YUKARI, -y yonu):
    x  -20..20    govde genisligi 40 birim
    y    0        burun
    y   90        govdenin arka kenari
    y   90..97    arkadan GORUNEN dikey panel (kamera arkada ve yukarida)
    y  <0         far huzmesi
    y  >97        boost alevi
Tekerlekler govdenin disina tasar (x +-23.6) -> toplam genislik 47.2 birim.

## 2026-08-15 duzeltmeleri (sahibin yakinlastirarak yaptigi inceleme)

1. **Zemin golgesi KALDIRILDI.** Aracin altina cizilen bulanik hale aractan
   buyuktu ve yakinlastirinca asfaltta kirli bir leke gibi duruyordu. Hacim
   artik yalnizca gövde gradyani, cam yansimasi ve tekerlek derinliginden
   geliyor. Aracin KENDI uzerindeki ic golgeler (kanat temas golgesi vb.)
   duruyor — kaldirilan, ASFALTA yansiyan dis hale.
2. **Arka yuz kisaldi.** Dikey panel 12 -> 7 birim; ustune asagi dogru
   kararan bir gradyan, tavanla arasina ince bir kirilma cizgisi kondu ve
   stop lambalari kalin bloklar yerine ince seritlere donustu. Ustten
   bakista arka yuz tavanla ayni duzlemde duramaz — kisalarak yansir.
3. **Alev yumusadi.** Iki keskin ucgen ("lego gibi") yerine halo + uce
   dogru hem daralan hem SAYDAMLASAN uc pluma.
"""

import math

from PIL import Image, ImageChops, ImageDraw, ImageFilter

from kron_art import ACCENT, FLAME_CORE, FLAME_INNER, FLAME_OUTER, TRIM

LX0, LX1 = -27.0, 27.0
LY0, LY1 = -58.0, 136.0

# Govde ust yuzeyi: (y, yari genislik). Burun dar; on/arka tekerlek
# kemerlerinde sisme, kapilarda bel var — tepeden bakinca tekerlekler dort
# koseden disari taskin gorunsun diye.
BODY_OUTLINE = [
    (0.0, 6.8), (4.0, 11.4), (10.0, 14.6), (15.0, 16.9), (21.0, 17.5),
    (27.0, 16.8), (34.0, 15.6), (48.0, 15.5), (58.0, 16.6), (67.0, 18.5),
    (76.0, 18.8), (84.0, 18.2), (90.0, 16.8),
]

WHEELS = [                     # (x0, y0, x1, y1) — arka tekerler daha genis
    (-24.0, 10.0, -17.0, 28.0), (17.0, 10.0, 24.0, 28.0),
    (-24.8, 58.0, -17.0, 80.0), (17.0, 58.0, 24.8, 80.0),
]

GLASS_DEEP = (0x12, 0x1B, 0x30)
GLASS_LIT = (0x46, 0x62, 0x8F)
SPOILER = (0x14, 0x18, 0x1F)
TAIL_LIGHT = (0xFF, 0x2E, 0x1A)
HEAD_LIGHT = (0xFF, 0xFA, 0xD2)     # drawNightHeadlights ile ayni sicak beyaz


def _mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def _hgrad(w, h, stops):
    """Yatay gradyan. stops: [(0..1 konum, rgb)] soldan saga sirali."""
    img = Image.new('RGB', (w, h))
    d = ImageDraw.Draw(img)
    for x in range(w):
        p = x / max(1, w - 1)
        col = stops[-1][1]
        for i in range(len(stops) - 1):
            p0, c0 = stops[i]
            p1, c1 = stops[i + 1]
            if p <= p1:
                t = 0.0 if p1 <= p0 else (p - p0) / (p1 - p0)
                col = _mix(c0, c1, max(0.0, min(1.0, t)))
                break
        d.line([(x, 0), (x, h)], fill=col)
    return img


def _vmul(w, h, stops):
    """Dikey carpan maskesi (L). stops: [(0..1 konum, 0..1 parlaklik)]."""
    img = Image.new('L', (w, h))
    d = ImageDraw.Draw(img)
    for y in range(h):
        p = y / max(1, h - 1)
        v = stops[-1][1]
        for i in range(len(stops) - 1):
            p0, v0 = stops[i]
            p1, v1 = stops[i + 1]
            if p <= p1:
                t = 0.0 if p1 <= p0 else (p - p0) / (p1 - p0)
                v = v0 + (v1 - v0) * max(0.0, min(1.0, t))
                break
        d.line([(0, y), (w, y)], fill=int(round(255 * v)))
    return img


def _soft_flame(size, P, unit, root, phase=1.37):
    """Yumusak, katmanli boost alevi (CarArtwork.drawCarBoostFlame ile ayni sema).

    halo + dis pluma + ic pluma + sicak cekirdek. Her pluma ucuna dogru hem
    DARALIR hem SAYDAMLASIR — kenarlarin sonumlenmesi buradan geliyor; keskin
    ucgen kenari "lego" hissini veren seydi.
    """
    flick = 1.0 + 0.13 * math.sin(phase * 17.0) + 0.07 * math.sin(phase * 27.3)
    wob = 1.0 + 0.09 * math.sin(phase * 21.0 + 1.7)
    layer = Image.new('RGBA', size, (0, 0, 0, 0))

    halo = Image.new('RGBA', size, (0, 0, 0, 0))
    hd = ImageDraw.Draw(halo)
    hr, steps = 26.0, 18
    c = P(0.0, root + hr * 0.40)
    for i in range(steps, 0, -1):
        t = i / steps
        rr = hr * t * unit
        hd.ellipse([c[0] - rr * 0.72, c[1] - rr, c[0] + rr * 0.72, c[1] + rr],
                   fill=FLAME_OUTER + (int(round(70 * (1.0 - t) ** 1.7)),))
    layer.alpha_composite(halo)

    def plume(hw, length, color, alpha0, samples=30):
        pl = Image.new('RGBA', size, (0, 0, 0, 0))
        pd = ImageDraw.Draw(pl)
        for i in range(samples):
            t0, t1 = i / samples, (i + 1) / samples
            w0, w1 = hw * (1.0 - t0 ** 1.5), hw * (1.0 - t1 ** 1.5)
            y0, y1 = root + length * t0, root + length * t1
            pd.polygon([P(-w0, y0), P(w0, y0), P(w1, y1), P(-w1, y1)],
                       fill=color + (int(round(alpha0 * (1.0 - t0) ** 1.25)),))
        return pl

    layer.alpha_composite(plume(8.5 * wob, 34.0 * flick, FLAME_OUTER, 215))
    layer.alpha_composite(plume(5.0 * wob, 21.0 * flick, FLAME_INNER, 235))
    layer.alpha_composite(plume(2.6 * wob, 13.0 * flick, FLAME_CORE, 250))
    return layer.filter(ImageFilter.GaussianBlur(max(1.0, unit * 0.9)))


def _outline():
    right = [(hw, y) for y, hw in BODY_OUTLINE]
    left = [(-hw, y) for y, hw in reversed(BODY_OUTLINE)]
    return right + left


def realistic_car(body_w, base, flame=False, beam=True, stripe=True,
                  stripe_color=None, squash=0.88, ss=3):
    """3/4 ust-arka acidan hacimli yaris otomobili. Burun YUKARI (-y) bakar.

    body_w : govde genisliginin cikti pikseli (tekerlekler bunun disina tasar)
    base   : ana govde rengi (oyuncu icin #E10600)
    Donen  : (katman, body_center_y). body_center_y katmanin ustunden govde
             merkezine olan piksel mesafesi — yerlestirme bunu kullanir, cunku
             katmanda far huzmesi ve alev icin asimetrik pay var.
    """
    unit_out = body_w / 40.0
    unit = unit_out * ss
    W = int(round((LX1 - LX0) * unit))
    H = int(round((LY1 - LY0) * unit))
    img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    def P(x, y):
        return ((x - LX0) * unit, (y - LY0) * unit)

    def poly(points, fill):
        d.polygon([P(*p) for p in points], fill=fill)

    def rrect(x0, y0, x1, y1, r, fill):
        a, b = P(min(x0, x1), min(y0, y1)), P(max(x0, x1), max(y0, y1))
        d.rounded_rectangle([a[0], a[1], b[0], b[1]], radius=r * unit, fill=fill)

    shade = _mix(base, (0, 0, 0), 0.55)
    dark = _mix(base, (0, 0, 0), 0.72)
    bright = _mix(base, (255, 255, 255), 0.46)
    rim = _mix(base, (255, 255, 255), 0.80)

    # --- 1. (zemin golgesi 2026-08-15'te KALDIRILDI, bkz. dosya basligi) ---

    # --- 2. far huzmesi: gece sahnesinde isik yolun ILERISINE duser --------
    if beam:
        bm = Image.new('RGBA', (W, H), (0, 0, 0, 0))
        ImageDraw.Draw(bm).polygon(
            [P(-11.0, 6.0), P(-30.0, -54.0), P(30.0, -54.0), P(11.0, 6.0)],
            fill=HEAD_LIGHT + (52,))
        img.alpha_composite(bm.filter(ImageFilter.GaussianBlur(int(5 * unit))))

    # --- 3. tekerlekler (govdenin ALTINDA) ---------------------------------
    for x0, y0, x1, y1 in WHEELS:
        rrect(x0, y0, x1, y1, 2.2, (0x0A, 0x0A, 0x0C, 255))
        rrect(x0 + 0.7, y0 + 1.2, x1 - 0.7, y0 + 2.6, 0.7, (0x3C, 0x42, 0x4C, 200))
        cy = (y0 + y1) / 2.0
        rrect(x0 + 1.4, cy - 1.6, x1 - 1.4, cy + 1.6, 1.2, TRIM + (95,))

    # --- 4. govde ust yuzeyi: silindirik yanal gradyan + boyuna kararma ----
    mask = Image.new('L', (W, H), 0)
    ImageDraw.Draw(mask).polygon([P(*p) for p in _outline()], fill=255)
    grad = _hgrad(W, H, [
        (0.00, dark), (0.14, shade), (0.28, bright), (0.42, base),
        (0.62, base), (0.80, shade), (1.00, dark),
    ])
    ylo = (0.0 - LY0) / (LY1 - LY0)
    yhi = (90.0 - LY0) / (LY1 - LY0)
    grad = ImageChops.multiply(grad, _vmul(W, H, [
        (0.0, 0.55), (ylo, 0.74), (ylo + 0.06, 1.0),
        (yhi - 0.05, 1.0), (yhi, 0.82), (1.0, 0.60),
    ]).convert('RGB'))
    body = grad.convert('RGBA')
    body.putalpha(mask)
    img.alpha_composite(body)

    # --- 5. arkadan GORUNEN dikey panel ------------------------------------
    #
    # KISA (7 birim = gövde boyunun %8'i) ve asagi dogru KARARAN bir trapez.
    # Ustten bakan bir kamera icin dogru olan bu: arka yuz tavana dik durur,
    # dolayisiyla bize dogru kisalarak yansir. Onceden 12 birimlik duz renkli
    # bir slabdi ve tavanla ayni duzlemdeymis gibi okunuyordu.
    rear_mask = Image.new('L', (W, H), 0)
    ImageDraw.Draw(rear_mask).polygon(
        [P(-17.8, 90.0), P(17.8, 90.0), P(16.4, 97.0), P(-16.4, 97.0)], fill=255)
    y_r0 = (90.0 - LY0) / (LY1 - LY0)
    y_r1 = (97.0 - LY0) / (LY1 - LY0)
    rear = Image.new('RGB', (W, H), _mix(base, (0, 0, 0), 0.34))
    rear = ImageChops.multiply(rear, _vmul(W, H, [
        (0.0, 1.0), (y_r0, 1.0), (y_r0 + 0.001, 0.86),
        (y_r1, 0.46), (1.0, 0.46),
    ]).convert('RGB')).convert('RGBA')
    rear.putalpha(rear_mask)
    img.alpha_composite(rear)

    # Tavan ile arka yuz arasindaki KIRILMA CIZGISI: gozun "iki ayri duzlem"
    # okumasini saglayan tek detay.
    poly([(-17.8, 89.4), (17.8, 89.4), (17.6, 90.6), (-17.6, 90.6)],
         rim + (150,))

    # --- 6. spoiler (tavan duzleminde, YUKSELTILMIS) ----------------------
    rrect(-18.6, 88.4, 18.6, 90.2, 0.8, (0, 0, 0, 90))     # temas golgesi
    rrect(-18.6, 82.0, 18.6, 89.5, 1.2, SPOILER + (255,))
    rrect(-18.6, 82.0, 18.6, 83.6, 0.8, _mix(SPOILER, (255, 255, 255), 0.35) + (200,))

    # --- 7. stop lambalari: arka yuz UZERINDE ince seritler ---------------
    tl = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    td = ImageDraw.Draw(tl)
    for sx in (-1, 1):
        x0, x1 = sorted((sx * 6.4, sx * 14.8))
        a, b = P(x0 - 0.6, 91.4), P(x1 + 0.6, 95.4)
        td.rectangle([a[0], a[1], b[0], b[1]], fill=TAIL_LIGHT + (150,))
    img.alpha_composite(tl.filter(ImageFilter.GaussianBlur(int(2.2 * unit))))
    for sx in (-1, 1):
        rrect(sx * 6.4, 91.6, sx * 14.8, 94.8, 0.7, TAIL_LIGHT + (255,))
        rrect(sx * 6.4, 91.6, sx * 14.8, 92.3, 0.4,
              _mix(TAIL_LIGHT, (255, 255, 255), 0.45) + (190,))

    # --- 8. kaput detaylari + yaris seridi (CAMLARIN ALTINDA kalir) -------
    # Serit kaput ve bagaj uzerinde; cami kesmez (kestiginde arac ikiye
    # bolunmus gibi duruyordu).
    rrect(-11.0, -0.5, 11.0, 3.4, 1.2, dark + (255,))            # on splitter
    for sx in (-1, 1):
        for k in range(3):                                        # kaput havalandirmasi
            rrect(sx * 5.2, 13.5 + k * 2.6, sx * 9.8, 15.0 + k * 2.6, 0.5, dark + (200,))
    if stripe:
        st = Image.new('RGBA', (W, H), (0, 0, 0, 0))
        sd2 = ImageDraw.Draw(st)
        # Bagaj seridi spoilerin ALTINDA biter (82); onceden 88'e kadar gidip
        # spoilerin ve kirilma cizgisinin uzerine tasiyordu.
        for y0, y1 in ((5.0, 27.5), (72.0, 81.4)):
            a, b = P(-3.2, y0), P(3.2, y1)
            sd2.rectangle([a[0], a[1], b[0], b[1]],
                          fill=(stripe_color or ACCENT) + (238,))
        st.putalpha(ImageChops.multiply(st.split()[3], mask))
        img.alpha_composite(st)

    # --- 9. camlar: on cam / tavan / arka cam ------------------------------
    def glass(y0, hw0, y1, hw1):
        gm = Image.new('L', (W, H), 0)
        ImageDraw.Draw(gm).polygon(
            [P(-hw0, y0), P(hw0, y0), P(hw1, y1), P(-hw1, y1)], fill=255)
        gg = _hgrad(W, H, [(0.0, GLASS_DEEP), (0.32, GLASS_LIT),
                           (0.56, GLASS_DEEP), (1.0, GLASS_DEEP)]).convert('RGBA')
        gg.putalpha(gm)
        img.alpha_composite(gg)

    # Tavan: gövde ile ayni silindirik gradyani tasir (duz renk verildiginde
    # aracin ortasindan gecen kirmizi bir bar gibi duruyordu).
    roof_mask = Image.new('L', (W, H), 0)
    ImageDraw.Draw(roof_mask).polygon(
        [P(-13.0, 41.0), P(13.0, 41.0), P(13.4, 59.0), P(-13.4, 59.0)], fill=255)
    roof = _hgrad(W, H, [
        (0.00, dark), (0.16, shade), (0.30, _mix(base, (255, 255, 255), 0.34)),
        (0.46, _mix(base, (255, 255, 255), 0.12)), (0.64, base),
        (0.82, shade), (1.00, dark),
    ]).convert('RGBA')
    roof.putalpha(roof_mask)
    img.alpha_composite(roof)
    glass(28.0, 10.5, 42.0, 13.0)                        # on cam
    glass(58.0, 13.4, 72.0, 11.5)                        # arka cam

    sp = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    spd = ImageDraw.Draw(sp)
    spd.polygon([P(-10.2, 41.0), P(-3.8, 28.5), P(0.4, 28.5), P(-5.6, 41.0)],
                fill=(255, 255, 255, 96))
    spd.polygon([P(-12.2, 71.0), P(-7.4, 58.5), P(-3.6, 58.5), P(-8.6, 71.0)],
                fill=(255, 255, 255, 66))
    img.alpha_composite(sp.filter(ImageFilter.GaussianBlur(max(1, int(0.7 * unit)))))

    # --- 10. aynalar -------------------------------------------------------
    for sx in (-1, 1):
        poly([(sx * 16.0, 26.5), (sx * 21.0, 28.0), (sx * 21.0, 31.0),
              (sx * 16.0, 31.5)], shade + (255,))

    # --- 11. farlar (burun) ------------------------------------------------
    for sx in (-1, 1):
        rrect(sx * 6.0, 2.4, sx * 12.4, 6.4, 1.0, HEAD_LIGHT + (255,))

    # --- 12. govde kenarinda ince spekuler vurgu ---------------------------
    ed = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    ring = [P(*p) for p in _outline()]
    ImageDraw.Draw(ed).line(ring + [ring[0]], fill=rim + (92,),
                            width=max(1, int(0.9 * unit)))
    ed.putalpha(ImageChops.multiply(ed.split()[3], mask))
    img.alpha_composite(ed)

    # --- 13. boost alevi: govdenin ARKASINDA, ayni donusumden gecer --------
    if flame:
        img.alpha_composite(_soft_flame((W, H), P, unit, 97.5))

    out_w = int(round((LX1 - LX0) * unit_out))
    out_h = max(1, int(round((LY1 - LY0) * unit_out * squash)))
    out = img.resize((out_w, out_h), Image.LANCZOS)
    body_center_y = (46.0 - LY0) / (LY1 - LY0) * out_h
    return out, body_center_y
