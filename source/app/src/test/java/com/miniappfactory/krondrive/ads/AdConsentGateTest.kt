package com.miniappfactory.krondrive.ads

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ONAY KAPISI (2026-08-19, uyum denetimi).
 *
 * Neden bu test var: onay kapisi yalnizca BANNER'a bagliydi. `GameScreen`
 * onay bayragini hic almiyordu, dosyada tek bir `ConsentManager` referansi
 * yoktu ve UMP onayini REDDEDEN bir AEA/Birlesik Krallik kullanicisi banner
 * gormedigi halde gecis ve odullu reklam goruyordu. Kusur "yanlis mantik"
 * degildi — mantik hic yoktu; bir kod yolu kontrolden gecmiyordu.
 *
 * Bu yuzden testler DEGERLERI degil, KAPI DAVRANISINI sabitliyor: her yeni
 * reklam yolu [AdConsentGate] uzerinden gecmek zorunda ve kapinin hicbir
 * girdisi tek basina "evet" diyemiyor.
 */
class AdConsentGateTest {

    // -----------------------------------------------------------------
    // adsAllowed — iki kaynak, ikisi de "evet" demeli
    // -----------------------------------------------------------------

    @Test
    fun `reklam ancak her iki kaynak da izin verirse istenir`() {
        assertTrue(AdConsentGate.adsAllowed(consentLatched = true, sdkCanRequestAds = true))
    }

    /**
     * ONAYI GERI CEKME SENARYOSU — kapinin asil varlik sebebi.
     *
     * `MainActivity`'deki mandal bir kez `true` olunca GERI ALINMIYOR. Oyuncu
     * Ayarlar > "Gizlilik secenekleri"nden onayini geri cektiginde mandal hala
     * `true`dur; degisen tek sey SDK'nin cevabidir. Mandal tek basina yetkili
     * sayilsaydi, onayini geri cekmis kullaniciya reklam gostermeye devam
     * ederdik — kapinin kapatmasi gereken en somut ihlal budur.
     */
    @Test
    fun `onay geri cekilince mandal hala acik olsa bile reklam istenmez`() {
        assertFalse(AdConsentGate.adsAllowed(consentLatched = true, sdkCanRequestAds = false))
    }

    /**
     * Ters yon: SDK "istenebilir" dese bile mandal kapaliyken reklam yok.
     * Acilisin ilk saniyeleri boyledir (onay akisi henuz cozulmedi) ve
     * BEKLEMEK dogru davranistir — reklamsiz gecen birkac saniye, onay
     * cozulmeden gosterilmis tek bir reklamdan iyidir.
     */
    @Test
    fun `mandal kapaliyken SDK izin verse bile reklam istenmez`() {
        assertFalse(AdConsentGate.adsAllowed(consentLatched = false, sdkCanRequestAds = true))
    }

    @Test
    fun `iki kaynak da hayir derse reklam istenmez`() {
        assertFalse(AdConsentGate.adsAllowed(consentLatched = false, sdkCanRequestAds = false))
    }

    // -----------------------------------------------------------------
    // Gecis reklami
    // -----------------------------------------------------------------

    @Test
    fun `gecis reklami onay ve siklik birlikte izin verirse gosterilir`() {
        assertTrue(
            AdConsentGate.shouldShowInterstitial(
                adsAllowed = true,
                activityAvailable = true,
                frequencyAllows = true
            )
        )
    }

    /**
     * ASIL ACIK: siklik kurali "goster" dese bile onay yoksa gosterilmez.
     *
     * Kusurlu haldeki `withOptionalInterstitial` tam olarak sadece bu
     * `frequencyAllows` kosuluna bakiyordu.
     */
    @Test
    fun `onay yokken siklik kurali gosterse bile gecis reklami cikmaz`() {
        assertFalse(
            AdConsentGate.shouldShowInterstitial(
                adsAllowed = false,
                activityAvailable = true,
                frequencyAllows = true
            )
        )
    }

    /** Activity yoksa (ekran kapaniyor) reklam gosterilemez — cokme yerine atlama. */
    @Test
    fun `activity yokken gecis reklami cikmaz`() {
        assertFalse(
            AdConsentGate.shouldShowInterstitial(
                adsAllowed = true,
                activityAvailable = false,
                frequencyAllows = true
            )
        )
    }

    @Test
    fun `siklik izin vermiyorsa onay olsa bile gecis reklami cikmaz`() {
        assertFalse(
            AdConsentGate.shouldShowInterstitial(
                adsAllowed = true,
                activityAvailable = true,
                frequencyAllows = false
            )
        )
    }

    // -----------------------------------------------------------------
    // Odullu reklam
    // -----------------------------------------------------------------

    @Test
    fun `odullu reklam onay varken teklif edilir`() {
        assertTrue(AdConsentGate.shouldOfferRewarded(adsAllowed = true, activityAvailable = true))
    }

    /**
     * Onay yokken odullu reklam TEKLIF BILE EDILMEZ.
     *
     * Burada kapali olan sey reklamin kendisi degil, VAADI: buton
     * gorunup de odul verilememesi, oyuncu acisindan bozuk bir butondur
     * (CLAUDE.md kural 4). Cagiran taraf bu `false` degerini butonu hic
     * cizmemek icin kullanir.
     */
    @Test
    fun `onay yokken odullu reklam teklif edilmez`() {
        assertFalse(AdConsentGate.shouldOfferRewarded(adsAllowed = false, activityAvailable = true))
    }

    @Test
    fun `activity yokken odullu reklam teklif edilmez`() {
        assertFalse(AdConsentGate.shouldOfferRewarded(adsAllowed = true, activityAvailable = false))
    }

    // -----------------------------------------------------------------
    // Kapinin butunu
    // -----------------------------------------------------------------

    /**
     * ONAYI REDDEDEN KULLANICI: HICBIR REKLAM YOLU ACIK KALMAMALI.
     *
     * Tek tek dallar yukarida test edildi; bu test kapinin BUTUNUNU tek
     * cumlede sabitliyor. Yeni bir reklam turu eklenirse (app-open, native)
     * karari yine buradan gecmeli ve bu test onun da kapali dogmasini
     * bekleyecek.
     */
    @Test
    fun `onayi reddeden kullaniciya hicbir reklam turu acilmaz`() {
        val allowed = AdConsentGate.adsAllowed(consentLatched = true, sdkCanRequestAds = false)

        assertFalse(allowed)
        assertFalse(
            AdConsentGate.shouldShowInterstitial(
                adsAllowed = allowed,
                activityAvailable = true,
                frequencyAllows = true
            )
        )
        assertFalse(
            AdConsentGate.shouldOfferRewarded(adsAllowed = allowed, activityAvailable = true)
        )
    }

    /**
     * ONAY VEREN KULLANICI: hicbir sey kaybetmemeli.
     *
     * Kapinin ters riski de var — fazla kisitlayici bir duzeltme gelirin
     * tamamini kapatabilirdi. Onay verilmis durumda her iki reklam turu de
     * eskisi gibi acik.
     */
    @Test
    fun `onay veren kullanicida reklam yollari acik kalir`() {
        val allowed = AdConsentGate.adsAllowed(consentLatched = true, sdkCanRequestAds = true)

        assertTrue(allowed)
        assertTrue(
            AdConsentGate.shouldShowInterstitial(
                adsAllowed = allowed,
                activityAvailable = true,
                frequencyAllows = true
            )
        )
        assertTrue(AdConsentGate.shouldOfferRewarded(adsAllowed = allowed, activityAvailable = true))
    }
}
