package com.miniappfactory.krondrive.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Banner reklam — SADECE menu/garaj/gorev gibi oyun disi ekranlarda kullanilir.
 * Oyun ekraninda banner YOKTUR: kontrollerin (sol/sag/fren/boost) uzerine
 * hicbir reklam gelmemeli.
 */
@Composable
fun BannerAdView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val adWidthDp = configuration.screenWidthDp

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = {
            AdView(context).apply {
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidthDp))
                adUnitId = AdIds.bannerAdUnitId()
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
