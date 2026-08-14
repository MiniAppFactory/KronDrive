package com.miniappfactory.krondrive.ui.levels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miniappfactory.krondrive.data.AppLanguage
import com.miniappfactory.krondrive.data.BoosterType
import com.miniappfactory.krondrive.data.PlayerProgress
import com.miniappfactory.krondrive.game.LevelCatalog
import com.miniappfactory.krondrive.game.LevelDef
import com.miniappfactory.krondrive.game.LevelGoal
import com.miniappfactory.krondrive.ui.common.KronScreen
import com.miniappfactory.krondrive.ui.common.PrimaryButton
import com.miniappfactory.krondrive.ui.common.StarRow
import com.miniappfactory.krondrive.ui.common.dimmed
import com.miniappfactory.krondrive.ui.theme.KronColors

/**
 * Kariyer haritasi: bolum izgarasi + secilen bolumun detay kartı.
 *
 * Booster secimi bilerek BURADA yapiliyor (garajda degil): oyuncu hedefi
 * gorduktan sonra hangi guclendiriciyi harcayacagina karar verebilsin.
 */
@Composable
fun LevelMapScreen(
    progress: PlayerProgress,
    selectedBoosters: Set<BoosterType>,
    adsConsentResolved: Boolean,
    onToggleBooster: (BoosterType) -> Unit,
    onPlayLevel: (Int) -> Unit,
    onBack: () -> Unit
) {
    val language = progress.language
    var selected by remember { mutableStateOf<LevelDef?>(null) }

    KronScreen(
        title = language.pick(tr = "KARİYER", en = "CAREER"),
        onBack = onBack,
        showBanner = adsConsentResolved
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(LevelCatalog.levels, key = { it.id }) { level ->
                LevelTile(
                    level = level,
                    stars = progress.starsOf(level.id),
                    locked = level.id > progress.highestUnlockedLevel,
                    language = language,
                    onClick = { selected = level }
                )
            }
        }
    }

    val detail = selected
    if (detail != null) {
        LevelDetailDialog(
            level = detail,
            earnedStars = progress.starsOf(detail.id),
            progress = progress,
            selectedBoosters = selectedBoosters,
            onToggleBooster = onToggleBooster,
            onDismiss = { selected = null },
            onPlay = {
                selected = null
                onPlayLevel(detail.id)
            }
        )
    }
}

@Composable
private fun LevelTile(
    level: LevelDef,
    stars: Int,
    locked: Boolean,
    language: AppLanguage,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            // dimmed once gelmeli: alpha katmani zemini de kapsasin.
            .dimmed(locked)
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .background(
                if (locked) KronColors.Locked else KronColors.Surface,
                RoundedCornerShape(16.dp)
            )
            .border(
                1.dp,
                if (locked) KronColors.SurfaceBorder else KronColors.Accent.copy(alpha = 0.35f),
                RoundedCornerShape(16.dp)
            )
            .clickable(enabled = !locked, onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "${level.id}",
            color = if (locked) KronColors.TextMuted else KronColors.TextPrimary,
            fontSize = 22.sp,
            textAlign = TextAlign.Center
        )
        if (locked) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = language.pick(tr = "Kilitli", en = "Locked"),
                tint = KronColors.TextMuted,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(14.dp)
            )
        } else {
            StarRow(
                earned = stars,
                starSize = 12,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/**
 * Bolum detayi. AlertDialog secildi cunku geri tusu/disariya dokunma ile
 * kapanmayi kendisi hallediyor — ayri bir "kapat" durumu tutmaya gerek kalmiyor.
 */
@Composable
private fun LevelDetailDialog(
    level: LevelDef,
    earnedStars: Int,
    progress: PlayerProgress,
    selectedBoosters: Set<BoosterType>,
    onToggleBooster: (BoosterType) -> Unit,
    onDismiss: () -> Unit,
    onPlay: () -> Unit
) {
    val language = progress.language

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(22.dp),
        containerColor = KronColors.Surface,
        titleContentColor = KronColors.Accent,
        textContentColor = KronColors.TextSecondary,
        title = {
            Text(
                text = language.pick(tr = "Bölüm ${level.id}", en = "Level ${level.id}"),
                color = KronColors.Accent,
                fontSize = 22.sp
            )
        },
        text = {
            Column(
                // Uzun hedef listesi + booster izgarasi 360x640'ta tasabilir.
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = goalText(level.goal, language),
                    color = KronColors.TextPrimary,
                    fontSize = 14.sp
                )

                Text(
                    text = language.pick(tr = "YILDIZ HEDEFLERİ", en = "STAR OBJECTIVES"),
                    color = KronColors.TextMuted,
                    fontSize = 11.sp,
                    letterSpacing = 1.1.sp
                )

                level.stars.forEachIndexed { index, objective ->
                    val earned = index < earnedStars
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            if (earned) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = null,
                            tint = if (earned) KronColors.Accent else KronColors.TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = objective.label(language),
                            color = if (earned) KronColors.TextPrimary else KronColors.TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }

                Text(
                    text = language.pick(tr = "GÜÇLENDİRİCİLER", en = "BOOSTERS"),
                    color = KronColors.TextMuted,
                    fontSize = 11.sp,
                    letterSpacing = 1.1.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )

                // 4 booster tek satira sigmaz (uzun isimler); 2x2 duzen kullaniliyor.
                BoosterType.entries.chunked(2).forEach { rowTypes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowTypes.forEach { type ->
                            BoosterChip(
                                type = type,
                                owned = progress.boosterCount(type),
                                selected = type in selectedBoosters,
                                language = language,
                                modifier = Modifier.weight(1f),
                                onClick = { onToggleBooster(type) }
                            )
                        }
                        // Tek elemanli son satirda hizalama bozulmasin.
                        if (rowTypes.size == 1) Column(modifier = Modifier.weight(1f)) {}
                    }
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = language.pick(tr = "BAŞLA", en = "START"),
                modifier = Modifier.fillMaxWidth(),
                onClick = onPlay
            )
        }
    )
}

@Composable
private fun BoosterChip(
    type: BoosterType,
    owned: Int,
    selected: Boolean,
    language: AppLanguage,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val available = owned > 0
    Column(
        modifier = modifier
            .dimmed(!available)
            .heightIn(min = 48.dp)
            .background(
                if (selected) KronColors.Blue.copy(alpha = 0.18f) else KronColors.SurfaceDeep,
                RoundedCornerShape(14.dp)
            )
            .border(
                1.dp,
                if (selected) KronColors.BlueBright else KronColors.SurfaceBorder,
                RoundedCornerShape(14.dp)
            )
            .clickable(enabled = available, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = type.title(language),
            color = if (selected) KronColors.BlueBright else KronColors.TextPrimary,
            fontSize = 12.sp,
            maxLines = 2
        )
        Text(
            text = if (available) {
                language.pick(tr = "Elinde: $owned", en = "Owned: $owned")
            } else {
                language.pick(tr = "Yok", en = "None")
            },
            color = KronColors.TextMuted,
            fontSize = 10.sp
        )
    }
}

/** Bolumun bitis kosulu — motor bunu [LevelGoal] olarak biliyor, oyuncu metin olarak. */
private fun goalText(goal: LevelGoal, language: AppLanguage): String = when (goal) {
    is LevelGoal.SurviveTime -> language.pick(
        tr = "${goal.seconds} saniye hayatta kal",
        en = "Survive ${goal.seconds} seconds"
    )

    is LevelGoal.ReachDistance -> language.pick(
        tr = "${goal.meters} m git (${goal.timeLimitSec} sn limit)",
        en = "Reach ${goal.meters} m (${goal.timeLimitSec}s limit)"
    )
}
