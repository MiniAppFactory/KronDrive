package com.miniappfactory.krondrive.data

import com.miniappfactory.krondrive.game.CarCatalog
import com.miniappfactory.krondrive.game.GameConfig
import com.miniappfactory.krondrive.game.GameEngine
import com.miniappfactory.krondrive.game.RunMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Arac ozellestirmesinin ilerleme kaydiyla ve motorla iliskisi.
 *
 * Buradaki asil soru: guncelleme oncesinden gelen bir kayit (arac alanlari
 * hic yokken yazilmis) bozulmadan varsayilana duser mi.
 */
class PlayerProgressCarTest {

    @Test
    fun `eski kayit varsayilan araca duser`() {
        // Arac alanlari HIC verilmeden kurulan ilerleme = guncelleme oncesi
        // kayittan okunan hal.
        val legacy = PlayerProgress(coins = 500, xp = 1200)

        assertEquals(CarCatalog.DEFAULT_SHAPE_ID, legacy.carShapeId)
        assertEquals(CarCatalog.DEFAULT_COLOR_ID, legacy.carColorId)
        assertTrue(legacy.ownedCarShapes.isEmpty())
        assertTrue(legacy.ownedCarColors.isEmpty())
        assertEquals(CarCatalog.defaultStyle, legacy.carStyle)
    }

    @Test
    fun `carStyle secili id'leri cozer`() {
        val paidShape = CarCatalog.shapes.first { it.priceCoins > 0 }
        val paidColor = CarCatalog.colors.first { it.priceCoins > 0 }
        val progress = PlayerProgress(
            carShapeId = paidShape.id,
            carColorId = paidColor.id,
            ownedCarShapes = setOf(paidShape.id),
            ownedCarColors = setOf(paidColor.id)
        )
        assertEquals(paidShape, progress.carStyle.shape)
        assertEquals(paidColor, progress.carStyle.color)
    }

    @Test
    fun `bozuk id oyunu kirmaz`() {
        val progress = PlayerProgress(carShapeId = "yok", carColorId = "yok")
        assertEquals(CarCatalog.defaultStyle, progress.carStyle)
    }

    @Test
    fun `carLevelForXp ile carLevel ayni formulu kullanir`() {
        listOf(0, 1, 499, 500, 501, 4999, 100_000).forEach { xp ->
            assertEquals(
                PlayerProgress.carLevelForXp(xp),
                PlayerProgress(xp = xp).carLevel
            )
        }
        assertEquals(1, PlayerProgress.carLevelForXp(0))
        assertEquals(2, PlayerProgress.carLevelForXp(GameConfig.XP_PER_CAR_LEVEL))
    }

    @Test
    fun `motor varsayilan olarak varsayilan araci kullanir`() {
        val engine = GameEngine(mode = RunMode.ENDLESS)
        assertEquals(CarCatalog.defaultStyle, engine.carStyle)
    }

    @Test
    fun `arac secimi carpisma kutusunu degistirmez`() {
        // Kritik: gorunum degisir, adalet degismez. Kutu sabitleri sekle
        // BAKMADAN GameConfig'ten gelir; motorun secili arac ile kurulmus
        // olmasi sahne olculerini etkilememeli.
        val plain = GameEngine(mode = RunMode.ENDLESS)
        CarCatalog.shapes.forEach { shape ->
            val styled = GameEngine(
                mode = RunMode.ENDLESS,
                carStyle = CarCatalog.style(shape.id, CarCatalog.colors.last().id)
            )
            plain.setViewport(360f, 640f)
            styled.setViewport(360f, 640f)
            assertEquals(plain.laneWidth, styled.laneWidth, 0.0001f)
            assertEquals(plain.playerY, styled.playerY, 0.0001f)
            assertEquals(plain.roadWidth, styled.roadWidth, 0.0001f)
        }
        // Kutu sabitleri hala gorselden turetiliyor (regresyon korumasi).
        assertEquals(
            GameConfig.CAR_WIDTH_PX * GameConfig.HITBOX_SCALE,
            GameConfig.CAR_HITBOX_WIDTH_PX,
            0.0001f
        )
    }

    /**
     * ⚠ YAYIN ENGELI — bu test KIRILMAK UZERE yazildi.
     *
     * [PlayerProgress.STARTING_COINS] 2026-08-17'de GECICI olarak 100.000
     * yapildi: sahibi butun araclari (en pahalisi 5.000) cihazda denemek
     * istedi ve *"aab yaparken degistiririz"* dedi.
     *
     * Bu deger yayina giderse oyunun ekonomisi tamamen anlamsizlasir — her
     * arac, her boya ve butun yukseltmeler (toplam ~51.000 coin) ilk saniyede
     * alinabilir. "Sonra geri almayi unuttuk" hatasi, sessizce olan turden.
     *
     * Test, degeri release degerine ([PlayerProgress.STARTING_COINS_RELEASE])
     * cektiginde KENDILIGINDEN gecer. Kirmizi yandigi surece kasitli bir test
     * degeri kullanildigini soyler.
     */
    @Test
    fun `baslangic coini ya yayin degeri ya BILINEN test degeri`() {
        // Bu test KIRMIZI YANMAZ — bilerek. Kalici kirmizi bir test her
        // build'de hata gosterir ve "hepsi yesil" sinyalini yok eder; o
        // sinyal bu projede tek dogrulama araci.
        //
        // Yaptigi is: degerin SESSIZCE kaymasini engellemek. Ya yayin degeri
        // olacak ya da tam olarak belgelenmis test degeri; arada bir sey
        // yazan (orn. birinin denerken biraktigi 5000) burada yakalanir.
        //
        // ⚠ YAYIN KAPISI TEST DEGIL, `docs/PLAY_RELEASE_CHECKLIST.md`
        // icindeki S-7 maddesidir. AAB'den once orasi kontrol edilmeli.
        val gecerli = setOf(
            PlayerProgress.STARTING_COINS_RELEASE,
            TEST_COINS
        )
        assertTrue(
            "STARTING_COINS = ${PlayerProgress.STARTING_COINS}; beklenen " +
                "${PlayerProgress.STARTING_COINS_RELEASE} (yayin) ya da " +
                "$TEST_COINS (belgelenmis test degeri)",
            PlayerProgress.STARTING_COINS in gecerli
        )
    }

    private companion object {
        /** Sahibinin butun araclari denemesi icin verilen gecici deger. */
        const val TEST_COINS = 100_000
    }

}
