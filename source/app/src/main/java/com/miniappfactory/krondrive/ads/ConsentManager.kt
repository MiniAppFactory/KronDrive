package com.miniappfactory.krondrive.ads

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * UMP (User Messaging Platform) onay akisi tek yerde.
 *
 * Iki kural, ikisi de Google'in EU User Consent Policy'sinden:
 *
 * 1. **Onay cozulmeden reklam istenmez.** Uygulama acilisindaki guvenlik agi
 *    zaman asimi bile bunu delemez — zaman asimi dolunca reklamlar ancak
 *    [ConsentInformation.canRequestAds] true ise acilir.
 * 2. **Onayini degistirebilmeli.** AEA/Birlesik Krallik kullanicisina
 *    "gizlilik secenekleri" gorunur ve tiklanabilir bir yerden sunulmali;
 *    bu yuzden ayarlarda [isPrivacyOptionsRequired] true iken bir buton cikar.
 *    (Uyum denetimi, 2026-08-14: bu giris noktasi hic yoktu.)
 */
object ConsentManager {

    private var consentInformation: ConsentInformation? = null

    /**
     * Onay bilgisini gunceller ve gerekiyorsa formu gosterir.
     * [onResolved] yalnizca reklam ISTENEBILIR duruma gelindiginde cagrilir.
     */
    fun requestConsent(activity: Activity, onResolved: () -> Unit) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        consentInformation = info
        info.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    // Form kapandi (ya da hic gerekmedi): karar ne olursa olsun
                    // reklam istenebilir mi diye SDK'ya soruyoruz.
                    if (info.canRequestAds()) onResolved()
                }
            },
            {
                // Guncelleme hata verdi (ag yok, form yayinlanmamis vb.).
                // Onceki oturumdan kalan onay yeterliyse reklamlara devam.
                if (info.canRequestAds()) onResolved()
            }
        )
    }

    /**
     * Guvenlik agi zaman asimi icin: akis hicbir dali cagirmadan takilirsa
     * bile reklamlar KOSULSUZ acilmamali. Sadece SDK izin veriyorsa true.
     */
    fun canRequestAds(context: Context): Boolean =
        (consentInformation ?: UserMessagingPlatform.getConsentInformation(context))
            .canRequestAds()

    /** Ayarlarda "gizlilik secenekleri" butonu gosterilmeli mi. */
    fun isPrivacyOptionsRequired(context: Context): Boolean =
        (consentInformation ?: UserMessagingPlatform.getConsentInformation(context))
            .privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /** Kullanicinin onayini degistirebilecegi formu acar. */
    fun showPrivacyOptions(activity: Activity, onDismissed: () -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { onDismissed() }
    }
}
