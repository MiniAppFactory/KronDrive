# Kron Drive — Kariyer Modu Zorluk İncelemesi

**Tarih:** 2026-08-17
**İnceleyen:** Game Director
**Kapsam:** kariyer modu zorluk eğrisi, hedef tasarımı, hız/ivme modeli
**Kapsam dışı:** performans, coin ekonomisi, görsel tasarım, reklam

> **Bu belge kod değiştirmez.** Hiçbir `.kt` dosyasına dokunulmadı, Gradle
> çalıştırılmadı, cihaza kurulum yapılmadı. Aşağıdaki her sayı ya koddan
> okundu, ya koddaki formüllerden türetildi, ya da elimizdeki `olcum dokumu`
> çıktısına oturtuldu. Hangisi olduğu her yerde belirtildi; **ölçülmemiş
> tahminler açıkça işaretlendi.**

---

## 0. Yönetici özeti

Proje sahibinin iki şikâyeti var. **İkisi de doğru. Ama ikisinin de
sebebi sanılan sabit değil.**

1. **"4. bölümde 3 perfect dodge çok."** Doğru — ve durum daha kötü.
   15 Ağustos'ta değişen kilit kuralı yüzünden bu hedef **zorunlu**: üç
   görevin üçü de yapılmadan 5. bölüm açılmıyor. Perfect dodge ise oyuncudan
   **2–3 kare (33–67 ms)** genişliğinde bir giriş istiyor ve oyun bu mekaniği
   **hiçbir yerde öğretmiyor**. Kariyer modu 4. bölümde bitiyor.

2. **"Standart araba ile 30 sn'de max hıza ulaştım neredeyse."** Doğru —
   hesap **~23 saniye** diyor, yani sahibinin tahmininden de erken. Ama bunun
   sebebi `ACCEL_RATE_BASE` değil, **skor kaynaklı hız rampası**. O sabiti
   düşürmek zorluğu hiç değiştirmez ve sahibinin kendi tasarım fikrini
   (boost'un hız yönetiminin ana aracı olması) **bozar**.

**Kök sebep bir cümle:** 14 Ağustos'ta ilk sekiz bölüm "birinci yıldız kolay
olsun, sonraki bölüm `stars > 0` ile açılır" ilkesine göre tasarlandı.
15 Ağustos'ta kilit kuralı "üç görevin üçü de" oldu. **O gün tasarımın
güvenlik payı sessizce yok oldu ve hiçbir test bunu yakalamadı** — testler
hâlâ eski kuralı doğruluyor.

**Ana öneri iki parça:**
- Yapısal: kilit 2 yıldıza insin, beceri hedefleri her bölümde 3. sıraya
  geçsin (§7). Sayılar neredeyse hiç değişmiyor.
- Hız: küresel sabit değil, **bölüm başına `speedRampScale`** eklensin
  (§6.6). Bu, 30 bölümün **hiçbir hedefini ulaşılamaz hâle getirmiyor** ve
  `startSpeedKmh` / `trafficDensity` ile birebir aynı desende.

---

## 1. Core Loop

> Şeritli trafikte ilerle, çarpmadan araç geç, coin topla, boost'u doğru
> yerde harca; koşu sonunda üç görevi ne kadar tutturduysan o kadar yıldız,
> coin ve yükseltme kazan.

Döngü sağlam ve okunabilir. Sorun döngüde değil, **üstüne konan hedef
katmanında** ve **koşu içi hız rampasında**.

---

## 2. Soru 1 — Perfect dodge hedefleri gerçekten fazla mı?

### 2.1 Sahibinin iddiası doğru mu? Evet.

`game/LevelCatalog.kt:89-99`:

```
id = 4, goal = SurviveTime(40), startSpeedKmh = 75, trafficDensity = 0.85f,
stars = listOf(CompleteRun, ScoreAtLeast(1800), PerfectDodges(3))
```

Perfect dodge kariyerde **ilk kez 4. bölümde** ve **3 tane** isteniyor.
İddia birebir doğru.

### 2.2 Asıl mesele sayı değil — o hedefin ZORUNLU olması

`GameEngine.kt:796-797`:

```kotlin
passed = mode == RunMode.CAREER && level != null &&
    level.awardsStars && stars == level.stars.size,
```

`GameStateRepository.kt:277-279` yalnızca `passed` true iken
`HIGHEST_LEVEL`'i artırıyor. Yıldızlar ayrıca **sıralı** kazanılıyor
(`LevelEvaluator.tiersReached`).

→ **Bir bölümün üç hedefinden herhangi biri o bölümü ilerleme kilidine
çevirir.** Hedefin listede kaçıncı sırada olduğunun artık hiçbir önemi yok.

### 2.3 Bu, 14 Ağustos tasarımının dayandığı ilkeyi geçersiz kıldı

`docs/BALANCE.md:163-167`:

> **İlk hedef asla beceri hedefi olamaz.** Yıldızlar sıralı kazanılır ve bir
> sonraki bölüm `stars > 0` ile açılır.

Bu cümle **artık yanlış**. Aynı yanlış varsayım dört yerde daha duruyor:

| Yer | Ne diyor | Gerçek |
|---|---|---|
| `LevelCatalog.kt:38-42` | "en az 1 yildizla acilir" | 3 yıldız |
| `LevelCurveTest.kt:24-26` | "`stars > 0` ile aciliyor" | 3 yıldız |
| `LevelCatalogTest.kt:124` | aynı varsayım | 3 yıldız |
| `BALANCE.md:104`, `GameConfig.kt:306` | çarpışma `\|dx\| < 42` | gerçek sınır **28.16** (§3.1) |

**Bu yüzden hiçbir test duvarı yakalamadı.** `LevelCurveTest` ilk 8 bölüm
için `stars >= 1` (satır 170), ilk 5 için `stars >= 2` (satır 185)
doğruluyor. **Gerçek kilit koşulu `stars == 3` yalnızca 1. bölüm için test
ediliyor** (satır 143). Test paketi oyunun asıl ilerleme kapısını 2–30.
bölümlerde hiç ölçmüyor.

### 2.4 Etkinin büyüklüğü: 19 bölüm

| Bölüm | Hedefler (sırasıyla) | İlk beceri hedefi kaçıncı sırada |
|---|---|---|
| 4 | Bitir · 1800 puan · **3 dodge** | 3 |
| 6 | 10 coin · **4 dodge** · **3x combo** | 2 |
| 7 | 2900 puan · **6 dodge** · **4x combo** | 2 |
| 8 | Bitir · 36 s altı · **4 dodge** | 3 |
| 9 | 15 coin · **5 dodge** · 3300 puan | 2 |
| 11 | **8 dodge** · **4x combo** · 3900 puan | **1** |
| 12 | Bitir · **3 dodge** · 55 s altı | 2 |
| 14 | **5x combo** · **10 dodge** · 4400 puan | **1** |
| 16 | 40 geçiş · **8 dodge** · 20 coin | 2 |
| 17 | 4800 puan · **5x combo** · **12 dodge** | 2 |
| 18 | Bitir · **8 dodge** · 66 s altı | 2 |
| 20 | **14 dodge** · **6x combo** · 24 coin | **1** |
| 22 | 5400 puan · **12 dodge** · 26 coin | 2 |
| 23 | **6x combo** · **16 dodge** · 5600 puan | **1** |
| 25 | Fren ≤1 · 5800 puan · **14 dodge** | 3 |
| 26 | **18 dodge** · **7x combo** · 30 coin | **1** |
| 28 | 6200 puan · **16 dodge** · 32 coin | 2 |
| 29 | **8x combo** · **20 dodge** · 6600 puan | **1** |
| 30 | Bitir · **15 dodge** · 88 s altı | 2 |

**30 bölümün 19'u** (4–30 arasının %70'i) beceri hedefi taşıyor. Beceri
hedefi olmayan 11 bölümün (1, 2, 3, 5, 10, 13, 15, 19, 21, 24, 27) sekizi
4. bölümün arkasında kilitli.

**Kural değişikliğinin bedeli sayıyla:** eski kuralda yalnızca beceri hedefi
**1. sırada** olan 6 bölüm duvardı. Yeni kuralda **19'unun hepsi duvar.**

`ComboAtLeast(n)`, `PerfectDodges(n)`'i **kapsar** — combo, 6 saniyelik
pencerede zincirlenmiş n dodge demektir (`COMBO_WINDOW_SEC = 6f`). Yani combo
hedefleri dodge hedeflerinden **kesin olarak daha zor**.

---

## 3. Soru 2 — Perfect dodge ne kadar zor?

### 3.1 Geometri (sahibin cihazı: SM-G950F, 360 dp)

Motor **dp uzayında** çalışıyor (`GameScreen.kt:374`:
`engine.setViewport(size.width / density, ...)`), yani sayılar doğrudan
sahibin cihazına ait.

```
laneWidth = min(290, 360 × 0.56) / 3      = 67.2 dp
CAR_WIDTH_PX = 40 × 0.80                  = 32.0 dp
hitbox       = 32.0 × 0.88                = 28.16 dp

Çarpışma     : |dx| < 28.16
Dodge eşiği  : 32 + (67.2 − 32) × 0.5     = 49.6 dp
GEÇERLİ BANT : 28.16 ≤ minDx ≤ 49.6       (21.4 dp)
```

> ⚠ `GameConfig.kt:306` ve `BALANCE.md:104` "çarpışma `|dx| < 42`" diyor.
> **Artık doğru değil** — `CAR_ART_SCALE` (0.80) ve `HITBOX_SCALE` (0.88)
> uygulandıktan sonra sınır 28.16. `perfectDodgeMaxDx` hâlâ tabanı
> `CAR_WIDTH_PX` (32) alıyor. Yani bant tasarlanandan (7.6 dp) **daha geniş**
> (21.4 dp) — mekanik niyet edilenden zaten affedici, ve buna rağmen aşağıdaki
> zamanlama duvarı duruyor.

### 3.2 Zamanlama: kaç kare?

`GameEngine.kt:365`: `playerX += (targetX − playerX) × min(1, 16 × dt)`.
60 Hz'de kare başına kalan oran `1 − 16/60 = 0.7333`.

**Yol A — kendi şeridindeki araçtan son anda kaçmak** (dx sıfırdan büyür):

| Kare | dx (dp) | Sonuç |
|---|---|---|
| 1 | 17.9 | çarpışma bölgesi |
| 2 | 31.1 | ✅ dodge |
| 3 | 40.7 | ✅ dodge |
| 4 | 47.8 | ✅ dodge |
| 5 | 52.9 | çok geç |

→ Giriş, dikey örtüşmeden **33–67 ms önce** düşmeli. **3 karelik pencere.**

**Yol B — yandaki araca yanaşıp geri çekilmek** (dx laneWidth'ten azalır):

| Kare | dx (dp) | Sonuç |
|---|---|---|
| 1 | 49.3 | ✅ dodge (kıl payı) |
| 2 | 36.1 | ✅ dodge |
| 3 | 26.5 | 💥 **KAZA** |

→ **2 karelik (33 ms) pencere, üçüncü karede kaza.**

Geniş ekranlarda (şerit 96.7 dp) her iki yolda da pencere yine 2 kare.
Sorun cihaza özel değil, **mekaniğin kendisinde.**

### 3.3 Oyuncu bunu kazara yapabilir mi? Hayır.

İnsan görsel tepki tabanı ~250 ms. Dodge kredisi girişin örtüşmeden
33–67 ms önce düşmesini istiyor. *Tepki vererek* dönen oyuncu pencereyi
4–8 kat erken kaçırır; yetişecek kadar geç dönerse **çarpar**. Arada kazara
düşülecek alan yok. Geriye tek yol kalıyor: bilerek geç dönmek ya da 2 kare
içinde yanaşıp çekmek — planlı, öğrenilmiş, frame-perfect hareketler.

### 3.4 Oyun bunu öğretiyor mu? **Hayır — hiçbir yerde.**

Perfect dodge'un oyuncuya göründüğü tüm yerler:
- `GameScreen.kt:267-273` — dodge **olduktan sonra** çıkan "PERFECT DODGE" başlığı
- `GameScreen.kt:1775` — sonuç ekranında "Perfect Dodge: N"
- `GameModels.kt:86` — hedef metni: *"3 Perfect Dodge yap"*

**Mekaniğin ne olduğunu anlatan tek bir metin, ipucu, tutorial ekranı ya da
öğretici bölüm yok.** Oyuncuya "3 Perfect Dodge yap" deniyor, perfect dodge'un
ne olduğu hiç söylenmiyor.

---

## 4. Ölçüm dökümünün yorumu

Sahibinin dikkat çektiği nokta doğru: **TEMKİNLİ profil sekiz bölümün
hiçbirinde tek bir dodge yapmıyor.**

### 4.1 Profil kusurlu mu, oyun mu böyle? — Oyun böyle.

`LevelCurveTest.kt:234` — `DANGER_AHEAD_PX = 420`. Temkinli profil 420 dp
öngörüyle dönüyor; 4. bölüm hızlarında bu 1.9–0.5 saniye demek. Şerit
değişimi ~12 karede (200 ms) oturduğu için, örtüşme başladığında `playerX`
çoktan şerit merkezinde ve `minDx` tam 67.2 dp — bant 49.6'da bittiği için
asla sayılmıyor.

**Bu bir profil kusuru değil.** §3.3'te gösterildiği gibi kredi penceresi
insan tepki tabanının altında. Profil 420 yerine 100 px öngörüyle oynasa da
pencereyi kaçırırdı; daha geç dönse çarpardı. Yani **"temkinli oynayan dodge
yapamaz" bulgusu gerçek oyuncuya genellenebilir**: dodge avlamayı *bilerek
seçmeyen* hiç kimse bunu üretemez.

### 4.2 Asıl kusurlu olan RİSKLİ profil

`LevelCurveTest.kt:100-108`:

```kotlin
val dx = abs(playerX - neighbour.x)
desired = if (dx > dodgeCeiling - GRAZE_MARGIN_PX) neighbour.lane else playerLane
```

Bu, **her karede piksel hassasiyetinde `dx` okuyup 46.6 dp'de geri çekilen
kapalı çevrimli bir denetleyici** — sıfır gecikmeyle, tam olarak mekaniğin
sınadığı boyutta insanüstü. Dolayısıyla şu testin çıkarımı **geçersiz**
(`LevelCurveTest.kt:192-215`):

> "Yanasma manevrasi yapan otopilot bunlari tutturabiliyorsa hedefler insan
> eliyle de tutturulabilir."

Dosyanın başındaki *"İkisi de bilerek VASAT: mükemmel zamanlama yapmazlar"*
(satır 21) ifadesi de RİSKLİ profil için doğru değil.

### 4.3 Gerçek kilit kuralına göre ölçüm

| Bölüm | TEMKİNLİ | RİSKLİ | Kilit açılır mı? |
|---|---|---|---|
| 1 | 3 ★ | 0 (kaza) | ✅ |
| 2 | 3 ★ | 0 (kaza) | ✅ |
| 3 | 3 ★ | 0 (kaza) | ✅ |
| 4 | **2 ★** | 0 (10 s'de kaza) | ❌ **DUVAR** |
| 5 | 3 ★ | 0 | (erişilemez) |
| 6 | **1 ★** | 0 | ❌ **DUVAR** |
| 7 | 1 ★ | 3 ★ | (erişilemez) |
| 8 | 2 ★ | 3 ★ | (erişilemez) |

**Her iki otopilot da 4. bölümü geçemiyor.** Ölçüm, sahibinin cihazda
yaşadığını birebir üretiyor.

---

## 5. Soru 3 — Öğretme sırası doğru mu?

Niyet (`LevelCatalog.kt:26-29`) doğru, uygulama iki yerde kırık:

1. **4. bölüm perfect dodge'u öğretmiyor, sınıyor.** Mekanik hiç
   gösterilmiyor, doğrudan 3 tekrar isteniyor ve tutturulmazsa oyun
   ilerlemiyor. **Öğretilmeden hedef olarak isteniyor.**
2. **6. bölüm "nefes bölümü" olarak tasarlanmış ama üç görevin ikisi beceri
   hedefi** (4 dodge + 3x combo). Kural değişikliğinden sonra 6. bölüm
   5'ten *daha* zor.

3. bölümün boost'u `BoostDistance(200)` ile öğretmesi iyi bir örnek: tek
   düğme, anında geri bildirim, "birkaç kez bas" seviyesinde hedef. Perfect
   dodge'un böyle bir karşılığı yok.

---

## 6. Soru 4 — İvme: iddia doğru mu, ne yapılmalı?

### 6.1 Sahibinin ölçülebilir iddiası — **DOĞRULANDI, hatta daha kötü**

> *"Standart araba ile 30 sn'de max hıza ulaştım neredeyse. 4. levelde
> 30. saniyede max hıza standart araba ile ulaşmak doğru değil."*

**Hesap: ~23 saniye.** Sahibinin tahmininden 7 saniye erken.

**Azami hız nedir?** Standart araç (Şehir, dört çarpan da 1.00), yükseltmesiz:
`UpgradeCatalog.scoreSpeedCap(1) = 3.2 + 1.12 × curve(1)`, `curve(1) = 0` →
**tavan 3.2**. 4. bölümde taban `speedFromKmh(75) = 2.0 + (15/180)×5.7 = 2.475`.

```
Boost'suz azami :  2.475 + 3.2       = 5.675  →  176 km/h
Boost'lu azami  :  5.675 + 1.8       = 7.475  →  232 km/h
Tavana ulaşma skoru: 3.2 × 600       = 1920 puan
```

**Tavana kaç saniyede ulaşılıyor?** İki fazlı model, `olcum dokumu`'ndaki
gerçek 4. bölüm koşusuna oturtuldu (TEMKİNLİ: skor 3635, süre 40 s):

```
Faz 1 (skor < 1920):  ds/dt = A + (11/600)·s     →  s(t) = (A/k)(e^(kt) − 1)
Faz 2 (tavanda)    :  ds/dt = A + 11 × 3.2 = A + 35.2      (doğrusal)

A = 11·tabanHız + bonusHızı + boostKatkısı        k = 11/600 = 0.018333
```

İki bilinmeyen (A ve t₁), iki denklem (faz 1 sonu = 1920; s(40) = 3635):

```
(A + 35.2) × (40 − 54.545·ln(1 + 35.2/A)) = 3635 − 1920 = 1715
→  A ≈ 66.6        t₁ ≈ 23.2 s
```

**Model kendi kendini doğruluyor:** A = 66.6'nın bileşenleri
`11 × 2.475 (=27.2) + 32.0 (geçiş+coin bonusu, ölçümden) + 7.4 (boost)`.
Boost katkısı 7.4 ⇒ ortalama +0.67 hız birimi ⇒ boost süresinin **%37'sinde
açık**. Bu, boost ekonomisinden bağımsız olarak türetilen sürdürülebilir
oranla (§6.4: %35.6) neredeyse birebir aynı. İki ayrı yoldan çıkan aynı sayı.

**İkinci doğrulama:** aynı model 3. bölümü de doğru tahmin ediyor. Ölçümde
3. bölüm 1400 puana **20. saniyede** ulaşıp erken bitiyor; model 20.1 s
veriyor.

### 6.2 Tabloyla: her bölüm ne zaman tavana oturuyor

Aynı yöntem, `olcum dokumu`'ndaki her TEMKİNLİ koşuya uygulandı:

| Bölüm | Süre | Başlangıç | Tavana ulaşma | Tavanda geçen | Bitişteki km/h |
|---|---|---|---|---|---|
| 1 | 25 s | 60 km/h | ~26.5 s (ulaşamıyor) | 0 | **154** |
| 2 | 30 s | 65 km/h | ~25.8 s | 4.2 s (%14) | 173 (tavan) |
| 3 | 35 s | 70 km/h | ulaşmıyor (20 s'de erken biter) | 0 | **143** |
| 4 | 40 s | 75 km/h | **~23.2 s** | 16.8 s (**%42**) | 176 (tavan) |
| 5 | 45 s | 80 km/h | ~23.6 s | 21.4 s (**%48**) | 181 (tavan) |

**Sahibinin şikâyeti sayısal olarak tam yerinde:** 4. bölümün süresinin
%42'si tavan hızda geçiyor ve "60 km/h'lik öğretici bölüm" 154 km/h'de
bitiyor.

### 6.3 En ağır bulgu: bölüm başına hız ayarı pratikte SİLİNİYOR

Tepki bütçesi = aracın doğduğu yerden oyuncuya varması
(`OBSTACLE_SPAWN_Y_PX = −150` → `playerY = H − 210`; 740 dp'lik ekranda
**680 dp**) bölü yaklaşma hızı `(v − aracHızı) × 187.5`.

| Bölüm | `startSpeedKmh` | Koşu SONUNDA tepki bütçesi |
|---|---|---|
| 1 | 60 | 0.92 s |
| 2 | 65 | 0.85 s |
| 3 | 70 | 1.05 s |
| 4 | 75 | **0.82 s** |
| 5 | 80 | 0.81 s |
| 6 | 75 | 0.82 s |
| 7 | 80 | 0.81 s |
| 8 | 80 | 0.81 s |

**Öğrenme eğrisinin hız boyutu diye bir şey yok.** `startSpeedKmh` yalnızca
ilk ~20 saniyeyi etkiliyor; her bölüm aynı tavana ve aynı 0.8 saniyelik tepki
bütçesine oturuyor. Sahibi "ilk levellerde özellikle" derken tam olarak bunu
tarif ediyor: 1. bölüm de, 8. bölüm de sonunda aynı hızda oynanıyor.

### 6.4 Sahibinin tasarım fikri: "boost hız yönetiminin ana aracı olsun"

> *"Level ilerledikçe mesafe hedefi zaten artıyor, boost kullanarak
> ivmelenmeyi tetiklemek zorunda kalırlar."*

**Fikir sağlam ve mekanik olarak destekleniyor.** Boost ekonomisi
(`GameConfig`):

```
BOOST_MAX 100, drain 38/s      → dolu bar = 2.63 s
regen 15/s                     → 0'dan dolum 6.67 s
coin iadesi 12 × ~0.5 coin/s   → efektif dolum ~21/s
Sürdürülebilir açık kalma oranı: 38·d = 21·(1−d)  →  d = %35.6
```

Yani boost, sürekli kullanımda zamanın **~%36'sında**, ≤2.6 saniyelik
darbeler hâlinde açık kalabilir. Bu, "şarj et → harca" ritmi kuran çalışır
bir döngü ve §6.1'deki ölçümle (%37) uyuşuyor — **oyuncu zaten böyle
oynuyor**, sadece boost şu an hızın *ana* kaynağı değil, ikramiyesi.

**Ama fikrin küresel uygulaması üç yan etki üretir:**

**(a) Sahibinin iki cümlesi birbiriyle ÇELİŞİYOR.** "İvmelenme düşürülmeli"
ile "boost hızı yönetsin" aynı anda olamaz. `ACCEL_RATE_BASE = 6.0` bir
birinci derece yakınsama oranı; τ = 1/6 = 0.167 s. Bir boost darbesi
boyunca elde edilen ortalama bonus oranı `1 − (τ/T)(1 − e^(−T/τ))`:

| `ACCEL_RATE_BASE` | τ | 2.63 s'lik darbede alınan boost etkisi |
|---|---|---|
| 6.0 (bugün) | 0.167 s | **%93.6** |
| 3.0 | 0.333 s | %87.4 |
| 2.0 | 0.500 s | %81.0 |
| 1.0 | 1.000 s | %64.7 |

Boost, hızın ana kaynağı olacaksa **ivme oranı yüksek kalmalı, hatta
artmalı**. Düşürmek her darbenin dörtte birini yakınsamaya harcatır ve
boost'u hamurlaştırır. → **`ACCEL_RATE_BASE` düşürülmemeli.**

**(b) Garaj dengesi sessizce kayar.** Boost, hızın ana kaynağı olursa:

| Etki | Bugün | Boost ana kaynak olursa |
|---|---|---|
| BOOST yükseltmesi (drain 38 → 24) | açık kalma %35.6 → %38.5, sadece bonusu uzatır | **hızın kendisini** uzatır → baskın dal olur |
| SPEED yükseltmesi (tavan 3.2 → 4.32) | ana hız dalı | zayıflar |
| `boostMul` (Kuş SLX 1.12, Dağ Keçisi 1.06) | bonus süresi | **tur hızının belirleyicisi** |
| `accelMul` (Boğa 67 −%8) | bugün neredeyse etkisiz (τ 0.167 s) | her darbede ceza → **ilk kez anlamlı** |

`accelMul`'ın anlam kazanması aslında iyi (bugün satılan ama hissedilmeyen
bir özellik), ama `BALANCE.md`'deki araç tablosu ve dört yükseltme dalının
dengesi **mevcut modele göre** ayarlandı. Küresel değişiklik bunların
hepsinin yeniden türetilmesini gerektirir.

**(c) Boost'un ölü bölgesi cezalandırıcı olur.** `BOOST_REENGAGE_MIN = 8` +
parmak kaldırma kilidi (`boostLockedUntilRelease`): bar boşalınca ~0.4 s
yeniden tutuşulamıyor. Bugün bu bir bonus kaybı; boost ana hız kaynağı
olursa **hız kaybı** olur ve gözden geçirilmesi gerekir.
`BOOST_REGEN_PER_SEC_BRAKING = 10` da freni çifte cezalandırır.

**Sonuç: fikir doğru, küresel uygulaması pahalı.** §6.6'daki bölüm bazlı
öneri aynı hissi **boost ekonomisine ve garaja hiç dokunmadan** veriyor.

### 6.5 Soru 3 — Perfect dodge ve combo hıza bağlı mı? **Hayır.**

- Dodge penceresi tamamen **yanal** dinamikten geliyor (`LANE_LERP_RATE`,
  `laneWidth`, hitbox) — §3.2 tablosunda hız hiç geçmiyor.
- Hız yalnızca **dikey örtüşme süresini** (107 dp / yaklaşma hızı) değiştirir.
  Yavaş = daha uzun örtüşme = manevrayı **planlamak için daha çok zaman**.
- `ComboAtLeast`'in 6 saniyelik penceresi zamana bağlı, hıza değil.
- `PassVehicles` ve `CoinsAtLeast` **zamana** bağlı doğuyor → etkilenmez.

→ **Yavaş başlangıç bu mekanikleri zorlaştırmaz; hafifçe kolaylaştırır.**
Çatışma yok.

### 6.6 Soru 4 — Kademeli/bölüm bazlı seçenek var mı? **Evet, ve tercih bu.**

`LevelDef` zaten iki bölüm-bazlı zorluk düğmesi taşıyor ve **ikisi de tam
olarak bu sorun için eklendi**: `startSpeedKmh` (birinci "fazla zor"
turunda) ve `trafficDensity` (ikinci turda). Üçüncüsü aynı desende:

```kotlin
/** Skordan gelen hız kazancının bölüm çarpanı. 1.0 = bugünkü davranış. */
val speedRampScale: Float = 1f
```

Uygulama noktası tek: `GameEngine.updateSpeed`, `min(scoreCap, score/600)`
ifadesinin sonucu bu çarpanla çarpılır. Varsayılan 1.0 ⇒ **8–30. bölümler
bit bit aynı davranır** (`x * 1f == x`), tıpkı `trafficDensity`'nin
eklenişindeki gibi.

**Önerilen değerler ve sonuçları** (standart araç, yükseltmesiz):

| Bölüm | σ | Boost'suz tavan | Boost'lu tavan | Tepki bütçesi (bugün → öneri) |
|---|---|---|---|---|
| 1 | 0.40 | **100** km/h | 157 | 0.92 s → **1.61 s** (+%75) |
| 2 | 0.50 | **115** | 175 | 0.85 → **1.37** (+%61) |
| 3 | 0.60 | **130** | 187 | 1.05 → **1.19** (+%13) |
| 4 | 0.65 | **141** | 197 | 0.82 → **1.11** (+%35) |
| 5 | 0.75 | **156** | 213 | 0.81 → **0.99** (+%22) |
| 6 | 0.70 | **146** | 203 | 0.82 → **1.05** (+%29) |
| 7 | 0.85 | **166** | 223 | 0.81 → **0.91** (+%12) |
| 8+ | 1.00 | 181 | 232 | 0.81 → 0.81 (değişmez) |

Bu tam olarak sahibinin istediği şeyi verir:

- **4. bölümde 30. saniyede tavan 176 değil 141 km/h.** Tepki bütçesi %35 artar.
- **Speedometrenin üst yarısına yalnızca boost'la çıkılır** (141 → 197 km/h).
  Sahibinin "boost kullanarak ivmelenmeyi tetiklemek zorunda kalırlar" cümlesi
  aynen gerçekleşir — **boost ekonomisine, garaja veya `ACCEL_RATE_BASE`'e hiç
  dokunmadan**.
- **Öğrenme eğrisi ilk kez bir hız boyutu kazanır**: tepki bütçesi
  1.61 → 1.37 → 1.19 → 1.11 → 0.99 → 1.05 → 0.91 → 0.81 s. Bugün bu dizi
  düz (0.92 → 0.81). 6. bölümdeki hafif geri adım, `LevelCatalog`'un kendi
  "testere dişi / nefes bölümü" ilkesiyle tutarlı.

### 6.7 Soru 3 — Ne kırılır? **Hedef hedef, sayıyla.**

Önce hangi hedef türlerinin hıza bağlı olduğu:

| Hedef türü | Hıza bağlı mı | Kaç bölümde |
|---|---|---|
| `PassVehicles` | ❌ hayır (zamana bağlı doğum) | 7 |
| `CoinsAtLeast` | ❌ hayır | 9 |
| `PerfectDodges` / `ComboAtLeast` | ❌ hayır (§6.5) | 19 / 9 |
| `BrakeTapsAtMost` | ❌ hayır | 3 |
| `CompleteRun` + `SurviveTime` hedefi | ❌ hayır | 21 |
| `ScoreAtLeast` | ✅ evet (kısmen) | 15 |
| `BoostDistance` | ✅ evet (artar) | 4 |
| `ReachDistance` hedefi + `FinishUnderSeconds` | ✅ **evet, en kırılgan** | 9 |

**Bölüm bazlı öneride (yalnızca 1–7 değişiyor) etkilenen hedefler:**

| Bölüm | Hedef | Bugün (ölçüm/model) | σ ile | Marj |
|---|---|---|---|---|
| 1 | Bitir · 3 geçiş · 3 coin | — | **etkilenmez** | — |
| 2 | Bitir · 6 coin · 14 geçiş | — | **etkilenmez** | — |
| 3 | `ScoreAtLeast(1400)` | 20.1 s'de tutuyor | 21.5 s'de tutuyor (limit 35 s) | ✅ +%63 süre payı |
| 3 | `BoostDistance(200)` | tutuyor | ~385 m | ✅ +%92 |
| 4 | `ScoreAtLeast(1800)` | 3635 | **3280** (−%10) | ✅ **+%82** |
| 5 | `ScoreAtLeast(2500)` | 3835 | **3605** (−%6) | ✅ **+%44** |
| 6 | 10 coin · 4 dodge · 3x combo | — | **etkilenmez** | — |
| 7 | `ScoreAtLeast(2900)` | 4270 | **4126** (−%3) | ✅ **+%42** |
| 8–30 | hepsi | — | **bit bit değişmez** | — |

> **Bölüm bazlı öneride hiçbir hedef ulaşılamaz hâle gelmiyor.** En dar marj
> %42. Skor hedeflerinin az etkilenmesinin sebebi: skorun %35–45'i
> **zamana bağlı** geçiş ve coin bonusundan geliyor (4. bölümde 32 puan/s),
> hız yalnızca kalanını besliyor.

**Küresel bir hız düşüşü olsaydı** (karşılaştırma için, ~%6 ortalama hız
kaybı varsayımıyla) — mesafe hedefleri kırılırdı:

| Bölüm | Mesafe | `FinishUnder` | Bugün ulaşılabilir | Marj bugün | Küresel −%6 | Marj |
|---|---|---|---|---|---|---|
| 8 | 1200 m | 36 s | 1200 m @ 29 s (ölçüm) | +%19 | ~31 s | ✅ +%14 |
| 10 | 2000 | 52 | 2504 m | +%25 | 2354 | ✅ +%18 |
| 12 | 2400 | 55 | 2667 | +%11 | 2507 | ✅ +%4 |
| 15 | 2800 | 62 | 3048 | +%9 | 2865 | ✅ +%2 |
| 18 | 3200 | 66 | 3266 | +%2 | 3070 | ❌ **−%4** |
| 21 | 3600 | 70 | 3483 | ❌ **−%3** | 3274 | ❌ −%9 |
| 24 | 4000 | 74 | 3701 | ❌ **−%7** | 3479 | ❌ −%13 |
| 27 | 4500 | 80 | 4027 | ❌ **−%10** | 3785 | ❌ −%16 |
| 30 | 5000 | 88 | 4463 | ❌ **−%11** | 4195 | ❌ −%16 |

> ⚠ **ÖLÇÜLMEDİ.** `LevelCurveTest` yalnızca ilk 8 bölümü oynatıyor;
> 9–30 için elimizde **hiç ölçüm yok**. Yukarıdaki mesafeler
> `ort. hız = [24 s × 4.84 + (T−24) × 6.44] / T` modelinden türetildi
> (%35 boost açık kalma varsayımı, yükseltmesiz standart araç) ve bu model
> 8. bölümün ölçülen değerini (%19 marj) doğru veriyor.

**Bu tablodan çıkan bağımsız bir bulgu:** 21, 24, 27 ve 30. bölümlerin
`FinishUnderSeconds` yıldızları **bugün, hiçbir değişiklik yapılmadan da
ulaşılamaz görünüyor** — yükseltmesiz standart araçla. SPEED dalı 8.
seviyeye çıkarsa tavan 3.2 → 4.32 olur (+%17 hız) ve 21 ile 24 kurtulur.
Yani **geç bölümler örtük olarak garaj ilerlemesine bağlı** — bu belgelenmiş
bir tasarım kararı değil, keşfedilmiş bir yan etki. Mevcut 3-yıldız kuralında
bu, geç bölümlerin coin biriktirmenin arkasına kilitlenmesi demek.

### 6.8 PROVENANCE — bu bir prototip sapması mı?

| Değişiklik | Prototip sapması mı? |
|---|---|
| `ACCEL_RATE_BASE` düşürmek | **Hayır** — 6.0 zaten sapma #1'in kendisi (prototipte hız anında zıplıyordu). Ama §6.4(a) gereği **yapılmamalı**. |
| `SCORE_SPEED_DIVISOR` (600) veya `SCORE_SPEED_CAP_BASE` (3.2) değiştirmek | **Evet, doğrudan fizik sapması.** Formül `2.63 + min(3.2, score/600)` prototipten birebir. PROVENANCE'a yeni numaralı madde gerekir. |
| `LevelDef.speedRampScale` eklemek (varsayılan 1.0) | **Hayır.** Prototipte bölüm/hedef kavramı **yok** (PROVENANCE #4). `startSpeedKmh` ve `trafficDensity` de aynı gerekçeyle sapma sayılmadı, PROVENANCE #10 altında "meta katmanın revizyonu" olarak kaydedildi. Varsayılan 1.0 olduğu için formül **hiç değişmiyor**; yalnızca 1–7. bölümler kendi çarpanını veriyor. |

→ **Bölüm bazlı yaklaşım, prototipten sapmadan sahibinin istediği sonucu
veren tek yol.** Yine de PROVENANCE #10'un devamı olarak kaydedilmeli
(1–7. bölümlerin hız profili değişiyor).

---

## 7. Soru 5 — Somut öneri

### 7.1 Sahibinin "1 perfect dodge olabilirdi" önerisi iyi mi?

**Yönü doğru, yeterli değil.** 3 → 1, "imkânsız şeyi üç kez yap"ı "imkânsız
şeyi bir kez yap"a çevirir. Mevcut kilit kuralında **1 perfect dodge da 5.
bölümü kalıcı olarak kapatır**; mekanik hâlâ öğretilmemiş ve hâlâ 2–3
karelik. Ayrıca aynı sorun 18 bölümde daha var — 4. bölümü yamamak duvarı
6. bölüme taşır.

### 7.2 Önerilen çözüm: yapıyı düzelt, sayıları neredeyse hiç değiştirme

Tek bir tasarım kuralı — kataloğun **zaten tasarlandığı** kural:

> **Her bölümün hedefleri `[ulaşılabilir, ulaşılabilir, beceri]` sırasında
> olur. Bir sonraki bölüm 3 yıldızla değil, ilk 2 yıldızla açılır.**

Yıldızlar sıralı olduğu için "2 yıldız" = ilk iki hedefin **ikisi de**
tamamlandı demek. Yani sahibinin 15 Ağustos'taki niyeti korunur (*süreyi
doldurup görev yapmayan geçemez*); vazgeçilen tek şey "her bölümü %100 yapma
zorunluluğu". Üçüncü yıldız **ustalık yıldızı** olur: coin
(`COINS_PER_STAR = 25`) ve XP ödemeye devam eder, haritada görünür, ama
ilerlemeyi kilitlemez.

#### Değişiklik A — kilit kuralı (1 satır)

`GameEngine.kt:796-797`: `stars == level.stars.size` → `stars >= 2`
(tercihen `GameConfig.STARS_TO_UNLOCK_NEXT = 2` sabiti üzerinden).
`GameScreen.kt:524` ve `:1726` aynı bayrağı okuduğu için kendiliğinden düzelir.

#### Değişiklik B — hedef SIRASI (16 bölüm)

**Yalnızca sıra değişiyor, hiçbir sayı değişmiyor (7 bölüm):**

| Bölüm | Şu an | Önerilen |
|---|---|---|
| 9 | 15 coin · 5 dodge · 3300 puan | 15 coin · 3300 puan · **5 dodge** |
| 12 | Bitir · 3 dodge · 55 s altı | Bitir · 55 s altı · **3 dodge** |
| 16 | 40 geçiş · 8 dodge · 20 coin | 40 geçiş · 20 coin · **8 dodge** |
| 18 | Bitir · 8 dodge · 66 s altı | Bitir · **72 s altı**¹ · **8 dodge** |
| 22 | 5400 puan · 12 dodge · 26 coin | 5400 puan · 26 coin · **12 dodge** |
| 28 | 6200 puan · 16 dodge · 32 coin | 6200 puan · 32 coin · **16 dodge** |
| 30 | Bitir · 15 dodge · 88 s altı | Bitir · **100 s altı**¹ · **15 dodge** |

¹ 18 ve 30'da süre yıldızı artık **kapı** hâline geldiği için gevşetilmeli:
§6.7 tablosunda 66 s yalnızca %2 marjla, 88 s ise **negatif marjla**
görünüyor. 72 ve 100 değerleri ölçülmemiş tahmindir, doğrulanmalıdır.

**Hiç değişmeyen 14 bölüm:** 1, 2, 3, 5, 10, 13, 15, 19, 21, 24, 27 (beceri
hedefi yok) ve **4, 8, 25** (beceri hedefi zaten 3. sırada).

> **4. bölüm — sahibinin şikâyet ettiği bölüm — hiç değişmiyor.**
> `[Bitir, 1800 puan, 3 dodge]` zaten doğru sırada. Ölçümde TEMKİNLİ profil
> bitiriyor ✓ ve 3635 puan alıyor ✓ → 2 yıldız → **5. bölüm açılır.**
> 3 dodge isteyen üçüncü yıldız ustalık hedefi olarak yerinde kalır.

**İki beceri hedefi taşıyan 9 bölümde** üç slotun ikisi beceri olduğu için
sıralama yetmiyor; birinin yerine ulaşılabilir bir hedef gerekiyor.
`ComboAtLeast(n)` zaten `PerfectDodges(n)`'i kapsadığından **combo korunup
dodge hedefi değiştiriliyor**:

| Bölüm | Şu an | Önerilen |
|---|---|---|
| 6 | 10 coin · 4 dodge · 3x combo | 10 coin · **30 geçiş** · 3x combo |
| 7 | 2900 puan · 6 dodge · 4x combo | 2900 puan · **40 geçiş** · 4x combo |
| 11 | 8 dodge · 4x combo · 3900 puan | **3900 puan** · **18 coin** · 4x combo |
| 14 | 5x combo · 10 dodge · 4400 puan | **4400 puan** · **55 geçiş** · 5x combo |
| 17 | 4800 puan · 5x combo · 12 dodge | 4800 puan · **22 coin** · 5x combo |
| 20 | 14 dodge · 6x combo · 24 coin | **24 coin** · **60 geçiş** · 6x combo |
| 23 | 6x combo · 16 dodge · 5600 puan | **5600 puan** · **60 geçiş** · 6x combo |
| 26 | 18 dodge · 7x combo · 30 coin | **30 coin** · **65 geçiş** · 7x combo |
| 29 | 8x combo · 20 dodge · 6600 puan | **6600 puan** · **28 coin** · 8x combo |

> ⚠ **Bu 9 satırdaki yeni sayılar ÖLÇÜLMEDİ.** Koddaki doğma oranlarından
> türetildi: `beklenen geçiş = süre / 0.78 × yoğunluk`,
> `beklenen coin doğumu = süre / 1.05`, hedefler beklenenin %40–60'ı (mevcut
> katalog geleneği). Türetme ilk 8 bölümde ölçümle tutuyor (bölüm 5: beklenen
> 57.7 / ölçülen 56; bölüm 7: beklenen 64 / ölçülen 62). Yine de
> **`LevelCurveTest` 30 bölüme genişletilip doğrulanmadan uygulanmamalı.**

#### Değişiklik C — hız profili (§6.6)

`LevelDef.speedRampScale` eklensin, 1–7. bölümlere §6.6 tablosundaki
değerler verilsin. 8–30 varsayılan 1.0 ile **hiç değişmez.**

#### Değişiklik D — mekaniği yaşanabilir kıl (bu turda opsiyonel)

A+B, perfect dodge'u ilerleme yolundan çıkarır ama **mekaniği düzeltmez**.
Üçüncü yıldızın anlamlı olması için:

1. **`PERFECT_DODGE_WINDOW_RATIO`: 0.5 → 0.85.** 360 dp'de eşik 49.6 → 61.9.
   Yol A penceresi **3 kareden 7 kareye (50 → 117 ms)** çıkar. Yan şeritte
   sabit duran araç hâlâ saymaz (67.2 > 61.9, 5.3 dp pay) —
   `BALANCE.md:124`'teki değişmez korunur. Prototip sapması değil: perfect
   dodge prototipte **yok** (PROVENANCE #4).
2. **Mekaniği öğreten bir an.** En ucuzu 4. bölümün geri sayımında tek satır:
   TR *"Şerit değiştirirken araca yakın geç: PERFECT DODGE"* /
   EN *"Change lanes close to a car: PERFECT DODGE"*.
   (UI işi — `ui-ux-mobile-designer`.)

#### Değişiklik E — testleri gerçek kurala bağla

- `LevelCurveTest` **30 bölüme** genişletilsin (şu an 8).
- Yeni değişmez: *"her bölüm TEMKİNLİ oyunla kilidi açan yıldızı verir"*
  (`stars >= STARS_TO_UNLOCK_NEXT`), bugünkü `stars >= 1` yerine.
- `LevelCatalogTest`'e biçimsel kural: *"1. ve 2. hedef asla `PerfectDodges`
  veya `ComboAtLeast` olamaz."* Aynı hatanın tekrarını yapısal olarak engeller.
- Yanlış yorumlar düzeltilsin: `LevelCatalog.kt:38-42`,
  `LevelCurveTest.kt:24-26`, `LevelCatalogTest.kt:124`, `BALANCE.md:163-167`,
  ve iki yerdeki `|dx| < 42`.

### 7.3 Alternatif: sahibi "üç görevin üçü de" kuralında ısrar ederse

Tek tutarlı yol: **perfect dodge ve combo hedeflerini kariyerden tamamen
çıkarıp yalnızca günlük/haftalık görevlerde bırakmak** — orada başarısızlık
ilerlemeyi durdurmaz ve `DailyChallengeGenerator` / `WeeklyMissionGenerator`
zaten `PERFECT_DODGES` kullanıyor. 19 bölüm düzenlemek demek; A+B'den pahalı
ve kariyerden bir beceri boyutunu tamamen siler. **Tavsiye etmiyorum**, ama
3-of-3 korunacaksa mantıklı tek seçenek bu.

---

## 8. Zorunlu inceleme sorularına cevaplar

| # | Soru | Cevap |
|---|---|---|
| 1 | İlk 30 saniyede ne yapacağını anlıyor mu? | **Evet.** 1. bölüm örnek bir açılış; beş tohumda da 3 yıldız. |
| 2 | Ana aksiyon tatmin edici mi? | Şerit değişimi evet (τ≈60 ms), boost evet. **Perfect dodge hayır** — oyuncu tetikleyemiyor. |
| 3 | Neden kaybettiğini anlıyor mu? | Çarpışma evet. **Bölümü geçememeyi hayır**: "3 Perfect Dodge yap" yazıyor, nasıl yapılacağı hiçbir yerde yazmıyor. |
| 4 | Retry 2–3 aksiyondan fazla mı? | Ölçülmedi, kapsam dışı. |
| 5 | İlk 5 dakikada yeterli çeşitlilik? | Evet — 4 mekanik, yoğunluk rampası, 4 tema. |
| 6 | Ödül davranışı güçlendiriyor mu? | **Kısmen hayır.** En büyük ödül (bölüm kilidi) tetiklenemeyen bir davranışa bağlı. |
| 7 | Zorluk artışı beceriyle uyumlu mu? | **Hayır.** Bölümler arası rampa iyi tasarlanmış ama koşu *içi* rampa onu eziyor: her bölüm aynı 0.8 s tepki bütçesinde bitiyor (§6.3). |
| 8 | Reklamlar akışı bozuyor mu? | Kapsam dışı. (İlk 4 bölüm reklamsız — doğru karar.) |
| 9 | Audio/VFX başarıyı vurguluyor mu? | Evet. Sorun geri bildirimde değil, olayın hiç oluşmamasında. |
| 10 | Geri gelmek için anlamlı hedef var mı? | Var (garaj, yükseltme, günlük görev) — ama kariyer 4. bölümde tıkandığı için ana ilerleme hedefi ölü. |

---

## 9. Must Fix Before Release

1. **Kilit kuralı** — `stars == 3` → `stars >= 2` (§7.2-A). Kök sebep.
2. **Hedef sırası** — 16 bölümde beceri hedefi 3. sıraya (§7.2-B).
   4, 8, 25 ve beceri hedefi olmayan 11 bölüm değişmiyor.
3. **9–30. bölümlerin ölçümü** — `LevelCurveTest` 30 bölüme genişletilmeli.
   Oyunun **üçte ikisi bugüne kadar bir kez bile oynatılmadan** yayına
   gidiyor; §6.7'deki dört kırık süre yıldızı bu yüzden fark edilmemiş.
4. **21/24/27/30'un `FinishUnderSeconds` yıldızları** — bugün ulaşılamaz
   görünüyor (§6.7), doğrulanıp gevşetilmeli.
5. **Yanlış yorum ve belgeler** — dört yerdeki "`stars > 0` ile açılır" ve
   iki yerdeki "çarpışma `|dx| < 42`" (§2.3).
6. **`LevelCatalogTest`'e biçimsel kural** — 1. ve 2. hedef beceri hedefi olamaz.

## 10. Nice to Have

1. **`LevelDef.speedRampScale`** + 1–7. bölüm değerleri (§6.6). Sahibinin
   ivme şikâyetinin doğru cevabı; 8–30'u hiç etkilemiyor. Sıralama tercihi:
   **§9'daki maddeler çıktıktan sonra, ayrı bir turda** — ikisi aynı anda
   çıkarsa hangisinin işe yaradığı ölçülemez.
2. `PERFECT_DODGE_WINDOW_RATIO` 0.5 → 0.85 (§7.2-D.1).
3. 4. bölümde perfect dodge'u anlatan tek satır ipucu (§7.2-D.2).
4. Boost'un ölü bölgesinin gözden geçirilmesi (`BOOST_REENGAGE_MIN` +
   parmak kaldırma kilidi) — yalnızca boost gerçekten hızın ana kaynağı
   olursa gerekli (§6.4-c).

## 11. Yapılmasın

- **`ACCEL_RATE_BASE` düşürülmesin.** Zorluğa etkisi yok; boost darbesinin
  etkisini %94'ten %81'e (rate 2.0) düşürür, yani sahibinin *kendi* tasarım
  fikrini bozar; `ACCELERATION` yükseltmesini anlamsızlaştırır (§6.4-a).
- **`SCORE_SPEED_DIVISOR` / `SCORE_SPEED_CAP_BASE` küresel değiştirilmesin.**
  Prototipten birebir geliyor (PROVENANCE sapması olur) ve 9 mesafe
  bölümünün süre yıldızlarını kırar — dördü zaten kırık (§6.7).
- **`WORLD_SPEED_SCALE` bu turda değiştirilmesin.** Hedeflerin hiçbirini
  bozmaz ama bölüm bazlı çözüm aynı işi daha hedefli yapıyor; ikisi birden
  yapılırsa ölçüm kirlenir.
- **"3 dodge → 1 dodge" yapılıp geçilmesin.** Duvarı kaldırmaz, 6. bölüme
  taşır (§7.1).

---

## Verdict: **REJECT**

Kariyer modu şu hâliyle yayına hazır değil.

**Birinci sebep:** oyun 4. bölümde bitiyor. 30 bölümün 26'sı, oyunun hiçbir
yerde öğretmediği ve 2–3 kare genişliğinde giriş isteyen bir mekaniğin
arkasında kilitli. Bunu oyunu yapan kişi de aşamadı.

**İkinci sebep:** öğrenme eğrisinin hız boyutu yok. `startSpeedKmh` ile
kurulan 60→80 km/h rampası, koşu içi skor rampası tarafından siliniyor;
1. bölüm de 8. bölüm de aynı 0.8 saniyelik tepki bütçesinde bitiyor ve
4. bölüm süresinin %42'sini tavan hızda geçiriyor. **Sahibinin ivme
şikâyeti sayısal olarak doğrulandı** (tahmini 30 s, gerçek ~23 s).

Reddin sebebi zorluğun yüksek olması değil — **zorluğun açıklanmamış,
kazanılamaz ve ölçülmemiş olması.** §9'daki altı madde çıktığında ve
`LevelCurveTest` 30 bölümde yeşil olduğunda karar **APPROVE**'a döner.

**Sahibinin vermesi gereken iki karar:**

1. **Kilit kuralı** (§7.2): "2 yıldız açar, 3. yıldız ustalık" modeli,
   15 Ağustos'taki kararı kısmen geri alır. Niyeti korur (görev yapmayan
   geçemez), vazgeçtiği tek şey "her bölümü %100 yapma zorunluluğu".
   Bu takas onaylanmadan §9'un 1. ve 2. maddeleri uygulanamaz.
2. **Hız modeli** (§6.6): bölüm bazlı `speedRampScale` mi, yoksa küresel
   sabit değişikliği mi? Öneri kesinlikle birincisi — ikincisi prototip
   sapmasıdır ve dokuz mesafe bölümünü yeniden dengelemeyi gerektirir.
