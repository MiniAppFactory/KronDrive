# Play Store yayın hazırlık denetimi — Kron Drive (versionCode 10, versionName 1.0.9)

Denetim tarihi: **2026-08-19** (önceki: 2026-08-17) · Denetleyen: `play-store-compliance-engineer`
Kapsam: yalnızca denetim ve bu belge. **Kod değiştirilmedi, Gradle
çalıştırılmadı, cihaza dokunulmadı, git'e dokunulmadı, hiçbir şey yayınlanmadı.**

> **Adım adım ne yapılacağını arıyorsan buraya değil, `PLAY_SUBMISSION_ORDER.md`'ye bak.**
> Bu dosya **denetim tablosudur** (ne kapalı, ne açık, kanıtı ne).
> Arka plandaki uzun analizler yerinde: `STORE_SUBMISSION_CHECKLIST.md`
> (politika analizi), `DATA_SAFETY_FORM.md`, `CONTENT_RATING.md`,
> `ADMOB_SETUP.md`, `play_store_assets/SCREENSHOT_PLAN.md`.

---

## ⚠ Bu denetimin okuma anındaki iki uyarısı

**1. Çalışma ağacı temiz değildi.** Denetim sırasında `git status`:

Denetimin başında `GameConfig.kt`, `GameEngine.kt`, `GameModels.kt`,
`LevelCatalog.kt` ve `LevelCurveTest.kt` değişik durumdaydı; denetim biterken
liste tamamen değişmişti:

```
 M .../ads/InterstitialAdManager.kt        M .../ui/game/GameScreen.kt
 M .../ads/RewardedAdManager.kt            M .../ui/navigation/AppNavigation.kt
 M .../data/DailyChallengeGenerator.kt     M .../test/.../DailyChallengeReachabilityTest.kt
?? .../ads/AdConsentGate.kt                ?? .../test/.../ads/
?? .../test/.../game/ZzHitboxPriceProbeTest.kt
```

Yani **başka ajanlar aynı anda bu dosyaları düzenliyordu**. Denetim
başlarken `GameConfig.kt`'te `INTERSTITIAL_EVERY_N_LEVELS` satır **635**'te
idi; yarım saat sonra aynı sabit satır **728**'de idi. Değerler değişmedi,
satır numaraları kaydı.

Somut sonucu A-3'te: madde ölçüldüğü anda gerçek bir açıktı, belge yazılırken
düzeltmesi indi. **Ölçüm ile teslim arasındaki fark belgede açıkça yazılıdır.**

> **Bu yüzden aşağıdaki her `GameConfig.kt` kanıtında satır numarasına değil
> SABİT ADINA güven.** `grep -n "SABIT_ADI" GameConfig.kt` ile kendin doğrula.

**2. AAB, temiz ve commit edilmiş bir ağaçtan üretilmelidir.** Yukarıdaki beş
değişik dosya commit edilmeden `assembleRelease` alınırsa, yayınlanan APK'nın
hangi koddan çıktığı git'ten geri izlenemez.

---

## 0. Yönetici özeti

| | Sayı | Maddeler |
|---|---|---|
| **Tamam** | 18 | A-1, A-2, A-4, A-5, A-6, A-7, A-8, A-9, B-2, C-2, C-2b, C-3, C-3b, C-4, C-5, D-1, D-2, D-4, D-6, D-7 |
| **Açık — yayını durduran bloker** | **7** | **S-7**, **S-8**, **A-3**, **D-8**, **A-11**, **D-5**, **B-1** |
| **Açık — bloker değil** | 5 | A-10, A-12, C-1, C-6, E-1, E-2 |
| **Sahibinden bilgi/karar gerekli** | 6 | S-1…S-5, S-9 (S-6 kapandı) |
| Öneri (zorunlu değil) | 5 | Ö-2…Ö-6 (Ö-1 kapandı) |

### Blokerler — yapılma sırası

Sıra keyfi değil, **bağımlılık zinciri**: 1-4 kod/metin işidir, 5 onların
üzerine build alır, 6 o build'den ekran görüntüsü çeker. 7 bağımsız,
istenildiği an yapılabilir.

| Sıra | # | Bloker | Kimde | Süre |
|---|---|---|---|---|
| 1 | **S-7** | Başlangıç coini **100.000** + tek seferlik test coini göçü | Kod | ~10 dk |
| 2 | **S-8** | Antrenman modu **açık** (orta şerit hep boş) | Kod | ~10 dk |
| 3 | **A-3** | Onay kapısı **yalnızca banner'a** bağlıydı — geçiş/ödüllü reklam onaysız da isteniyordu. **Kod düzeltildi, test (298/0) ve `assembleRelease` yeşil**; kalan: commit + cihaz doğrulaması | Kod ✔ / commit + cihaz | ~15 dk |
| 4 | **D-8** | Mağaza metni son bölüm için **5.000 m** diyor, kod **3.800 m** | Metin | ~5 dk |
| 5 | **A-11** | Yayınlanabilir güncel AAB yok; `versionCode` hâlâ **10**, AAB'den beri **31 commit** | Build | ~15 dk |
| 6 | **D-5** | Ekran görüntüsü yok (Play en az 2 ister) | Sahibi + cihaz | ~30 dk |
| 7 | **B-1** | Gizlilik politikası **canlı değil** (404) | Sahibi | ~2 dk |

**Sayım:** 7 bloker → **4'ü kodda/metinde** (S-7, S-8, A-3, D-8), **1'i
build'de** (A-11), **2'si sahibinde** (D-5, B-1).

### Bu denetimde (2026-08-19) değişenler

| Madde | Ne oldu | Neden |
|---|---|---|
| **A-3** | **TAMAM → AÇIK** → **kod düzeltildi (aynı gün)** | Önceki denetim "onay çözülmeden reklam istenmiyor" diyordu. Yanlış: bu yalnızca banner için doğruydu. Kapı `ads/AdConsentGate.kt` ile beş çağrı yerine de bağlandı; **298 test / 0 hata** ve `assembleRelease` yeşil. Madde commit + cihaz doğrulaması için açık |
| **A-4** | Tablo düzeltildi | `INTERSTITIAL_EVERY_N_ENDLESS_RUNS` **3 değil 1** |
| **C-3b** | Satır düzeltildi | "3 koşu" → **1 koşu**; ayrıca "TEKRAR DENE" reklamı eklendi |
| **A-11** | Boyut kanıtı geri çekildi | Alıntılanan APK dosyası **artık yok** (aşağıda) |
| **A-12** | **YENİ (bloker değil)** | Sonsuz modda reklam oranı AdMob sınırında, payı sıfır |
| **D-8** | **YENİ · BLOKER** | Son bölüm hedefi 5.000 → 3.800 m değişti, mağaza metni eskisini yazıyor |
| **S-9** | **YENİ (sahibi kararı)** | "Boğa 67" adı kodda **GEÇİCİ** ama iki mağaza metnine de girmiş |
| **S-8** | Yeri düzeltildi | Önceki sürümde yanlışlıkla A-9 ile A-10 arasına düşmüştü |

---

## A. Teknik / kod tarafı

### A-1 · targetSdk ve minSdk — **TAMAM**
`source/app/build.gradle.kts`: `minSdk = 24` (satır 14), `targetSdk = 36`
(satır 15), `compileSdk` 36 (satır 10). Play'in güncel eşiğinin üstünde.

### A-2 · İzin denetimi — **TAMAM, bugün de değişmedi**
`AndroidManifest.xml:5-6` yalnızca iki izin beyan ediyor:

- `android.permission.INTERNET`
- `android.permission.ACCESS_NETWORK_STATE`

Kamera, mikrofon, konum, rehber, depolama, bildirim, `QUERY_ALL_PACKAGES` **yok**.
Bugünkü (18-19 Ağustos) ~13 commit — antrenman modu, seviye atlama bedeli,
sonsuz mod hız merdiveni, yeni görev tipi, kazara tıklama kilidi, geri tuşu
düzeltmesi — **tek bir izin bile eklemedi**.

Bağımlılık listesi de değişmedi (`build.gradle.kts:69-84`): ağa çıkan yalnızca
`play.services.ads` ve `user.messaging.platform`. Analytics, Crashlytics,
Firebase, Billing **yok**.

`play-services-ads` birleşme yoluyla `AD_ID` ve `ACCESS_ADSERVICES_*` ekliyor
(kaldırılmamalı). Sonucu C-4'e bağlı.

### A-3 · UMP / GDPR onay akışı — **AÇIK (kod TAMAM, commit + cihaz kanıtı bekliyor) · 2026-08-19**

> Önceki iki denetim bu maddeyi **TAMAM** yazdı. **Yanlıştı.** Doğrulama
> `ConsentManager.kt` ve `MainActivity.kt`'te durup orada bitmiş; bayrağın
> nereye gittiği izlenmemiş.

**Ne doğru:**

- `ads/ConsentManager.kt` — UMP akışı düzgün: `requestConsent` her iki dalda
  da `info.canRequestAds()` kontrolünden geçiyor, `canRequestAds(context)`
  güvenlik ağı için ayrıca açık.
- `MainActivity.kt:44` onay akışını başlatıyor, `MainActivity.kt:52-53`
  4 saniyelik güvenlik ağı **koşulsuz açmıyor** — `ConsentManager.canRequestAds()`
  true ise açıyor.
- Ayarlarda "Gizlilik seçenekleri" girişi var (`isPrivacyOptionsRequired` →
  `showPrivacyOptions`).

**Ne yanlış — kanıt:**

`adsConsentResolved` bayrağının **bütün** kullanım yerleri
(`grep -rn "adsConsentResolved"`):

| Dosya:satır | Ne yapıyor |
|---|---|
| `MainActivity.kt:27,44,52-53,58,73` | Bayrağı üretiyor, `AppNavigation`'a veriyor |
| `AppNavigation.kt:46,72,86,105,136,147` | **Beş** ekrana aktarıyor |
| `MainMenuScreen.kt:69`, `GarageScreen.kt:91`, `LevelMapScreen.kt:115`, `MissionsScreen.kt:72`, `SettingsScreen.kt:60` | Hepsi tek bir yere: `showBanner = adsConsentResolved` |

Yani bayrak **yalnızca banner'ı** kapatıyor. Buna karşılık tam ekran reklam
çağrı yerleri:

| Dosya:satır | Çağrı | Onay kontrolü |
|---|---|---|
| `ui/game/GameScreen.kt:815` | `RewardedAdManager.loadAndShow` | **yok** |
| `ui/game/GameScreen.kt:870` | `RewardedAdManager.loadAndShow` | **yok** |
| `ui/game/GameScreen.kt:922` | `InterstitialAdManager.loadAndShow` (TEKRAR DENE) | **yok** |
| `ui/game/GameScreen.kt:961` | `InterstitialAdManager.loadAndShow` (`withOptionalInterstitial`) | **yok** |
| `ui/navigation/AppNavigation.kt:112` | `RewardedAdManager.loadAndShow` | **yok** |

`GameScreen.kt` **`adsConsentResolved` parametresini hiç almıyor** ve dosyada
tek bir `ConsentManager` referansı yok. `InterstitialAdManager.kt` ve
`RewardedAdManager.kt` de `canRequestAds`'i hiç çağırmıyor — ikisi de doğrudan
`AdRequest.Builder().build()` ile yükleme başlatıyor.

**Sonuç:** AEA/Birleşik Krallık'ta UMP formunda onayı **REDDEDEN** bir
kullanıcı banner görmüyor ama **geçiş ve ödüllü reklam görüyor**. Bu, Google'ın
EU User Consent Policy'sine karşı gerçek bir açıktır ve AdMob tarafında yayın
sonrası uygulama askıya alınmasına kadar gidebilir.

**Bir düzeltme denetim sırasında indi — ama henüz kanıtlanmadı.**

Yukarıdaki ölçüm 02:4x'te yapıldı; o an `ads/AdConsentGate.kt` dosya
sisteminde vardı ama **git'te takipsizdi ve hiçbir yerden çağrılmıyordu**
(`grep` → kendi dosyası dışında sıfır kullanım), testi de yoktu.

Belge yazılırken paralel bir ajan kapıyı bağladı. **Yeniden ölçtüm** — şu an
çalışma ağacında:

| Yer | Durum |
|---|---|
| `GameScreen.kt:196-199` | `adsAllowed` üretiliyor: `AdConsentGate.adsAllowed(mandal, ConsentManager.canRequestAds(context))` |
| `GameScreen.kt:850`, `908-909` | Ödüllü **teklif** `shouldOfferRewarded` ile kapatılıyor (buton hiç çıkmıyor) |
| `GameScreen.kt:981`, `1038-1046` | Geçiş reklamı `shouldShowInterstitial` / `adsAllowed` kapısından geçiyor |
| `AppNavigation.kt:122-136` | Garajdaki ödüllü de kapıya bağlandı |
| `InterstitialAdManager.kt:42`, `RewardedAdManager.kt:40` | İkinci savunma hattı: yükleme öncesi `ConsentManager.canRequestAds` |
| `test/.../ads/AdConsentGateTest.kt` | Dosya oluşturuldu |

Yani **beş çağrı yerinin beşi de** artık kapılı görünüyor.

**Kanıt standardının kod tarafı karşılandı (2026-08-19, `admob-monetization-engineer`).**
Yukarıdaki üç eksikten ikisi kapandı:

| Şart | Durum | Kanıt |
|---|---|---|
| `:app:testDebugUnitTest` yeşil | **TAMAM** | `BUILD SUCCESSFUL` (`--rerun-tasks`, filtresiz) · `app/build/test-results/testDebugUnitTest` → **298 test, 0 failure, 0 error** |
| `AdConsentGateTest.kt` geçiyor | **TAMAM** | `TEST-…ads.AdConsentGateTest.xml` → `tests="13"`, failure yok |
| `:app:assembleRelease` | **TAMAM** | `BUILD SUCCESSFUL in 2m 39s`; `lintVitalRelease` dâhil geçti |
| Commit | **YAPILMADI** | Görev kapsamı git'e dokunmayı yasakladı; ağaçta duruyor |
| Cihazda E-2 senaryosu | **YAPILMADI** | adb başka ekipte (görev kapsamı dışı) |

**Ödül yolu bozulmadı:** ödül hâlâ yalnızca SDK'nın gerçek "kazanıldı"
callback'inde veriliyor (`RewardedAdManager.kt` → `rewardedAd.show(activity) { onRewardEarned() }`).
Kapı yalnızca **teklifi** kapatıyor, ödül mantığına dokunmuyor.

**Akış bloklanmıyor:** onay yokken geçiş reklamı **atlanıyor ve `proceed()`
anında çalışıyor**; ödüllü butonu ise hiç çizilmiyor — yani "izle" deyip
hiçbir şey vermeyen bir buton yok (CLAUDE.md kural 4).

**Sıklık sayacı yakılmıyor:** onay kontrolü `consumeRetryInterstitial()` ve
`onInterstitialShown()` çağrılarından **önce** yapılıyor. Aksi hâlde onayı
reddeden oyuncuda gösterilmeyen reklamlar için sayaç ilerler, oyuncu onayı
sonradan verdiğinde reklam sırası kaymış olurdu.

**Madde AÇIK kalıyor** — ama artık gerekçesi kod değil, kapsam:

- **Commit yok.** Değişiklikler çalışma ağacında (`ads/AdConsentGate.kt`,
  `ads/InterstitialAdManager.kt`, `ads/RewardedAdManager.kt`,
  `ui/game/GameScreen.kt`, `ui/navigation/AppNavigation.kt`,
  `test/…/ads/AdConsentGateTest.kt`).
- **Cihaz doğrulaması yok.** E-2 senaryosu (onayı geri çek → koşu bitir →
  hiçbir tam ekran reklam çıkmasın) hâlâ cihazda denenmedi.
- **Garajda kozmetik bir açık kaldı.** Onay yokken "İZLE" butonu **hâlâ
  görünüyor**; basılınca reklam **istenmiyor** (politika açığı kapalı) ama
  ekran "Reklam yüklenemedi. İnternet bağlantını kontrol et." diyor — sebep bu
  değil. Doğrusu butonun hiç çıkmaması. `GarageScreen.kt`'e
  `rewardedOfferAllowed` parametresi gerekiyor; o dosya bu görevin kapsamı
  dışındaydı (paralel ajan çalışıyordu). **Yayını durdurmaz.**

**Kapanma şartı:** commit + cihazda E-2 senaryosu.

### A-4 · Reklam politikası uyumu — **TAMAM (frekans tablosu düzeltildi)**

Doğrulananlar:

- Oyun ekranında banner **yok** — `grep -n "showBanner\|BannerAdView" GameScreen.kt`
  → **sıfır eşleşme**
- App Open Ads **yok** — `grep -rn "AppOpenAd" source/app/src/` → **sıfır eşleşme**
- Geçiş reklamı yalnızca koşu **bittikten sonra**
- Ödül yalnızca SDK'nın gerçek "kazanıldı" callback'inde
  (`RewardedAdManager.kt` → `rewardedAd.show(activity) { onRewardEarned() }`)
- Reklam yüklenemezse akış devam ediyor (`InterstitialAdManager` `proceedOnce`
  deseni, her dalda tam bir kez)

**Frekans sabitleri — 2026-08-19 ölçümü:**

| Sabit | Değer | Önceki denetimde yazan |
|---|---|---|
| `INTERSTITIAL_AFTER_EVERY_RUN` | **false** | (yoktu) |
| `INTERSTITIAL_EVERY_N_LEVELS` | 3 | 3 ✔ |
| `INTERSTITIAL_EVERY_N_ENDLESS_RUNS` | **1** | ~~3~~ **yanlıştı** |
| `INTERSTITIAL_EVERY_N_RETRIES` | **1** | (yoktu) |
| `INTERSTITIAL_FREE_LEVELS` | 3 | 3 ✔ |
| `INTERSTITIAL_MIN_RUN_SECONDS` | 10 | 10 ✔ |
| `REWARDED_COIN_AMOUNT` | 150 | — |
| `REWARDED_COIN_DAILY_LIMIT` | 5 | — |

(Hepsi `game/GameConfig.kt`; satır numaraları denetim sırasında kaydı — sabit
adıyla arayın.)

**Play politikası açısından ihlal yok:** koşu içinde tam ekran reklam yok,
uygulama açılışında reklam yok, geri sayım/oyun bloklaması yok. Play frekansı
düzenlemez; *beklenmedikliği* düzenler. Sonsuz moddaki artışın AdMob tarafındaki
ayrı riski **A-12**'de.

**Kazara tıklama riski kapatıldı (2026-08-19, commit `1fa38e7`):** geçiş
reklamı önceden yüklenmediği için butona basıldıktan sonra 0,5-3 sn hiçbir şey
olmuyordu; kilitsiz ikinci basış reklamı parmağın altında açıyordu. `adInFlight`
kilidi artık TEKRAR DENE ve ANA MENÜ yollarında da var
(`GameScreen.kt:918-940`); butonlar "YÜKLENİYOR…" yazıp devre dışı kalıyor.
Ödüllü reklam butonlarında kilit zaten vardı.

Not: `game/AdFrequency.kt` muafiyeti yalnızca `RunMode.CAREER`'a veriyor;
günlük görev sayacı artırmıyor ama sayaç doluysa reklam gösterebiliyor. Bu
tasarım bilinçli ve belgeli.

### A-5 · Gerçek AdMob kimlikleri — **TAMAM**
`ads/AdIds.kt`: `USE_TEST_IDS_IN_RELEASE = false` (satır 33), üç üretim birimi
girili (`pub-8582550349019790`: banner `/6482084182`, geçiş `/2236501231`,
ödüllü `/5169002519`). `AndroidManifest.xml:24-26`: App ID
`ca-app-pub-8582550349019790~2279115293`. Debug build test reklamı gösteriyor
(`useTestIds = BuildConfig.DEBUG || USE_TEST_IDS_IN_RELEASE`).

### A-6 · `appCategory="game"` — **TAMAM**
`AndroidManifest.xml:10`.

### A-7 · Hesap silme gereksinimi — **KAPSAM DIŞI (doğrulandı)**
Kural yalnızca **hesap oluşturma sunan** uygulamaları bağlar. Kod tabanında
hesap/giriş/kayıt yok; ağa giden tek bileşen reklam SDK'sı; kullanıcı verisi
yalnızca cihazdaki DataStore'da (`kron_drive_progress`). **Console'da hesap
silme URL'i istenmeyecek.**

Kullanıcı beklentisi gizlilik politikasının 7. bölümüyle karşılanıyor.

### A-8 · İmzalama ve keystore — **TAMAM**
- Keystore var: `source/my-upload-key.jks` + `source/signing.properties`, alias `UPLOAD`
- **Sır sızıntısı yok:** `git ls-files` taraması `*.jks` / `signing.properties`
  için sıfır eşleşme; ikisi de `.gitignore`'da
- `builds/` `.gitignore`'da
- `build.gradle.kts:31-42`: keystore dosyası yoksa release `signingConfig` hiç
  tanımlanmıyor (başka makinede `assembleRelease` patlamıyor); sırlar ortam
  değişkeni ya da `signing.properties`'ten okunuyor, kaynak kodda değil

**Keystore ORTAK kullanılıyor — proje sahibi kararı, 2026-08-16.** Sertifika DN
`CN=Blast the Blocks, OU=AppDeveloper, O=AppDeveloper, L=Istanbul, C=TR`;
SHA-256 `1497473b7f18d1890b43254623605a65c35bfee9e627b0105e4b23bc22bde2d0`.
Play tek anahtarın birden çok uygulamada kullanılmasını engellemez.

> Alias `UPLOAD` olduğu için bu bir **upload** anahtarıdır: Play App Signing
> kayıtlıysa kaybolduğunda Google'dan sıfırlatılabilir.
>
> ⚠ **Bu, Play konsolunda DOĞRULANMADI.** Kayıt yoksa anahtar kaybı kalıcıdır.
> Sahibi Console'dan teyit etmeli (`PLAY_SUBMISSION_ORDER.md` Adım 7).

### A-9 · Uygulama içi sürüm metni — **TAMAM (2026-08-17'de kapandı)**
`ui/settings/SettingsScreen.kt:268-269` (satır kaydı, önceki denetimde 238-239):

```
tr = "Sürüm ${BuildConfig.VERSION_NAME}",
en = "Version ${BuildConfig.VERSION_NAME}"
```

Bir daha eskimez. **Madde kapalı.**

### A-10 · `developerTestDeviceIds` boş — **AÇIK (gelir riski, politika değil)**
`ads/AdIds.kt` son satırı → `val developerTestDeviceIds: List<String> = emptyList()`.

Release build gerçek reklam gösteriyor. Sahibi kendi cihazında release APK'yı
test ederken bir reklama dokunursa AdMob **geçersiz trafik** sayar ve hesap
askıya alınabilir. Test cihazının AdMob ID'si (ilk reklam isteğinde logcat'te
`Ads` etiketiyle yazılır) listeye eklenmeli.

⚠ Ek uyarı (2026-08-19): sabit **tanımlı ama hiçbir yerde kullanılmıyor** —
`RequestConfiguration.Builder().setTestDeviceIds(...)` çağrısı kodda yok. Yani
listeye ID yazmak tek başına yetmez; `MainActivity`'deki `MobileAds.initialize`
öncesinde `MobileAds.setRequestConfiguration(...)` de eklenmelidir.

**Yayını durdurmaz ama Adım 7'deki cihaz testinden ÖNCE yapılmalı.**

### A-11 · Yayınlanabilir güncel AAB yok — **AÇIK · BLOKER**

Elde tek güncel-adaylı release AAB: `builds/KronDrive_release_2026-08-15_2351_v1.0.9.aab`
(8.260.586 bayt, 2026-08-15 23:51). **Bu dosya bugünkü oyunu içermiyor.**

Kanıt (`git log --since="2026-08-15 23:51"`): **31 commit**, bunların
`source/` altına dokunanı **196 dosya değişikliği**. İçlerinde:

```
1fa38e7 Kazara tiklama kilidi, kayit codec'i test edilebilir, gunluk combo kaldirildi
b233679 Otonom oturum acik bulgulari + ekonomi yeniden turetimi
cae011b Otonom oturum: kritik geri tusu hatasi + olcum hijyeni + gorev baglantisi
6cc66e1 Antrenman modu (gecici), seviye atlama bedeli, "Lv 4" sorusu
94b8097 Sonsuz mod arac farkini yiyordu; garaj artik rakam yaziyor
3479cdb Gecis hedefleri olcumden turetildi (4. bolum imkansizdi)
7b33302 Sonsuz modda reklamli TEKRAR DENE + bedava reset kacagi kapatildi
30f6612 Takilan araclar, ikondaki unlem, geri tusu, dunya hizi ve arac merdiveni
c3d5a51 Uc yeni arac: Motosiklet, Tir, F1 + olcu sinifi altyapisi
… (toplam 31)
```

Bunların arasında **veri kaybettiren kritik bir hata düzeltmesi** var
(`cae011b`: çarpışma perdesinde geri tuşu koşunun coin/XP/görev/rekorunu
siliyordu). O düzeltme olmadan yayınlanacak bir build kabul edilemez.

Ayrıca `build.gradle.kts:16-17` hâlâ `versionCode = 10`, `versionName = "1.0.9"`.
Play aynı `versionCode` ile ikinci yükleme kabul etmez.

**Gereken sıra:** S-7 + S-8 + A-3 kapatılacak → değişiklikler commit edilecek →
`versionCode` 11'e çıkarılacak (öneri `versionName = "1.1.0"`, Ö-6) → yeni
imzalı AAB.

> Bu madde **build-release-engineer'ın** işidir. Bu denetimde Gradle
> çalıştırılmadı (görev sınırı).

**⚠ Önceki denetimin boyut kanıtı geri çekildi.** 2026-08-17 sürümü
"`builds/KronDrive_test_2026-08-17_2158_v1.0.9.apk` = 4.840.882 bayt" yazıyordu.
**O dosya `builds/` altında yok** (bugün `ls` ile bakıldı). Klasördeki en yeni
test APK'sı `KronDrive_test_2026-08-16_1515_v1.0.9.apk` (23.873.682 bayt — test
build, karşılaştırılabilir değil); en yeni **release** APK
`KronDrive_release_2026-08-15_2351_v1.0.9.apk` (4.472.274 bayt).
**Bugünkü kodun boyutu ölçülmedi.** Play'in 150 MB AAB sınırının çok altında
kalacağı neredeyse kesin ama bu bir tahmindir, ölçüm değil.

### A-12 · Sonsuz modda reklam oranı AdMob sınırında — **AÇIK (bloker değil)**

Bugünkü iki değişiklik birleşince sonsuz modda reklam yükü üç katına çıktı:

- `INTERSTITIAL_EVERY_N_ENDLESS_RUNS = 1` → her koşu sonunda reklam
- `INTERSTITIAL_EVERY_N_RETRIES = 1` → her TEKRAR DENE'de reklam
- Sonuç ekranındaki sistem geri tuşu da artık aynı reklam kapısından geçiyor
  (bedava reset kaçağı kapatıldı, `7b33302`)

Sahibinin açık isteği: *"tekrar dene deyince reklam çıksın, geri tuşuna basınca
da reklam çıksın ki ücretsiz reset şansı olmasın"*.

**Play politikası ihlali değil** — Play frekans düzenlemiyor. Risk **AdMob**
tarafında: politika iki kullanıcı eylemine en fazla bir geçiş reklamı istiyor.
Mevcut döngü (çarp → SONUÇLARI GÖR → TEKRAR DENE → reklam) tam **1/2**, yani
sınırın üstünde ama **payı sıfır**. Sonuç ekranına tek bir ara adım eklenmesi
ya da bir sabitin 1'den 2'ye çıkması bu sınırı aşar.

`AdEconomyTest`'e bu iki eşik için assert eklendi (aynı commit) — sayı sessizce
değişemez. Geri almak isteniyorsa tek satır. **Karar sahibinin; yayını
durdurmuyor.**

---

## S-7 ve S-8 · Yayına çıkmaması gereken geçici değerler

Bu ikisi "S" bölümünde numaralı ama teknik iş oldukları için kanıtları burada.

### S-7 · Başlangıç coini test değerinde — **AÇIK · BLOKER**

**Üç ayrı yerde iz bırakıyor, üçü de temizlenmeli:**

| # | Dosya:satır | Ne var |
|---|---|---|
| 1 | `data/PlayerProgress.kt:134` | `const val STARTING_COINS = 100_000` — yayın değeri hemen altında: `STARTING_COINS_RELEASE = 100` (satır 137) |
| 2 | `data/GameStateRepository.kt` | `Keys.TEST_COINS_GRANTED` (~satır 40) + `suspend fun grantTestCoinsOnce()` (~satır 137) — **mevcut profillere de** tek seferlik 100.000 yükleyen göç |
| 3 | `ui/KronViewModel.kt:46` | `viewModelScope.launch { repository.grantTestCoinsOnce() }` — açılışta çağrı |

**İyi haber:** `grantTestCoinsOnce()` ilk satırında
`if (STARTING_COINS == STARTING_COINS_RELEASE) return` yapıyor. Yani (1)
düzeltilirse (2) ve (3) **kendiliğinden ölü koda** dönüşür ve zarar vermez.
Yine de üçü birlikte silinmeli: DataStore anahtarı (`test_coins_granted`)
oyuncunun cihazında kalıcıdır ve ileride başka bir göçle karışabilir.

**Bu değerle yayına çıkılırsa:** 11 aracın hepsi (en pahalısı Formula 5.000),
11 boya ve dört yükseltme dalının tamamı (16.800 coin) ilk saniyede alınabilir.
Ekonomi tamamen anlamsızlaşır.

Bir test bu değeri kilitliyor (`PlayerProgressCarTest`) — release hazırlanırken
kırılıp hatırlatma yapması bekleniyor. **Not: bu iddia bu denetimde
çalıştırılarak doğrulanmadı** (Gradle yok, görev sınırı).

### S-8 · Antrenman modu açık — **AÇIK · BLOKER**

`game/GameConfig.kt:40` → `const val TRAINING_MODE_SIDE_LANES_ONLY = true`
(denetim anında satır 40; sabit adıyla arayın).

Açıkken trafik yalnızca en sol ve en sağ şeritte doğuyor, **orta şerit hep
boş** (uygulanışı `game/GameEngine.kt:571` civarındaki `spawnObstacle` dalı).
Sahibi test kolaylığı için istedi ve *"aab yaparken sileriz"* dedi.

**Yayına bu açık çıkarsa:** oyuncu orta şeritte durup sonsuza kadar hayatta
kalır. Oyun çökmez; bütün zorluk eğrisi, skor dengesi ve bölüm hedefleri
anlamsızlaşır — sessiz bir felaket.

⚠ **Ölçüm hijyeni uyarısı (CHANGELOG 2026-08-19 (3)):** sabit `const val`
olduğu için testler onu kapatamıyordu; orta şerit boşken otopilot hiç
çarpmıyor ve `LevelCurveTest` "bu hedef ulaşılabilir" diyordu. **İki ayrı ajan
bağımsız olarak bu tuzağa düştü.** `GameEngine`'e `sideLanesOnly` parametresi
eklenerek testler ayrıldı. Yani sabit `true` iken alınan **hiçbir denge ölçümü
güvenilir değildir**.

**Yapılacak:** sabiti kaldır, `spawnObstacle` içindeki dalı ve
`GameEngineTest`'teki "antrenman modu" testini sil.

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
| `docs/.nojekyll` | (Jekyll'i kapatır — dosya mevcut, doğrulandı) |

**Kanıt — bugün de canlı DEĞİL (2026-08-19, `curl`):**

```
404  https://miniappfactory.github.io/KronDrive/
404  https://miniappfactory.github.io/KronDrive/tr/
```

Tek eksik GitHub arayüzündeki **Pages ayarı**. Adımlar:
`PLAY_SUBMISSION_ORDER.md` Adım 1.

> **İçerik kontrolü (2026-08-19):** politikada sayılan cihaz verileri —
> coin/XP, yıldız, yükseltme, sahip olunan araç ve boya, rekorlar, görev
> durumu, reklam sayaçları, tercihler — bugünkü kodla **hâlâ örtüşüyor**.
> Bugün eklenen tek yeni kalıcı anahtar `test_coins_granted` (S-7) ve o zaten
> yayına çıkmayacak. **Politika metninde değişiklik gerekmiyor.**

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
Tam cevap seti: `docs/DATA_SAFETY_FORM.md`.

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

### C-2b · 18-19 Ağustos değişikliklerinin Data Safety'ye etkisi — **YOK (doğrulandı)**

| Kontrol | Sonuç | Kanıt |
|---|---|---|
| Yeni izin var mı? | **Hayır** | `AndroidManifest.xml:5-6` — hâlâ yalnızca `INTERNET` + `ACCESS_NETWORK_STATE` |
| Yeni bağımlılık / SDK var mı? | **Hayır** | `build.gradle.kts:69-84` — ağa çıkan tek şey `play.services.ads` + `user.messaging.platform` |
| Yeni veri kalemi cihazdan çıkıyor mu? | **Hayır** | Yeni görev tipi (`BOOST_DISTANCE`), seviye atlama bedeli, sonsuz mod hız merdiveni, antrenman modu — hepsi yalnızca DataStore'a yazıyor ya da bellekte kalıyor; dışarı gönderen kod yok |

**Sonuç: `DATA_SAFETY_FORM.md` olduğu gibi geçerli, güncelleme gerekmiyor.**

⚠ Tek nüans: A-3 kapatılınca *davranış* değişir (onay yokken reklam istenmez)
ama *toplanan veri kalemleri* değişmez. Form yine aynı kalır.

### C-3 · İçerik derecelendirme (IARC) — **CEVAPLAR HAZIR, giriş bekliyor**
Tam cevap seti: `docs/CONTENT_RATING.md`.

Özet: kategori **Oyun** · şiddet yok (araç çarpışması var; kan, yaralanma,
insan hedefi yok) · cinsellik yok · argo yok · madde yok · korku yok ·
kullanıcı etkileşimi yok · **reklam içerir → Evet** · konum paylaşımı yok ·
satın alma yok.

**Kumar/loot box → Hayır, yeniden doğrulandı (2026-08-19):** haftalık sandık
**sabit** ödül veriyor — `data/WeeklyMissionGenerator.kt:13`
`WEEKLY_CHEST_COINS = 750`, satır 16 `WEEKLY_CHEST_BOOSTER = BoosterType.SECOND_CHANCE`.
Rastgelelik yok, gerçek parayla alınamıyor. "Loot box" tanımını karşılamıyor.

Beklenen sonuç: **PEGI 3 / ESRB Everyone / USK 0**, "reklam içerir" notuyla.

### C-3b · 18-19 Ağustos değişikliklerinin derecelendirmeye etkisi — **YOK (doğrulandı)**

| Değişiklik | Derecelendirmeye etkisi |
|---|---|
| Dünya hızı %40 azaltıldı | Yok |
| Araç merdiveni (hız yayılımı %11 → %108) | Yok — şiddet/tema değişmiyor, çarpışma yine kansız |
| Sonsuz mod hız tavanı 240 → 280 km/s | Yok |
| Seviye atlama bedeli (coin ile seviye atlama) | Yok — **gerçek para yok**, yalnızca oyun içi coin |
| Yeni görev tipi `BOOST_DISTANCE`, günlük combo kaldırıldı | Yok |
| Reklam sıklığı (**sonsuz modda her koşu + her tekrar**, kariyerde 3 koşu, ilk 3 bölüm muaf) | Yok — "reklam içerir" cevabı zaten Evet |
| Antrenman modu (S-8) | Yok — zaten yayına çıkmayacak |

**Sonuç: `CONTENT_RATING.md` olduğu gibi geçerli, anket cevapları değişmiyor.**

> Önceki denetim bu satırda "reklam sıklığı (3 koşu…)" yazıyordu. **Bayattı** —
> sonsuz mod eşiği 1. Derecelendirme cevabı yine değişmiyor, ama sayı düzeltildi.

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
| Uygulama içi satın alma | **Yok** (Billing kütüphanesi bağımlılıklarda dahi yok — `build.gradle.kts:69-84`) |
| Government / Financial / Health / News / COVID-19 | Hepsi Hayır |
| Hesap silme URL'i | **İstenmeyecek** (A-7) |
| Data safety → veri toplanıyor mu | **Evet** (C-2) |

### C-6 · AdMob tarafındaki GDPR mesajı — **DOĞRULANAMADI**
UMP formu AdMob konsolunda **yayımlanmadıysa** AEA/UK kullanıcısına hiç
görünmez ve `canRequestAds()` sürekli false dönebilir.

⚠ A-3 kapatıldıktan sonra bu maddenin ağırlığı **artar**: bugün kapı yalnızca
banner'da olduğu için form yayımlanmasa bile geçiş/ödüllü gelir akmaya devam
eder. Kapı doğru bağlandığında, form yayımlanmamışsa o bölgelerde **tüm reklam
geliri** durur. Yani A-3 ve S-3 birlikte kapatılmalı.

**Konsol erişimim yok — bu denetimde kontrol edilemedi. Sahibi bakmalı** (S-3).

---

## D. Mağaza sayfası (Main store listing)

### D-1 · Başlıklar — **TAMAM**
`play_store_assets/store_titles.md`:
EN `Kron Drive: Retro Car Racer` (27/30) · TR `Kron Drive: Retro Araba Oyunu` (29/30).
Yasaklı ifade yok, üçüncü taraf marka adı yok.

> Not: `store_titles.md` gerekçe belgesi olarak `docs/ASO_STRATEGY_NOTES.md`'ye
> atıf yapıyor ama **o dosya repoda yok**. Uyum sorunu değil, belge borcu.

### D-2 · Kısa açıklama (80 karakter) — **TAMAM, bugün de doğru**

Karakter sayıları bu denetimde yeniden ölçüldü (PowerShell, UTF-8, `.Trim()`):

| Dil | Dosya | Metin | Karakter |
|---|---|---|---|
| EN | `store_short_description_en.txt` | `Dodge traffic, chain Perfect Dodges, race 30 offline arcade levels.` | **67**/80 |
| TR | `store_short_description_tr.txt` | `Trafiği atlat, combo yap, 30 bölümü internetsiz bitir. Retro araba oyunu.` | **73**/80 |

İçindeki üç iddia bugün de doğru:
- "30 levels" → `game/LevelCatalog.kt` içinde `grep -c "LevelDef("` → **30** ✔
- "Perfect Dodges" → mekanik duruyor (`GameConfig.kt`, combo çarpanları
  1.2/1.5/2/3, 6 sn pencere) ✔
- "offline / internetsiz" → sunucu yok ✔

### D-3 · Uzun açıklama — bkz. **D-7** (yapı) ve **D-8** (bugünkü yeni hata)

### D-4 · İkon (512×512) ve feature graphic (1024×500) — **TAMAM, ama bir uyarı**
`play_store_assets/play-store-icon-512.png` (137.098 bayt) ve
`feature_graphic_1024x500.png` (206.298 bayt) mevcut, oyunun gerçek araç
sprite'larını içeriyor. Okunabilirlik testi de var (`icon_48_readability.png`).

> ⚠ **Sahibinin göz kararı gereken nokta:** bu görseller 2026-08-15/16'da,
> başlangıç aracı **Şehir** iken üretildi. Bugün başlangıç aracı **Beety**
> (`CarCatalog.kt:488` → `DEFAULT_SHAPE_ID = SHAPE_BEETY`, `priceCoins = 0`
> satır 1141). Politika ihlali değil, ama ilk izlenim tutmaz. Sahibi bakıp
> "yeter" derse iş yok; demezse `game-art` işi.

### D-5 · Ekran görüntüleri — **AÇIK · BLOKER (değişmedi)**
Telefon için **en az 2**, önerilen 8. Gerçek oyun karesi **yok**.

`play_store_assets/` içeriği bugün listelendi: `screenshot_caption_example_{en,tr}.png`
ve `screenshot_caption_strip_1080x2400.png` (üçü de **şablon**), `previews/`
(tasarım önizlemeleri — mağazaya yüklenmez). Gerçek `screencap` yok.

Plan hazır: **`play_store_assets/SCREENSHOT_PLAN.md`**.

**Plana bu denetimde eklenen dört şart:**

1. ⚠ **Coin sayacı 100.000 gösteren kare yüklenemez** — S-7 geri alınmadan
   çekim yapılırsa bütün kareler çöp olur.
2. ⚠ **Antrenman modu açıkken çekim yapılamaz** — S-8 açıkken orta şerit boş
   görünür; ekran görüntüsü oyunun gerçek zorluğunu yanlış temsil eder
   (yanıltıcı görsel metadata riski).
3. Garaj karesi artık **Beety**'yi göstermeli; araç sayısı gösteren bir kare
   varsa **11 araç** olmalı.
4. Sonsuz mod karesi çekilecekse gösterge tavanı artık **280 km/s**
   (`SPEEDOMETER_MAX_KMH`), 240 değil.

**Sıra kesin: S-7 + S-8 → yeni release build (A-11) → çekim.**

Planın teknik uyarısı geçerli: Samsung S8 ham `screencap` **1440×2960 (≈2.06:1)**
— doğrudan yüklenmez, **1080×1920**'ye kırpılıp ölçeklenmeli. Hiçbir karede
reklam veya "Test Ad" olmayacak; çekim **release** build ile.

### D-6 · Dil ve lokalizasyon — **TAMAM**
EN varsayılan + TR eklenecek; her iki dilin başlık, kısa ve uzun açıklaması
hazır. `res/values-tr/` uygulamada mevcut. TR için ayrı ekran görüntüsü seti
zorunlu değil.

### D-7 · Uzun açıklamanın kodla uyumu — **TAMAM (2026-08-17'de düzeltildi, bugün yeniden denetlendi)**

2026-08-17'de beş yanlış iddia düzeltilmişti (11 gövde/11 boya, araçların fizik
farkı, çarpışma kutusu ölçü sınıfına bağlı, SPEED yükseltmesinin gerçek
mekanizması, perfect dodge yıldız hedefinin kalkması).

Bugün bu beşinin hepsi **yeniden ölçüldü ve hâlâ doğru**:

| İddia | Kanıt (2026-08-19) |
|---|---|
| 11 gövde | `CarCatalog.kt:1416` → `BEETY, HATCHBACK, RACE_SEDAN, KUS_SLX, MOUNTAIN_GOAT, MUSCLE, MUSCLE_67, MOTOSIKLET, SUPERCAR, TIR, F1` |
| 11 boya | `CarCatalog.kt:1509` → `colors` listesinde **11** `CarColorDef` |
| Araçların fizik farkı var | `UpgradeCatalog.kt:175,179,182,193` — dört eksende de `car.*Mul` uygulanıyor |
| Çarpışma kutusu ölçü sınıfından | `VehicleClass.kt:52,61,72` → `MOTOSIKLET(22×59)`, `BINEK(40×76)`, `AGIR(48×202)`; kutu `hitboxWidthPx`/`hitboxHeightPx` ile buradan (satır 95, 98) |
| Perfect dodge yıldız hedefi yok | `grep -c "Objective.PerfectDodges" LevelCatalog.kt` → **0** |
| "Üç görevden ikisi açar" | `GameConfig.kt` → `MIN_STARS_TO_PASS = 2` |
| "Her bölüm 60 km/s" | `grep -c "startSpeedKmh = 60"` → **30/30** |
| "Her dalın 8 seviyesi" | `UpgradeCatalog.kt:41` → `MAX_LEVEL = 8` |
| "İlk üç bölümde reklam yok" | `INTERSTITIAL_FREE_LEVELS = 3` |
| "90 yıldız" | `LevelCatalog.kt` içindeki `Objective.*` sayımı: 22+16+13+13+10+9+4+3 = **90** |
| "Beş haftalık görev" | `WeeklyMissionGenerator.kt` → 5 `MissionType` girdisi |
| "Sonsuz modda her 30 saniyede bir artış" | `GameConfig.kt` → `ENDLESS_STEP_SECONDS = 30f` |

Uzunluklar (bugün yeniden ölçüldü):

| Dosya | Karakter |
|---|---|
| `play_store_assets/store_long_description_en.txt` | **3708**/4000 |
| `play_store_assets/store_long_description_tr.txt` | **3486**/4000 |

### D-8 · Uzun açıklamada YENİ bir yanlış sayı — **AÇIK · BLOKER · YENİ (2026-08-19)**

Metinlerin ikisi de **son bölümün hedefini yanlış yazıyor**:

| Dosya | Yazan |
|---|---|
| `store_long_description_en.txt` (4. paragraf) | "The last one wants **5,000 metres** in 120 seconds." |
| `store_long_description_tr.txt:9` | "Sonuncusu 120 saniyede **5.000 metre** ister." |

**Kod ne diyor** — `game/LevelCatalog.kt:420-427`:

```
id = 30,
goal = LevelGoal.ReachDistance(meters = 3800, timeLimitSec = 120),
startSpeedKmh = 60,
```

**3.800 m**, 5.000 değil. Sebep: 2026-08-18'de dünya hızı %40 azaltıldığında
mesafe hedefleri ×0.75 ölçeklendi (`CHANGELOG` 2026-08-18 (3), madde 6);
mağaza metni 2026-08-17'de yazıldığı için eski sayıda kaldı.

Play'in *yanıltıcı metadata* maddesine giren, oyuncunun oyun içinde kolayca
yalanlayabileceği türden somut bir sayı hatası. **Düzeltmesi iki dosyada tek
satır.**

> **Kural:** bölüm hedefleri her değiştiğinde bu iki metin yeniden okunmalı.
> Bu, 2026-08-17'de düzeltilen beş hatanın aynı sınıfının altıncısıdır.

---

## E. Yayın ve test track

### E-1 · Test track planı — **AÇIK (plan yazıldı, karar S-2'ye bağlı)**
Sıra `PLAY_SUBMISSION_ORDER.md` Adım 7-9'da:
Internal testing → (gerekiyorsa) Closed testing → Production (aşamalı %20/%50/%100).

Kapalı testin **zorunlu olup olmadığı** hesap tipine bağlı → S-2.

### E-2 · Yayın öncesi cihaz doğrulaması — **AÇIK (bu denetimde yapılmadı)**
Görev sınırı gereği cihaza dokunulmadı. Release build'de doğrulanacaklar
listesi `PLAY_SUBMISSION_ORDER.md` Adım 7'de; bu denetimde eklenmesi gereken
**beş satır**:

- Yeni kurulumda başlangıç coini **100** (S-7'nin cihazda kanıtı)
- **Orta şeritte trafik var** — durduğunda çarpıyorsun (S-8'in kanıtı)
- İlk üç bölümde geçiş reklamı çıkmıyor; **4. bölümde çıkıyor**
- Sonsuz modda TEKRAR DENE ve sistem geri tuşu **ikisi de** reklam kapısından
  geçiyor; buton beklerken "YÜKLENİYOR…" yazıp devre dışı kalıyor (A-4)
- **A-3'ün kanıtı:** Ayarlar → Gizlilik seçenekleri → onayı geri çek → koşu
  bitir. **Hiçbir tam ekran reklam çıkmamalı** ve ödüllü reklam butonu
  görünmemeli. Denetimin **ölçüm anındaki** kodda bu test başarısız olurdu;
  düzeltme sonrası geçmesi beklenir ama **hiç denenmedi** (A-3).

### Test ve build kanıtı — **BU DENETİMDE ALINMADI**
`CHANGELOG` 2026-08-19 (3) "263 birim test / 0 hata, `assembleRelease` başarılı"
diyor. **Bu bir alıntıdır, benim ölçümüm değil** — Gradle çalıştırılmadı.
Ayrıca o kanıt, denetim sırasında çalışma ağacında duran beş değişik dosyadan
**öncesine** aittir.

---

## S. Sahibinden bilgi / karar gerekli

| # | Soru | Neden gerekli |
|---|---|---|
| **S-1** | **Hedef kitle yaş grubu onayı?** Öneri: **13-15 / 16-17 / 18+** | Console'da *Target audience and content* zorunlu alan. **12 yaş ve altı işaretlenirse** Families politikası devreye girer: kodda TFCD/TFUA set edilmeli, reklam frekansı gözden geçirilmeli (A-12 ile birlikte) ve **yeni bir AAB** gerekir |
| **S-2** | **Play geliştirici hesabın kişisel mi, organizasyon mu? Ne zaman açıldı?** | Google kişisel hesaplara production öncesi **kapalı test + tester sayısı + süre** şartı uyguluyor; E-1'deki plan buna göre değişir |
| **S-3** | **AdMob → Privacy & messaging → GDPR mesajı PUBLISHED mı?** | A-3 kapatıldıktan sonra kritik hâle gelir: form yayımlanmamışsa AEA/UK'de **tüm** reklam geliri durur (C-6) |
| **S-4** | **Dağıtım ülkeleri ve fiyat?** Öneri: tüm ülkeler, **ücretsiz** | Console zorunlu alanı. ⚠ Ücretsiz seçildikten sonra **ücretliye çevrilemez** |
| **S-5** | **AdMob'da app-ads.txt doğrulaması tetiklendi mi?** | Kökteki dosya canlı (B-2) ama AdMob'un "doğrulandı" durumu kontrol edilemedi |
| **S-7** | ⚠ **BLOKER — başlangıç coini test değerinde** | Kanıt ve üç temizlik noktası yukarıda (S-7 bölümü). Sahibi *"aab yaparken değiştiririz"* demişti — **hatırlatmadır, soru değil** |
| **S-8** | ⚠ **BLOKER — antrenman modu açık** | Kanıt yukarıda (S-8 bölümü). Sahibi *"aab yaparken sileriz"* demişti — **hatırlatmadır, soru değil** |
| **S-9** | **"Boğa 67" adı kalıcı mı?** (YENİ) | `CarCatalog.kt:928-931` → `// GECICI ad — sahibi onaylayana kadar (adaylar: Boğa 67 / Yıldırım GT / Demirtay)`. Ad **iki mağaza metnine de girmiş** (`store_long_description_en.txt` "ELEVEN VEHICLES" paragrafı, `store_long_description_tr.txt:18`). Ad değişirse üç yer birden değişir: kod, EN metin, TR metin. Yayından sonra değiştirmek mağaza güncellemesi gerektirir → **AAB'den önce karara bağlanmalı** |
| ~~S-6~~ | ~~Keystore repo dışında yedeklendi mi?~~ **KAPANDI** | Sahibi 2026-08-16'da yedeklediğini bildirdi. **Beyandır, teknik olarak doğrulanmadı** |

---

## Ö. Öneriler (zorunlu değil)

| # | Öneri | Etki |
|---|---|---|
| ~~Ö-1~~ | ~~Sürüm metnini `BuildConfig.VERSION_NAME`'den oku~~ | **KAPANDI** — `SettingsScreen.kt:268-269` |
| Ö-2 | `AdIds.developerTestDeviceIds`'e test cihazının AdMob ID'sini ekle **ve `MobileAds.setRequestConfiguration` ile bağla** (A-10) | Geçersiz trafik / hesap askıya alınma riski kalkar. **Cihaz testinden önce yapılmalı** |
| Ö-3 | Ayarlara **"İlerlemeyi sıfırla"** düğmesi | Kullanıcının "verilerimi sil" beklentisini uygulama içinde karşılar. `GameStateRepository`'de `clear()`/`reset()` yok |
| Ö-4 | `STORE_SUBMISSION_CHECKLIST.md` §2'deki `app-ads.txt` adresini düzelt (B-2) | Belge yanlış adres gösteriyor |
| Ö-5 | Gizlilik politikasındaki "Son güncelleme: 14 Ağustos 2026" tarihini yayın gününe çek | İçerik değişmediyse şart değil ama daha temiz |
| Ö-6 | `versionName`'i `1.1.0` yap (A-11) | 11 araç, yeniden dengelenmiş ekonomi ve yeni hız merdiveni bir yama değil, bir sürüm |
| Ö-7 | Mağaza metinlerini bölüm hedeflerine bağlayan bir kontrol yaz (D-8) | Aynı sınıftan altıncı hata oldu. Basit bir test — "son bölümün `meters` değeri metinde geçiyor mu" — bu sınıfı kapatır |

---

## Denetimin sınırları — neyi iddia ETMİYORUM

- **Play Console'a erişimim yok.** Play App Signing kaydı, hesap tipi, kapalı
  test şartı, girilen beyanlar — hepsi **doğrulanamadı**, sahibi bakmalı.
- **AdMob konsoluna erişimim yok.** GDPR mesajının yayımlanmış olup olmadığı
  (C-6/S-3) ve app-ads.txt doğrulama durumu (S-5) **doğrulanamadı**.
- **Gradle çalıştırmadım, cihaza dokunmadım, git'e dokunmadım.** Build'in
  geçtiğine, testlerin yeşil olduğuna, oyunun cihazda çalıştığına dair hiçbir
  **kendi** kanıtım yok. `CHANGELOG`'daki "263 test / 0 hata" alıntıdır.
- **APK/AAB boyutu ölçülmedi** (A-11) — önceki denetimin sayısı artık var
  olmayan bir dosyaya dayanıyordu, geri çekildi.
- **Kod değiştirmedim.** S-7, S-8, A-3, D-8 ve A-11 açık; bu belge onları
  kapatmıyor, yalnızca işaret ediyor.
- **`AdConsentGate.kt`'in doğru çalıştığını iddia etmiyorum.** Kapının beş
  çağrı yerine bağlandığını *okuyarak* gördüm; commit edilmemiş, testi
  çalıştırılmamış, cihazda denenmemiş. Kod okuması davranış kanıtı değildir.
- **Çalışma ağacı temiz değildi** ve denetim sırasında **iki kez tamamen
  değişti** (üstteki uyarı). `GameConfig.kt` satır numaraları kaydı — sabit
  **adlarına** güvenin. Bu belgedeki her kanıt, alındığı andaki ağacın
  fotoğrafıdır.
- Kapattığım her maddenin kanıtı **dosya + satır/`grep` sonucu** olarak yukarıda
  yazılı; okuyup kendin doğrulayabilirsin.

> **Otomatik yayın yapılmadı ve yapılmayacak.** Bu denetimin tek çıktısı bu
> belgedir; başka hiçbir dosyaya dokunulmadı.
