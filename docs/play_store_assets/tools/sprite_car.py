"""Magaza gorselleri icin SPRITE tabanli arac katmani.

`kron_art.car_layer` oyunun vektor geometrisini Python'da yeniden cizer;
burasi ayni sozlesmeyi oyunun SPRITE'lari ile karsilar. Ikisi degistirilebilir
cunku her ikisi de araci ayni kutuya oturtur:

    x: CAR_X0..CAR_X1 = -20..20      y: CAR_Y0..CAR_Y1 = -2..74

Sprite'lar `tools/build_car_sprites.py` ile uretilir ve tam bu orandadir
(240x456), bu yuzden kutuya birebir oturuyorlar.

Neden gerekti: magaza ikonu ve feature gorselindeki araclar elle cizilmis
ilkel govdelerdi; oyun 2026-08-15'te fotogercekci sprite'lara gecince magaza
gorseli oyunu temsil etmez oldu (proje sahibi: "app iconda da yeni
arabalardan birisini kullanmak lazim").

Vektor yolu SILINMEDI: sprite'i olmayan bir govde icin hala `kron_art`
kullanilabilir.
"""

from __future__ import annotations

import os

from PIL import Image

import kron_art as K

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
SPRITE_DIR = os.path.join(
    ROOT, "source", "app", "src", "main", "res", "drawable-nodpi"
)

# Sprite tuvali = arac kutusunun TAMAMI. Bu iki sabit build_car_sprites.py
# ile ayni olmak ZORUNDA; degisirse arac kutuya yanlis oturur.
SPRITE_W, SPRITE_H = 240, 456

_cache: dict[str, tuple[Image.Image, Image.Image]] = {}


def has_sprite(shape_id: str) -> bool:
    return os.path.exists(os.path.join(SPRITE_DIR, f"car_{shape_id}_body.webp"))


def _load(shape_id: str) -> tuple[Image.Image, Image.Image]:
    if shape_id not in _cache:
        body = Image.open(os.path.join(SPRITE_DIR, f"car_{shape_id}_body.webp"))
        detail = Image.open(os.path.join(SPRITE_DIR, f"car_{shape_id}_detail.webp"))
        _cache[shape_id] = (body.convert("RGBA"), detail.convert("RGBA"))
    return _cache[shape_id]


def tinted(shape_id: str, body_rgb: tuple[int, int, int]) -> Image.Image:
    """Gri govde katmanini boyayla CARPAR, ustune detay katmanini cizer.

    Oyundaki ile ayni islem (`BlendMode.Modulate`) — magaza gorselindeki
    arac, oyundakiyle ayni pikselleri gosterir.
    """
    body, detail = _load(shape_id)
    r, g, b, a = body.split()
    cr, cg, cb = body_rgb
    layer = Image.merge(
        "RGBA",
        (
            r.point(lambda i: i * cr // 255),
            g.point(lambda i: i * cg // 255),
            b.point(lambda i: i * cb // 255),
            a,
        ),
    )
    return Image.alpha_composite(layer, detail)


def car_layer(
    body_rgb: tuple[int, int, int],
    unit: float = 16.0,
    pad: float = 6.0,
    flame: bool = False,
    shape: str = "hatchback",
    phase: float = 0.0,
):
    """`kron_art.car_layer` ile AYNI donusu verir: (image, x0, y0, unit).

    Alev sprite'ta yok (sprite yalnizca gövde); alev yine `kron_art`'in
    pluma cizicisinden geliyor, boylece oyunla ayni sekilde kaliyor.
    """
    y1 = K.CAR_Y1 + (K.FLAME_GAP + K.FLAME_OL if flame else 0.0)
    x0, y0 = K.CAR_X0 - pad, K.CAR_Y0 - pad
    w = int(round((K.CAR_X1 + pad - x0) * unit))
    h = int(round((y1 + pad - y0) * unit))
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))

    def px(x, y):
        return ((x - x0) * unit, (y - y0) * unit)

    if flame:
        img.alpha_composite(_flame(w, h, px, unit, phase))

    car = tinted(shape, body_rgb)
    # Kutunun piksel karsiligi: sprite tam olarak -20..20 x -2..74 arasini
    # kapliyor, dolayisiyla hedef dikdortgen dogrudan hesaplanabilir.
    left, top = px(K.CAR_X0, K.CAR_Y0)
    right, bottom = px(K.CAR_X1, K.CAR_Y1)
    target = (int(round(right - left)), int(round(bottom - top)))
    img.alpha_composite(
        car.resize(target, Image.LANCZOS), (int(round(left)), int(round(top)))
    )
    return img, x0, y0, unit


def _flame(w, h, px, unit, phase):
    return K._flame_layer((w, h), px, unit, K.CAR_Y1 + 0.8, phase)


def realistic_car(
    body_w: float,
    base: tuple[int, int, int],
    flame: bool = False,
    shape: str = "supercar",
    pad: float = 4.0,
):
    """`kron_car3d.realistic_car` ile ayni sozlesme: (katman, body_center_y).

    body_center_y, katmanin ustunden GOVDE MERKEZINE olan piksel mesafesidir.
    Yerlestirme bunu kullanir cunku katmanda alev icin asagida asimetrik pay
    var; govde merkezi katmanin merkezi DEGILDIR.
    """
    unit = body_w / (K.CAR_X1 - K.CAR_X0)
    img, x0, y0, _ = car_layer(base, unit=unit, pad=pad, flame=flame, shape=shape)
    body_center_y = ((K.CAR_Y0 + K.CAR_Y1) / 2.0 - y0) * unit
    return img, body_center_y


def tilted_car(
    body_rgb: tuple[int, int, int],
    target_w: float,
    tilt: float = 0.62,
    squash: float = 0.74,
    unit: float = 16.0,
    flame: bool = False,
    ss: int = 2,
    shape: str = "hatchback",
):
    """3/4 aciya yatirilmis arac — `kron_art.tilted_car`'in sprite karsiligi.

    Ust kenari [tilt] oraninda daraltip yuksekligi [squash] ile ezmek
    "kamera aracin arkasinda ve biraz yukarida" hissini verir; ikon ve
    feature gorselinde kullanilan tek perspektif budur.
    """
    car, _, _, _ = car_layer(body_rgb, unit=unit, flame=flame, shape=shape)
    w, h = car.size
    ow = int(round(target_w * ss))
    oh = int(round(ow * (h / w) * squash))
    inset = ow * (1.0 - tilt) / 2.0
    out = K.perspective(car, (ow, oh), [(inset, 0), (ow - inset, 0), (ow, oh), (0, oh)])
    return out.resize((int(round(target_w)), max(1, oh // ss)), Image.LANCZOS)
