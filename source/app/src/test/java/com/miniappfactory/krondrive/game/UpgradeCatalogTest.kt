package com.miniappfactory.krondrive.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpgradeCatalogTest {

    @Test
    fun `maliyet MAX_LEVEL altinda artar ve MAX_LEVEL'da null olur`() {
        var previous = 0
        for (level in 1 until UpgradeCatalog.MAX_LEVEL) {
            val cost = UpgradeCatalog.cost(level)
            assertNotNull("seviye $level icin maliyet olmali", cost)
            assertTrue("maliyet artmali: $previous -> $cost", cost!! > previous)
            previous = cost
        }
        assertNull(UpgradeCatalog.cost(UpgradeCatalog.MAX_LEVEL))
        assertNull(UpgradeCatalog.cost(UpgradeCatalog.MAX_LEVEL + 3))
    }

    @Test
    fun `cost type overload'u dogru dali okur`() {
        val levels = UpgradeLevels(speed = 1, acceleration = 2, brake = 3, boost = UpgradeCatalog.MAX_LEVEL)
        UpgradeType.entries.forEach { type ->
            assertTrue(UpgradeCatalog.cost(type, levels) == UpgradeCatalog.cost(levels.levelOf(type)))
        }
        assertNull(UpgradeCatalog.cost(UpgradeType.BOOST, levels))
    }

    // -----------------------------------------------------------------
    // Arac carpanlariyla birlesim (2026-08-15)
    // -----------------------------------------------------------------

    @Test
    fun `varsayilan arac eski degerleri BIT BIT ayni birakir`() {
        val city = CarCatalog.defaultShape
        for (level in 1..UpgradeCatalog.MAX_LEVEL) {
            assertEquals(
                UpgradeCatalog.scoreSpeedCap(level), UpgradeCatalog.scoreSpeedCap(level, city), 0f
            )
            assertEquals(UpgradeCatalog.accelRate(level), UpgradeCatalog.accelRate(level, city), 0f)
            assertEquals(
                UpgradeCatalog.brakePenalty(level), UpgradeCatalog.brakePenalty(level, city), 0f
            )
            assertEquals(UpgradeCatalog.boostDrain(level), UpgradeCatalog.boostDrain(level, city), 0f)
        }
    }

    @Test
    fun `carpan yukseltmenin USTUNE uygulanir yerine gecmez`() {
        val city = CarCatalog.defaultShape
        val supercar = CarCatalog.shape(CarCatalog.SHAPE_SUPERCAR)
        // 2026-08-18: ESKI KURAL KALDIRILDI — *"tam yukseltilmis ucuz arac,
        // yukseltmesiz en iyi araci gecmeli"*. Sahibi tersini istedi: araba
        // almak HISSEDILMELI. O kuralla araclar kozmetige donuyordu.
        //
        // Ama kuralin ARKASINDAKI endise korunuyor: yukseltme yolu bir tuzak
        // olmamali. Gercek olcut coin basina kazanc — ve SPEED dali oyunun EN
        // UCUZ km/h'i olmaya devam ediyor:
        //
        //     SPEED dali 1->8 : 1750 coin -> +35 km/h  =>  49 coin/km-h
        //     Sehir           :  350 coin -> + 5 km/h  =>  73
        //     Super Araba     : 3200 coin -> +48 km/h  =>  67
        //     Formula         : 5000 coin -> +65 km/h  =>  77
        //
        // Yani hicbir arac, yukseltmeyi coin basina geride birakmiyor.
        val dalKazanci = UpgradeCatalog.scoreSpeedCap(UpgradeCatalog.MAX_LEVEL, city) -
            UpgradeCatalog.scoreSpeedCap(1, city)
        val dalMaliyeti = (1 until UpgradeCatalog.MAX_LEVEL).sumOf {
            UpgradeCatalog.cost(it) ?: 0
        }
        CarCatalog.shapes.filter { it.priceCoins > 0 }.forEach { arac ->
            val aracKazanci = UpgradeCatalog.scoreSpeedCap(1, arac) -
                UpgradeCatalog.scoreSpeedCap(1, CarCatalog.defaultShape)
            if (aracKazanci > 0f) {
                // Araclar DAHA BUYUK sicrama verir (sahibi karari) ama
                // yukseltme yolu tuzak olmamali: dal, coin basina hicbir
                // aractan IKI KATTAN fazla kotu olamaz.
                val dalBirim = dalMaliyeti / dalKazanci
                val aracBirim = arac.priceCoins / aracKazanci
                assertTrue(
                    "${arac.id}: arac $aracBirim coin/birim, SPEED dali " +
                        "$dalBirim coin/birim — yukseltme tuzaga donmus",
                    dalBirim < aracBirim * 2f
                )
            }
        }
        // Ayni seviyede ise arac farki gercekten hissedilir.
        assertTrue(
            UpgradeCatalog.scoreSpeedCap(4, supercar) > UpgradeCatalog.scoreSpeedCap(4, city)
        )
    }

    @Test
    fun `boost carpani SUREYI uzatir yani tuketimi dusurur`() {
        val city = CarCatalog.defaultShape
        val slx = CarCatalog.shapes.maxByOrNull { it.boostMul }!!
        assertTrue("uzun boostlu arac daha AZ tuketmeli",
            UpgradeCatalog.boostDrain(1, slx) < UpgradeCatalog.boostDrain(1, city))
        // Tuketim hicbir arac/seviye bilesiminde sifira ya da negatife inemez.
        CarCatalog.shapes.forEach { shape ->
            for (level in 1..UpgradeCatalog.MAX_LEVEL) {
                assertTrue(
                    "${shape.id} sv$level: boost tuketimi pozitif kalmali",
                    UpgradeCatalog.boostDrain(level, shape) > 0f
                )
            }
        }
    }

    @Test
    fun `hiz ivme ve boost gucu seviyeyle artar`() {
        for (level in 1 until UpgradeCatalog.MAX_LEVEL) {
            assertTrue(UpgradeCatalog.scoreSpeedCap(level + 1) > UpgradeCatalog.scoreSpeedCap(level))
            assertTrue(UpgradeCatalog.accelRate(level + 1) > UpgradeCatalog.accelRate(level))
            assertTrue(UpgradeCatalog.boostSpeedBonus(level + 1) > UpgradeCatalog.boostSpeedBonus(level))
            assertTrue(UpgradeCatalog.decelRate(level + 1) > UpgradeCatalog.decelRate(level))
            assertTrue(UpgradeCatalog.brakePenalty(level + 1) > UpgradeCatalog.brakePenalty(level))
        }
    }

    @Test
    fun `boost tuketimi seviyeyle azalir ve tabaninin altina inmez`() {
        for (level in 1 until UpgradeCatalog.MAX_LEVEL) {
            assertTrue(UpgradeCatalog.boostDrain(level + 1) < UpgradeCatalog.boostDrain(level))
        }
        for (level in 1..UpgradeCatalog.MAX_LEVEL) {
            assertTrue(UpgradeCatalog.boostDrain(level) >= BOOST_DRAIN_FLOOR)
        }
        // Formul asiri seviyelerde de tabanda durmali (regresyon korumasi).
        assertTrue(UpgradeCatalog.boostDrain(100) >= BOOST_DRAIN_FLOOR)
        assertTrue(UpgradeCatalog.boostDrain(1_000_000) >= BOOST_DRAIN_FLOOR)
    }

    @Test
    fun `displayValue her dal ve her seviye icin bos olmayan metin dondurur`() {
        // 2026-08-16: displayValue artik GOVDEYE bagli, o yuzden her govde
        // icin ayri ayri deneniyor. Once govde carpani atlaniyordu ve garaj
        // her araç icin ayni sayiyi yaziyordu.
        CarCatalog.shapes.forEach { car ->
            UpgradeType.entries.forEach { type ->
                for (level in 1..UpgradeCatalog.MAX_LEVEL) {
                    val value = UpgradeCatalog.displayValue(type, level, car)
                    assertTrue("${car.id}/$type/$level bos deger", value.isNotBlank())
                    assertTrue("${car.id}/$type/$level rakam yok: $value", value.any { it.isDigit() })
                }
            }
        }
    }

    @Test
    fun `displayValue her seviyede FARKLI bir deger gosterir`() {
        // Garajdaki sayi iki seviyede ayni cikarsa oyuncu "para verdim, hicbir
        // sey degismedi" der. ACCELERATION'da tam bu oluyordu: "0.2 s" disinda
        // tum seviyeler "0.1 s" gosteriyordu (2026-08-14'te duzeltildi).
        CarCatalog.shapes.forEach { car ->
            UpgradeType.entries.forEach { type ->
                val values = (1..UpgradeCatalog.MAX_LEVEL)
                    .map { UpgradeCatalog.displayValue(type, it, car) }
                assertTrue(
                    "${car.id}/$type ayni degeri birden fazla seviyede gosteriyor: $values",
                    values.toSet().size == values.size
                )
            }
        }
    }

    @Test
    fun `yukseltme egrisi disbukey — ilk adim son adimdan kucuk`() {
        // Sikayetin kaynagi dogrusal egriydi (her seviye ayni miktarda
        // iyilestiriyordu). Egri artik hizlanarak artmali.
        val firstStep = UpgradeCatalog.curve(2) - UpgradeCatalog.curve(1)
        val lastStep = UpgradeCatalog.curve(UpgradeCatalog.MAX_LEVEL) -
            UpgradeCatalog.curve(UpgradeCatalog.MAX_LEVEL - 1)
        assertTrue("ilk adim $firstStep, son adim $lastStep", lastStep > firstStep * 2f)

        // Uc noktalar: seviye 1 hicbir sey eklemez, MAX_LEVEL tam etkiyi verir.
        assertTrue(UpgradeCatalog.curve(1) == 0f)
        assertTrue(UpgradeCatalog.curve(UpgradeCatalog.MAX_LEVEL) == 1f)
        // Sinir disi seviyeler kirpilir (regresyon korumasi).
        assertTrue(UpgradeCatalog.curve(0) == 0f)
        assertTrue(UpgradeCatalog.curve(1_000) == 1f)
    }

    @Test
    fun `UpgradeLevels with ve levelOf tutarli`() {
        var levels = UpgradeLevels()
        UpgradeType.entries.forEachIndexed { index, type ->
            levels = levels.with(type, index + 2)
        }
        UpgradeType.entries.forEachIndexed { index, type ->
            assertTrue(levels.levelOf(type) == index + 2)
        }
    }

    private companion object {
        /** UpgradeCatalog.boostDrain icindeki coerceAtLeast tabani. */
        const val BOOST_DRAIN_FLOOR = 12f
    }
}
