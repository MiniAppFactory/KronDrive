package com.miniappfactory.krondrive.game

import com.miniappfactory.krondrive.data.AppLanguage

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
    DRIVER
}

/** Poligon kosesi (Compose'a bagimli olmamak icin kendi tipimiz). */
data class CarPoint(val x: Float, val y: Float)

/** Cizilebilir tek bir gövde parcasi. Sinirlar test icin acik. */
sealed interface CarPart {
    val paint: CarPaint
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
        val corner: Float = 0f
    ) : CarPart {
        override val right: Float get() = left + width
        override val bottom: Float get() = top + height
    }

    /** Daire (surucu basi, far). */
    data class Disc(
        override val paint: CarPaint,
        val centerX: Float,
        val centerY: Float,
        val radius: Float
    ) : CarPart {
        override val left: Float get() = centerX - radius
        override val top: Float get() = centerY - radius
        override val right: Float get() = centerX + radius
        override val bottom: Float get() = centerY + radius
    }

    /** Poligon (kama burun, genisleyen arka). */
    data class Wedge(
        override val paint: CarPaint,
        val points: List<CarPoint>
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

/** Bir govde sekli: isim, fiyat ve cizim parcalari. */
data class CarShapeDef(
    override val id: String,
    val nameTr: String,
    val nameEn: String,
    val descriptionTr: String,
    val descriptionEn: String,
    override val priceCoins: Int,
    override val requiredCarLevel: Int,
    /** Cizim sirasi = liste sirasi (ustteki parca sonra gelir). */
    val parts: List<CarPart>
) : CarItem {
    override fun name(language: AppLanguage): String = language.pick(tr = nameTr, en = nameEn)

    fun description(language: AppLanguage): String =
        language.pick(tr = descriptionTr, en = descriptionEn)

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
    val accentArgb: Long
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
        CarPaint.DRIVER -> CarCatalog.DRIVER_ARGB
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

    /** Yere dusen golge — sekilden bagimsiz, ayak izi hissi degismesin diye. */
    const val SHADOW_ARGB: Long = 0x3D000000
    const val SHADOW_LEFT: Float = -21f
    const val SHADOW_TOP: Float = 12f
    const val SHADOW_WIDTH: Float = 42f
    const val SHADOW_HEIGHT: Float = 68f

    // --- Boost alevi (prototipteki olculer) --------------------------------

    /** Alev, sekil nerede bitiyorsa bu kadar altinda baslar. */
    const val FLAME_GAP: Float = 6f
    const val FLAME_OUTER_LENGTH: Float = 16f
    const val FLAME_OUTER_HALF_WIDTH: Float = 5f
    const val FLAME_INNER_LENGTH: Float = 10f
    const val FLAME_INNER_HALF_WIDTH: Float = 3f
    const val FLAME_OUTER_ARGB: Long = 0xFF29B6FF
    const val FLAME_INNER_ARGB: Long = 0xFFFFD33D

    // --- Kimlikler ---------------------------------------------------------

    const val SHAPE_HATCHBACK = "hatchback"
    const val SHAPE_RACE_SEDAN = "race_sedan"
    const val SHAPE_MUSCLE = "muscle"
    const val SHAPE_SUPERCAR = "supercar"

    const val COLOR_KRON_RED = "kron_red"
    const val COLOR_GRAPHITE = "graphite"
    const val COLOR_EMERALD = "emerald"
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
     * Ilk sekil, prototipin ORIJINAL cizimidir — mevcut oyuncular guncelleme
     * sonrasi araclarini oldugu gibi gorur. Koordinatlari degistirme.
     */
    private val HATCHBACK = CarShapeDef(
        id = SHAPE_HATCHBACK,
        nameTr = "Şehir",
        nameEn = "City",
        descriptionTr = "Kron Drive'ın klasik gövdesi",
        descriptionEn = "The classic Kron Drive body",
        priceCoins = 0,
        requiredCarLevel = 1,
        parts = listOf(
            CarPart.Box(CarPaint.BODY_SHADE, -18f, 66f, 36f, 8f),
            CarPart.Box(CarPaint.BODY_SHADE, -16f, 4f, 32f, 6f),
            CarPart.Box(CarPaint.TIRE, -20f, 14f, 8f, 18f),
            CarPart.Box(CarPaint.TIRE, 12f, 14f, 8f, 18f),
            CarPart.Box(CarPaint.TIRE, -20f, 50f, 8f, 18f),
            CarPart.Box(CarPaint.TIRE, 12f, 50f, 8f, 18f),
            CarPart.Box(CarPaint.BODY, -10f, 8f, 20f, 62f, 8f),
            CarPart.Box(CarPaint.BODY, -6f, -2f, 12f, 18f, 6f),
            CarPart.Box(CarPaint.GLASS, -7f, 23f, 14f, 18f, 6f),
            CarPart.Box(CarPaint.BODY, -14f, 29f, 28f, 8f),
            CarPart.Disc(CarPaint.DRIVER, 0f, 29f, 5f),
            CarPart.Box(CarPaint.ACCENT, -4f, 11f, 8f, 34f)
        )
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
        parts = listOf(
            CarPart.Box(CarPaint.BODY_SHADE, -17f, 2f, 34f, 5f),
            CarPart.Box(CarPaint.TIRE, -20f, 13f, 8f, 19f),
            CarPart.Box(CarPaint.TIRE, 12f, 13f, 8f, 19f),
            CarPart.Box(CarPaint.TIRE, -20f, 49f, 8f, 19f),
            CarPart.Box(CarPaint.TIRE, 12f, 49f, 8f, 19f),
            CarPart.Box(CarPaint.BODY, -11f, 6f, 22f, 64f, 7f),
            CarPart.Box(CarPaint.BODY, -7f, -2f, 14f, 14f, 5f),
            // Seritler CAMDAN ONCE cizilir: kokpit kapanmasin, arac 60 Hz'de
            // "araba" gibi okunsun (hatchback'te serit prototipteki gibi ustte).
            CarPart.Box(CarPaint.ACCENT, -6f, 8f, 4f, 42f),
            CarPart.Box(CarPaint.ACCENT, 2f, 8f, 4f, 42f),
            CarPart.Box(CarPaint.GLASS, -8f, 24f, 16f, 16f, 5f),
            CarPart.Box(CarPaint.BODY, -15f, 28f, 30f, 8f),
            CarPart.Disc(CarPaint.DRIVER, 0f, 30f, 5f),
            CarPart.Box(CarPaint.BODY_SHADE, -14f, 62f, 28f, 8f),
            CarPart.Box(CarPaint.ACCENT, -13f, 58f, 26f, 4f, 2f)
        )
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
        parts = listOf(
            CarPart.Box(CarPaint.BODY_SHADE, -18f, 66f, 36f, 8f),
            CarPart.Box(CarPaint.BODY_SHADE, -15f, 3f, 30f, 6f),
            CarPart.Box(CarPaint.TIRE, -20f, 12f, 8f, 20f),
            CarPart.Box(CarPaint.TIRE, 12f, 12f, 8f, 20f),
            CarPart.Box(CarPaint.TIRE, -20f, 48f, 8f, 22f),
            CarPart.Box(CarPaint.TIRE, 12f, 48f, 8f, 22f),
            CarPart.Box(CarPaint.BODY, -13f, 6f, 26f, 62f, 4f),
            CarPart.Box(CarPaint.BODY, -9f, -2f, 18f, 12f, 3f),
            CarPart.Box(CarPaint.TRIM, -16f, 42f, 3f, 20f, 1.5f),
            CarPart.Box(CarPaint.TRIM, 13f, 42f, 3f, 20f, 1.5f),
            CarPart.Box(CarPaint.ACCENT, -7f, 4f, 4f, 60f),
            CarPart.Box(CarPaint.ACCENT, 3f, 4f, 4f, 60f),
            CarPart.Box(CarPaint.BODY_SHADE, -3f, 12f, 6f, 8f, 2f),
            CarPart.Box(CarPaint.GLASS, -9f, 26f, 18f, 14f, 3f),
            CarPart.Box(CarPaint.BODY, -14f, 30f, 28f, 7f),
            CarPart.Disc(CarPaint.DRIVER, 0f, 31f, 5f)
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
        parts = listOf(
            CarPart.Box(CarPaint.BODY_SHADE, -16f, 2f, 32f, 4f),
            CarPart.Box(CarPaint.TIRE, -20f, 14f, 8f, 17f),
            CarPart.Box(CarPaint.TIRE, 12f, 14f, 8f, 17f),
            CarPart.Box(CarPaint.TIRE, -20f, 50f, 8f, 20f),
            CarPart.Box(CarPaint.TIRE, 12f, 50f, 8f, 20f),
            CarPart.Wedge(
                CarPaint.BODY,
                listOf(
                    CarPoint(-8f, -2f),
                    CarPoint(8f, -2f),
                    CarPoint(12f, 26f),
                    CarPoint(-12f, 26f)
                )
            ),
            CarPart.Box(CarPaint.BODY, -12f, 24f, 24f, 22f, 3f),
            CarPart.Wedge(
                CarPaint.BODY,
                listOf(
                    CarPoint(-12f, 44f),
                    CarPoint(12f, 44f),
                    CarPoint(14f, 68f),
                    CarPoint(-14f, 68f)
                )
            ),
            CarPart.Box(CarPaint.BODY_SHADE, -13f, 44f, 4f, 12f, 1f),
            CarPart.Box(CarPaint.BODY_SHADE, 9f, 44f, 4f, 12f, 1f),
            CarPart.Box(CarPaint.ACCENT, -2f, 0f, 4f, 60f),
            CarPart.Box(CarPaint.GLASS, -8f, 25f, 16f, 14f, 5f),
            CarPart.Box(CarPaint.BODY, -14f, 30f, 28f, 7f),
            CarPart.Disc(CarPaint.DRIVER, 0f, 31f, 5f),
            CarPart.Box(CarPaint.BODY_SHADE, -17f, 64f, 34f, 6f),
            CarPart.Box(CarPaint.ACCENT, -15f, 60f, 30f, 4f, 2f)
        )
    )

    val shapes: List<CarShapeDef> = listOf(HATCHBACK, RACE_SEDAN, MUSCLE, SUPERCAR)

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
    // Arama ve sahiplik (saf mantik — repository ve garaj bunu kullanir)
    // -----------------------------------------------------------------

    /** Bilinmeyen/eski id varsayilana duser (kayit bozulmasin diye). */
    fun shape(id: String?): CarShapeDef = shapes.firstOrNull { it.id == id } ?: defaultShape

    fun color(id: String?): CarColorDef = colors.firstOrNull { it.id == id } ?: defaultColor

    /** Bedava olanlar satin alinmadan da sahiplenilmis sayilir. */
    fun isOwned(item: CarItem, owned: Set<String>): Boolean =
        item.priceCoins == 0 || item.id in owned

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
