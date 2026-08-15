package com.miniappfactory.krondrive.audio

import kotlin.concurrent.Volatile
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/**
 * Motor + nitro + korna sentezinin TAMAMI. **Saf Kotlin** — tek bir Android
 * importu yok.
 *
 * Bu ayrimin sebebi test edilebilirlik: bu makinede hoparlor ve adb yok, yani
 * sesi *dinleyerek* dogrulamak mumkun degil. Sentez Android'in `AudioTrack`
 * sinifindan ayri durursa, uretilen orneklerin **olculebilir** ozellikleri
 * (kirpma yok, profiller gercekten farkli cikti veriyor, korna bekleme suresi
 * calisiyor) JVM testiyle dogrulanabiliyor. Android tarafi
 * ([EngineSoundManager]) yalnizca bu sinifi bir thread'de dondurup ciktiyi
 * 16-bit PCM'e cevirir.
 *
 * Ses karakteri [CarSoundProfile] ile belirlenir; oyuncunun sectigi gövdeye
 * gore [setProfile] cagrilir.
 *
 * ## Kirpma (clipping) butcesi
 *
 * En kotu durumda (en yuksek profil, tam gaz, nitro ve korna ayni anda)
 * toplam tepe seviyesi ~0.65'te kalir; [render] yine de her ornegi
 * ±1'e sikistirir. Katmanlarin tepe hedefleri:
 *
 * | Katman | Tepe |
 * |---|---|
 * | Motor (surekli) | ~0.11 |
 * | Motor gurultu dokusu | ~0.02 |
 * | Nitro fisss + islik | ~0.20 |
 * | Korna | 0.30 |
 */
class EngineVoice(
    private val sampleRate: Int = DEFAULT_SAMPLE_RATE,
    profile: CarSoundProfile = CarSoundProfiles.DEFAULT,
    /** Korna bekleme suresi icin saat; test sahte saat verebilsin diye disaridan. */
    private val nowNanos: () -> Long = { System.nanoTime() }
) {

    @Volatile
    var profile: CarSoundProfile = profile
        private set

    @Volatile
    private var enabled = true

    @Volatile
    private var targetFreq = BASE_FREQ

    @Volatile
    private var targetGain = 0f

    @Volatile
    private var targetCutoff = BASE_CUTOFF

    @Volatile
    private var boostActive = false

    @Volatile
    private var nitroRemaining = 0f

    @Volatile
    private var hornRemaining = 0f

    /** Korna yeniden tetiklendiginde fazlar sifirlansin (arka arkaya net atak). */
    @Volatile
    private var hornRestart = false

    private var hornEverPlayed = false
    private var lastHornNanos = 0L

    // --- Yalnizca render thread'inin dokundugu durum ------------------------

    private val dt = 1f / sampleRate
    private val twoPi = 2f * PI.toFloat()

    /** Motor dokusu icin sabit kesme frekansli tek kutuplu filtre katsayisi. */
    private val textureAlpha = 1f - exp(-twoPi * TEXTURE_CUTOFF * dt)

    private var phase = 0f
    private var lopePhase = 0f
    private var whistlePhase = 0f
    private var hornPhaseA = 0f
    private var hornPhaseB = 0f
    private var freq = BASE_FREQ
    private var gain = 0f
    private var cutoff = BASE_CUTOFF
    private var lowpass = 0f
    private var noiseLp = 0f
    private var textureLp = 0f
    private var hissLevel = 0f
    private var noiseState = 0x9E3779B9.toInt()

    // --- Kontrol -----------------------------------------------------------

    fun setProfile(shapeId: String?) {
        profile = CarSoundProfiles.forShape(shapeId)
    }

    fun setProfile(value: CarSoundProfile) {
        profile = value
    }

    /**
     * Ses ayari. Kapatmak yalnizca sesi kismaz, TETIKLEYICILERI de sifirlar
     * (skill kurali: "sessize alma gercekten sussun").
     */
    fun setEnabled(value: Boolean) {
        enabled = value
        if (!value) {
            targetGain = 0f
            boostActive = false
            nitroRemaining = 0f
            hornRemaining = 0f
        }
    }

    fun isEnabled(): Boolean = enabled

    /**
     * Her karede motorun hedef degerleri. Frekans araligi bilincli olarak
     * prototipten DUSUK (45–150 Hz): yuksek frekansli dalga telefon
     * hoparlorunde ciyaklama gibi duyuluyordu. Profil bu araligi olcekler.
     */
    fun update(speed: Float, boosting: Boolean) {
        if (!enabled) return
        targetFreq = BASE_FREQ + speed * 12f + if (boosting) 14f else 0f
        targetGain = 0.030f + min(0.045f, speed * 0.0055f) + if (boosting) 0.012f else 0f
        targetCutoff = BASE_CUTOFF + speed * 130f + if (boosting) 500f else 0f
        boostActive = boosting
    }

    /** Kosu durunca motor sesi bosa duser. */
    fun idle() {
        targetGain = 0f
        boostActive = false
    }

    /**
     * Ses tamamen durdurulurken (arka plana alma, ekrandan cikma) tetiklenmis
     * efektleri de temizler; aksi halde geri donuldugunde yarim kalmis bir
     * nitro ya da korna kaldigi yerden patliyor.
     */
    fun reset() {
        idle()
        nitroRemaining = 0f
        hornRemaining = 0f
    }

    /** Boost'a basildigi an calan nitro efekti. */
    fun playNitro() {
        if (!enabled) return
        nitroRemaining = NITRO_DURATION
    }

    /**
     * Korna. Oynanisa etkisi YOKTUR; tamamen eglence.
     *
     * @return ses gercekten tetiklendiyse `true`; ses kapaliysa ya da bekleme
     *   suresi ([HORN_COOLDOWN_NANOS]) dolmadiysa `false`. Bekleme suresi
     *   olmadan uste uste basilinca zarflar ust uste biniyor ve ses yigiliyordu.
     */
    fun playHorn(): Boolean {
        if (!enabled) return false
        val now = nowNanos()
        if (hornEverPlayed && now - lastHornNanos < HORN_COOLDOWN_NANOS) return false
        hornEverPlayed = true
        lastHornNanos = now
        hornRemaining = profile.hornSeconds
        hornRestart = true
        return true
    }

    /** Test/gozlem icin: korna su an caliyor mu. */
    fun isHornActive(): Boolean = hornRemaining > 0f

    // --- Sentez ------------------------------------------------------------

    /**
     * [count] adet ornek uretip [out] icine yazar. Cikan her deger ±1
     * araligindadir.
     */
    fun render(out: FloatArray, count: Int = out.size) {
        val p = profile
        for (i in 0 until count) {
            freq += (targetFreq * p.freqMul - freq) * SMOOTHING
            gain += (targetGain * p.gainMul - gain) * SMOOTHING
            cutoff += (targetCutoff * p.cutoffMul - cutoff) * SMOOTHING

            phase = advance(phase, freq)
            lopePhase = advance(lopePhase, freq * p.lopeRate)

            val angle = phase * twoPi
            // Silindir vurusu: temel + harmonikler + az miktar testere "grit".
            var engine = sin(angle) +
                p.harmonic2 * sin(2f * angle) +
                p.harmonic3 * sin(3f * angle) +
                p.harmonic4 * sin(4f * angle) +
                p.harmonic5 * sin(5f * angle) +
                p.grit * (phase * 2f - 1f)
            engine *= p.waveNormalize
            // Lope: AYRI bir faz uzerinden, cunku ana fazin katsayili sinusu
            // 0.5 disindaki oranlarda sarma anlarinda siciyor ve tiklama yapiyor.
            engine *= (1f - p.lopeDepth) +
                p.lopeDepth * (0.5f + 0.5f * sin(lopePhase * twoPi))

            val alpha = 1f - exp(-twoPi * cutoff * dt)
            lowpass += alpha * (engine - lowpass)

            var sample = lowpass * gain

            // --- Motor dokusu: dizel tikirtisi / eski motor havasi ----------
            val needsNoise =
                p.noiseAmount > 0f || boostActive || hissLevel > 0.001f || nitroRemaining > 0f
            var raw = 0f
            if (needsNoise) {
                raw = noise()
                textureLp += textureAlpha * (raw - textureLp)
                if (p.noiseAmount > 0f) {
                    sample += textureLp * p.noiseAmount * gain * 4f
                }
            }

            // --- Nitro ------------------------------------------------------
            val hissTarget = if (boostActive) 1f else 0f
            hissLevel += (hissTarget - hissLevel) * 0.0008f

            if (hissLevel > 0.001f || nitroRemaining > 0f) {
                // Gurultuyu tek kutuplu filtreden gecirip yuksek bileseni
                // ayirmak, duz beyaz gurultudense "hava kacisi" gibi duyulur.
                val noiseCut = if (nitroRemaining > 0f) {
                    val t = 1f - (nitroRemaining / NITRO_DURATION)
                    (400f + 5200f * t) * p.nitroTone
                } else {
                    2600f * p.nitroTone
                }
                val na = 1f - exp(-twoPi * noiseCut * dt)
                noiseLp += na * (raw - noiseLp)
                val hiss = raw - noiseLp

                sample += hiss * 0.022f * hissLevel

                if (nitroRemaining > 0f) {
                    val t = 1f - (nitroRemaining / NITRO_DURATION)
                    // Hizli acilip yavas sonen zarf.
                    val envelope = (t * 6f).coerceAtMost(1f) * (1f - t) * (1f - t)
                    sample += hiss * 0.16f * envelope

                    // Yukari suzulen ince islik — profile gore renklenir.
                    whistlePhase = advance(whistlePhase, (700f + 1500f * t) * p.nitroTone)
                    sample += sin(whistlePhase * twoPi) * 0.035f * envelope

                    nitroRemaining -= dt
                    if (nitroRemaining < 0f) nitroRemaining = 0f
                }
            }

            // --- Korna ------------------------------------------------------
            if (hornRestart) {
                hornRestart = false
                hornPhaseA = 0f
                hornPhaseB = 0f
            }
            if (hornRemaining > 0f) {
                val elapsed = p.hornSeconds - hornRemaining
                // Atak ve birakma zarfi: ikisi de sifira inip cikar, bu yuzden
                // korna basinda/sonunda "tik" olmaz.
                val envelope = min(1f, elapsed / p.hornAttack) *
                    min(1f, hornRemaining / HORN_RELEASE)
                // Hava basincinin kurulmasi: ilk anlarda ton hafifce yukselir.
                val bend = 0.94f + 0.06f * min(1f, elapsed / (p.hornAttack * 3f))

                hornPhaseA = advance(hornPhaseA, p.hornBaseHz * bend)
                hornPhaseB = advance(hornPhaseB, p.hornBaseHz * p.hornInterval * bend)
                val reed = reed(hornPhaseA, p.hornBuzz) + reed(hornPhaseB, p.hornBuzz)
                sample += reed * p.hornNormalize * HORN_LEVEL * envelope

                hornRemaining -= dt
                if (hornRemaining < 0f) hornRemaining = 0f
            }

            out[i] = sample.coerceIn(-1f, 1f)
        }
    }

    private fun advance(current: Float, hz: Float): Float {
        var next = current + hz * dt
        if (next >= 1f) next -= next.toInt().toFloat()
        return next
    }

    /** Korna borusu: temel + sabit oranli tizlestirme harmonikleri. */
    private fun reed(phase: Float, buzz: Float): Float {
        val a = phase * twoPi
        return sin(a) + buzz * (0.5f * sin(2f * a) + 0.33f * sin(3f * a) + 0.25f * sin(4f * a))
    }

    /** Xorshift — java.util.Random'dan cok daha ucuz, bu is icin fazlasiyla yeterli. */
    private fun noise(): Float {
        noiseState = noiseState xor (noiseState shl 13)
        noiseState = noiseState xor (noiseState ushr 17)
        noiseState = noiseState xor (noiseState shl 5)
        return noiseState / Int.MAX_VALUE.toFloat()
    }

    companion object {
        const val DEFAULT_SAMPLE_RATE = 22050

        /** Temel motor frekansi (hiz 0'da) ve temel filtre acikligi. */
        const val BASE_FREQ = 45f
        const val BASE_CUTOFF = 700f

        /** Hedeflere yaklasma hizi (ornek basina) — parametre atlarken tik olmaz. */
        const val SMOOTHING = 0.0015f

        /** Nitro efektinin toplam suresi. */
        const val NITRO_DURATION = 0.55f

        /** Motor dokusu gurultusunun kesme frekansi (sabit). */
        const val TEXTURE_CUTOFF = 520f

        /** Korna tepe seviyesi (~-10 dBFS; tek atis SFX hedefi). */
        const val HORN_LEVEL = 0.30f

        /** Kornanin son bu kadar saniyesi sonumlenerek biter. */
        const val HORN_RELEASE = 0.10f

        /** Spam korumasi: iki korna arasindaki en kisa sure (0.4 sn). */
        const val HORN_COOLDOWN_NANOS = 400_000_000L
    }
}
