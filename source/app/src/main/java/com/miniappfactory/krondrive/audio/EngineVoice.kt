package com.miniappfactory.krondrive.audio

import kotlin.concurrent.Volatile
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/**
 * Motor + nitro + korna + carpisma sentezinin TAMAMI. **Saf Kotlin** — tek bir
 * Android importu yok.
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
 * En kotu durum: en yuksek profil, tam gaz, boost, nitro, korna VE carpisma
 * ayni anda. Bu yuk offline olarak on profilin hepsi icin sentezlendi
 * (2026-08-17); olculen en buyuk tepe **0.556**. [render] yine de her ornegi
 * ±1'e sikistirir. Katmanlarin olculen tek basina tepeleri:
 *
 * | Katman | Tepe |
 * |---|---|
 * | Motor (surekli) | ~0.11 |
 * | Motor gurultu dokusu | ~0.02 |
 * | Nitro fisss + islik | ~0.20 |
 * | Korna | 0.30 |
 * | Carpisma | 0.33–0.37 |
 *
 * ⚠ 2026-08-17'ye kadar burada "toplam tepe ~0.65'te kalir" yaziyordu ve bu
 * **olculmemis bir tahmindi**. Ayni yuk carpisma EKLENMEDEN olculunce tepe
 * 0.352 cikti, yani eski sayi gercek degerin neredeyse iki kati kadar
 * karamsardi. Carpisma seviyesi bu bos payin uzerine kondu: katalogun en
 * yuksek tek atisi olmasi bilincli, cunku olum anini o tasiyor.
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

    /**
     * Ham hiz. 2026-08-17'ye kadar burada hazir hesaplanmis bir
     * `targetCutoff` duruyordu; filtre egimi ve karartma artik PROFILE bagli
     * oldugu icin hesap [render]'a tasindi ve burada ham girdi tutuluyor.
     */
    @Volatile
    private var targetSpeed = 0f

    @Volatile
    private var boostActive = false

    @Volatile
    private var nitroRemaining = 0f

    @Volatile
    private var hornRemaining = 0f

    @Volatile
    private var crashRemaining = 0f

    /** Carpisma yeniden tetiklendiginde fazlar sifirlansin (net atak). */
    @Volatile
    private var crashRestart = false

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

    /**
     * Yumusatilmis karartma miktari (bkz. [CarSoundProfile.lowSpeedDarken]).
     * Diger hedefler gibi ornek basina yaklastirilir; aksi halde hiz
     * degistiginde harmonik agirliklari tampon sinirinda sicrar ve fermuar
     * (zipper) gurultusu duyulur.
     */
    private var darken = 0f
    private var lowpass = 0f
    private var noiseLp = 0f
    private var textureLp = 0f
    private var hissLevel = 0f
    private var noiseState = 0x9E3779B9.toInt()

    // Carpisma — yalnizca render thread'i dokunur.
    private val crashPhases = FloatArray(CRASH_PARTIALS.size)
    private var crashThudPhase = 0f
    private var crashLpHi = 0f
    private var crashLpLo = 0f

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
            crashRemaining = 0f
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
        // Filtre ve karartma hedefleri profile bagli; [render] hesapliyor.
        targetSpeed = speed
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
        crashRemaining = 0f
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

    /**
     * **Carpisma.** Oyuncunun oldugu andaki tek atislik darbe.
     *
     * `docs/REVIEW_GAMEPLAY.md` "olum ani yok" basligi altinda bunu tek
     * basina en buyuk his kaybi olarak isaretlemisti: carpismadan sonraki
     * karede siyah perde iniyor, arada ne ses ne sarsinti oluyordu.
     *
     * Kornadan farkli olarak **bekleme suresi yok**: carpisma oyuncunun
     * bastigi bir tus degil, kosu basina bir kez olan bir olay. Uste uste
     * cagrilirsa zarf sifirdan yeniden baslar (yiginmaz), cunku
     * [crashRestart] fazlari da sifirlar.
     *
     * Ses kapaliyken hicbir sey tetiklenmez.
     *
     * ⚠ Bu API'yi cagirmak **oynanisi hicbir sekilde etkilemez** ve oyun
     * dongusunu bekletmez: yalnizca iki `volatile` alan yazar, ornekleri
     * uretme isi ses thread'inde olur.
     */
    fun playCrash() {
        if (!enabled) return
        crashRemaining = CRASH_DURATION
        crashRestart = true
    }

    /** Test/gozlem icin: carpisma sesi su an caliyor mu. */
    fun isCrashActive(): Boolean = crashRemaining > 0f

    // --- Sentez ------------------------------------------------------------

    /**
     * [count] adet ornek uretip [out] icine yazar. Cikan her deger ±1
     * araligindadir.
     */
    fun render(out: FloatArray, count: Int = out.size) {
        val p = profile

        // --- Hiza bagli parlaklik egimi ---------------------------------
        // Tampon basina BIR KEZ: profil tampon icinde degismez ve hiz 60 Hz'de
        // gunceleniyor, yani 512 orneklik (~23 ms) cozunurluk fazlasiyla
        // yeterli. Buradan cikan iki hedef sonra ornek basina yumusatilir.
        val speed = targetSpeed
        val darkenT = ((DARKEN_PIVOT_SPEED - speed) /
            (DARKEN_PIVOT_SPEED - DARKEN_FLOOR_SPEED)).coerceIn(0f, 1f)
        val targetDarken = p.lowSpeedDarken * darkenT

        // Filtre egimi PIVOT hizinda donuyor: karartma ne olursa olsun
        // [DARKEN_PIVOT_SPEED]'te ve ustunde cutoff eskisiyle birebir ayni
        // kalir, yalnizca altinda kapanir.
        val cutoffSpeedGain = 1f + targetDarken * DARKEN_CUTOFF_GAIN
        // ⚠ coerceAtLeast KOZMETIK DEGIL: negatif cutoff, asagidaki tek
        // kutuplu filtrenin katsayisini (1 - exp(-2π·fc·dt)) negatif yapar ve
        // filtre patlar. Yuksek karartmada dusuk hizda bu deger gercekten
        // sifirin altina inebiliyor.
        val tilted = (BASE_CUTOFF + CUTOFF_PER_SPEED *
            (DARKEN_PIVOT_SPEED + cutoffSpeedGain * (speed - DARKEN_PIVOT_SPEED)))
            .coerceAtLeast(MIN_CUTOFF)
        val targetCutoff =
            (tilted + if (boostActive) CUTOFF_BOOST_BONUS else 0f) * p.cutoffMul

        for (i in 0 until count) {
            freq += (targetFreq * p.freqMul - freq) * SMOOTHING
            gain += (targetGain * p.gainMul - gain) * SMOOTHING
            cutoff += (targetCutoff - cutoff) * SMOOTHING
            darken += (targetDarken - darken) * SMOOTHING

            phase = advance(phase, freq)
            lopePhase = advance(lopePhase, freq * p.lopeRate)

            // Karartma agirliklari. Temel ve [CarSoundProfile.harmonic2]
            // BILEREK dokunulmadan kalir: kalinlik hissini onlar tasiyor ve
            // telefon hoparlorunun basabildigi bant da orasi. Ust harmonikler
            // giderek daha sert kisilir (h, h², h³) — yukari dogru inen bir
            // spektral egim.
            //
            // Hepsi <= 1 oldugu icin ham dalga [CarSoundProfile.waveNormalize]
            // in varsaydigi en kotu durumun ALTINDA kalir; bu alan hicbir
            // degerde kirpmaya yol acamaz.
            val h = 1f - darken
            val w3 = h
            val w4 = h * h
            val w5 = w4 * h

            val angle = phase * twoPi
            // Silindir vurusu: temel + harmonikler + az miktar testere "grit".
            var engine = sin(angle) +
                p.harmonic2 * sin(2f * angle) +
                p.harmonic3 * w3 * sin(3f * angle) +
                p.harmonic4 * w4 * sin(4f * angle) +
                p.harmonic5 * w5 * sin(5f * angle) +
                p.grit * w4 * (phase * 2f - 1f)
            engine *= p.waveNormalize
            // Lope: AYRI bir faz uzerinden, cunku ana fazin katsayili sinusu
            // 0.5 disindaki oranlarda sarma anlarinda siciyor ve tiklama yapiyor.
            engine *= (1f - p.lopeDepth) +
                p.lopeDepth * (0.5f + 0.5f * sin(lopePhase * twoPi))

            val alpha = 1f - exp(-twoPi * cutoff * dt)
            lowpass += alpha * (engine - lowpass)

            var sample = lowpass * gain

            // --- Motor dokusu: dizel tikirtisi / eski motor havasi ----------
            // Carpisma da ham gurultuyu kullanir; unutulursa carpismanin
            // kirilma dokusu sessiz kalir.
            val needsNoise = p.noiseAmount > 0f || boostActive ||
                hissLevel > 0.001f || nitroRemaining > 0f || crashRemaining > 0f
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

            // --- Carpisma ---------------------------------------------------
            if (crashRestart) {
                crashRestart = false
                crashPhases.fill(0f)
                crashThudPhase = 0f
            }
            if (crashRemaining > 0f) {
                val t = 1f - (crashRemaining / CRASH_DURATION)
                val elapsed = CRASH_DURATION - crashRemaining
                // Atak sifirdan acilir (basta "tik" olmaz), birakma sifira
                // iner (arkasinda kuyruk kalmaz).
                val envelope = min(1f, elapsed / CRASH_ATTACK) *
                    min(1f, crashRemaining / CRASH_RELEASE)
                val tone = p.crashTone

                // 1) Metalik darbe: dort ANHARMONIK kismi. Oranlar bilerek
                // tam sayi katlari DEGIL — tam katlar bir nota gibi perdeli
                // duyulur, carpismanin perdesiz olmasi gerekiyor. Ust
                // kismiler daha hizli soner; gercek metalin davranisi bu.
                var metal = 0f
                for (k in CRASH_PARTIALS.indices) {
                    crashPhases[k] = advance(
                        crashPhases[k], CRASH_BASE_HZ * CRASH_PARTIALS[k] * tone
                    )
                    metal += CRASH_PARTIAL_GAINS[k] *
                        exp(-t * CRASH_PARTIAL_DECAY[k]) *
                        sin(crashPhases[k] * twoPi)
                }
                sample += metal * CRASH_PARTIAL_NORMALIZE * CRASH_METAL_LEVEL * envelope

                // 2) Kirilma dokusu: iki tek kutuplu filtrenin farki =
                // bant geciren gurultu. Bant da araca gore kayar.
                val hiA = 1f - exp(-twoPi * CRASH_NOISE_HI_HZ * tone * dt)
                val loA = 1f - exp(-twoPi * CRASH_NOISE_LO_HZ * tone * dt)
                crashLpHi += hiA * (raw - crashLpHi)
                crashLpLo += loA * (raw - crashLpLo)
                val band = crashLpHi - crashLpLo
                // Sert kirilma + daha yavas dagilan parcalar.
                val noiseEnv = 0.62f * exp(-t * 22f) + 0.38f * exp(-t * 5.5f)
                sample += band * CRASH_NOISE_LEVEL * noiseEnv * envelope

                // 3) Govde darbesi: asagi kayan kisa vurus. 150 Hz'den
                // baslar cunku telefon hoparloru bunun altini zaten basmiyor
                // (dosya boyunca tekrarlanan 0.76 tabani gerekcesi).
                val thudHz = CRASH_THUD_HZ * tone *
                    (1f - CRASH_THUD_DROP * min(1f, t * 3.4f))
                crashThudPhase = advance(crashThudPhase, thudHz)
                sample += sin(crashThudPhase * twoPi) *
                    CRASH_THUD_LEVEL * exp(-t * 11f) * envelope

                crashRemaining -= dt
                if (crashRemaining < 0f) crashRemaining = 0f
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

        // --- Hiza bagli parlaklik egimi ---------------------------------
        //
        // Sabitlerin ikisi de OYUNUN GERCEK hiz bandindan alindi, tahminden
        // degil (GameConfig): bolumler `startSpeedKmh = 60` ile, yani
        // speed = 2.0 ile basliyor; tepe hiz taban 2.63 + skor tavani 3.2
        // (+1.12 yukseltme) + boost 1.8 (+0.36 yukseltme) ile ~9.1.
        // Yani oyunda hic gorulmeyen speed = 0 bandina gore ayar yapmak
        // yaniltici olurdu.

        /** Bu hizda ve USTUNDE karartma tam olarak sifir — 2026 oncesi davranis. */
        const val DARKEN_PIVOT_SPEED = 4.8f

        /** Bu hizda karartma tam ([CarSoundProfile.lowSpeedDarken] kadar). */
        const val DARKEN_FLOOR_SPEED = 2.0f

        /**
         * Karartmanin filtre egimine cevrimi. Olculen katkisi KUCUK (motorun
         * spektrumu besinci harmonikte bittigi icin filtrenin uzerinde
         * calisacagi malzeme az) ama yonu dogru ve bedava; ayrintili olcum
         * [CarSoundProfile.lowSpeedDarken] KDoc'unda.
         */
        const val DARKEN_CUTOFF_GAIN = 2.6f

        /** Hiz basina filtre acilmasi ve boost'un sabit ek acikligi. */
        const val CUTOFF_PER_SPEED = 130f
        const val CUTOFF_BOOST_BONUS = 500f

        /** Filtrenin inebilecegi taban — bkz. [render]'daki patlama notu. */
        const val MIN_CUTOFF = 120f

        // --- Carpisma ------------------------------------------------------

        /** Carpisma sesinin toplam suresi. */
        const val CRASH_DURATION = 0.46f

        /** Atak: "aninda" duyulacak kadar kisa, tik yapmayacak kadar uzun. */
        const val CRASH_ATTACK = 0.0016f

        /** Son bu kadar saniye sonumlenerek biter (kuyruk birakmaz). */
        const val CRASH_RELEASE = 0.03f

        /** Metalik darbenin temel frekansi; profil [CarSoundProfile.crashTone] ile kaydirir. */
        const val CRASH_BASE_HZ = 196f

        /**
         * ANHARMONIK kismi oranlari — tam sayi katlari OLMAMASI kasitli.
         * Tam katlar perdeli bir nota gibi duyulur; carpisma perdesiz olmali.
         */
        val CRASH_PARTIALS = floatArrayOf(1.00f, 1.78f, 2.61f, 3.43f)
        val CRASH_PARTIAL_GAINS = floatArrayOf(1.00f, 0.72f, 0.52f, 0.36f)

        /** Ust kismiler daha hizli soner — gercek metalin davranisi. */
        val CRASH_PARTIAL_DECAY = floatArrayOf(15f, 22f, 30f, 38f)

        /** Kismilerin toplami 1'i asmasin. */
        const val CRASH_PARTIAL_NORMALIZE = 1f / 2.60f

        /**
         * Katman seviyeleri. Offline olcumde carpismanin tek basina tepesi
         * profile gore 0.333–0.370 cikiyor — katalogun en yuksek tek atisi
         * (korna 0.30). Olum aninin en one cikan ses olmasi bilincli.
         */
        const val CRASH_METAL_LEVEL = 0.26f
        const val CRASH_NOISE_LEVEL = 0.25f
        const val CRASH_THUD_LEVEL = 0.17f

        /** Govde darbesi: 150 Hz'den %63 asagi kayar. */
        const val CRASH_THUD_HZ = 150f
        const val CRASH_THUD_DROP = 0.63f

        /** Kirilma dokusunun bant geciren sinirlari (profil oteler). */
        const val CRASH_NOISE_HI_HZ = 3600f
        const val CRASH_NOISE_LO_HZ = 380f
    }
}
