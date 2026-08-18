# Ekonomi durumu — 2026-08-19

Bu belge `docs/ECONOMY_STATUS_20260817.md`'nin **yerine geçer**, onu silmez.
O belgenin dayandığı sayıların yarısı 18–19 Ağustos'ta değişti; aşağıda
hangisinin neden geçersiz olduğu ve yerine ne konduğu yazılı.

**Tek cümlelik özet:** kalıcı harcama tavanı 57.150 → **45.950** coine indi
(−%20), ölçülen oyun geliri ise büyük ölçüde aynı kaldı; yani oyun daha bol
değil, **daha kısa** oldu. Asıl mesele bolluk değil, harcama kaleminin
yükseltmelerden araçlara kayması ve araç fiyatlarının yeni güç dağılımıyla
uyuşmaması.

---

## Dürüstlük çerçevesi

**Oyuncu verisi yok — oyun yayınlanmadı.** Bu belgede "retention %X artar"
tipi tek bir sayı yok, olamaz da. Mekanizma üzerinden konuşuluyor.

| Etiket | Anlamı |
|---|---|
| **ÖLÇÜM** | Test motoru gerçekten oynadı; komut, çıktı ve koşullar aşağıda |
| **TÜRETME** | Koddan aritmetikle çıkarıldı; formül gösteriliyor |
| **TAHMİN** | Oyuncu davranışı varsayımı içeriyor; varsayım açıkça yazılı |

---

## 0. Yönetici özeti

1. **Bölüm başına gerçek coin geliri: ilk geçişte ortalama 170, tekrar
   oynamada 109.** 30 bölümün tamamı, 3 tohum, ölçüldü. Kariyerin ilk
   geçişinin TAMAMI **5.099 coin** veriyor — kalıcı tavanın **%11'i** (§2).
2. **Kalıcı harcama tavanı 57.150 → 45.950.** Düşüşün tamamı yükseltme
   kalemi: 28.000 → 16.800 (§3).
3. **Ekonomi "çok bol" olmadı; %20 kısaldı.** Her şeyi almak ~15 / ~19 /
   ~29 gün (reklam izleyen / izlemeyen / sadece oynayan) — eskiden 21 / 28 /
   42 idi. **TAHMİN**, varsayımları §4'te (§4).
4. **Seviye atlama bedeli ekonomiyi bozmuyor — ama muhtemelen hiç
   kullanılmayacak.** Ölçüm, coin'in seviyeden ÇOK ÖNCE bittiğini gösteriyor:
   oyuncu Formula'nın seviye şartını (8) kariyerin 11. bölümünde geçiyor,
   fiyatını (5.000) ise ancak günler sonra. Seviye kapısı hiçbir araçta
   bağlayıcı değil (§5).
5. **Araç fiyatları yeni güce göre üç yerde tutarsız.** Çoğu araç 67–77
   coin/km-h bandında; **Tır 200, Dağ Keçisi 167, Kuş SLX 139**. Tır ayrıca
   3.2 kat büyük çarpışma kutusu taşıyor — 3.600 coine, 1.500 coinlik Kuş
   SLX'ten muhtemelen daha kötü bir araç (§6).
6. ⚠ **İki yayın engeli duruyor:** `STARTING_COINS = 100_000` ve
   `TRAINING_MODE_SIDE_LANES_ONLY = true`. İkincisi bu belgedeki ölçümü de
   etkiliyor — §1'deki uyarıya bakın.

---

## 1. Ölçüm — ne yapıldı, neye güvenilebilir

### Yöntem

Geçici bir test (`ZzzEkonomiDokumuTest`, `zzz ekonomi dokumu`) yazıldı,
çalıştırıldı ve **silindi**. Otopilot `LevelCurveTest`'in `Style.SAFE`
profilinden birebir kopyalandı: temkinli, hiç risk almayan, kasten vasat bir
oyuncu. Çalıştırma:

```
cd source
./gradlew :app:testDebugUnitTest --offline --tests "*zzz*" -i
BUILD SUCCESSFUL in 29s
```

Ölçüm koşulları:

- 30 bölümün tamamı, **3 tohum** (1, 7, 42), ortalama alındı
- İki senaryo: **ilk geçiş** (`previousStars = 0`, yıldız coini ödenir) ve
  **tekrar** (`previousStars = 3`, yıldız coini ödenmez)
- Yükseltme seviyesi 1, varsayılan gövde (Beety), booster yok
- Ekran 360×800, `dt = 0.016`

**Ölçümün alındığı kod durumu:** commit `6cc66e1` ("Antrenman modu (gecici),
seviye atlama bedeli"). Ölçüm sırasında çalışma ağacında paralel yürüyen
başka bir işin `SPEEDOMETER_MAX_KMH` (240 → 280) ve
`ENDLESS_SPEED_MAX_MULTIPLIER` (1.60 → 1.20) değişiklikleri vardı; ikisi de
yalnızca sonsuz modu ve göstergeyi ilgilendiriyor, kariyer ölçümüne
girmiyor (kariyerdeki tepe hızların hiçbiri 240'a yaklaşmıyor).

### ⚠ Ölçümün geçerlilik sınırı — antrenman modu AÇIK

`GameConfig.TRAINING_MODE_SIDE_LANES_ONLY = true` (kod satırı 40, commit
`6cc66e1`). Bu açıkken trafik **yalnızca en sol ve en sağ şeritte** doğuyor,
orta şerit hep boş. `const val` olduğu için testten kapatılamıyor.

Bunun ölçüme iki etkisi var:

1. **Kariyer bölümlerinde etki sınırlı ama tek yönlü.** 90 koşunun 90'ı
   kazasız tamamlandı (`tamamlanan = 3/3`, her bölüm). Antrenman modu
   kapalıyken bir kısmı çarpacaktı; çarpan koşu daha kısa sürer ve daha az
   skor/coin verir. Yani **aşağıdaki gelir rakamları bir ÜST SINIRDIR**,
   gerçek gelir bunun altında.
2. **Sonsuz mod ölçümü tamamen geçersiz.** Otopilot 5 tohumun 5'inde de
   **479 saniye boyunca hiç çarpmadı** (kare tavanına dayandı, `kaza=false`).
   Orta şerit boş olduğu için oyuncu orada sonsuza kadar yaşıyor. Sonsuz mod
   geliri bu yüzden bu belgede **ölçülmüş sayılmıyor**.

Antrenman modu kapatıldıktan sonra bu ölçüm **tekrarlanmalı**. O zamana kadar
§2'deki sayılar "temkinli oyuncunun alabileceği en iyi sonuç" olarak okunmalı.

---

## 2. Soru 1 — bölüm başına gerçek coin geliri (ÖLÇÜM)

### Formül (TÜRETME)

`GameEngine.finish()`:

```
coin = toplananCoin × 1  +  skor / 70  +  YENİ yıldız × 25
```

Yıldız coini yalnızca **daha önce kazanılmamış** yıldız için ödenir, bu yüzden
ilk geçiş ile tekrar arasında büyük fark var.

### İlk geçiş — 30 bölüm, 3 tohum, temkinli

| Bölüm | Coin (ort) | min–max | Süre (sn) | Yıldız | XP |
|---|---|---|---|---|---|
| 1 | 111.7 | 110–113 | 25.0 | 3.00 | 208 |
| 2 | 121.3 | 117–125 | 30.0 | 3.00 | 248 |
| 3 | 99.3 | 98–101 | 17.3 | 3.00 | 164 |
| 4 | 135.7 | 131–138 | 40.0 | 3.00 | 322 |
| 5 | 140.7 | 138–142 | 45.0 | 3.00 | 356 |
| 6 | 123.0 | 119–126 | 45.0 | 2.00 | 355 |
| 7 | 135.0 | 132–139 | 50.0 | 2.00 | 409 |
| 8 | **92.3** | 88–97 | 26.3 | 2.00 | 227 |
| 9 | 132.0 | 130–133 | 34.3 | 3.00 | 313 |
| 10 | 144.3 | 143–145 | 42.0 | 3.00 | 369 |
| 11 | 171.3 | 167–180 | 70.0 | 2.00 | 575 |
| 12 | 159.0 | 156–162 | 49.7 | 3.00 | 431 |
| 13 | 196.3 | 192–205 | 70.0 | 3.00 | 595 |
| 14 | 179.0 | 174–187 | 75.0 | 2.00 | 611 |
| 15 | 174.7 | 172–180 | 57.7 | 3.00 | 499 |
| 16 | 182.7 | 174–198 | 72.0 | 2.33 | 596 |
| 17 | 187.0 | 183–194 | 80.0 | 2.00 | 648 |
| 18 | 189.3 | 186–194 | 65.0 | 3.00 | 561 |
| 19 | 212.0 | 208–219 | 80.0 | 3.00 | 668 |
| 20 | 195.0 | 191–201 | 85.0 | 2.00 | 685 |
| 21 | 175.7 | 172–182 | 72.7 | 2.00 | 597 |
| 22 | 199.3 | 191–214 | 81.7 | 2.33 | 670 |
| 23 | 195.0 | 191–201 | 85.0 | 2.00 | 685 |
| 24 | 189.7 | 186–196 | 80.7 | 2.00 | 659 |
| 25 | 203.3 | 198–211 | 90.0 | 2.00 | 724 |
| 26 | 203.3 | 198–211 | 90.0 | 2.00 | 724 |
| 27 | 206.7 | 202–213 | 91.7 | 2.00 | 737 |
| 28 | 214.0 | 198–226 | 85.7 | 2.67 | 709 |
| 29 | 203.3 | 198–211 | 90.0 | 2.00 | 724 |
| 30 | **226.7** | 222–232 | 101.7 | 2.00 | 824 |

**Toplam: 5.099 coin, 15.896 XP, 1.929 saniye (32.1 dakika sürüş).**

- Bölüm başına ortalama: **170 coin**
- En düşük: bölüm 8 (92 coin) — 26 saniyelik kısa bölüm
- En yüksek: bölüm 30 (227 coin)
- Dakika başına: **159 coin/dk**

### Tekrar oynama (yıldız coini yok)

| Bölüm | Coin | Bölüm | Coin | Bölüm | Coin |
|---|---|---|---|---|---|
| 1 | 36.7 | 11 | 121.3 | 21 | 125.7 |
| 2 | 46.3 | 12 | 84.0 | 22 | 141.0 |
| 3 | 24.3 | 13 | 121.3 | 23 | 145.0 |
| 4 | 60.7 | 14 | 129.0 | 24 | 139.7 |
| 5 | 65.7 | 15 | 99.7 | 25 | 153.3 |
| 6 | 73.0 | 16 | 124.3 | 26 | 153.3 |
| 7 | 85.0 | 17 | 137.0 | 27 | 156.7 |
| 8 | 42.3 | 18 | 114.3 | 28 | 147.3 |
| 9 | 57.0 | 19 | 137.0 | 29 | 153.3 |
| 10 | 69.3 | 20 | 145.0 | 30 | **176.7** |

**Toplam: 3.265 coin.** Bölüm başına ortalama **109**; dakika başına
**84–104 coin/dk** (bölüm 1'de 88, bölüm 11 sonrasında istikrarlı ~103).

Dikkate değer: **coin/dakika bölüm numarasıyla neredeyse hiç değişmiyor.**
Bölüm 3 tekrarı 84 coin/dk, bölüm 30 tekrarı 104 coin/dk. Yani "hangi bölümü
farm etmeliyim" sorusunun anlamlı bir cevabı yok — bu iyi bir şey, tek bir
bölümün sömürülmesini engelliyor.

### Gelirin bileşimi (ÖLÇÜM + TÜRETME)

30 bölümlük ilk geçişte:

| Kalem | Coin | Pay |
|---|---|---|
| Skor / 70 | ~2.047 | %40 |
| Toplanan coin | ~1.219 | %24 |
| Yıldız (73.3 yıldız × 25) | ~1.833 | %36 |

Tekrar oynamada yıldız kalemi sıfırlanıyor ve gelir **%36 düşüyor** —
5.099 → 3.265. Bu, "aynı bölümü sonsuz tekrar et" farmını kıran mekanizmanın
ölçülmüş etkisi ve doğru çalışıyor.

### Riskli oyun tavanı (ÖLÇÜM)

`Style.RISKY` otopilotu (yan araca yanaşıp son anda çekilen, insanüstü tepkili
denetleyici) ilk geçişte **5.726 coin** aldı — temkinli oyundan **+%12**.
Yani risk almak ekonomik olarak ödüllendiriliyor ama az; oyun beceriyi coinle
değil yıldızla ödüllendiriyor.

---

## 3. Soru 2 — kalıcı harcama tavanı (TÜRETME)

### Katalog

**Araçlar** (`CarCatalog.kt`, Beety 0 coin):

| Araç | Fiyat | Seviye |
|---|---|---|
| Şehir | 350 | 1 |
| Yarış Sedan | 900 | 2 |
| Kuş SLX | 1.500 | 2 |
| Dağ Keçisi | 1.500 | 2 |
| Kas Arabası | 1.800 | 4 |
| Boğa 67 | 2.400 | 5 |
| Motosiklet | 2.800 | 5 |
| Süper Araba | 3.200 | 6 |
| Tır | 3.600 | 6 |
| Formula | 5.000 | 8 |
| **Toplam** | **23.050** | |

**Boyalar:** brüt 9.750. Üç gövde fabrika boyası hediye ediyor
(`defaultColorId`): Kuş SLX → Petrol (1.400), Dağ Keçisi → Buzul Beyazı (500),
Boğa 67 → Gece Siyahı (1.750) = **3.650 hediye**. Net **6.100**.

**Yükseltmeler:** `cost(level) = 150 × level`, seviye 1→8.
Bir dal = 150+300+…+1050 = **4.200**. Dört dal = **16.800**.
*(Eski: 250 × level → dal 7.000, dört dal 28.000.)*

### Tavan

| Kalem | 2026-08-17 | **2026-08-19** | Fark |
|---|---|---|---|
| Araçlar | 23.050 | 23.050 | — |
| Boyalar (net) | 6.100 | 6.100 | — |
| Yükseltmeler | 28.000 | **16.800** | **−11.200** |
| **Kalıcı tavan** | **57.150** | **45.950** | **−%19.6** |

Buna **seviye atlama bedelleri dahil değil** — onlar oyuncunun seçimine bağlı
ve zorunlu değil. En kötü durum (oyuncu her şeyi arac seviyesi 1'deyken
alırsa): araçlar için 15.500 + boyalar için 7.000 = **+22.500**, mutlak tavan
**68.450**. §5'te bunun neden pratikte gerçekleşmeyeceği gösteriliyor.

### Harcama kaleminin şekli değişti

| | 2026-08-17 | 2026-08-19 |
|---|---|---|
| Araçların payı | %40 | **%50** |
| Yükseltmelerin payı | %49 | **%37** |
| Boyaların payı | %11 | %13 |

Bu, yalnızca bir sayı değişimi değil. **Yükseltmeler her zaman satın
alınabilir** (seviye şartı yok, her araçta işe yarar, kademeli). **Araçlar
tek seferlik ve büyük.** Oyunun ana para deposu, sürekli akan bir kanaldan
sıçramalı bir kanala kaydı. Sonucu §4'te.

---

## 4. Soru 3 — yükseltme gideri %40 düştü; ekonomi çok mu bol?

### Kısa cevap: hayır, bol olmadı — %20 kısaldı, ama şekli bozuldu.

Gelir tarafı **değişmedi** (ölçüm §2), gider tarafı %19.6 düştü. Yani her
şeyi almak yaklaşık %20 daha az sürüyor. "Bol" demek için gelirin artması
gerekirdi; artmadı.

Asıl risk bolluk değil, **§3'teki şekil değişimi**: yükseltme dalı artık
oyunun para deposunun sadece %37'si ve tamamı 16.800 coine bitiyor. Bir
oyuncu 16.800 biriktirdiği anda **dört dalı da maksa çıkarır ve o eksende
yapacak hiçbir şeyi kalmaz**. Eskiden 28.000'lik bu depo, araç merdiveninin
tamamıyla (23.050) benzer büyüklükteydi ve iki ilerleme hattı paralel
akıyordu. Artık yükseltmeler önce bitiyor.

### Kaç günde her şeyi alır (TAHMİN)

**Varsayımlar** (`ECONOMY_STATUS_20260817 §5`'ten devralındı, ölçümle
güncellendi — varsayımı değiştirin, sonuç değişir):

| Varsayım | Değer | Kaynak |
|---|---|---|
| Günlük oyun süresi | ~15 dk sürüş | devralınan, doğrulanmadı |
| Kariyer ilk geçişi | 5.099 coin / 32 dk | **ÖLÇÜM** |
| Kariyer sonrası oyun geliri | 102 coin/dk | **ÖLÇÜM** (tekrar oynama) |
| Günlük görev | 500 coin/gün, her gün yapılıyor | `DailyChallengeGenerator.TIER_REWARDS` = 80+140+280 |
| Ödüllü reklam | 5 × 150 = 750 coin/gün | `REWARDED_COIN_DAILY_LIMIT`, `REWARDED_COIN_AMOUNT` |
| Haftalık görev | 1.750 ÷ 7 = 250 coin/gün | 5 görev × 200 + 750 sandık |

⚠ **Haftalık gelir muhtemelen bu kadar değil.** `big_combos` görevi haftada
12 kez 5x combo istiyor; ölçümde temkinli oyun 30 bölümün hiçbirinde 5x
comboya ulaşmıyor (yıldız ortalamaları 2.00 olan bölümlerin üçüncü hedefi
zaten combo). Bu görev tamamlanmazsa **haftalık sandık (750) da açılmaz** —
haftalık gelir 1.750 yerine 800'e, günlüğü 250 yerine **114**'e düşer.
Aşağıdaki tablo iyimser (250) rakamı kullanıyor.

**Faz 1 — kariyer ilk geçişi** (~32 dk sürüş ≈ 2.1 gün):

| Oyuncu | Faz 1 sonu birikim |
|---|---|
| Reklam izleyen | 5.099 + 2.1 × 1.500 = ~8.250 |
| Reklam izlemeyen | 5.099 + 2.1 × 750 = ~6.675 |
| Sadece oynayan | 5.099 |

**Faz 2 — kariyer sonrası günlük gelir:**

| Oyuncu | Oyun | Günlük | Reklam | Haftalık | Toplam/gün |
|---|---|---|---|---|---|
| Reklam izleyen | 1.530 | 500 | 750 | 250 | **3.030** |
| Reklam izlemeyen | 1.530 | 500 | — | 250 | **2.280** |
| Sadece oynayan | 1.530 | — | — | — | **1.530** |

**Her şeyi (45.950) almaya kadar geçen süre:**

| Oyuncu | 2026-08-17 | **2026-08-19** |
|---|---|---|
| Reklam izleyen | ~21 gün | **~15 gün** |
| Reklam izlemeyen | ~28 gün | **~19 gün** |
| Sadece oynayan | ~42 gün | **~29 gün** |

Formula'ya (5.000, en pahalı tek kalem) ulaşma: reklam izleyen ~4. gün,
sadece oynayan ~6. gün — kariyerin ilk geçişi bittikten hemen sonra.

**Değerlendirme.** 15–29 gün bir koşu oyunu için kısa değil. "Çok bol" demek
için bir gerekçe yok. Ama iki uyarı:

1. **Yükseltme deposu artık ~6 günde (reklam izleyen) bitiyor.** 16.800 coin,
   günde 3.030 gelirle 5.5 gün. Bu, oyuncunun ilk haftasında "garajda
   yapacak bir şey kalmadı, sadece araç biriktiriyorum" noktasına gelmesi
   demek. **TAHMİN** — varsayım: oyuncu yükseltmeleri araçlardan önce alır
   (çünkü ucuz ve her araçta işe yarıyor).
2. **Pasif gelir oyun gelirini yine geçiyor.** Ödüllü reklam 150 coin veriyor
   ve bir reklam ~30 saniye → **300 coin/dk**; oynamak **102 coin/dk**.
   Oran **2.9×**. Eski belge bu oranı 1.8× ölçmüştü — kötüleşti, çünkü
   `SCORE_SPEED_CAP_BASE` ve `WORLD_SPEED_SCALE` düşünce koşular uzadı ama
   koşu başına ödül aynı kaldı. **TÜRETME**, varsayım: reklam ~30 sn.
   *Not: pasif kaynak günde 5 ile sınırlı, yani toplam etkisi sınırlı; sorun
   büyüklük değil, oyuncuya "oynamak yerine reklam izle" sinyali vermesi.*

---

## 5. Soru 4 — seviye atlama bedeli ekonomiyi bozuyor mu?

### Kısa cevap: hayır. Muhtemelen hiç kullanılmayacak.

`LEVEL_SKIP_COIN_PER_LEVEL = 500`; bedel `(gerekenSeviye − mevcutSeviye) × 500`.

### Bedelin fiyatı doğru mu? (TÜRETME + ÖLÇÜM)

- Bir araç seviyesi = **500 XP** (`XP_PER_CAR_LEVEL`)
- Ölçülen XP geliri: 30 bölüm ilk geçişte **15.896 XP**, bölüm başına
  ortalama **530 XP** → **oynanan her kariyer bölümü kabaca bir araç
  seviyesi veriyor**
- O bölüm aynı zamanda **170 coin** (ilk geçiş) veya **109 coin** (tekrar)
  ödüyor

Yani bir seviyeyi beklemek ≈ bir bölüm oynamak ≈ **+1 seviye ve +109…170
coin**. Atlamak ise **−500 coin**. Atlamanın gerçek maliyeti
500 + 109…170 = **609–670 coin, yaklaşık bir dakikalık zaman kazancı için**.

**Bedel doğru fiyatlanmış.** Sabırsız oyuncuya bir kapı açıyor, kestirme
sunmuyor. Sahibinin kendi gerekçesi ("atlamak beklemekten ucuz olmasın")
aritmetikle tutuyor.

### Ama kapı zaten kilitli değil — coin seviyeden önce bitiyor (ÖLÇÜM)

Ölçülen XP birikimiyle oyuncunun her aracın seviye şartını ne zaman
karşıladığını, coin şartını ne zaman karşıladığıyla yan yana koyalım.
Kümülatif ilk geçiş verisi:

| Araç | Fiyat | Sv | Seviye şartı karşılanıyor | O anda birikmiş coin | Coin yetiyor mu |
|---|---|---|---|---|---|
| Yarış Sedan | 900 | 2 | kariyer bölümü 3 | 332 | **hayır** |
| Kuş SLX / Dağ Keçisi | 1.500 | 2 | kariyer bölümü 3 | 332 | **hayır** |
| Kas Arabası | 1.800 | 4 | kariyer bölümü 6 | 732 | **hayır** |
| Boğa 67 | 2.400 | 5 | kariyer bölümü 7 | 867 | **hayır** |
| Motosiklet | 2.800 | 5 | kariyer bölümü 7 | 867 | **hayır** |
| Süper Araba | 3.200 | 6 | kariyer bölümü 9 | 1.091 | **hayır** |
| Tır | 3.600 | 6 | kariyer bölümü 9 | 1.091 | **hayır** |
| Formula | 5.000 | 8 | kariyer bölümü **11** | 1.407 | **hayır** |

*(XP kümülatifi: bölüm 3 sonunda 621 XP → seviye 2; bölüm 6 sonunda 1.653 →
seviye 4; bölüm 7 sonunda 2.063 → seviye 5; bölüm 9 sonunda 2.603 → seviye 6;
bölüm 11 sonunda 3.547 → seviye 8. Kariyer sonunda 15.896 XP → **seviye 32**.)*

**Sonuç: on aracın onunda da seviye şartı, coin şartından önce karşılanıyor.**
Seviye kapısı hiçbir araçta bağlayıcı değil. Oyuncu Formula'nın seviye
şartını kariyerin üçte birinde geçiyor, parasını ise günler sonra buluyor.

Bunun iki anlamı var:

1. **Atlama bedeli ekonomiyi bozamaz** — çünkü tetiklenmiyor. Bedeli ödemek
   isteyen oyuncunun elinde zaten fiyatın kendisi yok.
2. **Seviye şartı sistemi bugün işlevsiz.** `requiredCarLevel` alanları,
   `CarUnlockState.LEVEL_LOCKED` durumu ve garajdaki seviye rozeti, normal
   ilerleyen bir oyuncunun hiç görmeyeceği bir yolu tarif ediyor. Sahibinin
   *"hem coin hem seviye istiyoruz"* isteği bugünkü XP eğrisiyle karşılanmıyor
   — seviye pratikte bedava.

⚠ Bu sonucun bir uyarısı var: XP kariyer dışından da geliyor. Sonsuz mod
koşusu `skor/10` XP ödüyor ve ölçümde (antrenman modu açık, 479 sn) tek bir
koşu **4.345 XP = 8 araç seviyesi** verdi. Antrenman modu kapalıyken bu
sayı çok daha düşük olacak, ama yönü değiştirmez: XP her yerden akıyor.

### Ne yapılabilir (öneri, karar sahibinin)

Seviye şartının gerçekten bir kapı olması isteniyorsa `XP_PER_CAR_LEVEL`
500'den yukarı çekilmeli. Ölçüme göre kariyer ilk geçişi 15.896 XP veriyor;
Formula'nın seviye 8 şartının kariyerin **sonuna** denk gelmesi için
XP/seviye ≈ **2.200** olmalı (7 seviye × 2.200 = 15.400). Bu tek satırlık bir
değişiklik ama garajdaki seviye çubuğunun hızını da 4 kat yavaşlatır — yani
bir denge kararı, teknik bir düzeltme değil.

Alternatif: seviye şartı tamamen kaldırılıp fiyat tek kapı bırakılabilir.
Ölçüm bugün zaten böyle davrandığını söylüyor; kod bunu yansıtmıyor.

---

## 6. Soru 5 — araç fiyatları yeni GÜCE göre doğru mu?

### Tepe hız merdiveni (TÜRETME)

`scoreSpeedCap(1, car) = 1.90 × topSpeedMul`; gösterge
`speedToKmh(60 km/h tabanı + bu)`; 1 hız birimi = 180 ÷ 5.7 = **31.58 km/h**.
Yükseltmesiz (seviye 1) tepe hızlar:

| Araç | Fiyat | topSpeedMul | Tepe (sv1) | Beety'ye fark | **coin/km-h** |
|---|---|---|---|---|---|
| Beety | 0 | 1.00 | 120 | — | — |
| Şehir | 350 | 1.08 | 124 | +4.8 | **73** |
| Yarış Sedan | 900 | 1.22 | 133 | +13.2 | **68** |
| Kuş SLX | 1.500 | 1.18 | 130 | +10.8 | **139** |
| Dağ Keçisi | 1.500 | 1.15 | 128 | +9.0 | **167** |
| Kas Arabası | 1.800 | 1.42 | 145 | +25.2 | **71** |
| Boğa 67 | 2.400 | 1.55 | 153 | +33.0 | **73** |
| Motosiklet | 2.800 | 1.62 | 157 | +37.2 | **75** |
| Süper Araba | 3.200 | 1.80 | 168 | +48.0 | **67** |
| Tır | 3.600 | 1.30 | 138 | +18.0 | **200** |
| Formula | 5.000 | 2.08 | 184 | +64.8 | **77** |
| *SPEED dalı 1→8* | *4.200* | — | *+35.4* | *+35.4* | *119* |

### Bulgu 1 — "hız araçları" tutarlı, doğrulandı

Şehir, Yarış Sedan, Kas Arabası, Boğa 67, Motosiklet, Süper Araba, Formula:
hepsi **67–77 coin/km-h** bandında. Yedi araç %13'lük bir bantta. Bu
tesadüf değil, iyi ayarlanmış bir merdiven. **Formula 5.000 coine +%108
hız veriyor ve 77 coin/km-h ile bandın içinde** — sorunun kendisi değil.

`UpgradeCatalog` yorumundaki hedef de tutmuş: yükseltme dalı 119 coin/km-h,
araçlar 67–77. Yani araçlar hâlâ daha büyük sıçrama veriyor (sahibinin
istediği) ama yükseltme yolu tuzak olmaktan çıkmış.

### Bulgu 2 — üç araç bandın dışında ve gerekçeleri eşit güçlü değil

**Kuş SLX (139)** ve **Dağ Keçisi (167)** hızı değil başka eksenleri
satıyorlar: Kuş boost 1.12 + fren 1.06, Keçi fren 1.12 + boost 1.06. Prensip
olarak makul. İki sorun var:

- **İkisi de 1.500 coin, ikisi de seviye 2, ama Kuş SLX dört eksenin üçünde
  daha iyi** (hız 1.18 > 1.15, ivme 0.96 > 0.94, boost 1.12 > 1.06). Dağ
  Keçisi yalnızca frende (1.12 > 1.06) kazanıyor. Aynı fiyata neredeyse
  domine edilen bir araç.
- **Fren muhtemelen en değersiz eksen.** Katalogda `BrakeTapsAtMost(0)` ve
  `BrakeTapsAtMost(1)` hedefleri var (bölüm 13, 19) — yani oyun frene
  basmamayı ödüllendiriyor. Ölçümde temkinli otopilot 30 bölümün hiçbirinde
  frene basmadı. Dağ Keçisi'nin primi en zayıf eksende duruyor.

**Tır (200) açık bir fiyat hatası.**

| | Kuş SLX | Tır |
|---|---|---|
| Fiyat | 1.500 | **3.600** |
| Tepe hız | 130 | 138 (+8) |
| İvme | 0.96 | **0.86** |
| Fren | 1.06 | **0.88** |
| Boost süresi | 1.12 | 1.20 (+7%) |
| Ölçü sınıfı | BINEK (40×76 = 3.040 br²) | **AGIR (48×202 = 9.696 br²)** |

Tır, 2.100 coin fazlasına **+8 km/h ve +%7 boost süresi** veriyor; karşılığında
ivmesi ve freni belirgin daha kötü ve **çarpışma kutusu 3.2 kat büyük**
(`VehicleClass.AGIR`, boyu 2.66 kat uzun). Bir koşu oyununda çarpışma kutusu
büyümesi doğrudan hayatta kalma kaybıdır; ölçüyü kutu üzerinden yapmadım ama
mekanizma tartışmasız.

**Motosiklet (75) tam tersi yönde dengesiz — ama iyi yönde ucuz.** 2.800 coine
+37 km/h, katalogun ikinci en iyi ivmesi (1.12) **ve çarpışma kutusu
22×59 = 1.298 br², yani BINEK'in %43'ü.** Süper Araba (3.200) ondan sadece
+11 km/h fazla veriyor ve normal kutu taşıyor. Motosiklet muhtemelen oyunun
en güçlü aracı ve on araçlık merdivende yedinci sırada fiyatlanmış.

### Bulgu 3 — çarpışma kutusu fiyata hiç girmemiş

`VehicleClass` üç kutu tanımlıyor ve fark üç kata varıyor, ama fiyat
merdiveni yalnızca dört sürüş çarpanına göre kurulmuş. Kutu, oyundaki en
büyük tek güç ekseni (ölmemek) ve fiyatlamada yok.

### Öneri (karar sahibinin)

Denge değerleri tek yerde olduğu için üçü de tek satırlık değişiklik:

| Öneri | Değişiklik | Gerekçe |
|---|---|---|
| **Ö1** | Tır 3.600 → **2.200** | 200 → 122 coin/km-h; kutu cezası hâlâ fiyata girmiyor ama araç en azından Kuş SLX'in üstünde bir fiyat/güç oranına oturur |
| **Ö2** | Motosiklet 2.800 → **3.400** | Kutu avantajı (%57 küçük) + ivme + hız için Süper Araba seviyesinde fiyat |
| **Ö3** | Dağ Keçisi 1.500 → **1.100**, ya da fren 1.12 → 1.20 | Kuş SLX ile aynı fiyatta domine edilmesin |

Üçü de yapılırsa kalıcı tavan 45.950 → **45.550** olur, yani §4'teki gün
hesabı değişmez. **Bu bir fiyat/güç düzeltmesidir, ekonomi değişikliği
değildir.**

⚠ Ö1–Ö3 uygulanırsa `CarCatalogTest` ve garaj metinlerini kontrol edin;
fiyat sabitleri testlerde beklenen dize olarak geçiyor olabilir.

---

## 7. Eski belgeden geçersiz olanlar

`ECONOMY_STATUS_20260817.md` içindeki şu sayılar artık kullanılmamalı:

| Eski sayı | Nerede | Bugünkü |
|---|---|---|
| Kalıcı tavan **57.150** | §5 sonu | **45.950** |
| Yükseltmeler **28.000** | §5 | **16.800** |
| Her şeyi alma **21 / 28 / 42 gün** | §5 tablosu | **~15 / ~19 / ~29 gün** |
| Pasif ÷ oynanış oranı **1.8×** | §4 | **~2.9×** (varsayım: reklam 30 sn) |
| Garaj hızı **180–216 km/h**, kariyer **161–196** | §0.5, Öneri 2 | Beety 120–155, Formula 184–220 |
| "Ölçüm 8 bölümlük ve tek tohumlu" | §1 | 30 bölüm, 3 tohum, bu belgede |

Geçerliliğini koruyanlar: dürüstlük çerçevesi, yıldız coininin tek seferlik
ödendiği mekanizma, Çift Ödül booster'ının zararda olduğu tespiti
(300 coin fiyat, ×2 yalnızca koşu ödülüne uygulanıyor — ölçülen en iyi tekrar
koşusu 177 coin, yani ×2 ile net **+177 − 300 = −123**; **ÖLÇÜM + TÜRETME**,
bölüm 30 tekrarı), ve `STARTING_COINS = 100_000` yayın engeli.

---

## 8. Emin olmadıklarım

1. **Antrenman modu açıkken ölçtüm.** `TRAINING_MODE_SIDE_LANES_ONLY = true`
   olduğu için 90 koşunun 90'ı kazasız bitti. `const val` olduğundan testten
   kapatılamıyor ve kod değiştirmem yasaktı. §2'deki bütün gelir rakamları
   bu yüzden **üst sınır**; gerçek gelirin ne kadar altında olduğunu
   bilmiyorum. Mod kapatıldığında bu ölçüm tekrarlanmalı — sadece o zaman
   §4'teki gün sayıları savunulabilir olur.

2. **Sonsuz mod gelirini ölçemedim.** Otopilot 479 saniye boyunca hiç
   çarpmadı. Sonsuz mod, kariyer bittikten sonraki ana gelir kanalı olabilir
   ve o kanalın coin/dakikası elimde yok. §4'ün "kariyer sonrası 102 coin/dk"
   varsayımı, kariyer tekrarı ölçümünden alındı; oyuncu bunun yerine sonsuz
   mod oynarsa rakam değişir.

3. **Günlük oyun süresi (~15 dk) doğrulanmamış bir devralma.** Eski belgeden
   aldım, o da `ECONOMY_BALANCE_PROPOSAL`'dan almış, o da bir yerden. Zincirin
   başında bir ölçüm yok. §4'teki bütün gün sayıları doğrudan bu sayıyla
   orantılı — 15 yerine 10 dakika ise günler %30 uzar.

4. **"Oyuncu yükseltmeleri araçlardan önce alır" varsayımını test etmedim.**
   §4'ün "yükseltme deposu 6 günde biter" uyarısı buna dayanıyor. Oyuncu
   önce Formula'yı hedefleyip yükseltmeleri geciktirirse eğri tamamen başka
   türlü olur.

5. **Reklam süresi 30 saniye varsaydım.** Pasif ÷ oynanış oranı (2.9×)
   doğrudan buna bağlı. Gerçek AdMob ödüllü reklam süresi 15–60 sn arasında
   değişir; 60 sn ise oran 1.5×'e iner ve §4'ün 2. uyarısı büyük ölçüde
   geçersiz olur.

6. **Çarpışma kutusunun oynanışa etkisini ölçmedim.** Tır'ın 3.2 kat büyük
   kutusunun "kaç kat daha sık ölüm" ettiğini bilmiyorum. Ö1'deki 2.200
   rakamı sadece hız/boost aritmetiğinden geldi; kutu cezası fiyata hâlâ
   girmedi. Doğru yol, Tır'la 30 bölümü oynatan bir ölçüm yapmak.

7. **Fren ekseninin değersiz olduğu iddiam kısmen çıkarım.** Ölçüm temkinli
   otopilotun frene hiç basmadığını gösteriyor, ama otopilot insan değil.
   İnsan oyuncu freni yoğun kullanıyorsa Dağ Keçisi'nin primi haklı olabilir.

8. **Haftalık gelirin 250/gün mü 114/gün mü olduğunu ölçmedim.**
   `big_combos` görevinin (haftada 12 × 5x combo) temkinli oyunla
   tamamlanabilirliğini doğrudan test etmedim; bölüm yıldız ortalamalarından
   çıkardım. Bu görev tamamlanmazsa haftalık sandık da kapanır ve §4'ün
   gün sayıları ~%5 uzar.

9. **Cihazda hiçbir şey doğrulamadım.** Bu belgedeki her ölçüm JVM birim
   testinden; Samsung S8'de tek bir koşu oynanmadı. Cihazın ~40 FPS'i ile
   testin sabit 62.5 FPS'i (`dt = 0.016`) aynı sonucu vermeyebilir.

10. **Ölçüm 3 tohumla yapıldı, 5 ile değil.** Tohum yayılımı dar
    (çoğu bölümde min–max farkı %5'in altında), ama 30 bölüm × 3 senaryo
    zaten uzun sürüyordu. Bölüm 16, 22 ve 28'de yıldız ortalamaları kesirli
    (2.33, 2.33, 2.67) — yani o bölümlerde tohum gerçekten sonucu
    değiştiriyor ve 3 tohum orada az.
