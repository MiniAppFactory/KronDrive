package com.miniappfactory.krondrive.ui.levels

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.miniappfactory.krondrive.ui.theme.KronColors

/**
 * Kariyer haritasindaki pistin CIZIM YOLLARI.
 *
 * Kaydirma sirasinda her karede yeniden Path kurmak kabul edilemez: harita 30
 * segment uzunlugunda ve LazyColumn kaydirirken ayni segmentler tekrar tekrar
 * cizilir. Neyse ki pist [TrackLayout] geometrisinde yalnizca IKI farkli
 * segment sekli barindirir (sinusun isareti her segmentte donuyor), yani tum
 * harita iki yol setiyle cizilebilir. Bu set genislik/yogunluk basina BIR KEZ
 * kurulup `remember` ile saklanir.
 *
 * Yollar piksel uzayindadir ve segmentin YEREL koordinatlarina gore kurulur
 * (0 = segmentin ust kenari), boylece her segment kendi Canvas'inda ayni
 * yolu kullanabilir.
 */
class TrackSegmentArt internal constructor(
    /** Asfaltin disindaki koyu bant — yol zeminden ayrilsin diye. */
    internal val shoulder: List<Path>,
    internal val road: List<Path>,
    /** Kerbin kirmizi ve acik bloklari (birbirinin arasina giren desen). */
    internal val kerbRed: List<Path>,
    internal val kerbLight: List<Path>,
    internal val laneDash: List<Path>
)

/** Kerbin acik bloklari — oyundaki kerb ile ayni ton. */
private val KerbLight = Color(0xFFDCE2E9)

/** Yolun kenarindaki koyu bant. */
private val Shoulder = Color(0xCC040D1B)

/** Kenar bandinin asfalttan disariya tasma miktari (dp). */
private const val SHOULDER_SPREAD = 16f

/** Bir seridin (kenar egrisinin) kac dogru parcasina bolunecegi. */
private const val RIBBON_STEPS = 18

/** Segment sinirinda tuy kalinliginda dikis izi kalmasin diye tasma (dp). */
private const val OVERHANG = 0.6f

/**
 * [TrackSegmentArt]'i kurar. [density] piksel donusumu icindir; dp uzayindaki
 * [layout] burada bir kez piksele cevrilir, cizim aninda carpma yapilmaz.
 */
fun buildTrackSegmentArt(layout: TrackLayout, density: Float): TrackSegmentArt {
    val segments = listOf(0, 1)

    fun ribbon(segment: Int, y0: Float, y1: Float, near: Float, far: Float, into: Path) {
        // Ileri yonde bir kenar, geri yonde diger kenar: kapali bir serit.
        for (i in 0..RIBBON_STEPS) {
            val y = y0 + (y1 - y0) * i / RIBBON_STEPS
            val point = layout.localEdgePoint(y, near, segment)
            if (i == 0) {
                into.moveTo(point.x * density, point.y * density)
            } else {
                into.lineTo(point.x * density, point.y * density)
            }
        }
        for (i in RIBBON_STEPS downTo 0) {
            val y = y0 + (y1 - y0) * i / RIBBON_STEPS
            val point = layout.localEdgePoint(y, far, segment)
            into.lineTo(point.x * density, point.y * density)
        }
        into.close()
    }

    val height = layout.segmentHeight
    val half = layout.roadHalfWidth
    val kerb = layout.kerbWidth
    val blockCount = (height / TrackLayout.KERB_BLOCK).toInt()

    val shoulder = segments.map { segment ->
        Path().also {
            ribbon(
                segment, -OVERHANG, height + OVERHANG,
                -(half + kerb + SHOULDER_SPREAD), half + kerb + SHOULDER_SPREAD, it
            )
        }
    }
    val road = segments.map { segment ->
        Path().also { ribbon(segment, -OVERHANG, height + OVERHANG, -half, half, it) }
    }

    val kerbRed = segments.map { Path() }
    val kerbLight = segments.map { Path() }
    segments.forEach { segment ->
        for (block in 0 until blockCount) {
            val y0 = block * TrackLayout.KERB_BLOCK
            val y1 = y0 + TrackLayout.KERB_BLOCK + 0.3f
            val target = if (block % 2 == 0) kerbRed[segment] else kerbLight[segment]
            ribbon(segment, y0, y1, -(half + kerb), -half, target)
            ribbon(segment, y0, y1, half, half + kerb, target)
        }
    }

    // Orta serit cizgisi: kerb blogunun yarisi dolu, yarisi bos.
    val laneDash = segments.map { segment ->
        Path().also { path ->
            for (block in 0 until blockCount step 2) {
                val y0 = block * TrackLayout.KERB_BLOCK
                ribbon(segment, y0, y0 + TrackLayout.KERB_BLOCK * 0.55f, -1.6f, 1.6f, path)
            }
        }
    }

    return TrackSegmentArt(shoulder, road, kerbRed, kerbLight, laneDash)
}

/** Onbelleklenmis yollari cizer. Segmentin tek/ciftligi hangi seti secer. */
fun DrawScope.drawTrackSegment(art: TrackSegmentArt, segmentIndex: Int) {
    val shape = if (segmentIndex % 2 == 0) 0 else 1
    drawPath(art.shoulder[shape], Shoulder)
    drawPath(art.road[shape], KronColors.Road)
    drawPath(art.kerbRed[shape], KronColors.KerbRed)
    drawPath(art.kerbLight[shape], KerbLight)
    drawPath(art.laneDash[shape], KronColors.RoadLine, alpha = 0.5f)
}

/**
 * Yolu enine kesen damali bant — pistin baslangici (bolum 1) ve bitisi
 * (son bolum) icin. Yalnizca iki segmentte cizildiginden onbelleklenmiyor.
 */
fun DrawScope.drawCheckerBand(
    layout: TrackLayout,
    density: Float,
    segmentIndex: Int,
    localY: Float,
    columns: Int = 8,
    rows: Int = 2
) {
    val span = layout.roadHalfWidth * 2f
    val cell = span / columns
    for (row in 0 until rows) {
        val y0 = localY + row * cell
        val y1 = y0 + cell
        for (column in 0 until columns) {
            val near = -layout.roadHalfWidth + column * cell
            val far = near + cell
            val path = Path()
            listOf(
                layout.localEdgePoint(y0, near, segmentIndex),
                layout.localEdgePoint(y0, far, segmentIndex),
                layout.localEdgePoint(y1, far, segmentIndex),
                layout.localEdgePoint(y1, near, segmentIndex)
            ).forEachIndexed { index, point ->
                if (index == 0) {
                    path.moveTo(point.x * density, point.y * density)
                } else {
                    path.lineTo(point.x * density, point.y * density)
                }
            }
            path.close()
            val light = (row + column) % 2 == 0
            drawPath(path, if (light) KronColors.TextPrimary else Color(0xFF10151F))
        }
    }
}
