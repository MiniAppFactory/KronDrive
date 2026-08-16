# FUN GATE — Kron Drive v1.0.9 (versionCode 10)

**Değerlendiren:** Game Director
**Tarih:** 2026-08-16
**Kapsam:** salt okuma. Kod, `PROVENANCE.md`, `docs/BALANCE.md`, `docs/CHANGELOG.md`,
`docs/HANDOVER_20260815.md`. Hiçbir dosya değiştirilmedi (bu rapor hariç),
cihaza dokunulmadı, build alınmadı.

**Yöntem:** ana okuma Game Director tarafından yapıldı; ayrıca iki paralel
salt-okuma denetimi çalıştırıldı (onboarding/kontrol hissi ve
ekonomi/tutunma). Aşağıdaki her iddia dosya:satır ile bağlanmıştır —
hiçbir madde yalnızca bir ajan raporuna dayanmıyor.

---

## 0. Bu raporun kanıt sınırı (önce bunu oku)

**Oyunu oynamadım.** Cihaza dokunmadım, build almadım, ekran görüntüsü çekmedim.
Aşağıdaki her iddia iki kutudan birine düşer ve hangisi olduğu tek tek yazılıdır:

| İşaret | Anlamı |
|---|---|
| **[KOD]** | Kaynak dosyadan doğrudan okundu. Dosya:satır verildi. Tartışmaya kapalı olgu. |
| **[CİHAZ]** | Koddan çıkarım. His, okunabilirlik, tempo, gecikme. Samsung S8'de doğrulanmadan karar dayanağı yapılmamalı. |

Bu ayrım kozmetik değil: aşağıdaki **KALDI** kararının tamamı **[KOD]**
maddelerine dayanıyor. Tek bir **[CİHAZ]** maddesi bile kararı taşımıyor —
yani karar cihazda test yapılmadan da geçerli, cihaz testi yalnızca listeyi
uzatabilir, kısaltamaz.

Ayrıca not: `docs/BALANCE.md` iki yerde **güncel değil** (aşağıda §4).
Denge tartışması yaparken kod kazanır, belge değil.

---

## 1. Core Loop

**Tanım (iki cümle):**
> Üç şeritli trafikte yukarı doğru sürersin; şerit değiştirerek araçlardan
> kaçar, coin toplar, boost'la boşluğu kapatırsın. Koşu bitince topladığın
> coin'i garajda gövde, boya ve dört yükseltme dalına yatırırsın.

Bu, türü için sağlam ve doğru bir loop. Sorun loop'un tanımında değil,
**loop'un kapanmamasında** — koşunun ödülü garajda anlamlı bir şeye
dönüşmüyor (§4, §5).

### Koşu süreleri gerçekte ne kadar? [KOD]

Görev "25-40 saniyelik koşular" diyor; katalog bunu doğrulamıyor.
`game/LevelCatalog.kt` içindeki `LevelGoal` süreleri:

| Bölüm aralığı | Süre | Kaç bölüm |
|---|---|---|
| 1-4 | 25 / 30 / 35 / 40 s | 4 |
| 5-9 | 45-60 s | 5 |
| 10-20 | 60-85 s | 11 |
| 21-30 | 85-120 s | 10 |

Yani **kariyerin yalnızca %13'ü (4/30 bölüm) 25-40 saniye bandında**.
Geri kalanın çoğu 70-120 saniye. Kariyerin tamamı, her bölüm ilk denemede
geçilirse, `LevelCatalog.kt` sürelerinin toplamı = **2.245 saniye ≈ 37 dakika**
saf sürüş.

**Yorum:** 25-40 s bandı bu tür için doğru bandtır ve oyun o bandı ilk dört
bölümden sonra terk ediyor. 90-120 saniyelik bir "şeritten şeride geç"
koşusu, mekanik hiç değişmediği için ikinci yarısında tekrar hissi verir.
Bu **[CİHAZ]** ile doğrulanmalı ama tasarım tarafında uyarı işareti:
uzun koşu, yeni mekanik gelmiyorsa zorluk değil dayanıklılık testidir.

### Koşu başına oyuncunun kazandığı şey [KOD]

`game/GameEngine.kt:771-781` — formül tam olarak şu:

```
coinsEarned = toplananCoin × 1
            + skor / 120
            + YENİ görev sayısı × 25
            (Double Reward booster varsa × 2)
            (koşu < 10 s ise 0)
```

`GameEngine.kt:781-782` — XP: `skor/10 + görevSayısı × 20`.

Bunun sayısal karşılığı (bölüm 5, 45 s, skor ≈ 2.840, ~12 coin toplanmış):

| Durum | Coin |
|---|---|
| **İlk kez 3/3 ile geçmek** | 23 (skor) + 12 (coin) + 75 (görev) ≈ **110** |
| **Aynı bölümü tekrar oynamak** | 23 + 12 ≈ **35** |

`docs/CHANGELOG.md:429` bunu kendi ağzıyla doğruluyor: *"3. kademe hâlâ bir
kariyer koşusunun 14 katını ödüyor"* — 520/14 ≈ **37 coin/koşu**.

**Geri dönüş hissi ne kadar güçlü?** Zayıf. Bir koşunun ödülünün **%68'i**
(75/110) tek seferlik görev primi. O prim alındıktan sonra aynı bölüm
kalıcı olarak 35 coin'e düşüyor — yani oyuncunun en iyi oynadığı, en iyi
tanıdığı bölüm, en az ödeyen içerik hâline geliyor. Bu, farm engelleme
kararının (`GameEngine.kt:766-770`, doğru bir karar) **ikinci yarısının
yapılmamış olmasından** geliyor: kaçak kapatıldı, yerine gelir konmadı.

---

## 2. Öğrenme eğrisi — ilk 3 dakika

### Gerçekte ne var: hiçbir öğretici yok [KOD]

- `ui/navigation/AppNavigation.kt:58-61` — ilk açılışta **tek** kapı var:
  dil seçimi (`LanguageGateScreen`). Dil seçilir seçilmez oyuncu ana menüye
  düşer.
- `ui/onboarding/` altında **yalnızca `LanguageGateScreen.kt` var** — oyunla
  ilgili tek bir öğretici ekran yok.
- Kaynak ağacında `tutorial`, `ipucu`, `hint`, `nasıl oynanır` geçen
  **tek bir metin yok**.
- `data/GameStateRepository.kt:79,491-492` bir `HAS_SEEN_ONBOARDING` anahtarı
  ve `markOnboardingSeen()` fonksiyonu tanımlıyor,
  `ui/KronViewModel.kt:349-350` onu sarıyor — **ve hiçbir yerden çağrılmıyor.**
  `hasSeenOnboarding` (`data/PlayerProgress.kt:70`) hiçbir ekran tarafından
  okunmuyor. Yani onboarding iskeleti kurulmuş, sonra unutulmuş: **ölü kod.**

Oyuncu kontrolleri **yalnızca bölüm içinde deneyerek** öğrenmek zorunda.
İlk bölüm bunun için tasarlanmış (2.60 saniyede bir araç, kaybetmek çok zor)
ve bu iyi bir karar — ama "boş yol" bir öğretici değildir: oyuncuya ekranın
neresine basacağını söylemez.

**Üç ek boşluk [KOD]:**

1. **Kaydırarak şerit değiştirme tamamen keşfe bırakılmış.**
   `GameScreen.kt:308-331` — tam ekran, 36 dp eşikli yatay sürükleme
   katmanı var ve **hiçbir görsel göstergesi yok**. Butonları gören oyuncu
   bu alternatifin varlığını hiç öğrenmeyebilir.

2. **Terimler hiçbir yerde tanımlanmıyor.** HUD kısaltmaları
   `BİTİR / GEÇİŞ / DODGE / BOOST / PUAN / COMBO / COIN`
   (`GameModels.kt:68,77,87,100,126,136,146`). "Perfect Dodge"un ne demek
   olduğu — yani *aracın yanından kıl payı geçmek* — oyunun hiçbir ekranında
   yazmıyor. Oyuncudan bölüm 4'te ilerlemek için 3 tane yapması isteniyor
   (§3), ama ne olduğu söylenmiyor.

3. **Geri sayım 3 saniyelik boş öğretme alanı.**
   `CountdownOverlay` (`GameScreen.kt:1412-1431`) yalnızca *"HAZIR OL"* ve
   `3 → 2 → 1 → GO!` gösteriyor. Her koşuda garantili görülen 3 saniye,
   hiçbir bilgi taşımıyor — §11 madde 5'in en ucuz uygulama yeri burası.

**Buna karşılık iyi olan:** bölümün üç hedefi **koşu sırasında** sağ üstte
canlı gösteriliyor, ilerleme sayısıyla ve tamamlanınca yeşil tikle
(`GameScreen.kt:672-677`, `ObjectiveRow :552-589`, `buildHud :527-533`).
Oyuncu neyi kaçırdığını koşu bitmeden görüyor. Bu doğru yapılmış.

### Ne, hangi sırayla öğretiliyor [KOD]

`LevelCatalog.kt` içindeki `trafficDensity` ve görev dizilimi
(spawn aralığı = `0.78 / trafficDensity`, `GameEngine.kt:524-527`):

| Bölüm | Yoğunluk | Araç/sn | Öğrettiği şey | Görevler (sıralı) |
|---|---|---|---|---|
| 1 | 0.30 | 1 / 2.60 s | şerit değiştirme | Bitir · 3 geçiş · 3 coin |
| 2 | 0.55 | 1 / 1.42 s | trafik | Bitir · 6 coin · 14 geçiş |
| 3 | 0.70 | 1 / 1.11 s | **boost** | 10 geçiş · 200 m boost · 1400 puan |
| 4 | 0.85 | 1 / 0.92 s | **perfect dodge** | Bitir · 1800 puan · 3 dodge |
| 5 | 1.00 | 1 / 0.78 s | tam trafik | Bitir · 30 geçiş · 2500 puan |
| 6 | 0.85 | 1 / 0.92 s | **combo** (nefes bölümü) | 10 coin · 4 dodge · 3x combo |
| 7 | 1.00 | 1 / 0.78 s | baskı altında combo | 2900 puan · 6 dodge · 4x combo |
| 8 | 1.00 | 1 / 0.78 s | mesafe + süre | Bitir · 36 s altı · 4 dodge |

Bu rampanın **kendisi iyi tasarlanmış**. Testere dişi (6. bölüm 5'ten kolay),
tek eksende zorlaşma, her bölüm tek yeni şey — hepsi doğru. Bu tasarımı
öven bir not `docs/BALANCE.md:161-171`'de yazılı ve hak edilmiş.

### İlk 3 dakikada oyuncu nereye kadar gelir?

Bölüm 1+2+3 = 90 saniye sürüş + 3×3 s geri sayım + sonuç ekranları.
Yani **ilk üç dakika ≈ bölüm 1-3**, her biri ilk denemede geçilirse.
Bu bandda oyuncu şerit değiştirme, trafik ve boost'u görmüş olur.

### Hiç öğretilmeyen: FREN ve KORNA [KOD]

Bu, öğretme sırasının en somut açığı.

**Korna** — `PROVENANCE.md` #15 açıkça söylüyor: *"tamamen eğlence —
oynanışa hiçbir etkisi yok"*. Sorun değil, kabul.

**Fren** — asıl mesele bu. `LevelCatalog.kt`'nin 30 bölümünde freni konu
alan **tek bir hedef türü** var: `Objective.BrakeTapsAtMost`, ve yalnızca
**3 yerde** geçiyor:

- `LevelCatalog.kt:298` — bölüm 13: `BrakeTapsAtMost(1)`
- `LevelCatalog.kt:245` — bölüm 19: `BrakeTapsAtMost(0)`
- `LevelCatalog.kt:298` — bölüm 25: `BrakeTapsAtMost(1)`

Üçü de **"frene BASMA"** diyor. Yani oyun oyuncuya freni hiçbir zaman
olumlu öğretmiyor; ilk kez bölüm 13'te bahsediyor ve o bahsediş
"bunu kullanma" biçiminde.

Buna karşılık garajda **dört yükseltme dalından biri FREN**
(`game/UpgradeCatalog.kt:8`, `92-93`) ve sonuna kadar çıkarmak
**7.000 coin**. Oyun, oyuncudan hiç öğretmediği ve görevlerde
cezalandırdığı bir mekaniğe 7.000 coin yatırmasını istiyor.

Bu bir denge hatası değil, bir **iletişim çelişkisi**. İkisinden biri
düzeltilmeli: ya fren gerçekten işe yarar hâle gelip öğretilmeli
(örn. "frenle bir araca yaklaş, arkasından geç" tipi bir erken hedef),
ya da FREN dalı yükseltme listesinden çıkarılmalı.

---

## 3. Bölüm geçiş kuralı — **oyunun en büyük yapısal hatası**

### Kural [KOD]

`game/GameEngine.kt:796-797`:
```kotlin
passed = mode == RunMode.CAREER && level != null &&
    level.awardsStars && stars == level.stars.size
```

`data/GameStateRepository.kt:272-281`:
```kotlin
suspend fun recordLevelResult(levelId: Int, stars: Int, passed: Boolean) {
    ...
    if (passed) {
        val highest = prefs[Keys.HIGHEST_LEVEL] ?: 1
        if (levelId >= highest) prefs[Keys.HIGHEST_LEVEL] = levelId + 1
    }
}
```

Yani: **bir sonraki bölüm ancak üç görevin üçü de aynı koşuda
tamamlanırsa açılıyor.** Kısmi ilerleme kaydediliyor
(`starsMap[levelId] = maxOf(...)`, satır 275) ama **kilidi açmıyor** —
farklı koşularda toplanan görevler birikmiyor, hepsi tek koşuda gerekli.

### Bu erken oyunda duvar yaratır mı? Evet — ve bunu projenin kendi testi söylüyor.

Projede `LevelCurveTest.kt` var: iki otopilotla bölümleri gerçekten oynayan,
çok iyi yazılmış bir ulaşılabilirlik testi. Şunları **garanti ediyor**:

| Test | Satır | Garanti ettiği |
|---|---|---|
| `bolum 1 ... uc yildiz vermedi` | `LevelCurveTest.kt:136-145` | Bölüm 1: 5 tohumda da **3/3** |
| `ilk sekiz bolum ... carpmadan tamamlanabilir` | `:147-158` | Bölüm 1-8: `completed` |
| `ilk sekiz bolumde ilerleme tikanmaz` | `:160-174` | Bölüm 1-8: **stars ≥ 1** |
| `ilk bes bolumde temkinli oyun iki yildizi da alir` | `:176-189` | Bölüm 1-5: **stars ≥ 2** |

**Dikkat:** en güçlü garanti bölüm 2-5 için **2 yıldız**. Üç yıldız yalnızca
bölüm 1 için kanıtlanmış. Bölüm 6, 7, 8 için hiçbir 3/3 garantisi yok.

Ve testin kendi başlık yorumu (`LevelCurveTest.kt:24-27`) şunu diyor:

> *"En kritik degismez: **her bolum en az 1 yildiz verebilmeli**, cunku bir
> sonraki bolum ancak `stars > 0` ile aciliyor
> (`GameStateRepository.recordLevelResult`)."*

**Bu cümle artık YANLIŞ.** Kural 2026-08-15'te `stars > 0`'dan
`stars == 3`'e değişti (`docs/HANDOVER_20260815.md:44`), ama test
güncellenmedi. Test hâlâ eski kuralın değişmezini koruyor.

**Doğrulama:** tüm test ağacında `passed` kelimesi **sıfır kez** geçiyor
(`grep -rn "passed" source/app/src/test/` → boş çıktı). Yani oyunun
**ilerleme kapısı hiç test edilmiyor** ve ilk 8 bölümün geçilebilir olduğuna
dair **hiçbir kanıt yok**.

**Eski varsayım kodun içinde ikinci bir yerde daha duruyor:**
`LevelCatalog.kt:43-44` — *"bir sonraki bolum ancak EN AZ 1 yildizla acilir"*.
Yani ilk 8 bölümün **tasarımının kendisi** bu eski kurala göre yapılmış;
kural değişti, tasarım gözden geçirilmedi. Bu, §11 madde 2'nin neden bir
"ayar" değil bir **düzeltme** olduğunu gösteriyor.

**Ve çarpışma hiçbir şey kaydetmiyor:** `KronViewModel.kt:168` —
`if (levelId != null && stats.completed)`. Yani kaza ile biten koşu
`recordLevelResult`'a hiç gitmiyor. Oyuncu 44. saniyede, üç görevin ikisi
tamamlanmışken çarparsa **hiçbir ilerleme kaydedilmiyor** — kısmi görev
sayısı bile. Tek kazandığı, koşu içinde biriktirdiği coin.

### Sayısal değerlendirme: duvar tam olarak nerede

Üç görevin üçü de gerekince, `docs/BALANCE.md:161-165`'te yazılı olan
altın kural — *"İlk hedef asla beceri hedefi olamaz"* — **anlamsızlaşıyor.
Artık ÜÇÜNCÜ hedef de bir ilerleme kilidi.** Bu kuralın koruduğu şey
sırayla ilk hedefti; şimdi her hedef ilk hedef kadar zorunlu.

Sonuç, beceri hedeflerinin ilerleme kilidine dönüşmesi:

| Bölüm | İlerleme için zorunlu hâle gelen beceri hedefi |
|---|---|
| **4** | `PerfectDodges(3)` — `LevelCatalog.kt:97` |
| **6** | `PerfectDodges(4)` **ve** `ComboAtLeast(3)` — `:122-123` |
| **7** | `PerfectDodges(6)` **ve** `ComboAtLeast(4)` — `:132-133` |
| **8** | `FinishUnderSeconds(36)` **ve** `PerfectDodges(4)` — `:145-147` |

Perfect Dodge tanımı gereği risk gerektiriyor: `GameConfig.kt:298-304`,
eşik = çarpışma sınırı (42 px) ile şerit aralığının tam ortası. 360 dp'lik
bir telefonda geçerli pencere **42-54.6 px** — yan şeritten temiz geçmek
saymıyor, gerçekten yanaşmak gerekiyor (`docs/BALANCE.md:108-122`).
Bu, mekaniğin doğru tasarımı. Ama **temkinli oyunun ürettiği dodge sayısı
sıfıra yakın** (`LevelCurveTest.kt:22-23`: *"hic risk almaz, dolayisiyla
neredeyse hic Perfect Dodge yapmaz"*).

Yani **bölüm 4, oyunun 4. dakikasında, oyuncudan bilinçli olarak çarpışma
riskine girmesini ZORUNLU tutuyor.** Yeni oyuncu için bu erken.

Bölüm 7 daha da sert: `ComboAtLeast(4)` = 4 dodge'un her biri bir öncekinden
**6 saniye içinde** (`GameConfig.kt:307`, `GameEngine.kt:613`). Tam yoğunlukta
6 saniyede ~7 araç geliyor (0.78 s aralık) — yani 7 aracın 4'üne üst üste
yanaşmak gerekiyor. Bu bir "casual" hedefi değil.

**Ve `LevelCurveTest.kt:191-215` bunu kanıtlamıyor:** riskli otopilot testi
dodge sayısını ve combo'yu **ayrı koşulardan, 5 tohumun EN İYİSİNİ alarak**
ölçüyor (`maxByOrNull`, `maxOf`). Yani "aynı koşuda hem 6 dodge hem 4x combo
hem 2900 puan hem bitirme" hiç denenmemiş. İlerleme için gereken tam olarak bu.

---

## 4. Ekonomi

### Fiyatlar [KOD] — `game/CarCatalog.kt`

**Gövdeler (7):** 0 · 900 · 1500 · 1500 · 1800 · 2400 · 3200 = **11.300 coin**
**Boyalar (10):** 0 · 250 · 400 · 500 · 600 · 850 · 1100 · 1400 · 1750 · 2200 = **9.050 coin**
**Yükseltmeler:** `UpgradeCatalog.kt:44-45`, `250 × mevcutSeviye`,
1→8 = 7.000 coin/dal × 4 dal = **28.000 coin**

**Oyundaki her şeyin toplam bedeli: 48.350 coin.**

### Gelir [KOD]

| Kaynak | Miktar | Kaynak dosya |
|---|---|---|
| Başlangıç | 100 | `PlayerProgress.STARTING_COINS` |
| Kariyer koşusu (tekrar) | ~35 | `GameEngine.kt:771-773` |
| Kariyer koşusu (ilk 3/3) | ~110 | aynı |
| Günlük görev (3 kademe) | **900** | `DailyChallengeGenerator.kt:86` → `120, 260, 520` |
| Haftalık görev (5 görev × 3 kademe) | **1.000** | `WeeklyMissionGenerator.kt:20-56` → 40/60/100 × 5 |
| Haftalık sandık | **750** + 1 Second Chance | `PlayerProgress.kt:13-16` |
| Ödüllü reklam | 150 × günde 5 = **750/gün** | `GameConfig.kt:444,452` |

**Günlük toplam bütçe ≈ 2.000 coin** (900 günlük + 750 reklam + haftalıkların
günlük payı ~250 + koşu geliri). Buna karşılık bir kariyer koşusu **35 coin.**
Yani oyuncunun bir günlük coin bütçesinin **%2'sinden azı** oyunu oynamaktan
geliyor.

> ⚠️ **`docs/BALANCE.md` iki yerde yanlış:**
> - **Satır 203:** *"Günlük görev 400–500 coin"* → kodda **900**
>   (`TIER_REWARDS = intArrayOf(120, 260, 520)`). Değişiklik
>   `docs/CHANGELOG.md:425`'te kayıtlı, BALANCE.md'ye işlenmemiş.
> - **Satır 204:** *"Haftalık kademe 40/60/100 (5 görev × 3 kademe = **900**)"*
>   → aritmetik hata. (40+60+100) × 5 = **1.000**.

### İlk ücretli aracı almak kaç koşu sürüyor?

**Yarış Sedan, 900 coin** (`CarCatalog.kt:649`), oyuncu 100 coin ile başlıyor
→ **800 coin gerekiyor**. Üç yol var ve üçü çok farklı:

| Yol | Süre |
|---|---|
| Aynı bölümü tekrar oynamak (35 coin) | **23 koşu ≈ 20 dakika** |
| Bölüm 1-8'i ilk kez 3/3 geçmek (~110 coin) | **~8 bölüm** — ama 3/3 gerektiği için garanti değil |
| **Günlük görevi bir kez yapmak** | **1 koşu (900 coin)** |

**Bu bekleme motive edici mi, kırıcı mı? İkisi de değil — ANLAMSIZ.**

Çünkü üçüncü yol diğer ikisini siliyor. Tek bir günlük görev koşusu
(180 saniye tavanı, `DailyChallengeGenerator.kt:70`) **26 kariyer koşusu
kadar ödüyor**. Oyuncunun ilk aracı ne kadar iyi oynadığına değil,
uygulamayı hangi gün açtığına bağlı.

### Uygulanmamış ekonomi önerisiyle karşılaştırma

`docs/HANDOVER_20260815.md:83-85` ve `docs/CHANGELOG.md:434-436`:

> *"Uygulanmayan (sahibi 'aşamalı' dedi): gelir/maliyet sabitleri
> (`SCORE_PER_BONUS_COIN`, `REWARDED_COIN_AMOUNT`, maliyet tablosu, booster
> fiyatları) ve sonsuz mod zorluk tavanı — ekonomi raporunda hazır duruyor."*

**Not:** o raporun kendisi depoda **yok** — `docs/` altında ekonomi önerisi
içeren bir dosya bulunamadı. Elimizde yalnızca "hazır duruyor" cümlesi var.
Yani karşılaştırma yapılamıyor; öneri kayıp.

Karşılaştırma yerine ölçüm koyuyorum. **Kariyerin tamamının ömür boyu geliri:**

- Görev primi: 30 bölüm × 3 görev × 25 coin = **2.250**
- Skor + toplanan coin (30 ilk geçiş, ~40 coin/koşu): **~1.200**
- **Toplam ≈ 3.450 coin**

**Yani oyunun ana içeriği olan 30 bölümlük kariyeri baştan sona bitirmek,
oyundaki içeriğin %7'sini (3.450 / 48.350) satın alıyor.**

Geri kalan %93 yalnızca günlük görev (900/gün) + reklam (750/gün) ile
gelebilir → **en hızlı ihtimalle ~30 gün**, kariyer hiç oynanmadan.

Bu, ekonominin yönünü tersine çevirmiş durumda: **oyunu oynamak coin
kazanmanın en yavaş yolu.** Bu bir denge ayarı meselesi değil, loop'un
kopması.

### Yükseltme eğrisinin ilk basamağı görünmez [KOD]

`UpgradeCatalog.kt:64-69`, üs 1.5 dışbükey eğri:
`curve(1..8) = 0, .054, .153, .281, .433, .606, .796, 1.0`

İlk yükseltme (250 coin) `curve(2) = .054` demek. Sayısal karşılığı:

| Dal | Formül | Sv1 → Sv2 |
|---|---|---|
| SPEED | `3.2 + 1.12×curve` (`:72-73`) | 181 → **182.9 km/h** (+1.9) |
| ACCELERATION | `6.0 + 4.0×curve` (`:76-77`) | 167 → **161 ms** (−6 ms) |
| BRAKE | `0.9 + 0.60×curve` (`:92-93`) | 28.4 → **29.4 km/h** (+1.0) |
| BOOST | `38 − 14×curve` (`:100-101`) | 2.63 → **2.69 s** (+0.06 s) |

**Dördü de algı eşiğinin altında.** Oyuncunun yapabileceği ilk satın alma —
yani yükseltme sistemini ona satması gereken satın alma — hiçbir şey
hissettirmiyor.

İlk **hissedilir** basamak seviye 5 (`curve = .433`, BOOST 2.63 → 3.13 s,
+%19). Oraya çıkmanın maliyeti: 250+500+750+1000 = **2.500 coin ≈ 71 kariyer
koşusu**, tek bir dal için.

Bu, `docs/CHANGELOG.md:396-403`'te kayıtlı bir düzeltmenin yan etkisi:
oyuncu *"upgrade'ler çok hızla iyileşiyor"* demişti, eğri dışbükey yapıldı.
Doğru teşhis, fazla sert ilaç — sorun uçtan alındı, başlangıca yıkıldı.

### Ayrıca [KOD]: SPEED yükseltmesi ilk bölümlerde tamamen etkisiz

`GameEngine.kt:387-394`: `target = baseSpeed + min(scoreCap, score/600)`.
Yani `scoreCap` **ancak `score > scoreCap × 600` olduğunda devreye giriyor** —
seviye 1'de skor **1.920**'yi geçmeden SPEED yükseltmesinin de,
`topSpeedMul`'un da hiçbir etkisi yok.

Skor eğrisinden (geçilen araç ve coin katkısı dahil) hesaplanan bağlanma anı:

| Bölüm | Koşu sonu skoru | Tavan devreye giriyor mu? |
|---|---|---|
| 1 (25 s) | ~1.110 | **Hayır — hiç** |
| 2 (30 s) | ~1.570 | **Hayır — hiç** |
| 3 (35 s) | ~2.100 | Evet, son ~2 saniye |
| 4 (40 s) | ~2.500 | Evet, son ~9 saniye |

Yani bölüm 1-3'te SPEED yükseltmesi alan oyuncu **kesinlikle hiçbir fark
göremez**. Aynı hata bir kez daha yapılmış durumda: `CHANGELOG.md:405-408`
İVME göstergesinin *"bu yükseltme hiçbir şey yapmıyor"* dediğini anlatıyor
ve gösterimi düzeltmiş; ama SPEED'de **gösterim değil mekaniğin kendisi**
erken oyunda ölü.

---

## 5. Araç çeşitliliğinin anlamı

### Çarpanlar [KOD] — `CarCatalog.kt`

| Araç | Fiyat | topSpeed | accel | brake | boost |
|---|---|---|---|---|---|
| Şehir | 0 | 1.00 | 1.00 | 1.00 | 1.00 |
| Yarış Sedan | 900 | 1.04 | 1.08 | 1.00 | 1.00 |
| Kuş SLX | 1500 | 0.97 | 1.00 | 1.06 | **1.12** |
| Dağ Keçisi | 1500 | 1.00 | 0.94 | **1.12** | 1.06 |
| Kas Arabası | 1800 | 1.08 | 0.96 | 0.96 | 1.00 |
| Boğa 67 | 2400 | 1.10 | 0.92 | 0.90 | 1.04 |
| Süper Araba | 3200 | **1.12** | 1.10 | 0.94 | 1.00 |

### Elde hissediliyor mu? Dört eksenin üçünde HAYIR.

Çarpanlar yükseltmenin üstüne uygulanıyor (`UpgradeCatalog.kt:119-137`).
En uç iki aracı (Şehir vs Süper Araba) **seviye 1**'de karşılaştırırsak:

| Eksen | Şehir | Süper Araba | Fark | Hissedilir mi? |
|---|---|---|---|---|
| Son hız | 181 km/h | 185 km/h | **+4 km/h** | Hayır — üstelik bölüm 1-3'te hiç devreye girmiyor (§4) |
| İvme | 167 ms | 152 ms | **−15 ms** | Hayır — insan bu farkı sürüşte ayırt edemez |
| Fren | −28.4 km/h | −26.7 km/h | **1.7 km/h** | Hayır |
| Boost süresi | 2.63 s | 2.63 s | **0** | — (Süper Araba'da bu eksen nötr) |

Tek istisna **boost süresi**: Kuş SLX ×1.12 → 2.63 s yerine **2.95 s**
(+0.32 s, +%12). Bu tek eksen **[CİHAZ]** doğrulamasıyla hissedilebilir
olabilir; diğer üçü için koddaki fark zaten algı eşiğinin altında.

Seviye 8'de bile en uç fark küçük: SPEED sv8'de Şehir 216 km/h,
Süper Araba 233 km/h → **+17 km/h (%8)**.

**Yani araç farkı büyük ölçüde tabloda kalıyor.** Ve bu bilinçli bir
karardı — `docs/BALANCE.md:291-293`, sınır 1: *"Çarpanlar yükseltmelerin
ÜSTÜNE uygulanır... Fark ~%10 bandında kalır: ana ilerleme garaj
yükseltmeleri olmalı."* Karar tutarlı ve gerekçesi sağlam.

**Ama sonuç şu:** ana ilerleme olması istenen garaj yükseltmeleri de
hissedilmiyor (§4 — ilk 4 basamak algı eşiğinin altında, 2.500 coin sonra
hissediliyor). **İki sistem birbirine "asıl ilerleme öteki" diyor ve
ikisi de ilerleme vermiyor.**

### Garaj çubukları farkı olduğundan büyük gösteriyor [KOD]

`docs/BALANCE.md:301-306` ve `CarCatalog.statFraction`: çubuklar mutlak
değil **katalog içinde normalize** — en iyi araç dolu, en kötü 0.22.
Yani %4'lük bir gerçek fark, ekranda **dolu çubuk ile neredeyse boş çubuk**
olarak görünüyor.

Gerekçe anlaşılır (%112 dolu göstermek yedi aracı aynı gösterirdi) ve
yanına gerçek yüzde de yazılıyor (`+12% / −6%`) — bu dürüstlük iyi.
Yine de dikkat: oyuncu 3.200 coin'i **çubuğa bakarak** ödüyor, yüzdeye
bakarak değil. Bu, manipülatif olma sınırına yaklaşan bir sunum.
Şu hâliyle kabul edilebilir (yüzde yazılı olduğu için) ama **çarpanlar
gerçekten hissedilir hâle getirilmeden çubukları daha da dramatikleştirmek
yanıltıcı olur.**

---

## 6. Tutunma (retention)

### Var olanlar [KOD]

| Sistem | Durum |
|---|---|
| Günlük görev | Var — 3 kademe, 120/260/520 = 900 coin (`DailyChallengeGenerator.kt:86`) |
| Haftalık görev | Var — `data/WeeklyMissionGenerator.kt` |
| Sonsuz mod rekoru | Var — `GameStateRepository.kt:456-465`, saniye + skor |
| Rekora yakınlık mesajı | Var — `GameConfig.kt:365`, ≤5 s kalınca özel mesaj |

Sonsuz moddaki *"rekoruna 3 saniye kaldı"* mesajı **oyundaki en iyi
tutunma fikri** — kayıptan sonra hemen tekrar oynama isteği yaratan tek
mekanizma. Doğru içgüdü.

### 3. gün geri dönme sebebi ne? [KOD]

**Yok denecek kadar az. Somut sebepler:**

1. **Hiçbir bildirim yok.** `source/app/src/main/AndroidManifest.xml`
   yalnızca iki izin içeriyor: `INTERNET` ve `ACCESS_NETWORK_STATE`.
   `POST_NOTIFICATIONS` **yok**. Kaynak ağacında `Notification`,
   `AlarmManager`, `WorkManager` **hiç geçmiyor**. Yani oyunun oyuncuyu
   geri çağırma yolu **sıfır** — oyuncunun kendi aklına gelmesi gerekiyor.

2. **Seri (streak) sistemi yok.** Günlük görev her gün sıfırdan başlıyor;
   arka arkaya oynamak hiçbir ek şey vermiyor. Kaybedilecek bir şey yok →
   bir günü kaçırmanın bedeli yok → geri dönme baskısı yok.

3. **Rekor kimseyle karşılaştırılmıyor.** `recordEndlessRun`
   (`GameStateRepository.kt:456`) yalnızca cihazda tek bir sayı tutuyor.
   Liderlik tablosu, arkadaş, hayalet araç, haftalık sıralama — hiçbiri yok.
   (Proje kararı gereği sunucu/hesap yok — bu kısıt kabul, ama **cihaz içi**
   alternatifler de kullanılmamış: örn. "geçen haftanın rekoru", "bu haftanın
   en iyisi", hayalet.)

4. **Kariyer 37 dakikalık içerik** (§1). 3. günde biten oyuncu için geriye
   yalnızca sonsuz mod ve günlük görev kalıyor.

5. **Günlük görev çeşitliliği sınırlı ve öngörülebilir.**
   `DailyChallengeGenerator.kt:167-168` — şablon seçimi
   `TEMPLATES[floorMod(dayId.hashCode(), 7)]`, yani **7 şablon** ve tarihten
   türetilen sabit bir dönüşüm. 8. günde oyuncu görevleri tanımaya başlıyor.

6. **Sonsuz modun skor rekoru saklanıyor ama hiçbir ekranda gösterilmiyor.**
   `ENDLESS_BEST_SCORE` yazılıyor (`GameStateRepository.kt:456-465`), UI'da
   tek referansı yok. Yalnızca süre rekoru gösteriliyor
   (`MainMenuScreen.kt:156-162`). Ölü veri — ve rekabet edilecek ikinci bir
   eksen bedavaya duruyorken kullanılmıyor.

7. **Booster'lar sonsuz ve günlük modda seçilemiyor.** Booster çipleri
   yalnızca `LevelDetailDialog` içinde (`LevelMapScreen.kt:576-602`);
   `AppNavigation.kt:74-75` sonsuz ve günlük modu doğrudan oyun ekranına
   gönderiyor. Yani 400 coin'lik Second Chance, oyuncunun rekor kovaladığı
   modda **kullanılamıyor** — tam da en çok isteneceği yerde.

**Eksik olan ne, tek cümleyle:** oyuncunun kaybetmekten korkacağı hiçbir
şey yok. Retention, ödül vaadinden değil **biriktirilmiş bir şeyi
sürdürme** isteğinden gelir; oyunda biriken tek şey coin ve o da (§4)
hiçbir şeye yetmiyor.

---

## 7. Reklam yükü

### Yapılandırma [KOD] — `GameConfig.kt:407-468`

| Reklam | Sıklık | Satır |
|---|---|---|
| Geçiş (kariyer) | 2 tamamlanan bölümde bir | `:428` |
| Geçiş (sonsuz) | 3 koşuda bir | `:431` |
| Her koşu sonu geçiş | **KAPALI** (`false`) | `:425` |
| Rewarded "devam et" | koşu başına 1 kez | `:468` |
| Rewarded coin | 150 coin, günde 5 | `:444,452` |
| Oyun ekranında banner | **yok** | `CLAUDE.md` §4 |

### Değerlendirme: **bu bölüm oyunun en sağlam tarafı.** Akışı bozmuyor.

Gerekçeler, hepsi koddan:

- `INTERSTITIAL_AFTER_EVERY_RUN = false` kararının gerekçesi
  `GameConfig.kt:412-424`'te yazılı ve **doğru**: bölümler 30-90 saniye
  sürüyor, her koşuda reklam demek her 30-90 saniyede bir tam ekran demekti.
  Bu kararı veren kişi türü tanıyor.
- Oyun ekranında banner yok — sürüş alanı temiz.
- Ödül **yalnızca** SDK'nın gerçek "kazanıldı" geri çağrısında veriliyor
  (`GameScreen.kt:1745-1750` yorumu ve `onRewardEarned` dalı).
- **Reklam ödül ekranını asla kesmiyor.** `withOptionalInterstitial`
  (`GameScreen.kt:498-512`) yalnızca sonuç ekranından **çıkarken** çağrılıyor
  (`onNext :481`, `onHome :486`, duraklatmadan çıkış `:394`). Oyuncu önce
  ödülünü görüyor, reklam sonra geliyor. Bu, doğru sıra — tersi bu türde
  en sık yapılan hatadır.
- Reklam yüklenemezse akış **beklemeden** devam ediyor — oyun reklama
  bağımlı değil.
- Reklamdan dönüşte güvenli pencere var: 3 s dokunulmazlık
  (`GameConfig.kt:395`) + 1.2 s araç doğmama (`:402`) + yol temizliği.
  Bu, *"reklamı izledim, hemen tekrar çarptım"* şikâyetine verilmiş
  doğru cevap.
- Buton **kırpılmış** miktarı yazıyor (`GameScreen.kt:1712-1714`:
  *"butonda '+500' görüp 150 almak güven kırar"*). Dürüst.
- Günlük limit garaj ve sonuç ekranı arasında **paylaşılıyor**
  (`GameConfig.kt:446-452`) — eski sınırsız coin açığı kapatılmış.

**Tek uyarı [CİHAZ]:** kariyerde geçiş reklamı "2 tamamlanan bölümde bir"
tetikleniyor. §3'teki 3/3 kapısı yüzünden bölüm tamamlama **seyrekleşiyor**,
yani pratikte reklam sıklığı planlanandan düşük olacak. Bu, gelir tarafında
bir sorun — ama **oyuncu deneyimi açısından sorun değil**, ve §3 düzeltilince
kendiliğinden normale döner. Şu an için reklam frekansına **dokunulmamalı**;
önce oyun düzelsin, sonra veriye bakılsın (bu zaten `GameConfig.kt:421-423`'te
planlanmış: 30 gün veri, puan 4.2 üstünde kalırsa kademeli artır).

---

## 8. Control / Feel Review

### Kontroller [KOD]

- Şerit lerp oranı `GameConfig.kt:73`: `LANE_LERP_RATE = 16` → ~0.06 s'de
  şerit ortasına oturuyor. Prototipte 12'ydi, *"geç tepki veriyor"* geri
  bildirimiyle 16'ya çıkarılmış. Bu iyi bir sayı — **[CİHAZ]** ile
  doğrulanmalı ama koddaki değer sağlıklı.
- Çarpışma kutusu görselden **küçük**: `HITBOX_SCALE = 0.88`
  (`GameConfig.kt:56`), ve gerekçesi yazılı: *"kutunun görselden birkaç
  piksel KÜÇÜK olması adil hissettirir, büyük olması haksız"*.
  Doğru karar, doğru yönde hata payı.
- Hissedilen hız `WORLD_SPEED_SCALE = 0.75` (`:162`) — gösterge 160 km/h
  derken dünya 120 km/h gibi akıyor. Oyuncu isteğiyle yapılmış bilinçli
  sapma (`PROVENANCE.md` #5). **[CİHAZ]:** bunun "ağır" hissettirip
  hissettirmediği ölçülmeli; göstergenin söylediği ile ekranın gösterdiği
  arasındaki bu ayrım his tarafında bedelsiz değildir.

### Geri bildirim hiyerarşisi — **ciddi eksik** [KOD]

`ui/game/GameScreen.kt` içinde `performHapticFeedback` **tam 4 yerde**
çağrılıyor (satır 322, 326, 343, 346) ve **dördü de girdi/direksiyon**
için — hepsi `HapticFeedbackType.TextHandleMove`, yani en hafif tık.

**Titreşim OLMAYAN olaylar:**
- **çarpışma** — oyundaki en önemli an
- **perfect dodge** — oyundaki tek beceri ödülü
- coin toplama
- combo yükselmesi
- görev tamamlanması

Yani oyun, oyuncunun parmağına **yalnızca kendi bastığı tuşu** bildiriyor;
oyunda **olan hiçbir şeyi** bildirmiyor. Manifest'te `VIBRATE` izni de yok
(Compose haptikleri için gerekmiyor, doğru — ama daha güçlü bir çarpışma
titreşimi istenirse gerekecek).

### Çarpışma anı tamamen boş — titreşimden ibaret değil [KOD]

Denetim, sorunun sandığımdan geniş olduğunu gösterdi. Çarpışmada:

| Kanal | Durum | Kanıt |
|---|---|---|
| Ses | **yok** | `audio/` altında `crash` geçen tek satır yok; `EngineVoice` yalnızca `playNitro()` ve `playHorn()` sunuyor |
| Parçacık | **yok** | `addParticles()` yalnızca iki yerde: boost izi (`GameEngine.kt:442`) ve coin (`:637`) |
| Ekran sarsıntısı | **yok** | `GameRenderer.kt`'de çarpışmaya özel çizim yok |
| Slow-motion / hit-stop | **yok** | `timeScale`/`slowMo` kavramı motorda hiç yok |
| Titreşim | **yok** | yukarıdaki 4 çağrı yalnızca girdi |

`GameEngine.onCrash()` (`:697-704`) tek karede skoru düşürüp `phase`'i
değiştiriyor ve overlay geliyor. Yani **oyunun en yüksek duygusal anı,
teknik olarak bir durum değişkeni atamasından ibaret.**

Aynısı ödül anları için de geçerli: `GameScreen.kt:226-246` olay döngüsünde
`GameEvent.CoinPicked` ve `GameEvent.VehiclePassed` **`else -> Unit`**
dalına düşüyor (`:244`). Perfect Dodge'un tek geri bildirimi ekran
ortasında **900 ms süren sessiz bir metin banner'ı** (`:228-235`, `:291-301`).

### Perfect Dodge geri bildirimi geç geliyor [KOD]

`GameEngine.kt:588-595`: dodge, araç oyuncuyu **tamamen geçtiği anda**
(`o.y > playerVisualBottom`) tescil ediliyor — en yakın geçiş anında değil.
Mantık doğru (`minDx` boyunca en küçük mesafe saklanıyor), ama **ödül,
heyecan anından sonra** patlıyor.

Taban hızda yaklaşma hızı 240 px/s (`docs/BALANCE.md:74`), araç yüksekliği
~61 px → hizalanmadan tam geçişe **~0.25 saniye**. Yani oyuncu "kıl payı
kurtuldum" dediği andan çeyrek saniye sonra ödüllendiriliyor.

**[CİHAZ]:** bu gecikmenin gerçekten kopukluk yaratıp yaratmadığı ölçülmeli.
Ama tasarım kuralı net: *near-miss'in ödülü, near-miss anında verilir.*

---

## 9. Fail / Retry Review — **oyunun en acil hatası**

### Oyuncu neden kaybettiğini anlıyor mu? EVET — bu iyi yapılmış [KOD]

`GameScreen.kt:1558-1582`, sonuç ekranı dürüst:
- Başlık `"GÖREVLER EKSİK"` (geçilmediyse), `"TAMAMLANDI"` değil.
- Altında açıklama: *"Bölümü geçmek için üç görevin üçü de gerekli."*
- Her hedefin yanında **"1280/1400" biçiminde ilerleme**
  (`GameScreen.kt:441-446`), yalnızca "olmadı" değil.
- `hasNext` yalnızca `runResult.passed` ise true (`:432-436`) — oyuncu
  kilitli bir bölüme yönlendirilmiyor.

Bu bölüm örnek niteliğinde. Şeffaflık tam.

### Retry kaç aksiyon? **4 dokunuş + reklam + ~1.5 s animasyon + 3 s geri sayım.** [KOD]

Çarpıştıktan sonra aynı bölümü tekrar denemek için:

| # | Aksiyon | Kanıt |
|---|---|---|
| 1 | `CrashOverlay` → **"SONUÇLARI GÖR"** | `GameScreen.kt:1518-1522` |
| 2 | `RunResultOverlay` → **"ANA MENÜ"** (başarısızsa **tek** buton) | `:1766-1770` |
| — | Araya **geçiş reklamı** girebilir (`withOptionalInterstitial`) → kapatmak için +1 dokunuş | `GameScreen.kt:486, 504-505` |
| — | Bölüm haritasında araç animasyonu + otomatik kaydırma: 350 ms gecikme + 1100 ms hareket | `LevelMapScreen.kt:140-156` |
| 3 | Bölüm düğümüne dokun | `LevelMapScreen.kt:177, 343` |
| 4 | Açılan diyalogda **"BAŞLA"** | `LevelMapScreen.kt:605-611` |

Sonra **3 saniye geri sayım** (`GameConfig.kt:475`).

> **Düzeltme (denetim sonrası):** İlk taslakta bunu 5 dokunuş saymıştım —
> "ANA MENÜ" düğmesinin ana menüye gittiğini varsaymıştım. Gerçekte
> `onExit = navController.popBackStack()` (`AppNavigation.kt:164`) oyuncuyu
> **doğrudan bölüm haritasına** döndürüyor, ana menüye değil. Yani dokunuş
> sayısı 4. (Yan not: düğmenin etiketi gittiği yeri yanlış söylüyor —
> oyuncunun lehine bir tutarsızlık, ama yine de tutarsızlık.)
>
> Toplam sürtünme yine de bütçenin çok üstünde: 4 dokunuş + olası bir tam
> ekran reklam + ~1.5 s harita animasyonu + 3 s geri sayım, 25 saniyelik
> bir bölüm için.

**"TEKRAR" butonu YOK ve bilerek kaldırılmış.** Gerekçe
`GameScreen.kt:1763-1766` ve `GameConfig.kt:434-441`'de yazılı: koşuyu
bedavaya sıfırlamak, reklamsız tur çevirmenin ve kısa koşu farmlamanın
en kolay yoluydu.

**O gerekçe 2026-08-14 tarihli ve o tarihte GEÇERLİYDİ** — çünkü o zaman
bölüm geçmek için **tek görev** yetiyordu, yani başarısızlık nadir bir
durumdu. Ertesi gün (2026-08-15) kapı 3/3'e çıkarıldı ve **başarısızlık
istisna olmaktan çıkıp KURAL hâline geldi** — ama retry akışı o eski
varsayıma göre kaldı.

Sonuç: **25-45 saniyelik bir koşuyu, sık sık başarısız olacak şekilde
tasarlanmış bir kapının arkasında, 5 dokunuş + 2 ekran geçişi + 3 saniye
geri sayım ödeyerek tekrarlamak.** Bu, hyper/hybrid-casual bir oyunda
kabul edilemez. Türün bir numaralı kuralı: *kaybetmekle tekrar başlamak
arasında tek dokunuş vardır.*

Ve dikkat: bu iki karar **tek başına** alındığında ikisi de savunulabilir.
Zarar **birlikte** oluşuyor ve kimse ikisini yan yana koymamış.

---

## 10. Fun Risks (özet)

| # | Risk | Kanıt | Şiddet |
|---|---|---|---|
| R1 | 3/3 kapısı + 5 dokunuşlu retry = erken oyunda bırakma | §3, §9 | **Kritik** |
| R2 | İlerleme kapısı hiç test edilmemiş; test dosyası eski kuralı koruyor | `LevelCurveTest.kt:24-27`, `grep passed` boş | **Kritik** |
| R3 | Kariyer, oyundaki içeriğin %7'sini finanse ediyor | §4 | **Yüksek** |
| R4 | İlk yükseltme ve araç farkları algı eşiğinin altında | §4, §5 | **Yüksek** |
| R5 | Çarpışma ve dodge **tamamen sessiz ve efektsiz** (ses/parçacık/sarsıntı/haptik yok) | §8 | **Yüksek** |
| R6 | Hiç öğretici yok; onboarding iskeleti ölü kod; terimler tanımsız | §2 | Orta |
| R7 | Fren hiç öğretilmiyor ama 7.000 coinlik dal | §2 | Orta |
| R8 | 3. gün geri dönme sebebi yok; bildirim/seri/sıralama yok | §6 | Orta |
| R9 | 70-120 s'lik geç bölümlerde mekanik tekrarı | §1 | Orta **[CİHAZ]** |

---

## 11. MUST FIX BEFORE RELEASE

Beş madde. Öncelik sırası bağlayıcı — 1 ve 2 aynı sorunun iki yarısı,
**birlikte** yapılmalı.

### 1. Sonuç ekranına "TEKRAR DENE" butonunu geri koy
`ui/game/GameScreen.kt:1752-1770`

Kaldırılma gerekçesi (farm) hâlâ geçerli, o yüzden **koşulsuz geri koyma**:
buton yalnızca **başarısız kariyer koşusunda** (`!result.passed`) görünsün.
Başarılı koşuda zaten "SONRAKİ BÖLÜM" var, farm riski oradan gelmiyordu.
`MIN_PAID_RUN_SECONDS` (10 s) ve "yıldız coini yalnızca yeni yıldıza"
kuralları zaten yerinde duruyor ve farmı bağımsız olarak kapatıyor.

**Neden bu oyunu daha eğlenceli yapar:** Bu türde eğlence, hata ile yeni
deneme arasındaki mesafeden gelir. Şu an o mesafe 5 dokunuş + 3 saniye;
oyuncu 25 saniyelik bir bölümü tekrar denemek için oyunun yarısını
dolaşıyor. Tek dokunuşa indirmek, aynı içerikten alınan deneme sayısını
katlar — ve bu türde "bir daha" hissi, oyunun kendisidir.

### 2. 3/3 kapısını yumuşat: ilerleme için 2/3, tam ödül için 3/3
`game/GameEngine.kt:796-797`

Sahibinin kararı — *"görevleri tamamlamadıysa neden geçiyor ki bölümü"* —
haklı bir tepkiydi: eski kural (`stars > 0`) gerçekten fazla gevşekti,
tek görevle geçiliyordu. Ama düzeltme uçtan uca gitti. Orta yol:

- **Kilit açma: 2/3.** İlerleme tıkanmaz.
- **3/3: tam ödül + haritada "mükemmel" işareti.** Beceri hedefleri
  ödül olarak kalır, kilit olmaktan çıkar.
- Alternatif (daha da iyi): görevler **koşular arasında birikssin**.
  `starsMap` zaten kalıcı olarak kaydediliyor (`GameStateRepository.kt:275`);
  kilidi tek koşuya değil **birikmiş** görev sayısına bağlamak tek satırlık
  bir değişiklik.

**Neden bu oyunu daha eğlenceli yapar:** Şu anki kural, `docs/BALANCE.md`'de
özenle yazılmış olan *"ilk hedef asla beceri hedefi olamaz"* kuralını
sessizce iptal etti — artık her hedef beceri kilidi. Bölüm 4'te oyuncudan,
oyunun 4. dakikasında, ilerlemek için bilerek çarpışma riskine girmesi
isteniyor (`PerfectDodges(3)`); bölüm 7'de 6 saniyede 4 art arda yanaşma
(`ComboAtLeast(4)`). Beceri hedefleri **hedeflemek için** eğlencelidir,
**mecbur kalmak için** değil. 2/3'e indirmek, o güzel testere dişi eğriyi
geri getirir.

### 3. Çarpışmaya ve Perfect Dodge'a duyusal karşılık ver
`ui/game/GameScreen.kt`, `game/GameEngine.kt:588-595`, `audio/EngineVoice.kt`

Şu an **her iki an da tamamen boş**: ses yok, parçacık yok, ekran sarsıntısı
yok, hit-stop yok, titreşim yok (§8 tablosu). `GameScreen.kt:244` coin ve
geçiş olaylarını `else -> Unit` ile yutuyor.

- **Çarpışma:** çarpma sesi + güçlü titreşim + kısa ekran sarsıntısı +
  ~120 ms hit-stop + parçacık. (`addParticles()` altyapısı zaten var,
  `GameEngine.kt:442,637` — yalnızca çarpışmadan çağrılmıyor.)
- **Perfect Dodge:** kısa keskin ses + tık + combo çarpanının ekranda
  büyümesi. Şu anki tek geri bildirim 900 ms'lik sessiz metin.
- **Coin:** en azından bir ses. Şu an sessiz.
- **Dodge'u erken tescil et:** `GameEngine.kt:588-595` aracın tamamen
  geçmesini bekliyor (~0.25 s gecikme). En yakın geçiş anında tetiklenmeli.

**Neden bu oyunu daha eğlenceli yapar:** Perfect Dodge bu oyundaki tek
gerçek beceri anı ve şu an oyuncuya hiçbir kanaldan geri dönmüyor — oyun
ona yalnızca kendi bastığı tuşu bildiriyor, olan biteni bildirmiyor.
Trafik atlatma oyununun tamamı "kıl payı" hissi üzerine kuruludur; o his
sese, titreşime ve ekran tepkisine verilmezse mekanik sayaçta kalır.
Motor sesi sentezi altyapısı (`EngineVoice.kt`) zaten çalışıyor ve APK'ya
0 bayt ekliyor — çarpma ve coin sesi eklemenin maliyeti düşük.

### 4. Kariyer koşusunun coin ödülünü anlamlı hâle getir + ilk yükseltmeyi hissettir
`game/GameConfig.kt:372` (`SCORE_PER_BONUS_COIN = 120`),
`game/UpgradeCatalog.kt:64` (`CURVE_EXPONENT = 1.5`)

İki sayı, iki ayrı sorun:

- **Gelir:** kariyer koşusu ~35 coin, günlük görev 900 coin. Günlük,
  26 kariyer koşusuna bedel. Kariyerin ömür boyu geliri (~3.450) oyundaki
  içeriğin (%48.350) **%7'si**. `SCORE_PER_BONUS_COIN`'i düşürmek
  (örn. 120 → 60) kariyer koşusunu ~2 katına çıkarır; ya da bölüm
  numarasına bağlı bir tamamlama primi eklenir.
- **Eğri:** `curve(2) = .054` yüzünden ilk 250 coin'lik yükseltme
  +1.9 km/h / −6 ms / +0.06 s veriyor — dördü de hissedilmez. Üssü
  1.5'ten ~1.2'ye çekmek uç noktayı korur, ilk basamağı görünür kılar.

**Neden bu oyunu daha eğlenceli yapar:** Şu an oyunu oynamak, coin
kazanmanın en yavaş yolu — ve kazanılan ilk coin'le alınan ilk şey hiçbir
fark yaratmıyor. Bir ilerleme sistemi, ilk satın almada kendini
kanıtlayamazsa oyuncu bir daha bakmaz. Loop'un kapanması buna bağlı:
koşu → coin → hissedilir gelişme → daha iyi koşu.

### 5. 60 saniyelik bir kontrol öğreticisi ekle
`ui/onboarding/` (iskelet zaten var ama ölü:
`GameStateRepository.kt:79,491`, `KronViewModel.kt:349`, hiçbir yerden
çağrılmıyor)

Ayrı bir ekran gerekmez — **bölüm 1'in içine** yerleştirilmiş, sırayla
sönen üç ipucu yeter: *"Şerit değiştir"* → *"Coin topla"* → (bölüm 3'te)
*"Boost'a bas"*. Ölü `hasSeenOnboarding` bayrağı tam da bunun için var.

**Neden bu oyunu daha eğlenceli yapar:** Zorunlu değil ama ucuz. Bölüm 1
zaten "kaybetmesi neredeyse imkânsız" olacak şekilde tasarlanmış — yani
öğretmek için doğru yer hazır, içine öğretilecek şey konmamış. Oyuncunun
ilk 30 saniyede ne yapacağını anlaması bu kapının açık şartıdır ve şu an
bunu yalnızca deneme yanılmayla öğreniyor.

---

## 12. Nice to Have (yayın engeli değil)

1. **Sonsuz modda seri (streak) ve haftalık rekor.** Sunucu gerekmez;
   `GameStateRepository`'ye iki alan yeter. Kaybedilecek bir şey yaratmak,
   3. gün geri dönüşünün en ucuz yolu.
2. **Bildirim.** `POST_NOTIFICATIONS` + günlük görev hatırlatması.
   Şu an oyunun oyuncuyu geri çağırma yolu sıfır.
3. **Fren mekaniğine bir varlık sebebi.** Ya erken bir bölümde olumlu
   öğretilsin, ya FREN dalı yükseltmelerden çıkarılsın (§2).
4. **Geç bölümlerde (70-120 s) yeni bir değişken.** Mekanik hiç
   değişmediği için uzun koşular tekrara düşüyor **[CİHAZ]**.
5. **Booster'lar sonsuz ve günlük modda da seçilebilsin** (`AppNavigation.kt:74-75`).
   400 coin'lik Second Chance, rekor kovalanan modda kullanılamıyor.
6. **Sonsuz modun skor rekoru gösterilsin** — saklanıyor ama hiçbir ekranda yok.
7. **`docs/BALANCE.md` güncellensin:** günlük görev 400-500 değil **900**
   (satır 203); haftalık toplam 900 değil **1.000** (satır 204, aritmetik hata).
8. **`LevelCurveTest.kt:24-27` ve `LevelCatalog.kt:43-44` yorumları
   düzeltilsin** — ikisi de eski `stars > 0` kuralını anlatıyor.
9. **Latent hata:** `coinsCollected` hem `GameEngine.kt:636`'da hem
   `:771`'de `COINS_PER_PICKUP` ile çarpılıyor — çift uygulama. Sabit 1
   olduğu için bugün etkisi yok; 2 yapılırsa coin **4 kat** olur. Tuzak.
10. **Ölü kod temizliği:** `INTERSTITIAL_EVERY_N_RETRIES` (`GameConfig.kt:441`)
    ve `consumeRetryInterstitial` hiçbir yerden çağrılmıyor; `runKey`
    (`GameScreen.kt:151`) hiç artırılmıyor; `markOnboardingSeen()` /
    `hasSeenOnboarding` bağlanmamış.
11. **Küçük sayaç açığı:** `levelsSinceInterstitial` yalnızca CAREER'da artıyor
    (`KronViewModel.kt:174`) ama DAILY de onu **tüketiyor** (`:213-214`) —
    günlük görev koşusu geçiş reklamı sayacını artırmadan sıfırlayabiliyor.
12. **"ANA MENÜ" düğmesi ana menüye gitmiyor**, bölüm haritasına dönüyor
    (`AppNavigation.kt:164`, `popBackStack`). Etiket düzeltilsin.
13. Boğa 67'nin geçici adı kalıcılaştırılsın (`CarCatalog.kt:875`).

---

## 13. Mandatory Review Questions — kısa cevaplar

| # | Soru | Cevap |
|---|---|---|
| 1 | İlk 30 saniyede ne yapacağını anlıyor mu? | **Kısmen.** Öğretici yok; bölüm 1 boş yol olduğu için deneyerek öğreniyor. **[CİHAZ]** |
| 2 | Ana aksiyon tatmin edici mi? | **[CİHAZ]** — kodda lerp 16 ve hitbox 0.88 sağlıklı, ama dokunsal geri bildirim yok (§8) |
| 3 | Neden kaybettiğini anlıyor mu? | **EVET.** Sonuç ekranı örnek niteliğinde dürüst (§9) |
| 4 | Retry 2-3 aksiyondan fazla mı? | **EVET — 4 dokunuş + reklam + ~1.5 s animasyon + 3 s geri sayım.** Kritik (§9) |
| 5 | İlk 5 dakikada yeterli variation var mı? | **Evet.** Bölüm 1-5 rampası iyi tasarlanmış (§2) |
| 6 | Reward davranışı güçlendiriyor mu? | **HAYIR.** Kariyer %7 finanse ediyor; ilk yükseltme hissedilmiyor (§4) |
| 7 | Difficulty artışı beceriyle uyumlu mu? | **Rampanın kendisi evet, kapı hayır** (§3) |
| 8 | Reklamlar akışı bozuyor mu? | **HAYIR.** Oyunun en sağlam tarafı (§7) |
| 9 | Audio/VFX başarıyı vurguluyor mu? | **HAYIR.** Çarpışma ve dodge'da ses/parçacık/sarsıntı/haptik yok; dodge 0.25 s geç tescil (§8) |
| 10 | Geri gelmek için anlamlı hedef var mı? | **HAYIR.** Bildirim/seri/sıralama yok (§6) |

---

## 14. Şu an eksik olan tek en büyük eğlence unsuru

> ## Riskin, alındığı anda ödediği bir karşılık.

Kron Drive'ın eğlence çekirdeği tek bir şey: **iki aracın arasından kıl
payı geçmek.** Oyun bunu doğru tanımlamış — `PERFECT_DODGE_WINDOW_RATIO`
ekran genişliğine göre hesaplanıyor, yan şeritten temiz geçmek saymıyor,
gerçekten yanaşmak gerekiyor (`GameConfig.kt:283-304`). Mekanik doğru
kurulmuş. Ama **oyun o anı oyuncuya hiç geri vermiyor:**

1. **Hiçbir duyusal kanal çalışmıyor.** `performHapticFeedback` dört yerde
   çağrılıyor ve dördü de direksiyon (`GameScreen.kt:322,326,343,346`).
   Kıl payı geçişte titreşim yok, **ses yok, parçacık yok, ekran tepkisi
   yok** — tek geri bildirim 900 ms'lik sessiz bir metin (`:291-301`).
2. **Ödül geç geliyor.** Dodge, araç tamamen geçtikten sonra tescil
   ediliyor (`GameEngine.kt:588-594`) — heyecan anından ~0.25 s sonra.
3. **Ödülün değeri yok.** 5x combo'da bir dodge 75 puan
   (`GameConfig.kt:309` × `comboMultiplier` 3.0) = **0.6 coin**
   (`SCORE_PER_BONUS_COIN = 120`). Yani oyunun en zor becerisi,
   bir coin bile etmiyor.
4. **Ve dahası: risk almak ödül değil, ÖDEV.** Perfect Dodge oyunda
   yalnızca üç yerde geçiyor ve üçünde de **ilerleme kilidi** olarak:
   bölüm 4, 6, 7 (§3). Oyuncu kıl payı geçişi *istediği için* değil,
   *bölüm geçmek zorunda olduğu için* yapıyor.

**Savunma.** Bu türün rakipleri (Traffic Racer, Crossy Road, Subway
Surfers) tek bir şeye dayanır: yakın kaçış anının kendisi bir mikro-ödüldür
— ses yükselir, ekran titrer, sayı büyür, çarpan artar. Oyuncuyu ekranda
tutan şey bölüm hedefi değil, **o anın tekrar yaşanma isteğidir.**
Kron Drive'da o an teknik olarak var (ölçülüyor, sayılıyor, kaydediliyor)
ama **duyusal olarak yok** ve **ekonomik olarak yok**. Bir sayaçta artan
bir sayı, eğlence değil muhasebedir.

Bunun neden diğer tüm maddelerden önce geldiği: §11'deki 1, 2 ve 4. maddeler
oyunun **önündeki engelleri kaldırıyor** — retry hızlanır, kapı açılır,
coin akar. Ama engeller kalktığında geriye oyuncunun *yapmak isteyeceği*
bir şey kalmalı. Şu an o şey — kıl payı geçiş — oyunun içinde bir ödev
olarak duruyor. **Onu ödevden ödüle çevirmek, bu oyunu eğlenceli yapan
tek değişikliktir; geri kalan her şey o değişikliğin önünü açar.**

Somut karşılığı büyük bir iş değil: dodge anında titreşim + ekran
tepkisi + yükselen ses + ekranda büyüyen çarpan, ve dodge'un **coin
olarak** gerçekten ödemesi (örn. combo 3+ iken dodge başına doğrudan coin).
Bölüm hedeflerinden çıkarılması ise §11 madde 2 ile zaten geliyor.

---

## 15. VERDICT

# ⛔ KALDI

**"Build passes" ≠ "game is done."** v1.0.9 teknik olarak sağlam: 160 birim
test geçiyor, debug/release build alınıyor, mimari temiz, belgeler örnek
niteliğinde. Reklam akışı türün ortalamasının üstünde dürüst. Öğrenme eğrisi
rampası (`trafficDensity`) iyi tasarlanmış. Bunların hiçbiri tartışma konusu
değil.

**Ama yayına çıkamaz. Dört bağlayıcı sebep, hepsi [KOD]:**

1. **Erken oyunda ilerleme duvarı var ve hiç test edilmemiş.** Kapı 3/3
   (`GameEngine.kt:796`), test ağacında `passed` kelimesi geçmiyor, ve
   projenin kendi eğri testi yalnızca **2/3** garanti ediyor
   (`LevelCurveTest.kt:176-189`). Bölüm 2-8'in geçilebilir olduğuna dair
   **hiçbir kanıt yok.** Bu tek başına yayın engeli: oyuncuların bölüm 4'te
   veya 7'de kalıcı olarak tıkanma ihtimali ölçülmemiş.

2. **Başarısızlık kural, retry ise 5 dokunuş.** İki karar ayrı ayrı
   savunulabilir (3/3 kapısı 15 Ağustos, TEKRAR'ın kaldırılması 14 Ağustos)
   ama **birlikte** hyper-casual bir oyunda kabul edilemez bir döngü
   üretiyor. Kimse ikisini yan yana koymamış.

3. **Ödül döngüsü kapanmıyor.** Kariyeri baştan sona bitirmek oyundaki
   içeriğin **%7'sini** finanse ediyor; ilk yükseltme algı eşiğinin altında;
   araç çarpanlarının dördünden üçü hissedilmiyor. Oyuncunun oynama sebebi
   ile oyunun ödüllendirdiği şey birbirine bağlı değil.

4. **Oyunun en önemli iki anının hiçbir duyusal karşılığı yok.** Çarpışmada
   ve Perfect Dodge'da ses, parçacık, ekran sarsıntısı, hit-stop ve titreşim
   — **beşi de yok**; dodge ayrıca 0.25 s geç tescil ediliyor.

**ITERATE değil KALDI, çünkü:** 1. madde bir denge ayarı değil,
**doğrulanmamış bir ilerleme riski.** Oyuncunun oyunda kalıcı olarak
tıkanıp tıkanmayacağını bilmiyoruz ve bunu bilmeden mağazaya çıkmak,
1 yıldızlı yorumların en pahalı türünü davet eder — geri dönüşü olmayan tür.

### Yeniden değerlendirme için gereken

**ŞARTLI GEÇTİ'ye çıkmak için:** §11 madde **1 ve 2** (retry + kapı).
Bu ikisi düzeltilirse oyun oynanabilir hâle gelir.

**GEÇTİ için:** madde **3, 4, 5** ve §14'teki tek maddenin karşılanması.

**Ve her hâlükârda — pazarlık dışı:**
- `LevelCurveTest`'e `passed` değişmezi eklensin: *ilk 8 bölüm, temkinli
  otopilotla, 5 tohumda da geçilebilmeli.* Bu test yeşile dönmeden §11
  madde 2'nin doğru ayarlandığı iddia edilemez.
- **Samsung S8'de oynanış doğrulaması yapılsın.** Bu raporun tüm **[CİHAZ]**
  maddeleri (kontrol hissi, hız algısı, uzun bölümlerdeki tekrar hissi,
  ses) açık duruyor. `CLAUDE.md` §6 zaten bunu şart koşuyor: *"bir davranışı
  iddia etmeden önce cihazda dene."* Ben denemedim ve bu raporda hiçbir
  his iddiası kanıt sayılmadı.

---

*Game Director onayı olmadan oyun release-ready kabul edilmez. Bu rapor
onay değildir.*
