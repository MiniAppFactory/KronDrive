# Play Store yayın hazırlık denetimi — Kron Drive v1.0.9 (versionCode 10)

Denetim tarihi: **2026-08-16** · Denetleyen: `play-store-compliance-engineer`
Kapsam: yalnızca denetim ve `docs/` altındaki belgeler. **Kod değiştirilmedi,
cihaza dokunulmadı, hiçbir şey yayınlanmadı.**

> Bu dosya **güncel durum tablosudur**. Arka plandaki gerekçeler ve uzun
> analizler yerinde duruyor, tekrar edilmiyor:
> `STORE_SUBMISSION_CHECKLIST.md` (politika analizi + C-1…C-9 kod denetimi),
> `DATA_SAFETY_FORM.md`, `CONTENT_RATING.md`, `ADMOB_SETUP.md`,
> `play_store_assets/SCREENSHOT_PLAN.md`.

---

## 0. Yönetici özeti

| | Sayı | Maddeler |
|---|---|---|
| **Tamam** | 18 | A-1…A-8, B-2, C-2…C-5, D-1…D-4, D-6 |
| **Eksik** | 8 (**2'si yayını durduran bloker**) | **B-1**, **D-5**, A-9, A-10, C-1, C-6, E-1, E-2 |
| **Sahibinden bilgi/karar gerekli** | 5 | S-1…S-5 (S-6 kapandı) |
| Öneri (zorunlu değil) | 5 | Ö-1…Ö-5 |

Bu denetimde `docs/` altında kapatılan eksikler: **kısa açıklama** (D-2, zorunlu
Play alanıydı ve hiç yoktu), **uzun açıklamadaki yanlış araç/boya sayısı** (D-3),
**ekran görüntüsü planı** (D-5 için).

### Yayını durduran iki bloker

| # | Bloker | Kanıt |
|---|---|---|
| **B-1** | **Gizlilik politikası canlı değil.** GitHub Pages `MiniAppFactory/KronDrive` reposunda **açılmamış**. Play, çalışan bir HTTPS URL olmadan uygulamayı kabul etmez. | `curl` → `https://miniappfactory.github.io/KronDrive/` **404**, `.../KronDrive/tr/` **404** (2026-08-16) |
| **B-2** | **Ekran görüntüsü yok.** Play telefon için **en az 2** zorunlu. `play_store_assets/` altında yalnızca şablon/örnek var, gerçek oyun karesi yok. | Klasör listesi: `screenshot_caption_*` şablonları var, `screenshots/` klasörü yok |

İyi haber: B-1'in dosyaları **zaten hazır ve commitli** (`docs/index.html`,
`docs/tr/index.html`). Tek yapılacak GitHub'da Pages ayarını açmak — kod veya
metin işi yok. B-2 için de plan hazır: `play_store_assets/SCREENSHOT_PLAN.md`.

---

## A. Teknik / kod tarafı

### A-1 · targetSdk ve minSdk — **TAMAM**
`build.gradle.kts`: `minSdk = 24`, `targetSdk = 36`, `compileSdk 36`.
Play'in güncel target API eşiğinin üstünde. Aksiyon yok.

### A-2 · İzin denetimi — **TAMAM, gereksiz izin yok**
`AndroidManifest.xml` yalnızca iki izin beyan ediyor:

- `android.permission.INTERNET` — reklam SDK'sı için gerekli
- `android.permission.ACCESS_NETWORK_STATE` — reklam SDK'sının bağlantı kontrolü

Kamera, mikrofon, konum, rehber, depolama, bildirim, `QUERY_ALL_PACKAGES` **yok**.
İzin minimizasyonu beklentisi karşılanıyor.

`play-services-ads` birleşme yoluyla ayrıca `com.google.android.gms.permission.AD_ID`
ve `ACCESS_ADSERVICES_*` izinlerini ekliyor (kaldırılmadı, kaldırılmamalı — reklam
gelirinin temeli). **Sonucu Console beyanına bağlıyor:** bkz. C-4.

### A-3 · UMP / GDPR onay akışı — **TAMAM (önceki iki bulgu kapanmış)**
Kod okundu, `ads/ConsentManager.kt` + `MainActivity.kt` + `ui/settings/SettingsScreen.kt`:

- **Onay çözülmeden reklam istenmiyor.** `MainActivity.kt` içindeki 4 saniyelik
  güvenlik ağı artık koşulsuz açmıyor:
  `if (!adsConsentResolved.value && ConsentManager.canRequestAds(this))`.
  (Eski C-2b bulgusu — **düzeltilmiş.**)
- **"Gizlilik seçenekleri" giriş noktası var.** `SettingsScreen.kt` içinde
  `ConsentManager.isPrivacyOptionsRequired(context)` okunuyor ve `REQUIRED` iken
  `showPrivacyOptions(activity)` çağıran görünür bir satır çiziliyor.
  (Eski C-2a bulgusu — **düzeltilmiş.**) AEA/UK kullanıcısı onayını geri alabiliyor.
- `requestConsent` her iki dalda da (başarı ve hata) `canRequestAds()` kontrolünden
  geçiyor.

AB/GDPR açısından **eksik bulunamadı.** Kalan tek şart kod değil konfigürasyon:
AdMob konsolunda **Privacy & messaging → GDPR mesajı yayınlanmış** olmalı,
aksi halde form hiç görünmez. Bkz. C-6.

### A-4 · Reklam politikası uyumu — **TAMAM**
- Oyun ekranında banner **yok**; banner yalnızca menü/garaj/görev ekranlarında
  (`ui/common/KronComponents.kt`, tek çağrı noktası).
- Geçiş reklamı yalnızca koşu bittikten sonra; oynanış sırasında tam ekran reklam yok.
- Uygulama açılışında tam ekran reklam yok (App Open Ads kullanılmıyor).
- Ödül **yalnızca** SDK'nın gerçek kazanıldı callback'inde veriliyor
  (`RewardedAdManager.kt`); yarıda kesilen video ödül vermiyor.
- Reklam yüklenemezse akış devam ediyor (`InterstitialAdManager.kt` → `proceedOnce`),
  oyun kilitlenmiyor.

"Disruptive Ads" ve "ödül aldatmacası" ihlali yok.

### A-5 · Gerçek AdMob kimlikleri — **TAMAM**
`AdIds.kt`: `USE_TEST_IDS_IN_RELEASE = false`, üç üretim birimi girili
(`pub-8582550349019790` altında banner/interstitial/rewarded).
`AndroidManifest.xml`: gerçek App ID `ca-app-pub-8582550349019790~2279115293`.
Debug build hâlâ test reklamı gösteriyor (`BuildConfig.DEBUG` dalı) — doğru desen.

### A-6 · `appCategory="game"` — **TAMAM**
`AndroidManifest.xml` içinde `android:appCategory="game"` mevcut (eski C-7 kapandı).

### A-7 · Hesap silme gereksinimi — **KAPSAM DIŞI (doğrulandı)**
Google'ın hesap silme kuralı yalnızca **hesap oluşturma sunan** uygulamaları bağlar.

Doğrulama: kod tabanında hesap/giriş/kayıt akışı yok; ağa giden tek bileşen reklam
SDK'sı; kullanıcı verisi yalnızca cihazdaki DataStore'da (`kron_drive_progress`).
Sunucu, backend, kullanıcı kimliği yok. **Console'da "hesap oluşturma yok" beyanı
verilecek, hesap silme URL'i istenmeyecek.**

Kullanıcı beklentisi yine de karşılanıyor: gizlilik politikasının 7. bölümü
cihaz üzerinden silme yolunu adım adım anlatıyor (uygulamayı kaldır / Ayarlar →
Uygulamalar → Depolama → Verileri temizle / reklam kimliğini sıfırla). Bu yeterlidir.

### A-8 · İmzalama ve AAB — **TAMAM**
- Keystore var: `source/my-upload-key.jks` + `source/signing.properties`, alias `UPLOAD`.
- **Sır sızıntısı yok:** `git ls-files` taraması `*.jks` / `signing.properties`
  için **sıfır** eşleşme döndü; her ikisi de hem kök hem `source/` `.gitignore`'ında.
- İmzalı üretim çıktıları: `builds/KronDrive_release_2026-08-15_2351_v1.0.9.aab`
  (8.3 MB) ve eşleniği `.apk`.
- `builds/` `.gitignore`'da — repo şişmiyor.

**Keystore ORTAK kullanılıyor — proje sahibi kararı, 2026-08-16.** Sertifika
DN'i `CN=Blast the Blocks, OU=AppDeveloper, O=AppDeveloper, L=Istanbul, C=TR`;
SHA-256 `1497473b7f18d1890b43254623605a65c35bfee9e627b0105e4b23bc22bde2d0`.
Anahtar başka bir MiniAppFactory uygulamasından geliyor ve **bilerek**
paylaşılıyor. Play tek anahtarın birden çok uygulamada kullanılmasını
engellemez. Kron Drive'a ayrı keystore üretilmeyecek.

Paylaşımın bilinen sınırı: uygulamalardan biri başka bir geliştiriciye
devredilirse anahtarın ayrılması gerekir. Uygulamalar birbirinin verisine
erişmez (`sharedUserId` tanımlı değil).

> **Hatırlatma — düzeltildi.** Bu kutu önceden "keystore kaybolursa güncelleme
> bir daha yayınlanamaz" diyordu. Alias `UPLOAD` olduğu için bu bir **upload**
> anahtarıdır ve Play App Signing'de asıl imzalama anahtarını Google tutar:
> upload anahtarı kaybolur veya sızarsa **Google'dan sıfırlatılabilir**.
> Yine de yedek şart — sıfırlama bir destek süreci, günler alabilir ve o süre
> boyunca güncelleme yayınlanamaz. Sahibi 2026-08-16'da **yedeklediğini
> bildirdi** (S-6 kapandı; beyan, doğrulama değil).
>
> ⚠ Bu düzeltme dosya adına ve alias'a dayanıyor; Play konsolunda "Play App
> Signing" kaydının **açık olduğu doğrulanmadı**. Kayıt yoksa eski uyarı
> geçerlidir (anahtar kaybı = kalıcı). Konsoldan teyit edilmeli.

### A-9 · Uygulama içi sürüm metni yanlış — **EKSİK (küçük)**
`ui/settings/SettingsScreen.kt:233` hâlâ `"Sürüm 1.0.0" / "Version 1.0.0"` yazıyor;
gerçek sürüm **1.0.9**. Politika ihlali değil ama incelemeci ve kullanıcı gözünde
tutarsız; destek e-postasında "hangi sürümdesiniz?" sorusunu da bozar.

**Öneri (kod sahipliği bu denetimde bende değil, uygulanmadı):** sabit metin yerine
`BuildConfig.VERSION_NAME` okunsun — bir daha hiç eskimez.

### A-10 · `developerTestDeviceIds` boş — **EKSİK (gelir riski, politika değil)**
`AdIds.kt` → `developerTestDeviceIds: List<String> = emptyList()`.

Artık release build **gerçek** reklam gösteriyor. Sahibi kendi cihazında release
APK'yı test ederken bir reklama dokunursa AdMob bunu **geçersiz trafik** sayar ve
hesap askıya alınabilir. Test cihazının AdMob ID'si (ilk reklam isteğinde logcat'te
`Ads` etiketiyle yazılır) bu listeye eklenirse o cihaza her zaman güvenli test
reklamı gider.

**Öneri:** yayından önce cihaz ID'si eklensin. Kod işi — bu denetimde uygulanmadı.

---

## B. Barındırma (GitHub Pages + app-ads.txt)

### B-1 · Gizlilik politikası URL'i — **EKSİK · BLOKER**

**Metin tarafı tamam:** TR ve EN olarak **iki ayrı, kendi kendine yeten HTML sayfası**
hazır ve commitli. Dış CSS/font/script yok, açık/koyu temaya uyumlu, mobilde
okunabilir, iki dil birbirine bağlı. İletişim e-postası her iki sayfada da
`whatsthisapp@proton.me`.

| Dosya | Yayınlanacağı adres |
|---|---|
| `docs/index.html` (EN, 11 bölüm) | `https://miniappfactory.github.io/KronDrive/` ← **Console'a girilecek URL** |
| `docs/tr/index.html` (TR, 11 bölüm) | `https://miniappfactory.github.io/KronDrive/tr/` |
| `docs/.nojekyll` | (Jekyll'i kapatır) |

İçerik denetlendi ve **v1.0.9 ile tutarlı**: cihazda saklanan değerlerin tam listesi
(coin/XP, yıldız, yükseltme, araç ve boya, rekorlar, görev durumu, reklam sayaçları,
tercihler), Android Auto Backup açıklaması, AdMob bölümü, AEA/UK onayı, çocuk
gizliliği, saklama ve silme, GDPR/KVKK/CCPA hakları. Abartılı güvenlik iddiası yok.

**Kanıt — şu an canlı DEĞİL (2026-08-16, `curl`):**

```
404  https://miniappfactory.github.io/KronDrive/
404  https://miniappfactory.github.io/KronDrive/tr/
200  https://github.com/MiniAppFactory/KronDrive     ← repo public, sorun bu değil
```

Repo public ve dosyalar `main`'de (yerel `main`, `origin/main` ile eşit — 0 commit
ileride). **Yani tek eksik GitHub arayüzündeki Pages ayarı.**

**Yapılacak (sahibi, ~2 dakika):**

1. GitHub → `MiniAppFactory/KronDrive` → **Settings → Pages**
2. *Source: Deploy from a branch* → Branch **`main`**, Folder **`/docs`** → **Save**
3. 1-3 dakika bekle, sonra **gizli sekmede** iki adresin de açıldığını doğrula
4. Play Console → *App content → Privacy policy* alanına
   `https://miniappfactory.github.io/KronDrive/` gir
5. Mağaza sayfasındaki *Website* alanına da aynı adresi gir

Pages sorun çıkarırsa yedek yol (Netlify Drop) `STORE_SUBMISSION_CHECKLIST.md` §2'de.

### B-2 · `app-ads.txt` — **TAMAM (canlı doğrulandı) + bir belge düzeltmesi**

**Gerekli mi?** Play'in zorunlu şartı değil; **AdMob doluluk oranı ve sahtecilik
koruması için gerekir** — dosyası olmayan envanter bazı alıcılar tarafından
satın alınmaz. Yani gelir meselesi, uyum meselesi değil.

**Nereye konur?** Mağaza sayfasındaki *Website* alanında yazan alan adının
**kökü** taranır — alt klasör değil.

**Kanıt — kökteki dosya ZATEN CANLI ve doğru (2026-08-16, `curl`):**

```
200  https://miniappfactory.github.io/app-ads.txt
     google.com, pub-8582550349019790, DIRECT, f08c47fec0942fa0
```

Demek ki `MiniAppFactory.github.io` kullanıcı-sayfası reposu mevcut ve dosyayı
kökten sunuyor (büyük olasılıkla kardeş proje Kaboom Blocks için kurulmuş).
Yayıncı ID'si Kron Drive'ınkiyle **aynı hesap** olduğu için bu tek satır her iki
uygulamayı da kapsar. **Ek iş gerekmiyor.**

> **Belge düzeltmesi:** `STORE_SUBMISSION_CHECKLIST.md` §2, `app-ads.txt`'in
> `https://miniappfactory.github.io/KronDrive/app-ads.txt` adresinde yayınlanacağını
> yazıyor. Bu adres **404 veriyor ve zaten doğru adres değil** — tarayıcılar alt
> klasöre bakmaz. Repodaki `docs/app-ads.txt` kopyası **zararsız ama işlevsiz**;
> geçerli olan kökteki dosyadır. Bu satır düzeltilmeli veya not düşülmeli.

**Kalan tek adım (S-5):** AdMob konsolunda Kron Drive uygulamasını Play kaydıyla
eşleştirip **app-ads.txt → Check for updates** ile doğrulamayı tetiklemek.
Doğrulamanın geçtiği bu denetimde **kontrol edilemedi** (AdMob konsolu erişimi yok).

---

## C. Play Console — App content beyanları

Cevapların tamamı hazır; **hiçbiri henüz Console'a girilmedi** (girişi sahibi yapacak).

### C-1 · Gizlilik politikası — **EKSİK** (B-1'e bağlı)
URL canlı olur olmaz girilecek.

### C-2 · Data Safety (Veri güvenliği) formu — **CEVAPLAR HAZIR, giriş bekliyor**
Tam cevap seti: `docs/DATA_SAFETY_FORM.md`.

Özet — **beyan edilecekler (hepsi AdMob kaynaklı, oyunun kendisi hiçbir veriyi
cihazdan çıkarmıyor):**

| Kalem | Toplanır | Paylaşılır | Amaç |
|---|---|---|---|
| **Device or other IDs** (reklam kimliği) | Evet | Evet | Reklamcılık / pazarlama |
| Approximate location | Evet | Evet | Reklamcılık (IP'den kabaca) |
| App interactions | Evet | Evet | Analiz, reklamcılık |
| Diagnostics (çökme/performans) | Evet | Evet | Analiz |
| Fraud prevention amacı | — | — | İşaretlenecek |

**"Hayır" işaretlenecekler:** ad, e-posta, telefon, adres, kişiler, fotoğraf/video,
ses, dosya, takvim, sağlık, finans, mesaj, hassas kimlik bilgileri, tam konum,
kullanıcı üretimi içerik.

**Diğer işaretler:** veri aktarımı şifreli (Evet, Google SDK TLS) · kullanıcı veri
silme talebinde bulunabilir (politikadaki cihaz üzerinden silme yolu) · veriler
geçici işlenmiyor (ephemeral **değil**).

> **Tutarlılık şartı:** Data Safety'de "Device IDs → Evet" derken C-4'teki
> Advertising ID beyanının da **Evet** olması zorunlu; ikisi çelişirse gönderim reddedilir.

### C-3 · İçerik derecelendirme (IARC anketi) — **CEVAPLAR HAZIR, giriş bekliyor**
Tam cevap seti: `docs/CONTENT_RATING.md`.

Özet: kategori **Oyun** · şiddet yok (araç çarpışması var, kan/yaralanma/insan
hedefi yok) · cinsellik yok · argo yok · madde yok · korku yok · **kumar yok**
(şans kutusu var ama gerçek parayla satın alınamıyor — anket bunu ayrıca ele alıyor) ·
kullanıcı etkileşimi **yok** (sohbet, liderlik tablosu, arkadaş listesi yok) ·
**reklam içerir → Evet** · konum paylaşımı yok · satın alma yok.

Beklenen sonuç: **PEGI 3 / ESRB Everyone / USK 0** bandı, "reklam içerir" notuyla.

> Anket **Console'da doldurulmadan** hiçbir track yayınlanamaz.

### C-4 · Reklam beyanı ve Advertising ID — **CEVAP NET, giriş bekliyor**

| Console sorusu | Cevap | Gerekçe |
|---|---|---|
| *Does your app contain ads?* | **Evet** | Banner + geçiş + ödüllü |
| *Advertising ID kullanılıyor mu?* | **Evet** — zorunlu | `AD_ID` izni birleştirilmiş manifestte beyanlı (A-2). "Hayır" denirse beyan-manifest çelişkisi doğar ve gönderim **reddedilir** |
| Reklam kimliği amacı | **Reklamcılık / pazarlama** + Analiz | AdMob kişiselleştirilmiş reklam sunuyor |

Mağaza sayfasında **"Contains ads"** rozeti çıkacak — bu beklenen ve doğru davranış.

### C-5 · Diğer App content beyanları — **CEVAPLAR NET, giriş bekliyor**

| Beyan | Cevap |
|---|---|
| App access (giriş gerektiriyor mu) | **Hayır — tüm içerik girişsiz erişilebilir** |
| Uygulama içi satın alma | **Yok** |
| Government app | Hayır |
| Financial features | Hayır |
| Health apps | Hayır |
| News app | Hayır |
| COVID-19 contact tracing | Hayır |
| Hesap silme URL'i | **İstenmeyecek** — hesap oluşturma yok (A-7) |
| Data safety → veri toplanıyor mu | **Evet** (C-2) |

### C-6 · AdMob tarafındaki GDPR mesajı — **DOĞRULANAMADI**
Kod tarafı hazır (A-3) ama UMP formu **AdMob konsolunda yayınlanmadıysa** AEA/UK
kullanıcısına hiç görünmez ve `canRequestAds()` sürekli reklamı kapatabilir
(→ o bölgelerde sıfır gelir).

**Sahibi doğrulamalı:** AdMob → **Privacy & messaging → GDPR** mesajı oluşturulmuş
ve **yayımlanmış** mı; **IDFA/US eyalet düzenlemeleri** mesajı isteğe bağlı.
Konsol erişimi olmadığı için bu denetimde kontrol edilemedi.

---

## D. Mağaza sayfası (Main store listing)

### D-1 · Başlıklar — **TAMAM**
`play_store_assets/store_titles.md`:
EN `Kron Drive: Retro Car Racer` (27/30) · TR `Kron Drive: Retro Araba Oyunu` (29/30).
Yasaklı ifade (ücretsiz/free/#1/best/emoji) yok, üçüncü taraf marka adı yok.

### D-2 · Kısa açıklama (80 karakter) — **BUGÜN OLUŞTURULDU**
Zorunlu bir Play alanıydı ve **hiç yoktu**. Eklendi:

| Dil | Dosya | Metin | Karakter |
|---|---|---|---|
| EN | `play_store_assets/store_short_description_en.txt` | `Dodge traffic, chain Perfect Dodges, race 30 offline arcade levels.` | 67/80 |
| TR | `play_store_assets/store_short_description_tr.txt` | `Trafiği atlat, combo yap, 30 bölümü internetsiz bitir. Retro araba oyunu.` | 77/80 |

TR metni `araba oyunu` ve `internetsiz` anahtar kalıplarını taşıyor (ASO kararıyla
tutarlı), EN metni `traffic`/`arcade`/`offline` terimlerini.

### D-3 · Uzun açıklama — **TAMAM (bugün düzeltildi)**
`store_long_description_en.txt` (3100/4000) · `store_long_description_tr.txt` (3166/4000).

**Bulunan hata düzeltildi:** her iki metin de "Dört gövde … dokuz boya" diyordu.
`game/CarCatalog.kt` okundu — v1.0.9'da **7 oynanabilir gövde** (Şehir, Yarış Sedan,
Kuş SLX, Dağ Keçisi, Kas Arabası, Boğa 67, Süper Araba) ve **10 boya** var.
Metinler "Yedi gövde … on boya" olarak güncellendi.

> Bu, uyum açısından da önemliydi: mağaza metniyle uygulamanın örtüşmemesi Play'in
> yanıltıcı metadata maddesine girer. Burada eksik beyan yönündeydi (zarar sınırlı),
> ama artık doğru.

### D-4 · İkon (512×512) ve feature graphic (1024×500) — **TAMAM**
`play_store_assets/play-store-icon-512.png` ve `feature_graphic_1024x500.png`
2026-08-15'te yenilendi, oyunun gerçek araç sprite'larını içeriyor.
Okunabilirlik testi de var (`icon_48_readability.png`).

### D-5 · Ekran görüntüleri — **EKSİK · BLOKER**
Telefon için **en az 2**, önerilen **8**. Şu an gerçek oyun karesi **yok**.

Plan yazıldı: **`play_store_assets/SCREENSHOT_PLAN.md`** — 8 karenin ne olacağı,
nasıl o duruma gelineceği, hangi yeniliği kanıtladığı tek tek yazılı. Kareler
v1.0.9'un **yeni araç sprite'larını** (Kare 3-4: garaj + boya) ve **yenilenen
kontrol ikonlarını** (Kare 1-2: oynanış) göstermek üzere seçildi.

**Planın en kritik teknik uyarısı:** Samsung S8'in ham `screencap` çıktısı
**1440×2960 (≈2.06:1)** — bu oran 9:16 değil ve doğrudan yüklenmemeli.
Kareler **1080×1920'ye kırpılmalı/ölçeklenmeli**. Mevcut
`screenshot_caption_strip_1080x2400.png` şablonu da (2.22:1) bu hedefin dışında.

Ayrıca: hiçbir karede reklam veya "Test Ad" etiketi olmayacak; çekim **release
APK** ile yapılacak.

### D-6 · Dil ve lokalizasyon — **TAMAM (metin), ekran görüntüsü isteğe bağlı**
EN varsayılan + TR eklenecek; her iki dilin başlık, kısa ve uzun açıklaması hazır.
`res/values-tr/` uygulamada mevcut. TR sayfası için ayrı ekran görüntüsü seti
zorunlu değil ama dönüşümü artırır (plan §3).

---

## E. Yayın ve test track

### E-1 · Test track planı — **EKSİK (plan yok)**
Bu denetimde bir track planı bulunamadı. Önerilen sıra:

1. **Internal testing** — AAB'yi buraya yükle. Anında dağıtım, incelemesiz.
   Amaç: gerçek AdMob kimlikleriyle üretilmiş imzalı build'in cihazda açıldığını,
   reklamların yüklendiğini, UMP formunun (VPN ile AEA bölgesinden) çıktığını görmek.
2. **Closed testing** — S-2'ye bağlı, aşağıya bak. Hesap tipine göre **zorunlu olabilir**.
3. **Production** — yukarıdakiler yeşilse, aşamalı yayın (%20 → %50 → %100)
   önerilir; erken çökme oranı görülürse durdurulabilir.

### E-2 · Yayın öncesi cihaz doğrulaması — **BU DENETİMDE YAPILMADI**
Cihaz başka ajanda olduğu için release APK ile hiçbir şey denenmedi. Yayından önce
release build'de doğrulanması gerekenler:

- [ ] Gerçek reklamlar yükleniyor (banner menüde, geçiş koşu sonunda, ödüllü çalışıyor)
- [ ] Ödüllü reklam yarıda kesilince ödül **verilmiyor**
- [ ] Uçak modunda oyun sorunsuz, reklam yokluğu akışı bloklamıyor
- [ ] Ayarlar → "Gizlilik seçenekleri" satırı (AEA bölgesinden) görünüyor ve formu açıyor
- [ ] R8/minify açık release build'de çökme yok (`isMinifyEnabled = true`)

---

## S. Sahibinden bilgi / karar gerekli

| # | Konu | Neden gerekli |
|---|---|---|
| **S-1** | **Hedef kitle yaş grubu onayı** | Console'da *Target audience and content* zorunlu. Öneri: **13-15 / 16-17 / 18+** (yani 13+), kardeş uygulama Kaboom Blocks ile aynı çizgi. Gerekçe ve Yol A/Yol B karşılaştırması: `STORE_SUBMISSION_CHECKLIST.md` §6. **Çocuklar (9-12 ve altı) dâhil edilirse** Families politikası devreye girer: kodda TFCD/TFUA set edilmeli, reklam frekansı gözden geçirilmeli ve **yeni bir AAB üretilmeli** |
| **S-2** | **Play geliştirici hesabının tipi ve açılış tarihi** | Google, **kişisel** (organizasyon değil) geliştirici hesapları için üretim erişiminden önce **kapalı test + belirli sayıda tester + belirli bir süre** şartı uyguluyor. Hesap kuruluş hesabıysa veya bu şart getirilmeden önce açıldıysa doğrudan production'a çıkılabilir. **Hangi durumda olduğu bilinmiyor** — E-1'deki plan buna göre değişir |
| **S-3** | **AdMob → Privacy & messaging → GDPR mesajı yayımlandı mı?** | Kod hazır (A-3) ama mesaj yayımlanmadıysa AEA/UK'de form hiç çıkmaz. Konsol erişimi yok, doğrulanamadı (C-6) |
| **S-4** | **Dağıtım ülkeleri ve fiyat** | Console zorunlu alanı. Varsayılan öneri: **tüm ülkeler, ücretsiz**. Ücretsiz seçildikten sonra **ücretliye çevrilemez** |
| **S-5** | **AdMob'da app-ads.txt doğrulaması tetiklendi mi?** | Kökteki dosya canlı (B-2) ama AdMob'un "doğrulandı" durumuna geçtiği kontrol edilemedi. AdMob → Apps → Kron Drive → App settings → app-ads.txt → *Check for updates* |
| ~~**S-6**~~ | ~~Keystore repo dışında yedeklendi mi?~~ **KAPANDI** | Proje sahibi 2026-08-16'da yedeklediğini bildirdi. **Sahibi beyanıdır, teknik olarak doğrulanmadı** — yedeğin yeri, kapsamı (`.jks` yanında `signing.properties` de dâhil mi) ve geri yüklenebilirliği kontrol edilmedi |

---

## Ö. Öneriler (zorunlu değil)

| # | Öneri | Etki |
|---|---|---|
| Ö-1 | `SettingsScreen.kt` sürüm metnini `BuildConfig.VERSION_NAME`'den oku (A-9) | Bir daha eskimez |
| Ö-2 | `AdIds.developerTestDeviceIds`'e test cihazının AdMob ID'sini ekle (A-10) | Geçersiz trafik / hesap askıya alınma riski kalkar |
| Ö-3 | Ayarlara **"İlerlemeyi sıfırla"** düğmesi | Kullanıcının "verilerimi sil" beklentisini uygulama içinde karşılar, destek trafiğini azaltır. Şu an `GameStateRepository`'de `clear()`/`reset()` yok |
| Ö-4 | `STORE_SUBMISSION_CHECKLIST.md` §2'deki `app-ads.txt` adresini düzelt (B-2) | Belge yanlış adres gösteriyor; ileride yanlış teşhise yol açar |
| Ö-5 | Gizlilik politikasındaki "Son güncelleme: 14 Ağustos 2026" tarihini yayın gününe çek | İçerik değişmediyse şart değil; ama yayın tarihiyle uyumlu görünmesi daha temiz |

---

## Gönderim öncesi son kontrol (sırayla)

**Barındırma**
- [ ] GitHub Pages açıldı (`main` / `/docs`) — **B-1**
- [ ] `https://miniappfactory.github.io/KronDrive/` ve `/tr/` gizli sekmede açılıyor
- [ ] AdMob'da app-ads.txt doğrulaması tetiklendi — **S-5**

**Görsel**
- [ ] 8 ekran görüntüsü çekildi, 1080×1920'ye dönüştürüldü — **B-2**, plan: `SCREENSHOT_PLAN.md`
- [ ] Hiçbirinde reklam / "Test Ad" yok
- [ ] İkon 512 + feature graphic 1024×500 yüklendi

**Beyan**
- [ ] Hedef kitle onaylandı ve girildi — **S-1**
- [ ] Data Safety formu `DATA_SAFETY_FORM.md` ile birebir dolduruldu
- [ ] IARC anketi `CONTENT_RATING.md` ile dolduruldu
- [ ] Contains ads **Evet** · Advertising ID **Evet** · App access **giriş yok** · hesap silme **istenmiyor**
- [ ] Gizlilik politikası URL'i ve Website alanı dolduruldu

**Metin**
- [ ] EN: başlık + kısa + uzun açıklama girildi
- [ ] TR dili eklendi, üç metin de girildi

**Build**
- [ ] `KronDrive_release_2026-08-15_2351_v1.0.9.aab` internal testing'e yüklendi — **E-1**
- [ ] Cihazda release doğrulaması yapıldı — **E-2**
- [x] Keystore repo dışına yedeklendi — **S-6** (sahibi beyanı, 2026-08-16)

**Yayın**
- [ ] Track kararı S-2'ye göre verildi
- [ ] Production'a aşamalı yayınla çıkıldı

> **Otomatik yayın yapılmadı ve yapılmayacak.** Bu denetimin çıktısı yalnızca
> bu belge ve `docs/` altındaki metin dosyalarıdır.
