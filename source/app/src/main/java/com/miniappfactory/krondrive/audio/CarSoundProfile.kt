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
 * ## Tir hazirligi
 *
 * Planlanan tir gövdesi icin bu yapida hicbir degisiklik gerekmiyor; tek
 * yapilacak sey [CarSoundProfiles] icine su karakterde bir satir eklemek:
 * cok dusuk [freqMul] (~0.62), yuksek [harmonic3]/[harmonic5], yuksek
 * [noiseAmount], **cok dusuk** [hornBaseHz] (~110 Hz), genis [hornInterval]
 * (~1.5 = tam beslinin uzerinde), uzun [hornSeconds] (~1.1) ve yavas
 * [hornAttack] (~0.06) — hava kornasinin basinc kurmasi budur.
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
     * Dag Kecisi — tok, dizel havasi: katalogun EN KAPALI filtresi (0.68) ve
     * en yuksek motor gurultusu. Tirin ses yonunun provasi.
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
     * Super Araba — yuksek devirli, ince ve keskin: katalogun en yuksek
     * frekansi ve en acik filtresi, neredeyse hic duzensizlik yok (yaris
     * motoru puruzsuz doner). Kornasi da en ince.
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

    /** Tum profiller — testler ve olasi bir ses ayari ekrani icin acik. */
    val all: List<CarSoundProfile> = listOf(
        BEETY, RACE_SEDAN, KUS_SLX, MOUNTAIN_GOAT, MUSCLE, MUSCLE_67, SUPERCAR
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
