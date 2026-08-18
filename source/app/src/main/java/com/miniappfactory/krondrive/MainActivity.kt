package com.miniappfactory.krondrive

import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.miniappfactory.krondrive.ads.AdIds
import com.miniappfactory.krondrive.ads.ConsentManager
import com.miniappfactory.krondrive.audio.EngineSoundManager
import com.miniappfactory.krondrive.ui.KronViewModel
import com.miniappfactory.krondrive.ui.navigation.AppNavigation
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

        ConsentManager.requestConsent(this) { adsConsentResolved.value = true }
        // Guvenlik agi: consent akisi (ag hatasi, yayinlanmamis Privacy form vb.)
        // hicbir dali cagirmadan takilirsa banner O OTURUM BOYUNCA hic gorunmez.
        // 4 saniye sonra TEKRAR SORULUR — ama kosulsuz acilmaz: reklam ancak
        // SDK "istenebilir" diyorsa gosterilir. Eskiden burada kosulsuz `true`
        // vardi ve onay cozulmeden banner acilabiliyordu (uyum denetimi,
        // 2026-08-14; EU User Consent Policy ihlali riski).
        Handler(Looper.getMainLooper()).postDelayed({
            if (!adsConsentResolved.value && ConsentManager.canRequestAds(this)) {
                adsConsentResolved.value = true
            }
        }, 4000)

        setContent {
            val consentResolved by adsConsentResolved
            KronDriveTheme {
                // BURADA TAM EKRAN BIR ZEMIN KATMANI YOK — bilerek.
                //
                // Eskiden burasi `Surface(fillMaxSize, KronColors.Background)`
                // idi. Amaci sistem cubuklarinin ARKASINI oyunun rengiyle
                // doldurmakti (`enableEdgeToEdge` ile pencere oraya uzaniyor).
                // Ama ayni isi pencere arkaplani zaten yapiyor ve o
                // kaldirilamaz — yani ayni piksel iki kez boyaniyordu.
                //
                // Renk `res/values/themes.xml` icindeki `android:windowBackground`
                // olarak duruyor; sistem cubuklarinin arkasi eskisi gibi dolu,
                // bir doldurma katmani eksik. Ic bosluklari (safe area) her
                // ekran kendi yonetir — oyun ekraninda tuval tam ekran,
                // sadece kontroller icerlek olmali.
                AppNavigation(viewModel = viewModel, adsConsentResolved = consentResolved)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Ekran arka plana giderken ses kesin sussun (oyun ekrani da kendi
        // tarafinda durduruyor; burasi her ekran icin garanti).
        EngineSoundManager.stop()
    }

}
