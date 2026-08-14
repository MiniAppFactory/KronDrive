package com.miniappfactory.krondrive.ads

import com.miniappfactory.krondrive.BuildConfig

/**
 * TUM AdMob kimlikleri burada — baska hicbir dosyada reklam ID'si yok.
 *
 * Su an hepsi Google'in herkese acik TEST birimleridir. Yayin oncesi yapilacak
 * TEK sey: asagidaki dort PRODUCTION_* sabitini AdMob konsolundaki gercek
 * ID'lerle degistirmek + AndroidManifest.xml icindeki APPLICATION_ID meta-data
 * degerini gercek App ID ile guncellemek (bkz. docs/ADMOB_SETUP.md).
 *
 * Debug build'ler HER ZAMAN test ID'leriyle calisir — gelistirirken kendi
 * reklamlarina tiklayip AdMob "invalid traffic" ihlali olusturmak imkansiz.
 */
object AdIds {
    // PLACEHOLDER — gercek ID ile degistirilecek.
    private const val PRODUCTION_BANNER_AD_UNIT_ID = "ca-app-pub-0000000000000000/0000000000"
    private const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"

    // PLACEHOLDER — gercek ID ile degistirilecek.
    private const val PRODUCTION_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-0000000000000000/0000000000"
    private const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    // PLACEHOLDER — gercek ID ile degistirilecek.
    private const val PRODUCTION_REWARDED_AD_UNIT_ID = "ca-app-pub-0000000000000000/0000000000"
    private const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    /**
     * Gercek ID'ler girilene kadar release build'lerde de test reklamlari
     * kullanilir — yanlislikla gecersiz bir ID ile yayina cikilmasin diye.
     * Gercek ID girildiginde bu bayrak `false` yapilmalidir.
     */
    private const val USE_TEST_IDS_IN_RELEASE = true

    private val useTestIds: Boolean get() = BuildConfig.DEBUG || USE_TEST_IDS_IN_RELEASE

    fun bannerAdUnitId(): String =
        if (useTestIds) TEST_BANNER_AD_UNIT_ID else PRODUCTION_BANNER_AD_UNIT_ID

    fun interstitialAdUnitId(): String =
        if (useTestIds) TEST_INTERSTITIAL_AD_UNIT_ID else PRODUCTION_INTERSTITIAL_AD_UNIT_ID

    fun rewardedAdUnitId(): String =
        if (useTestIds) TEST_REWARDED_AD_UNIT_ID else PRODUCTION_REWARDED_AD_UNIT_ID

    /**
     * Gelistirici/test cihazlarinin AdMob ID'leri. Release build gercek
     * reklamlarla test edilirken bu cihazlara her zaman guvenli test reklami
     * gosterilir. Cihaz ID'si, ilk reklam isteginde logcat'te "Ads" etiketiyle
     * yazdirilir.
     */
    val developerTestDeviceIds: List<String> = emptyList()
}
