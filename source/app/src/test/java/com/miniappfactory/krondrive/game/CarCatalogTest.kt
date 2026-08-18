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
            // AYNI fiyat serbest: iki nostalji aracı (Kus SLX / Dag Kecisi)
            // bilerek ayni basamakta (1500) — biri otekinin ucuz alternatifi
            // olmasin diye (sahibi karari, 2026-08-15). Geriye gitmek yine yasak.
            assertTrue(
                "sekiller artan fiyatla siralanmali: $previousShape -> ${shape.priceCoins}",
                shape.priceCoins >= previousShape
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
    // Surus ozellikleri (2026-08-15) — katalog akli
    // -----------------------------------------------------------------

    /** Fiyati olan araclar; Sehir referans oldugu icin ayri degerlendirilir. */
    private val paidShapes get() = CarCatalog.shapes.filter { it.priceCoins > 0 }

    @Test
    fun `varsayilan arac dort eksende de tam referans`() {
        // Bu test kirilirsa TUM denge kayar: yukseltme egrileri, bolum
        // hedefleri ve skor egrisi bu aracin davranisina gore hesaplandi.
        UpgradeType.entries.forEach { type ->
            assertEquals(
                "varsayilan govde ${type.name} ekseninde 1.0 olmali",
                1f,
                CarCatalog.defaultShape.multiplier(type),
                0f
            )
        }
    }

    @Test
    fun `hicbir carpan asiri degil`() {
        // 2026-08-18: SPEED ekseninin bandi acildi, digerleri ayni kaldi.
        //
        // Eski band dortu icin de 0.80–1.25 idi ve gerekcesi *"fark ~%10
        // civarinda kalmali, yoksa dort yukseltme dali anlamsizlasir"*. Sahibi
        // bunun bedelini oynarken gordu: butun araclarin hiz carpani
        // 0.97–1.18'e sikismisti, yani bedava aractan Formula'ya toplam kazanc
        // +%18 iken TEK bir yukseltme dali +%20 veriyordu — araclar kozmetige
        // donmustu.
        //
        // Yukseltmelerin anlamsizlasma endisesi hala gecerli ama ayri bir
        // testte korunuyor (UpgradeCatalogTest: SPEED dali oyunun en ucuz
        // km/h'i olmali). Burada yalnizca carpanlarin makul kalmasi bakiliyor.
        CarCatalog.shapes.forEach { shape ->
            UpgradeType.entries.forEach { type ->
                val mul = shape.multiplier(type)
                val band = if (type == UpgradeType.SPEED) 0.95f..2.10f else 0.80f..1.25f
                assertTrue(
                    "${shape.id}.${type.name} = $mul, $band bandi disinda",
                    mul in band
                )
            }
        }
    }

    @Test
    fun `her ucretli aracin en az bir gucu var`() {
        paidShapes.forEach { shape ->
            assertTrue(
                "${shape.id}: hicbir eksende referansi gecmiyor, alma sebebi yok",
                UpgradeType.entries.any { shape.multiplier(it) > 1f }
            )
        }
    }

    @Test
    fun `zayif yonu olmayan tek arac giris seviyesi aracidir`() {
        // Tasarim kurali: pahali arac ucuz araci COPE CEVIRMEZ, her aracin bir
        // bedeli olur. Tek istisna bilerek birakildi — 900 coinlik ilk satin
        // alma tereddutsuz "iyi" hissettirmeli (docs/BALANCE.md tablosu bu
        // arac icin fren ve boost sutununu bos birakiyor).
        val flawless = paidShapes.filter { shape ->
            UpgradeType.entries.none { shape.multiplier(it) < 1f }
        }
        assertEquals(
            "zayifsiz arac sayisi 1 olmali, bulunan: ${flawless.map { it.id }}",
            1,
            flawless.size
        )
        assertEquals(
            "zayifsiz olabilecek tek arac en ucuz ucretli aractir",
            paidShapes.minByOrNull { it.priceCoins }?.id,
            flawless.single().id
        )
    }

    @Test
    fun `hicbir ucretli arac digerini dort eksende birden gecmiyor`() {
        paidShapes.forEach { a ->
            paidShapes.filter { it.id != a.id }.forEach { b ->
                val dominates = UpgradeType.entries.all { a.multiplier(it) >= b.multiplier(it) } &&
                    UpgradeType.entries.any { a.multiplier(it) > b.multiplier(it) }
                assertFalse(
                    "${a.id}, ${b.id} aracini her eksende geciyor — ${b.id} copa dondu",
                    dominates
                )
            }
        }
    }

    @Test
    fun `her sekilde tek satirlik karakter cumlesi var`() {
        AppLanguage.entries.forEach { language ->
            val traits = CarCatalog.shapes.map { it.trait(language) }
            CarCatalog.shapes.forEach { shape ->
                val trait = shape.trait(language)
                assertTrue("${shape.id}: karakter cumlesi bos", trait.isNotBlank())
                assertFalse("${shape.id}: karakter cumlesi tek satir olmali", trait.contains('\n'))
            }
            assertEquals("karakter cumleleri ayni olamaz", traits.size, traits.toSet().size)
        }
    }

    @Test
    fun `garaj cubuklari karsilastirmali - en iyi dolu en kotu kisa`() {
        UpgradeType.entries.forEach { type ->
            val best = CarCatalog.shapes.maxByOrNull { it.multiplier(type) }!!
            val worst = CarCatalog.shapes.minByOrNull { it.multiplier(type) }!!
            assertEquals(
                "${type.name}: en iyi arac (${best.id}) cubugu dolu olmali",
                1f,
                CarCatalog.statFraction(best, type),
                1e-4f
            )
            assertTrue(
                "${type.name}: en kotu arac (${worst.id}) cubugu belirgin kisa olmali",
                CarCatalog.statFraction(worst, type) < 0.5f
            )
            assertTrue(
                "${type.name}: en kotu cubuk bile gorunur kalmali",
                CarCatalog.statFraction(worst, type) > 0f
            )
        }
    }

    @Test
    fun `cubuk uzunlugu carpanla ayni siradadir`() {
        UpgradeType.entries.forEach { type ->
            val sortedByMul = CarCatalog.shapes.sortedBy { it.multiplier(type) }
            sortedByMul.zipWithNext { low, high ->
                assertTrue(
                    "${type.name}: ${low.id} cubugu ${high.id} aracindan uzun olmamali",
                    CarCatalog.statFraction(low, type) <= CarCatalog.statFraction(high, type) + 1e-5f
                )
            }
        }
    }

    @Test
    fun `yuzde gosterimi carpanla tutarli`() {
        assertEquals(0, CarCatalog.statDeltaPercent(CarCatalog.defaultShape, UpgradeType.SPEED))
        // KIMLIKLE bulunuyor, "en pahali govde" ile DEGIL. Eskiden
        // maxByOrNull { priceCoins } ile bulunuyordu ve Beety (4000) gelince
        // test kirildi — oysa olcmek istedigi sey Super Araba'nin carpanlari.
        val supercar = CarCatalog.shapes.first { it.id == CarCatalog.SHAPE_SUPERCAR }
        // 2026-08-18: hiz yayilimi acildi (bkz. GameConfig.SCORE_SPEED_CAP_BASE).
        assertEquals(80, CarCatalog.statDeltaPercent(supercar, UpgradeType.SPEED))
        assertEquals(-6, CarCatalog.statDeltaPercent(supercar, UpgradeType.BRAKE))

        // Beety 2026-08-16'da BASLANGIC ARACI ve referans oldu: dort eksen
        // de 1.00, yani her eksende %0 fark. Once 4000 coinlik karakterli bir
        // aracti (-8 hiz / +12 fren); gerekce CarCatalog'daki notta.
        val beety = CarCatalog.shapes.first { it.id == CarCatalog.SHAPE_BEETY }
        UpgradeType.entries.forEach { type ->
            assertEquals(
                "Beety referans arac: ${type.name} ekseninde fark 0 olmali",
                0,
                CarCatalog.statDeltaPercent(beety, type)
            )
        }
        // Sehir en ucuz ucretli arac; fren guclu yonu, hizda da kucuk bir adim.
        val sehir = CarCatalog.shapes.first { it.id == CarCatalog.SHAPE_HATCHBACK }
        assertEquals(4, CarCatalog.statDeltaPercent(sehir, UpgradeType.BRAKE))
        assertEquals(8, CarCatalog.statDeltaPercent(sehir, UpgradeType.SPEED))
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
                    "${shape.id}: parca soldan tasiyor (${part.left} < ${shape.boxLeft})",
                    part.left >= shape.boxLeft - EPS
                )
                assertTrue(
                    "${shape.id}: parca sagdan tasiyor (${part.right} > ${shape.boxRight})",
                    part.right <= shape.boxRight + EPS
                )
                assertTrue(
                    "${shape.id}: parca ustten tasiyor (${part.top} < ${shape.boxTop})",
                    part.top >= shape.boxTop - EPS
                )
                assertTrue(
                    "${shape.id}: parca alttan tasiyor (${part.bottom} > ${shape.boxBottom})",
                    part.bottom <= shape.boxBottom + EPS
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
                width >= (shape.boxRight - shape.boxLeft) * MIN_BOX_FILL
            )
            assertTrue(
                "${shape.id} kutunun yuksekligini doldurmuyor ($height)",
                height >= (shape.boxBottom - shape.boxTop) * MIN_BOX_FILL
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
            // Tekerlek sayisi SINIFA bagli: motosikletin iki tekeri var ve
            // bu bir eksiklik degil, aracin tanimi (2026-08-16).
            val enAzTeker = if (shape.vehicleClass == VehicleClass.MOTOSIKLET) 2 else 4
            assertTrue(
                "${shape.id}: en az $enAzTeker tekerlek olmali",
                shape.parts.count { it.paint == CarPaint.TIRE } >= enAzTeker
            )
        }
    }

    @Test
    fun `ilk sekil cizim kutusunu tam olarak doldurur`() {
        // Varsayilan gövde 2026-08-15'te yeniden cizildi (PROVENANCE #11), ama
        // kutuyu TAM doldurmasi degismez bir sart: hem sanat olcegi hem de
        // carpisma kutusu bu dort sayidan turetiliyor.
        // 2026-08-16: varsayilan govde SHAPE_HATCHBACK'ten SHAPE_BEETY'ye
        // gecti. Test artik KIMLIGE degil DEGISMEZ SARTA bakiyor — hangi
        // govde varsayilansa kutuyu tam doldurmali.
        val classic = CarCatalog.defaultShape
        assertEquals(CarCatalog.SHAPE_BEETY, classic.id)
        assertEquals(-2f, classic.artTop, 0.001f)
        assertEquals(74f, classic.artBottom, 0.001f)
        assertEquals(-20f, classic.artLeft, 0.001f)
        assertEquals(20f, classic.artRight, 0.001f)
    }

    // -----------------------------------------------------------------
    // 2026-08-15'te eklenen iki govde (Kus SLX + Dag Kecisi)
    //
    // Ikisi de SAF KOZMETIK. Asagidaki testler bunu veriden dogruluyor:
    // ayni kutu, ayni siniflar, oyuna hicbir avantaj/dezavantaj tasimiyor.
    // -----------------------------------------------------------------

    @Test
    fun `yeni govdeler kutuyu birebir doldurur`() {
        listOf(
            CarCatalog.SHAPE_KUS_SLX,
            CarCatalog.SHAPE_MOUNTAIN_GOAT,
            CarCatalog.SHAPE_MUSCLE_67
        ).forEach { id ->
            val shape = CarCatalog.shapes.first { it.id == id }
            assertEquals("$id sol kenar", CarCatalog.ART_LEFT, shape.artLeft, 0.001f)
            assertEquals("$id sag kenar", CarCatalog.ART_RIGHT, shape.artRight, 0.001f)
            assertEquals("$id ust kenar", CarCatalog.ART_TOP, shape.artTop, 0.001f)
            assertEquals("$id alt kenar", CarCatalog.ART_BOTTOM, shape.artBottom, 0.001f)
        }
    }

    @Test
    fun `govde fiyat merdiveni beklenen sirada`() {
        // Iki nostalji araci AYNI basamakta (1500, sahibi karari 2026-08-15):
        // biri otekinin ucuz alternatifi olmamali, secim zevke kalmali.
        // Boga 67 (2400) 1800 ile 3200 arasindaki EN GENIS bosluga girdi.
        // Beety once 4000 ile merdiveni UZATMISTI; ayni gun bedava baslangic
        // araci olunca merdivenin TAVANI 3200'e geri dondu ve "yeni govdeler
        // merdiveni uzatmaz" kurali icin acilan istisna kapandi.
        // Sehir 350: Beety baslangic olunca en ucuz ucretli govde oldu ve
        // katalogun ilk satin alma basamagi 900'den 350'ye indi.
        assertEquals(
            listOf(
                CarCatalog.SHAPE_BEETY to 0,
                CarCatalog.SHAPE_HATCHBACK to 350,
                CarCatalog.SHAPE_RACE_SEDAN to 900,
                CarCatalog.SHAPE_KUS_SLX to 1500,
                CarCatalog.SHAPE_MOUNTAIN_GOAT to 1500,
                CarCatalog.SHAPE_MUSCLE to 1800,
                CarCatalog.SHAPE_MUSCLE_67 to 2400,
                // 2026-08-16: uc sinif disi arac. Motosiklet ve tir kendi
                // olcu siniflarini getiriyor; F1 BINEK'te kaliyor (referans
                // cizim otomobilden DAHA DAR, bkz. docs/VEHICLE_CLASSES.md).
                CarCatalog.SHAPE_MOTOSIKLET to 2800,
                CarCatalog.SHAPE_SUPERCAR to 3200,
                CarCatalog.SHAPE_TIR to 3600,
                CarCatalog.SHAPE_F1 to 5000
            ),
            CarCatalog.shapes.map { it.id to it.priceCoins }
        )
    }

    @Test
    fun `Kus SLX kimligi kare hat ve krom uzerine kurulu`() {
        val kus = CarCatalog.shapes.first { it.id == CarCatalog.SHAPE_KUS_SLX }
        // Kimligin tasiyicisi kose yaricapi: garajin EN kare govdesi olmali.
        val corner = kus.parts.filterIsInstance<CarPart.Box>()
            .first { it.paint == CarPaint.BODY && it.height > 40f }.corner
        CarCatalog.shapes.filter { it.id != CarCatalog.SHAPE_KUS_SLX }.forEach { other ->
            val otherCorner = other.parts.filterIsInstance<CarPart.Box>()
                .filter { it.paint == CarPaint.BODY && it.height > 40f }
                .minOfOrNull { it.corner } ?: Float.MAX_VALUE
            assertTrue(
                "${other.id} Kus SLX'ten daha kare (${otherCorner} <= $corner)",
                otherCorner > corner
            )
        }
        // Krom: farlar + izgara cubugu + iki yan serit. Baska govdede yan
        // serit yok, 60 Hz'de ayirt edici tek isaret bu.
        assertTrue(
            "Kus SLX'te krom parca yok",
            kus.parts.count { it.paint == CarPaint.TRIM } >= 4
        )
    }

    @Test
    fun `Dag Kecisi tavani digerlerinden belirgin uzun`() {
        val goat = CarCatalog.shapes.first { it.id == CarCatalog.SHAPE_MOUNTAIN_GOAT }
        // SW kimligi tavan uzunlugundan okunuyor: kokpit camindan SONRA gelen
        // en uzun BODY paneli tavandir.
        // Pencere 25..40: alt sinir SUPERCAR'in 24'ten baslayan 22 birimlik
        // GÖVDE panelini disarida tutuyor (o tavan degil), ust sinir ise
        // kokpiti geriye alan govdelere (Boga 67, tavan tepesi 34.6) yer
        // birakiyor. Daraltirsan bir govdenin tavani hic eslesmez ve
        // maxOf bos listede PATLAR — bilerek boyle genis.
        // Kiyas YALNIZCA binek govdeleri arasinda: motosikletin tavani yok,
        // tirin dorsesi 152 birim ve "tavan" olarak okunursa Dag Kecisi'ni
        // ezer — ikisi de bu iddianin konusu degil (2026-08-16).
        fun roofHeight(shape: CarShapeDef): Float = shape.parts
            .filterIsInstance<CarPart.Box>()
            .filter { it.paint == CarPaint.BODY && it.top > 25f && it.top < 40f }
            .maxOf { it.height }

        val goatRoof = roofHeight(goat)
        assertTrue("Dag Kecisi tavani cok kisa ($goatRoof)", goatRoof >= 20f)
        // Kiyas yalnizca TAVANI OLAN govdeler arasinda:
        //  - motosiklet ve tir BINEK degil, silueti bu iddianin konusu degil
        //  - F1 acik kokpitli, yani TAVANI YOK. Testin sezgisi ("kokpitten
        //    sonraki en uzun BODY paneli tavandir") onun ana govdesini tavan
        //    saniyor ve 34 birimle Dag Kecisi'ni eziyor. Yanlis olan F1
        //    degil, sezginin acik kokpite uymamasi (2026-08-16).
        val tavanliGovdeler = setOf(CarCatalog.SHAPE_F1, CarCatalog.SHAPE_MOUNTAIN_GOAT)
        CarCatalog.shapes.filter {
            it.id !in tavanliGovdeler && it.vehicleClass == VehicleClass.BINEK
        }.forEach {
            assertTrue(
                "${it.id} tavani Dag Kecisi kadar uzun",
                roofHeight(it) < goatRoof * 0.75f
            )
        }
    }

    /**
     * Iki kas arabasi ayni garajda: [CarCatalog.SHAPE_MUSCLE] ve
     * [CarCatalog.SHAPE_MUSCLE_67]. Ikisini almanin anlami olmasi icin 32
     * px'te AYRILMALARI gerekiyor; asagidaki uc olcut o ayrimin VERIDEKI
     * karsiligi. Biri bozulursa iki arac tek arac gibi gorunmeye baslar.
     */
    @Test
    fun `iki kas arabasi siluetle ayrisir`() {
        val old = CarCatalog.shapes.first { it.id == CarCatalog.SHAPE_MUSCLE }
        val new = CarCatalog.shapes.first { it.id == CarCatalog.SHAPE_MUSCLE_67 }

        fun widestStripe(shape: CarShapeDef): Float = shape.parts
            .filterIsInstance<CarPart.Box>()
            .filter { it.paint == CarPaint.ACCENT }
            .maxOf { it.width }

        // 1) Serit deseni: Boga 67'de TEK kalin bar, Kas Arabasi'nda iki ince.
        assertTrue(
            "Boga 67'nin orta seridi kalin degil (${widestStripe(new)})",
            widestStripe(new) >= widestStripe(old) * 2f
        )

        // 2) Kola-sisesi bel: gövde konturu ortada DARALIP arkada tekrar
        //    genisliyor. Kas Arabasi'nin yanlari bastan sona duz.
        val outline = new.parts.filterIsInstance<CarPart.Wedge>()
            .first { it.paint == CarPaint.BODY }
        val shoulder = outline.points.filter { it.y in 12f..16f }.maxOf { it.x }
        val waist = outline.points.filter { it.y in 28f..42f }.maxOf { it.x }
        val hip = outline.points.filter { it.y in 54f..72f }.maxOf { it.x }
        assertTrue("bel omuzdan dar degil ($waist >= $shoulder)", waist < shoulder - 1f)
        assertTrue("kalca belden genis degil ($hip <= $waist)", hip > waist + 1f)

        // 3) Kokpit ARKADA: uzun kaput / kisa bagaj. On camin basladigi y,
        //    Kas Arabasi'ndan belirgin buyuk olmali.
        fun windshieldTop(shape: CarShapeDef): Float = shape.parts
            .filterIsInstance<CarPart.Box>()
            .filter { it.paint == CarPaint.GLASS }
            .minOf { it.top }
        assertTrue(
            "Boga 67'nin kokpiti yeterince arkada degil",
            windshieldTop(new) >= windshieldTop(old) + 3f
        )
    }

    @Test
    fun `her govde her boyayla kullanilabilir`() {
        // Sahibin sorusu (2026-08-15): "hepsinin renkleri degistirilebilir
        // olsun". Sekil ve boya BAGIMSIZ secilir; katalogda gövdeye ozel renk
        // kisiti YOKTUR. Test bunu carpim kumesi uzerinde dogruluyor: her
        // (sekil, boya) ciftinde her parca turu icin opak bir renk cikiyor.
        CarCatalog.shapes.forEach { shape ->
            CarCatalog.colors.forEach { color ->
                val style = CarStyle(shape, color)
                assertEquals(color.bodyArgb, style.argbOf(CarPaint.BODY))
                shape.parts.map { it.paint }.toSet().forEach { paint ->
                    assertEquals(
                        "${shape.id} + ${color.id}: $paint saydam cikti",
                        0xFFL,
                        (style.argbOf(paint) ushr 24) and 0xFFL
                    )
                }
            }
        }
    }

    @Test
    fun `beyaz boya var ve trafik beyaziyla karismiyor`() {
        val white = CarCatalog.colors.first { it.id == CarCatalog.COLOR_GLACIER }
        // Gercekten beyaz okunmali: her kanal en az 0xE0.
        listOf(16, 8, 0).forEach { shiftBits ->
            val channel = (white.bodyArgb ushr shiftBits) and 0xFFL
            assertTrue("Buzul Beyazi yeterince acik degil ($channel)", channel >= 0xE0L)
        }
        // ...ama trafikteki BEYAZ engelle (FFFFFF) ayni olmamali.
        assertNotEquals(0xFFFFFFFFL, white.bodyArgb)
        val trafficArgb = GameEngine.OBSTACLE_COLORS.map { it.toLong() and 0xFFFFFFFFL }
        assertFalse(
            "oyuncu beyazi bir engel rengiyle ayni",
            (white.bodyArgb and 0xFFFFFFFFL) in trafficArgb
        )
        // Serit koyu olmali: beyaz uzerine beyaz tavan rayi gorunmez olurdu.
        assertTrue(
            "Buzul Beyazi'nin serit rengi acik kalmis",
            ((white.accentArgb ushr 16) and 0xFFL) < 0x80L
        )
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

    @Test
    fun `her govdenin fabrika boyasi katalogda var`() {
        CarCatalog.shapes.forEach { shape ->
            val color = CarCatalog.colors.firstOrNull { it.id == shape.defaultColorId }
            assertTrue(
                "${shape.id} tanimsiz bir fabrika boyasi gosteriyor: ${shape.defaultColorId}",
                color != null
            )
        }
    }

    @Test
    fun `iki nostalji aracinin fabrika boyasi kendi rengi`() {
        // Proje sahibi karari (2026-08-15): referans cizimlerdeki renkler.
        val kus = CarCatalog.shapes.first { it.id == CarCatalog.SHAPE_KUS_SLX }
        val goat = CarCatalog.shapes.first { it.id == CarCatalog.SHAPE_MOUNTAIN_GOAT }
        assertEquals(CarCatalog.COLOR_TEAL, kus.defaultColorId)
        assertEquals(CarCatalog.COLOR_GLACIER, goat.defaultColorId)
    }

    @Test
    fun `govdeye sahip olmak fabrika boyasini acar`() {
        val kus = CarCatalog.shapes.first { it.id == CarCatalog.SHAPE_KUS_SLX }
        val teal = CarCatalog.colors.first { it.id == CarCatalog.COLOR_TEAL }

        // Gövde yokken boya da kapali (Petrol ucretli).
        assertTrue(teal.priceCoins > 0)
        assertTrue(
            "gövdesiz oyuncuda fabrika boyasi acilmamali",
            !CarCatalog.isOwned(teal, CarCatalog.effectiveOwnedColors(emptySet(), emptySet()))
        )

        // Gövdeyi alinca boya da acilir — arac tasarlandigi renkte gorunsun.
        val owned = CarCatalog.effectiveOwnedColors(setOf(kus.id), emptySet())
        assertTrue(CarCatalog.isOwned(teal, owned))

        // Satin alinan boyalar kaybolmaz.
        val withBought = CarCatalog.effectiveOwnedColors(
            ownedShapes = setOf(kus.id),
            ownedColors = setOf(CarCatalog.COLOR_KHAKI)
        )
        assertTrue(CarCatalog.COLOR_KHAKI in withBought)
        assertTrue(CarCatalog.COLOR_TEAL in withBought)
    }

    @Test
    fun `bedava govdelerin fabrika boyasi bastan acik`() {
        // Bedava gövdeler ([isOwned] onlari satin alinmis sayar) fabrika
        // boyalarini da bedava acar; aksi halde yeni oyuncu kendi aracini
        // yanlis renkte gorurdu.
        val free = CarCatalog.shapes.filter { it.priceCoins == 0 }
        val owned = CarCatalog.effectiveOwnedColors(emptySet(), emptySet())
        free.forEach { shape ->
            assertTrue(
                "${shape.id} fabrika boyasi bedava gövdeye ragmen kapali",
                shape.defaultColorId in owned
            )
        }
    }

    private companion object {
        /** Sekil, kutunun en az bu kadarini doldurmali. */
        const val MIN_BOX_FILL = 0.9f

        /** Float turetme toleransi (bkz. kutu testi). */
        const val EPS = 0.001f
    }
}
