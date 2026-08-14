package com.miniappfactory.krondrive.ads

import android.app.Activity
import android.content.Context
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
 * hatasi... hangi dal olursa olsun [onProceed] tam olarak bir kez calisir.
 */
object InterstitialAdManager {

    fun loadAndShow(context: Context, activity: Activity, onProceed: () -> Unit) {
        var proceeded = false
        fun proceedOnce() {
            if (!proceeded) {
                proceeded = true
                onProceed()
            }
        }

        InterstitialAd.load(
            context,
            AdIds.interstitialAdUnitId(),
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    interstitialAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() = proceedOnce()
                        override fun onAdFailedToShowFullScreenContent(error: AdError) = proceedOnce()
                    }
                    interstitialAd.show(activity)
                }

                override fun onAdFailedToLoad(error: LoadAdError) = proceedOnce()
            }
        )
    }
}
