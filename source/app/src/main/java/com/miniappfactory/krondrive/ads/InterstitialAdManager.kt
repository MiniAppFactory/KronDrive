package com.miniappfactory.krondrive.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Bolumler arasi gecis reklami. Odul yok; kullanici reklami kapatinca (veya
 * reklam hic yuklenemezse) [onProceed] cagrilir.
 *
 * KURAL: reklam akisi oyunu ASLA bloklamaz — no-fill, ag hatasi, gosterim
 * hatasi, ONAY YOKLUGU... hangi dal olursa olsun [onProceed] tam olarak bir
 * kez calisir.
 */
object InterstitialAdManager {

    /**
     * Reklamin yuklenmesi icin verilen sure. Dolarsa akis BEKLEMEDEN devam
     * eder ve o istek icin reklam artik gosterilmez.
     *
     * ⚠ 2026-08-19'a kadar hicbir sinir yoktu: `InterstitialAd.load` yavas
     * agda saniyelerce, kotu durumda hic donmeyebiliyor ve o sure boyunca
     * oyuncu sonuc ekraninda KILITLI kaliyordu (ne buton ne geri tusu bir sey
     * yapiyordu). Sahibi bunu *"geri tusu calismiyor"* diye bildirdi. Bu,
     * projenin 4 numarali degismez kuralinin ("reklam akisi oyunu ASLA
     * bloklamaz") dogrudan ihlaliydi.
     *
     * 3,5 sn: cihazda olculen tipik yukleme 0,5-2 sn; esik onun ustunde ama
     * oyuncunun "dondu" diyecegi surenin altinda.
     */
    private const val LOAD_TIMEOUT_MS = 3_500L

    fun loadAndShow(context: Context, activity: Activity, onProceed: () -> Unit) {
        var proceeded = false
        fun proceedOnce() {
            if (!proceeded) {
                proceeded = true
                onProceed()
            }
        }

        // Sure dolarsa devam edilir; geciken reklam ARTIK GOSTERILMEZ —
        // aksi halde oyuncu menuye dondukten saniyeler sonra ekranina bir
        // reklam duserdi (AdMob'un "unexpected interstitial" kurali).
        val timeout = Handler(Looper.getMainLooper())
        timeout.postDelayed({ proceedOnce() }, LOAD_TIMEOUT_MS)

        // ONAY KAPISI — SON SAVUNMA HATTI (uyum denetimi, 2026-08-19).
        //
        // Cagiran taraf zaten [AdConsentGate] ile kontrol ediyor; buradaki
        // kontrol o kontrolun yerine gecmez, ONUN UNUTULMASINA karsi durur.
        // Acigin cikis sebebi tam olarak buydu: gecis reklaminin cagrildigi
        // dort ayri yol vardi ve hicbiri onaya bakmiyordu. Kontrol istegin
        // ciktigi TEK noktada da durursa, yarin eklenecek besinci yol da
        // otomatik olarak kapali dogar.
        //
        // Onay yoksa BEKLEMEDEN devam: oyuncu icin fark, reklamin hic
        // acilmamasidir — takilma yok.
        if (!ConsentManager.canRequestAds(context)) {
            timeout.removeCallbacksAndMessages(null)
            proceedOnce()
            return
        }

        InterstitialAd.load(
            context,
            AdIds.interstitialAdUnitId(),
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    // Sure dolmus ve akis devam etmisse reklam GOSTERILMEZ.
                    if (proceeded) return
                    timeout.removeCallbacksAndMessages(null)
                    interstitialAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() = proceedOnce()
                        override fun onAdFailedToShowFullScreenContent(error: AdError) = proceedOnce()
                    }
                    interstitialAd.show(activity)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    timeout.removeCallbacksAndMessages(null)
                    proceedOnce()
                }
            }
        )
    }
}
