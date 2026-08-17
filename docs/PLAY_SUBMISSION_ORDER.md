# Kron Drive — Play Console gönderim sırası (tek liste)

Hazırlayan: `play-store-compliance-engineer` · Tarih: **2026-08-17**

Bu dosya **sahibinin elindeki tek liste**dir. Bilgi daha önce üç belgeye
dağılmıştı (`PLAY_RELEASE_CHECKLIST.md`, `STORE_SUBMISSION_CHECKLIST.md`,
`DATA_SAFETY_FORM.md`); burada **hangi ekranda hangi alana ne yazılacağı**
sırayla toplandı. Gerekçe okumak istersen her adımın altında kaynak belge yazılı.

> **Hiçbir şey otomatik yayınlanmadı ve yayınlanmayacak.** Bu belge yalnızca
> talimattır; Play Console'a girişleri sahibi yapar.

---

## Adım 0 — Bunlar bitmeden Console'a girmenin anlamı yok

Dördü de **yayını durduran** maddedir. İkisi kodda, ikisi sahibinde.

| # | Ne | Kimde | Süre |
|---|---|---|---|
| 1 | `PlayerProgress.STARTING_COINS` 100.000 → 100'e döndürülecek | **Kod** | 1 dk |
| 2 | Yeni imzalı AAB üretilecek (elde olan AAB 2026-08-15 tarihli, bugünkü 11 araçlı oyunu içermiyor) + `versionCode` 10 → 11 | **Kod/Build** | 10 dk |
| 3 | GitHub Pages açılacak (gizlilik politikası şu an 404) | **Sahibi** | 2 dk |
| 4 | En az 2 (tercihen 8) ekran görüntüsü çekilecek | **Sahibi + cihaz** | 30 dk |

Ayrıntı ve kanıt: `PLAY_RELEASE_CHECKLIST.md` §0.

---

## Adım 1 — GitHub Pages'i aç (gizlilik politikası)

Dosyalar **hazır ve commitli**; tek iş GitHub arayüzündeki ayardır.

1. GitHub → `MiniAppFactory/KronDrive` → **Settings → Pages**
2. *Source:* **Deploy from a branch**
3. *Branch:* **`main`** · *Folder:* **`/docs`** → **Save**
4. 1-3 dakika bekle
5. **Gizli sekmede** iki adresi de aç, 404 olmadığını gör:
   - `https://miniappfactory.github.io/KronDrive/` (EN)
   - `https://miniappfactory.github.io/KronDrive/tr/` (TR)

Pages çalışmazsa yedek yol (Netlify Drop): `STORE_SUBMISSION_CHECKLIST.md` §2.

---

## Adım 2 — AdMob konsolu (Play'den önce yapılmalı)

| Nerede | Ne yapılacak | Neden |
|---|---|---|
| AdMob → **Privacy & messaging → GDPR** | Mesaj oluşturulmuş ve **PUBLISHED** mı, doğrula. Değilse oluştur ve yayımla | Kod hazır ama mesaj yayımlanmazsa AEA/UK'de onay formu **hiç çıkmaz** → o bölgelerde reklam gelmez |
| AdMob → Apps → Kron Drive → **App settings** | Uygulamayı Play kaydıyla eşleştir (Play'de kayıt oluşturduktan sonra) | app-ads.txt doğrulaması bunu ister |
| AdMob → Apps → Kron Drive → **app-ads.txt** | **Check for updates** bas | Dosya canlı (`https://miniappfactory.github.io/app-ads.txt` → 200), AdMob'un görmesi gerek |

---

## Adım 3 — Play Console'da uygulamayı oluştur

| Alan | Değer |
|---|---|
| App name | `Kron Drive` |
| Default language | **English (United States)** |
| App or game | **Game** |
| Free or paid | **Free** ⚠ geri alınamaz |
| Declarations | Developer Program Policies + US export laws → işaretle |

Paket adı build'den gelir: `com.miniappfactory.krondrive`

---

## Adım 4 — App content (Uygulama içeriği) beyanları

Bu bölümün tamamı doldurulmadan hiçbir track yayınlanamaz.

### 4.1 Privacy policy
| Alan | Değer |
|---|---|
| Privacy policy URL | `https://miniappfactory.github.io/KronDrive/` |

### 4.2 App access
| Alan | Değer |
|---|---|
| All functionality is available without special access | **İşaretle** |

Gerekçe: giriş/hesap yok.

### 4.3 Ads
| Soru | Cevap |
|---|---|
| Does your app contain ads? | **Yes** |

Mağaza sayfasında "Contains ads" rozeti çıkar — beklenen davranış.

### 4.4 Content ratings (IARC anketi)
Cevapların tamamı: **`docs/CONTENT_RATING.md`** — birebir oradan doldur.

Özet: Kategori **Game** · şiddet **yok** (araç çarpışması var; kan, yaralanma,
insan hedefi yok) · cinsellik yok · argo yok · madde yok · korku yok ·
**kumar/loot box yok** (haftalık sandık **sabit** ödül verir: 750 coin +
sabit booster, rastgelelik yok) · kullanıcı etkileşimi **yok** (sohbet,
liderlik tablosu, arkadaş yok) · **reklam içerir → Evet** · konum paylaşımı
yok · satın alma yok.

Beklenen sonuç: **PEGI 3 / ESRB Everyone / USK 0**, "reklam içerir" notuyla.

### 4.5 Target audience and content
| Alan | Değer |
|---|---|
| Target age groups | **13-15, 16-17, 18+** (öneri; sahibi onaylamalı — bkz. S-1) |
| Appeal to children | **No** |

⚠ 12 yaş ve altı işaretlenirse **Families politikası** devreye girer: kodda
TFCD/TFUA set edilmeli ve **yeni bir AAB** üretilmeli. Öneri: işaretleme.

### 4.6 Data safety
Cevapların tamamı: **`docs/DATA_SAFETY_FORM.md`** — birebir oradan doldur.

**Toplanan ve paylaşılan (hepsi AdMob kaynaklı; oyunun kendisi cihazdan
hiçbir veri çıkarmıyor):**

| Kategori | Toplanır | Paylaşılır | Amaç | Zorunlu/Opsiyonel |
|---|---|---|---|---|
| Device or other IDs | Evet | Evet | Advertising; Analytics; Fraud prevention | **Opsiyonel** |
| Approximate location | Evet | Evet | Advertising; Analytics; Fraud prevention | Zorunlu |
| App interactions | Evet | Evet | Advertising; Analytics; Fraud prevention | Zorunlu |
| Diagnostics | Evet | Evet | Analytics; Fraud prevention | Zorunlu |

**Hepsi "No":** kişisel bilgi, finansal bilgi, sağlık, mesaj, foto/video, ses
dosyası, dosya/doküman, takvim, rehber, **tam konum**, web geçmişi, kurulu
uygulamalar, kullanıcı üretimi içerik, **crash logs** (çökme SDK'sı yok).

**Diğer işaretler:** veri aktarımı şifreli → **Evet** · kullanıcı silme
talebinde bulunabilir → **Evet** (yol: gizlilik politikası §7) · veri geçici
işleniyor → **Hayır**.

⚠ **Tutarlılık şartı:** "Device or other IDs → Evet" ile 4.7'deki Advertising
ID beyanı **aynı yönde** olmalı; çelişirse gönderim reddedilir.

### 4.7 Advertising ID
| Soru | Cevap |
|---|---|
| Uygulamanız advertising ID kullanıyor mu? | **Evet** |
| Amaç | **Advertising or marketing** + **Analytics** |

Gerekçe: `AD_ID` izni AdMob SDK'sı üzerinden birleştirilmiş manifeste giriyor;
"Hayır" denirse beyan-manifest çelişkisi doğar ve gönderim reddedilir.

### 4.8 Kalan beyanlar (hepsi tek seferde)
| Beyan | Cevap |
|---|---|
| Government apps | Hayır |
| Financial features | Hayır |
| Health apps | Hayır |
| News app | Hayır |
| COVID-19 contact tracing | Hayır |
| In-app purchases | **Yok** |
| Account deletion URL | **İstenmeyecek** — hesap oluşturma yok |

---

## Adım 5 — Store listing (mağaza sayfası)

### 5.1 İngilizce (varsayılan dil)

| Alan | Değer / dosya |
|---|---|
| App name (30) | `Kron Drive: Retro Car Racer` (27) |
| Short description (80) | `docs/play_store_assets/store_short_description_en.txt` (67) |
| Full description (4000) | `docs/play_store_assets/store_long_description_en.txt` (3708) |
| App icon 512×512 | `docs/play_store_assets/play-store-icon-512.png` |
| Feature graphic 1024×500 | `docs/play_store_assets/feature_graphic_1024x500.png` |
| Phone screenshots | **en az 2, hedef 8** — 1080×1920, plan: `play_store_assets/SCREENSHOT_PLAN.md` |
| Website | `https://miniappfactory.github.io/KronDrive/` |
| E-posta | `whatsthisapp@proton.me` |

### 5.2 Türkçe (ikinci dil ekle)

| Alan | Değer / dosya |
|---|---|
| App name (30) | `Kron Drive: Retro Araba Oyunu` (29) |
| Short description | `store_short_description_tr.txt` (73) |
| Full description | `store_long_description_tr.txt` (3486) |

Görseller EN setiyle paylaşılabilir; TR'ye ayrı ekran görüntüsü **zorunlu değil**.

> ⚠ Metin dosyaları **2026-08-17'de düzeltildi** (11 araç / 11 boya, çarpışma
> kutusu iddiası, hız yükseltmesi iddiası, kaldırılan perfect dodge hedefleri).
> Eski kopyaları kullanma — Console'a **bugünkü dosyaları** yapıştır.

### 5.3 Kategori ve iletişim
| Alan | Değer |
|---|---|
| App category | **Game → Racing** |
| Tags | racing, arcade, offline, casual (Console'un sunduğu listeden) |
| Contact email | `whatsthisapp@proton.me` |

---

## Adım 6 — Ekran görüntüleri

Plan hazır: **`docs/play_store_assets/SCREENSHOT_PLAN.md`** (8 kare, hangisi ne
gösterecek, o duruma nasıl gelinecek).

Zorunlu teknik kurallar:

- Samsung S8 ham `screencap` çıktısı **1440×2960 (≈2.06:1)** — doğrudan
  yüklenmez, **1080×1920'ye** kırpılıp ölçeklenmeli
- Hiçbir karede **reklam veya "Test Ad" etiketi olmayacak**
- Çekim **release** build ile yapılacak (test reklamı çıkmasın)
- ⚠ **Coin sayacı 100.000 gösteren kare yüklenemez** — Adım 0/1 yapılmadan
  çekim yapma, yoksa hepsi çöpe gider
- Yeni başlangıç aracı **Beety**'dir; garaj karesi Şehir'i değil Beety'yi
  gösterecek şekilde güncellenmeli

---

## Adım 7 — Build yükle (Internal testing)

1. Play Console → **Testing → Internal testing → Create new release**
2. **Play App Signing**: ilk yüklemede kayıt teklif edilir → **kabul et**
   (alias `UPLOAD` bir *upload* anahtarıdır; kayıt varsa kaybolursa
   sıfırlatılabilir — kayıt yoksa kayıp **kalıcıdır**)
3. AAB'yi yükle (Adım 0'da üretilen **yeni** dosya; 2026-08-15 tarihli
   `KronDrive_release_2026-08-15_2351_v1.0.9.aab` **kullanılmaz**)
4. Release notes gir (TR + EN)
5. Kendini tester listesine ekle, linki aç, cihaza kur

### Cihazda doğrulanacaklar (release build ile)
- [ ] Gerçek reklam yükleniyor (banner menüde, geçiş koşu sonunda, ödüllü)
- [ ] İlk üç bölümde geçiş reklamı **çıkmıyor**
- [ ] Ödüllü reklam yarıda kesilirse ödül **verilmiyor**
- [ ] Uçak modunda oyun akıyor, reklam yokluğu bloklamıyor
- [ ] Ayarlar → "Gizlilik seçenekleri" satırı (AEA bölgesinden/VPN) görünüyor ve formu açıyor
- [ ] R8/minify açık build'de çökme yok
- [ ] Ayarlardaki sürüm metni doğru sürümü gösteriyor
- [ ] Başlangıç coini **100** (yeni kurulumda)

⚠ Kendi cihazında gerçek reklama **dokunma** — AdMob geçersiz trafik sayar.
Önce `AdIds.developerTestDeviceIds`'e cihaz ID'sini ekle (logcat'te `Ads`
etiketiyle yazılır).

---

## Adım 8 — Kapalı test (gerekliyse)

Google, **kişisel** (organizasyon olmayan) geliştirici hesaplarına production
öncesi **kapalı test + belirli sayıda tester + belirli bir süre** şartı
uyguluyor. Hesap tipin bilinmiyor (S-2).

- Console → *Testing → Closed testing* ekranında bu şart varsa **gösterilir**
- Şart varsa: tester topla, süreyi doldur, sonra production'a başvur
- Şart yoksa: Adım 9'a geç

---

## Adım 9 — Production

1. Countries/regions: **tüm ülkeler** (öneri, S-4)
2. Production → Create new release → aynı AAB'yi promote et
3. **Aşamalı yayın:** %20 → %50 → %100 (erken çökme görülürse durdurulabilir)
4. Gönder → inceleme

---

## Gönderimden hemen önce son bakış

**Bloker**
- [ ] `STARTING_COINS` = 100
- [ ] Yeni AAB üretildi, `versionCode` 11
- [ ] Pages açık, iki URL de 200
- [ ] En az 2 ekran görüntüsü yüklendi

**Beyan**
- [ ] IARC anketi dolduruldu
- [ ] Data safety `DATA_SAFETY_FORM.md` ile birebir dolduruldu
- [ ] Contains ads **Evet** · Advertising ID **Evet** (ikisi tutarlı)
- [ ] Target audience 13+ girildi
- [ ] Privacy policy URL + Website alanı dolu

**Metin**
- [ ] EN başlık + kısa + uzun açıklama (**2026-08-17 sürümü**)
- [ ] TR dili eklendi, üç metin de girildi

**AdMob**
- [ ] GDPR mesajı **published**
- [ ] app-ads.txt doğrulaması tetiklendi

**Anahtar**
- [ ] Play App Signing kaydı **açık** (Console'da görüldü)
- [x] Keystore repo dışına yedeklendi (sahibi beyanı, 2026-08-16)
