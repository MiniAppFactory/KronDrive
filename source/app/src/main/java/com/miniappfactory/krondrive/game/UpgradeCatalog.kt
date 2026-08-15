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

    /** Seviye 1'den 8'e toplam maliyet: 250+500+...+1750 = 7000 coin. */
    fun cost(currentLevel: Int): Int? =
        if (currentLevel >= MAX_LEVEL) null else 250 * currentLevel

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
    fun scoreSpeedCap(speedLevel: Int): Float =
        GameConfig.SCORE_SPEED_CAP_BASE + 1.12f * curve(speedLevel)

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

    fun scoreSpeedCap(speedLevel: Int, car: CarShapeDef): Float =
        scoreSpeedCap(speedLevel) * car.topSpeedMul

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

    /** Ornek: SPEED icin "181 km/h", BOOST icin "2.6 s". */
    fun displayValue(type: UpgradeType, level: Int): String = when (type) {
        UpgradeType.SPEED ->
            "${GameConfig.speedToKmh(GameConfig.BASE_SPEED + scoreSpeedCap(level))} km/h"

        // Saniye yerine MILISANIYE: "0.2 s" gosterimi seviye 2'den sonra tum
        // seviyelerde "0.1 s"e yuvarlaniyordu, yani garaj oyuncuya "ivme
        // yukseltmesi hicbir sey yapmiyor" diyordu (2026-08-14'te bulundu).
        // Yeni gosterim: 167, 161, 151, 140, 129, 119, 109, 100 ms.
        UpgradeType.ACCELERATION ->
            "${(1000f / accelRate(level)).roundToInt()} ms"

        UpgradeType.BRAKE ->
            "-${(brakePenalty(level) / GameConfig.SPEEDOMETER_SPAN * GameConfig.SPEEDOMETER_RANGE_KMH).roundToInt()} km/h"

        UpgradeType.BOOST ->
            "${oneDecimal(GameConfig.BOOST_MAX / boostDrain(level))} s"
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
}
