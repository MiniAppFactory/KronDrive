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
            // Sonra en kotu durum: motor + surekli tislama + nitro + korna
            // AYNI ANDA.
            voice.playNitro()
            voice.playHorn()
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
        assertEquals(CarCatalog.SHAPE_HATCHBACK, voice.profile.id)

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
