package com.miniappfactory.krondrive.ui.levels

import com.miniappfactory.krondrive.game.LevelCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Pist geometrisinin JVM testleri. Ekrani bu makinede calistiramadigimiz icin
 * (adb/emulator yok) haritanin "dogru" olmasi bu testlere bagli: duraklar
 * cakismamali, pist ekrandan tasmamali, ilerleme orani 0..1 kalmali.
 */
class TrackLayoutTest {

    /** Test cihazlarinin en darina yakin genislik. */
    private val narrow = 320f

    private fun layout(width: Float = 360f, stops: Int = LevelCatalog.count) =
        TrackLayout(stopCount = stops, width = width)

    @Test
    fun `durak sayisi bolum sayisina esittir ve her bolumun bir duragi vardir`() {
        val track = layout()
        assertEquals(LevelCatalog.count, track.stopCount)

        val ys = (0 until track.stopCount).map { track.stopY(it) }
        assertEquals("her bolum icin ayri bir durak", track.stopCount, ys.toSet().size)

        // Bolum 1 EN ALTTA, son bolum EN USTTE.
        assertTrue("bolum 1 en altta olmali", ys.first() > ys.last())
        ys.zipWithNext().forEach { (lower, upper) ->
            assertTrue("duraklar sirali yukari gitmeli", upper < lower)
        }
    }

    @Test
    fun `duraklar cakismaz ve dokunma hedefinden uzaktir`() {
        listOf(narrow, 360f, 411f, 600f).forEach { width ->
            val track = layout(width)
            val points = (0 until track.stopCount).map { track.stopPoint(it) }
            points.zipWithNext().forEach { (a, b) ->
                val distance = hypot(a.x - b.x, a.y - b.y)
                assertTrue(
                    "genislik $width: iki durak arasi $distance dp, " +
                        "dokunma hedefi ${TrackLayout.STOP_DIAMETER} dp",
                    distance > TrackLayout.STOP_DIAMETER
                )
            }
        }
    }

    @Test
    fun `pist hicbir yerde ekrandan tasmaz`() {
        listOf(300f, narrow, 360f, 411f, 480f).forEach { width ->
            val track = layout(width)
            // Yolun tamami 0.5 dp adimlarla taranir; hata mesaji ucuz kalsin
            // diye uc degerler toplanip TEK iddia yapiliyor.
            var minX = Float.MAX_VALUE
            var maxX = -Float.MAX_VALUE
            var y = 0f
            while (y <= track.totalHeight) {
                val left = track.edgePoint(y, -track.halfWidthWithKerb).x
                val right = track.edgePoint(y, track.halfWidthWithKerb).x
                if (left < minX) minX = left
                if (right > maxX) maxX = right
                y += 0.5f
            }
            assertTrue("genislik $width: soldan tasti ($minX)", minX >= 0f)
            assertTrue("genislik $width: sagdan tasti ($maxX)", maxX <= width)
        }
    }

    @Test
    fun `duraklar pistin tam ortasinda oturur`() {
        val track = layout()
        (0 until track.stopCount).forEach { index ->
            val point = track.stopPoint(index)
            assertEquals(
                "durak $index yolun merkezinde olmali",
                track.centerX(point.y),
                point.x,
                1e-3f
            )
            // Durak = sinusun tepesi: merkezden tam genlik kadar sapmis olmali.
            assertEquals(
                "durak $index sinus tepesinde degil",
                track.amplitude,
                abs(point.x - track.width / 2f),
                1e-2f
            )
        }
        // Duraklar sirayla sag-sol dizilir.
        val sides = (0 until track.stopCount).map { track.stopPoint(it).x > track.width / 2f }
        sides.zipWithNext().forEach { (a, b) ->
            assertTrue("ardisik duraklar ayni tarafta", a != b)
        }
    }

    @Test
    fun `ilerleme orani her zaman 0 ile 1 arasindadir`() {
        val track = layout()
        assertEquals(0f, track.progressOf(0f), 1e-4f)
        assertEquals(1f, track.progressOf((track.stopCount - 1).toFloat()), 1e-4f)

        var position = -2f
        while (position <= track.stopCount + 2f) {
            val progress = track.progressOf(position)
            assertTrue("position=$position -> $progress", progress in 0f..1f)
            position += 0.25f
        }

        // Tek duraklik bir pist bile 0'a bolunmemeli.
        assertEquals(1f, TrackLayout(stopCount = 1, width = 360f).progressOf(0f), 1e-4f)
    }

    @Test
    fun `arac kesirli konumda da pistin uzerindedir`() {
        val track = layout()
        var position = 0f
        while (position <= (track.stopCount - 1).toFloat()) {
            val point = track.pointAt(position)
            assertEquals(
                "position=$position yolun disina cikti",
                track.centerX(point.y),
                point.x,
                1e-3f
            )
            assertTrue("position=$position icerigin disinda", point.y in 0f..track.totalHeight)
            position += 0.1f
        }

        // Aracin park yeri (duragin biraz gerisi) de icerik icinde kalmali.
        val parked = track.pointAt(0f - TrackLayout.CAR_TRAIL)
        assertTrue("park yeri icerigin disinda: ${parked.y}", parked.y in 0f..track.totalHeight)
    }

    @Test
    fun `arac yonu pistin tegetini izler ve makul sinirlarda kalir`() {
        val track = layout()
        // Duraklarda (sinus tepesi) pist dikeydir: yon ~0 derece.
        (0 until track.stopCount).forEach { index ->
            assertEquals(
                "durak $index'te arac egik durmamali",
                0f,
                track.headingDegreesAt(index.toFloat()),
                0.5f
            )
        }
        // Iki durak arasinda en fazla WEAVE_SLOPE kadar sapar (45 derece).
        var position = 0f
        val limit = Math.toDegrees(kotlin.math.atan(TrackLayout.WEAVE_SLOPE.toDouble())).toFloat()
        while (position <= (track.stopCount - 1).toFloat()) {
            val heading = track.headingDegreesAt(position)
            assertTrue("position=$position yonu $heading", abs(heading) <= limit + 0.5f)
            position += 0.05f
        }
    }

    @Test
    fun `segment ile bolum indeksi birbirine tersten eslenir`() {
        val track = layout()
        (0 until track.stopCount).forEach { levelIndex ->
            val segment = track.segmentOfLevel(levelIndex)
            assertTrue("segment araligi disinda: $segment", segment in 0 until track.stopCount)
            assertEquals(levelIndex, track.levelOfSegment(segment))

            // Durak, kendi segmentinin TAM ORTASINDA olmali — ekran dosyasi
            // durak dairesini bu varsayimla yerlestiriyor.
            val localY = track.stopY(levelIndex) - track.segmentTop(segment)
            assertEquals("durak segment ortasinda degil", track.segmentHeight / 2f, localY, 1e-2f)
        }
        // Bolum 1 en alttaki segmentte.
        assertEquals(track.stopCount - 1, track.segmentOfLevel(0))
    }

    @Test
    fun `segmentin yerel egrisi genel egriyle ayni yeri gosterir`() {
        // Cizim onbellegi yalnizca IKI segment sekli sakliyor; bu ancak yerel
        // formul genel formulle birebir ortusursa dogru olur.
        val track = layout()
        (0 until track.stopCount).forEach { segment ->
            var localY = 0f
            while (localY <= track.segmentHeight) {
                val globalY = track.segmentTop(segment) + localY
                assertEquals(
                    "segment $segment, localY=$localY",
                    track.centerX(globalY),
                    track.localCenterX(localY, segment),
                    1e-2f
                )
                assertEquals(
                    "segment $segment egimi, localY=$localY",
                    track.slopeAt(globalY),
                    track.localSlopeAt(localY, segment),
                    1e-3f
                )
                localY += 3.5f
            }
        }
    }

    @Test
    fun `kerb bloklari segment sinirinda desen kaydirmadan devam eder`() {
        // Kerb renkleri blok indeksinin tek-ciftligine gore donuyor; segment
        // basina DUSEN blok sayisi cift olmazsa desen her segmentte kayar.
        val blocks = TrackLayout.SEGMENT_HEIGHT / TrackLayout.KERB_BLOCK
        assertEquals("blok sayisi tam olmali", blocks, blocks.toInt().toFloat(), 1e-4f)
        assertEquals("blok sayisi cift olmali", 0, blocks.toInt() % 2)
    }

    @Test
    fun `dar ekranda genlik kuculur ama pist yine ortalidir`() {
        val wide = layout(600f)
        val tight = TrackLayout(stopCount = 6, width = 150f)
        assertTrue("dar ekranda genlik kisilmali", tight.amplitude < wide.amplitude)
        assertTrue("genlik negatif olamaz", tight.amplitude >= 0f)
        assertEquals("pist merkezi ekran merkezi", 75f, tight.centerX(0f), 1e-3f)
    }
}
