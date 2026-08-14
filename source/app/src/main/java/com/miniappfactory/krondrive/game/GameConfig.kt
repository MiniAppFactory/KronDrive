package com.miniappfactory.krondrive.game

/**
 * TUM denge/ayar degerleri burada. Baska hicbir dosyada "sihirli sayi" yok —
 * seviye hedefleri [LevelCatalog], yukseltme maliyetleri [UpgradeCatalog],
 * gorev tanimlari `data/` altinda.
 *
 * Fizik sabitleri, HTML prototipinden (KRON_DRIVE_FINAL_BALANCED_80_3.html)
 * BIREBIR alindi — oyun hissi korunsun diye. Hangi degerin prototipin neresine
 * karsilik geldigi tek tek isaretli; degistirirken oyunun dengesinin oradan
 * geldigini bilerek degistir.
 */
object GameConfig {

    // ---------------------------------------------------------------------
    // Sahne / gorunum (prototipteki metrics() ve resize() ile ayni)
    // ---------------------------------------------------------------------

    /** Yol genisligi: min(290, ekranGenisligi * 0.56) */
    const val ROAD_MAX_WIDTH_PX = 290f
    const val ROAD_WIDTH_RATIO = 0.56f
    const val LANE_COUNT = 3

    /** Oyuncu araci ekranin altindan bu kadar yukarida durur (player.y = H - 210). */
    const val PLAYER_BOTTOM_OFFSET_PX = 210f

    // --- Arac olculeri ve carpisma kutusu ---
    //
    // Prototipte carpisma kutusu 42x90 idi ama CIZIM o kutuyu doldurmuyordu:
    // gorunur arac y ekseninde -2..74 (yaklasik 76) araligindaydi, yani aracin
    // ALTINDA ~16 px'lik gorunmez bir carpisma alani vardi. Oyuncu bunu
    // "carpma mekanigi arabanin cok disinda" diye bildirdi (2026-08-13) ve
    // ekran goruntusunde iki arac arasinda gozle gorulur bosluk varken kaza
    // olustugu dogrulandi.
    //
    // Cozum iki parcali:
    //   1. Cizim [CAR_ART_SCALE] ile kucultuldu (araclar seridi bogmuyor).
    //   2. Carpisma kutusu artik CIZIMDEN turetiliyor, ustune [HITBOX_SCALE]
    //      ile biraz da daraltiliyor — bu tur oyunlarda kutunun gorselden
    //      birkac piksel KUCUK olmasi adil hissettirir, buyuk olmasi haksiz.

    /** Prototipteki cizim koordinatlarinin olcek carpani (1.0 = orijinal boy). */
    const val CAR_ART_SCALE = 0.80f

    /** Cizimin arac y'sine gore ust/alt siniri (prototip: -2 ve 74). */
    const val CAR_ART_TOP_OFFSET = -2f * CAR_ART_SCALE
    const val CAR_ART_BOTTOM_OFFSET = 74f * CAR_ART_SCALE

    /** Gorunur arac genisligi (prototipte tekerlekler dahil 40 birim). */
    const val CAR_WIDTH_PX = 40f * CAR_ART_SCALE

    /** Gorunur arac yuksekligi. */
    const val CAR_HEIGHT_PX = CAR_ART_BOTTOM_OFFSET - CAR_ART_TOP_OFFSET

    /** Carpisma kutusu gorselin bu oranidir (1.0 = tam gorsel boyut). */
    const val HITBOX_SCALE = 0.88f

    const val CAR_HITBOX_WIDTH_PX = CAR_WIDTH_PX * HITBOX_SCALE
    const val CAR_HITBOX_HEIGHT_PX = CAR_HEIGHT_PX * HITBOX_SCALE

    /** Carpisma kutusunun arac y'sine gore ust kenari (gorselin icinde ortali). */
    const val CAR_HITBOX_TOP_OFFSET =
        CAR_ART_TOP_OFFSET + (CAR_HEIGHT_PX - CAR_HITBOX_HEIGHT_PX) / 2f

    /**
     * Serit degistirirken x'in hedefe yaklasma hizi: x += (hedef-x) * min(1, RATE*dt).
     * Prototipte 12 idi; kontroller "gec tepki veriyor" geri bildirimi uzerine
     * 16'ya cikarildi (~0.06 s'de serit ortasina oturur).
     */
    const val LANE_LERP_RATE = 16f

    /** Engel/coin dogma yuksekligi (ekranin ustunde, negatif y). */
    const val OBSTACLE_SPAWN_Y_PX = -150f
    const val COIN_SPAWN_Y_PX = -60f

    /** Ekranin altinda bu kadar asagi inen nesne temizlenir. */
    const val OBSTACLE_DESPAWN_MARGIN_PX = 110f
    const val COIN_DESPAWN_MARGIN_PX = 40f

    // ---------------------------------------------------------------------
    // Hiz modeli (prototipteki currentSpeed())
    // ---------------------------------------------------------------------

    /** Baslangic/temel hiz. Prototip: `2.63 + min(3.2, score/600)` */
    const val BASE_SPEED = 2.63f
    const val SCORE_SPEED_DIVISOR = 600f

    /** Skordan gelen hiz artisinin tavani (SPEED yukseltmesi bunu buyutur). */
    const val SCORE_SPEED_CAP_BASE = 3.2f
    const val MIN_SPEED = 2.0f

    /** Boost basiliyken hedef hiza eklenen degeri. */
    const val BOOST_SPEED_BONUS_BASE = 1.8f

    /** Fren basiliyken hedef hizdan dusulen deger (BRAKE yukseltmesi bunu buyutur). */
    const val BRAKE_SPEED_PENALTY_BASE = 0.9f

    /**
     * Hiz birimi -> piksel/saniye. Prototipte her yerde `speed * 250 * dt`
     * seklinde geciyordu (yol kaymasi, engel ve coin hareketi).
     */
    const val WORLD_PX_PER_SPEED_UNIT_PROTOTYPE = 250f

    /**
     * HISSEDILEN hiz carpani — prototipten BILINCLI sapma (bkz. PROVENANCE.md).
     *
     * Oyuncu geri bildirimi (2026-08-14): "gorunen hiz ayni olsun ama hissedilen
     * hiz yavaslamali, 160 ile giderken bile asiri hizli oynanmiyor."
     *
     * Gostergedeki km/h yalnizca `speed` degerinden hesaplanir
     * ([speedToKmh]), dunyanin kaymasi ise `speed * WORLD_PX_PER_SPEED_UNIT`.
     * Ikisini ayirip ikincisini 0.75 ile carpinca gosterge aynen kalir, dunya
     * %25 daha yavas akar: 160 km/h artik eski 120 km/h gibi oynanir
     * (yaklasma hizi ve engeller arasi ekran mesafesi o hizin degerleri).
     *
     * Neden trafik sikligi bozulmuyor: engeller ZAMANA gore dogar
     * ([OBSTACLE_SPAWN_INTERVAL_SEC]), hiza gore degil. Saniyedeki arac sayisi
     * ayni kalir, sadece her araci gorup tepki verme suresi 1/0.75 = %33 uzar.
     *
     * Neden [PIXELS_PER_METER] de ayni carpanla kuculuyor: metre/saniye
     * degismesin diye. Aksi halde tum bolumlerin mesafe hedefleri ve gunluk
     * gorevlerin "3000 m" tipi hedefleri sessizce %25 uzardi.
     *
     * Tek dugme: hala hizli gelirse bu sayiyi kucult (0.65 -> 160 km/h ~ 105
     * km/h hissi), yavas gelirse 1.0'a yaklastir. Baska hicbir yeri elleme.
     */
    const val WORLD_SPEED_SCALE = 0.75f

    const val WORLD_PX_PER_SPEED_UNIT = WORLD_PX_PER_SPEED_UNIT_PROTOTYPE * WORLD_SPEED_SCALE

    /**
     * Prototipte hiz ANINDA hedefe zipliyordu (`state.speed = currentSpeed()`).
     * ACCELERATION yukseltmesinin bir anlami olabilmesi icin birinci dereceden
     * bir gecikme eklendi; taban oran (6.0/s) bilerek yuksek secildi — ~0.17s
     * ile hedefe oturur, yani orijinal "anlik" his pratikte korunur.
     */
    const val ACCEL_RATE_BASE = 6.0f
    const val DECEL_RATE_BASE = 8.0f

    // ---------------------------------------------------------------------
    // Boost enerjisi (prototipteki update() boost blogu)
    // ---------------------------------------------------------------------

    const val BOOST_MAX = 100f
    const val BOOST_START = 100f
    const val BOOST_DRAIN_PER_SEC_BASE = 38f
    const val BOOST_REGEN_PER_SEC = 15f

    /**
     * Boost'un YENIDEN tutusmasi icin gereken en az enerji (devam etmesi icin
     * degil — bir kez tutustuysa bar bitene kadar calisir).
     *
     * Prototipte bu esik yoktu ve bar bosalinca sunlar oluyordu: `wantsBoost`
     * ayni karede false oluyor, dolum basliyor, bir sonraki karede boost yine
     * tutusuyordu — parmak basiliyken boost her karede acilip kapaniyor, motor
     * sesi ve egzoz alevi titriyordu. Simdi bar bosaldiginda boost KILITLENIR
     * (bkz. GameEngine.boostLockedUntilRelease) ve parmak kalkana kadar ne
     * calisir ne de dolar; birakip tekrar basmak gerekir.
     */
    const val BOOST_REENGAGE_MIN = 8f

    /** Fren basiliyken sarj daha yavas (prototip: `keys.Space ? 10 : 15`). */
    const val BOOST_REGEN_PER_SEC_BRAKING = 10f

    // ---------------------------------------------------------------------
    // Trafik ve toplanabilirler
    // ---------------------------------------------------------------------

    const val OBSTACLE_SPAWN_INTERVAL_SEC = 0.78f
    const val COIN_SPAWN_INTERVAL_SEC = 1.05f

    /** Engellerin kendi hiz carpani: 1.0 .. 1.14 arasi rastgele. */
    const val OBSTACLE_SPEED_MUL_MIN = 1.0f
    const val OBSTACLE_SPEED_MUL_MAX = 1.14f

    // ---------------------------------------------------------------------
    // Skor
    // ---------------------------------------------------------------------

    /** Saniyede kazanilan skor: `score += speed * 11 * dt` */
    const val SCORE_PER_SPEED_PER_SEC = 11f
    const val SCORE_PER_PASSED_VEHICLE = 8
    const val SCORE_PER_COIN = 35
    const val BOOST_REFUND_PER_COIN = 12f
    const val CRASH_SCORE_PENALTY = 80

    // ---------------------------------------------------------------------
    // PERFECT DODGE (yeni mekanik — prototipte yoktu)
    // ---------------------------------------------------------------------

    /**
     * Perfect dodge penceresinin genisligi: carpisma siniri ile serit araligi
     * ARASINDA nereye dusecegi (0f = hic, 1f = tam serit araligi).
     *
     * Carpisma zaten `|dx| < 42` (iki aracin genisligi) oldugunda olusur.
     * Esik bu yuzden 42 ile serit araligi arasinda bir yerde olmali:
     * yan seritten TEMIZ gecmek dodge SAYMAMALI — sadece serit degistirirken
     * aracin seritler arasinda oldugu, gercekten riskli anlar saymali.
     *
     * Neden sabit bir piksel degeri degil: serit araligi ekran genisligine
     * bagli (`min(290, W*0.56)/3`). 360 dp'lik bir telefonda serit 67 dp,
     * 320 dp'likte 60 dp. Sabit 64 px esigi kucuk ekranlarda serit
     * araligindan BUYUK kalir ve yan seritten duz gecmek bile bedava dodge
     * verirdi (combo ve skor sisirilirdi).
     */
    const val PERFECT_DODGE_WINDOW_RATIO = 0.5f

    /** Verilen serit araligi icin gecerli dodge esigi. */
    fun perfectDodgeMaxDx(laneWidth: Float): Float {
        val lane = laneWidth.coerceAtLeast(CAR_WIDTH_PX)
        return CAR_WIDTH_PX + (lane - CAR_WIDTH_PX) * PERFECT_DODGE_WINDOW_RATIO
    }

    /** Combo bu sure icinde yeni bir dodge gelmezse sifirlanir. */
    const val COMBO_WINDOW_SEC = 6f

    const val PERFECT_DODGE_BASE_SCORE = 25

    /** Combo carpanlari: combo 1,2,3,4,5+ -> x1, x1.2, x1.5, x2, x3 */
    val COMBO_MULTIPLIERS = floatArrayOf(1f, 1.2f, 1.5f, 2f, 3f)

    fun comboMultiplier(combo: Int): Float =
        if (combo <= 0) 1f else COMBO_MULTIPLIERS[minOf(combo, COMBO_MULTIPLIERS.size) - 1]

    // ---------------------------------------------------------------------
    // Mesafe / hiz gostergesi
    // ---------------------------------------------------------------------

    /**
     * Prototipteki hiz gostergesi: `((speed - 2) / 5.7) * 180 + 60` km/h.
     * Taban hizda (2.63) ~80 km/h gosteriyor.
     */
    const val SPEEDOMETER_SPAN = 5.7f
    const val SPEEDOMETER_RANGE_KMH = 180f
    const val SPEEDOMETER_MIN_KMH = 60f
    const val SPEEDOMETER_MAX_KMH = 240f

    /**
     * Metre donusumu, hiz gostergesiyle TUTARLI olacak sekilde secildi:
     * taban hiz 2.63 -> 657.5 px/s ve gosterge 80 km/h = 22.2 m/s
     * => 657.5 / 22.2 = 29.6 px = 1 metre.
     *
     * [WORLD_SPEED_SCALE] ile ayni oranda kucultuluyor: dunya daha yavas
     * kaysa da 80 km/h yine 22.2 m/s eder, yani mesafe hedefleri degismez.
     */
    const val PIXELS_PER_METER_PROTOTYPE = 29.6f

    const val PIXELS_PER_METER = PIXELS_PER_METER_PROTOTYPE * WORLD_SPEED_SCALE

    fun speedToKmh(speed: Float): Int {
        val kmh = ((speed - MIN_SPEED) / SPEEDOMETER_SPAN) * SPEEDOMETER_RANGE_KMH + SPEEDOMETER_MIN_KMH
        return kmh.coerceIn(0f, SPEEDOMETER_MAX_KMH).toInt()
    }

    /** [speedToKmh] tersi — bolumlerin baslangic hizini km/h olarak yazabilmek icin. */
    fun speedFromKmh(kmh: Int): Float =
        MIN_SPEED + ((kmh - SPEEDOMETER_MIN_KMH) / SPEEDOMETER_RANGE_KMH) * SPEEDOMETER_SPAN

    // ---------------------------------------------------------------------
    // Sonsuz mod zorluk egrisi
    // ---------------------------------------------------------------------

    /** Her 30 saniyede hiz carpani bu kadar artar (30sn -> +%10, 60sn -> +%20 ...). */
    const val ENDLESS_SPEED_STEP = 0.10f
    const val ENDLESS_SPEED_MAX_MULTIPLIER = 1.60f
    const val ENDLESS_STEP_SECONDS = 30f

    /** Trafik yogunlugu de artar (spawn araligi bu carpana bolunur). */
    const val ENDLESS_TRAFFIC_STEP = 0.06f
    const val ENDLESS_TRAFFIC_MAX_MULTIPLIER = 1.50f

    /** Sonsuz modda rekora bu kadar yakin bitirilirse ozel mesaj gosterilir. */
    const val NEAR_RECORD_SECONDS = 5

    // ---------------------------------------------------------------------
    // Odul ekonomisi
    // ---------------------------------------------------------------------

    const val COINS_PER_PICKUP = 1
    const val SCORE_PER_BONUS_COIN = 120
    const val COINS_PER_STAR = 25
    const val XP_PER_SCORE_POINT_DIVISOR = 10
    const val XP_PER_STAR = 20

    /** Arac seviyesi = 1 + xp / bu deger. */
    const val XP_PER_CAR_LEVEL = 500

    // ---------------------------------------------------------------------
    // Booster etkileri
    // ---------------------------------------------------------------------

    /** TURBO_START: kosunun ilk saniyelerinde boost enerjisi harcanmaz. */
    const val TURBO_START_FREE_BOOST_SEC = 3f

    /** SECOND_CHANCE / revive sonrasi dokunulmazlik suresi. */
    const val INVULNERABLE_SEC_AFTER_SAVE = 2f

    const val SCORE_BOOSTER_MULTIPLIER = 1.25f
    const val DOUBLE_REWARD_MULTIPLIER = 2

    // ---------------------------------------------------------------------
    // Reklam frekansi (AdMob)
    // ---------------------------------------------------------------------

    /**
     * HER kosu sonunda gecis reklami gosterilir (sahibi karari, 2026-08-14):
     * sonuc ekranindan cikisin her yolu — "ana menu", "sonraki bolum" ve
     * duraklatma menusunden "cik" — reklamdan gecer. Asagidaki N sayaclari
     * yalnizca bu bayrak false yapilirsa devreye girer.
     *
     * Not: bu agresif bir frekans. Play politikasina aykiri degil (reklam
     * oyunun icinde degil, dogal gecis noktasinda), ama terk oranini
     * yukseltebilir; sahibi bunu bilerek istedi.
     */
    const val INTERSTITIAL_AFTER_EVERY_RUN = true

    /** Kac seviye tamamlaninca bir gecis reklami gosterilecek. */
    const val INTERSTITIAL_EVERY_N_LEVELS = 2

    /** Kac sonsuz-mod kosusundan sonra gecis reklami gosterilecek. */
    const val INTERSTITIAL_EVERY_N_ENDLESS_RUNS = 3

    /**
     * KULLANILMIYOR (2026-08-14). Sonuc ekranindaki "TEKRAR" butonu kaldirildi:
     * kosuyu bedavaya sifirlayip yeniden baslatmak, hem reklamsiz tur cevirmenin
     * hem de kisa kosu farmlamanin en kolay yoluydu (sahibi: "bedava oyunu reset
     * yapan yer varsa kesfetmeyi engellemeli"). Yerine carpisma aninda REKLAMLI
     * ve kosu basina BIR KEZ "devam et" var (bkz. [REVIVE_MAX_PER_RUN]).
     * Sabit, geri alinmak istenirse diye duruyor.
     */
    const val INTERSTITIAL_EVERY_N_RETRIES = 2

    /** Odullu reklam karsiligi verilen coin. */
    const val REWARDED_COIN_AMOUNT = 150

    /**
     * Odullu coin reklaminin gunluk siniri (ekonomi sismesin diye).
     * Sinir GARAJDAKI "coin kazan" ve SONUC EKRANINDAKI "odulu ikiye katla"
     * reklamlari arasinda PAYLASILIR — ikincisinin eskiden hic siniri yoktu
     * ve ayni kosu tekrarlanarak sinirsiz coin basilabiliyordu (2026-08-14).
     */
    const val REWARDED_COIN_DAILY_LIMIT = 5

    /**
     * "Odulu ikiye katla" reklaminin bir seferde verebilecegi en yuksek coin.
     * Kural basit tutuldu: bir reklam en fazla bir reklam kadar oder.
     */
    const val REWARDED_DOUBLE_COINS_CAP = REWARDED_COIN_AMOUNT

    /**
     * Bundan kisa suren kosu HIC coin odemez. "Basla, hemen birak, tekrar
     * basla" dongusu de bir farm yoluydu; kosuyu bedavaya sifirlayan her yol
     * kapatiliyor (sahibi karari, 2026-08-14).
     */
    const val MIN_PAID_RUN_SECONDS = 10

    /** Bir kosuda en fazla kac kez reklamla devam edilebilir. */
    const val REVIVE_MAX_PER_RUN = 1

    // ---------------------------------------------------------------------
    // Kosu akisi
    // ---------------------------------------------------------------------

    /** Kosu baslamadan onceki geri sayim (prototipte 5 idi; 3 daha akici). */
    const val COUNTDOWN_SECONDS = 3

    /** Bir kare icin islenecek en buyuk dt (prototip: 0.032). */
    const val MAX_FRAME_DT = 0.032f
}
