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
