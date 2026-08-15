"""Ekran goruntusu metin seridi (caption) sablonu.

Ekran goruntulerinin KENDISI burada uretilmez — bu makinede adb/emulator yok.
Sahibi telefondan ham PNG gonderdiginde bu betik ustune metin seridini gecirir.

Kullanim:
    py caption_screenshot.py ham.png cikti.png "Baslik" ["Alt satir"]

Sabitler ui/theme/Color.kt'ten: Accent F5C100, TextPrimary EAF1FB,
BlueBright 56E9FF, Background 020915.
"""

import os
import sys

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import kron_art as K

FONT_BLACK = r'C:\Windows\Fonts\ariblk.ttf'
FONT_BOLD = r'C:\Windows\Fonts\arialbd.ttf'

STRIP_RATIO = 0.20          # ust %20
SIDE_MARGIN = 0.08          # her yandan %8 guvenli bosluk


def _fit(text, path, max_w, start_px):
    """Metin max_w'ye sigana kadar punto kucultur."""
    size = start_px
    while size > 10:
        f = ImageFont.truetype(path, size)
        if f.getlength(text) <= max_w:
            return f
        size -= 2
    return ImageFont.truetype(path, 10)


def caption(base, title, subtitle=None):
    """Ham ekran goruntusunun ustune yari saydam koyu serit + metin gecirir."""
    img = base.convert('RGBA')
    w, h = img.size
    strip_h = int(h * STRIP_RATIO)
    max_w = int(w * (1 - 2 * SIDE_MARGIN))

    # Serit: ustte opak, altta seffaf — ekran goruntusune yumusak baglanir
    strip = Image.new('RGBA', (w, strip_h), (0, 0, 0, 0))
    d = ImageDraw.Draw(strip)
    r, g, b = 0x02, 0x09, 0x15                       # KronColors.Background
    for y in range(strip_h):
        t = y / max(1, strip_h - 1)
        a = int(238 * (1.0 - t ** 2.1))
        d.line([(0, y), (w, y)], fill=(r, g, b, a))
    img.alpha_composite(strip)

    d = ImageDraw.Draw(img)
    f_title = _fit(title, FONT_BLACK, max_w, int(strip_h * 0.30))
    f_sub = _fit(subtitle, FONT_BOLD, max_w, int(strip_h * 0.135)) if subtitle else None

    # Yukseklikler gercek cizim kutusundan olculur; punto degistiginde de
    # ogeler ust uste binmesin diye "size * katsayi" tahmini KULLANILMAZ.
    tb = d.textbbox((0, 0), title, font=f_title)
    title_h = tb[3] - tb[1]
    rule_h = max(4, int(w * 0.006))
    gap1, gap2 = int(strip_h * 0.085), int(strip_h * 0.075)
    total = title_h + gap1 + rule_h
    sb = None
    if f_sub:
        sb = d.textbbox((0, 0), subtitle, font=f_sub)
        total += gap2 + (sb[3] - sb[1])

    y = int((strip_h - total) * 0.46)                 # seride dikey ortala
    d.text(((w - f_title.getlength(title)) / 2, y - tb[1]), title,
           font=f_title, fill=K.ACCENT_YELLOW)

    ry = y + title_h + gap1
    d.rectangle([(w - 130) / 2, ry, (w + 130) / 2, ry + rule_h], fill=K.BLUE_BRIGHT)

    if f_sub:
        sy = ry + rule_h + gap2
        d.text(((w - f_sub.getlength(subtitle)) / 2, sy - sb[1]), subtitle,
               font=f_sub, fill=K.TEXT_PRIMARY)
    return img.convert('RGB')


def main():
    if len(sys.argv) < 4:
        print(__doc__)
        return 1
    src, dst, title = sys.argv[1], sys.argv[2], sys.argv[3]
    sub = sys.argv[4] if len(sys.argv) > 4 else None
    out = caption(Image.open(src), title, sub)
    out.save(dst)
    print('%s  %sx%s' % (dst, out.width, out.height))
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
