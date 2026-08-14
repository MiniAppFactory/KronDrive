package com.miniappfactory.krondrive.ui.missions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miniappfactory.krondrive.data.AppLanguage
import com.miniappfactory.krondrive.data.DailyChallengeState
import com.miniappfactory.krondrive.data.PlayerProgress
import com.miniappfactory.krondrive.data.WeeklyMissionDef
import com.miniappfactory.krondrive.data.WeeklyMissionGenerator
import com.miniappfactory.krondrive.data.WeeklyMissionProgress
import com.miniappfactory.krondrive.ui.common.KronCard
import com.miniappfactory.krondrive.ui.common.KronProgressBar
import com.miniappfactory.krondrive.ui.common.KronScreen
import com.miniappfactory.krondrive.ui.common.PrimaryButton
import com.miniappfactory.krondrive.ui.common.dimmed
import com.miniappfactory.krondrive.ui.theme.KronColors

/** Kademe rozetleri hem dokunulabilir hem hizali olsun diye tek bir yukseklikte. */
private val TIER_CHIP_HEIGHT = 48.dp

/**
 * Gorevler: ustte gunun gorevi, ortada haftalik gorev listesi, altta haftalik sandik.
 *
 * Odul talep butonlari SADECE gercekten talep edilebilir durumda tiklanabilir;
 * repository ayrica dogrular ama arayuz hic yanlis vaatte bulunmaz.
 */
@Composable
fun MissionsScreen(
    progress: PlayerProgress,
    missions: WeeklyMissionProgress,
    daily: DailyChallengeState,
    adsConsentResolved: Boolean,
    onClaimTier: (missionId: String, tierIndex: Int) -> Unit,
    onClaimChest: () -> Unit,
    onPlayDaily: () -> Unit,
    onBack: () -> Unit
) {
    val language = progress.language

    KronScreen(
        title = language.pick(tr = "GÖREVLER", en = "MISSIONS"),
        onBack = onBack,
        showBanner = adsConsentResolved
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                DailyCard(daily = daily, language = language, onPlayDaily = onPlayDaily)
            }

            item {
                SectionTitle(text = language.pick(tr = "HAFTALIK GÖREVLER", en = "WEEKLY MISSIONS"))
            }

            items(items = missions.missions, key = { it.id }) { mission ->
                MissionCard(
                    mission = mission,
                    missions = missions,
                    language = language,
                    onClaimTier = onClaimTier
                )
            }

            item {
                ChestCard(missions = missions, language = language, onClaimChest = onClaimChest)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = KronColors.TextMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
    )
}

@Composable
private fun DailyCard(
    daily: DailyChallengeState,
    language: AppLanguage,
    onPlayDaily: () -> Unit
) {
    KronCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = KronColors.Accent.copy(alpha = 0.35f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = language.pick(tr = "GÜNÜN GÖREVİ", en = "DAILY CHALLENGE"),
                color = KronColors.Accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )
            // Baslikta PESINDE OLUNAN kademe yazar: ilk kademe alinmissa
            // oyuncuya artik ikincinin hedefi gosterilir.
            Text(
                text = daily.challenge.title(
                    language,
                    daily.tiersGranted.coerceAtMost(daily.challenge.tiers.lastIndex)
                ),
                color = KronColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )

            // Uc kademe: alinmis (tik), siradaki (vurgulu), kilitli (soluk).
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                daily.challenge.tiers.forEachIndexed { index, tier ->
                    DailyTierChip(
                        modifier = Modifier.weight(1f),
                        target = tier.objective.targetValue,
                        rewardCoins = tier.rewardCoins,
                        granted = index < daily.tiersGranted,
                        next = index == daily.tiersGranted
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (daily.completed) {
                        "+${daily.challenge.totalRewardCoins}"
                    } else {
                        "+${daily.remainingCoins}"
                    },
                    color = KronColors.Coin,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )

                if (daily.completed) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = KronColors.Blue,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = language.pick(
                                tr = "Bugün tamamlandı",
                                en = "Completed today"
                            ),
                            color = KronColors.Blue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                } else {
                    PrimaryButton(
                        text = language.pick(tr = "OYNA", en = "PLAY"),
                        modifier = Modifier
                            .width(128.dp)
                            .heightIn(min = 48.dp),
                        onClick = onPlayDaily
                    )
                }
            }
        }
    }
}

/**
 * Gunluk gorevin tek kademesi. Haftalik [TierChip]'ten farki: burada odul
 * TIKLANARAK alinmaz — kademe kosu icinde tutturuldugu anda otomatik odenir,
 * bu yuzden rozet sadece durum gosterir (alindi / siradaki / kilitli).
 */
@Composable
private fun DailyTierChip(
    modifier: Modifier = Modifier,
    target: Int?,
    rewardCoins: Int,
    granted: Boolean,
    next: Boolean
) {
    val shape = RoundedCornerShape(14.dp)
    val background = when {
        granted -> KronColors.SurfaceDeep
        next -> KronColors.Accent
        else -> KronColors.Locked
    }

    Box(
        modifier = modifier
            .height(TIER_CHIP_HEIGHT)
            .background(background, shape)
            .border(
                1.dp,
                if (next) KronColors.AccentBright else KronColors.SurfaceBorder,
                shape
            )
            .dimmed(!granted && !next),
        contentAlignment = Alignment.Center
    ) {
        if (granted) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = KronColors.Blue,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "+$rewardCoins",
                    color = if (next) KronColors.Background else KronColors.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                target?.let {
                    Text(
                        text = "$it",
                        color = if (next) KronColors.Background else KronColors.TextMuted,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun MissionCard(
    mission: WeeklyMissionDef,
    missions: WeeklyMissionProgress,
    language: AppLanguage,
    onClaimTier: (missionId: String, tierIndex: Int) -> Unit
) {
    val current = missions.progressOf(mission)

    // Aktif kademe = henuz ulasilmamis ilk hedef; hepsi tamamsa son kademede kaliriz
    // (bar dolu gorunsun, baslikta da nihai hedef yazsin).
    val activeIndex = mission.tiers
        .indexOfFirst { current < it.target }
        .takeIf { it >= 0 } ?: mission.tiers.lastIndex
    val activeTier = mission.tiers[activeIndex]

    KronCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = mission.title(language, activeTier.target),
                color = KronColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )

            KronProgressBar(
                progress = current.toFloat() / activeTier.target.toFloat(),
                color = KronColors.Blue
            )

            Text(
                text = "${current.coerceAtMost(activeTier.target)} / ${activeTier.target}",
                color = KronColors.TextSecondary,
                fontSize = 12.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                mission.tiers.forEachIndexed { index, tier ->
                    val claimed = missions.isClaimed(mission, index)
                    val claimable = current >= tier.target && !claimed

                    TierChip(
                        modifier = Modifier.weight(1f),
                        target = tier.target,
                        rewardCoins = tier.rewardCoins,
                        claimed = claimed,
                        claimable = claimable,
                        onClaim = { onClaimTier(mission.id, index) }
                    )
                }
            }
        }
    }
}

/**
 * Kademe rozeti uc durumda olabilir: alinabilir (tiklanabilir odul), alinmis
 * (tik isareti), kilitli (soluk hedef sayisi).
 */
@Composable
private fun TierChip(
    modifier: Modifier = Modifier,
    target: Int,
    rewardCoins: Int,
    claimed: Boolean,
    claimable: Boolean,
    onClaim: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val background = when {
        claimable -> KronColors.Accent
        claimed -> KronColors.SurfaceDeep
        else -> KronColors.Locked
    }

    Box(
        modifier = modifier
            .height(TIER_CHIP_HEIGHT)
            .background(background, shape)
            .border(
                1.dp,
                if (claimable) KronColors.AccentBright else KronColors.SurfaceBorder,
                shape
            )
            .clickable(enabled = claimable, onClick = onClaim)
            .dimmed(!claimable && !claimed),
        contentAlignment = Alignment.Center
    ) {
        when {
            claimable -> Text(
                text = "+$rewardCoins",
                color = KronColors.Background,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )

            claimed -> Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = KronColors.Blue,
                modifier = Modifier.size(20.dp)
            )

            else -> Text(
                text = "$target",
                color = KronColors.TextMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ChestCard(
    missions: WeeklyMissionProgress,
    language: AppLanguage,
    onClaimChest: () -> Unit
) {
    val chestClaimed = WeeklyMissionGenerator.CHEST_CLAIM_KEY in missions.claimed
    val chestReady = missions.allTiersComplete() && !chestClaimed
    val boosterName = WeeklyMissionGenerator.WEEKLY_CHEST_BOOSTER.title(language)
    val coins = WeeklyMissionGenerator.WEEKLY_CHEST_COINS

    KronCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (chestReady) KronColors.Accent.copy(alpha = 0.5f) else KronColors.SurfaceBorder
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = language.pick(tr = "HAFTALIK SANDIK", en = "WEEKLY CHEST"),
                color = KronColors.Accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = language.pick(
                    tr = "$coins coin + 1 $boosterName",
                    en = "$coins coins + 1 $boosterName"
                ),
                color = KronColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = language.pick(
                    tr = "Tüm görevlerin tüm kademelerini bitir.",
                    en = "Finish every tier of every mission."
                ),
                color = KronColors.TextMuted,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            PrimaryButton(
                text = if (chestClaimed) {
                    language.pick(tr = "ALINDI", en = "CLAIMED")
                } else {
                    language.pick(tr = "SANDIĞI AÇ", en = "OPEN CHEST")
                },
                enabled = chestReady,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                onClick = onClaimChest
            )
        }
    }
}
