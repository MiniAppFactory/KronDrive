# Değişiklik günlüğü

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
