package com.miniappfactory.krondrive.game

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
        UpgradeType.entries.forEach { type ->
            for (level in 1..UpgradeCatalog.MAX_LEVEL) {
                val value = UpgradeCatalog.displayValue(type, level)
                assertTrue("$type/$level bos deger dondurdu", value.isNotBlank())
                assertTrue("$type/$level rakam icermiyor: $value", value.any { it.isDigit() })
            }
        }
    }

    @Test
    fun `displayValue her seviyede FARKLI bir deger gosterir`() {
        // Garajdaki sayi iki seviyede ayni cikarsa oyuncu "para verdim, hicbir
        // sey degismedi" der. ACCELERATION'da tam bu oluyordu: "0.2 s" disinda
        // tum seviyeler "0.1 s" gosteriyordu (2026-08-14'te duzeltildi).
        UpgradeType.entries.forEach { type ->
            val values = (1..UpgradeCatalog.MAX_LEVEL).map { UpgradeCatalog.displayValue(type, it) }
            assertTrue(
                "$type ayni degeri birden fazla seviyede gosteriyor: $values",
                values.toSet().size == values.size
            )
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
