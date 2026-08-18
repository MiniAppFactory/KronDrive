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

    /**
     * ⚠⚠ GECICI ANTRENMAN MODU — AAB'DEN ONCE SILINECEK. ⚠⚠
     *
     * Acikken trafik YALNIZCA en soldaki ve en sagdaki seritte dogar, orta
     * serit hep bos kalir. Sahibi test kolayligi icin istedi (2026-08-19):
     * orta seritte durup sahneyi, hizi, sesi ve arayuzu carpmadan
     * izleyebilmek icin.
     *
     * Yayina bu ACIK cikarsa oyun oynanamaz hale gelmez ama TAMAMEN
     * kolaylasir: oyuncu ortada durup sonsuza kadar hayatta kalir, butun
     * zorluk egrisi ve skor dengesi anlamsizlasir.
     *
     * Iki iz birakildi: `PLAY_RELEASE_CHECKLIST` S-8 maddesi ve
     * `GameEngineTest`'teki "antrenman modu kapali" testi. Test bu deger
     * `true` iken KIRMIZI YANAR — bilerek, unutulmasin diye.
     */
    const val TRAINING_MODE_SIDE_LANES_ONLY = true

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

    // ---------------------------------------------------------------------
    // Yol deseni (cizim) — goz yorgunlugu ayari
    // ---------------------------------------------------------------------
    //
    // Oyuncu geri bildirimi (2026-08-15): "cizgiler cok sik, oynarken goz cok
    // yoruluyor". Sebep dunyanin hizi degil, DESEN FREKANSI: prototipte kerb
    // bloklari 24 px, serit cizgileri 20 dolu / 20 bos idi. Arac yuksekligi
    // ~61 px; yani ekranda ~100 kerb blogu ve cizgi araligi ayni anda akiyor
    // ve yuksek kontrastla birlesince stroboskop etkisi yapiyordu.
    //
    // Cozum: ayni desen KORUNDU ama frekansi dusuruldu (blok ve aralik
    // uzatildi). Hiz hissi degismedi — o [WORLD_SPEED_SCALE] ile ayri
    // ayarlaniyor. Renk kontrasti da cizim tarafinda biraz yumusatildi.

    /**
     * Kirmizi/beyaz kerb blogunun boyu (prototip: 24).
     *
     * 46 -> 50 (2026-08-16): kerb artik yol yuzeyiyle AYNI hizda akiyor
     * (bkz. `GameRenderer.drawTrack`), eskiden 12 kat sonuktu. Hiz 12 katina
     * cikinca titresim de 12 katina cikiyor; blok boyu buna gore secildi.
     *
     * 50 nereden geliyor: en yuksek hizda (~1650 px/s, tam SPEED yukseltmesi
     * + en hizli arac + boost) kirmizi/beyaz gecis orani
     * `1650 / (2 * 50) = 16.5 Hz` olur. Ust sinir olarak serit cizgilerinin
     * ayni kosuldaki orani alindi (`1650 / 96 = 17.2 Hz`) — cunku o desen
     * ZATEN 1.00 carpanda calisiyor ve proje sahibi gorunumunu onaylamis
     * durumda. Yani kerb, oyuncunun her kosuda baktigi seritten daha hizli
     * titremiyor.
     *
     * Periyot (100) serit periyodundan (96) bilerek FARKLI: esitlenirse yol
     * duzleminin tamami ayni anda "tik" yapar ve senkron titresim tek tek
     * titresimden daha yorucu okunur.
     */
    const val KERB_BLOCK_HEIGHT_PX = 50f

    /** Serit cizgisi: dolu ve bos parca boylari (prototip: 20 / 20). */
    const val LANE_DASH_ON_PX = 42f
    const val LANE_DASH_OFF_PX = 54f

    /**
     * Engel/coin dogma yuksekligi (ekranin ustunde, negatif y).
     *
     * [OBSTACLE_SPAWN_Y_PX] artik SABIT DEGIL, sinifa gore hesaplaniyor
     * ([obstacleSpawnY]). Sebep 2026-08-16'da olculdu: sabit -150 binek icin
     * dogruydu (cizim boyu 60.8 px) ama AGIR sinifi 161.6 px uzun, yani tir
     * -150'de dogsa **arkasi +7.35'te, ekranin ICINDE** belirirdi — arac
     * yoktan var olmus gibi gorunurdu.
     *
     * Eski sabit tarihsel deger olarak duruyor ve binek icin uretilen sayiya
     * esit; cagri noktalari [obstacleSpawnY] kullanmali.
     */
    const val OBSTACLE_SPAWN_Y_PX = -150f

    /** Dogma yuksekliginin cizim boyunun otesinde biraktigi pay (px). */
    const val OBSTACLE_SPAWN_CLEARANCE_PX = 89.2f

    /**
     * Bu sinifa ait bir aracin dogacagi y: butun govdesi ekranin USTUNDE
     * kalacak sekilde. Binek icin -150 uretir (eski sabitin ta kendisi).
     */
    fun obstacleSpawnY(vehicleClass: VehicleClass): Float =
        -(vehicleClass.artHeightPx + OBSTACLE_SPAWN_CLEARANCE_PX)

    const val COIN_SPAWN_Y_PX = -60f

    /** Ekranin altinda bu kadar asagi inen nesne temizlenir. */
    const val OBSTACLE_DESPAWN_MARGIN_PX = 110f
    const val COIN_DESPAWN_MARGIN_PX = 40f

    /**
     * Ekranin USTUNDEN cikan arac bu kadar yukarida temizlenir.
     *
     * Trafik kendi hiziyla ilerlemeye baslayinca (bkz.
     * [TRAFFIC_SPEED_RATIO_MIN]) yaklasma hizi `oyuncuHizi - aracHizi`
     * oldu; oyuncu frene basip trafigin ALTINA duserse bu fark negatife
     * doner ve araclar yukari dogru uzaklasir. Eskiden yalnizca alt sinir
     * vardi, yani boyle bir arac listede sonsuza kadar kalirdi.
     *
     * Esik dogma yuksekliginin ([OBSTACLE_SPAWN_Y_PX]) UZERINDE olmali,
     * aksi halde yeni dogan arac ayni karede silinirdi.
     */
    const val OBSTACLE_DESPAWN_TOP_MARGIN_PX = 160f

    // ---------------------------------------------------------------------
    // Hiz modeli (prototipteki currentSpeed())
    // ---------------------------------------------------------------------

    /** Baslangic/temel hiz. Prototip: `2.63 + min(3.2, score/600)` */
    const val BASE_SPEED = 2.63f
    const val SCORE_SPEED_DIVISOR = 600f

    /** Skordan gelen hiz artisinin tavani (SPEED yukseltmesi bunu buyutur). */
    /**
     * Skordan gelen hiz tavaninin TABANI — referans arac (carpan 1.0) icin.
     *
     * 2026-08-18: 3.2 -> 1.90. Sahibi: *"Beety 180 yapamaz"* ve *"arabalar
     * sadece gorsel, defaulttan ne kadar +/- sapma o kadar"*. Ikisi de ayni
     * kokten geliyordu: TABAN zaten super araba rakamiydi.
     *
     * Olculdu (2026-08-18): butun araclarin hiz carpani 0.97-1.18 arasinda
     * sikisikti, yani bedava aractan 5000 coinlik Formula'ya toplam kazanc
     * **+%18**. Ayni anda TEK bir yukseltme dali (SPEED 1->8) +%20 veriyordu.
     * Yani oyundaki butun araclari almak, bedava arabanin tek bir dalini
     * sonuna kadar acmaktan daha az hiz veriyordu — "araclar kozmetik"
     * hissinin aritmetigi buydu.
     *
     * Yeni taban bedava araci **120 km/h**'e oturtuyor; merdivenin tepesi
     * (Formula, tam yukseltme) 220. Araclar arasi fark %11'den **%108**'e
     * cikti. Gosterge tavani 240, yani tepe hala icinde.
     */
    const val SCORE_SPEED_CAP_BASE = 1.90f
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
     * **2026-08-18: 0.75 -> 0.45** (sahibi: "dunyanin akis hizini %40 azalt").
     * Mevcut akisin %60'i. Gostergedeki km/h AYNEN kalir.
     *
     * ⚠ ARTIK TEK DUGME DEGIL. Yukaridaki "trafik sikligi bozulmuyor"
     * gerekcesi 0.75'te geciyordu, 0.45'te GECMIYOR. Engeller ZAMANA gore
     * dogdugundan dunya yavaslayinca ardisik araclar ekranda birbirine
     * yaklasir; olculdu (2026-08-18): tek basina 0.45, temkinli otopilotu
     * **bolum 4'te carptiriyor** ve kariyer orada kesiliyor.
     *
     * Bu yuzden ucu BIRLIKTE degisti — biri digerleri olmadan degistirilemez:
     *   1. WORLD_SPEED_SCALE      0.75 -> 0.45   (dunya 0.6x hizda)
     *   2. OBSTACLE_SPAWN_INTERVAL_SEC 0.78 -> 1.30  (0.78 / 0.6; ekrandaki
     *      arac araligi birebir korunsun diye)
     *   3. LevelCatalog'daki 22 [Objective.PassVehicles] hedefi x0.6 —
     *      saniyedeki arac %40 azaldigi icin eski sayilar erisilemez olurdu
     *      (olculdu: yalniz 1+2 yapilinca bolum 6, 45 saniyede 28 gecisle
     *      2 yildiz yerine 1 yildiz veriyordu).
     *
     * Korunanlar: metre/saniye ([PIXELS_PER_METER] ayni carpanla kuculuyor,
     * yani mesafe hedefleri kaymadi) ve skor/coin formulu (`speed`ten
     * besleniyor, bu carpandan degil).
     */
    const val WORLD_SPEED_SCALE = 0.45f

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

    /**
     * Engel dogma araligi (prototip: `spawnAcc > 0.78`).
     *
     * 2026-08-14'te trafik kendi hiziyla ilerlemeye baslayinca (bkz.
     * [TRAFFIC_SPEED_RATIO_MIN]) yaklasma hizi yariya indi; bu sabitin
     * DEGISMEMESI gerektigi soyle turetildi:
     *
     *   Dogma ZAMANA baglidir, mesafeye degil. Yaklasma hizi pozitif
     *   kaldigi surece dogan her arac er ya da gec oyuncuya varir:
     *       saniyedeki gecilen arac = 1 / OBSTACLE_SPAWN_INTERVAL_SEC
     *   Bu deger yaklasma hizindan BAGIMSIZDIR. Iki arac oyuncuya hep
     *   0.78 s arayla ulasir, yani oyuncunun bir aractan otekine tepki
     *   verme BUTCESI de aynen korunur (serit degistirme ~0.06 s).
     *
     *   Degisen tek sey aracin ekranda gorunur kalma suresi ve dolayisiyla
     *   ayni anda ekranda kac arac oldugudur:
     *       ekrandakiArac = yaklasmaMesafesi / (yaklasmaHizi x aralik)
     *   Yaklasma hizi ~%48'e dustugu icin ekranda kabaca iki kat arac olur.
     *   Bu ZORLUK degil GORUNURLUK degisikligidir: ayni tehdit daha
     *   uzaktan goruluyor, tepki suresi ise sabit.
     *
     * Araligi buyutmek karsilasma sikligini gercekten dusururdu,
     * kucultmek gercekten artirirdi — ikisi de istenmedigi icin bu sayiya
     * dokunulmadi. Bolum bazinda seyreltme [LevelDef.trafficDensity] ile
     * yapilir.
     */
    /**
     * 2026-08-18: 0.78 -> 1.30 (= 0.78 / 0.6). [WORLD_SPEED_SCALE] ile
     * BIRLIKTE degisti; gerekce orada yazili. Dunya 0.6x hizda aktigi icin
     * dogus araligi 1/0.6 ile uzatildi — boylece ardisik araclar arasindaki
     * EKRAN mesafesi eskisiyle ayni kalir, degisen yalnizca zaman.
     */
    const val OBSTACLE_SPAWN_INTERVAL_SEC = 1.30f
    const val COIN_SPAWN_INTERVAL_SEC = 1.05f

    /**
     * Trafik araclarinin KENDI ileri hizi — kosunun taban hizinin orani.
     *
     * Sorun (sahibi geri bildirimi, 2026-08-14): *"Yoldaki arabalar hareket
     * etmiyor, park etmis gibiler."* Sebep: engel `speed * K * dt * speedMul`
     * (speedMul 1.00..1.14) ile asagi akiyordu, yol kaymasi ise tam olarak
     * `speed * K * dt`. Yani engeller ASFALTLA ayni hizda — hatta biraz daha
     * hizli — geriye gidiyordu; asfalta gore duruyor ya da geri geri
     * gidiyorlardi.
     *
     * Cozum: her aracin kendi ileri hizi var ve ekranda
     *     asagiHiz = (oyuncuHizi - aracHizi) * WORLD_PX_PER_SPEED_UNIT
     * kadar akiyor. Zemin yine `oyuncuHizi` ile kaydigi icin arac ASFALTA
     * GORE `aracHizi` kadar ILERI gider — goz bunu "onumdeki arac gidiyor"
     * diye okur.
     *
     * Neden ANLIK hizin degil, kosunun TABAN hizinin orani: boost'a basmanin
     * bir anlami olsun diye. Oran anlik hizdan turetilseydi hizlandikca trafik
     * de hizlanir, yaklasma hizi hic degismezdi. Taban sabit oldugu icin
     * boost (+1.8 birim) yaklasma hizina TAM olarak eklenir: taban hizda
     * yaklasma 1.28 -> 3.08 birim, yani +%141 (eski modelde +%69 idi).
     *
     * Aralik neden dar (1.29 kat): fark buyudukce hizli araclar yavaslara
     * yetisip ayni y'de kumelenir ve 3 seridi birden kapatabilir. Bu aralikta
     * bir arac tum yaklasma boyunca en fazla ~120 px yetisir, iki arac arasi
     * mesafe ise ~190 px — yani kumelenme var ama kapali duvar yok.
     *
     * Ust sinir MIN_SPEED'in altinda kalmali: aksi halde tam frende oyuncu
     * trafigin altina duser ve araclar hic gecilemez. Varsayilan tabanda
     * 0.58 * 2.63 = 1.53 < 2.0. Yine de motor bu durumu tolere eder
     * ([OBSTACLE_DESPAWN_TOP_MARGIN_PX]).
     */
    const val TRAFFIC_SPEED_RATIO_MIN = 0.45f
    const val TRAFFIC_SPEED_RATIO_MAX = 0.58f

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

    /**
     * Bir sonraki bolumun acilmasi icin gereken gorev sayisi (bolum basina uc
     * gorev var).
     *
     * TARIHCE — bu sabit bir hatanin dersidir:
     *  - Baslangic: `stars > 0`. Ilk 8 bolum acikca buna gore tasarlandi
     *    ("ilk hedef her zaman en kolayi olmali", bkz. LevelCatalog).
     *  - 2026-08-15: sahibi *"gorevleri tamamlamadiysa neden geciyor ki"*
     *    dedi ve kural UCU DE olarak degistirildi. Ama bolum tasarimi
     *    guncellenmedi; ilk hedefin kolay olmasinin sagladigi guvenlik payi
     *    sessizce yok oldu. Eski kural uc ayri yerde yorum olarak kaldi ve
     *    testler de eski kurali dogruladigi icin kimse yakalamadi.
     *  - 2026-08-16: zorluk incelemesi (`docs/DIFFICULTY_REVIEW.md`) duvar
     *    sayisinin 6'dan 19'a ciktigini olctu. Sahibi **2**'de karar kildi.
     *
     * Kural: **iki gorev bolumu acar, ucuncusu ustalik yildizidir.** Yani
     * beceri hedefi (PerfectDodge/Combo) UCUNCU sirada oldugu surece hicbir
     * oyuncu ona takilip ilerleyemez hale gelmez.
     */
    const val MIN_STARS_TO_PASS = 2

    const val COINS_PER_PICKUP = 1

    /**
     * Kac skor puani 1 bonus coin eder. **120 -> 70** (2026-08-16, sahibi
     * onayi; ekonomi analizi `docs/ECONOMY_BALANCE_PROPOSAL.md`).
     *
     * Neden: oynanis geliri pasif geliri (gunluk gorev + odullu reklam)
     * yakalayamiyordu. Bu carpan **oynayarak** kazanmanin tek olceklenen
     * kalemi — toplanan coin ve yildiz coini tavanli, skor degil.
     *
     * OLCULDU (`LevelCurveTest.olcum dokumu`, 8 bolum, temkinli oynayis):
     * ilk gecişte bolum basina ortalama **100 -> 118 coin** (+%18). Etki
     * TEKRAR oynamada daha buyuk: yildiz coini yalnizca YENI yildiz icin
     * odendiginden (bkz. GameEngine), tekrar kosusunun geliri neredeyse
     * tamamen bu carpandan geliyor — orn. bolum 5 tekrari 51 -> 74 coin.
     * Zaten en az odenen etkinlik tekrar oynamakti.
     */
    const val SCORE_PER_BONUS_COIN = 70

    const val COINS_PER_STAR = 25
    const val XP_PER_SCORE_POINT_DIVISOR = 10
    const val XP_PER_STAR = 20

    /** Arac seviyesi = 1 + xp / bu deger. */
    const val XP_PER_CAR_LEVEL = 500

    /**
     * ARAC SEVIYESI ATLAMA BEDELI — eksik seviye basina coin.
     *
     * Sahibi (2026-08-19): *"formula arabasi icin hem coin hem seviye
     * istiyoruz; o kadar odemek isteyen varsa seviye doldurmadan da ek bir
     * coin harcayip araci acsin"*. Onerdigi formul birebir alindi:
     * `(gerekenSeviye - mevcutSeviye) x 500`.
     *
     * Ornek — Formula (seviye 8, 5.000 coin):
     *   seviye 1'de: 5.000 + (8-1)x500 = **8.500**
     *   seviye 4'te: 5.000 + (8-4)x500 = **7.000**
     *   seviye 8'de: 5.000 + 0         = **5.000**
     *
     * Neden bu buyukluk dogru: bir seviye 500 XP, XP ise `skor/10 +
     * yildiz x20` — yani kosu basina ~200-500 XP. Bir seviye kabaca bir-iki
     * kosu demek. 500 coin de kabaca dort bes bolumluk gelir, yani atlamak
     * beklemekten UCUZ degil; sabirsiz oyuncuya bir kapi aciyor, kestirme
     * sunmuyor.
     */
    const val LEVEL_SKIP_COIN_PER_LEVEL = 500

    // ---------------------------------------------------------------------
    // Booster etkileri
    // ---------------------------------------------------------------------

    /** TURBO_START: kosunun ilk saniyelerinde boost enerjisi harcanmaz. */
    const val TURBO_START_FREE_BOOST_SEC = 3f

    /**
     * SECOND_CHANCE / revive sonrasi dokunulmazlik suresi.
     *
     * 2 -> 3 saniye (2026-08-14): oyuncu reklami izleyip devam ettiginde
     * "baslar baslamaz tekrar carpti" diye bildirdi. Iki sebebi vardi:
     * dokunulmazlik kisaydi ve ekranin USTUNDE bekleyen araclar siliniyordu
     * (bkz. [REVIVE_SPAWN_PAUSE_SEC] ve GameEngine.revive).
     */
    const val INVULNERABLE_SEC_AFTER_SAVE = 3f

    /**
     * Revive'dan sonra bu sure boyunca YENI arac dogmaz. Dokunulmazlik tek
     * basina yetmiyordu: koruma bitesiye kadar yeni dogan araclar oyuncunun
     * uzerine gelmis oluyordu. Once bos yol, sonra trafik.
     */
    const val REVIVE_SPAWN_PAUSE_SEC = 1.2f

    const val SCORE_BOOSTER_MULTIPLIER = 1.25f
    const val DOUBLE_REWARD_MULTIPLIER = 2

    // ---------------------------------------------------------------------
    // Reklam frekansi (AdMob)
    // ---------------------------------------------------------------------

    /**
     * true ise HER kosu sonunda gecis reklami cikar; false ise asagidaki
     * N sayaclari devreye girer.
     *
     * Once true yapildi (sahibi istegi: "her oyun sonrasi reklam cikmali"),
     * ayni gun **lansman icin false'a alindi** (2026-08-14, ASO denetimi):
     * bolumler 30-90 saniye surdugu icin oyuncu her 30-90 saniyede bir tam
     * ekran reklam goruyordu. Bu turde 1 yildizli yorumlarin bir numarali
     * sebebi budur ve magaza puani 3.8'in altina inerse hicbir anahtar
     * kelime calismasi bunu telafi etmiyor.
     *
     * Plan: 30 gun veri toplanacak; puan 4.2 uzerinde kalirsa frekans
     * kademeli artirilacak. Geri almak icin tek satir yeter.
     */
    const val INTERSTITIAL_AFTER_EVERY_RUN = false

    /**
     * Kac kariyer kosusundan sonra bir gecis reklami gosterilecek.
     *
     * 2 -> 3 (2026-08-16). Ayni gun sayac kacagi kapatildi: eskiden sayac
     * yalnizca bolum TAMAMLANDIYSA artiyordu, yani carpip cikan oyuncu
     * sinirsiz reklamsiz oynuyordu. Kacak kapatilinca ayni "2" esigi gercekte
     * cok daha sik reklam demek olacakti; mevcut oyuncunun hissi bozulmasin
     * diye esik 3'e alindi (bkz. [INTERSTITIAL_MIN_RUN_SECONDS]).
     */
    const val INTERSTITIAL_EVERY_N_LEVELS = 3

    /** Kac sonsuz-mod kosusundan sonra gecis reklami gosterilecek. */
    /**
     * Sonsuz modda gecis reklami sikligi. **1 = her kosu sonunda.**
     *
     * 2026-08-18: 3 -> 1 (sahibi istegi). Gerekce onun sozleriyle:
     * *"tekrar dene deyince reklam ciksin, geri tusuna basinca da reklam
     * ciksin ki ucretsiz reset sansi olmasin"*.
     *
     * 3'te kosunun ikisi bedava cikisti; sonsuz mod bir skor kovalamacasi
     * oldugu icin kotu baslayan kosuyu aninda sifirlamak serbest kaliyordu —
     * hem reklamsiz tur cevirme hem kisa kosu farmlama yolu. Ayni endise
     * 2026-08-14'te "TEKRAR" butonunu kaldirtmisti; buton simdi reklamli
     * olarak geri geldi ve kacak yollarin da kapanmasi gerekti.
     *
     * ⚠ Bu, sonsuz modda reklam yukunu UC KATINA cikarir. Kariyer ve gunluk
     * gorev ETKILENMEZ ([INTERSTITIAL_EVERY_N_LEVELS] hala 3, ilk bolumler
     * hala reklamsiz). Geri almak isteniyorsa tek sayi.
     */
    const val INTERSTITIAL_EVERY_N_ENDLESS_RUNS = 1

    /**
     * Bir kariyer kosusunun reklam sayacini artirmasi icin gereken en kisa
     * sure. Basari sarti DEGIL — carpip biten kosu da sayilir, yoksa
     * "carpip ana ekrana don, bolumu tekrar sec" dongusu reklamsiz kalir
     * (2026-08-16'da bulunan kacak).
     *
     * Esik neden var: oyuncu yanlis bolume girip 2 saniyede cikarsa bu bir
     * "oturum" degildir; sayilirsa reklam menude gezinmenin cezasi olur.
     * Deger [MIN_PAID_RUN_SECONDS] ile ayni mantiktan geliyor.
     */
    const val INTERSTITIAL_MIN_RUN_SECONDS = 10

    /**
     * Bu bolum numarasina kadar (dahil) kariyer gecis reklami HIC
     * gosterilmez.
     *
     * Sayac kacagi tek basina kapatilsaydi en cok zorlanan oyuncu — ayni
     * bolumu tekrar tekrar deneyen yeni oyuncu — en cok reklam goren kesime
     * donusurdu ve erken hunide terk artardi. Ilk bolumler oyunun vitrini;
     * orada reklam gosterilmiyor.
     *
     * **4 -> 3 (2026-08-17, sahibi karari.)** Sahibi cihazda oynarken iki
     * kez fark etti: 4. bolumde basarisiz olup menuye donerken reklam yok,
     * 4'u bitirip 5'e gecerken de yok. Ikisi de kuralin dogru calismasiydi
     * ama BIRLIKTE muafiyeti pratikte BES bolume cikariyordu — cunku reklam
     * karari BITEN bolume bakar, gidilene degil. Yani 4 yazip 5 bolum
     * muafiyet vermek, sayinin kendisini yaniltici kiliyordu.
     *
     * Sahibi siniri 3'e cekmeyi secti: ilk uc bolum reklamsiz, 4. bolumde
     * takilan oyuncu artik reklam goruyor. Kabul edilen bedel, ucuncu
     * maddenin koruma amacinin bir miktar zayiflamasi.
     */
    const val INTERSTITIAL_FREE_LEVELS = 3

    /**
     * "TEKRAR DENE" basina reklam sikligi. **1 = her tekrarda.**
     *
     * Gecmis: buton 2026-08-14'te kaldirilmisti — *"bedava oyunu reset yapan
     * yer varsa kesfetmeyi engellemeli"*. Itiraz butona degil, BEDAVAYA idi.
     *
     * 2026-08-18'de sahibi geri istedi ve itirazi da kendisi kapatti:
     * *"sonsuz modda yandiginda tekrar dene deyince reklam ciksin, geri
     * tusuna basinca da reklam ciksin ki ucretsiz reset sansi olmasin"*.
     *
     * Bu yuzden esik 2 DEGIL 1: 2'de ilk tekrar bedava kalirdi, yani tam da
     * kapatilmak istenen kapi acik kalirdi. Kacis yolu da yok — sonuc
     * ekranindaki ANA MENU zaten [withOptionalInterstitial]'dan geciyor.
     *
     * Yalnizca SONSUZ mod. Kariyerde tekrar, bolum haritasindan girilir ve
     * oradaki sayac isler; gunluk gorev gunde bir kez oynanir.
     */
    const val INTERSTITIAL_EVERY_N_RETRIES = 1

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

    /**
     * Bir kare icin islenecek en buyuk dt. **0.032 -> 0.050** (2026-08-17).
     *
     * Prototipteki deger 0.032'ydi ve prototip 60 FPS'te calisan bir tarayici
     * oyunuydu — orada 32 ms, nominal karenin (16.7 ms) iki kati, yani genis
     * bir pay. Bu cihazda (Samsung S8, API 24) oyun ~40 FPS, yani nominal
     * kare **25 ms**: kirpma sinirina yalnizca 7 ms kaliyordu.
     *
     * Sonuc: bir kare 45 ms'ye takildiginda motor yalnizca 32 ms'lik hareket
     * isliyor ve aradaki 13 ms KAYBOLUYOR. Dunya her janky karede gercek
     * zamanin gerisine dusuyor ve bir daha yakalamiyor — nesneler ileri
     * SICRAMAK yerine DURAKSIYOR.
     *
     * Sahibinin sikayeti tam olarak buydu: *"yoldaki arabalar hala takila
     * takila gidiyor"*. En cok trafikte gorunmesi de tutarli: engeller yola
     * gore yavas hareket ediyor (oyuncunun %45-58'i), yol deseni tekrarli
     * oldugu icin jitter orada gizleniyor, tek tek araclar ise referans
     * noktasi oluyor.
     *
     * 0.050 = 20 FPS tabani. Kirpmanin KORUMA amaci duruyor (uzun bir
     * donmada oyuncu aracin icine isinlanmaz: 50 ms'de ~50 px, bir arac
     * boyundan kisa) ama 40 FPS'teki jitter kaybi bitiyor.
     *
     * ⚠ HIPOTEZE DAYALI, HENUZ OLCULMEDI. Dogru cozum sabit adimli birikirici
     * olurdu (`while (acc >= SABIT) step(SABIT)`) ama simulasyon maliyetini
     * ~%50 artiriyor ve bu cihaz zaten zorlaniyor. Olcum geldiginde hangisinin
     * gercekten fark yarattigina bakilacak.
     */
    const val MAX_FRAME_DT = 0.050f
}
