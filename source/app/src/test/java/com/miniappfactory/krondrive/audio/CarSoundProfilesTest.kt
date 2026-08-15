package com.miniappfactory.krondrive.audio

import com.miniappfactory.krondrive.game.CarCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ses profili tablosunun dogrulanmasi.
 *
 * Bu makinede hoparlor yok — sesin "guzel" olup olmadigi burada
 * dogrulanamaz, o karar proje sahibinin. Burada dogrulanan sey tablonun
 * TUTARLILIGI: her gövde bir profil buluyor mu, bilinmeyen id cokuyor mu,
 * profiller gercekten birbirinden farkli mi ve sahibin acikca istedigi
 * karakter ("Boğa 67 en gürültülü") sayilara yansimis mi.
 */
class CarSoundProfilesTest {

    @Test
    fun `katalogdaki her govde bir ses profili bulur`() {
        CarCatalog.shapes.forEach { shape ->
            val profile = CarSoundProfiles.forShape(shape.id)
            assertEquals(
                "${shape.id} icin ozel profil olmali, varsayilana dusmemeli",
                shape.id,
                profile.id
            )
        }
        assertEquals(
            "profil sayisi gövde sayisiyla ayni olmali",
            CarCatalog.shapes.size,
            CarSoundProfiles.all.size
        )
    }

    @Test
    fun `bilinmeyen bos ve null id varsayilana duser`() {
        assertSame(CarSoundProfiles.DEFAULT, CarSoundProfiles.forShape(null))
        assertSame(CarSoundProfiles.DEFAULT, CarSoundProfiles.forShape(""))
        assertSame(CarSoundProfiles.DEFAULT, CarSoundProfiles.forShape("tir_2027"))
        assertSame(CarSoundProfiles.DEFAULT, CarSoundProfiles.forShape("HATCHBACK"))
    }

    @Test
    fun `profiller birbirinden ayirt edilebilir`() {
        val profiles = CarSoundProfiles.all
        // Kimlikler benzersiz.
        assertEquals(profiles.size, profiles.map { it.id }.toSet().size)
        // Iki gövde ayni ses parametreleriyle gelmemeli — aksi halde oyuncu
        // arac degistirdiginde hicbir sey duymaz.
        val fingerprints = profiles.map {
            listOf(
                it.freqMul, it.harmonic2, it.harmonic3, it.harmonic4, it.harmonic5,
                it.grit, it.noiseAmount, it.lopeDepth, it.lopeRate,
                it.gainMul, it.cutoffMul
            )
        }
        assertEquals("iki gövde ayni motor sesine sahip", profiles.size, fingerprints.toSet().size)
        // Korna da araca gore degisir; en az bes farkli temel frekans olsun.
        assertTrue(
            "kornalar birbirine cok yakin",
            profiles.map { it.hornBaseHz }.toSet().size >= 5
        )
    }

    @Test
    fun `Boga 67 katalogun en gurultulu egzozu`() {
        val boga = CarSoundProfiles.forShape(CarCatalog.SHAPE_MUSCLE_67)
        val digerleri = CarSoundProfiles.all.filter { it.id != boga.id }

        assertTrue(
            "en yuksek genlik Boga 67'de olmali",
            digerleri.all { it.gainMul < boga.gainMul }
        )
        assertTrue(
            "en kalin motor Boga 67'de olmali",
            digerleri.all { it.freqMul > boga.freqMul }
        )
        assertTrue(
            "en derin lope Boga 67'de olmali",
            digerleri.all { it.lopeDepth < boga.lopeDepth }
        )
        assertTrue(
            "en kalin korna Boga 67'de olmali",
            digerleri.all { it.hornBaseHz > boga.hornBaseHz }
        )
        // V8 karakterinin sayisal tanimi: TEK sayili harmonikler cift
        // sayililardan baskin.
        assertTrue(
            "tek sayili harmonikler baskin olmali",
            boga.harmonic3 + boga.harmonic5 > 2f * (boga.harmonic2 + boga.harmonic4)
        )
    }

    @Test
    fun `dag kecisi tok super araba keskin`() {
        val keci = CarSoundProfiles.forShape(CarCatalog.SHAPE_MOUNTAIN_GOAT)
        val super_ = CarSoundProfiles.forShape(CarCatalog.SHAPE_SUPERCAR)
        val sehir = CarSoundProfiles.DEFAULT

        assertTrue("dag kecisi en kapali filtreye sahip olmali", keci.cutoffMul < sehir.cutoffMul)
        assertTrue("dizel dokusu en yuksek dag kecisinde olmali",
            CarSoundProfiles.all.filter { it.id != keci.id }.all { it.noiseAmount < keci.noiseAmount })
        assertTrue("super araba en tiz olmali",
            CarSoundProfiles.all.filter { it.id != super_.id }.all { it.freqMul < super_.freqMul })
        assertTrue("super araba en acik filtreye sahip olmali",
            CarSoundProfiles.all.filter { it.id != super_.id }.all { it.cutoffMul < super_.cutoffMul })
    }

    @Test
    fun `normalizasyon ham dalgayi bir birimin altinda tutar`() {
        CarSoundProfiles.all.forEach { p ->
            // Tum bilesenler ayni anda tepe yapsa bile:
            val worstCase =
                (1f + p.harmonic2 + p.harmonic3 + p.harmonic4 + p.harmonic5 + p.grit) *
                    p.waveNormalize
            assertTrue(
                "${p.id}: ham motor dalgasi 1'i asiyor ($worstCase)",
                worstCase <= 1f + 1e-4f
            )
            val hornWorst = 2f * (1f + p.hornBuzz * 1.08f) * p.hornNormalize
            assertTrue("${p.id}: ham korna dalgasi 1'i asiyor ($hornWorst)", hornWorst <= 1f + 1e-4f)
        }
    }
}
