"""Ekran goruntusu metin seridi sablonunun ORNEKLERI (TR + EN) ve bos katmani.

Onemli: buradaki zemin OYUNDAN ALINMA DEGILDIR — bilerek "ham ekran
goruntusu buraya" yazan bir yer tutucudur. Bu makinede adb/emulator yok;
sahte oynanis goruntusu uretilmez.
"""

import os
import sys

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import kron_art as K
from caption_screenshot import caption, STRIP_RATIO, FONT_BOLD

OUT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
W, H = 1080, 2400          # tipik telefon ekran goruntusu (9:20)


def placeholder(label):
    img = K.vertical_gradient((W, H), (0x0A, 0x13, 0x22), (0x04, 0x0D, 0x1B)).convert('RGBA')
    d = ImageDraw.Draw(img)
    f = ImageFont.truetype(FONT_BOLD, 46)
    for i, line in enumerate(label):
        tw = f.getlength(line)
        d.text(((W - tw) / 2, H * 0.48 + i * 66), line, font=f, fill=(0x60, 0x72, 0x8B))
    # Serit sinirini goster (yer tutucuda; gercek ekran goruntusune cizilmez)
    y = int(H * STRIP_RATIO)
    for x in range(0, W, 40):
        d.line([(x, y), (x + 20, y)], fill=(0x33, 0x44, 0x5C), width=3)
    d.rectangle([2, 2, W - 3, H - 3], outline=(0x1C, 0x27, 0x40), width=4)
    return img


def strip_only():
    """Sadece serit — herhangi bir gorsel duzenleyicide katman olarak kullanilir."""
    img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    strip_h = int(H * STRIP_RATIO)
    d = ImageDraw.Draw(img)
    for y in range(strip_h):
        t = y / max(1, strip_h - 1)
        d.line([(0, y), (W, y)], fill=(0x02, 0x09, 0x15, int(238 * (1.0 - t ** 2.1))))
    return img


def main():
    jobs = [
        # Kisa baslik = buyuk punto. Uzun cumle yazarsan otomatik kuculur.
        ('tr', ['HAM EKRAN GÖRÜNTÜSÜ', 'BURAYA GELECEK'],
         'ŞERİDİ DEĞİŞTİR', 'Tek parmakla kontrol · 4 pist'),
        ('en', ['RAW SCREENSHOT', 'GOES HERE'],
         'SWITCH LANES', 'One-thumb control · 4 tracks'),
    ]
    for lang, label, title, sub in jobs:
        out = caption(placeholder(label), title, sub)
        p = os.path.join(OUT, 'screenshot_caption_example_%s.png' % lang)
        out.save(p)
        print('%s  %sx%s' % (p, out.width, out.height))

    p = os.path.join(OUT, 'screenshot_caption_strip_1080x2400.png')
    strip_only().save(p)
    print(p)


if __name__ == '__main__':
    main()
