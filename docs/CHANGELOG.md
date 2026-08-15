# Değişiklik günlüğü

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
