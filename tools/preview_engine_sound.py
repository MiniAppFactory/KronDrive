"""Motor sesi envanteri: oyunun EngineVoice sentezini WAV'a dokerek dinletir.

## Neden var

Ses profilleri (`audio/CarSoundProfile.kt`) on alti sayidan olusuyor ve o
sayilarin kulakta ne ettigi koda bakarak anlasilmiyor. Bir profili denemek
icin APK derleyip cihaza kurup oynamak gerekiyordu; bu betik o donguyu
kaldiriyor.

## Envanter nerede

**Ses envanterinin TEK kaynagi `audio/CarSoundProfile.kt`.** Bu betik o
dosyayi PARSE eder — elle kopyalanmis bir tablo tutmaz, cunku kopya er ya da
gec kodla ayrisir ve envanter yalan soylemeye baslar. Yeni bir araç eklenip
profili yazildigi anda burada da cikar.

    py tools/preview_engine_sound.py <cikis_klasoru>

Her govde icin bir WAV yazar ve karsilastirma tablosunu basar.

## Dogruluk sozlesmesi

Asagidaki `render` fonksiyonu `EngineVoice.render`'in BIREBIR kopyasidir;
Kotlin tarafi degisirse burasi da degismeli. Nitro/korna/hiss YOK — bu betik
yalnizca motorun kendisini dinletir.
"""

from __future__ import annotations

import math
import os
import re
import struct
import sys
import wave

SAMPLE_RATE = 22050          # EngineVoice.DEFAULT_SAMPLE_RATE
BASE_FREQ = 45.0             # EngineVoice.BASE_FREQ
BASE_CUTOFF = 700.0          # EngineVoice.BASE_CUTOFF
SMOOTHING = 0.0015           # EngineVoice.SMOOTHING
TWO_PI = math.pi * 2.0

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PROFILE_KT = os.path.join(
    ROOT, "source", "app", "src", "main", "java", "com", "miniappfactory",
    "krondrive", "audio", "CarSoundProfile.kt")

# CarSoundProfile'in varsayilan degerleri; profilde yazilmayan alanlar bunlar.
DEFAULTS = {
    "freqMul": 1.00, "harmonic2": 0.50, "harmonic3": 0.25, "harmonic4": 0.00,
    "harmonic5": 0.00, "grit": 0.12, "noiseAmount": 0.02, "lopeDepth": 0.15,
    "lopeRate": 0.50, "gainMul": 1.00, "cutoffMul": 1.00,
}


class Profile:
    def __init__(self, name, fields):
        self.name = name
        for k, v in DEFAULTS.items():
            setattr(self, k, fields.get(k, v))

    @property
    def wave_normalize(self):
        return 1.0 / (1.0 + self.harmonic2 + self.harmonic3 +
                      self.harmonic4 + self.harmonic5 + self.grit)


def parse_profiles(path=PROFILE_KT):
    """CarSoundProfile.kt icindeki tum profilleri okur."""
    src = open(path, encoding="utf-8").read()
    profiles = []

    # 1) `val DEFAULT = CarSoundProfile(id = ...)` — tum alanlar varsayilan.
    if re.search(r"val DEFAULT = CarSoundProfile\(id = [^)]*\)", src):
        profiles.append(Profile("00_SEHIR_varsayilan", {}))

    # 2) `private val X = CarSoundProfile( ... )`
    for m in re.finditer(
            r"private val (\w+) = CarSoundProfile\((.*?)\n    \)", src, re.S):
        name, body = m.group(1), m.group(2)
        fields = {}
        for fm in re.finditer(r"(\w+)\s*=\s*([\d.]+)f", body):
            key, val = fm.group(1), float(fm.group(2))
            if key in DEFAULTS:
                fields[key] = val
        profiles.append(Profile(name, fields))
    return profiles


def speed_curve(t, total):
    """Rolanti -> hizlanma -> seyir. Oyundaki `speed` degeri 0..~8 bandinda."""
    if t < total * 0.18:
        return 0.4
    if t < total * 0.62:
        return 0.4 + 7.2 * ((t - total * 0.18) / (total * 0.44))
    return 7.6


def render(p: Profile, seconds=7.0):
    n = int(SAMPLE_RATE * seconds)
    dt = 1.0 / SAMPLE_RATE
    freq, cutoff, gain = BASE_FREQ, BASE_CUTOFF, 0.0
    phase = lope_phase = lowpass = texture_lp = 0.0
    texture_alpha = 1.0 - math.exp(-TWO_PI * 1400.0 * dt)
    rnd = 0x2545F491  # sabit tohum: ayni parametre ayni dosyayi versin

    out = []
    for i in range(n):
        speed = speed_curve(i * dt, seconds)
        target_freq = BASE_FREQ + speed * 12.0
        target_gain = 0.030 + min(0.045, speed * 0.0055)
        target_cutoff = BASE_CUTOFF + speed * 130.0

        freq += (target_freq * p.freqMul - freq) * SMOOTHING
        gain += (target_gain * p.gainMul - gain) * SMOOTHING
        cutoff += (target_cutoff * p.cutoffMul - cutoff) * SMOOTHING

        phase = (phase + freq * dt) % 1.0
        lope_phase = (lope_phase + freq * p.lopeRate * dt) % 1.0

        a = phase * TWO_PI
        engine = (math.sin(a)
                  + p.harmonic2 * math.sin(2 * a)
                  + p.harmonic3 * math.sin(3 * a)
                  + p.harmonic4 * math.sin(4 * a)
                  + p.harmonic5 * math.sin(5 * a)
                  + p.grit * (phase * 2.0 - 1.0))
        engine *= p.wave_normalize
        engine *= (1.0 - p.lopeDepth) + p.lopeDepth * (
            0.5 + 0.5 * math.sin(lope_phase * TWO_PI))

        alpha = 1.0 - math.exp(-TWO_PI * cutoff * dt)
        lowpass += alpha * (engine - lowpass)
        sample = lowpass * gain

        if p.noiseAmount > 0.0:
            rnd = (1103515245 * rnd + 12345) & 0x7FFFFFFF
            raw = (rnd / 0x3FFFFFFF) - 1.0
            texture_lp += texture_alpha * (raw - texture_lp)
            sample += texture_lp * p.noiseAmount * gain * 4.0

        out.append(sample)
    return out


def to_pcm(samples, headroom=0.89):
    peak = max(1e-6, max(abs(s) for s in samples))
    scale = headroom / peak
    return [int(max(-32767, min(32767, s * scale * 32767))) for s in samples]


def write_wav(path, pcm):
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SAMPLE_RATE)
        w.writeframes(struct.pack("<%dh" % len(pcm), *pcm))


def main():
    out_dir = sys.argv[1] if len(sys.argv) > 1 else "."
    os.makedirs(out_dir, exist_ok=True)
    profiles = parse_profiles()

    # DIKKAT: her dosya kendi icinde normalize edilirse gainMul farki
    # DUYULMAZ — Boga 67'nin "en yuksek" olmasi kaybolur. Bu yuzden olcek
    # TUM profiller icinde ORTAK: en yuksek tepe neyse hepsi ona gore.
    rendered = [(p, render(p)) for p in profiles]
    global_peak = max(max(abs(s) for s in samples) for _, samples in rendered)

    print("%-22s %6s %6s %6s %6s %6s %6s %6s %6s" %
          ("profil", "freq", "grit", "gurul", "lopeD", "lopeR", "gain",
           "cutoff", "goreli"))
    for p, samples in rendered:
        peak = max(abs(s) for s in samples)
        pcm = [int(max(-32767, min(32767, s / global_peak * 0.89 * 32767)))
               for s in samples]
        write_wav(os.path.join(out_dir, "motor_%s.wav" % p.name), pcm)
        print("%-22s %6.2f %6.2f %6.3f %6.2f %6.2f %6.2f %6.2f %6.0f%%" %
              (p.name, p.freqMul, p.grit, p.noiseAmount, p.lopeDepth,
               p.lopeRate, p.gainMul, p.cutoffMul, 100 * peak / global_peak))

    print("\n%d profil, cikis: %s" % (len(profiles), out_dir))
    print("Not: 'goreli' sutunu oyunda birbirine gore ne kadar yuksek "
          "duyuldugudur; dosyalar ORTAK olcekle yazildi.")


if __name__ == "__main__":
    main()
