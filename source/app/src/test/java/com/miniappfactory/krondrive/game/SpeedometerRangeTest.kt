package com.miniappfactory.krondrive.game

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gosterge, oyunda ULASILABILEN hicbir hizi kirpmamali.
 *
 * ## Neden ayri bir test
 *
 * 2026-08-18'de arac hiz yayilimi acildi (carpanlar 0.97–1.18 → 1.00–2.08).
 * Butun testler yesil kaldi, cunku hicbiri gostergeye bakmiyordu. Ama sonsuz
 * modda ust araclar tavana (240) yapisti:
 *
 *     Super Araba sv8 ham 309 → ekranda 240
 *     Formula     sv8 ham 336 → ekranda 240
 *
 * Sonuc: kariyerde %53 olan arac farki sonsuz modda %7'ye dusuyordu. Merdiven
 * vardi ama GORUNMUYORDU — yani o gunku isin tamami sessizce bosa gidiyordu.
 * Sahibi bunu "Beety'nin top speed'i kac" diye sorunca fark edildi.
 *
 * Ders: bir denge sabitini genisletmek, o degeri GOSTEREN yerin sinirlarini da
 * ilgilendirir. Bu test o baglantiyi kaliciya baglar.
 */
class SpeedometerRangeTest {

    /** Bir aracin o yukseltme seviyesindeki sonsuz-mod tepe hizi (kirpilmamis). */
    private fun hamKmh(car: CarShapeDef, level: Int, carpan: Float): Float {
        val hedef = GameConfig.BASE_SPEED +
            UpgradeCatalog.scoreSpeedCap(level, car) * carpan
        return (hedef - GameConfig.MIN_SPEED) / GameConfig.SPEEDOMETER_SPAN *
            GameConfig.SPEEDOMETER_RANGE_KMH + GameConfig.SPEEDOMETER_MIN_KMH
    }

    @Test
    fun `hicbir arac sonsuz modda gostergeyi kirpmiyor`() {
        val carpan = GameConfig.ENDLESS_SPEED_MAX_MULTIPLIER
        CarCatalog.shapes.forEach { car ->
            for (level in 1..UpgradeCatalog.MAX_LEVEL) {
                val ham = hamKmh(car, level, carpan)
                assertTrue(
                    "${car.id} sv$level sonsuz modda ${ham.toInt()} km/h yapiyor ama " +
                        "gosterge ${GameConfig.SPEEDOMETER_MAX_KMH.toInt()}'de kirpiyor — " +
                        "arac farki ekranda kayboluyor",
                    ham <= GameConfig.SPEEDOMETER_MAX_KMH
                )
            }
        }
    }

    /**
     * Kirpmamak yetmez: tavan gereginden yuksek olursa gosterge yarisi bos
     * kalir ve ibre hicbir zaman dolmaz. En hizli kombinasyon tavanin en az
     * %85'ine ulasmali.
     */
    @Test
    fun `gosterge tavani gereginden yuksek degil`() {
        val carpan = GameConfig.ENDLESS_SPEED_MAX_MULTIPLIER
        val enHizli = CarCatalog.shapes.maxOf { hamKmh(it, UpgradeCatalog.MAX_LEVEL, carpan) }
        assertTrue(
            "en hizli kombinasyon ${enHizli.toInt()} km/h, gosterge tavani " +
                "${GameConfig.SPEEDOMETER_MAX_KMH.toInt()} — ibre hicbir zaman dolmaz",
            enHizli >= GameConfig.SPEEDOMETER_MAX_KMH * 0.85f
        )
    }

    /**
     * Sonsuz modda arac merdiveni GORUNUR olmali: en ucuz ve en pahali arac
     * arasindaki fark, kirpma sonrasi bile anlamli kalmali.
     */
    @Test
    fun `sonsuz modda arac farki korunuyor`() {
        val carpan = GameConfig.ENDLESS_SPEED_MAX_MULTIPLIER
        val bedava = CarCatalog.defaultShape
        val enIyi = CarCatalog.shapes.maxByOrNull { it.topSpeedMul }!!
        val altKmh = GameConfig.speedToKmh(
            GameConfig.BASE_SPEED + UpgradeCatalog.scoreSpeedCap(1, bedava) * carpan
        )
        val ustKmh = GameConfig.speedToKmh(
            GameConfig.BASE_SPEED + UpgradeCatalog.scoreSpeedCap(1, enIyi) * carpan
        )
        assertTrue(
            "sonsuz modda bedava arac $altKmh, en iyi arac $ustKmh km/h — " +
                "fark %${((ustKmh - altKmh) * 100 / altKmh)} , en az %30 olmali",
            (ustKmh - altKmh) * 100 / altKmh >= 30
        )
    }
}
