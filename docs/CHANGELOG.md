# Değişiklik günlüğü

## 2026-08-19 (2) — Antrenman modu, seviye atlama bedeli, "Lv 4" sorusu

### Antrenman modu (⚠ GEÇİCİ — AAB'den önce silinecek)

`GameConfig.TRAINING_MODE_SIDE_LANES_ONLY = true`. Trafik yalnızca en sol ve
en sağ şeritte doğuyor, **orta şerit hep boş**. Sahibi test kolaylığı için
istedi. Cihazda doğrulandı.

RNG akışı bilerek bozulmadı: her iki dalda da `random`dan tam bir değer
çekiliyor, yani aynı tohum aynı trafik dizisini üretmeye devam ediyor ve
ölçümler karşılaştırılabilir kalıyor.

**Yayına bu açık çıkarsa** oyuncu orta şeritte durup sonsuza kadar hayatta
kalır — oyun çökmez, sadece bütün zorluk eğrisi ve skor dengesi anlamsızlaşır.
İki iz bırakıldı: `PLAY_RELEASE_CHECKLIST` **S-8** (yeni bloker) ve
`GameEngineTest`'te davranışı doğrulayan bir test. Kalıcı kırmızı test
**bilerek eklenmedi** — projenin kendi kuralı (`PlayerProgressCarTest`):
*"kalıcı kırmızı bir test 'hepsi yeşil' sinyalini yok eder"*.

### Seviye atlama bedeli

Sahibi: *"o kadar ödemek isteyen varsa seviye doldurmadan da ek bir coin
harcayıp aracı açsın"*. Önerdiği formül birebir alındı:

```
bedel = (gerekenSeviye − mevcutSeviye) × 500
```

Formula (seviye 8, 5.000 coin) için cihazda doğrulandı: seviye 1'deki oyuncuya
buton **8500** yazıyor, altında **"5000 + 3500 seviye"**, yanında *"seviye 8
gerekiyor — 3500 coin ile şimdi açabilirsin"*.

Büyüklük neden doğru: bir seviye 500 XP, XP ise `skor/10 + yıldız×20` — yani
koşu başına ~200–500 XP. Bir seviye kabaca bir-iki koşu. 500 coin de kabaca
dört beş bölümlük gelir, yani atlamak beklemekten **ucuz değil**; sabırsız
oyuncuya kapı açıyor, kestirme sunmuyor.

**Dikkat edilen tuzak:** kontrol `canBuy` toplama bakarken tahsilat
`item.priceCoins`'e bakıyor olsaydı oyuncu seviyeyi bedavaya atlardı. Repository
artık `CarCatalog.totalPrice` düşüyor.

`CarUnlockState.LEVEL_LOCKED` artık *"seviyen yetmiyor"* değil, **"seviyen
yetmiyor VE atlama bedelini de karşılayamıyorsun"** demek.

### "Lv 4" — hata değil

Sahibi oyunu açınca araç seviyesi 4 gördü ve *"standart araba 4 lv gerektirdiği
için olabilir"* dedi. Değil: `carLevel = 1 + xp/500` ve `xp = skor/10 +
yıldız×20`. Koşu başına 200–500 XP geliyor, yani **3-4 koşuda Lv 4** normal.
Hiçbir araç seviyeyi zorlamıyor; varsayılan araç (Beety) seviye 1 istiyor.

**KANIT:** 221 birim test / 0 hata (2 yeni: atlama bedeli formülü, antrenman
modu davranışı). `assembleDebug` + `assembleRelease` başarılı. Antrenman modu
ve F1 fiyatı cihazda ekran görüntüsüyle doğrulandı.


## 2026-08-19 — Sonsuz mod araç farkını yiyordu; garaj artık rakam yazıyor

Sahibi *"Beety'nin top speed'i kaç"* diye sordu. Cevap tek sayı değildi ve
aradaki fark sorunun kendisiydi:

| Nerede | Beety yükseltmesiz | tam yükseltme |
|---|---|---|
| Garajda yazan | 120 | 155 |
| Kariyer, bölüm 1 (rampaÖlçek 0,4) | **84** | — |
| Sonsuz mod başı | 139 | 175 |
| Sonsuz mod tam çarpanda | **225** | **240 (tavan)** |

### Sonsuz mod, araç merdivenini yiyordu

Zaman çarpanı (×1,6) hazır hedefin **tamamına** uygulanıyordu ve sonuç
gösterge tavanında (240) kırpılıyordu:

| Araç | Kariyer | Sonsuz, tam çarpanda |
|---|---|---|
| Beety | 120 | 225 |
| Şehir | 124 | 233 |
| Süper Araba | 168 | **240** |
| Formula | 184 | **240** |

Kariyerdeki %53'lük fark sonsuz modda **%7**'ye düşüyordu. Yani 18 Ağustos'ta
açılan araç merdivenini sonsuz mod yiyordu.

**Düzeltme:** çarpan artık yalnızca RAMPAYA uygulanıyor, tabana değil. Rampa
zaten `scoreSpeedCap` üzerinden aracın çarpanını taşıdığı için iyi araç
zamanla **daha çok** kazanıyor. Boost ve fren çarpanın dışında kaldı.

Sonuç (Beety artık kırpılmıyor): 139 → **232**. Şehir 144 → 240.

**⚠ ÜST YARI HÂLÂ KIRPILIYOR** ve bu açık bir madde:

| Araç | sv | ham değer | gösterge |
|---|---|---|---|
| Süper Araba | 1 | 252 | 240 |
| Süper Araba | 8 | **309** | 240 |
| Formula | 1 | 279 | 240 |
| Formula | 8 | **336** | 240 |

Aritmetik acımasız: Formula tam yükseltmeli, sonsuz modun tabanıyla (80 km/h)
**çarpan olmadan bile** 240'a oturuyor. Yani mevcut merdivenle 240'lık gösterge
ve zaman çarpanı aynı anda var olamıyor. Seçenekler: gösterge tavanını
yükseltmek (~290), sonsuz modun tabanını kariyerinkine (60) indirmek, ya da
çarpanı küçültmek. **Sahibinin kararı bekleniyor.**

### Garaj artık yüzde değil rakam yazıyor

Sahibi: *"araç tanıtım garajında hız gibi özellikler var ama rakamlarla
yazmıyor, +/- şeklinde"*. Yüzde iki şeyi birden gizliyordu: aracın gerçek
hızını ve yükseltmelerle nereye gidebileceğini.

Araç kartı artık somut aralık gösteriyor (cihazda doğrulandı):

```
HIZ    120 → 155 km/h        FREN   -28.4 → -47.4 km/h
İVME   167 → 100 ms          BOOST  2.63 → 4.17 s
```

Uç noktalar `displayValue` ile hesaplanıyor, yani birim ve yuvarlama kuralları
tek yerde kalıyor. Çubuğun rengi hâlâ referans araca göre farkı anlatıyor.

**KANIT:** 219 birim test / 0 hata. `assembleDebug` + `assembleRelease`
başarılı. Garaj ekran görüntüsüyle doğrulandı.


## 2026-08-18 (5) — Geçiş hedefleri ölçümden türetildi

Sahibi: *"4. bölümde sürekli boosta bassan bile 29 araç geçmen imkânsız"* ve
*"3. bölüm 6, 4. bölüm 29, 5. bölüm 18 — saçma olmuş"*. İkisi de doğruydu.

**Kök sebep bendim:** aynı gün dünya %40 yavaşlatılınca hedefleri körü körüne
×0.6 ile ölçekledim. Ölçüm değil aritmetikti — oysa geçilen araç sayısı hıza
doğrusal bağlı değil (bölümün süresi, trafik yoğunluğu ve aracın kendi hızı da
giriyor).

### Ölçümün kendisi de tuzaklıydı

İlk denemede tavanı olduğu gibi ölçtüm ve sayılar hedefe göre değişti:
hedef 36 iken 36, 27 iken 27, 20 iken 24 ölçüldü. Sebep
`GameEngine.checkGoalReached`: kariyerde **tüm hedefler tutunca koşu bitiyor**,
yani hedefi düşürmek koşuyu kısaltıyor ve ölçüm kendi ölçtüğü şeye bağımlı
hâle geliyor.

Doğrusu: hedefleri ulaşılamaz değerlerle değiştirip ölçmek. O zaman koşu
bölümün kendi hedefiyle (süre/mesafe) bitiyor ve **gerçek tavan** çıkıyor.

### Yeni eğri

Hedef = tavanın, bölüm ilerledikçe artan bir oranı (%50 → %78), asla önceki
bölümün %85'inin altına inmeyecek şekilde:

```
2, 5, 8, 12, 17, 17, 20, 25, 23, 30, 34, 36, 39, 40, 43, 45, 45, 45, 50, 52, 52, 59
```

Eski dizi 6 → **29** → 18 diye zıplayıp düşüyordu; artık düzgün yükseliyor.
Tek küçük düşüş 10. bölümde (25 → 23) ve gerçek bir sebebi var: o bölüm mesafe
hedefli ve komşularından kısa, tavanı 30 (komşusununki 44).

Belirgin değişimler: **bölüm 4: 29 → 12** (ulaşılamazdı), bölüm 9: 36 → 25,
bölüm 30: 63 → 59.

### İki kalıcı bekçi eklendi

Bu hata neden fark edilmedi: mevcut testler *"üç hedeften ikisi tutsun"* diye
bakıyor, yani **tek bir hedefin imkânsız olması sessizce geçiyordu**.

1. `her gecis hedefi ulasilabilir` — hedef, gerçek tavanın %80'ini aşamaz
   (tavan, hedefler kapatılarak ölçülür).
2. `gecis hedefleri yukselen bir egri olusturur` — bir hedef, önceki bölümün
   %85'inin altına inemez.

**KANIT:** 219 birim test / 0 hata. `assembleDebug` + `assembleRelease`
başarılı.


## 2026-08-18 (4) — Sonsuz modda "TEKRAR DENE" (reklamlı) ve bedava reset kaçağı

Sahibi: *"sonsuz modda yandığında tekrar dene deyince reklam çıksın, geri
tuşuna basınca da reklam çıksın ki ücretsiz reset şansı olmasın"*.

### Buton geri geldi — itiraz butona değil, bedavaya idi

`TEKRAR` 2026-08-14'te kaldırılmıştı: *"bedava oyunu reset yapan yer varsa
keşfetmeyi engellemeli"*. Sabit (`INTERSTITIAL_EVERY_N_RETRIES`) ve ViewModel
mantığı **geri alınmak istenirse diye bırakılmıştı** — ikisi de kullanıldı.

Eşik **2 değil 1**: 2'de ilk tekrar bedava kalırdı, yani kapatılmak istenen
kapı açık kalırdı.

### Asıl kaçak: sonuç ekranında sistem geri tuşu

`BackHandler` yalnızca `RUNNING || paused` iken etkindi. Sonuç ekranında devre
dışı olduğu için **sistem geri tuşu nav yığınını doğrudan atıyor ve koşu
hiçbir reklam görmeden bitiyordu** — ekrandaki ANA MENÜ butonu ise reklam
kapısından geçiyordu. Yani aynı niyetin iki yolu yine iki farklı sonuç
veriyordu (2026-08-16'daki hatanın aynı deseni).

Sonsuz modda bu, sınırsız bedava reset demekti: kötü başladın → geri tuşu →
tekrar gir. Geri tuşu artık butonla aynı yoldan gidiyor. Cihazda doğrulandı:
`AdActivity` ön plana geliyor.

### `INTERSTITIAL_EVERY_N_ENDLESS_RUNS` 3 → 1

Kapıyı aynı yola bağlamak yetmedi: kural "3 koşuda bir" olduğu için geri tuşu
koşuların ikisinde yine bedava çıkıyordu (cihazda ölçüldü — ilk denemede
reklam çıkmadı). Sonsuz modda artık her koşu sonunda reklam var.

⚠ **Sonsuz modda reklam yükü üç katına çıktı.** Kariyer ve günlük görev
etkilenmedi: `INTERSTITIAL_EVERY_N_LEVELS` hâlâ 3, ilk bölümler hâlâ reklamsız.
Geri alınmak istenirse tek sayı.

### F1 takılması: ölçüldü, F1'in suçu değil

Sahibi *"F1 aracını seçtim, sürerken ekran çok takıldı"* dedi. Ölçmek için F1
geçici olarak açıldı ve **tema sabitlendi** (temalar arası çizim yükü 4 kat
değişiyor, sabitlemeden karşılaştırma gürültü olurdu):

| | Beety | F1 |
|---|---|---|
| Kare süresi p50 | 28 ms | **28 ms** |
| p90 | 38 ms | 40 ms |
| Missed Vsync | 52/1148 (%4,5) | 54/1155 (%4,7) |

**Çizim maliyeti aynı.** F1'e özel bir kusur yok.

Muhtemel sebep: F1 çok daha hızlı (yeni merdivende 184 km/h, Beety 120). Kare
düşme oranı aynı kalıyor ama düşen her karede dünya iki kat fazla yol aldığı
için **sıçrama iki kat büyük görünüyor**. Yani çözüm F1'de değil, kalan %4,6
kare düşmesinde.

**ÖLÇÜLMEDİ:** bu açıklamanın doğru olup olmadığı — sahibinin F1'i yavaşken de
takılıyor mu diye denemesi gerekir.

**KANIT:** 217 birim test / 0 hata. `assembleDebug` + `assembleRelease`
başarılı. TEKRAR DENE ve geri tuşu akışları cihazda ekran görüntüsü ve logcat
(`AdActivity`) ile doğrulandı. Geçici ölçüm değişiklikleri (F1 kilidi, tema
sabitlemesi) geri alındı.


## 2026-08-18 (3) — Araç merdiveni: taban düşürüldü, yayılım açıldı

Sahibi: *"arabalar sadece görsel ve defaulttan ne kadar +/- sapma o kadar"*,
*"Beety 180 yapamaz"*, *"insanlar ilerledikçe araba aldıklarında aradaki farkı
hissetmeli"*. Ölçüm ikisini de doğruladı ve **tek bir kıyas** her şeyi anlattı:

> Bedava araçtan 5000 coinlik Formula'ya toplam hız kazancı **+%18** idi.
> Tek bir yükseltme dalı (SPEED 1→8) **+%20** veriyordu.
> Yani oyundaki bütün araçları almak, bedava arabanın tek bir dalını sonuna
> kadar açmaktan daha az hız veriyordu.

Kök sebep araçlar değil **taban**dı: referans araç (Beety) 1.00 = 180 km/h,
yani merdivenin ilk basamağı zaten süper araba rakamıydı, yukarıda yer yoktu.

### Değişenler

1. `SCORE_SPEED_CAP_BASE` **3.20 → 1.90** — bedava araç 161 → **120 km/h**.
2. Araç hız çarpanları **0.97–1.18 → 1.00–2.08**. Araçlar arası fark
   **%11 → %108**. Tepe (Formula, tam yükseltme) **220 km/h**, gösterge
   tavanının (240) altında.
3. **Yükseltme artık araç çarpanıyla ÇARPILMIYOR, sabit ekleniyor.**
   Eskiden `cap(level) * carMul` idi; yayılım açılınca bileşik etki Formula'yı
   sv8'de 258 km/h'e çıkarıyordu ve gösterge 240'ta kırpıyordu — oyuncu
   yükseltme alıp hiçbir şey görmeyecekti. Ayrıca ayırmak, yükseltmeyi ucuz
   araçta oransal olarak daha değerli yapıyor (120 üzerine +%29, 184 üzerine
   +%19): yükseltme yetişme aracı, araçlar tavan aracı.
4. **Yükseltme maliyeti `250×seviye` → `150×seviye`** (dal 7.000 → 4.200).
   Bu bir ekonomi tercihi değil, (2)'nin zorunlu sonucu — aşağıya bakın.
5. Kuş SLX `accelMul` 1.00 → **0.96**, Tır `boostMul` 0.94 → **1.20**.
   Gerekçe aşağıda; ikisi de testlerin yakaladığı gerçek hata.
6. Bölüm hedefleri ölçeklendi: 17 `ScoreAtLeast` ×0.75, 9 `ReachDistance`
   ×0.75, 4 `BoostDistance` ×0.75. Skor ve mesafe `speed`ten beslendiği için
   taban düşünce ikisi de düştü.

### Testler iki gerçek hata yakaladı — ikisi de düzeltildi

**(a) Kuş SLX zayıf yönünü kaybetti.** Hız çarpanını 0.97 → 1.18 yapınca
1500 coinlik araç, 350 coinlik Şehir'i **dört eksende birden** geçer oldu —
`hatchback çöpe döndü`. Kimliği "boost uzmanı, hızı zayıf" idi; `accelMul`
0.96 ile zayıf yön geri geldi.

**(b) Tır 3600 coine düpedüz kazıktı.** Gerçekçi (yavaş) hız verilince Kas
Arabası (1800), Boğa 67 (2400) ve Süper Araba (3200) tarafından dört eksende
birden eziliyordu. Kurgusuna uyan gerçek bir üstünlük verildi: ağır motor =
**en uzun boost** (1.20, kataloğun en iyisi).

### Yükseltme maliyeti neden düşmek ZORUNDAYDI

İlk hesabımda dal maliyetini 1.750 sandım; gerçeği **7.000** (`250×seviye`,
1'den 8'e 28 basamak). Doğru tabloyla bakınca araçları güçlendirmek
yükseltmeleri tuzağa çevirmişti:

| Yol | Maliyet | Kazanç | coin/km-h |
|---|---|---|---|
| SPEED dalı 1→8 (eski maliyet) | 7.000 | +35 km/h | **200** |
| Şehir | 350 | +5 | 70 |
| Süper Araba | 3.200 | +48 | 67 |
| Formula | 5.000 | +65 | 77 |

Eski `CarCatalogTest` kuralı (*"fark %10'da kalmalı, yoksa dört yükseltme dalı
anlamsızlaşır"*) tam da bunu önlüyordu; kural kalkınca koruduğu şey de
korumasız kaldı. `150×seviye` ile dal 120 coin/km-h olur: araçlar hâlâ daha
büyük sıçrama verir (istenen bu) ama yükseltme yolu makul kalır.

⚠ Bu, oyunun **en büyük coin gideriydi** (28.000 → 16.800). Ekonominin kalan
yarısı (`docs/ECONOMY_STATUS_20260817.md`) bu sayıya göre yeniden bakılmalı.

### Testlerin felsefesi değişti — ama korudukları şey değişmedi

`UpgradeCatalogTest` ve `GameEngineTest`'teki *"yükseltme her zaman aracın
önünde olmalı"* kuralı kaldırıldı (sahibi tersini istedi). Yerine arkasındaki
gerçek endişe kondu: **yükseltme yolu tuzak olmamalı** — dal, coin başına
hiçbir araçtan iki kattan fazla kötü olamaz. `CarCatalogTest`'teki band
yalnızca SPEED ekseni için açıldı (0.95–2.10); diğer üç eksen 0.80–1.25'te
kaldı.

**KANIT:** 217 birim test / 0 hata. `assembleDebug` + `assembleRelease`
başarılı. Cihazda koşuldu.

**ÖLÇÜLMEDİ:** yeni merdivenin oynanışta doğru hissettirip hissettirmediği ve
araç fiyatlarının yeni güce göre doğru olup olmadığı — ikisi de oyuncu kararı.


## 2026-08-18 (2) — Dünyanın akış hızı %40 azaltıldı

Sahibi isteği: *"dünyanın akış hızını %40 azalt"*. Seçilen yorum **saf ağır
çekim**: ekrandaki dizilim birebir korunur, yalnızca zaman yavaşlar.

`WORLD_SPEED_SCALE` **artık tek düğme değil.** Belgesindeki *"trafik sıklığı
bozulmuyor, çünkü engeller zamana göre doğuyor"* gerekçesi 0.75'te geçiyordu,
0.45'te geçmiyor: dünya yavaşlayınca ardışık araçlar ekranda birbirine
yaklaşıyor.

**Ölçüm — neden üç değişiklik birden gerekti:**

| Denenen | Sonuç |
|---|---|
| Yalnız `WORLD_SPEED_SCALE` 0.45 | Otopilot **bölüm 4'te çarpıyor**, kariyer kesiliyor |
| + doğuş aralığı 1.30 | Çarpma yok ama **bölüm 6** 45 sn'de 28 geçişle 2 yıldız yerine 1 veriyor |
| + `PassVehicles` ×0.6 | **216 test / 0 hata** |

Ayrıca ölçüldü: bölüm tasarımına hiç dokunmadan yapılabilecek en büyük
yavaşlatma **0.64** (%15). 0.62 kırıyor. İstenen %40 bu toleransın çok
ötesinde olduğu için bölüm hedefleri de değişti.

**Değişenler:**

1. `WORLD_SPEED_SCALE` **0.75 → 0.45** — dünya mevcut hızın %60'ında.
2. `OBSTACLE_SPAWN_INTERVAL_SEC` **0.78 → 1.30** (= 0.78 / 0.6) — ardışık
   araçlar arasındaki **ekran** mesafesi eskisiyle aynı kalsın diye.
3. `LevelCatalog`'daki **22 `Objective.PassVehicles` hedefi ×0.6**
   (3→2 … 105→63) — saniyedeki araç %40 azaldığı için eski sayılar
   erişilemez olurdu.
4. `GameEngineTest` içindeki revive testi doğuş aralığını **sabit yazmıştı**
   (`1.2f`, eski 0.78'e göre seçilmiş). Artık `obstacleSpawnInterval()`'dan
   türetiyor, yani bir sonraki denge ayarında yanlışlıkla "hata" raporlamaz.

**Korunanlar:** göstergedeki km/h (yalnızca `speed`ten hesaplanıyor),
metre/saniye (`PIXELS_PER_METER` aynı çarpanla küçülüyor → mesafe hedefleri ve
günlük görevlerin "3000 m" tipi hedefleri kaymadı), skor ve coin formülü
(`speed`ten besleniyor, bu çarpandan değil).

**⚠ AÇIK — kapsam dışı bırakıldı:** coinler de zamana göre doğuyor
(`COIN_SPAWN_INTERVAL_SEC = 1.05`). Ölçeklenmediği için yolda **%40 daha sık**
duruyorlar. Ölçeklenseydi coin geliri saniyede %40 düşerdi — istenmeyen bir
ekonomi değişikliği olurdu, o yüzden dokunulmadı. Sahibi karar verecek.

**KANIT:** 216 birim test / 0 hata. `assembleDebug` + `assembleRelease`
başarılı. Cihazda koşuldu: gösterge 64 km/h (değişmedi), bölüm 1 geçiş hedefi
0/2 (eski 0/3), araç aralıkları normal.

**ÖLÇÜLMEDİ:** yeni hızın oynanışta doğru hissettirip hissettirmediği.


## 2026-08-18 — Takılan rakip araçlar: sebep dt yumuşatmasıymış

Sahibi *"ekrandaki takılarak akan rakip araçlar"* dedi. `3411a4b` bu şikâyet
için yazılmıştı ama kendi commit mesajı *"ÖLÇÜLMEDİ"* diyordu. Cihazda ölçüldü
(S8, 1080x2220, Mali-G71) ve **teşhis yanlış çıktı**.

### Ölçüm

Ham `withFrameNanos` dt histogramı, 1200 kare:

| kova | kare |
|---|---|
| 14–19 ms (1 vsync) | **1151** |
| 19–24 ms | **0** |
| 24–30 ms | **0** |
| 30–37 ms (2 vsync) | 44 |

`framestats`: vsync aralığı ort. **16,68 ms → 60,0 FPS**.

Yani dt **dalgalanmıyor** — vsync'e kusursuz kuantize ve ekran sabit 60 Hz
sunuyor. Tek gerçek olay karelerin **%3,7'sinin düşmesi**. `3411a4b`'nin
dayandığı *"dt dalgalanıyor, nesne 8 dp / 14 dp / 6 dp ilerliyor"* gözlemi
gerçek değil.

### Neden yumuşatma zararlıydı

Filtre (katsayı 0,20 ≈ 5 kare) düşen karedeki 33 ms'i sonraki beş kareye
**yayıyordu**. Beş kare boyunca dünya, ekranın gerçekte gösterdiği zamandan
farklı bir zamana göre ilerliyordu. Tek ve kısa bir hıçkırık, saniyede ~2 kez
tekrarlayan bir salınıma dönüşüyordu — görülen tam olarak buydu.

Filtrenin ürettiği ara değerler sayıldı: hamda **0** kare olan 20–28 ms
kovasında, filtreyle **99** kare vardı.

**Düzeltme:** yumuşatma kaldırıldı, geçen gerçek süre kullanılıyor.
Doğrulama: `buckets=1271, 0, 45, 0, 1, 0, 3` — ara kova artık tam sıfır.
`MAX_FRAME_DT = 0.050` üst sınırı yerinde (arka plandan dönüşte ışınlanmayı
o engelliyor).

### Yan bulgu: iki tam ekran zemin katmanı boşa boyanıyordu

`debug.hwui.overdraw` + ekran görüntüsü piksel sayımı: oyun ekranının
**%98,7'si 4x+ overdraw**. Katmanlar sayıldı, ikisi gereksizdi:

1. `MainActivity` içindeki `Surface(fillMaxSize)` — aynı işi pencere arkaplanı
   zaten yapıyordu. Kaldırıldı; renk `themes.xml`'e
   (`android:windowBackground` → yeni `colors.xml`) taşındı, sistem çubuklarının
   arkası eskisi gibi dolu.
2. `GameScreen`'deki `Box.background(KronColors.Background)` — tuval zaten tüm
   ekranı opak dolduruyor. Sarsıntıda kenarda açılan şeridi artık
   `drawGameScene` **yalnızca sarsıntı varken** dolduruyor.

**Dürüstlük notu:** bu ikisi kare süresini ölçülebilir şekilde
değiştirmedi (p50 28 ms → 28 ms). Fill-rate darboğaz değil. Doğru ve bedava
oldukları için tutuldular, "performans düzeltmesi" diye sayılmazlar.

### Garaj önizlemesi: tır kendi karesinden taşıyordu

Sahibi ekran görüntüsüyle bildirdi. `CarPreview` (`CarArtwork.kt`) sığdırma
kutusunu `CarCatalog.ART_*`'tan alıyordu; o değerler `VehicleClass.BINEK`
(40×76). Ama `drawCarSprite` **aynı gün** (`3411a4b`) her gövdeyi kendi sınıf
kutusuna çizmeye geçirilmişti — önizleme yolu o değişiklikten geçmemişti.

Sonuç: sığdırma 76 birime göre, çizim 202 birime göre → **2,66 kat taşma**.
Tır garajda kendi karesinden taşıp ekranın yarısını kaplıyordu. Motosiklet ters
yönde etkileniyordu (59 birimlik gövde 76'ya göre sığdırıldığı için küçük).

Düzeltme: kutu artık `style.shape.box*`'tan geliyor. Gölge yalnızca vektör
yolunda hesaba katılıyor — `drawCarShadowIfVector` sprite varken zaten
çizmiyor, her zaman katsaydık sabit 42×68 gölge 22 birimlik motosikleti boşuna
küçültürdü.

Tek çağrı noktası: `CarPreview`. Üç ekran (garaj listesi, garaj detayı, bölüm
haritası) oradan geçiyor, üçü de düzeldi. Cihazda doğrulandı.

**KANIT:** 216 birim test / 0 hata. `assembleDebug` + `assembleRelease`
başarılı. Cihazda kuruldu, garajda tır/motosiklet/formula ekran görüntüsüyle
kontrol edildi.

### `3411a4b`'den sızan iki geçici kanca temizlendi

İkisi de "commit EDİLMEZ / geri alınacak" diye işaretliydi ama commit'e girmişti:

- `GameEngine.theme` **CROWD'a sabitlenmişti** — her oyuncu her koşuda aynı
  temayı görüyordu, dört temanın üçü ölüydü. Rastgeleye döndü.
- `GameEngine` içindeki KDPERF sayacı `step()` içinde çalışıyordu (release
  dahil), 120 karede bir `String.format` + `println`. Kaldırıldı.

**KANIT:** 216 birim test / 0 hata. `assembleDebug` + `assembleRelease`
başarılı. Cihazda kuruldu, koşuldu, crash yok, görsel regresyon yok.

**ÖLÇÜLMEDİ:** hareketin sahibinin gözüne artık düzgün görünüp görünmediği —
bu his kararı, cihazda sahibi bakacak.


## 2026-08-16 (3) — Ekonomi: oynanış geliri ölçülüp yükseltildi

`docs/ECONOMY_BALANCE_PROPOSAL.md`'deki öneri 1+2 uygulandı (sahibi onayı).
Sahibi "önce ölç, sonra uygula" dedi; ölçüm `LevelCurveTest.olcum dokumu`
ile yapıldı ve öneri **ölçülen sayılarla** teyit edildi.

**Ölçüm (8 bölüm, temkinli oynayış, ilk geçiş).** Coin formülü kodda
doğrulandı: `toplanan×1 + skor/SCORE_PER_BONUS_COIN + yeniYıldız×25`,
10 saniyenin altındaki koşu hiç ödemiyor.

| Bölüm | skor | toplanan | yıldız | Önce | Sonra |
|---|---|---|---|---|---|
| 1 | 1790 | 17 | 3 | 106 | 117 |
| 2 | 2281 | 18 | 3 | 112 | 125 |
| 3 | 1400 | 10 | 3 | 96 | 105 |
| 4 | 3635 | 27 | 2 | 107 | 128 |
| 5 | 3835 | 20 | 3 | 126 | 149 |
| 6 | 4163 | 31 | 1 | 90 | 115 |
| 7 | 4270 | 21 | 1 | 81 | 107 |
| 8 | 2271 | 12 | 2 | 80 | 94 |
| **ort.** | | | | **100** | **118** |

**Değişen iki sabit:**

1. `SCORE_PER_BONUS_COIN` **120 → 70** (`GameConfig.kt`). Bölüm başına ilk
   geçiş ortalaması 100 → 118 coin (+%18). Etki **tekrar oynamada çok daha
   büyük**: yıldız coini yalnızca YENİ yıldız için ödendiğinden tekrar
   koşusunun geliri neredeyse tamamen bu çarpandan geliyor — örn. bölüm 5
   tekrarı 51 → 74 coin (+%45). Zaten en az ödenen etkinlik tekrar oynamaktı.
2. `TIER_REWARDS` **120/260/520 → 80/140/280** (`DailyChallengeGenerator.kt`),
   yani günlük görev tavanı 900 → 500. `docs/BALANCE.md` günlük görevi zaten
   "400–500 coin" diye belgeliyordu; kod bir noktada 900'e çıkmış, belge
   güncellenmemişti. Ölçülen 100 coin/bölüm ile kıyas: 900'lük günlük dokuz
   bölümlük ilerlemeye bedeldi, 500'de bu beşe iniyor.

**Göç riski yok.** İkisi de kayıtlı veriye dokunmuyor: `DAILY_TIER` anahtarı
kademe *sayısını* saklıyor, coin miktarını değil — ödül her seferinde diziden
yeniden hesaplanıyor. `SCORE_PER_BONUS_COIN` zaten koşu sonunda uygulanıyor.

**Yapılmadı:** öneri 3 (`XP_PER_CAR_LEVEL` 500 → 1250) sahibi tarafından
kapsam dışı bırakıldı — tek seferlik XP migrasyonu gerektiriyor.

**Kanıt.** 177 birim test / 0 hata. `assembleDebug` + `assembleRelease`
başarılı.

**Ölçülmedi:** gerçek oyuncu davranışı. Yukarıdaki tablo motorun deterministik
simülasyonundan geliyor, gerçek oyuncudan değil; "temkinli" ve "riskli" iki
yapay oynayış profili. Gelir etkisi ancak yayından sonra görülür.

## 2026-08-16 (2) — Geçiş reklamı sayacı kaçağı kapatıldı

**Kusur:** geçiş reklamı sayacı yalnızca bölüm **tamamlandığında** artıyordu
(`KronViewModel.onRunFinished`, artış `stats.completed` bloğunun içindeydi).
Sonuç: "çarp → ana menü → bölümü tekrar seç" döngüsü **sınırsız ve
reklamsız**dı. Bu döngü oyunun en çok yürünen yolu — bölümü tutturana kadar
tekrar denemek — yani reklam gelirinin büyük kısmı sessizce kaybediliyordu.
`INTERSTITIAL_EVERY_N_RETRIES` sabiti tam da bunun için duruyordu ama
14 Ağustos'ta "TEKRAR" butonu kaldırılınca **hiçbir yerden çağrılmıyordu**.

**Düzeltme üç parçalı** (proje sahibi onayı ile; tek başına 1. madde
uygulansaydı en çok zorlanan oyuncu en çok reklam gören kesime dönüşürdü):

1. Sayaç **10 saniyeyi geçen her kariyer koşusunda** artar — başarı şartı yok.
   Yeni sabit `INTERSTITIAL_MIN_RUN_SECONDS = 10`; eşiğin sebebi yanlış bölüme
   girip hemen çıkmanın reklam cezasına dönüşmemesi.
2. Sıklık `INTERSTITIAL_EVERY_N_LEVELS` **2 → 3**. Kaçak kapanınca aynı eşik
   gerçekte çok daha sık reklam demek olurdu.
3. **İlk 4 bölüm tamamen reklamsız** — yeni sabit `INTERSTITIAL_FREE_LEVELS = 4`.

Karar mantığı `game/AdFrequency.kt` içinde **saf Kotlin**e alındı
(`countsTowardInterstitial` / `shouldShow`); `KronViewModel` yalnızca çağırıyor.
Sebep: eski hâlinde karar Android'e bağlı ViewModel'in içindeydi ve JVM
testiyle doğrulanamıyordu — kaçağın 2 gün fark edilmemesinin sebebi de bu.
`shouldShowInterstitial` artık `levelId` alıyor; `GameScreen`'deki üç çağrı
güncellendi (sonuç ekranında **biten** bölümün numarası geçiliyor, bir
sonrakinin değil).

Günlük görev koşusu sayacı artırmaz (günde bir kez oynanır) ama sayaç
dolduysa çıkışta reklam gösterebilir — eski davranış korundu.

**Kanıt.** 177 birim test / 0 hata (164 + 13 yeni `AdFrequencyTest`).
`assembleDebug` + `assembleRelease` başarılı, release APK 4.27 MB.
Cihazda (SM-G950F, DataStore dosyası `run-as` ile doğrudan okunarak):

| Koşu | Süre | Sonuç | `levels_since_interstitial` |
|---|---|---|---|
| Bölüm 1 | 00:05 | çarptı | 0 → **0** (eşiğin altı, doğru) |
| Bölüm 1 | ~00:20 | çarptı, 2/3 görev | 0 → **1** (eskiden 0 kalırdı) |

Bölüm 1 muafiyet bölgesinde olduğu için reklam çıkmadı (logcat'te tek satır
`interstitial` yok) — beklenen davranış.

**Ölçülmedi:** gerçek gelir etkisi. 2 → 3 sıklığı ve 4 bölümlük muafiyet,
kaçağın kapanmasıyla gelen artışı ne kadar dengeliyor bilinmiyor; bu ancak
yayından sonraki AdMob verisiyle görülür.

## 2026-08-16 — Yol deseni cihazda düzeltildi + kontrol tuşu ikonları

Günün tamamı **cihazda görülen** kusurların düzeltilmesiydi (SM-G950F,
Android 7.0 / API 24). Beşi de baseline'dan beri vardı; hiçbiri dün gelen
sprite/fabrika boyası işinin yan etkisi değil.

**Şerit çizgileri artık gerçekten kesikli.** Kesikler tek bir `drawLine` +
`PathEffect.dashPathEffect` ile çiziliyordu ve bu mekanizma bu cihazda
**sessizce yok sayılıyor** — hata yok, çizgi var, ama düz. Mekanizma
kaldırıldı; kesikler kerb blokları gibi ayrık `drawRect` çağrılarıyla
çiziliyor. Aynısı `LanguageGateScreen`'e de uygulandı. `LANE_DASH_ON_PX` /
`LANE_DASH_OFF_PX` sabitleri değişmedi.
Ölçüm (cihaz): **126 px dolu / 162 px boş = ayarlı 42 / 54 dp.**

> **Açık kalan soru — dürüstlük notu.** Sahibi "önceden kesikliydi, yeni
> bozuldu" dedi ve 16:04'teki bir APK'dan kesikli ekran görüntüsü gönderdi.
> Ama commit geçmişinde mekanizma baseline'dan beri **değişmemiş** ve
> `builds/` altındaki eski arşiv APK'sı **aynı cihazda düz çiziyor**. O 16:04
> APK'sı commit edilmemiş bir ağaçtan derlendiği için diff'i yok. "Hangi
> değişiklik bozdu" sorusu **cevapsız** kaldı; bir commit suçlanmıyor.

**Yol kenarı desenleri yanlış yöne akıyordu.** Çimen çizgileri, plaj köpüğü,
seyirci bayrakları, gece çizgileri ve gece şehir ışıkları **yukarı**, şerit
çizgileri ve asfalt dokusu **aşağı** gidiyordu. Beşinin de işareti çevrildi.
Ölçüm (cihaz, kare farkı çapraz-korelasyonu): çimen **+16 px/kare aşağı**,
şerit **+57 px/kare aşağı** — hız farkı kasıtlı, derinlik ondan geliyor.

**Kerb iki ayrı sebepten yanlıştı.** (a) Bloklar **sabit ızgaraya**
çiziliyordu; `roadOffset` konumu değil yalnızca **rengi** çeviriyordu — kerb
kaymıyor, yerinde faz atlıyordu. Ölçüm: kırmızı/beyaz sınırları kare kare
aynı y'de (**+0 px**), düzeltmeden sonra **+38 px aşağı**. (b) Hız çarpanı
1/12 idi, **1.00** yapıldı. Sahibi: *"kerbler aslında sabit, araba yanından
geçiyor; o hissi vermek için hareket ettiriyorsun. Kerb hızı araba ile aynı
olmalı ki araba hızlandıkça hızlansın, yavaşladıkça yavaşlasın — kerb sabit
kalırsa bu his kaybolur."* İlke: paralaks **derinlikten** doğar, yanal
mesafeden değil — kerb yola boyalıdır, kameradan şerit çizgisiyle aynı
uzaklıktadır.

Hız 12 katına çıkınca titreşim de 12 katına çıktığı için blok boyu
**46 → 50 px** (`GameConfig.KERB_BLOCK_HEIGHT_PX`): en yüksek hızda
(~1650 px/s) geçiş oranı 16.5 Hz, üst sınır olarak şerit çizgilerinin aynı
koşuldaki 17.2 Hz'i alındı — o desen 1.00 çarpanda zaten çalışıyor ve sahibi
görünümünü onayladı. Periyot (100) şerit periyodundan (96) **bilerek** farklı;
eşitlenirse yol düzleminin tamamı aynı anda "tık" yapar.

> Sıra önemliydi: (a) düzeltilmeden (b) yapılsaydı iş **kötüleşirdi** — kayan
> bordür değil, saniyede birkaç kez topluca yanıp sönen bordür olurdu.

**Seyirci renk titremesi.** Renk kayan **ekran** y'sine bağlıydı, **dünya**
y'sine bağlandı; negatifte bütün kalabalığı siyaha düşüren `%` yerine `mod`
kullanıldı. Kalıntı titreme için ikinci düzeltme: indeks anahtarı
`x + rowKey / 6f` **daima tam sayıya oturuyor** (rowKey 42'nin, x 3'ün katı),
`floor()` bıçak sırtında kalıyor ve epsilon indeksi kaydırıyordu →
`roundToInt()`. Cihazda **ölçüldü**: tema geçici olarak CROWD'a sabitlenip
üç ardışık kare karşılaştırıldı — üst bantta %0.3, alt bantta %0.0 renk
değişimi (düzeltmeden önce alt bantta ~%20). Tema sabiti ölçümden sonra
geri alındı.

**Kontrol tuşları: emoji gitti, çizim geldi.** `◀ ▶ ⚡ 📣` glifleri kaldırıldı;
yerine Compose Canvas ikonları — dolu yuvarlatılmış üçgen oklar, **sarı**
şimşek (`KronColors.AccentBright`), ampullü korna. Gerekçe: emoji her Android
sürümünde farklı yazı tipinden çiziliyor (cihaz API 24), kendi renkleriyle
geliyor ve butonların düz beyaz diliyle uyuşmuyor. Sahibi referans görsel
gönderdi; görseller stok/clipart olduğu için **gömülmedi** — bakılıp aynı biçim
kendimiz çizildi. **Korna sağ sütuna, yön okunun üstüne alındı** (sahibi:
*"zaten bir fonksiyonu yok"*); dünkü "alt ortada 48 dp" yerleşimi geçersiz.

**Boğa 67'nin fabrika boyası siyah** (`COLOR_MIDNIGHT`, sahibi kararı). Fabrika
boyası kuralı aynı: gövdeye sahip olmak boyayı da açıyor, boya gövdeye kilitli
değil. En koyu gece zemininde araç kayboluyor mu diye bakıldı — kaybolmuyor,
beyaz çift şerit ve camlar siluet taşıyor (**gözle denetim**, ölçülmüş kontrast
değeri değil).

**Dünkü performans iddiası geri çekildi.** Ayrıntı aşağıda ve `PROVENANCE.md`
#16'nın düzeltme kutusunda.

Kanıt (bu belgeye yazılan, cihazda ölçülmüş değerler): şerit 126/162 px;
çimen +16 px/kare, şerit +57 px/kare; kerb +0 → +38 px/kare. Ölçümlerin hepsi
SM-G950F üzerinde. Derleme/test çıktısı bu belgeyi yazan tarafça görülmedi —
build kanıtı uygulayan ajanların görev raporundadır ve orada aranmalıdır.
Ayrıntı: `PROVENANCE.md` sapma #18–#23.

## 2026-08-15 (akşam) — Araç sprite'ları, fabrika boyası, kontrol tuşu düzeltmesi

**Araçlar artık sprite.** `incoming/car_refs/` altındaki referans çizimler
oyuna alındı. Her gövde iki katman: gri tonlamalı boyanabilir gövde +
renkli detay; gövde çalışma anında seçili boyayla çarpılıyor, böylece tek
dosyadan 10 boya çıkıyor. Üretici: `tools/build_car_sprites.py`. 8 gövde
(7 oynanabilir + trafik), 552 KB. Vektör çizim geri düşüş olarak duruyor.
Çarpışma kutusu değişmedi. Ayrıntı: `PROVENANCE.md` #16.

**Fabrika boyası.** Kuş SLX petrol yeşili, Dağ Keçisi beyaz olarak geliyor.
Gövdeye sahip olmak fabrika boyasını da açıyor; boya araç başına
hatırlanıyor (`car_color_by_shape`). Garaj çipleri her aracı kendi renginde
gösteriyor. Ayrıntı: `PROVENANCE.md` #17.

**Cihazda bulunan hata — kontrol tuşlarındaki "gölge çerçeve".** Sahibi yön
tuşlarının arkasında, özellikle gece temasında kötü duran yuvarlak-kare bir
leke bildirdi. Gölge sanıldı; `CONTROL_ELEVATION` 0 yapılıp ölçülünce leke
DURDU, yani sebep gölge değildi. Gerçek sebep: üstteki cam parlaması kutusu
butonun yalnızca üst %45'ini kaplıyor (64×28.8 dp) ve kendini `CircleShape`
ile kırpıyordu — `CircleShape` = %50 köşe yarıçapı, kare olmayan bir kutuda
**daire değil hap** üretir ve hapın köşeleri dairenin omuzlarından taşıyordu.
Parlama kutusu artık butonun tamamını kaplıyor (kare → kırpma gerçek daire),
sönümlemeyi gradyan durakları yapıyor.

**Mağaza görselleri.** İkon ve feature graphic sprite'larla yenilendi
(`tools/sprite_car.py`); ikonda Süper Araba (sahibi seçti). `gen_feature.py`
içindeki bir öz-denetim düzeltildi: bütün araçların farklı açıda olmasını
bekliyordu, oysa açı yalnızca şeridin fonksiyonu — aynı şeritte iki araç
olduğu için denetim her zaman patlıyordu.

**Performans.** Sprite geçişi ölçüldü (S8): kare süresi medyanı 24–27 ms,
vektörde 25 ms — regresyon yok. Sprite'lar süreç boyunca tek kopya tutuluyor.

> **Düzeltme (2026-08-16): yukarıdaki "regresyon yok" iddiası geri çekildi.**
> Tema her koşuda **rastgele** seçiliyor (`GameEngine.kt:100`) ve temalar
> arasında kare başına çizim çağrısı ~185 ile ~780 arasında (**4 kat**)
> değişiyor. O A/B ölçümü temayı sabitlemedi, dolayısıyla 24–27 ms ile 25 ms
> farklı temaların karşılaştırması da olabilir — sayılar bir şey kanıtlamıyor.
> Sprite kararı **mimari gerekçeyle** duruyor (hazır bitmap kompozitlemek,
> karede onlarca poligon + gradyan kurmaktan az iş), ama **elde temiz bir
> ölçüm yok**. "Tek kopya" gözlemi tema seçiminden bağımsız, o geçerli.
> **Bundan sonraki her performans ölçümünde tema sabitlenmelidir.**

## 2026-08-15 — Araca özel motor sesi + korna

Sahibinin isteği: *"Araba seslerini arabaya göre yapabilir miyiz? Mesela Boğa
67 daha böyle egzozu gürültülü olur. Bir tane de korna efekti koyalım, işe
yaramasa da eğlencelik olur."*

**Oynanış değişmedi.** Ses profili hız, ivme, fren, boost ve çarpışma
kutusuna dokunmuyor; korna tamamen dekoratif. Yeni ses dosyası **yok**, her
şey çalışma anında sentezleniyor (APK'ya eklenen ses baytı: 0).

**Yedi gövde, yedi ses.** Profil tablosu `audio/CarSoundProfile.kt`:

| Gövde | Temel frek. | Karakter |
|---|---|---|
| Şehir | ×1.00 | referans — **eski sesle bit bit aynı** |
| Yarış Sedan | ×1.16 | tiz ve çevik, en parlak filtreye yakın, lope yok denecek kadar az |
| Kuş SLX | ×0.94 | mütevazı, boğuk filtre (0.84), yavaş ve derin titreme (lope 0.26 / 0.34) |
| Dağ Keçisi | ×0.80 | tok dizel: **katalogun en kapalı filtresi** (0.68), en yüksek gürültü dokusu (0.10) |
| Kas Arabası | ×0.86 | derin ve gürültülü, genlik 1.12 |
| **Boğa 67** | **×0.76** | **en gürültülü egzoz**: tek sayılı harmonikler baskın (h3 0.64 / h5 0.36), en derin+en yavaş lope (0.34 / 0.25), en yüksek genlik (1.22) |
| Süper Araba | ×1.30 | yüksek devirli, ince ve keskin, en açık filtre (1.48) |

Nitro da profile göre renkleniyor (ıslık ve gürültü süzülmesi `nitroTone` ile
ölçekleniyor). Korna her araçta farklı: Boğa 67 250 Hz (en kalın), Süper Araba
640 Hz (en ince).

**Mimari.** Sentez `game/` paketine **girmedi** ve `CarCatalog.kt`'ye
dokunulmadı: profiller `audio/` altında, gövde **id**'siyle eşleşiyor,
bilinmeyen id varsayılana düşüyor. Sentezin tamamı yeni `audio/EngineVoice.kt`
içinde ve **saf Kotlin** — `EngineSoundManager` yalnızca `AudioTrack` köprüsü.
Bu ayrım sesi JVM testiyle doğrulanabilir kıldı (bu makinede hoparlör yok).

**Korna düğmesi: oyun ekranının alt ortasında**, 48 dp, fren/boost ile aynı
hizada. Üstteki HUD satırına konmadı (sürerken başparmak ekranın tepesine
uzanamaz), kontrol kümelerinin arasına da konmadı (o boşluk üç kez oyuncu
geri bildirimiyle daraltılmıştı). Ses kapalıyken düğme hiç görünmez. Spam
koruması 0.4 sn; bekleme dolmadan basılırsa ses de titreşim de olmaz.

Tır geldiğinde tabloya tek satır eklemek yetecek (hava kornası için
`hornBaseHz` ~110 Hz, `hornSeconds` ~1.1, yavaş atak) — yapı hazır.

Değişen/eklenen dosyalar: `audio/CarSoundProfile.kt` (yeni),
`audio/EngineVoice.kt` (yeni), `audio/EngineSoundManager.kt` (ince Android
köprüsüne indirgendi), `ui/game/GameScreen.kt` (profil seçimi + korna düğmesi),
`test/…/audio/CarSoundProfilesTest.kt` + `test/…/audio/EngineVoiceTest.kt`
(yeni, 13 test), `PROVENANCE.md` (sapma #15 + "Ses varlıkları" bölümü
güncellendi). `game/`, `ui/levels/`, `ui/garage/`, `data/` ve mağaza
dosyalarına dokunulmadı; `SettingsScreen.kt` de değişmedi (mevcut ses anahtarı
yeterliydi).

Kanıt: **160 birim testi geçti** (0 hata, 13'ü yeni),
`:app:assembleDebug` BUILD SUCCESSFUL, `:app:assembleRelease` BUILD SUCCESSFUL
(`app/build/outputs/apk/release/app-release.apk`, 3.88 MB). Ölçülen ses
özellikleri: her profilde en ağır yükte (tam gaz + boost + nitro + korna aynı
anda) tepe seviye < 0.9, kırpma yok; ses kapalıyken çıkış tam sessiz (0.0).
**Cihazda dinlenmedi** — bu makinede hoparlör ve adb yok, sesin kulağa nasıl
geldiği proje sahibinin doğrulamasına bırakıldı.

## 2026-08-15 — Araçlara gerçek özellik: dört sürüş çarpanı + garajda gösterim

Sahibinin tespiti: *"Bu arabaların özellikleri görünmüyor garajda, yani neden
süper araba alsın?"* Araçlar bugüne kadar tamamen kozmetikti; 3200 coinlik
araç hiçbir şey yapmıyordu ve bu garajda yazmıyordu bile.

**Katalog.** Her `CarShapeDef` dört çarpan taşıyor (varsayılanı 1.0): son hız,
ivme, fren, boost süresi. Değerler `docs/BALANCE.md` tablosundan birebir;
Boğa 67'nin profili (tabloda yoktu) kas arabası ailesinden türetildi.

| Araç | Fiyat | Son hız | İvme | Fren | Boost |
|---|---|---|---|---|---|
| Şehir | 0 | 1.00 | 1.00 | 1.00 | 1.00 |
| Yarış Sedan | 900 | 1.04 | 1.08 | 1.00 | 1.00 |
| Kuş SLX | 1500 | 0.97 | 1.00 | 1.06 | 1.12 |
| Dağ Keçisi | 1500 | 1.00 | 0.94 | 1.12 | 1.06 |
| Kas Arabası | 1800 | 1.08 | 0.96 | 0.96 | 1.00 |
| Boğa 67 | 2400 | 1.10 | 0.92 | 0.90 | 1.04 |
| Süper Araba | 3200 | 1.12 | 1.10 | 0.94 | 1.00 |

Her araca ayrıca tek satırlık **karakter cümlesi** eklendi (TR + EN).

**Motor.** Çarpanlar yükseltmeden gelen değerin **üstüne** uygulanıyor, yerine
geçmiyor: `scoreSpeedCap` × son hız, `accelRate` × ivme, `brakePenalty` × fren,
`boostDrain` ÷ boost. `decelRate` bilerek ölçeklenmedi (o oran frenin değil her
aşağı yönlü yakınsamanın oranı; gerekçe `docs/BALANCE.md`). `GameConfig`
sabitleri ve **çarpışma kutusu değişmedi** — yedi gövde için test var.

**Garaj.** Araç kartında dört çubuk (HIZ / İVME / FREN / BOOST) + karakter
cümlesi + referansa göre yüzde. Etiketler `UpgradeCatalog.title` üzerinden,
yani yükseltme bölümüyle aynı görsel dil. Çubuklar mutlak değil **katalog
içinde karşılaştırmalı** (en iyi dolu, en kötü kısa). Kilitli araçta da
görünüyor.

Değişen dosyalar: `game/CarCatalog.kt`, `game/UpgradeCatalog.kt`,
`game/GameEngine.kt`, `ui/garage/GarageScreen.kt`,
`test/…/CarCatalogTest.kt` (+9 test), `test/…/UpgradeCatalogTest.kt` (+3),
`test/…/GameEngineTest.kt` (+5), `docs/BALANCE.md`, `PROVENANCE.md` (sapma
#14 + #6/#12/#13 güncelleme notları).

Kanıt: **147 birim testi geçti, 0 hata** (eskiden 130; hiçbiri kırılmadı),
`:app:assembleDebug` BUILD SUCCESSFUL, `:app:assembleRelease` BUILD SUCCESSFUL.
Bu makinede cihaz doğrulaması yapılmadı.

**Açık karar (sahibi):** Yarış Sedan tabloda dört eksende de zayıf değil.
Bilinçli bırakıldı (900 coinlik ilk satın alma tereddütsüz iyi hissettirmeli)
ve test bu istisnayı tek araçla sınırlıyor. İstenirse boost süresine −4%
verilip istisna kaldırılabilir.

## 2026-08-15 — Garaja yedinci gövde: ikinci kas arabası (geçici ad "Boğa 67")

Sahibinin isteği: 60'lar/70'ler Amerikan kas arabası havasında bir gövde.
**Saf kozmetik** — çizim kutusu (`x -20..20`, `y -2..74`), hitbox,
`CAR_ART_SCALE` / `HITBOX_SCALE` ve fizik değişmedi.

**Marka sınırı.** Hiçbir tescilli marka/model adı kodda ya da mağaza metninde
geçmiyor; gövde birebir kopya değil, kullanılan şey dönemin **biçim dili**.
Kod içi kimlik nötr: `SHAPE_MUSCLE_67`. Ad geçici — sahibi üç aday arasından
seçecek: **Boğa 67** / **Yıldırım GT** / **Demirtay**.

**Mevcut Kas Arabası'ndan siluetle ayrımı** (bu gövdenin varlık sebebi):
tek **kalın** orta şerit (9.4 birim; Kas Arabası'nda iki ince yan şerit, 3.6),
kola şişesi bel (33.2 → 29.6 → 33.8; Kas Arabası baştan sona düz 33.2), kokpit
arkada (kaput/bagaj 28/15.1; Kas Arabası 24.4/21.4), fastback arka cam (8.6 vs
6.2), dörtlü yuvarlak far (Kas Arabası'nda far yok, Kuş SLX'te iki dikdörtgen),
köşelere itilmiş stop lambaları + ortada iki krom egzoz ucu.

**Fiyat 2400 / seviye 5.** Merdiven uzamadı; mevcut merdivenin en geniş
boşluğuna girdi (1800 → 3200 arası 1400 coinlik uçurumdu):
0 → 900 → 1500 → 1500 → 1800 → **2400** → 3200.

**Renk sorusu cevaplandı.** Gövde ve boya bağımsız saklanıyor (`carShapeId` /
`carColorId`), envanterleri ayrı, `CarStyle` serbest çarpım. Gövdeye özel renk
kısıtı **yok**: her gövde her boyayla kullanılabilir. Yeni test bunu 7 × 10 =
70 çift üzerinde doğruluyor.

Değişen dosyalar: `game/CarCatalog.kt` (yalnızca veri — `CarArtwork.kt` ve
`GameRenderer` dosyalarına dokunulmadı), `test/…/CarCatalogTest.kt` (2 yeni
test + fiyat merdiveni güncellendi), `docs/play_store_assets/tools/kron_art.py`
(Python aynası senkron), `tools/car_lineup.py` (yedi gövde + iki kas arabası
karşılaştırma bloğu), `PROVENANCE.md` (sapma #13).

Kanıt: `docs/play_store_assets/previews/car_lineup_7.png` (yedi gövde, 32 px
oyun ölçeği + 4×, ayrıca iki kas arabası yan yana), **130 birim testi geçti**
(0 hata, 2'si yeni), `assembleDebug` BUILD SUCCESSFUL. Bu makinede cihaz
doğrulaması yapılmadı.

## 2026-08-15 — Garaja iki yeni gövde: Kuş SLX ve Dağ Keçisi

Sahibinin isteği: garaja bir 80'ler Türk sedanı ve bir station wagon. **İkisi
de saf kozmetik** — çizim kutusu, hitbox, ölçek sabitleri ve fizik değişmedi;
hiçbir gövde oyunda avantaj ya da dezavantaj getirmiyor.

**Kuş SLX** (450 coin, seviye 1). Kimliği üç işaretten okunuyor: köşe yarıçapı
1.6 (garajın en kare gövdesi; Kas Arabası 2.5, diğerleri 5–6), kısa/dik ön cam
(8.2 birim) + uzun düz tavan (12 birim), ve krom — iki yanda tam boy ince şerit,
ızgarada yatay çubuk, burnun iki ucunda dikdörtgen farlar. Krom yan şerit ve far
başka hiçbir gövdede (trafik dahil) yok; 32 px'de ayırt eden şey bu.

**Dağ Keçisi** (2500 coin, seviye 5). SW kimliği: 22 birimlik uzun tavan
(diğerleri ~10–12), tavan bagaj rayları + çapraz çubuklar, tavanın yanında arka
yan camlar, geniş bagaj kapağı camı. Beyaz boyayla tasarlandı.

**Yeni boya — Buzul Beyazı** (500 coin, seviye 1). Trafikteki beyaz engel
**tam beyaz** (`FFFFFF`) olduğu için oyuncu beyazı bilerek kırık ve soğuk
(`EDF1F5`), gölgesi belirgin gri, şeridi koyu lacivert. Karışma riski
karşılaştırma görselinde ayrı bir blokta gözle denetlendi.

Fiyat merdiveni uzamadı, iki yeni gövde mevcut basamakların **arasına** girdi:
0 → **450** → 900 → 1800 → **2500** → 3200.

Değişen dosyalar: `game/CarCatalog.kt` (yalnızca veri — çizici `CarArtwork.kt`
ve `GameRenderer` dosyalarına dokunulmadı), `test/…/CarCatalogTest.kt` (5 yeni
test), `docs/play_store_assets/tools/kron_art.py` (Python aynası senkron),
`tools/car_lineup.py` (yeni).

Kanıt: `docs/play_store_assets/previews/car_lineup_6.png` (altı gövde yan yana,
32 px oyun ölçeği + 4×), 128 birim testi geçti (5'i yeni), `assembleDebug`
BUILD SUCCESSFUL. Bu makinede adb/emülatör yok — cihazda doğrulama yapılmadı.

## 2026-08-15 — Bölüm haritası bir yarış pistine dönüştü

Sahibinin isteği: *"Bölüm haritasını kerbli kavisli bir pist gibi yapıp
üzerinde bir araba olsa, levelleri geçtikçe hareket eden, ne dersin?"*

Eski 4'lü kart ızgarası kaldırıldı. Kariyer ekranı artık dikey akan, kerbli,
kıvrımlı tek bir pist; 30 bölüm bu pistin üzerinde sırayla dizili **duraklar**.
Oyuncunun garajda seçtiği araç pistin üzerinde, bulunduğu durağın hemen
gerisinde park ediyor; yeni bölüm açıldığında bir sonraki durağa **kayarak**
gidiyor (pistin eğrisini ve eğimini izleyerek, 1.1 sn).

**Geometri tek bir sinüsten türüyor** (`ui/levels/TrackLayout.kt`, saf Kotlin):
`centerX(y) = width/2 + amplitude · sin(π·y / SEGMENT_HEIGHT)`. Yarım dalga
boyu tam olarak bir segment olduğu için her durak sinüsün bir tepesine düşüyor;
duraklar sırayla sağ-sol diziliyor ve iki durak arası hiçbir zaman 140 dp'nin
altına inmiyor (dokunma hedefi 56 dp). Genlik iki sınırın küçüğü: eğim sınırı
(dikeyden en fazla 45°) ve taşma sınırı (yol + kerb + kenar boşluğu ekrana
sığmalı) — 320 dp'de de pist ekrandan taşmıyor.

**Performans.** Pist `LazyColumn` ile sanallaştırıldı (30 segmentten yalnızca
görünen 4-5'i bestelenir). Sinüsün işareti her segmentte döndüğü için pistte
yalnızca **iki farklı segment şekli** var; asfalt/kerb/şerit yolları
`buildTrackSegmentArt` ile bir kez kurulup `remember` ile paylaşılıyor. Aracın
konumu ve açısı `offset {}` / `graphicsLayer {}` lambdalarında okunuyor, yani
kaydırma ve ilerleme animasyonu yeniden besteleme yapmıyor.

**Yıldız dili kaldırıldı** (sahibi kararı): haritada da, bölüm kartında da
★ yok. Tamamlanan bölümde damalı bayrak + yeşil tikli görev yuvarlakları
(`ObjectiveDots`), oynanabilir bölüm sarı vurgulu, kilitli bölümler soluk.

Harita açıldığında oyuncunun **kendi bölümü** görünür oluyor (otomatik
kaydırma). Değişmeyenler: bölüme dokununca oyun başlıyor, güçlendirici seçimi
bölüm kartında, geri tuşu, banner reklam, TR/EN metinler.

Kanıt: `docs/play_store_assets/previews/level_map_mock.png` (1080×2400,
Compose kodunun geometrisiyle birebir aynı hesaptan üretildi —
`tools/level_map_mock.py`), 121 birim testi geçti (11'i yeni:
`TrackLayoutTest`), `assembleDebug` BUILD SUCCESSFUL.

## 2026-08-15 — Araç çizimleri yeniden çizildi (perspektif + hacim)

Sahibinin üç maddelik eleştirisi: *"üstten bakıyoruz araçlara, stop lambası ve
tampon o kadar tavan ile aynı paralelde olmamalı"*, *"boost ışıklandırması çok
yapay, lego gibi"*, *"oyundaki [araba] çok ama çok kötü"*.

**Perspektif.** Kamera aracın üstünde ve biraz arkasında; arka yüz tavana dik
durduğu için bize doğru kısalarak yansımalı. Eski çizimde arka tampon 8
birimlik düz renkli bir slabdı, stop lambaları da onun üstünde ayrı kutulardı —
ikisi de tavan düzleminde okunuyordu. Yeni `CarCatalog.rearFace()`: gövde
boyunun ~%7'si kadar, aşağı doğru kararan, hafif daralan trapez + tavanla
arasında ince kırılma çizgisi + üzerinde ince stop şeritleri.

**Hacim.** Gövde genişletildi, tekerlekler çamurlukların altına alındı, tavan
omuzlardan içerde ayrı bir düzlem oldu, ön cam / tavan / arka cam ayrıldı.
`CarPart` artık isteğe bağlı `gradient` + `alpha` taşıyor; `CarGradient` mutlak
renk değil **ton kaydırması** tuttuğu için aynı hacim tanımı 9 boyanın ve 4
trafik renginin hepsinde çalışıyor. Şekle özel Compose kodu hâlâ yok — çizici
`CarArtwork.kt` içinde tek yerde ve fırçaları önbelleğe alıyor (60 Hz'de
karede shader üretilmesin diye).

**Trafik** artık oyuncuyla aynı boru hattını kullanıyor
(`CarCatalog.trafficShape`); `GameRenderer` içindeki elle yazılmış kopya
kaldırıldı. Ayırt edilebilirlik iki kanaldan korunuyor: palet (gövde
`OBSTACLE_COLORS`, sürücü mavi) ve siluet (trafik bilerek daha köşeli).

**Boost alevi** dört katman oldu: halo + dış/iç/çekirdek plumalar; her pluma
ucuna doğru hem daralıyor hem saydamlaşıyor, iki farklı frekansta titriyor.

**Mağaza araçları.** `kron_art.py` (CarCatalog'un Python aynası) senkron
tutuldu ve 4 gövde + trafik şeklinin tamamını taşıyor. `kron_car3d.py` aynı
perspektif düzeltmesini aldı; ayrıca sahibin isteğiyle **asfalta düşen zemin
gölgesi kaldırıldı** (hale araçtan büyüktü, yakınlaştırınca leke gibiydi).

Değişmeyenler: `CAR_ART_SCALE`, `CAR_WIDTH_PX`, `CAR_HEIGHT_PX`,
`HITBOX_SCALE`, çizim kutusu (`x -20..20`, `y -2..74`) ve çarpışma davranışı.

Kanıt: `docs/play_store_assets/previews/car_before_after.png` (her gövde, oyun
ölçeği + 4x), 110 birim testi geçti, `assembleDebug` BUILD SUCCESSFUL.
Ayrıntı: `PROVENANCE.md` sapma #11.

### Uygulama ikonu yeni araçla yenilendi

Sahibi: *"Ayrıca app icon'daki resmi de düzenle."* Seçilen kompozisyon (aday C
— perspektif yol + kerb + far konisi + kırmızı araç) **aynı kaldı**; değişen
yalnızca içindeki araç: eski düz sprite'ın Pillow kopyası yerine yeni hacimli
çizim. `gen_launcher.py` tek koşuda hepsini üretiyor:

- `mipmap-{mdpi…xxxhdpi}/ic_launcher.png` + `ic_launcher_round.png` (48–192 px)
- adaptive: `drawable/ic_launcher_background.xml` (zemin) +
  `ic_launcher_foreground.png` (araç + far konisi, gövde 66 dp güvenli
  dairenin içinde — maske tekerlekleri kesmiyor, kanıt
  `icon_adaptive_layers.png` panel 3)
- `drawable-nodpi/app_icon.png` (192 px) — **ilk açılıştaki dil ekranı** bunu
  kullanıyor (`painterResource` adaptive XML'i yükleyemiyor), ikon değişince
  bu dosya da tazelenmeli
- `docs/play_store_assets/{icon_512_c,play-store-icon-512}.png`, eskiler
  `_old.png` olarak yedeklendi

Yan düzeltmeler: (a) `gen_icons.player_car` içindeki "egzoz ışığı" halesi
kaldırıldı — eski kopuk üçgen alevi gövdeye bağlamak için eklenmişti, yeni
alevin kendi halosuyla üst üste binince asfaltta büyük mavi bir leke
oluşuyordu; (b) coin konumu tek yere alındı (`gen_icons.C_COIN_*`) ve araç
genişleyince altında kaldığı için sol öne taşındı — raster zemin ile vector
arka plan artık aynı yerden okuyor, düz ikon ile launcher ikonu kaymıyor;
(c) `write_store_icon` içindeki `get_flattened_data()` çağrısı (Pillow'da
yok, üretim bu yüzden yarım kalmıştı) `getdata()` ile düzeltildi.

## 2026-08-15 — Trafik gerçekten sürüyor + öğrenme eğrisi ikinci tur

**Yoldaki araçlar park etmiş gibi duruyordu.** Sahibi: *"Yoldaki arabalar
hareket etmiyor, park etmiş gibiler. Sabit bir hızla gidiyor olmalılar."*
Kök neden doğrulandı: engel `speed × K × dt × speedMul` (speedMul 1.00–1.14)
ile akıyor, asfalt ise tam `speed × K × dt` kayıyordu — yani engeller
**asfaltla aynı hızda, hatta biraz daha hızlı** geriye gidiyordu; asfalta
göre duruyor ya da geri geri gidiyorlardı.

Artık her aracın kendi ileri hızı var: ekrandaki aşağı hız
`(oyuncuHızı − aracHızı) × K`, araç hızı ise **koşunun taban hızının**
0.45–0.58 katı (doğumda sabitlenir). Zemin yine `oyuncuHızı` ile kaydığı
için araçlar asfalta göre ileri gider. Trafik hızı oyuncunun **anlık**
hızına bağlı olmadığı için boost artık gerçekten yaklaştırıyor: yaklaşma
hızındaki artış %69 → %141.

Değişmeyenler (bilerek): `WORLD_SPEED_SCALE`, `WORLD_PX_PER_SPEED_UNIT`,
`HITBOX_SCALE`, çarpışma kutusu ve **spawn aralığı** — doğma zamana bağlı
olduğu için saniyedeki geçilen araç ve araçlar arası tepki süresi (0.78 s)
aynen korunuyor. Türetme `GameConfig.OBSTACLE_SPAWN_INTERVAL_SEC` yorumunda.

Kenar durum: fren yapıp trafiğin altına düşülürse araçlar yukarı uzaklaşır;
ekranın **üstünden** çıkanlar artık temizleniyor. `evaluated` bayrağı
korunduğu için aynı araç iki kez puan/dodge vermiyor.

Çizim: trafik aracına **arka stop lambaları** eklendi — aracın arkasının bize
dönük olduğu okunmadan "önümde giden araç" hissi tamamlanmıyordu. Gövde,
boyut ve çarpışma kutusu aynı.

**İlk bölümler hâlâ fazla zordu (ikinci geri bildirim).** İlk turda yalnızca
başlangıç hızı düşürülmüştü; zorluğun asıl ekseni olan saniyedeki araç sayısı
hiç değişmemişti — 1. bölüm de 30. bölüm de 0.78 s'de bir araç doğuruyordu.
`LevelDef.trafficDensity` eklendi (varsayılan 1.0, 9–30. bölümler etkilenmedi)
ve ilk 8 bölüm yeniden tasarlandı: 1 şerit değiştirme (yoğunluk 0.30,
neredeyse boş yol) → 2 trafik → 3 boost → 4 perfect dodge → 5 ilk tam
yoğunluk → 6 combo (nefes bölümü) → 7 baskı altında combo → 8 mesafe.

Bulunan iki somut hata: eski 2. bölümün `ScoreAtLeast(2200)` hedefi gerçek
skor eğrisinin **%92'sine** denk geliyordu (pratikte ulaşılamaz), ve eski 3.
ile 4. bölümün **ilk** yıldızı beceri hedefiydi (`PerfectDodges(3)`,
`BoostDistance(500)`) — yıldızlar sıralı kazanıldığı ve sonraki bölüm
`stars > 0` ile açıldığı için bu bölümler birer ilerleme kilidiydi.

Yeni `LevelCurveTest` bölümleri gerçekten oynuyor (temkinli ve riskli iki
otopilot, beş sabit tohum): ilk 8 bölüm çarpmadan bitirilebiliyor, her bölüm
en az 1 yıldız veriyor, 1. bölüm her tohumda 3 yıldız veriyor, dodge/combo
hedeflerine riskli oyunla ulaşılıyor.

Testler: 89 → 108, hepsi geçiyor.

## 2026-08-14 — Faz 4: kontroller, reklam akışı, farm engelleri (v1.0.3, versionCode 4)

Proje sahibinin ikinci geri bildirim turu.

**Kontroller.** Dört buton 88 → 76 dp; aynı taraftaki fren↔sol-yön ve
boost↔sağ-yön arasındaki dikey boşluk 30 → 12 dp. Şikâyet: *"hız artınca
frene bastıktan sonra sola yön vermek için uzak kalıyor."* Ölçüler artık
`GameScreen.kt` başındaki dört sabitten geliyor. "FREN" etiketi tek dilliydi,
`AppLanguage.pick` ile TR/EN oldu.

**Menü köşe görselleri.** Prototipteki menü kartında logonun solunda araba,
sağında damalı bayrak vardı. Geri geldi — ama prototipin PNG'leri stok
görseldi ve arabanın üstünde **"Designed by pngtree" filigranı** vardı;
lisanssız görsel mağazaya giremez. Bayrak Canvas ile çizildi, soldaki araba
ise **oyuncunun garajda seçtiği araç** (`CarPreview`).

**Her koşu sonunda reklam** (`INTERSTITIAL_AFTER_EVERY_RUN = true`). Sonuç
ekranından çıkışın her yolu ve duraklatmadan "çıkış" geçiş reklamından geçer.
Agresif bir frekans; sahibi bilerek istedi, tek bayrakla geri alınabilir.

**"TEKRAR" butonu kaldırıldı.** Koşuyu bedavaya sıfırlayıp yeniden başlatmak
hem reklamsız tur çevirmenin hem de kısa koşu farmlamanın en kolay yoluydu.
Yerine çarpışma anındaki **reklamlı ve koşu başına bir kez** "devam et" kaldı.

**Duraklatmada reklamlı güçlendirici.** Reklam izleyince bu koşu için İkinci
Şans + dolu boost barı, koşu başına bir kez, yalnızca oyuncuda zaten İkinci
Şans yokken.

**Yükseltme eğrisi dışbükey oldu.** Dördü de doğrusaldı: 1. seviye alımı ile
7. seviye alımı aynı miktarda iyileştiriyordu (*"upgrade'ler çok hızla
iyileşiyor"*). Yeni eğri `UpgradeCatalog.curve()`, üs 1.5. Fren örneği:
28 → 29 → 31 → 34 → 37 → 40 → 43 → 47 km/h (eskiden 28'den 55'e, ilk adım
+4 km/h). Uç noktalar hız ve boost'ta korundu, fren ve toparlanma kısıldı.

**Garajdaki İVME göstergesi bozukmuş.** `1/accelRate` saniye olarak
yuvarlanınca 2. seviyeden sonra tüm seviyeler "0.1 s" gösteriyordu — oyuncuya
"bu yükseltme hiçbir şey yapmıyor" diyordu. Artık milisaniye: 167 → 100 ms.
Yeni test her dalın 8 seviyede 8 **farklı** değer göstermesini zorunlu kılıyor.

**Dört farm açığı kapatıldı** (ekonomi ajanının raporundan):

1. **Yıldız coini sadece yeni yıldız için ödeniyor.** Eskiden her oynayışta
   yeniden ödeniyordu; bölüm 1'i tekrarlamak (3,3 coin/sn) bölüm 30'u
   oynamaktan (2,3 coin/sn) daha kârlıydı. Sonuç ekranı "Yıldız ödülü +75"
   veya "alınmıştı" diye ayrıca yazıyor.
2. **"Ödülü ikiye katla" reklamının günlük limiti yoktu** — `addCoins`'i
   doğrudan çağırıyor, ödüllü reklam sayacını tamamen atlıyordu. Artık
   garajdaki reklamla **aynı** günlük sayacı kullanıyor ve tek seferde en
   fazla `REWARDED_DOUBLE_COINS_CAP` ödüyor. Buton da kırpılmış miktarı yazıyor.
3. **Cihaz saatini geri almak** günlük görevi ve reklam limitini sıfırlıyordu.
   Sıfırlama artık yalnızca **ileri** giden tarihte oluyor
   (`GameStateRepository.isFreshDay`, 6 testi var). Saati ileri alıp beklemeye
   karşı koruma yok — sunucusuz bir oyunda tam koruma mümkün değil.
4. **10 saniyeden kısa koşu hiç coin ödemiyor** (`MIN_PAID_RUN_SECONDS`).

**Günlük görev ödülü 1400 → 900** (120/260/520, sahibi kararı). Aynı gün
400-500'den 1400'e çıkarılmıştı; 1400 casual bir oyuncunun tüm günlük
bütçesinin (~1000) üstündeydi. Kademeli yapı korundu: 3. kademe hâlâ bir
kariyer koşusunun 14 katını ödüyor (eskiden 6 katı).

Doğrulama: **88 birim test geçiyor**, `assembleDebug` + `assembleRelease`
başarılı. Cihazda test edilmedi (bu makinede adb/emülatör yok).

Uygulanmayan (sahibi "aşamalı" dedi): gelir/maliyet sabitleri
(`SCORE_PER_BONUS_COIN`, `REWARDED_COIN_AMOUNT`, maliyet tablosu, booster
fiyatları) ve sonsuz mod zorluk tavanı — ekonomi raporunda hazır duruyor.

## 2026-08-14 — Araç özelleştirme (gövde şekli + boya)

Proje sahibi: *"sürülen araba şekli ve rengi customise edilebilmeli."*

- Yeni **`game/CarCatalog.kt`** (saf Kotlin): 4 gövde şekli, 9 boya, fiyatlar
  ve araç seviyesi şartları. Çizim geometrisi de burada **veri** olarak duruyor
  (`CarPart.Box/Disc/Wedge`); şekle özel Compose kodu yok, yeni şekil eklemek
  bu dosyaya bir kayıt eklemek demek.
- **Çarpışma kutusu değişmedi.** `CAR_ART_SCALE`, `CAR_WIDTH_PX`,
  `CAR_HEIGHT_PX`, `HITBOX_SCALE` aynı; her şekil aynı kutuya sığıyor ve
  `CarCatalogTest` bunu parça parça doğruluyor. Varsayılan şekil, prototipin
  **birebir orijinal çizimi** — güncelleme mevcut oyuncunun aracını değiştirmez.
- **Trafik araçları değişmedi** (`GameRenderer.drawObstacleCar`): tehdidin
  görünümü sabit kalmalı. Oyuncu paleti bilerek engel renklerinin (sarı,
  camgöbeği, beyaz, turuncu) dışından seçildi; bir test bunu koruyor.
- Kalıcılık: `car_shape`, `car_color`, `owned_car_shapes`, `owned_car_colors`
  DataStore anahtarları. Eski kayıtlar bozulmuyor — bilinmeyen ya da sahip
  olunmayan bir id **okurken** sessizce varsayılana düşüyor.
- Garaj ekranına **ARAÇ** bölümü: canlı Canvas önizlemesi, gövde şeridi, renk
  şeridi. Kilitli içeriğe dokunmak satın almaz, sadece önizler; satın alma
  ayrı ve fiyatlı butonda (yanlışlıkla coin harcanmasın diye).
- Ekonomi: gövdeler 0 / 900 / 1800 / 3200 coin, boyalar 0 → 2200 coin.
  Bu sayılar tek dosyada; denge ayrı bir çalışmada yeniden ayarlanacak.

Doğrulama: 78 birim test geçiyor (yeni `CarCatalogTest` 19 +
`PlayerProgressCarTest` 6), `assembleDebug` + `assembleRelease` başarılı.
Cihazda test edilmedi (bu makinede adb/emülatör yok).

## 2026-08-14 — Faz 3: hız hissi + kademeli günlük görev (v1.0.2, versionCode 3)

**Hissedilen hız yavaşlatıldı, gösterge aynı kaldı.** Oyuncu: *"görünen hız
aynı olsun ama hissedilen hız yavaşlamalı; 160 ile giderken bile aşırı hızlı,
oynanmıyor."* Gösterge zaten yalnızca `speed`'den hesaplanıyordu, dünyanın
kayması ise `speed × 250 × dt` idi. İkisi ayrıldı: yeni
`GameConfig.WORLD_SPEED_SCALE = 0.75` sadece dünyanın kaymasını çarpıyor.

- 160 km/h artık eski 120 km/h gibi oynanıyor: aracın yaklaşma hızı ve
  engeller arası ekran mesafesi o hızın değerleri, tepki süresi %33 uzun.
- Trafik sıklığı bozulmadı — engeller zamana göre doğuyor, hıza göre değil.
- `PIXELS_PER_METER` de aynı çarpanla küçültüldü; metre/saniye değişmedi,
  dolayısıyla bölümlerin mesafe hedefleri ve "3000 m ilerle" tipi görevler
  aynen kaldı.
- Tek ayar noktası: hâlâ hızlıysa `WORLD_SPEED_SCALE` küçültülür (0.65 →
  160 km/h ≈ 105 km/h hissi). Başka hiçbir sabite dokunmaya gerek yok.

**Günlük görev artık 3 kademeli ve artan ödüllü.** Önceden tek hedef + tek
ödül (400–500 coin) vardı ve hedef tutmazsa oyuncu hiçbir şey alamıyordu.

- Her günün görevi aynı türden 3 artan hedef: örneğin 6 / 14 / 25 Perfect
  Dodge. Ödüller **200 / 400 / 800** — tamamı 1400 coin (eskisinin ~3 katı).
  *(Aynı gün Faz 4'te 120/260/520 = 900'e çekildi, bkz. yukarısı.)*
- Kademeler **aynı koşuda** toplanır: koşu artık ilk hedefte değil, son
  hedefte otomatik biter.
- **Çarpmak kademeleri silmez.** 25 dodge yapıp son saniyede çarpan oyuncu da
  ulaştığı kademelerin ödülünü alır (`LevelEvaluator.tiersReached`).
- Aynı kademe gün içinde ikinci kez ödenmez; alınmış kademe sayısı DataStore'da
  tutulur (`daily_tier`). v1.0.1'den gelen "tamamlandı" kaydı okunmaya devam
  ediyor, o gün için üç kademe de alınmış sayılıyor.
- Ekranlar: ana menüde `0/3` ve sıradaki kademenin hedefi + ödülü, görevler
  ekranında üç rozet (alındı / sıradaki / kilitli), HUD'da peşinde olunan
  kademenin sayacı, sonuç ekranında kademe listesi ve gerçekten ödenen coin.
- Yeni hedef türü: `Objective.SurviveSeconds` (45 / 90 / 140 sn kademeleri).

Doğrulama: 53 birim test geçiyor, `assembleDebug` + `assembleRelease` başarılı.
Cihazda test edilmedi (bu makinede adb/emülatör yok).

## 2026-08-13 — Faz 2: ilk oyuncu geri bildirimi (v1.0.1, versionCode 2)

Proje sahibi debug APK'yı telefonda oynadı; şunlar değişti:

**Çarpışma adaleti (en önemlisi).** Ekran görüntüsünde iki araç arasında gözle
görülür boşluk varken kaza oluşuyordu. Kök neden: prototipten gelen çarpışma
kutusu 42×90'dı ama çizim o kutuyu doldurmuyordu — aracın **altında ~16 px
görünmez çarpışma alanı** vardı. Artık kutu çizimden türetiliyor ve üstüne
`HITBOX_SCALE = 0.88` ile biraz daraltılıyor (bu tür oyunlarda kutunun
görselden birkaç piksel küçük olması adil hissettirir).

- Araçlar küçültüldü: `CAR_ART_SCALE = 0.80` (40×76 → 32×61 görünür).
  Çizim ve çarpışma kutusu aynı çarpandan türediği için bir daha ayrışamazlar.
- Perfect Dodge eşiği otomatik olarak yeniden hesaplanıyor (şerit aralığına
  bağlı olduğu için).

**Hız.** İlk dört bölüm daha yavaş başlıyor: 60 / 65 / 70 / 75 km/h
(varsayılan 80). Skordan gelen hızlanma aynı — bölüm yine hızlanarak ilerliyor.
`LevelDef.startSpeedKmh` ile bölüm başına ayarlanabiliyor.

**Ses.** Tek testere dalgası "vızıltı" gibiydi; motor artık temel frekans +
2./3. harmonik + yarım frekansta hafif genlik modülasyonu ile üretiliyor ve
frekans aralığı düşürüldü. Boost'a basınca **nitro** çalıyor (kesme frekansı
açılan gürültü + yukarı süzülen ıslık), boost basılı kaldıkça altta hafif
tıslama kalıyor.

**Kontroller.** Şerit değiştirme yumuşatması hızlandırıldı (12 → 16), butonlar
büyütüldü (78 → 88 dp), **ekranda parmağı sağa/sola sürükleyerek de şerit
değiştirilebiliyor**, şerit değişiminde titreşimli geri bildirim var.

**HUD.** Üstteki koyu panel kaldırıldı. Skor sol üst, süre/hedef sağ üst köşede
(gölgeli metin, arkasında dikdörtgen yok), boost ekranın en üstünde 4 dp'lik
ince bir şerit. Bölüm hedefi artık tam cümle yerine "DODGE 3/8" biçiminde.

**Reklamlar.** "Tekrar dene" kendi eşiğiyle geçiş reklamı gösteriyor
(`INTERSTITIAL_EVERY_N_RETRIES = 2`). Sonuç ekranına "reklam izle → ödülü ikiye
katla", garaja "reklam izle → +150 coin" (günde 5 kez) eklendi.

**Doğrulama:** 46 birim test geçiyor, `assembleDebug` ve `assembleRelease`
başarılı. Ses kalitesi ve dokunma hissi **cihazda doğrulanmadı** — bu makinede
emülatör/adb yok.

## 2026-08-13 — Faz 1: native yeniden yazım (v1.0.0, versionCode 1)

HTML prototipi, Boom Blocks mimarisiyle native Kotlin/Compose Android
uygulamasına dönüştürüldü. WebView kullanılmadı.

**Taşınan (birebir):** hız eğrisi, boost/fren, trafik ve coin doğma oranları,
skorlama, çarpışma, yol/araç/coin/parçacık çizimi, dört yol kenarı teması
(grass/beach/crowd/night), hız göstergesi, WebAudio motor sesi → `AudioTrack`
sentezi.

**Eklenen meta katman:** Perfect Dodge + combo çarpanları, 30 bölümlük kariyer
modu ve 3 yıldız sistemi, sonsuz mod (zorluk eğrisi + kişisel rekor), garajda
4 dallı yükseltme (8 seviye), coin/XP ekonomisi, 4 booster, günlük görev,
haftalık görevler (5 görev × 3 kademe) + haftalık sandık, DataStore ile yerel
kayıt, TR/EN dil desteği, AdMob (banner / interstitial / rewarded revive) ve
UMP consent.

**Doğrulama:**
- `:app:compileDebugKotlin` — BUILD SUCCESSFUL
- `:app:assembleDebug` — BUILD SUCCESSFUL (debug APK üretildi)
- `:app:assembleRelease` — BUILD SUCCESSFUL (R8 + kaynak küçültme + lintVital
  geçti). Çıktı: `app-release-unsigned.apk`, **3.72 MB** — imzasız, çünkü
  henüz keystore yok.
- Motor birim testleri — bkz. `app/src/test/`
- **Cihaz/emülatör testi yapılmadı** (bu makinede adb/emülatör yok), release
  imzalama yapılmadı, gerçek AdMob kimlikleri girilmedi.

**Birim testlerin yakaladığı ve düzeltilen üç hata:**
1. Perfect Dodge eşiği sabit 64 px'ti; 320 dp'lik telefonlarda şerit
   aralığından (59.7) büyük kaldığı için yan şeritten düz geçmek bedava dodge
   veriyordu. Eşik artık şerit aralığından türetiliyor
   (`GameConfig.perfectDodgeMaxDx`), test her ekran genişliği için doğruluyor.
2. Boost barı boşalınca boost her karede açılıp kapanıyordu (ses/alev
   titremesi). Artık bar boşalınca kilitleniyor, parmağın kalkması gerekiyor.
3. `DailyChallengeGenerator.forDay` `abs(hashCode())` kullanıyordu;
   `abs(Int.MIN_VALUE)` negatif kaldığı için bazı gün kimlikleri dizin dışı
   hatası verebilirdi → `Math.floorMod`.

Ayrıca `GameEngine.step()` olay listesini kare **sonunda** temizliyor; böylece
`finish()` gibi dışarıdan çağrılan fonksiyonların ürettiği olaylar sessizce
kaybolmuyor.

**Bilinen eksikler:** `docs/RELEASE_CHECKLIST.md`.
