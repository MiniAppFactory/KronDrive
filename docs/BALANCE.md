# Denge — sayılar ve nereden geldikleri

**Son eşitleme: 2026-08-17.** Bu belge kodla satır satır karşılaştırılarak
yeniden yazıldı. Önceki hâli üç ayrı incelemede (`REVIEW_PRODUCT.md`,
`REVIEW_REGRESSION.md`, `ECONOMY_BALANCE_PROPOSAL.md`) kodla çelişir bulundu.

## Bu belge nasıl okunur

Her sayının yanında **dosya:satır** var. Belge ile kod çelişirse **kod
doğrudur** — belgeyi düzeltin, kodu değil. Satır numaraları 2026-08-17
tarihli `main` (`2608363`) durumundadır; kod değişince kayabilirler, sabit
adı kaymaz.

Tüm ayarlanabilir değer dört dosyada:

| Ne | Nerede |
|---|---|
| Fizik, skor, ödül, reklam sabitleri | `game/GameConfig.kt` |
| Bölüm hedefleri ve zorluk | `game/LevelCatalog.kt` |
| Yükseltme maliyet/etkileri | `game/UpgradeCatalog.kt` |
| Araç ve boya fiyatları, sürüş çarpanları | `game/CarCatalog.kt` |
| Ölçü sınıfları (çizim + çarpışma kutusu) | `game/VehicleClass.kt` |
| Görev ödülleri | `data/DailyChallengeGenerator.kt`, `data/WeeklyMissionGenerator.kt` |
| Booster fiyatları, başlangıç coini | `data/PlayerProgress.kt` |

Başka hiçbir yerde denge sabiti yoktur.

### Ölçüm aracı

`LevelCurveTest` bölümleri gerçekten oynar. İki otopilot var:

- **TEMKİNLİ** (`Style.SAFE`): hiç risk almaz, şerit değiştirmeyi erken
  yapar, frene hiç basmaz. Neredeyse hiç Perfect Dodge yapmaz.
- **RİSKLİ** (`Style.RISKY`): yan araca bilerek yanaşıp son anda çekilir.

İkisi de **bilerek vasat**; mükemmel zamanlama yapmazlar. Otopilotun
başaramadığı hedef yeni oyuncu için de fazla zordur
(`LevelCurveTest.kt:8-27`).

İki döküm testi var (ikisinde de assert yok — sayı basarlar):

| Test | Kapsam | Çıktısı kayıtlı mı |
|---|---|---|
| `olcum dokumu` (`:297`) | ilk 8 bölüm, **tek tohum**, TEMKİNLİ + RİSKLİ | ✔ `docs/CHANGELOG.md:13-23` (2026-08-16) |
| `tam olcum dokumu — otuz bolum` (`:321`) | **30 bölüm**, üç tohum, medyan | ✘ **hiçbir dosyada yok** |

⚠ **Bu belgedeki her ölçülmüş coin/skor sayısı 8 bölümlük, tek tohumlu
dökümden gelir.** 9–30. bölümler için ölçüm yoktur; 30 bölümü oynatan test
yazılmış ama çıktısı bir kez bile kaydedilmemiştir. Bkz.
`docs/ECONOMY_STATUS_20260817.md` Öneri 1.

Ölçüm ekranı 360 × 800 dp varsayar (`LevelCurveTest.kt:341`).

---

## 1. Prototipten gelen çekirdek

HTML prototipi (`KRON_DRIVE_FINAL_BALANCED_80_3.html`) oyun hissinin doğruluk
kaynağıdır. Bilinçli sapmaların tamamı `PROVENANCE.md`'de yazılı.

| Değer | Kod | Sabit | Kaynak (HTML) |
|---|---|---|---|
| Taban hız | 2.63 | `GameConfig.kt:160` | `currentSpeed()` |
| Skordan gelen hız | `min(3.2, skor/600)` | `:161`, `:164` | `currentSpeed()` |
| En düşük hız | 2.0 | `:165` | — |
| Boost hız bonusu | +1.8 | `:168` | `keys.Shift` dalı |
| Fren cezası | −0.9 | `:171` | `keys.Space` dalı |
| Hız → piksel (prototip) | ×250 px/s | `:177` | `speed * 250 * dt` |
| **Hissedilen hız çarpanı** | **×0.75** | `:202` | *(prototipte yok — sapma)* |
| Hız → piksel (gerçek) | ×187.5 px/s | `:204` | türetilmiş |
| Skor kazancı | hız × 11 /s | `:313` | `state.score += speed*11*dt` |
| Geçilen araç | +8 puan | `:314` | `state.score += 8` |
| Coin | +35 puan, +12 boost | `:315`, `:316` | coin toplama bloğu |
| Çarpışma | −80 puan, koşu biter | `:317` | çarpışma bloğu |
| Boost tüketimi | 38 /s | `:221` | `state.boost -= 38*dt` |
| Boost dolumu | 15 /s (frendeyken 10) | `:222`, `:238` | `keys.Space ? 10 : 15` |
| Boost yeniden tutuşma | ≥8 enerji **ve** parmağın kalkması | `:235` | *(prototipte yok)* |
| Engel doğma | 0.78 s | `:270` | `spawnAcc > 0.78` |
| Coin doğma | 1.05 s | `:271` | `coinAcc > 1.05` |
| Yol genişliği | `min(290, W×0.56)`, 3 şerit | `:20-22` | `metrics()` |
| Oyuncunun ekrandaki yeri | alttan 210 px | `:25` | `player.y = H - 210` |
| Şerit değiştirme oranı | 16 /s (~0.06 s) | `:70` | prototip 12, hızlandırıldı |
| Hız göstergesi | `((hız−2)/5.7)×180+60` km/h | `:365-368`, `:382` | `drawSpeedometer()` |
| Kare başına en büyük dt | 0.032 s | `:598` | prototip |
| Geri sayım | 3 s | `:594` | prototip 5 idi |

**Araç ölçüsü artık 42 × 90 değil.** Prototipin çarpışma kutusu 42 × 90'dı ama
çizim onu doldurmuyordu; 2026-08-13'te kutu çizimden türetilir hâle geldi
(`GameConfig.kt:27-63`). Bugünkü referans (BINEK) ölçüleri §4'te.

Taban hızda gösterge ~80 km/h okur. Metre dönüşümü (`PIXELS_PER_METER`,
`GameConfig.kt:380` = 29.6 × 0.75 = **22.2 px/m**) bu göstergeyle tutarlı
seçildi: 80 km/h = 22.2 m/s. `WORLD_SPEED_SCALE` her ikisini aynı oranda
küçülttüğü için mesafe hedefleri (`ReachDistance`, günlük görev "3000 m")
değişmedi.

---

## 2. Hız modeli ve bölüm rampası

Motorun her karede hesapladığı hedef hız (`GameEngine.updateSpeed`,
`GameEngine.kt:436-447`):

```
hedef = tabanHız + speedRampScale × min(hızTavanı, skor / 600)
```

- `tabanHız`: bölüm kendi başlangıç hızını verirse ondan
  (`GameEngine.kt:106-107`), yoksa `BASE_SPEED` 2.63 (= 80 km/h).
- `hızTavanı`: `UpgradeCatalog.scoreSpeedCap(seviye, araç)` — yükseltme ve
  araç çarpanı burada (`UpgradeCatalog.kt:72`, `:119-120`).
- `speedRampScale`: **bölüm başına rampa çarpanı** (`GameModels.kt:243`).

### `speedRampScale` — 2026-08-17'de eklendi

Sahibi: *"4. levelde 30. saniyede max hıza standart araba ile ulaşmak doğru
değil."* Ölçüm onu doğruladı ve daha kötüsünü buldu: bölüm 4 tavana ~23
saniyede ulaşıyor ve süresinin **%42'sini** tavanda geçiriyordu
(`docs/DIFFICULTY_REVIEW.md`, commit `6d2b2a4`).

⚠ **Çarpan `min(...)` ifadesinin SONUCUNA uygulanıyor** — tavanı sabitlemez,
**orantılı küçültür**. Reddedilen alternatif (yalnızca rampayı yavaşlatıp
tavanı bırakmak) ölçüldü: tavan değişmediği için koşu sonunda tepki bütçesi
hiç iyileşmiyor ve **tam yükseltmeli oyuncuda hiçbir şey değişmiyordu**
(`GameModels.kt:220-231`). İki test bu seçimi kilitliyor.

Global ivmeye (`ACCEL_RATE_BASE`) dokunulmadı: sahibinin ilk sezgisi oydu ama
ölçüm bir boost darbesinden alınan etkinin %94 → %81'e ineceğini gösterdi
(commit `6d2b2a4`).

### Başlangıç hızı: 30 bölümün 30'unda **60 km/h**

2026-08-17'de eşitlendi (`LevelCatalog.kt:25-32`). Eskiden 60/65/70/75
rampalıydı ve 9. bölümden sonrası varsayılana (80 km/h) düşüyordu. İki ayrı
rampa (başlangıç hızı + `speedRampScale`) üst üste biniyordu; sahibi bölüm
4'ün ilk saniyeden hızlı başladığını cihazda fark etti.

`speedFromKmh(60)` = **2.0** = `MIN_SPEED`. Yani her kariyer koşusu tabanın
en altından başlar; ilerlemeyi artık tek başına `speedRampScale` taşıyor.

Tavan hız = `speedToKmh(2.0 + s × 3.2)`, yükseltmesiz ve çarpanı 1.00 olan
referans araçla:

| Bölüm | `speedRampScale` | Tavan hız |
|---|---|---|
| 1 | 0.40 | 100 km/h |
| 2 | 0.48 | 108 |
| 3 | 0.56 | 116 |
| 4 | 0.65 | 125 |
| 5 | 0.74 | 134 |
| 6 | 0.82 | 142 |
| 7 | 0.91 | 151 |
| 8–30 | **1.00** (varsayılan) | 161 |

Kaynak: `LevelCatalog.kt:77, 90, 105, 123, 137, 152, 171`; 8+ için alan
yazılmadığı için varsayılan 1.0 (`GameModels.kt:243`).

⚠ **`DIFFICULTY_REVIEW.md:475-481` farklı sayılar veriyor** (0.40/0.50/0.60/
0.65/0.75/0.70/0.85 ve 100/115/130/141/156/146/166 km/h). O belge bir
**öneridir**; koda giren değerler yukarıdakilerdir. Çelişkide kod doğrudur.

Boost bunun **üstüne** +1.8 birim (≈ +57 km/h) ekler; gösterge tavanı
240 km/h'dir (`GameConfig.kt:368`). Tam SPEED yükseltmesiyle (seviye 8) tavan
bölüm 1'de 114, bölüm 8+'da **196 km/h** olur.

### ⚠ Garajdaki hız sayısı kariyerde geçerli değil

`UpgradeCatalog.displayValue` (`UpgradeCatalog.kt:153-155`) hızı
`BASE_SPEED + scoreSpeedCap(...)` ile hesaplıyor, yani **2.63** tabanla:
seviye 1 için **180 km/h**, seviye 8 için **216 km/h**.

Ama artık hiçbir kariyer bölümü 2.63'ten başlamıyor; hepsi 2.0'dan başlıyor.
Gerçek kariyer tavanı (rampa 1.00, seviye 1) **161 km/h**, seviye 8'de
**196 km/h**. Garajın yazdığı sayı yalnızca **sonsuz modda** doğrudur (orada
`level = null` ve taban 2.63 kalır — `GameEngine.kt:106-107`).

Bu, 2026-08-16'da kapatılan kusurun ("garaj her araç için aynı hızı yazıyordu")
aynı ailesinden. **Karar bekliyor**, kod değiştirilmedi.

---

## 3. Skor eğrisi (hedefler buradan hesaplandı)

Skor kendi kendini hızlandırır: hız skorla artar, skor hızla artar.

```
skor(t) ≈ 600 × tabanHız × (e^(t/54.5) − 1) + 8×geçilenAraç + 35×coin
```

Prototip tabanıyla (2.63 / 80 km/h):

- 0–43 s: üstel artış, `skor(t) ≈ 1577 × (e^0.01833t − 1)`
- t ≈ 43 s: skor 1920'ye ulaşır, hız tavanı (5.83) dolar
- sonrası: sabit ~64 puan/s

| Koşu süresi | Beklenen skor (iyi oyuncu, tam rampa) |
|---|---|
| 45 s | ~3.000 |
| 60 s | ~4.300 |
| 75 s | ~5.600 |
| 90 s | ~6.900 |

`LevelCatalog` içindeki puan hedefleri bunun **%75–85'i** olacak şekilde
seçildi.

⚠ **Bu eğri 2.63 tabanlıdır ve artık ilk 7 bölümde geçerli değil.** Başlangıç
hızı 2.0'a inip `speedRampScale` devreye girince o bölümlerin skoru daha yavaş
birikiyor. Ölçülen etki (temkinli oyun, mesafe — commit `6d2b2a4`):

| Bölüm | Rampa öncesi | Rampa sonrası | Fark |
|---|---|---|---|
| 1 | 862 m | 691 m | −%20 |
| 4 | 1788 m | 1522 m | −%15 |
| 8 (rampasız) | 1200 m | 1200 m | değişmedi |

Otuz bölümün hepsi hâlâ geçilebilir (aynı commit, `LevelCurveTest`).

---

## 4. Ölçü sınıfları — araç seçimi artık çarpışma kutusunu değiştiriyor

2026-08-16'da katalog ilk kez BINEK dışına çıktı (`game/VehicleClass.kt`,
tam tasarım `docs/VEHICLE_CLASSES.md`). `PROVENANCE #6`'nın "araç seçimi
çarpışmayı değiştirmez" kuralı **"AYNI SINIF İÇİNDE değiştirmez"** olarak
daraltıldı.

| Sınıf | Çizim birimi | Çizim px | Çarpışma kutusu px | Referans oranı |
|---|---|---|---|---|
| `MOTOSIKLET` | 22 × 59 | 17.6 × 47.2 | **15.49 × 41.54** | 0.373 (ref. 0.375) |
| `BINEK` | 40 × 76 | 32.0 × 60.8 | **28.16 × 53.50** | 0.526 |
| `AGIR` | 48 × 202 | 38.4 × 161.6 | **33.79 × 142.21** | 0.238 (ref. 0.237) |

Kaynak: `VehicleClass.kt:52, 61, 72`; px değerleri `CAR_ART_SCALE` 0.80
(`GameConfig.kt:43`) ve `HITBOX_SCALE` 0.88 (`:56`) ile türetilir
(`VehicleClass.kt:89-98`).

**BINEK ÇIPADIR: asla değişmez** (`VehicleClass.kt:55-61`). Bütün denge,
bütün bölüm hedefleri ve bütün yükseltme eğrileri onun davranışına göre
ayarlandı. Trafik aracı da BINEK'tir (`GameEngine.kt:554-560`).

Genişlik tasarım kararıdır, **boy referans çiziminin oranından türetilmiştir**
— oran tutmazsa fark saydam boşluğa döner ve oyuncu aracın yanındaki boşluğa
çarpar (`VehicleClass.kt:16-25`).

### Çarpışma sonuçları (`VEHICLE_CLASSES.md` §2)

Minkowski toplamıyla, motosiklet oyuncu vs. binek trafik:

| | Otomobil oyuncu | Motosiklet oyuncu | Fark |
|---|---|---|---|
| Yatay çarpma eşiği | 28.16 dp | 21.83 dp | −%22.5 |
| Dikey örtüşme eşiği | 53.50 dp | 47.52 dp | −%11.2 |
| "Tehlike dikdörtgeni" alanı | 1.00 | 0.688 | **−%31** |

**Bu −%31 kâğıt üzerindeki tavandır, ölçülmüş avantaj değildir.** Aynı
şeritteyken hiçbir şey değişmez (trafik şerit merkezine kilitli, `dx = 0`) ve
şerit değiştirme 0.06 saniye sürüyor. Gerçek kazanç "kenardan sıyırma" anına
sıkışmış durumda ve **ölçülmedi** (`VEHICLE_CLASSES.md:183-196`).

### Şerit uyumu (en kötü durum: 320 dp ekran, şerit 59.73 dp)

| Sınıf | Görsel dp | Şeridin %'si | Çarpma eşiği | Güvenli mi |
|---|---|---|---|---|
| `MOTOSIKLET` | 17.60 | %29.5 | 21.83 | ✔ (pay 37.9) |
| `BINEK` | 32.00 | %53.6 | 28.16 | ✔ (pay 31.6) |
| `AGIR` | 38.40 | %64.3 | 30.98 | ✔ (pay 28.8) |

İki `AGIR` yan yana bile sığıyor (33.79 < 59.73). **Tır şeride sığıyor.**
Tırın uzunluğu bir özellik değil **bedel**: kutusu 142 dp, yandan geçen
trafik çok daha uzun süre temas riskinde.

Doğma yüksekliği de sınıfa bağlı (`GameConfig.kt:132`,
`obstacleSpawnY`): sabit −150 binek için doğruydu ama AGIR 161.6 px uzun,
yani tır orada doğsa **arkası ekranın içinde** belirirdi.

---

## 5. Trafik hızı (2026-08-14)

Sahibi: *"Yoldaki arabalar hareket etmiyor, park etmiş gibiler."* Haklıydı —
engel `speed × K × dt × speedMul` (1.00–1.14) ile akıyordu, asfalt ise tam
`speed × K × dt`. Engeller **asfalta göre duruyor ya da geri geri gidiyordu**.

```
ekrandaAşağıHız = (oyuncuHızı − aracHızı) × WORLD_PX_PER_SPEED_UNIT
aracHızı        = koşununTabanHızı × r,   r ∈ [0.45, 0.58]   (doğumda sabit)
```

`GameConfig.kt:305-306`.

| Değer | Eski | Yeni | Neden |
|---|---|---|---|
| Yaklaşma hızı (taban hız 2.63) | 2.81 birim (527 px/s) | 1.28 birim (240 px/s) | araç ilerlediği için fark azalır |
| Boost'un yaklaşmaya etkisi | 2.81 → 4.74 (+%69) | 1.28 → 3.08 (+%141) | trafik hızı **taban** hıza bağlı |
| Aynı anda ekrandaki araç | ~1.5 | ~3 | tehdit daha uzaktan görünüyor |
| Saniyede geçilen araç | 1 / 0.78 | 1 / 0.78 | **değişmedi** |
| Araçlar arası tepki süresi | 0.78 s | 0.78 s | **değişmedi** |

Spawn aralığına neden dokunulmadı: doğma **zamana** bağlı, mesafeye değil.
`saniyedeki geçilen araç = 1 / aralık` ve bu değer yaklaşma hızından
bağımsızdır. Değişen tek şey aracın ekranda görünür kalma süresi — bu zorluk
değil **görünürlük** artışıdır. Bölüm bazında seyreltme
`LevelDef.trafficDensity` ile yapılır (`GameModels.kt:257`).

`r` aralığı neden dar (1.29 kat): fark büyüdükçe hızlı araçlar yavaşlara
yetişip aynı y'de kümelenir ve 3 şeridi birden kapatabilir. Bu aralıkta bir
araç tüm yaklaşma boyunca en fazla ~120 px yetişir, iki araç arası mesafe ise
~190 px.

Kenar durum: oyuncu frene basıp trafiğin altına düşerse yaklaşma hızı negatife
döner. Ekranın **üstünden** çıkanlar temizleniyor
(`OBSTACLE_DESPAWN_TOP_MARGIN_PX`, `GameConfig.kt:153`); `evaluated` bayrağı
korunduğu için geri gelen araç ikinci kez puan/dodge vermez
(`GameEngine.kt:680-692`).

**Geçiş çizgisi engelin BURNUNDAN hesaplanır**, kuyruğundan değil
(`GameEngine.kt:659-670`) — tırın kuyruğu çok önce geçerdi.

---

## 6. Perfect Dodge

Eşik = çarpışma sınırı ile şerit aralığının tam ortası
(`GameConfig.kt:338-344`):

```
eşik = 32 + (şeritAralığı − 32) × 0.5        // 32 = CAR_WIDTH_PX (BINEK çizim genişliği)
```

Sınıflı dünyada motor buna bir **çift düzeltmesi** ekler
(`GameEngine.kt:721-724`): taban terim çiftin yarı genişliğidir. Binek–binek
çiftinde düzeltme tam olarak `0f`, yani eski davranış bit bazında korunur.

| Ekran | Şerit aralığı | Eşik (binek–binek) | Geçerli pencere |
|---|---|---|---|
| 320 dp | 59.73 | 45.87 | 28.16 – 45.87 |
| 360 dp | 67.20 | 49.60 | 28.16 – 49.60 |
| 411 dp | 76.72 | 54.36 | 28.16 – 54.36 |
| 600 dp+ (tavan) | 96.67 | 64.33 | 28.16 – 64.33 |

Düzeltme olmasaydı motosiklet oyuncunun penceresi 360 dp'de 21.44 → 27.78 dp
olurdu, **%30 daha geniş**: dar araç hem daha az çarpar hem daha çok combo
yapardı. Düzeltmeyle motosiklet penceresi 20.58 dp (bineğe göre −%4), tır
trafiğinde 21.82 dp (+%1.8).

Sabit bir piksel değeri kullanılamaz: motor dp uzayında çalışır ve şerit
aralığı ekran genişliğine bağlıdır. Birim test bu değişmezi her ekran
genişliği için doğruluyor.

Combo çarpanları 1× / 1.2× / 1.5× / 2× / **3× (5 ve üstü)**
(`GameConfig.kt:352`). Zincir 6 saniye yeni dodge gelmezse kopar (`:347`).
Taban dodge puanı 25 (`:349`). **Combo 6, 7, 8 hiçbir ek ödül vermez** —
çarpan dizisi 5'te doyuyor.

### ⚠ Perfect dodge cihazda tek karelik bir pencere

40 FPS'te dodge penceresi **~25 ms**; insan tepki tabanı ~250 ms
(`docs/REVIEW_GAMEPLAY.md`). Mekanik bir **ödül** olarak duruyor (skor +
combo verir) ama artık hiçbir bölümde **şart değil** — bkz. §7.

---

## 7. Bölüm hedefleri

### Kural: iki görev bölümü açar, üçüncüsü ustalık yıldızıdır

`MIN_STARS_TO_PASS = 2` (`GameConfig.kt:430`). Yıldızlar **sıralı** kazanılır
(`LevelEvaluator`), yani beceri hedefi **üçüncü sırada** olduğu sürece kimseyi
tıkamaz.

Bu sabit bir hatanın dersidir (`GameConfig.kt:415-429`):

1. Başlangıç: `stars > 0`. İlk 8 bölüm açıkça buna göre tasarlandı.
2. 2026-08-15: sahibi *"görevleri tamamlamadıysa neden geçiyor ki"* dedi,
   kural **üçü de** oldu. Bölüm tasarımı güncellenmedi; güvenlik payı sessizce
   yok oldu.
3. 2026-08-16: `DIFFICULTY_REVIEW.md` duvar sayısının 6'dan 19'a çıktığını
   ölçtü. Sahibi **2**'de karar kıldı.

### Perfect Dodge hedefleri katalogdan tamamen kaldırıldı (2026-08-16)

Sahibi: *"dodge hedeflerden kalksın, çok zorlaştırıyor çünkü."* İki ölçüm
destekliyor (`LevelCatalog.kt:52-61`):

- Otuz bölümün tamamında temkinli oyun **tek bir dodge bile** yapmıyor
  (`LevelCurveTest`, `tam olcum dokumu`). Yani dodge hedefi olan her bölüm
  temkinli oyuncu için duvardı.
- Cihazda dodge penceresi tek kare, ~25 ms.

Kaldırmadan önce **30 bölümün 14'ü** ilerlemeyi tıkıyordu.

**Combo da yalnızca ÜÇÜNCÜ sırada ve en fazla 5** — oyun combo 6/7/8'i
ödüllendirmiyor, dolayısıyla istemesi de yanlış (`LevelCatalog.kt:63-65`).

Perfect Dodge **görevlerde** hâlâ var: günlük `dodge` şablonu (6/14/25) ve
haftalık `perfect_dodges` (15/35/60). Orada ilerleme kilidi değil, isteğe
bağlı ödül.

### Öğrenme eğrisi — bölüm 1–8

| # | Ne öğretir | Hedef | Başl. hız | Rampa | Yoğunluk | Araç/sn | Yıldızlar (sıralı) |
|---|---|---|---|---|---|---|---|
| 1 | şerit değiştirme | 25 s hayatta kal | 60 | 0.40 | 0.30 | 1 / 2.60 s | Bitir · 3 geçiş · 3 coin |
| 2 | trafik | 30 s | 60 | 0.48 | 0.55 | 1 / 1.42 s | Bitir · 6 coin · 14 geçiş |
| 3 | boost | 35 s | 60 | 0.56 | 0.70 | 1 / 1.11 s | 10 geçiş · 200 m boost · 1400 puan |
| 4 | yoğun trafik | 40 s | 60 | 0.65 | 0.85 | 1 / 0.92 s | Bitir · 1800 puan · 48 geçiş |
| 5 | ilk "normal" bölüm | 45 s | 60 | 0.74 | 1.00 | 1 / 0.78 s | Bitir · 30 geçiş · 2500 puan |
| 6 | combo (**nefes bölümü**) | 45 s | 60 | 0.82 | 0.85 | 1 / 0.92 s | 10 coin · 30 geçiş · 3× combo |
| 7 | baskı altında combo | 50 s | 60 | 0.91 | 1.00 | 1 / 0.78 s | 2900 puan · 45 geçiş · 4× combo |
| 8 | mesafe + süre | 1200 m / 60 s | 60 | 1.00 | 1.00 | 1 / 0.78 s | Bitir · 36 s altı · 22 coin |

Kaynak: `LevelCatalog.kt:72-197`.

Bölüm 6 ve 7 daha önce **iki beceri hedefini birden** taşıyordu
(`PerfectDodges` + `ComboAtLeast`). Ölçüm temkinli oyunun oralardan yalnızca
1 yıldız aldığını gösterdi — `MIN_STARS_TO_PASS = 2` kuralında bölüm
tıkanıyordu (`LevelCatalog.kt:154-160`, `:173-176`).

### Üç tasarım kuralı

1. **İlk hedef asla beceri hedefi olamaz.** Yıldızlar sıralı kazanılır ve
   sonraki bölüm 2 yıldızla açılır. İlk sıraya konan
   `PerfectDodges`/`ComboAtLeast`/`BoostDistance`, o bölümü ilerleme kilidi
   yapar.
2. **Testere dişi eğri.** 6. bölüm bilerek 5'ten kolay: yeni mekanik önce
   güvenli ortamda öğrenilir.
3. **Tek eksende zorlaşma.** Hız, yoğunluk ve hedef sıkılığı aynı bölümde
   birlikte artmaz.

### Bölüm 9–30 (yapı)

Üç tema dönüşümlü ilerliyor (`LevelCatalog.kt:198-417`):

| Tip | Bölümler | Hedef biçimi |
|---|---|---|
| Hayatta kal | 9, 11, 13, 14, 16, 17, 19, 20, 22, 23, 25, 26, 28, 29 | `SurviveTime` 60 → 90 s |
| Mesafe + süre | 8, 10, 12, 15, 18, 21, 24, 27, 30 | `ReachDistance` 1200 → 5000 m |
| Fren disiplini | 13, 19, 25 | `BrakeTapsAtMost` 0–1 |

Hepsinde başlangıç hızı **60**, hepsinde rampa **1.00** (varsayılan).
`ComboAtLeast` yalnızca 3. sırada ve **en fazla 5**: bölüm 11 ve 12'de 4,
14/17/20/23/25/26/29'da 5.

⚠ `FinishUnderSeconds` hedefleri (18, 21, 24, 27, 30) **yükseltmesiz araçla
ulaşılamaz görünüyor** (`DIFFICULTY_REVIEW.md`, ölçüm: bölüm 21'de −%3,
24'te −%7). Bunlar üçüncü sırada olduğu için **ilerlemeyi tıkamıyorlar**,
yalnızca üçüncü yıldızı yükseltme arkasına koyuyorlar. Bilinçli mi, karar
bekliyor.

---

## 8. Yükseltmeler

Dört dal, her biri 8 seviye. Maliyet **250 × mevcut seviye**
(`UpgradeCatalog.kt:44-45`): 1→2: 250, 2→3: 500 … 7→8: 1750.
**Bir dalı tam açmak 7.000 coin, dördü birden 28.000 coin.**

Etki eğrisi **dışbükey** (`UpgradeCatalog.kt:64-69`):

```
curve(seviye) = ((seviye − 1) / 7) ^ 1.5
```

| Seviye | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 |
|---|---|---|---|---|---|---|---|---|
| `curve` | 0.000 | 0.054 | 0.153 | 0.281 | 0.432 | 0.604 | 0.794 | 1.000 |
| Toplam ödenen coin | 0 | 250 | 750 | 1.500 | 2.500 | 3.750 | 5.250 | 7.000 |

Üs neden 1.5 (`UpgradeCatalog.kt:60-62`): 2.0 denendi ve SPEED/BRAKE'te 1→2
adımı garajda **aynı sayıyı** gösteriyordu ("para verdim, hiçbir şey
değişmedi"); 1.0 ise şikâyetin kaynağı olan doğrusal eğriydi.

### Dal başına gerçek değerler (referans araç, çarpan 1.00)

| Seviye | SPEED (garajda) | ACCELERATION | BRAKE | BOOST |
|---|---|---|---|---|
| 1 | 180 km/h | 167 ms | −28.4 km/h | 2.63 s |
| 2 | 182 | 161 | −29.4 | 2.69 |
| 3 | 186 | 151 | −31.3 | 2.79 |
| 4 | 190 | 140 | −33.7 | 2.93 |
| 5 | 196 | 129 | −36.6 | 3.13 |
| 6 | 202 | 119 | −39.9 | 3.38 |
| 7 | 209 | 109 | −43.5 | 3.72 |
| 8 | 216 | 100 | −47.4 | 4.17 |

Formüller: `scoreSpeedCap` `UpgradeCatalog.kt:72`, `accelRate` `:76`,
`brakePenalty` `:92`, `boostDrain` `:100`; gösterim `displayValue` `:153-183`.
SPEED sütunu garajın yazdığı değerdir (2.63 tabanlı) — kariyerdeki gerçek
karşılığı için §2'nin sonundaki uyarıya bakın.

**İlk 250 coin ne alıyor:** +2 km/h son hız, ya da −6 ms tepki, ya da
+1.0 km/h fren, ya da +0.06 s boost.

`ACCELERATION` ayrıca yavaşlamadan toparlanmayı da iyileştirir
(`decelRate`, `:84`). BRAKE gizlice **çift** yükseltmeydi (hem daha çok
yavaşlatıyor hem daha hızlı toparlıyordu); 2026-08-14'te ayrıldı.
BOOST seviyesi hızı da bir miktar artırır (`boostSpeedBonus`, `:104`).

**Fren çarpanı `decelRate`'e uygulanmaz** (bilinçli karar): `decelRate`
yalnızca frenin değil her aşağı yönlü yakınsamanın oranı; boost bırakıldıktan
sonraki sönümleme de oradan geçiyor. Ölçeklenseydi "freni iyi" araç boost'un
artığını da daha çabuk kaybederdi.

SPEED aynı zamanda oyunu **zorlaştırır** (daha hızlı trafik), karşılığında
daha yüksek skor verir — bu bir denge değiş tokuşudur, hata değil.

---

## 9. Araçlar — **11 gövde** (2026-08-17)

Araçlar 2026-08-15'e kadar tamamen kozmetikti. Sahibi: *"Bu arabaların
özellikleri görünmüyor garajda, yani neden süper araba alsın?"*

Seçilen model: **karakterli üstünlük.** Pahalı araç net olarak daha iyi ama
her aracın bir zayıf yönü var.

| Araç | Fiyat | Seviye | Sınıf | Son hız | İvme | Fren | Boost | Fabrika boyası |
|---|---|---|---|---|---|---|---|---|
| **Beety** | **0** | 1 | BINEK | 1.00 | 1.00 | 1.00 | 1.00 | Kron Kırmızısı |
| Şehir | 350 | 1 | BINEK | 1.00 | 1.00 | **1.04** | 1.00 | Kron Kırmızısı |
| Yarış Sedan | 900 | 2 | BINEK | 1.04 | 1.08 | 0.98 | 1.00 | Kron Kırmızısı |
| Kuş SLX | 1.500 | 2 | BINEK | 0.97 | 1.00 | 1.06 | **1.12** | Petrol (1.400) |
| Dağ Keçisi | 1.500 | 2 | BINEK | 1.00 | 0.94 | **1.12** | 1.06 | Buzul Beyazı (500) |
| Kas Arabası | 1.800 | 4 | BINEK | 1.08 | 0.96 | 0.96 | 1.00 | Kron Kırmızısı |
| Boğa 67 | 2.400 | 5 | BINEK | 1.10 | 0.92 | 0.90 | 1.04 | Gece Siyahı (1.750) |
| **Motosiklet** | **2.800** | 5 | **MOTOSIKLET** | 1.06 | 1.12 | 0.88 | 1.02 | Kron Kırmızısı |
| Süper Araba | 3.200 | 6 | BINEK | 1.12 | 1.10 | 0.94 | 1.00 | Kron Kırmızısı |
| **Tır** | **3.600** | 6 | **AGIR** | **1.14** | 0.86 | 0.88 | 0.94 | Kron Kırmızısı |
| **Formula** | **5.000** | 8 | BINEK | **1.18** | **1.15** | **0.85** | 1.06 | Kron Kırmızısı |

Kaynak: `CarCatalog.kt` — Beety `:1135-1147`, Şehir `:547-574`, Yarış Sedan
`:696-714`, Kuş SLX `:627-648`, Dağ Keçisi `:824-841`, Kas Arabası
`:754-766`, Boğa 67 `:926-958`, Motosiklet `:1224-1236`, Süper Araba
`:1032-1044`, Tır `:1278-1301`, Formula `:1343-1364`. Liste sırası `:1415-1418`.

Toplam gövde bedeli (Beety hariç): **23.050 coin**.

### Beety referans araçtır — çarpanları 1.00 OLMAK ZORUNDA

2026-08-16'da başlangıç aracı Şehir'den Beety'ye geçti
(`CarCatalog.kt:483-488`, `:1129-1133`). Sonuçları:

- "En pahalı ama en yavaş" çelişkisi ortadan kalktı (Beety önce 4.000'di).
- Süper Araba yeniden 3.200 ile zirve; "yeni gövdeler fiyat merdivenini
  uzatmaz" kuralı için açılan istisna kapandı — sonra Formula 5.000 ile onu
  yeniden açtı.
- Mağaza başlığı "Retro Araba Oyunu" vaadi artık **oyunun ilk saniyesinde**
  teslim ediliyor.

Yükseltme eğrileri, bölüm hedefleri ve skor eğrisi bu aracın davranışına göre
hesaplandı; `CarCatalogTest` çarpanların 1.00 kalmasını kilitliyor. Beety'nin
eski "çevik ama yavaş" kimliği bu yüzden bırakıldı — kimliği artık görselden
geliyor.

### Şehir 350 coine düştü

Beety başlangıç aracı olunca Şehir **en ucuz ücretli gövde** oldu ve
"kusursuz araç" rolünü (hiçbir eksende 1.00'ın altına inmeyen tek araç)
Yarış Sedan'dan devraldı — kural "EN UCUZ ücretli araç" olduğu için
(`CarCatalog.kt:555-570`). Yarış Sedan karşılığında bir bedel kazandı:
fren 1.00 → 0.98.

Fiyat bilerek düşük: ürün incelemesi "ilk anlamlı satın alma çok uzakta"
demişti. **350, ölçülen bölüm başına gelirle üçüncü bölüm civarında
ulaşılabilir bir ilk hedef veriyor** (§11).

### Üç sınıf dışı araç (2026-08-16)

- **Motosiklet**: üstünlüğü çarpanlarda değil **kutusunda** (çarpışma alanı
  kâğıt üzerinde −%31). Bu yüzden çarpanları ölçülü; hem dar kutu hem her
  eksende üstünlük verilseydi diğerleri çöpe dönerdi. Bedeli fren (0.88).
- **Tır**: uzunluğu bir özellik değil **bedel** (kutu 142 dp). 2026-08-17'de
  sahibi *"tırın boostu çok yüksek, tır boostlanamaz ki"* dedi; boost
  1.18 → 0.94 indi ve güç **son hıza** taşındı (0.94 → 1.14). 1.14 rastgele
  değil: 1.12'de Süper Araba tırı **dört eksende birden** geçiyor ve
  dominasyon testi kırılıyordu.
- **Formula**: BINEK sınıfında **kaldı**. Sahibinin notu "F1 daha geniş" idi
  ama referans çizim bunu söylemiyor: oranı 0.473, Şehir'inki 0.518 — F1
  sanatı otomobilden **daha dar**. Ölçüme uyuldu. 2026-08-17'de boost
  0.90 → 1.06 (sahibi: *"boostu da + olmalı"*); freni −%15'te bırakıldı,
  bedeli o.

**Dominasyon kuralı:** on bir aracın hiçbiri diğerini dört eksende birden
geçmiyor. Tarama commit `2608363`'te yeniden çalıştırıldı.

### Motora nereye bağlandığı

| Eksen | Ne çarpılıyor | Nerede |
|---|---|---|
| Son hız | `scoreSpeedCap(...)` sonucu | `UpgradeCatalog.kt:119` |
| İvme | `accelRate(...)` | `:122` |
| Fren | `brakePenalty(...)` | `:125` |
| Boost süresi | `boostDrain(...)` **bölünür** | `:136` |

**Çarpanlar yükseltmelerin ÜSTÜNE uygulanır**, yerine geçmez. Seviye 8 bir
Şehir hâlâ seviye 1 bir Formula'dan çok daha hızlıdır.

### Garaj gösterimi

Her araç için dört çubuk (HIZ / İVME / FREN / BOOST) + tek satırlık karakter
cümlesi. **Çubuklar mutlak değil karşılaştırmalı** (`CarCatalog.statFraction`,
`:1665`): çarpanlar 0.85–1.18 arasında geziyor, doğrudan çubuğa çevirmek
on bir aracı da neredeyse aynı gösterirdi. Eksenin en iyisi dolu, en kötüsü
0.22 uzunlukta. Kilitli araçta da görünür — oyuncu almadan önce ne aldığını
görür.

---

## 10. Boyalar — **11 boya** (2026-08-17)

| Boya | Fiyat | Gerekli araç seviyesi |
|---|---|---|
| Kron Kırmızısı | 0 | 1 |
| Grafit | 250 | 1 |
| Zümrüt | 400 | 1 |
| Buzul Beyazı | 500 | 1 |
| Neon Magenta | 600 | 2 |
| **Gün Sarısı** | **700** | **2** |
| Kraliyet Mavisi | 850 | 2 |
| Mor | 1.100 | 3 |
| Petrol | 1.400 | 3 |
| Gece Siyahı | 1.750 | 4 |
| Haki | 2.200 | 5 |

Kaynak: `CarCatalog.kt:1509-1636`. Toplam boya bedeli: **9.750 coin**.

**Gün Sarısı 2026-08-16'da eklendi** (`CarCatalog.kt:1568-1585`): palette sarı
yoktu, oysa sarı oyunun vurgu rengi (boost şimşeği, `KronColors.AccentBright`).
Beety'nin referans çizimi sarı; sahibi kararı gereği Beety **kırmızı başlar**
ve oyuncu sarıyı satın alarak referanstaki hâline getirir. Fabrika boyası
**değil** — bilerek: bedava verilseydi 700'lük bir boya bedava gövdeye hediye
olurdu.

### Fabrika boyası

Gövdeye sahip olmak `defaultColorId` boyasını da açar
(`CarCatalog.effectiveOwnedColors`, `:1698-1699`). Üç gövdenin fabrika boyası
ücretli:

| Gövde | Fabrika boyası | Hediye edilen değer |
|---|---|---|
| Kuş SLX | Petrol | 1.400 |
| Dağ Keçisi | Buzul Beyazı | 500 |
| Boğa 67 | Gece Siyahı | 1.750 |
| | | **toplam 3.650** |

Yani on bir gövdenin hepsi alınırsa boyalar için gerçekte ödenen
**9.750 − 3.650 = 6.100 coin**.

Fabrika boyası gövdeye kilitli değil, **hediye**: Kuş SLX'i alan oyuncu
Petrol'ü başka gövdelere de sürebilir.

### Sanat yönü kuralı

**Trafik renkleri oyuncuya verilmez.** Engel araçları sarı (`FFD60A`),
camgöbeği (`00C2FF`), beyaz (`FFFFFF`) ve turuncu (`FF7B00`)
(`GameEngine.kt:1002`). Oyuncu paleti bilerek bu tonların dışında seçildi;
tehdit ile oyuncu 60 Hz'de karışmasın diye. Bir test bunu doğruluyor.
Oyuncunun beyazı bu yüzden **kırık ve soğuk** (`EDF1F5`), tam beyaz değil.

---

## 11. Ekonomi

### Coin kaynakları

| Kaynak | Miktar | Kod |
|---|---|---|
| Başlangıç | ⚠ **100.000** (yayında 100) | `PlayerProgress.kt:125`, `:128` |
| Toplanan coin | 1 / adet | `GameConfig.kt:432` |
| Skor bonusu | her **70** puan = 1 coin | `:449` |
| Yıldız | 25 coin, **yalnızca YENİ yıldız için** | `:451`, `GameEngine.kt:881-888` |
| Günlük görev | **80 / 140 / 280** = günde en fazla **500** | `DailyChallengeGenerator.kt:92` |
| Haftalık kademe | 40 / 60 / 100 × 5 görev = **1.000** | `WeeklyMissionGenerator.kt:25-53` |
| Haftalık sandık | **750** + 1 İkinci Şans | `:13`, `:16` |
| Ödüllü reklam | **150** × günde en fazla **5** = 750 | `GameConfig.kt:564`, `:572` |

Koşu ödülü formülü (`GameEngine.kt:886-894`):

```
coin = toplananCoin + skor/70 + yeniYıldız×25
if (ÇiftÖdül) coin ×= 2
if (süre < 10 s) coin = 0
```

**Yıldız coini yalnızca yeni kazanılan yıldız için ödenir.** Eskiden her
oynayışta tekrar ödeniyordu ve bölüm 1'i tekrar tekrar oynamak bölüm 30'u
oynamaktan daha kârlıydı — oyunun en yüksek coin/saniye oranı bir farm
döngüsüydü (2026-08-14).

**10 saniyeden kısa koşu hiç ödemez** (`GameConfig.kt:585`): "başla, hemen
bırak, tekrar başla" da bir farm yoluydu.

### `SCORE_PER_BONUS_COIN`: 120 → 70 (2026-08-16)

Oynanış geliri pasif geliri (günlük görev + ödüllü reklam) yakalayamıyordu.
Bu çarpan **oynayarak** kazanmanın tek ölçeklenen kalemi — toplanan coin ve
yıldız coini tavanlı, skor değil (`GameConfig.kt:434-449`).

Ölçülen etki: ilk geçişte bölüm başına ortalama **100 → 118 coin** (+%18).
Etki tekrar oynamada daha büyük, çünkü tekrar koşusunun geliri neredeyse
tamamen bu çarpandan geliyor — örn. bölüm 5 tekrarı **51 → 74 coin**.

⚠ **Bu ölçüm 2026-08-16'da alındı; ertesi gün üç değişiklik onu eskitti**
(bölüm kilidi 2 yıldıza indi, başlangıç hızı 60 oldu, `speedRampScale`
eklendi). Hız değişikliklerinin türetilmiş etkisi: bölüm başına ilk geçiş
geliri **118 → ~113**. Kilit değişikliğinin bir koşunun ödemesine etkisi
**yoktur** — yıldız coini her yeni yıldız için ödenir, kilitten bağımsız
olarak. Ayrıntı, düzeltme ve yeni ilerleme eğrisi:
`docs/ECONOMY_STATUS_20260817.md`.

### ⚠ Başlangıç coini geçici olarak 100.000

`PlayerProgress.STARTING_COINS = 100_000` (`PlayerProgress.kt:125`).

Sahibi bütün araçları (en pahalısı 5.000) cihazda denemek istedi:
*"aab yaparken değiştiririz"* (commit `2608363`). İki koruma kondu:

- `STARTING_COINS_RELEASE = 100` — geri dönülecek değer (`:128`)
- `docs/PLAY_RELEASE_CHECKLIST.md` **S-7 yayın engeli** maddesi

Test kırmızı yakmıyor, bilerek: kalıcı kırmızı bir test her build'de hata
gösterip "hepsi yeşil" sinyalini yok ederdi. Test değerin **sessizce
kaymasını** yakalıyor — ya yayın değeri ya belgelenmiş test değeri; arada bir
şey yazan yakalanır (`PlayerProgressCarTest`).

**Bu değer 100'e dönmeden AAB üretilmez.**

### Kalıcı harcama tavanı

| Kalem | Coin |
|---|---|
| 10 ücretli gövde | 23.050 |
| 11 boya (3.650'si fabrika boyası olarak bedava) | 6.100 |
| 4 yükseltme dalı × 7.000 | 28.000 |
| **Toplam** | **57.150** |

Boyalar hiç gövde alınmadan tek tek satın alınırsa tavan **60.800**'e çıkar.
Booster'lar tüketilir, tavana girmez.

### Booster'lar

| Booster | Fiyat | Etki | Kod |
|---|---|---|---|
| Turbo Başlangıç | 150 | İlk 3 saniye boost bedava | `PlayerProgress.kt:11`, `GameConfig.kt:463` |
| Skor Yükseltici | 250 | Koşu boyunca skor +%25 | `:20`, `GameConfig.kt:482` |
| **Çift Ödül** | **300** | Koşunun coin ödülü ×2 | `:17`, `GameConfig.kt:483` |
| İkinci Şans | 400 | İlk çarpışmayı yok sayar | `:14` |

⚠ **Çift Ödül matematiksel olarak hiç kâra geçmiyor.** ×2 yalnızca koşu
ödülüne uygulanır (`GameEngine.kt:889-891`) — günlük görev kademelerine,
haftalık ödüllere ve ödüllü reklam coinine uygulanmaz (o ödemeler
`GameStateRepository` üzerinden ayrı geçiyor). Başa baş noktası **koşunun
300 coinden fazla ödemesi**; ölçülen bölüm başına gelir 94–149 coin, en iyi
gerçekçi koşu ~248 coin (net **−52**). Ayrıntı ve öneriler:
`docs/ECONOMY_STATUS_20260817.md`.

### XP ve araç seviyesi

| Değer | Miktar | Kod |
|---|---|---|
| Skordan XP | skor / 10 | `GameConfig.kt:452` |
| Yıldızdan XP | 20 / yıldız | `:453` |
| Araç seviyesi | `1 + xp / 500` | `:456` |

Araç seviyesi coinden **bağımsız ikinci bir kapıdır**: Formula 5.000 coin
**ve** araç seviyesi 8 (= 3.500 XP) istiyor; Haki boyası seviye 5 istiyor.
Yıldız XP'si tekrar oynamada da ödendiği için (`GameEngine.kt:895-896`,
`stars` — `newStars` değil) seviye ilerlemesi coin ilerlemesinden hızlıdır.

---

## 12. Görevler

### Günlük görev

Tek bir koşuda artan **üç kademe**; ödüller her şablonda aynı: **80 / 140 /
280** = günde en fazla **500** (`DailyChallengeGenerator.kt:92`).

Tarihçe (`:72-91`): önce 200/400/800 (=1.400), aynı gün 900'e çekildi,
**2026-08-16'da 500'e indirildi**. 900 neden fazlaydı: bir kariyer bölümünü
ilk kez geçmek ortalama 100 coin ödüyordu, yani 900'lük günlük **dokuz
bölümlük ilerlemeye** bedeldi — oynamak yerine günde bir kez girmek daha
kârlıydı. 500'de bu oran beşe iner.

Göç riski yok: kayıt kademe **sayısını** saklıyor, coin miktarını değil.

Yedi şablon, tarihten türetilir (sunucu yok, `:173-174`):

| Şablon | Kademeler |
|---|---|
| `dodge` | 6 / 14 / 25 Perfect Dodge |
| `distance` | 1.200 / 2.600 / 4.200 m |
| `combo` | 3× / 5× / 7× combo |
| `score` | 2.000 / 4.500 / 8.000 puan |
| `survive` | 45 / 90 / 140 s |
| `coins` | 10 / 22 / 38 coin |
| `pass` | 25 / 55 / 90 araç |

⚠ `combo` şablonunun 3. kademesi **7× combo** istiyor ama
`COMBO_MULTIPLIERS` 5'te doyuyor — oyun 7× comboyu ödüllendirmiyor. Aynı
gerekçeyle kariyer hedeflerinden 5 üstü combo kaldırılmıştı (§7); günlük
görevde kalmış.

### Haftalık görevler

5 sabit görev × 3 kademe; kademeler **kümülatiftir**
(`WeeklyMissionGenerator.kt:20-56`).

| Görev | Kademeler | Ödül |
|---|---|---|
| Bölüm tamamla | 3 / 8 / 15 | 40 / 60 / 100 |
| Toplam mesafe | 5.000 / 15.000 / 25.000 m | 40 / 60 / 100 |
| Araç geç | 80 / 200 / 400 | 40 / 60 / 100 |
| Perfect Dodge | 15 / 35 / 60 | 40 / 60 / 100 |
| 5× combo | 3 / 7 / 12 | 40 / 60 / 100 |

Hepsi tamamlanınca sandık: **750 coin + 1 İkinci Şans** (`:13`, `:16`).
Haftalık toplam tavan: **1.750 coin + 1 booster**.

⚠ `perfect_dodges` görevi haftada 60 dodge istiyor. Ölçüm temkinli oyunun
30 bölümün hiçbirinde tek bir dodge bile yapmadığını gösteriyor (§7). Bu
görev pratikte yalnızca risk alan oyuncuya açık.

---

## 13. Reklam frekansı

| Kural | Değer | Kod |
|---|---|---|
| Her koşu sonunda geçiş reklamı | **kapalı** | `GameConfig.kt:503` |
| Kariyer: kaç koşuda bir | **3** | `:514` |
| Sonsuz mod: kaç koşuda bir | 3 | `:517` |
| Sayacı artırmak için en kısa koşu | 10 s | `:529` |
| **Reklamsız ilk bölümler** | **1–3** | `:551` |
| Reklamla devam | koşu başına 1 kez | `:588` |
| Ödüllü coin | 150, günde en fazla 5 | `:564`, `:572` |
| "Ödülü ikiye katla" tavanı | 150 (= bir reklam) | `:578` |

**Belge eskiden "her 2 bölümde" diyordu; kod 3.** Değişimin sebebi
(`GameConfig.kt:505-513`): 2026-08-16'da sayaç kaçağı kapatıldı — eskiden
sayaç yalnızca bölüm **tamamlandıysa** artıyordu, yani çarpıp çıkan oyuncu
sınırsız reklamsız oynuyordu. Kaçak kapanınca aynı "2" eşiği gerçekte çok
daha sık reklam demek olacaktı; eşik 3'e alındı.

**İlk 3 bölüm tamamen reklamsız** (`:531-551`). 2026-08-17'de 4 → 3 indi:
sahibi cihazda oynarken muafiyetin pratikte **beş** bölüme çıktığını fark
etti — reklam kararı **biten** bölüme bakar, gidilene değil. "4" yazıp 5
bölüm muafiyet vermek sayının kendisini yanıltıcı kılıyordu.

Karar iki parçaya ayrılmıştır (`AdFrequency.kt:7-13`):

- `countsTowardInterstitial` (koşu **bittiğinde**): kariyerde başarı şartı
  **yok**, çarpan koşu da sayılır; günlük görev sayacı artırmaz; sonsuz mod
  kendi sayacını kullanır (`:27-31`).
- `shouldShow` (koşudan **çıkarken**): muafiyet **yalnızca kariyere** ait
  (`:44-74`).

Günlük görev muafiyete girmez ama sayaç doluysa çıkışta reklam gösterebilir.
Önce CAREER ile DAILY aynı daldaydı ve günlük görev sessizce hiç reklam
göstermez olmuştu: kimliği −1 ve `−1 <= 4` olduğu için her gün "erken bölüm"
sayılıyordu (`AdFrequency.kt:50-59`). **Ders: eşik karşılaştırmasını sıralı
olmayan bir kimlikle yapmak.** İlk test bunu `levelId = null` ile kontrol
ediyordu, oysa üretim yolu −1 geçiriyor — test yeşil kalıp gerçeği gizledi.

`INTERSTITIAL_EVERY_N_RETRIES = 2` **kullanılmıyor** (`GameConfig.kt:561`):
sonuç ekranındaki "TEKRAR" butonu 2026-08-14'te kaldırıldı.

Yayın planı: 30 gün veri toplanacak; mağaza puanı 4.2 üzerinde kalırsa
frekans kademeli artırılacak. Geri almak için tek satır yeter.

**Oyun ekranında banner yok. Ödül yalnızca SDK'nın gerçek "kazanıldı" geri
çağrısında verilir.**

---

## 14. Sonsuz mod zorluk eğrisi

| Değer | Miktar | Kod |
|---|---|---|
| Hız çarpanı adımı | ×1.10 / 30 s | `GameConfig.kt:396`, `:398` |
| Hız çarpanı tavanı | ×1.60 | `:397` |
| Trafik yoğunluğu adımı | ×1.06 / 30 s | `:401` |
| Trafik yoğunluğu tavanı | ×1.50 | `:402` |
| "Rekoruna N saniye kaldı" eşiği | 5 s | `:405` |

Sonsuz modda bölüm yok, dolayısıyla taban hız `BASE_SPEED` = 2.63 (80 km/h)
ve rampa 1.00 (`GameEngine.kt:106-107`, `:434`). **Kariyerden hızlı başlar.**

---

## 15. Yol deseni — göz yorgunluğu ayarı

Oyuncu geri bildirimi (2026-08-15): *"çizgiler çok sık, oynarken göz çok
yoruluyor."* Sebep dünyanın hızı değil **desen frekansı**: prototipte kerb
blokları 24 px, şerit çizgileri 20 dolu / 20 boştu.

| Değer | Prototip | Şimdi | Kod |
|---|---|---|---|
| Kerb blok boyu | 24 | **50** | `GameConfig.kt:105` |
| Şerit çizgisi dolu | 20 | **42** | `:108` |
| Şerit çizgisi boş | 20 | **54** | `:109` |

Kerb 50 nereden geliyor: en yüksek hızda (~1650 px/s) kırmızı/beyaz geçiş
oranı `1650 / (2×50) = 16.5 Hz`. Üst sınır olarak şerit çizgilerinin aynı
koşuldaki oranı alındı (`1650 / 96 = 17.2 Hz`) — o desen zaten 1.00 çarpanda
çalışıyor ve sahibi görünümünü onaylamış durumda.

Periyot (100) şerit periyodundan (96) bilerek **farklı**: eşitlenirse yol
düzleminin tamamı aynı anda "tik" yapar ve senkron titreşim tek tek
titreşimden daha yorucu okunur.

---

## 16. Bilinen tutarsızlıklar — karar bekliyor

| # | Ne | Nerede | Durum |
|---|---|---|---|
| 1 | **Başlangıç coini 100.000** | `PlayerProgress.kt:125` | ⚠ Yayın engeli, `PLAY_RELEASE_CHECKLIST` S-7 |
| 2 | Garaj hızı (180–216 km/h) kariyerde geçerli değil (161–196) | `UpgradeCatalog.kt:153-155` vs. `LevelCatalog.kt` | Açık, §2 |
| 3 | Çift Ödül booster'ı hiç kâra geçmiyor | `PlayerProgress.kt:17` | Açık, `ECONOMY_STATUS_20260817.md` |
| 4 | İlk yükseltme 250 coine +2 km/h | `UpgradeCatalog.kt:44`, `:64` | Açık, `ECONOMY_STATUS_20260817.md` |
| 5 | Günlük `combo` şablonu 7× istiyor, oyun 5'te doyuyor | `DailyChallengeGenerator.kt:132-135` vs. `GameConfig.kt:352` | Açık |
| 6 | Haftalık 60 dodge, temkinli oyun 0 dodge yapıyor | `WeeklyMissionGenerator.kt:46` | Açık |
| 7 | `FinishUnderSeconds` (18, 21, 24, 27, 30) yükseltmesiz ulaşılamaz | `LevelCatalog.kt` | Açık, ilerlemeyi tıkamıyor |
| 8 | Motosikletin gerçek avantajı ölçülmedi | `VEHICLE_CLASSES.md:183-196` | Açık |
