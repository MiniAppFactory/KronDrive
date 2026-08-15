package com.miniappfactory.krondrive.ui.levels

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Bir noktanin harita uzayindaki yeri. Compose'un `Offset`'i yerine kendi
 * tipimiz kullaniliyor ki bu dosya SAF KOTLIN kalsin ve JVM testleriyle
 * dogrulanabilsin (bkz. `app/src/test/.../ui/levels/TrackLayoutTest.kt`).
 */
data class TrackPoint(val x: Float, val y: Float)

/**
 * Kariyer haritasinin PIST GEOMETRISI.
 *
 * Harita artik bir izgara degil, dikey akan kivrimli bir yaris pisti (sahibi
 * istegi, 2026-08-15). Pistin butun olculeri burada, tek bir sinus egrisinden
 * turetilir; ekran dosyasi ([LevelMapScreen]) yalnizca bu sayilari cizer.
 *
 * ## Koordinat sistemi
 *
 * Olculer **dp** cinsindendir ve Canvas ile ayni yonu kullanir: `y` asagi
 * dogru buyur, `0` icerigin EN USTU, [totalHeight] EN ALTI.
 *
 * Bolum 1 pistin **en altinda**, son bolum en ustundedir — yani oyuncu
 * ilerledikce arac yukari dogru cikar. Bu yuzden 0 tabanli bolum indeksi
 * (`0` = bolum 1) ile ekrandaki segment indeksi (`0` = en ustteki segment)
 * BIRBIRININ TERSIDIR; donusum icin [segmentOfLevel] / [levelOfSegment].
 *
 * ## Egri
 *
 * ```
 * centerX(y) = width/2 + amplitude * sin(PI * y / segmentHeight)
 * ```
 *
 * Yarim dalga boyu tam olarak bir segment: bu sayede her durak sinusun bir
 * TEPESINE dusuyor (`sin = +/-1`), duraklar sirayla sag-sol diziliyor ve iki
 * durak arasindaki mesafe hicbir zaman [segmentHeight]'in altina inmiyor.
 *
 * Genlik iki sinirin kucugudur:
 *  - Egim siniri: `WEAVE_SLOPE * segmentHeight / PI` — yolun dikeyden en fazla
 *    ne kadar sapacagini belirler. Buyuk genlik pisti zikzak yapar.
 *  - Tasma siniri: yol + kerb + kenar boslugu ekrana sigmali (bkz.
 *    [halfWidthWithKerb]). Dar ekranda (320 dp) bu sinir devreye girer.
 */
class TrackLayout(
    val stopCount: Int,
    val width: Float,
    val segmentHeight: Float = SEGMENT_HEIGHT,
    val roadHalfWidth: Float = ROAD_WIDTH / 2f,
    val kerbWidth: Float = KERB_WIDTH,
    val edgeMargin: Float = EDGE_MARGIN
) {
    init {
        require(stopCount > 0) { "stopCount > 0 olmali" }
        require(segmentHeight > 0f) { "segmentHeight > 0 olmali" }
    }

    /** Yolun kerbleriyle birlikte merkezden olculen yari genisligi. */
    val halfWidthWithKerb: Float = roadHalfWidth + kerbWidth

    /** Kaydirilabilir icerigin toplam yuksekligi. */
    val totalHeight: Float = segmentHeight * stopCount

    /** Egrinin merkezden sapmasi — yukaridaki iki sinirin kucugu. */
    val amplitude: Float = minOf(
        WEAVE_SLOPE * segmentHeight / PI.toFloat(),
        (width / 2f - halfWidthWithKerb - edgeMargin).coerceAtLeast(0f)
    )

    // -----------------------------------------------------------------
    // Egri
    // -----------------------------------------------------------------

    /** Pistin orta cizgisinin [y] yuksekligindeki x'i. */
    fun centerX(y: Float): Float =
        width / 2f + amplitude * sin(PI.toFloat() * y / segmentHeight)

    /** `dx/dy` — pistin o noktadaki egimi. Dikey pist icin 0. */
    fun slopeAt(y: Float): Float =
        amplitude * (PI.toFloat() / segmentHeight) * cos(PI.toFloat() * y / segmentHeight)

    /**
     * Orta cizgiden [offset] kadar YANA (pozitif = saga) kaydirilmis nokta.
     * Kaydirma egrinin NORMALI boyunca yapilir; kavisli bolumde yol kenari
     * boylece daralmadan paralel gider.
     */
    fun edgePoint(y: Float, offset: Float): TrackPoint {
        val slope = slopeAt(y)
        // Teget (dx, dy) = (slope, 1); normali (1, -slope).
        val length = hypot(1f, slope)
        return TrackPoint(
            x = centerX(y) + offset / length,
            y = y - offset * slope / length
        )
    }

    // -----------------------------------------------------------------
    // Duraklar
    // -----------------------------------------------------------------

    /**
     * [levelIndex] (0 tabanli; `0` = bolum 1) duraginin y'si.
     * Duraklar segmentlerin TAM ORTASINDA durur.
     */
    fun stopY(levelIndex: Int): Float =
        totalHeight - (levelIndex + 0.5f) * segmentHeight

    fun stopPoint(levelIndex: Int): TrackPoint {
        val y = stopY(levelIndex)
        return TrackPoint(centerX(y), y)
    }

    /**
     * Kesirli bolum indeksinin pist uzerindeki karsiligi — arac iki durak
     * arasinda KAYARKEN bu kullanilir. Ara deger y'de dogrusal, x'te ise
     * egriden okundugu icin arac pistin disina cikmaz.
     */
    fun pointAt(position: Float): TrackPoint {
        val y = totalHeight - (position + 0.5f) * segmentHeight
        return TrackPoint(centerX(y), y)
    }

    /**
     * Aracin o noktadaki yonu, dereceli. `0` = yukari (aracin cizimdeki
     * varsayilan yonu), pozitif = saat yonu.
     */
    fun headingDegreesAt(position: Float): Float {
        val y = totalHeight - (position + 0.5f) * segmentHeight
        // Arac y'nin AZALDIGI yone gidiyor, yani x degisimi -slope.
        return Math.toDegrees(atan2(-slopeAt(y).toDouble(), 1.0)).toFloat()
    }

    /** Kariyerin ne kadari tamamlandi — her zaman 0..1. */
    fun progressOf(position: Float): Float {
        if (stopCount <= 1) return 1f
        return (position / (stopCount - 1)).coerceIn(0f, 1f)
    }

    // -----------------------------------------------------------------
    // Segment <-> bolum donusumu
    // -----------------------------------------------------------------

    /** Ekranda ustten [segmentIndex]. sirada duran segmentin bolum indeksi. */
    fun levelOfSegment(segmentIndex: Int): Int = stopCount - 1 - segmentIndex

    /** [levelIndex] bolumunun ustten kacinci segmentte oldugu. */
    fun segmentOfLevel(levelIndex: Int): Int = stopCount - 1 - levelIndex

    /** Segmentin ust kenarinin icerik icindeki y'si. */
    fun segmentTop(segmentIndex: Int): Float = segmentIndex * segmentHeight

    /**
     * Segment icindeki YEREL y icin orta cizgi. Segmentler yalnizca IKI farkli
     * sekle sahiptir (tek/cift), cunku sinusun isareti her segmentte donuyor —
     * cizim yollari bu sayede iki kez hesaplanip onbelleklenebiliyor.
     */
    fun localCenterX(localY: Float, segmentIndex: Int): Float =
        width / 2f + parity(segmentIndex) * amplitude * sin(PI.toFloat() * localY / segmentHeight)

    fun localSlopeAt(localY: Float, segmentIndex: Int): Float =
        parity(segmentIndex) * amplitude * (PI.toFloat() / segmentHeight) *
            cos(PI.toFloat() * localY / segmentHeight)

    /** Segment icinde, orta cizgiden [offset] kadar yana kaydirilmis nokta. */
    fun localEdgePoint(localY: Float, offset: Float, segmentIndex: Int): TrackPoint {
        val slope = localSlopeAt(localY, segmentIndex)
        val length = hypot(1f, slope)
        return TrackPoint(
            x = localCenterX(localY, segmentIndex) + offset / length,
            y = localY - offset * slope / length
        )
    }

    private fun parity(segmentIndex: Int): Float =
        if (abs(segmentIndex % 2) == 0) 1f else -1f

    companion object {
        /** Bir bolume ayrilan dikey alan. Dalga boyunun yarisi da budur. */
        const val SEGMENT_HEIGHT = 140f

        /** Asfaltin genisligi (kerbler haric). */
        const val ROAD_WIDTH = 88f

        /** Kerb seridinin kalinligi. */
        const val KERB_WIDTH = 7f

        /** Kerbin ekran kenarina en yakin mesafesi. */
        const val EDGE_MARGIN = 10f

        /**
         * Pistin dikeyden en fazla sapmasi (`|dx/dy|`). 1.0 = 45 derece;
         * daha buyugu pisti zikzak, daha kucugu duz otoyol yapar.
         */
        const val WEAVE_SLOPE = 1.0f

        /** Kerb bloklarinin boyu. [SEGMENT_HEIGHT] bunun CIFT kati olmali. */
        const val KERB_BLOCK = 14f

        /** Durak dairesinin capi — dokunma hedefi (>= 48 dp olmali). */
        const val STOP_DIAMETER = 56f

        /**
         * Aracin, uzerinde durdugu duragin ne kadar GERISINDE parkettigi
         * (segment orani). 0.42 x 140 = ~59 dp: aracin burnu (merkezinden 41 dp
         * ileride) tam durak dairesinin kenarinda kaliyor, bolum numarasi
         * kapanmiyor. Mockup ile dogrulandi.
         */
        const val CAR_TRAIL = 0.42f

        /**
         * Arac cizim kutusu (CarCatalog kutusunun en-boy orani korunur).
         *
         * 42x82 -> 30x59 (sahibi geri bildirimi, 2026-08-15: "haritada araba
         * buyuk olmus, kucultulebilir"). Yol genisligi degismedi; arac artik
         * seridi doldurmuyor, uzerinde ilerledigi bir pist arac gibi duruyor.
         */
        const val CAR_WIDTH = 30f
        const val CAR_HEIGHT = 59f
    }
}
