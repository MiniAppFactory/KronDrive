package com.miniappfactory.krondrive.game

import com.miniappfactory.krondrive.data.AppLanguage
import kotlin.math.pow
import kotlin.math.roundToInt

/** Garajdaki dort yukseltme dali. */
enum class UpgradeType { SPEED, ACCELERATION, BRAKE, BOOST }

/** Oyuncunun mevcut yukseltme seviyeleri (hepsi 1'den baslar). */
data class UpgradeLevels(
    val speed: Int = 1,
    val acceleration: Int = 1,
    val brake: Int = 1,
    val boost: Int = 1
) {
    fun levelOf(type: UpgradeType): Int = when (type) {
        UpgradeType.SPEED -> speed
        UpgradeType.ACCELERATION -> acceleration
        UpgradeType.BRAKE -> brake
        UpgradeType.BOOST -> boost
    }

    fun with(type: UpgradeType, level: Int): UpgradeLevels = when (type) {
        UpgradeType.SPEED -> copy(speed = level)
        UpgradeType.ACCELERATION -> copy(acceleration = level)
        UpgradeType.BRAKE -> copy(brake = level)
        UpgradeType.BOOST -> copy(boost = level)
    }
}

/**
 * Yukseltmelerin maliyeti ve fizige etkisi. Artislar bilerek KUCUK tutuldu:
 * oyuncu gelismeyi hissetmeli ama oyun bir anda kolaylasmamali (urun notu).
 *
 * Etki formullerinin tabani [GameConfig] icindeki prototip degerleridir;
 * seviye 1 = prototipin orijinal davranisi.
 */
object UpgradeCatalog {

    const val MAX_LEVEL = 8

    /**
     * Seviye 1'den 8'e toplam maliyet: 150+300+...+1050 = **4.200 coin**.
     *
     * 2026-08-18: adim 250 -> 150 (dal 7.000 -> 4.200, dort dal 28.000 ->
     * 16.800). Tek basina bir ekonomi karari degil, arac merdiveninin
     * ZORUNLU sonucu:
     *
     * Ayni gun arac hiz yayilimi %11'den %108'e acildi (sahibi karari —
     * "araba aldiklarinda farki hissetmeli"). O degisiklikten SONRA coin
     * basina kazanc soyle oldu:
     *
     *     SPEED dali 1->8 : 7.000 coin -> +35 km/h  =>  200 coin/km-h
     *     Sehir           :   350 coin -> + 5 km/h  =>   70
     *     Super Araba     : 3.200 coin -> +48 km/h  =>   67
     *     Formula         : 5.000 coin -> +65 km/h  =>   77
     *
     * Yani yukseltmeler araclardan uc kat pahali hale geldi — tuzaga
     * dondu. Eski `CarCatalogTest` kurali ("fark %10'da kalmali, yoksa dort
     * yukseltme dali anlamsizlasir") tam da bunu onluyordu; kural kalkinca
     * korudugu sey de korunmasiz kaldi.
     *
     * 150 ile dal 120 coin/km-h olur: araclar hala daha buyuk sicrama verir
     * (sahibinin istedigi bu) ama yukseltme yolu makul kalir. Ustelik bir dal
     * yalnizca hizi degil kendi eksenini de iyilestiriyor.
     *
     * ⚠ Bu, oyunun EN BUYUK coin gideriydi (28.000 -> 16.800). Ekonominin
     * kalan yarisi (`docs/ECONOMY_STATUS_20260817.md`) bu sayiya gore
     * yeniden bakilmali.
     */
    fun cost(currentLevel: Int): Int? =
        if (currentLevel >= MAX_LEVEL) null else 150 * currentLevel

    fun cost(type: UpgradeType, levels: UpgradeLevels): Int? = cost(levels.levelOf(type))

    // --- Fizige etkiler (seviye 1 = prototip degeri) ---

    /**
     * Yukseltme egrisi. Eskiden dort dal da DOGRUSALDI: ilk seviye alimi ile
     * son seviye alimi ayni miktarda iyilestiriyordu ve toplam artis cok
     * buyuktu (oyuncu: "upgrade'ler cok hizla iyilesiyor", 2026-08-14).
     *
     * Artik disbukey: 0, .054, .153, .281, .433, .606, .796, 1.0. Ilk
     * seviyeler kucuk, son seviyeler buyuk adim atar — hem "cabuk maxladim"
     * hissini geciktirir hem son seviyeyi gercek bir odul yapar.
     *
     * Us degeri 1.5: 2.0 denendi ve SPEED/BRAKE'te 1->2 adimi 1 km/h'nin
     * altinda kalip garajda AYNI sayiyi gosteriyordu ("para verdim, hicbir
     * sey degismedi"); 1.0 zaten sikayetin kaynagi olan dogrusal egri.
     */
    private const val CURVE_EXPONENT = 1.5f

    fun curve(level: Int): Float {
        val t = (level - 1).coerceIn(0, MAX_LEVEL - 1).toFloat() / (MAX_LEVEL - 1)
        return t.pow(CURVE_EXPONENT)
    }

    /** SPEED: skordan gelen hiz tavani buyur (seviye 8'de +1.12 birim ~ 216 km/h). */
    /**
     * Kariyerin baslangic hizi (birim). Butun bolumler bunu kullaniyor
     * ([LevelCatalog], `startSpeedKmh = 60`); garaj gostergesi de buna gore
     * hesaplar. [GameConfig.BASE_SPEED] (80 km/h) yalnizca sonsuz modun
     * tabani.
     */
    val CAREER_BASE_SPEED: Float = GameConfig.speedFromKmh(60)

    /**
     * SPEED yukseltmesinin tepe hiza katkisi (referans araca gore ~+35 km/h).
     * Arac carpaniyla CARPILMAZ — bkz. [scoreSpeedCap].
     */
    const val SPEED_UPGRADE_GAIN = 1.12f

    fun scoreSpeedCap(speedLevel: Int): Float =
        GameConfig.SCORE_SPEED_CAP_BASE + SPEED_UPGRADE_GAIN * curve(speedLevel)

    /** ACCELERATION: hedef hiza yaklasma orani (6.0 -> 10.0). */
    fun accelRate(accelerationLevel: Int): Float =
        GameConfig.ACCEL_RATE_BASE + 4.0f * curve(accelerationLevel)

    /**
     * ACCELERATION ayni zamanda yavaslamadan toparlanmayi da iyilestirir.
     * BRAKE gizlice CIFT yukseltmeydi (hem daha cok yavaslatiyor hem daha
     * hizli toparliyor); ikinci etkinin toplam artisi %79 -> %50'ye indi.
     */
    fun decelRate(brakeLevel: Int): Float =
        GameConfig.DECEL_RATE_BASE + 4.0f * curve(brakeLevel)

    /**
     * BRAKE: fren basiliyken hedef hizdan dusulen miktar.
     * Garajdaki gorunum artik 28 -> 29 -> 31 -> 33 -> 36 -> 39 -> 43 -> 47 km/h
     * (eskiden 28'den 55'e dogrusal cikiyordu; ilk adim +4 km/h idi).
     */
    fun brakePenalty(brakeLevel: Int): Float =
        GameConfig.BRAKE_SPEED_PENALTY_BASE + 0.60f * curve(brakeLevel)

    /**
     * BOOST: enerji tuketimi azalir, yani boost daha uzun surer.
     * Dolu bardan cikan sure 2.6 -> 4.2 s (+%58); eskiden 2.6 -> 5.1 s (+%92)
     * idi. Alt sinir guvenlik icin duruyor, yeni egride devreye girmiyor.
     */
    fun boostDrain(boostLevel: Int): Float =
        (GameConfig.BOOST_DRAIN_PER_SEC_BASE - 14f * curve(boostLevel)).coerceAtLeast(12f)

    /** BOOST seviyesi hizi da bir miktar artirir (guc). */
    fun boostSpeedBonus(boostLevel: Int): Float =
        GameConfig.BOOST_SPEED_BONUS_BASE + 0.36f * curve(boostLevel)

    // -----------------------------------------------------------------
    // Arac carpanlariyla birlesik degerler (2026-08-15)
    // -----------------------------------------------------------------
    //
    // Kural: carpan yukseltmeden gelen degerin USTUNE uygulanir, yerine
    // GECMEZ. Yani seviye 8 bir "Sehir" hâlâ seviye 1 bir "Super Araba"dan
    // cok daha hizlidir; arac secimi ilerlemenin yerini almaz, uzerine
    // karakter ekler (bkz. `docs/BALANCE.md` — Arac ozellikleri, sinir 1).
    //
    // Varsayilan govdede dort carpan da tam 1.0 oldugu icin bu asiri
    // yuklemeler eski davranisi BIT BIT ayni uretir (`x * 1f == x`).

    /**
     * Arac TABANI olcekler, yukseltme SABIT ekler.
     *
     * 2026-08-18'e kadar `scoreSpeedCap(level) * car.topSpeedMul` idi, yani
     * yukseltme de arac carpaniyla carpiliyordu. Carpan araligi genisletilince
     * bu bilesik etki tavani patlatti: Formula sv8'de 258 km/h cikiyordu ve
     * gosterge 240'ta kirpiyordu — oyuncu yukseltme alip hicbir sey
     * gormeyecekti.
     *
     * Ayirmanin ikinci faydasi: yukseltme herkese ayni +35 km/h'i veriyor,
     * yani UCUZ aracta oransal olarak daha degerli (120 uzerine +%29,
     * 184 uzerine +%19). Yukseltmeler yetisme araci, araclar tavan araci.
     */
    fun scoreSpeedCap(speedLevel: Int, car: CarShapeDef): Float =
        GameConfig.SCORE_SPEED_CAP_BASE * car.topSpeedMul +
            SPEED_UPGRADE_GAIN * curve(speedLevel)

    fun accelRate(accelerationLevel: Int, car: CarShapeDef): Float =
        accelRate(accelerationLevel) * car.accelMul

    fun brakePenalty(brakeLevel: Int, car: CarShapeDef): Float =
        brakePenalty(brakeLevel) * car.brakeMul

    /**
     * Carpan SUREYI olcekler, tuketimi degil — bu yuzden BOLME.
     * `boostMul = 1.12` => tuketim 38 / 1.12 = 33.9 => bar %12 daha uzun surer.
     *
     * [boostDrain] icindeki 12/s alt siniri asilmaz: bolen 1.25'i gecmedigi
     * icin (katalog testi bunu zorunlu kiliyor) sonuc hicbir zaman sifira
     * ya da negatife inemez.
     */
    fun boostDrain(boostLevel: Int, car: CarShapeDef): Float =
        boostDrain(boostLevel) / car.boostMul

    // --- Garaj ekraninda gosterilen somut degerler ---

    /**
     * Ornek: SPEED icin "181 km/h", BOOST icin "2.6 s".
     *
     * [car] SECILI GOVDE — 2026-08-16'da eklendi ve sebebi bir kusurdu:
     * fonksiyon `scoreSpeedCap(level)` cagiriyordu, yani govde carpanini
     * ATLIYORDU. Sonuc: garaj her araç icin AYNI hizi yaziyordu. Super
     * Araba'nin gercek tavani 193 km/h iken ekran 180 diyordu.
     *
     * Bu, "hiz yukseltmesi aldim ama bir sey degismedi" hissinin dogrudan
     * kaynagiydi — deger gercekten degisiyordu, ekran soylemiyordu
     * (bkz. docs/REVIEW_GAMEPLAY.md).
     */
    fun displayValue(type: UpgradeType, level: Int, car: CarShapeDef): String = when (type) {
        // TABAN: kariyerin baslangic hizi, [GameConfig.BASE_SPEED] DEGIL.
        //
        // 2026-08-17'de her bolum 60 km/h'den baslar oldu (once 60-75 arasi
        // rampaliydi ve 9+ bolumler 80'lik varsayilana dusuyordu). Garaj ise
        // hesabini hala 80 tabaniyla yapiyordu, yani KARIYERDE ULASILMAYACAK
        // bir sayi yaziyordu: seviye 1 icin 177, gercekte 158.
        //
        // Bu, 2026-08-16'da kapatilan "garaj her arac icin ayni hizi yaziyor"
        // kusurunun ayni ailesinden — ama bu sefer sayinin kendisi yanlisti.
        //
        // Neden kariyer tabani secildi: 30 bolumun 30'u kariyerde ve garaj
        // yukseltmeleri oraya hizmet ediyor. Sonsuz modda oyuncu bu sayinin
        // biraz USTUNE cikar; az soz verip fazlasini vermek, tersinden
        // daha iyidir.
        //
        // ⚠ Bolum bazli [LevelDef.speedRampScale] yuzunden erken bolumlerde
        // gercek tavan daha da dusuk (bolum 1'de ~100 km/h). Tek bir sayi
        // butun bolumleri anlatamaz; burada gosterilen, rampa 1.0 olan
        // bolumlerin tavanidir.
        UpgradeType.SPEED ->
            "${GameConfig.speedToKmh(CAREER_BASE_SPEED + scoreSpeedCap(level, car))} km/h"

        // Saniye yerine MILISANIYE: "0.2 s" gosterimi seviye 2'den sonra tum
        // seviyelerde "0.1 s"e yuvarlaniyordu, yani garaj oyuncuya "ivme
        // yukseltmesi hicbir sey yapmiyor" diyordu (2026-08-14'te bulundu).
        // Yeni gosterim: 167, 161, 151, 140, 129, 119, 109, 100 ms.
        UpgradeType.ACCELERATION ->
            "${(1000f / accelRate(level, car)).roundToInt()} ms"

        // Bir ondalik: tam sayiya yuvarlanınca Boga 67'nin (fren 0.90) 1. ve
        // 2. seviyesi ikisi de "-26 km/h" cikiyordu (2026-08-16). BOOST ile
        // ayni kusur, ayni cozum — garaj bir seviye farkini gostermek
        // ZORUNDA, yoksa oyuncu parasini bosa verdigini sanir.
        UpgradeType.BRAKE ->
            "-${oneDecimal(brakePenalty(level, car) / GameConfig.SPEEDOMETER_SPAN * GameConfig.SPEEDOMETER_RANGE_KMH)} km/h"

        // IKI ONDALIK, bir ondalik DEGIL (2026-08-16). Bir ondalikta Dag
        // Kecisi'nin (boost 1.06) 1. ve 2. seviyesi ikisi de "2.8 s"
        // gosteriyordu — yani oyuncu boost yukseltmesini satin aliyor ve
        // garajda hicbir sey degismiyordu. ACCELERATION dalinda 14 Agustos'ta
        // ayni kusur bulunmus ve saniye yerine milisaniyeye gecilerek
        // cozulmustu; bu ayni kusurun boost'taki hali.
        //
        // NOT: gosterim duzeldi ama ALTTAKI mesele duruyor — maliyet dogrusal,
        // etki disbukey ([curve]), yani ilk seviyeler gercekten cok az sey
        // yapiyor (bkz. docs/REVIEW_PRODUCT.md). O bir denge karari.
        UpgradeType.BOOST ->
            "${twoDecimals(GameConfig.BOOST_MAX / boostDrain(level, car))} s"
    }

    /**
     * Garajdaki ARAC KARTI icin somut aralik: "120 → 155 km/h".
     *
     * 2026-08-18'e kadar orada yuzde yaziyordu ("+18%"). Sahibi: *"arac
     * tanitim garajinda hiz gibi ozellikler var ama rakamlarla yazmiyor,
     * +/- seklinde"*. Yuzde iki seyi birden gizliyordu: aracin GERCEK hizini
     * ve yukseltmelerle nereye gidebilecegini.
     *
     * Iki uc nokta [displayValue] ile hesaplanir (yani birim ve yuvarlama
     * kurallari tek yerde kalir), sonra ortak birim bir kez yazilir:
     * "120 → 155 km/h", "167 → 100 ms".
     */
    fun displayRange(type: UpgradeType, car: CarShapeDef): String {
        val ilk = displayValue(type, 1, car)
        val son = displayValue(type, MAX_LEVEL, car)
        val birim = ilk.substringAfter(' ', "")
        return if (birim.isNotEmpty() && son.endsWith(birim)) {
            "${ilk.removeSuffix(" $birim")} → ${son.removeSuffix(" $birim")} $birim"
        } else {
            "$ilk → $son"
        }
    }

    fun title(type: UpgradeType, language: AppLanguage): String = when (type) {
        UpgradeType.SPEED -> language.pick(tr = "HIZ", en = "SPEED")
        UpgradeType.ACCELERATION -> language.pick(tr = "İVME", en = "ACCELERATION")
        UpgradeType.BRAKE -> language.pick(tr = "FREN", en = "BRAKE")
        UpgradeType.BOOST -> language.pick(tr = "BOOST", en = "BOOST")
    }

    fun description(type: UpgradeType, language: AppLanguage): String = when (type) {
        UpgradeType.SPEED -> language.pick(
            tr = "Aracın ulaşabileceği en yüksek hız",
            en = "Maximum speed the car can reach"
        )

        UpgradeType.ACCELERATION -> language.pick(
            tr = "Fren ve boost sonrası toparlanma süresi",
            en = "Recovery time after braking and boosting"
        )

        UpgradeType.BRAKE -> language.pick(
            tr = "Frene basınca ne kadar yavaşladığın",
            en = "How much the brake slows you down"
        )

        UpgradeType.BOOST -> language.pick(
            tr = "Dolu bardan çıkan toplam boost süresi",
            en = "Total boost time from a full bar"
        )
    }

    private fun oneDecimal(value: Float): String {
        val scaled = (value * 10f).roundToInt()
        return "${scaled / 10}.${scaled % 10}"
    }

    private fun twoDecimals(value: Float): String {
        val scaled = (value * 100f).roundToInt()
        return "${scaled / 100}.${(scaled % 100).toString().padStart(2, '0')}"
    }
}
