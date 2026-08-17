package com.miniappfactory.krondrive.game

/**
 * Bir gövdenin ÖLÇÜ SINIFI: çizim tuvali ve çarpışma kutusu buradan gelir.
 *
 * Tasarımın tamamı ve ölçümler: `docs/VEHICLE_CLASSES.md`.
 *
 * ## Neden enum, neden `CarShapeDef`'e iki float değil
 *
 * Gövde başına serbest ölçü ilk bakışta esnek görünür ama altı ay sonra
 * birbirinden %3 farklı on iki kutu bırakır ve "şeride sığıyor mu" sorusunu
 * kimse tek başına cevaplayamaz. Her sınıf bir sprite tuvali, bir denge
 * muhakemesi ve bir test kümesi demektir; enum kutuyu **sonlu ve
 * gerekçelendirilebilir** tutar.
 *
 * ## Oranlar keyfi değil
 *
 * Sınıfın oranı **referans çiziminin oranıyla eşleşmek zorunda**. Sprite hattı
 * (`tools/build_car_sprites.py`) çizimi kutuya genişlik öncelikli oturtuyor;
 * oran tutmazsa fark saydam boşluğa döner ve çizilen araç çarpışma kutusundan
 * dar kalır — yani oyuncu aracın yanındaki boşluğa çarpar. 2026-08-16'da Boğa
 * 67'de bunun 0.21 dp'lik hâli ölçülüp düzeltilmişti.
 *
 * Bu yüzden her sınıfta **genişlik tasarım kararı**, **boy referans oranından
 * türetilmiş**tir.
 */
enum class VehicleClass(
    /** Çizim uzayında genişlik (birim). */
    val widthUnits: Float,
    /** Çizim uzayında boy (birim). */
    val lengthUnits: Float,
    /**
     * Çizim kutusunun üst kenarı (birim). Kutunun burnu merkezden bu kadar
     * yukarıda başlar.
     *
     * AÇIKÇA yazılıyor, BINEK'in -2'sinden oranlanarak TÜRETİLMİYOR: türetme
     * 57.4473684 gibi sayılar üretiyordu ve gövde geometrisinin o sayıya
     * birebir oturması imkânsızdı — "parça alttan taşıyor (57.45 > 57.44737)"
     * gibi bir testi kimse elle çözemez. Yuvarlak sınır, geometriyi yazan
     * insanın kutuyu gerçekten doldurabilmesi demek.
     */
    val artTopUnits: Float
) {
    /**
     * Motosiklet — 22 × 59, oran 0.373 (referans `07_motosiklet`: 0.375).
     *
     * Gerçek oran daha da dar olurdu (0.8 m / 1.8 m = %44 → 17.8 birim) ama
     * 17.8 birim = 14.2 dp ve oyun ölçeğinde bir çubuk gibi okunuyor; fabrika
     * boyası hiç görünmüyor. %55 (22 birim), "belirgin şekilde dar" ile "hâlâ
     * araç" arasındaki uzlaşma.
     */
    MOTOSIKLET(22f, 59f, -1.55f),

    /**
     * Binek — 40 × 76. **ÇIPA: asla değişmez.**
     *
     * Bütün denge, bütün bölüm hedefleri ve bütün yükseltme eğrileri bu
     * kutunun davranışına göre ayarlandı. Mevcut sekiz gövde ve trafik aracı
     * bu sınıfta kalır.
     */
    BINEK(40f, 76f, -2f),

    /**
     * Ağır (tır) — 48 × 202, oran 0.238 (referans `09_tir`: 0.237).
     *
     * Gerçek oran %139 olurdu (2.5 m / 1.8 m) → 55.6 birim = 44.5 dp, yani en
     * dar şeridin %74'ü. Sığıyor ama görsel olarak şeridi tıkıyor ve oyuncuya
     * "burada geçilecek yer yok" diyor — oysa var. %120 bilinçli bir
     * kısaltma: tır hâlâ gözle "diğerlerinden geniş" okunuyor, şeritte nefes
     * payı kalıyor.
     */
    AGIR(48f, 202f, -5.3f);

    // -----------------------------------------------------------------
    // Türetilmiş ölçüler — hepsi tek yerden, elle yazılan sayı yok
    // -----------------------------------------------------------------

    /** Çizim kutusunun sağ kenarı (birim). Sol kenar bunun negatifi. */
    val artRight: Float get() = widthUnits / 2f

    val artLeft: Float get() = -artRight

    val artTop: Float get() = artTopUnits

    /** Çizim kutusunun alt kenarı (birim). */
    val artBottom: Float get() = artTop + lengthUnits

    /** Ekrandaki çizim genişliği (px). */
    val artWidthPx: Float get() = widthUnits * GameConfig.CAR_ART_SCALE

    /** Ekrandaki çizim boyu (px). */
    val artHeightPx: Float get() = lengthUnits * GameConfig.CAR_ART_SCALE

    /** Çarpışma kutusu genişliği (px) — çizimden [GameConfig.HITBOX_SCALE] ile. */
    val hitboxWidthPx: Float get() = artWidthPx * GameConfig.HITBOX_SCALE

    /** Çarpışma kutusu boyu (px). */
    val hitboxHeightPx: Float get() = artHeightPx * GameConfig.HITBOX_SCALE

    companion object {
        /**
         * BINEK'in çizim kutusu üst kenarı (birim). Tarihsel değer; kutunun
         * burnu merkezden 2 birim yukarıda başlıyor.
         */
        const val BINEK_ART_TOP = -2f
    }
}
