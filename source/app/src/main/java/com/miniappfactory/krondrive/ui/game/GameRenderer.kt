package com.miniappfactory.krondrive.ui.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import com.miniappfactory.krondrive.game.CarCatalog
import com.miniappfactory.krondrive.game.GameConfig
import com.miniappfactory.krondrive.game.GameEngine
import com.miniappfactory.krondrive.game.RoadTheme
import com.miniappfactory.krondrive.ui.common.CarSpriteSet
import com.miniappfactory.krondrive.ui.common.drawCarBody
import com.miniappfactory.krondrive.ui.common.drawCarShadowIfVector
import com.miniappfactory.krondrive.ui.common.drawStyledCar
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Sahnenin cizimi. Prototipteki `drawSideBackgrounds` / `drawTrack` /
 * `drawSpeedometer` / `drawCar` / `drawCoin` / `drawParticles` fonksiyonlarinin
 * BIREBIR karsiligi — ayni koordinatlar, ayni renkler, ayni katman sirasi.
 *
 * Koordinat sistemi: motor "dp uzayinda" calisir (HTML'in CSS pikseli ile ayni
 * olcek). Cizerken tum sahne [density] ile olceklenir; boylece oyun her ekran
 * yogunlugunda AYNI buyuklukte gorunur — ham piksel kullansaydik yuksek DPI
 * telefonlarda yol ve araclar minicik kalirdi.
 */
fun DrawScope.drawGameScene(
    engine: GameEngine,
    density: Float,
    textMeasurer: TextMeasurer,
    gaugeValueSize: TextUnit,
    gaugeLabelSize: TextUnit,
    gaugeSmallSize: TextUnit,
    // Sprite kumesi disaridan gelir: yukleme Composable bir istir, cizim ise
    // her karede yurur. null verilirse cizim vektor yoluna duser.
    sprites: CarSpriteSet? = null
) {
    scale(density, density, pivot = Offset.Zero) {
        drawSideBackgrounds(engine)
        drawTrack(engine)
    }
    drawSpeedometer(engine, density, textMeasurer, gaugeValueSize, gaugeLabelSize, gaugeSmallSize)
    scale(density, density, pivot = Offset.Zero) {
        engine.coins.forEach { drawCoin(it) }
        engine.obstacles.forEach {
            drawObstacleCar(
                it.x,
                it.y,
                GameEngine.OBSTACLE_COLORS[it.colorIndex].toLong() and 0xFFFFFFFFL,
                sprites
            )
        }
        drawNightHeadlights(engine)
        // Dokunulmazlik sirasinda arac yanip soner (Second Chance / reklamla devam).
        val blink = engine.isInvulnerable() && ((engine.timeElapsed * 10f).toInt() % 2 == 0)
        if (!blink) {
            // Oyuncu araci garajda secilen sekil + boya ile cizilir. Trafik
            // ayni cizici ile ama SABIT bir sekil/palet ile cizilir
            // (drawObstacleCar) — tehdidin gorunumu her kosuda ayni olmali.
            drawStyledCar(
                x = engine.playerX,
                y = engine.playerY,
                style = engine.carStyle,
                boosting = engine.boosting,
                // Alevin titremesi kosunun zamanindan beslenir; ayri bir
                // animasyon durumu tutmuyoruz (motor zaten her kare adiyor).
                flamePhase = engine.timeElapsed,
                sprites = sprites
            )
        }
        drawParticles(engine)
    }
}

// ---------------------------------------------------------------------------
// Cizim onbellekleri
// ---------------------------------------------------------------------------
//
// NEDEN: sahne 60 Hz'de yeniden ciziliyor ve desenler (seyirci, kerb, serit,
// sehir isiklari) YUZLERCE kucuk dikdortgenden olusuyor. Iki ayri maliyet
// vardi:
//
//  1. Cizim cagrisi basina sabit gider. Her `drawRect` paint'i yeniden
//     yapilandirip display-list'e ayri bir komut yaziyor; CROWD temasinda bu
//     kare basina 400'un uzerinde komut demekti. Ayni renkteki bloklari tek
//     bir [Path]'te toplayip renk basina TEK `drawPath` yapmak ayni pikselleri
//     uretir (bloklar birbirine DEGMIYOR — asagida her cagri yerinde
//     gerekcesi yazili), ama komut sayisini renk sayisina indirir.
//
//  2. Tahsis. Her karede yeni [Path] / [Brush] / [TextStyle] uretiliyordu.
//     [Brush] shader'ini kendi ORNEGI icinde onbelleklediginden her kare yeni
//     ornek uretmek onbellegi bosa cikariyor ve native shader her kare bastan
//     kuruluyordu.
//
// Onbellekler dosya seviyesinde: Compose cizimi tek is parcaciginda (main)
// yurudugu icin senkronizasyon gerekmiyor. Ayni gerekce
// [com.miniappfactory.krondrive.ui.common] icindeki firca onbellegi icin de
// yazili. [Path] nesneleri her karede `rewind()` ile SIFIRLANIR, yeniden
// tahsis edilmez — `reset()` degil `rewind()`, cunku rewind ayrilmis belleği
// koruyup sadece nokta sayacini sifirlar.
private object ScenePaths {
    /** Seyirci bloklari — [CROWD_COLORS] ile ayni sirada, renk basina bir yol. */
    val crowd = Array(CROWD_COLORS_SIZE) { Path() }

    /** Gece sehir isiklari: 0 = sicak (sari), 1 = soguk (mavi). */
    val cityLight = Array(2) { Path() }

    /** Kerb: 0 = kirmizi, 1 = beyaz. */
    val kerb = Array(2) { Path() }

    val laneDash = Path()
    val speck = Path()

    /** Yol kenari egik cizgileri (cim / kopuk / gece) — tek renk, tek yol. */
    val sideStripe = Path()

    val flagPole = Path()
    val flagCloth = Path()
}

private const val CROWD_COLORS_SIZE = 4

/** [crowdColorIndex] ile birebir ayni sira. */
private val CROWD_COLORS = arrayOf(
    Color(0xFFFFFFFF),
    Color(0xFFD62828),
    Color(0xFF1D3557),
    Color(0xFF111111)
)

private val CITY_LIGHT_COLORS = arrayOf(Color(0xFFFFD54F), Color(0xFF90CAF9))

private val KERB_COLORS = arrayOf(Color(0xFFC8393B), Color(0xFFDCE2E9))

// Stroke/Brush nesneleri de her karede yeniden uretilmesin diye sabit.
private val THIN_STROKE = Stroke(width = 2f)

/**
 * Eksene paralel bir blok ekler. `addRect(Rect)` yerine elle kose cizmenin
 * sebebi tahsis: [androidx.compose.ui.geometry.Rect] normal bir siniftir ve
 * kare basina yuzlerce blok icin yuzlerce kisa omurlu nesne demektir.
 * moveTo/lineTo dogrudan native yola yaziyor, hic nesne uretmiyor.
 *
 * Kose sirasi tum cagrilarda ayni yonde (saat yonu); NonZero doldurmada
 * bitisik olmayan alt-yollar tek tek cizilmis dikdortgenlerle ayni kapsama
 * degerini verir.
 */
private fun Path.addBlockRect(left: Float, top: Float, width: Float, height: Float) {
    val right = left + width
    val bottom = top + height
    moveTo(left, top)
    lineTo(right, top)
    lineTo(right, bottom)
    lineTo(left, bottom)
    close()
}

/** Tek bir cizgi parcasi (stroke ile cizilecek yollar icin). */
private fun Path.addSegment(x0: Float, y0: Float, x1: Float, y1: Float) {
    moveTo(x0, y0)
    lineTo(x1, y1)
}

// Gece arkaplan gradyani yalnizca yuksekligie bagli; viewport sabit oldugu
// surece tek ornek her karede yeniden kullanilir.
private var nightSkyHeight = Float.NaN
private var nightSkyBrush: Brush? = null

private fun nightSkyBrush(h: Float): Brush {
    val cached = nightSkyBrush
    if (cached != null && nightSkyHeight == h) return cached
    val brush = Brush.verticalGradient(
        listOf(Color(0xFF07111F), Color(0xFF0F2138)),
        startY = 0f,
        endY = h
    )
    nightSkyBrush = brush
    nightSkyHeight = h
    return brush
}

// ---------------------------------------------------------------------------
// Yol kenari temalari
// ---------------------------------------------------------------------------

private fun DrawScope.drawSideBackgrounds(engine: GameEngine) {
    val w = engine.viewWidth
    val h = engine.viewHeight
    val leftW = engine.roadX
    val rightX = engine.roadX + engine.roadWidth
    val rightW = w - rightX
    if (leftW <= 0f || h <= 0f) return

    when (engine.theme) {
        RoadTheme.GRASS -> {
            drawRect(Color(0xFF92D050), Offset(0f, 0f), Size(leftW, h))
            drawRect(Color(0xFF92D050), Offset(rightX, 0f), Size(rightW, h))
            // Cim cizgileri tek renk ve tek kalinlikta; hepsi tek stroke
            // yolunda toplaniyor. Sol ve sag parcalar ayri x araliklarinda
            // (leftW < rightX), ardisik siralar 120 birim arayla ve yalnizca
            // 18 birim yukseliyor — hicbir parca digerine degmiyor, bu yuzden
            // yari saydam renk cift boyanmiyor.
            val path = ScenePaths.sideStripe
            path.rewind()
            var y = -120f + (engine.roadOffset * 0.22f) % 120f
            while (y < h + 120f) {
                path.addSegment(0f, y, leftW, y + 18f)
                path.addSegment(rightX, y + 18f, w, y)
                y += 120f
            }
            drawPath(path, Color(0x0FFFFFFF), style = THIN_STROKE)
        }

        RoadTheme.BEACH -> {
            drawRect(Color(0xFFEED9A0), Offset(0f, 0f), Size(leftW, h))
            drawRect(Color(0xFFEED9A0), Offset(rightX, 0f), Size(rightW, h))
            val waterW = max(22f, (leftW * 0.2f))
            drawRect(Color(0xFF5AC8FA), Offset(0f, 0f), Size(waterW, h))
            drawRect(Color(0xFF5AC8FA), Offset(w - waterW, 0f), Size(waterW, h))
            // Kopuk parcalari: sol ve sag su seridinin uzerinde, 80 birim
            // arayla, en fazla 28 birim uzunlukta — ust uste binmiyorlar.
            val path = ScenePaths.sideStripe
            path.rewind()
            var y = -80f + (engine.roadOffset * 0.3f) % 80f
            while (y < h + 80f) {
                path.addSegment(waterW - 3f, y, waterW + 2f, y + 18f)
                path.addSegment(w - waterW + 3f, y + 10f, w - waterW - 2f, y + 28f)
                y += 80f
            }
            drawPath(path, Color(0x61FFFFFF), style = THIN_STROKE)
        }

        RoadTheme.CROWD -> {
            drawRect(Color(0xFF6EA04C), Offset(0f, 0f), Size(leftW, h))
            drawRect(Color(0xFF6EA04C), Offset(rightX, 0f), Size(rightW, h))
            // Pist bariyerleri
            drawRect(Color(0xFFCFD8DC), Offset(leftW - 10f, 0f), Size(10f, h))
            drawRect(Color(0xFFCFD8DC), Offset(rightX, 0f), Size(10f, h))
            // Piksel seyirci bloklari.
            //
            // Renk EKRAN y'sine degil DUNYA y'sine bagli. Eskiden kayan `y`
            // veriliyordu: sira asagi aktikca renk indeksi de degisiyor ve her
            // seyirci saniyede onlarca kez renk atiyordu (titreme). Dunya
            // koordinati bir siranin omru boyunca sabit oldugu icin seyirci
            // artik kendi rengini koruyarak kayiyor.
            //
            // Bloklar RENGE GORE toplanip renk basina tek `drawPath` ile
            // ciziliyor (eskiden blok basina bir `drawRect` vardi). Guvenli,
            // cunku bloklar birbirine DEGMIYOR: yatayda 3 birim genislik / 6
            // birim adim (3 birim bosluk), dikeyde 5 birim yukseklik / 42
            // birim adim. Ust uste binme olmadigi icin cizim SIRASI da fark
            // etmiyor — renkler zaten opak.
            val crowdShift = engine.roadOffset * 0.35f
            val paths = ScenePaths.crowd
            for (p in paths) p.rewind()
            var y = -42f + crowdShift % 42f
            while (y < h + 42f) {
                val rowKey = y - crowdShift
                var x = 3f
                while (x < leftW - 14f) {
                    paths[crowdColorIndex(x, rowKey)].addBlockRect(x, y, 3f, 5f)
                    x += 6f
                }
                x = rightX + 14f
                while (x < w - 3f) {
                    paths[crowdColorIndex(x, rowKey)].addBlockRect(x, y + 9f, 3f, 5f)
                    x += 6f
                }
                y += 42f
            }
            for (i in paths.indices) {
                val p = paths[i]
                if (!p.isEmpty) drawPath(p, CROWD_COLORS[i])
            }

            // Bayraklar: once TUM direkler, sonra TUM bezler.
            //
            // Sira onemli — bez direge biniyor (ucgen x..x+10 araliginda,
            // direk x-1..x+1) ve eskiden de bez direkten SONRA ciziliyordu.
            // Toplu cizimde de ayni sira korunuyor, boylece bindirme aynen
            // eskisi gibi gorunuyor. Farkli bayraklar birbirine degmiyor
            // (yatayda ekranin iki ucu, dikeyde 170 birim ara).
            val pole = ScenePaths.flagPole
            val cloth = ScenePaths.flagCloth
            pole.rewind()
            cloth.rewind()
            var flagY = -170f + (engine.roadOffset * 0.18f) % 170f
            while (flagY < h + 170f) {
                addMiniFlag(pole, cloth, leftW * 0.18f, flagY + 28f)
                addMiniFlag(pole, cloth, rightX + rightW * 0.82f, flagY + 76f)
                flagY += 170f
            }
            if (!pole.isEmpty) drawPath(pole, Color(0xFF444444), style = THIN_STROKE)
            if (!cloth.isEmpty) drawPath(cloth, Color(0xFFFFEB3B))
        }

        RoadTheme.NIGHT -> {
            val gradient = nightSkyBrush(h)
            drawRect(gradient, Offset(0f, 0f), Size(leftW, h))
            drawRect(gradient, Offset(rightX, 0f), Size(rightW, h))
            // Gece cizgileri: sol ve sag ayri x araliklarinda, siralar 105
            // birim arayla ve 12 birim yukseliyor — degme yok.
            val stripes = ScenePaths.sideStripe
            stripes.rewind()
            var y = -105f + (engine.roadOffset * 0.28f) % 105f
            while (y < h + 105f) {
                stripes.addSegment(0f, y, leftW, y + 12f)
                stripes.addSegment(rightX, y + 12f, w, y)
                y += 105f
            }
            drawPath(stripes, Color(0x2E56E9FF), style = THIN_STROKE)

            // Uzaktaki sehir isiklari — seyirci bloklariyla ayni mantik:
            // 3 birim genislik / 22 birim adim, 7 birim yukseklik / 88 birim
            // adim. Degme yok, renk basina tek cagri.
            val lights = ScenePaths.cityLight
            for (p in lights) p.rewind()
            var lightY = -88f + (engine.roadOffset * 0.4f) % 88f
            while (lightY < h + 88f) {
                var x = 10f
                while (x < leftW - 8f) {
                    lights[cityLightIndex(x)].addBlockRect(x, lightY, 3f, 7f)
                    x += 22f
                }
                x = rightX + 8f
                while (x < w - 10f) {
                    lights[cityLightIndex(x)].addBlockRect(x, lightY + 12f, 3f, 7f)
                    x += 22f
                }
                lightY += 88f
            }
            for (i in lights.indices) {
                val p = lights[i]
                if (!p.isEmpty) drawPath(p, CITY_LIGHT_COLORS[i])
            }
        }
    }
}

/**
 * Seyirci renginin [CROWD_COLORS] icindeki indeksi. [rowKey] EKRAN y'si degil
 * DUNYA y'sidir (bkz. cagri yeri) — aksi halde renk her karede degisir.
 *
 * `mod` kullaniliyor, `%` degil: dunya koordinati negatife gidiyor ve `%`
 * negatif sonuc verip butun seyircileri son dala (siyah) dusuruyordu.
 */
private fun crowdColorIndex(x: Float, rowKey: Float): Int =
    // `floor` DEGIL `roundToInt`: `rowKey` daima 42'nin, `x` 3'un katidir,
    // yani `x + rowKey / 6f` TAM SAYIYA oturur ve floor bicak sirtinda kalir —
    // en kucuk kayan nokta hatasi indeksi bir kaydirir ve blok renk atlar.
    // Cihazda goruldu (2026-08-16): ekranin ust %70'i durgunken alt bantta
    // ~%20 oraninda kalinti titreme vardi ve bozulmalarin TAMAMI +-1 indeks
    // kaymasiydi. Deger zaten tam sayi oldugu icin yuvarlamak niyeti birebir
    // verir ve epsilon'a bagisik olur.
    (x + rowKey / 6f).roundToInt().mod(CROWD_COLORS_SIZE)

/** 0 = sicak sari, 1 = soguk mavi (bkz. [CITY_LIGHT_COLORS]). */
private fun cityLightIndex(x: Float): Int = if (x.toInt() % 44 == 0) 0 else 1

/**
 * Bayragi cizmez, iki toplu yola EKLER: direk stroke yoluna, bez dolgu
 * yoluna. Boylece kare basina bayrak sayisi kadar degil, toplam iki cizim
 * cagrisi yapilir.
 */
private fun addMiniFlag(pole: Path, cloth: Path, x: Float, y: Float) {
    pole.addSegment(x, y, x, y + 20f)
    cloth.moveTo(x, y)
    cloth.lineTo(x + 10f, y + 4f)
    cloth.lineTo(x, y + 8f)
    cloth.close()
}

// ---------------------------------------------------------------------------
// Yol
// ---------------------------------------------------------------------------

private fun DrawScope.drawTrack(engine: GameEngine) {
    val h = engine.viewHeight
    if (h <= 0f) return
    drawRect(Color(0xFF3A4048), Offset(engine.roadX, 0f), Size(engine.roadWidth, h))

    // Kirmizi/beyaz kerb bloklari. Blok boyu ve renk kontrasti bilerek
    // dusuruldu — bkz. GameConfig.KERB_BLOCK_HEIGHT_PX (goz yorgunlugu).
    //
    // Bloklar dikeyde BITISIK ama renkleri donusumlu: ayni renkteki iki blok
    // arasinda daima karsi renkten bir blok var, yani AYNI yola komsu blok
    // girmiyor ve toplu cizimde dikis olusmuyor. Iki renk opak ve ust uste
    // binmedigi icin "once tum kirmizilar, sonra tum beyazlar" sirasi
    // gorunumu degistirmiyor.
    // KERB GERCEKTEN KAYAR (2026-08-16). Onceki hali bloklari SABIT bir
    // izgaraya koyuyor, `roadOffset` ile yalnizca rengi cevirmiyordu; sonuc
    // kayan bir bordur degil, YERINDE FAZ ATLAYAN bir bordurdu. Cihazda
    // olculdu: kirmizi/beyaz sinirlari 25 kare boyunca ayni y'lerde durdu,
    // butun bloklar toplu halde renk degistirdi. Bu yuzden hizi 1.00'a
    // cikarmak tek basina isi KOTULESTIRIRDI — kayma degil, saniyede birkac
    // kez tum bordurun yanip sonmesi olurdu.
    //
    // Cozum serit cizgileriyle ayni: baslangic y'si `roadOffset` ile kaydirilir,
    // desen asagi akar. Carpan 1.00 (proje sahibi, 2026-08-16): *"kerbler
    // aslinda sabit, araba yanindan geciyor; o hissi vermek icin hareket
    // ettiriyorsun. Kerb hizi araba ile ayni olmali ki araba hizlandikca
    // hizlansin."* Paralaks DERINLIKTEN dogar, yanal mesafeden degil; kerb
    // yola boyalidir, kameradan serit cizgisiyle ayni uzakliktadir.
    val kerbPaths = ScenePaths.kerb
    for (p in kerbPaths) p.rewind()
    val block = GameConfig.KERB_BLOCK_HEIGHT_PX
    val kerbPeriod = block * 2f
    var y = -kerbPeriod + (engine.roadOffset % kerbPeriod)
    while (y < h + kerbPeriod) {
        // Bir periyot = bir kirmizi + bir beyaz blok. Ikisini birlikte
        // yerlestirmek pariteyi konumdan bagimsiz kilar; eski kod pariteyi
        // `floor()` ile hesapliyordu ve negatif y'de ters donuyordu.
        kerbPaths[0].addBlockRect(engine.roadX - 8f, y, 8f, block)
        kerbPaths[0].addBlockRect(engine.roadX + engine.roadWidth, y, 8f, block)
        kerbPaths[1].addBlockRect(engine.roadX - 8f, y + block, 8f, block)
        kerbPaths[1].addBlockRect(engine.roadX + engine.roadWidth, y + block, 8f, block)
        y += kerbPeriod
    }
    for (i in kerbPaths.indices) {
        val p = kerbPaths[i]
        if (!p.isEmpty) drawPath(p, KERB_COLORS[i])
    }

    // Kesik serit cizgileri: daha uzun ve daha seyrek, biraz da soluk.
    //
    // BLOK BLOK ciziliyor, `dashPathEffect` ile DEGIL. Sebep: Android'in
    // donanim hizlandirmali canvas'i `drawLine` icin PathEffect'i **API
    // 28'den once sessizce yok sayar** ve cizgi DUZ cikar. Proje minSdk 24;
    // test cihazinda (SM-G950F, Android 7.0 / API 24) seritler kesiksiz
    // goruntulendi ve yol olu duruyordu — proje sahibi 2026-08-16'da bildirdi,
    // ekran goruntusunun pikselleri olculerek dogrulandi (1500 px kesintisiz
    // beyaz sutun). Elle blok cizmek her API'de ayni sonucu veriyor; hemen
    // yukaridaki kerb deseni zaten boyle ciziliyor.
    //
    // Bloklar tek renkte oldugu icin hepsi TEK yolda toplanip tek cagriyla
    // ciziliyor. Renk yari saydam (0xC2) — bindirme olsa cift boyanirdi, ama
    // yok: dikeyde 42 birim blok / 96 birim periyot (54 birim bosluk),
    // yatayda seritler serit genisligi kadar ayri.
    val on = GameConfig.LANE_DASH_ON_PX
    val off = GameConfig.LANE_DASH_OFF_PX
    val period = on + off
    val laneStroke = 5f
    val dashPath = ScenePaths.laneDash
    dashPath.rewind()
    var dashY = -period + (engine.roadOffset % period)
    while (dashY < h) {
        for (i in 1 until GameConfig.LANE_COUNT) {
            val x = engine.roadX + engine.laneWidth * i
            dashPath.addBlockRect(x - laneStroke / 2f, dashY, laneStroke, on)
        }
        dashY += period
    }
    if (!dashPath.isEmpty) drawPath(dashPath, Color(0xC2FFFFFF))

    // Asfalt dokusu (cok soluk noktalar). Tek renk, tek cagri.
    // Noktalarin x'i `(i * 37) % roadWidth` ile dagiliyor; 24 nokta icin en
    // yakin iki x arasindaki fark nokta genisliginden (3) buyuk, yani
    // bindirme yok.
    val speckPath = ScenePaths.speck
    speckPath.rewind()
    for (i in 0 until 24) {
        val sx = engine.roadX + (i * 37f) % engine.roadWidth
        val sy = (i * 83f + engine.roadOffset * 1.4f) % h
        speckPath.addBlockRect(sx, sy, 3f, 3f)
    }
    drawPath(speckPath, Color(0x08FFFFFF))
}

// ---------------------------------------------------------------------------
// Hiz gostergesi (sol serit disinda, prototipteki drawSpeedometer)
// ---------------------------------------------------------------------------

/**
 * Gosterge zemininin lineer gradyani. Merkez ve yaricap yalnizca viewport ile
 * yogunluga bagli, yani kosu boyunca SABIT — ama eskiden her karede yeni bir
 * [Brush] uretiliyor ve native shader her karede bastan kuruluyordu. Tek
 * slotluk onbellek pratikte her karede isabet ediyor.
 */
private var gaugeBrushCx = Float.NaN
private var gaugeBrushCy = Float.NaN
private var gaugeBrushR = Float.NaN
private var gaugeBrush: Brush? = null

private fun gaugeBackdropBrush(cx: Float, cy: Float, r: Float): Brush {
    val cached = gaugeBrush
    if (cached != null && gaugeBrushCx == cx && gaugeBrushCy == cy && gaugeBrushR == r) {
        return cached
    }
    val brush = Brush.linearGradient(
        listOf(Color(0x2E061226), Color(0x8C000C1C)),
        start = Offset(cx - r, cy - r),
        end = Offset(cx + r, cy + r)
    )
    gaugeBrush = brush
    gaugeBrushCx = cx
    gaugeBrushCy = cy
    gaugeBrushR = r
    return brush
}

/**
 * Tek bir gosterge yazisinin olcum onbellegi.
 *
 * `rememberTextMeasurer()` kendi icinde onbellek tutar ama varsayilan boyutu 8
 * ve anahtari METNI iceriyor; hiz her karede degistigi icin km/h degeri hemen
 * her karede iska geciyordu ve tam bir metin duzeni (font cozumleme + satir
 * kirma) yeniden kuruluyordu. Burada sonuc son cizilen degerle birlikte
 * saklaniyor: rakam degismedigi surece yeniden olcum yok.
 *
 * [TextStyle] de her cagrida yeniden tahsis ediliyordu; artik yalnizca punto
 * degisince kuruluyor. Renk ve italik her cagri yeri icin sabit oldugundan
 * anahtara girmiyor — her yazi kendi slotunu kullanir.
 *
 * Olcum yogunluga ve font olceklemesine bagli; ikisi de [TextMeasurer]
 * ornegine gomulu oldugu ve `rememberTextMeasurer` bunlar degisince YENI bir
 * ornek urettigi icin anahtar olarak olcerin kimligi yetiyor.
 */
private class GaugeTextSlot {
    private var style: TextStyle? = null
    private var styleSize: TextUnit = TextUnit.Unspecified
    private var measurer: TextMeasurer? = null
    private var value: String? = null
    private var cached: TextLayoutResult? = null

    fun layout(
        measurer: TextMeasurer,
        value: String,
        color: Color,
        fontSize: TextUnit,
        italic: Boolean
    ): TextLayoutResult {
        val hit = cached
        if (hit != null && this.value == value && this.measurer === measurer && styleSize == fontSize) {
            return hit
        }
        var s = style
        if (s == null || styleSize != fontSize) {
            s = TextStyle(
                color = color,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
                fontFamily = FontFamily.SansSerif
            )
            style = s
            styleSize = fontSize
        }
        val result = measurer.measure(text = value, style = s)
        this.measurer = measurer
        this.value = value
        cached = result
        return result
    }
}

private val gaugeValueText = GaugeTextSlot()
private val gaugeLabelText = GaugeTextSlot()

private fun DrawScope.drawSpeedometer(
    engine: GameEngine,
    density: Float,
    textMeasurer: TextMeasurer,
    valueSize: TextUnit,
    labelSize: TextUnit,
    smallSize: TextUnit
) {
    if (engine.viewHeight <= 0f) return
    val kmh = engine.speedKmh()
    val pct = kmh / GameConfig.SPEEDOMETER_MAX_KMH

    // Gosterge YOLUN USTUNE TASMAZ ve ekranin ortasinda durmaz.
    // Oyuncu geri bildirimi (2026-08-15): "hiz gostergesi cok buyuk bence ve
    // cok ortada". Eskiden `max(150, roadX)` ile taban genislik zorlaniyordu;
    // dar ekranlarda bu deger yolun ic tarafina tasiyor ve gosterge tam
    // oynanis alaninin uzerine oturuyordu.
    //
    // Artik yalnizca GERCEK sol bosluk (roadX) kullaniliyor: yaricap bosluga
    // gore kirpiliyor, merkez de dairenin sag kenari yola degmeyecek sekilde
    // sinirlaniyor. Dikeyde de ortadan yukari alindi (%32) — orta seride
    // bakan goz artik gostergeye takilmiyor.
    val marginPx = engine.roadX * density
    val r = min(min(marginPx * 0.40f, engine.viewHeight * density * 0.055f), 34f * density)
    if (r <= 8f * density) return
    val cx = min(marginPx * 0.5f, marginPx - r - 4f * density)
    val cy = engine.viewHeight * 0.32f * density

    val startDeg = -0.78f * 180f
    val sweepDeg = 0.78f * 2f * 180f
    val valueSweep = sweepDeg * pct

    // Gostergenin yumusak zemini (halkanin biraz disina tasar).
    drawCircle(
        brush = gaugeBackdropBrush(cx, cy, r),
        radius = r + 7f * density,
        center = Offset(cx, cy)
    )

    val arcTopLeft = Offset(cx - r, cy - r)
    val arcSize = Size(r * 2, r * 2)

    // Cizgi kalinliklari da yaricapla olceklenir; sabit kalirlarsa kucuk
    // gostergede halka tamamen kapanip disk gibi gorunuyordu.
    val trackStroke = r * 0.20f
    val valueStroke = r * 0.16f

    drawArc(
        color = Color(0x14FFFFFF),
        startAngle = startDeg,
        sweepAngle = sweepDeg,
        useCenter = false,
        topLeft = arcTopLeft,
        size = arcSize,
        style = Stroke(width = trackStroke)
    )
    drawArc(
        color = Color(0xFF56E9FF),
        startAngle = startDeg,
        sweepAngle = valueSweep,
        useCenter = false,
        topLeft = arcTopLeft,
        size = arcSize,
        style = Stroke(width = valueStroke, cap = StrokeCap.Round)
    )

    fun text(slot: GaugeTextSlot, value: String, color: Color, fontSize: TextUnit, italic: Boolean, center: Offset) {
        val layout = slot.layout(textMeasurer, value, color, fontSize, italic)
        drawText(
            textLayoutResult = layout,
            topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f)
        )
    }

    // Yalnizca hiz ve birimi. Prototipteki "375nm" (tork) ve "%100" (boost)
    // yazilari KALDIRILDI: tork tamamen susleme bir sayiydi, boost zaten
    // ekranin en ustundeki serit. Gosterge kuculunce ikisi de okunmuyordu.
    text(
        gaugeValueText,
        "$kmh",
        Color(0xFFF8F0E2),
        valueSize,
        italic = true,
        center = Offset(cx, cy - r * 0.10f)
    )
    text(
        gaugeLabelText,
        "KM/H",
        Color(0xFF56E9FF),
        labelSize,
        italic = false,
        center = Offset(cx, cy + r * 0.62f)
    )
}

// ---------------------------------------------------------------------------
// Araclar, coinler, parcaciklar
// ---------------------------------------------------------------------------

/**
 * Trafikteki engel araci.
 *
 * 2026-08-15'te oyuncu araciyla AYNI cizim boru hattina bagladi
 * ([CarCatalog.trafficShape] + [drawCarBody]); onceden burada elle yazilmis
 * bir kopya vardi ve oyuncu araci yenilenince trafik geride kalirdi.
 *
 * Tehdit yine ayirt edilebilir kalir, iki ayri kanaldan:
 *  * palet — govde [GameEngine.OBSTACLE_COLORS]'tan gelir, surucu basi mavi
 *    (oyuncuda sari) ve oyuncu boyalari bu tonlarin DISINDA secilmistir;
 *  * siluet — trafik gövdesi bilerek daha kutu (kucuk kose yaricapi, kunt
 *    burun, serit yok denecek kadar soluk).
 *
 * Govde boyutu ve carpisma kutusu DEGISMEDI.
 */
private fun DrawScope.drawObstacleCar(
    x: Float,
    y: Float,
    bodyArgb: Long,
    sprites: CarSpriteSet?
) {
    translate(x, y) {
        // Cizim koordinatlari prototipin ham arac uzayina ait; tek bir olcek
        // carpaniyla kucultuluyor ki carpisma kutusu (GameConfig'te ayni
        // carpandan turetiliyor) gorselle birebir ortussun.
        scale(GameConfig.CAR_ART_SCALE, GameConfig.CAR_ART_SCALE, pivot = Offset.Zero) {
            drawCarShadowIfVector(CarCatalog.trafficStyle(bodyArgb), sprites)
            drawCarBody(CarCatalog.trafficStyle(bodyArgb), sprites)
        }
    }
}

private fun DrawScope.drawCoin(coin: GameEngine.Coin) {
    val pulse = 1f + sin(coin.pulse) * 0.1f
    val radius = 10f * pulse
    drawCircle(Color(0xFFF6C000), radius, Offset(coin.x, coin.y))
    drawCircle(Color(0xFF8F6A00), radius, Offset(coin.x, coin.y), style = COIN_RIM_STROKE)
    drawRect(
        Color(0xFFFFF0A8),
        Offset(coin.x - 2f * pulse, coin.y - 6f * pulse),
        Size(4f * pulse, 12f * pulse)
    )
}

private val COIN_RIM_STROKE = Stroke(width = 3f)

/**
 * Far konisi. Yol ve firca ARAC UZAYINDA (oyuncunun merkezi orijin) bir kez
 * kuruluyor, her karede yalnizca `translate` ile yerine tasiniyor.
 *
 * Eskiden hem [Path] hem radyal gradyan oyuncunun mutlak koordinatlariyla
 * kuruluyordu; oyuncu surekli hareket ettigi icin her kare yeni bir firca
 * ornegi ve yeni bir native shader demekti. Geometri ayni: cevirme donusumu
 * noktalari birebir eski mutlak degerlere goturuyor.
 */
private val headlightPath = Path().apply {
    moveTo(-88f, -18f)
    quadraticTo(0f, -230f, 88f, -18f)
    close()
}

private val headlightBrush: Brush = Brush.radialGradient(
    colors = listOf(Color(0x38FFFAD2), Color(0x1FFFF8BE), Color(0x00FFF8BE)),
    center = Offset(0f, -92f),
    radius = 170f
)

private fun DrawScope.drawNightHeadlights(engine: GameEngine) {
    if (engine.theme != RoadTheme.NIGHT) return
    translate(engine.playerX, engine.playerY) {
        drawPath(path = headlightPath, brush = headlightBrush)
    }
}

private fun DrawScope.drawParticles(engine: GameEngine) {
    engine.particles.forEach { p ->
        val alpha = (p.life * 1.8f).coerceIn(0f, 1f)
        // Color bir `value class`; buradaki uretim ve `copy` yigin tahsisi
        // yapmiyor, bu yuzden dokunulmadi.
        drawRect(
            color = Color(p.color).copy(alpha = alpha),
            topLeft = Offset(p.x, p.y),
            size = Size(p.size, p.size)
        )
    }
}
