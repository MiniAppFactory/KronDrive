package com.miniappfactory.krondrive.audio

import com.miniappfactory.krondrive.game.CarCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Sentezin saf Kotlin kismi. Ses KULAKLA dogrulanamaz (bu makinede hoparlor
 * ve adb yok), o yuzden burada uretilen dalganin OLCULEBILIR ozellikleri
 * dogrulanir: kirpma yok, profiller gercekten farkli cikti veriyor, korna
 * bekleme suresi calisiyor ve ses kapaliyken hicbir sey tetiklenmiyor.
 */
class EngineVoiceTest {

    private val sampleRate = EngineVoice.DEFAULT_SAMPLE_RATE

    /** [seconds] kadar ornek uretir ve gorulen en buyuk mutlak degeri doner. */
    private fun EngineVoice.renderPeak(seconds: Float): Float {
        val buffer = FloatArray(512)
        var peak = 0f
        repeat((sampleRate * seconds / buffer.size).toInt().coerceAtLeast(1)) {
            render(buffer)
            buffer.forEach { if (abs(it) > peak) peak = abs(it) }
        }
        return peak
    }

    @Test
    fun `her profil en agir yukte bile kirpmaz`() {
        CarSoundProfiles.all.forEach { profile ->
            val voice = EngineVoice(sampleRate, profile)
            // Tam gaz + boost: gain hedefe yaklassin diye once 1 sn surulur.
            voice.update(speed = 8.5f, boosting = true)
            voice.renderPeak(1f)
            // Sonra en kotu durum: motor + surekli tislama + nitro + korna +
            // CARPISMA + geri sayimin son bipi, hepsi AYNI ANDA. (Oyunda korna
            // ile carpismanin ust uste binmesi mumkun: oyuncu kornaya basip
            // hemen carpabilir. Bip de girebilir: "BASLA" bipi kosunun ilk
            // yarim saniyesinde caliyor ve oyuncu o sirada boost'a basip
            // carpabilir.)
            voice.playNitro()
            voice.playHorn()
            voice.playCrash()
            voice.playCountdownBeep(final = true)
            val peak = voice.renderPeak(1f)

            assertTrue("${profile.id}: sessiz kaldi", peak > 0.01f)
            assertTrue("${profile.id}: kirpma sinirinda ($peak)", peak <= 1f)
            // Kirpma sinirina degmemesi yetmez, PAY kalmali: baska bir katman
            // eklendiginde ilk kirpan bu olmasin.
            assertTrue("${profile.id}: bosluk kalmadi ($peak)", peak < 0.9f)
        }
    }

    @Test
    fun `farkli profiller farkli dalga uretir`() {
        fun sample(profile: CarSoundProfile): FloatArray {
            val voice = EngineVoice(sampleRate, profile)
            voice.update(speed = 6f, boosting = false)
            // Yumusatma hedefe otursun.
            voice.renderPeak(1f)
            return FloatArray(1024).also { voice.render(it) }
        }

        val profiles = CarSoundProfiles.all
        for (i in profiles.indices) {
            for (j in i + 1 until profiles.size) {
                val a = sample(profiles[i])
                val b = sample(profiles[j])
                val fark = a.indices.maxOf { abs(a[it] - b[it]) }
                assertTrue(
                    "${profiles[i].id} ve ${profiles[j].id} ayni sesi veriyor (fark $fark)",
                    fark > 1e-4f
                )
            }
        }
    }

    @Test
    fun `profil govde id'sinden secilir ve sesi degistirir`() {
        val voice = EngineVoice(sampleRate)
        // Varsayilan artik Beety (baslangic araci); Sehir de ayni profili
        // paylasiyor, bkz. CarSoundProfiles.ALIASES.
        assertEquals(CarCatalog.SHAPE_BEETY, voice.profile.id)

        voice.setProfile(CarCatalog.SHAPE_MUSCLE_67)
        assertEquals(CarCatalog.SHAPE_MUSCLE_67, voice.profile.id)

        voice.setProfile("boyle-bir-govde-yok")
        assertEquals(
            "bilinmeyen id varsayilana dusmeli",
            CarSoundProfiles.DEFAULT.id,
            voice.profile.id
        )
    }

    @Test
    fun `korna bekleme suresi ust uste basmayi engeller`() {
        var now = 0L
        val voice = EngineVoice(sampleRate, CarSoundProfiles.DEFAULT, nowNanos = { now })

        assertTrue("ilk korna calmali", voice.playHorn())
        assertTrue(voice.isHornActive())

        assertFalse("hemen ardindan gelen basis reddedilmeli", voice.playHorn())
        now += EngineVoice.HORN_COOLDOWN_NANOS - 1
        assertFalse("bekleme suresi dolmadan calmamali", voice.playHorn())

        now += 1
        assertTrue("bekleme suresi dolunca yeniden calmali", voice.playHorn())
    }

    @Test
    fun `ses kapaliyken korna ve nitro tetiklenmez`() {
        val voice = EngineVoice(sampleRate)
        voice.setEnabled(false)

        assertFalse("ses kapaliyken korna calmamali", voice.playHorn())
        assertFalse(voice.isHornActive())

        voice.update(speed = 8.5f, boosting = true)
        voice.playNitro()
        val peak = voice.renderPeak(0.5f)
        assertEquals("ses kapaliyken cikis tamamen sessiz olmali", 0f, peak, 1e-6f)
    }

    @Test
    fun `korna zarfi sifirdan baslar ve sifira doner`() {
        // Motor susturulmus durumda (gain 0) — olculen tek sey korna.
        val voice = EngineVoice(sampleRate, CarSoundProfiles.forShape(CarCatalog.SHAPE_MUSCLE_67))
        voice.playHorn()

        val ilk = FloatArray(8)
        voice.render(ilk)
        assertEquals("korna basinda tik olmamali", 0f, ilk[0], 1e-6f)
        assertTrue("atak cok hizli acilmamali", abs(ilk[7]) < 0.05f)

        val orta = voice.renderPeak(0.2f)
        assertTrue("korna duyulmuyor ($orta)", orta > 0.05f)
        assertTrue("korna hedef seviyeyi asiyor ($orta)", orta <= EngineVoice.HORN_LEVEL + 1e-3f)

        // Sure dolunca kendiliginden susar ve arkasinda kuyruk birakmaz.
        voice.renderPeak(1f)
        assertFalse("korna suresi dolmus olmali", voice.isHornActive())
        assertEquals("korna bittikten sonra sessizlik", 0f, voice.renderPeak(0.1f), 1e-6f)
    }

    // ------------------------------------------------------------------
    // Hiza bagli parlaklik egimi (CarSoundProfile.lowSpeedDarken)
    // ------------------------------------------------------------------

    /**
     * Kaba parlaklik olcusu: ortalama mutlak fark / ortalama mutlak deger.
     *
     * Ardisik ornekler arasindaki fark bir yuksek geciren gibi davranir —
     * yuksek frekans ne kadar coksa deger o kadar buyuk. Burada FFT
     * kullanilmamasinin sebebi testin bagimsiz kalmasi; olcum tarafinda
     * (offline) ayni sonuc spektral agirlik merkeziyle de dogrulandi.
     *
     * ⚠ SIFIR GECIS SAYISI (zero-crossing) bu is icin DENENDI ve olculerek
     * ELENDI: dusuk hizda dalga temel frekansin hakimiyetinde oldugu icin
     * karartilmis ve karartilmamis sesler 133'e 131 gibi ayirt edilemez
     * degerler veriyor. Carpisma sesi icin ise iyi calisiyor (asagida).
     */
    private fun brightness(x: FloatArray): Float {
        var diff = 0f
        var level = 0f
        for (i in 1 until x.size) {
            diff += abs(x[i] - x[i - 1])
            level += abs(x[i])
        }
        return if (level <= 0f) 0f else diff / level
    }

    /** [seconds] on-surusten sonra tek tampon yakalar (yumusatma otursun). */
    private fun capture(profile: CarSoundProfile, speed: Float, seconds: Float = 1f): FloatArray {
        val voice = EngineVoice(sampleRate, profile)
        voice.update(speed, boosting = false)
        voice.renderPeak(seconds)
        return FloatArray(4096).also { voice.render(it) }
    }

    /**
     * Karartma ekseninin en onemli garantisi: pivot hizinda ve USTUNDE
     * hicbir sey yapmaz. Bu sadece "kucuk fark" degil, **birebir ayni
     * ornekler** demek — yani bu alanin eklenmesi hicbir aracin yuksek hiz
     * kimligini degistirmedi ve degistiremez.
     */
    @Test
    fun `karartma pivot hizinin ustunde hicbir sey yapmaz`() {
        CarSoundProfiles.all.filter { it.lowSpeedDarken > 0f }.forEach { profile ->
            val kapali = profile.copy(lowSpeedDarken = 0f)
            val hiz = EngineVoice.DARKEN_PIVOT_SPEED + 1.2f
            val a = capture(profile, hiz)
            val b = capture(kapali, hiz)
            val fark = a.indices.maxOf { abs(a[it] - b[it]) }
            assertEquals(
                "${profile.id}: pivot ustunde karartma etki etmemeli (fark $fark)",
                0f, fark, 0f
            )
        }
    }

    /**
     * Sahibin istegi: *"baslangic hizlarinda tok/bogus/basli"*. Bolum
     * baslangic hizi [com.miniappfactory.krondrive.game.GameConfig] icinde
     * `startSpeedKmh = 60` -> speed 2.0.
     */
    @Test
    fun `karartma dusuk hizda tiniyi koyulastirir`() {
        val karartanlar = CarSoundProfiles.all.filter { it.lowSpeedDarken > 0f }
        assertTrue("karartma kullanan hicbir profil yok", karartanlar.isNotEmpty())

        karartanlar.forEach { profile ->
            val kapali = profile.copy(lowSpeedDarken = 0f)
            val hiz = EngineVoice.DARKEN_FLOOR_SPEED
            val koyu = brightness(capture(profile, hiz))
            val duz = brightness(capture(kapali, hiz))
            // Offline olculen oranlar: F1 0.58, Yaris Sedan 0.78.
            assertTrue(
                "${profile.id}: dusuk hizda belirgin koyulasmali ($koyu vs $duz)",
                koyu < 0.85f * duz
            )
        }
    }

    /**
     * Karartma yalnizca CIKARAN yonde calisir; hicbir profilde tepe seviyeyi
     * yukseltemez. Normalizasyon sozlesmesi ([CarSoundProfile.waveNormalize])
     * statik harmoniklere gore hesaplandigi icin bu sart.
     */
    @Test
    fun `karartma tepe seviyeyi yukseltmez`() {
        CarSoundProfiles.all.forEach { profile ->
            val kapali = profile.copy(lowSpeedDarken = 0f)
            val hiz = EngineVoice.DARKEN_FLOOR_SPEED
            val koyuTepe = capture(profile, hiz).maxOf { abs(it) }
            val duzTepe = capture(kapali, hiz).maxOf { abs(it) }
            assertTrue(
                "${profile.id}: karartma tepeyi yukseltti ($koyuTepe > $duzTepe)",
                koyuTepe <= duzTepe + 1e-6f
            )
        }
    }

    // ------------------------------------------------------------------
    // Carpisma
    // ------------------------------------------------------------------

    /** Motor sustururken (gain 0) yalnizca carpismayi uretir. */
    private fun crashOnly(profile: CarSoundProfile, seconds: Float = 0.5f): FloatArray {
        val voice = EngineVoice(sampleRate, profile)
        voice.playCrash()
        return FloatArray((sampleRate * seconds).toInt()).also { voice.render(it) }
    }

    @Test
    fun `carpisma zarfi sifirdan baslar ve sifira doner`() {
        val voice = EngineVoice(sampleRate, CarSoundProfiles.forShape(CarCatalog.SHAPE_TIR))
        voice.playCrash()
        assertTrue(voice.isCrashActive())

        val ilk = FloatArray(8)
        voice.render(ilk)
        assertEquals("carpisma basinda tik olmamali", 0f, ilk[0], 1e-6f)

        // Sure dolunca kendiliginden susar ve kuyruk birakmaz.
        voice.renderPeak(EngineVoice.CRASH_DURATION + 0.1f)
        assertFalse("carpisma suresi dolmus olmali", voice.isCrashActive())
        assertEquals("carpisma bittikten sonra sessizlik", 0f, voice.renderPeak(0.1f), 1e-6f)
    }

    @Test
    fun `carpisma duyulur ama korna kadar bile kirpmaya yaklasmaz`() {
        CarSoundProfiles.all.forEach { profile ->
            val tepe = crashOnly(profile).maxOf { abs(it) }
            // Offline olculen aralik: 0.333–0.370 (korna 0.30).
            assertTrue("${profile.id}: carpisma duyulmuyor ($tepe)", tepe > 0.25f)
            assertTrue("${profile.id}: carpisma fazla yuksek ($tepe)", tepe < 0.45f)
        }
    }

    /**
     * Sahibin tarifi: *"tirin carpmasi motosikletinkinden tok olmali"*.
     *
     * Burada sifir gecis sayisi (zero-crossing) parlaklik vekili olarak
     * KULLANILABILIYOR, cunku carpisma genis bantli gurultu icerir — motor
     * sesinin aksine temel frekansin hakimiyetinde degil. Offline olculen
     * degerler: Tir 3434, Motosiklet 3916 (spektral agirlik merkeziyle de
     * dogrulandi: 1894 Hz / 2643 Hz).
     */
    @Test
    fun `carpisma araca gore renklenir`() {
        fun sifirGecis(x: FloatArray): Int {
            var n = 0
            for (i in 1 until x.size) if ((x[i] < 0f) != (x[i - 1] < 0f)) n++
            return n
        }

        val tir = sifirGecis(crashOnly(CarSoundProfiles.forShape(CarCatalog.SHAPE_TIR)))
        val moto = sifirGecis(crashOnly(CarSoundProfiles.forShape(CarCatalog.SHAPE_MOTOSIKLET)))
        assertTrue("tirin carpmasi motosikletinkinden tok olmali ($tir vs $moto)", tir < moto)
    }

    @Test
    fun `ses kapaliyken carpisma tetiklenmez`() {
        val voice = EngineVoice(sampleRate)
        voice.setEnabled(false)
        voice.playCrash()
        assertFalse("ses kapaliyken carpisma calmamali", voice.isCrashActive())
        assertEquals("cikis tamamen sessiz olmali", 0f, voice.renderPeak(0.5f), 1e-6f)
    }

    /** [EngineVoice.reset] yarim kalmis carpismayi da temizler. */
    @Test
    fun `reset yarim kalmis carpismayi temizler`() {
        val voice = EngineVoice(sampleRate)
        voice.playCrash()
        voice.renderPeak(0.05f)
        assertTrue(voice.isCrashActive())

        voice.reset()
        assertFalse("reset carpismayi durdurmali", voice.isCrashActive())
    }

    // ------------------------------------------------------------------
    // Geri sayim bipi
    // ------------------------------------------------------------------

    /** Motor sustururken (gain 0) yalnizca bipi uretir. */
    private fun beepOnly(
        final: Boolean,
        profile: CarSoundProfile = CarSoundProfiles.DEFAULT,
        seconds: Float = 0.7f
    ): FloatArray {
        val voice = EngineVoice(sampleRate, profile)
        voice.playCountdownBeep(final)
        return FloatArray((sampleRate * seconds).toInt()).also { voice.render(it) }
    }

    @Test
    fun `bip zarfi sifirdan baslar ve sifira doner`() {
        val voice = EngineVoice(sampleRate)
        voice.playCountdownBeep(final = false)
        assertTrue(voice.isCountdownBeepActive())

        val ilk = FloatArray(8)
        voice.render(ilk)
        assertEquals("bip basinda tik olmamali", 0f, ilk[0], 1e-6f)
        // 8 ornek = 0.36 ms; atak 6 ms, yani zarf henuz acilmis olamaz.
        assertTrue("atak cok hizli acilmamali", abs(ilk[7]) < 0.05f)

        // Sure dolunca kendiliginden susar ve kuyruk birakmaz.
        voice.renderPeak(EngineVoice.COUNTDOWN_BEEP_SECONDS + 0.1f)
        assertFalse("bip suresi dolmus olmali", voice.isCountdownBeepActive())
        assertEquals("bip bittikten sonra sessizlik", 0f, voice.renderPeak(0.1f), 1e-6f)
    }

    /**
     * Olculen tepeler (`tools/preview_countdown_beep.py`): hazirlik 0.163,
     * "BASLA" 0.224. Ikisi de korna (0.30) ve carpismanin (0.33–0.37)
     * ALTINDA — geri sayim sessizligin uzerine caldigi icin bilincli.
     */
    @Test
    fun `bip duyulur ama kornadan yuksek degildir`() {
        val hazirlik = beepOnly(final = false).maxOf { abs(it) }
        val basla = beepOnly(final = true).maxOf { abs(it) }

        assertTrue("hazirlik bipi duyulmuyor ($hazirlik)", hazirlik > 0.12f)
        assertTrue("BASLA bipi duyulmuyor ($basla)", basla > 0.18f)
        assertTrue("BASLA bipi vurguyu tasimali ($basla vs $hazirlik)", basla > hazirlik)
        assertTrue(
            "bip kornadan yuksek olmamali ($basla)",
            basla <= EngineVoice.HORN_LEVEL
        )
    }

    /**
     * Son bip bir OKTAV tiz ve belirgin olarak daha uzun. Oktav olmasi,
     * kulagin onu "baska bir ses" degil "ayni sesin tizi" olarak isitmesini
     * sagliyor; cozulme hissini tasiyan sey bu.
     *
     * Perde olcusu sifir gecis sayisi: `sin(a) + 0.30·sin(2a)` =
     * `sin(a)·(1 + 0.6·cos(a))` ve ikinci carpan hicbir zaman isaret
     * degistirmedigi icin (0.6 < 1) dalga tam olarak periyot basina iki kez
     * sifiri gecer. Yani sayim dogrudan frekansi olcer.
     */
    @Test
    fun `BASLA bipi bir oktav tiz ve daha uzun`() {
        fun sifirGecis(x: FloatArray): Int {
            var n = 0
            for (i in 1 until x.size) if ((x[i] < 0f) != (x[i - 1] < 0f)) n++
            return n
        }

        // Ikisinin de kesin caldigi ORTAK pencere: 0.10 sn (kisa bip 0.12 sn).
        val pencere = (sampleRate * 0.10f).toInt()
        val hazirlik = sifirGecis(beepOnly(final = false).copyOf(pencere))
        val basla = sifirGecis(beepOnly(final = true).copyOf(pencere))

        assertTrue(
            "son bip bir oktav tiz olmali ($hazirlik -> $basla)",
            basla > 1.9f * hazirlik && basla < 2.1f * hazirlik
        )

        // Sure: kisa bip biterken uzun bip hala caliyor olmali.
        val voice = EngineVoice(sampleRate)
        voice.playCountdownBeep(final = true)
        voice.renderPeak(EngineVoice.COUNTDOWN_BEEP_SECONDS + 0.05f)
        assertTrue("BASLA bipi kisa bipten uzun olmali", voice.isCountdownBeepActive())
    }

    /**
     * **Bip bir arayuz sesi, motor sesi degil.** Motor, korna, nitro ve
     * carpisma araca gore renklenir; bip bilerek RENKLENMEZ — geri sayim
     * isareti her araçta ayni duyulmali, yoksa oyuncu her yeni araci
     * aldiginda isareti yeniden ogrenmek zorunda kalir.
     *
     * Bu yuzden olculen sey "benzer" degil **birebir ayni ornekler**.
     */
    @Test
    fun `bip araca gore degismez`() {
        val referans = beepOnly(final = true, profile = CarSoundProfiles.DEFAULT)
        CarSoundProfiles.all.forEach { profile ->
            val x = beepOnly(final = true, profile = profile)
            val fark = x.indices.maxOf { abs(x[it] - referans[it]) }
            assertEquals("${profile.id}: bip araca gore degismis (fark $fark)", 0f, fark, 0f)
        }
    }

    @Test
    fun `ses kapaliyken geri sayim bipi tetiklenmez`() {
        val voice = EngineVoice(sampleRate)
        voice.setEnabled(false)
        voice.playCountdownBeep(final = false)
        assertFalse("ses kapaliyken bip calmamali", voice.isCountdownBeepActive())
        voice.playCountdownBeep(final = true)
        assertFalse("ses kapaliyken son bip de calmamali", voice.isCountdownBeepActive())
        assertEquals("cikis tamamen sessiz olmali", 0f, voice.renderPeak(0.6f), 1e-6f)
    }

    /** [EngineVoice.reset] yarim kalmis bipi de temizler (ekrandan cikma). */
    @Test
    fun `reset yarim kalmis bipi temizler`() {
        val voice = EngineVoice(sampleRate)
        voice.playCountdownBeep(final = true)
        voice.renderPeak(0.05f)
        assertTrue(voice.isCountdownBeepActive())

        voice.reset()
        assertFalse("reset bipi durdurmali", voice.isCountdownBeepActive())
    }

    /**
     * Uste uste tetiklenirse zarf SIFIRDAN baslar (yiginmaz) — cunku
     * `beepRestart` fazi da sifirlar. Oyunda bu ancak rakam bir kare icinde
     * iki kez degisirse olur, ama davranisin tanimli olmasi gerekiyor.
     */
    @Test
    fun `bip yeniden tetiklenince bastan baslar`() {
        val voice = EngineVoice(sampleRate)
        voice.playCountdownBeep(final = false)
        voice.renderPeak(0.05f)

        voice.playCountdownBeep(final = false)
        val ilk = FloatArray(8)
        voice.render(ilk)
        assertEquals("yeniden tetiklenen bip de sifirdan acilmali", 0f, ilk[0], 1e-6f)
    }

    @Test
    fun `motor sesi hiza gore acilir ve idle susturur`() {
        val voice = EngineVoice(sampleRate)
        val sessiz = voice.renderPeak(0.2f)
        assertEquals("hiz verilmeden once sessiz olmali", 0f, sessiz, 1e-6f)

        voice.update(speed = 2.2f, boosting = false)
        val yavas = voice.renderPeak(1f)
        voice.update(speed = 8.5f, boosting = false)
        val hizli = voice.renderPeak(1f)
        assertTrue("hizlaninca ses yukselmeli ($yavas -> $hizli)", hizli > yavas)

        voice.idle()
        // Yumusatma nedeniyle aninda degil, sonumlenerek susar.
        voice.renderPeak(3f)
        assertTrue("kosu bitince ses sonmeli", voice.renderPeak(0.5f) < 0.002f)
    }
}
