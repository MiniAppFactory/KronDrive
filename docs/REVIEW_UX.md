# Arayüz ve akış incelemesi — 2026-08-17

**Kapsam:** ekranlar, akış, okunabilirlik, erişilebilirlik.
**Yöntem:** yalnızca kod okuması + `docs/play_store_assets/previews/level_map_mock.png`.
Cihazda hiçbir şey görülmedi (cihaz proje sahibinde), ölçüler koddaki `dp`
sabitlerinden hesaplandı. Aşağıda "hesap" diyen her madde bu anlamda tahmindir.

Kod değiştirilmedi, gradle çalıştırılmadı.

---

## Önce: iyi çalışan yerler

Bunları değiştirmeye gerek yok, incelemenin sonucu bu maddeleri de kapsıyor.

**Garaj, 16 Ağustos düzeltmelerinden sonra doğru anlatıyor.** Soru 5'in cevabı
büyük ölçüde "evet". `statusText` (GarageScreen.kt:724) seviye kilidini
*fiyatla birlikte* yazıyor (`Sv 2 · 900`), `UnlockAction` üç durumu ayırıyor
(LEVEL_LOCKED / TOO_EXPENSIVE / AFFORDABLE), satın alma butonu para yetmiyorsa
hiç tıklanmıyor, kilitli içeriğe dokunmak satın almıyor sadece önizliyor ve
fabrika boyası için ikinci bir satın alma satırı gösterilmiyor. Fiyat / seviye /
sahiplik üçlüsü karışmıyor. `CarStatPanel` de "neden bu aracı alayım" sorusunu
karşılaştırmalı çubuklarla cevaplıyor.

**Renk körlüğü riski yok.** Bilgi taşıyan her yerde renge ek olarak ikinci bir
kanal var: `ObjectiveDots` yeşil dolgu **+ tik ikonu**, `TierChip` sarı zemin
**+ "+N" metni** ile tik ikonunu ayırıyor, `CarStatPanel` yüzdeleri renkle
birlikte **+/− işareti** taşıyor, boost hazır değilken renk *ve* parlaklık
düşüyor. Renk tek başına bilgi taşıyan bir yer bulamadım.

**Koşu içi HUD bilgi yükü doğru dengelenmiş.** Skor sol üst, süre + hedefler
sağ üst, boost en üstte 4 dp'lik ince şerit, hız göstergesi yolun üstünde
küçültülmüş. Hedef satırları sürüş sırasında **yer değiştirmiyor**, sadece
durum değiştiriyor (boş halka → yeşil tik) — bu yüzden göz onları "okumak"
zorunda kalmıyor, sadece kontrol ediyor. 11 sp + Black + gölge kombinasyonu
yol üzerinde okunur. Panel kaldırma kararı (2026-08-13) doğru karardı, geri
alınmasını önermiyorum.

**İlk açılış akışı fazladan sürtünme yaratmıyor.** Dil ekranı → menü →
KARİYER → bölüm → BAŞLA = **4 dokunuş**, SONSUZ modu ise **2 dokunuşta**
sürüşe giriyor. Dil ekranı bir kez çıkıyor, iki dilde birden yazıyor, iki
seçenek eşit ağırlıkta. Kaldırılmasını önermiyorum.

---

## Öneriler

### 1. Sonuç ekranı kaydırılamıyor — dar ekranda "ANA MENÜ" butonu kırpılıyor

**Ne.** `RunResultOverlay` bir `KronCard`, `OverlayScrim` içinde ortalanmış
duruyor (GameScreen.kt:1607-1617 ve :1741). Hiçbirinde `verticalScroll` yok.
Projedeki diğer dört ekranın hepsinde var (menü, garaj, ayarlar, bölüm
diyaloğu) — sonuç ekranı bu listede eksik olan tek ekran.

**Neden.** Kariyerde başarısız bir koşunun sonuç kartı en uzun hali:
başlık + "2 görev gerekli" satırı + 3 nokta + 7 istatistik satırı +
"Görev ödülü" satırı + 3 hedef satırı + reklam butonu + ANA MENÜ.
Hesap: **≈535 dp** (kart dolgusu dahil). 360×640 dp bir telefonda
scrim dolgusu ve sistem çubukları düşünce **≈528 dp** kalıyor. Kart
ortalandığı için taşma alttan ve üstten eşit kırpılıyor; en alttaki
**ANA MENÜ butonu kısmen ekran dışında kalıyor**. Yazı tipi ölçeği 1.15'te
(erişilebilirlik ayarı) taşma ~60-80 dp'ye çıkıyor ve buton tamamen kayboluyor.

S8'de (360×740 dp) ~100 dp fazladan yer var — **bu yüzden cihazda hiç
görülmemiş olması normal.** Bu hesabı 360×640 bir cihazda veya S8'de yazı
ölçeğini büyüterek doğrulamak gerekir.

Oyuncu tamamen kilitlenmiyor: sistem geri hareketi menüye dönüyor. Ama bunu
yapan oyuncu geçiş reklamını da atlıyor (madde 7).

**Dosya.** `ui/game/GameScreen.kt` — `OverlayScrim` (satır 1607).
Aynı sarmalayıcıyı `PausedOverlay`, `CrashOverlay` ve `CountdownOverlay` de
kullanıyor; onlar kısa, düzeltmeden etkilenmezler.

**Büyüklük.** Küçük — `OverlayScrim`'in içteki `Box`'ına `verticalScroll`.

**Bilinçli karar geri alınıyor mu.** Hayır. Kodda sonuç kartının
kaydırılmamasını savunan bir yorum yok; menüdeki `verticalScroll` yorumu
("360x640 cihazlarda … tek ekrana sığmaz") tam tersine bu düzeltmeyi destekliyor.

---

### 2. "Üç görevden ikisi yeter" kuralı yalnızca kaybettikten sonra söyleniyor

**Ne.** `GameConfig.MIN_STARS_TO_PASS = 2` (GameConfig.kt:408). Bu sayı
oyuncuya **sadece** sonuç ekranında, **sadece bölüm geçilemediğinde**
gösteriliyor (GameScreen.kt:1783-1796, `careerFailed` bloğu). Koşudan önce
ve koşu sırasında hiçbir yerde geçmiyor:

- `LevelDetailDialog` (LevelMapScreen.kt:552-573) "BÖLÜM GÖREVLERİ" başlığı
  altında üç görevi eşit ağırlıkta listeliyor, kaçının yeteceğini söylemiyor.
- HUD (GameScreen.kt:791-796) üç hedef satırını yine eşit gösteriyor.
- Haritadaki `StopPanel` üç nokta gösteriyor.

**Neden.** Oyuncu üç ekranda üst üste "üç görev var" mesajı alıyor, sonra
kaybedince ilk kez "iki yeterliymiş" diye okuyor. Eşik bugün 3 → 2'ye
düştüğü için sorun **büyüdü**: kural artık daha affedici ama oyuncu bunu
bilmiyor, oyunu olduğundan sert sanıyor. Sürerken de yanlış karar veriyor —
ikisini tutturmuşken üçüncüsü için risk alıp çarpıyor, oysa çıkmasa geçmişti.

**Ne öneriyorum.** Yeni bir kavram veya yeni bir görsel dil değil, tek bir
satır sayı. Bölüm diyaloğundaki "BÖLÜM GÖREVLERİ" başlığının yanına
`(2/3 yeterli)`, HUD'daki hedef bloğunun üstüne aynı kısa ibare. Metin
**sabit yazılmamalı**, sonuç ekranındaki gibi `GameConfig.MIN_STARS_TO_PASS`
ve `level.stars.size` üzerinden kurulmalı — eşik yine değişirse ekran sessizce
yalan söylemesin.

**Dosya.** `ui/levels/LevelMapScreen.kt` (:552), `ui/game/GameScreen.kt` (:791).

**Büyüklük.** Küçük–orta (iki ekran, iki metin, `AppLanguage.pick` ile).

**Bilinçli karar geri alınıyor mu.** Hayır. "Yıldız değil görev" dili
(KronComponents.kt:203-211) aynen korunuyor; eklenen şey sayaç, kavram değil.

---

### 3. HUD'daki hız kilidi butonu tek dilli — İngilizce oyuncu "HIZ" görüyor

**Ne.** GameScreen.kt:840:

```kotlin
text = if (hud.speedLocked) "🔒 HIZ" else "HIZ",
```

`AppLanguage.pick` kullanılmıyor. İngilizce oynayan oyuncu sonsuz modda
"HIZ" yazan bir buton görüyor.

**Neden.** Proje kuralı 7'nin doğrudan ihlali ("Tek dilli sabit metin yazma").
Bulduğum **tek** gerçek ihlal — dil ekranındaki iki dilli metinler kasıtlı ve
belgeli (LanguageGateScreen.kt:66-68), `"COMBO ×N"` ve `"PERFECT DODGE"` ise
oyun terimi olarak iki dilde de aynı bırakılmış, onlara dokunmayın.

**Ayrıca:** `🔒` bir emoji. Kodun kendisi 2026-08-16'da tam bu sebeple bütün
kontrol ikonlarını emojiden Canvas çizimine geçirdi (GameScreen.kt:991-1005:
*"emoji … sistem onu kendi renginde çiziyor, biz rengini belirleyemiyoruz"*).
Kilit emojisi aynı sınıfın son kalıntısı. Metin `KİLİTLİ HIZ` / `SPEED LOCKED`
gibi yazıya çevrilirse emoji de gerekmez.

**Dosya.** `ui/game/GameScreen.kt:840`.

**Büyüklük.** Küçük (tek satır).

**Bilinçli karar geri alınıyor mu.** Hayır — bu bir gözden kaçma.

---

### 4. Ödüllü reklam yüklenemeyince sonuç ekranı ve garaj hiçbir şey demiyor

**Ne.** İki çağrı yerinde `onFailure` boş:

- Sonuç ekranı, coin ikiye katlama: `GameScreen.kt:564` → `onFailure = { }`
- Garaj, bedava coin: `AppNavigation.kt:110` → `onFailure = { }`

`RewardedAdManager` yükleme hatasında `onAdClosed`'u da çağırıyor
(RewardedAdManager.kt:43-46), yani buton "YÜKLENİYOR…"da **asılı kalmıyor** —
eski etiketine geri dönüyor. Takılma yok, ama **açıklama da yok.**

**Neden.** Oyuncu "REKLAM İZLE → +150 COIN"e basıyor; buton bir an
"YÜKLENİYOR…" olup normale dönüyor, coin gelmiyor, ekran susuyor. Okunuşu
"buton bozuk" veya "oyun beni kandırdı". Uçakta / zayıf bağlantıda bu en
sık karşılaşılacak durum.

**Çözüm zaten kodda var.** `CrashOverlay` (GameScreen.kt:1717-1728) aynı
durumu doğru ele alıyor: `adFailed` bayrağı + *"Reklam yüklenemedi. İnternet
bağlantını kontrol et."*. Aynı desen diğer iki yere taşınmalı; yeni bir
tasarım gerekmiyor.

**Dosya.** `ui/game/GameScreen.kt` (:564 ve `RunResultOverlay`),
`ui/navigation/AppNavigation.kt` (:110) + `ui/garage/GarageScreen.kt`
(`FreeCoinsCard`'a bir `adFailed` parametresi).

**Büyüklük.** Küçük.

**Bilinçli karar geri alınıyor mu.** Hayır. "Reklam oyunu asla bloklamaz"
kuralı korunuyor — eklenen şey sessiz başarısızlığın yerine bir cümle.

---

### 5. Hız kilidi butonu 36 dp — bugün 48'e çıkarılan duraklat tuşunun tam yanında

**Ne.** GameScreen.kt:826-828:

```kotlin
.heightIn(min = 36.dp)
.background(...)
.padding(horizontal = 10.dp)
```

Yükseklik 36 dp, genişlik metinden geliyor ("HIZ" + 10 dp yatay dolgu ≈ 45 dp).
İki eksende de 48 dp'nin altında.

**Neden.** Bu tuş, bugün 36 → 48 dp'ye çıkarılan duraklat tuşunun **yanında**,
aynı HUD satırında duruyor. O değişikliğin gerekçesi kodda yazılı
(GameScreen.kt:806-809): *"36 dp, Android'in önerdiği 48 dp'lik asgari dokunma
hedefinin ALTINDAYDI — ekranın tepesinde, sürüş sırasında ve tek elle basılan
bir tuş için fazla küçüktü."* Gerekçenin her kelimesi yanındaki tuş için de
geçerli; sadece o tuş güncellenmiş. Sonsuz modda uzun süre sürerken hızı
sabitlemek isteyen oyuncu, ıskalayınca duraklat tuşuna basma riski taşıyor —
iki tuş arası 8 dp.

**Dosya.** `ui/game/GameScreen.kt:826`.

**Büyüklük.** Küçük.

**Bilinçli karar geri alınıyor mu.** Hayır, tersine: bugünkü kararı yarım
kalmış olduğu yerde tamamlıyor.

---

### 6. Geri tuşu 40 dp; en küçük metinler 9 sp

**Ne — geri tuşu.** `KronScreen`'in başlık satırındaki geri butonu 40 dp
(KronComponents.kt:78-91). Bu buton **Kariyer, Garaj, Görevler ve Ayarlar**
ekranlarının hepsinde tek geri dönüş yolu. 48 dp'nin altında.

**Ne — 9 sp metinler.** Garajda üç yerde 9 sp kullanılıyor:
`CarStatPanel` etiketleri (GarageScreen.kt:510), yüzde açıklama satırı (:547),
`ShapeChip` ve `ColorSwatch` durum metinleri (:668, :719). Bunların çoğu
`TextMuted` (#8092AB) renginde. Kontrast oranı `Surface` üzerinde ≈5:1, yani
AA'yı **geçiyor** — sorun kontrast değil, boyut: 9 sp Android'in önerdiği
12 sp gövde alt sınırının belirgin biçimde altında ve bu metinler
*fiyat/sahiplik durumu* gibi karar verdiren bilgiler taşıyor.

**Neden.** İkisi de "hata" değil, ama ikisi de aynı yerden geliyor: 48 dp ve
12 sp tabanları projede bilinçli olarak tutulmuş (`MIN_TOUCH = 48.dp`
GarageScreen.kt:61, `ROW_MIN_HEIGHT = 56.dp` SettingsScreen.kt:37, durak
çapı 56 dp LevelMapScreen.kt:312-315, `TIER_CHIP_HEIGHT = 48.dp`) — bu iki
yer o kuralın dışında kalmış.

**Dosya.** `ui/common/KronComponents.kt:80`, `ui/garage/GarageScreen.kt`
(:510, :547, :668, :719).

**Büyüklük.** Geri tuşu küçük. 9 sp metinler orta — `ShapeChip` 68 dp genişlikte
ve `CarStatPanel` dört satır hizalı, punto büyütmek yerleşimi etkiler, cihazda
bakmadan büyütmeyin.

**Bilinçli karar geri alınıyor mu.** Hayır. Ama 9 sp tarafı **cihazda
görülmeden dokunulmamalı** — dar cipte taşma yaratabilir.

---

### 7. Sürüş sırasında sistem geri tuşu koşuyu sessizce siliyor

**Ne.** Projede hiç `BackHandler` yok (tüm `src/main` tarandı). Oyun
ekranında sistem geri hareketi doğrudan `navController` seviyesinde işleniyor
ve `GAME` hedefini backstack'ten atıyor.

**Neden.** Koşu ortasında geri hareketi yapan oyuncu:
- onay ekranı görmüyor,
- sonuç ekranını görmüyor,
- o koşuda topladığı coin'i, XP'yi ve görev ilerlemesini kaybediyor
  (`viewModel.onRunFinished` hiç çağrılmıyor).

Bu, `PausedOverlay`'in ÇIKIŞ yolunun tam tersi: orası koşuyu `finish()` ile
düzgün kapatıyor, sonucu yayınlıyor ve geçiş reklamından geçiriyor
(GameScreen.kt:478-484, yorumda: *"reklamsız çıkış yolu bırakılmıyor
(sahibi kararı, 2026-08-14)"*). Sistem geri tuşu o kararın etrafından
dolaşıyor. `docs/CHANGELOG.md`'deki 2026-08-16 (2) maddesi "çarp → ana menü →
tekrar" kaçağını kapatmış; bu kaçak daha ucuz, çünkü çarpmayı bile gerektirmiyor.

**Not.** Reklam/gelir tarafı benim alanım değil, ürün ve regresyon ajanlarının
kapsamına giriyor olabilir. Buraya **akış kusuru** olarak yazıyorum: oyuncu
kazandığı ilerlemeyi uyarısız kaybediyor. Madde 1 ile de bağlantılı — sonuç
ekranındaki ANA MENÜ kırpılırsa oyuncunun kalan tek çıkışı zaten bu yol oluyor.

**Dosya.** `ui/game/GameScreen.kt` (bir `BackHandler`, koşu sırasında
duraklatmaya bağlanabilir).

**Büyüklük.** Küçük–orta.

**Bilinçli karar geri alınıyor mu.** Hayır — aksine 2026-08-14 kararını
kapatılmamış bir delikte uyguluyor.

---

### 8. Ayarlar "Sürüm 1.0.0" diyor, build 1.0.9

**Ne.** `SettingsScreen.kt:233` sürümü sabit metin olarak yazıyor:

```kotlin
text = language.pick(tr = "Sürüm 1.0.0", en = "Version 1.0.0"),
```

`source/app/build.gradle.kts` ise `versionName = "1.0.9"`, `versionCode = 10`.

**Neden.** Küçük ama görünür: destek yazışmasında oyuncunun bildireceği sürüm
yanlış olur ve her sürümde elle güncellenmesi gereken bir sabit olarak kalır.
`BuildConfig.VERSION_NAME`'den okunmalı.

**Dosya.** `ui/settings/SettingsScreen.kt:233`.

**Büyüklük.** Küçük.

**Bilinçli karar geri alınıyor mu.** Hayır.

---

## Soruların kısa cevapları

**1. İlk açılış.** 4 dokunuşta kariyer, 2 dokunuşta sonsuz sürüş. Dil ekranı
gereksiz sürtünme yaratmıyor — bir kez çıkıyor, iki dilde birden yazıyor,
arkasında oyunun kendi pisti duruyor. Değiştirmeyin.

**2. Ana menü.** Hiyerarşi doğru: KARİYER tek dolu (birincil) buton ve altında
"Bölüm 7" yazıyor, SONSUZ rekoru, GÜNLÜK GÖREV kademe sayacını ve sıradaki
hedefi taşıyor. "Şimdi ne yapmalıyım" sorusu KARİYER butonunun alt satırından
cevaplanıyor. GÖREVLER'de alınabilir ödül varsa kırmızı nokta çıkıyor. Rozetler
(COIN / GÖREV / ARAÇ) durum, butonlar eylem — ayrım net. Sorun bulmadım.

**3. Koşu içi HUD.** Aynı anda 6 bilgi: skor, süre, 3 hedef satırı, boost
şeridi, hız göstergesi, (varsa) combo. Hepsi gerekli ve hepsi ayrı köşede.
Hedef satırları sürüşte okunabilir — konumları sabit, yalnızca durumları
değişiyor. Tek eksik "ikisi yeter" bilgisi (madde 2).

**4. Sonuç ekranı.** Başlık dürüst ("GÖREVLER EKSİK", "TAMAMLANDI" değil),
eşik metni sabit değil `MIN_STARS_TO_PASS`'ten türetiliyor, her hedefin yanında
"1280/1400" ilerlemesi var, alınmış görev ödülü "alınmıştı" diye ayrılıyor.
**Bugünkü 3 → 2 değişikliğini doğru anlatıyor.** Tek sorunu kaydırılamaması
(madde 1) ve kuralın sadece burada söylenmesi (madde 2).

**5. Garaj.** Doğru çalışıyor, yukarıda ayrıntılı yazdım. Sadece 9 sp metinler
(madde 6) not düşülecek kadar küçük.

**6. Erişilebilirlik / dar ekran.** 48 dp'yi tutmayan iki yer: hız kilidi
(madde 5) ve geri tuşu (madde 6). Uzun Türkçe metinlerde taşma koruması
düşünülmüş (`StopPanel`'de `maxLines=1` + `Ellipsis`, `BoosterChip`'te
`maxLines=2`, 2×2 booster düzeni, menü butonlarında sabit yükseklik yerine
`heightIn(min=)`). Renk tek başına bilgi taşımıyor. Kontrast yeterli.
`level_map_mock.png`'de "HAYATTA KAL" başlığının asfaltın altına girdiği
görülüyor ama kodda bunun düzeltildiği yazılı (LevelMapScreen.kt:249-256) —
**cihazda doğrulanmadı**, 360 dp genişlikte bakmakta fayda var.

**7. Boş/hata/yükleniyor.** Çarpışma sonrası reklam hatası doğru anlatılıyor.
Günlük görev bitince buton kapanmıyor, "✓" ile işaretlenip soluklaşıyor ve
tekrar oynanabiliyor — doğru karar. Coin yetmediğinde satın alma butonu pasif
ve satırda "yeterli coin yok" yazıyor. Günlük reklam sınırı dolduğunda buton
yerine açıklama geliyor. **Tek boşluk:** sonuç ekranı ve garajda reklam
yüklenemediğinde hiçbir şey denmiyor (madde 4).

---

## Doğrulanmayanlar

- Hiçbir ekran cihazda görülmedi; bütün ölçüler koddaki `dp` sabitlerinden.
- Madde 1'deki 535 dp hesabı bir tahmindir. 360×640 bir cihazda veya S8'de
  yazı tipi ölçeği büyütülerek sınanmalı.
- Madde 6'daki 9 sp metinlerin gerçekte okunup okunmadığı ölçülmedi.
- `level_map_mock.png` bir mockup, derlenmiş ekran değil.
