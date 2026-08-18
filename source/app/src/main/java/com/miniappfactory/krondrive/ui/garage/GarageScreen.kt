package com.miniappfactory.krondrive.ui.garage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miniappfactory.krondrive.data.AppLanguage
import com.miniappfactory.krondrive.data.BoosterType
import com.miniappfactory.krondrive.data.PlayerProgress
import com.miniappfactory.krondrive.game.CarCatalog
import com.miniappfactory.krondrive.game.CarColorDef
import com.miniappfactory.krondrive.game.CarItem
import com.miniappfactory.krondrive.game.CarShapeDef
import com.miniappfactory.krondrive.game.CarStyle
import com.miniappfactory.krondrive.game.CarUnlockState
import com.miniappfactory.krondrive.game.UpgradeCatalog
import com.miniappfactory.krondrive.game.GameConfig
import com.miniappfactory.krondrive.game.UpgradeType
import com.miniappfactory.krondrive.ui.common.CarPreview
import com.miniappfactory.krondrive.ui.common.KronCard
import com.miniappfactory.krondrive.ui.common.KronProgressBar
import com.miniappfactory.krondrive.ui.common.KronScreen
import com.miniappfactory.krondrive.ui.common.PrimaryButton
import com.miniappfactory.krondrive.ui.common.StatChip
import com.miniappfactory.krondrive.ui.theme.KronColors
import kotlin.math.roundToInt

/** Satin alma butonlarinin ortak genisligi — 360dp ekranda metin sutununa yer kalsin diye sabit. */
private val BUY_BUTTON_WIDTH = 96.dp

/** Dokunma hedefi minimumu (mobil kalite kurali). */
private val MIN_TOUCH = 48.dp

/**
 * Garaj: arac seviyesi, dort yukseltme dali ve guclendirici dukkani.
 *
 * Ekran yalnizca gosterir ve niyet bildirir; coin dusme/seviye artirma islemi
 * repository katmanindadir. Yine de buton, oyuncu parayi karsilamiyorsa hic
 * tiklanabilir olmaz (yanlis geri bildirim vermemek icin).
 */
@Composable
fun GarageScreen(
    progress: PlayerProgress,
    adsConsentResolved: Boolean,
    adInFlight: Boolean,
    /** Odullu reklam yuklenemediyse "bedava coin" kartinda aciklama cikar. */
    adFailed: Boolean,
    onWatchAdForCoins: () -> Unit,
    onUpgrade: (UpgradeType) -> Unit,
    onBuyBooster: (BoosterType) -> Unit,
    onSelectCarShape: (String) -> Unit,
    onBuyCarShape: (String) -> Unit,
    onSelectCarColor: (String) -> Unit,
    onBuyCarColor: (String) -> Unit,
    onBack: () -> Unit
) {
    val language = progress.language

    KronScreen(
        title = language.pick(tr = "GARAJ", en = "GARAGE"),
        onBack = onBack,
        showBanner = adsConsentResolved,
        headerContent = {
            StatChip(
                label = language.pick(tr = "COIN", en = "COINS"),
                value = "${progress.coins}",
                tint = KronColors.Coin
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CarHeaderCard(progress = progress, language = language)

            SectionTitle(text = language.pick(tr = "ARAÇ", en = "CAR"))

            CarCustomizeCard(
                progress = progress,
                language = language,
                onSelectShape = onSelectCarShape,
                onBuyShape = onBuyCarShape,
                onSelectColor = onSelectCarColor,
                onBuyColor = onBuyCarColor
            )

            FreeCoinsCard(
                remainingToday = progress.rewardedCoinsRemainingToday,
                adInFlight = adInFlight,
                adFailed = adFailed,
                language = language,
                onWatchAd = onWatchAdForCoins
            )

            SectionTitle(text = language.pick(tr = "YÜKSELTMELER", en = "UPGRADES"))

            UpgradeType.entries.forEach { type ->
                UpgradeRow(
                    type = type,
                    level = progress.upgrades.levelOf(type),
                    coins = progress.coins,
                    // Gosterilen deger SECILI GOVDEYE bagli: garaj eskiden
                    // her araç icin ayni sayiyi yaziyordu (2026-08-16).
                    car = progress.carStyle.shape,
                    language = language,
                    onUpgrade = { onUpgrade(type) }
                )
            }

            SectionTitle(text = language.pick(tr = "GÜÇLENDİRİCİLER", en = "BOOSTERS"))

            BoosterType.entries.forEach { type ->
                BoosterRow(
                    type = type,
                    owned = progress.boosterCount(type),
                    coins = progress.coins,
                    language = language,
                    onBuy = { onBuyBooster(type) }
                )
            }
        }
    }
}

/**
 * Odullu reklamla coin kazanma. Gunluk sinir var (bkz.
 * [GameConfig.REWARDED_COIN_DAILY_LIMIT]) — sinirsiz olsaydi yukseltme
 * ekonomisi anlamsizlasirdi.
 */
@Composable
private fun FreeCoinsCard(
    remainingToday: Int,
    adInFlight: Boolean,
    adFailed: Boolean,
    language: AppLanguage,
    onWatchAd: () -> Unit
) {
    val available = remainingToday > 0
    KronCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = language.pick(tr = "BEDAVA COIN", en = "FREE COINS"),
                    color = KronColors.Coin,
                    fontSize = 15.sp
                )
                Text(
                    text = language.pick(
                        tr = "Reklam izle, +${GameConfig.REWARDED_COIN_AMOUNT} coin kazan",
                        en = "Watch an ad, earn +${GameConfig.REWARDED_COIN_AMOUNT} coins"
                    ),
                    color = KronColors.TextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = if (available) {
                        language.pick(
                            tr = "Bugün kalan hak: $remainingToday",
                            en = "Remaining today: $remainingToday"
                        )
                    } else {
                        language.pick(
                            tr = "Bugünlük hakkın doldu, yarın tekrar gel",
                            en = "Daily limit reached, come back tomorrow"
                        )
                    },
                    color = KronColors.TextMuted,
                    fontSize = 11.sp
                )
                // Reklam yuklenemezse tek cumlelik aciklama. CUMLE
                // CrashOverlay'dekiyle birebir ayni (docs/REVIEW_UX.md §4):
                // oyuncu ayni hatayi ekrandan ekrana ayni sozlerle okusun.
                //
                // RENK farkli (orada TextMuted, burada Danger) ve bu bilincli:
                // burada mesajin hemen ustunde zaten TextMuted 11 sp bir satir
                // var ("Bugun kalan hak: N"). Ayni gri ile yazsaydik mesaj o
                // satirin devami gibi okunur ve fark edilmezdi — yani sessiz
                // basarisizligi baska bir bicimde surdururduk.
                //
                // Yalnizca hata aninda ciktigi icin kartin normal yuksekligi
                // degismiyor.
                if (adFailed) {
                    Text(
                        text = language.pick(
                            tr = "Reklam yüklenemedi. İnternet bağlantını kontrol et.",
                            en = "Ad could not be loaded. Check your connection."
                        ),
                        color = KronColors.Danger,
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            PrimaryButton(
                text = if (adInFlight) {
                    language.pick(tr = "…", en = "…")
                } else {
                    language.pick(tr = "İZLE", en = "WATCH")
                },
                enabled = available && !adInFlight,
                modifier = Modifier.width(96.dp),
                onClick = onWatchAd
            )
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
private fun CarHeaderCard(progress: PlayerProgress, language: AppLanguage) {
    val percent = (progress.carLevelProgress * 100f).roundToInt()

    KronCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.pick(tr = "ARAÇ SEVİYESİ", en = "CAR LEVEL"),
                    color = KronColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "${progress.carLevel}",
                    color = KronColors.Accent,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
            }

            KronProgressBar(progress = progress.carLevelProgress)

            Text(
                text = language.pick(
                    tr = "Sonraki seviyeye %$percent",
                    en = "$percent% to next level"
                ),
                color = KronColors.TextMuted,
                fontSize = 11.sp
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Arac ozellestirme (2026-08-14, proje sahibi istegi: "surulen araba sekli ve
// rengi customise edilebilmeli")
// ---------------------------------------------------------------------------

/** Sekil kucuk onizlemesinin olculeri — aracin en/boy orani korunur. */
private val SHAPE_CHIP_WIDTH = 68.dp
private val SHAPE_CHIP_PREVIEW_HEIGHT = 76.dp
private val COLOR_SWATCH = 44.dp

/**
 * Govde sekli + boya secimi.
 *
 * Kilitli bir icerige dokunmak SATIN ALMAZ; yalnizca onizlemeye alir ve altta
 * fiyatli bir satin alma butonu cikar. Yanlislikla coin harcanmasin diye.
 * Sahip olunan bir icerige dokunmak ise dogrudan secer ve kaydeder.
 */
@Composable
private fun CarCustomizeCard(
    progress: PlayerProgress,
    language: AppLanguage,
    onSelectShape: (String) -> Unit,
    onBuyShape: (String) -> Unit,
    onSelectColor: (String) -> Unit,
    onBuyColor: (String) -> Unit
) {
    // Onizleme durumu kalici secimden ayri tutulur; kalici secim degisince
    // (ornegin satin alma sonrasi) onizleme de ona hizalanir.
    var previewShapeId by remember(progress.carShapeId) { mutableStateOf(progress.carShapeId) }
    var previewColorId by remember(progress.carColorId) { mutableStateOf(progress.carColorId) }

    val shape = CarCatalog.shape(previewShapeId)
    val color = CarCatalog.color(previewColorId)
    val style = CarStyle(shape, color)

    val shapeState = CarCatalog.stateOf(
        item = shape,
        owned = progress.ownedCarShapes,
        coins = progress.coins,
        carLevel = progress.carLevel
    )
    val colorState = CarCatalog.stateOf(
        item = color,
        owned = progress.ownedCarColors,
        coins = progress.coins,
        carLevel = progress.carLevel
    )

    KronCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(width = 96.dp, height = 118.dp)
                        .background(KronColors.SurfaceDeep, RoundedCornerShape(16.dp))
                        .border(1.dp, KronColors.SurfaceBorder, RoundedCornerShape(16.dp))
                        .padding(6.dp)
                ) {
                    CarPreview(style = style, modifier = Modifier.fillMaxSize())
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = shape.name(language),
                        color = KronColors.TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = shape.description(language),
                        color = KronColors.TextMuted,
                        fontSize = 11.sp
                    )
                    Text(
                        text = language.pick(
                            tr = "Renk: ${color.name(language)}",
                            en = "Colour: ${color.name(language)}"
                        ),
                        color = KronColors.TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = if (shapeState == CarUnlockState.OWNED && colorState == CarUnlockState.OWNED) {
                            language.pick(tr = "Bu araçla yarışıyorsun", en = "You drive this car")
                        } else {
                            language.pick(tr = "Önizleme — henüz kilitli", en = "Preview — still locked")
                        },
                        color = if (shapeState == CarUnlockState.OWNED && colorState == CarUnlockState.OWNED) {
                            KronColors.Blue
                        } else {
                            KronColors.TextMuted
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Ozellikler satin almadan ONCE gorunur: kilitli bir govdeye
            // dokunmak onu onizlemeye alir ve bu blok o govdenin degerlerini
            // gosterir. Sahibinin tespiti (2026-08-15) tam olarak buydu —
            // "neden super araba alsin", cunku ne aldigi hicbir yerde
            // yazmiyordu.
            CarStatPanel(shape = shape, language = language)

            UnlockAction(
                item = shape,
                state = shapeState,
                carLevel = progress.carLevel,
                language = language,
                buyLabel = language.pick(tr = "GÖVDEYİ AL", en = "BUY BODY"),
                onBuy = { onBuyShape(shape.id) }
            )
            // Onizlenen govdenin FABRIKA boyasi icin satin alma satiri
            // GOSTERILMEZ: o boya gövdeyi almakla birlikte zaten aciliyor
            // (bkz. CarCatalog.effectiveOwnedColors). Aksi halde ekran
            // "Kuş SLX · seviye 2 gerekiyor" ile "Petrol · seviye 3 gerekiyor"
            // satirlarini yan yana gosteriyor ve oyuncu boyayi AYRICA almasi
            // gerektigini saniyor — yanlis bilgi.
            if (color.id != shape.defaultColorId) {
                UnlockAction(
                    item = color,
                    state = colorState,
                    carLevel = progress.carLevel,
                    language = language,
                    buyLabel = language.pick(tr = "RENGİ AL", en = "BUY COLOUR"),
                    onBuy = { onBuyColor(color.id) }
                )
            }

            SubSectionTitle(language.pick(tr = "GÖVDE", en = "BODY"))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CarCatalog.shapes.forEach { def ->
                    ShapeChip(
                        shape = def,
                        // Secili cip oyuncunun GERCEK boyasini gosterir, gecici
                        // onizleme boyasini degil: kilitli bir araca bakarken
                        // onizleme onun fabrika boyasina geciyor ve bu, sahibin
                        // kendi aracinin cipini de o renge boyuyordu.
                        color = CarCatalog.color(progress.carColorId),
                        selected = def.id == progress.carShapeId,
                        previewed = def.id == previewShapeId,
                        state = CarCatalog.stateOf(
                            item = def,
                            owned = progress.ownedCarShapes,
                            coins = progress.coins,
                            carLevel = progress.carLevel
                        ),
                        language = language,
                        onClick = {
                            previewShapeId = def.id
                            if (CarCatalog.isOwned(def, progress.ownedCarShapes)) {
                                onSelectShape(def.id)
                            } else {
                                // SAHIP OLUNMAYAN araca bakarken onizleme onun
                                // FABRIKA boyasina gecer (2026-08-16, proje
                                // sahibi: *"burada on izlemede default verdigimiz
                                // renkler gozukurse harika olur"*).
                                //
                                // Eskiden buyuk onizleme oyuncunun KENDI boyasini
                                // kullaniyordu: cipte Kuş SLX petrol yesili
                                // gorunuyor, hemen ustundeki karta bakinca kirmizi
                                // cikiyordu. Ayni arac iki yerde iki renk =
                                // celiski. Sahip olunan aracta oyuncunun secimi
                                // korunur, cunku orada gercekten surdugu renk odur.
                                previewColorId = def.defaultColorId
                            }
                        }
                    )
                }
            }

            SubSectionTitle(language.pick(tr = "RENK", en = "COLOUR"))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CarCatalog.colors.forEach { def ->
                    ColorSwatch(
                        color = def,
                        selected = def.id == progress.carColorId,
                        previewed = def.id == previewColorId,
                        state = CarCatalog.stateOf(
                            item = def,
                            owned = progress.ownedCarColors,
                            coins = progress.coins,
                            carLevel = progress.carLevel
                        ),
                        language = language,
                        onClick = {
                            previewColorId = def.id
                            if (CarCatalog.isOwned(def, progress.ownedCarColors)) onSelectColor(def.id)
                        }
                    )
                }
            }
        }
    }
}

/** Ozellik cubuklarinin etiket ve yuzde sutun genisligi (dort satir hizali dursun). */
private val STAT_LABEL_WIDTH = 46.dp
private val STAT_DELTA_WIDTH = 92.dp   // yuzde degil aralik yaziyor (2026-08-18)

/**
 * Bir govdenin dort surus ozelligi + karakter cumlesi.
 *
 * Cubuklar MUTLAK degil KARSILASTIRMALIdir ([CarCatalog.statFraction]):
 * eksenin en iyisi dolu, en kotusu kisa. Carpanlar 0.90–1.12 arasinda
 * gezdigi icin mutlak gosterim yedi araci da ayni gosterirdi.
 *
 * Gorsel dil yukseltme bolumunden aynen aliniyor — ayni etiketler
 * ([UpgradeCatalog.title]), ayni [KronProgressBar], ayni renk rolleri
 * (Blue = mevcut deger, AccentBright = kazanc). Yeni bir dil icat edilmedi.
 */
@Composable
private fun CarStatPanel(shape: CarShapeDef, language: AppLanguage) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = shape.trait(language),
            color = KronColors.BlueBright,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black
        )

        UpgradeType.entries.forEach { type ->
            val delta = CarCatalog.statDeltaPercent(shape, type)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = UpgradeCatalog.title(type, language),
                    modifier = Modifier.width(STAT_LABEL_WIDTH),
                    color = KronColors.TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
                KronProgressBar(
                    progress = CarCatalog.statFraction(shape, type),
                    modifier = Modifier.weight(1f),
                    color = when {
                        delta > 0 -> KronColors.AccentBright
                        delta < 0 -> KronColors.Danger
                        else -> KronColors.Blue
                    }
                )
                Text(
                    // YUZDE DEGIL SOMUT ARALIK (2026-08-18). Sahibi: *"arac
                    // tanitim garajinda hiz gibi ozellikler var ama rakamlarla
                    // yazmiyor, +/- seklinde"*. Yuzde hem aracin gercek hizini
                    // hem yukseltmelerle nereye gidecegini gizliyordu.
                    // Cubugun RENGI hala farki anlatiyor (yesil/kirmizi).
                    text = UpgradeCatalog.displayRange(type, shape),
                    modifier = Modifier.width(STAT_DELTA_WIDTH),
                    textAlign = TextAlign.End,
                    color = when {
                        delta > 0 -> KronColors.AccentBright
                        delta < 0 -> KronColors.Danger
                        else -> KronColors.TextMuted
                    },
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Text(
            // Referans aracin ADI elle yazilmiyor (2026-08-16): "Sehir"
            // yaziyordu ama referans o gun Beety'ye gecti ve ekran eskidi.
            // Artik katalogdan okunuyor, bir daha yalan soyleyemez.
            text = language.pick(
                tr = "Yükseltmesiz → tam yükseltmeli değer; kariyer tabanına göre",
                en = "Un-upgraded → fully upgraded; based on the career start speed"
            ),
            color = KronColors.TextMuted,
            fontSize = 9.sp
        )
    }
}

/** Onizlenen icerik kilitliyse fiyatli satin alma satiri; degilse hicbir sey. */
@Composable
private fun UnlockAction(
    item: CarItem,
    state: CarUnlockState,
    /** Seviye atlama bedelini hesaplamak icin — bkz. CarCatalog.levelSkipFee. */
    carLevel: Int,
    language: AppLanguage,
    buyLabel: String,
    onBuy: () -> Unit
) {
    if (state == CarUnlockState.OWNED) return

    val skipFee = CarCatalog.levelSkipFee(item, carLevel)
    val total = CarCatalog.totalPrice(item, carLevel)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = when {
                // SEVIYE ARTIK DUVAR DEGIL (2026-08-19): eksik seviye coinle
                // atlanabiliyor. Metin bunu ACIKCA soyler, yoksa oyuncu neden
                // fiyattan fazla odedigini anlamaz.
                skipFee > 0 && state == CarUnlockState.AFFORDABLE -> language.pick(
                    tr = "${item.name(language)} · seviye ${item.requiredCarLevel} gerekiyor — " +
                        "$skipFee coin ile şimdi açabilirsin",
                    en = "${item.name(language)} · needs level ${item.requiredCarLevel} — " +
                        "unlock now for $skipFee extra coins"
                )

                state == CarUnlockState.LEVEL_LOCKED -> language.pick(
                    tr = "${item.name(language)} · seviye ${item.requiredCarLevel} ya da " +
                        "toplam $total coin gerekiyor",
                    en = "${item.name(language)} · needs level ${item.requiredCarLevel} " +
                        "or $total coins in total"
                )

                state == CarUnlockState.TOO_EXPENSIVE -> language.pick(
                    tr = "${item.name(language)} · yeterli coin yok",
                    en = "${item.name(language)} · not enough coins"
                )

                else -> language.pick(
                    tr = "${item.name(language)} · satın alınabilir",
                    en = "${item.name(language)} · available now"
                )
            },
            modifier = Modifier.weight(1f),
            color = if (state == CarUnlockState.AFFORDABLE) KronColors.TextSecondary else KronColors.TextMuted,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.width(10.dp))
        PrimaryButton(
            // Butonda ODENECEK TOPLAM yazar. Fiyati yazip toplami tahsil
            // etmek oyuncuyu kandirmak olurdu.
            text = "$total",
            subtitle = when {
                skipFee > 0 -> language.pick(
                    tr = "${item.priceCoins} + $skipFee seviye",
                    en = "${item.priceCoins} + $skipFee level"
                )
                else -> buyLabel
            },
            enabled = state == CarUnlockState.AFFORDABLE,
            modifier = Modifier
                .width(112.dp)
                .heightIn(min = MIN_TOUCH),
            onClick = onBuy
        )
    }
}

@Composable
private fun SubSectionTitle(text: String) {
    Text(
        text = text,
        color = KronColors.TextMuted,
        fontSize = 10.sp,
        fontWeight = FontWeight.Black
    )
}

@Composable
private fun ShapeChip(
    shape: CarShapeDef,
    color: CarColorDef,
    selected: Boolean,
    previewed: Boolean,
    state: CarUnlockState,
    language: AppLanguage,
    onClick: () -> Unit
) {
    val borderColor = when {
        selected -> KronColors.Accent
        previewed -> KronColors.Blue
        else -> KronColors.SurfaceBorder
    }
    Column(
        modifier = Modifier
            .width(SHAPE_CHIP_WIDTH)
            .background(KronColors.SurfaceDeep, RoundedCornerShape(14.dp))
            .border(if (selected || previewed) 2.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        CarPreview(
            // SECILI arac kendi boyasiyla, digerleri FABRIKA boyasiyla
            // cizilir (2026-08-15). Cipler bir vitrin: oyuncu Kuş SLX'i
            // almadan once onun petrol yesili, Dağ Keçisi'nin beyaz
            // oldugunu gormeli. Hepsini secili boyayla cizmek butun vitrini
            // tek renge boyuyor ve araclarin kimligini siliyordu.
            style = CarStyle(shape, if (selected) color else CarCatalog.color(shape.defaultColorId)),
            modifier = Modifier
                .fillMaxWidth()
                .height(SHAPE_CHIP_PREVIEW_HEIGHT)
        )
        Text(
            text = shape.name(language),
            color = if (state == CarUnlockState.OWNED) KronColors.TextPrimary else KronColors.TextMuted,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Black
        )
        Text(
            text = statusText(shape, state, selected, language),
            color = statusColor(state, selected),
            fontSize = 9.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ColorSwatch(
    color: CarColorDef,
    selected: Boolean,
    previewed: Boolean,
    state: CarUnlockState,
    language: AppLanguage,
    onClick: () -> Unit
) {
    val borderColor = when {
        selected -> KronColors.Accent
        previewed -> KronColors.Blue
        else -> KronColors.SurfaceBorder
    }
    Column(
        modifier = Modifier
            .width(COLOR_SWATCH + 18.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(COLOR_SWATCH)
                .background(Color(color.bodyArgb), CircleShape)
                .border(if (selected || previewed) 3.dp else 1.dp, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Serit rengi de gorunsun: kucuk bir cizgi yeterli.
            Box(
                modifier = Modifier
                    .size(width = 6.dp, height = COLOR_SWATCH - 14.dp)
                    .background(Color(color.accentArgb), RoundedCornerShape(3.dp))
            )
        }
        Text(
            text = color.name(language),
            color = if (state == CarUnlockState.OWNED) KronColors.TextSecondary else KronColors.TextMuted,
            fontSize = 9.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = statusText(color, state, selected, language),
            color = statusColor(state, selected),
            fontSize = 9.sp,
            textAlign = TextAlign.Center
        )
    }
}

private fun statusText(
    item: CarItem,
    state: CarUnlockState,
    selected: Boolean,
    language: AppLanguage
): String = when {
    selected -> language.pick(tr = "SEÇİLİ", en = "IN USE")
    state == CarUnlockState.OWNED -> language.pick(tr = "sahipsin", en = "owned")
    // Seviye kilidi FIYATI GIZLEMEZ. Eskiden yalnizca "Sv 2" yaziyordu ve
    // oyuncu seviyeyi gorunce aracin BEDAVA acilacagini saniyordu (proje
    // sahibi 2026-08-16'da tam bunu sordu: *"garajdan araclari acmak icin
    // level gerekiyor peki bedava mi aliyorlar?"*). Iki sart da ayri ayri
    // gecerli: once seviye, sonra coin.
    state == CarUnlockState.LEVEL_LOCKED -> language.pick(
        tr = "Sv ${item.requiredCarLevel} · ${item.priceCoins}",
        en = "Lv ${item.requiredCarLevel} · ${item.priceCoins}"
    )

    else -> "${item.priceCoins}"
}

private fun statusColor(state: CarUnlockState, selected: Boolean): Color = when {
    selected -> KronColors.Accent
    state == CarUnlockState.OWNED -> KronColors.Blue
    state == CarUnlockState.AFFORDABLE -> KronColors.Coin
    else -> KronColors.TextMuted
}

@Composable
private fun UpgradeRow(
    type: UpgradeType,
    level: Int,
    coins: Int,
    car: CarShapeDef,
    language: AppLanguage,
    onUpgrade: () -> Unit
) {
    // cost == null => dal maksimumda; ok isareti ve fiyat gosterilmez.
    val cost = UpgradeCatalog.cost(level)
    val affordable = cost != null && coins >= cost

    KronCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = UpgradeCatalog.title(type, language),
                    color = KronColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = UpgradeCatalog.description(type, language),
                    color = KronColors.TextMuted,
                    fontSize = 11.sp
                )
                Text(
                    text = language.pick(tr = "Seviye $level", en = "Level $level"),
                    color = KronColors.TextSecondary,
                    fontSize = 12.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = UpgradeCatalog.displayValue(type, level, car),
                        color = KronColors.Blue,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                    if (cost != null) {
                        Text(text = "→", color = KronColors.TextMuted, fontSize = 13.sp)
                        Text(
                            text = UpgradeCatalog.displayValue(type, level + 1, car),
                            color = KronColors.AccentBright,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            PrimaryButton(
                text = cost?.toString() ?: language.pick(tr = "MAX", en = "MAX"),
                subtitle = if (cost != null) language.pick(tr = "coin", en = "coins") else null,
                enabled = affordable,
                modifier = Modifier
                    .width(BUY_BUTTON_WIDTH)
                    .heightIn(min = MIN_TOUCH),
                onClick = onUpgrade
            )
        }
    }
}

@Composable
private fun BoosterRow(
    type: BoosterType,
    owned: Int,
    coins: Int,
    language: AppLanguage,
    onBuy: () -> Unit
) {
    val affordable = coins >= type.coinPrice

    KronCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = type.title(language),
                    color = KronColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = type.description(language),
                    color = KronColors.TextMuted,
                    fontSize = 11.sp
                )
                Text(
                    text = language.pick(tr = "Elindeki: $owned", en = "Owned: $owned"),
                    color = if (owned > 0) KronColors.Blue else KronColors.TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            PrimaryButton(
                text = "${type.coinPrice}",
                subtitle = language.pick(tr = "coin", en = "coins"),
                enabled = affordable,
                modifier = Modifier
                    .width(BUY_BUTTON_WIDTH)
                    .heightIn(min = MIN_TOUCH),
                onClick = onBuy
            )
        }
    }
}
