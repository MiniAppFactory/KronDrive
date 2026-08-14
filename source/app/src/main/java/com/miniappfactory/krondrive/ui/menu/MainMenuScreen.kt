package com.miniappfactory.krondrive.ui.menu

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miniappfactory.krondrive.R
import com.miniappfactory.krondrive.data.DailyChallengeState
import com.miniappfactory.krondrive.data.PlayerProgress
import com.miniappfactory.krondrive.game.LevelCatalog
import com.miniappfactory.krondrive.ui.common.CarPreview
import com.miniappfactory.krondrive.ui.common.KronScreen
import com.miniappfactory.krondrive.ui.common.PrimaryButton
import com.miniappfactory.krondrive.ui.common.SecondaryButton
import com.miniappfactory.krondrive.ui.common.StatChip
import com.miniappfactory.krondrive.ui.common.dimmed
import com.miniappfactory.krondrive.ui.theme.KronColors

/**
 * Ana menu: oyuncunun her acilista gordugu ilk ekran.
 *
 * Menu bilerek "durum tasimaz" — tum veriler parametre olarak gelir, boylece
 * ekran hem ViewModel'den hem de Preview/test icinden ayni sekilde beslenebilir.
 */
@Composable
fun MainMenuScreen(
    progress: PlayerProgress,
    daily: DailyChallengeState,
    missionsHaveClaimable: Boolean,
    adsConsentResolved: Boolean,
    onCareer: () -> Unit,
    onEndless: () -> Unit,
    onDaily: () -> Unit,
    onGarage: () -> Unit,
    onMissions: () -> Unit,
    onSettings: () -> Unit
) {
    val language = progress.language

    // Son bolum de bitince highestUnlockedLevel katalogun disina tasabilir;
    // butonun "Bolum 31/30" gibi anlamsiz bir sayi gostermemesi icin kirpiyoruz.
    val careerLevel = progress.highestUnlockedLevel.coerceIn(1, LevelCatalog.count)

    KronScreen(showBanner = adsConsentResolved) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // 360x640 cihazlarda 6 buton + logo tek ekrana sigmaz.
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Prototipteki menu karti: logonun solunda yaris otomobili, saginda
            // damali bayrak (HTML'de kose kose iki 42 px gorsel). Prototipteki
            // PNG'ler stok gorseldi ve arabanin uzerinde "Designed by pngtree"
            // filigrani vardi — lisans riski nedeniyle tasinmadi, ikisi de
            // Canvas ile yeniden cizildi (bkz. PROVENANCE.md).
            Box(modifier = Modifier.fillMaxWidth()) {
                // Soldaki araba, oyuncunun GARAJDA SECTIGI arac: menude kendi
                // aracini gormek ozellestirmenin karsiligi (bkz. CarCatalog).
                CarPreview(
                    style = progress.carStyle,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(width = 46.dp, height = 62.dp)
                )
                Image(
                    painter = painterResource(R.drawable.kron_logo),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(92.dp)
                )
                CheckeredFlagBadge(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(width = 46.dp, height = 38.dp)
                )
            }

            Text(
                text = "KRON DRIVE",
                color = KronColors.Accent,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "DRIVE · DODGE · SURVIVE",
                color = KronColors.TextMuted,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatChip(
                    label = language.pick(tr = "COIN", en = "COINS"),
                    value = "${progress.coins}",
                    tint = KronColors.Coin
                )
                StatChip(
                    label = language.pick(tr = "YILDIZ", en = "STARS"),
                    value = "${progress.totalStars}/${LevelCatalog.count * 3}",
                    tint = KronColors.Accent
                )
                StatChip(
                    label = language.pick(tr = "ARAÇ", en = "CAR"),
                    value = "Lv ${progress.carLevel}",
                    tint = KronColors.Blue
                )
            }

            PrimaryButton(
                text = language.pick(tr = "KARİYER", en = "CAREER"),
                subtitle = language.pick(tr = "Bölüm $careerLevel", en = "Level $careerLevel"),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                onClick = onCareer
            )

            MenuButton(
                text = language.pick(tr = "SONSUZ", en = "ENDLESS"),
                subtitle = if (progress.endlessBestSeconds > 0) {
                    language.pick(
                        tr = "REKOR ${formatDuration(progress.endlessBestSeconds)}",
                        en = "BEST ${formatDuration(progress.endlessBestSeconds)}"
                    )
                } else {
                    language.pick(tr = "İlk rekorunu kur", en = "Set your first record")
                },
                modifier = Modifier.fillMaxWidth(),
                onClick = onEndless
            )

            // Gunluk gorev tamamlaninca buton KAPATILMAZ: oyuncu gorevi tekrar
            // oynayabilsin diye aktif kalir, sadece "bugun bitti" sinyali verilir.
            MenuButton(
                text = language.pick(tr = "GÜNLÜK GÖREV", en = "DAILY CHALLENGE") +
                    if (daily.completed) {
                        "  ✓"
                    } else {
                        "  ${daily.tiersGranted}/${daily.challenge.tiers.size}"
                    },
                // Alt satirda siradaki kademenin hedefi ve odulu yazar.
                subtitle = daily.nextTier?.let { tier ->
                    "${tier.objective.label(language)}  ·  +${tier.rewardCoins}"
                } ?: daily.challenge.title(language),
                modifier = Modifier
                    .dimmed(daily.completed)
                    .fillMaxWidth(),
                onClick = onDaily
            )

            SecondaryButton(
                text = language.pick(tr = "GARAJ", en = "GARAGE"),
                modifier = Modifier.fillMaxWidth(),
                onClick = onGarage
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                SecondaryButton(
                    text = language.pick(tr = "GÖREVLER", en = "MISSIONS"),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onMissions
                )
                if (missionsHaveClaimable) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .size(10.dp)
                            .background(KronColors.Danger, CircleShape)
                    )
                }
            }

            SecondaryButton(
                text = language.pick(tr = "AYARLAR", en = "SETTINGS"),
                modifier = Modifier.fillMaxWidth(),
                onClick = onSettings
            )
        }
    }
}

/**
 * [SecondaryButton]'un alt satirli surumu. Rekor/gorev bilgisini butonun ICINE
 * koymak, menuye ayri bir bilgi satiri eklemekten daha az dikey yer kapliyor —
 * kucuk ekranlarda bu fark tum menunun sigip sigmadigini belirliyor.
 */
@Composable
private fun MenuButton(
    text: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            // Sabit yukseklik degil: buyuk yazi tipi olceginde metin kirpilmasin.
            .heightIn(min = 56.dp)
            .background(Color.Transparent, RoundedCornerShape(18.dp))
            .border(2.dp, KronColors.Accent.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            color = KronColors.Accent,
            fontSize = 16.sp,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                color = KronColors.TextMuted,
                fontSize = 11.sp,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Menunun sagindaki damali bayrak rozeti. Dalgalanma hissi icin her sutun
 * dikeyde biraz kaydirilir — duz bir dama tahtasi bayrak gibi okunmuyordu.
 */
@Composable
private fun CheckeredFlagBadge(modifier: Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val columns = 5
        val rows = 3

        // Direk
        drawRect(KronColors.TextSecondary, Offset(w * 0.05f, 0f), Size(w * 0.06f, h))

        val flagLeft = w * 0.11f
        val cellW = (w - flagLeft) / columns
        val cellH = h * 0.52f / rows

        for (col in 0 until columns) {
            // Sutun basina kucuk bir dikey kayma: bayragin dalgasi.
            val wave = h * 0.05f * kotlin.math.sin(col * 1.1f)
            for (row in 0 until rows) {
                val dark = (col + row) % 2 == 0
                drawRect(
                    color = if (dark) Color(0xFF0B1626) else KronColors.RoadLine,
                    topLeft = Offset(flagLeft + col * cellW, h * 0.08f + row * cellH + wave),
                    size = Size(cellW, cellH)
                )
            }
        }
    }
}

/** Sonsuz mod rekoru "01:47" bicimindedir (dakika bilgisi olmadan okunmasi zor). */
private fun formatDuration(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    return "%02d:%02d".format(safe / 60, safe % 60)
}
