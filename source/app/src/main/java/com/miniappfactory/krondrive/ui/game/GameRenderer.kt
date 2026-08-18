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
import androidx.compose.ui.graphics.lerp
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
import com.miniappfactory.krondrive.ui.theme.KronColors
import com.miniappfactory.krondrive.ui.common.drawStyledCar
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

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
    sprites: CarSpriteSet? = null,
    /**
     * Carpisma vurusu ([CrashImpact]). null veya sonmus ise sahne bit bazinda
     * eskisi gibi cizilir; kosu sirasindaki tum maliyeti asagidaki tek
     * `takeIf` ve sifir kaymali bir `translate`tir.
     */
    impact: CrashImpact? = null
) {
    val beat = impact?.takeIf { it.isActive }
    // Sarsinti TUM sahneyi kaydiriyor ve bir kenarda en cok [SHAKE_AMPLITUDE_DP]
    // kadar bos serit aciliyor. Eskiden bu seridi alttaki Box'in zemin rengi
    // kapatiyordu — ama o zemin HER KARE tam ekran boyaniyordu, oysa serit
    // yalnizca sarsinti aninda var. Artik doldurma da yalnizca o anda yapiliyor.
    if (beat != null) drawRect(KronColors.Background)
    // Kamera sarsintisi TUM sahneyi kaydirir (yol, trafik, oyuncu, gosterge) —
    // kamera sallaniyor, sahnedeki nesneler degil. HUD disarida kalir: Compose
    // katmanindaki metinlerin sallanmasi "titreme" degil "arayuz bozuldu"
    // okunur. Sarsinti sirasinda bir kenarda en cok [SHAKE_AMPLITUDE_DP] kadar
    // (360 dp ekranda ~%1) bos serit acilir; alttaki Box zaten
    // `KronColors.Background` ile dolu oldugu icin bu serit koyu kaliyor.
    translate(
        left = (beat?.shakeX ?: 0f) * density,
        top = (beat?.shakeY ?: 0f) * density
    ) {
        scale(density, density, pivot = Offset.Zero) {
            drawSideBackgrounds(engine)
            drawTrack(engine)
        }
        drawSpeedometer(
            engine,
            density,
            textMeasurer,
            gaugeValueSize,
            gaugeLabelSize,
            gaugeSmallSize
        )
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
            drawParticles(engine.particles)
            // Carpisma kivilcimlari motorun partikulleriyle AYNI cizici ve ayni
            // katmanda: darbe sahnenin uzerine yapistirilmis bir efekt degil,
            // sahnenin kendi olayidir.
            if (beat != null) drawParticles(beat.particles)
        }
    }
    // Darbe flasi SARSINTININ DISINDA: tam ekrani kaplamasi gerekiyor, sahneyle
    // birlikte kaysaydi bir kenarinda boyanmamis serit kalirdi.
    if (beat != null) drawImpactFlash(beat)
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
            // DESEN HER KARE YENIDEN INSA EDILMIYOR — bir kez kurulup
            // kaydiriliyor (2026-08-19). Gerekce ve dogrulugu icin
            // [CrowdCache]'in belgesine bak.
            val crowdShift = engine.roadOffset * 0.35f
            CrowdCache.ensure(w, h, leftW, rightX)
            translate(top = crowdShift % CROWD_PERIOD - CROWD_PERIOD) {
                val paths = CrowdCache.paths
                for (i in paths.indices) {
                    val p = paths[i]
                    if (!p.isEmpty) drawPath(p, CROWD_COLORS[i])
                }
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

/**
 * Partikul cizimi. Eskiden dogrudan `engine`i aliyordu; artik LISTEYI aliyor,
 * cunku ayni cizici iki kaynagi birden besliyor: motorun kendi partikulleri
 * (coin, boost izi) ve [CrashImpact]'in kivilcimlari. Piksel cikti birebir
 * ayni — degisen yalnizca listenin nereden geldigi.
 */
private fun DrawScope.drawParticles(particles: List<GameEngine.Particle>) {
    particles.forEach { p ->
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

private fun DrawScope.drawImpactFlash(impact: CrashImpact) {
    val alpha = impact.flashAlpha
    // Gorunmez bir tam ekran katmani cizmek bedava degil; esigin altinda hic
    // cizmiyoruz.
    if (alpha < FLASH_MIN_VISIBLE_ALPHA) return
    // Konum/boyut verilmeyen `drawRect` tum cizim alanini kaplar.
    drawRect(color = impact.flashColor.copy(alpha = alpha))
}

// ---------------------------------------------------------------------------
// Carpisma vurusu ("crash beat")
// ---------------------------------------------------------------------------
//
// SORUN (docs/REVIEW_GAMEPLAY.md §6.1, olculmus): carpismadan BIR SONRAKI
// karede %70 opak siyah perde iniyordu. Simulasyon zaten donuyor ve sahne
// cizilmeye devam ediyor — yani "neye carptim" bilgisi ekranda VAR, ama ayni
// karede perdenin arkasina gomuluyor. Oyuncunun kendi hatasina bakabilecegi
// tek bir karesi yoktu.
//
// COZUM ucu bir arada: perdeyi 300 ms geciktir + o surede kamerayi sars +
// darbe flasi + temas noktasindan kivilcim. Ses BURADA YOK; carpma sesi
// `audio/` tarafinda ayri yuruyor ve bagi disaridan kuruluyor.
//
// SABITLER NEDEN GameConfig'TE DEGIL: bunlarin hicbiri simulasyona girmiyor —
// motor bu degerleri gormuyor, JVM testleri bunlara bakmiyor, degistirmek
// hicbir dengeyi oynatmiyor. GameRenderer'in kendi gorsel sabitleriyle
// (renkler, olculer, onbellekler) ayni kategoridedirler. `game/` paketine
// tasinacaklarsa bunun sebebi "denge sabiti olmalari" degil, "tek yerde
// dursunlar" tercihi olur.

/**
 * Kamera sarsintisinin suresi ve genligi.
 *
 * `game-depth-3d` skill'inin kurali: *"Kamera sarsintisi yalnizca carpmada.
 * Süreklisi mide bulandirir. ≤ 200 ms, genlik ≤ 4 dp, azalan zarf."* Ucu de
 * birebir uygulandi.
 *
 * Sure 300 ms'lik gecikmeden KISA olmasi bilincli: sarsinti 200 ms'de biter,
 * geriye perde inmeden once ~100 ms'lik DURGUN bir kare kalir. Sarsinti
 * "carptim" der, durgun kare "neye carptigimi gor" der. Ikisi ust uste
 * binseydi oyuncu sallanan bir goruntuden bilgi okumaya calisirdi.
 */
private const val SHAKE_SEC = 0.20f
private const val SHAKE_AMPLITUDE_DP = 4f

/**
 * Second Chance booster'i carpismayi yuttugunda kullanilan zayif surum: oyun
 * DEVAM ediyor, yani bu kareler oynanis kareleri. Yarisi kadar genlik, daha
 * kisa sure ve **flas yok** — kosu sirasinda tam ekran bir alfa katmani
 * cizmek hedef cihazda (S8, ~40 FPS) odenmek istenmeyen bir maliyet.
 */
private const val SHAKE_SEC_SAVED = 0.12f
private const val SHAKE_AMPLITUDE_DP_SAVED = 2.5f

/**
 * Sarsintinin iki ekseni FARKLI frekansta: esit olsalardi kayma tek bir
 * kosegen boyunca gidip gelir ve "sarsinti" degil "kayma" okunurdu.
 *
 * Ikisi de 20 Hz'in ALTINDA secildi. Hedef cihaz ~40 FPS'te ornekleniyor;
 * 20 Hz uzeri bir frekans orneklemeye takilip (aliasing) tahmin edilemez,
 * cihazdan cihaza degisen yavas bir salinima donusurdu. 15 ve 11 Hz'de
 * salinim kare basina belirgin sicrar (~2.7 ve ~3.6 ornek/cevrim), yani
 * "sert" okunur — istenen tam olarak budur.
 */
private const val SHAKE_FREQ_X = 15f
private const val SHAKE_FREQ_Y = 11f

/**
 * Baslangic fazlari SIFIR DEGIL: `sin(0) = 0` oldugu icin sifir fazda darbenin
 * ILK karesi hic kaymadan cizilirdi — yani en sert olmasi gereken kare en
 * sakini olurdu.
 */
private const val SHAKE_PHASE_X = 1.1f
private const val SHAKE_PHASE_Y = 2.6f

private const val TWO_PI = 6.2831855f

/**
 * Darbe flasi. Ilk karede beyaza yakin sicak bir vurgu ([FLASH_HOT]), sonra
 * hizla kirmiziya donerek soner ([FLASH_COOL]) — "carpma" once isik, sonra
 * hasar rengidir.
 *
 * Sonme karesel (`fade²`): dogrusal sonme 40 FPS'te bir "solma animasyonu"
 * gibi okunuyordu, karesel sonme ilk iki karede biten bir VURUS gibi.
 *
 * 160 ms, perdenin 300 ms'inden once biter: flas ile perde ust uste binmez.
 */
private const val FLASH_SEC = 0.16f
private const val FLASH_PEAK_ALPHA = 0.55f
private const val FLASH_MIN_VISIBLE_ALPHA = 0.004f
private val FLASH_HOT = Color(0xFFFFF1DC)
private val FLASH_COOL = Color(0xFFD8281E)

/**
 * Kivilcim sayisi. Coin'in 12'sinden fazla, cunku carpisma kosunun en buyuk
 * olayi ve sahne zaten donmus — burada cizim butcesi bosta. Second Chance
 * surumu OYNANIS sirasinda ciziliyor, o yuzden coin'in altinda tutuldu.
 */
private const val CRASH_PARTICLE_COUNT = 22
private const val CRASH_PARTICLE_COUNT_SAVED = 8

/**
 * Kivilcim hizi (dp / KARE — saniye degil).
 *
 * Birim bilerek boyle: `GameEngine.updateParticles` partikulleri
 * `p.x += p.vx` diye ilerletiyor, yani kare basina. Ayni mekanizmayi yeniden
 * kullanmak demek ayni birimi kullanmak demek. Sonucu: dusuk kare hizinda
 * kivilcimlar saniyede daha az yol alir. Motorun coin patlamasi bu davranisi
 * prototipten beri tasiyor; burada AYRISMAK, iki partikul kaynaginin ayni
 * sahnede farkli fizikle hareket etmesi anlamina gelirdi.
 */
private const val CRASH_SPARK_SPEED_MIN = 1.2f
private const val CRASH_SPARK_SPEED_MAX = 4.0f

/**
 * Kivilcimlara verilen yukari dogru sapma. Trafik YUKARIDAN geliyor, temas
 * oyuncunun burnunda oluyor; simetrik bir patlama kivilcimlarin yarisini
 * aracin altina savururdu.
 */
private const val CRASH_SPARK_LIFT = 1.0f

private const val CRASH_SPARK_LIFE_MIN = 0.35f
private const val CRASH_SPARK_LIFE_SPAN = 0.40f
private const val CRASH_SPARK_SIZE_MIN = 2.5f
private const val CRASH_SPARK_SIZE_SPAN = 3.5f

/**
 * Yercekimi — `GameEngine.updateParticles`taki `p.vy += 0.03f` ile AYNI deger.
 * Kopya oldugu icin buraya yazildi: orasi degisirse burasi da degismeli,
 * yoksa iki partikul kaynagi ayni sahnede farkli duser.
 */
private const val CRASH_SPARK_GRAVITY = 0.03f

/**
 * Kivilcim paleti: beyaz-sicak, kehribar, kirmizi. Coin patlamasi sari/turuncu
 * — carpismanin kendi rengi olsun ki oyuncu goz ucuyla bile ikisini
 * karistirmasin (`docs/REVIEW_GAMEPLAY.md` §6.2: *"Kirmizi/turuncu bir renk
 * cifti eklenirse kimlik de kazanir"*).
 */
private val CRASH_SPARK_COLORS = intArrayOf(
    0xFFFFF4D6.toInt(),
    0xFFFF9A2E.toInt(),
    0xFFE0362C.toInt()
)

/**
 * Carpisma aninin GORSEL durumu: kamera sarsintisi + darbe flasi + kivilcim.
 *
 * **NEDEN COMPOSE DURUMU DEGIL.** Bu nesnenin alanlari 60 (cihazda ~40) Hz'de
 * degisiyor. `mutableStateOf` ile sarilsalardi her kare `GameScreen`'in
 * tamamini yeniden bestelerlerdi — projedeki en pahali hata, `GameScreen`
 * icindeki `HudState` ve `boostReadyState` yorumlarinda uzun uzun anlatilan
 * sey (HUD 20 Hz'de yaziliyordu ve tum ekrani yeniliyordu). Cizim tarafi zaten
 * her kare gecersiz kilinmis durumda (`GameScreen`deki `frame` sayaci Canvas
 * icinde okunuyor), yani bu nesnenin ekrani uyandirmasi GEREKMIYOR: yalnizca
 * bir sonraki cizimde dogru degeri tasimasi yetiyor. Bu yuzden duz alanlar,
 * tipki `GameEngine`in kendisi gibi.
 *
 * **Yasam dongusu.** `GameScreen` bunu `remember(engine)` ile tutar — yani her
 * yeni kosu temiz bir nesneyle baslar. [step] her karede cagrilir ama [isActive]
 * false iken ilk satirda doner: kosunun %99'unda maliyeti bir boolean okumasi.
 */
class CrashImpact {

    /** Darbeden bu yana gecen sure (saniye). */
    private var elapsed = 0f

    /** Kosuyu bitiren carpisma mi, Second Chance ile yutulan mi. */
    private var fatal = false

    /**
     * Motorun kendi partikulleriyle AYNI tip: ayni alanlar, ayni integrasyon
     * ([step] icinde), ayni cizici ([drawParticles]). Sifirdan bir partikul
     * sistemi yazilmadi — mekanizma zaten vardi, yalnizca ikinci bir kaynak
     * kazandi.
     *
     * Liste MOTORUNKI DEGIL. `engine.particles`e yazmak simulasyon durumunu
     * disaridan degistirmek olurdu; ayrica o liste `engine.step()` icinde
     * yonetiliyor ve sahibi motordur.
     */
    private val sparks = ArrayList<GameEngine.Particle>(CRASH_PARTICLE_COUNT)

    internal val particles: List<GameEngine.Particle> get() = sparks

    /** Sahnenin bu kare kaydirilacagi mesafe, dp cinsinden. */
    var shakeX = 0f
        private set

    var shakeY = 0f
        private set

    /** false iken ne [step] ne cizim is yapar. */
    var isActive = false
        private set

    internal val flashAlpha: Float
        get() {
            // Second Chance surumunde flas YOK (bkz. [SHAKE_SEC_SAVED]).
            if (!isActive || !fatal) return 0f
            val t = elapsed / FLASH_SEC
            if (t >= 1f) return 0f
            val fade = 1f - t
            return FLASH_PEAK_ALPHA * fade * fade
        }

    internal val flashColor: Color
        get() = lerp(FLASH_HOT, FLASH_COOL, (elapsed / FLASH_SEC).coerceIn(0f, 1f))

    /**
     * Darbeyi baslatir.
     *
     * [x]/[y] TEMAS noktasidir, oyuncunun konumu degil — cagiran taraf bunu
     * oyuncu ile carpilan aracin arasindan hesaplar. Kivilcimlarin oradan
     * fiskirmasi oyuncunun gozunu "neye carptim" sorusunun cevabina goturur.
     *
     * [fatal] false iken (Second Chance) yalnizca zayif bir sarsinti ve az
     * sayida kivilcim uretilir: oyun devam ediyor, ekrani bogmamali.
     */
    fun trigger(x: Float, y: Float, fatal: Boolean) {
        this.fatal = fatal
        elapsed = 0f
        isActive = true
        sparks.clear()
        val count = if (fatal) CRASH_PARTICLE_COUNT else CRASH_PARTICLE_COUNT_SAVED
        repeat(count) { index ->
            // Aci esit dilimlere bolunup her dilime rastgelelik eklenir: saf
            // rastgele acilarda kivilcimlar kumelenip patlama degil "sacilma"
            // gorunumu veriyordu.
            val angle = (index + Random.nextFloat()) / count * TWO_PI
            val speed = CRASH_SPARK_SPEED_MIN +
                Random.nextFloat() * (CRASH_SPARK_SPEED_MAX - CRASH_SPARK_SPEED_MIN)
            sparks.add(
                GameEngine.Particle(
                    x = x,
                    y = y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed - CRASH_SPARK_LIFT,
                    life = CRASH_SPARK_LIFE_MIN + Random.nextFloat() * CRASH_SPARK_LIFE_SPAN,
                    size = CRASH_SPARK_SIZE_MIN + Random.nextFloat() * CRASH_SPARK_SIZE_SPAN,
                    color = CRASH_SPARK_COLORS[index % CRASH_SPARK_COLORS.size]
                )
            )
        }
        // Tetiklendigi KARE de tam genlikte cizilsin diye kayma hemen
        // hesaplaniyor; ilk adimi bir sonraki kareye birakmak darbenin en sert
        // karesini kaciriyordu.
        updateShake()
    }

    /**
     * Bir kare ilerletir. `GameEngine.step`ten ONCE cagrilmali — cagri sirasi
     * `GameScreen`in dongusunde yazili.
     */
    fun step(dt: Float) {
        if (!isActive) return
        elapsed += dt

        // Integrasyon `GameEngine.updateParticles` ile birebir ayni: ayni
        // sirada, ayni birimlerle, ayni yercekimiyle.
        var i = sparks.size - 1
        while (i >= 0) {
            val p = sparks[i]
            p.x += p.vx
            p.y += p.vy
            p.life -= dt
            p.vy += CRASH_SPARK_GRAVITY
            if (p.life <= 0f) sparks.removeAt(i)
            i--
        }

        updateShake()

        // Sonme kosulu: hem zamanli efektler (sarsinti, flas) bitmis hem son
        // kivilcim sonmus olmali. Ikisi FARKLI surelerde biter — sarsinti
        // 200 ms, kivilcimlar 750 ms'ye kadar.
        if (sparks.isEmpty() && elapsed >= SHAKE_SEC && elapsed >= FLASH_SEC) {
            isActive = false
            shakeX = 0f
            shakeY = 0f
        }
    }

    private fun updateShake() {
        val duration = if (fatal) SHAKE_SEC else SHAKE_SEC_SAVED
        if (elapsed >= duration) {
            shakeX = 0f
            shakeY = 0f
            return
        }
        // Karesel zarf: genlik basta hizli, sonda yavas duser — carpma boyle
        // hissedilir. Dogrusal zarf "sallanan kamera" gibi okunuyordu.
        val fade = 1f - elapsed / duration
        val amplitude =
            (if (fatal) SHAKE_AMPLITUDE_DP else SHAKE_AMPLITUDE_DP_SAVED) * fade * fade
        shakeX = amplitude * sin(elapsed * SHAKE_FREQ_X * TWO_PI + SHAKE_PHASE_X)
        shakeY = amplitude * sin(elapsed * SHAKE_FREQ_Y * TWO_PI + SHAKE_PHASE_Y)
    }
}

/**
 * Seyirci deseninin TEKRAR PERIYODU (dunya birimi).
 *
 * Satirlar 42 birim arayla; bir satirin renk indeksi
 * `(x + rowKey / 6).roundToInt().mod(4)` ve `rowKey` satir basina 42 arttigi
 * icin indeks satir basina **7** kayiyor. `mod 4` altinda 7 ≡ 3, dort satirda
 * toplam kayma 28 ≡ 0 → desen **4 satirda = 168 birimde** birebir tekrar eder.
 */
private const val CROWD_PERIOD = 168f

/**
 * Seyirci bloklarinin onbellegi.
 *
 * ## Neden
 *
 * Eski kod deseni HER KARE bastan kuruyordu. 1080x2220 / density 3 ekranda
 * bu, kare basina ~440 blok ve blok basina 5 native `Path` cagrisi demek:
 * **~2.200 JNI cagrisi**. Olculen `sideBg = 2,73 ms`'in neredeyse tamami
 * piksel degil YOL INSASIYDI ve UI thread'de yaniyordu (2026-08-19 analizi).
 *
 * Ikinci ve gizli maliyet: `Path.rewind()` Skia'da yolun generation ID'sini
 * sifirliyor. Skia'nin GPU yol onbellekleri o ID ile anahtarlandigi icin her
 * kare garantili cache miss oluyordu.
 *
 * ## Neden dogru — cevirinin desene etkisi yok
 *
 * Desen [CROWD_PERIOD] birimde tekrar ettigi icin, deseni DUNYA uzayinda bir
 * kez kurup her karede `crowdShift % CROWD_PERIOD` kadar kaydirmak birebir
 * ayni pikseli uretir. Ispat: onbellekte j'inci satirin rengi `(x + 7j).mod(4)`;
 * ekranda o satirin gercek dunya indeksi `k = j - 4(m+1)` (m = tam periyot
 * sayisi), yani gercek renk `(x + 7k).mod(4) = (x + 7j - 28(m+1)).mod(4)`.
 * `28 ≡ 0 (mod 4)` oldugu icin iki ifade **esit**. Kayma ne olursa olsun
 * renkler kaymaz.
 *
 * Bu, 2026-08-16'da duzeltilen "seyirci titriyor" hatasinin tam tersi yonde
 * ayni meseledir: renk EKRAN y'sine degil DUNYA y'sine bagli kalmali. Burada
 * da oyle kaliyor.
 *
 * ## Gecersiz kilma
 *
 * Onbellek viewport'a bagli (genislik, yukseklik, yol kenarlari). Dordunden
 * biri degisirse desen yeniden kurulur; aksi halde ekran donduruldugunde ya da
 * yol genisligi degistiginde eski desen kalirdi.
 */
private object CrowdCache {
    val paths = Array(CROWD_COLORS_SIZE) { Path() }

    private var w = Float.NaN
    private var h = Float.NaN
    private var leftW = Float.NaN
    private var rightX = Float.NaN

    fun ensure(w: Float, h: Float, leftW: Float, rightX: Float) {
        if (w == this.w && h == this.h && leftW == this.leftW && rightX == this.rightX) return
        this.w = w; this.h = h; this.leftW = leftW; this.rightX = rightX

        for (p in paths) p.rewind()
        // Ekran yuksekligi + bir tam periyot kadar satir gerekiyor: cizim
        // `-CROWD_PERIOD` ile basliyor ve kayma [0, CROWD_PERIOD) araliginda.
        var j = 0
        while (42f * j <= h + 2f * CROWD_PERIOD) {
            val y = 42f * j
            // rowKey = 42*j; crowdColorIndex bunu kendi icinde /6 ediyor.
            val rowKey = y
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
            j++
        }
    }
}
