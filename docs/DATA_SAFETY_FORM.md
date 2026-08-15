# Play Console — Data Safety (Veri güvenliği) formu cevapları

**Uygulama:** Kron Drive: Retro Racer · `com.miniappfactory.krondrive`
**Hazırlanma tarihi:** 2026-08-14
**Geçerli olduğu sürüm:** versionCode 4 / versionName 1.0.3
**Play Console yolu:** *App content → Data safety*

> Bu belge formun **cevap taslağıdır**, formun kendisi değildir. Console'un soru
> sıralaması ve kelimeleri Google tarafından zaman zaman değiştirilir; ekranda gördüğün
> ifade bu belgeden farklıysa **ekrana** uy ve bu dosyayı güncelle.

---

## 0. Bu cevaplar neye dayanıyor

| Kaynak | Ne için kullanıldı |
|---|---|
| Kod incelemesi (`GameStateRepository.kt`, `ads/`, `MainActivity.kt`, `AndroidManifest.xml`, birleştirilmiş release manifesti) | Oyunun kendisinin ne topladığı, hangi izinlerin geldiği |
| https://developers.google.com/admob/android/privacy/play-data-disclosure | AdMob / Google Mobile Ads SDK'nın topladığı veri listesi |
| https://support.google.com/googleplay/android-developer/answer/10787469 | Formun genel kuralları |
| https://support.google.com/googleplay/android-developer/answer/13327111 | Hesap silme zorunluluğunun kapsamı |

**Doğrulanan teknik gerçekler:**

- Projede tek üçüncü taraf SDK var: `com.google.android.gms:play-services-ads:25.4.0` +
  `com.google.android.ump:user-messaging-platform:4.0.0`. Analitik, çökme raporlama,
  attribution veya sosyal SDK **yok**.
- Uygulamanın kendi manifesti yalnızca `INTERNET` ve `ACCESS_NETWORK_STATE` tanımlıyor.
- Birleştirilmiş **release** manifestinde reklam SDK'sından gelen izinler:
  `com.google.android.gms.permission.AD_ID`, `ACCESS_ADSERVICES_AD_ID`,
  `ACCESS_ADSERVICES_ATTRIBUTION`, `ACCESS_ADSERVICES_TOPICS`, `WAKE_LOCK`,
  `FOREGROUND_SERVICE`.
- Tüm oyuncu verisi DataStore Preferences (`kron_drive_progress`) içinde, cihazda.
  Ağa çıkan tek bileşen reklam SDK'sıdır.

---

## 1. Genel sorular (Data collection and security)

| # | Formdaki soru | Cevap | Gerekçe |
|---|---|---|---|
| 1.1 | Does your app collect or share any of the required user data types? | **Yes** | Oyunun kendisi toplamıyor; **Google Mobile Ads SDK topluyor ve paylaşıyor.** Play, SDK'ların topladığını da senin beyan etmen gerektiğini açıkça söylüyor. |
| 1.2 | Is all of the user data collected by your app encrypted in transit? | **Yes** | Uygulama kendisi hiçbir veri göndermiyor; giden tek trafik Mobile Ads SDK'ya ait ve Google bu verinin TLS ile şifrelendiğini beyan ediyor. Dayanak: AdMob Play data disclosure sayfası. |
| 1.3 | Do you provide a way for users to request that their data be deleted? | **Yes** | Kullanıcı veriyi kendisi tamamen silebiliyor: uygulamayı kaldırmak veya Ayarlar → Uygulamalar → Kron Drive → Depolama → *Verileri temizle*. Hiçbir veri harici bir sunucuda/hesapta tutulmadığı için bu **eksiksiz** bir silmedir. Console bir URL isterse gizlilik politikası adresi verilir — silme yordamı politikanın 7. bölümünde adım adım yazılı. **Boom Blocks ile aynı cevap** (`Boom-Blocks/docs/PLAY_STORE_DATA_SAFETY.md` §0, §3) |
| 1.4 | Has your app's data collection been independently validated against a global security standard? | **No** | Bağımsız güvenlik denetimi yapılmadı. Bu soru opsiyoneldir ve "Hayır" demek ihlal değildir. |

> **1.3 hakkında not.** Google'ın ayrı bir politikası olan **hesap silme** zorunluluğu
> yalnızca *"app enables account creation"* olan uygulamaları kapsar; bizde hesap
> oluşturma akışı yok, dolayısıyla o zorunluluk **kapsam dışı**. Data Safety'deki bu soru
> ise daha geniştir ("verinin silinmesini talep etme yolu") ve cihaz üzerinden tam silme
> mümkün olduğu için **Yes** doğru cevaptır. Kardeş uygulama Kaboom Blocks'ta da aynı
> gerekçeyle **Yes** verilmiştir; portföy genelinde tutarlılık korunuyor.
> *(Console akışının bu soruyu tam olarak nasıl sorduğu ve URL isteyip istemediği ekranda
> doğrulanmalı.)*

---

## 2. Veri türü tablosu

Aşağıdaki tablo formun *Data types* adımındaki her kategoriyi kapsar. "—" işaretli
satırlarda Console hiçbir şey sormayacaktır (kategori işaretlenmez).

### 2.1 Toplanan / paylaşılan veriler (AdMob kaynaklı)

| Play kategorisi | Veri türü | Toplanıyor? | Paylaşılıyor? | Amaç(lar) | Zorunlu mu / Opsiyonel mi | Geçici (ephemeral)? | Kaynak |
|---|---|---|---|---|---|---|---|
| Location | **Approximate location** | Evet | Evet | Advertising or marketing; Fraud prevention, security and compliance; Analytics | Zorunlu (kullanıcı reddedemez) | Hayır | AdMob disclosure — IP adresinden türetilen genel konum. **GPS değil**; uygulama konum izni istemiyor |
| App activity | **App interactions** | Evet | Evet | Advertising or marketing; Analytics; Fraud prevention, security and compliance | Zorunlu | Hayır | AdMob disclosure — reklam gösterimi/tıklaması |
| App info and performance | **Diagnostics** | Evet | Evet | Analytics; Fraud prevention, security and compliance | Zorunlu | Hayır | AdMob disclosure — SDK performans verisi |
| Device or other IDs | **Device or other IDs** (AAID, App set ID, hesap tanımlayıcıları) | Evet | Evet | Advertising or marketing; Analytics; Fraud prevention, security and compliance | **Opsiyonel** | Hayır | AdMob disclosure |

> **Not — "Opsiyonel" işareti.** AdMob belgesi *Android ad ID* satırını "optional" olarak
> gösteriyor (kullanıcı cihaz ayarlarından silebilir, AEA/UK'de UMP formunda
> reddedebilir); *App set ID* ve *Account identifiers* satırlarını "required" olarak.
> Console tek bir "Device or other IDs" kutusu sunduğu için tek bir seçim yapılıyor.
> **Opsiyonel** seçilmesi hem Google'ın kendi "optional" işaretiyle hem de Kaboom
> Blocks'taki cevapla (`PLAY_STORE_DATA_SAFETY.md` §1) tutarlıdır — portföy genelinde aynı
> beyan veriliyor. *(Console'un o adımda alt kırılım sunup sunmadığı ekranda
> doğrulanmalı.)*

### 2.2 Toplanmayan kategoriler (hepsi "No" işaretlenecek)

| Play kategorisi | Cevap | Gerekçe (kod kanıtı) |
|---|---|---|
| Personal info (isim, e-posta, adres, telefon, ırk/etnik köken, siyasi/dinî görüş, cinsel yönelim, kimlik belgesi, diğer) | **Hayır** | Hesap yok, form yok, giriş yok. `GameStateRepository` içindeki hiçbir anahtar kişisel veri değil |
| Financial info (satın alma geçmişi, ödeme bilgisi, kredi notu, diğer) | **Hayır** | Uygulama içi satın alma **yok**; Billing kütüphanesi projede bağımlılık olarak dahi bulunmuyor. Coin yalnızca oyun içi, gerçek parayla alınamıyor |
| Health and fitness | **Hayır** | İlgili API yok |
| Messages (e-posta, SMS, diğer) | **Hayır** | Mesajlaşma yok, SMS izni yok |
| Photos and videos | **Hayır** | Kamera/galeri erişimi yok |
| Audio files (ses kaydı, müzik dosyaları) | **Hayır** | Ses **üretiliyor** (AudioTrack ile sentez), kaydedilmiyor. Mikrofon izni yok |
| Files and docs | **Hayır** | Depolama izni yok |
| Calendar | **Hayır** | — |
| Contacts | **Hayır** | — |
| Location → **Precise location** | **Hayır** | `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` izni **yok** (birleştirilmiş manifestte de yok) |
| Web browsing history | **Hayır** | — |
| App activity → In-app search history, Installed apps, Other user-generated content, Other actions | **Hayır** | Arama yok, kurulu uygulama okuma yok, kullanıcı içeriği yok |
| App info and performance → **Crash logs** | **Hayır** | Çökme raporlama SDK'sı **yok** (Crashlytics/Sentry vb. bağımlılık listesinde yok). Google Play'in kendi otomatik çökme toplaması beyan gerektirmez |
| App info and performance → Other app performance data | **Hayır** | — |

---

## 3. "Data is processed ephemerally" ve diğer işaretler

| Soru | Cevap | Not |
|---|---|---|
| Is this data processed ephemerally? | **Hayır** (tüm AdMob satırları için) | Google'ın kendi disclosure tablosu bu satırları "ephemeral: No" gösteriyor |
| Is this data required for your app, or can users choose whether it's collected? | 2.1'deki tabloya göre | AEA/UK'de UMP formu üzerinden reddedilebilir; ama SDK reklam sunumu için yine de temel veri işler |
| Is data collected by a third party SDK? | **Evet** — Google Mobile Ads SDK | Formda ayrı bir kutu olmayabilir; "collected/shared" beyanı SDK'yı da kapsar |

---

## 4. Bu formla birlikte doldurulması gereken diğer beyanlar

| Console alanı | Cevap | Gerekçe |
|---|---|---|
| *App content → Ads* → "Does your app contain ads?" | **Yes, my app contains ads** | Banner + interstitial + rewarded. Mağaza sayfasında "Contains ads" rozeti çıkar |
| *App content → Advertising ID* → "Does your app use advertising ID?" | **Yes** | Birleştirilmiş release manifestinde `com.google.android.gms.permission.AD_ID` var. Kullanım amacı olarak **Advertising or marketing** + **Analytics** + **Fraud prevention** işaretlenmeli. Bu beyan yapılmazsa uygulama reddedilir |
| *App content → Privacy policy* | Gizlilik politikası URL'si | Bkz. `docs/STORE_SUBMISSION_CHECKLIST.md` |
| *App content → Data deletion* (varsa) | Hesap oluşturma yok | Bkz. 1.3 |
| *App content → Government apps / Financial features / Health* | Hepsi **Hayır** | — |
| AdMob tarafı: `app-ads.txt` | **Eksik — kurulmalı** | AdMob'da "Authorized sellers" için, Play listesindeki geliştirici web sitesinin kökünde `app-ads.txt` yayınlanmalı. Doğrudan bir Play şartı değildir ama reklam talebi doldurma oranını doğrudan etkiler |

---

## 4b. Kardeş uygulama (Kaboom Blocks) ile karşılaştırma

Emsal: `C:\Users\bhdre\APPDeveloper\projects\Boom-Blocks\docs\PLAY_STORE_DATA_SAFETY.md`
(2026-08-12). Aynı geliştirici hesabı, aynı AdMob publisher (`pub-8582550349019790`), aynı
SDK sürümleri (`play-services-ads 25.4.0`, `user-messaging-platform 4.0.0`). Beyanların
tutarlı olması **kendi başına bir uyum değeridir** — aynı hesap altındaki iki uygulamanın
aynı SDK için farklı beyan vermesi incelemede dikkat çeker.

| Konu | Kaboom Blocks | Kron Drive | Aynı mı? |
|---|---|---|---|
| "Collect or share any data?" | Yes | Yes | ✅ |
| "Encrypted in transit?" | Yes | Yes | ✅ |
| "Way to request deletion?" | Yes (uninstall) | Yes (uninstall / verileri temizle) | ✅ |
| Device or other IDs — toplanıyor/paylaşılıyor | Yes / Yes | Yes / Yes | ✅ |
| Device or other IDs — required/optional | Optional | Optional | ✅ |
| Contains ads | Yes | Yes | ✅ |
| App access | Giriş yok | Giriş yok | ✅ |
| Hedef kitle | 13+ ve yetişkinler, "13 altı" işaretlenmedi | Aynı (öneri) | ✅ |
| `tagForChildDirectedTreatment` | false/unspecified | false/unspecified | ✅ |
| **Device IDs amaçları** | Advertising or marketing, Analytics | Advertising or marketing, Analytics, **+ Fraud prevention, security and compliance** | ⚠️ **SAPMA** |
| **Approximate location** | Beyan edilmemiş | **Beyan ediliyor** | ⚠️ **SAPMA** |
| **App activity → App interactions** | "sunucuya gitmiyor, işaretlenmesi gerekmez" denmiş | **Beyan ediliyor** (AdMob kaynaklı) | ⚠️ **SAPMA** |
| **App info and performance → Diagnostics** | Beyan edilmemiş | **Beyan ediliyor** | ⚠️ **SAPMA** |

### Sapmaların gerekçesi

Dört sapmanın **tamamı aynı sebepten**: Kaboom Blocks dokümanı AdMob'u "yalnızca reklam
kimliği toplar" diye özetliyor; Google'ın **kendi yayımladığı** Mobile Ads SDK disclosure
tablosu ise dört kalem daha listeliyor (yaklaşık konum, uygulama etkileşimleri, tanılama ve
dolandırıcılık önleme amacı). Kron Drive belgesi Google'ın o sayfasını doğrudan kaynak
alıyor.

- **Bu bir eksik beyandır, fazla beyan değil.** Data Safety'de eksik beyan yaptırım
  sebebidir; fazladan doğru beyan ise değildir.
- **Not:** Kaboom Blocks §2'deki "oyun ilerlemesi App activity olarak işaretlenmeli mi"
  tartışması ayrı bir konudur ve orada verilen cevap doğrudur — **cihazdan çıkmayan** veri
  Play'in tanımına göre "collected" sayılmaz. Kron Drive'daki App interactions satırı oyun
  ilerlemesi için değil, **reklam etkileşimleri** içindir.
- **Aksiyon önerisi (bu ajanın kapsamı dışında, sahibine bildiriliyor):**
  Kaboom Blocks'un Data Safety beyanı da aynı dört kalemle güncellenmeli. Aksi hâlde aynı
  hesap altında aynı SDK için iki farklı beyan bulunur.

---

## 5. Belirsiz kalan ve doğrulanması gereken noktalar

| Konu | Neden belirsiz | Nasıl doğrulanır |
|---|---|---|
| "Device or other IDs" zorunlu mu opsiyonel mi | AdMob belgesi alt satırları farklı işaretliyor, Console tek kutu sunuyor | Console'daki adımda alt kırılım var mı bak; yoksa **Required** seç |
| 1.3 "data deletion" sorusunun tam ifadesi | Google bu bölümü 2023'ten beri birkaç kez değiştirdi | Console ekranındaki metni oku, bu dosyayı güncelle |
| AdMob disclosure sayfasının o günkü hâli | Sayfa SDK sürümüyle güncelleniyor; bu belge `play-services-ads 25.4.0` ile aynı sürüm referansına dayanıyor | Gönderimden hemen önce https://developers.google.com/admob/android/privacy/play-data-disclosure sayfasını tekrar aç |
| Hedef kitleye çocuk eklenirse | TFCD/TFUA açılınca AAID kullanımı değişir ve bu formun cevapları da değişebilir | Önce hedef kitle kararı verilmeli (bkz. `docs/STORE_SUBMISSION_CHECKLIST.md`) |

---

## 6. Formun tutarlılık kontrolü (gönderimden önce)

- [ ] Data Safety'de beyan edilen her şey gizlilik politikasında da yazıyor mu?
      *(Politika 4. bölüm ile 2.1 tablosu birebir aynı kalemleri sayıyor.)*
- [ ] "Contains ads" işaretlendi mi?
- [ ] Advertising ID beyanı yapıldı mı?
- [ ] Gizlilik politikası URL'si açılıyor mu (404 değil, redirect değil, giriş istemiyor)?
- [ ] Uygulamaya yeni bir SDK eklendiyse bu form tekrar gözden geçirildi mi?
