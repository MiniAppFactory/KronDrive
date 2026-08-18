package com.miniappfactory.krondrive.ads

/**
 * "Bu reklam istenebilir mi" kararinin TEK yeri.
 *
 * Neden ayri ve neden SAF KOTLIN (Android importu yok): karar mantigi
 * [ConsentManager] icinde otursaydi yalnizca cihazda dogrulanabilirdi —
 * UMP SDK'si `Activity` ister. Burada durdugu icin JVM testiyle
 * dogrulanabiliyor (`ads/AdConsentGateTest.kt`).
 *
 * ⚠ NEDEN VAR (uyum denetimi, 2026-08-19): onay kapisi yalnizca BANNER'a
 * bagliydi. `adsConsentResolved` bayragi menü/garaj/bölüm/görev/ayarlar
 * ekranlarina geciyordu ama [GameScreen] bu parametreyi HIC almiyordu ve
 * dosyada tek bir `ConsentManager` referansi yoktu. Sonuc: UMP onayini
 * REDDEDEN bir AEA/Birlesik Krallik kullanicisi banner gormuyor ama gecis ve
 * odullu reklam goruyordu — Google EU User Consent Policy'sine karsi gercek
 * bir acik.
 *
 * Kapinin iki girdisi var ve ikisi de gerekli:
 *
 * 1. **Mandal** (`consentLatched`): `MainActivity` acilista bir kez `true`
 *    yapar ve bir daha geri almaz.
 * 2. **SDK'nin anlik cevabi** (`sdkCanRequestAds`): tek YETKILI kaynak budur.
 *    Mandal tek basina yetmez, cunku kullanici Ayarlar > "Gizlilik
 *    secenekleri" ile onayini GERI CEKEBILIR; o an mandal hala `true`'dur ama
 *    SDK artik `false` der.
 *
 * Yani kapi HER ZAMAN mevcut banner davranisi kadar veya ondan DAHA
 * KISITLAYICIDIR; hicbir dal bannerdan daha gevsek degildir.
 */
object AdConsentGate {

    /**
     * Reklam istenebilir mi. VE (`&&`) bilincli: iki kaynaktan biri "hayir"
     * diyorsa reklam yok. Emin olunamayan durumda reklam GOSTERILMEZ —
     * yanlis tarafa dusmenin bedeli, kacan bir gosterim ile bir politika
     * ihlali arasindaki fark.
     */
    fun adsAllowed(consentLatched: Boolean, sdkCanRequestAds: Boolean): Boolean =
        consentLatched && sdkCanRequestAds

    /**
     * Gecis reklami gosterilsin mi.
     *
     * `frequencyAllows` cagiran taraftaki sıklık kurali (bkz.
     * `GameConfig.INTERSTITIAL_AFTER_EVERY_RUN` / `AdFrequency`). Onay
     * kontrolu ONCE gelir ve bu SIRA onemlidir: cagiran taraf sıklık
     * sayacini ancak bu fonksiyon `true` dedikten sonra tuketmeli, yoksa
     * onay yokken "gosterildi" sayilan bir reklam sayaci yakar.
     */
    fun shouldShowInterstitial(
        adsAllowed: Boolean,
        activityAvailable: Boolean,
        frequencyAllows: Boolean
    ): Boolean = adsAllowed && activityAvailable && frequencyAllows

    /**
     * Odullu reklam TEKLIF EDILSIN mi (buton gorunsun/etkin olsun mu).
     *
     * Bu bir "gosterilebilir mi" degil, "vaat edilebilir mi" sorusudur:
     * `false` iken buton HIC CIKMAZ. Onay yokken "REKLAM İZLE → +500 COIN"
     * yazip hicbir sey verememek, sessiz basarisizligin en kotu turudur
     * (CLAUDE.md kural 4: reklam oyunu bloklamaz, ama ödül de vaat edilemez).
     */
    fun shouldOfferRewarded(adsAllowed: Boolean, activityAvailable: Boolean): Boolean =
        adsAllowed && activityAvailable
}
