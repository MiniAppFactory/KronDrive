# Kron Drive — Ekonomi Denge Önerisi

**Tarih:** 2026-08-16 · **Durum:** öneri, uygulanmadı · **Karar:** proje sahibinde

Bu belge bir **tasarım önerisidir, bir gerçek değil.** Oyun yayınlanmadı,
oyuncu davranışına dair hiçbir verimiz yok. Aşağıdaki "gün" ve "koşu"
sayıları oyuncu davranışı hakkında **varsayımlarla** kurulmuş modellerdir;
mekanizma argümanları (şu değişiklik şu döngüyü şöyle etkiler) güvenilir,
zaman tahminleri değildir. Hiçbir yerde "retention %X artar" tipi bir sayı
yok, çünkü onu ölçemeyiz.

---

## 0. Özet

| İddia | Sonuç |
|---|---|
| 1 — Pasif gelir oynanış gelirini eziyor | **DOĞRULANDI**, ama "pasif" kelimesi yanlış: günlük görev oynanır. Asıl sorun **coin/dakika** oranı. |
| 2 — Seviye kapısı ekonomiyle bağlamıyor | **DOĞRULANDI** ve iddia edildiğinden daha kötü: kapı her seferinde tutarlı olarak **~2.5 kat erken** açılıyor ve 9. bölümde tamamen dekoratifleşiyor. |
| 3 — Yükseltmeler araçlara göre orantısız pahalı | **SAYILAR DOĞRU, TEŞHİS KISMEN YANLIŞ.** 28.000 / 11.300 oranı `docs/BALANCE.md`'de bilinçli bir karar. Gerçek sorun oran değil, **ilk yükseltme adımının görünmez olması**. |

**Önerilen ilk hamle:** `DailyChallengeGenerator.TIER_REWARDS`
`120/260/520` → `80/140/280`. Tek satır, üç sayı, sıfır göç riski.

**Toplam öneri:** 3 sabit (5 sayı). Ayrıntı için bölüm 8.

---

## 1. Yöntem ve güven düzeyi

Bu belgeyi yazarken **hiçbir kod değiştirilmedi, hiçbir build çalıştırılmadı**
(başka bir ajan aynı depoda Gradle kullanıyordu).

| Bilgi türü | Nasıl elde edildi | Güven |
|---|---|---|
| Sabitler ve formüller | Kaynak dosyalar okundu, satır numarası verildi | **Kesin** |
| Skor eğrisi | `docs/BALANCE.md` §"Skor eğrisi" (45 s → ~3.000 puan vb.) | Yüksek — projenin kendi ölçümü |
| Koşu başına toplanan coin | `LevelCatalog` yıldız eşiklerinden geriye türetildi (eşikler beklenenin %75–85'i olarak seçilmiş, `BALANCE.md` §"Skor eğrisi") | **Orta — ±%25** |
| Gün/koşu tahminleri | Yukarıdakilerin üstüne oyuncu davranışı varsayımı | **Düşük — model** |

### Bu belgeyi ölçüme çevirmenin yolu

`LevelCurveTest` içinde zaten bir `olcum dokumu` testi var
(`source/app/src/test/java/com/miniappfactory/krondrive/game/LevelCurveTest.kt:218`)
ve ilk 8 bölümü gerçekten oynayıp `skor / coin / geçiş / süre` değerlerini
`println` ile yazdırıyor. Aşağıdaki tahminler yerine gerçek sayıları koymak
için tek gereken:

```
cd source
./gradlew :app:testDebugUnitTest --tests '*LevelCurveTest*' --offline --info
```

çıktısındaki `bolum N TEMKINLI: ... skor=... coin=...` satırları. **Bu
belgedeki 3. ve 7. bölümlerin sayıları bu ölçümle değiştirilmelidir** —
o zaman öneri tahmin değil ölçüm üstüne oturur. (Kapsam gereği çalıştırılmadı.)

---

## 2. Mevcut durumun röntgeni — koddaki gerçek sabitler

### 2.1 Coin kaynakları

| Kaynak | Miktar | Dosya:satır |
|---|---|---|
| Başlangıç bakiyesi | 100 | `data/PlayerProgress.kt:110` |
| Yoldan toplanan coin | 1 coin (+35 skor) | `game/GameConfig.kt:389`, `:293` |
| Skor bonusu | `skor / 120` coin | `game/GameConfig.kt:390` |
| Yıldız (yalnızca **yeni** yıldız) | 25 coin | `game/GameConfig.kt:391` |
| Günlük görev, 3 kademe | **120 + 260 + 520 = 900/gün** | `data/DailyChallengeGenerator.kt:86` |
| Haftalık görev, 5 görev × 3 kademe | 5 × (40+60+100) = **900/hafta** | `data/WeeklyMissionGenerator.kt:25-55` |
| Haftalık sandık | **750** + 1 booster | `data/WeeklyMissionGenerator.kt:13` |
| Ödüllü reklam | 150 × 5/gün = **750/gün** | `game/GameConfig.kt:493`, `:501` |
| Çift Ödül booster'ı | koşu coin'i ×2 | `game/GameConfig.kt:423` |

**Koşu ödülü formülü** (`game/GameEngine.kt:771-779`):

```kotlin
coinsEarned = coinsCollected * 1
            + score / 120
            + newStars * 25
if (DOUBLE_REWARD) coinsEarned *= 2
if (timeSurvivedSec < 10) coinsEarned = 0
```

`newStars = stars - previousStars` (`GameEngine.kt:770`) — aynı bölümü tekrar
oynamak yıldız coin'i **ödemez**. Bu bilinçli bir farm engeli ve doğru
çalışıyor.

**XP formülü** (`game/GameEngine.kt:780-781`):

```kotlin
xpEarned = score / 10 + stars * 20
```

Araç seviyesi: `1 + xp / 500` (`GameConfig.kt:396`, `PlayerProgress.kt:116`).

### 2.2 Coin harcama noktaları (sink)

| Sink | Toplam | Dosya:satır |
|---|---|---|
| Yükseltmeler | 4 dal × 7.000 = **28.000** | `game/UpgradeCatalog.kt:41`, `:44-45` |
| Araçlar (6 ücretli) | **11.300** | `game/CarCatalog.kt:584,649,705,775,881,983` |
| Boyalar (9 ücretli) | 9.050 → fabrika boyaları düşülünce ~**7.150** | `game/CarCatalog.kt:1157-1245` |
| Booster'lar | 150 / 250 / 300 / 400, tüketilir | `data/PlayerProgress.kt:9-20` |
| **Kalıcı toplam** | **~46.450 coin** | |

Yükseltme maliyeti `250 × mevcutSeviye`, 1→8 arası
`250+500+750+1000+1250+1500+1750 = 7.000` (dal başına). Araç fiyatları:
Şehir 0 · Yarış Sedan 900 · Kuş SLX 1.500 · Dağ Keçisi 1.500 · Kas Arabası
1.800 · Boğa 67 2.400 · Süper Araba 3.200 → **11.300**. Görevdeki her iki
sayı da doğru.

### 2.3 İlerleme kapıları

Bir bölüm ancak **üç görevin üçü de** tamamlanınca geçilir
(`GameEngine.kt:796-797`: `passed = stars == level.stars.size`). Yani her
ilk geçiş **her zaman** 3 yıldız = 75 coin öder. Erken oyunda coin gelirinin
büyük kısmı budur — bu, bölüm 8'deki önerilerin neden erken oyunu az
etkilediğini açıklıyor.

### 2.4 Yol boyunca bulunan tutarsızlıklar (öneriden bağımsız)

`docs/BALANCE.md` üç yerde **kodla uyumsuz** — okuyan herkesi yanıltıyor:

| BALANCE.md diyor | Kod diyor | Satır |
|---|---|---|
| "Günlük görev 400–500 coin" (satır 203) | 900 | `DailyChallengeGenerator.kt:86` |
| "Geçiş reklamı: her 2 tamamlanan bölümde bir" (satır 221) | 3 koşuda bir + ilk 4 bölüm reklamsız | `GameConfig.kt:454`, `:480` |
| Yükseltme tablosu doğrusal ("hız tavanı +0.16/seviye") (satır 186) | Dışbükey eğri, `CURVE_EXPONENT = 1.5` | `UpgradeCatalog.kt:64` |

İlginç not: `BALANCE.md`'nin yazdığı **400–500 coin, bu belgenin bağımsız
olarak vardığı öneriyle (500) aynı.** Yani günlük görev bir noktada 900'e
çıkarılmış ve belge güncellenmemiş; öneri aslında **belgelenmiş niyete geri
dönüş**.

---

## 3. İddia 1 — "Pasif gelir oynanış gelirini eziyor"

### Sonuç: DOĞRULANDI, ama daha keskin biçimde ifade edilmeli

Önce iddianın sayılarını kontrol edelim:

- Günlük görev **900** ✓ (`120+260+520`, `DailyChallengeGenerator.kt:86`)
- Ödüllü reklam **750/gün** ✓ (`150 × 5`, `GameConfig.kt:493`, `:501`)
- Toplam **1.650/gün** ✓
- "Bir bölümü bitirmek ~95 coin" ✓ (aşağıdaki tabloda 90–127 arası, ortalama ~110)

### Bir düzeltme: günlük görev pasif değildir

Günlük görev **oynanır** — `RunMode.DAILY`'de bir koşu gerekir
(`ui/KronViewModel.kt:190-199`). Gerçekten pasif olan tek kaynak **ödüllü
reklamdır** (garajdaki "coin kazan" düğmesi, oynamayı hiç gerektirmez).

Bu, iddianın özünü çürütmez ama doğru ölçüye götürür: sorun "pasif vs aktif"
değil, **coin/dakika**.

### Coin/dakika tablosu (asıl bulgu)

| Etkinlik | Coin | Süre | **Coin/dakika** |
|---|---|---|---|
| Günlük görev, 3 kademe | 900 | ~3 dk koşu | **300** |
| Günlük görev, kademe 1–2 (yeni oyuncu) | 380 | ~1 dk | **380** |
| Ödüllü reklam ×5 | 750 | ~3 dk | **250** |
| Kariyer bölümü, ilk geçiş | ~110 | ~50 sn | **130** |
| Kariyer bölümü, tekrar oynama | ~38 | ~50 sn | **45** |
| Sonsuz mod, ortalama oyuncu | ~38–83 | 45–90 sn | **~50** |

**Oyunun en yüksek coin/dakika oranı, oyunun kendisi değil, günün ilk
koşusudur.** Bir yeni oyuncu için günlük görevin ilk iki kademesi
(1 dakikada 380 coin) oyunun geri kalanının **8 katı** verimlidir.

### En sert somut karşılaştırma

30 bölümlük kariyerin **tamamı**, her biri ilk kez geçilerek:

| Bileşen | Coin |
|---|---|
| Yıldız coin'i (30 bölüm × 3 × 25) | 2.250 |
| Toplanan coin + skor bonusu (30 bölüm) | ~1.800 |
| **Toplam** | **~4.050** |

- Bu, günlük 1.650'lik tavanın **2.5 gününe** eşit.
- 28.000 coinlik yükseltme hattının **%14'ü**.
- 46.450 coinlik toplam sink'in **%8.7'si**.

Yani: *oyuncu tüm oyunu bitirdiğinde, iki buçuk gün uygulamayı açıp
düğmeye basmakla aynı parayı kazanmış olur.* İddia 1'in mekanizması budur ve
doğrudur.

### Bir de bunu ekleyelim: günlük koşu XP'yi de eziyor

Günlük görev hedefi `SurviveTime(180)` (`DailyChallengeGenerator.kt:91`).
180 saniye dayanan bir oyuncunun skoru `BALANCE.md` eğrisiyle ~13.800 →
**xp = 1.379 tek koşuda**. Bu, 2.75 araç seviyesi demek (500 XP/seviye).
Aynı koşu ayrıca ~166 koşu coin'i + 900 kademe coin'i öder.

**Tek bir 3 dakikalık günlük koşu ≈ 1.070 coin + 1.379 XP.** Karşılaştırma:
9 kariyer bölümünü ilk kez geçmek ~1.070 coin + 2.825 XP verir. Yani bir
koşu, dokuz bölümün coin'ini ödüyor.

---

## 4. İddia 2 — "Seviye kapısı ekonomiyle bağlamıyor"

### Sonuç: DOĞRULANDI, ve iddia edildiğinden daha kötü

Kariyer bölümlerini ilk kez geçerken biriken XP ve coin
(skor eğrisi `BALANCE.md`'den, coin toplama yıldız eşiklerinden türetildi):

| Geçilen bölüm | Kümülatif XP | Araç seviyesi | Kümülatif coin (+100 başlangıç) |
|---|---|---|---|
| 3 | ~570 | **2** | 382 |
| 5 | ~1.220 | 3 | 500 |
| 6 | ~1.570 | **4** | 712 |
| 8 | ~2.335 | **5** | 1.056 |
| 9 | ~2.825 | **6** | 1.169 |

Şimdi her kapıyı, o kapının açtığı aracın fiyatıyla yan yana koyalım:

| Kapı | Kaç geçişte açılır | O andaki coin | Aracın fiyatı | **Fark** |
|---|---|---|---|---|
| Sv. 2 → Yarış Sedan | 3 | 382 | 900 | **2.4×** |
| Sv. 2 → Kuş SLX / Dağ Keçisi | 3 | 382 | 1.500 | **3.9×** |
| Sv. 4 → Kas Arabası | 6 | 712 | 1.800 | **2.5×** |
| Sv. 5 → Boğa 67 | 8 | 1.056 | 2.400 | **2.3×** |
| Sv. 6 → Süper Araba | 9 | 1.169 | 3.200 | **2.7×** |

İddianın "3 koşuda seviye 2, ama araç 1500 coin" tespiti doğru. Ama tabloda
görülen şey daha kullanışlı: **fark rastgele değil, her kapıda tutarlı
olarak ~2.5 kat.** Seviye kapısı gereken paranın yaklaşık **%40'ında**
açılıyor.

Bu iyi haber: tutarlı bir sapma **tek bir çarpanla** düzeltilebilir
(bkz. öneri 3).

### Daha ağır bulgu: kapı 9. bölümde tamamen ölüyor

Oyundaki **en yüksek seviye şartı 6'dır** (Süper Araba,
`CarCatalog.kt:984`). Oyuncu bunu **9. bölümde** — 30 bölümlük kariyerin
%30'unda — geçiyor. O andan sonra `requiredCarLevel` alanı hiçbir şeyi
kapatmıyor; kataloğun 17 girdisindeki seviye şartı **dekoratif** hale
geliyor.

Ve bölüm 3.4'te gösterildiği gibi, tek bir iyi günlük koşu 1.379 XP veriyor
— yani **iki günlük koşu** bile oyuncuyu seviye 6'ya taşıyor, hiç kariyer
oynamadan.

Yorum: XP ve coin **aynı sinyali** (skor) farklı sabitlerle ölçüyor
(`xp = skor/10`, `coin ⊃ skor/120`). Aynı şeyi ölçen iki kapı, ikisinden
biri gereksiz demektir. Doğru karar ya (a) çarpanı hizalamak ya da (b)
`requiredCarLevel`'ı tamamen kaldırıp coin'i tek kapı yapmaktır. (b) daha
temiz ama 17 katalog girdisine dokunmayı gerektirir; bu belge minimum
değişiklik istendiği için (a)'yı öneriyor.

---

## 5. İddia 3 — "Yükseltmeler araçlara göre orantısız pahalı"

### Sonuç: SAYILAR DOĞRU, TEŞHİS KISMEN YANLIŞ

Sayılar doğrulandı: yükseltmeler **28.000**, araçlar **11.300**, oran 2.5:1.

Ama bu bir hata değil, **belgelenmiş bir karar.** `docs/BALANCE.md:291-293`:

> "Çarpanlar yükseltmelerin ÜSTÜNE uygulanır, onların yerine geçmez. Fark
> ~%10 bandında kalır: **ana ilerleme garaj yükseltmeleri olmalı**, yoksa
> 8 seviyelik dört dal anlamsızlaşır."

Yani yükseltmelerin en büyük sink olması tasarımın kendisi. Araçlar
karakter katıyor, ilerlemeyi taşımıyor. Bu tutarlı bir tasarım ve
değiştirilmesi için bir gerekçe göremedim. **Bu iddiaya dayanarak fiyat
değiştirmeyi önermiyorum.**

### Aynı yerde bulunan gerçek sorun: ilk yükseltme adımı görünmez

Maliyet eğrisi **doğrusal** (`250 × seviye`), etki eğrisi **dışbükey**
(`t^1.5`, `UpgradeCatalog.kt:64-69`). En kötü kombinasyon: en ucuz adım
aynı zamanda en az şey veren adım.

`curve(2) = (1/7)^1.5 = 0.054`. HIZ dalı için garajda gösterilen değer
(`UpgradeCatalog.kt:143-144`):

| Seviye | Maliyet | Garajda görünen | Adım |
|---|---|---|---|
| 1 | — | 181 km/h | — |
| 2 | 250 | 182 km/h | **+1** |
| 3 | 500 | 186 km/h | +4 |
| 4 | 750 | 190 km/h | +4 |
| 8 | 1.750 | 216 km/h | +7 |

**Oyuncunun oyundaki ilk satın alması 250 coin karşılığı +1 km/h.** Kod
yorumları bu sorunla daha önce boğuşulduğunu gösteriyor (`CURVE_EXPONENT`
2.0'dan 1.5'e çekilmiş, `UpgradeCatalog.kt:60-62`) ama 1. adımda hâlâ
görünmüyor.

Bu **gerçek bir sorun**, ama iddia 1 ve 2'den daha küçük ve çözümü
maliyet/etki eğrilerinden birine dokunmayı gerektiriyor — minimum değişiklik
bütçesinin dışında. **Kayda geçiriyorum, bu turda önermiyorum.**

---

## 6. İkincil bulgular (istenmedi ama ekonomiyi etkiliyor)

### 6.1 Çift Ödül booster'ı matematiksel olarak hiç kârlı olamaz

`DOUBLE_REWARD` 300 coin (`PlayerProgress.kt:18`), koşu coin'ini ikiye
katlıyor (`GameConfig.kt:423`, `GameEngine.kt:774-776`). Kârlı olması için
koşunun **300'den fazla** coin ödemesi gerekir.

En iyi gerçekçi koşu (180 sn sonsuz mod, ~13.800 skor):
`51 + 13.800/120 = 166 coin`. İkiye katlanınca kazanç **+166**, maliyet 300.
**Net −134.**

Yani bu ürün, oyunda satılan ve **her koşuda zarar ettiren** bir üründür.
Skor/coin oranı düzeltilmedikçe (bkz. öneri 2) hiçbir oyuncu senaryosunda
kâra geçmez. Ayrı bir iş olarak ele alınmalı: ya fiyat ~120'ye inmeli ya da
etkisi genişlemeli.

### 6.2 Başlangıç bakiyesi hiçbir şeye yetmiyor

`STARTING_COINS = 100` (`PlayerProgress.kt:110`), oyundaki **en ucuz şey**
250 coinlik ilk yükseltme. Oyuncunun garajla ilk teması "hiçbir şey
alamazsın" oluyor. 100 → 250 tek sabitlik bir onboarding düzeltmesi olurdu;
ama 5.1'deki bulgu yüzünden o ilk satın alma **+1 km/h** verecek, yani ilk
izlenim yine kötü. İkisi birlikte çözülmeli veya hiçbiri.

---

## 7. İlerleme eğrisi — reklam izleyen ve izlemeyen oyuncu

> **Uyarı:** buradaki her sayı bir modeldir. Varsayımlar açıkça yazıldı;
> varsayımı değiştirin, sonuç değişir.

**Varsayımlar:** günde ~15 dakika oyun; kariyer fazında günde ~20 koşu
(geçiş + tekrar denemeler); günde 1 günlük görev koşusu; reklam izleyen
oyuncu 5 ödüllü reklamın hepsini alıyor; haftalık gelir (900 + 750 = 1.650)
güne bölünmüş (~236/gün).

### Faz 1 — kariyer (yaklaşık 3 gün, ~60 koşu)

| Gelir kalemi | Coin |
|---|---|
| 30 bölüm ilk geçiş | ~4.050 |
| ~30 başarısız/tekrar koşu (yıldız coin'i yok) | ~1.350 |
| Günlük görev × 3 | 2.700 |
| Haftalık (kısmi) | ~900 |
| Başlangıç | 100 |
| **Ara toplam (reklamsız)** | **~9.100** |
| Ödüllü reklam × 3 gün | +2.250 |
| **Ara toplam (reklamlı)** | **~11.350** |

### Faz 2 — kariyer bitti, sadece sonsuz mod / tekrar oynama

Yıldız coin'i tükendi, koşu başına gelir düştü:

| Oyuncu tipi | Günlük gelir | Bileşim |
|---|---|---|
| Reklam + günlük görev | ~2.560 | oynanış %26, günlük %35, reklam %29, haftalık %9 |
| Günlük görev, reklam yok | ~1.810 | oynanış %37, günlük %50, haftalık %13 |
| Sadece oynayan (günlük görev de yok) | ~910 | oynanış %74, haftalık %26 |

### Hedeflere ulaşma süresi

Hedef: **31.200 coin** = dört yükseltme dalı tam (28.000) + Süper Araba (3.200).

| Oyuncu tipi | İlk araca (900) | Süper Araba'ya (3.200) | Tüm yükseltmelere |
|---|---|---|---|
| Reklam + günlük | ~8 koşu (1. gün) | 2. gün | **~11 gün** |
| Günlük, reklam yok | ~8 koşu (2. gün) | 3. gün | **~15 gün** |
| Sadece oynayan | ~8 koşu | 6.–7. gün | **~31 gün** |

**Dikkat edilecek üç şey:**

1. **İlk araç her üç oyuncuda da ~8 kariyer geçişi.** Çünkü erken oyunda
   gelirin %70'i yıldız coin'idir (bölüm 2.3) ve o herkes için aynıdır.
   Yani erken oyun aslında **dengeli**; sorun kariyerden sonra başlıyor.
2. **Sadece oynayan oyuncu, reklam+günlük oyuncusunun 2.8 katı sürede
   bitiriyor.** Oyunu en çok oynayan, en yavaş ilerleyen kişi.
3. **Reklam izlemenin faydası sadece 15 gün → 11 gün (%27).** Reklam,
   oynanışı ezecek kadar büyük ama oyuncuyu reklam izlemeye ikna edecek
   kadar cazip değil — çünkü günlük görev zaten daha fazlasını veriyor.
   Reklam gelirini artırmak isteyen biri için bu kötü bir konum.

---

## 8. Öneri — 3 sabit, 5 sayı

Tasarım hedefi: **oynanışı ana gelir kaynağı yapmak, pasif geliri destek
konumuna indirmek, seviye kapısını cüzdanla hizalamak** — mümkün olan en az
sabitle.

Reklam sabitlerine (`REWARDED_COIN_AMOUNT`, `REWARDED_COIN_DAILY_LIMIT`)
**bilerek dokunulmuyor** — gerekçe bölüm 9.3'te.

---

### Öneri 1 — Günlük görev ödülünü düşür ⭐ ÖNCE BU

```kotlin
// data/DailyChallengeGenerator.kt:86
private val TIER_REWARDS = intArrayOf(120, 260, 520)   // toplam 900
                        →  intArrayOf( 80, 140, 280)   // toplam 500
```

**Neden 500:** `docs/BALANCE.md:203` günlük görevi zaten **"400–500 coin"**
diye belgeliyor. Kod bir noktada 900'e çıkmış, belge güncellenmemiş. Bu
öneri yeni bir sayı icat etmiyor, **belgelenmiş niyete geri dönüyor**.

**Neden kademeler orantısız değil:** 120/260/520 oranı 1 : 2.2 : 4.3.
80/140/280 oranı 1 : 1.75 : 3.5 — üçüncü kademenin ağırlığı hafifçe azaldı,
çünkü 3. kademe (25 perfect dodge, 8.000 puan) yeni oyuncu için zaten
ulaşılamaz; ödülün oraya yığılması yeni oyuncuya hiçbir şey vermiyordu.

**Tahmini etki:**

| Ölçü | Şimdi | Sonra |
|---|---|---|
| Günlük görev coin/dakika | 300 | **167** |
| Günlük görev, bir kariyer geçişinin kaç katı | 8.2× | **4.5×** |
| Günlük pasif tavan (günlük + reklam) | 1.650 | **1.250** (−%24) |
| Yeni oyuncunun 1 dakikada aldığı (kademe 1–2) | 380 | **220** |

---

### Öneri 2 — Skor→coin dönüşümünü zenginleştir ⭐ SONRA BU

```kotlin
// game/GameConfig.kt:390
const val SCORE_PER_BONUS_COIN = 120  →  70
```

**Neden:** oynanışın **ölçeklenebilen** tek gelir kalemi bu. Toplanan coin
oyuncunun risk almasına bağlı ama tavanlı (1.05 sn'de bir doğuyor,
`GameConfig.kt:249`); yıldız coin'i tek seferlik ve 30 bölümde tükeniyor.
Geriye yalnızca skor bonusu kalıyor ve 1/120'lik oran onu görünmez
kılıyor — 90 saniyelik iyi bir koşu 57 coin veriyor.

**Neden 70 (60 ve 80 değil):** hedef, kariyer bittikten sonra oynanışın
coin/dakikasını günlük görevin **yarısına** çıkarmak — böylece oynamak
günlük görevden hâlâ daha yavaş (günlük bir ödül olarak kalır) ama
**tavansız** olduğu için 20 dakikalık bir seans günlük görevi geçer.

| Bölen | 60 sn koşuda skor coin'i | Koşu toplamı | coin/dk |
|---|---|---|---|
| 120 (şimdi) | 35 | 52 | ~50 |
| 80 | 54 | 71 | ~71 |
| **70** | **61** | **78** | **~78** |
| 60 | 72 | 89 | ~89 |

Günlük görev (öneri 1'den sonra) 167 coin/dk. 70 → oynanış 78 coin/dk =
günlüğün %47'si. 60 aynı oranı %53'e çıkarır ama enflasyon riskini de
büyütür; 70 daha temkinli seçim.

**Tahmini etki:**

| Ölçü | Şimdi | Sonra |
|---|---|---|
| 90 sn sonsuz koşu | 83 coin | **125 coin** |
| Kariyer sonrası günlük oynanış geliri (15 dk) | ~675 | **~945** (+%40) |
| Sadece oynayan oyuncunun günlük geliri | ~910 | **~1.180** (+%30) |
| Yarış Sedan'a ulaşma | ~8 kariyer geçişi | **~7 geçiş** |

**Dürüst uyarı: erken oyunu neredeyse hiç değiştirmiyor.** Kariyer
başında gelirin %70'i yıldız coin'i olduğu için (bölüm 2.3) skor bonusunu
%71 artırmak ilk 8 koşuda ancak bir koşuluk fark yaratıyor. Bu önerinin
etkisi **kariyer bittikten sonra** — yani 28.000 coinlik yükseltme
grindinin yaşadığı yerde.

---

### Öneri 3 — Araç seviyesi eşiğini cüzdanla hizala ⚠️ EN DÜŞÜK GÜVEN

```kotlin
// game/GameConfig.kt:396
const val XP_PER_CAR_LEVEL = 500  →  1250
```

**Neden 1250:** bölüm 4'teki tablo, kapının her seferinde gereken paranın
~%40'ında (yani **2.5 kat erken**) açıldığını gösterdi. Sapma tutarlı
olduğu için tek çarpanla kapanır: `500 × 2.5 = 1250`.

**Tahmini etki:**

| Kapı | Şimdi açılır | Fark | Sonra açılır | Fark |
|---|---|---|---|---|
| Sv. 2 → Yarış Sedan (900) | 3 geçiş, 382 coin | 2.4× | **6 geçiş, 712 coin** | **1.26×** |
| Sv. 4 → Kas Arabası (1.800) | 6 geçiş, 712 coin | 2.5× | **11 geçiş, ~1.440 coin** | **1.25×** |
| Sv. 6 → Süper Araba (3.200) | 9 geçiş, 1.169 coin | 2.7× | **16 geçiş, ~2.160 coin** | **1.48×** |

Kapı artık cüzdandan **hemen önce** açılıyor — "görüyorum, biraz daha
lazım" hissi, ki istenen budur. Ayrıca `requiredCarLevel` 30 bölümün
yarısına kadar canlı kalıyor, 9. bölümde ölmüyor.

**Neden en düşük güven ve neden en sona:**

1. Bu, **kayıtlı oyuncuların seviyesini düşüren tek öneri** (bkz. 9.1) —
   üçü içinde göç riski taşıyan tek değişiklik.
2. Teşhis ("seviye kapısı dekoratif") tartışmaya açık. Hiç kimseyi
   engellemeyen bir kapı zarar vermez, sadece tasarım yüzeyi harcar.
   **Yapmamak da geçerli bir karardır.**
3. Öneri 2 uygulanınca coin daha hızlı biriktiği için sapma zaten
   2.5×'ten ~2.1×'e düşer; 1250 yerine ~1050 yeterli olabilir. Bu yüzden
   **öneri 2'nin etkisi görülmeden bu sayı kesinleştirilmemeli.**

---

### Uygulanmayan, ama masada duran dördüncü kol

```kotlin
// game/GameConfig.kt:493
const val REWARDED_COIN_AMOUNT = 150  →  120     // günlük 750 → 600
```

**Bu turda önermiyorum.** Gerekçe: reklam akışı yeni ayarlandı
(2026-08-16) ve hiç ölçülmedi. Öneri 1 ve 2 uygulandıktan sonra reklamın
**göreli** ağırlığı zaten kendiliğinden düşer (750 coin, %40 zenginleşmiş
bir ekonominin içinde daha küçük bir paydır) — reklam gelirini riske
atmadan aynı sonuca varılır. Ölçüm geldikten sonra tekrar bakılmalı.

---

## 9. Ne bozulabilir

### 9.1 Kayıtlı oyuncular (DataStore'da coin'i olanlar)

| Öneri | Mevcut kayda etkisi | Risk |
|---|---|---|
| 1 — `TIER_REWARDS` | `DAILY_TIER` anahtarı **kademe sayısını** (0..3) saklıyor, coin miktarını değil (`GameStateRepository.kt:600`). `rewardBetween` ödülü her seferinde yeni diziden hesaplıyor (`DailyChallengeGenerator.kt:44-47`). Bugün 2 kademe almış oyuncu 3.'yü **yeni** fiyattan (280) alır. Çift ödeme yok, negatif yok, çökme yok. | **Yok** |
| 2 — `SCORE_PER_BONUS_COIN` | Yalnızca ileriye dönük; koşu bitiminde hesaplanıyor (`GameEngine.kt:772`). Bankadaki coin'e dokunmuyor. Eski oranla kazanmış oyuncu kazandığını korur — küçük ve zararsız bir kalıcı avantaj. | **Yok** |
| 3 — `XP_PER_CAR_LEVEL` | **YIKICI.** `carLevel = 1 + xp/500` (`PlayerProgress.kt:116`). 3.000 XP'li bir oyuncu şu an seviye **7**, değişiklikten sonra seviye **3**. Garajda seviyesi düşmüş görünür. | **Yüksek** |

**Öneri 3'ün göç planı (uygulanacaksa zorunlu):** Satın alınmış araçlar
**kaybolmaz** — sahiplik bir `Set` ve `CarCatalog.isOwned` seviyeye
bakmıyor (`GameStateRepository.kt:415`). Ama **alınmamış** araçlar tekrar
`LEVEL_LOCKED` olur ve `buyCarItem` içindeki `canBuy` kontrolü
(`GameStateRepository.kt:394`) satın almayı engeller. Bu yüzden değişiklik
tek seferlik bir XP migrasyonuyla gelmelidir:

```
yeniXp = eskiXp * 2.5      // hiç kimsenin görünen seviyesi düşmez
```

Bu **kod** demektir, sabit değil — öneri 3'ün neden 3. sırada olduğunun
asıl sebebi budur.

### 9.2 Enflasyon

**Toplam sink değişmiyor** (46.450 coin). Değişen sadece gelirin
bileşimi:

| | Şimdi | Öneri 1+2 sonrası |
|---|---|---|
| Reklam+günlük oyuncusu, kariyer sonrası günlük gelir | ~2.560 | ~2.430 |
| Sadece oynayan oyuncu, günlük gelir | ~910 | ~1.180 |
| Oynanışın gelirdeki payı (reklam+günlük oyuncusu) | %26 | **%39** |
| Tüm yükseltmelere süre — reklam+günlük | ~11 gün | **~11 gün** |
| Tüm yükseltmelere süre — sadece oynayan | ~31 gün | **~23 gün** |
| **Oyuncu tipleri arası fark** | **2.8×** | **2.1×** |

Yani öneri 1+2 **para basmıyor, para taşıyor**: en çok oynayan oyuncunun
geliri artıyor, sadece giriş yapanınki azalıyor, toplam ekonomi ve bitirme
süresi neredeyse sabit kalıyor. Enflasyon riski düşük.

Yan not: 28.000 coinlik yükseltme hattı, 30 bölümlük kariyerden
(~4 saat içerik) çok daha uzun bir rün sunuyor. Süreyi kısaltmak burada
zarar değil, muhtemelen fayda.

### 9.3 Reklam geliri

Öneri 1 ve 2 **hiçbir reklam sabitine dokunmuyor.** Mekanizma üzerinden
beklenen etkiler:

- **Geçiş reklamı: artabilir.** Sayaç koşu **sayısına** bakıyor
  (`GameConfig.kt:454`, `AdFrequency`), koşu değerine değil. Öneri 2 koşu
  başına ödülü artırdığı için oyuncunun daha çok koşu yapması beklenir →
  gösterim sayısı artar.
- **Ödüllü reklam: göreli değeri düşer, mutlak değeri aynı kalır.** 750
  coin, %40 zenginleşmiş bir oynanış ekonomisinin içinde daha küçük bir
  pay. Ama 750 hâlâ 6 kariyer geçişine bedel — cazibesini kaybetmez.
  Günlük 5 limitinin çoğu oyuncu tarafından zaten doldurulmadığı tahmin
  ediliyor (**ölçülmedi**), o yüzden burada kayıp beklemiyorum.
- **Öneri 1 reklamı güçlendiriyor.** Günlük görev 900'den 500'e inince
  ödüllü reklam (750) **günün en büyük tek coin kaynağı** olur. Reklam
  izlemenin cazibesi artar.

### 9.4 Mevcut testler

Öneriler `game/` sabitlerine dokunuyor; şu testler **etkilenebilir** ve
değişiklikle birlikte gözden geçirilmelidir:

- `game/LevelCurveTest.kt` — bölümleri oynuyor, coin/skor **iddia
  etmiyor** (yıldız ve tamamlanma iddia ediyor). Öneri 1 ve 2 yıldızları
  değiştirmediği için **kırılmaması beklenir**, ama doğrulanmalı.
- `data/DailyChallengeGeneratorTest.kt` — ödül miktarını sabit olarak
  kontrol ediyorsa **öneri 1 ile kırılır**; kırılması doğrudur, test
  güncellenir.
- `data/PlayerProgressCarTest.kt` — `carLevelForXp` kullanıyorsa **öneri 3
  ile kırılır**.
- `game/UpgradeCatalogTest.kt` — bu önerilerden etkilenmez (yükseltme
  fiyatlarına dokunulmuyor).

---

## 10. Öncelik sırası

Üçünü aynı anda değiştirmek etkiyi ölçülemez hale getirir. Sıra ve
gerekçe:

### 1. Öneri 1 — `TIER_REWARDS` 120/260/520 → 80/140/280

**Neden önce:** Üç sorunun en büyüğü bu ve tek satırda duruyor. Bir
sayının bir günde 900 ödemesi, oyunun geri kalanında hiçbir şeyin
900 ödememesiyle çelişiyor. Göç riski **sıfır** (bölüm 9.1). Geri alması
tek satır. Ayrıca kendi belgemizin yazdığı değere dönüş, yani yeni bir
tasarım kararı bile sayılmaz.

**Doğrulama:** `DailyChallengeGeneratorTest` güncelle, test paketi yeşil,
cihazda günlük görevi bir kez oyna ve ödenen coin'i logcat'ten doğrula.
**Bir sonraki adıma geçmeden önce bu tek başına oynanmalı** — günlük
görevin artık "günün ödülü" gibi mi yoksa "önemsiz" gibi mi hissettirdiği
sadece elde anlaşılır.

### 2. Öneri 2 — `SCORE_PER_BONUS_COIN` 120 → 70

**Neden sonra:** Öneri 1 pasif tarafı indirdi; bu, aktif tarafı kaldırıyor.
İkisi birlikte "oyna, kazan" döngüsünü kuruyor ama sırayla yapılırsa
hangisinin ne kadarını yaptığı görülebilir. Ayrıca öneri 1'i uygulayıp
oynayınca oynanış gelirinin **ne kadar** düşük hissettiği somut olarak
ölçülür ve 70 sayısı gerekirse revize edilir.

**Doğrulama:** `LevelCurveTest` içindeki `olcum dokumu` testini çalıştır,
`coin=` değerlerini önce/sonra karşılaştır. Cihazda bir sonsuz mod koşusu
yap, sonuç ekranındaki coin'i not et.

### 3. Öneri 3 — `XP_PER_CAR_LEVEL` 500 → 1250

**Neden en sonda ve neden opsiyonel:** Tek göç riski taşıyan öneri
(bölüm 9.1), tek kod (migrasyon) gerektiren öneri, ve teşhisi en
tartışmalı olan öneri. Ayrıca öneri 2 uygulandıktan **sonra** doğru çarpan
2.5 değil ~2.1 olabilir — sayı, öncekiler ölçülmeden kesinleştirilemez.

**Karar noktası:** Proje sahibi "seviye kapısı bir işe yarasın mı, yoksa
kaldırılsın mı" sorusuna cevap vermeden bu değişiklik yapılmamalı.
Kaldırma kararı çıkarsa bu öneri geçersiz olur ve yerine
`requiredCarLevel` alanlarının temizlenmesi gelir.

---

## 11. Kapsam dışı bıraktıklarım

Bunlar **bilerek** önerilmedi; gerekçeleri yukarıda:

- **Araç fiyatları** — 11.300 toplam, `BALANCE.md`'de gerekçeli bir karar
  (bölüm 5).
- **Yükseltme fiyatları** — 28.000 toplam, ana ilerleme hattı olması
  tasarımın kendisi (bölüm 5).
- **Yükseltme etki eğrisi** (`CURVE_EXPONENT`) — ilk adımın görünmezliği
  gerçek bir sorun ama minimum değişiklik bütçesinin dışında (bölüm 5.1).
- **Reklam sabitleri** — yeni ayarlandı, ölçülmedi (bölüm 8, dördüncü kol).
- **Booster fiyatları** — Çift Ödül'ün kârsızlığı ayrı bir iş (bölüm 6.1).
- **`STARTING_COINS`** — tek başına çözülmemeli (bölüm 6.2).
- **Haftalık görevler** — günlüğün yanında küçük (~236/gün) ve gerçekten
  oynanarak kazanılıyor; dokunmaya gerek görmedim.

## 12. Yapılması gereken belge düzeltmesi

Öneriden bağımsız: `docs/BALANCE.md` üç yerde kodla çelişiyor
(bölüm 2.4). Bu belge projede "denge nereden geliyor" sorusunun cevabı
olarak kullanıldığı için, yanlış kalması gelecekteki her denge kararını
zehirler. Öneri hangi sırayla uygulanırsa uygulansın, **BALANCE.md aynı
commit'te güncellenmelidir.**
