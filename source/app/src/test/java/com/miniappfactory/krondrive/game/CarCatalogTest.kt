package com.miniappfactory.krondrive.game

import com.miniappfactory.krondrive.data.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Arac ozellestirme katalogunun tutarliligi.
 *
 * En kritik test [her sekil carpisma kutusuna sigar]: cizimin kutudan tasmasi
 * 2026-08-13'te duzeltilmis bir adalet hatasiydi ("havaya carptim"). Yeni bir
 * govde sekli eklerken bu testin kirilmasi, kutuyu buyutmen gerektigi anlamina
 * GELMEZ — sekli kucultmen gerektigi anlamina gelir.
 */
class CarCatalogTest {

    // -----------------------------------------------------------------
    // Katalog tutarliligi
    // -----------------------------------------------------------------

    @Test
    fun `sekil ve renk idleri benzersiz`() {
        val shapeIds = CarCatalog.shapes.map { it.id }
        assertEquals("sekil id'leri benzersiz olmali", shapeIds.size, shapeIds.toSet().size)
        val colorIds = CarCatalog.colors.map { it.id }
        assertEquals("renk id'leri benzersiz olmali", colorIds.size, colorIds.toSet().size)
    }

    @Test
    fun `kapsam en az 4 sekil ve 8 renk`() {
        assertTrue("en az 4 govde sekli olmali", CarCatalog.shapes.size >= 4)
        assertTrue("en az 8 boya olmali", CarCatalog.colors.size >= 8)
    }

    @Test
    fun `fiyatlar negatif degil ve listede artan gider`() {
        var previousShape = -1
        CarCatalog.shapes.forEach { shape ->
            assertTrue("${shape.id} fiyati negatif olamaz", shape.priceCoins >= 0)
            assertTrue(
                "sekiller artan fiyatla siralanmali: $previousShape -> ${shape.priceCoins}",
                shape.priceCoins > previousShape
            )
            previousShape = shape.priceCoins
        }
        var previousColor = -1
        CarCatalog.colors.forEach { color ->
            assertTrue("${color.id} fiyati negatif olamaz", color.priceCoins >= 0)
            assertTrue(
                "renkler artan fiyatla siralanmali: $previousColor -> ${color.priceCoins}",
                color.priceCoins > previousColor
            )
            previousColor = color.priceCoins
        }
    }

    @Test
    fun `seviye sarti da artan gider ve 1'in altina inmez`() {
        var previous = 0
        CarCatalog.shapes.forEach { shape ->
            assertTrue("${shape.id} seviye sarti en az 1 olmali", shape.requiredCarLevel >= 1)
            assertTrue("sekil seviye sarti geriye gitmemeli", shape.requiredCarLevel >= previous)
            previous = shape.requiredCarLevel
        }
        previous = 0
        CarCatalog.colors.forEach { color ->
            assertTrue("${color.id} seviye sarti en az 1 olmali", color.requiredCarLevel >= 1)
            assertTrue("renk seviye sarti geriye gitmemeli", color.requiredCarLevel >= previous)
            previous = color.requiredCarLevel
        }
    }

    @Test
    fun `varsayilan sekil ve renk bedava ve seviye 1`() {
        assertEquals(0, CarCatalog.defaultShape.priceCoins)
        assertEquals(1, CarCatalog.defaultShape.requiredCarLevel)
        assertEquals(0, CarCatalog.defaultColor.priceCoins)
        assertEquals(1, CarCatalog.defaultColor.requiredCarLevel)
        // Bedava olan yalnizca varsayilan sekil olmali; aksi halde "satin
        // alinmis" kaydi olmadan birden fazla govde acilmis olurdu.
        assertEquals(1, CarCatalog.shapes.count { it.priceCoins == 0 })
        assertEquals(1, CarCatalog.colors.count { it.priceCoins == 0 })
    }

    @Test
    fun `isimler iki dilde de bos degil ve birbirinden ayirt edilebilir`() {
        AppLanguage.entries.forEach { language ->
            val shapeNames = CarCatalog.shapes.map { it.name(language) }
            shapeNames.forEach { assertTrue("bos sekil ismi", it.isNotBlank()) }
            assertEquals("sekil isimleri ayni olamaz", shapeNames.size, shapeNames.toSet().size)

            val colorNames = CarCatalog.colors.map { it.name(language) }
            colorNames.forEach { assertTrue("bos renk ismi", it.isNotBlank()) }
            assertEquals("renk isimleri ayni olamaz", colorNames.size, colorNames.toSet().size)

            CarCatalog.shapes.forEach {
                assertTrue("${it.id} aciklamasi bos", it.description(language).isNotBlank())
            }
        }
    }

    @Test
    fun `renkler opak ve gövde-golge-serit tonlari birbirinden farkli`() {
        CarCatalog.colors.forEach { color ->
            listOf(color.bodyArgb, color.shadeArgb, color.accentArgb).forEach { argb ->
                assertEquals(
                    "${color.id} tam opak olmali (alfa FF)",
                    0xFFL,
                    (argb ushr 24) and 0xFFL
                )
            }
            assertNotEquals("${color.id}: koyu ton ana renkle ayni", color.bodyArgb, color.shadeArgb)
            assertNotEquals("${color.id}: serit ana renkle ayni", color.bodyArgb, color.accentArgb)
        }
    }

    @Test
    fun `oyuncu paleti trafik renklerini kullanmaz`() {
        // Sanat yonu kurali: tehdit rengi baska hicbir seyde kullanilmaz.
        val trafficArgb = GameEngine.OBSTACLE_COLORS.map { it.toLong() and 0xFFFFFFFFL }.toSet()
        CarCatalog.colors.forEach { color ->
            assertFalse(
                "${color.id} bir engel araci rengiyle ayni",
                (color.bodyArgb and 0xFFFFFFFFL) in trafficArgb
            )
        }
    }

    // -----------------------------------------------------------------
    // Cizim kutusu (carpisma adaleti)
    // -----------------------------------------------------------------

    @Test
    fun `her sekil carpisma kutusuna sigar`() {
        CarCatalog.shapes.forEach { shape ->
            shape.parts.forEach { part ->
                // EPS: sinirlar GameConfig'ten bolme ile turetiliyor, kil payi
                // float hatasi testi kirmasin (gercek tasmalar birim buyuklugunde).
                assertTrue(
                    "${shape.id}: parca soldan tasiyor (${part.left} < ${CarCatalog.ART_LEFT})",
                    part.left >= CarCatalog.ART_LEFT - EPS
                )
                assertTrue(
                    "${shape.id}: parca sagdan tasiyor (${part.right} > ${CarCatalog.ART_RIGHT})",
                    part.right <= CarCatalog.ART_RIGHT + EPS
                )
                assertTrue(
                    "${shape.id}: parca ustten tasiyor (${part.top} < ${CarCatalog.ART_TOP})",
                    part.top >= CarCatalog.ART_TOP - EPS
                )
                assertTrue(
                    "${shape.id}: parca alttan tasiyor (${part.bottom} > ${CarCatalog.ART_BOTTOM})",
                    part.bottom <= CarCatalog.ART_BOTTOM + EPS
                )
            }
        }
    }

    @Test
    fun `kutu sinirlari GameConfig ile birebir ortusur`() {
        // Kutu, motor tarafindaki carpisma sabitlerinden TURETILIR. Bu test,
        // birinin GameConfig'i degistirip katalogu unutmasini yakalar.
        assertEquals(
            GameConfig.CAR_WIDTH_PX,
            (CarCatalog.ART_RIGHT - CarCatalog.ART_LEFT) * GameConfig.CAR_ART_SCALE,
            0.001f
        )
        assertEquals(
            GameConfig.CAR_HEIGHT_PX,
            (CarCatalog.ART_BOTTOM - CarCatalog.ART_TOP) * GameConfig.CAR_ART_SCALE,
            0.001f
        )
    }

    @Test
    fun `her sekil kutuyu makul olcude doldurur`() {
        // Cok kucuk bir sekil, carpisma kutusundan gorunur sekilde kucuk olur
        // ve oyuncu yine "havaya carptim" der. Alt sinir bilerek gevsek.
        CarCatalog.shapes.forEach { shape ->
            val width = shape.artRight - shape.artLeft
            val height = shape.artBottom - shape.artTop
            assertTrue(
                "${shape.id} kutunun genisligini doldurmuyor ($width)",
                width >= (CarCatalog.ART_RIGHT - CarCatalog.ART_LEFT) * MIN_BOX_FILL
            )
            assertTrue(
                "${shape.id} kutunun yuksekligini doldurmuyor ($height)",
                height >= (CarCatalog.ART_BOTTOM - CarCatalog.ART_TOP) * MIN_BOX_FILL
            )
        }
    }

    @Test
    fun `her seklin govde cam lastik ve serit parcasi var`() {
        CarCatalog.shapes.forEach { shape ->
            val paints = shape.parts.map { it.paint }.toSet()
            listOf(CarPaint.BODY, CarPaint.GLASS, CarPaint.TIRE, CarPaint.ACCENT).forEach { paint ->
                assertTrue("${shape.id} icinde $paint parcasi yok", paint in paints)
            }
            assertTrue(
                "${shape.id}: dort tekerlek olmali",
                shape.parts.count { it.paint == CarPaint.TIRE } >= 4
            )
        }
    }

    @Test
    fun `ilk sekil prototipin orijinal cizimidir`() {
        // Guncelleme sonrasi mevcut oyuncular araclarini AYNI gormeli.
        val classic = CarCatalog.defaultShape
        assertEquals(CarCatalog.SHAPE_HATCHBACK, classic.id)
        assertEquals(-2f, classic.artTop, 0.001f)
        assertEquals(74f, classic.artBottom, 0.001f)
        assertEquals(-20f, classic.artLeft, 0.001f)
        assertEquals(20f, classic.artRight, 0.001f)
    }

    @Test
    fun `boya cozumu her parca turu icin renk dondurur`() {
        val style = CarStyle(CarCatalog.defaultShape, CarCatalog.colors.last())
        CarPaint.entries.forEach { paint ->
            val argb = style.argbOf(paint)
            assertEquals("$paint saydam cikti", 0xFFL, (argb ushr 24) and 0xFFL)
        }
        assertEquals(CarCatalog.colors.last().bodyArgb, style.argbOf(CarPaint.BODY))
        assertEquals(CarCatalog.GLASS_ARGB, style.argbOf(CarPaint.GLASS))
    }

    // -----------------------------------------------------------------
    // Sahiplik / satin alma / varsayilana dusme
    // -----------------------------------------------------------------

    @Test
    fun `bedava icerik satin alinmadan sahiplenilmis sayilir`() {
        assertTrue(CarCatalog.isOwned(CarCatalog.defaultShape, emptySet()))
        assertTrue(CarCatalog.isOwned(CarCatalog.defaultColor, emptySet()))
        val paid = CarCatalog.shapes.first { it.priceCoins > 0 }
        assertFalse(CarCatalog.isOwned(paid, emptySet()))
        assertTrue(CarCatalog.isOwned(paid, setOf(paid.id)))
    }

    @Test
    fun `durum coin ve arac seviyesine gore hesaplanir`() {
        val paid = CarCatalog.shapes.first { it.priceCoins > 0 }

        assertEquals(
            CarUnlockState.LEVEL_LOCKED,
            CarCatalog.stateOf(paid, emptySet(), Int.MAX_VALUE, paid.requiredCarLevel - 1)
        )
        assertEquals(
            CarUnlockState.TOO_EXPENSIVE,
            CarCatalog.stateOf(paid, emptySet(), paid.priceCoins - 1, paid.requiredCarLevel)
        )
        assertEquals(
            CarUnlockState.AFFORDABLE,
            CarCatalog.stateOf(paid, emptySet(), paid.priceCoins, paid.requiredCarLevel)
        )
        assertEquals(
            CarUnlockState.OWNED,
            CarCatalog.stateOf(paid, setOf(paid.id), 0, 1)
        )
    }

    @Test
    fun `canBuy sadece tam olarak alinabilir durumda true doner`() {
        CarCatalog.shapes.forEach { shape ->
            // Sahip olunan (ya da bedava) icerik tekrar satin alinamaz.
            assertFalse(
                "${shape.id} zaten sahipken tekrar satin alinabiliyor",
                CarCatalog.canBuy(shape, setOf(shape.id), Int.MAX_VALUE, 99)
            )
            if (shape.priceCoins > 0) {
                assertTrue(
                    CarCatalog.canBuy(shape, emptySet(), shape.priceCoins, shape.requiredCarLevel)
                )
                assertFalse(
                    "seviye yetmezken satin alinabiliyor",
                    CarCatalog.canBuy(
                        shape,
                        emptySet(),
                        Int.MAX_VALUE,
                        shape.requiredCarLevel - 1
                    )
                )
                assertFalse(
                    "coin yetmezken satin alinabiliyor",
                    CarCatalog.canBuy(
                        shape,
                        emptySet(),
                        shape.priceCoins - 1,
                        shape.requiredCarLevel
                    )
                )
            }
        }
    }

    @Test
    fun `bilinmeyen id varsayilana duser`() {
        assertEquals(CarCatalog.defaultShape, CarCatalog.shape(null))
        assertEquals(CarCatalog.defaultShape, CarCatalog.shape("bozuk_kayit"))
        assertEquals(CarCatalog.defaultColor, CarCatalog.color(null))
        assertEquals(CarCatalog.defaultColor, CarCatalog.color(""))
        assertEquals(CarCatalog.defaultStyle, CarCatalog.style("yok", "yok"))
    }

    @Test
    fun `sahip olunmayan secim varsayilana duser`() {
        val paidShape = CarCatalog.shapes.first { it.priceCoins > 0 }
        val paidColor = CarCatalog.colors.first { it.priceCoins > 0 }

        // Kayit "supercar" diyor ama envanterde yok (elle bozulmus kayit,
        // ya da geri alinmis icerik) -> varsayilan.
        assertEquals(CarCatalog.defaultShape, CarCatalog.selectedShape(paidShape.id, emptySet()))
        assertEquals(CarCatalog.defaultColor, CarCatalog.selectedColor(paidColor.id, emptySet()))

        // Envanterde varsa aynen secilir.
        assertEquals(paidShape, CarCatalog.selectedShape(paidShape.id, setOf(paidShape.id)))
        assertEquals(paidColor, CarCatalog.selectedColor(paidColor.id, setOf(paidColor.id)))

        // Hic kayit yoksa (eski kurulum) yine varsayilan — kayit bozulmaz.
        assertEquals(CarCatalog.defaultShape, CarCatalog.selectedShape(null, emptySet()))
        assertEquals(CarCatalog.defaultColor, CarCatalog.selectedColor(null, emptySet()))
    }

    private companion object {
        /** Sekil, kutunun en az bu kadarini doldurmali. */
        const val MIN_BOX_FILL = 0.9f

        /** Float turetme toleransi (bkz. kutu testi). */
        const val EPS = 0.001f
    }
}
