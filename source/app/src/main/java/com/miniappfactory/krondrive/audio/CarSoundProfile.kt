package com.miniappfactory.krondrive.audio

import com.miniappfactory.krondrive.game.CarCatalog

/**
 * Bir gövdenin SES kimligi: motorun tinisi, egzoz gurultusu ve kornasi.
 *
 * ## Neden burada, `game/CarCatalog.kt` icinde degil
 *
 * `game/` paketi simulasyona aittir ve saf Kotlin kalir; ses ise Android
 * tarafinin isidir. Ayrica bu tablo **oynanisi hicbir sekilde etkilemez** —
 * hiz, ivme, fren, boost ve carpisma kutusu ses profilinden tamamen bagimsiz.
 * Iki tablo ayri dosyalarda durunca "sesi degistirdim, denge kaydi mi?" sorusu
 * hic sorulmuyor.
 *
 * Eslesme gövde **id'si** uzerinden yapilir ([CarSoundProfiles.forShape]);
 * bilinmeyen id [CarSoundProfiles.DEFAULT]'a duser, boylece yeni bir gövde
 * (ornegin planlanan tir) ses tarafi guncellenmeden eklenirse oyun sessiz
 * kalmaz, sadece referans sesi kullanir.
 *
 * ## Alanlarin anlami
 *
 * Motor dalgasi su sekilde uretilir (bkz. [EngineVoice]):
 *
 * ```
 * ham = sin(a) + h2·sin(2a) + h3·sin(3a) + h4·sin(4a) + h5·sin(5a) + grit·testere(a)
 * ham *= waveNormalize          // tepe degeri 1'i asmasin
 * ham *= lope zarfi             // rolanti duzensizligi
 * cikis = alcakGecirenFiltre(ham, cutoff) * gain
 * ```
 *
 * Yani:
 *  - **[freqMul]** motorun kalinligi. 1.0 = referans (45–150 Hz). Kucuk deger
 *    daha kalin. 0.76'nin altina INILMEDI: temel frekans telefon
 *    hoparlorunun tasiyamayacagi kadar dusuyor ve ses "kaybolmus" gibi
 *    duyuluyor. Kalinlik hissini asagida temel yerine tek sayili harmonikler
 *    tasiyor (eksik temel etkisi).
 *  - **[harmonic2]…[harmonic5]** tini. Cift sayili harmonikler (2, 4) yumusak
 *    ve "duzgun" duyulur; tek sayili harmonikler (3, 5) sert, bogus ve
 *    gurultulu — V8 egzoz karakterinin kaynagi budur.
 *  - **[grit]** testere dalgasi payi: metalik pürüz.
 *  - **[noiseAmount]** motora karisan filtreli gurultu: dizel tikirtisi,
 *    eski motorun havasi. 0 = tertemiz.
 *  - **[lopeDepth] / [lopeRate]** rolanti duzensizligi. Derinlik genlik
 *    dalgalanmasinin miktari, oran ise temel frekansa gore hizi. Dusuk oran
 *    (0.25) = yavas, belirgin "lope"; V8'in duzensiz rolantisi.
 *  - **[gainMul]** genel yukseklik. Sadece bu carpan farkli sesleri birbirine
 *    gore yuksek/alcak yapar; dalga sekli her zaman normalize edildigi icin
 *    kirpma (clipping) riski profilden gelmez.
 *  - **[cutoffMul]** alcak geciren filtrenin acikligi: buyuk deger = daha
 *    parlak/tiz, kucuk deger = tok/bogus.
 *  - **[nitroTone]** nitro efektinin islik ve gurultu suzulmesini olcekler;
 *    nitro da araca gore hafifce renklensin diye.
 *
 * Korna alanlari ([hornBaseHz], [hornInterval], [hornBuzz], [hornSeconds],
 * [hornAttack]) ayni mantikla calisir: iki borulu (iki tonlu) bir korna,
 * aralarindaki oran [hornInterval]. Ince araclarda yuksek temel + kisa sure,
 * kalin araclarda dusuk temel + uzun sure ve yavas atak.
 *
 * ## Tir hazirligi — NE ONGORULDU, NE YAPILDI (2026-08-17)
 *
 * Bu bolum tir gövdesi eklenmeden once bir TAHMINDI. Tir gercekten
 * eklenince tahminin iki sayisi tutmadi; kayit dursun diye ikisi de burada:
 *
 *  - Ongoru: cok dusuk [freqMul] (~0.62). **Yapilmadi.** 0.62 hem yukaridaki
 *    0.76 tabaninin altinda hem de Boga 67'nin "katalogun en pesi" tacini
 *    elinden aliyor. Tir 0.78'de kaldi; kalinlik tek sayili harmoniklerden
 *    ve 0.44 filtreden geliyor (bkz. [CarSoundProfiles.TIR]).
 *  - Ongoru: cok dusuk [hornBaseHz] (~110 Hz). **Yapilmadi.** 110 Hz,
 *    Boga 67'nin 250 Hz'lik "en kalin korna" tacini kiriyor ve bunu bir test
 *    koruyor. Tir 262 Hz'de — katalogun ikinci en kalini.
 *  - Ongoru: genis [hornInterval] (~1.5), uzun [hornSeconds] (~1.1), yavas
 *    [hornAttack] (~0.06). **Aynen yapildi** — ucu de bostu, ve hava
 *    kornasinin "basinc kurma" karakterini asil tasiyan bunlar.
 *
 * Cikarilan ders: bir karakter tek bir sayiya bagliysa o sayi baskasinin
 * taciyla carpisir. Ayni karakteri BIRDEN COK bos eksene dagitmak hem
 * carpismayi onluyor hem de sonucu daha ayirt edilebilir yapiyor.
 */
data class CarSoundProfile(
    val id: String,
    val freqMul: Float = 1.00f,
    val harmonic2: Float = 0.50f,
    val harmonic3: Float = 0.25f,
    val harmonic4: Float = 0.00f,
    val harmonic5: Float = 0.00f,
    val grit: Float = 0.12f,
    val noiseAmount: Float = 0.02f,
    val lopeDepth: Float = 0.15f,
    val lopeRate: Float = 0.50f,
    val gainMul: Float = 1.00f,
    val cutoffMul: Float = 1.00f,
    val nitroTone: Float = 1.00f,
    val hornBaseHz: Float = 420f,
    val hornInterval: Float = 1.25f,
    val hornBuzz: Float = 0.45f,
    val hornSeconds: Float = 0.50f,
    val hornAttack: Float = 0.018f
) {

    /**
     * Ham motor dalgasini ±1'e sigdiran carpan. Butun bilesenler ayni anda
     * tepe yapsa bile sonuc 1'i gecemez; boylece **hicbir profil kirpmaya yol
     * acamaz** ve yuksek/alcak farki yalnizca [gainMul]'dan gelir (kasitli
     * karar). `EngineVoiceTest` bunu her profil icin dogruluyor.
     */
    val waveNormalize: Float =
        1f / (1f + harmonic2 + harmonic3 + harmonic4 + harmonic5 + grit)

    /**
     * Korna icin ayni normalizasyon: iki boru x (temel + tizlestirme
     * harmonikleri 0.5 + 0.33 + 0.25 = 1.08).
     */
    val hornNormalize: Float = 1f / (2f * (1f + hornBuzz * 1.08f))
}

/**
 * Gövde id'si -> ses profili tablosu. Karakterler proje sahibinin 2026-08-15
 * tarifinden turetildi: *"Boğa 67 daha böyle egzozu gürültülü olur… tırın sesi
 * de tok bir ses olmalı"*.
 */
object CarSoundProfiles {

    /**
     * **Sehir** ve bilinmeyen govdeler icin geri dusus — Beety'nin profilini
     * PAYLASIR (ayni nesne, kopya degil).
     *
     * 2026-08-16'ya kadar burada CarSoundProfile'in ciplak varsayilanlari
     * duruyordu: 2026-08-15 oncesindeki TEK motor sesi. Yani Sehir'e hic ses
     * TASARLANMAMISTI, sadece eski varsayilan orada kalmisti. Sahibi duyup
     * "standart araç sesi kotu" dedi ve karari verdi: Sehir, Beety ile AYNI
     * sesi kullansin.
     *
     * NEDEN KOPYA DEGIL PAYLASIM: ayni sayilari iki profile yazmak
     * `profiller birbirinden ayirt edilebilir` testini kirar — ve o test
     * sahibinin baska bir istegini koruyor ("her birisi farkli olmali").
     * Tek nesneyi iki kimlige baglayinca envanterde tek kayit kaliyor,
     * kural da bozulmuyor.
     *
     * Ayni olmasi tutarli: iki govde mekanik olarak da neredeyse ayni
     * (ikisi de 1.00 bandinda, tek fark Sehir'in 1.04 freni) ve ikisi de
     * oyunun giris araclari. Fark gorselde, seste degil.
     */
    val DEFAULT: CarSoundProfile get() = BEETY

    /** Yaris Sedan — tiz ve cevik: yuksek devir, parlak filtre, az duzensizlik. */
    private val RACE_SEDAN = CarSoundProfile(
        id = CarCatalog.SHAPE_RACE_SEDAN,
        freqMul = 1.16f,
        harmonic2 = 0.42f,
        harmonic3 = 0.30f,
        harmonic4 = 0.14f,
        harmonic5 = 0.07f,
        grit = 0.10f,
        noiseAmount = 0.02f,
        lopeDepth = 0.08f,
        gainMul = 0.96f,
        cutoffMul = 1.28f,
        nitroTone = 1.12f,
        hornBaseHz = 520f,
        hornInterval = 1.26f,
        hornBuzz = 0.50f,
        hornSeconds = 0.42f,
        hornAttack = 0.014f
    )

    /**
     * Kus SLX — mutevazi ve titrek: bogus filtre, en dusuk genlik, yavas ve
     * derin lope. Eski bir motorun duzensiz rolantisi.
     */
    private val KUS_SLX = CarSoundProfile(
        id = CarCatalog.SHAPE_KUS_SLX,
        freqMul = 0.94f,
        harmonic2 = 0.56f,
        harmonic3 = 0.20f,
        harmonic4 = 0.05f,
        harmonic5 = 0.03f,
        grit = 0.15f,
        noiseAmount = 0.05f,
        lopeDepth = 0.26f,
        lopeRate = 0.34f,
        gainMul = 0.88f,
        cutoffMul = 0.84f,
        nitroTone = 0.92f,
        hornBaseHz = 400f,
        hornInterval = 1.20f,
        hornBuzz = 0.38f,
        hornSeconds = 0.55f,
        hornAttack = 0.030f
    )

    /**
     * Dag Kecisi — tok, dizel havasi: **katalogun en yuksek motor
     * gurultusu** (0.10, kimlik garantisi) ve cok kapali filtre (0.68).
     *
     * 2026-08-17'ye kadar burada "katalogun EN KAPALI filtresi" yaziyordu ve
     * bu artik dogru degil: tir eklenince en kapali filtre 0.44 ile ona
     * gecti. Zaten bu profilin gorevi "tirin ses yonunun provasi" olarak
     * tanimlanmisti — prova bitti, asil arac geldi. Gurultu taci burada.
     */
    private val MOUNTAIN_GOAT = CarSoundProfile(
        id = CarCatalog.SHAPE_MOUNTAIN_GOAT,
        freqMul = 0.80f,
        harmonic2 = 0.62f,
        harmonic3 = 0.34f,
        harmonic4 = 0.18f,
        harmonic5 = 0.09f,
        grit = 0.20f,
        noiseAmount = 0.10f,
        lopeDepth = 0.18f,
        gainMul = 1.06f,
        cutoffMul = 0.68f,
        nitroTone = 0.86f,
        hornBaseHz = 330f,
        hornInterval = 1.20f,
        hornBuzz = 0.55f,
        hornSeconds = 0.60f,
        hornAttack = 0.028f
    )

    /** Kas Arabasi — derin ve gurultulu, ama Boga 67 kadar degil. */
    private val MUSCLE = CarSoundProfile(
        id = CarCatalog.SHAPE_MUSCLE,
        freqMul = 0.86f,
        harmonic2 = 0.56f,
        harmonic3 = 0.42f,
        harmonic4 = 0.16f,
        harmonic5 = 0.12f,
        grit = 0.18f,
        noiseAmount = 0.04f,
        lopeDepth = 0.22f,
        lopeRate = 0.42f,
        gainMul = 1.12f,
        cutoffMul = 0.94f,
        nitroTone = 0.95f,
        hornBaseHz = 300f,
        hornInterval = 1.25f,
        hornBuzz = 0.58f,
        hornSeconds = 0.55f,
        hornAttack = 0.020f
    )

    /**
     * **Boga 67 — katalogun en gurultulu egzozu** (sahibin acik istegi).
     * Dort isaret birlikte calisiyor:
     *
     *  1. En dusuk temel frekans (0.76) — kalinlik.
     *  2. **Tek sayili harmonikler baskin** (h3 0.64, h5 0.36; h2/h4 bilerek
     *     zayif). Ayirt edici ozellik bu: cift harmonikler yumusak, tek
     *     harmonikler bogus ve sert duyulur.
     *  3. En derin ve **en yavas** lope (0.34 / 0.25) — V8 rolantisinin
     *     duzensiz "pat… pat… pat" karakteri.
     *  4. En yuksek genlik (1.22) ve en yuksek grit (0.24).
     *
     * Kornasi da katalogun en kalini (250 Hz) ve en sert vizlayani.
     */
    private val MUSCLE_67 = CarSoundProfile(
        id = CarCatalog.SHAPE_MUSCLE_67,
        // 2026-08-16 — sahibi: "bir Amerikan arabasi, ABARTI EGZOZ SESLI
        // olmali". Abarti dort kaldiracla yapildi, hicbiri frekansi
        // DUSURMEDEN (0.76 sinir: altinda telefon hoparloru tasimiyor):
        //  1. gain 1.22 -> 1.40: kataloğun acik ara en yuksegi.
        //  2. cutoff 1.02 -> 1.34: filtre acildi. Kapali egzoz "hom" der,
        //     acik egzoz HAVLAR — fark buradan geliyor.
        //  3. tek sayili harmonikler artti (h3 0.64 -> 0.74, h5 0.36 -> 0.44):
        //     V8 imzasi keskinlesti, ses daha "yirtik".
        //  4. lope 0.34 -> 0.40 ve rate 0.25 -> 0.20: rolantideki
        //     "pat… pat… pat" hem derinlesti hem yavasladi.
        // grit de 0.24 -> 0.32; metalik purüz egzoz gurultusunun kendisi.
        freqMul = 0.76f,
        harmonic2 = 0.30f,
        harmonic3 = 0.74f,
        harmonic4 = 0.10f,
        harmonic5 = 0.44f,
        grit = 0.32f,
        noiseAmount = 0.06f,
        lopeDepth = 0.40f,
        lopeRate = 0.20f,
        gainMul = 1.40f,
        cutoffMul = 1.34f,
        nitroTone = 0.90f,
        hornBaseHz = 250f,
        hornInterval = 1.20f,
        hornBuzz = 0.66f,
        hornSeconds = 0.62f,
        hornAttack = 0.026f
    )

    /**
     * Super Araba — yuksek devirli, ince ve keskin: **katalogun en yuksek
     * frekansi (1.30) ve en acik filtresi (1.48)** — ikisi de kimlik
     * garantisi, testle korunuyor. Yaris motoru puruzsuz doner: cok az
     * duzensizlik.
     *
     * 2026-08-17 notu: korna artik en ince DEGIL (F1 780 Hz ile daha ince);
     * bu profilinki 640 Hz ile ikinci. Motor tarafindaki iki tac ise yerinde
     * — F1 olcume gore 1.75/2.01 isterken bilerek 1.28/1.46'da tutuldu.
     */
    private val SUPERCAR = CarSoundProfile(
        id = CarCatalog.SHAPE_SUPERCAR,
        freqMul = 1.30f,
        harmonic2 = 0.36f,
        harmonic3 = 0.26f,
        harmonic4 = 0.20f,
        harmonic5 = 0.16f,
        grit = 0.08f,
        noiseAmount = 0.02f,
        lopeDepth = 0.05f,
        gainMul = 0.98f,
        cutoffMul = 1.48f,
        nitroTone = 1.25f,
        hornBaseHz = 640f,
        hornInterval = 1.26f,
        hornBuzz = 0.52f,
        hornSeconds = 0.38f,
        hornAttack = 0.010f
    )

    /**
     * Beety — hava sogutmali boksor dortlu.
     *
     * ILK DENEME REDDEDILDI ve sebebi burada dursun: profil once katalogun en
     * pesi (0.72), en derin lope'u (0.40) ve en gurultulusu (0.14) yapilmisti.
     * Testler kirildi ve HAKLIYDILAR — o ucu Boga 67'nin ve Dağ Keçisi'nin
     * kimlik garantileri. Yeni bir govde, var olan bir govdenin tek cumlelik
     * tarifini elinden alarak kendine kimlik kuramaz; iki arac birden
     * bulaniklasir.
     *
     * Beety'nin kimligi bu yuzden BASKA BIR EKSENDE: **harmonik yapisi**.
     * Boksor dortlu CIFT sayili harmonikleri one cikarir — Boga 67'nin V8
     * imzasinin (tek sayililar baskin) tam ayna goruntusu. Katalogda bu
     * karakteri tasiyan baska govde yok ve kimsenin ustunlugunu almiyor.
     *
     * Geri kalan degerler bilerek IKINCI sirada: frekans 0.78 (Boga 0.76'nin
     * hemen ustunde), lope 0.32 (Boga 0.34'un hemen altinda), gurultu 0.08
     * (Dağ Keçisi 0.10'un altinda). "Neredeyse en pes, neredeyse en
     * duzensiz" — ama tac sahibinde kaliyor.
     */
    private val BEETY = CarSoundProfile(
        id = CarCatalog.SHAPE_BEETY,
        // "PUF PUF" (sahibi secimi, 2026-08-16 — bes aday dinletildi).
        // Karakteri tasiyan sey lopeRate: 0.50 -> 0.26. Lope YAVASLAYINCA
        // genlik dalgalanmasi tek tek duyulur hale geliyor, yani motor
        // "puf… puf… puf" diye vuruyor. Derinlik de 0.33'e cikti ama
        // Boga 67'nin 0.34'unun ALTINDA kalmak zorunda (test).
        freqMul = 0.82f,
        // CIFT sayililar baskin: boksor imzasi (test bunu zorunlu kiliyor).
        harmonic2 = 0.52f,
        harmonic3 = 0.11f,
        harmonic4 = 0.31f,
        harmonic5 = 0.05f,
        grit = 0.30f,
        noiseAmount = 0.088f,
        lopeDepth = 0.33f,
        lopeRate = 0.26f,
        gainMul = 0.94f,
        cutoffMul = 0.74f,
        nitroTone = 0.88f,
        hornBaseHz = 470f,
        hornInterval = 1.18f,
        hornBuzz = 0.74f,
        hornSeconds = 0.34f,
        hornAttack = 0.030f
    )

    // -----------------------------------------------------------------
    // 2026-08-17 — uc yeni gövde: F1, motosiklet, tir
    // -----------------------------------------------------------------
    //
    // Bu ucunun sayilari TAHMIN DEGIL, OLCUM. Proje sahibinin
    // `incoming/car_refs/sounds/` altina koydugu referans kayitlarin
    // spektrumu olculdu. Kayitlar oyunda kullanilamiyor (asagida neden), ama
    // HEDEF KARAKTER olarak gecerli.
    //
    // Olcumun durust olmasi icin: profili ZATEN kodda olan yedi aracin
    // referanslari da ayni cetvelle olculdu, boylece "olcum -> parametre"
    // esleme egrisi kalibre edildi. Iki egri cikti (Boga 67 disarida
    // birakildi — onun degerleri sahibinin "abarti egzoz" istegiyle bilerek
    // referanstan SAPTIRILMISTI, 2026-08-16):
    //
    //   cutoffMul ~= 0.544 * ln(spektral_agirlik_merkezi_Hz) - 1.556
    //   freqMul   ~= 0.426 * ln(baskin_tepe_Hz)              - 0.991
    //
    // Kalibrasyon kaynagi (olculen -> kodda yazili): merkez 61->0.68,
    // 73->0.74, 80->0.84, 101->0.94, 195->1.28, 264->1.48. Egri bu alti
    // noktaya oturuyor; ilk egrinin uyumu cok iyi, ikincisi daha gevsek
    // (Kas Arabasi 0.12 sapiyor).
    //
    // ⚠ OLCUMUN UYMADIGI YER — bilerek uyulmadi. Egriler uc yeni arac icin
    // sunu istedi:
    //
    //   F1        : freqMul 1.75, cutoffMul 2.01
    //   Motosiklet: freqMul 1.41, cutoffMul 1.64
    //   Tir       : freqMul 0.68, cutoffMul 0.54
    //
    // Uc degerin besi mevcut araclarin taclarini kiriyordu (Super Araba
    // freqMul 1.30 + cutoffMul 1.48; Boga 67 freqMul 0.76). Beety'de
    // ogrenilen kural burada da uygulandi: yeni bir gövde, var olan bir
    // gövdenin tek cumlelik tarifini elinden alarak kendine kimlik kuramaz.
    // Sadece Tir'in cutoff'u (0.54 -> 0.44) olcume uydu, cunku o eksende
    // korunan bir tac yoktu.
    //
    // Referanslarin neden oyunda kullanilamadigi da olculdu — 80 Hz altinda
    // kalan enerji payi: Tir %86, Dag Kecisi %84, Beety %82. Telefon
    // hoparloru o bandi zaten basmiyor, yani kaydin yarisindan fazlasi
    // cihazda YOK. Sentez bu yuzden duruyor: kalinligi temel frekansla
    // degil, harmoniklerle kuruyor.

    /**
     * **F1 — katalogun en puruzsuz ve en dengeli tarakli motoru.**
     *
     * Olcum, F1 referansini butun katalogdan AYRI bir kategoriye koydu.
     * En carpici uc sayi (parantez icinde mevcut yedi aracin araligi):
     *
     *  - 500 Hz ustu / alti enerji orani: **1.35** (digerleri 0.002–0.089).
     *    Yani F1, enerjisinin cogu 500 Hz'in USTUNDE olan tek referans;
     *    ikinci sirada Super Araba 0.089 ile, 15 kat geride.
     *  - En guclu 8 tepenin guc degisim katsayisi: **0.108** (digerleri
     *    0.999–1.901). Dusuk deger "tepeler birbirine esit" demek.
     *  - O 8 tepenin en zayifi / en guclusu: **0.714** (digerleri
     *    0.007–0.032). Yuzde birler yerine yuzde yetmis.
     *
     * Ucu ayni seyi soyluyor: F1 tek bir kalin harmonige yiginmiyor, cok
     * sayida ust harmonigi AYNI ANDA ve NEREDEYSE ESIT suruyor. "Ciglik"
     * dedigimiz sey bu yogun, duz tarak.
     *
     * ## Kimlik hangi eksende kuruldu
     *
     * Tizlik tacini alamadi (Super Araba'da, 1.30) — bu yuzden F1 1.28'de,
     * tacin bir tik altinda. Kimligi tasiyan sey bunun yerine **duz harmonik
     * tarak**: [harmonic2]…[harmonic5] neredeyse esit (0.62/0.58/0.56/0.52)
     * ve hepsi yuksek. Katalogda bu yapiya sahip baska gövde yok — Boga 67
     * tek sayililara, Beety cift sayililara yiginir, geri kalan herkeste
     * harmonikler yukari dogru azalir. F1 hicbirine benzemiyor.
     *
     * Bunun yan etkisi kasitli: enerjiyi ust harmoniklere yaymak, temel
     * frekansi YUKSELTMEDEN sesin agirlik merkezini yukari tasiyor. Yani
     * F1 tizlik tacini almadan da Super Araba'dan parlak duyulabiliyor;
     * tac bir muhasebe kaydi olarak sahibinde kaliyor.
     *
     * Ikinci eksen **duzensizligin yoklugu**: [lopeDepth] 0.02, [grit] 0.04
     * ve [noiseAmount] 0.01 ile ucu de katalogun EN DUSUGU. Bu taclar bostu
     * (en derin lope Boga 67'nin, ama en SIGI kimsenin degildi) ve olcumle
     * de destekleniyor: F1 referansinin zarf dalgalanmasi 0.31, butun setin
     * en dusugu (Super Araba 0.58, Beety 0.75).
     *
     * Kornasi: F1'de korna YOK. Sifir yazilamadigi icin en ince (780 Hz),
     * en kisa (0.22 s), en hizli atakli (0.006 s) ve en az vizlayan (0.30)
     * korna verildi — "korna sayilmayacak kadar kucuk" en yakin karsilik.
     */
    private val F1 = CarSoundProfile(
        id = CarCatalog.SHAPE_F1,
        freqMul = 1.28f,
        // DUZ TARAK — F1'in sayisal imzasi. Dorduncu harmonik ucuncunun
        // %97'si, besinci ikincinin %84'u; en zayif/en guclu = 0.84.
        // Test bunu >= 0.80 olarak kilitliyor.
        harmonic2 = 0.62f,
        harmonic3 = 0.58f,
        harmonic4 = 0.56f,
        harmonic5 = 0.52f,
        grit = 0.04f,
        noiseAmount = 0.01f,
        lopeDepth = 0.02f,
        // Lope zaten duyulmayacak kadar sig; hizli tutulunca kalan kirinti
        // da ayri vuruslara ayrismiyor, dokuya karisiyor.
        lopeRate = 0.90f,
        gainMul = 1.02f,
        cutoffMul = 1.46f,
        nitroTone = 1.35f,
        hornBaseHz = 780f,
        hornInterval = 1.33f,
        hornBuzz = 0.30f,
        hornSeconds = 0.22f,
        hornAttack = 0.006f
    )

    /**
     * Motosiklet — tek/cift silindir: yuksek devirli ama F1'den HAM.
     *
     * Olcum onu F1 ile ayni ailede gosterdi (tepe degisim katsayisi 0.079,
     * F1 0.108 — ikisi de mevcut katalogun 0.999–1.901 bandindan kopuk),
     * ama iki sayi ikisini net ayiriyor:
     *
     *  - 500 Hz ustu/alti orani: motosiklet **0.115**, F1 1.348. Yani
     *    motosikletin agirligi hala altta; F1 gibi ciglik degil.
     *  - 200 Hz altindaki enerji payi: motosiklet **%28**, F1 %8.
     *
     * Bu yuzden harmonikleri F1 kadar duz DEGIL ve TEK sayililara hafifce
     * yatik (tek silindir/cift silindir az sayida, buyuk ateslemeyle calisir).
     * Yatiklik bilerek olculu: tek toplami 0.86, cift toplami 0.70. Boga
     * 67'nin "V8 imzasi" testi tek > 2x cift ariyor (0.86 > 1.40 degil),
     * yani o imza Boga'da EXCLUSIVE kaliyor.
     *
     * Karakterin geri kalani iki bos eksende: [grit] 0.26 (metalik puruz —
     * zincir/tek silindir sertligi) ve yavas-derin [lopeDepth] 0.27 /
     * [lopeRate] 0.32 ile belirgin vurus.
     *
     * NOT — olcumle celisen tek yer: referansin zarf dalgalanmasi 0.47
     * cikti, yani mevcut kalin araclardan (Boga 0.65, Dag Kecisi 0.72) daha
     * DUZGUN. Yine de lope belirgin yazildi, cunku (a) o araclarin yuksek
     * olcumu buyuk olcude olcum eseri — zarflari lope degil, cok alcak
     * frekansli vurudan dalgalaniyor; (b) fiziksel gerekce net: cift
     * silindir devir basina iki kez atesler, V8 sekiz kez, dolayisiyla
     * cevrim ici genlik degisimi dogasi geregi daha buyuktur. Sahibin
     * dinleyip "vurus fazla/az" demesi bu sayiyi tek basina degistirir.
     */
    private val MOTOSIKLET = CarSoundProfile(
        id = CarCatalog.SHAPE_MOTOSIKLET,
        freqMul = 1.22f,
        harmonic2 = 0.40f,
        harmonic3 = 0.52f,
        harmonic4 = 0.30f,
        harmonic5 = 0.34f,
        grit = 0.26f,
        noiseAmount = 0.05f,
        lopeDepth = 0.27f,
        lopeRate = 0.32f,
        gainMul = 1.08f,
        cutoffMul = 1.38f,
        nitroTone = 1.18f,
        // Ince ve kisa, ama F1'inkinden kalin/uzun — sirayi bozmuyor.
        hornBaseHz = 700f,
        hornInterval = 1.30f,
        hornBuzz = 0.42f,
        hornSeconds = 0.26f,
        hornAttack = 0.008f
    )

    /**
     * **Tir — katalogun en kapali filtreli motoru** (0.44). Dizel.
     *
     * ⚠ Bilerek "en tok/en koyu SES" DENMIYOR, cunku olculdu ve degil.
     * Sentezlenen ciktinin spektral agirlik merkezi: tir 173 Hz (tonal
     * govde, tikirti kapali) — Dag Kecisi 160, Beety 161, Kus SLX 165'ten
     * YUKSEKTE. Tikirti da hesaba katilinca tirin merkezi 352 Hz'e cikiyor
     * ve katalogun en koyusu olmaktan iyice uzaklasiyor.
     *
     * Iki sebebi var ve ikisi de kasitli:
     *
     *  1. Asagida kalinlik icin eklenen TEK sayili harmonikler, tanimi
     *     geregi enerjiyi yukari tasir. "Eksik temel" ile "koyu spektrum"
     *     ayni sey degil: ilki algilanan PERDEYI dusurur, ikincisi tiniyi.
     *     Tir ilkini hedefliyor.
     *  2. [noiseAmount] motorun alcak geciren filtresini BYPASS eder
     *     (EngineVoice'ta gurultu, filtreden SONRA eklenir ve yalnizca
     *     1400 Hz'de suzulur). Yani yuksek dizel tikirtisi, filtre ne kadar
     *     kapali olursa olsun tepeye parlak enerji koyar.
     *
     * Ucuncusu da fiziksel olarak DOGRU: gercek bir dizel tam olarak boyle
     * duyulur — koyu bir govde uzerinde parlak enjektor/supap tikirtisi.
     * Yani buradaki "yuksek merkez" bir kusur degil, tarifin sonucu.
     * Yine de sahibi dinlerken "tir Dag Kecisi'nden parlak" izlenimi
     * edinirse sebebi budur ve tek dokunusla ([noiseAmount]) geri alinir.
     *
     * Buradaki tasarim sorunu sudur: tir KALIN olmali, ama kalinligin dogal
     * kaldiraci olan [freqMul] iki yandan kapali. Asagidan 0.76 tabani
     * (altinda telefon hoparloru temeli tasimiyor), ustunden Boga 67'nin
     * "katalogun en pesi" taci. Geriye 0.78 kaliyor — Dag Kecisi'nin
     * 0.80'inin bir tik altinda, ki bu tek basina "tir" hissi vermez.
     *
     * Kalinlik bu yuzden BASKA IKI YERDEN geliyor:
     *
     *  1. **Eksik temel etkisi.** Tek sayili harmonikler yuksek
     *     (h3 0.56, h5 0.30, toplam 0.86; ciftler 0.56). Kulak, guclu bir
     *     harmonik serisinden temeli kendi tamamlar — yani ses, temel
     *     frekansinin isaret ettiginden daha KALIN algilanir. Telefon
     *     hoparloru zaten basamayacagi bir temeli calmak zorunda kalmadigi
     *     icin bu yontem cihazda daha saglam.
     *  2. **Katalogun en kapali filtresi** ([cutoffMul] 0.44; onceki en
     *     kapali Dag Kecisi 0.68 idi). Uretilen ust harmonikler sonra
     *     suzuluyor: geriye kalinlik kaliyor, havlama kalmiyor.
     *
     * Ikinci maddede olcum ve tasarim AYNI YERE dustu: referansin spektral
     * agirlik merkezi 47 Hz olculdu ve kalibrasyon egrisi 0.54 istedi —
     * yazilan 0.44 ondan da kapali. Uc yeni aracta olcume uyulabilen tek
     * parametre bu, cunku o eksende korunan bir tac yoktu.
     *
     * ## Boga 67 ile neden karismiyor
     *
     * Ikisi de tek sayili harmoniklere yatik, ama FILTRE zit yonde:
     * Boga 1.34 ile acik (yirtik, havlayan egzoz), tir 0.44 ile kapali
     * (tok homurtu). Ayrica tirin yatikligi olculu — tek 0.86'ya karsi
     * cift 0.56, yani Boga'nin "tek > 2x cift" imzasini KARSILAMIYOR;
     * o tanim Boga'da exclusive kaliyor ve test bunu koruyor.
     *
     * Ayirt eden ucuncu sey dizel tikirtisi: [noiseAmount] 0.09, katalogun
     * ikincisi — Dag Kecisi'nin 0.10'u onun kimlik garantisi, dokunulmadi.
     *
     * ## Hava kornasi
     *
     * Dosyanin basindaki "Tir hazirligi" bolumu ~110 Hz oneriyordu; Boga
     * 67'nin 250 Hz'lik "en kalin korna" taci buna izin vermedi (test).
     * Korna 262 Hz'de — katalogun ikinci en kalini. Hava kornasi karakteri
     * bunun yerine bos duran uc eksenden geliyor ve ucu de acik ara birinci:
     * en genis aralik ([hornInterval] 1.50; digerleri 1.18–1.26), acik ara
     * en uzun ses ([hornSeconds] 1.10; onceki en uzun 0.62) ve acik ara en
     * yavas atak ([hornAttack] 0.060; onceki en yavas 0.030). Basincin
     * kurulmasi, borularin acilmasi ve uzun uzun otmesi bunlar.
     */
    private val TIR = CarSoundProfile(
        id = CarCatalog.SHAPE_TIR,
        // 0.76 tabani + Boga 67'nin taci = tek bosluk 0.78. Kalinlik
        // asagidaki harmoniklerden ve 0.44 filtreden geliyor.
        freqMul = 0.78f,
        harmonic2 = 0.34f,
        harmonic3 = 0.56f,
        harmonic4 = 0.22f,
        // h5 once 0.30'du; olcum onun PAHALI oldugunu gosterdi (asagidaki
        // "olculen" notu). Besinci harmonik kalinliga neredeyse hic
        // katmiyor ama parlakligi belirgin yukseltiyor — dizelde ustelik
        // fiziksel karsiligi da zayif (dizel spektrumu 3. harmonikten sonra
        // hizla duser). 0.20'ye cekildi: tek sayili tilt korundu
        // (tek 0.76 / cift 0.56), gereksiz parlaklik gitti.
        harmonic5 = 0.20f,
        grit = 0.16f,
        // Dizel tikirtisi. Dag Kecisi'nin 0.10'unun ALTINDA kalmali (test).
        noiseAmount = 0.09f,
        // Agir ve yavas rolanti. Derinlik Boga 67'nin 0.40'ini, hiz onun
        // 0.20'sini gecmiyor — iki tac da yerinde kaliyor.
        lopeDepth = 0.30f,
        lopeRate = 0.22f,
        gainMul = 1.16f,
        // 0.52'den 0.44'e: "kalinligi filtreden kur" tarifinin agirligini
        // artirir ve Dag Kecisi'nin 0.68'iyle arayi genisletir.
        cutoffMul = 0.44f,
        nitroTone = 0.78f,
        hornBaseHz = 262f,
        hornInterval = 1.50f,
        hornBuzz = 0.70f,
        hornSeconds = 1.10f,
        hornAttack = 0.060f
    )

    /** Tum profiller — testler ve olasi bir ses ayari ekrani icin acik. */
    val all: List<CarSoundProfile> = listOf(
        BEETY, RACE_SEDAN, KUS_SLX, MOUNTAIN_GOAT, MUSCLE, MUSCLE_67, SUPERCAR,
        F1, MOTOSIKLET, TIR
    )

    /**
     * BILEREK ses paylasan govdeler: `paylasan kimlik -> profil sahibi`.
     *
     * Kural sudur: hicbir govde ses profiline sessizce "genel sese duserek"
     * ulasamaz — ya kendi profili olur, ya da BURADA acikca yazilir. Test bu
     * listeyi okuyor, yani bir paylasim ancak niyetle eklenebilir; unutulan
     * bir araç hala testi kirar.
     */
    val ALIASES: Map<String, String> = mapOf(
        CarCatalog.SHAPE_HATCHBACK to CarCatalog.SHAPE_BEETY
    )

    private val byId: Map<String, CarSoundProfile> = buildMap {
        all.forEach { put(it.id, it) }
        ALIASES.forEach { (alias, owner) -> put(alias, getValue(owner)) }
    }

    /**
     * Gövde id'sinden ses profili. Bilinmeyen, bos ya da null id
     * [DEFAULT]'a duser — kayit bozulsa ya da ses tablosu yeni bir gövdeden
     * geri kalsa bile oyun sessiz kalmaz.
     */
    fun forShape(shapeId: String?): CarSoundProfile = byId[shapeId] ?: DEFAULT
}
