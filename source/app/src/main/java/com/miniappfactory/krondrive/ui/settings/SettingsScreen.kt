package com.miniappfactory.krondrive.ui.settings

import com.miniappfactory.krondrive.BuildConfig
import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miniappfactory.krondrive.data.AppLanguage
import com.miniappfactory.krondrive.data.PlayerProgress
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.miniappfactory.krondrive.ads.ConsentManager
import com.miniappfactory.krondrive.ui.common.KronCard
import com.miniappfactory.krondrive.ui.common.KronScreen
import com.miniappfactory.krondrive.ui.common.PrimaryButton
import com.miniappfactory.krondrive.ui.common.SecondaryButton
import com.miniappfactory.krondrive.ui.theme.KronColors

/** Satirlarin dokunma hedefi minimumu. */
private val ROW_MIN_HEIGHT = 56.dp

/**
 * Ayarlar: ses, titresim, dil ve salt okunur bilgi blogu.
 *
 * Dil degisikligi Activity'yi yeniden yaratmaz — metinler [AppLanguage.pick] ile
 * secildigi icin recomposition yeterlidir.
 */
@Composable
fun SettingsScreen(
    progress: PlayerProgress,
    adsConsentResolved: Boolean,
    onSoundEnabled: (Boolean) -> Unit,
    onVibrationEnabled: (Boolean) -> Unit,
    onLanguage: (AppLanguage) -> Unit,
    onBack: () -> Unit
) {
    val language = progress.language

    KronScreen(
        title = language.pick(tr = "AYARLAR", en = "SETTINGS"),
        onBack = onBack,
        showBanner = adsConsentResolved
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ToggleCard(
                title = language.pick(tr = "SES", en = "SOUND"),
                subtitle = if (progress.soundEnabled) {
                    language.pick(tr = "Efektler açık", en = "Effects on")
                } else {
                    language.pick(tr = "Efektler kapalı", en = "Effects off")
                },
                checked = progress.soundEnabled,
                onCheckedChange = onSoundEnabled
            )

            // Titresim anahtari (2026-08-17). Bu anahtardan once serit
            // degistirme, korna ve carpisma titresimleri kosulsuzdu ve
            // oyuncunun kapatma yolu yoktu (docs/REVIEW_UX.md §6 notu).
            // Varsayilan ACIK — bkz. PlayerProgress.vibrationEnabled.
            ToggleCard(
                title = language.pick(tr = "TİTREŞİM", en = "VIBRATION"),
                // Alt satir SES kartiyla ayni kalipta ve kisa tutuldu:
                // "Şerit değiştirme ve çarpışmada titreşir" 11 sp'de ~220 dp
                // ve 360 dp'lik ekranda anahtardan sonra kalan ~252 dp'lik
                // sutuna ancak siginiyordu — Turkce metin biraz uzasa ya da
                // yazi olcegi buyutulse iki satira dusuyordu.
                subtitle = if (progress.vibrationEnabled) {
                    language.pick(tr = "Titreşim açık", en = "Vibration on")
                } else {
                    language.pick(tr = "Titreşim kapalı", en = "Vibration off")
                },
                checked = progress.vibrationEnabled,
                onCheckedChange = onVibrationEnabled
            )

            LanguageCard(current = language, onLanguage = onLanguage)

            PrivacyCard(language = language)

            AboutCard(language = language)
        }
    }
}

/**
 * Reklam gizliligi. Buton YALNIZCA UMP "gerekli" dediginde (AEA/Birlesik
 * Krallik gibi bolgelerde) gorunur; baska yerde onay formu zaten hic
 * gosterilmedigi icin bos bir dugme koymanin anlami yok.
 *
 * Google'in EU User Consent Policy'si onayi degistirmek icin "gorunur ve
 * tiklanabilir" bir giris noktasi sart kosuyor; bu giris noktasi hic yoktu
 * (uyum denetimi, 2026-08-14).
 */
@Composable
private fun PrivacyCard(language: AppLanguage) {
    val context = LocalContext.current
    val activity = context as? Activity ?: return
    // Form durumu Activity yeniden olusunca degisebilir; her bestede sorulur.
    var required by remember { mutableStateOf(ConsentManager.isPrivacyOptionsRequired(context)) }
    if (!required) return

    KronCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = language.pick(tr = "REKLAM GİZLİLİĞİ", en = "AD PRIVACY"),
                color = KronColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = language.pick(
                    tr = "Kişiselleştirilmiş reklam tercihini buradan değiştirebilirsin.",
                    en = "You can change your personalised ads choice here."
                ),
                color = KronColors.TextSecondary,
                fontSize = 12.sp
            )
            SecondaryButton(
                text = language.pick(tr = "GİZLİLİK SEÇENEKLERİ", en = "PRIVACY OPTIONS"),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = ROW_MIN_HEIGHT),
                onClick = {
                    ConsentManager.showPrivacyOptions(activity) {
                        required = ConsentManager.isPrivacyOptionsRequired(context)
                    }
                }
            )
        }
    }
}

/**
 * Baslik + durum satiri + anahtar. Eskiden yalnizca ses icin vardi
 * (`SoundCard`); titresim anahtari eklenirken ayni karti ikinci kez yazmak
 * yerine metinler disari alindi. Duzen, olculer ve anahtar renkleri
 * DEGISMEDI — ses karti bu degisiklikten once nasil goruniyorsa oyle
 * gorunmeye devam eder.
 */
@Composable
private fun ToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    KronCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ROW_MIN_HEIGHT),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title,
                    color = KronColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = subtitle,
                    color = KronColors.TextMuted,
                    fontSize = 11.sp
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = KronColors.Background,
                    checkedTrackColor = KronColors.Accent,
                    checkedBorderColor = KronColors.Accent,
                    uncheckedThumbColor = KronColors.TextMuted,
                    uncheckedTrackColor = KronColors.Locked,
                    uncheckedBorderColor = KronColors.SurfaceBorder
                )
            )
        }
    }
}

@Composable
private fun LanguageCard(current: AppLanguage, onLanguage: (AppLanguage) -> Unit) {
    KronCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = current.pick(tr = "DİL", en = "LANGUAGE"),
                color = KronColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppLanguage.entries.forEach { entry ->
                    val chipModifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)

                    // Secili dil dolu sari, digeri cerceveli — palet ayni kalsin diye
                    // yeni bir bilesen yerine mevcut butonlar kullanildi.
                    if (entry == current) {
                        PrimaryButton(
                            text = entry.displayName,
                            modifier = chipModifier,
                            onClick = { onLanguage(entry) }
                        )
                    } else {
                        SecondaryButton(
                            text = entry.displayName,
                            modifier = chipModifier,
                            onClick = { onLanguage(entry) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutCard(language: AppLanguage) {
    KronCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Kron Drive",
                color = KronColors.Accent,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                // ELLE YAZILMIYOR (2026-08-16): dize "1.0.0"da kalmisti, oysa
                // uygulama 1.0.9. Artik BuildConfig'ten okunuyor, yani bir
                // daha eskiyemez.
                text = language.pick(
                    tr = "Sürüm ${BuildConfig.VERSION_NAME}",
                    en = "Version ${BuildConfig.VERSION_NAME}"
                ),
                color = KronColors.TextSecondary,
                fontSize = 13.sp
            )
            Text(
                text = language.pick(
                    tr = "İlerlemen yalnızca bu cihazda saklanır.",
                    en = "Your progress is stored only on this device."
                ),
                color = KronColors.TextMuted,
                fontSize = 12.sp
            )
            Text(
                text = language.pick(
                    tr = "Hesap açmana gerek yok.",
                    en = "No account needed."
                ),
                color = KronColors.TextMuted,
                fontSize = 12.sp
            )
        }
    }
}
