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

## Web3 durumu

**KAPALI.** Blockchain, cüzdan, token, NFT veya play-to-earn işlevi yok ve
proje sahibi açıkça istemedikçe eklenmeyecek (CLAUDE.md §6).

## Ses varlıkları

Projede **hiç ses dosyası yok**. Motor sesi ve boost efekti, prototipteki
WebAudio osilatörlerinin birebir karşılığı olarak `AudioTrack` ile çalışma
anında sentezleniyor (`audio/EngineSoundManager.kt`).
