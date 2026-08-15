# Play Console gönderim kontrol listesi — uyum tarafı

**Uygulama:** Kron Drive: Retro Racer · `com.miniappfactory.krondrive`
**Sürüm:** versionCode 4 / versionName 1.0.3
**Hazırlanma tarihi:** 2026-08-14
**Kapsam:** Play Console'da doldurulacak **politika/form/beyan** maddeleri.

> **`docs/RELEASE_CHECKLIST.md` ile ilişkisi:** o dosya **teknik** yayın hattıdır
> (keystore, imzalama, AdMob kimlikleri, cihaz testi, AAB üretimi). Bu dosya ise
> **politika ve mağaza formları** tarafıdır. Çakışan tek bölüm RELEASE_CHECKLIST'in
> "5. Play Console" başlığıydı; oradaki maddeler bu dosyaya taşındı ve orası bu dosyaya
> yönlendirecek şekilde güncellendi. **Teknik adımlar için RELEASE_CHECKLIST, form ve
> politika için bu dosya.**

Durum etiketleri: **HAZIR** · **EKSİK** (yapılabilir, iş bizde) · **KULLANICIDAN BEKLENİYOR** (karar/varlık sahibinden gelecek)

> **Emsal — kardeş proje Kaboom Blocks.** Aynı geliştirici hesabı, aynı AdMob yayıncısı ve
> aynı SDK'lar kullanılıyor. Bu belge, `Boom-Blocks/docs/PLAY_CONSOLE_SUBMISSION_CHECKLIST.md`
> ve `.../PLAY_STORE_DATA_SAFETY.md` yapısıyla hizalıdır; farklı bir şey söylediğim her yerde
> sebebini yazdım (bölüm 9).

---

## 0. Ön koşullar — sahibinde ZATEN olanlar

| Varlık | Durum | Ayrıntı |
|---|---|---|
| Play Console geliştirici hesabı | ✅ **VAR**, doğrulanmış | Kaboom Blocks için kullanıldı |
| AdMob yayıncı hesabı | ✅ **VAR** | Publisher ID `ca-app-pub-8582550349019790` |
| Upload keystore | ✅ **VAR** | `source/my-upload-key.jks` + `source/signing.properties` (alias `UPLOAD`) — ikisi de `.gitignore`'da |
| İmzalı AAB | ✅ **ÜRETİLDİ** | `source/app/build/outputs/bundle/release/app-release.aab` (7,5 MB, 2026-08-14 23:20) |
| GitHub Pages emsali | ✅ **ÇALIŞIYOR** | `https://miniappfactory.github.io/boomblast/` canlı → aynı hesapta Pages kurulu |
| Uygulama ikonu 512×512 | ✅ **VAR** | `docs/play_store_assets/play-store-icon-512.png` |
| Git remote | ✅ | `https://github.com/MiniAppFactory/KronDrive.git`, branch `main`, `docs/` repo içinde |

**Bu, brief'te varsayılandan çok daha ileri bir başlangıç noktası.** Aşağıdaki engel
listesi bu gerçeklere göre yeniden yazıldı.

---

## 0b. Yönetici özeti — yayın önünde KALAN gerçek engeller

| # | Engel | Durum | Kim |
|---|---|---|---|
| 1 | **Gerçek AdMob kimlikleri girilmedi** (`USE_TEST_IDS_IN_RELEASE = true`, manifestte Google'ın test App ID'si) | EKSİK | AdMob'da Kron Drive uygulaması + 3 reklam birimi açılmalı, sonra `docs/ADMOB_SETUP.md` uygulanıp **AAB yeniden üretilmeli**. Şu anki AAB test reklamıyla imzalanmış durumda |
| 2 | Gizlilik politikası **henüz canlı değil** | EKSİK — dosyalar hazır | `docs/index.html` + `docs/tr/index.html` üretildi; Pages açılıp push edilecek (bölüm 2) |
| 3 | Özellik grafiği (1024×500) ve **ekran görüntüleri yok** | KULLANICIDAN BEKLENİYOR | Cihaz gerekiyor; bu makinede emülatör/adb yok |
| 4 | Mağaza metinleri (kısa/uzun açıklama TR + EN) | EKSİK | ASO ajanına verilebilir |
| 5 | Uygulama içinde **gizlilik seçenekleri (consent'i değiştir) düğmesi yok** | EKSİK — politika riski | Bölüm 7, bulgu C-2 |
| 6 | **Hedef kitle kararı** teyidi | KULLANICIDAN BEKLENİYOR | Kaboom Blocks'ta zaten "13+ ve yetişkinler" kararı alınmış; **aynı çizgi öneriliyor** — bölüm 6 |
| 7 | `android:appCategory="game"` yok | EKSİK — uyum değil, kalite | Bölüm 7, bulgu C-7 |

**Zorunlu engeller: 1, 2, 3, 4.** 5 ve 7 güçlü öneri; 6 bir onaydan ibaret.

---

## 1. Uygulama oluşturma ve temel bilgiler

| Madde | Durum | Not |
|---|---|---|
| Geliştirici hesabı | **HAZIR** | Mevcut ve doğrulanmış (Kaboom Blocks ile kullanıldı) — kayıt ücreti ödenmiş |
| Play Console'da uygulama kaydı | KULLANICIDAN BEKLENİYOR | *Create app* → aşağıdaki değerler |
| Uygulama adı (50 karakter) | HAZIR | `Kron Drive: Retro Racer` (23 karakter) |
| Varsayılan dil | HAZIR | **English (US)** — Kaboom Blocks ile aynı çizgi (`PLAY_CONSOLE_SUBMISSION_CHECKLIST.md` §1). Türkçe, *Add language → Turkish* ile ikinci dil olarak eklenir |
| Uygulama mı oyun mu | HAZIR | **Oyun** → Kategori: **Racing** (alternatif: Arcade) |
| Ücretsiz / ücretli | HAZIR | **Ücretsiz** (geri döndürülemez seçim) |
| Paket adı | HAZIR | `com.miniappfactory.krondrive` — Console'da bir kez seçilir, değiştirilemez |
| İletişim e-postası | HAZIR | `whatsthisapp@proton.me` |
| Web sitesi (opsiyonel) | HAZIR | `https://github.com/MiniAppFactory/KronDrive` |

---

## 2. Gizlilik politikası URL'si — **zorunlu**

Play, **çalışan, herkese açık, giriş istemeyen bir HTTPS URL** ister; Markdown dosyasının
repoda durması yetmez. Console'a girilen URL 404 veriyorsa veya login istiyorsa uygulama
reddedilir.

**Yayınlanacak dosyalar üretildi — Kaboom Blocks'taki `boomblast` deseninin birebir aynısı
(`source/docs/` → GitHub Pages):**

| Dosya | Yayınlanacağı adres |
|---|---|
| `docs/index.html` | `https://miniappfactory.github.io/KronDrive/` ← **Play Console'a girilecek URL** |
| `docs/tr/index.html` | `https://miniappfactory.github.io/KronDrive/tr/` |
| `docs/app-ads.txt` | `https://miniappfactory.github.io/KronDrive/app-ads.txt` |
| `docs/.nojekyll` | (Jekyll işlemesini kapatır — `.html` dosyaları olduğu gibi sunulur) |

HTML sayfaları kendi kendine yeter: dış CSS/font/script yok, açık ve koyu temaya uyumlu,
mobilde okunabilir, iki dil birbirine bağlı. İçerik `docs/PRIVACY_POLICY_EN.md` ve
`docs/PRIVACY_POLICY_TR.md` ile aynıdır; **iç notlar HTML'e taşınmadı.** Markdown
dosyaları bundan sonra "kaynak metin" olarak kalır — politika değişirse **ikisi birden**
güncellenmelidir.

### Yayına alma adımları

1. Dosyaları commit et ve `main` branch'ine push et:
   ```
   git add docs/index.html docs/tr/index.html docs/app-ads.txt docs/.nojekyll
   git commit -m "docs: gizlilik politikasi sayfalari + app-ads.txt (GitHub Pages)"
   git push origin main
   ```
2. GitHub → `MiniAppFactory/KronDrive` → **Settings → Pages**
   → *Source: Deploy from a branch* → Branch: **`main`**, Folder: **`/docs`** → **Save**.
3. 1-3 dakika bekle (ilk derleme). Sonra **gizli sekmede** doğrula:
   - `https://miniappfactory.github.io/KronDrive/` → İngilizce politika
   - `https://miniappfactory.github.io/KronDrive/tr/` → Türkçe politika
   - `https://miniappfactory.github.io/KronDrive/app-ads.txt` → tek satır metin
4. Play Console → *App content → Privacy policy* alanına
   **`https://miniappfactory.github.io/KronDrive/`** gir.
5. Mağaza sayfasındaki *Website* alanına da aynı adresi gir (AdMob eşleşmesi ve
   `app-ads.txt` doğrulaması için — boomblast'ta bu şekilde yapıldı ve doğrulama geçti).

> **Repo public olmalı.** GitHub Pages ücretsiz planda private repoda yayınlanmaz.
> Repo zaten public ise `docs/` altındaki diğer `.md` dosyaları (CHANGELOG, ARCHITECTURE,
> HANDOVER…) github.com üzerinden zaten görülebilir durumdadır; Pages bu bakımdan yeni bir
> şey açığa çıkarmaz. Gizli tutulması gereken bir şey yok — keystore ve `signing.properties`
> `.gitignore`'da.

### Yedek yol (Pages sorun çıkarırsa) — Netlify Drop

1. `docs/` klasöründen `index.html`, `tr/index.html`, `app-ads.txt` dosyalarını ayrı bir
   klasöre kopyala.
2. https://app.netlify.com/drop adresine klasörü **sürükle bırak**.
3. Site settings → Change site name → `krondrive` → `https://krondrive.netlify.app/`.
4. Gizli sekmede doğrula, URL'yi Console'a gir.

Bu yalnızca acil durum yoludur; **tercih GitHub Pages'tir** — kurumsal kimlikle (bölüm 17,
global kurallar) ve boomblast emsaliyle tutarlıdır.

### app-ads.txt

`docs/app-ads.txt` içeriği, Kaboom Blocks'takiyle **aynı satır** (aynı yayıncı hesabı):

```
google.com, pub-8582550349019790, DIRECT, f08c47fec0942fa0
```

AdMob konsolunda: Apps → Kron Drive → App settings → **App store details** ile Play
kaydını eşleştir, ardından **app-ads.txt → Check for updates** ile doğrulamayı tetikle.
Boom Blocks deneyimi (CHANGELOG Faz 67): **Play Store'da yayınlanmış olmak ön koşul
değil** — dosya canlı olduğunda doğrulama geçiyor, yayılması birkaç dakika sürebiliyor.

| Madde | Durum |
|---|---|
| Politika kaynak metni (TR/EN Markdown) | HAZIR |
| Yayınlanacak HTML sayfaları | **HAZIR** — `docs/index.html`, `docs/tr/index.html` |
| `app-ads.txt` | **HAZIR** — `docs/app-ads.txt` |
| Push + Pages ayarı | EKSİK — yukarıdaki 1-3. adımlar |
| Console'a girilmesi | KULLANICIDAN BEKLENİYOR |

---

## 3. App content (Uygulama içeriği) bölümü

| Console alanı | Cevap | Durum | Kaynak |
|---|---|---|---|
| Privacy policy | `https://miniappfactory.github.io/KronDrive/` | Sayfa HAZIR, yayına alınacak | Bölüm 2 |
| **Ads** — "Uygulamanız reklam içeriyor mu?" | **Evet** | HAZIR | Banner + geçiş + ödüllü |
| **App access** — giriş gerekiyor mu? | **Tüm işlevler kısıtlamasız kullanılabilir** | HAZIR | Hesap/giriş yok; incelemeci için test hesabı gerekmiyor |
| **Content rating** anketi | `docs/CONTENT_RATING.md` | HAZIR (cevaplar) | Ankete girilmesi bekleniyor |
| **Target audience and content** | Bölüm 6 | **KULLANICIDAN BEKLENİYOR** | Kritik karar |
| **News app** | Hayır | HAZIR | — |
| **COVID-19 contact tracing** | Hayır | HAZIR | — |
| **Data safety** | `docs/DATA_SAFETY_FORM.md` | HAZIR (cevaplar) | Forma girilmesi bekleniyor |
| **Advertising ID** | **Evet** — Advertising/marketing + Analytics + Fraud prevention | HAZIR | Birleştirilmiş manifestte `AD_ID` izni doğrulandı |
| **Government apps** | Hayır | HAZIR | — |
| **Financial features** | Hiçbiri | HAZIR | IAP ve ödeme yok |
| **Health apps** | Hayır | HAZIR | — |
| **Data deletion / hesap silme** | Data Safety'de "silme yolu var mı" → **Evet** (kaldır / verileri temizle). Ayrı **hesap silme** zorunluluğu kapsam dışı (hesap yok) | HAZIR | Bölüm 7 bulgu C-4; `DATA_SAFETY_FORM.md` §1.3 |

---

## 4. Mağaza sayfası (Main store listing)

| Madde | Gereksinim | Durum |
|---|---|---|
| Uygulama adı | ≤30 karakter | HAZIR — `Kron Drive: Retro Racer` |
| Kısa açıklama | ≤80 karakter, TR + EN | **EKSİK** — ASO ajanına verilebilir |
| Tam açıklama | ≤4000 karakter, TR + EN | **EKSİK** |
| Uygulama ikonu | 512×512 PNG, 32-bit, ≤1 MB | HAZIR — `docs/play_store_assets/play-store-icon-512.png` |
| Özellik grafiği | 1024×500 PNG/JPG | **EKSİK** — üretilmeli (`game-art` skill) |
| Telefon ekran görüntüsü | En az 2, en fazla 8; 16:9–9:16, kenar 320–3840 px | **KULLANICIDAN BEKLENİYOR** — cihaz/emülatör gerekiyor, bu makinede yok |
| 7"/10" tablet görüntüleri | Opsiyonel | EKSİK — yoksa tablette "bu cihaz için tasarlanmadı" uyarısı çıkar |
| Tanıtım videosu | Opsiyonel | Yok |
| Kategori | Oyun → Racing | HAZIR |
| Etiketler (tags) | En fazla 5 | EKSİK |
| İletişim e-postası | Zorunlu | HAZIR — `whatsthisapp@proton.me` |
| Website | Opsiyonel ama **doldur** | HAZIR — `https://miniappfactory.github.io/KronDrive/` (AdMob eşleşmesi ve `app-ads.txt` için; boomblast'ta da böyle yapıldı) |
| **Türkçe mağaza sayfası** | *Add language → Turkish* | EKSİK — metin bekleniyor. Uygulama tarafı buna hazır: `res/values-tr/strings.xml` mevcut (bkz. bulgu C-8) |

**Metinlerde yasak:** "En iyi", "1 numara" gibi doğrulanamayan iddialar; emoji spam'i;
başka markaların adı; "reklamsız" gibi yanlış beyan; anahtar kelime doldurma. Ekran
görüntüleri **gerçek oyun ekranı** olmalı — oyunda olmayan bir şeyi gösteren görsel
"yanıltıcı tanıtım" ihlalidir.

---

## 5. Sürüm (Release) tarafı

| Madde | Durum | Not |
|---|---|---|
| Upload keystore | **HAZIR** | `source/my-upload-key.jks` + `signing.properties`, alias `UPLOAD`. **Kaybolursa güncelleme bir daha yayınlanamaz — yedekle** |
| İmzalı AAB (`bundleRelease`) | **ÜRETİLDİ** | `source/app/build/outputs/bundle/release/app-release.aab` (7,5 MB) |
| ⚠️ Bu AAB yüklenebilir mi? | **HAYIR — henüz değil** | İçinde hâlâ **test reklam kimlikleri** var (`USE_TEST_IDS_IN_RELEASE = true`). Gerçek AdMob kimlikleri girildikten sonra **yeniden üretilmeli**. Internal testing için yüklenebilir, üretim için hayır |
| Play App Signing | KULLANICIDAN BEKLENİYOR | Yeni uygulamalarda zorunlu; upload key ayrı tutulur |
| **targetSdk = 36** | **HAZIR** | Bölüm 7, bulgu C-1 |
| versionCode benzersiz ve artan | HAZIR | 4 — reklam kimlikleri girilip yeni AAB üretilirken **5'e çıkar** |
| Sürüm notları (TR + EN) | EKSİK | ≤500 karakter |
| **Internal testing** | Başlanabilir | Kaboom Blocks'ta olduğu gibi kapalı test şartına tabi değil; doğrudan başlanabilir |
| **Kapalı test (closed testing) şartı** | KULLANICIDAN BEKLENİYOR | Üretime çıkarken: yeni **kişisel** geliştirici hesapları için Play en az **12 test kullanıcısıyla 14 gün** sürekli kapalı test istiyor; kurumsal hesaplarda bu şart yok. Hesap Kaboom Blocks ile aynı olduğundan durum oradan görülebilir — **Console'da doğrulanmalı** |
| Ülke/bölge seçimi | KULLANICIDAN BEKLENİYOR | — |
| Fiyat: ücretsiz | HAZIR | — |

---

## 6. **KRİTİK KARAR — hedef kitle**

Console: *App content → Target audience and content*. Yaş grupları: 5 ve altı, 6-8, 9-12,
13-15, 16-17, 18+. **Seçilen gruplardan herhangi biri 13 yaşın altındaysa** uygulama
Google Play **Families Policy** kapsamına girer.

> **Emsal var.** Kaboom Blocks'ta bu karar zaten verildi: **"13 ve üzeri + yetişkinler",
> "13 yaş altını da hedefliyorum" İŞARETLENMEDİ, "Designed for Families" → Hayır**
> (`Boom-Blocks/docs/PLAY_CONSOLE_SUBMISSION_CHECKLIST.md` §3,
> `PLAY_STORE_DATA_SAFETY.md` §5). Aşağıdaki analiz bu kararı bağımsız olarak doğruluyor
> ve **Kron Drive için de aynı çizgi öneriliyor** — hem doğru olduğu için hem de aynı
> hesap altındaki iki oyunun aynı beyanı vermesi tutarlılık sağladığı için.
>
> **Ama bir fark var, kayda geçiyor:** Kaboom Blocks bir blok bulmaca oyunu; Kron Drive
> bir **araba/trafik oyunu** ve bu tür oyunlar çocukları belirgin biçimde daha fazla
> çeker. Karar aynı kalsa da **riski aynı değildir**: Kron Drive'da mağaza görsellerinin
> ve metinlerinin çocuk kitlesine göz kırpmaması Kaboom Blocks'takinden daha kritiktir
> (bkz. aşağıdaki "Yol A riski" ve bölüm 4'teki görsel notları).

### Yol A — Hedef kitle **13+** (yalnızca 13-15, 16-17, 18+ seçilir) — **ÖNERİLEN**

**Sonuçları:**
- Families Policy kapsamı **dışında** kalınır; Families Self-Certified Ads SDK zorunluluğu yok.
- **Kişiselleştirilmiş reklam serbest** (AEA/UK'de UMP onayına bağlı). Gelir tarafında
  belirleyici olan budur.
- TFCD (`tagForChildDirectedTreatment`) / TFUA (`tagForUnderAgeOfConsent`) bayrakları
  **set edilmez** — yani **kodda değişiklik gerekmez**, mevcut hâl bu yola uygun.
- Mağaza sayfasında yaş beyanı görünür; içerik derecelendirmesi yine 3+/Everyone çıkar
  (ikisi farklı şeydir).
- Play, uygulamanın **görselleri ve metinleri çocuklara hitap ediyorsa** ("Store presence"
  sorusu) bunu sorgular. Retro/arcade estetik sınırda sayılabilir; ikon ve ekran
  görüntülerinde çizgi film karakteri, çocuk figürü, "çocuklar için" ifadesi kullanılmamalı.
- Ödüllü reklam ve mevcut geçiş reklamı frekansı (`INTERSTITIAL_AFTER_EVERY_RUN = true`)
  bu yolda serbesttir.

**Yol A riski:** oyun çocukları çekerse ve mağaza sayfası çocuklara yönelik algılanırsa
Google yeniden sınıflandırma isteyebilir. Bu, **düzeltilebilir** bir uyarıdır (görselleri
değiştir, beyanı güncelle), hesap kapatma sebebi değildir. **Yarış oyununda bu risk bir
bulmaca oyunundakinden yüksektir** — riski düşürmek için mağaza görsellerinde ve
metinlerinde şunlar kullanılmamalı: çizgi film karakteri, çocuk figürü, "çocuklar için /
for kids", oyuncak/emoji ağırlıklı tasarım, aşırı yumuşak-pastel "kids game" estetiği.
Retro/arcade neon çizgisi bu bakımdan güvenlidir.

### Yol B — Hedef kitleye çocuklar dâhil (9-12 ve altı seçilir)

**Sonuçları:**
- Families Policy Requirements'ın tamamı uygulanır.
- **Kişiselleştirilmiş reklam yasak.** Tüm reklamlar kişiselleştirilmemiş (NPA) olmak
  zorunda; ilgi alanına dayalı reklam ve remarketing kapalı.
- Kodda **zorunlu değişiklik:** `RequestConfiguration.Builder()` üzerinde
  `.setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)` ve
  `.setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)`. Bu bayraklar
  set edilmeden Families uygulaması yayınlanamaz.
- Yalnızca **Families Self-Certified Ads SDK** kullanılabilir. AdMob bu programda
  sertifikalıdır, yani SDK değiştirmek gerekmez — ama sertifikalı **sürüm listesi**
  gönderim öncesi kontrol edilmelidir.
- **Reklam formatı kısıtları:** uygulama açılışında hemen tam ekran reklam yasak; ödüllü
  ve opt-in reklamlar **5 saniye sonra kapatılabilir** olmalı; sayfa başına birden fazla
  reklam yerleşimi yasak; kapatma düğmesinin başka bir reklamı tetiklemesi yasak.
  → Mevcut `INTERSTITIAL_AFTER_EVERY_RUN = true` frekansı bu yolda **yeniden
  değerlendirilmelidir**.
- Ek mağaza yükümlülükleri: Families için ayrı içerik/görsel incelemesi, daha sıkı
  moderasyon.

**Kazancı:** Play'de "Teacher Approved" / Kids sekmesinde görünürlük ihtimali; çocuk
oyuncu kitlesine yasal erişim.

### Rakamlarla karşılaştırma

| Ölçüt | Yol A (13+) | Yol B (çocuk dâhil) |
|---|---|---|
| Kişiselleştirilmiş reklam | Serbest | **Yasak** |
| Beklenen eCPM etkisi | Taban (%100) | Sektör tahminlerine göre NPA, kişiselleştirilmiş envantere kıyasla tipik olarak **%30-60 daha düşük** eCPM getirir. *(Bu bir Google politika maddesi değil, yayıncı raporlarına dayanan tahmin aralığıdır — kendi AdMob raporunla doğrula.)* | 
| Kod değişikliği | **Yok** | TFCD + TFUA zorunlu, reklam frekansı gözden geçirilmeli |
| Ek inceleme süresi | Standart | Families incelemesi daha uzun sürebilir |
| Politika ihlali yüzeyi | Dar | Geniş (reklam formatı, içerik, görsel, veri) |
| Ödüllü reklam | Serbest | 5 sn kuralı ve opt-in kısıtları |

### **Öneri: Yol A — hedef kitle 13-15 / 16-17 / 18+**

Gerekçe:

1. **Gelir.** Uygulamanın **tek** gelir kaynağı reklam. IAP yok, abonelik yok. Yol B,
   tek gelir kalemini doğrudan ve kalıcı olarak düşürür.
2. **Kod değişikliği gerektirmez.** Yol A, bugünkü kodun (TFCD/TFUA yok, mevcut reklam
   frekansı) beyanla tutarlı olduğu tek yoldur. Yol B seçilirse yayın öncesi kod
   değişikliği + yeniden test + yeni AAB gerekir.
3. **Oyun çocuklara yönelik tasarlanmadı.** İçerikte çocuklara özel bir çekicilik unsuru
   (çizgi film karakteri, eğitim içeriği, çocuk sesi/anlatımı) yok; retro arcade estetiği
   yetişkin nostaljisine yaslanıyor. "Çocuklar da oynayabilir" ile "çocuklara yönelik"
   Play'in gözünde farklı şeylerdir.
4. **Geri dönüşü kolay.** 13+ ile yayına çıkıp sonradan Families'e geçmek mümkündür;
   tersi (Families'ten çıkmak) daha sancılıdır ve mevcut kurulumları etkiler.
5. **Risk yönetilebilir.** Tek koşul: mağaza görselleri ve metinleri çocuk kitlesine
   göz kırpmasın.
6. **Portföy tutarlılığı.** Aynı geliştirici hesabındaki Kaboom Blocks zaten bu beyanla
   ilerliyor. Aynı hesap altında iki oyunun aynı SDK için farklı çocuk beyanı vermesi
   incelemede gereksiz soru işareti yaratır.

> **Karar sahibinindir.** Bu belge öneri sunar; Play Console'da beyanı yapan ve sorumluluğu
> üstlenen taraf uygulama sahibidir. Yol B seçilirse yukarıdaki kod değişiklikleri
> `admob-monetization-engineer` ajanına ayrı bir görev olarak verilmelidir; bu ajan kod
> değiştirmedi.

---

## 7. Kod tarafı uyum denetimi — bulgular

*(Bu bölümde hiçbir kod değiştirilmedi. Kanıt olarak dosya ve satır referansı verildi.)*

### C-1 · targetSdk — **UYUMLU, ama tarihe dikkat**

`source/app/build.gradle.kts:15` → `targetSdk = 36`, `compileSdk` release(36), `minSdk = 24`.

Google Play kuralı: **31 Ağustos 2026**'dan itibaren yeni uygulamalar ve güncellemeler
**Android 16 (API 36)** veya üstünü hedeflemek zorunda (uzatma başvurusu 1 Kasım 2026'ya
kadar mümkün). `targetSdk = 36` bu şartı **karşılıyor** — bugün (14 Ağustos 2026) itibarıyla
son tarihe **17 gün** var; bu sürümle çıkmak sorun değil, ancak yayın bu tarihi aşarsa
aynı targetSdk yine geçerli olacaktır.
Kaynak: https://support.google.com/googleplay/android-developer/answer/11926878

`minSdk = 24` (Android 7.0) — Play'in alt sınır zorunluluğu yok, sorun değil.

### C-2 · UMP consent akışı — **İKİ BULGU, düzeltilmeli**

`MainActivity.kt:83-96` ve `MainActivity.kt:49-55`.

**C-2a (yüksek): "Gizlilik seçenekleri" giriş noktası yok.**
Kodda `UserMessagingPlatform.loadAndShowConsentFormIfRequired` çağrılıyor ama
`consentInformation.getPrivacyOptionsRequirementStatus()` hiç okunmuyor ve
`showPrivacyOptionsForm` hiçbir yerde çağrılmıyor (tüm kaynak ağacında arandı — tek
eşleşme yok). Google'ın UMP belgesi bunu açıkça şart koşuyor: durum `REQUIRED` ise
*"add a visible and interactable UI element to your app that presents the privacy options
form."* GDPR açısından da onayın geri alınması, verilmesi kadar kolay olmalıdır.
**Etki:** AEA/UK kullanıcıları tercihlerini değiştiremiyor → Google EU User Consent
Policy riski.
**Düzeltme:** Ayarlar ekranına (`ui/settings/SettingsScreen.kt`) bir "Gizlilik
seçenekleri" satırı; yalnızca `privacyOptionsRequirementStatus == REQUIRED` iken görünür.
Kaynak: https://developers.google.com/admob/android/privacy

**C-2b (orta): 4 saniyelik güvenlik ağı, onay çözülmeden reklamı açabiliyor.**
`MainActivity.kt:53-55` bir `Handler.postDelayed` ile 4 sn sonra `adsConsentResolved`'ı
koşulsuz `true` yapıyor. Amaç anlaşılır (form takılırsa banner o oturum boyunca
kaybolmasın) ama sonuç, **onay formu hâlâ ekrandayken veya hiç yanıtlanmamışken** banner
yüklenmesidir. Google'ın kuralı net: *"Before requesting ads, use `canRequestAds()` to
check if you've obtained consent from the user."*
**Düzeltme:** zamanlayıcının içinde koşulsuz `true` yerine
`if (consentInformation.canRequestAds()) adsConsentResolved.value = true`. Davranış aynı
kalır (form hiç gelmeyen bölgelerde `canRequestAds()` zaten true döner), politika riski
kalkar.
Not: başarı dalında (`MainActivity.kt:90`) de onResolved doğrudan çağrılıyor; orada da
`canRequestAds()` kontrolü eklenmesi daha doğru olur.

### C-3 · İzinler — **GEREKSİZ İZİN YOK**

Uygulamanın kendi manifesti (`AndroidManifest.xml:5-6`) yalnızca:
- `android.permission.INTERNET` — reklam SDK'sı için **gerekli**
- `android.permission.ACCESS_NETWORK_STATE` — reklam SDK'sının bağlantı kontrolü için
  **gerekli** (SDK zaten kendisi de ekler)

Kamera, mikrofon, konum, rehber, depolama, bildirim, `QUERY_ALL_PACKAGES` gibi hassas
izin **yok**. Play'in "izin minimizasyonu" beklentisi karşılanıyor.

Birleştirilmiş **release** manifestinde (`app/build/intermediates/merged_manifest/release/...`)
reklam SDK'sından gelenler doğrulandı:

```
com.google.android.gms.permission.AD_ID
android.permission.ACCESS_ADSERVICES_AD_ID
android.permission.ACCESS_ADSERVICES_ATTRIBUTION
android.permission.ACCESS_ADSERVICES_TOPICS
android.permission.WAKE_LOCK
android.permission.FOREGROUND_SERVICE
com.miniappfactory.krondrive.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
```

**`AD_ID` izni beyan edilmiş durumda** (elle değil, `play-services-ads` birleşmesiyle).
Sonuç: Console'daki **Advertising ID beyanı "Evet" olmak zorunda.** "Hayır" denirse
beyan-manifest çelişkisi doğar ve gönderim reddedilir.

*Bilgi notu:* reklam kimliği kullanılmak istenmeseydi manifeste
`<uses-permission android:name="com.google.android.gms.permission.AD_ID" tools:node="remove" />`
eklenirdi. **Bu oyunda gerekmiyor** — reklam gelirinin temeli o kimlik.

### C-4 · Hesap silme gereksinimi — **BİZDE GEÇERLİ DEĞİL, ama beklenti karşılanmalı**

Google'ın kuralı yalnızca *"app enables account creation"* olan uygulamaları kapsar.
Kron Drive'da hesap oluşturma akışı yok; kod tarafında ağa giden tek bileşen reklam
SDK'sı, kullanıcı verisi yalnızca DataStore'da (`GameStateRepository.kt:19`,
`kron_drive_progress`). **Zorunluluk kapsam dışı.**

Ancak "verilerimi sil" beklentisi kullanıcı tarafında yine oluşur. Karşılanma biçimi:
- Gizlilik politikasının 7. bölümü cihaz üzerinden silme yolunu adım adım anlatıyor
  (uygulamayı kaldır / Ayarlar → Uygulamalar → Depolama → Verileri temizle / reklam
  kimliğini sıfırla). **Bu yeterlidir.**
- **Öneri (zorunlu değil):** Ayarlar ekranına "İlerlemeyi sıfırla" düğmesi eklemek
  kullanıcı beklentisini uygulama içinde karşılar ve destek e-postası trafiğini azaltır.
  Şu an böyle bir işlev **yok** (`SettingsScreen.kt` yalnızca ses, dil ve hakkında kartı
  içeriyor); `GameStateRepository`'de de bir `clear()`/`reset()` fonksiyonu yok.

### C-5 · Reklam politikası uyumu — **UYUMLU**

- Oyun ekranında banner **yok**; banner yalnızca menü/garaj/görev/ayarlar ekranlarında
  (`KronComponents.kt:42`, ekranların `showBanner` parametresi). Kontrollerin üzerine
  reklam gelmiyor → Play "Disruptive Ads" ihlali yok.
- Geçiş reklamı yalnızca **koşu bittikten sonra**, doğal geçiş noktasında
  (`GameConfig.INTERSTITIAL_AFTER_EVERY_RUN = true`). Oynanış sırasında tam ekran reklam
  yok. Frekans agresif ama politikaya aykırı değil.
- **Uygulama açılışında** tam ekran reklam yok (App Open Ads kullanılmıyor) — Families
  yolu seçilse bile bu madde temiz.
- Ödül **yalnızca** SDK'nın gerçek kazanıldı callback'inde veriliyor
  (`RewardedAdManager.kt:40`); yarıda kesilen video ödül vermiyor → "ödül aldatmacası"
  ihlali yok.
- Reklam yüklenemediğinde akış devam ediyor (`InterstitialAdManager.kt:22-28`
  `proceedOnce`) → kullanıcı reklam yüzünden kilitlenmiyor.
- **Uyarı:** `AdIds.kt:34` `USE_TEST_IDS_IN_RELEASE = true` ve `AndroidManifest.xml:23`
  Google'ın **test App ID'si**. Bu hâlde yayına çıkmak politika ihlali değildir ama
  **sıfır gelir** demektir ve AdMob hesabında uygulama eşleşmez. Yayın öncesi
  `docs/ADMOB_SETUP.md` uygulanmalı.
- `app-ads.txt` henüz yok. Play şartı değil, AdMob doluluk oranı için önerilir.

### C-6 · Yedekleme ayarı — **BİLGİ, aksiyon isteğe bağlı**

`AndroidManifest.xml:9` `android:allowBackup="true"` ve `backup_rules.xml` /
`data_extraction_rules.xml` varsayılan hâlde → oyun ilerlemesi kullanıcının kendi Google
Drive yedeğine kopyalanabilir. Kişisel/hassas veri içermediği için sorun değil ve
kullanıcı lehinedir (cihaz değişince ilerleme korunur). **Gizlilik politikasında bu
açıkça yazıldı** (2. bölüm, "Android yedeklemesi") — beyan-davranış tutarlılığı için
gerekliydi.

### C-7 · `android:appCategory="game"` eksik — **DOĞRULANDI, eklenmesi önerilir**

ASO ajanının bulgusu **teknik olarak doğru.** `AndroidManifest.xml:8-16` içindeki
`<application>` etiketinde `android:appCategory` **yok** (dosya baştan sona okundu).

Android 16 (API 36) davranış değişikliği: `targetSdk = 36` olan uygulamalarda,
**smallest width ≥ 600dp** olan ekranlarda yönelim (orientation), yeniden boyutlanabilirlik
ve en-boy oranı kısıtlamaları **yok sayılır**. Yani `android:screenOrientation="portrait"`
(`AndroidManifest.xml:35`) tablet ve katlanabilir cihazlarda **etkisiz kalır**; oyun yatay
pencerede açılabilir. Google'ın belgesi muafiyetleri açıkça sayıyor:

> *"The Android 16 orientation, resizability, and aspect ratio restrictions don't apply in
> the following situations: **Games (based on the `android:appCategory` flag)**, Users
> explicitly opting in…, Screens that are smaller than `sw600dp`."*

Kaynak: https://developer.android.com/about/versions/16/behavior-changes-16

**Uyum açısından değerlendirme:**
- **Bu bir Play politika ihlali DEĞİLDİR.** Beyan, form veya politika maddesi değil;
  bir davranış/kalite meselesidir. Uygulama bu hâliyle de kabul edilir.
- **Ama uyumla dolaylı ilgisi var:** oyun dikey tasarlandı (HUD, şerit düzeni, kontrol
  yerleşimi dikeye göre ölçülüyor — bkz. manifest yorumu). Tablette yatay açılması bozuk
  bir düzen demektir; bozuk düzen **kullanıcı şikâyeti, düşük puan ve "yanıltıcı ekran
  görüntüsü"** zeminine dönüşebilir (mağazadaki görseller dikey, cihazdaki görüntü yatay).
- **Önerilen düzeltme (kod değişikliği — bu ajan yapmadı):**
  `<application>` etiketine `android:appCategory="game"` eklemek. Tek satır, davranışı
  yalnızca büyük ekranlarda etkiler, muafiyeti aktive eder. Aynı zamanda Android'in
  veri/pil kullanım ekranlarında uygulamayı doğru kategoride gösterir.
- Alternatif geçici yol `PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY` özelliğidir ama
  Google bunu **geçici** ilan etti (API 37'de çalışmayacak). Oyun için doğru çözüm
  `appCategory`'dir.
- **Bu değişiklik yeni bir AAB gerektirir** — zaten reklam kimlikleri için de yeniden
  build alınacak; ikisi aynı sürümde birleştirilmeli.

### C-8 · `res/values-tr/` — **ASO'nun bulgusu ARTIK GEÇERLİ DEĞİL, dosya mevcut**

ASO raporunda "boş" denen klasör bugün itibarıyla **dolu**:
`source/app/src/main/res/values-tr/strings.xml` var ve `app_name` tanımlıyor (dosya
okundu; içindeki yorum bu denetime 2026-08-14 tarihiyle atıf yapıyor — yani düzeltme
zaten yapılmış).

**Uyum açısından değerlendirme:**
- Play'in **mağaza sayfası dilleri** ile **uygulamanın bildirdiği diller** iki ayrı
  şeydir. Türkçe mağaza sayfası, Console'da *Add language → Turkish* ile her hâlükârda
  açılabilir; bunun için APK/AAB'de `values-tr` bulunması **şart değildir**.
- `values-tr` klasörünün asıl etkisi mağaza sayfasındaki **"Diller / Languages"**
  listesidir; bu liste bundle'ın kaynak konfigürasyonundan türetilir. Klasör boş kalsaydı
  uygulama "yalnızca İngilizce" görünecekti — oysa oyun gerçekten iki dilli
  (`AppLanguage.pick`). **Bu bir tutarsızlık ve dolaylı olarak yanıltıcı beyan
  zeminidir**; şimdi giderilmiş durumda.
- **Kalan iş uyum değil, içerik:** Türkçe mağaza metinleri (başlık, kısa/uzun açıklama)
  hâlâ yazılmadı (bölüm 4). Türkçe sayfa açılacaksa bunlar gerekir.
- Not: oyun içi metinler kaynak dosyalarından değil `AppLanguage.pick(tr =…, en =…)`
  üzerinden geliyor (proje kuralı). Yani `values-tr/strings.xml`'in tek işlevi,
  bundle'a "bu uygulama Türkçe destekler" sinyalini vermektir — mevcut hâli bu iş için
  yeterlidir.

### C-9 · Küçük tutarsızlık — mağaza/uygulama sürüm metni

`SettingsScreen.kt:176` "Sürüm 1.0.0" / "Version 1.0.0" yazıyor; gerçek sürüm
`build.gradle.kts:17` → **1.0.3**. Politika ihlali değil ama incelemeci ve kullanıcı
gözünde tutarsızdır; `BuildConfig.VERSION_NAME`'den okunması önerilir.
*(Bulgu raporlandı, kod değiştirilmedi.)*

---

## 8. Kaboom Blocks emsalinden sapmalar ve sebepleri

Hizalanan her şey (aynı e-posta, aynı website deseni, aynı Pages yapısı, aynı app-ads.txt
satırı, aynı hedef kitle kararı, aynı IARC cevapları, aynı "App access: giriş yok", aynı
"Contains ads: Yes") burada tekrar edilmiyor. **Yalnızca farklar:**

| # | Sapma | Sebep |
|---|---|---|
| 1 | Data Safety'de **dört kalem daha** beyan ediliyor (Approximate location, App interactions, Diagnostics ve Fraud prevention amacı) | Kaboom Blocks dokümanı AdMob'u "yalnızca reklam kimliği" diye özetlemiş; Google'ın kendi Mobile Ads SDK disclosure sayfası bu kalemleri de listeliyor. Eksik beyan yaptırım sebebi, fazla-ama-doğru beyan değil. Ayrıntı: `DATA_SAFETY_FORM.md` §4b. **Kaboom Blocks'un beyanı da güncellenmeli** |
| 2 | Gizlilik politikası **iki dilde iki ayrı sayfa** (`/` ve `/tr/`), Kaboom Blocks'ta tek sayfada TR+EN alt alta | Play, mağaza sayfasının her dili için ayrı politika URL'si kabul eder; ayrı sayfa hem daha okunabilir hem de Türkçe mağaza sayfasına doğrudan bağlanabilir. İçerik aynı, yapı farklı |
| 3 | Politika metni **belirgin biçimde daha uzun ve daha spesifik** (saklanan alanların tam listesi, Android yedeklemesi, güvenlik iddialarının sınırı) | Kron Drive daha fazla yerel veri tutuyor (görevler, araç özelleştirme, günlük sayaçlar). Ayrıca `allowBackup=true` nedeniyle ilerleme kullanıcının Google Drive yedeğine çıkabiliyor — bu, beyan edilmesi gereken gerçek bir davranış (bulgu C-6) |
| 4 | Politikada **abartılı güvenlik iddiası yok** ("şifreleme/sunucu güvenliği" cümlesi kurulmadı) | Sunucumuz yok; olmayan bir korumayı iddia etmek Play User Data politikası açısından yanlış beyandır. TLS iddiası yalnızca Google'ın kendi SDK'sı için ve kaynak göstererek yapılıyor |
| 5 | Bu belgede ayrı bir **kod uyum denetimi** bölümü var (C-1…C-9) | Kaboom Blocks'ta ayrı bir denetim bölümü yok. Kron Drive'da denetim iki gerçek bulgu çıkardı (UMP privacy options eksik, 4 sn'lik consent baypası) — raporlanmadan geçilemezdi |
| 6 | IARC bölümünde yarış oyununa özgü sorular (kasıtlı çarpma, hasar/enkaz) ve "üçüncü taraflarla paylaşım → Evet" satırı açıkça ele alınıyor | Oyun türü farklı; ayrıca Data Safety ile IARC cevaplarının çelişmemesi gerekiyor (`CONTENT_RATING.md` §10b) |

Sapma **olmayan**, ama Kron Drive'a özgü olduğu için emsalde bulunmayan konular:
`appCategory` (C-7), `values-tr` (C-8), reklam frekansı değerlendirmesi (C-5).

---

## 9. Gönderim öncesi son kontrol

**Karar ve beyan**
- [ ] Hedef kitle "13-15 / 16-17 / 18+" olarak onaylandı (Kaboom Blocks ile aynı) ve Console'a girildi
- [ ] Yol B seçildiyse TFCD/TFUA kodda set edildi, reklam frekansı gözden geçirildi, yeni AAB üretildi
- [ ] Data Safety formu `docs/DATA_SAFETY_FORM.md` ile birebir dolduruldu (dört ek kalem dâhil)
- [ ] İçerik derecelendirme anketi `docs/CONTENT_RATING.md` ile dolduruldu
- [ ] Advertising ID beyanı **Evet** · "Contains ads" **Evet** · App access "giriş yok"

**Barındırma**
- [ ] `docs/index.html`, `docs/tr/index.html`, `docs/app-ads.txt`, `docs/.nojekyll` push edildi
- [ ] Pages ayarı: `main` / `/docs` → Save
- [ ] `https://miniappfactory.github.io/KronDrive/` gizli sekmede açılıyor
- [ ] `.../KronDrive/app-ads.txt` açılıyor ve AdMob'da doğrulama tetiklendi

**Build**
- [ ] AdMob'da Kron Drive uygulaması + banner/interstitial/rewarded birimleri oluşturuldu
- [ ] Gerçek App ID manifeste, 3 unit ID `AdIds.kt`'ye girildi, `USE_TEST_IDS_IN_RELEASE = false`
- [ ] `android:appCategory="game"` eklendi (C-7) — aynı build'de
- [ ] C-2a / C-2b düzeltildi **veya** bilinçli ertelendiği kayda geçti
- [ ] versionCode 5'e çıkarıldı, **AAB yeniden üretildi ve imzalandı**
- [ ] Keystore yedeklendi (kaybolursa güncelleme yayınlanamaz)

**Mağaza içeriği**
- [ ] En az 2 telefon ekran görüntüsü + 1024×500 özellik grafiği yüklendi
- [ ] Kısa/uzun açıklama EN girildi; Türkçe dil eklendi ve metinleri girildi
- [ ] Görsellerde çocuk kitlesine yönelik öge yok (hedef kitle beyanıyla tutarlı)
- [ ] Website alanı `https://miniappfactory.github.io/KronDrive/` olarak dolduruldu

**Kapanış**
- [ ] Internal testing sürümü yüklendi ve cihazda doğrulandı
- [ ] Kapalı test şartı (hesap tipine göre) netleştirildi
- [ ] `docs/CHANGELOG.md` bu sürüm için güncellendi
