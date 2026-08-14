package com.miniappfactory.krondrive

import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.miniappfactory.krondrive.ads.AdIds
import com.miniappfactory.krondrive.audio.EngineSoundManager
import com.miniappfactory.krondrive.ui.KronViewModel
import com.miniappfactory.krondrive.ui.navigation.AppNavigation
import com.miniappfactory.krondrive.ui.theme.KronColors
import com.miniappfactory.krondrive.ui.theme.KronDriveTheme

class MainActivity : ComponentActivity() {

    private val viewModel: KronViewModel by viewModels()

    /** Banner reklam, consent akisi cozulmeden gosterilmez. */
    private val adsConsentResolved = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Donanim ses tuslari HER ZAMAN oyunun kullandigi akisi kontrol etsin
        // (motor sesi AudioTrack ile STREAM_MUSIC'e cikiyor).
        volumeControlStream = AudioManager.STREAM_MUSIC

        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setTestDeviceIds(AdIds.developerTestDeviceIds)
                .build()
        )
        MobileAds.initialize(applicationContext) {}

        requestAndShowUmpConsentIfRequired { adsConsentResolved.value = true }
        // Guvenlik agi: consent akisi (ag hatasi, yayinlanmamis Privacy form vb.)
        // hicbir dali cagirmadan takilirsa banner O OTURUM BOYUNCA hic gorunmez.
        // 4 saniye sonra reklamlari yine de ac.
        Handler(Looper.getMainLooper()).postDelayed({
            if (!adsConsentResolved.value) adsConsentResolved.value = true
        }, 4000)

        setContent {
            val consentResolved by adsConsentResolved
            KronDriveTheme {
                // Zemin rengi Activity seviyesinde veriliyor: sistem cubuklarinin
                // ARKASI da oyunun rengiyle dolsun (siyah serit gorunmesin).
                // Ic bosluklari (safe area) her ekran kendi yonetir — oyun
                // ekraninda tuval tam ekran, sadece kontroller icerlek olmali.
                Surface(modifier = Modifier.fillMaxSize(), color = KronColors.Background) {
                    AppNavigation(viewModel = viewModel, adsConsentResolved = consentResolved)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Ekran arka plana giderken ses kesin sussun (oyun ekrani da kendi
        // tarafinda durduruyor; burasi her ekran icin garanti).
        EngineSoundManager.stop()
    }

    /**
     * UMP consent: guncelleme HATA verse bile `canRequestAds()` true ise
     * reklamlara devam edilir — AdMob konsolunda Privacy & Messaging formu
     * yayinlanmamissa hata donuyor ama bu reklam istemeyi engellememeli.
     */
    private fun requestAndShowUmpConsentIfRequired(onResolved: () -> Unit) {
        val params = ConsentRequestParameters.Builder().build()
        val consentInformation = UserMessagingPlatform.getConsentInformation(this)
        consentInformation.requestConsentInfoUpdate(
            this,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(this) { onResolved() }
            },
            {
                if (consentInformation.canRequestAds()) onResolved()
            }
        )
    }
}
