package com.miniappfactory.krondrive.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Odullu reklam — Kron Drive'da tek kullanimi: carpisma sonrasi "reklam izle,
 * devam et".
 *
 * Odul SADECE SDK'nin gercek "kazanildi" callback'inde verilir; kullanici
 * videoyu yarida keserse odul yoktur. [onAdClosed] ise reklam ekrani NASIL
 * kapanirsa kapansin her zaman cagrilir — cagiran taraftaki "yukleniyor"
 * durumu asili kalmasin diye.
 */
object RewardedAdManager {

    fun loadAndShow(
        context: Context,
        activity: Activity,
        onRewardEarned: () -> Unit,
        onFailure: () -> Unit,
        onAdClosed: () -> Unit = {}
    ) {
        RewardedAd.load(
            context,
            AdIds.rewardedAdUnitId(),
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() = onAdClosed()
                        override fun onAdFailedToShowFullScreenContent(error: AdError) = onAdClosed()
                    }
                    rewardedAd.show(activity) { onRewardEarned() }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    onFailure()
                    onAdClosed()
                }
            }
        )
    }
}
