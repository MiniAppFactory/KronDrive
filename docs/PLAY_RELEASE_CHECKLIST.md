# Play Store yayın hazırlık denetimi — Kron Drive (versionCode 10, versionName 1.0.9)

Denetim tarihi: **2026-08-17** (önceki: 2026-08-16) · Denetleyen: `play-store-compliance-engineer`
Kapsam: yalnızca denetim ve `docs/` altındaki belgeler. **Kod değiştirilmedi,
Gradle çalıştırılmadı, cihaza dokunulmadı, hiçbir şey yayınlanmadı.**

> **Adım adım ne yapılacağını arıyorsan buraya değil, `PLAY_SUBMISSION_ORDER.md`'ye bak.**
> Bu dosya **denetim tablosudur** (ne kapalı, ne açık, kanıtı ne).
> Arka plandaki uzun analizler yerinde: `STORE_SUBMISSION_CHECKLIST.md`
> (politika analizi), `DATA_SAFETY_FORM.md`, `CONTENT_RATING.md`,
> `ADMOB_SETUP.md`, `play_store_assets/SCREENSHOT_PLAN.md`.

---

## 0. Yönetici özeti

| | Sayı | Maddeler |
|---|---|---|
| **Tamam** | 20 | A-1…A-9, B-2, C-2…C-5, D-1…D-4, D-6, D-7 |
| **Açık — yayını durduran bloker** | **4** | **A-11**, **B-1**, **D-5**, **S-7** |
| **Açık — bloker değil** | 4 | A-10, C-1, C-6, E-1, E-2 |
| **Sahibinden bilgi/karar gerekli** | 5 | S-1…S-5 (S-6 kapandı) |
| Öneri (zorunlu değil) | 4 | Ö-2…Ö-5 (Ö-1 kapandı) |

### Blokerler nerede duruyor

| # | Bloker | Kimde | Kanıt (2026-08-17) |
|---|---|---|---|
| **S-7** | Başlangıç coini **100.000** | **Kod** | `data/PlayerProgress.kt` → `const val STARTING_COINS = 100_000` (denetim anında satır 134); yayın değeri hemen altında: `STARTING_COINS_RELEASE = 100` (satır 137). ⚠ Dosya bu sırada başka bir ajan tarafından düzenleniyordu, satır numarası kaymış olabilir — **sabit adıyla ara** |
| **A-11** | Elde yayınlanabilir **güncel AAB yok** | **Kod/Build** | Tek release AAB `builds/KronDrive_release_2026-08-15_2351_v1.0.9.aab`; ondan sonra **18 commit** geldi (11 araç, ekonomi, reklam sıklığı). `versionCode` hâlâ 10 |
| **B-1** | Gizlilik politikası **canlı değil** | **Sahibi** | `curl` bugün: `https://miniappfactory.github.io/KronDrive/` → **404**, `/tr/` → **404** |
| **D-5** | **Ekran görüntüsü yok** (Play en az 2 ister) | **Sahibi + cihaz** | `docs/play_store_assets/` altında yalnızca şablon/örnek/önizleme; gerçek oyun karesi yok |

**Sayım:** 4 bloker → **2'si kodda** (S-7, A-11), **2'si sahibinde** (B-1, D-5).
Kod tarafındaki ikisi toplam ~15 dakikalık iş; sahibindeki B-1 ~2 dakika,
D-5 ~30 dakika.

### Bu denetimde (2026-08-17) kapatılanlar

| Madde | Ne kapandı | Kanıt |
|---|---|---|
| **A-9** | Ayarlardaki sabit sürüm metni | `ui/settings/SettingsScreen.kt:238-239` artık `BuildConfig.VERSION_NAME` okuyor |
| **Ö-1** | Aynı maddenin öneri karşılığı | ↑ uygulandı |
| **D-7** | Mağaza metinlerindeki **beş** yanlış iddia | `store_long_description_{en,tr}.txt` bugün yeniden yazıldı (aşağıda D-7) |
| **C-2 / C-3 gözden geçirme** | Bugünkü değişikliklerin Data Safety / içerik derecelendirmeye etkisi | Etki **yok** — yeni izin yok, yeni bağımlılık yok, yeni veri toplama yok (aşağıda C-2b) |

---

## A. Teknik / kod tarafı

### A-1 · targetSdk ve minSdk — **TAMAM**
`source/app/build.gradle.kts`: `minSdk = 24` (satır 14), `targetSdk = 36`
(satır 15), `compileSdk 36` (satır 10). Play'in güncel eşiğinin üstünde.

### A-2 · İzin denetimi — **TAMAM, bugün de değişmedi**
`AndroidManifest.xml:5-6` yalnızca iki izin beyan ediyor:

- `android.permission.INTERNET`
- `android.permission.ACCESS_NETWORK_STATE`

Kamera, mikrofon, konum, rehber, depolama, bildirim, `QUERY_ALL_PACKAGES` **yok**.
Bugünkü üç yeni araç, yeni sesler ve ekonomi değişiklikleri **tek bir izin bile
eklemedi** — ses *sentezleniyor* (AudioTrack), kaydedilmiyor.

`play-services-ads` birleşme yoluyla `AD_ID` ve `ACCESS_ADSERVICES_*` ekliyor
(kaldırılmamalı). Sonucu C-4'e bağlı.

### A-3 · UMP / GDPR onay akışı — **TAMAM**
`ads/ConsentManager.kt` + `MainActivity.kt` + `ui/settings/SettingsScreen.kt`:

- Onay çözülmeden reklam istenmiyor (4 sn güvenlik ağı `canRequestAds()` kontrolüne bağlı)
- Ayarlarda "Gizlilik seçenekleri" giriş noktası var (`isPrivacyOptionsRequired` → `showPrivacyOptions`)
- `requestConsent` her iki dalda da `canRequestAds()` kontrolünden geçiyor

Kalan tek şart kod değil konfigürasyon → C-6.

### A-4 · Reklam politikası uyumu — **TAMAM (bugünkü değişiklikle birlikte doğrulandı)**
- Oyun ekranında banner yok; banner yalnızca menü/garaj/görev ekranlarında
- Geçiş reklamı yalnızca koşu bittikten sonra
- App Open Ads yok
- Ödül yalnızca SDK'nın gerçek "kazanıldı" callback'inde
- Reklam yüklenemezse akış devam ediyor

**Bugün değişen frekans, politika açısından İYİ yönde:**

| Sabit | Değer | Dosya |
|---|---|---|
| `INTERSTITIAL_EVERY_N_LEVELS` | **3** | `game/GameConfig.kt:514` |
| `INTERSTITIAL_EVERY_N_ENDLESS_RUNS` | 3 | `GameConfig.kt:517` |
| `INTERSTITIAL_FREE_LEVELS` | **3** (4'tü) | `GameConfig.kt:551` |
| `INTERSTITIAL_MIN_RUN_SECONDS` | 10 | `GameConfig.kt:529` |

Muafiyet 4 bölümden 3'e indi, yani reklam **bir bölüm daha erken** başlıyor.
"Disruptive Ads" eşiğinin çok altında — koşu içinde tam ekran reklam yok,
uygulama açılışında reklam yok. **İhlal yok.**

Not: `game/AdFrequency.kt` muafiyeti yalnızca `RunMode.CAREER`'a veriyor;
günlük görev sayacı artırmıyor ama sayaç doluysa reklam gösterebiliyor. Bu
tasarım bilinçli ve belgeli.

### A-5 · Gerçek AdMob kimlikleri — **TAMAM**
`AdIds.kt`: `USE_TEST_IDS_IN_RELEASE = false`, üç üretim birimi girili
(`pub-8582550349019790`). `AndroidManifest.xml`: App ID
`ca-app-pub-8582550349019790~2279115293`. Debug build test reklamı gösteriyor.

### A-6 · `appCategory="game"` — **TAMAM**
`AndroidManifest.xml:10`.

### A-7 · Hesap silme gereksinimi — **KAPSAM DIŞI (doğrulandı)**
Kural yalnızca **hesap oluşturma sunan** uygulamaları bağlar. Kod tabanında
hesap/giriş/kayıt yok; ağa giden tek bileşen reklam SDK'sı; kullanıcı verisi
yalnızca cihazdaki DataStore'da. **Console'da hesap silme URL'i istenmeyecek.**

Kullanıcı beklentisi gizlilik politikasının 7. bölümüyle karşılanıyor.

### A-8 · İmzalama ve keystore — **TAMAM**
- Keystore var: `source/my-upload-key.jks` + `source/signing.properties`, alias `UPLOAD`
- **Sır sızıntısı yok:** `git ls-files` taraması `*.jks` / `signing.properties` için sıfır eşleşme; ikisi de `.gitignore`'da
- `builds/` `.gitignore`'da

**Keystore ORTAK kullanılıyor — proje sahibi kararı, 2026-08-16.** Sertifika DN
`CN=Blast the Blocks, OU=AppDeveloper, O=AppDeveloper, L=Istanbul, C=TR`;
SHA-256 `1497473b7f18d1890b43254623605a65c35bfee9e627b0105e4b23bc22bde2d0`.
Play tek anahtarın birden çok uygulamada kullanılmasını engellemez.

> Alias `UPLOAD` olduğu için bu bir **upload** anahtarıdır: Play App Signing
> kayıtlıysa kaybolduğunda Google'dan sıfırlatılabilir.
>
> ⚠ **Bu, Play konsolunda DOĞRULANMADI.** Kayıt yoksa anahtar kaybı kalıcıdır.
> Sahibi Console'dan teyit etmeli (`PLAY_SUBMISSION_ORDER.md` Adım 7).

### A-9 · Uygulama içi sürüm metni — **TAMAM (bugün kapandı)**
Önceki denetimde `SettingsScreen.kt` sabit `"Sürüm 1.0.0"` yazıyordu.
Şimdi `ui/settings/SettingsScreen.kt:238-239`:

```
tr = "Sürüm ${BuildConfig.VERSION_NAME}",
en = "Version ${BuildConfig.VERSION_NAME}"
```

Bir daha eskimez. **Madde kapandı.**

### A-10 · `developerTestDeviceIds` boş — **AÇIK (gelir riski, politika değil)**
`AdIds.kt` → `developerTestDeviceIds: List<String> = emptyList()`.

Release build gerçek reklam gösteriyor. Sahibi kendi cihazında release APK'yı
test ederken bir reklama dokunursa AdMob **geçersiz trafik** sayar ve hesap
askıya alınabilir. Test cihazının AdMob ID'si (ilk reklam isteğinde logcat'te
`Ads` etiketiyle yazılır) listeye eklenmeli.

**Yayını durdurmaz ama Adım 7'deki cihaz testinden ÖNCE yapılmalı.**

### A-11 · Yayınlanabilir güncel AAB yok — **AÇIK · BLOKER · YENİ**

Elde tek release AAB var: `builds/KronDrive_release_2026-08-15_2351_v1.0.9.aab`.
**Bu dosya bugünkü oyunu içermiyor.** 2026-08-15'ten bu yana gelen commit'ler:

```
2608363 Hiz 60'a esitlendi, tir/F1 dengesi, test coini ve reklam siniri
c3d5a51 Uc yeni arac: Motosiklet, Tir, F1 + olcu sinifi altyapisi
e1e0351 Beety baslangic araci oldu + garaj/arayuz kusurlari duzeltildi
c24c00d Perfect dodge hedefleri kaldirildi
e7e1310 Bolum kilidi 2 goreve indi
9a072de Ekonomi: oynanis geliri olculup yukseltildi
d3f2d28 Gecis reklami sayaci kacagi kapatildi
… (toplam 18 commit; bunların 12'si `source/` altına dokunuyor)
```

Ayrıca `build.gradle.kts:16-17` hâlâ `versionCode = 10`, `versionName = "1.0.9"`.
Play aynı `versionCode` ile ikinci yükleme kabul etmez ve o build zaten yanlış
içeriğe sahip.

**Gereken:** S-7 geri alındıktan **sonra** `versionCode` 11'e çıkarılıp yeni
imzalı AAB üretilecek. Sürüm adı da içeriğe uygun olmalı (öneri: `1.1.0` —
11 araç ve yeniden dengelenmiş ekonomi bir yama değil, bir sürüm).

> Bu madde **build-release-engineer'ın** işidir. Bu denetimde Gradle
> çalıştırılmadı (görev sınırı).

**Boyut notu (kanıt, bloker değil):** en yeni test APK
`builds/KronDrive_test_2026-08-17_2158_v1.0.9.apk` = **4.840.882 bayt ≈ 4.62 MB**
(2026-08-16 sürümü 4.474.334 bayt ≈ 4.27 MB idi). Play'in 200 MB APK / 150 MB
AAB sınırının çok altında — sorun yok.

---

## B. Barındırma (GitHub Pages + app-ads.txt)

### B-1 · Gizlilik politikası URL'i — **AÇIK · BLOKER (değişmedi)**

**Metin tarafı tamam:** TR ve EN iki ayrı, kendi kendine yeten HTML sayfası
hazır ve commitli. Dış CSS/font/script yok, açık/koyu temaya uyumlu, iki dil
birbirine bağlı, iletişim e-postası `whatsthisapp@proton.me`.

| Dosya | Yayınlanacağı adres |
|---|---|
| `docs/index.html` (EN) | `https://miniappfactory.github.io/KronDrive/` ← **Console'a girilecek** |
| `docs/tr/index.html` (TR) | `https://miniappfactory.github.io/KronDrive/tr/` |
| `docs/.nojekyll` | (Jekyll'i kapatır) |

**Kanıt — bugün de canlı DEĞİL (2026-08-17, `curl`):**

```
404  https://miniappfactory.github.io/KronDrive/
404  https://miniappfactory.github.io/KronDrive/tr/
```

Tek eksik GitHub arayüzündeki **Pages ayarı**. Adımlar:
`PLAY_SUBMISSION_ORDER.md` Adım 1.

> **İçerik kontrolü (2026-08-17):** politikada sayılan cihaz verileri —
> coin/XP, yıldız, yükseltme, sahip olunan araç ve boya, rekorlar, görev
> durumu, reklam sayaçları, tercihler — bugünkü kodla **hâlâ örtüşüyor**.
> Yeni araçlar ve yeni boya mevcut "sahip olunan araç ve boya" kalemine
> giriyor; **politika metninde değişiklik gerekmiyor.**

### B-2 · `app-ads.txt` — **TAMAM (bugün yeniden doğrulandı)**

```
200  https://miniappfactory.github.io/app-ads.txt
     google.com, pub-8582550349019790, DIRECT, f08c47fec0942fa0
```

Play'in zorunlu şartı değil; AdMob doluluk oranı ve sahtecilik koruması için
gerekir. Kök alan adından sunuluyor (doğru yer — tarayıcılar alt klasöre
bakmaz). Yayıncı ID'si Kron Drive'ınkiyle aynı hesap. **Ek iş yok.**

Kalan tek adım S-5 (AdMob'da doğrulamayı tetiklemek).

> **Belge düzeltmesi (Ö-4, hâlâ açık):** `STORE_SUBMISSION_CHECKLIST.md` §2,
> dosyanın `.../KronDrive/app-ads.txt` adresinde yayınlanacağını yazıyor.
> O adres 404 verir ve zaten doğru adres değildir. Repodaki `docs/app-ads.txt`
> kopyası zararsız ama işlevsiz.

---

## C. Play Console — App content beyanları

Cevapların tamamı hazır; **hiçbiri Console'a girilmedi** (girişi sahibi yapacak,
sıra: `PLAY_SUBMISSION_ORDER.md` Adım 4).

### C-1 · Gizlilik politikası — **AÇIK** (B-1'e bağlı)
URL canlı olur olmaz girilecek. Aynı adres mağaza sayfasındaki *Website*
alanına da girilecek (app-ads.txt taraması buna bakar).

### C-2 · Data Safety formu — **CEVAPLAR HAZIR, giriş bekliyor**
Tam cevap seti: `docs/DATA_SAFETY_FORM.md` (denetlendi, güncel).

| Kalem | Toplanır | Paylaşılır | Amaç | Zorunlu/Ops. |
|---|---|---|---|---|
| Device or other IDs | Evet | Evet | Advertising; Analytics; Fraud prevention | **Opsiyonel** |
| Approximate location | Evet | Evet | Advertising; Analytics; Fraud prevention | Zorunlu |
| App interactions | Evet | Evet | Advertising; Analytics; Fraud prevention | Zorunlu |
| Diagnostics | Evet | Evet | Analytics; Fraud prevention | Zorunlu |

Hepsi AdMob kaynaklı; oyunun kendisi cihazdan hiçbir veri çıkarmıyor.
"Hayır" işaretlenecekler ve diğer işaretler için `DATA_SAFETY_FORM.md` §2.2, §3.

> **Tutarlılık şartı:** Data Safety "Device IDs → Evet" ile C-4'teki
> Advertising ID beyanı aynı yönde olmalı; çelişirse gönderim reddedilir.

### C-2b · Bugünkü değişikliklerin Data Safety'ye etkisi — **YOK (doğrulandı)**

Sahibi özellikle bunu sordu; üç yönden kontrol edildi:

| Kontrol | Sonuç | Kanıt |
|---|---|---|
| Yeni izin var mı? | **Hayır** | `AndroidManifest.xml:5-6` — hâlâ yalnızca `INTERNET` + `ACCESS_NETWORK_STATE` |
| Yeni bağımlılık / SDK var mı? | **Hayır** | `build.gradle.kts` dependencies bloğu: ağa çıkan tek şey `play.services.ads` + `user.messaging.platform`. Analytics, Crashlytics, Firebase, Billing **yok** |
| Yeni veri kalemi cihazdan çıkıyor mu? | **Hayır** | Yeni araçlar/boya/ekonomi yalnızca DataStore'a yazıyor (`kron_drive_progress`); dışarı gönderen kod yok |

**Sonuç: `DATA_SAFETY_FORM.md` olduğu gibi geçerli, güncelleme gerekmiyor.**

### C-3 · İçerik derecelendirme (IARC) — **CEVAPLAR HAZIR, giriş bekliyor**
Tam cevap seti: `docs/CONTENT_RATING.md` (denetlendi, güncel).

Özet: kategori **Oyun** · şiddet yok (araç çarpışması var; kan, yaralanma,
insan hedefi yok) · cinsellik yok · argo yok · madde yok · korku yok ·
kullanıcı etkileşimi yok · **reklam içerir → Evet** · konum paylaşımı yok ·
satın alma yok.

**Kumar/loot box → Hayır, doğrulandı:** haftalık sandık **sabit** ödül veriyor —
`WeeklyMissionGenerator.WEEKLY_CHEST_COINS = 750` ve
`WEEKLY_CHEST_BOOSTER = BoosterType.SECOND_CHANCE`. Rastgelelik **yok**,
gerçek parayla alınamıyor. Bu, "loot box" tanımını karşılamıyor.

Beklenen sonuç: **PEGI 3 / ESRB Everyone / USK 0**, "reklam içerir" notuyla.

### C-3b · Bugünkü değişikliklerin içerik derecelendirmesine etkisi — **YOK (doğrulandı)**

| Bugünkü değişiklik | Derecelendirmeye etkisi |
|---|---|
| 3 yeni araç (Motosiklet, Tır, Formula) | Yok — şiddet/tema değişmiyor, çarpışma yine kansız |
| Yeni boya (Gün Sarısı) | Yok |
| Başlangıç aracı Beety | Yok |
| Perfect dodge hedefleri kaldırıldı | Yok |
| Bölüm kilidi 2 göreve indi | Yok |
| Reklam sıklığı (3 koşu, ilk 3 bölüm muaf) | Yok — "reklam içerir" cevabı zaten Evet |
| Ekonomi (günlük tavan 500, çarpan 70) | Yok — hâlâ satın alma yok, gerçek para yok |

**Sonuç: `CONTENT_RATING.md` olduğu gibi geçerli, anket cevapları değişmiyor.**

### C-4 · Reklam beyanı ve Advertising ID — **CEVAP NET, giriş bekliyor**

| Console sorusu | Cevap | Gerekçe |
|---|---|---|
| Does your app contain ads? | **Evet** | Banner + geçiş + ödüllü |
| Advertising ID kullanılıyor mu? | **Evet** — zorunlu | `AD_ID` izni birleştirilmiş manifestte (A-2). "Hayır" → beyan-manifest çelişkisi → **red** |
| Amaç | Advertising/marketing + Analytics | AdMob kişiselleştirilmiş reklam sunuyor |

Mağaza sayfasında "Contains ads" rozeti çıkacak — beklenen davranış.

### C-5 · Diğer App content beyanları — **CEVAPLAR NET, giriş bekliyor**

| Beyan | Cevap |
|---|---|
| App access | **Hayır — tüm içerik girişsiz** |
| Uygulama içi satın alma | **Yok** (Billing kütüphanesi bağımlılıklarda dahi yok) |
| Government / Financial / Health / News / COVID-19 | Hepsi Hayır |
| Hesap silme URL'i | **İstenmeyecek** (A-7) |
| Data safety → veri toplanıyor mu | **Evet** (C-2) |

### C-6 · AdMob tarafındaki GDPR mesajı — **DOĞRULANAMADI**
Kod tarafı hazır (A-3) ama UMP formu AdMob konsolunda **yayımlanmadıysa**
AEA/UK kullanıcısına hiç görünmez ve `canRequestAds()` sürekli reklamı
kapatabilir (→ o bölgelerde sıfır gelir).

**Konsol erişimim yok — bu denetimde kontrol edilemedi. Sahibi bakmalı** (S-3).

---

## D. Mağaza sayfası (Main store listing)

### D-1 · Başlıklar — **TAMAM**
`play_store_assets/store_titles.md`:
EN `Kron Drive: Retro Car Racer` (27/30) · TR `Kron Drive: Retro Araba Oyunu` (29/30).
Yasaklı ifade yok, üçüncü taraf marka adı yok.

> Not: `store_titles.md` gerekçe belgesi olarak `docs/ASO_STRATEGY_NOTES.md`'ye
> atıf yapıyor ama **o dosya repoda yok** (ürün incelemesi de bulmuş).
> Uyum sorunu değil, belge borcu.

### D-2 · Kısa açıklama (80 karakter) — **TAMAM, bugün de doğru**

| Dil | Dosya | Metin | Karakter |
|---|---|---|---|
| EN | `store_short_description_en.txt` | `Dodge traffic, chain Perfect Dodges, race 30 offline arcade levels.` | **67**/80 |
| TR | `store_short_description_tr.txt` | `Trafiği atlat, combo yap, 30 bölümü internetsiz bitir. Retro araba oyunu.` | **73**/80 |

**Bugün değiştirilmedi — çünkü içinde yanlış bir iddia yok:**
- "30 levels" → `LevelCatalog.kt` içinde tam **30** `LevelDef` var ✔
- "Perfect Dodges" → mekanik **duruyor** (`GameConfig.kt:347-355`, combo
  çarpanları 1.2/1.5/2/3, 6 sn pencere). Kaldırılan şey mekanik değil,
  **yıldız hedefi** olarak kullanılmasıydı ✔
- "offline / internetsiz" → sunucu yok ✔

(Önceki denetim TR uzunluğunu 77 yazmıştı; ölçüm **73**. Küçük düzeltme.)

### D-3 · Uzun açıklama — bkz. **D-7**
Önceki denetimin D-3 düzeltmesi (7 gövde / 10 boya) bugün **yeniden geçersiz
oldu**; tüm mağaza metni işi D-7'de toplandı.

### D-4 · İkon (512×512) ve feature graphic (1024×500) — **TAMAM, ama bir uyarı**
`play_store_assets/play-store-icon-512.png` ve `feature_graphic_1024x500.png`
mevcut, oyunun gerçek araç sprite'larını içeriyor. Okunabilirlik testi de var
(`icon_48_readability.png`).

> ⚠ **Sahibinin göz kararı gereken nokta:** bu görseller 2026-08-15/16'da,
> başlangıç aracı **Şehir** iken üretildi. Bugün başlangıç aracı **Beety**.
> Politika ihlali değil (görseldeki araç oyunda var), ama feature graphic'te
> öne çıkan araç oyunun ilk açılışında görülen araç değilse ilk izlenim
> tutmaz. Sahibi bakıp "yeter" derse iş yok; demezse `game-art` işi.

### D-5 · Ekran görüntüleri — **AÇIK · BLOKER (değişmedi)**
Telefon için **en az 2**, önerilen 8. Gerçek oyun karesi **yok**.

`play_store_assets/` altında bulunanlar: `screenshot_caption_example_{en,tr}.png`
(şablon), `screenshot_caption_strip_1080x2400.png` (şablon),
`previews/` (4 adet tasarım önizlemesi — mağazaya yüklenmez).

Plan hazır: **`play_store_assets/SCREENSHOT_PLAN.md`**.

**Plana bugün eklenmesi gereken üç şart:**

1. ⚠ **Coin sayacı 100.000 gösteren kare yüklenemez** — S-7 geri alınmadan
   çekim yapılırsa bütün kareler çöp olur. Sıra: S-7 → yeni build → çekim.
2. Garaj karesi artık **Beety**'yi göstermeli (başlangıç aracı değişti).
3. Araç sayısı gösteren bir kare varsa **11 araç** olmalı.

Planın teknik uyarısı geçerli: Samsung S8 ham `screencap` **1440×2960 (≈2.06:1)**
— doğrudan yüklenmez, **1080×1920**'ye kırpılıp ölçeklenmeli. Hiçbir karede
reklam veya "Test Ad" olmayacak; çekim **release** build ile.

### D-6 · Dil ve lokalizasyon — **TAMAM**
EN varsayılan + TR eklenecek; her iki dilin başlık, kısa ve uzun açıklaması
hazır. `res/values-tr/` uygulamada mevcut. TR için ayrı ekran görüntüsü seti
zorunlu değil.

### D-7 · Uzun açıklamadaki yanlış iddialar — **TAMAM (bugün düzeltildi)**

Önceki metin **beş** noktada bugünkü kodla çelişiyordu. Üçü sayı hatası, ikisi
**mekanik iddiası** — ikinciler daha ağır, çünkü Play'in *yanıltıcı metadata*
maddesine girer ve oyuncu tarafından oyun içinde kolayca yalanlanabilir.

| # | Eski metin | Kod ne diyor | Kanıt |
|---|---|---|---|
| 1 | "Yedi gövde … on boya" | **11 gövde, 11 boya** | `CarCatalog.kt:1415-1418` (`BEETY, HATCHBACK, RACE_SEDAN, KUS_SLX, MOUNTAIN_GOAT, MUSCLE, MUSCLE_67, MOTOSIKLET, SUPERCAR, TIR, F1`) · `CarCatalog.kt:1509`'dan başlayan `colors` listesinde 11 kayıt |
| 2 | "özelleştirme kimseye avantaj vermez" | **Yanlış.** Her gövdenin dört fizik çarpanı var; aralık 0.85–1.18 | `CarCatalog.kt:191-235` (`topSpeedMul`/`accelMul`/`brakeMul`/`boostMul`); uygulanışı `UpgradeCatalog.kt:119-126`. Örn. Formula `1.18/1.15/0.85/1.06`, Beety `1.00/1.00/1.00/1.00` |
| 3 | "Görünüm değişir, **çarpışma kutusu değişmez**" | **Yanlış — ve bu, ikisinden daha ağırdı.** Ölçü sınıfı çarpışma kutusunu doğrudan belirliyor | `VehicleClass.kt:52,61,72`: `MOTOSIKLET(22×59)`, `BINEK(40×76)`, `AGIR(48×202)`; kutu `hitboxWidthPx`/`hitboxHeightPx` ile buradan türetiliyor (satır 95-98). Motosiklet binekten **belirgin dar**, Tır çok daha büyük |
| 4 | "hız yükseltmesi … trafiği de hızlandırır" | **Yanlış mekanizma.** Trafik hızı `baseSpeed * ratio`; `baseSpeed` yalnızca bölümün `startSpeedKmh`'inden geliyor | `GameEngine.kt:570-575` (`return baseSpeed * ratio`) · `GameEngine.kt:106-107` (`baseSpeed` tanımı) · SPEED yükseltmesi yalnızca `scoreSpeedCap`'i büyütüyor (`UpgradeCatalog.kt:72-73`, `GameEngine.kt:446`) |
| 5 | Yıldız hedefleri arasında "perfect dodge sayısı" ve "İlk bölümler 60 km/s ile sakin başlar" | **İkisi de artık yanlış.** Perfect dodge hedefi katalogda kalmadı; **30 bölümün hepsi** 60 km/s ile başlıyor | `LevelCatalog.kt:52` ("PERFECT DODGE HEDEFLERI KALDIRILDI") — aktif `Objective.PerfectDodges` **sıfır** · `grep -c "startSpeedKmh = 60"` → **30/30** |

**Yeni metinlerde ne yazıyor (özet):**

- "Eleven vehicles / On bir araç" + 11 ismin tamamı + "eleven paints / on bir boya"
- Araç = kaplama değil: her aracın kendi hız/ivme/fren/boost karakteri var,
  garajda çubuklarla gösteriliyor; **Motosiklet ve Tır yolda farklı boyutta**
- **Boya** tamamen kozmetik (bu kısım doğru, korundu)
- SPEED yükseltmesi: "skorun seni çıkarabileceği tavan hızı yükseltir, sen
  hızlandıkça trafik üzerine daha hızlı gelir" — yaklaşma hızı gerçekten
  artıyor, mekanizma artık doğru anlatılıyor
- Yıldız hedefleri gerçekte kullanılan türlerle sayıldı: geçilen araç, skor,
  coin, combo, boost mesafesi, frensiz bitirme, süre
- "Üç görevden **ikisi** sonraki bölümü açar" (`GameConfig.kt:430` → `MIN_STARS_TO_PASS = 2`)
- "Her bölüm aynı 60 km/s ile başlar; artan şey trafik yoğunluğu"
- Reklam bölümüne "**ilk üç bölümde hiç çıkmaz**" eklendi (`INTERSTITIAL_FREE_LEVELS = 3`)
- "Beety ile bedava başlarsın" + "gerçek parayla satın alınacak hiçbir şey yok"

| Dosya | Uzunluk |
|---|---|
| `play_store_assets/store_long_description_en.txt` | **3708**/4000 |
| `play_store_assets/store_long_description_tr.txt` | **3486**/4000 |

Perfect Dodge **mekanik olarak** anlatılmaya devam ediyor — kod doğruluyor
(`GameConfig.kt:338-355`).

> **Ürün incelemesinin uyarısı duruyor (uyum maddesi değil):**
> `REVIEW_PRODUCT.md` §1.4, mağaza metninin "bu oyunun kalbi" dediği Perfect
> Dodge'un oyun içinde 900 ms'lik bir yazı dışında hiçbir geri bildirimi
> olmadığını ölçmüş (ses yok, titreşim yok). Metin yalan söylemiyor ama vaat
> ile hissin arası açık. Bu `gameplay-developer` işidir, yayın blokeri değil.

---

## E. Yayın ve test track

### E-1 · Test track planı — **AÇIK (plan yazıldı, karar S-2'ye bağlı)**
Sıra artık `PLAY_SUBMISSION_ORDER.md` Adım 7-9'da yazılı:
Internal testing → (gerekiyorsa) Closed testing → Production (aşamalı %20/%50/%100).

Kapalı testin **zorunlu olup olmadığı** hesap tipine bağlı → S-2.

### E-2 · Yayın öncesi cihaz doğrulaması — **AÇIK (bu denetimde yapılmadı)**
Görev sınırı gereği cihaza dokunulmadı. Release build'de doğrulanacaklar
listesi `PLAY_SUBMISSION_ORDER.md` Adım 7'de; bugünkü değişiklikler için
**iki yeni satır** eklendi:

- İlk üç bölümde geçiş reklamı çıkmıyor (muafiyet 4 → 3 oldu)
- Yeni kurulumda başlangıç coini **100** (S-7'nin cihazda kanıtı)

---

## S. Sahibinden bilgi / karar gerekli

Beşi de tek oturuşta cevaplanabilir.

| # | Soru | Neden gerekli |
|---|---|---|
| **S-1** | **Hedef kitle yaş grubu onayı?** Öneri: **13-15 / 16-17 / 18+** | Console'da *Target audience and content* zorunlu alan. Kardeş uygulama Kaboom Blocks ile aynı çizgi. **12 yaş ve altı işaretlenirse** Families politikası devreye girer: kodda TFCD/TFUA set edilmeli, reklam frekansı gözden geçirilmeli ve **yeni bir AAB** gerekir |
| **S-2** | **Play geliştirici hesabın kişisel mi, organizasyon mu? Ne zaman açıldı?** | Google, kişisel hesaplara production öncesi **kapalı test + tester sayısı + süre** şartı uyguluyor. Hangi durumda olduğu bilinmiyor; E-1'deki plan buna göre değişir |
| **S-3** | **AdMob → Privacy & messaging → GDPR mesajı PUBLISHED mı?** | Kod hazır (A-3) ama mesaj yayımlanmadıysa AEA/UK'de onay formu hiç çıkmaz → o bölgelerde gelir sıfır. Konsol erişimim yok (C-6) |
| **S-4** | **Dağıtım ülkeleri ve fiyat?** Öneri: tüm ülkeler, **ücretsiz** | Console zorunlu alanı. ⚠ Ücretsiz seçildikten sonra **ücretliye çevrilemez** |
| **S-5** | **AdMob'da app-ads.txt doğrulaması tetiklendi mi?** | Kökteki dosya canlı (B-2) ama AdMob'un "doğrulandı" durumuna geçtiği kontrol edilemedi |
| **S-7** | ⚠ **BLOKER — başlangıç coini test değerinde** | `data/PlayerProgress.kt` → `STARTING_COINS = 100_000` (denetim anında satır 134). Bu değerle yayına çıkılırsa **11 aracın hepsi** (en pahalısı Formula 5.000), 11 boya ve bütün yükseltmeler ilk saniyede alınabilir; ekonomi tamamen anlamsızlaşır. Geri alma tek satır: `STARTING_COINS = STARTING_COINS_RELEASE` (değeri 100). **Bu bir soru değil, hatırlatma** — sahibi *"aab yaparken değiştiririz"* demişti |
| ~~S-6~~ | ~~Keystore repo dışında yedeklendi mi?~~ **KAPANDI** | Sahibi 2026-08-16'da yedeklediğini bildirdi. **Beyandır, teknik olarak doğrulanmadı** — yedeğin yeri, `signing.properties`'in de dâhil olup olmadığı ve geri yüklenebilirliği kontrol edilmedi |

---

## Ö. Öneriler (zorunlu değil)

| # | Öneri | Etki |
|---|---|---|
| ~~Ö-1~~ | ~~Sürüm metnini `BuildConfig.VERSION_NAME`'den oku~~ | **KAPANDI** — `SettingsScreen.kt:238-239` |
| Ö-2 | `AdIds.developerTestDeviceIds`'e test cihazının AdMob ID'sini ekle (A-10) | Geçersiz trafik / hesap askıya alınma riski kalkar. **Cihaz testinden önce yapılmalı** |
| Ö-3 | Ayarlara **"İlerlemeyi sıfırla"** düğmesi | Kullanıcının "verilerimi sil" beklentisini uygulama içinde karşılar. `GameStateRepository`'de `clear()`/`reset()` yok |
| Ö-4 | `STORE_SUBMISSION_CHECKLIST.md` §2'deki `app-ads.txt` adresini düzelt (B-2) | Belge yanlış adres gösteriyor; ileride yanlış teşhise yol açar |
| Ö-5 | Gizlilik politikasındaki "Son güncelleme: 14 Ağustos 2026" tarihini yayın gününe çek | İçerik değişmediyse şart değil ama daha temiz |
| Ö-6 | `versionName`'i `1.1.0` yap (A-11) | 11 araç + yeniden dengelenmiş ekonomi bir yama değil, bir sürüm. Sürüm notu yazmayı da kolaylaştırır |

---

## Denetimin sınırları — neyi iddia ETMİYORUM

Dürüstlük gereği açıkça yazıyorum:

- **Play Console'a erişimim yok.** "Şu ayar açık/kapalı" diye hiçbir iddiada
  bulunmadım. Play App Signing kaydı, hesap tipi, kapalı test şartı, girilen
  beyanlar — hepsi **doğrulanamadı**, sahibi bakmalı.
- **AdMob konsoluna erişimim yok.** GDPR mesajının yayımlanmış olup olmadığı
  (C-6/S-3) ve app-ads.txt doğrulama durumu (S-5) **doğrulanamadı**.
- **Gradle çalıştırmadım, cihaza dokunmadım** (görev sınırı). Build'in
  geçtiğine, testlerin yeşil olduğuna, oyunun cihazda çalıştığına dair
  hiçbir kanıtım yok — o kanıt `build-release-engineer` ve
  `qa-test-engineer`'dan gelmeli.
- **Kod değiştirmedim.** S-7 ve A-11 hâlâ açık; bu belge onları kapatmıyor,
  yalnızca işaret ediyor.
- Kapattığım her maddenin kanıtı **dosya + satır numarası** olarak yukarıda
  yazılı; okuyup kendin doğrulayabilirsin.

> **Otomatik yayın yapılmadı ve yapılmayacak.** Bu denetimin çıktısı yalnızca
> bu belge, `PLAY_SUBMISSION_ORDER.md` ve `play_store_assets/` altındaki
> düzeltilmiş metin dosyalarıdır.
