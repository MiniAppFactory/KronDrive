# Kron Drive — Oynanış Hissi İncelemesi

**Tarih:** 2026-08-17 · **Kapsam:** kareden kareye his — kontrol, geri bildirim,
çarpışma anı, araç karakteri, perfect dodge mekaniği
**Kapsam dışı:** ürün/ekonomi kararları, arayüz düzeni, regresyon (paralel ajanlar)

> **Bu belge kod değiştirmedi.** Hiçbir `.kt` dosyasına dokunulmadı, Gradle
> çalıştırılmadı, cihaza kurulum yapılmadı. Aşağıdaki her sayı ya koddan
> okundu ya koddaki formülden **elle türetildi**. Türetme yapılan yerde formül
> yazılı, böylece kontrol edilebilir. **Cihazda hiçbir şey denenmedi.**
> Skor eğrisine dayanan (yani zaman/mesafe içeren) sayılar
> `docs/DIFFICULTY_REVIEW.md` §6.1'deki modelden gelir ve **ölçüm değildir** —
> her birinde ayrıca işaretlendi.

**Ortam varsayımı:** SM-G950F, 360 × 740 dp, oyun ~40 FPS. Bu son sayı
aşağıda iki yerde belirleyici oluyor (§3.2 ve §2.1) — his değerlendirmesi
60 Hz varsayımıyla yapılırsa yanlış çıkıyor.

---

# 1. ACİL SORU — `speedRampScale` × SPEED yükseltmesi

## 1.0 Önce: sahibinin yakaladığı açık kısmen bir okuma farkı

Sahibinin cümlesi:

> *"ajan aynı zamanda araçların top speedini güçlendiren bir garaj ekipmanı
> sattığımızı değerlendirdi mi?"*

Endişe **haklı ve önemli**. Ama `DIFFICULTY_REVIEW.md` §6.6'nın *yazdığı*
uygulama noktasına bakınca durum sahibinin korktuğu gibi değil:

> "`GameEngine.updateSpeed`, `min(scoreCap, score/600)` **ifadesinin sonucu**
> bu çarpanla çarpılır."

`scoreCap` zaten `UpgradeCatalog.scoreSpeedCap(upgrades.speed, car)`
(`GameEngine.kt:388`), yani **yükseltmeyi ve aracın `topSpeedMul`'unu içeriyor.**
Sonucu σ ile çarpmak tavanı *sabitlemez*, **orantılı olarak küçültür**:

```
bugün      : gain = min(3.2 + 1.12·curve(L) , score/600)
σ ile      : gain = min(3.2 + 1.12·curve(L) , score/600) · σ
                    └────────── yükseltme burada, σ'dan ÖNCE ──────────┘
```

Yani bölüm 4'te SPEED yükseltmesi **hiçbir şey yapmıyor** olmaz; **değerinin
%65'ini yapar** (σ=0.65 için). Sahibinin "141 km/h'ye sabitlenirse" okuması,
σ'nın mutlak bir km/h tavanı olduğu varsayımına dayanıyor — öneri bu değil.

**Ama endişe yine de yerinde**, iki sebeple:

1. Formülün **başka bir uygulama noktası** (σ yalnızca `score/600` terimini
   çarpsın) sahibinin tarif ettiği felaketi birebir üretir — ve §6.6'nın metni
   ("hız tavanını düşür") bu okumaya da açık. Hangi noktanın seçildiği
   yazılmadan uygulanırsa yanlış olan seçilebilir.
2. Asıl sorun fizikte değil **garajda**: garaj oyuncuya "216 km/h" yazan bir
   ürün satıyor ve σ'dan sonra bölüm 4'te 164 km/h veriyor. Fizik dürüst,
   **vitrin yalancı olur.** Bu, "paramın karşılığını görmedim" şikâyetinin
   gerçek kaynağı olur (§1.5).

## 1.1 Dört seçenek, bölüm 4'te sayıyla

Formüller (`GameConfig.kt:360-367`, `UpgradeCatalog.kt:66-73`):

```
kmh(v)        = ((v − 2.0) / 5.7) · 180 + 60      , tavan 240
bölüm 4 tabanı= speedFromKmh(75) = 2.475
cap(L)        = 3.2 + 1.12 · ((L−1)/7)^1.5
curve         : L1 0 · L2 .0540 · L3 .1527 · L4 .2806
                L5 .4320 · L6 .6037 · L7 .7936 · L8 1.0
```

**σ = 0.65** (§6.6'nın bölüm 4 için önerdiği değer), standart araç
(`topSpeedMul = 1.00`), boost kapalı:

| SPEED sv. | **A — bugün (σ yok)** | **B — σ min() sonucunu çarpar** *(önerilen)* | **C — σ yalnızca `score/600`'ü çarpar** | **D — σ yalnızca tabanı çarpar, yükseltme üstüne eklenir** |
|---|---|---|---|---|
| 1 (yükseltmesiz) | **176** km/h | **141** | 176 *(geç ulaşılır)* | **141** |
| 2 | 178 | 142 | 176 | 143 |
| 3 | 181 | 144 | 176 | 146 |
| 4 | 186 | 147 | 176 | 151 |
| 5 | 191 | 151 | 176 | 156 |
| 6 | 197 | 155 | 176 | 162 |
| 7 | 204 | 159 | 176 | 169 |
| 8 (tam) | **211** | **164** | ~199 *(kosu sonunda, bkz. not)* | **176** |

**C sütununun notu — sahibinin korktuğu senaryo tam olarak budur.** σ yalnızca
rampayı yavaşlatırsa tavan 3.2'de kalır; tavana ulaşmak için gereken skor
`600 · 3.2 / 0.65 = 2954` puana çıkar (bugün 1920). Bölüm 4 koşusu ~3300–3600
puana çıktığı için (model, `DIFFICULTY_REVIEW` §6.7) yükseltmesiz oyuncu tavana
**yine ulaşır**, sadece ~23 s yerine ~34 s'de. Yani:

- Koşu **sonundaki** tepki bütçesi **hiç iyileşmez** (0.82 s'de kalır) →
  önerinin ana amacı gerçekleşmez.
- Tam yükseltmeli oyuncu için `cap = 4.32` demek `600·4.32/0.65 = 3988` puan
  demek; bölüm 4 koşusu oraya çıkmaz → **yükseltme koşunun son birkaç
  saniyesine kadar hiçbir şey yapmaz.**

→ **C uygulanmamalı.** Sahibinin yakaladığı açık, yalnızca C'de gerçek.

**Bölüm 4'teki tepki bütçesi** (araç doğumdan oyuncuya 680 dp;
yaklaşma hızı `(v − trafikHızı) · 187.5`, trafik ort. `2.475 · 0.515 = 1.274`):

| | yükseltmesiz | SPEED 8 | SPEED 8 + BOOST 8 basılı |
|---|---|---|---|
| **A — bugün** | 0.82 s | **0.66 s** | **0.47 s** *(gösterge 240'a çakılı, gerçek 279)* |
| **B — önerilen** | 1.11 s | 0.90 s | 0.59 s |
| **D** | 1.11 s | 0.82 s | 0.52 s |

**A sütunundaki 0.66 s ve 0.47 s, bu incelemenin en sert bulgusudur ve
`DIFFICULTY_REVIEW` bunu hiç hesaplamamış:** bugün SPEED dalını yükselten
oyuncu bölüm 4'ü **yükseltmesiz oyuncudan %20 daha zor** oynuyor, boost'a
basınca %43 daha zor. Yükseltmeyi geri satmak da mümkün değil. Yani
**bugün SPEED yükseltmesi erken bölümlerde bir güç değil, geri alınamaz bir
ceza.** σ (B veya D) bunu düzeltir; C düzeltmez.

## 1.2 B ile D arasındaki fark, oyuncunun gerçekten sahip olduğu seviyede

Tablodaki B/D farkı SPEED 8'de 12 km/h. Ama **bölüm 4'te hiç kimsenin SPEED 8'i
yok.** Cüzdana bakalım:

- `UpgradeCatalog.cost(L) = 250 · L` → sv.2 = 250, sv.3 = +500, sv.4 = +750.
- `PlayerProgress` başlangıç 100 coin.
- `GameConfig.SCORE_PER_BONUS_COIN` yorumu, **ölçülmüş** değer veriyor:
  ilk geçişte bölüm başına ortalama **118 coin**.

| Bölüm 4'e gelene kadar | coin | alınabilecek en yüksek SPEED |
|---|---|---|
| 3 bölüm geçilmiş | 100 + 3·118 = **454** | sv. 2 (250) — geriye 204 |
| 4 bölüm geçilmiş | **572** | sv. 2, sv. 3 için 250 eksik |

Ve bu, coin'in **tamamı** SPEED'e gittiği varsayımıyla; oyuncunun önünde
900 coinlik Yarış Sedan ve üç yükseltme dalı daha var.

**Bölüm 4'te gerçekçi SPEED seviyesi: 2, iyimser 3.** O seviyede B ile D
arasındaki fark:

| SPEED sv. | B | D | **fark** |
|---|---|---|---|
| 2 | 142 | 143 | **1 km/h** |
| 3 | 144 | 146 | **2 km/h** |

**Bütün tartışma, önemli olduğu noktada 1–2 km/h ediyor.** Sebep dışbükey
yükseltme eğrisi: `curve(2) = 0.054`, yani ilk satın alma tavanın %5.4'ünü
veriyor. `ECONOMY_BALANCE_PROPOSAL.md` §5.1 bunu zaten bulmuş
("ilk yükseltme adımı görünmez, 250 coin = +1 km/h"); σ o adımı 1.9 km/h'den
1.2 km/h'ye indirir. **Zaten görünmeyen bir şeyi biraz daha görünmez yapar.**

## 1.3 B'nin D'ye üstün olduğu yer: geri dönen usta oyuncu

`MIN_STARS_TO_PASS = 2` olduğu için (`GameConfig.kt:408`) üçüncü yıldız bir
**ustalık yıldızı** ve oyuncu onu almak için erken bölümlere geri dönecek —
o zaman tam yükseltmeli olarak. Bölüm 1'de (σ = 0.40, taban 60 km/h):

| | yükseltmesiz | SPEED 8 |
|---|---|---|
| **B** | 100 km/h ✅ *(σ=0.40 tam bunun için seçilmiş)* | **115 km/h** |
| **D** | 100 km/h | **136 km/h** — hedefin %36 üstü |

D'de "100 km/h'lik öğretici bölüm" tanımı tam yükseltmeli oyuncu için
çöküyor ve σ tablosunun anlamı kalmıyor. B'de bölüm σ'sı her cüzdan
durumunda geçerli kalıyor.

## 1.4 KARAR

> **B: `σ`, `min(scoreCap, score/600)` ifadesinin *sonucuna* uygulanır.**
> Yani `DIFFICULTY_REVIEW` §6.6'nın yazdığı şey — ama **yazılı olarak
> sabitlenmeli**, çünkü C ve D aynı cümleden okunabiliyor.

```kotlin
// GameEngine.updateSpeed — tek satır
val ramp = level?.speedRampScale ?: 1f
baseSpeed + min(scoreCap, score / GameConfig.SCORE_SPEED_DIVISOR) * ramp
```

Gerekçe, sırayla:

1. **Yükseltme asla değersizleşmez.** Değerinin σ kadarını korur (bölüm 4'te
   %65). C'de sıfırlanır, D'de tam kalır ama bölüm tasarımını ezer.
2. **Gerçek oyuncuda B ile D farkı 1–2 km/h** (§1.2) — garajın kendi gösterim
   çözünürlüğünün altında.
3. **Aracın `topSpeedMul`'u da orantılı ölçeklenir**, çünkü `scoreCap` onu
   zaten içeriyor. Süper Araba bölüm 4'te σ=0.65 ile 149 km/h (standart 141),
   yani araç kimliği erken bölümlerde de okunur kalır. C'de kaybolur.
4. **Boost ölçeklenmez** — `boostSpeedBonus` σ'dan *sonra* toplanıyor
   (`GameEngine.kt:405`). Bölüm 4: 141 → 197 km/h. Sahibinin
   *"boost kullanarak ivmelenmeyi tetiklemek zorunda kalırlar"* cümlesi
   birebir gerçekleşir ve boost'un göreli ağırlığı σ küçüldükçe **artar**.
   Bu, önerinin en değerli yan etkisi ve yalnızca B ve D'de var.
5. **Varsayılan 1.0 ⇒ bölüm 8–30 bit bit aynı** (`x * 1f == x`), tıpkı
   `trafficDensity` gibi. Bölüm 1–7'nin hepsi `SurviveTime` hedefli
   (`LevelCatalog.kt:52-138`) — σ'nın kırabileceği tek hedef türü olan
   `ReachDistance`/`FinishUnderSeconds` ilk kez **bölüm 8'de**, yani σ=1.0'da
   başlıyor. **Öneri hiçbir mesafe hedefine dokunmuyor.**
6. **PROVENANCE sapması değil**, §6.8'in gerekçesi geçerli: prototipte bölüm
   kavramı yok (#4), formül değişmiyor, `startSpeedKmh`/`trafficDensity` ile
   aynı desen. Yine de PROVENANCE #10'un devamı olarak kaydedilmeli.

**Zorunlu ek:** `LevelDef.speedRampScale`'in KDoc'una hangi terimin
çarpıldığı **açıkça** yazılmalı ve bir JVM testi kilitlemeli:

```
"σ, yükseltme tavanını da ölçekler" :
  GameEngine(level = σ0.65, upgrades = speed 8) tavanı
  > GameEngine(level = σ0.65, upgrades = speed 1) tavanı
```

Bu test olmadan bir sonraki oturum C'yi uygular ve kimse fark etmez —
`stars == 3` hatasının aynısı.

## 1.5 Asıl düzeltilmesi gereken: garaj SPEED sayısı zaten yalan

σ'dan **bağımsız, bugün var olan bir kusur.** `UpgradeCatalog.kt:143-144`:

```kotlin
UpgradeType.SPEED ->
    "${GameConfig.speedToKmh(GameConfig.BASE_SPEED + scoreSpeedCap(level))} km/h"
```

İki şeyi görmezden geliyor:

1. **Aracı.** `scoreSpeedCap(level)` çağrılıyor, `scoreSpeedCap(level, car)`
   değil. Süper Araba'nın (`topSpeedMul = 1.12`) gerçek tavanı sv.1'de
   **193 km/h**, garaj **180** yazıyor. Beety'nin (0.92) gerçeği **172**,
   garaj yine **180** yazıyor. Yani garaj, oyuncunun aldığı 3200 coinlik
   aracın en belirgin özelliğini **hiç göstermiyor** — üstelik sapma #14
   tam olarak *"araçların özellikleri görünmüyor garajda"* şikâyetini
   çözmek için yapılmıştı.
2. **Bölümü.** σ geldiğinde 216 km/h yazan bir ürün bölüm 4'te 164 verecek.

σ'yı garaja bağlamak yanlış olur (garaj bölüm bilmiyor ve bilmemeli). Doğru
düzeltme **mutlak sayıyı bırakıp farkı göstermek**:

```
HIZ   sv.3 → sv.4      +5 km/h        (seçili araçla hesaplanır)
```

Fark bölümden bağımsızdır (B'de σ ile ölçeklenir ama işareti ve sırası
değişmez) ve satın almanın gerçekten ne verdiğini söyler. Mutlak sayı
kalacaksa en azından `scoreSpeedCap(level, car)` çağrılmalı ve altına
*"bölüm hız sınırına tabidir"* satırı eklenmeli.

**Sahibinin "oyuncu parasının karşılığını göremiyor" endişesinin gerçek
cevabı budur.** Fizikte değil vitrinde.

---

# 2. Kontrol hissi

## 2.1 `LANE_LERP_RATE = 16` hızlı mı yavaş mı? — Hızlı. Dokunma.

`GameEngine.kt:365`: `playerX += (targetX − playerX) · min(1, 16·dt)`.

| | 60 Hz | **40 FPS (cihaz)** |
|---|---|---|
| kare başına kalan oran | 0.733 | 0.600 |
| çarpışma kutusundan çıkış (28.16 dp) | 2 kare = **33 ms** | 2 kare = **50 ms** |
| şerit ortasına %95 oturma | 10 kare = 167 ms | 6 kare = **150 ms** |

**Kare hızına duyarlılığı yok denecek kadar az** (üstel yakınsama `rate·dt`
ile ölçekleniyor; 40 FPS'te hatta 17 ms *daha erken* oturuyor). `MAX_FRAME_DT
= 0.032` sayesinde `min(1, ...)` hiçbir zaman 1'e ulaşmıyor, yani ışınlanma
yok. **Sabit sağlam, dokunulmamalı** — zaten prototipteki 12'den bu değere
"geç tepki veriyor" geri bildirimiyle çıkarılmış (`GameConfig.kt:66-70`).

**"Araba sürüyorum" mu "şerit seçiyorum" mu?** Şerit seçiyor. Ama **sebebi
lerp oranı değil**, oyuncunun sahip olduğu yanal durumun yokluğu:

- `playerLane` dokunuşta **anında** değişiyor (`GameEngine.kt:230-240`);
  `playerX` yalnızca çizim için yumuşatılıyor.
- Şeritler arasında **durmak imkânsız**, yanal hız yok, ataleti yok, direksiyon
  ekseni yok, **başlamış bir manevrayı iptal etmek yok** (ters yöne basmak
  yeni bir şerit değişimi başlatır).
- Sürükleme kanalı da aynı şeyi yapıyor: `detectHorizontalDragGestures`,
  36 dp'de bir **ayrık** şerit değişimi (`GameScreen.kt:408-431`), analog
  değil.

Lerp oranını **düşürmek** işi kötüleştirir (kontroller yine "gecikmeli"
olur, üstelik dodge penceresi genişlerken çarpışmadan çıkış gecikir).
Yükseltmek daha da dijital yapar. Bu his ancak **simülasyona dokunmadan**
düzeltilebilir: yanal hızdan türetilen bir **gövde yatması** (çizimde
`playerX`'in kare farkına orantılı ±6–8° eğim + hafif gölge kayması).
`CarArtwork` zaten dönüşüm alıyor; motor değişmez, denge değişmez, PROVENANCE
sapması olmaz. Göz "araba direksiyon kırdı" diye okur.

## 2.2 Fren/boost basılı tutma + yön anlık dokunma — tutarlı mı? Evet, bir istisna hariç

Kural içsel olarak tutarlı: **sürekli bir büyüklüğü değiştiren girdi basılı
tutulur, ayrık bir büyüklüğü değiştiren girdi dokunulur.**

- Boost/fren `target` hızını sürekli değiştiriyor (`GameEngine.kt:405-406`)
  → `HoldButton` (`GameScreen.kt:1132-1163`) doğru.
- Yön `playerLane`'i 0/1/2 arasında değiştiriyor → `detectTapGestures(onPress)`
  (`GameScreen.kt:1247-1274`) doğru; basma kenarında ateşliyor, bırakmada
  değil, yani en hızlı olası tepki.

**İstisna — `BrakeTapsAtMost` hedefi bu tutarlılığı kırıyor.**
`GameEngine.kt:280`: `if (down && !brakeDown && phase == RUNNING) brakeTaps++`
— yalnızca **basma kenarı** sayılıyor. Yani bölüm 25'in "frene en fazla 1 kez
bas" yıldızı, freni **koşu boyunca basılı tutarak** tutturulabilir. Hedef
"fren kullanma" demek istiyor, ölçtüğü şey "parmağını kaldırma". Ya hedef
metni ya ölçüm düzeltilmeli (fren **süresi** ölçülmeli). Küçük ama gerçek
bir sömürü.

**Girdi tamponu yok.** `steerLeft/Right` `phase != RUNNING` iken sessizce
düşüyor (`GameEngine.kt:231`). Geri sayımın son karesinde yapılan dokunuş
kayboluyor; çarpışma karesinde yapılan dokunuş da. 40 FPS'te bu 25 ms'lik
bir kör nokta — küçük, ama "bastım, gitmedi" hissinin bilinen kaynağı.
Basit çözüm: son 100 ms içindeki dokunuşu `RUNNING`'e geçince bir kez
uygulamak. Bu turda zorunlu değil.

---

# 3. Perfect dodge

## 3.1 `DIFFICULTY_REVIEW`'ın geometri ve zamanlama ölçümü — **DOĞRULANDI**

Koddan tekrar türetildi:

```
laneWidth       = min(290, 360·0.56)/3            = 67.20 dp   (GameEngine.kt:76)
CAR_WIDTH_PX    = 40 · 0.80                       = 32.00      (GameConfig.kt:50)
hitW            = 32 · 0.88                       = 28.16      (GameConfig.kt:58)
çarpışma        : |dx| < 28.16                    (rectHit, GameEngine.kt:870)
dodge eşiği     : 32 + (67.2−32)·0.5              = 49.60      (GameConfig.kt:319-322)
GEÇERLİ BANT    : 28.16 ≤ minDx ≤ 49.60           (21.44 dp)
```

`minDx`, dikey örtüşme boyunca görülen **en küçük** `|dx|`
(`GameEngine.kt:566-567`), yani dokunuş örtüşme **başlamadan önce** düşmeli.
Dokunuş örtüşmeden k kare önce yapılırsa `minDx = 67.2·(1 − r^k)`,
`r = 1 − 16·dt`:

**60 Hz'de (r = 0.7333):**

| k | minDx | sonuç |
|---|---|---|
| 1 | 17.92 | 💥 kaza |
| 2 | 31.06 | ✅ |
| 3 | 40.70 | ✅ |
| 4 | 47.77 | ✅ |
| 5 | 52.95 | ✗ çok erken |

→ **3 kare, 50 ms.** `DIFFICULTY_REVIEW` §3.2 doğru; tablosu birebir tutuyor.

## 3.2 Ama cihazda daha kötü — bu ölçülmemişti

**40 FPS'te (r = 0.600):**

| k | minDx | sonuç |
|---|---|---|
| 1 | 26.88 | 💥 kaza *(28.16'nın 1.28 dp altında)* |
| 2 | **43.01** | ✅ **tek geçerli kare** |
| 3 | 52.68 | ✗ çok erken |

→ **Sahibinin cihazında pencere 1 kare, 25 ms.** Ve k=1 ile k=3 arasında
ara yok: bir kare geç = kaza, bir kare erken = sayılmaz.

Daha da kötüsü, ~31 FPS'te `MAX_FRAME_DT = 0.032` devreye giriyor
(r = 0.488): k=1 → 34.4 dp ✅, k=2 → 51.2 ✗. Yine **tek kare**, ama bu kez
*farklı* bir kare. Yani **kare hızı dalgalandıkça oyuncunun öğrenmesi gereken
zamanlama da değişiyor.** Kas hafızası kurulamaz.

**Sonuç: mekanik cihazda yalnızca zor değil, öğrenilemez.** §3.3'ün
*"kazara asla olmaz"* tespiti çürütülmedi — **güçlendirildi**.

## 3.3 Nasıl oynanabilir hâle gelir

**(a) Pencereyi genişlet — `PERFECT_DODGE_WINDOW_RATIO 0.5 → 0.80.**

Eşik `32 + 35.2·0.80 = 60.16` dp. Yeniden hesaplandı:

| | bugün (0.5) | **0.80** | 0.85 *(§7.2-D.1'in önerisi)* |
|---|---|---|---|
| eşik (360 dp) | 49.60 | 60.16 | 61.92 |
| **40 FPS pencere** | **1 kare / 25 ms** | **3 kare / 75 ms** | 3 kare / 75 ms |
| 60 Hz pencere | 3 kare / 50 ms | 6 kare / 100 ms | 7 kare / 117 ms |
| yan şeritteki sabit araca pay | 17.6 dp | **7.0 dp** | 5.3 dp |

**0.85 yerine 0.80 öneriyorum:** hedef cihazda (40 FPS) **ikisi de aynı 3
kareyi** veriyor, ama 0.80 "yan şeritten düz geçmek bedava dodge vermesin"
değişmezine (`GameConfig.kt:309-315`, `BALANCE.md:124`) %32 daha fazla pay
bırakıyor. Dar ekranda da güvenli: 320 dp → şerit 59.73, eşik 54.18, pay
5.55 dp. Bedava dodge riski yok. Prototip sapması değil — perfect dodge
prototipte yok (PROVENANCE #4).

**(b) Asıl eksik: tetikleyici görünmez.** Pencere 3 kareye çıksa bile oyuncu
"ne kadar yakın yeterince yakın" bilgisini **hiçbir yerden** alamıyor. Bugün
tek geri bildirim olay *bittikten sonra* çıkan bir yazı. Bu, bir beceriyi
değil bir piyangoyu öğretiyor.

Öneri, çizim tarafında, motora dokunmadan: bir trafik aracı dikey olarak
örtüşürken ve `|dx|` bandın içindeyken **iki araç arasına canlı bir kıvılcım
/ ince ışık çizgisi** çiz. `GameRenderer` zaten her karede `obstacles`
listesini geziyor ve `playerX` elinde; ek durum gerekmiyor. Böylece oyuncu
mekaniği **yaparken** görüyor: "işte, tam buraya kadar yaklaşırsam sayıyor."
Metinle öğretmek (§7.2-D.2) bunun yerine geçmez — perfect dodge sözle değil
mesafeyle tanımlı bir şey.

**(c) Mekaniği değiştirmek gerekmiyor.** Bant zaten tasarlanandan geniş
(§3.1'de 21.44 dp, tasarım niyeti 7.6 dp idi çünkü `perfectDodgeMaxDx`
tabanı `CAR_WIDTH_PX = 32`, gerçek çarpışma sınırı ise 28.16). Yani mekanik
niyet edilenden **affedici**; sorun mesafede değil **zamanda** ve o da tek
sabitle çözülüyor.

---

# 4. Geri bildirim — oyuncu iyi bir şey yaptığında oyun ne diyor?

Tam envanter (kaynak: `GameScreen.kt:257-315` olay anahtarı, `GameRenderer`,
`EngineVoice`):

| Olay | Görsel | Ses | Haptik | Partikül |
|---|---|---|---|---|
| **Perfect dodge** | 0.9 s düz yazı, animasyonsuz, sert açılıp kapanır (`:267-274`, `:391-401`) | ❌ | ❌ | ❌ |
| **Combo ×N** | HUD satırı, **yalnızca combo > 1** (`:765-772`), ~20 Hz tazeleniyor → 50 ms'ye kadar geç | ❌ | ❌ | ❌ |
| **Combo bozuldu** | ❌ satır sessizce kaybolur (`ComboBroken → else -> Unit`, `:283`) | ❌ | ❌ | ❌ |
| **Araç geçildi** (+8 puan) | ❌ yalnızca skor sayısı 20 Hz'de artar | ❌ | ❌ | ❌ |
| **Coin** (+35 puan, +12 boost) | 12 partikül + boost barı sıçrar | ❌ | ❌ | ✅ 12 |
| **Boost başladı** | egzoz alevi + kare başına 3 partikül | ✅ nitro 0.55 s | ❌ | ✅ |
| **Şerit değiştirme** | — | — | ✅ `TextHandleMove` | — |
| **Çarpışma** | overlay (bkz. §6) | ❌ motor 0.1 s'de sönümlenir | ❌ | ❌ |

**Üç bulgu:**

1. **Haptik yalnızca yönde ve kornada var** (`:333, :339, :349, :422, :426` —
   projedeki tüm `performHapticFeedback` çağrıları). Yani telefon oyuncuya
   *"şerit değiştirdin"* diyor ama *"mükemmel bir manevra yaptın"*,
   *"coin aldın"* ve *"öldün"* demiyor. **Titreşim bütçesi en anlamsız
   olaya harcanmış.** Perfect dodge + coin + çarpışma için üç `performHaptic`
   satırı, projedeki en ucuz his kazancı. (`VIBRATE` izni gerekmiyor —
   `HapticFeedback` API'si kullanılıyor.)

2. **Combo 1'de hiçbir şey görünmüyor.** İlk dodge'u yapan oyuncu 0.9 s'lik
   bir yazı görüp ardından **hiçbir kalıcı iz görmüyor**; zincirin başladığını
   ve 6 saniyesi olduğunu bilmiyor (`COMBO_WINDOW_SEC = 6f`). Zincir mekaniği
   ancak zincirin ikinci halkasında görünür oluyor — yani oyuncu, zaten
   kurması en zor olan şeyi kurduktan *sonra*. Combo satırı **1'den itibaren**
   ve **kalan süreyi gösteren bir çubukla** görünmeli.

3. **Yakın geçiş tamamen sessiz.** `VehiclePassed` koşunun en sık olayı
   (bölüm 4'te ~50 kez) ve hiçbir karşılığı yok. Perfect dodge'a
   *yaklaşan* ama tutturamayan geçiş, tutturamayan geçişten ayırt edilemiyor.
   Bir "whoosh" + `minDx` bandın hemen dışındaysa daha güçlü bir çeşidi,
   oyuncuya "az kaldı" sinyali verir — bu, §3.3(b) ile birlikte mekaniği
   keşfedilebilir kılan ikinci yarıdır.

**Sesin altyapısı hazır:** `EngineVoice` saf Kotlin sentez, dosya yok, APK
maliyeti sıfır (PROVENANCE "Ses varlıkları"). Kısa bir "tık/whoosh/çarpma"
eklemek yeni bir mimari gerektirmiyor, mevcut mikser bütçesine sığıyor
(motor 0.11, nitro 0.20, korna 0.30; ölçülen tepe < 0.9).

---

# 5. Aracın kimliği hissediliyor mu?

Dört çarpanın **tam katalog aralığı**nın oynanışa değeri, yükseltme sv.1'de,
bölüm 5+ tabanında (2.63):

| Eksen | Aralık | Sayısal karşılığı | Hissedilir mi |
|---|---|---|---|
| `topSpeedMul` | 0.92 – 1.12 | tavan **172 ↔ 193 km/h** = 21 km/h; tepki bütçesi 0.86 ↔ 0.75 s (**114 ms**) | ✅ evet — ama **zorluk** olarak, hız olarak değil |
| `brakeMul` | 0.90 – 1.12 | fren yetkisi 25.6 ↔ 31.8 km/h = 6.2 km/h, **yalnızca fren basılıyken** | 🟡 marjinal |
| `boostMul` | 1.00 – 1.12 | dolu bar 2.63 → 2.95 s (+0.32 s); sürdürülebilir açık kalma %35.6 → %38.2 → ortalama **+1.5 km/h** | 🟡 zar zor |
| `accelMul` | 0.92 – 1.14 | τ 181 ↔ 146 ms. 2.63 s'lik bir boost darbesinde alınan bonus oranı %93.1 ↔ %94.4 → **+0.7 km/h ortalama** | ❌ **hayır** |

`accelMul` hesabı: bir darbede alınan ortalama oran `1 − (τ/T)(1 − e^(−T/τ))`,
T = 2.63 s. Fark %1.3 × 1.8 birim = 0.023 birim = 0.7 km/h.
`DIFFICULTY_REVIEW` §6.4(b)'nin *"`accelMul` bugün neredeyse etkisiz"*
tespiti doğru; **sayısı budur: bütün 0.92→1.14 aralığı 0.7 km/h ediyor.**

## 5.1 İki uç araç arasında bir koşuda ne kadar fark oluşur?

Beety (`topSpeedMul 0.92`) vs Süper Araba (`1.12`), yükseltme sv.1,
bölüm 5 (45 s), %35 boost açık kalma:

| | Beety | Süper Araba |
|---|---|---|
| tavan | 172 km/h | 193 km/h |
| tavana ulaşma | ~21.2 s | ~24.9 s |
| **45 s'de kat edilen mesafe** | **~2075 m** | **~2192 m** |

→ **~117 m fark (+%5.6); Süper Araba'nın tempo­sunda ~2.0 saniyelik bir
avantaj.**

> ⚠ **MODEL, ÖLÇÜM DEĞİL.** `DIFFICULTY_REVIEW` §6.1'in iki fazlı skor
> modeliyle (A ≈ 68.3, k = 11/600) elle integre edildi. Tavan hızları ve
> yüzdeler koddan kesin; mesafeler ±%10 belirsizlikte.

## 5.2 İki sonuç

**(a) Araç kimliği tek boyutlu.** Oyuncunun *hissettiği* tek eksen
`topSpeedMul`; diğer üçünün toplamı ~2 km/h. Ama garaj kartı **dört çubuğu
eşit görsel ağırlıkta** gösteriyor (`CarCatalog.statFraction`, sapma #14).
Yani vitrin dörtte üçü ölçülemez olan bir vaat veriyor. Ya `accelMul` gerçek
hâle getirilmeli (ki `DIFFICULTY_REVIEW` §6.4(a) haklı olarak
`ACCEL_RATE_BASE`'i düşürmemeyi söylüyor — o zaman `accelMul` fren/boost
*sonrası toparlanmaya* bağlanmalı, ki orada τ farkı gerçekten hissedilir),
ya da çubuk "İVME" yerine dürüst bir şey söylemeli.

**(b) Beety, 4000 coinlik bir gerileme.** Katalogun **en pahalı** aracı
(`priceCoins = 4000`, `requiredCarLevel = 6`):

```
avantaj : accelMul 1.14 → +0.7 km/h   +   brakeMul 1.12 → +3.4 km/h fren
dezavantaj: topSpeedMul 0.92 → −21 km/h tavan (Süper Araba'ya göre)
                              −13 km/h tavan (standart Şehir'e göre)
```

3200 coinlik Süper Araba'dan **her ölçülebilir eksende daha yavaş** ve 800
coin daha pahalı. `BALANCE.md`'nin "her aracın bir zayıflığı olur" ilkesine
uyuyor ama fiyat merdiveninin **tepesine** konmuş. Ya fiyatı Kuş SLX
bandına (~1500) inmeli ya `topSpeedMul` 0.98–1.00'a çekilmeli.
**Bu bir ekonomi kararı** — `product-owner`'a not düşülmeli, burada
yalnızca sayısı veriliyor.

---

# 6. Ölüm anı

## 6.1 Ne oluyor

`onCrash()` (`GameEngine.kt:697-704`): `crashed = true`, −80 puan, combo 0,
`phase = CRASHED`, olay. **Partikül yok, çarptığı araç listeden silinmiyor,
enkaz/hasar çizimi hiç yok.**

UI (`GameScreen.kt:278-280`): `showCrashDialog = true` — **hepsi bu.**
`CrashOverlay` **aynı `withFrameNanos` geri çağrısında** kuruluyor, yani
**bir sonraki karede (~16–25 ms)** ekranı `0xB3010610` — **%70 opak, neredeyse
siyah** bir perde kaplıyor (`OverlayScrim`, `:1607-1617`) ve ortasına bir
kart geliyor.

Yani sıralama:

```
kare N     : çarpışma olur
kare N+1   : ekranın %70'i siyah, ortada diyalog
```

**Çarpışma anı diye bir şey yok.** Ne sarsıntı, ne flash, ne donma vuruşu,
ne yavaşlatma, ne kıvılcım, ne çarpma sesi. (`shake|flash|slowmo|freeze`
projede sıfır eşleşme.) Tek işitsel iz: bir sonraki kareden itibaren
`EngineSoundManager.idle()` çağrıldığı için motor **~0.1 s'de sönüyor** —
yani ses "çarptım" demiyor, "motor kapandı" diyor.

## 6.2 Oyuncu neden çarptığını anlıyor mu? — Hayır, ve sebebi düzeltilebilir

Simülasyon donuyor (`step()` içinde `CRASHED -> Unit`, `:347`) ama çizim
devam ediyor, çarpılan araç hâlâ sahnede. **Yani bilgi ekranda var** —
sonra %70 siyah perdenin arkasına gömülüyor, aynı karede. Oyuncunun
"hangi araca, neden, hangi şeritte" sorusuna bakacak **tek bir karesi bile**
yok.

**Öneri — "çarpışma vuruşu" (crash beat), üç parça:**

1. **Overlay'i 300 ms geciktir.** `showCrashDialog` bir
   `LaunchedEffect { delay(300); showCrashDialog = true }` arkasına alınsın.
   Bu süre boyunca sahne zaten donuk ve çizilmeye devam ediyor — yani
   **ek maliyet sıfır**, oyuncu sadece kendi hatasını görüyor.
2. **Temas noktasında partikül.** `onCrash()` içinde
   `addParticles(playerX, playerY, boostTrail = false)` — mekanizma coin için
   zaten var (12 partikül, 0.4–0.6 s ömür), tek satır. Kırmızı/turuncu bir
   renk çifti eklenirse kimlik de kazanır.
3. **Çarpma sesi.** `EngineVoice`'a kısa (~0.25 s) bir gürültü patlaması +
   hızlı sönümlü alçak ton. Dosya yok, APK maliyeti 0. Mevcut nitro
   sentezinin (`:232-259`) yapısı birebir kullanılabilir.

Üçü birlikte, "çarptım" ile "menü açıldı" arasına oyuncunun sebebi görebildiği
bir an koyar. Bu, listedeki tek en büyük his kazancı — çünkü **her başarısız
koşu buradan bitiyor** ve oyun 4. bölümde takılan oyuncu için en sık gördüğü
an bu.

## 6.3 "Havaya çarptım" hissi kaldı mı? — Hayır, ama komşusu kaldı

`PROVENANCE` #6/#11 ve `GameConfig.kt:27-59`'daki düzeltme **çalışıyor**:
çarpışma kutusu artık çizimden türetiliyor (`hitW = 28.16`, çizim genişliği
32) ve **görselden 3.84 dp dar** — yani kutu gerçekten görünenin içinde.
Her gövde aynı kutuya sığmak zorunda ve `CarCatalogTest` bunu doğruluyor.
Sprite'lara geçişte de oran korunmuş (#16). **Bu konuda bir kalıntı
bulamadım.**

İki uyarı:

- `BALANCE.md:104` ve `GameConfig.kt:306` hâlâ *"çarpışma `|dx| < 42`"*
  diyor. Gerçek sınır **28.16**. `DIFFICULTY_REVIEW` §2.3 bunu zaten
  yakalamış; düzeltilmeden başka biri bu sayıya güvenip yeniden ayar yapar.
- **Dikey tarafta bir asimetri var:** `verticallyOverlapping` testi
  (`GameEngine.kt:562-563`) `hitH = 53.5` kullanıyor, `playerVisualBottom`
  ise `CAR_HEIGHT_PX = 60.8`. İkisi farklı; çarpışma dar, "geçildi"
  değerlendirmesi geniş. Bu **doğru yönde** bir tutarsızlık (kaza dar, puan
  cömert) ama belgesiz. Bir yorum satırı hak ediyor.

---

# 7. Öneriler — öncelik sırası

> Hiçbiri denenmedi. Her birinin sonrasında `LevelCurveTest` ve
> `GameEngineTest` yeniden yeşillenmeli; 1, 3 ve 7 denge değiştiriyor.

### 1. `LevelDef.speedRampScale` — **`min()` sonucuna** uygula, testle kilitle
**Ne:** `GameEngine.updateSpeed`'de
`baseSpeed + min(scoreCap, score/600) * (level?.speedRampScale ?: 1f)`.
Değerler `DIFFICULTY_REVIEW` §6.6'daki tablo (bölüm 1–7: 0.40 / 0.50 / 0.60 /
0.65 / 0.75 / 0.70 / 0.85), bölüm 8–30 varsayılan 1.0.
**Neden:** SPEED yükseltmesini değerinin %65'inde korur (bölüm 4: yükseltmesiz
141, tam yükseltmeli 164 km/h), aracın `topSpeedMul`'unu da orantılı taşır,
boost'u hiç ölçeklemez (141 → 197). Alternatif C yükseltmeyi öldürür,
alternatif D bölüm 1'i tam yükseltmeli oyuncuda 100 yerine 136 km/h yapar.
Gerçek oyuncunun sahip olduğu SPEED sv.2–3'te B ile D farkı **1–2 km/h**.
**Büyüklük:** bölüm 4 tepki bütçesi 0.82 → 1.11 s (yükseltmesiz),
0.66 → 0.90 s (tam yükseltmeli). **Yan koşul:** KDoc'a hangi terimin
çarpıldığı yazılsın + *"σ yükseltme tavanını da ölçekler"* testi eklensin,
yoksa bir sonraki oturum C'yi uygular.

### 2. Garaj SPEED göstergesini düzelt — `scoreSpeedCap(level, car)` + mutlak yerine **fark**
**Ne:** `UpgradeCatalog.displayValue` bugün aracı **hiç** hesaba katmıyor.
**Neden:** Süper Araba'nın gerçek tavanı 193 km/h, garaj 180 yazıyor;
Beety 172, garaj yine 180. σ geldiğinde üstüne bölüm sapması da binecek.
Sahibinin *"oyuncu parasının karşılığını göremiyor"* endişesinin gerçek
kaynağı fizik değil **bu satır**.
**Büyüklük:** bugün 8–13 km/h'lik sessiz hata; σ sonrası bölüm 4'te 52 km/h.
**Sabit:** `UpgradeCatalog.kt:142-158` (motor sabiti değişmiyor).

### 3. `PERFECT_DODGE_WINDOW_RATIO` 0.5 → **0.80**
**Ne:** eşik 49.6 → 60.16 dp (360 dp ekran).
**Neden:** cihazda (40 FPS) pencere bugün **tek kare, 25 ms** — bir kare geç
kaza, bir kare erken saymıyor; 31 FPS'te *başka* bir tek kare, yani kas
hafızası kurulamıyor.
**Büyüklük:** 40 FPS'te 1 → **3 kare (75 ms)**, 60 Hz'de 3 → 6 kare.
Yan şeritteki sabit araca pay 7.0 dp (0.85 önerisinde 5.3) — bedava dodge
riski yok, dar ekranda da (320 dp) 5.6 dp pay kalıyor.
**Sabit:** `GameConfig.PERFECT_DODGE_WINDOW_RATIO`. Prototip sapması değil
(perfect dodge prototipte yok, PROVENANCE #4).

### 4. Çarpışma vuruşu — 300 ms gecikme + partikül + ses
**Ne:** `showCrashDialog` 300 ms geciktirilsin; `onCrash()` partikül üretsin;
`EngineVoice`'a ~0.25 s'lik çarpma sentezi eklensin.
**Neden:** bugün oyuncu, çarpıştığı kareden **sonraki karede** %70 opak siyah
bir perde görüyor. Neden çarptığını görebileceği tek bir kare yok. Her
başarısız koşu buradan bitiyor.
**Büyüklük:** 300 ms; partikül mekanizması coin için zaten var (tek satır);
ses APK'ya 0 bayt ekler (her şey sentezleniyor).
**Yer:** `GameScreen.kt:278-280`, `GameEngine.onCrash`, `EngineVoice`.

### 5. Perfect dodge'un canlı göstergesi — çizim tarafında
**Ne:** bir trafik aracı dikey örtüşürken ve `|dx|` bandın (28.16 … eşik)
içindeyken iki araç arasına kıvılcım/ışık çizgisi çiz.
**Neden:** pencere genişlese bile tetikleyici görünmez; bugünkü tek geri
bildirim olay bittikten *sonra* çıkan 0.9 s'lik yazı. Oyuncuya bir beceri
değil piyango öğretiliyor. Metinle anlatmak yetmez — mekanik mesafeyle
tanımlı, sözle değil.
**Büyüklük:** motor değişmez, denge değişmez. `GameRenderer` zaten her karede
`obstacles`'ı geziyor ve `playerX` elinde.

### 6. Haptik + ses bütçesini doğru olaylara taşı
**Ne:** perfect dodge, coin ve çarpışmaya `performHapticFeedback`; perfect
dodge, coin ve yakın geçişe kısa sentez sesi. Combo satırı **1'den itibaren**
ve kalan 6 saniyeyi gösteren çubukla görünsün.
**Neden:** bugün titreşim yalnızca **şerit değiştirmede ve kornada** var —
en anlamsız olaylara harcanmış. Coin, perfect dodge, combo bozulması ve
çarpışmanın **hiçbirinin sesi yok**. Combo 1'de HUD hiçbir şey göstermiyor,
yani zincir ancak ikinci halkasında görünür oluyor.
**Büyüklük:** 3 `performHaptic` satırı + 3 kısa sentez; APK'ya 0 bayt.
**Yer:** `GameScreen.kt:267-283` olay anahtarı, `:765-772` combo satırı.

### 7. `accelMul` ya gerçek olsun ya vitrinden insin — ve Beety'nin fiyatı
**Ne:** `accelMul`'ın tüm 0.92→1.14 aralığı **0.7 km/h** ediyor (τ 181 vs
146 ms, 2.63 s'lik darbede alınan bonus %93.1 vs %94.4). Garaj kartı yine de
dört çubuğu eşit ağırlıkta gösteriyor. Ayrıca Beety 4000 coin ile katalogun
**en pahalısı** ve Süper Araba'dan (3200) her ölçülebilir eksende yavaş
(`topSpeedMul` 0.92 vs 1.12 = 21 km/h tavan farkı).
**Neden:** vitrin, dörtte üçü ölçülemez olan bir vaat veriyor — bu, sapma
#14'ün çözmeye çalıştığı şikâyetin aynısı.
**Büyüklük:** `topSpeedMul` tek başına 21 km/h ve 114 ms tepki bütçesi;
diğer üç eksenin toplamı ~2 km/h. Bir koşuda iki uç araç arası fark
**~117 m / ~2.0 s** (45 s'lik bölüm 5, model).
**Not:** fiyat kararı `product-owner`'ın; burada yalnızca ölçü veriliyor.
`ACCEL_RATE_BASE` **düşürülmesin** (`DIFFICULTY_REVIEW` §6.4-a haklı).

### 8. Şerit değişiminde gövde yatması — yalnızca çizim
**Ne:** `playerX`'in kare farkına orantılı ±6–8° eğim + gölge kayması.
**Neden:** oyuncu bugün "araba sürüyorum" değil "şerit seçiyorum" hissediyor.
Sebep `LANE_LERP_RATE = 16` **değil** (o sağlam: 40 FPS'te 2 karede çarpışma
kutusundan çıkıyor, 150 ms'de oturuyor, kare hızına duyarsız), sebep oyuncunun
sahip olduğu yanal durumun yokluğu: yanal hız yok, şeritler arasında durulamaz,
başlamış manevra iptal edilemez. Lerp oranını değiştirmek işi kötüleştirir.
**Büyüklük:** motor ve denge sıfır değişir, PROVENANCE sapması olmaz.
`CarArtwork` zaten dönüşüm alıyor.

---

## Öneri dışı — bulundu, kayda geçiyor

- **`BrakeTapsAtMost` sömürüsü.** `brakeTaps` yalnızca basma kenarını sayıyor
  (`GameEngine.kt:280`); bölüm 25'in "frene en fazla 1 kez bas" yıldızı freni
  koşu boyunca **basılı tutarak** tutturulur. Hedef "fren kullanma" demek
  istiyor, ölçtüğü "parmağını kaldırma".
- **Girdi tamponu yok.** `phase != RUNNING` iken dokunuş sessizce düşüyor
  (`GameEngine.kt:231`) — geri sayımın ve çarpışmanın son karesinde 25 ms'lik
  kör nokta.
- **Hız göstergesi üst uçta yalan söylüyor.** `SPEEDOMETER_MAX_KMH = 240`;
  bölüm 5+ tabanında SPEED 8 + BOOST 8 basılıyken gerçek değer **284 km/h**,
  gösterge 240'a çakılı. "Üst yarıya yalnızca boost'la çıkılır" vaadi tam
  yükseltmeli oyuncuda göstergeyle doğrulanamıyor. σ (öneri 1) erken
  bölümlerde bunu kendiliğinden düzeltiyor (bölüm 4: 220 km/h, çakılmıyor)
  ama geç bölümlerde duruyor.
- **Bölüm 6 hâlâ bir duvar.** `MIN_STARS_TO_PASS = 2` uygulanmış ✅ ve bölüm 4
  düzelmiş (`[Bitir, 1800 puan, 3 dodge]` doğru sırada). Ama bölüm 6 hâlâ
  `[10 coin, 4 dodge, 3x combo]` — ilk **iki** hedefin ikincisi beceri hedefi,
  yani perfect dodge yine zorunlu. `DIFFICULTY_REVIEW` §7.2-B'nin bölüm 6/7
  satırları uygulanmamış. **Ürün/senaryo alanı**, ama oynanış sonucu doğrudan:
  §3.2'deki tek-kare penceresiyle birleşince kariyer bölüm 6'da bitiyor.
- **`|dx| < 42` yorumu iki yerde hâlâ yanlış** (`GameConfig.kt:306`,
  `BALANCE.md:104`); gerçek 28.16. Ayrıca dikey testte `hitH = 53.5` ile
  `playerVisualBottom = 60.8` farklı — doğru yönde ama belgesiz.

---

## Doğrulanmadı — açıkça

- Cihazda **hiçbir şey denenmedi**; build çalıştırılmadı.
- ~40 FPS varsayımı `docs/` içindeki önceki ölçümlerden alındı, bu oturumda
  ölçülmedi. §3.2'nin sonucu bu sayıya duyarlı: 60 Hz'de pencere 3 kare,
  40 FPS'te 1 kare. **Öneri 3'ten önce gerçek kare hızı `dumpsys gfxinfo`
  ile doğrulanmalı** (PROVENANCE #16'nın uyarısı gereği **tema sabitlenerek**).
- §5.1'deki mesafe/süre sayıları `DIFFICULTY_REVIEW` §6.1'in skor modelinden
  elle integre edildi; ±%10 belirsizlik. Tavan hızları ve yüzdeler koddan
  kesin.
- §1'deki coin tahmini `GameConfig.SCORE_PER_BONUS_COIN` yorumundaki
  **ölçülmüş** 118 coin/bölüm değerine dayanıyor; oyuncunun coin'i nereye
  harcadığı varsayım.
