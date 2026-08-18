package com.miniappfactory.krondrive.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.miniappfactory.krondrive.game.CarAxis
import com.miniappfactory.krondrive.game.CarCatalog
import com.miniappfactory.krondrive.game.CarGradient
import com.miniappfactory.krondrive.game.CarPart
import com.miniappfactory.krondrive.game.CarStyle
import com.miniappfactory.krondrive.game.VehicleClass
import com.miniappfactory.krondrive.game.GameConfig
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Arac cizimi — TEK kaynak. Oyun sahnesi
 * ([com.miniappfactory.krondrive.ui.game.drawGameScene]), garaj onizlemesi ve
 * TRAFIK araclari ayni fonksiyonlari kullanir; boylece garajda gordugun arac
 * ile yolda surdugun arac (ve onundeki trafik) ayni koddan cikar.
 *
 * Sekle ozel Compose kodu YOKTUR: geometri [CarCatalog] icinde saf veri olarak
 * durur, burasi sadece o veriyi Canvas'a cevirir. Yeni sekil eklemek icin bu
 * dosyaya dokunmak gerekmez.
 *
 * Koordinatlar prototipin ham arac uzayindadir (x -20..20, y -2..74); olcegi
 * cagiran uygular.
 */

// ---------------------------------------------------------------------------
// Renk / firca yardimcilari
// ---------------------------------------------------------------------------

/** Ton kaydirma: `+1` tam beyaz, `-1` tam siyah (bkz. [com.miniappfactory.krondrive.game.CarTint]). */
private fun shifted(color: Color, shift: Float): Color = when {
    shift >= 0f -> Color(
        red = color.red + (1f - color.red) * shift,
        green = color.green + (1f - color.green) * shift,
        blue = color.blue + (1f - color.blue) * shift,
        alpha = color.alpha
    )

    else -> {
        val k = 1f + shift
        Color(color.red * k, color.green * k, color.blue * k, color.alpha)
    }
}

/**
 * Firca onbellegi.
 *
 * Gerekli, cunku sahne 60 Hz'de yeniden ciziliyor ve her karede oyuncu +
 * trafik araclari icin duzinelerce gradyan uretilirdi. [Brush] nesnesi kendi
 * shader'ini boyuta gore icinde onbelleklediginden, ayni ORNEGI yeniden
 * kullanmak native shader'in da yeniden uretilmesini engeller.
 *
 * Anahtar (renk + parca) sonlu: 4 govde + trafik x 13 renk. Sinirsiz
 * buyumez, bu yuzden tahliye politikasi yok. Compose cizimi tek is
 * parcaciginda (main) yurudugu icin senkronizasyon da gerekmiyor.
 */
private val brushCache = HashMap<Pair<Long, CarPart>, Brush>()

/**
 * Sprite tinti onbellegi — [brushCache] ile ayni gerekcede.
 *
 * [ColorFilter.tint] her cagrida yeni bir Compose nesnesi VE yeni bir native
 * `PorterDuffColorFilter` kuruyordu; sahnede oyuncu + ~8 trafik araci var, yani
 * kare basina ~9 tahsis. Palet SONLU: [com.miniappfactory.krondrive.game.GameEngine.OBSTACLE_COLORS]
 * (4 renk) + garajdan secilebilen oyuncu boyalari. Sinirsiz buyumez, o yuzden
 * tahliye politikasi yok; cizim tek is parcaciginda (main) yurudugu icin
 * senkronizasyon da gerekmiyor.
 */
private val tintCache = HashMap<Long, ColorFilter>()

private fun tintFor(argb: Long): ColorFilter =
    tintCache.getOrPut(argb) { ColorFilter.tint(Color(argb), BlendMode.Modulate) }

private fun brushFor(argb: Long, part: CarPart, gradient: CarGradient): Brush =
    brushCache.getOrPut(argb to part) {
        val base = Color(argb)
        val stops = gradient.stops
            .map { it.position to shifted(base, it.shift) }
            .toTypedArray()
        when (gradient.axis) {
            CarAxis.HORIZONTAL -> Brush.horizontalGradient(
                colorStops = stops,
                startX = part.left,
                endX = part.right
            )

            CarAxis.VERTICAL -> Brush.verticalGradient(
                colorStops = stops,
                startY = part.top,
                endY = part.bottom
            )
        }
    }

// ---------------------------------------------------------------------------
// Cizim
// ---------------------------------------------------------------------------

/** Yere dusen golge — sekilden BAGIMSIZ, aracin ayak izi hissi sabit kalsin diye. */
fun DrawScope.drawCarShadow() {
    drawOval(
        color = Color(CarCatalog.SHADOW_ARGB),
        topLeft = Offset(CarCatalog.SHADOW_LEFT, CarCatalog.SHADOW_TOP),
        size = Size(CarCatalog.SHADOW_WIDTH, CarCatalog.SHADOW_HEIGHT)
    )
}

/**
 * Yere dusen golgeyi YALNIZCA vektor cizimde cizer.
 *
 * Sprite'larda cizilmiyor (2026-08-16, proje sahibi: *"arabanin altinda golge
 * var sisman gozukuyor, o golge olmasin bence"*). Sebep: referans cizimler
 * kendi temas golgelerini ve kenar karartmalarini zaten tasiyor. Ustune
 * [drawCarShadow]'un 42x68'lik ovali binince arac gercek silueti disina
 * tasan bulanik bir hale kazaniyor ve genis/sisman okunuyor.
 *
 * Vektor yolunda golge KALIYOR: orada gövde duz renkli poligonlardan olusuyor
 * ve yere basma hissini yalnizca bu oval veriyor.
 */
fun DrawScope.drawCarShadowIfVector(style: CarStyle, sprites: CarSpriteSet?) {
    if (sprites?.of(style.shape.id) == null) drawCarShadow()
}

/** Katalogdaki parcalari sirayla cizer (liste sirasi = katman sirasi). */
fun DrawScope.drawCarParts(style: CarStyle) {
    style.shape.parts.forEach { part ->
        val argb = style.argbOf(part.paint)
        val gradient = part.gradient
        val brush = gradient?.let { brushFor(argb, part, it) }
        val color = Color(argb)
        val alpha = part.alpha
        when (part) {
            is CarPart.Box -> {
                val topLeft = Offset(part.left, part.top)
                val size = Size(part.width, part.height)
                if (part.corner > 0f) {
                    val radius = CornerRadius(part.corner)
                    if (brush != null) {
                        drawRoundRect(brush, topLeft, size, radius, alpha = alpha)
                    } else {
                        drawRoundRect(color, topLeft, size, radius, alpha = alpha)
                    }
                } else {
                    if (brush != null) {
                        drawRect(brush, topLeft, size, alpha = alpha)
                    } else {
                        drawRect(color, topLeft, size, alpha = alpha)
                    }
                }
            }

            is CarPart.Disc -> {
                val center = Offset(part.centerX, part.centerY)
                if (brush != null) {
                    drawCircle(brush, part.radius, center, alpha = alpha)
                } else {
                    drawCircle(color, part.radius, center, alpha = alpha)
                }
            }

            is CarPart.Wedge -> {
                val path = Path().apply {
                    part.points.forEachIndexed { index, point ->
                        if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                    }
                    close()
                }
                if (brush != null) {
                    drawPath(path, brush, alpha = alpha)
                } else {
                    drawPath(path, color, alpha = alpha)
                }
            }
        }
    }
}

/**
 * Sprite yolu: gri govde katmani secili boyayla CARPILIR, uzerine renkli
 * detay katmani cizilir. Ikisi de [CarCatalog]'un cizim kutusunun TAMAMINA
 * oturtulur — sprite'lar ayni oranda uretildigi ve dar araclar saydam dolguyla
 * ortalandigi icin bu tek dikdortgen dogru hizalamayi veriyor.
 *
 * Carpim ([BlendMode.Modulate]) secildi cunku gri katman isik/golge bilgisini
 * 0..1 arasi bir carpan olarak tutuyor: `boya x golge` dogru gölgeli boyayi
 * verir. Duz [BlendMode.SrcIn] tinti butun hacmi duz renge cevirirdi.
 *
 * Kutu GOVDENIN KENDI SINIFINDAN gelir ([CarShapeDef.boxLeft] vb.), tek bir
 * sabitten degil.
 *
 * 2026-08-17'de burasi [CarCatalog.ART_LEFT] ailesini kullaniyordu ve o
 * degerler [VehicleClass.BINEK]'in kutusu (40x76). Ayni gun eklenen
 * motosiklet (22x59) ve tir (48x202) sprite'lari bu yuzden BINEK kutusuna
 * SIKISTIRILARAK ciziliyordu: motosiklet enine gerilmis, tir boyuna
 * ezilmis olurdu. Sprite hatti dosyalari dogru oranda uretiyordu, hata
 * yalnizca cizim tarafindaydi.
 *
 * Kutu sinirlari tam sayi ([drawImage] tam sayi istiyor) ve sinif tanimlari
 * bilerek yuvarlak secildi, o yuzden yuvarlama kaybi yok.
 */
private fun DrawScope.drawCarSprite(style: CarStyle, sprite: CarSprite) {
    val shape = style.shape
    val left = shape.boxLeft.roundToInt()
    val top = shape.boxTop.roundToInt()
    val offset = IntOffset(left, top)
    val size = IntSize(shape.boxRight.roundToInt() - left, shape.boxBottom.roundToInt() - top)
    drawImage(
        image = sprite.body,
        dstOffset = offset,
        dstSize = size,
        // Tint ONBELLEKTEN: bkz. [tintCache]. Renk basina tek nesne, tek native
        // filtre — kare basina yeniden kurulmuyor.
        colorFilter = tintFor(style.color.bodyArgb)
    )
    drawImage(image = sprite.detail, dstOffset = offset, dstSize = size)
}

/**
 * Aracin govdesini cizer: sprite varsa sprite, yoksa katalog geometrisi.
 *
 * Cagiranlarin hangisinin kullanildigini bilmesi GEREKMEZ; golge ve alev her
 * iki yolda da ayni yerden geliyor.
 */
fun DrawScope.drawCarBody(style: CarStyle, sprites: CarSpriteSet?) {
    val sprite = sprites?.of(style.shape.id)
    if (sprite != null) drawCarSprite(style, sprite) else drawCarParts(style)
}

/**
 * Boost alevi. Dort katman: halo + dis pluma + ic pluma + sicak cekirdek.
 *
 * Her pluma ucuna dogru hem DARALIR hem SAYDAMLASIR — kenarlarin sonumlenmesi
 * buradan geliyor. Eski cizim iki duz ucgendi ve sahibin ifadesiyle "lego
 * gibi" duruyordu (2026-08-15). Alev yalnizca cizimdir, carpismaya girmez.
 *
 * [phase] saniye cinsinden bir zaman; iki farkli frekansta sinus ile
 * titrestirilir ki tekrar mekanik hissettirmesin. Varsayilani 0 — durgun
 * onizlemeler (garaj) icin.
 */
fun DrawScope.drawCarBoostFlame(style: CarStyle, phase: Float = 0f) {
    val root = style.shape.artBottom + CarCatalog.FLAME_ROOT_OFFSET
    val flicker = 1f + 0.13f * sin(phase * 17f) + 0.07f * sin(phase * 27.3f)
    val wobble = 1f + 0.09f * sin(phase * 21f + 1.7f)

    // Butun alev KOK NOKTASINA tasinir. Kok govdeden govdeye degistigi icin
    // (artBottom) eskiden halo merkezi de her cagrida farkliydi ve gradyan
    // yeniden kurulmak zorundaydi. Cevirmeden sonra halo ile plumalar sabit
    // sayilarla ifade edilebiliyor, boylece fircalar birer kez kuruluyor.
    translate(0f, root) {
        // 1) Halo: yumusak kenari veren radyal sonum.
        drawCircle(
            brush = FLAME_HALO_BRUSH,
            radius = CarCatalog.FLAME_HALO_RADIUS,
            center = FLAME_HALO_CENTER
        )

        // 2-4) Plumalar: kok genis ve opak, uc sivri ve saydam.
        plume(
            CarCatalog.FLAME_OUTER_HALF_WIDTH * wobble,
            CarCatalog.FLAME_OUTER_LENGTH * flicker,
            FLAME_OUTER_BRUSH
        )
        val innerLength = CarCatalog.FLAME_INNER_LENGTH * (1f + 0.10f * sin(phase * 23f + 0.6f))
        plume(
            CarCatalog.FLAME_INNER_HALF_WIDTH * wobble, innerLength,
            FLAME_INNER_BRUSH
        )
        plume(
            CarCatalog.FLAME_INNER_HALF_WIDTH * wobble * 0.52f, innerLength * 0.62f,
            FLAME_CORE_BRUSH
        )
    }
}

/**
 * Pluma yolu NORMALIZE uzayda: kok `y = 0`, uc `y = 1`, yarim genislik `1`.
 *
 * Eskiden her cagrida yeni bir [Path] kuruluyordu (kare basina 3 native path
 * nesnesi + 12 segment cagrisi) cunku kok/genislik/uzunluk her karede
 * titresiyor. Sekil aslinda hep AYNI — degisen yalnizca olcek. Bu yuzden yol
 * bir kez kuruluyor, [plume] icindeki `scale` istenen olcuye goturuyor.
 * Noktalar birebir eski formulun (`-halfWidth`, `root + length * 0.55f` ...)
 * `halfWidth = 1, length = 1, root = 0` halidir, yani geometri degismedi.
 */
private val PLUME_PATH = Path().apply {
    moveTo(-1f, 0f)
    // Kenarlar duz degil hafif ic bukey: sivri uc daha organik okunuyor.
    quadraticTo(-0.72f, 0.55f, 0f, 1f)
    quadraticTo(0.72f, 0.55f, 1f, 0f)
    close()
}

/**
 * Kokten uca saydamlasan pluma gradyani, normalize uzayda (`0` kok, `1` uc).
 *
 * Duraklar ve alfa egrisi eskisiyle BIREBIR ayni; tek fark `startY/endY`nin
 * mutlak `root..tip` yerine `0..1` olmasi. Gradyan cizim anindaki donusum
 * matrisiyle birlikte olceklendigi icin ekranda ayni yere dusuyor.
 */
private fun plumeBrush(argb: Long, alpha: Float): Brush {
    val color = Color(argb)
    return Brush.verticalGradient(
        colorStops = arrayOf(
            0f to color.copy(alpha = alpha),
            0.45f to color.copy(alpha = alpha * 0.55f),
            1f to color.copy(alpha = 0f)
        ),
        startY = 0f,
        endY = 1f
    )
}

// Uc pluma, uc SABIT (renk, alfa) cifti — onbellege gerek yok, dogrudan
// birer alan. [Brush] shader'ini kendi icinde tuttugu icin ayni ORNEGI
// yeniden kullanmak native shader'in da yeniden uretilmesini engelliyor.
private val FLAME_OUTER_BRUSH = plumeBrush(CarCatalog.FLAME_OUTER_ARGB, 0.84f)
private val FLAME_INNER_BRUSH = plumeBrush(CarCatalog.FLAME_INNER_ARGB, 0.92f)
private val FLAME_CORE_BRUSH = plumeBrush(CarCatalog.FLAME_CORE_ARGB, 0.98f)

/** Halo merkezi — kok noktasina cevrilmis uzayda sabit. */
private val FLAME_HALO_CENTER = Offset(0f, CarCatalog.FLAME_HALO_RADIUS * 0.42f)

private val FLAME_HALO_BRUSH = Color(CarCatalog.FLAME_OUTER_ARGB).let { outer ->
    Brush.radialGradient(
        colorStops = arrayOf(
            0f to outer.copy(alpha = 0.30f),
            0.45f to outer.copy(alpha = 0.13f),
            1f to outer.copy(alpha = 0f)
        ),
        center = FLAME_HALO_CENTER,
        radius = CarCatalog.FLAME_HALO_RADIUS
    )
}

/**
 * Tek bir alev plumasi. Normalize [PLUME_PATH] istenen yarim genislik ve
 * uzunluga olceklenir; gradyan da ayni donusumden gectigi icin kokten uca
 * sonumlenme aynen korunur.
 *
 * Olcek NON-UNIFORM (x ve y farkli) ama sorun degil: yol yalnizca dolduruluyor,
 * kontur yok — dolayisiyla cizgi kalinligi bozulmuyor.
 */
private fun DrawScope.plume(halfWidth: Float, length: Float, brush: Brush) {
    if (length <= 0f || halfWidth <= 0f) return
    scale(halfWidth, length, pivot = Offset.Zero) {
        drawPath(path = PLUME_PATH, brush = brush)
    }
}

/**
 * Oyuncu araci: golge + gövde + (boost basiliysa) alev.
 * [GameConfig.CAR_ART_SCALE] burada uygulanir — carpisma kutusu ayni sabitten
 * turetildigi icin gorsel ile kutu birebir ortusur.
 */
fun DrawScope.drawStyledCar(
    x: Float,
    y: Float,
    style: CarStyle,
    boosting: Boolean,
    flamePhase: Float = 0f,
    sprites: CarSpriteSet? = null
) {
    translate(x, y) {
        scale(GameConfig.CAR_ART_SCALE, GameConfig.CAR_ART_SCALE, pivot = Offset.Zero) {
            drawCarShadowIfVector(style, sprites)
            drawCarBody(style, sprites)
            if (boosting) drawCarBoostFlame(style, flamePhase)
        }
    }
}

/**
 * Garaj onizlemesi. Arac, verilen alana golgesiyle birlikte ORTALANIR ve
 * sigacak kadar buyutulur.
 *
 * ## Kutu SABIT DEGIL — govdenin kendi olcu sinifindan gelir
 *
 * Burasi 2026-08-18'e kadar [CarCatalog.ART_LEFT] ailesini kullaniyordu ve o
 * degerler [VehicleClass.BINEK]'in kutusu (40x76). Sekiz govde de binek
 * oldugu surece dogruydu; **2026-08-17'de motosiklet (22x59) ve tir (48x202)
 * eklenince bozuldu**.
 *
 * Ayni gun [drawCarSprite] her govdeyi KENDI sinif kutusuna cizmeye
 * gecirilmisti (commit `3411a4b`) ama onizleme yolu ayni degisiklikten
 * gecmemisti. Sonuc: sigdirma 76 birime gore hesaplaniyor, cizim 202 birim
 * yapiyordu — **2,66 kat tasma**. Tir garajda kendi karesinden tasip ekranin
 * yarisini kapliyordu (sahibi ekran goruntusuyle bildirdi, 2026-08-18).
 * Motosiklet ters yonde etkileniyordu: 59 birimlik govde 76 birimlik kutuya
 * gore sigdirildigi icin gereginden kucuk cikiyordu.
 *
 * Golge yalnizca VEKTOR yolunda hesaba katilir. [drawCarShadowIfVector]
 * sprite varken zaten cizmiyor; kutuya her zaman katsaydik golge sabit 42x68
 * oldugu icin 22 birimlik motosikleti bosuna kuculturdu.
 */
@Composable
fun CarPreview(
    style: CarStyle,
    modifier: Modifier = Modifier,
    boosting: Boolean = false,
    // Varsayilan degeri Composable: cagiranlarin sprite'i ayrica tasimasi
    // gerekmiyor, garaj/harita/menu ekranlari degismeden sprite'a gecti.
    sprites: CarSpriteSet = rememberCarSprites()
) {
    Canvas(modifier = modifier) {
        // Onizleme kutusu = GOVDENIN KENDI sinif kutusu (+ vektor yolunda golge).
        val shape = style.shape
        val vector = sprites.of(shape.id) == null
        val shadowRight = CarCatalog.SHADOW_LEFT + CarCatalog.SHADOW_WIDTH
        val shadowBottom = CarCatalog.SHADOW_TOP + CarCatalog.SHADOW_HEIGHT
        val left = if (vector) minOf(shape.boxLeft, CarCatalog.SHADOW_LEFT) else shape.boxLeft
        val right = if (vector) maxOf(shape.boxRight, shadowRight) else shape.boxRight
        val top = if (vector) minOf(shape.boxTop, CarCatalog.SHADOW_TOP) else shape.boxTop
        val bottom = if (vector) maxOf(shape.boxBottom, shadowBottom) else shape.boxBottom
        val artWidth = right - left
        val artHeight = bottom - top
        if (artWidth <= 0f || artHeight <= 0f) return@Canvas

        val fit = minOf(size.width / artWidth, size.height / artHeight)
        translate(size.width / 2f, size.height / 2f) {
            scale(fit, fit, pivot = Offset.Zero) {
                translate(-(left + right) / 2f, -(top + bottom) / 2f) {
                    drawCarShadowIfVector(style, sprites)
                    drawCarBody(style, sprites)
                    if (boosting) drawCarBoostFlame(style)
                }
            }
        }
    }
}
