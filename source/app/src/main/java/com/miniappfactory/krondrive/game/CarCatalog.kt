package com.miniappfactory.krondrive.game

import com.miniappfactory.krondrive.data.AppLanguage
import kotlin.math.roundToInt

/**
 * Arac ozellestirme katalogu: govde sekilleri, boyalar, fiyatlar ve cizim
 * geometrisi. **Saf Kotlin** — Android importu yok (game/ paketinin kurali),
 * bu sayede sekillerin carpisma kutusuna sigdigi JVM testiyle dogrulanabiliyor.
 *
 * Neden geometri de burada: cizim [com.miniappfactory.krondrive.ui.common]
 * altindaki jenerik bir cizici tarafindan yapiliyor, sekil basina Compose kodu
 * YOK. Yeni sekil eklemek = bu dosyaya bir [CarShapeDef] eklemek. Boylece
 * "araclar ayni kutuya siger" kurali koddan degil VERIDEN dogrulanabiliyor.
 *
 * ## Degismez kural: kutu buyumez
 *
 * Cizim koordinatlari prototipin 42x90'lik arac uzayindadir ve ekranda
 * [GameConfig.CAR_ART_SCALE] ile kucultulur. Carpisma kutusu ayni sabitlerden
 * turetilir ([GameConfig.CAR_HITBOX_WIDTH_PX]). Bu yuzden HER sekil
 * [ART_LEFT]..[ART_RIGHT] x [ART_TOP]..[ART_BOTTOM] araligina sigmak
 * ZORUNDADIR — aksi halde gorunur arac carpisma kutusundan tasar ve
 * 2026-08-13'te duzeltilen "havaya carptim" hatasi geri gelir.
 * `CarCatalogTest` bunu her sekil icin dogrular.
 *
 * Fiyatlar burada TEK YERDE; ekonomi dengesi ayri bir calismada yeniden
 * ayarlanacak, ekran/motor dosyalarina sizmamalari bu yuzden onemli.
 *
 * ## Perspektif sozlesmesi (2026-08-15)
 *
 * Kamera aracin USTUNDE ve biraz ARKASINDADIR. Bunun cizime iki zorunlu
 * sonucu var:
 *
 *  1. Gordugumuz yuzeyin neredeyse tamami TAVAN/KAPUT duzlemidir.
 *  2. Aracin ARKA yuzu (tampon + stop lambalari) tavanla ayni duzlemde
 *     DEGILDIR; ona dik durdugu icin bize dogru KISALARAK yansir ve gövde
 *     boyunun ancak ~%7'sini kaplar. Bu yuzden arka yuz [rearFace] ile
 *     uretilir: asagi dogru kararan kisa bir trapez, ustunde tavanla
 *     arasindaki kirilma cizgisi, uzerinde ince stop seritleri.
 *
 * Sahibin 2026-08-15 elestirisi tam olarak buydu: *"ustten bakiyoruz
 * araclara, stop lambasi ve tampon o kadar tavan ile ayni paralelde
 * olmamali"*.
 */

// ---------------------------------------------------------------------------
// Cizim parcalari (saf veri)
// ---------------------------------------------------------------------------

/** Bir parcanin hangi boyayla dolacagi. Renk secimi calisma aninda cozulur. */
enum class CarPaint {
    /** Secili boyanin ana rengi. */
    BODY,

    /** Ana rengin koyu tonu: tampon, difuzor, hava girisi. */
    BODY_SHADE,

    /** Boyaya ait kontrast serit rengi. */
    ACCENT,

    /** Cam (tum araclarda ayni). */
    GLASS,

    /** Lastik (tum araclarda ayni). */
    TIRE,

    /** Krom/egzoz detayi. */
    TRIM,

    /** Surucu basi. */
    DRIVER,

    /** Arka stop lambasi — kirmizi, tum boyalarda ayni. */
    TAIL,

    /** Spekuler vurgu (dusuk alfa ile kullanilir). */
    GLOSS
}

/** Poligon kosesi (Compose'a bagimli olmamak icin kendi tipimiz). */
data class CarPoint(val x: Float, val y: Float)

/** Gradyanin hangi eksende aktigi. */
enum class CarAxis {
    /** Parcanin sol kenarindan sag kenarina (silindirik hacim). */
    HORIZONTAL,

    /** Parcanin ust kenarindan alt kenarina (duzlem kararmasi). */
    VERTICAL
}

/**
 * Tek bir gradyan duragi.
 *
 * [shift] parcanin KENDI rengine uygulanan ton kaydirmasidir: `+1` tam beyaz,
 * `-1` tam siyah, `0` degismez. Mutlak renk yerine kaydirma tutuluyor cunku
 * ayni gradyan 9 boyanin hepsinde ve 4 trafik renginde kullaniliyor — hacim
 * hissi boyadan bagimsiz olmali.
 */
data class CarTint(val position: Float, val shift: Float)

/** Bir parcaya uygulanan yon + duraklar. */
data class CarGradient(val axis: CarAxis, val stops: List<CarTint>)

/**
 * Cizilebilir tek bir gövde parcasi. Sinirlar test icin acik.
 *
 * [gradient] null ise duz renk; [alpha] 1 disinda ise parca yari saydam cizilir
 * (cam yansimasi, spekuler vurgu ve temas golgeleri boyle yapiliyor —
 * [CarPaint] renkleri her zaman tam opak kalir, testi bozmamak icin).
 */
sealed interface CarPart {
    val paint: CarPaint
    val gradient: CarGradient?
    val alpha: Float
    val left: Float
    val top: Float
    val right: Float
    val bottom: Float

    /** Dikdortgen (kose yaricapi 0 ise duz). */
    data class Box(
        override val paint: CarPaint,
        override val left: Float,
        override val top: Float,
        val width: Float,
        val height: Float,
        val corner: Float = 0f,
        override val gradient: CarGradient? = null,
        override val alpha: Float = 1f
    ) : CarPart {
        override val right: Float get() = left + width
        override val bottom: Float get() = top + height
    }

    /** Daire (surucu basi, far). */
    data class Disc(
        override val paint: CarPaint,
        val centerX: Float,
        val centerY: Float,
        val radius: Float,
        override val gradient: CarGradient? = null,
        override val alpha: Float = 1f
    ) : CarPart {
        override val left: Float get() = centerX - radius
        override val top: Float get() = centerY - radius
        override val right: Float get() = centerX + radius
        override val bottom: Float get() = centerY + radius
    }

    /** Poligon (kama burun, genisleyen arka, kisalmis arka yuz). */
    data class Wedge(
        override val paint: CarPaint,
        val points: List<CarPoint>,
        override val gradient: CarGradient? = null,
        override val alpha: Float = 1f
    ) : CarPart {
        override val left: Float get() = points.minOf { it.x }
        override val top: Float get() = points.minOf { it.y }
        override val right: Float get() = points.maxOf { it.x }
        override val bottom: Float get() = points.maxOf { it.y }
    }
}

// ---------------------------------------------------------------------------
// Katalog tipleri
// ---------------------------------------------------------------------------

/** Coin ile acilan her sey icin ortak alanlar (fiyat + seviye sarti). */
sealed interface CarItem {
    val id: String
    val priceCoins: Int
    val requiredCarLevel: Int
    fun name(language: AppLanguage): String
}

/**
 * Bir govde sekli: isim, fiyat, SURUS OZELLIKLERI ve cizim parcalari.
 *
 * Dort carpan (2026-08-15, sahibi karari — `docs/BALANCE.md` "Arac
 * ozellikleri"): araclar o tarihe kadar tamamen kozmetikti, 3200 coinlik
 * arac hicbir sey yapmiyordu. Carpanlar yukseltmelerin **ustune** uygulanir,
 * yerine gecmez; boylece ana ilerleme yine garaj yukseltmeleridir.
 *
 * Hepsi 1.0 = varsayilan gövde ([CarCatalog.defaultShape]) = eski davranis.
 * Buyuk deger her eksende IYIdir (fren dahil: buyuk = daha cok yavaslatir).
 *
 * **Carpisma kutusu hicbir sekilde etkilenmez** (bkz. PROVENANCE #6) — bu
 * alanlarin hicbiri cizim/kutu olculerine dokunmaz.
 */
data class CarShapeDef(
    override val id: String,
    val nameTr: String,
    val nameEn: String,
    val descriptionTr: String,
    val descriptionEn: String,
    override val priceCoins: Int,
    override val requiredCarLevel: Int,
    /** Cizim sirasi = liste sirasi (ustteki parca sonra gelir). */
    val parts: List<CarPart>,
    /** Skordan gelen hiz tavanini olcekler ([UpgradeCatalog.scoreSpeedCap]). */
    val topSpeedMul: Float = 1f,
    /** Hedef hiza yaklasma oranini olcekler ([UpgradeCatalog.accelRate]). */
    val accelMul: Float = 1f,
    /** Fren cezasini olcekler ([UpgradeCatalog.brakePenalty]). */
    val brakeMul: Float = 1f,
    /** Boost SURESINI olcekler — tuketimi boler ([UpgradeCatalog.boostDrain]). */
    val boostMul: Float = 1f,
    /**
     * FABRIKA BOYASI: bu gövde satin alindiginda otomatik acilan ve secilen
     * boya (proje sahibi karari, 2026-08-15).
     *
     * Neden var: gövde cizimleri artik referans sprite'lardan geliyor ve her
     * aracin bir "kendi rengi" var — Kuş SLX petrol, Dağ Keçisi beyaz. Bu
     * renkler katalogda pahali ([CarCatalog.COLOR_TEAL] 1400 coin); araci
     * alan oyuncu onu tasarlandigi renkte GORMEDEN once bir 1400 daha
     * odemek zorunda kalmamali.
     *
     * Sonuc: gövdeye sahip olmak fabrika boyasini da acar
     * ([CarCatalog.effectiveOwnedColors]). Oyuncu boyayi degistirebilir ve
     * secimi O GOVDE ICIN hatirlanir — baska bir gövdeye gecince onun kendi
     * boyasi gelir.
     */
    val defaultColorId: String = CarCatalog.DEFAULT_COLOR_ID,
    /** Garajda gosterilen tek satirlik karakter cumlesi. */
    val traitTr: String = "",
    val traitEn: String = ""
) : CarItem {
    override fun name(language: AppLanguage): String = language.pick(tr = nameTr, en = nameEn)

    fun description(language: AppLanguage): String =
        language.pick(tr = descriptionTr, en = descriptionEn)

    fun trait(language: AppLanguage): String = language.pick(tr = traitTr, en = traitEn)

    /**
     * Dort eksenin carpani. Eksen adlari [UpgradeType] ile AYNI: garaj hem
     * yukseltme bolumunde hem arac kartinda tek bir etiket kumesi kullansin
     * diye (HIZ / IVME / FREN / BOOST).
     */
    fun multiplier(type: UpgradeType): Float = when (type) {
        UpgradeType.SPEED -> topSpeedMul
        UpgradeType.ACCELERATION -> accelMul
        UpgradeType.BRAKE -> brakeMul
        UpgradeType.BOOST -> boostMul
    }

    val artLeft: Float get() = parts.minOf { it.left }
    val artTop: Float get() = parts.minOf { it.top }
    val artRight: Float get() = parts.maxOf { it.right }
    val artBottom: Float get() = parts.maxOf { it.bottom }
}

/**
 * Bir boya. Renkler ARGB `Long` olarak tutulur; `game/` paketi Compose'a
 * bagimli olamayacagi icin `Color` tipi burada kullanilamaz.
 */
data class CarColorDef(
    override val id: String,
    val nameTr: String,
    val nameEn: String,
    override val priceCoins: Int,
    override val requiredCarLevel: Int,
    val bodyArgb: Long,
    /** Tampon/difuzor icin koyu ton. */
    val shadeArgb: Long,
    /** Serit/kanat icin kontrast ton. */
    val accentArgb: Long,
    /**
     * Surucu basi. Oyuncuda sari, trafikte mavi — ayni gövde verisinden
     * cizilen iki arac 60 Hz'de bu tek noktadan da ayrilabilsin diye
     * boyaya bagli.
     */
    val driverArgb: Long = CarCatalog.DRIVER_ARGB
) : CarItem {
    override fun name(language: AppLanguage): String = language.pick(tr = nameTr, en = nameEn)
}

/** Oyuncunun secili gorunumu (sekil + boya). */
data class CarStyle(val shape: CarShapeDef, val color: CarColorDef) {
    /** Bir parcanin gercek rengini cozer. */
    fun argbOf(paint: CarPaint): Long = when (paint) {
        CarPaint.BODY -> color.bodyArgb
        CarPaint.BODY_SHADE -> color.shadeArgb
        CarPaint.ACCENT -> color.accentArgb
        CarPaint.GLASS -> CarCatalog.GLASS_ARGB
        CarPaint.TIRE -> CarCatalog.TIRE_ARGB
        CarPaint.TRIM -> CarCatalog.TRIM_ARGB
        CarPaint.DRIVER -> color.driverArgb
        CarPaint.TAIL -> CarCatalog.TAIL_ARGB
        CarPaint.GLOSS -> CarCatalog.GLOSS_ARGB
    }
}

/** Bir sekil/boyanin oyuncuya gore durumu — garaj bunu gosterir. */
enum class CarUnlockState {
    /** Sahip olunuyor (bedava olanlar da dahil). */
    OWNED,

    /** Kilitli ama coin yetiyor. */
    AFFORDABLE,

    /** Kilitli, coin yetmiyor. */
    TOO_EXPENSIVE,

    /** Arac seviyesi yetmiyor — coin ne olursa olsun alinamaz. */
    LEVEL_LOCKED
}

// ---------------------------------------------------------------------------
// Katalog
// ---------------------------------------------------------------------------

object CarCatalog {

    // --- Cizim kutusu (GameConfig'ten TURETILIR, elle yazilmaz) ------------
    //
    // GameConfig'teki degerler ekran olceginde (CAR_ART_SCALE uygulanmis);
    // buradaki parcalar ise ham prototip uzayinda. Olcege bolerek ayni uzaya
    // geri getiriyoruz. Sonuc: x -20..20, y -2..74.

    val ART_RIGHT: Float = GameConfig.CAR_WIDTH_PX / GameConfig.CAR_ART_SCALE / 2f
    val ART_LEFT: Float = -ART_RIGHT
    val ART_TOP: Float = GameConfig.CAR_ART_TOP_OFFSET / GameConfig.CAR_ART_SCALE
    val ART_BOTTOM: Float = GameConfig.CAR_ART_BOTTOM_OFFSET / GameConfig.CAR_ART_SCALE

    // --- Tum araclarda ayni olan renkler -----------------------------------

    const val GLASS_ARGB: Long = 0xFF1E2A47
    const val TIRE_ARGB: Long = 0xFF050505
    const val TRIM_ARGB: Long = 0xFFB9C6D7
    const val DRIVER_ARGB: Long = 0xFFFFD33D
    const val TAIL_ARGB: Long = 0xFFFF2D1F
    const val GLOSS_ARGB: Long = 0xFFFFFFFF

    /** Yere dusen golge — sekilden bagimsiz, ayak izi hissi degismesin diye. */
    const val SHADOW_ARGB: Long = 0x3D000000
    const val SHADOW_LEFT: Float = -21f
    const val SHADOW_TOP: Float = 12f
    const val SHADOW_WIDTH: Float = 42f
    const val SHADOW_HEIGHT: Float = 68f

    // --- Gradyanlar --------------------------------------------------------
    //
    // Hepsi ton KAYDIRMASI tutar, mutlak renk degil (bkz. [CarTint]); ayni
    // tanim 9 oyuncu boyasinda ve 4 trafik renginde calisir.

    /** Gövde ust yuzeyi: silindirik yanal yuvarlanma (sol omuzda vurgu). */
    val BODY_ROLL = CarGradient(
        CarAxis.HORIZONTAL,
        listOf(
            CarTint(0.00f, -0.52f), CarTint(0.06f, -0.24f), CarTint(0.19f, 0.30f),
            CarTint(0.33f, 0.13f), CarTint(0.52f, 0.00f), CarTint(0.72f, -0.15f),
            CarTint(0.88f, -0.36f), CarTint(1.00f, -0.56f)
        )
    )

    /** Tavan: gövdeden daha yuksek duzlem, isigi daha cok alir. */
    val ROOF_ROLL = CarGradient(
        CarAxis.HORIZONTAL,
        listOf(
            CarTint(0.00f, -0.26f), CarTint(0.18f, 0.20f), CarTint(0.46f, 0.07f),
            CarTint(0.74f, -0.08f), CarTint(1.00f, -0.32f)
        )
    )

    /** Burun/hava girisi: uste dogru kararan girinti. */
    val NOSE_FADE = CarGradient(
        CarAxis.VERTICAL,
        listOf(CarTint(0.00f, -0.38f), CarTint(0.30f, -0.08f), CarTint(1.00f, 0.02f))
    )

    /** Arka yuz: tavandan uzaklastikca kararir — perspektifin can damari. */
    val REAR_FADE = CarGradient(
        CarAxis.VERTICAL,
        listOf(CarTint(0.00f, -0.12f), CarTint(0.38f, -0.46f), CarTint(1.00f, -0.70f))
    )

    /** Cam: ust kenarda gok yansimasi, ortada koyu, altta hafif parlama. */
    val GLASS_SHEEN = CarGradient(
        CarAxis.VERTICAL,
        listOf(
            CarTint(0.00f, 0.42f), CarTint(0.28f, 0.12f),
            CarTint(0.62f, -0.16f), CarTint(1.00f, 0.04f)
        )
    )

    /** Lastik: silindir hissi (duz siyah blok "lego" gibi duruyordu). */
    val WHEEL_ROLL = CarGradient(
        CarAxis.HORIZONTAL,
        listOf(
            CarTint(0.00f, 0.02f), CarTint(0.26f, 0.26f),
            CarTint(0.58f, 0.10f), CarTint(1.00f, 0.00f)
        )
    )

    /** Stop lambasi: ust kenari parlak, alti koyu (lens hacmi). */
    val TAIL_GLOW = CarGradient(
        CarAxis.VERTICAL,
        listOf(CarTint(0.00f, 0.32f), CarTint(0.55f, 0.00f), CarTint(1.00f, -0.26f))
    )

    // --- Boost alevi -------------------------------------------------------
    //
    // Olculer prototipten; KATMANLAMA 2026-08-15'te degisti. Eskiden iki duz
    // ucgen vardi ve sahibin ifadesiyle "lego gibi" duruyordu. Artik alev
    // dort katman: halo (radyal camgobegi) + dis pluma + ic pluma + sicak
    // cekirdek; her pluma ucuna dogru hem daralir hem SAYDAMLASIR, kenarlar
    // boylece sonumlenir. Alev yalnizca cizimdir, carpismaya girmez.

    /** Alevin toplam uzanma payi (arac bittikten sonraki bos alan degil). */
    const val FLAME_GAP: Float = 6f
    const val FLAME_OUTER_LENGTH: Float = 16f
    const val FLAME_OUTER_HALF_WIDTH: Float = 5f
    const val FLAME_INNER_LENGTH: Float = 10f
    const val FLAME_INNER_HALF_WIDTH: Float = 3f

    /** Alev koku aracin bittigi yerin bu kadar altinda baslar (kopmasin). */
    const val FLAME_ROOT_OFFSET: Float = 0.8f

    /** Yumusak halonun yaricapi. */
    const val FLAME_HALO_RADIUS: Float = 13f

    const val FLAME_OUTER_ARGB: Long = 0xFF29B6FF
    const val FLAME_INNER_ARGB: Long = 0xFFFFD33D
    const val FLAME_CORE_ARGB: Long = 0xFFFFF6E0

    // --- Kimlikler ---------------------------------------------------------

    const val SHAPE_HATCHBACK = "hatchback"
    const val SHAPE_KUS_SLX = "kus_slx"
    const val SHAPE_RACE_SEDAN = "race_sedan"
    const val SHAPE_MUSCLE = "muscle"
    const val SHAPE_MOUNTAIN_GOAT = "mountain_goat"
    const val SHAPE_MUSCLE_67 = "muscle_67"
    const val SHAPE_SUPERCAR = "supercar"

    const val COLOR_KRON_RED = "kron_red"
    const val COLOR_GRAPHITE = "graphite"
    const val COLOR_EMERALD = "emerald"
    const val COLOR_GLACIER = "glacier"
    const val COLOR_MAGENTA = "magenta"
    const val COLOR_ROYAL = "royal"
    const val COLOR_VIOLET = "violet"
    const val COLOR_TEAL = "teal"
    const val COLOR_MIDNIGHT = "midnight"
    const val COLOR_KHAKI = "khaki"

    const val DEFAULT_SHAPE_ID = SHAPE_HATCHBACK
    const val DEFAULT_COLOR_ID = COLOR_KRON_RED

    // -----------------------------------------------------------------
    // Sekiller
    // -----------------------------------------------------------------

    /**
     * KISALMIS arka yuz — perspektif sozlesmesinin uygulanmasi (bkz. dosya
     * basligi). Dort parca uretir:
     *
     *  1. Tavandan 74'e inen, asagi dogru kararan ve hafif DARALAN trapez.
     *     Kisaligi (gövde boyunun ~%7'si) "bu yuzey bize dik duruyor" bilgisini
     *     tasir; duz bir tampon slabi bunu tasimiyordu.
     *  2. Tavan ile arka yuz arasindaki kirilma cizgisi (ince, parlak).
     *  3-4. Stop lambalari — arka yuzun UZERINDE ince seritler; ayri bir kutu
     *     gibi degil, cunku ayri kutu onlari yine tavan duzlemine tasiyordu.
     */
    private fun rearFace(
        top: Float,
        halfTop: Float,
        halfBottom: Float,
        tailInset: Float = 1.6f
    ): List<CarPart> {
        val bottom = ART_BOTTOM
        val height = bottom - top
        val tailHalf = halfBottom - 1.2f
        val stripTop = top + height * 0.30f
        val stripHeight = height * 0.42f
        return listOf(
            CarPart.Wedge(
                CarPaint.BODY_SHADE,
                listOf(
                    CarPoint(-halfTop, top), CarPoint(halfTop, top),
                    CarPoint(halfBottom, bottom), CarPoint(-halfBottom, bottom)
                ),
                REAR_FADE
            ),
            CarPart.Box(
                CarPaint.GLOSS, -halfTop + 0.4f, top - 0.55f,
                (halfTop - 0.4f) * 2f, 1f, 0.5f, alpha = 0.55f
            ),
            CarPart.Box(
                CarPaint.TAIL, -tailHalf, stripTop,
                tailHalf - tailInset, stripHeight, 0.9f, TAIL_GLOW
            ),
            CarPart.Box(
                CarPaint.TAIL, tailInset, stripTop,
                tailHalf - tailInset, stripHeight, 0.9f, TAIL_GLOW
            )
        )
    }

    /**
     * Varsayilan gövde. 2026-08-15'te yeniden cizildi (sahibin "oyundaki araba
     * cok ama cok kotu" geri bildirimi, bkz. PROVENANCE.md sapma #11): gövde
     * genisletildi ve tekerlekler camurluklarin ALTINA alindi, tavan ayri bir
     * duzlem oldu, arka yuz kisaldi. Kutu ve carpisma DEGISMEDI.
     */
    private val HATCHBACK = CarShapeDef(
        id = SHAPE_HATCHBACK,
        nameTr = "Şehir",
        nameEn = "City",
        descriptionTr = "Kron Drive'ın klasik gövdesi",
        descriptionEn = "The classic Kron Drive body",
        priceCoins = 0,
        requiredCarLevel = 1,
        // REFERANS ARAC: dort eksen de 1.0. Katalogdaki her sey buna gore
        // okunur ve bu degerler degistirilirse butun denge kayar.
        topSpeedMul = 1.00f,
        accelMul = 1.00f,
        brakeMul = 1.00f,
        boostMul = 1.00f,
        traitTr = "Her yolda idare eder, hiçbir eksende parlamaz",
        traitEn = "Gets by on any road, shines on no axis",
        parts = listOf(
            // Tekerlekler gövdenin ALTINDA kalir, camurluktan ~2 birim tasar.
            CarPart.Box(CarPaint.TIRE, -20f, 11f, 7f, 20f, 2.4f, WHEEL_ROLL),
            CarPart.Box(CarPaint.TIRE, 13f, 11f, 7f, 20f, 2.4f, WHEEL_ROLL),
            CarPart.Box(CarPaint.TIRE, -20f, 47f, 7f, 21f, 2.4f, WHEEL_ROLL),
            CarPart.Box(CarPaint.TIRE, 13f, 47f, 7f, 21f, 2.4f, WHEEL_ROLL),
            CarPart.Box(CarPaint.BODY, -15.5f, 16f, 31f, 56f, 6f, BODY_ROLL),
            CarPart.Wedge(
                CarPaint.BODY,
                listOf(
                    CarPoint(-9f, -2f), CarPoint(9f, -2f),
                    CarPoint(15.5f, 22f), CarPoint(-15.5f, 22f)
                ),
                BODY_ROLL
            ),
            CarPart.Box(CarPaint.BODY_SHADE, -8.4f, -2f, 16.8f, 2.2f, 1f, NOSE_FADE),
            // Seritler CAMDAN ONCE ve cami KESMEYECEK sekilde: kokpit kapanmasin.
            CarPart.Box(CarPaint.ACCENT, -2.8f, 0.6f, 5.6f, 20.4f, 1.2f, alpha = 0.88f),
            CarPart.Box(CarPaint.ACCENT, -2.8f, 47.6f, 5.6f, 20.4f, 1.2f, alpha = 0.88f),
            // Kokpit: on cam / TAVAN (omuzlardan icerde) / arka cam.
            CarPart.Box(CarPaint.GLASS, -11.4f, 21.5f, 22.8f, 10.2f, 4f, GLASS_SHEEN),
            CarPart.Disc(CarPaint.DRIVER, 0f, 26.4f, 4f),
            CarPart.Box(CarPaint.BODY, -12.4f, 30.6f, 24.8f, 10.4f, 3f, ROOF_ROLL),
            CarPart.Box(
                CarPaint.BODY_SHADE, -12.2f, 39.4f, 24.4f, 1.5f, 0.7f, alpha = 0.32f
            ),
            CarPart.Box(CarPaint.GLASS, -11f, 40.4f, 22f, 7f, 3f, GLASS_SHEEN),
            CarPart.Box(CarPaint.GLOSS, -14.3f, 13f, 1.5f, 50f, 0.75f, alpha = 0.14f)
        ) + rearFace(top = 69f, halfTop = 14.9f, halfBottom = 13.8f)
    )

    /**
     * 1980'ler Turk sedani havasi (proje sahibi karari, 2026-08-15). TAMAMEN
     * KOZMETIK — kutu, hitbox ve fizik digerleriyle birebir ayni.
     *
     * Silueti 32 px'de ayiran uc sey, oncelik sirasiyla:
     *
     *  1. **Kose yaricapi 1.6.** Kas Arabasi 2.5, digerleri 5-6. En kare govde
     *     bu; nostaljinin tasiyicisi da bu.
     *  2. **Dik on cam + uzun duz tavan.** Kamera ustte oldugu icin DIK bir
     *     cam kisalarak yansir: on cam 8.2 birim (digerleri ~10), tavan ise
     *     12 birim (digerleri ~10.4). Ikisinin orani "dik cam, duz tavan"
     *     bilgisini tek basina tasiyor.
     *  3. **Krom.** Iki yanda tam boy ince TRIM seridi + izgarada yatay krom
     *     cubuk + iki DIKDORTGEN far. Baska hicbir govdede krom yan serit ya
     *     da far yok; trafik aracinda da yok, bu yuzden 60 Hz'de karismiyor.
     *
     * Seritler (ACCENT) bilerek yarisci uzunlamasina serit DEGIL: kaput ve
     * bagaj uzerinde ince YATAY bantlar — donem aracinin garnitur cizgisi.
     */
    private val KUS_SLX = CarShapeDef(
        id = SHAPE_KUS_SLX,
        nameTr = "Kuş SLX",
        // Ozel ad, cevrilmez.
        nameEn = "Kuş SLX",
        descriptionTr = "Kare hatlar, dik ön cam, krom şerit",
        descriptionEn = "Square lines, upright screen, chrome trim",
        // Sahibi karari (2026-08-15): Kus SLX ve Dag Kecisi ayni fiyatta,
        // ikisi de 1500. Nostalji araclari birbirinin alternatifi;
        // birini otekine ucuz diye tercih ettirmenin anlami yok.
        priceCoins = 1500,
        requiredCarLevel = 2,
        // Sakin, uzun soluklu: son hizi referansin ALTINDA, karsiliginda
        // kataloğun en uzun boost'u ve saglam freni.
        topSpeedMul = 0.97f,
        accelMul = 1.00f,
        brakeMul = 1.06f,
        boostMul = 1.12f,
        traitTr = "Acelesi yok ama nitrosu bir türlü bitmez",
        traitEn = "In no hurry, but the nitro never runs dry",
        // Referans cizim petrol yesili; arac bu renkte tasarlandi.
        defaultColorId = COLOR_TEAL,
        parts = listOf(
            // Donem lastigi: dar ve kare (yaricap 1.4, en kucugu).
            CarPart.Box(CarPaint.TIRE, -20f, 12f, 7f, 19f, 1.4f, WHEEL_ROLL),
            CarPart.Box(CarPaint.TIRE, 13f, 12f, 7f, 19f, 1.4f, WHEEL_ROLL),
            CarPart.Box(CarPaint.TIRE, -20f, 47f, 7f, 20f, 1.4f, WHEEL_ROLL),
            CarPart.Box(CarPaint.TIRE, 13f, 47f, 7f, 20f, 1.4f, WHEEL_ROLL),
            CarPart.Box(CarPaint.BODY, -16f, 10f, 32f, 60f, 1.6f, BODY_ROLL),
            // Kunt burun: kama degil, neredeyse dikdortgen (kenarlar 1.5 birim
            // toplaniyor). Kaput duzlugu donemin imzasi.
            CarPart.Wedge(
                CarPaint.BODY,
                listOf(
                    CarPoint(-14.5f, -2f), CarPoint(14.5f, -2f),
                    CarPoint(16f, 12f), CarPoint(-16f, 12f)
                ),
                BODY_ROLL
            ),
            CarPart.Box(CarPaint.BODY_SHADE, -14f, -2f, 28f, 2.2f, 0.6f, NOSE_FADE),
            // Ince YATAY izgara + uzerinde krom cubuk.
            CarPart.Box(CarPaint.BODY_SHADE, -9.5f, 0.6f, 19f, 2.6f, 0.5f, NOSE_FADE),
            CarPart.Box(CarPaint.TRIM, -9.5f, 1.6f, 19f, 0.7f, 0.35f, alpha = 0.9f),
            // Dikdortgen farlar: izgaranin iki yaninda. TRIM cerceve + uzerine
            // GLOSS lens — 32 px'de burnun iki ucunda iki PARLAK nokta kaliyor
            // ve bu, hicbir govdede/trafikte olmayan tek isaret.
            CarPart.Box(CarPaint.TRIM, -13.8f, 0.4f, 4f, 3f, 0.5f, alpha = 0.95f),
            CarPart.Box(CarPaint.TRIM, 9.8f, 0.4f, 4f, 3f, 0.5f, alpha = 0.95f),
            CarPart.Box(CarPaint.GLOSS, -13.4f, 0.8f, 3.2f, 2.2f, 0.4f, alpha = 0.85f),
            CarPart.Box(CarPaint.GLOSS, 10.2f, 0.8f, 3.2f, 2.2f, 0.4f, alpha = 0.85f),
            // Tam boy krom yan seritler — govdenin en dis kenarinda.
            CarPart.Box(CarPaint.TRIM, -16f, 12f, 1.3f, 52f, 0.5f, alpha = 0.88f),
            CarPart.Box(CarPaint.TRIM, 14.7f, 12f, 1.3f, 52f, 0.5f, alpha = 0.88f),
            // Garnitur bantlari (kaput + bagaj), uzunlamasina serit DEGIL.
            CarPart.Box(CarPaint.ACCENT, -8f, 5.4f, 16f, 1.4f, 0.6f, alpha = 0.8f),
            CarPart.Box(CarPaint.ACCENT, -9f, 55f, 18f, 1.6f, 0.7f, alpha = 0.8f),
            CarPart.Box(CarPaint.GLOSS, -14.4f, 12f, 1.3f, 50f, 0.65f, alpha = 0.14f),
            // Kokpit: KISA on cam (dik) -> UZUN duz tavan -> kisa arka cam.
            CarPart.Box(CarPaint.GLASS, -12.6f, 21.8f, 25.2f, 8.2f, 1.6f, GLASS_SHEEN),
            CarPart.Disc(CarPaint.DRIVER, 0f, 25.8f, 3.8f),
            CarPart.Box(CarPaint.BODY, -13.4f, 30f, 26.8f, 12f, 1.2f, ROOF_ROLL),
            CarPart.Box(
                CarPaint.BODY_SHADE, -13.2f, 40.6f, 26.4f, 1.4f, 0.6f, alpha = 0.32f
            ),
            CarPart.Box(CarPaint.GLASS, -12.2f, 41.8f, 24.4f, 6.4f, 1.4f, GLASS_SHEEN)
        ) + rearFace(top = 69f, halfTop = 15.6f, halfBottom = 14.6f)
    )

    /** Uzun, alcak gövde + arka kanat. */
    private val RACE_SEDAN = CarShapeDef(
        id = SHAPE_RACE_SEDAN,
        nameTr = "Yarış Sedan",
        nameEn = "Race Sedan",
        descriptionTr = "Uzun gövde, arka kanat, ikiz şerit",
        descriptionEn = "Long body, rear wing, twin stripes",
        priceCoins = 900,
        requiredCarLevel = 2,
        // GIRIS SEVIYESI: tablonun tek zayifsiz araci (fren ve boost notr).
        // Bilincli — ilk satin alinan arac tereddutsuz "iyi" hissettirmeli.
        // Ustunlugu kucuk kaliyor, 1500+ araclarin hepsi onu bir eksende
        // belirgin geciyor. CarCatalogTest bu istisnayi TEK arac icin
        // dondurur: ikinci bir zayifsiz arac eklenirse test kirilir.
        topSpeedMul = 1.04f,
        accelMul = 1.08f,
        brakeMul = 1.00f,
        boostMul = 1.00f,
        traitTr = "Hafif ve çevik — trafiğin arasından süzülmek için",
        traitEn = "Light and nimble — built for weaving through traffic",
        parts = listOf(
            CarPart.Box(CarPaint.TIRE, -20f, 10f, 7f, 21f, 2.2f, WHEEL_ROLL),
            CarPart.Box(CarPaint.TIRE, 13f, 10f, 7f, 21f, 2.2f, WHEEL_ROLL),
            CarPart.Box(CarPaint.TIRE, -20f, 46f, 7f, 22f, 2.2f, WHEEL_ROLL),
            CarPart.Box(CarPaint.TIRE, 13f, 46f, 7f, 22f, 2.2f, WHEEL_ROLL),
            CarPart.Box(CarPaint.BODY, -16f, 14f, 32f, 56f, 5f, BODY_ROLL),
            CarPart.Wedge(
                CarPaint.BODY,
                listOf(
                    CarPoint(-8f, -2f), CarPoint(8f, -2f),
                    CarPoint(16f, 20f), CarPoint(-16f, 20f)
                ),
                BODY_ROLL
            ),
            CarPart.Box(CarPaint.BODY_SHADE, -9f, -2f, 18f, 2.4f, 1f, NOSE_FADE),
            CarPart.Box(CarPaint.ACCENT, -5f, 0.6f, 3.4f, 19.4f, 0.9f, alpha = 0.88f),
            CarPart.Box(CarPaint.ACCENT, 1.6f, 0.6f, 3.4f, 19.4f, 0.9f, alpha = 0.88f),
            CarPart.Box(CarPaint.ACCENT, -5f, 46.6f, 3.4f, 11f, 0.9f, alpha = 0.88f),
            CarPart.Box(CarPaint.ACCENT, 1.6f, 46.6f, 3.4f, 11f, 0.9f, alpha = 0.88f),
            CarPart.Box(CarPaint.GLASS, -11.9f, 20f, 23.8f, 10.4f, 4f, GLASS_SHEEN),
            CarPart.Disc(CarPaint.DRIVER, 0f, 25f, 4f),
            CarPart.Box(CarPaint.BODY, -12.8f, 29.8f, 25.6f, 10.6f, 3f, ROOF_ROLL),
            CarPart.Box(
                CarPaint.BODY_SHADE, -12.6f, 38.9f, 25.2f, 1.5f, 0.7f, alpha = 0.32f
            ),
            CarPart.Box(CarPaint.GLASS, -11.4f, 39.9f, 22.8f, 6.6f, 3f, GLASS_SHEEN),
            CarPart.Box(CarPaint.GLOSS, -14.8f, 12f, 1.5f, 50f, 0.75f, alpha = 0.14f),
            // Arka kanat gövdenin USTUNDE durur: altina ince temas golgesi.
            CarPart.Box(
                CarPaint.BODY_SHADE, -17.4f, 62.6f, 34.8f, 3f, 1f, alpha = 0.5f
            ),
            CarPart.Box(CarPaint.BODY_SHADE, -17f, 58.4f, 34f, 4.6f, 1.6f, ROOF_ROLL),
            CarPart.Box(CarPaint.ACCENT, -16.6f, 58.6f, 33.2f, 1.2f, 0.6f, alpha = 0.6f)
        ) + rearFace(top = 68.4f, halfTop = 15.2f, halfBottom = 14.1f)
    )

    /** Köşeli, geniş omuzlu, kaput hava girişli kas arabası. */
    private val MUSCLE = CarShapeDef(
        id = SHAPE_MUSCLE,
        nameTr = "Kas Arabası",
        nameEn = "Muscle Car",
        descriptionTr = "Köşeli geniş gövde, kaput hava girişi",
        descriptionEn = "Boxy wide body, hood scoop",
        priceCoins = 1800,
        requiredCarLevel = 4,
        // Kas arabasi ailesi: yuksek son hiz, zayif ivme ve zayif fren.
        topSpeedMul = 1.08f,
        accelMul = 0.96f,
        brakeMul = 0.96f,
        boostMul = 1.00f,
        traitTr = "Düz yolda canavar, durdurmak ayrı bir mesele",
        traitEn = "A monster on the straight; stopping it is another matter",
        parts = listOf(
            CarPart.Box(CarPaint.TIRE, -20f, 9f, 7f, 22f, 1.8f, WHEEL_ROLL),
            CarPart.Box(CarPaint.TIRE, 13f, 9f, 7f, 22f, 1.8f, WHEEL_ROLL),
            CarPart.Box(CarPaint.TIRE, -20f, 45f, 7f, 24f, 1.8f, WHEEL_ROLL),
            CarPart.Box(CarPaint.TIRE, 13f, 45f, 7f, 24f, 1.8f, WHEEL_ROLL),
            // Kas arabasi kimligi kose YARICAPINDAN okunur: 2.5 (digerleri 5-6).
            CarPart.Box(CarPaint.BODY, -16.6f, 12f, 33.2f, 58f, 2.5f, BODY_ROLL),
            CarPart.Wedge(
                CarPaint.BODY,
                listOf(
                    CarPoint(-12f, -2f), CarPoint(12f, -2f),
                    CarPoint(16.6f, 16f), CarPoint(-16.6f, 16f)
                ),
                BODY_ROLL
            ),
            CarPart.Box(CarPaint.BODY_SHADE, -11.4f, -2f, 22.8f, 2.6f, 0.8f, NOSE_FADE),
            CarPart.Box(CarPaint.TRIM, -18.3f, 46f, 2.1f, 15f, 0.9f, WHEEL_ROLL, 0.85f),
            CarPart.Box(CarPaint.TRIM, 16.2f, 46f, 2.1f, 15f, 0.9f, WHEEL_ROLL, 0.85f),
            CarPart.Box(CarPaint.BODY_SHADE, -4.2f, 10f, 8.4f, 9f, 1.6f, NOSE_FADE),
            CarPart.Box(CarPaint.GLOSS, -4f, 10.2f, 8f, 0.9f, 0.4f, alpha = 0.4f),
            CarPart.Box(CarPaint.ACCENT, -6.6f, 0f, 3.6f, 8.6f, 0.7f, alpha = 0.88f),
            CarPart.Box(CarPaint.ACCENT, 3f, 0f, 3.6f, 8.6f, 0.7f, alpha = 0.88f),
            CarPart.Box(CarPaint.ACCENT, -6.6f, 47.4f, 3.6f, 19f, 0.7f, alpha = 0.88f),
            CarPart.Box(CarPaint.ACCENT, 3f, 47.4f, 3.6f, 19f, 0.7f, alpha = 0.88f),
            CarPart.Box(CarPaint.GLASS, -12.5f, 22.4f, 25f, 9.6f, 2.5f, GLASS_SHEEN),
            CarPart.Disc(CarPaint.DRIVER, 0f, 27.2f, 4f),
            CarPart.Box(CarPaint.BODY, -13.4f, 31.2f, 26.8f, 10f, 2f, ROOF_ROLL),
            CarPart.Box(
                CarPaint.BODY_SHADE, -13.2f, 40.2f, 26.4f, 1.5f, 0.7f, alpha = 0.32f
            ),
            CarPart.Box(CarPaint.GLASS, -12f, 41.2f, 24f, 6.2f, 2f, GLASS_SHEEN),
            CarPart.Box(CarPaint.GLOSS, -15.4f, 11f, 1.6f, 52f, 0.75f, alpha = 0.14f)
        ) + rearFace(top = 68.8f, halfTop = 16.2f, halfBottom = 15f)
    )

    /**
     * Station wagon (proje sahibi karari, 2026-08-15). TAMAMEN KOZMETIK —
     * kutu, hitbox ve fizik digerleriyle birebir ayni; "daha cok esya tasir"
     * gibi bir oyun etkisi YOK.
     *
     * SW kimligi ustten bakista uc isaretten okunuyor:
     *
     *  1. **Tavan 22 birim** (digerlerinde ~10-12) ve arkaya dogru DUZ devam
     *     ediyor; govdenin arkasi daralmiyor. Siluetin yarisi tavan.
     *  2. **Tavan bagaj citalari**: iki uzunlamasina ACCENT rayi + iki TRIM
     *     capraz cubugu. 32 px'de bile tavanin uzerindeki iki koyu cizgi
     *     okunuyor ve baska hicbir govdede (trafik dahil) boyle bir sey yok.
     *  3. **Arka yan camlar**: tavanin iki yanindaki ince GLASS seritleri —
     *     sedanda olmayan ucuncu cam. Arkasinda genis bagaj kapagi cami var.
     *
     * Beyaz boyayla ([COLOR_GLACIER]) tasarlandi. Trafikteki beyaz engel
     * (`FFFFFF`) ile karismamasi icin oyuncu beyazi bilerek soguk ve KIRIK
     * (`EDF1F5`); ustune bu govdenin koyu tavan raylari trafikte hic
     * bulunmayan bir isaret.
     */
    private val MOUNTAIN_GOAT = CarShapeDef(
        id = SHAPE_MOUNTAIN_GOAT,
        nameTr = "Dağ Keçisi",
        nameEn = "Mountain Goat",
        descriptionTr = "Uzun tavanlı station wagon, tavan bagajı",
        descriptionEn = "Long-roof wagon with roof rails",
        priceCoins = 1500,
        requiredCarLevel = 2,
        // Agir ama tutusu iyi: kataloğun EN IYI freni, en kotu ivmesi.
        topSpeedMul = 1.00f,
        accelMul = 0.94f,
        brakeMul = 1.12f,
        boostMul = 1.06f,
        traitTr = "Ağır kalkar, ama frende kimse onu geçemez",
        traitEn = "Slow off the line, unbeatable under braking",
        // Beyaz tasarlandi (bkz. yukaridaki not: tavan raylari beyaz gövdede
        // okunsun diye koyu birakildi).
        defaultColorId = COLOR_GLACIER,
        parts = listOf(
            // Kalin lastik: yaricap 2.0, arka aks one gore genis.
            CarPart.Box(CarPaint.TIRE, -20f, 11f, 7f, 21f, 2f, WHEEL_ROLL),
            CarPart.Box(CarPaint.TIRE, 13f, 11f, 7f, 21f, 2f, WHEEL_ROLL),
            CarPart.Box(CarPaint.TIRE, -20f, 46f, 7f, 22f, 2f, WHEEL_ROLL),
            CarPart.Box(CarPaint.TIRE, 13f, 46f, 7f, 22f, 2f, WHEEL_ROLL),
            CarPart.Box(CarPaint.BODY, -16.2f, 12f, 32.4f, 58f, 2.2f, BODY_ROLL),
            CarPart.Wedge(
                CarPaint.BODY,
                listOf(
                    CarPoint(-13f, -2f), CarPoint(13f, -2f),
                    CarPoint(16.2f, 14f), CarPoint(-16.2f, 14f)
                ),
                BODY_ROLL
            ),
            CarPart.Box(CarPaint.BODY_SHADE, -12.4f, -2f, 24.8f, 2.4f, 0.8f, NOSE_FADE),
            // Izgara tamponun HEMEN arkasinda: 6'ya koyulunca beyaz kaputun
            // ortasinda yuzen bir kutu gibi okunuyordu.
            CarPart.Box(CarPaint.BODY_SHADE, -8f, 1f, 16f, 3.6f, 0.9f, NOSE_FADE),
            // Vurgu camlardan ONCE: ortada camlarin altinda kalsin, yalnizca
            // on ve arka omuzda gorunsun.
            CarPart.Box(CarPaint.GLOSS, -14.8f, 14f, 1.5f, 52f, 0.75f, alpha = 0.14f),
            CarPart.Box(CarPaint.GLASS, -12.4f, 20.6f, 24.8f, 9.6f, 2.4f, GLASS_SHEEN),
            CarPart.Disc(CarPaint.DRIVER, 0f, 25.2f, 4f),
            // UZUN tavan — siluetin SW okunmasini saglayan ana kutle.
            CarPart.Box(CarPaint.BODY, -13.2f, 30f, 26.4f, 22f, 2f, ROOF_ROLL),
            // Tavan bagaji. Raylar TAM opak: 32 px'de beyaz tavanin uzerindeki
            // iki koyu cizgi bu govdenin tek okunur imzasi, alfa ile
            // zayiflatilirsa trafikteki beyaz aracla karisiyor. Capraz cubuklar
            // bilerek sonuk (0.5) — 4x'te bagaji tamamliyorlar, oyun olceginde
            // gurultu yapmiyorlar.
            // Raylar tavanin ICINDE: kenara yapisirlarsa yandaki arka camla
            // tek kalin koyu kenar gibi okunuyorlar, aralarindaki beyaz seride
            // ihtiyac var.
            CarPart.Box(CarPaint.ACCENT, -10.6f, 30.6f, 2f, 20.4f, 0.8f),
            CarPart.Box(CarPaint.ACCENT, 8.6f, 30.6f, 2f, 20.4f, 0.8f),
            CarPart.Box(CarPaint.TRIM, -10.6f, 34f, 21.2f, 0.9f, 0.4f, alpha = 0.5f),
            CarPart.Box(CarPaint.TRIM, -10.6f, 47.4f, 21.2f, 0.9f, 0.4f, alpha = 0.5f),
            // Arka yan camlar: tavanin YANINDA, omuz duzleminde.
            CarPart.Box(CarPaint.GLASS, -15.6f, 34f, 2.2f, 14f, 0.9f, GLASS_SHEEN),
            CarPart.Box(CarPaint.GLASS, 13.4f, 34f, 2.2f, 14f, 0.9f, GLASS_SHEEN),
            CarPart.Box(
                CarPaint.BODY_SHADE, -13f, 51.4f, 26f, 1.4f, 0.6f, alpha = 0.32f
            ),
            // Genis bagaj kapagi cami + altinda garnitur cizgisi.
            CarPart.Box(CarPaint.GLASS, -12.6f, 52.6f, 25.2f, 8.4f, 1.8f, GLASS_SHEEN),
            CarPart.Box(CarPaint.ACCENT, -10f, 63.6f, 20f, 2f, 0.8f, alpha = 0.85f)
        ) + rearFace(top = 68.8f, halfTop = 15.8f, halfBottom = 14.8f)
    )

    /**
     * Ikinci kas arabasi (proje sahibi karari, 2026-08-15). TAMAMEN KOZMETIK —
     * kutu, hitbox ve fizik digerleriyle birebir ayni.
     *
     * ## Marka notu (pazarlik disi)
     *
     * Bu gövde 60'lar/70'ler Amerikan kas arabasi *dilini* kullanir (uzun
     * kaput, kisa bagaj, genis omuz, dortlu far, kaput cikintisi, kalin orta
     * serit) ama hicbir gercek modelin kopyasi DEGILDIR. Kod, isim ve magaza
     * metinlerinde tescilli bir marka adi gecmez; id bilerek notr
     * ([SHAPE_MUSCLE_67]). Ad "Boğa 67" GECICI — sahibi uc aday arasindan
     * secmeden kalicilastirma.
     *
     * ## Mevcut [MUSCLE]'dan siluetle nasil ayriliyor (32 px'lik test)
     *
     *  1. **Tek KALIN orta serit** (9.4 birim, kaput + tavan + bagaj boyunca)
     *     — Kas Arabasi'nda IKI INCE serit var (3.6 birim, kenarlarda). 32
     *     px'te "ortada tek kalin parlak bar" ile "iki ince cizgi" ilk bakista
     *     ayrilan iki desendir.
     *  2. **Kola-sisesi bel.** Gövde tek poligon: omuz 33.2 -> bel 29.6 ->
     *     kalca 33.8. Kas Arabasi'nin yanlari BASTAN SONA duz (33.2 sabit).
     *     Siluetin kendisi farkli, dolgudan bagimsiz.
     *  3. **Uzun kaput / kisa bagaj.** Kaput 28 birim, bagaj 15.1 (oran 1.85);
     *     Kas Arabasi'nda 24.4 / 21.4 (oran 1.14). Kokpit belirgin sekilde
     *     ARKADA.
     *  4. **Fastback arka cam** 8.6 birim (Kas Arabasi 6.2) — tavan kisa,
     *     cam uzun.
     *  5. **Kararmis izgara + DORT yuvarlak far.** Kas Arabasi'nda far yok;
     *     Kus SLX'te IKI DIKDORTGEN far var. Dort yuvarlak nokta ucunu de
     *     ayirir.
     *  6. **Arka imza**: stop lambalari kosellere itilmis ([rearFace] tailInset
     *     6.2), ortada iki krom egzoz ucu. Kas Arabasi'nda lambalar ortaya
     *     kadar gelir ve egzozlar YANDA uzun krom cubuklardir.
     */
    private val MUSCLE_67 = CarShapeDef(
        id = SHAPE_MUSCLE_67,
        // GECICI ad — sahibi onaylayana kadar (adaylar: Boğa 67 / Yıldırım GT /
        // Demirtay). Ozel ad oldugu icin iki dilde de ayni.
        nameTr = "Boğa 67",
        nameEn = "Boğa 67",
        descriptionTr = "Uzun kaput, geniş kalça, kalın orta şerit",
        descriptionEn = "Long hood, wide hips, bold centre stripe",
        // 1800 (Kas Arabasi) ile 3200 (Super Araba) arasindaki EN BUYUK bosluga
        // oturuyor; merdiven uzamadi, yeni basamak acildi.
        priceCoins = 2400,
        requiredCarLevel = 5,
        // Kas ailesinin AGIR kanadi (tabloda yok; profil kas arabasi
        // mantigindan turetildi, gerekce `docs/BALANCE.md`):
        //  - son hiz 1.10  -> Kas Arabasi (1.08) ile Super Araba (1.12) ARASI,
        //                     fiyat merdiveniyle ayni sirada.
        //  - ivme 0.92     -> ailenin en agiri; en dusuk ivme burada.
        //  - fren 0.90     -> "yuksek son hiz, zayif fren" kuralinin ucu;
        //                     katalogun en kotu freni, band alt siniri 0.80'in
        //                     icinde.
        //  - boost 1.04    -> buyuk depo. Super Araba'ya karsi TEK ustunlugu;
        //                     olmasaydi 2400'luk basamak "ucuz Super Araba"
        //                     olurdu ve 3200'u almanin anlami kalmazdi.
        topSpeedMul = 1.10f,
        accelMul = 0.92f,
        brakeMul = 0.90f,
        boostMul = 1.04f,
        traitTr = "Ağır kas: gaza yaslanır, yavaşlamayı hiç sevmez",
        traitEn = "Heavy muscle: leans on the throttle, hates slowing down",
        parts = listOf(
            // Kas duruşu: on lastik dar, arka lastik BELIRGIN daha uzun.
            CarPart.Box(CarPaint.TIRE, -20f, 10f, 7f, 21f, 2f, WHEEL_ROLL),
            CarPart.Box(CarPaint.TIRE, 13f, 10f, 7f, 21f, 2f, WHEEL_ROLL),
            CarPart.Box(CarPaint.TIRE, -20f, 44f, 7f, 25f, 2f, WHEEL_ROLL),
            CarPart.Box(CarPaint.TIRE, 13f, 44f, 7f, 25f, 2f, WHEEL_ROLL),
            // Gövde TEK poligon: kola sisesi. Kutu + kama yerine tek parca,
            // cunku bel daralmasi ancak kesintisiz bir konturla okunuyor.
            CarPart.Wedge(
                CarPaint.BODY,
                listOf(
                    CarPoint(-14.8f, -2f), CarPoint(14.8f, -2f),
                    CarPoint(16.6f, 14f), CarPoint(14.8f, 30f),
                    CarPoint(14.8f, 40f), CarPoint(16.9f, 56f),
                    CarPoint(16.9f, 70f), CarPoint(-16.9f, 70f),
                    CarPoint(-16.9f, 56f), CarPoint(-14.8f, 40f),
                    CarPoint(-14.8f, 30f), CarPoint(-16.6f, 14f)
                ),
                BODY_ROLL
            ),
            CarPart.Box(CarPaint.BODY_SHADE, -13.8f, -2f, 27.6f, 2.4f, 0.8f, NOSE_FADE),
            // Neredeyse tam genislikte KARARMIS izgara bandi.
            CarPart.Box(CarPaint.BODY_SHADE, -12.8f, 0.6f, 25.6f, 4.2f, 0.8f, NOSE_FADE),
            // Dortlu far: TRIM cerceve + GLOSS lens. 32 px'te burunda dort
            // parlak nokta — ne Kas Arabasi'nda (far yok) ne Kus SLX'te
            // (iki dikdortgen) boyle bir desen var.
            CarPart.Disc(CarPaint.TRIM, -11f, 2.7f, 1.6f),
            CarPart.Disc(CarPaint.TRIM, -7f, 2.7f, 1.6f),
            CarPart.Disc(CarPaint.TRIM, 7f, 2.7f, 1.6f),
            CarPart.Disc(CarPaint.TRIM, 11f, 2.7f, 1.6f),
            CarPart.Disc(CarPaint.GLOSS, -11f, 2.7f, 1.05f),
            CarPart.Disc(CarPaint.GLOSS, -7f, 2.7f, 1.05f),
            CarPart.Disc(CarPaint.GLOSS, 7f, 2.7f, 1.05f),
            CarPart.Disc(CarPaint.GLOSS, 11f, 2.7f, 1.05f),
            // KALIN orta serit — bu govdenin tek numarali imzasi.
            CarPart.Box(CarPaint.ACCENT, -4.7f, 5.6f, 9.4f, 20.4f, 1.2f, alpha = 0.9f),
            // Kaput cikintisi: seridin UZERINDE iki koyu agiz. Serit disina
            // tasmiyor ki 32 px'te "kalin bar + iki delik" olarak okunsun.
            CarPart.Box(CarPaint.GLOSS, -4.4f, 9.4f, 8.8f, 0.9f, 0.4f, alpha = 0.45f),
            CarPart.Box(CarPaint.BODY_SHADE, -4f, 10f, 3.6f, 8.6f, 0.9f, NOSE_FADE),
            CarPart.Box(CarPaint.BODY_SHADE, 0.4f, 10f, 3.6f, 8.6f, 0.9f, NOSE_FADE),
            // Omuz vurgusu BELE gore hizalanir (-14.4), gövde kenarina degil:
            // kola sisesi oldugu icin kenar y'ye gore geziyor, kenara
            // yapistirilirsa belde gövdenin DISINA tasip yuzen bir sliver
            // gibi okunuyor (ilk denemede oyle oldu).
            CarPart.Box(CarPaint.GLOSS, -14.4f, 16f, 1.5f, 46f, 0.75f, alpha = 0.14f),
            // Kokpit ARKADA: kaput 28 birim, bagaj 15.1.
            CarPart.Box(CarPaint.GLASS, -12.4f, 26f, 24.8f, 8.6f, 2.6f, GLASS_SHEEN),
            CarPart.Disc(CarPaint.DRIVER, 0f, 30.2f, 4f),
            CarPart.Box(CarPaint.BODY, -13.2f, 34.6f, 26.4f, 10.2f, 2.2f, ROOF_ROLL),
            CarPart.Box(CarPaint.ACCENT, -4.7f, 34.9f, 9.4f, 9.6f, 1f, alpha = 0.9f),
            CarPart.Box(
                CarPaint.BODY_SHADE, -13f, 43.6f, 26f, 1.5f, 0.7f, alpha = 0.32f
            ),
            // Fastback: arka cam tavandan UZUN.
            CarPart.Box(CarPaint.GLASS, -12.2f, 44.9f, 24.4f, 8.6f, 2.2f, GLASS_SHEEN),
            CarPart.Box(CarPaint.ACCENT, -4.7f, 53.8f, 9.4f, 13.4f, 1.2f, alpha = 0.9f),
            // Ordek kuyrugu bagaj dudagi — kanat DEGIL (kanat Yaris Sedan ve
            // Super Araba'nin imzasi), gövdeye yapisik ince bir kabarma.
            CarPart.Box(CarPaint.BODY_SHADE, -16.6f, 66.6f, 33.2f, 2f, 0.8f, alpha = 0.38f)
        ) + rearFace(
            top = 68.6f, halfTop = 16.9f, halfBottom = 15.7f,
            // Lambalar kosellere itilir; ortada egzozlar icin yer acilir.
            tailInset = 6.2f
        ) + listOf(
            // Kucuk tutuluyor: buyurse stop lambalarinin yanindaki iki BEYAZ
            // blok gibi okunuyor ve arka yuzu gurultuye bogar.
            CarPart.Box(CarPaint.TRIM, -4.6f, 70.4f, 3.6f, 2.4f, 1.1f, WHEEL_ROLL, 0.85f),
            CarPart.Box(CarPaint.TRIM, 1f, 70.4f, 3.6f, 2.4f, 1.1f, WHEEL_ROLL, 0.85f)
        )
    )

    /** Kama burun, genişleyen arka, büyük spoiler. */
    private val SUPERCAR = CarShapeDef(
        id = SHAPE_SUPERCAR,
        nameTr = "Süper Araba",
        nameEn = "Supercar",
        descriptionTr = "Kama burun, geniş arka, büyük spoiler",
        descriptionEn = "Wedge nose, wide rear, big spoiler",
        priceCoins = 3200,
        requiredCarLevel = 6,
        // Katalogun en hizlisi ve en kivragi; bedeli fren.
        topSpeedMul = 1.12f,
        accelMul = 1.10f,
        brakeMul = 0.94f,
        boostMul = 1.00f,
        traitTr = "En hızlısı ve en kıvrağı — tek hatayı affetmez",
        traitEn = "Fastest and sharpest here — it forgives nothing",
        parts = listOf(
            CarPart.Box(CarPaint.TIRE, -20f, 12f, 7f, 19f, 2.6f, WHEEL_ROLL),
            CarPart.Box(CarPaint.TIRE, 13f, 12f, 7f, 19f, 2.6f, WHEEL_ROLL),
            CarPart.Box(CarPaint.TIRE, -20f, 46f, 7f, 23f, 2.6f, WHEEL_ROLL),
            CarPart.Box(CarPaint.TIRE, 13f, 46f, 7f, 23f, 2.6f, WHEEL_ROLL),
            // Kama burun -> daralan bel -> genisleyen arka omuz.
            CarPart.Wedge(
                CarPaint.BODY,
                listOf(
                    CarPoint(-6.5f, -2f), CarPoint(6.5f, -2f),
                    CarPoint(14f, 26f), CarPoint(-14f, 26f)
                ),
                BODY_ROLL
            ),
            CarPart.Box(CarPaint.BODY, -14f, 24f, 28f, 22f, 3f, BODY_ROLL),
            CarPart.Wedge(
                CarPaint.BODY,
                listOf(
                    CarPoint(-14f, 44f), CarPoint(14f, 44f),
                    CarPoint(16.8f, 70f), CarPoint(-16.8f, 70f)
                ),
                BODY_ROLL
            ),
            CarPart.Box(CarPaint.BODY_SHADE, -6f, -2f, 12f, 2f, 0.8f, NOSE_FADE),
            CarPart.Box(CarPaint.BODY_SHADE, -14.8f, 47f, 3f, 13f, 1.2f, NOSE_FADE),
            CarPart.Box(CarPaint.BODY_SHADE, 11.8f, 47f, 3f, 13f, 1.2f, NOSE_FADE),
            CarPart.Box(CarPaint.ACCENT, -2.2f, 0f, 4.4f, 21f, 0.9f, alpha = 0.88f),
            CarPart.Box(CarPaint.ACCENT, -2.2f, 46.6f, 4.4f, 10f, 0.9f, alpha = 0.88f),
            CarPart.Box(CarPaint.GLASS, -10.9f, 21.6f, 21.8f, 10f, 4f, GLASS_SHEEN),
            CarPart.Disc(CarPaint.DRIVER, 0f, 26.4f, 4f),
            CarPart.Box(CarPaint.BODY, -11.6f, 30.8f, 23.2f, 10f, 3f, ROOF_ROLL),
            CarPart.Box(
                CarPaint.BODY_SHADE, -11.4f, 39.8f, 22.8f, 1.5f, 0.7f, alpha = 0.32f
            ),
            CarPart.Box(CarPaint.GLASS, -11.6f, 40.8f, 23.2f, 6f, 3f, GLASS_SHEEN),
            CarPart.Box(CarPaint.GLOSS, -12.9f, 14f, 1.5f, 48f, 0.75f, alpha = 0.14f),
            CarPart.Box(
                CarPaint.BODY_SHADE, -17.8f, 62.2f, 35.6f, 3.4f, 1f, alpha = 0.5f
            ),
            CarPart.Box(CarPaint.BODY_SHADE, -17.6f, 57.4f, 35.2f, 5f, 1.8f, ROOF_ROLL),
            CarPart.Box(CarPaint.ACCENT, -17.2f, 57.6f, 34.4f, 1.3f, 0.6f, alpha = 0.6f)
        ) + rearFace(top = 69.4f, halfTop = 16.8f, halfBottom = 15.6f)
    )

    /**
     * Liste sirasi = garajdaki sira = FIYAT merdiveni (test bunu zorunlu
     * kiliyor: fiyat kesin artan, seviye sarti geri gitmez).
     *
     *   Sehir 0/lv1 · Yaris Sedan 900/lv2 · Kus SLX 1500/lv2 ·
     *   Dag Kecisi 1500/lv2 · Kas Arabasi 1800/lv4 · Boga 67 2400/lv5 ·
     *   Super Araba 3200/lv6
     *
     * Yeni govdeler merdiveni UZATMIYOR, mevcut basamaklara ya da aralarindaki
     * bosluklara oturuyor. Boga 67 (2026-08-15) 1800 ile 3200 arasindaki EN
     * GENIS bosluga girdi: Kas Arabasi'ni alan oyuncunun onunde 1400 coinlik
     * bos bir ucurum vardi, artik ara bir hedef var. Hepsi yalnizca kozmetik.
     */
    val shapes: List<CarShapeDef> = listOf(
        HATCHBACK, RACE_SEDAN, KUS_SLX, MOUNTAIN_GOAT, MUSCLE, MUSCLE_67, SUPERCAR
    )

    // -----------------------------------------------------------------
    // Trafik araci
    // -----------------------------------------------------------------
    //
    // Oyuncu ile AYNI cizim boru hattini kullanir (tek cizici, tek veri
    // modeli) ama [shapes] listesinde DEGILDIR — garajda gorunmez, satin
    // alinamaz. Silueti bilerek daha KUTU (kose yaricapi 1.2-2, kunt burun)
    // ki 60 Hz'de oyuncu araciyla karismasin; palet ayrimi da korunuyor
    // (govde GameEngine.OBSTACLE_COLORS'tan, surucu basi mavi).

    private const val TRAFFIC_SHADE_ARGB: Long = 0xFF0A0D11
    private const val TRAFFIC_DRIVER_ARGB: Long = 0xFF8ECAE6

    /**
     * Trafik govdesinin kimligi. Sabit, cunku cizici sprite'i bu adla ariyor
     * ([com.miniappfactory.krondrive.ui.common.CarSpriteSet]); elle yazilmis
     * bir dize iki yerde birden degistirilmeyi bekliyordu.
     */
    const val TRAFFIC_SHAPE_ID = "traffic"

    val trafficShape = CarShapeDef(
        id = TRAFFIC_SHAPE_ID,
        nameTr = "Trafik",
        nameEn = "Traffic",
        descriptionTr = "Yoldaki engel aracı",
        descriptionEn = "Road traffic obstacle",
        priceCoins = 0,
        requiredCarLevel = 1,
        parts = listOf(
            CarPart.Box(CarPaint.TIRE, -20f, 12f, 7f, 20f, 1.6f, WHEEL_ROLL),
            CarPart.Box(CarPaint.TIRE, 13f, 12f, 7f, 20f, 1.6f, WHEEL_ROLL),
            CarPart.Box(CarPaint.TIRE, -20f, 47f, 7f, 21f, 1.6f, WHEEL_ROLL),
            CarPart.Box(CarPaint.TIRE, 13f, 47f, 7f, 21f, 1.6f, WHEEL_ROLL),
            CarPart.Box(CarPaint.BODY, -15.8f, 12f, 31.6f, 57f, 2f, BODY_ROLL),
            CarPart.Wedge(
                CarPaint.BODY,
                listOf(
                    CarPoint(-13f, -2f), CarPoint(13f, -2f),
                    CarPoint(15.8f, 14f), CarPoint(-15.8f, 14f)
                ),
                BODY_ROLL
            ),
            CarPart.Box(CarPaint.BODY_SHADE, -12.4f, -2f, 24.8f, 2.4f, 0.8f, NOSE_FADE),
            CarPart.Box(CarPaint.GLASS, -12.4f, 21f, 24.8f, 10f, 2.5f, GLASS_SHEEN),
            CarPart.Disc(CarPaint.DRIVER, 0f, 25.8f, 4f),
            CarPart.Box(CarPaint.BODY, -12.8f, 30.2f, 25.6f, 10.4f, 2f, ROOF_ROLL),
            CarPart.Box(
                CarPaint.BODY_SHADE, -12.6f, 39.2f, 25.2f, 1.5f, 0.7f, alpha = 0.32f
            ),
            CarPart.Box(CarPaint.GLASS, -11.8f, 40.2f, 23.6f, 6.4f, 2f, GLASS_SHEEN),
            CarPart.Box(CarPaint.ACCENT, -3f, 1f, 6f, 18f, 1f, alpha = 0.7f),
            CarPart.Box(CarPaint.GLOSS, -14.4f, 14f, 1.5f, 50f, 0.75f, alpha = 0.14f)
        ) + rearFace(top = 68.6f, halfTop = 15.5f, halfBottom = 14.3f)
    )

    /** Renk basina tek stil — 60 Hz'de her engel icin tahsis yapilmasin diye. */
    private val trafficStyles = HashMap<Long, CarStyle>()

    /**
     * Verilen trafik govde rengiyle cizilebilir bir stil. Bu boya [colors]
     * listesinde DEGILDIR (garajda cikmaz, satin alinamaz) — yalnizca
     * cizicinin bekledigi tipi saglar.
     */
    fun trafficStyle(bodyArgb: Long): CarStyle = trafficStyles.getOrPut(bodyArgb) {
        CarStyle(
            shape = trafficShape,
            color = CarColorDef(
                id = "traffic",
                nameTr = "Trafik",
                nameEn = "Traffic",
                priceCoins = 0,
                requiredCarLevel = 1,
                bodyArgb = bodyArgb,
                shadeArgb = TRAFFIC_SHADE_ARGB,
                accentArgb = 0xFFFFFFFF,
                driverArgb = TRAFFIC_DRIVER_ARGB
            )
        )
    }

    // -----------------------------------------------------------------
    // Boyalar
    // -----------------------------------------------------------------
    //
    // Sanat yonu kurali: TRAFIK renkleri oyuncuya verilmez. Engel araclari
    // sari (FFD60A), camgobegi (00C2FF), beyaz (FFFFFF) ve turuncu (FF7B00)
    // — bkz. GameEngine.OBSTACLE_COLORS. Oyuncu paleti bilerek bu tonlarin
    // DISINDA secildi; tehdit ile oyuncu 60 Hz'de karismasin diye.

    val colors: List<CarColorDef> = listOf(
        CarColorDef(
            id = COLOR_KRON_RED,
            nameTr = "Kron Kırmızısı",
            nameEn = "Kron Red",
            priceCoins = 0,
            requiredCarLevel = 1,
            bodyArgb = 0xFFE10600,
            shadeArgb = 0xFF8E0400,
            accentArgb = 0xFFFFFFFF
        ),
        CarColorDef(
            id = COLOR_GRAPHITE,
            nameTr = "Grafit",
            nameEn = "Graphite",
            priceCoins = 250,
            requiredCarLevel = 1,
            bodyArgb = 0xFF39414F,
            shadeArgb = 0xFF1E242E,
            accentArgb = 0xFFFFD33D
        ),
        CarColorDef(
            id = COLOR_EMERALD,
            nameTr = "Zümrüt",
            nameEn = "Emerald",
            priceCoins = 400,
            requiredCarLevel = 1,
            bodyArgb = 0xFF00A05A,
            shadeArgb = 0xFF00623A,
            accentArgb = 0xFFEAF1FB
        ),
        // Beyaz — [MOUNTAIN_GOAT] icin tasarlandi (proje sahibi: "varsayilan
        // boyasi BEYAZ olmali"). Trafikteki beyaz engel TAM beyaz (FFFFFF);
        // oyuncu beyazi bilerek KIRIK ve soguk, ustelik golgesi belirgin gri
        // — 60 Hz'de tehditle ayni okunmasin diye. Kural geregi kontrol
        // edildi: `oyuncu paleti trafik renklerini kullanmaz` testi bunu
        // ayrica dogruluyor.
        CarColorDef(
            id = COLOR_GLACIER,
            nameTr = "Buzul Beyazı",
            nameEn = "Glacier White",
            priceCoins = 500,
            requiredCarLevel = 1,
            bodyArgb = 0xFFEDF1F5,
            shadeArgb = 0xFF98A2AE,
            // Tavan raylari/garnitur koyu okunsun: beyaz uzerine beyaz serit
            // gorunmez olurdu.
            accentArgb = 0xFF2A3340
        ),
        CarColorDef(
            id = COLOR_MAGENTA,
            nameTr = "Neon Magenta",
            nameEn = "Neon Magenta",
            priceCoins = 600,
            requiredCarLevel = 2,
            bodyArgb = 0xFFE5007D,
            shadeArgb = 0xFF8E004D,
            accentArgb = 0xFFFFFFFF
        ),
        CarColorDef(
            id = COLOR_ROYAL,
            nameTr = "Kraliyet Mavisi",
            nameEn = "Royal Blue",
            priceCoins = 850,
            requiredCarLevel = 2,
            bodyArgb = 0xFF1B3FD1,
            shadeArgb = 0xFF102585,
            accentArgb = 0xFFEAF1FB
        ),
        CarColorDef(
            id = COLOR_VIOLET,
            nameTr = "Mor",
            nameEn = "Violet",
            priceCoins = 1100,
            requiredCarLevel = 3,
            bodyArgb = 0xFF7B2FF7,
            shadeArgb = 0xFF4A1C96,
            accentArgb = 0xFFEAF1FB
        ),
        CarColorDef(
            id = COLOR_TEAL,
            nameTr = "Petrol",
            nameEn = "Teal",
            priceCoins = 1400,
            requiredCarLevel = 3,
            bodyArgb = 0xFF0E7C7B,
            shadeArgb = 0xFF084A49,
            accentArgb = 0xFFEAF1FB
        ),
        CarColorDef(
            id = COLOR_MIDNIGHT,
            nameTr = "Gece Siyahı",
            nameEn = "Midnight",
            priceCoins = 1750,
            requiredCarLevel = 4,
            bodyArgb = 0xFF14161C,
            shadeArgb = 0xFF08090E,
            accentArgb = 0xFFF5C100
        ),
        CarColorDef(
            id = COLOR_KHAKI,
            nameTr = "Haki",
            nameEn = "Khaki",
            priceCoins = 2200,
            requiredCarLevel = 5,
            bodyArgb = 0xFF5B6B2E,
            shadeArgb = 0xFF36401B,
            accentArgb = 0xFFEAF1FB
        )
    )

    val defaultShape: CarShapeDef = shapes.first { it.id == DEFAULT_SHAPE_ID }
    val defaultColor: CarColorDef = colors.first { it.id == DEFAULT_COLOR_ID }
    val defaultStyle: CarStyle = CarStyle(defaultShape, defaultColor)

    // -----------------------------------------------------------------
    // Garaj cubuklari — MUTLAK degil KARSILASTIRMALI
    // -----------------------------------------------------------------
    //
    // Carpanlar 0.90–1.12 arasinda geziyor; bunu dogrudan cubuga cevirmek
    // ("%112 dolu") yedi aracin da neredeyse ayni gorunmesine yol acardi ve
    // oyuncunun sordugu soruyu ("neden super araba alayim?") yine
    // cevaplamazdi. Bu yuzden her eksen KATALOG ICINDE normalize edilir:
    // o eksenin en iyisi dolu, en kotusu [STAT_BAR_FLOOR] kadar kisa.
    //
    // Taban sifir DEGIL: sifir uzunluktaki cubuk "veri yok" gibi okunur,
    // oysa en kotu arac da yolda gidiyor.

    private const val STAT_BAR_FLOOR = 0.22f

    /** Eksen basina (en kucuk, en buyuk) carpan — [shapes] uzerinden turetilir. */
    private val statRange: Map<UpgradeType, Pair<Float, Float>> =
        UpgradeType.entries.associateWith { type ->
            val values = shapes.map { it.multiplier(type) }
            values.min() to values.max()
        }

    /** Garaj cubugunun dolulugu (0..1) — katalogdaki digerlerine GORE. */
    fun statFraction(shape: CarShapeDef, type: UpgradeType): Float {
        val (min, max) = statRange.getValue(type)
        if (max - min < 1e-4f) return 1f
        val t = (shape.multiplier(type) - min) / (max - min)
        return STAT_BAR_FLOOR + (1f - STAT_BAR_FLOOR) * t.coerceIn(0f, 1f)
    }

    /** Referans araca gore yuzde fark: 1.12 -> 12, 0.94 -> -6, 1.0 -> 0. */
    fun statDeltaPercent(shape: CarShapeDef, type: UpgradeType): Int =
        ((shape.multiplier(type) - 1f) * 100f).roundToInt()

    // -----------------------------------------------------------------
    // Arama ve sahiplik (saf mantik — repository ve garaj bunu kullanir)
    // -----------------------------------------------------------------

    /** Bilinmeyen/eski id varsayilana duser (kayit bozulmasin diye). */
    fun shape(id: String?): CarShapeDef = shapes.firstOrNull { it.id == id } ?: defaultShape

    fun color(id: String?): CarColorDef = colors.firstOrNull { it.id == id } ?: defaultColor

    /** Bedava olanlar satin alinmadan da sahiplenilmis sayilir. */
    fun isOwned(item: CarItem, owned: Set<String>): Boolean =
        item.priceCoins == 0 || item.id in owned

    /**
     * Gercekten satin alinan boyalar + sahip olunan gövdelerin FABRIKA
     * BOYALARI (bkz. [CarShapeDef.defaultColorId]).
     *
     * Garaj, secim dogrulamasi ve satin alma durumu hep bu genisletilmis
     * kumeye bakar; boylece kural tek yerde kaliyor. Ornek: Kuş SLX'i alan
     * oyuncu Petrol boyasini da acmis olur, ama Petrol'u baska gövdelere de
     * surebilir — fabrika boyasi gövdeye kilitli DEGIL, hediye.
     */
    fun effectiveOwnedColors(ownedShapes: Set<String>, ownedColors: Set<String>): Set<String> =
        ownedColors + shapes.filter { isOwned(it, ownedShapes) }.map { it.defaultColorId }

    /**
     * Kayitli secim gecerli mi: yoksa, bilinmiyorsa veya sahip olunmuyorsa
     * VARSAYILANA duser. Boylece silinen bir icerik ya da elle bozulmus kayit
     * oyunu kirmaz.
     */
    fun selectedShape(id: String?, owned: Set<String>): CarShapeDef =
        shapes.firstOrNull { it.id == id && isOwned(it, owned) } ?: defaultShape

    fun selectedColor(id: String?, owned: Set<String>): CarColorDef =
        colors.firstOrNull { it.id == id && isOwned(it, owned) } ?: defaultColor

    fun style(shapeId: String?, colorId: String?): CarStyle =
        CarStyle(shape(shapeId), color(colorId))

    /** Garajdaki rozet/buton durumu. */
    fun stateOf(item: CarItem, owned: Set<String>, coins: Int, carLevel: Int): CarUnlockState =
        when {
            isOwned(item, owned) -> CarUnlockState.OWNED
            carLevel < item.requiredCarLevel -> CarUnlockState.LEVEL_LOCKED
            coins >= item.priceCoins -> CarUnlockState.AFFORDABLE
            else -> CarUnlockState.TOO_EXPENSIVE
        }

    /** Satin alma yalnizca [CarUnlockState.AFFORDABLE] durumunda gecerlidir. */
    fun canBuy(item: CarItem, owned: Set<String>, coins: Int, carLevel: Int): Boolean =
        stateOf(item, owned, coins, carLevel) == CarUnlockState.AFFORDABLE
}
