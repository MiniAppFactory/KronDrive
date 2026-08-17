# Ekonomi durumu — 2026-08-17

Bu belge `docs/ECONOMY_BALANCE_PROPOSAL.md`'nin (2026-08-16) **yerine geçer**,
onu silmez. O belgenin taşıyıcı varsayımı çöktü; aşağıda hangi sayısının neden
geçersiz olduğu ve yerine ne konduğu yazılı.

## Dürüstlük çerçevesi

**Oyuncu verisi yok — oyun yayınlanmadı.** Bu belgede "retention %X artar"
tipi tek bir sayı yok, olamaz da. Mekanizma üzerinden konuşuluyor: *şu
değişiklik şu döngüyü şöyle etkiler.*

Her sayı üç etiketten birini taşır:

| Etiket | Anlamı |
|---|---|
| **ÖLÇÜM** | `LevelCurveTest` gerçekten oynadı, çıktı bir dosyaya yazıldı |
| **TÜRETME** | Koddan aritmetikle çıkarıldı; formül gösteriliyor, tahmin yok |
| **TAHMİN** | Oyuncu davranışı varsayımı içeriyor; varsayımı değiştirin, sonuç değişir |

---

## 0. Yönetici özeti

1. **Elimizdeki tek ölçüm 8 bölümlük, tek tohumlu ve üç değişiklik eskimiş.**
   30 bölümü oynatan test (`LevelCurveTest.kt:321`) var ama **çıktısı hiçbir
   dosyaya yazılmamış** — repoda 9–30. bölümler için tek bir ölçülmüş coin
   sayısı yok. Öneri 1 bunu kapatıyor ve diğer dördü onun sonucuna göre
   ayarlanmalı.
2. **`REVIEW_REGRESSION.md`'nin "93 coin / −%7" sonucu ölçüm değil, iki kat
   türetme — ve tabanı hatalı.** Düzeltilmiş rakam ve gerçek mekanizma §2'de.
3. **İki ekonomi kaldıracı (skor/70 + günlük 500) işe yaradı ve ölçülebilir.**
   Pasif gelirin oynanış gelirine oranı **8.2× → 1.8×** indi (§4).
4. **Çift Ödül booster'ı hâlâ zararda: en iyi gerçekçi koşuda net −52.**
   Öneri 3.
5. **Garaj, kariyerde hiçbir zaman ulaşılmayacak bir hız yazıyor** (180–216
   km/h; kariyerdeki gerçek 161–196). Öneri 2.
6. ⚠ **Yayın engeli:** `STARTING_COINS = 100_000`
   (`data/PlayerProgress.kt:125`). Bu belgedeki her ilerleme hesabı yayın
   değeri **100** varsayar. `PLAY_RELEASE_CHECKLIST` S-7.

---

## 1. Elimizdeki ölçüm — ne var, ne yok

### Var olan tek ölçüm

`LevelCurveTest.olcum dokumu` (`LevelCurveTest.kt:297`), **2026-08-16**.
Çıktısı `docs/CHANGELOG.md:13-23`'te. Kapsamı:

- **Yalnızca ilk 8 bölüm** (`LEARNING_LEVELS = LevelCatalog.levels.take(8)`,
  `LevelCurveTest.kt:358`)
- **Yalnızca tek tohum** (`SEEDS.first()` = 1)
- **Yalnızca TEMKİNLİ profil** (yapay otopilot, gerçek oyuncu değil)

| Bölüm | Skor | Toplanan | Yıldız | `/120` | `/70` |
|---|---|---|---|---|---|
| 1 | 1.790 | 17 | 3 | 106 | 117 |
| 2 | 2.281 | 18 | 3 | 112 | 125 |
| 3 | 1.400 | 10 | 3 | 96 | 105 |
| 4 | 3.635 | 27 | **2** | 107 | 128 |
| 5 | 3.835 | 20 | 3 | 126 | 149 |
| 6 | 4.163 | 31 | **1** | 90 | 115 |
| 7 | 4.270 | 21 | **1** | 81 | 107 |
| 8 | 2.271 | 12 | **2** | 80 | 94 |
| **ort.** | | | **2.25** | **100** | **118** |

Formül doğrulandı: `toplanan×1 + skor/70 + yeniYıldız×25`
(`GameEngine.kt:886-888`). Örnek, bölüm 1: `17 + 1790/70 + 3×25 = 17+25+75 =
117` ✓.

### Olmayan ölçüm

`tam olcum dokumu — otuz bolum` (`LevelCurveTest.kt:321`) 2026-08-16'da
eklendi, üç tohumla 30 bölümü oynatıyor ve tam dökümü basıyor. **Çıktısı hiçbir
belgeye kaydedilmemiş.** Repo genelinde `bolum N TEMKINLI` / `coin=` biçiminde
tek bir yakalanmış satır yok.

Sonuç: **9–30. bölümler için ölçüm sıfır.** `REVIEW_REGRESSION.md:690-692`
bunu kendisi de kabul ediyor: *"B3'teki 16 duvarın 14'ü katalog yapısından
çıkarıldı, ölçümden değil."*

### Ölçümü eskiten üç değişiklik

| # | Değişiklik | Commit | Ölçüme etkisi |
|---|---|---|---|
| 1 | `MIN_STARS_TO_PASS` 3 → 2 | `e7e1310` | Yıldız coini akışının zamanlaması |
| 2 | `startSpeedKmh` 30 bölümde de **60** | `2608363` | Skor −%10 civarı |
| 3 | `speedRampScale` eklendi (1–7) | `6d2b2a4` | Skor −%9 … −%13 (1–7) |

Ölçüm 2026-08-16'da alındı; 2 ve 3 **ertesi gün** girdi. Yani yukarıdaki tablo
bugünkü oyunun tablosu değil.

---

## 2. `ECONOMY_BALANCE_PROPOSAL`'in çöken varsayımı ve "93 coin" düzeltmesi

### Çöken cümle

`ECONOMY_BALANCE_PROPOSAL.md:116-120`:

> Bir bölüm ancak **üç görevin üçü de** tamamlanınca geçilir. Yani her ilk
> geçiş **her zaman** 3 yıldız = 75 coin öder.

Bu cümle `MIN_STARS_TO_PASS = 2` ile (`GameConfig.kt:430`) **yanlış** ve o
belgenin §7 ile §8'inin tamamı bunun üstüne kurulu.

### `REVIEW_REGRESSION`'ın 93 rakamı — neden kullanılmamalı

`REVIEW_REGRESSION.md:299-306` şu ayrıştırmayı kurdu:

| Durum | Yıldız coini | Diğer | Toplam |
|---|---|---|---|
| `/120`, 3 yıldız zorunlu | 75 | 25 | 100 |
| `/70`, 3 yıldız zorunlu | 75 | 43 | 118 |
| `/70`, 2 yıldızla geçiş | **50** | 43 | **93** |

**Bu ayrıştırma ölçümle çelişiyor.** Ölçümde otopilotun aldığı yıldızlar
3/3/3/**2**/3/**1**/**1**/**2**; ortalaması **2.25**, yani ölçülen ortalama
yıldız coini `2.25 × 25 = 56` — 75 değil. 100 ve 118 ortalamaları **zaten 2
yıldızlı ve 1 yıldızlı koşuları içeriyor.**

Yani 75/25 ayrıştırması ölçümün ayrıştırması değil, ideal bir koşunun
ayrıştırması; 93 da onun üstüne kurulmuş ikinci bir türetme. **93 coin ve
−%7, bu belgede kullanılmıyor.**

### Kilit değişikliğinin GERÇEK mekanizması

Kod, yıldız coinini **her yeni yıldız için** öder, kilitten bağımsız olarak
(`GameEngine.kt:881-888`, `newStars = stars − previousStars`). Yani:

1. **Verilen bir koşunun ödemesi değişmedi.** Aynı koşu, aynı yıldız, aynı
   coin.
2. **Değişen şey kaç koşu yapıldığı.** 3 yıldız kuralında 2 yıldızda kalan
   oyuncu ilerlemek için o bölümü **tekrar oynamak zorundaydı**; her tekrar
   ~70 coin ödüyordu ve sonunda 3. yıldızın 25'i de geliyordu.
3. **Ömür boyu yıldız coini tavanı değişmedi** (30 × 3 × 25 = 2.250). Değişen,
   ne zaman geldiği ve gelip gelmeyeceği.

Net sonuç, iki yönlü:

| Ölçü | Yön | Neden |
|---|---|---|
| Coin / dakika | **↑ arttı** | Zorunlu düşük ödeyen tekrar koşuları kalktı |
| Coin / ilerleme adımı | **↓ azaldı** | 3. yıldızı almadan geçen oyuncu 25 coin bırakıyor |
| Ömür boyu tavan | **=** | 3. yıldız sonradan alınırsa 25 yine ödenir |

Üçüncü yıldızın peşine düşmeyen oyuncu 30 bölümde en fazla **750 coin**
kaybeder — kalıcı harcama tavanının **%1.3'ü**. **TÜRETME.**

---

## 3. Hız rampasının gelire etkisi — tam ilişki

Skorun hızdan gelen kısmı ile mesafe arasında **kesin** bir bağ var
(`GameEngine`, `GameConfig.kt:313`, `:204`, `:380`):

```
skor_hız / metre = SCORE_PER_SPEED_PER_SEC ÷ (WORLD_PX_PER_SPEED_UNIT ÷ PIXELS_PER_METER)
                 = 11 ÷ (187.5 ÷ 22.2)
                 = 11 ÷ 8.446
                 = 1.3024
```

**Doğrulama (bölüm 1, rampa öncesi):**

```
862 m × 1.3024        = 1.123   (hızdan)
17 coin × 35          =   595   (toplama)
9 geçiş × 8           =    72   (geçiş)
                        ------
                          1.790  = ÖLÇÜLEN SKOR ✓
```

Commit `6d2b2a4`'ün mesafe ölçümü ile `CHANGELOG.md`'nin skor ölçümü **aynı
koşudan** geliyor ve birbirini bit bit doğruluyor. Bu, rampanın gelire etkisini
tahmin etmeden hesaplamamıza izin veriyor.

### Rampa sonrası tahmini gelir

Geçiş ve coin doğması **zamana** bağlıdır (`GameConfig.kt:270-271`), hıza
değil — bu yüzden yalnızca `skor_hız` terimi küçülüyor.

| Bölüm | Mesafe önce → sonra | Skor önce → sonra | Coin önce → sonra |
|---|---|---|---|
| 1 | 862 → 691 m (ÖLÇÜM) | 1.790 → ~1.567 | 117 → **~114** |
| 4 | 1.788 → 1.522 m (ÖLÇÜM) | 3.635 → ~3.288 | 128 → **~123** |
| 8 | 1.200 → 1.200 m (hedef sabit) | ~aynı, süre uzadı | 94 → **~94** |

Mesafeler **ÖLÇÜM** (commit `6d2b2a4`), coin sütunu **TÜRETME**.
2, 3, 5, 6, 7 için mesafe ölçümü yok; aynı büyüklükte (−%4 … −%5 coin)
olduğu **TAHMİN**.

**Bölüm başına ilk geçiş geliri: 118 → ~113 (−%4).**

### Yan etki: `FinishUnderSeconds` yıldızları zorlaştı

Başlangıç hızı 80 → 60 düştüğü için mesafe hedefli bölümler daha uzun sürüyor.
Ölçümde bölüm 8 hedefi 36 s, koşu 29 s'de bitiyordu (**+%19 pay**,
`REVIEW_REGRESSION.md:263`). ~%10 daha düşük hızda bu ~32 s'ye çıkar, pay
**+%11**'e iner. Bölüm 21 ve 24'ün payları rampa öncesinde bile **negatifti**
(−%3, −%7 — `DIFFICULTY_REVIEW.md:540-541`).

Dokuz bölümde (8, 10, 12, 15, 18, 21, 24, 27, 30) `FinishUnderSeconds`
**üçüncü** sırada olduğu için ilerlemeyi tıkamıyor; yalnızca 3. yıldızı ve
onun 25 coinini yükseltme arkasına koyuyor. En kötü hâlde kariyer geliri
**−225 coin**. **TÜRETME + TAHMİN.**

---

## 4. İki ekonomi kaldıracı işe yaradı — ölçülebilir kısım

`ECONOMY_BALANCE_PROPOSAL`'in ana bulgusu şuydu: *"Oyunun en yüksek
coin/dakika oranı, oyunun kendisi değil, günün ilk koşusudur."* İki sabit
değişti; oran şimdi şöyle (**TÜRETME**, süreler bölüm tanımlarından):

| Etkinlik | Coin | Süre | Coin/dk | Günlük tavan |
|---|---|---|---|---|
| Ödüllü reklam ×5 | 750 | ~3 dk | **250** | 5 kez |
| Günlük görev (3 kademe + koşu) | ~625 | ~3 dk | **208** | 1 kez |
| Kariyer bölümü, tekrar | ~74 | 45 s | **99** | sınırsız |
| Sonsuz mod, 90 s | ~125 | 1.5 dk | **83** | sınırsız |

Eski hâliyle (`ECONOMY_BALANCE_PROPOSAL.md:159-173`):

| Ölçü | 2026-08-16 öncesi | Bugün |
|---|---|---|
| En yüksek pasif kaynak | Günlük görev, 300/dk | Ödüllü reklam, 250/dk |
| En yüksek **sınırsız** oynanış kaynağı | Tekrar, 45/dk | Tekrar, **99/dk** |
| Oran (pasif ÷ oynanış) | **8.2×** | **1.8×** |

**Bu, iki sabit değişikliğinin doğrudan sonucudur ve mekanizma üzerinden
söylenebilir:** `SCORE_PER_BONUS_COIN` oynanışın tek ölçeklenen kalemini
büyüttü (`GameConfig.kt:434-449`), `TIER_REWARDS` tavanlı kalemi küçülttü
(`DailyChallengeGenerator.kt:92`).

### Yeni bir yan etki: sonsuz mod kariyerden daha iyi ödemeye başladı

Başlangıç hızı yalnızca **kariyerde** 60'a indi; sonsuz modda bölüm yok,
dolayısıyla taban `BASE_SPEED` = 2.63 (80 km/h) kaldı
(`GameEngine.kt:106-107`). Yani sonsuz mod rampa değişikliğinden hiç
etkilenmedi, kariyer tekrarı ~%10 etkilendi.

Bu **istenmiş bir sonuç değil**, iki ayrı kararın kesişimi. Bugün zararsız
(iki mod da benzer ödüyor) ama kariyeri daha da yavaşlatan bir sonraki
değişiklikte "kariyeri boşver, sonsuz oyna" tavsiyesi doğru cevap hâline
gelebilir. Öneri 1'in ölçümünde bu ikisi yan yana koyulmalı.

---

## 5. Yeni ilerleme eğrisi

### Varsayımlar (değiştirin, sonuç değişir) — **TAHMİN**

| Varsayım | Değer | Kaynak |
|---|---|---|
| Günlük oyun süresi | ~15 dk | `ECONOMY_BALANCE_PROPOSAL.md:337` (devralındı) |
| Kariyer fazında koşu/gün | ~20 (10 ilk geçiş + 10 tekrar) | aynı |
| Kariyer sonrası koşu/gün | ~12 | aynı |
| Günlük görev | her gün yapılıyor | aynı |
| Ödüllü reklam | izleyen 5/5, izlemeyen 0 | aynı |
| Haftalık gelir | 1.750 ÷ 7 = 250/gün | `WeeklyMissionGenerator.kt:13,25-53` |
| Bölüm 1–8 ilk geçiş | ~113 coin | §3 |
| Bölüm 9–30 ilk geçiş | ~145 coin | süre uzun, skor yüksek — **TAHMİN** |
| Kariyer tekrarı | ~74 coin | bölüm 5 ÖLÇÜM |
| Sonsuz koşu (90 s) | ~125 coin | `ECONOMY_BALANCE_PROPOSAL.md:457` |
| Başlangıç bakiyesi | **100** (yayın değeri) | `PlayerProgress.kt:128` |

### Günlük gelir

| Faz | Reklam izleyen | Reklam izlemeyen | Sadece oynayan (görev de yok) |
|---|---|---|---|
| Kariyer (gün 1–3) | ~3.500 | ~2.750 | ~1.890 |
| Kariyer sonrası | ~2.700 | ~1.950 | ~1.330 |

### Araçlar — kaç koşuda, kaç günde

Sırayla ve **başka hiçbir şey alınmadan** (yükseltme yok, boya yok).
Fiyatlar `CarCatalog.kt`; sıra fiyat sırası.

| Araç | Fiyat | Kümülatif | Gereken ilk geçiş sayısı¹ | Reklam izleyen | Reklam izlemeyen |
|---|---|---|---|---|---|
| Beety | 0 | 0 | — | başlangıç | başlangıç |
| Şehir | 350 | 350 | **3** | 1. gün | 1. gün |
| Yarış Sedan | 900 | 1.250 | **8** | 1. gün | 1. gün |
| Kuş SLX | 1.500 | 2.750 | 17 | 1. gün | 1. gün |
| Dağ Keçisi | 1.500 | 4.250 | 27 | 2. gün | 2. gün |
| Kas Arabası | 1.800 | 6.050 | (kariyer > 30) | 2. gün | 3. gün |
| Boğa 67 | 2.400 | 8.450 | — | 3. gün | 4. gün |
| Motosiklet | 2.800 | 11.250 | — | 4. gün | 5. gün |
| Süper Araba | 3.200 | 14.450 | — | 5. gün | 7. gün |
| Tır | 3.600 | 18.050 | — | 6. gün | 8. gün |
| **Formula** | **5.000** | **23.050** | — | **8. gün** | **11. gün** |

¹ "Yalnızca kariyer ilk geçişleriyle, tekrar/görev/reklam olmadan kaç bölüm
geçmek gerekir." 30 bölümün tamamı **~4.100 coin** öder (§5.1), yani
**kariyerin tamamı Kas Arabası'nı (1.800) rahat, Boğa 67'yi (2.400) ancak
öder; Formula'nın (5.000) yanına yaklaşmaz.**

**Şehir'in 350 fiyatı doğru çıktı.** Ölçülen ~113 coin/bölüm ile üçüncü
bölümde alınabiliyor. `ECONOMY_BALANCE_PROPOSAL §6.2`'nin *"oyuncunun garajla
ilk teması 'hiçbir şey alamazsın' oluyor"* şikâyeti **çözüldü** — o zaman en
ucuz gövde 900'dü.

### Araç seviyesi kapısı artık bağlayıcı değil

XP = `skor/10 + yıldız×20` (`GameConfig.kt:452-453`), seviye = `1 + xp/500`
(`:456`). Ölçülen skorlarla ilk 8 bölüm **~2.720 XP** veriyor → **araç
seviyesi 6**. Formula'nın istediği seviye 8 (3.500 XP) 10–11. bölüm civarında
açılıyor, oysa parası 8–11 gün sürüyor.

Yani seviye kapıları **paradan çok daha önce** açılıyor — 2026-08-16'da
ölçülen "kapı, gereken paranın %40'ında açılıyor" oranı bugün daha da düşük.
Kapılar artık ilerlemeyi şekillendirmiyor, sadece erken oyunda vitrini
kısıtlıyor. **TÜRETME.**

### Tam tamamlama

Kalıcı harcama tavanı **57.150 coin** (10 ücretli gövde 23.050 + boyalar net
6.100 + yükseltmeler 28.000 — hesabı `BALANCE.md` §11'de).

| Oyuncu | Tüm araçlar | %100 (araç + boya + yükseltme) |
|---|---|---|
| Reklam izleyen | ~8 gün | **~21 gün** |
| Reklam izlemeyen | ~11 gün | **~28 gün** |
| Sadece oynayan | ~15 gün | **~42 gün** |

Karşılaştırılabilirlik için eski hedefle (`31.200` = dört dal + Süper Araba):
reklam izleyen ~13 gün, sadece oynayan ~22 gün. `ECONOMY_BALANCE_PROPOSAL`
aynı hedef için **11** ve **31** gün modelliyordu; yani **oyuncu tipleri
arasındaki fark 2.8× → 1.7×** indi. Bu, iki ekonomi kaldıracının istenen
sonucu. **TAHMİN** (her iki taraf da model).

⚠ **Üç kalıcı-tavan rakamı dolaşımda ve üçü de farklı:** 46.450
(`ECONOMY_BALANCE_PROPOSAL §2.2`, Beety öncesi), 51.150
(`REVIEW_REGRESSION §7`), 49.400 (`REVIEW_PRODUCT §4.1`). Fark, fabrika
boyalarının nasıl düşüldüğünden geliyor. **Bugünkü doğru rakam 57.150'dir**
ve üç yeni araç ile Gün Sarısı onu eskilerin hepsinin üstüne çıkardı.

---

## 6. Öneriler — öncelik sırasıyla

> Hiçbiri uygulanmadı. Bu turda kod değişmedi, Gradle çalışmadı, cihaza
> dokunulmadı.

---

### Öneri 1 — 30 bölümün ölçümünü al ve dosyala (**en yüksek öncelik**)

**Ne.** `LevelCurveTest` içindeki `tam olcum dokumu — otuz bolum` testini
çalıştır, çıktısını `docs/MEASUREMENT_30.md` olarak kaydet, `BALANCE.md` ve bu
belgeden oraya bağlan. İki satır ek: koşu sonu coinini de bastır
(`RunResult.coinsEarned`) ve sonsuz modun 90 s'lik referans koşusunu aynı
döküme ekle.

**Neden.** Bugün ekonomiyle ilgili her karar, **8 bölümlük, tek tohumlu ve üç
değişiklik eskimiş** bir ölçüme dayanıyor. 9–30. bölümler bir kez bile
oynatılmadı; o 22 bölüm kariyerin **%73'ü** ve gelirinin (tahminen)
**%78'i**. Test zaten yazılmış ve assert içermiyor — yalnızca çıktısı
kaydedilmemiş. Bu, tüm belgede **karşılığı en yüksek tek iş**.

**Hangi sabit.** Hiçbiri. Yalnızca test çalıştırma + belge.

**Tahmini etki.** Bu belgedeki ~15 "TAHMİN" etiketi "ÖLÇÜM"e dönüşür. §5'in
gün sayıları, §3'ün rampa tahmini ve Öneri 3'ün fiyat aritmetiği doğrudan
bunun üstüne oturur.

**Ne bozulabilir.** Hiçbir şey — test assert içermiyor
(`LevelCurveTest.kt:316`). Tek risk **zamanlama**: Öneri 2, 3, 4, 5'in
herhangi biri uygulandıktan sonra alınan ölçüm "mevcut hâlin fotoğrafı"
olmaz. **Ya hepsinden önce ya hepsinden sonra alın, arada değil**
(`REVIEW_PRODUCT.md:609-611` aynı uyarıyı yapıyor).

---

### Öneri 2 — Garaj, kariyerde ulaşılmayan bir hız yazıyor

**Ne.** `UpgradeCatalog.displayValue` (`UpgradeCatalog.kt:153-155`) SPEED
değerini `GameConfig.BASE_SPEED` (2.63 = 80 km/h) tabanıyla hesaplıyor. Ama
2026-08-17'den beri **30 kariyer bölümünün 30'u da 60 km/h'den başlıyor**
(`LevelCatalog.kt`), yani taban 2.0.

| | Garaj yazıyor | Kariyerde gerçek | Fark |
|---|---|---|---|
| SPEED seviye 1 | 180 km/h | **161 km/h** | −19 |
| SPEED seviye 8 | 216 km/h | **196 km/h** | −20 |

Garajın sayısı yalnızca **sonsuz modda** doğru (`GameEngine.kt:106-107`).
Üstelik bölüm 1–7'de `speedRampScale` tavanı ayrıca kısıyor: bölüm 1'de
gerçek tavan **100 km/h**. **TÜRETME**, hesap `BALANCE.md` §2'de.

**Neden.** Bu tam olarak 2026-08-16'da kapatılan kusurun aynısı: o zaman
garaj araç çarpanını atlıyordu ve "hız yükseltmesi aldım ama bir şey
değişmedi" hissinin doğrudan kaynağıydı (`UpgradeCatalog.kt:144-151`). Aynı
his şimdi başka bir sebeple geri geldi ve **bu sefer sayı gerçekten yanlış**
— oyuncu 250 coin verip 182 yazısını görüyor, sonra bölüm 3'te 116 km/h'yi
geçemiyor.

**Hangi sabit.** `UpgradeCatalog.displayValue`, `UpgradeType.SPEED` dalı.
Üç seçenek, tercih sırasıyla:

1. **Bağlama göre göster** — garaj hangi moddan açıldıysa o tabanla yaz.
   Doğru ama iki taban gerektirir, `displayValue`'ya parametre ekler.
2. **En düşük tabanı yaz** (2.0 → 161–196 km/h) + altına "sonsuz modda daha
   yüksek". Tek satır, hiçbir zaman **fazla** vaat etmez.
3. Değiştirme, `BALANCE.md`'de belgele. En ucuzu; sayı yanlış kalır.

**Tahmini etki.** Denge değişmez, tek satır bile. Etkilenen tek şey oyuncunun
yükseltmeyi satın alırken kurduğu beklenti — ve o beklenti şu an ölçülebilir
şekilde yanlış.

**Ne bozulabilir.** Seçenek 2'de garaj sayısı **düşer** (180 → 161). Sahibi
"araba yavaşladı mı?" diye sorabilir — hayır, sayı düzeldi. `UpgradeCatalog`
testlerinde beklenen dize varsa güncellenir.

---

### Öneri 3 — Çift Ödül booster'ı: 300 → 120, ya da kaldır

**Ne.** `BoosterType.DOUBLE_REWARD(300)` (`PlayerProgress.kt:17`) koşunun coin
ödülünü ikiye katlıyor (`GameEngine.kt:889-891`). Kâra geçmesi için koşunun
**300'den fazla** ödemesi gerekir.

**Neden.** Ödenebilecek en yüksek gerçekçi koşu — 180 saniyelik günlük görev
koşusu, modellenen ~13.800 skor — `51 + 13.800/70 = 248` coin öder. İkiye
katlanınca kazanç +248, maliyet 300 → **net −52**. Tipik koşuda (ölçülen
113–150) net **−187 … −150**.

Üstelik ×2 **yalnızca koşu ödülüne** uygulanıyor: günlük görev kademelerine,
haftalık ödüllere ve ödüllü reklam coinine uygulanmıyor — o ödemeler
`GameStateRepository` üzerinden ayrı geçiyor (`:591-603`, `:563-575`,
`:239-245`). Yani booster'ın kapsayabileceği en büyük kalem zaten dışarıda.

**Bu, oyunda satılan ve satın alanı her koşuda zarara sokan tek üründür.**
`ECONOMY_BALANCE_PROPOSAL §6.1` bunu 2026-08-16'da bulmuş (o zamanki net
−134), `SCORE_PER_BONUS_COIN` 70'e inince zarar küçülmüş ama kapanmamış
(`REVIEW_PRODUCT §4.4`).

**Hangi sabit.** `BoosterType.DOUBLE_REWARD(300)` → **120**.

120 rastgele değil: ölçülen tipik koşu geliri **113–150** aralığında. 120'de
booster *iyi koşuda kâr, kötü koşuda zarar* olur — yani bir **bahis**, garantili
zarar değil. Fiyatı 100'ün altına indirmek onu "her koşuda al" hâline getirir
ve otomatik kâr döngüsü kurar; 150'nin üstü ise tipik koşunun üstünde kalır ve
sorun sürer.

**Alternatif (daha büyük iş):** fiyat kalsın, kapsam genişlesin — günlük görev
kademelerini de ikiye katlasın. Reddediyorum: günlük görev zaten **en büyük
tek coin kaynağı** ve tam da onu küçültmek için `TIER_REWARDS` 900'den 500'e
indirilmişti. İkiye katlamak o kararı geri alır.

**Tahmini etki.** Booster'ın kullanım sıklığı ölçülemez (oyuncu verisi yok).
Ekonomik tavanı hesaplanabilir: en yoğun kullanımda bile oynanış gelirini en
fazla ikiye katlar, ki oynanış geliri günlük gelirin yaklaşık **yarısı** —
yani günlük gelir tavanı en kötü hâlde ~%50 artar. Kıyas: ödüllü reklam
tek başına günlük gelirin ~%28'i.

**Ne bozulabilir.**

- **Coin enflasyonu.** Bir koşu gerektirdiği için oynanış süresine bağlı ve
  sınırlı; yine de tavan hesabı Öneri 1'in ölçümüyle yeniden yapılmalı.
- **Booster fiyat merdiveni.** Şu an 150 / 250 / 300 / 400. 120 en ucuz kalem
  olur ve Turbo Başlangıç'ın (150) altına iner. Turbo Başlangıç'ın etkisi
  koşuya bağlı değil (3 saniye bedava boost, `GameConfig.kt:463`), Çift
  Ödül'ünki bağlı — merdiven bozulmuş görünse de mantık tutarlı: **belirsiz
  ödül daha ucuz olmalı.**
- **Sıfır risk seçeneği:** booster'ı katalogdan tamamen kaldırmak. Kalan üç
  booster'ın üçü de matematiksel olarak savunulabilir. Sahibi "zararlı ürün
  satmayalım" derse en temiz cevap budur.

---

### Öneri 4 — Görevlerdeki ulaşılamaz beceri hedeflerini kariyerle hizala

**Ne.** İki görev hedefi, kariyerde 2026-08-16'da kaldırılan hatanın aynısını
taşıyor:

| Görev | Hedef | Sorun |
|---|---|---|
| Günlük `combo` şablonu | 3× / 5× / **7×** | `COMBO_MULTIPLIERS` **5'te doyuyor** (`GameConfig.kt:352`) — oyun 7× comboyu ödüllendirmiyor |
| Haftalık `perfect_dodges` | 15 / 35 / **60** | ÖLÇÜM: temkinli oyun 8 bölümün hiçbirinde **tek bir dodge bile** yapmıyor |

**Neden.** Kariyer kataloğunda `PerfectDodges` hedefleri tam bu iki gerekçeyle
kaldırıldı ve combo 5 ile sınırlandı (`LevelCatalog.kt:52-65`). Aynı gerekçe
görev tarafında uygulanmadı; iki hedef **denetimden kaçtı**.

Cihazda dodge penceresi 40 FPS'te **tek kare, ~25 ms**; insan tepki tabanı
~250 ms (`REVIEW_GAMEPLAY.md`). Yani haftalık 60 dodge, oyunun kendi ölçümüne
göre temkinli oyuncuya **tamamen kapalı**. Bu, "haftalık sandığı hiç
açamıyorum" demektir — sandık 750 coin + 1 booster, haftalık gelirin **%43'ü**
(`WeeklyMissionGenerator.kt:13,16`).

**Hangi sabit.**

- `DailyChallengeGenerator.kt:130-136` → `ComboAtLeast(3/4/5)`
- `WeeklyMissionGenerator.kt:46` → `MissionTier(15,40), MissionTier(35,60),
  MissionTier(60,100)` kademelerini düşür, ya da görevi
  `MissionType.PERFECT_DODGES`'tan başka bir eksene çevir

**Tahmini etki.** Haftalık sandığın gerçekten açılabilir olması, sadece
oynayan oyuncunun günlük gelirine ~250 coin/gün ekler — yani **§5'teki "sadece
oynayan" satırı 1.330 → ~1.580**, tamamlanma ~42 günden ~36 güne iner.
Ölçülemeyen kısım: kaç oyuncunun risk aldığı.

**Ne bozulabilir.**

- **Haftalık gelir tavanı yükselir** (bugün 1.750, pratikte 1.000). Öneri 3
  ile birleşince ikisi aynı yöne iter; Öneri 1'in ölçümünden sonra birlikte
  değerlendirilmeli.
- **Perfect dodge mekaniği tamamen ödülsüz kalabilir.** Kariyerden çıkarıldı,
  günlükten çıkarılırsa geriye yalnızca skor/combo bonusu kalır. Bu **kabul
  edilebilir** — mekaniğin amacı zaten "risk almak ödüllendirilir", zorunlu
  bir hedef olmak değil. Ama `game/GameConfig.kt`'teki dodge sabitlerinin
  neden var olduğu belgelenmiş kalmalı, yoksa altı ay sonra biri onları ölü
  kod sanır.
- Günlük görev testleri kademe değerlerini kontrol ediyorsa güncellenir.

---

### Öneri 5 — Yükseltmenin ilk adımı: teşhis yanlış, sorun gerçek

**Ne (teşhis düzeltmesi).** `ECONOMY_BALANCE_PROPOSAL §5.1` sorunu
*"maliyet doğrusal, etki dışbükey — en kötü kombinasyon"* diye koydu.
**Bu doğru değil.** Tek bir satın alma doğrusal fiyatlıdır (`250 × seviye`,
`UpgradeCatalog.kt:44-45`) ama **kümülatif** maliyet karesel:

```
kümülatifMaliyet(seviye) / 7000 = 0.875 t² + 0.125 t        (t = (seviye−1)/7)
kümülatifEtki(seviye)           = t^1.5                     (UpgradeCatalog.kt:64-69)
```

| Seviye | Ödenen coinin %'si | Alınan etkinin %'si | Oran |
|---|---|---|---|
| 2 | %3.6 | %5.4 | **1.51** |
| 3 | %10.7 | %15.3 | 1.43 |
| 4 | %21.4 | %28.1 | 1.31 |
| 5 | %35.7 | %43.2 | 1.21 |
| 6 | %53.6 | %60.4 | 1.13 |
| 7 | %75.0 | %79.4 | 1.06 |
| 8 | %100 | %100 | 1.00 |

**Etki eğrisi her seviyede maliyet eğrisinin üstünde** ve fark ilk
seviyelerde en büyük. Yani coin başına en iyi alışveriş **ilk yükseltmedir**.
Fiyat/etki oranı bozuk değil. **TÜRETME.**

**Neden (gerçek sorun).** Sorun oran değil **algılanabilirlik**. İlk 250 coin
ne alıyor:

| Dal | Seviye 1 → 2 | Göreli |
|---|---|---|
| SPEED | 180 → **182** km/h | %1.1 |
| ACCELERATION | 167 → **161** ms | %3.6 |
| BRAKE | −28.4 → **−29.4** km/h | %3.5 |
| BOOST | 2.63 → **2.69** s | %2.3 |

(`ECONOMY_BALANCE_PROPOSAL §5.1` bu adımı "+1 km/h" yazıyor; kodun kendi
yuvarlaması `toInt()` olduğu için garajda gerçekten **180 → 182**, yani +2.
Argüman değişmiyor.)

Oyuncu 250 coin — yani **iki bölümlük ilerlemesi** — karşılığında %1'lik bir
sayı görüyor. Bunun üstüne Öneri 2'deki sorun biniyor: gördüğü o sayı
kariyerde zaten geçerli değil.

**Hangi sabit.** Üç seçenek, tercih sırasıyla:

1. **Sabit değiştirme, GÖSTER.** Garajda dalın hedefini ve doluluğunu yaz:
   "HIZ 2/8 · 182 km/h · tam: 216 km/h". Oyuncu %1'lik adımı değil **yolun
   1/7'sini** görür. Denge riski **sıfır**, iş yalnızca `GarageScreen`.
2. **`CURVE_EXPONENT` 1.5 → 1.2.** `curve(2)` 0.054 → 0.097, ilk adım
   180 → 185 km/h olur (%2.8). Risk: eğri doğrusala yaklaşır ve 2026-08-14'te
   düzeltilen *"upgrade'ler çok hızla iyileşiyor"* şikâyeti geri gelebilir —
   üs 2.0 → 1.5 zaten bir kez bu yüzden kısıldı (`UpgradeCatalog.kt:60-62`).
3. **`MAX_LEVEL` 8 → 6.** Adımlar büyür, toplam maliyet düşer. En büyük
   değişiklik; kayıtlı ilerlemeyi (seviye 7–8'de oyuncu varsa) göçürmek
   gerekir. Bu turda önerilmiyor.

**Tahmini etki.** Seçenek 1'de hiçbir sayı değişmez, yalnızca ilk satın
almanın "bir yolun başlangıcı" olarak okunması hedeflenir. Ölçülemez —
oyuncu verisi yok. Seçenek 2'de dört dalın da 2–4. seviyeleri hissedilir
şekilde güçlenir; buna karşılık 30 bölümün zorluk eğrisi **yükseltmesiz**
oyuncuya göre ayarlandığı için kariyer tarafında bir şey bozulmaz, sonsuz mod
rekorları şişer.

**Ne bozulabilir.**

- Seçenek 2, `UpgradeCatalogTest`'in eğri değerlerini kilitliyorsa kırar.
- Seçenek 2 aynı zamanda **`FinishUnderSeconds` yıldızlarını kolaylaştırır**
  (§3'te zorlaştıkları söylenen dokuz bölüm). Bu tesadüfen iyi bir yan etki
  ama iki değişikliği aynı anda yapmak, Öneri 1'in ölçümünü ikisinin toplamı
  hâline getirir — hangisinin ne yaptığı ayrılamaz.
- Seçenek 1'in riski yok ama sorunu **çözmez**, yalnızca doğru çerçeveler.
  Sahibi "hâlâ az" derse seçenek 2 masada kalır.

---

## 7. Bu turda ÖNERMEDİĞİM ve neden

| Fikir | Neden hayır |
|---|---|
| `REWARDED_COIN_AMOUNT` 150 → 120 | `ECONOMY_BALANCE_PROPOSAL §8` dördüncü kolu olarak ertelemişti; `REVIEW_REGRESSION.md:406-409` **iptal** dedi. Günlük görev 500'e inince ödüllü reklam zaten günün en büyük tek kaynağı oldu; daha da kısmak reklam izleyen oyuncuyu iki kez cezalandırır. |
| Araç fiyatlarını düşürmek | §5'e göre araçlar zaten 1–11 gün arasında dağılıyor ve merdiven düzgün. Sorun fiyat değil, fiyatın **karşılığının garajda yanlış yazılması** (Öneri 2). |
| Yükseltme maliyetini etkiyle hizalamak | Hizalarsa ilk adım **250 → 378 coin**'e çıkar; onboarding kötüleşir. Öneri 5'teki tabloya bakın: hizalama gereksiz, etki zaten maliyetin önünde. |
| `MIN_STARS_TO_PASS`'i geri almak | Ölçüm 3 yıldız kuralında 19 duvar, 2 yıldızda 16 duvar gösteriyordu; dodge hedefleri kaldırıldıktan sonra kariyer uçtan uca bitirilebilir oldu (commit `c24c00d`). Geri almak o işi çöpe atar. |
| Sonsuz modun tabanını 60'a indirmek | §4'teki asimetriyi kapatırdı ama sonsuz mod prototipin taban davranışıdır ve `PROVENANCE.md`'ye bağlı. Önce Öneri 1'in ölçümü, sonra karar. |

---

## 8. Emin olmadıklarım

- **9–30. bölümlerin geliri hakkında hiçbir şey bilmiyorum.** §5'teki "~145
  coin/bölüm" bir tahmindir; kariyer gelirinin %78'i o tahminin üstünde
  duruyor. Öneri 1 bunun için var.
- **Rampa sonrası coin sayıları ölçülmedi.** §3'teki 114 / 123 rakamları,
  ölçülmüş mesafeler + kesin bir formülden türetildi; ama geçiş ve coin
  sayılarının değişmediğini **varsaydım**. Yaklaşma hızı düştüğü için geçiş
  sayısı da bir miktar düşmüş olabilir, yani gerçek gelir tahminimden biraz
  daha düşük olabilir.
- **Gün sayılarının hiçbiri ölçüm değil.** 15 dk/gün ve 20 koşu/gün
  varsayımları `ECONOMY_BALANCE_PROPOSAL`'den devralındı; onlar da tahmindi.
- **Booster kullanım sıklığı bilinmiyor.** Öneri 3'ün "coin enflasyonu"
  riskini büyüklük olarak sınırlayabildim ama gerçekleşme olasılığını değil.
- **Cihazda oynamadım** (görev sınırı gereği). Buradaki her davranış iddiası
  kod okuması ya da `LevelCurveTest` çıktısıdır.
- **Motosikletin dar kutusunun gelire etkisi ölçülmedi.** Daha az çarpma =
  daha uzun koşu = daha çok coin olabilir; 2.800 coinlik aracın kendini geri
  ödeyip ödemediği bilinmiyor (`VEHICLE_CLASSES.md:183-196` de aynı şeyi
  söylüyor).
