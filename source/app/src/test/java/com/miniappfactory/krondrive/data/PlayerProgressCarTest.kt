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
}
