# Kron Drive — Kaynak ve Köken

## Oyunun kökeni

Kron Drive önce bir HTML/JavaScript prototipi olarak yazıldı. Bu Android projesi
o prototipin **native Kotlin yeniden yazımıdır** (proje sahibinin kararı,
2026-08-13): oyun bir WebView içinde çalışmıyor, tüm simülasyon ve çizim
Kotlin + Jetpack Compose ile yapılıyor.

| Girdi | Ne için kullanıldı |
|---|---|
| `KRON_DRIVE_FINAL_BALANCED_80_3.html` (portrait sürüm, proje sahibi tarafından verildi) | **Doğruluk kaynağı.** Tüm fizik, hız eğrisi, boost/fren davranışı, spawn oranları, skorlama, yol/araç çizimi ve motor sesi buradan birebir taşındı. |
| `kron_drive_package.zip` (Capacitor/PWA paketi, landscape sürüm) | Sadece **görsel varlıklar**: `assets/icon-512.png` (KRON logosu) launcher ikonlarına ve menü logosuna dönüştürüldü. Oyun kodu bu paketten alınmadı — içindeki sürüm eski ve yatay. |
| Boom Blocks (`C:\Users\bhdre\APPDeveloper\projects\Boom-Blocks`) | **Mimari şablon.** Gradle yapılandırması, DataStore repository deseni, AdMob yöneticileri, UMP consent akışı, klasör düzeni aynı desenle kuruldu. Kod kopyalanmadı, desen izlendi. |

## Prototipten bilinçli sapmalar

Bunlar dışında fizik birebirdir:

1. **Hız gecikmesi.** Prototipte hız anında hedefe atlıyordu (`state.speed = currentSpeed()`).
   ACCELERATION yükseltmesinin bir anlamı olabilmesi için birinci dereceden bir
   gecikme eklendi. Taban oran (6.0/s ≈ 0.17 s) bilerek yüksek seçildi, orijinal
   "anlık" his pratikte korunur. → `GameConfig.ACCEL_RATE_BASE`
2. **Geri sayım 5 → 3 saniye.** Kısa bölümlerde 5 saniye bekletmek akışı kırıyordu.
   → `GameConfig.COUNTDOWN_SECONDS`
3. **Boost yeniden tutuşma kilidi.** Prototipte bar boşalınca `wantsBoost` aynı
   karede false oluyor, dolum başlıyor ve bir sonraki karede boost yeniden
   tutuşuyordu — parmak basılıyken boost her karede açılıp kapanıyordu (ses ve
   alev titremesi). Artık bar boşalınca boost kilitlenir; parmağın kalkması ve
   en az 8 enerji gerekir. → `GameConfig.BOOST_REENGAGE_MIN`
4. **Perfect Dodge, combo, coin para birimi, yıldız, seviye, garaj, görev, booster**
   prototipte yoktu; bunlar ürün taslağındaki meta katmandır.
5. **Hissedilen hız ölçeği (`WORLD_SPEED_SCALE = 0.75`).** Prototipte dünya
   `speed * 250 * dt` piksel kayıyordu. Oyuncu geri bildirimi (2026-08-14):
   *"görünen hız aynı olsun ama hissedilen hız yavaşlamalı, 160 ile giderken
   bile aşırı hızlı, oynanmıyor."* Gösterge yalnızca `speed`'den hesaplandığı
   için ikisi ayrıldı: km/h aynen kaldı, dünyanın kayması 0.75 ile çarpıldı.
   Sonuç: 160 km/h artık eski 120 km/h gibi oynanır (yaklaşma hızı ve engeller
   arası ekran mesafesi o hızın değerleri). Trafik sıklığı bozulmaz — engeller
   zamana göre doğar, hıza göre değil. `PIXELS_PER_METER` de aynı çarpanla
   küçültüldü ki metre/saniye değişmesin, yani bölümlerin mesafe hedefleri
   olduğu gibi kalsın. → `GameConfig.WORLD_SPEED_SCALE`
6. **Oyuncu aracının şekli ve rengi seçilebilir.** Prototipte oyuncu aracı tek
   ve sabitti (kırmızı `#E10600`, tek gövde). Proje sahibi isteği (2026-08-14):
   *"sürülen araba şekli ve rengi customise edilebilmeli."* Artık 4 gövde
   şekli ve 9 boya var (`game/CarCatalog.kt`). **Prototipin orijinal çizimi
   `hatchback` şekli olarak birebir korundu ve varsayılan olarak duruyor** —
   mevcut kayıtlar güncellemeden sonra aracı aynı görür.

   Fizik ve çarpışma DEĞİŞMEDİ: `CAR_ART_SCALE`, `CAR_WIDTH_PX`,
   `CAR_HEIGHT_PX`, `HITBOX_SCALE` aynı; katalogdaki her şekil aynı çizim
   kutusuna (`x -20..20`, `y -2..74`) sığmak zorunda ve bunu `CarCatalogTest`
   her şekil için doğruluyor. Trafikteki engel araçları da değişmedi —
   tehdidin görünümü sabit kalmalı (bkz. `GameRenderer.drawObstacleCar`).

   > **Güncelleme (2026-08-15, sapma #11):** bu maddenin iki cümlesi artık
   > geçmişi anlatıyor. Hatchback prototipin birebir çizimi *değil* (yeniden
   > çizildi) ve trafik araçları da yenilendi. Değişmeyen kısım — çizim
   > kutusu, ölçek sabitleri ve çarpışma davranışı — hâlâ geçerli.
   >
   > **Güncelleme (2026-08-15, sapma #14):** *"Fizik değişmedi"* ifadesi de
   > artık yalnızca **çarpışma** için geçerli. Gövde seçimi bugünden itibaren
   > son hızı, ivmeyi, freni ve boost süresini etkiliyor. Çarpışma kutusu ve
   > ölçek sabitleri hâlâ hiçbir araçta değişmiyor.

7. **Menü köşe görselleri yeniden çizildi.** Prototipin menü kartında iki
   42 px'lik PNG vardı (solda araba, sağda damalı bayrak). İkisi de stok
   görseldi ve arabanın üzerinde **"Designed by pngtree" filigranı** vardı —
   lisanssız bir görsel mağaza sürümüne giremez. Yerleşim korundu, görseller
   Compose Canvas ile yeniden üretildi: bayrak `CheckeredFlagBadge`, soldaki
   araba ise oyuncunun seçtiği araç (`CarPreview`, bkz. sapma #6).
8. **Menü alt başlığı.** Prototipteki `$KRON · DRIVE, EARN & BURN` yerine
   `DRIVE · DODGE · SURVIVE` kullanıldı (proje sahibi kararı, 2026-08-13).
   Oyunda token/cüzdan/kazanç mekaniği **yoktur**; gerçek olmayan bir finansal
   vaat gibi okunabilecek metin taşınmadı.

9. **Trafik kendi hızıyla ilerliyor.** Prototipte engeller
   `speed * 250 * dt * speedMul` (speedMul 1.00–1.14) ile aşağı akıyordu,
   yol kayması ise tam olarak `speed * 250 * dt`. Yani engeller **asfaltla
   aynı hızda — hatta biraz daha hızlı** geriye gidiyordu; asfalta göre
   duruyor ya da geri geri gidiyorlardı. Proje sahibi geri bildirimi
   (2026-08-14): *"Yoldaki arabalar hareket etmiyor, park etmiş gibiler.
   Sabit bir hızla gidiyor olmalılar."*

   Artık her trafik aracının kendi ileri hızı var:

   ```
   ekrandaAşağıHız = (oyuncuHızı − aracHızı) × WORLD_PX_PER_SPEED_UNIT
   aracHızı        = kosununTabanHızı × [0.45 … 0.58]   (doğumda sabitlenir)
   ```

   Zemin yine `oyuncuHızı` ile kaydığı için araçlar asfalta göre **ileri**
   gider. `WORLD_SPEED_SCALE`, `WORLD_PX_PER_SPEED_UNIT`, `HITBOX_SCALE` ve
   çarpışma kutusu **değişmedi**; spawn aralığı da değişmedi (türetme
   `GameConfig.OBSTACLE_SPAWN_INTERVAL_SEC` yorumunda). Trafik hızı
   oyuncunun **anlık** hızına değil koşunun **taban** hızına bağlı — boost'a
   basınca gerçekten daha hızlı yaklaşılsın diye.
   → `GameConfig.TRAFFIC_SPEED_RATIO_MIN/MAX`

   Yan etki olarak trafik aracının çizimine **arka stop lambaları** eklendi
   (`GameRenderer.drawObstacleCar`). Sapma #6'daki "trafik araçları
   değişmez" kuralına bilinçli tek istisna: aracın arkasının bize dönük
   olduğu okunmadan "önümde giden araç" hissi tamamlanmıyordu. Gövde
   şekli, boyutu ve çarpışma kutusu aynı.

10. **İlk sekiz bölümün öğrenme eğrisi yeniden tasarlandı** (2026-08-14,
    sahibinin ikinci "ilk bölümler fazla zor" geri bildirimi). Prototipte
    bölüm/hedef kavramı zaten yoktu (bkz. sapma #4), yani bu prototipten
    sapma değil meta katmanın revizyonu — ama eski değerlerle karşılaştırma
    `docs/BALANCE.md` içinde tablo hâlinde duruyor. `LevelDef` yeni bir
    `trafficDensity` alanı aldı (varsayılan 1.0; 9–30. bölümler etkilenmedi).

11. **Araç çizimleri yeniden çizildi — perspektif düzeltmesi** (2026-08-15).
    Proje sahibinin geri bildirimi üç maddeydi:

    > *"Üstten bakıyoruz araçlara, stop lambası ve tampon o kadar tavan ile
    > aynı paralelde olmamalı."*
    > *"Boost ışıklandırması çok yapay, lego gibi."*
    > *"Oyundaki [araba], retro oyun olsa da çok ama çok kötü."*

    **Perspektif sözleşmesi.** Kamera aracın üstünde ve biraz arkasında.
    Görülen yüzeyin neredeyse tamamı tavan/kaput düzlemi; aracın arka yüzü
    ise tavana dik durduğu için bize doğru **kısalarak** yansır. Eski çizimde
    arka tampon 8 birimlik düz renkli bir slabdı ve stop lambaları onun
    üzerinde ayrı kutulardı — ikisi de tavanla aynı düzlemde okunuyordu.
    Artık `CarCatalog.rearFace()` üretiyor: gövde boyunun ~%7'si kadar,
    aşağı doğru kararan, hafif daralan bir trapez + tavanla arasında ince
    kırılma çizgisi + üzerinde ince stop şeritleri.

    **Gövde.** Gövde genişletildi ve tekerlekler çamurlukların altına alındı
    (eskiden dar bir gövde ve ondan kopuk dört siyah blok vardı); tavan
    omuzlardan içerde ayrı bir düzlem oldu; ön cam / tavan / arka cam ayrıldı.

    **Veri modeli genişledi, yapı korundu.** `CarPart` artık isteğe bağlı
    `gradient` ve `alpha` taşıyor; `CarGradient` mutlak renk değil **ton
    kaydırması** tutuyor, böylece aynı hacim tanımı 9 oyuncu boyasının ve 4
    trafik renginin hepsinde çalışıyor. Şekle özel Compose kodu hâlâ yok.
    Yeni boyalar: `TAIL` (stop) ve `GLOSS` (speküler vurgu).

    **Trafik araçları** artık oyuncuyla aynı çizim boru hattını kullanıyor
    (`CarCatalog.trafficShape` + `trafficStyle()`); `GameRenderer` içindeki
    elle yazılmış kopya kaldırıldı. Bu, sapma #6'daki "trafik araçları
    değişmez" kuralının bilinçli olarak gevşetilmesidir — sahibin isteği
    oyun içi araç kalitesiydi ve trafiğin geride kalması kabul edilemezdi.
    Tehdit yine iki kanaldan ayırt edilebilir: palet (gövde
    `OBSTACLE_COLORS`, sürücü mavi; oyuncu paleti bu tonların dışında) ve
    siluet (trafik gövdesi bilerek daha köşeli, burnu künt).

    **Boost alevi.** İki keskin üçgen yerine dört katman: halo (radyal
    camgöbeği sönüm) + dış/iç/çekirdek plumalar. Her pluma ucuna doğru hem
    daralır hem saydamlaşır; iki farklı frekansta sinüsle titrer. Alev
    yalnızca çizimdir, çarpışmaya girmez.

    **Değişmeyen:** `CAR_ART_SCALE`, `CAR_WIDTH_PX`, `CAR_HEIGHT_PX`,
    `HITBOX_SCALE` ve çizim kutusu (`x -20..20`, `y -2..74`). Katalogdaki
    her şekil hâlâ bu kutuya sığıyor ve `CarCatalogTest` bunu parça parça
    doğruluyor. Yani sapma #6'daki "hatchback prototipin birebir çizimidir"
    ifadesi **artık geçerli değil** (şekil yeniden çizildi), ama kutu, ölçek
    ve çarpışma davranışı birebir aynı.

    Mağaza tarafındaki `docs/play_store_assets/tools/kron_art.py` aynı
    geometrinin Python aynasıdır ve senkron tutuldu. `kron_car3d.py`
    (yalnızca feature graphic / dil ekranı) aynı perspektif düzeltmesini
    aldı; ayrıca sahibin isteğiyle **asfalta düşen zemin gölgesi kaldırıldı**
    (hale araçtan büyüktü, yakınlaştırınca leke gibi duruyordu).

    Önce/sonra karşılaştırma görseli:
    `docs/play_store_assets/previews/car_before_after.png`

11. **Yol deseninin frekansı düşürüldü.** Prototipte kerb blokları 24 px,
    şerit çizgileri 20 dolu / 20 boştu. Araç yüksekliği ~61 px olduğu için
    ekranda aynı anda ~100 kerb bloğu ve onlarca çizgi aralığı akıyordu;
    yüksek kontrastla birleşince stroboskop etkisi yapıyordu. Proje sahibi
    (2026-08-15): *"çizgiler çok sık, oynarken göz çok yoruluyor."*

    Desen aynı, frekansı seyrek: kerb bloğu 24 → 46 px, şerit çizgisi
    20/20 → 42/54. Kontrast da bir tık yumuşatıldı (kerb beyazı `#EFEFEF`
    → `#DCE2E9`, kırmızısı `#D62828` → `#C8393B`; çizgi %92 → %76 beyaz).
    Hız hissi DEĞİŞMEDİ — o ayrı bir düğme (`WORLD_SPEED_SCALE`).
    → `GameConfig.KERB_BLOCK_HEIGHT_PX`, `LANE_DASH_ON_PX`, `LANE_DASH_OFF_PX`

12. **Garaja iki gövde daha eklendi** (2026-08-15, proje sahibi kararı):
    **Kuş SLX** (1980'ler Türk sedanı) ve **Dağ Keçisi** (station wagon).
    Prototipte tek gövde vardı (bkz. sapma #6), yani bu prototipten sapma
    değil o sapmanın genişlemesi.

    **İkisi de saf kozmetik.** Sapma #6'nın değişmez kısmı aynen geçerli:
    `CAR_ART_SCALE`, `CAR_WIDTH_PX`, `CAR_HEIGHT_PX`, `HITBOX_SCALE` ve çizim
    kutusu (`x -20..20`, `y -2..74`) değişmedi; her iki gövde de kutuyu
    **birebir** dolduruyor ve `CarCatalogTest` bunu parça parça doğruluyor.
    Hiçbir gövde oyuna avantaj/dezavantaj taşımıyor.

    > **Güncelleme (2026-08-15, sapma #14):** son cümle artık geçerli değil —
    > her gövdenin dört sürüş çarpanı var. Çizim kutusu ve çarpışma yine
    > değişmiyor.

    Sadece `game/CarCatalog.kt` içine **veri** eklendi — çizici
    (`ui/common/CarArtwork.kt`) ve motor dosyaları değişmedi. Katalogun
    tasarım sözü buydu: yeni şekil = yeni `CarShapeDef`, yeni Compose kodu
    değil.

    **32 px'de ayırt edilebilirlik**, gövde başına tek bir taşıyıcı işaret
    üzerine kuruldu (aşırı detay okunmayı zorlaştırır):

    | Gövde | 32 px'deki taşıyıcı işaret |
    |---|---|
    | Kuş SLX | iki yanda tam boy krom şerit + burunda iki parlak dikdörtgen far |
    | Dağ Keçisi | uzun beyaz tavanın üstünde iki koyu bagaj rayı |

    **Fiyat merdiveni uzamadı**, ikisi mevcut basamakların arasına girdi.
    Proje sahibi kararıyla ikisi de **aynı basamakta** (1500, lv2): nostalji
    araçları birbirinin alternatifi, biri ötekinin ucuz sürümü olmamalı —
    seçim zevke kalsın. Merdiven: 0 → 900 → 1500 → 1500 → 1800 → 3200.

    **Beyaz sorunu.** Dağ Keçisi beyaz tasarlandı ama trafikteki engel
    renklerinden biri **tam beyaz** (`FFFFFF`, `GameEngine.OBSTACLE_COLORS`).
    Sanat yönü kuralı gereği (tehdit rengi başka hiçbir şeyde kullanılmaz)
    oyuncu beyazı kırık ve soğuk seçildi: `EDF1F5` gövde, `98A2AE` gölge,
    `2A3340` şerit. Ayrım ayrıca sürücü başından da okunuyor (oyuncu sarı,
    trafik mavi) ve bagaj rayları trafikte hiç bulunmuyor.

    Mağaza tarafındaki Python aynası (`kron_art.py`) senkron tutuldu.
    Karşılaştırma görseli — altı gövde, 32 px + 4×, ayrıca beyaz oyuncu
    aracı vs beyaz trafik denetimi:
    `docs/play_store_assets/previews/car_lineup_6.png`
    (üreten: `docs/play_store_assets/tools/car_lineup.py` — bu betik artık
    yedi gövdelik `car_lineup_7.png` üretiyor, bkz. sapma #13)

13. **Garaja yedinci gövde: ikinci kas arabası** (2026-08-15, proje sahibi
    kararı). Sapma #12'nin devamı; aynı değişmez kısım geçerli:
    `CAR_ART_SCALE`, `HITBOX_SCALE` ve çizim kutusu (`x -20..20`, `y -2..74`)
    **değişmedi**, gövde kutuyu birebir dolduruyor, oyuna hiçbir
    avantaj/dezavantaj taşımıyor. Yalnızca `game/CarCatalog.kt` içine veri
    eklendi; `ui/common/CarArtwork.kt` ve motor dosyalarına dokunulmadı.

    > **Güncelleme (2026-08-15, sapma #14):** "avantaj/dezavantaj taşımıyor"
    > artık geçerli değil. Boğa 67'nin profili: son hız +10%, ivme −8%,
    > fren −10%, boost süresi +4% (gerekçe `docs/BALANCE.md`).

    **Marka sınırı (pazarlık dışı).** Proje sahibinin istediği his 60'lar/70'ler
    Amerikan kas arabası. Hiçbir tescilli marka ya da model adı ne kodda ne
    mağaza metninde geçmez ve gövde birebir kopya değildir; kullanılan şey
    **dönemin biçim dili**: uzun kaput, kısa bagaj, geniş omuz, dörtlü far,
    kaput çıkıntısı, kalın orta şerit. Kod içi kimlik bilerek nötr:
    `SHAPE_MUSCLE_67`. Ad **geçici** (`Boğa 67`); sahibi üç aday arasından
    seçtikten sonra kalıcılaşacak (adaylar: Boğa 67 / Yıldırım GT / Demirtay).

    **Mevcut Kas Arabası ile karışmaması** bu gövdenin varlık sebebi, o yüzden
    ayrım tek işarete değil beş işarete dayandırıldı (üstteki üçü 32 px'de
    okunuyor):

    | # | Boğa 67 | Kas Arabası |
    |---|---|---|
    | 1 | tek **kalın** orta şerit (9.4 birim) | iki **ince** yan şerit (3.6) |
    | 2 | kola şişesi bel (33.2 → 29.6 → 33.8) | baştan sona düz yan (33.2) |
    | 3 | kokpit arkada; kaput/bagaj 28/15.1 | kokpit önde; 24.4/21.4 |
    | 4 | fastback arka cam 8.6 | arka cam 6.2 |
    | 5 | dörtlü yuvarlak far; stoplar köşede + ortada iki krom egzoz | far yok; yanda uzun krom egzoz, stoplar ortaya kadar |

    **Fiyat: 2400 / lv5.** Merdiven uzamadı, mevcut merdivenin **en geniş
    boşluğuna** girdi: 1800 (Kas Arabası) ile 3200 (Süper Araba) arasında
    1400 coinlik bir uçurum vardı ve Kas Arabası'nı alan oyuncunun önünde
    ara hedef kalmıyordu. Yeni merdiven:
    0 → 900 → 1500 → 1500 → 1800 → **2400** → 3200.

    **Renk bağımsızlığı doğrulandı** (sahibin sorusu üzerine): gövde ve boya
    ayrı alanlarda saklanır (`carShapeId` / `carColorId`), ayrı envanterlerde
    (`ownedCarShapes` / `ownedCarColors`) ve `CarStyle` bunların serbest
    çarpımıdır. Katalogda gövdeye özel renk kısıtı **yok** — her gövde,
    oyuncunun sahip olduğu her boyayla kullanılabilir.
    `CarCatalogTest.her govde her boyayla kullanilabilir` bunu 7 × 10 = 70
    çift üzerinde doğruluyor.

    Python aynası (`kron_art.py`) senkron tutuldu. Karşılaştırma görseli —
    yedi gövde, 32 px + 4×, ayrıca **iki kas arabası yan yana** ve beyaz
    oyuncu aracı vs beyaz trafik denetimi:
    `docs/play_store_assets/previews/car_lineup_7.png`

14. **Araçlar artık kozmetik değil — dört sürüş çarpanı** (2026-08-15, proje
    sahibi kararı). Sapma #6, #12 ve #13'teki *"oyuna hiçbir avantaj/dezavantaj
    taşımıyor"* cümlesi **artık geçmişi anlatıyor**. Sahibinin tespiti:

    > *"Bu arabaların özellikleri görünmüyor garajda, yani neden süper araba
    > alsın?"*

    3200 coinlik bir aracın hiçbir etkisinin olmaması ve bunun garajda da
    yazmaması, oyuncuyu yanlış beklentiye sokuyordu. Prototipte araç seçimi
    zaten yoktu (bkz. sapma #6), yani bu prototipten sapma değil o sapmanın
    fiziğe bağlanması.

    Her `CarShapeDef` dört çarpan taşıyor (varsayılan 1.0 = eski davranış):
    `topSpeedMul`, `accelMul`, `brakeMul`, `boostMul`. Değerler ve gerekçeleri
    `docs/BALANCE.md` → "Araç özellikleri" bölümünde; oradan başka bir yerde
    denge sabiti yok.

    **Çarpanlar yükseltmelerin ÜSTÜNE uygulanır, yerine geçmez.** Fark ~%10
    bandında (0.90–1.12; test 0.80–1.25 sınırını zorunlu kılıyor). Seviye 8
    bir "Şehir", seviye 1 bir "Süper Araba"dan hâlâ açık ara hızlı — ana
    ilerleme garaj yükseltmeleri olarak kalıyor, yoksa 8 seviyelik dört dal
    anlamsızlaşırdı. `UpgradeCatalogTest` bunu doğruluyor.

    **Çarpışma kutusu HİÇBİR araçta değişmedi.** Sapma #6'nın değişmez kısmı
    aynen geçerli: `CAR_ART_SCALE`, `CAR_WIDTH_PX`, `CAR_HEIGHT_PX`,
    `HITBOX_SCALE` ve çizim kutusu (`x -20..20`, `y -2..74`) aynı. Çarpanlar
    yalnızca hedef hıza, yaklaşma oranına, fren cezasına ve boost tüketimine
    dokunuyor. `GameEngineTest.carpisma kutusu araca gore DEGISMEZ` yedi
    gövdenin hepsi için aynı çarpışma sonucunu doğruluyor.

    **Fren çarpanı bilerek `decelRate`'e uygulanmadı** — o oran frenin değil
    her aşağı yönlü yakınsamanın oranı (boost sönümlemesi dahil), ölçeklenseydi
    "freni iyi" araç boost artığını da daha çabuk kaybederdi. Gerekçenin tamamı
    `docs/BALANCE.md` içinde.

    **Garajda gösteriliyor**: araç kartında dört çubuk (HIZ / İVME / FREN /
    BOOST) + tek satırlık karakter cümlesi, kilitli araçlarda da. Çubuklar
    mutlak değil katalog içinde karşılaştırmalı (`CarCatalog.statFraction`).
    Gösterilmeyen özellik yok sayılır — sorunun yarısı zaten görünmemesiydi.

15. **Motor sesi araca göre değişiyor + korna eklendi** (2026-08-15, proje
    sahibi kararı):

    > *"Araba seslerini arabaya göre yapabilir miyiz? Mesela Boğa 67 daha
    > böyle egzozu gürültülü olur. Bir tane de korna efekti koyalım, işe
    > yaramasa da eğlencelik olur."*

    Prototipte tek bir motor sesi vardı (tek osilatör) ve korna yoktu.
    Artık her gövdenin bir **ses profili** var: temel frekans çarpanı, 2–5.
    harmonik ağırlıkları, testere "grit" payı, gürültü dokusu, rölanti
    düzensizliği (lope) derinliği/hızı, genlik, filtre açıklığı, nitro tonu
    ve korna parametreleri. Tablo `audio/CarSoundProfile.kt` içinde.

    **Neden `game/CarCatalog.kt` içinde değil**: `game/` paketi simülasyona
    ait ve saf Kotlin kalır; ses Android tarafının işi. Ayrıca bu tablo
    oynanışı **hiç** etkilemiyor (sapma #14'teki dört çarpandan tamamen
    bağımsız), iki tablonun ayrı durması "sesi değiştirdim, denge kaydı mı?"
    sorusunu baştan kaldırıyor. Eşleşme gövde **id**'si üzerinden;
    bilinmeyen id varsayılana (Şehir = eski ses) düşer.

    **Şehir aracının sesi bit bit aynı kaldı** — referans profilin tüm
    değerleri 1.0/eski sabitler. Yani değişiklikten önceki oyuncu, aracını
    değiştirmediyse hiçbir fark duymaz.

    **Boğa 67 "en gürültülü egzoz"un sayısal karşılığı** (sahibin isteği):
    en düşük temel frekans (0.76), **tek sayılı harmonikler baskın**
    (h3 0.64 / h5 0.36; h2 ve h4 bilerek zayıf — çift harmonikler yumuşak,
    tek harmonikler boğuk ve sert duyulur), en derin ve en yavaş lope
    (0.34 / 0.25 = V8 rölantisi), en yüksek genlik (1.22).
    `CarSoundProfilesTest` bu dört maddeyi kural olarak doğruluyor.

    **Kırpma profilden gelemez**: ham dalga her profilde
    `1/(1+Σharmonikler+grit)` ile normalize edilir, yükseklik farkı yalnızca
    `gainMul`'dan gelir. Karışım bütçesi motor ~0.11, nitro ~0.20,
    korna 0.30; en ağır yükte ölçülen tepe < 0.9.

    **Korna** tamamen eğlence — oynanışa hiçbir etkisi yok, oyun ekranının
    **alt ortasında** 48 dp'lik küçük bir düğme (fren/boost kümeleriyle
    aynı hizada, aralarındaki geniş boşlukta). Üstteki HUD satırına
    (duraklat + hız kilidi) konmadı: sürerken başparmakların ekranın
    tepesine uzanması gerekirdi. Kontrol kümelerinin arasına da konmadı:
    o boşluk üç kez oyuncu geri bildirimiyle daraltılmıştı. Ses kapalıyken
    düğme hiç görünmez; 0.4 sn bekleme süresi üst üste basınca sesin
    yığılmasını engeller.

    > **Güncelleme (2026-08-16, sapma #22):** korna düğmesinin **yeri
    > değişti** — artık alt ortada değil, sağ sütunda yön okunun üstünde.
    > Yukarıdaki "alt ortada, 48 dp" tarifi geçmişi anlatıyor. Sesle ilgili
    > her şey (profil tablosu, bekleme süresi, ses kapalıyken görünmemesi)
    > aynen geçerli.

    **Tır hazırlığı**: planlanan tır gövdesi için yapı değişikliği gerekmiyor,
    yalnızca tabloya bir satır — çok düşük `freqMul` (~0.62), yüksek
    `harmonic3`/`harmonic5`, yüksek `noiseAmount`, **çok düşük** `hornBaseHz`
    (~110 Hz), geniş `hornInterval` (~1.5), uzun `hornSeconds` (~1.1) ve yavaş
    `hornAttack` (~0.06) = hava kornasının basınç kurması.

16. **Araçlar sprite'a geçti — iki katmanlı, çalışma anında boyanan çizimler**
    (2026-08-15, proje sahibi kararı). Prototipte ve 15 Ağustos'a kadar
    buradaki her araç **Canvas'ta vektör olarak** çiziliyordu: gövde
    geometrisi `CarCatalog.kt` içinde veriydi, `CarArtwork.kt` onu kutulara
    ve poligonlara çeviriyordu. Proje sahibi ChatGPT ile tepeden bakış
    araç görselleri üretti (`incoming/car_refs/`, 15 gövde + maske) ve
    bunların oyuna alınmasını istedi.

    **Neden iki katman.** 7 gövde × 10 boya = 70 hazır PNG demekti. Bunun
    yerine her gövde ikiye ayrılıyor: `_body` gri tonlamalı boyanabilir alan,
    `_detail` cam/lastik/far/şerit. Çizerken gövde seçili boyayla **çarpılıyor**
    (`BlendMode.Modulate`), üstüne detay geliyor. Tek dosya 10 boyayı da
    veriyor. Ayrımı proje sahibinin ürettiği maskeler söylüyor (gövde
    magenta); maskeler kayıplı sıkıştırmadan geçtiği için sert eşik değil
    yumuşak bir kroma ağırlığı kullanılıyor — sert eşik kenarlara merdiven
    yapıyordu.

    **Referans tonu eşitleniyor.** Referans çizimlerin gövde parlaklığı
    birbirinden çok farklı (beyaz station wagon ortanca 238, siyah kas
    arabası 41). Çarpım boyamada bu fark doğrudan "boyayı göremiyorum"a
    dönüşüyordu: siyah referans hangi boya seçilirse seçilsin siyah
    kalıyordu. Her gövdenin ortancası gamma ile 205'e çekiliyor; gamma
    0.40'ta tabanlanıyor çünkü daha fazla germek sıkıştırma gürültüsünü de
    büyütüyor.

    **Çarpışma kutusu DEĞİŞMEDİ.** Sprite'lar çizim kutusuyla aynı orana
    (40:76) üretiliyor ve tam o kutuya oturtuluyor; kutu kuralı (bkz. #11)
    aynen geçerli.

    **Vektör yolu silinmedi.** Sprite'ı olmayan bir gövde için çizici
    `drawCarParts`'a düşer; mağaza görselleri de her iki yolu kullanabiliyor.

    **Ölçüldü** (Samsung S8, aynı bölüm, aynı süre): kare süresi medyanı
    sprite 24–27 ms, vektör 25 ms — regresyon yok, p90 sprite'ta biraz daha
    iyi (32–38 ms / 36 ms). Oyunun ~%95 janky frame'i sprite'tan gelmiyor,
    vektörde de var; ayrı bir iş. Sprite'lar süreç boyunca **tek kopya**
    tutuluyor: ilk kurulumda her `CarPreview` çağrı noktası 16 sprite'i
    baştan çözüyordu ve toplam PSS 165 MB'den 302 MB'ye çıkmıştı.

    APK'ya eklenen: 8 gövde × 2 katman = 552 KB (WebP, 240×456).

    > **Düzeltme (2026-08-16) — yukarıdaki "Ölçüldü" paragrafı DAYANAKSIZ.**
    > O A/B karşılaştırması geçersizdir: tema her koşuda **rastgele** seçiliyor
    > (`GameEngine.kt:100`) ve temalar arasında kare başına çizim çağrısı
    > **~185 ile ~780 arasında (4 kat)** değişiyor. Ölçüm temayı sabitlemediği
    > için "sprite 24–27 ms / vektör 25 ms" iki farklı temanın karşılaştırması
    > da olabilir — sayılar bir şey kanıtlamıyor.
    >
    > Sprite kararı **mimari gerekçeyle** duruyor: hazır bir bitmap'i
    > kompozitlemek, her karede onlarca poligon + gradyan fırçası kurmaktan az
    > iş. Ama **elde temiz bir performans ölçümü YOK** ve "regresyon yok"
    > iddiası geri çekilmiştir. p90 sayıları (32–38 ms / 36 ms) da aynı sebeple
    > geçersiz. Bellek gözlemi (`CarPreview` başına 16 sprite'in yeniden
    > çözülmesi, PSS 165 → 302 MB) tema seçiminden bağımsız olduğu için
    > geçerliliğini korur.
    >
    > **Bundan sonraki her performans ölçümünde tema sabitlenmelidir** —
    > sabitlenmemiş bir ölçüm bu projede kanıt sayılmaz.

17. **Her aracın bir fabrika boyası var** (2026-08-15, proje sahibi kararı:
    *"kuş slx in default rengi bu olacak dağ keçisinin ise beyaz
    (değiştirilebilir olsun ama default u bu olsun)"*).

    Referans çizimler geldiğinde ortaya çıkan sorun: Kuş SLX petrol yeşili,
    Dağ Keçisi beyaz tasarlanmıştı ama bu boyalar katalogda **ücretliydi**
    (Petrol 1400 coin / Sv 3, Buzul Beyazı 500). Aracı alan oyuncu onu
    tasarlandığı renkte görmeden önce bir 1400 daha ödemek zorunda kalıyordu.

    Kural: **gövdeye sahip olmak fabrika boyasını da açar**
    (`CarCatalog.effectiveOwnedColors`). Boya gövdeye kilitli değil, hediye —
    açıldıktan sonra başka gövdelere de sürülebilir.

    Boya artık **araç başına** hatırlanıyor (`car_color_by_shape`); gövde
    değiştirmek o aracın en son rengini, hiç boyanmadıysa fabrika boyasını
    getiriyor. Eski tek anahtarlı kayıt (`car_color`) hâlâ okunuyor ki sürüm
    yükselten oyuncunun rengi kaybolmasın — ama **yalnızca fabrika boyası
    varsayılan olan gövdelerde**; bu iki araç sürüm yükseltmede de kendi
    renginde görünsün diye istisna tutuldu.

    Garajdaki gövde çipleri artık **kendi fabrika boyalarıyla** çiziliyor
    (seçili olan hariç): çipler bir vitrin, oyuncu Kuş SLX'i almadan önce
    onun petrol yeşili olduğunu görmeli. Hepsini seçili boyayla çizmek bütün
    vitrini tek renge boyuyor ve araçların kimliğini siliyordu.

18. **Şerit çizgileri artık blok blok çiziliyor — `dashPathEffect` kaldırıldı**
    (2026-08-16). Şerit kesikleri baştan beri tek bir `drawLine` + `PathEffect
    .dashPathEffect(42 dp / 54 dp)` ile çiziliyordu. Bu mekanizma **SM-G950F
    (Android 7.0, API 24) üzerinde sessizce yok sayılıyor**: hata vermiyor,
    çizgi çiziliyor — ama **düz**, kesiksiz. Yani prototipin ve sapma #11'in
    (yol deseni frekansı; belgede "11" numarası iki kez kullanılmış, kastedilen
    ikincisi) tarif ettiği kesikli şerit, en azından bu cihazda hiç
    görünmüyordu.

    Mekanizma tamamen kaldırıldı. Kesikler artık kerb bloklarıyla **aynı
    yöntemle** — dünya konumundan türeyen ayrık `drawRect` çağrılarıyla —
    çiziliyor. Aynı düzeltme `LanguageGateScreen`'e de uygulandı (orada da
    aynı efekt kullanılıyordu).

    **Cihazda ölçüldü** (SM-G950F): dolu 126 px / boş 162 px, yani ayarlı
    42 dp / 54 dp değerleri birebir. Sabitler değişmedi
    (`GameConfig.LANE_DASH_ON_PX`, `LANE_DASH_OFF_PX`) — değişen yalnızca
    çizim yöntemi.

    **DÜRÜSTLÜK NOTU — hangi değişikliğin bozduğu CEVAPSIZ.** Proje sahibi
    "önceden kesikliydi, yeni bozuldu" dedi ve 16:04'te derlenmiş bir APK'dan
    kesikli şeritlerin göründüğü bir ekran görüntüsü gönderdi. Buna karşılık:
    (a) commit geçmişinde `dashPathEffect` mekanizması **baseline'dan beri
    değişmemiş**; (b) `builds/` altındaki eski arşiv APK'sı **aynı cihazda
    düz çiziyor**. O 16:04 APK'sı commit edilmemiş bir çalışma ağacından
    derlendiği için karşılaştırılabilir bir diff'i yok. İki gözlem
    uzlaştırılamadı; bu belgeye **hiçbir commit suçlaması yazılmamıştır**.
    Bilinen tek doğrulanmış gerçek şu: kaldırılan mekanizma bu cihazda
    çalışmıyordu, yerine konan mekanizma çalışıyor.

19. **Yol kenarı desenlerinin akış yönü düzeltildi** (2026-08-16). Beş desen
    araç ilerlerken **yukarı** akıyordu: çimen çizgileri, plaj köpüğü, seyirci
    bayrakları, gece çizgileri ve gece şehir ışıkları. Aynı karede şerit
    çizgileri ve asfalt dokusu **aşağı** akıyordu — yani ekranın ortası ileri,
    kenarları geri gidiyordu. Baseline'dan beri var olan bir kusur; prototipten
    bilinçli bir sapma değil, taşıma hatası.

    Beş desenin de kaydırma işareti çevrildi. **Cihazda ölçüldü** (ardışık
    kare farklarının çapraz-korelasyonu, SM-G950F): çimen deseni +16 px/kare
    aşağı, şerit çizgileri +57 px/kare aşağı. İki hızın farklı olması
    **kasıtlı** — kenar katmanı yola göre daha yavaş akar, derinlik hissi
    bundan doğar (bkz. sapma #20'nin tasarım ilkesi).

20. **Kerb GERÇEKTEN kayıyor artık — ve hızı yola eşitlendi** (2026-08-16,
    proje sahibi tespiti). İki ayrı kusur üst üste binmişti:

    **(a) Kerb hiç kaymıyordu, yerinde faz atlıyordu.** Bloklar ekranda
    **sabit bir ızgaraya** çiziliyordu; `roadOffset` blokların konumunu değil
    yalnızca **rengini** (kırmızı/beyaz sırasını) çeviriyordu. Sonuç: kenar
    şeridi kaymıyor, olduğu yerde renk değiştiriyordu.
    **Cihazda ölçüldü:** kırmızı/beyaz sınırları kare kare **aynı y'lerde**
    duruyordu (+0 px/kare). Düzeltmeden sonra +38 px/kare aşağı.

    **(b) Hız çarpanı 1/12 idi → 1.00 yapıldı.** Proje sahibinin gerekçesi:

    > *"kerbler aslında sabit, araba yanından geçiyor; o hissi vermek için
    > hareket ettiriyorsun. Kerb hızı araba ile aynı olmalı ki araba
    > hızlandıkça hızlansın, yavaşladıkça yavaşlasın — kerb sabit kalırsa bu
    > his kaybolur."*

    **Tasarım ilkesi (bundan sonrası için kural):** paralaks **derinlikten**
    doğar, yanal mesafeden değil. Kerb yola **boyalıdır** — kameradan şerit
    çizgisiyle tam olarak aynı uzaklıkta. Aynı uzaklıktaki iki desen farklı
    hızda akarsa yol düzlemi eğrilmiş gibi okunur. Kenar **katmanlarının**
    (çimen, seyirci, şehir — bkz. #19) yavaş akması ise doğru: onlar gerçekten
    daha uzakta.

    **Blok boyu 46 → 50 px** (`GameConfig.KERB_BLOCK_HEIGHT_PX`; 46 sapma
    #11'den geliyordu). Gerekçe: hız 12 katına çıkınca desenin göz üzerindeki
    titreşim frekansı da 12 katına çıkar. 50 px ile en yüksek hızda
    (~1650 px/s) geçiş oranı **16.5 Hz**. Üst sınır olarak şerit çizgilerinin
    aynı koşuldaki **17.2 Hz**'i alındı — çünkü o desen 1.00 çarpanda **zaten
    çalışıyor** ve sahibi görünümünü onaylamış. Yani kabul ölçütü teorik bir
    eşik değil, projede halihazırda onaylanmış bir referans.

    **Periyot bilerek şeritten farklı.** Kerb periyodu 100 px (50 kırmızı +
    50 beyaz), şerit periyodu 96 px (42 + 54). Eşitlenirse yol düzleminin
    tamamı aynı anda "tık" yapar; 100/96 sürüklenmesi bunu dağıtıyor.

    **Sıra önemliydi:** (a) düzeltilmeden (b) yapılsaydı iş **kötüleşirdi** —
    ortaya kayan bir bordür değil, saniyede birkaç kez topluca yanıp sönen bir
    bordür çıkardı.

21. **Seyirci teması renk titremesi** (2026-08-16). Seyircilerin rengi kayan
    **ekran** y'sinden hesaplanıyordu; kalabalık kaydıkça her seyirci her karede
    renk değiştiriyordu. Renk artık **dünya** y'sine bağlı, yani bir seyirci
    ömrü boyunca aynı renkte kalıyor. Ayrıca indeks hesabındaki `%` operatörü
    negatif dünya koordinatlarında negatif dönüp bütün kalabalığı ilk renge
    (siyah) düşürüyordu → `mod`.

    **İkinci düzeltme (kalıntı titreme).** İndeks anahtarı `x + rowKey / 6f`
    **daima tam sayıya oturuyor** (rowKey 42'nin katı → /6 tam; x 3'ün katı).
    `floor()` bıçak sırtında kaldığı için kayan nokta epsilon'u indeksi bir
    öteye kaydırıyor, seyirci renk değiştiriyordu. `roundToInt()` ile hesap
    epsilon'a bağışık hale getirildi.

    **Doğrulama sınırı:** düzeltme kod düzeyinde ve JVM davranışıyla
    gerekçelendirildi ve cihazda **ölçüldü**: tema geçici olarak CROWD'a
    sabitlenip üç ardışık kare karşılaştırıldı — üst bantta %0.3, alt bantta
    %0.0 renk değişimi. Düzeltmeden önce alt bantta ~%20 idi. Tema sabiti
    ölçümden sonra geri alındı (tema yine rastgele).

22. **Kontrol tuşlarının emoji glifleri kaldırıldı** (2026-08-16, proje sahibi
    isteği). Dört kontrol tuşu metin gliflerini kullanıyordu: `◀` `▶` `⚡` `📣`.
    Üç sorun: (1) bu glifler her Android sürümünde farklı bir yazı tipinden
    çiziliyor ve hedef cihaz API 24 — görünüm garanti edilemiyor; (2) emoji
    yazı tipi **kendi renkleriyle** geliyor, `tint` uygulanmıyor; (3) sonuç
    butonların düz beyaz görsel diliyle uyuşmuyordu.

    İkonlar artık **Compose Canvas ile çiziliyor**: dolu, köşeleri yuvarlatılmış
    üçgen yön okları; **sarı** şimşek (`KronColors.AccentBright`); ampullü
    korna. Şimşeğin sarısı bilinçli — boost tek "enerji" göstergesiyle aynı
    rengi taşımalı.

    **Referans görseller GÖMÜLMEDİ.** Proje sahibi ne istediğini anlatmak için
    referans görsel gönderdi; görseller stok/clipart olduğu için (bkz. sapma
    #7'deki pngtree filigranı olayı) projeye alınmadı — bakılıp aynı biçim
    kendimiz çizildi.

    **Korna yeri değişti**: alt ortadan (sapma #15) sağ sütuna, yön okunun
    **üstüne** alındı. Sahibi: *"zaten bir fonksiyonu yok."* Yani korna artık
    ayrı bir kontrol bölgesi işgal etmiyor, mevcut sağ kümeye eklendi. Kornanın
    oynanışa etkisizliği değişmedi.

23. **Boğa 67'nin fabrika boyası siyah** (2026-08-16, proje sahibi kararı) —
    `COLOR_MIDNIGHT`. Sapma #17'nin ("her aracın bir fabrika boyası var")
    üçüncü uygulaması; kural aynı: gövdeye sahip olmak fabrika boyasını da
    açar, boya gövdeye kilitli değil.

    **Okunabilirlik denetlendi:** en koyu gece zemininde bile araç kayboluyor
    mu diye bakıldı — kaybolmuyor, siluet iki işaretten taşınıyor: gövdedeki
    **beyaz çift şerit** ve camlar. Bu **gözle denetimdir**, ölçülmüş bir
    kontrast değeri değildir.

    (Not: sapma #13'ün siluet tablosu Boğa 67'yi "tek kalın orta şerit" diye
    tarif ediyor. O tablo **vektör** geometrisini anlatıyordu; sprite'a geçişte
    (sapma #16) şerit çift beyaz olarak çizildi. Tablo bu ayrıntıda artık
    güncel değil, ayırt edici işaretlerin geri kalanı geçerli.)

## Web3 durumu

**KAPALI.** Blockchain, cüzdan, token, NFT veya play-to-earn işlevi yok ve
proje sahibi açıkça istemedikçe eklenmeyecek (CLAUDE.md §6).

## Ses varlıkları

Projede **hiç ses dosyası yok** — motor, nitro ve korna dahil her şey çalışma
anında sentezleniyor. APK'ya eklenen ses baytı: **0**.

Kod üç parçaya ayrılmış durumda (2026-08-15):

| Dosya | Ne yapar | Android? |
|---|---|---|
| `audio/CarSoundProfile.kt` | Gövde başına ses kimliği tablosu | hayır (saf Kotlin) |
| `audio/EngineVoice.kt` | Sentezin tamamı: motor + nitro + korna | hayır (saf Kotlin) |
| `audio/EngineSoundManager.kt` | `AudioTrack` köprüsü, ses thread'i, yaşam döngüsü | evet |

Bu ayrımın sebebi doğrulanabilirlik: bu makinede hoparlör ve adb yok, sesi
*dinleyerek* doğrulamak mümkün değil. Sentez Android'den ayrı durunca üretilen
örneklerin ölçülebilir özellikleri (kırpma yok, profiller gerçekten farklı
çıktı veriyor, korna bekleme süresi çalışıyor, ses kapalıyken çıkış tam
sessiz) JVM testiyle doğrulanabiliyor — `EngineVoiceTest` ve
`CarSoundProfilesTest`.

Sesin **hoş olup olmadığı** hâlâ bu makinede doğrulanamaz; o karar proje
sahibinin kulağına ait.
