# Kron Drive — Çapraz Etki İncelemesi (2026-08-17)

**Kapsam:** bugünün değişiklikleri birbirini bozuyor mu.
**Yöntem:** git diff + kaynak okuma + `docs/DIFFICULTY_REVIEW.md` ve
`docs/ECONOMY_BALANCE_PROPOSAL.md` içindeki ölçümlerin yeniden hesabı.
**Yapılmadı:** kod değişmedi, Gradle çalıştırılmadı, cihaza kurulmadı.
Aşağıda hiçbir yerde "test geçer/geçmez" iddiası yok; "şu test risk altında,
gerekçesi şu" var.

---

## 0. Yönetici özeti

| # | Etkileşim | Ağırlık |
|---|---|---|
| **B1** | Reklam muafiyeti + `DAILY_LEVEL_ID = -1` → **günlük görev artık hiçbir zaman geçiş reklamı göstermiyor** ve testi bunu yanlış varsayımla yeşile boyuyor | **Yüksek — gelir kaybı, kanıtlı** |
| **B2** | Kilit gevşemesi + reklam muafiyetinin 4. bölümde bitmesi → yeni oyuncu **geçemediği 6. bölümde her 3 denemede bir reklam** görüyor | **Yüksek — muafiyetin amacını tersine çeviriyor** |
| **B3** | Kilit gevşemesi tek başına yetersiz: 19 duvarın **yalnızca 3'ü** kalktı (4, 8, 25). Ölçüme göre **6. ve 7. bölüm hâlâ duvar** | **Yüksek — `LevelCurveTest` bunu yakalayacak** |
| **B4** | Kilit gevşemesi + yıldız coini → koşu başı gelir ölçülen **118 → 93 coin**; ekonomi zammı (+%18) yutuldu, net **−%7** | **Orta — ekonomi belgesinin dayanağı çürüdü** |
| **B5** | Beety 4000 + Gün Sarısı 700 → kalıcı sink %10 arttı; reklam+günlük oynayan oyuncu **dünkünden yavaş** ilerliyor | **Orta** |
| **B6** | `LevelCatalogTest` yorumu "ilk İKİ hedef" diyor ama assert hâlâ yalnızca `stars.first()` bakıyor | **Orta — kuralı hiçbir şey korumuyor** |
| **B7** | Yön tuşu 76 dp ile ekran kenarından **10 dp**'ye indi (sistem geri-jesti şeridi ~20 dp) | Düşük–Orta |
| **B8** | Duraklat 48 dp + TopEnd hedef listesi: 320 dp genişlikte İngilizce en uzun etikette çakışma payı 18 → ~12 dp | Düşük — cihazda bakılmalı |
| **B9** | İki yorum hâlâ "TÜM görevler" diyor — geçen seferki hatanın birebir tekrarı | Düşük |

Zararsız bulunanlar bölüm 10'da.

---

## 1. Doğrulanan değişiklik listesi

`git log` ile doğrulandı; görevde verilen sıra eksikti, aradaki üç commit
belge/hazırlık commit'i:

```
d3f2d28  Gecis reklami sayaci kacagi kapatildi
8cfa323  Ortak keystore karari
50eb4d6  S-6 kapandi (keystore yedegi)
6c7ae48  Devir notu + ekonomi onerisi + perf probe yamasi
84cfebf  Devir notuna girmemis araclar + F1/Beety fiyat celiskisi
f3951ea  Beety fiyati 4000 + car_refs klasoru
9a072de  Ekonomi: oynanis geliri olculup yukseltildi (oneri 1+2)
03360c1  Beety araci eklendi + kontrol duzeni buyutuldu
(commit edilmemis)  MIN_STARS_TO_PASS = 2
```

Commit edilmemiş değişiklik 6 dosyada:
`GameConfig.kt`, `GameEngine.kt`, `LevelCatalog.kt`, `GameScreen.kt`,
`LevelCatalogTest.kt`, `LevelCurveTest.kt`.

**Not:** ekonomi değişikliği "gelir artışı" değil, **gelirin taşınması**.
`SCORE_PER_BONUS_COIN` 120 → 70 (+), `TIER_REWARDS` 900 → 500/gün (−).
Görevde "ekonomi arttı" deniyor; reklam+günlük oynayan oyuncu için net
**azaldı** (bkz. B5).

---

## 2. B1 — Günlük görev artık hiç geçiş reklamı göstermiyor

**Hangi iki değişiklik:** `INTERSTITIAL_FREE_LEVELS = 4` muafiyeti (d3f2d28)
+ `GameScreen`'in reklam kararına `runResult.levelId` geçirmesi (03360c1).

**Mekanizma — kanıt zinciri:**

```
DailyChallengeGenerator.kt:55   const val DAILY_LEVEL_ID = -1
DailyChallengeGenerator.kt:30   toLevelDef(): LevelDef(id = DAILY_LEVEL_ID, ...)
GameEngine.kt:791               levelId = level?.id          // günlük koşuda -1
GameScreen.kt:572,577           withOptionalInterstitial(..., runResult.levelId, ...)
AdFrequency.kt:51               val earlyLevel = levelId != null && levelId <= 4
```

`-1 <= 4` → `earlyLevel = true` → `!earlyLevel && ...` → **false**.
Günlük görev koşusu, sayaç kaç olursa olsun, **hiçbir zaman** geçiş reklamı
göstermiyor.

**Somut senaryo:** 25. bölümdeki, oyunu 3 haftadır oynayan bir oyuncu her gün
günlük görevi oynuyor. Sonuç ekranından "Ana Menü"ye basıyor. Reklam
çıkmıyor — ne bugün, ne yarın, ne 25. bölümde, ne 30'da.

**Neden testler yakalamadı:** `AdFrequencyTest.kt:146`
`` `gunluk gorevin levelId si yok, muafiyete takilmaz` `` testi
`levelId = null` ile çağırıyor. Üretimde geçen değer `null` değil `-1`.
Test doğru davranışı doğruluyor, uygulama başka bir şey yapıyor. Testin adı
("levelId'si yok") bu yanlış varsayımı açıkça yazıya dökmüş.

**Tutarsızlık kanıtı (aynı ekran, iki farklı yol):** duraklat menüsünden
çıkış yolu (`GameScreen.kt:483`) ekranın `levelId` parametresini kullanıyor;
günlük modda o `null`. Yani **günlük görevden duraklat tuşuyla çıkarsan reklam
çıkabiliyor, koşuyu bitirip çıkarsan çıkmıyor.** Aynı modda iki farklı davranış
— tek başına bunun kasıtlı olmadığının kanıtı.

**Bir de ters yönde kaçak:** `AdFrequency.countsTowardInterstitial`
günlük koşuyu saymıyor (doğru), ama günlük koşu duraklat yolundan reklam
gösterirse `onInterstitialShown` çağrılıp **kariyer sayacını sıfırlıyor**
(`KronViewModel.kt:255`, `RunMode.DAILY -> resetLevelsSinceInterstitial()`).
Yani günlük görev kariyerin biriktirdiği reklam borcunu silebiliyor.

**Öneri (küçükten büyüğe):**
1. `AdFrequency.shouldShow` içinde muafiyeti pozitif bölümle sınırla:
   `levelId != null && levelId in 1..GameConfig.INTERSTITIAL_FREE_LEVELS`.
   Tek satır, `-1` sentinel'i muafiyetin dışına çıkar.
2. `AdFrequencyTest`'e `levelId = DAILY_LEVEL_ID` (yani `-1`) ile bir vaka
   ekle — mevcut `null` vakası üretim yolunu temsil etmiyor.
3. Kararı ver: günlük görev reklam göstermeli mi? Gösterecekse (1) yeterli.
   Göstermeyecekse bunu `mode == DAILY` üzerinden **açıkça** yaz, negatif bir
   id'nin bir eşitsizliğe takılmasına bırakma.

---

## 3. B2 — Kilit gevşedi + reklam muafiyeti 4. bölümde bitiyor

**Hangi iki değişiklik:** `INTERSTITIAL_FREE_LEVELS = 4` + `MIN_STARS_TO_PASS = 2`.

### Önce doğru soruyu kuralım

Reklam sıklığı **koşu başına** sabit: 3 koşuda 1. Kilidin gevşemesi bunu
değiştirmiyor. Değiştirdiği şey **oyuncunun muafiyet bölgesinden ne kadar
hızlı çıktığı**.

### Muafiyet penceresinin gerçek uzunluğu

Sayaç, 1–4. bölümlerde de artıyor (`countsTowardInterstitial` `levelId`'ye
bakmıyor) ve muafiyet bölgesinde hiç reklam gösterilmediği için **hiç
sıfırlanmıyor**. Sayaç yalnızca reklam gösterildiğinde sıfırlanıyor
(`onInterstitialShown`).

Bölüm süreleri 25/30/35/40 s — hepsi 10 s eşiğinin üstünde, yani
tamamlanan her koşu sayılıyor.

| | Değer |
|---|---|
| 1–4. bölümü kusursuz geçmek için en az koşu | **4** |
| Eşik | 3 |
| 5. bölüme varıldığında sayaç | **≥ 4** |

→ **5. bölümün BİTEN İLK KOŞUSU her zaman reklam gösterir.** Muafiyet
penceresi hiçbir koşulda "4 bölüm + biraz" olamıyor; tam olarak 4 bölüm ve
borç dolu olarak devrediyor.

### İlk 4 bölüm muafiyeti bu yeni hızda hâlâ yeterli mi? — Hayır

Ölçüm (`DIFFICULTY_REVIEW.md` §4.3, TEMKİNLİ profil):

| Bölüm | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 |
|---|---|---|---|---|---|---|---|---|
| Yıldız | 3 | 3 | 3 | **2** | 3 | **1** | **1** | **2** |
| 2 yıldız kuralıyla geçer mi | ✓ | ✓ | ✓ | ✓ | ✓ | **✗** | **✗** | ✓ |

Muafiyet 4. bölümde bitiyor. Ölçüme göre **ilk duvar 6. bölüm** — muafiyetin
bittiği yerden **iki bölüm sonra**.

**Somut senaryo:** Oyunu ilk kez açan oyuncu 1–5'i akıcı geçiyor (toplam
~2,5 dk sürüş), ilk reklamı 5. bölümü bitirince görüyor. 6. bölüme geçiyor,
45 saniyelik koşuyu tamamlıyor ama 2. hedef (4 perfect dodge) tutmuyor →
1 yıldız → bölüm açılmıyor. Tekrar deniyor. Tekrar deniyor. **Her 3.
denemede tam ekran reklam.** 45 s koşu + menü ≈ 60 s → yaklaşık **3 dakikada
bir reklam**, hem de geçemediği bir bölümde.

`INTERSTITIAL_FREE_LEVELS`'ın koddaki gerekçesi aynen şu:

> "en cok tekrar deneyen (= en cok zorlanan) yeni oyuncu en cok reklam goren
> kesim olmasin diye"

Bugünkü haliyle bu tam olarak gerçekleşiyor, sadece 4. bölümde değil 6.
bölümde. **Muafiyet doğru yazılmış, yanlış yere konmuş.**

### Karşılaştırma: kilit değişikliği olmasaydı ne olurdu

3 yıldız kuralında 4. bölüm bir duvardı (ölçüm 2★). Yani oyuncu muafiyet
bölgesinden **hiç çıkamıyordu** ve ilk reklamı hiç görmüyordu. Kilit
gevşemesi ilk reklamı "hiç" konumundan "~3. dakika" konumuna taşıdı. Bu
tek başına doğru bir düzeltme; sorun sonrasında ne olduğu.

**Öneri:** `INTERSTITIAL_FREE_LEVELS`'ı **4 → 8** yap (öğrenme eğrisinin
tamamı). Tek sabit, sıfır göç riski. Alternatif ve daha doğru olan çözüm 6.
ve 7. bölümün hedef sırasını düzeltmek (B3) — ama o sabit değil tasarım işi;
muafiyeti uzatmak o gelene kadar köprü olur. İkisi birden yapılırsa muafiyet
sonradan tekrar 4'e çekilebilir.

---

## 4. B3 — Kilit gevşemesi 19 duvarın yalnızca 3'ünü kaldırıyor

**Hangi iki değişiklik:** `MIN_STARS_TO_PASS = 2` + `LevelCatalog`'un hedef
sırasının değişmemiş olması.

`DIFFICULTY_REVIEW.md` iki parçalı bir düzeltme öneriyordu:
- **Değişiklik A** — kilit kuralı `stars == 3` → `stars >= 2` ✅ yapıldı
- **Değişiklik B** — 16 bölümde hedef sırası düzeltmesi ❌ yapılmadı

Yıldızlar sıralı kazanılıyor (`LevelEvaluator.tiersReached` ilk
tutmayan hedefte `break` ediyor). Yani "2 yıldız" = **ilk iki hedefin ikisi
de** tutmuş demek. 1. veya 2. sırada bir beceri hedefi varsa bölüm hâlâ duvar.

### 30 bölümün 2. slot dökümü (kaynak: `LevelCatalog.kt`)

| Kilit eşiği | Duvar sayısı | Duvarlar |
|---|---|---|
| `stars == 3` (dünkü) | **19** | 4,6,7,8,9,11,12,14,16,17,18,20,22,23,25,26,28,29,30 |
| `stars >= 2` (bugünkü) | **16** | yukarıdakiler eksi **4, 8, 25** |
| `stars >= 2` + Değişiklik B | 0 (9 bölümde hedef değişimi de gerekiyor) | — |

**Yalnızca 4, 8 ve 25. bölümler açıldı.** 6, 7, 9, 11, 12, 14, 16, 17, 18,
20, 22, 23, 26, 28, 29, 30 aynen duvar.

**Ölçülü kanıt:** 6. bölüm hedefleri `[10 coin, 4 dodge, 3x combo]`,
7. bölüm `[2900 puan, 6 dodge, 4x combo]`. Ölçümde TEMKİNLİ profil
**sekiz bölümün hiçbirinde tek bir dodge yapmıyor**
(`DIFFICULTY_REVIEW.md` satır 226–227). Dolayısıyla 6 ve 7'de 2. hedef
yapısal olarak tutmuyor → 1 yıldız → geçilemez.

**Görevdeki varsayımın düzeltmesi:** "Bölüm kilidi 3 görevden 2'ye indi, yani
oyuncu bölümleri daha hızlı geçecek" — **hayır.** Ölçülebilen bölgede
(1–8) hız kazancı yalnızca 4. bölümün duvarının kalkması. 6. bölümde oyun
yine duruyor, sadece iki bölüm ileride duruyor. Reklam ve ekonomi
hesaplarında "ilerleme hızlandı" varsayımı **kurulmamalı**.

### İyi haber (aynı değişikliğin kazandırdığı)

Modellemede **bugün bile ulaşılamaz** görünen `FinishUnderSeconds` hedefleri
— bölüm 21 (−%3), 24 (−%7), 27 (−%10), 30 (−%11),
`DIFFICULTY_REVIEW.md` §6.7 — katalogda hepsi **3. sırada**:

```
21: CompleteRun · BoostDistance(800)  · FinishUnderSeconds(70)
24: CompleteRun · PassVehicles(55)    · FinishUnderSeconds(74)
27: CompleteRun · BoostDistance(1000) · FinishUnderSeconds(80)
30: CompleteRun · PerfectDodges(15)   · FinishUnderSeconds(88)
```

3 yıldız kuralında bunlar oyunu **bitirilemez** yapıyordu (özellikle 30 —
son bölüm). 2 yıldız kuralında ustalık yıldızına düşüyorlar. Kilit
değişikliği bu tarafta gerçek ve büyük bir düzeltme. **Bunu kayda geçirin;**
kilit kararı geri alınacak olursa bu dört bölüm hedefleri de düzeltilmeden
geri alınmamalı.

---

## 5. B3-ek — `LevelCurveTest` hangi bölümlerde risk altında

Test (`LevelCurveTest.kt:170-178`) artık şunu doğruluyor:

```kotlin
LEARNING_LEVELS.forEach { level ->      // LevelCatalog.levels.take(8)
    SEEDS.forEach { seed ->             // 1, 7, 42, 1234, 90210
        assertTrue(..., r.stars >= GameConfig.MIN_STARS_TO_PASS)   // >= 2
    }
}
```

Aynı harness'in (`Style.SAFE`, TEMKİNLİ) ölçüm dökümüne göre:

| Bölüm | 2. hedef | Ölçülen yıldız | Marj | Durum |
|---|---|---|---|---|
| 1 | `PassVehicles(3)` | 3 ★ | geniş | ✅ güvenli |
| 2 | `CoinsAtLeast(6)` | 3 ★ | geniş | ✅ güvenli |
| 3 | `BoostDistance(200)` | 3 ★ | ölçümde tutuyor | ✅ güvenli (ama beceri hedefi, bkz. B6) |
| 4 | `ScoreAtLeast(1800)` | 2 ★ | ölçüm 3635 → **+%102** | ✅ güvenli |
| 5 | `PassVehicles(30)` | 3 ★ | geniş | ✅ güvenli |
| **6** | `PerfectDodges(4)` | **1 ★** | dodge sayısı 0 | ❌ **kırılması bekleniyor** |
| **7** | `PerfectDodges(6)` | **1 ★** | dodge sayısı 0 | ❌ **kırılması bekleniyor** |
| 8 | `FinishUnderSeconds(36)` | 2 ★ | ölçüm 29 s → **+%19** | ⚠️ **en ince marj** |

**İddiam:** 6. ve 7. bölümler risk altında ve kırılma gerekçesi teknik değil,
tasarımsal — test doğru olanı yapıp gerçek duvarı gösteriyor. Bu kırılma
**bastırılmamalı**; testi zayıflatmak 15 Ağustos'taki hatanın aynısını
tekrarlar (kural değişti, test eski kuralı doğruladığı için kimse görmedi).

**Uyarı — ölçüm tek tohumdan:** `DIFFICULTY_REVIEW.md` §4.3 tablosu
`olcum dokumu` testinden geliyor ve o test yalnızca `SEEDS.first()` (= 1)
ile çalışıyor. Güçlendirilmiş test **beş tohumu birden** deniyor. Yani 6 ve 7
için ölçüm kesin, 8 için (+%19 marj) **diğer dört tohumda ne olduğunu
bilmiyorum** — tahmin: 8. bölüm muhtemelen geçer ama en dar paylı bölüm o.

**Öneri:** testi çalıştırıp çıktıyı 6, 7, 8 için görün. 6 ve 7 kırılırsa
çözüm testi değil, katalogu düzeltmek: `DIFFICULTY_REVIEW.md` §7'nin
Değişiklik B önerisi (6 ve 7'de dodge hedefini ulaşılabilir bir hedefle
değiştir, combo'yu 3. slota al).

---

## 6. B4 — Kilit gevşemesi + yıldız coini: ekonomi zammı yutuluyor

**Hangi iki değişiklik:** `MIN_STARS_TO_PASS = 2` + `SCORE_PER_BONUS_COIN` 120→70.

### Mekanizma

```
GameEngine.kt:770  newStars = (stars - previousStars).coerceAtLeast(0)
GameEngine.kt:771  coinsEarned = coinsCollected + score/70 + newStars*25
```

Yıldız coini yalnızca YENİ yıldız için ödeniyor. Oyuncu 2 yıldızla geçip
ilerlerse ilk geçişte **50 coin** alıyor, 75 değil.

### Sayı — bölüm başına ilk geçiş geliri

`GameConfig.kt` yorumundaki ölçüm (8 bölüm, TEMKİNLİ, `LevelCurveTest`):

| Durum | Yıldız coini | Diğer | **Toplam** |
|---|---|---|---|
| Dün (`/120`, 3 yıldız zorunlu) | 75 | 25 | **100** |
| Ekonomi commit'inden sonra (`/70`, 3 yıldız) | 75 | 43 | **118** (+%18) |
| **Kilit değişikliğiyle birlikte (`/70`, 2 yıldızla geçiş)** | 50 | 43 | **93** |

→ **Ekonomi commit'inin +%18'i, kilit değişikliğiyle birlikte −%7'ye
dönüşüyor.** İki değişiklik ayrı ayrı doğru, birlikte birbirini siliyor.

### Toplam kariyer geliri

`ECONOMY_BALANCE_PROPOSAL.md` §7 modeli: 30 bölüm ilk geçiş = **4.050 coin**,
bunun **2.250'si** yıldız coini. 2 yıldızla ilerleyen oyuncuda yıldız coini
30 × 50 = **1.500** → toplam **~3.300**. Fark: **−750 coin, kariyerin
%18,5'i.**

### Bu para kayboldu mu? Hayır — ertelendi

`GameStateRepository.kt:275` `starsMap[levelId] = maxOf(eski, yeni)` ve
`newStars = stars - previousStars`. Oyuncu sonradan dönüp 3. yıldızı alırsa
25 coin o zaman ödeniyor. **Ömür boyu yıldız coini toplamı değişmiyor
(2.250).** Değişen şey **ne zaman geldiği**: erken oyundan çıkıp "belki hiç"
kutusuna taşınıyor.

Risk buradaki varsayımda: kaç oyuncu geri döner? Ölçemem. Ama
`ECONOMY_BALANCE_PROPOSAL.md` §7 açıkça "erken oyunda gelirin **%70'i**
yıldız coinidir" diyor ve tüm §8 önerileri bunun üstüne kurulu. Yeni oranla
bu **%54** (93 coinin 50'si).

### Erken oyuna somut etkisi

Belgenin §4 tablosu, 8 bölüm sonunda kümülatif **1.056 coin** diyor.
Yeniden hesap:

```
1.056 (dün)  + 8×18 (ekonomi)  − 8×25 (kilit)  =  1.000 coin
```

→ 8 bölüm sonunda oyuncu **dünkünden 56 coin daha fakir**. İlk araç
(Yarış Sedan, 900) yine ~8. bölümde alınıyor — pratikte fark yok, ama
belgenin "öneri 2 ilk aracı 8 geçişten 7'ye indirir" iddiası **artık
geçersiz.**

### Belge düzeltmesi zorunlu

`ECONOMY_BALANCE_PROPOSAL.md` §2.3 aynen şöyle diyor:

> "Bir bölüm ancak **üç görevin üçü de** tamamlanınca geçilir. Yani her ilk
> geçiş **her zaman** 3 yıldız = 75 coin öder."

Bu cümle **artık yanlış** ve §7 ile §8'in taşıyıcı varsayımı. Kilit
değişikliği commit edilirken bu belge aynı commit'te düzeltilmeli — aynı
belgenin §12'de `BALANCE.md` için istediği şeyin birebir aynısı.

---

## 7. B5 — Beety 4000 + Gün Sarısı 700: ekonomi önerisi hâlâ geçerli mi

**Hangi iki değişiklik:** ekonomi commit'i (9a072de) + Beety/boya (03360c1).

### Sink'in yeni hali (`CarCatalog.kt`'ten sayıldı)

| Kalem | Önce | Sonra | Fark |
|---|---|---|---|
| Yükseltmeler | 28.000 | 28.000 | — |
| Araçlar (ücretli) | 11.300 | **15.300** | +4.000 |
| Boyalar (ücretli) | 9.050 | **9.750** | +700 |
| Boyalar (fabrika boyaları düşülünce) | ~7.150 | **~7.850** | +700 |
| **Kalıcı toplam** | **~46.450** | **~51.150** | **+4.700 (+%10,1)** |

Belgedeki bitirme hedefi (tüm yükseltmeler + en pahalı araç):
**31.200 → 35.200** (+%12,8). Gün Sarısı da sayılırsa 35.900.

### Gelir tarafı (belgenin kendi §9.2 tablosu)

| Oyuncu tipi | Dün | Ekonomi commit'inden sonra |
|---|---|---|
| Reklam + günlük | 2.560/gün | **2.430/gün (−%5)** |
| Sadece oynayan | 910/gün | **1.180/gün (+%30)** |

### Bitirme süresi — belgenin gün sayılarını 35.200'e ölçekleyerek

| Oyuncu tipi | Dün | Ekonomi sonrası | **+ Beety** | Net |
|---|---|---|---|---|
| Reklam + günlük | ~11 gün | ~11 gün | **~12,4 gün** | **+%13 YAVAŞLADI** |
| Sadece oynayan | ~31 gün | ~23 gün | **~26 gün** | kazanç 8 gün → **5 gün** |

**Cevap: öneri kısmen yutuldu.**

- **Reklam+günlük oynayan oyuncu için tamamen yutuldu ve tersine döndü.**
  Geliri %5 düştü, hedefi %12,8 büyüdü. Bu oyuncu dünkünden **daha yavaş**
  ilerliyor. Ekonomi önerisinin bu oyuncu tipi için ilan ettiği sonuç
  ("~11 gün → ~11 gün, nötr") artık doğru değil.
- **Sadece oynayan oyuncu için kazancın %38'i yendi** (8 gün → 5 gün).
  Önerinin ana hedefi buydu ve hedefin çoğu ayakta.

**Dürüstlük payı:** Beety kozmetik ve zorunlu değil. Bu tabloların hepsi
"oyuncu en pahalı aracı hedefliyor" varsayımına dayanıyor. Beety'yi
umursamayan oyuncu için hiçbir şey değişmedi. Ama Beety katalogun **en
pahalısı ve en yenisi** — yani vitrindeki hedef. "Kimse istemez" varsayımı
kurulamaz.

**Öneri:** karar noktası, düzeltme değil. Üç seçenek:
1. **Hiçbir şey yapma.** Beety kozmetik, sink artışı %10, kabul edilebilir.
   (Bence savunulabilir seçenek.)
2. Ekonomi önerisinin **dördüncü kolunu** masadan al: `REWARDED_COIN_AMOUNT`
   düşürme fikri iptal — zaten reklam+günlük oyuncusunun geliri düşmüş
   durumda, daha fazla düşürülmemeli. (Belge bunu "sonra bakılır" diye
   ertelemişti; cevap artık **hayır**.)
3. Beety'nin `requiredCarLevel = 6` şartı zaten dekoratif
   (belge §4: oyuncu seviye 6'ya 9. bölümde ulaşıyor). Fiyat gerçek kapı
   olduğuna göre buna dokunmaya gerek yok — bu kısım doğru yapılmış.

---

## 8. B6 — `LevelCatalogTest` yorumu kuralı anlatıyor, assert korumuyor

Kaynak yorumu (`LevelCatalog.kt:35-44`, commit edilmemiş) artık şöyle:

> "ilk **İKİ** hedef makul olmalı; PerfectDodge/Combo gibi beceri hedefleri
> **ÜÇÜNCÜ** sıraya konur"

Testin assert'i (`LevelCatalogTest.kt:128`) **değişmedi**:

```kotlin
LevelCatalog.levels.take(8).forEach { level ->
    val first = level.stars.first()      // <-- hâlâ yalnızca 1. hedef
    ...
}
```

Yorumu güncellenmiş, assert'i güncellenmemiş. Bugünkü katalogda kuralı ihlal
eden bölümler:

| Bölüm | 2. hedef | İhlal |
|---|---|---|
| 3 | `BoostDistance(200)` | beceri hedefi, ama ölçümde tutuyor |
| **6** | `PerfectDodges(4)` | **duvar** |
| **7** | `PerfectDodges(6)` | **duvar** |
| 8 | `FinishUnderSeconds(36)` | süre hedefi, +%19 marj |
| 9 | `PerfectDodges(5)` | ölçülmedi |

Yani yeni kural yazıldığı anda **beş bölüm tarafından ihlal ediliyor** ve
hiçbir test bunu söylemiyor. Bu, `MIN_STARS_TO_PASS` sabitinin başındaki
tarihçe notunun anlattığı hatanın **birebir aynısı**: kural değişti, yorum
güncellendi, assert eski kaldı.

**Öneri:** assert'i `level.stars.take(2)` üzerinde çalışacak şekilde genişlet.
Kırılacaktır — kırılması doğrudur ve kırıldığı yer B3'ün düzeltilmesi gereken
yerlerini tam olarak listeler.

---

## 9. Kontrol boyutları — hesap

Portrait sabit (`AndroidManifest.xml:37` `screenOrientation="portrait"`),
yani en dar makul senaryo **320 dp genişlik**.

### Sabitlerden türetilen konumlar

```
CONTROL_EDGE_PADDING   = 16       CONTROL_BOTTOM_PADDING = 24
PEDAL_SIZE             = 64       CONTROL_VERTICAL_GAP   =  5
STEER_SIZE             = 76       HORN_SIZE              = 48   HORN_GAP = 24

CONTROL_COLUMN_CENTER  = 16 + 64/2            = 48
STEER_EDGE_PADDING     = 48 − 76/2            = 10
HORN_EDGE_PADDING      = 48 − 48/2            = 24
STEER_BOTTOM_PADDING   = 24 + 64 + 5          = 93
HORN_BOTTOM_PADDING    = 93 + 76 + 24         = 193
```

### Çakışma kontrolü — 320 dp genişlik

| Buton | x aralığı | y aralığı (alttan) |
|---|---|---|
| Sol fren | 16 – 80 | 24 – 88 |
| Sol yön | 10 – 86 | 93 – 169 |
| Sağ boost | 240 – 304 | 24 – 88 |
| Sağ yön | 234 – 310 | 93 – 169 |
| Korna | 248 – 296 | 193 – 241 |

- Sol yön ↔ sağ yön arası boşluk: **148 dp** → çakışma yok ✅
- Sol pedal ↔ sağ pedal arası: **160 dp** → çakışma yok ✅
- Dikeyde pedal↔yön 5 dp, yön↔korna 24 dp → çakışma yok ✅
- Ekran dışına taşma: yön butonu kenardan 10 dp içeride → taşma yok ✅

**Sonuç: kontroller çakışmıyor, taşmıyor, 320 dp'de bile sığıyor.
Bu şüphede sorun yok.** Merkez hizalaması (`STEER_EDGE_PADDING`) doğru
kurulmuş — 12 dp'lik boyut farkı sütunu kaydırmıyor.

### B7 — ama yön tuşu sistem jest şeridine girdi

`STEER_EDGE_PADDING = 10 dp`. Android'in jest navigasyonunda geri-jesti
şeridi her iki kenarda tipik olarak ~20 dp. Depoda hiçbir yerde
`systemGestureExclusion` çağrısı yok (arandı, sonuç boş).

| | Kenardan | Jest şeridiyle örtüşen genişlik |
|---|---|---|
| Dün (64 dp, padding 16) | 16 dp | ~4 dp |
| Bugün (76 dp, padding 10) | 10 dp | ~10 dp |

Buton büyüdü ama **kenara doğru da büyüdü** — merkez hizalaması bunu
kaçınılmaz kıldı. Yön tuşunun en dış ~10 dp'sinde başlayan yatay bir kaydırma
sistem "geri"sini tetikleyebilir; oyun sırasında bu **oyundan çıkmak**
demektir.

Bunun bugünkü değişiklikle *yaratıldığını* iddia etmiyorum (16 dp de şeridin
içindeydi) — **kötüleştiğini** iddia ediyorum, %150 kadar.

**Öneri:** `DrivingControls` içindeki dört butona
`Modifier.systemGestureExclusion()` ekle. Tek satırlık, davranışı
değiştirmeyen, geri alınabilir bir düzeltme. Cihazda (S8, jest navigasyonu
açıksa) doğrulanabilir: yön tuşunun sol kenarından sağa doğru kaydır, geri
tetikleniyor mu.

### B8 — duraklat 48 dp + hedef listesi, 320 dp'de dar

- Duraklat: `TopCenter`, 48 dp → 320 dp'de x = **136 – 184**
- Hedef kontrol listesi: `TopEnd`, `padding(end = 12.dp)`, 13 sp metin

| | Duraklatın sağ kenarı | Hedef metninin tahmini sol kenarı | Pay |
|---|---|---|---|
| Dün (36 dp) | 178 | ~196 | ~18 dp |
| Bugün (48 dp) | 184 | ~196 | **~12 dp** |

**Bu bir tahmindir**, ölçüm değil: metin genişliğini 13 sp'de ~7,2 dp/karakter
varsayarak hesapladım. Türkçe en uzun etiket (`○ 30 ARAÇ GEÇ`, 13 karakter)
güvenli görünüyor; **İngilizce karşılığı** (`○ PASS 30 VEHICLES`, 18 karakter
≈ 130 dp) sol kenarı ~178'e taşır ve **duraklat tuşuyla çakışır**.

İki değişiklik yok burada, tek değişiklik var — ama etkisi başka bir ekibin
alanına (hedef listesi, 15 Ağustos'ta eklendi) taşıyor, o yüzden yazıyorum.

**Öneri:** cihazda İngilizce dilde, 5. bölümde (`○ PASS 30 VEHICLES`) bir
ekran görüntüsü al. Çakışıyorsa duraklatı `TopCenter` yerine `TopStart`'a,
skorun soluna al — orada boş yer var.

---

## 10. `speedRampScale` uygulanırsa ne bozulur

Henüz uygulanmadı. `DIFFICULTY_REVIEW.md` §6.6–6.7 önerisi: yalnızca
**1–7. bölümlere** çarpan, 8–30 varsayılan `1f`.

### Etkilenen mevcut hedefler (belgenin ölçüm/model tablosu)

| Bölüm | Hedef | Bugün | σ ile | Marj |
|---|---|---|---|---|
| 3 | `ScoreAtLeast(1400)` | 20,1 s'de tutuyor | 21,5 s'de tutuyor (limit 35 s) | +%63 |
| 3 | `BoostDistance(200)` | tutuyor | ~385 m | +%92 |
| 4 | `ScoreAtLeast(1800)` | 3635 | 3280 (−%10) | +%82 |
| 5 | `ScoreAtLeast(2500)` | 3835 | 3605 (−%6) | +%44 |
| 7 | `ScoreAtLeast(2900)` | 4270 | 4126 (−%3) | +%42 |
| 1, 2, 6 | — | — | etkilenmez | — |
| 8–30 | hepsi | — | bit bit değişmez | — |

### Süre ve mesafe hedefleri — özellikle sorulan kısım

**Mesafe hedefli bölümlerin hiçbiri etkilenmiyor.** Katalogdaki
`ReachDistance` bölümleri: **8, 10, 12, 15, 18, 21, 24, 27, 30** — hepsi
σ = 1.00 bölgesinde. `FinishUnderSeconds` taşıyan bölümler de aynı küme
(artı 12). **Öneri hiçbir mesafe/süre hedefine dokunmuyor.** ✅

Karşılaştırma: belgede reddedilen **küresel** −%6 hız kesme alternatifi
18, 21, 24, 27, 30'u bozuyordu. Bölüm bazlı öneri bunu tam da bu yüzden
tercih etmiş. Doğru tercih.

### Bugünkü değişikliklerle çapraz etkisi

1. **Ekonomiyle:** 3, 4, 5, 7. bölümlerde skor %3–10 düşüyor. Skor hem coin
   (`/70`) hem XP (`/10`) besliyor. 4. bölümde: −355 skor → **−5 coin**,
   **−35 XP**. Bölüm başı 93 coinin %5'i. **Zararsız.**
2. **Kilit kuralıyla:** σ hiçbir bölümde 2. hedefi bozmuyor (en dar marj
   %42, üstelik o hedefler zaten TEMKİNLİ profilin tuttuğu hedefler). 6. ve
   7. bölümün duvarı **dodge hedefinden** kaynaklanıyor ve dodge sayısı hıza
   bağlı değil (`DIFFICULTY_REVIEW.md` §6.5) — yani **σ B3'ü çözmüyor da
   bozmuyor da.** İki iş birbirinden bağımsız.
3. **Reklamla:** σ ile bölümler daha yavaş oynanıyor ama `SurviveTime`
   bölümleri saatle bitiyor, süre değişmiyor. Reklam sayacı koşu sayısına
   baktığı için **etkilenmez.**

**Sonuç: `speedRampScale` bugünkü değişikliklerin hiçbiriyle çatışmıyor.**
Ayrı bir turda çıkarılabilir. Tek şart, belgenin kendi dediği: B3 (hedef
sırası) ile aynı anda çıkmasın, yoksa hangisinin işe yaradığı ölçülemez.

---

## 11. B9 — Geride kalan yalan yorumlar

Kilit değişikliği yorumları üç yerde güncelledi, **iki yerde unuttu**:

| Dosya:satır | Metin | Durum |
|---|---|---|
| `data/GameStateRepository.kt:264` | "`passed` yalnizca **TUM gorevler** tamamlandiysa true gelir" | ❌ yanlış |
| `ui/KronViewModel.kt:172` | "bir sonraki bolumun kilidi ancak **TUM gorevler** tamamlandiysa acilir" | ❌ yanlış |

`MIN_STARS_TO_PASS` sabitinin başına yazılan tarihçe notu aynen şunu diyor:

> "Eski kural **uc ayri yerde yorum olarak kaldi** ve testler de eski kurali
> dogruladigi icin kimse yakalamadi."

Aynı değişiklik, aynı hatayı iki yerde tekrarlıyor. Commit edilmeden
düzeltilmeli.

### Ek: sonuç ekranı metni teknik olarak yanıltıcı

`GameScreen.kt:1786` artık şunu yazıyor:

```
tr = "Bölümü geçmek için ${GameConfig.MIN_STARS_TO_PASS} görev gerekli."
→ "Bölümü geçmek için 2 görev gerekli."
```

Ama yıldızlar **sıralı**. Gereken "herhangi 2 görev" değil, **ilk 2 görev**.
1. ve 3. hedefi tutturan oyuncu 1 yıldız alıyor ve mesajı okuyunca haklı
olarak "ama ben 2 görev yaptım" diyecek.

Sayıdan türetme kararı doğru; ifade eksik. Öneri:
`"Bölümü geçmek için ilk 2 görevi tamamlaman gerekli."` /
`"Complete the first 2 objectives to pass."`

---

## 12. Kontrol ettim, sorun yok

Aşağıdakiler şüpheli görünüyordu ama incelendi ve **zararsız**:

1. **`TIER_REWARDS` göç riski.** `GameStateRepository` `DAILY_TIER`
   anahtarında **kademe sayısını** saklıyor, coin miktarını değil; ödül her
   seferinde diziden yeniden hesaplanıyor. Bugün 2 kademe almış oyuncu 3.'yü
   yeni fiyattan alır. Çift ödeme, negatif bakiye veya çökme yok. ✅
2. **`SCORE_PER_BONUS_COIN` göç riski.** Yalnızca ileriye dönük, koşu
   bitiminde hesaplanıyor. Bankadaki coine dokunmuyor. ✅
3. **Beety'nin fiyat merdivenini bozması.** `CarCatalog.shapes` listesi
   0 · 900 · 1500 · 1500 · 1800 · 2400 · 3200 · **4000** — kesin artan.
   Fiyat merdiveni testi kırılmaz. `requiredCarLevel = 6` geri gitmiyor. ✅
4. **Beety'nin denge dominasyonu.** Eksen toplamı 4,18 (Süper Araba 4,16).
   En düşük son hız (0,92; sonraki en düşük Kuş SLX 0,97) gerçek bir bedel.
   Hiçbir gövdeyi domine etmiyor, edilmiyor. Yorumdaki analiz doğru. ✅
5. **Gün Sarısı'nın fabrika boyası olmaması.** `defaultColorId` yazılmamış →
   Beety kırmızı başlıyor. 4000'lik gövdeye 700'lük boya hediye edilmiyor. ✅
6. **Yön tuşu ikonu 30 → 36 dp.** Buton 76 dp, ikon 36 dp → oran 0,47.
   Dünkü oran 30/64 = 0,47. **Orantı korunmuş**, ikon büyümedi de küçülmedi
   de. ✅
7. **`STEER_BOTTOM_PADDING` ve `HORN_BOTTOM_PADDING` türetmesi.**
   `PEDAL_SIZE` üzerinden hesaplanıyor, `STEER_SIZE` değişince kaymıyor.
   Korna 24 dp'lik kendi boşluğuyla ayrılmış — istenen buydu. ✅
8. **Reklam eşiğinin sonsuz modla karışması.** `ENDLESS` ayrı sayaç
   (`endlessRunsSince`) ve ayrı eşik (3) kullanıyor; muafiyet
   (`levelId <= 4`) `ENDLESS` dalına hiç girmiyor. ✅
9. **`INTERSTITIAL_MIN_RUN_SECONDS = 10` ile `MIN_PAID_RUN_SECONDS`
   çakışması.** İkisi de 10 s ve aynı yönde çalışıyor: 10 s'nin altındaki
   koşu ne coin ödüyor ne reklam sayıyor. Tutarlı, oyuncu için sürprizsiz. ✅
10. **`speedRampScale` mesafe hedeflerini bozar mı** → hayır, mesafe
    bölümlerinin hepsi σ = 1.00 bölgesinde (bölüm 10). ✅
11. **`recordLevelResult`'ın yıldızı düşürmesi.** `maxOf(eski, yeni)` —
    3 yıldızlı bölümü sonra 1 yıldızla oynamak kaydı bozmuyor. ✅
12. **Kontrol butonlarının 320 dp'de çakışması/taşması** → hesaplandı,
    çakışma yok (bölüm 9). ✅

---

## 13. Yapılması önerilenler — sıra

| Sıra | İş | Boyut | Gerekçe |
|---|---|---|---|
| 1 | **B1** — `AdFrequency.shouldShow` muafiyetini `levelId in 1..FREE_LEVELS` yap + `-1` test vakası ekle | 2 satır | Şu an günlük görev reklam geliri **sıfır** |
| 2 | **B9** — iki yalan yorumu düzelt + sonuç ekranı metnini "ilk 2 görev" yap | 3 satır | Commit edilmeden |
| 3 | **B3/B5** — `LevelCurveTest`'i çalıştır, 6/7/8. bölüm çıktısını gör | test koşusu | Kilit değişikliğini commit etmeden önce kanıt |
| 4 | **B2** — `INTERSTITIAL_FREE_LEVELS` 4 → 8 (köprü çözüm) | 1 sabit | 6. bölüm duvarı düzelene kadar |
| 5 | **B6** — `LevelCatalogTest` assert'ini `take(2)`'ye genişlet | 3 satır | Kırılacak; kırılması istenen |
| 6 | **B3** — 6. ve 7. bölümün hedef sırası (Değişiklik B) | tasarım | Asıl çözüm; oynanış ajanının işi |
| 7 | **B4** — `ECONOMY_BALANCE_PROPOSAL.md` §2.3/§7/§8'i yeni kilit kuralıyla düzelt | belge | Yanlış kalırsa gelecekteki her denge kararını zehirler |
| 8 | **B7** — dört kontrol butonuna `systemGestureExclusion()` | 1 satır | Cihazda doğrulanabilir |
| 9 | **B8** — 320 dp / İngilizce ekran görüntüsü | doğrulama | Tahmin, ölçüm değil |
| 10 | **B5** — Beety'nin sink etkisi: karar (bir şey yapma / reklam kolunu masadan al) | karar | Sahibinde |

---

## 14. Ne yapamadım

- **Testleri çalıştıramadım** (kapsam dışı). 6. ve 7. bölüm için "kırılır"
  demiyorum; "aynı harness'in ölçümü 1 yıldız veriyor, eşik 2, dolayısıyla
  risk altında" diyorum.
- **Metin genişliği ölçemedim** (B8). 7,2 dp/karakter tahmini; cihaz
  ekran görüntüsü gerek.
- **Sistem jest şeridi genişliği cihaza göre değişir** (B7). ~20 dp tipik
  değer; S8'de gerçek değer ölçülmedi.
- **9–30. bölümler için hiç ölçüm yok.** `LevelCurveTest` sadece ilk 8'i
  oynatıyor. B3'teki 16 duvarın 14'ü **katalog yapısından** çıkarıldı,
  ölçümden değil.
- **Oyuncu davranışı bilinmiyor.** "Kaç oyuncu 3. yıldız için geri döner"
  (B4) ve "kaç oyuncu Beety'yi hedefler" (B5) sorularının cevabı yok; ikisi
  de o bölümlerin sonuç tablolarını doğrudan etkiliyor.
