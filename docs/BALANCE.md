# Denge — sayılar ve nereden geldikleri

Tüm ayarlanabilir değer tek dosyada: `game/GameConfig.kt`.
Bölüm hedefleri `game/LevelCatalog.kt`, yükseltme maliyet/etkileri
`game/UpgradeCatalog.kt`, görevler `data/*Generator.kt`.
Başka hiçbir yerde denge sabiti yoktur.

## Prototipten gelen çekirdek

| Değer | Sabit | Kaynak (HTML) |
|---|---|---|
| Taban hız | 2.63 | `currentSpeed()` |
| Skordan gelen hız | `min(3.2, skor/600)` | `currentSpeed()` |
| Boost hız bonusu | +1.8 | `keys.Shift` dalı |
| Fren cezası | −0.9 | `keys.Space` dalı |
| Hız → piksel | ×250 px/s | `speed * 250 * dt` |
| Skor kazancı | hız × 11 /s | `state.score += speed*11*dt` |
| Boost tüketimi | 38 /s | `state.boost -= 38*dt` |
| Boost dolumu | 15 /s (frendeyken 10) | `keys.Space ? 10 : 15` |
| Boost yeniden tutuşma | ≥8 enerji **ve** parmağın kalkması | *(prototipte yok, aşağıya bak)* |
| Engel doğma | 0.78 s | `spawnAcc > 0.78` |
| Engel hızı | ~~× 1.00–1.14~~ → kendi hızı, bkz. "Trafik hızı" | `speed*250*dt*speedMul` |
| Coin doğma | 1.05 s | `coinAcc > 1.05` |
| Geçilen araç | +8 puan | `state.score += 8` |
| Coin | +35 puan, +12 boost | coin toplama bloğu |
| Çarpışma | −80 puan, koşu biter | çarpışma bloğu |
| Araç ölçüsü | 42 × 90 | `player.width/height` |
| Yol genişliği | `min(290, W*0.56)`, 3 şerit | `metrics()` |
| Hız göstergesi | `((hız−2)/5.7)*180+60` km/h | `drawSpeedometer()` |

Taban hızda gösterge ~80 km/h okur. Metre dönüşümü (29.6 px = 1 m) bu
göstergeyle **tutarlı** olacak şekilde seçildi: 80 km/h = 22.2 m/s.

## Skor eğrisi (hedefler buradan hesaplandı)

Skor kendi kendini hızlandırır (hız skorla artar, skor hızla artar):

- 0–43 s: üstel artış, `skor(t) ≈ 1577·(e^0.01833t − 1)`
- t ≈ 43 s: skor 1920'ye ulaşır, hız tavanı (5.83) dolar
- sonrası: sabit ~64 puan/s

Buna geçilen araçlar (~10/s) ve toplanan coinler eklenir. Kabaca:

| Koşu süresi | Beklenen skor (iyi oyuncu) |
|---|---|
| 45 s | ~3.000 |
| 60 s | ~4.300 |
| 75 s | ~5.600 |
| 90 s | ~6.900 |

`LevelCatalog` içindeki puan hedefleri bunun **%75–85'i** olacak şekilde
seçildi. Bu yüzden ürün taslağındaki örnek rakamlar (ör. "60 saniyede 2.000
puan") yukarı çekildi — gerçek eğride 2.000 puan 30 saniyede doluyor ve hedef
hiç hedef olmuyordu.

## Trafik hızı (2026-08-14)

Sahibi: *"Yoldaki arabalar hareket etmiyor, park etmiş gibiler."* Haklıydı —
engel `speed × K × dt × speedMul` (1.00–1.14) ile akıyordu, asfalt ise tam
`speed × K × dt`. Engeller **asfalta göre duruyor ya da geri geri gidiyordu**.

Yeni model:

```
ekrandaAşağıHız = (oyuncuHızı − aracHızı) × WORLD_PX_PER_SPEED_UNIT
aracHızı        = koşununTabanHızı × r,   r ∈ [0.45, 0.58]   (doğumda sabit)
```

Zemin yine `oyuncuHızı` ile kaydığı için araç asfalta göre `aracHızı` kadar
**ileri** gider.

| Değer | Eski | Yeni | Neden |
|---|---|---|---|
| Yaklaşma hızı (taban hız 2.63) | 2.81 birim (527 px/s) | 1.28 birim (240 px/s) | araç ilerlediği için fark azalır |
| Boost'un yaklaşmaya etkisi | 2.81 → 4.74 (+%69) | 1.28 → 3.08 (+%141) | trafik hızı **taban** hıza bağlı, anlık hıza değil |
| Aynı anda ekrandaki araç | ~1.5 | ~3 | tehdit daha uzaktan görünüyor |
| Saniyede geçilen araç | 1 / 0.78 | 1 / 0.78 | **değişmedi** |
| Araçlar arası tepki süresi | 0.78 s | 0.78 s | **değişmedi** |

Spawn aralığına neden dokunulmadı: doğma zamana bağlı, mesafeye değil.
Yaklaşma hızı pozitif kaldıkça doğan her araç oyuncuya varır, yani
`saniyedeki geçilen araç = 1 / aralık` ve bu değer yaklaşma hızından
**bağımsızdır**. Aralığı büyütmek karşılaşma sıklığını gerçekten düşürürdü.
Değişen tek şey aracın ekranda görünür kalma süresi — bu zorluk değil
**görünürlük** artışıdır.

`WORLD_SPEED_SCALE`, `WORLD_PX_PER_SPEED_UNIT`, `HITBOX_SCALE` ve çarpışma
kutusu **değişmedi**.

Neden `r` aralığı dar (1.29 kat): fark büyüdükçe hızlı araçlar yavaşlara
yetişip aynı y'de kümelenir ve 3 şeridi birden kapatabilir. Bu aralıkta bir
araç tüm yaklaşma boyunca en fazla ~120 px yetişir, iki araç arası mesafe
ise ~190 px.

Kenar durum: oyuncu frene basıp trafiğin altına düşerse yaklaşma hızı
negatife döner ve araçlar yukarı uzaklaşır. Ekranın **üstünden** çıkanlar
artık temizleniyor (`OBSTACLE_DESPAWN_TOP_MARGIN_PX`); `evaluated` bayrağı
korunduğu için geri gelen bir araç ikinci kez puan/dodge vermez. Varsayılan
tabanda en hızlı trafik 1.53 < `MIN_SPEED` 2.0 olduğu için bu durum pratikte
oluşmaz, ama motor tolere eder ve test eder.

## Perfect Dodge

Çarpışma zaten `|dx| < 42` (iki aracın genişliği) olduğunda oluşur.
Dodge eşiği **çarpışma sınırı ile şerit aralığının tam ortasıdır**:

```
eşik = 42 + (şeritAralığı − 42) × 0.5        // GameConfig.perfectDodgeMaxDx
```

| Ekran | Şerit aralığı | Eşik | Geçerli pencere |
|---|---|---|---|
| 320 dp | 59.7 | 50.8 | 42–50.8 |
| 360 dp | 67.2 | 54.6 | 42–54.6 |
| 411 dp | 76.7 | 59.4 | 42–59.4 |
| 600 dp+ (tavan) | 96.7 | 69.3 | 42–69.3 |

Sabit bir piksel değeri **kullanılamaz**: motor dp uzayında çalışır ve şerit
aralığı ekran genişliğine bağlıdır (`min(290, W×0.56)/3`). İlk sürümde eşik
sabit 64'tü; 320 dp'lik bir telefonda bu şerit aralığından büyük kalıyor ve
**yan şeritten dümdüz geçmek bile bedava dodge veriyordu** (combo ve skor
şişerdi). Birim test bu değişmezi her ekran genişliği için doğruluyor.

Böylece yan şeritten temiz geçmek hiçbir cihazda dodge saymaz — sadece şerit
değiştirirken aracın şeritler arasında olduğu, gerçekten riskli anlar sayılır.
Mekaniğin amacı budur: güvenli sürüş değil, risk almak.

Combo çarpanları: 1× / 1.2× / 1.5× / 2× / 3× (5 ve üstü). Zincir 6 saniye
yeni dodge gelmezse kopar.

## Öğrenme eğrisi — bölüm 1–8 (2026-08-14, ikinci tur)

Sahibi ikinci kez "ilk bölümler fazla zor" dedi. İlk turda yalnızca başlangıç
hızı düşürülmüştü; zorluğun asıl ekseni olan **saniyedeki araç sayısı** hiç
değişmemişti — 1. bölüm de 30. bölüm de 0.78 s'de bir araç doğuruyordu.
`LevelDef.trafficDensity` (varsayılan 1.0) bu eksende de rampa açtı.

| Bölüm | Ne öğretir | Hedef | Hız | Yoğunluk | Araç/sn | Yıldızlar (sıralı) |
|---|---|---|---|---|---|---|
| 1 | şerit değiştirme | 25 s hayatta kal | 60 | 0.30 | 1 / 2.60 s | Bitir · 3 geçiş · 3 coin |
| 2 | trafik | 30 s | 65 | 0.55 | 1 / 1.42 s | Bitir · 6 coin · 14 geçiş |
| 3 | boost | 35 s | 70 | 0.70 | 1 / 1.11 s | 10 geçiş · 200 m boost · 1400 puan |
| 4 | perfect dodge | 40 s | 75 | 0.85 | 1 / 0.92 s | Bitir · 1800 puan · 3 dodge |
| 5 | ilk "normal" bölüm | 45 s | 80 | 1.00 | 1 / 0.78 s | Bitir · 30 geçiş · 2500 puan |
| 6 | combo (**nefes bölümü**) | 45 s | 75 | 0.85 | 1 / 0.92 s | 10 coin · 4 dodge · 3x combo |
| 7 | baskı altında combo | 50 s | 80 | 1.00 | 1 / 0.78 s | 2900 puan · 6 dodge · 4x combo |
| 8 | mesafe + süre | 1200 m / 60 s | 80 | 1.00 | 1 / 0.78 s | Bitir · 36 s altı · 4 dodge |

Eski hâliyle karşılaştırma:

| | Eski | Yeni |
|---|---|---|
| Bölüm 1 süresi | 30 s | 25 s |
| Bölüm 1 yoğunluk | 1.00 (38 araç) | 0.30 (~9 araç) |
| Bölüm 2 ilk yıldız | `PassVehicles(5)` | `CompleteRun` |
| Bölüm 2 ikinci yıldız | `ScoreAtLeast(2200)` — eğrinin **%92'si**, pratikte ulaşılamaz | `CoinsAtLeast(6)` |
| Bölüm 3 ilk yıldız | `PerfectDodges(3)` — beceri hedefi **kilit noktasında** | `PassVehicles(10)` |
| Bölüm 4 ilk yıldız | `BoostDistance(500)` — aynı hata | `CompleteRun` |
| Bölüm 8 | 1500 m / 75 s | 1200 m / 60 s |

Üç tasarım kuralı:

1. **İlk hedef asla beceri hedefi olamaz.** Yıldızlar sıralı kazanılır
   (`LevelEvaluator`) ve bir sonraki bölüm `stars > 0` ile açılır
   (`GameStateRepository.recordLevelResult`). İlk sıraya konan
   `PerfectDodges`/`ComboAtLeast`/`BoostDistance`, o bölümü **ilerleme
   kilidi** hâline getirir. Eski 3. ve 4. bölüm tam olarak böyleydi.
2. **Testere dişi eğri.** 6. bölüm bilerek 5'ten kolay: yeni mekanik
   (combo) önce güvenli ortamda öğrenilir.
3. **Tek eksende zorlaşma.** Hız, yoğunluk ve hedef sıkılığı aynı bölümde
   birlikte artmaz.

Doğrulama testle yapılıyor: `LevelCurveTest` bölümleri gerçekten oynar.
İki otopilot var — temkinli (hiç risk almaz) ve riskli (yan araca yanaşıp
son anda çekilir). Testler şunu garanti eder: ilk 8 bölüm temkinli oyunla
çarpmadan bitirilebilir, her bölüm en az 1 yıldız verir (ilerleme
tıkanmaz), 1. bölüm beş farklı tohumda da üç yıldız verir, ve dodge/combo
hedeflerine riskli oyunla ulaşılabilir.

## Yükseltmeler

8 seviye, maliyet `250 × mevcut seviye` (1→2: 250 … 7→8: 1750; tam max = 7.000 coin).

| Dal | Seviye başına etki | Seviye 1 → 8 |
|---|---|---|
| SPEED | hız tavanı +0.16 | 181 → 216 km/h |
| ACCELERATION | yaklaşma oranı +0.7/s | 0.17 → 0.09 s tepki |
| BRAKE | fren cezası +0.12 (+ yavaşlama oranı) | −28 → −55 km/h |
| BOOST | tüketim −2.6/s (taban 12) | 2.6 → 5.0 s boost süresi |

Artışlar bilinçli olarak küçük: oyuncu gelişmeyi hisseder ama oyun bir anda
kolaylaşmaz. SPEED aynı zamanda oyunu **zorlaştırır** (daha hızlı trafik),
karşılığında daha yüksek skor verir — bu bir denge değiş tokuşudur, hata değil.

## Ekonomi

| Kaynak | Miktar |
|---|---|
| Başlangıç | 100 coin |
| Toplanan coin | 1 coin |
| Skor bonusu | her 120 puan = 1 coin |
| Yıldız | 25 coin |
| Günlük görev | 400–500 coin |
| Haftalık kademe | 40 / 60 / 100 coin (5 görev × 3 kademe = 900) |
| Haftalık sandık | 750 coin + 1 booster |

Booster fiyatları: Turbo Start 150, Score Booster 250, Double Reward 300,
Second Chance 400.

Kabaca 60 saniyelik iyi bir koşu 60–90 coin verir; tek bir yükseltme dalını
sonuna kadar çıkarmak (7.000 coin) uzun vadeli bir hedeftir.

## Sonsuz mod zorluk eğrisi

Her 30 saniyede hız ×1.10 (tavan ×1.60) ve trafik yoğunluğu ×1.06 (tavan ×1.50).
Rekora 5 saniye veya daha az kalındıysa sonuç ekranında "rekoruna N saniye
kaldı" mesajı çıkar.

## Reklam frekansı

- Gecis reklamı: her 2 tamamlanan bölümde bir, 3 sonsuz koşuda bir.
- Reklamla devam: koşu başına 1 kez.

Hepsi `GameConfig` içinde tek satır — yayın sonrası veriye bakıp değiştirilebilir.

## Araç özellikleri (karar: 2026-08-15, sahibi)

Araçlar bugüne kadar **tamamen kozmetikti**. Sahibinin tespiti: *"Bu
arabaların özellikleri görünmüyor garajda, yani neden süper araba alsın?"*
3200 coin'lik bir aracın hiçbir etkisinin olmaması ve bunun garajda da
yazmaması, oyuncuyu yanlış beklentiye sokuyordu.

Seçilen model: **karakterli üstünlük.** Pahalı araç net olarak daha iyi
(alma sebebi var) ama her aracın bir zayıf yönü var (ucuz araç çöpe
dönmüyor, seçim oyun tarzına göre anlam kazanıyor).

| Araç | Fiyat | Son hız | İvme | Fren | Boost süresi | Karakter |
|---|---|---|---|---|---|---|
| Şehir | 0 | — | — | — | — | Denge, referans |
| Yarış Sedan | 900 | +4% | +8% | — | — | Çevik |
| Kuş SLX | 1500 | −3% | — | +6% | +12% | Sakin, uzun soluklu |
| Dağ Keçisi | 1500 | — | −6% | +12% | +6% | Ağır ama tutuşu iyi |
| Kas Arabası | 1800 | +8% | −4% | −4% | — | Düz yolda canavar |
| **Boğa 67** | **2400** | **+10%** | **−8%** | **−10%** | **+4%** | **Ağır kas, uzun soluklu** |
| Süper Araba | 3200 | +12% | +10% | −6% | — | Hızlı ama affetmez |

Uygulanan hâli `game/CarCatalog.kt` içindeki dört alan (`topSpeedMul`,
`accelMul`, `brakeMul`, `boostMul`) — hepsinin varsayılanı 1.0. Yukarıdaki
altı satır tablodan **birebir** alındı.

**Boğa 67 neden bu profille (tabloda yoktu, 2026-08-15'te eklendi):**
Kas arabası ailesinin ağır kanadı, yani "yüksek son hız + zayıf fren"
kuralının en uç noktası.

- Son hız **+10%**: Kas Arabası (+8%) ile Süper Araba (+12%) tam arasında —
  fiyat merdivenindeki yeriyle (1800 → 2400 → 3200) aynı sıra.
- İvme **−8%**: ailenin en ağırı; katalogdaki en düşük ivme.
- Fren **−10%**: katalogdaki en kötü fren. Bandın (0.80–1.25) rahat içinde.
- Boost süresi **+4%**: büyük depo. Bu, Süper Araba'ya karşı **tek**
  üstünlüğü; olmasaydı 2400'lük basamak "ucuz Süper Araba" olur ve 3200'ü
  almanın anlamı kalmazdı.

**Yarış Sedan'ın zayıf yönü yok — bilinçli istisna.** Tablodaki fren ve
boost sütunları boş, yani araç dört eksende de Şehir'den kötü değil. 900
coinlik ilk satın alma tereddütsüz "iyi" hissettirmeli. Üstünlüğü küçük
kalıyor ve 1500+ araçların hepsi onu en az bir eksende belirgin geçiyor.
`CarCatalogTest` bu istisnayı **tek araçla sınırlıyor**: zayıf yönü olmayan
ikinci bir araç eklenirse test kırılır. *Sahibinin onayına açık: istenirse
boost süresine −4% verilip bu istisna tamamen kaldırılabilir.*

**Motorda nereye bağlandı:**

| Eksen | Ne çarpılıyor | Nerede |
|---|---|---|
| Son hız | `UpgradeCatalog.scoreSpeedCap(...)` sonucu | `GameEngine.updateSpeed` |
| İvme | `accelRate(...)` (hedefe yaklaşma oranı) | `GameEngine.updateSpeed` |
| Fren | `brakePenalty(...)` (hedef hızdan düşülen miktar) | `GameEngine.updateSpeed` |
| Boost süresi | `boostDrain(...)` **bölünür** (uzun süre = az tüketim) | `GameEngine.updateBoostEnergy` |

**Fren çarpanı `decelRate`'e uygulanmadı** (bilinçli karar). `decelRate`
yalnızca frenin değil **her aşağı yönlü yakınsamanın** oranı — boost
bırakıldıktan sonraki sönümleme de oradan geçiyor. Çarpanla ölçeklenseydi
"freni iyi" bir araç boost'un artığını da daha çabuk kaybederdi, yani bir
güç gizli bir ceza getirirdi. Ayrıca garajdaki FREN çubuğu `brakePenalty`
üzerinden okunuyor; ikinci bir gizli etki eklemek aşağıdaki 3. sınırı
(gösterilmeyen özellik yok sayılır) bozardı. Aynı hata yükseltme tarafında
bir kez yapılmıştı — BRAKE gizlice çift yükseltmeydi, 2026-08-14'te ayrıldı.

**Sınırlar (bunlar tasarımın kendisi):**

1. **Çarpanlar yükseltmelerin ÜSTÜNE uygulanır**, onların yerine geçmez.
   Fark ~%10 bandında kalır: ana ilerleme garaj yükseltmeleri olmalı,
   yoksa 8 seviyelik dört dal anlamsızlaşır.
2. **Çarpışma kutusu HİÇBİR araçta değişmez.** Fizik/görsel ayrımı korunur
   (bkz. PROVENANCE #6). Kutu değişimi ancak ayrı bir "araç sınıfı"
   kavramıyla gelir (motosiklet/tır planı) ve o ayrı bir iştir.
3. **Garajda GÖSTERİLİR**: her araç için dört çubuk (HIZ / İVME / FREN /
   BOOST) + tek satırlık karakter cümlesi. Gösterilmeyen özellik yok
   sayılır — sorunun yarısı zaten görünmemesiydi.

**Çubuklar mutlak değil karşılaştırmalı.** Çarpanlar 0.90–1.12 arasında
geziyor; bunu doğrudan çubuğa çevirmek ("%112 dolu") yedi aracı da neredeyse
aynı gösterirdi. Her eksen katalog içinde normalize ediliyor
(`CarCatalog.statFraction`): o eksenin en iyisi dolu, en kötüsü 0.22 uzunlukta.
Taban sıfır değil — sıfır uzunluktaki çubuk "veri yok" gibi okunur, oysa en
kötü araç da yolda gidiyor. Çubuğun yanında referansa göre yüzde de yazıyor
(+12% / −6% / —). Etiketler `UpgradeCatalog.title` üzerinden geliyor, yani
garajın yükseltme bölümüyle **aynı** dört kelime.

Kilitli araçta da görünür: kilitli bir gövdeye dokunmak satın almaz, önizlemeye
alır ve panel o gövdenin değerlerini gösterir. Oyuncu almadan önce ne aldığını
görür.
