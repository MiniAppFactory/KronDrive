"""Geri sayim bipini WAV'a doker ve kirpma butcesini OLCER.

## Neden ayri bir betik

`tools/preview_engine_sound.py` profil basina motor sesi uretir; bip ise
**profile bagli degil** (arayuz sesi, motor sesi degil), o yuzden orada
cikmiyor. Ayrica o betik yalnizca motoru sentezliyor — nitro/korna/carpisma
yok, dolayisiyla kirpma butcesi sorusunu da cevaplayamiyor.

    py tools/preview_countdown_beep.py <cikis_klasoru>

Uretilenler:

  1. `gerisayim_bip.wav`   — oyundaki dizinin birebir zamanlamasi:
                             t=0 "3", t=1 "2", t=2 "1", t=3 "BASLA".
                             Yalniz bip; geri sayim sirasinda motorun hedef
                             genligi zaten 0 (`phase != RUNNING`).
  2. `gerisayim_ve_motor_<profil>.wav`
                           — ayni dizi, ama son bip kosunun basladigi ana
                             denk geliyor ve motor o anda aciliyor. "BASLA
                             bipi motorun altinda kayboluyor mu" sorusu
                             yalnizca burada cevaplanabilir.

Ayrica **kirpma tablosu** basilir: `EngineVoiceTest`'in en kotu durum
senaryosu (tam gaz + boost + nitro + korna + carpisma) bip ILE ve bip
OLMADAN, her profil icin.

## Dogruluk sozlesmesi

Asagidaki sentez `EngineVoice.render`'in BIREBIR kopyasidir — bu sefer
TAMAMI (motor + doku + nitro + korna + carpisma + bip). Kotlin tarafi
degisirse burasi da degismeli.

⚠ `preview_engine_sound.py` doku filtresini 1400 Hz ile kuruyor; Kotlin'de
`TEXTURE_CUTOFF = 520f`. Burada 520 kullanildi.

⚠ Kotlin `Float` (32-bit), Python `float` (64-bit) kullanir. Tepe olcumleri
ucuncu haneden sonra bir tik sapabilir; testteki esikler (0.90) bu sapmanin
cok uzaginda.
"""

from __future__ import annotations

import math
import os
import re
import struct
import sys
import wave

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
KT = os.path.join(ROOT, "source", "app", "src", "main", "java", "com",
                  "miniappfactory", "krondrive", "audio")
PROFILE_KT = os.path.join(KT, "CarSoundProfile.kt")

TWO_PI = math.pi * 2.0
MASK32 = 0xFFFFFFFF

# --- EngineVoice sabitleri ------------------------------------------------
SAMPLE_RATE = 22050
BASE_FREQ = 45.0
BASE_CUTOFF = 700.0
SMOOTHING = 0.0015
NITRO_DURATION = 0.55
TEXTURE_CUTOFF = 520.0
HORN_LEVEL = 0.30
HORN_RELEASE = 0.10
DARKEN_PIVOT_SPEED = 4.8
DARKEN_FLOOR_SPEED = 2.0
DARKEN_CUTOFF_GAIN = 2.6
CUTOFF_PER_SPEED = 130.0
CUTOFF_BOOST_BONUS = 500.0
MIN_CUTOFF = 120.0
CRASH_DURATION = 0.46
CRASH_ATTACK = 0.0016
CRASH_RELEASE = 0.03
CRASH_BASE_HZ = 196.0
CRASH_PARTIALS = (1.00, 1.78, 2.61, 3.43)
CRASH_PARTIAL_GAINS = (1.00, 0.72, 0.52, 0.36)
CRASH_PARTIAL_DECAY = (15.0, 22.0, 30.0, 38.0)
CRASH_PARTIAL_NORMALIZE = 1.0 / 2.60
CRASH_METAL_LEVEL = 0.26
CRASH_NOISE_LEVEL = 0.25
CRASH_THUD_LEVEL = 0.17
CRASH_THUD_HZ = 150.0
CRASH_THUD_DROP = 0.63
CRASH_NOISE_HI_HZ = 3600.0
CRASH_NOISE_LO_HZ = 380.0

# --- Geri sayim bipi ------------------------------------------------------
COUNTDOWN_BEEP_HZ = 880.0
COUNTDOWN_GO_HZ = 1760.0
COUNTDOWN_BEEP_SECONDS = 0.12
COUNTDOWN_GO_SECONDS = 0.50
COUNTDOWN_ATTACK = 0.006
COUNTDOWN_RELEASE = 0.045
COUNTDOWN_DECAY = 1.2
COUNTDOWN_BEEP_LEVEL = 0.20
COUNTDOWN_GO_LEVEL = 0.26
COUNTDOWN_HARMONIC = 0.30
COUNTDOWN_NORMALIZE = 1.0 / (1.0 + COUNTDOWN_HARMONIC)

# GameConfig.COUNTDOWN_SECONDS — dizinin uzunlugu.
COUNTDOWN_SECONDS = 3


# --------------------------------------------------------------------------
# Profiller: TEK KAYNAK CarSoundProfile.kt (elle kopyalanmis tablo YOK).
# --------------------------------------------------------------------------

DEFAULTS = {
    "freqMul": 1.00, "harmonic2": 0.50, "harmonic3": 0.25, "harmonic4": 0.00,
    "harmonic5": 0.00, "grit": 0.12, "noiseAmount": 0.02, "lopeDepth": 0.15,
    "lopeRate": 0.50, "gainMul": 1.00, "cutoffMul": 1.00,
    "lowSpeedDarken": 0.00, "nitroTone": 1.00, "hornBaseHz": 420.0,
    "hornInterval": 1.25, "hornBuzz": 0.45, "hornSeconds": 0.50,
    "hornAttack": 0.018,
}


class Profile:
    def __init__(self, name, fields):
        self.name = name
        for k, v in DEFAULTS.items():
            setattr(self, k, fields.get(k, v))
        self.waveNormalize = 1.0 / (1.0 + self.harmonic2 + self.harmonic3 +
                                    self.harmonic4 + self.harmonic5 + self.grit)
        self.hornNormalize = 1.0 / (2.0 * (1.0 + self.hornBuzz * 1.08))
        self.crashTone = min(1.35, max(0.70,
                             (0.55 + 0.45 * self.freqMul) *
                             (0.80 + 0.20 * self.cutoffMul)))


def parse_profiles(path=PROFILE_KT):
    src = open(path, encoding="utf-8").read()
    profiles = []
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


# --------------------------------------------------------------------------
# EngineVoice — birebir port.
# --------------------------------------------------------------------------

class Voice:
    def __init__(self, profile):
        self.p = profile
        self.dt = 1.0 / SAMPLE_RATE
        self.textureAlpha = 1.0 - math.exp(-TWO_PI * TEXTURE_CUTOFF * self.dt)

        self.targetFreq = BASE_FREQ
        self.targetGain = 0.0
        self.targetSpeed = 0.0
        self.boostActive = False
        self.nitroRemaining = 0.0
        self.hornRemaining = 0.0
        self.crashRemaining = 0.0
        self.beepRemaining = 0.0
        self.beepFinal = False

        self.phase = self.lopePhase = self.whistlePhase = 0.0
        self.hornPhaseA = self.hornPhaseB = 0.0
        self.beepPhase = 0.0
        self.freq, self.gain, self.cutoff = BASE_FREQ, 0.0, BASE_CUTOFF
        self.darken = self.lowpass = self.noiseLp = 0.0
        self.textureLp = self.hissLevel = 0.0
        self.crashPhases = [0.0] * len(CRASH_PARTIALS)
        self.crashThudPhase = 0.0
        self.crashLpHi = self.crashLpLo = 0.0
        self.noiseState = 0x9E3779B9

    # --- kontrol ---------------------------------------------------------
    def update(self, speed, boosting):
        self.targetFreq = BASE_FREQ + speed * 12.0 + (14.0 if boosting else 0.0)
        self.targetGain = (0.030 + min(0.045, speed * 0.0055) +
                           (0.012 if boosting else 0.0))
        self.targetSpeed = speed
        self.boostActive = boosting

    def play_nitro(self):
        self.nitroRemaining = NITRO_DURATION

    def play_horn(self):
        self.hornRemaining = self.p.hornSeconds
        self.hornPhaseA = self.hornPhaseB = 0.0

    def play_crash(self):
        self.crashRemaining = CRASH_DURATION
        self.crashPhases = [0.0] * len(CRASH_PARTIALS)
        self.crashThudPhase = 0.0

    def play_beep(self, final):
        self.beepFinal = final
        self.beepRemaining = COUNTDOWN_GO_SECONDS if final else COUNTDOWN_BEEP_SECONDS
        self.beepPhase = 0.0

    # --- yardimcilar ------------------------------------------------------
    def _noise(self):
        s = self.noiseState
        s ^= (s << 13) & MASK32
        s ^= (s >> 17)
        s ^= (s << 5) & MASK32
        s &= MASK32
        self.noiseState = s
        signed = s - 0x100000000 if s >= 0x80000000 else s
        return signed / 2147483647.0

    @staticmethod
    def _reed(phase, buzz):
        a = phase * TWO_PI
        return (math.sin(a) + buzz * (0.5 * math.sin(2 * a) +
                0.33 * math.sin(3 * a) + 0.25 * math.sin(4 * a)))

    # --- sentez -----------------------------------------------------------
    def render(self, count):
        p, dt = self.p, self.dt
        out = [0.0] * count

        beepDuration = COUNTDOWN_GO_SECONDS if self.beepFinal else COUNTDOWN_BEEP_SECONDS
        beepHz = COUNTDOWN_GO_HZ if self.beepFinal else COUNTDOWN_BEEP_HZ
        beepLevel = COUNTDOWN_GO_LEVEL if self.beepFinal else COUNTDOWN_BEEP_LEVEL

        speed = self.targetSpeed
        darkenT = min(1.0, max(0.0, (DARKEN_PIVOT_SPEED - speed) /
                               (DARKEN_PIVOT_SPEED - DARKEN_FLOOR_SPEED)))
        targetDarken = p.lowSpeedDarken * darkenT
        cutoffSpeedGain = 1.0 + targetDarken * DARKEN_CUTOFF_GAIN
        tilted = max(MIN_CUTOFF, BASE_CUTOFF + CUTOFF_PER_SPEED *
                     (DARKEN_PIVOT_SPEED + cutoffSpeedGain *
                      (speed - DARKEN_PIVOT_SPEED)))
        targetCutoff = ((tilted + (CUTOFF_BOOST_BONUS if self.boostActive else 0.0))
                        * p.cutoffMul)

        for i in range(count):
            self.freq += (self.targetFreq * p.freqMul - self.freq) * SMOOTHING
            self.gain += (self.targetGain * p.gainMul - self.gain) * SMOOTHING
            self.cutoff += (targetCutoff - self.cutoff) * SMOOTHING
            self.darken += (targetDarken - self.darken) * SMOOTHING

            self.phase = (self.phase + self.freq * dt) % 1.0
            self.lopePhase = (self.lopePhase + self.freq * p.lopeRate * dt) % 1.0

            h = 1.0 - self.darken
            w3, w4 = h, h * h
            w5 = w4 * h
            a = self.phase * TWO_PI
            engine = (math.sin(a)
                      + p.harmonic2 * math.sin(2 * a)
                      + p.harmonic3 * w3 * math.sin(3 * a)
                      + p.harmonic4 * w4 * math.sin(4 * a)
                      + p.harmonic5 * w5 * math.sin(5 * a)
                      + p.grit * w4 * (self.phase * 2.0 - 1.0))
            engine *= p.waveNormalize
            engine *= ((1.0 - p.lopeDepth) + p.lopeDepth *
                       (0.5 + 0.5 * math.sin(self.lopePhase * TWO_PI)))

            alpha = 1.0 - math.exp(-TWO_PI * self.cutoff * dt)
            self.lowpass += alpha * (engine - self.lowpass)
            sample = self.lowpass * self.gain

            needsNoise = (p.noiseAmount > 0.0 or self.boostActive or
                          self.hissLevel > 0.001 or self.nitroRemaining > 0.0 or
                          self.crashRemaining > 0.0)
            raw = 0.0
            if needsNoise:
                raw = self._noise()
                self.textureLp += self.textureAlpha * (raw - self.textureLp)
                if p.noiseAmount > 0.0:
                    sample += self.textureLp * p.noiseAmount * self.gain * 4.0

            hissTarget = 1.0 if self.boostActive else 0.0
            self.hissLevel += (hissTarget - self.hissLevel) * 0.0008

            if self.hissLevel > 0.001 or self.nitroRemaining > 0.0:
                if self.nitroRemaining > 0.0:
                    t = 1.0 - (self.nitroRemaining / NITRO_DURATION)
                    noiseCut = (400.0 + 5200.0 * t) * p.nitroTone
                else:
                    noiseCut = 2600.0 * p.nitroTone
                na = 1.0 - math.exp(-TWO_PI * noiseCut * dt)
                self.noiseLp += na * (raw - self.noiseLp)
                hiss = raw - self.noiseLp
                sample += hiss * 0.022 * self.hissLevel

                if self.nitroRemaining > 0.0:
                    t = 1.0 - (self.nitroRemaining / NITRO_DURATION)
                    envelope = min(1.0, t * 6.0) * (1.0 - t) * (1.0 - t)
                    sample += hiss * 0.16 * envelope
                    self.whistlePhase = (self.whistlePhase +
                                         (700.0 + 1500.0 * t) * p.nitroTone * dt) % 1.0
                    sample += math.sin(self.whistlePhase * TWO_PI) * 0.035 * envelope
                    self.nitroRemaining = max(0.0, self.nitroRemaining - dt)

            if self.hornRemaining > 0.0:
                elapsed = p.hornSeconds - self.hornRemaining
                envelope = (min(1.0, elapsed / p.hornAttack) *
                            min(1.0, self.hornRemaining / HORN_RELEASE))
                bend = 0.94 + 0.06 * min(1.0, elapsed / (p.hornAttack * 3.0))
                self.hornPhaseA = (self.hornPhaseA + p.hornBaseHz * bend * dt) % 1.0
                self.hornPhaseB = (self.hornPhaseB +
                                   p.hornBaseHz * p.hornInterval * bend * dt) % 1.0
                reed = (self._reed(self.hornPhaseA, p.hornBuzz) +
                        self._reed(self.hornPhaseB, p.hornBuzz))
                sample += reed * p.hornNormalize * HORN_LEVEL * envelope
                self.hornRemaining = max(0.0, self.hornRemaining - dt)

            if self.crashRemaining > 0.0:
                t = 1.0 - (self.crashRemaining / CRASH_DURATION)
                elapsed = CRASH_DURATION - self.crashRemaining
                envelope = (min(1.0, elapsed / CRASH_ATTACK) *
                            min(1.0, self.crashRemaining / CRASH_RELEASE))
                tone = p.crashTone

                metal = 0.0
                for k in range(len(CRASH_PARTIALS)):
                    self.crashPhases[k] = (self.crashPhases[k] +
                                           CRASH_BASE_HZ * CRASH_PARTIALS[k] *
                                           tone * dt) % 1.0
                    metal += (CRASH_PARTIAL_GAINS[k] *
                              math.exp(-t * CRASH_PARTIAL_DECAY[k]) *
                              math.sin(self.crashPhases[k] * TWO_PI))
                sample += metal * CRASH_PARTIAL_NORMALIZE * CRASH_METAL_LEVEL * envelope

                hiA = 1.0 - math.exp(-TWO_PI * CRASH_NOISE_HI_HZ * tone * dt)
                loA = 1.0 - math.exp(-TWO_PI * CRASH_NOISE_LO_HZ * tone * dt)
                self.crashLpHi += hiA * (raw - self.crashLpHi)
                self.crashLpLo += loA * (raw - self.crashLpLo)
                band = self.crashLpHi - self.crashLpLo
                noiseEnv = 0.62 * math.exp(-t * 22.0) + 0.38 * math.exp(-t * 5.5)
                sample += band * CRASH_NOISE_LEVEL * noiseEnv * envelope

                thudHz = (CRASH_THUD_HZ * tone *
                          (1.0 - CRASH_THUD_DROP * min(1.0, t * 3.4)))
                self.crashThudPhase = (self.crashThudPhase + thudHz * dt) % 1.0
                sample += (math.sin(self.crashThudPhase * TWO_PI) *
                           CRASH_THUD_LEVEL * math.exp(-t * 11.0) * envelope)
                self.crashRemaining = max(0.0, self.crashRemaining - dt)

            if self.beepRemaining > 0.0:
                elapsed = beepDuration - self.beepRemaining
                t = elapsed / beepDuration
                envelope = (min(1.0, elapsed / COUNTDOWN_ATTACK) *
                            min(1.0, self.beepRemaining / COUNTDOWN_RELEASE) *
                            math.exp(-t * COUNTDOWN_DECAY))
                self.beepPhase = (self.beepPhase + beepHz * dt) % 1.0
                ba = self.beepPhase * TWO_PI
                tonev = math.sin(ba) + COUNTDOWN_HARMONIC * math.sin(2 * ba)
                sample += tonev * COUNTDOWN_NORMALIZE * beepLevel * envelope
                self.beepRemaining = max(0.0, self.beepRemaining - dt)

            out[i] = max(-1.0, min(1.0, sample))
        return out


# --------------------------------------------------------------------------
# WAV
# --------------------------------------------------------------------------

def write_wav(path, samples, headroom=None):
    """headroom=None -> HIC olceklenmez (gercek seviye duyulur)."""
    if headroom is not None:
        peak = max(1e-9, max(abs(s) for s in samples))
        samples = [s * headroom / peak for s in samples]
    pcm = [int(max(-32767, min(32767, s * 32767))) for s in samples]
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SAMPLE_RATE)
        w.writeframes(struct.pack("<%dh" % len(pcm), *pcm))


def beep_sequence(profile, with_engine):
    """Oyundaki dizinin birebir zamanlamasi.

    `GameScreen` bipi RAKAM DEGISTIGINDE calar; rakam
    `ceil(countdownRemaining)` oldugu icin degisimler tam olarak
    t = 0, 1, 2 ve 3 saniyededir. t = 3 kosunun basladigi andir.
    """
    v = Voice(profile)
    out = []

    def run(seconds, speed=None, boosting=False):
        if speed is not None:
            v.update(speed, boosting)
        out.extend(v.render(int(SAMPLE_RATE * seconds)))

    for tick in range(COUNTDOWN_SECONDS, 0, -1):
        v.play_beep(final=False)
        run(1.0)                       # rakam bir saniye ekranda kalir
    v.play_beep(final=True)            # t=3: rakam 0, faz RUNNING
    if with_engine:
        # GameConfig.startSpeedKmh = 60 -> speed 2.0. Motor SMOOTHING ile
        # ~90 ms'de seviyesine cikar, yani bip motorun uzerine biner.
        run(1.6, speed=2.0)
    else:
        run(1.6)
    return out


def worst_case_peak(profile, with_beep):
    """`EngineVoiceTest`'teki en kotu durum senaryosunun tepesi."""
    v = Voice(profile)
    v.update(8.5, True)
    v.render(int(SAMPLE_RATE))                 # gain hedefe otursun
    v.play_nitro()
    v.play_horn()
    v.play_crash()
    if with_beep:
        v.play_beep(final=True)
    return max(abs(s) for s in v.render(int(SAMPLE_RATE)))


def beep_only_peak(final):
    v = Voice(Profile("bos", {}))              # motor sustur: update yok
    v.play_beep(final=final)
    dur = COUNTDOWN_GO_SECONDS if final else COUNTDOWN_BEEP_SECONDS
    return max(abs(s) for s in v.render(int(SAMPLE_RATE * (dur + 0.1))))


def main():
    out_dir = sys.argv[1] if len(sys.argv) > 1 else "."
    os.makedirs(out_dir, exist_ok=True)
    profiles = parse_profiles()
    by_name = {p.name: p for p in profiles}
    default = by_name.get("BEETY", profiles[0])   # CarSoundProfiles.DEFAULT

    # 1) Yalniz bip — geri sayim sirasinda oyunda duyulanin aynisi.
    seq = beep_sequence(default, with_engine=False)
    write_wav(os.path.join(out_dir, "gerisayim_bip.wav"), seq)

    # 2) Son bip + motorun acilisi. Iki uc profil yeter: en yuksek (Boga 67)
    #    ve varsayilan (Beety). "BASLA bipi motorun altinda kayboluyor mu"
    #    sorusu burada duyulur.
    for name in ("BEETY", "MUSCLE_67"):
        if name in by_name:
            write_wav(os.path.join(out_dir, "gerisayim_ve_motor_%s.wav" % name),
                      beep_sequence(by_name[name], with_engine=True))

    print("Bipin TEK BASINA tepesi (motor sustururken):")
    print("  hazirlik (880 Hz, 0.12 sn): %.3f  [nominal %.2f]" %
          (beep_only_peak(False), COUNTDOWN_BEEP_LEVEL))
    print("  BASLA    (1760 Hz, 0.50 sn): %.3f  [nominal %.2f]" %
          (beep_only_peak(True), COUNTDOWN_GO_LEVEL))

    print("\nKIRPMA BUTCESI — EngineVoiceTest'in en kotu durumu")
    print("(tam gaz + boost + nitro + korna + carpisma [+ BASLA bipi])")
    print("%-14s %8s %8s %8s" % ("profil", "bipsiz", "bipli", "artis"))
    worst = 0.0
    for p in profiles:
        a = worst_case_peak(p, with_beep=False)
        b = worst_case_peak(p, with_beep=True)
        worst = max(worst, b)
        print("%-14s %8.3f %8.3f %8.3f" % (p.name, a, b, b - a))
    print("\nEn buyuk tepe: %.3f   (test siniri < 0.90)" % worst)


if __name__ == "__main__":
    main()
