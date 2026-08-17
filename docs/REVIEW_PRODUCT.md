# Kron Drive — Ürün Bütünlüğü İncelemesi

**Tarih:** 2026-08-17 · **İnceleyen:** Product Owner · **Durum:** inceleme, karar sahibinde

**Kapsam:** vaat/ürün uyumu, çekirdek döngü, ilerleme ekonomisi, harcama
anlamlılığı, tutunma, ürün riskleri.
**Kapsam dışı:** oynanış hissi (`REVIEW_GAMEPLAY.md`), arayüz
(`REVIEW_UX.md`), regresyon (`REVIEW_REGRESSION.md`), zorluk eğrisi
(`docs/DIFFICULTY_REVIEW.md` — Game Director, 2026-08-17), ekonomi sabit
analizi (`docs/ECONOMY_BALANCE_PROPOSAL.md`, 2026-08-16).

> **Bu belge kod değiştirmedi.** Hiçbir `.kt` dosyasına dokunulmadı, Gradle
> çalıştırılmadı, cihaza kurulum yapılmadı. Her iddia ya koddan okundu (dosya
> ve satır verildi) ya da koddaki formülden türetildi. **Oyuncu verisi yok** —
> oyun yayınlanmadı; "retention %X" tipi hiçbir sayı yok, olamaz da.
> Emin olmadığım yerler §8'de tek tek yazılı.

---

## 0. Özet

| Soru | Kısa cevap |
|---|---|
| Vaadi tutuyor mu | `DRIVE · DODGE · SURVIVE` **evet**. "Retro" **hayır** — yapı arcade, deri değil. Mağaza uzun metninde **kodla çelişen iki iddia** var. |
| Çekirdek döngü sağlam mı | Döngünün kendisi sağlam. **Tek kopuk halka var ve tam da kritik yerde:** başarısız koşudan sonra "TEKRAR" yok. |
| İlerleme ekonomisi tutarlı mı | Üç eksen değil, **bir eksen üç ayrı göstergeyle** ölçülüyor (hepsi skordan türüyor). Seviye kapısı 9. bölümden sonra ölü. |
| Harcama anlamlı mı | Yükseltmeler **evet** (asıl ilerleme, bilinçli karar). Araçlar **karakter**, ilerleme değil — ama fiyat merdiveni fabrika boyaları yüzünden bozuk. |
| Yarın neden gelinir | **Tek mekanizma: oyuncunun hatırlaması.** Bildirim yok, seri yok, hatırlatma yok. |

**En büyük üç risk (ayrıntı §6):** (1) başarısız koşudan sonra döngünün
kesilmesi, (2) Perfect Dodge'un hiçbir yerde öğretilmemesi, (3) kariyerin
%73'ünün parametrik olarak aynı koşu olması.

---

## 1. Vaat ve ürün

### 1.1 `DRIVE · DODGE · SURVIVE` — tutuluyor

Alt başlık dürüst. `LevelGoal` yalnızca iki tip: `SurviveTime` (21 bölüm) ve
`ReachDistance` (9 bölüm) — yani ürünün üçte ikisi kelimenin tam anlamıyla
"survive". Perfect Dodge gerçekten döngünün merkezinde (skor bonusu +
`COMBO_MULTIPLIERS` x3'e kadar çarpan, `GameConfig.kt:327-333`). Mağaza
metninin "bu oyunun kalbi budur" cümlesi mekanik olarak doğru.

### 1.2 "Retro" — yapı arcade, deri değil

Başlık `Kron Drive: Retro Araba Oyunu`, uzun metin "80'ler arcade
salonlarının kısa ve sert ritmi" diyor. Kodda **retro'yu taşıyan tek şey
yapı**:

- 3 şerit, 25-90 saniyelik koşular, geri sayım, skor, combo, hikâye yok,
  menü derinliği yok. Bu gerçekten arcade kabin ritmi.

Retro'yu **taşımayan** taraf, tek tek:

| Kanal | Durum | Kanıt |
|---|---|---|
| Tipografi | Sistem `FontFamily.SansSerif` + `FontWeight.Black`. Piksel/arcade yazı tipi yok, `res/font` klasörü **yok**. | `ui/theme/Type.kt:13-17` |
| Palet | Koyu lacivert + altın + camgöbeği. Modern mobil koyu tema; CRT, tarama çizgisi, neon ızgara yok. | `ui/theme/Color.kt:11-48` |
| Ses | **Müzik yok.** Sentezlenen motor + nitro + korna, o kadar. Chiptune/synthwave yok. | `audio/EngineVoice.kt` (`update`/`playNitro`/`playHorn` — başka olay yok) |
| Araç sanatı | Gradyanlı, spekuler vurgulu, perspektif düzeltmeli vektör araçlar. Bu **modern illüstrasyon**, retro'nun tam tersi. | `game/CarCatalog.kt:349-401` |
| Arka plan | GRASS/BEACH/CROWD/NIGHT — prototipten geliyor, arcade kökenli. Ama **her koşuda rastgele seçiliyor.** | `game/GameEngine.kt:100` |

**Boşluk:** başlıktaki "Retro" bir ASO anahtar kelimesi olarak seçilmiş
(`docs/play_store_assets/store_titles.md`), ama gerekçe belgesi
`docs/ASO_STRATEGY_NOTES.md` **repoda yok** — `store_titles.md` var olmayan
bir dosyaya atıf yapıyor. Yani "retro" kararının arkasındaki muhakeme
kayıtsız.

Bu **kendi başına bir hata değil** — anahtar kelime seçimi meşru bir karar
ve oyunun yapısı iddiayı kısmen taşıyor. Ama oyuncu mağazada "retro" görüp
ekran görüntüsünde modern gradyanlı araçlar görüyorsa, ilk temasta bir
uyumsuzluk var. Karar iki yönden de verilebilir: ya metin yumuşar
("arcade"), ya bir görsel kanal retro'yu üstlenir (en ucuzu: tek bir başlık
yazı tipi, `res/font` + `KronTypography.displayLarge`).

### 1.3 Mağaza metninde kodla çelişen iki iddia — bu bir hata

İkisi de hem TR hem EN uzun açıklamada:

**(a) "Görünüm değişir, çarpışma kutusu değişmez — özelleştirme kimseye
avantaj vermez, sadece garajın senin olur."**
Çarpışma kutusu kısmı doğru; **"avantaj vermez" yanlış.** 2026-08-15'ten
beri her gövdenin dört fizik çarpanı var: `topSpeedMul`, `accelMul`,
`brakeMul`, `boostMul`, aralık 0.90–1.14 (`game/CarCatalog.kt:191-246`).
Süper Araba (`1.12` hız) ile Beety (`0.92` hız) arasında son hızda **%22
fark** var. Bu bir avantajdır ve garajda çubuklarla gösteriliyor bile
(`CarCatalog.statFraction`). Yani ürün doğru davranıyor, **metin yanlış.**

**(b) "hız yükseltmesi skoru artırır ama trafiği de hızlandırır."**
Yanlış. Trafik hızı `baseSpeed * ratio` (`game/GameEngine.kt:511-514`) ve
`baseSpeed` yalnızca bölümün `startSpeedKmh`'inden geliyor
(`GameEngine.kt:103-104`). SPEED yükseltmesi `scoreSpeedCap`'i büyütür
(`UpgradeCatalog.kt:72-73`) — trafiğe **hiç dokunmaz**. Yükseltme oyuncunun
kendi hızını artırdığı için yaklaşma hızı artar ve oyun zorlaşır, yani
"bedava kazanç değil" kısmı ruhen doğru; ama yazılan mekanizma kodda yok.

**(c) Sayı hatası:** metin "Yedi gövde ... ve on boya" diyor; katalogda
**sekiz gövde** (`CarCatalog.kt:1152-1155`, Beety eklendi) ve **on bir boya**
(`:1246-1373`) var.

---

## 2. Çekirdek döngü

### 2.1 Döngünün kendisi

> Bölümü seç → geri sayım → şerit değiştir / boost / fren → koşu biter →
> yıldız + coin + XP → sonuç ekranı → sonraki bölüm.

Bu iyi kurulmuş. "Bir koşu daha" dürtüsünü **mekanik olarak** üreten dört
şey var ve dördü de kodda gerçekten çalışıyor:

1. **Sonraki bölüm butonu.** Bölüm geçildiyse sonuç ekranından tek
   dokunuşla devam (`ui/game/GameScreen.kt:1966-1971`); yeni ekran mevcut
   oyun ekranının **yerine** geliyor, menüye dönülmüyor
   (`AppNavigation.kt:166-167`). Bu doğru tasarım.
2. **Eksik hedefin ne kadar eksik olduğunu göstermek.** Sonuç ekranı her
   hedefi `1280/1400` biçiminde yazıyor (`GameScreen.kt:529-535`). "Az
   kalmıştı" hissi döngünün en ucuz yakıtıdır ve burada var.
3. **Yıldız coini yalnızca YENİ yıldız için ödeniyor** (`GameEngine.kt:770`)
   — tekrar oynamak farm değil, ustalık.
4. **Günlük görevde kademeler çarpsan bile ödeniyor**
   (`LevelEvaluator.tiersReached`, `completed` şartı yok). Kaybeden koşu bile
   bir şey veriyor.

### 2.2 Kopuk halka: başarısız koşudan sonra "TEKRAR" yok

Bu incelemenin en somut bulgusu.

`RunResultOverlay` içinde "SONRAKİ BÖLÜM" butonu **yalnızca**
`runResult.passed == true` iken çıkıyor (`GameScreen.kt:521-524`). Aksi
halde ekranda tek bir buton kalıyor: **ANA MENÜ** (`:1979-1983`). "TEKRAR"
butonu 2026-08-14'te bilerek kaldırılmış (`:1974-1978`, gerekçe:
`GameConfig.INTERSTITIAL_EVERY_N_RETRIES` yorumu, `:520-528`).

Sonuç, oyuncunun gerçekten yaşayacağı akış:

| Durum | Tekrar denemek için gereken |
|---|---|
| Bölüm 12'de çarptım | ANA MENÜ → KARİYER → haritada bölüm 12'yi bul → dokun → 3 sn geri sayım |
| Sonsuz modda rekorun 2 sn altında kaldım | ANA MENÜ → SONSUZ → 3 sn geri sayım |

Yani **oyunun tekrar oynama dürtüsü en yüksek olduğu anda** — az kalmış bir
başarısızlıktan hemen sonra — döngü kasıtlı olarak kesiliyor. 30-90
saniyelik bir arcade oyununda çarpmak normal sonuçtur, istisna değil.

**Kaldırma gerekçesi bugün artık geçerli değil.** Farm endişesi üç ayrı
mekanizmayla zaten kapalı:

- `MIN_PAID_RUN_SECONDS = 10` → kısa koşu hiç coin ödemiyor
  (`GameEngine.kt:779`)
- `newStars` → aynı yıldız ikinci kez ödenmiyor (`GameEngine.kt:770`)
- **2026-08-16'da kapatılan sayaç kaçağı** → başarısız koşu da artık reklam
  sayacını artırıyor (`AdFrequency.countsTowardInterstitial`, `:27-31`).
  Yani "çarp, çık, tekrar gir" döngüsü artık reklamsız değil.

Retry butonunun kaldırıldığı gün bu üçüncü koruma **yoktu**. Şimdi var.

### 2.3 Sessiz döngü: motorun ürettiği olayların yarısı kullanılmıyor

`GameEngine` altı olay üretiyor: `PerfectDodge`, `ComboBroken`,
`CoinPicked`, `VehiclePassed`, `BoostStarted`, `Crash`, `Finished`
(`GameModels.kt:301-310`). `GameScreen`'in olay `when` bloğu bunlardan
**üçünü `else -> Unit` ile yutuyor** (`GameScreen.kt:283`): `CoinPicked`,
`ComboBroken`, `VehiclePassed`.

Perfect Dodge'un tek geri bildirimi 900 ms'lik bir yazı
(`GameScreen.kt:265-272`). Ses yok, titreşim yok. Combo kopması hiç
bildirilmiyor. Yani mağaza metninin "bu oyunun kalbi" dediği mekanik,
oyuncuya **görsel bir yazı dışında hiçbir şey söylemiyor**.

(Ses tarafı `REVIEW_GAMEPLAY.md`'nin alanı; buraya ürün açısından yazıyorum:
vaadin merkezindeki mekanik, ödül geri bildiriminin en zayıf olduğu yer.)

---

## 3. İlerleme ekonomisi — üç eksen mi, bir eksen mi?

### 3.1 Üç eksenin de kaynağı aynı

| Eksen | Neyle ilerler | Formül |
|---|---|---|
| Bölüm kilidi | Sıralı hedeflerin ilk **ikisi** | `stars >= MIN_STARS_TO_PASS` (`GameEngine.kt:798-799`) |
| Araç seviyesi | XP | `xp = skor/10 + yıldız*20` (`:780-781`), `seviye = 1 + xp/500` |
| Coin | Toplanan coin + skor + yeni yıldız | `:771-773` |

XP ve coin **aynı sinyali** (skor) farklı bölenlerle ölçüyor. Bu tespit
zaten `ECONOMY_BALANCE_PROPOSAL.md §4`'te var ve doğru: aynı şeyi ölçen iki
kapı, birinin gereksiz olduğu anlamına gelir.

### 3.2 "%40'ında açılıyor" hâlâ geçerli mi? — Kısmen, ve ölçüm eskidi

`ECONOMY_BALANCE_PROPOSAL.md §4` "seviye kapısı gereken paranın ~%40'ında
açılıyor" diyor. O tablo **iki varsayımla** kuruldu ve ikisi de artık
geçersiz:

1. **§2.3: "Bir bölüm ancak üç görevin üçü de tamamlanınca geçilir ...
   Yani her ilk geçiş **her zaman** 3 yıldız = 75 coin öder."**
   Kod artık `MIN_STARS_TO_PASS = 2`. Oyuncu iki yıldızla (50 coin)
   geçebiliyor. Coin **kaybolmuyor** — 3. yıldızı sonra almaya gidebilir —
   ama ilk geçişte cebine giren para %33 azaldı.
2. `SCORE_PER_BONUS_COIN` 120 → 70 oldu (bugün).

İkisi erken oyunda **ters yönde** çalışıyor. Bölüm 3 için kaba hesap
(yıldız eşiği 1400 puan):

| | Eski (3 yıldız zorunlu, /120) | Yeni (2 yıldız yeter, /70) |
|---|---|---|
| Yıldız coini | 75 | 50 |
| Skor bonusu | 11 | 20 |
| Toplanan coin (~8) | 8 | 8 |
| **Toplam** | **94** | **78** |

Yani "koşu geliri +%18" ölçümü doğru ama **eksik**: `SCORE_PER_BONUS_COIN`
ölçüldüğünde `MIN_STARS_TO_PASS` hâlâ 3'tü. İkisi birlikte erken kariyerde
geliri muhtemelen **düşürdü**. Bunu iddia değil, **ölçülmesi gereken bir
şüphe** olarak yazıyorum (öneri Ö7).

**Bu bir sorun mu?** Kapının erken açılması tek başına zarar vermez —
kimseyi engellemeyen bir kapı sadece tasarım yüzeyi harcar. Asıl sorun
`ECONOMY_BALANCE_PROPOSAL §4`'ün ikinci bulgusu: **en yüksek seviye şartı
6** (Süper Araba ve Beety, `CarCatalog.kt:986`, `:1080`) ve oyuncu buna
9. bölümde ulaşıyor. Kariyerin %70'inde `requiredCarLevel` alanı 19 katalog
girdisinde **tamamen dekoratif**.

Dikkat: Beety (4000 coin, kataloğun en pahalısı) da `requiredCarLevel = 6`.
Yani fiyat merdiveni uzatıldı ama **seviye merdiveni uzatılmadı** — yeni
zirve ürün, eski zirve ürünle aynı kapının arkasında.

---

## 4. Neye para harcanıyor

### 4.1 Güncel sink tablosu (Beety ve 11. boya dahil)

| Sink | Toplam | Not |
|---|---|---|
| Yükseltmeler | 4 × 7.000 = **28.000** | `UpgradeCatalog.kt:41,44` |
| Araçlar (7 ücretli) | **15.300** | 900+1500+1500+1800+2400+3200+4000 |
| Boyalar (10 ücretli) | 9.750 → fabrika boyaları düşülünce **6.100** | Petrol 1400 (Kuş SLX), Buzul 500 (Dağ Keçisi), Gece 1750 (Boğa 67) hediye |
| **Kalıcı toplam** | **~49.400** | (ECONOMY doc'taki 46.450, Beety öncesi) |

### 4.2 Yükseltmeler mi araçlar mı ana ilerleme? — Yükseltmeler, ve bu doğru

Sayı: bir yükseltme dalı 1→8 arasında `scoreSpeedCap`'i 3.2'den 4.32'ye
çıkarıyor (**+%35**, `UpgradeCatalog.kt:72-73`). Sekiz aracın tamamı aynı
eksende **0.92–1.12** aralığında, yani **±%12**. Araç seçimi yükseltmenin
üçte biri kadar bile etki etmiyor.

Bu `docs/BALANCE.md`'de gerekçeli bir karar ("çarpanlar yükseltmelerin
ÜSTÜNE uygulanır, yerine geçmez") ve **doğru bir karar** — araçlar
karakter katıyor, ilerlemeyi taşımıyor. İkisi birbiriyle **yarışmıyor**;
farklı işler yapıyorlar. Sorun burada değil.

### 4.3 Sorun: fiyat merdiveni "güç" diyor, istatistikler "yan geçiş" diyor

**(a) Fabrika boyası merdiveni bozdu.** Aracın net fiyatı = fiyat − hediye
boyanın fiyatı:

| Araç | Fiyat | Fabrika boyası | **Net** | Seviye şartı |
|---|---|---|---|---|
| Yarış Sedan | 900 | yok | **900** | 2 |
| Kuş SLX | 1.500 | Petrol (1.400) | **100** | 2 |
| Dağ Keçisi | 1.500 | Buzul (500) | **1.000** | 2 |
| Kas Arabası | 1.800 | yok | **1.800** | 4 |
| Boğa 67 | 2.400 | Gece Siyahı (1.750) | **650** | 5 |
| Süper Araba | 3.200 | yok | **3.200** | 6 |
| Beety | 4.000 | yok | **4.000** | 6 |

Yarış Sedan katalogda "ilk satın alınan araç tereddütsüz iyi hissettirmeli"
diye tasarlanmış (`CarCatalog.kt:653-657`) ama **aynı seviye şartıyla**,
600 coin fazlaya, Kuş SLX + 1.400'lük bir boya alınabiliyor. Yani tasarımın
giriş aracı, kendi fiyat bandında domine ediliyor.

**(b) Zirve satın alma skoru düşürüyor.** Beety 4.000 coin, `topSpeedMul =
0.92` — **kataloğun en düşüğü**. Skor `speed * 11 * dt` ile birikiyor
(`GameConfig.kt:291`), yani en pahalı araç oyuncuyu Süper Araba'ya göre
kabaca **%10 daha az** skor/coin/XP kazandırıyor. Üstüne Beety referans
çiziminde sarı ve Gün Sarısı ayrı satılıyor (700, `CarCatalog.kt:1305-1316`)
→ aracı tasarlandığı hâlde görmek **4.700 coin**.

Bunun tamamen savunulabilir bir okuması var: Beety bir yan geçiş
(en iyi ivme 1.14 + en iyi frenlerden 1.12), bir güç yükseltmesi değil. Ve
garaj bunu karşılaştırmalı çubuklarla **gösteriyor**
(`CarCatalog.statFraction/statDeltaPercent`). Ama **fiyat en gürültülü
sinyaldir**: 4.000 coin "bu en iyisi" der. Oyuncu 4.000 biriktirip aldıktan
sonra skorunun düştüğünü fark ederse, bu "yan geçiş" olarak değil "kandırıldım"
olarak okunur.

### 4.4 Kayda geçmiş, çözülmemiş: Çift Ödül matematiksel olarak kârsız

`ECONOMY_BALANCE_PROPOSAL §6.1` bunu bulmuştu: `DOUBLE_REWARD` 300 coin,
koşu coinini ikiye katlıyor. `SCORE_PER_BONUS_COIN` 70'e inince en iyi koşu
(~13.800 skor) `51 + 197 = 248` coin ödüyor; ikiye katlanınca kazanç +248,
maliyet 300 → **hâlâ net −52**. Bugünkü değişiklik sorunu küçülttü ama
kapatmadı. Oyunda **her koşuda zarar ettiren bir ürün** satılıyor.

---

## 5. Yarın neden geri gelinir?

### 5.1 Bugün var olanlar

- **Günlük görev:** tarihten türetilen 7 şablondan biri, üç kademe,
  80/140/280 = günde en fazla 500 coin. Kademeler çarpsan bile ödeniyor.
  Tasarım olarak doğru.
- **Haftalık görev:** 5 sabit görev × 3 kademe = 900 coin + tüm kademeler
  bitince 750'lik sandık + bir booster.
- **90 yıldız meta-hedefi:** `MIN_STARS_TO_PASS = 2` olduğu için üçüncü
  yıldızlar geride kalıyor → geri dönmek için gerçek bir sebep. Menüdeki
  `GÖREV n/90` çipi bunu görünür tutuyor (`MainMenuScreen.kt:133-137`).
- **Sonsuz mod rekoru** + "rekoruna 5 saniye kaldı" mesajı.

Bunlar **içerik olarak yeterli**. Eksik olan içerik değil, **tetikleyici**.

### 5.2 Eksik olan: hiçbir geri çağırma mekanizması yok

`AndroidManifest.xml` iki izin istiyor: `INTERNET` ve
`ACCESS_NETWORK_STATE`. **`POST_NOTIFICATIONS` yok**, `WorkManager` yok,
hiçbir hatırlatma yok. Ayrıca kodda **gün serisi (streak) sayacı yok** —
`PlayerProgress` içinde ardışık gün ya da toplam giriş günü tutan hiçbir
alan yok.

Yani: günlük görev "bugün girdiysen ödül var" diyor, ama oyuncuya
"bugün gir" diyen hiçbir şey yok. Tutunma tamamen oyuncunun uygulamayı
hatırlamasına bağlı.

### 5.3 Sunucu ve hesap OLMADAN yapılabilecekler

Üçü de tamamen yerel, hiçbiri Web3/hesap/sunucu gerektirmiyor:

1. **Gün serisi (streak).** DataStore'da `lastPlayedDay` + `streakDays`.
   Menüde "3 gün üst üste" rozeti, 3/7/14. günde artan bir ödül. Sunucusuz,
   göç riski yok, Play uyum etkisi sıfır. **En ucuz kazanç.**
2. **Yerel bildirim.** `WorkManager` + `POST_NOTIFICATIONS`, günde bir kez
   "günün görevi hazır". Veri toplamadığı için Data Safety'de yeni bir
   beyan gerektirmez ama `DATA_SAFETY_FORM.md` ve izin listesi güncellenmeli
   → `play-store-compliance-engineer` onayı şart.
3. **Günlük görevi menüde büyütmek.** Şu an sıradan bir `MenuButton`
   (`MainMenuScreen.kt:170-185`). "Bugünün ödülü: +80" görsel olarak
   KARİYER'den zayıf.

### 5.4 Dürüst çerçeve: bu "bir kez bitirilip bırakılan" bir oyun mu?

Kariyer ~30 bölüm × ~60 sn = **yaklaşık 30-45 dakika saf oyun süresi**
(tekrar denemeler hariç). Yükseltme hattı 28.000 coin —
`ECONOMY_BALANCE_PROPOSAL §7`'nin modeline göre sadece oynayan oyuncu için
**~23-31 gün**. Yani ürünün uzun kuyruğu kariyerde değil, **garajda**.

Bu kötü bir konum değil ama şunu gerektirir: oyuncunun garaja gitmek için
bir sebebi olmalı ve o sebep şu an zayıf, çünkü (§4.2) araçlar ±%12 fark
yaratıyor ve ilk yükseltme adımı görünmez (`ECONOMY_BALANCE_PROPOSAL §5.1`:
250 coin karşılığı +1 km/h).

---

## 6. En büyük üç ürün riski

### Risk 1 — İlk 10 dakika: Perfect Dodge öğretilmiyor, hiç

Kodda **tek bir öğretici, ipucu veya ilk-koşu açıklaması yok.** Arama
sonucu: `ui/` altında `hint`/`tutorial`/`ipucu` geçen hiçbir yer yok.
`PlayerProgress.hasSeenOnboarding` alanı var, `GameStateRepository.kt:134`
onu **okuyor**, ama hiçbir yer **yazmıyor** ve hiçbir şeyi kapatmıyor — ölü
bayrak.

Oyuncunun ilk teması: dil ekranı → menü → geri sayım → üç buton. Ne Perfect
Dodge'un ne olduğu, ne boost barının nasıl dolduğu söyleniyor. HUD'da
`DODGE 0/3` yazıyor ama "dodge" ne demek yazmıyor
(`Objective.PerfectDodges.shortLabel` = `"DODGE"`).

Bölüm 4'ün üçüncü hedefi `PerfectDodges(3)`. `MIN_STARS_TO_PASS = 2`
sayesinde bu **artık geçişi engellemiyor** (bugünkü değişiklik doğru işi
yaptı) — ama bölüm 9'dan itibaren durum değişiyor, bkz. Risk 3.

Üstüne §2.3: mekanik hiçbir ses/titreşim geri bildirimi vermiyor. Yani
oyuncu yanlışlıkla bir Perfect Dodge yapsa bile, ne yaptığını anlamıyor.

**Mağaza metni "bu oyunun kalbi budur" diyor. Oyun bunu ne öğretiyor ne de
hissettiriyor.**

### Risk 2 — İlk gün: başarısız koşudan sonra döngü kesiliyor

§2.2'nin tamamı. Bir arcade oyununda çarpmak normal sonuçtur; ürün bu
normal sonuca "ana menüye dön" diye cevap veriyor. İlk gün bir oyuncunun
20 koşusunun belki 12'si başarısızdır ve her birinde üç dokunuş + iki ekran
geçişi ödüyor.

Bu risk **Risk 1'i büyütüyor**: öğrenme eğrisinin doğal yolu "dene, öl,
hemen tekrar dene"dir. Tekrar denemek pahalıysa öğrenme de pahalılaşıyor.

### Risk 3 — İlk hafta: kariyerin %73'ü aynı koşu, sonra ani beceri duvarı

**(a) Parametre tekdüzeliği.** `LevelCatalog`'da `startSpeedKmh` ve
`trafficDensity` **yalnızca 1-6. bölümlerde** yazılı (grep: satır 55, 56,
68, 69, 82, 83, 95, 96, 108, 121, 122 — hepsi ilk 6 bölüm). **7-30 arası 24
bölümün tamamı** varsayılan 80 km/h + yoğunluk 1.0 ile çalışıyor. Bölüm 9
ile bölüm 30 arasındaki tek fark: süre (60 sn → 90 sn) ve hedef sayıları.

Üstüne arka plan teması **koşu başına rastgele** (`GameEngine.kt:100`) —
yani bölümlerin görsel kimliği de yok. "30 bölüm" bir içerik vaadidir;
ürün 8 bölümlük tasarım + 22 sayı varyasyonu sunuyor.

Onaylanan `speedRampScale` önerisi (`DIFFICULTY_REVIEW.md §6.6`) tam bu
boşluğu kapatmak için ve **henüz uygulanmadı.**

**(b) Beceri duvarı geç ve hazırlıksız geliyor.** `MIN_STARS_TO_PASS = 2`
+ yıldızların **sıralı** kazanılması (`LevelEvaluator.tiersReached`)
demektir ki: **ilk iki hedef zorunlu.** `GameConfig.kt:405-407`'deki kural
açıkça şöyle yazıyor:

> "**iki görev bölümü açar, üçüncüsü ustalık yıldızıdır.** Yani beceri
> hedefi (PerfectDodge/Combo) ÜÇÜNCÜ sırada olduğu sürece hiçbir oyuncu ona
> takılıp ilerleyemez hale gelmez."

Bu kural **1-8. bölümlerde uygulanmış, 9-30 arasında uygulanmamış.**
Geçmek için iki beceri hedefinin ikisini de gerektiren bölümler:

| Bölüm | 1. hedef (zorunlu) | 2. hedef (zorunlu) |
|---|---|---|
| 11 | PerfectDodges(8) | ComboAtLeast(4) |
| 14 | ComboAtLeast(5) | PerfectDodges(10) |
| 17 | ScoreAtLeast(4800) | ComboAtLeast(5) |
| 20 | PerfectDodges(14) | ComboAtLeast(6) |
| 23 | ComboAtLeast(6) | PerfectDodges(16) |
| 26 | PerfectDodges(18) | ComboAtLeast(7) |
| 29 | ComboAtLeast(8) | PerfectDodges(20) |

Ayrıca **fren yasağı** bölüm 19'da `BrakeTapsAtMost(0)` ve **birinci
sırada**: frene bir kez basan oyuncu **sıfır yıldız** alır ve bölüm
geçilmez. Tek bir dokunuş = koşu tamamen boşa. (Bölüm 13 ve 25'te aynı
hedef `AtMost(1)` ile, yine birinci sırada.)

**(c) İstenen combo'nun ödülü yok.** `COMBO_MULTIPLIERS` 5 elemanlı ve
`comboMultiplier` combo'yu dizi boyuna kırpıyor (`GameConfig.kt:330-333`):
combo 5, 6, 7, 8 → **hepsi x3**. Bölüm 20 combo 6, bölüm 26 combo 7, bölüm
29 combo 8 istiyor. Oyun, oyuncudan **kendi puanlama sisteminin
ödüllendirmediği** sayıları istiyor.

---

## 7. Öneri seti

Önem sırasıyla, sekiz madde. Her biri bağımsız uygulanabilir; çakışmalar
belirtildi.

---

### Ö1 — Sonuç ekranına "TEKRAR" geri gelsin ⭐ ÖNCE BU

**Ne:** `RunResultOverlay`'de, `passed == false` olan kariyer koşularında ve
**tüm sonsuz mod koşularında** "TEKRAR" butonu. Kariyerde aynı bölümü,
sonsuzda yeni koşuyu mevcut ekranın yerine başlatır (SONRAKİ BÖLÜM'ün
kullandığı `AppNavigation.kt:166-167` deseni birebir uygulanabilir).

**Neden:** Döngünün tek kopuk halkası (§2.2) ve tam olarak dürtünün en
yüksek olduğu noktada. Kaldırılma gerekçesi (farm) bugün üç ayrı mekanizmayla
zaten kapalı — özellikle 2026-08-16'da kapatılan reklam sayacı kaçağı, ki o
kaçak kapandığında bu buton zaten yoktu.

**Dosya/sabit:** `ui/game/GameScreen.kt` (`RunResultOverlay`, ~1966-1983 ve
çağrı yeri ~517-545), `ui/navigation/AppNavigation.kt`.
`GameConfig.INTERSTITIAL_EVERY_N_RETRIES` (şu an kullanılmıyor) yeniden
devreye alınabilir ya da mevcut `AdFrequency` yeterli sayılabilir.

**Büyüklük:** küçük-orta.

**Çakışma:** Ö5 ile **etkileşimli** — retry koşu sayısını artıracağı için
gerçek reklam sıklığı da artar (`INTERSTITIAL_EVERY_N_LEVELS = 3` sabit
kalırsa). İkisi aynı sürümde giderse reklam sıklığı cihazda ölçülmeli.

---

### Ö2 — İlk koşuda Perfect Dodge öğretilsin, `hasSeenOnboarding` canlansın

**Ne:** Bölüm 1'in ilk koşusunda üç kısa, oyunu durdurmayan ipucu:
şerit değiştirme → boost/fren → "bir aracın yanından kıl payı geç = PERFECT
DODGE". Bir kez gösterilir, `hasSeenOnboarding` yazılır.

**Neden:** Risk 1. Ürünün kendi mağaza metninin "kalbi" dediği mekanik
in-app'te hiç anlatılmıyor ve `hasSeenOnboarding` bayrağı bunun için zaten
duruyor — okunuyor ama hiç yazılmıyor (`GameStateRepository.kt:134`), yani
altyapı yarım bırakılmış.

**Dosya/sabit:** `ui/game/GameScreen.kt` (yeni ipucu katmanı),
`data/GameStateRepository.kt` (`HAS_SEEN_ONBOARDING` yazımı),
`data/PlayerProgress.kt:70`.

**Büyüklük:** orta.

**Çakışma:** `REVIEW_UX.md` ile örtüşebilir (yerleşim/okunabilirlik onun
alanı). Ö4 ile birlikte yapılırsa daha etkili — öğret + hissettir.

---

### Ö3 — Mağaza uzun açıklamasındaki üç hata düzeltilsin ⚠️ YAYIN ÖNCESİ ZORUNLU

**Ne:** TR ve EN metinlerde:
1. "özelleştirme kimseye avantaj vermez" → araç çarpanları var, cümle
   yeniden yazılmalı (§1.3a). Doğru mesaj: "çarpışma kutusu değişmez,
   araçların karakteri değişir".
2. "hız yükseltmesi ... trafiği de hızlandırır" → yanlış mekanizma (§1.3b).
3. "Yedi gövde ... on boya" → sekiz gövde, on bir boya.

**Neden:** Mağaza açıklaması bir vaattir. Yanlış vaat, yayın sonrası
düzeltmesi en pahalı şeydir (yorumlarda kalır). Üçü de **20 dakikalık**
metin işi.

**Dosya:** `docs/play_store_assets/store_long_description_tr.txt`,
`store_long_description_en.txt`.

**Büyüklük:** küçük.

**Çakışma:** Ö8 ile — araç fiyatları/istatistikleri değişirse metin ikinci
kez elden geçer. Ö8 kararı önce verilirse tek seferde yapılır.

---

### Ö4 — Bölüm 7-30 parametrik olarak farklılaşsın (`speedRampScale` + yoğunluk)

**Ne:** `DIFFICULTY_REVIEW.md §6.6`'daki `speedRampScale` uygulansın **ve**
`trafficDensity` 30 bölüme yayılsın (şu an yalnızca 1-6'da yazılı). Ayrıca
`RoadTheme` bölüme bağlansın — koşu başına rastgele olmasın
(`GameEngine.kt:100`), böylece bölümlerin görsel kimliği olsun.

**Neden:** Risk 3a. Kariyerin %73'ü aynı koşu ve arka planı bile rastgele.
"30 bölüm" içerik vaadi, ürün 8 bölümlük tasarım veriyor. `speedRampScale`
zaten **onaylanmış**, sadece uygulanmamış.

**Dosya/sabit:** `game/LevelCatalog.kt`, `game/GameModels.kt` (`LevelDef`'e
yeni alan), `game/GameEngine.kt:100`.

**Büyüklük:** orta.

**Çakışma:** Ö5 ile **aynı dosya** — ikisi tek bir `LevelCatalog` turunda
yapılmalı, yoksa iki kez test/ölçüm gerekir.

---

### Ö5 — Geç kariyer kapıları kendi kuralına uysun + combo tavanı hizalansın

**Ne:** İki parça.
1. `LevelCatalog`'da 9-30 arası bölümlerde beceri hedefleri (PerfectDodge /
   Combo / BrakeTapsAtMost) **üçüncü sıraya** taşınsın —
   `GameConfig.MIN_STARS_TO_PASS` yorumunda yazılı olan ve 1-8'de zaten
   uygulanan kural. §6.3b'deki 7 bölüm + fren yasaklı 13/19/25.
   **Sayılar değişmiyor, sadece sıra.**
2. `COMBO_MULTIPLIERS` combo 5'te doyuyor ama bölümler 6/7/8 istiyor
   (§6.3c). Ya diziye 6-8 için değerler eklensin, ya bölüm hedefleri 5'i
   aşmasın. İkisinden biri; ikisi de olmaz.

**Neden:** Risk 3b/3c. Özellikle bölüm 19'da `BrakeTapsAtMost(0)` birinci
sırada: tek fren dokunuşu = sıfır yıldız = bölüm geçilmez. Bu bir zorluk
değil, bir tuzak.

**Dosya/sabit:** `game/LevelCatalog.kt`, `game/GameConfig.kt:330`.

**Büyüklük:** küçük (sıra) + küçük (combo tavanı) = küçük-orta.

**Çakışma:** Ö4 ile aynı dosya (birlikte yapılmalı).
`app/src/test/` altındaki `LevelCurveTest` güncellenmeli.

---

### Ö6 — Geri dönme kancası: gün serisi (streak)

**Ne:** DataStore'a `lastPlayedDayId` + `streakDays`; menüde seri rozeti;
3/7/14. günde artan bir coin/booster ödülü. Yerel bildirim (`WorkManager` +
`POST_NOTIFICATIONS`) **ikinci adım** olarak değerlendirilsin.

**Neden:** §5.2. Günlük görev "bugün girdiysen ödül var" diyor ama "bugün
gir" diyen hiçbir şey yok. Streak sunucusuz, hesapsız, izinsiz ve göç
riski sıfır — en ucuz tutunma kazancı.

**Dosya/sabit:** `data/PlayerProgress.kt`, `data/GameStateRepository.kt`,
`ui/menu/MainMenuScreen.kt`.

**Büyüklük:** streak küçük-orta; bildirim orta (izin + Data Safety +
`play-store-compliance-engineer` onayı).

**Çakışma:** Bildirim kısmı `docs/DATA_SAFETY_FORM.md` ve
`PLAY_RELEASE_CHECKLIST.md` güncellemesi gerektirir. Streak kısmının hiçbir
çakışması yok — **bildirimden ayrı ve önce yapılabilir.**

---

### Ö7 — Ekonomi ölçümü tazelensin (kod değişikliği önermiyorum)

**Ne:** `LevelCurveTest`'teki `olcum dokumu` testi, `MIN_STARS_TO_PASS = 2`
**ve** `SCORE_PER_BONUS_COIN = 70` birlikteyken tekrar çalıştırılsın; çıkan
`coin=` değerleriyle `ECONOMY_BALANCE_PROPOSAL.md §2.3, §4, §7` tabloları
güncellensin. Aynı commit'te `docs/BALANCE.md`'nin koddan sapan üç yeri
düzeltilsin (o belge listesi `ECONOMY_BALANCE_PROPOSAL §2.4`'te hazır).

**Neden:** §3.2. `ECONOMY_BALANCE_PROPOSAL` açıkça "her ilk geçiş her zaman
3 yıldız = 75 coin öder" varsayımıyla kurulmuş ve o varsayım artık yanlış.
Bugünkü iki değişiklik erken oyunda birbirini kısmen götürüyor olabilir —
**bu bir şüphe, ölçülmeden kimse iddia etmemeli.** Ve o belgedeki Öneri 3
(`XP_PER_CAR_LEVEL` 500 → 1250, göç riski **yüksek**) bu ölçüm gelmeden
karara bağlanmamalı.

**Dosya:** `app/src/test/.../LevelCurveTest.kt` (çalıştırma),
`docs/ECONOMY_BALANCE_PROPOSAL.md`, `docs/BALANCE.md`.

**Büyüklük:** küçük.

**Çakışma:** Ö4, Ö5, Ö8'in **hepsi** bu ölçümü geçersiz kılar. Ya bu ölçüm
önce alınır (mevcut hâlin fotoğrafı), ya da hepsinden sonra bir kez alınır.
İkisinin arasında yapmak en kötüsü.

---

### Ö8 — Araç fiyat merdiveni gözden geçirilsin (fabrika boyası + Beety)

**Ne:** İki karar noktası, ikisi de sahibinin:
1. **Fabrika boyası fiyata sayılsın mı?** Kuş SLX net 100, Boğa 67 net 650,
   Yarış Sedan net 900 (§4.3a). Ya araç fiyatları hediye boyayı yansıtsın,
   ya fabrika boyası "kilidi açar ama ücretsiz seçilemez" olsun, ya da
   olduğu gibi kabul edilip Yarış Sedan'ın fiyatı düşsün.
2. **Beety 4.000'e değer mi?** `topSpeedMul = 0.92` ile kataloğun en pahalı
   aracı skor/coin/XP kazancını ~%10 düşürüyor (§4.3b). Ya fiyat düşsün,
   ya çarpanlar zirveye yakışsın, ya da garajda "yan geçiş" olduğu fiyattan
   daha yüksek sesle söylensin.

**Neden:** Fiyat, oyuncunun okuduğu en gürültülü güç sinyalidir. Şu an
fiyat sıralaması ile güç sıralaması uyuşmuyor ve bu, ilerleme hissini
değil güven hissini etkiliyor.

**Dosya/sabit:** `game/CarCatalog.kt` (`priceCoins`, `defaultColorId`,
`topSpeedMul`).

**Büyüklük:** küçük (sabit değişimi) — ama denge etkisi orta.

**Çakışma:** Ö3 (metin) ve Ö7 (ölçüm) ile. **Ö7'nin ölçümü gelmeden fiyat
değiştirilmemeli.** Ayrıca `CarCatalogTest` "tek zayıfsız araç" istisnasını
donduruyor — çarpan değişirse test kırılır (kırılması doğrudur).

---

### Bilerek önermediklerim

- **Yükseltme fiyatları / eğrisi** — `ECONOMY_BALANCE_PROPOSAL §5`'te
  gerekçeli bir karar; ilk adımın görünmezliği gerçek bir sorun ama orada
  zaten kayda geçmiş.
- **Reklam sabitleri** — bugün ayarlandı, hiç ölçülmedi. Ö1 uygulanınca
  gerçek sıklık kendiliğinden artacak; önce o görülmeli.
- **Çift Ödül booster fiyatı** — §4.4'te kayıtlı, `ECONOMY_BALANCE_PROPOSAL
  §6.1`'in devamı, ayrı bir iş.
- **`STARTING_COINS`** — tek başına çözülmemeli
  (`ECONOMY_BALANCE_PROPOSAL §6.2`).
- **"Retro" başlığı** — ASO kararı, ürün kararı değil; `app-store-growth-aso-engineer`
  alanı. Sadece kayda geçirdim (§1.2) ve gerekçe belgesinin eksik olduğunu
  not ettim.
- **Ses/titreşim geri bildirimi** — `REVIEW_GAMEPLAY.md` alanı; §2.3'te
  yalnızca ürün açısından işaret ettim.

---

## 8. İyi çalışan ve emin olmadığım şeyler

### İyi çalışıyor — dokunulmamalı

- **Farm kapatma mekanizmalarının tamamı tutarlı.** `newStars`,
  `MIN_PAID_RUN_SECONDS`, `REVIVE_MAX_PER_RUN = 1`, ödüllü reklamın garaj ve
  sonuç ekranı arasında **paylaşılan** günlük limiti. Her biri gerçek bir
  sömürüye cevap ve hiçbiri oyunu bozmuyor.
- **Reklam politikası bu türde iyi bir konum.** Oyun ekranında banner yok,
  ilk 4 bölüm reklamsız, ödül yalnızca SDK'nın gerçek "kazanıldı"
  callback'inde veriliyor, ödüllü reklamın vereceği miktar **önceden
  kırpılarak** yazılıyor (`GameScreen.kt:1925-1928`) — "+500 gördüm, 150
  aldım" güven kırığı bilerek engellenmiş.
- **Günlük görevin kademeli yapısı.** Çarpsan bile ulaştığın kademelerin
  ödenmesi doğru tasarım; "ya hep ya hiç" hedeflerin klasik hatasından
  kaçınılmış.
- **1-6. bölümlerin öğrenme eğrisi gerçekten tasarlanmış.** Her bölüm tek
  yeni şey öğretiyor, 6. bölüm bilerek 5'ten kolay (testere dişi eğri).
  Sorun bu tasarımın 7'den sonra devam etmemesi, tasarımın kendisi değil.
- **Katalogların veri odaklı olması.** Yeni bölüm/araç/boya eklemek tek
  satır; `game/` paketinin saf Kotlin kalması JVM testiyle doğrulanabilirlik
  sağlıyor. Ö4/Ö5/Ö8'in hepsi bu yüzden **ucuz**.
- **Offline / hesapsız / Web3'süz konum net ve savunulabilir**, mağaza
  metninde de dürüstçe anlatılmış.

### Emin değilim

- **Combo 6/7/8'in pratikte ulaşılabilir olup olmadığını ölçmedim.**
  Mekanik olarak mümkün (0.78 sn'de bir araç, 6 sn'lik combo penceresi),
  ama gerçek bir cihazda kaç oyuncunun yapabileceğini bilmiyorum. §6.3c'de
  yazdığım şey **ödülün doyduğu**, hedefin imkânsız olduğu değil.
- **"Retro" algısı bir hipotez.** Oyuncunun mağazada "retro" görüp ekran
  görüntüsünde modern araç sanatı görünce ne hissettiğini ölçemem. Kodda
  gördüğüm şey kanallar arası tutarsızlık; sonucun ne olacağı tahmin.
- **§3.2'deki bölüm 3 hesabı bir model, ölçüm değil.** Yıldız eşiklerinden
  ve skor eğrisinden türetildi. Doğru sayı Ö7'nin ölçümünden gelir.
- **Kariyerin ~30-45 dakika sürdüğü tahmini** bölüm süreleri toplamından
  geliyor; tekrar denemeler dahil değil, ölçülmedi.
- **Cihazda hiç oynamadım** (görev sınırı gereği). Buradaki her davranış
  iddiası kod okumasıdır. Ö1 ve Ö2'nin gerçek etkisi ancak elde anlaşılır.
