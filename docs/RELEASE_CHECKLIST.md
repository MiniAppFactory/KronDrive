# Yayın kontrol listesi

## Şu anki durum (2026-08-14 güncellendi)

| Adım | Durum |
|---|---|
| Debug APK derleniyor | ✅ `:app:assembleDebug` BUILD SUCCESSFUL |
| Motor birim testleri | testler yazıldı, sonuç için CHANGELOG'a bak |
| Gerçek cihaz/emülatör testi | ❌ **YAPILMADI** — bu makinede adb/emülatör yok |
| Release imzalama | ✅ keystore var (`source/my-upload-key.jks`, alias `UPLOAD`) |
| İmzalı AAB | ✅ üretildi — `source/app/build/outputs/bundle/release/app-release.aab` |
| Gerçek AdMob kimlikleri | ❌ **hâlâ test kimlikleri** — mevcut AAB bu yüzden üretime uygun değil |
| Uyum belgeleri | ✅ hazır — bkz. aşağıdaki tablo (bölüm 5) |
| Play Store listesi (görsel + metin) | ❌ hazırlanmadı |

Aşağıdaki hiçbir madde "yapıldı" sayılmamalı — hiçbiri cihazda doğrulanmadı.

## 1. Keystore oluştur (bir kez) — ✅ **YAPILDI (2026-08-14)**

> `source/my-upload-key.jks` ve `source/signing.properties` mevcut (alias `UPLOAD`),
> ikisi de `.gitignore`'da. Aşağıdaki yordam yalnızca referans/yeniden kurulum içindir.
> **Keystore'u yedekle — kaybolursa bu uygulamanın güncellemesi bir daha yayınlanamaz.**

```
keytool -genkeypair -v -keystore my-upload-key.jks -keyalg RSA -keysize 2048 \
        -validity 10000 -alias upload
```

Dosyayı `source/my-upload-key.jks` konumuna koy ve `source/signing.properties`
oluştur (ikisi de `.gitignore`'da, asla commit edilmez):

```
storePassword=...
keyPassword=...
keyAlias=upload
```

`app/build.gradle.kts` bu dosyayı görür görmez release signingConfig'i
kendiliğinden devreye alır. Keystore yoksa release build imzasız üretilir
(kasıtlı: build hiç patlamasın diye).

**Keystore kaybolursa uygulamanın güncellemesi bir daha yayınlanamaz.** Yedekle.

## 2. Gerçek AdMob kimliklerini gir

`docs/ADMOB_SETUP.md` — iki dosya, üç sabit, bir bayrak.

## 3. Sürüm numarası

`app/build.gradle.kts` → `versionCode` / `versionName`.
Play Console her track'te **tekil ve artan** versionCode ister.

## 4. Cihazda doğrulanması gerekenler (henüz hiçbiri yapılmadı)

Oyun mekaniği:
- [ ] Kontroller: sol/sağ dokunuşu, fren ve boost basılı tutma
- [ ] Perfect Dodge gerçekten şerit değiştirirken tetikleniyor mu, yan şeritten
      temiz geçişte tetiklenmiyor mu
- [ ] Combo zinciri ve çarpanlar
- [ ] Bölüm hedefleri ve yıldızların doğru verilmesi
- [ ] Sonsuz mod zorluk artışı ve rekor mesajı

Platform:
- [ ] Portre kilidi, güvenli alan (centikli ekran, gesture bar)
- [ ] Uygulama arka plana alınıp geri gelince oyun duraklamış ve ses susmuş mu
- [ ] Telefonun ses tuşları oyun sesini kontrol ediyor mu
- [ ] Düşük/orta seviye cihazda kare hızı (hedef: sabit 60 fps, düşüşsüz)
- [ ] Bellek ve ısınma davranışı
- [ ] Uçak modunda: oyun sorunsuz oynanıyor, reklam yokluğu akışı bloklamıyor

Reklamlar:
- [ ] Banner sadece menü ekranlarında, kontrollerin üstünde değil
- [ ] Gecis reklamı her 2 bölümde bir, oyun sırasında asla
- [ ] Ödüllü reklam izlenince devam ediyor, yarıda kesilince devam ETMİYOR
- [ ] Reklam yüklenemediğinde akış devam ediyor

Kalıcılık:
- [ ] Uygulama kapatılıp açılınca coin, yıldız, yükseltme, rekor korunuyor
- [ ] Günlük görev ertesi gün değişiyor, haftalık görevler hafta başında sıfırlanıyor

## 5. Play Console

> **Bu bölüm taşındı.** Politika, form ve mağaza beyanlarının tamamı artık
> **`docs/STORE_SUBMISSION_CHECKLIST.md`** dosyasındadır — orada her madde için
> hazır/eksik/beklenen durumu, kod uyum denetimi (C-1…C-9) ve hedef kitle
> analizi var. Aynı listeyi iki yerde tutmamak için burada yalnızca özet ve
> teknik olan tek madde bırakıldı.

Destekleyici belgeler:

| Belge | İçerik |
|---|---|
| `docs/STORE_SUBMISSION_CHECKLIST.md` | Play Console adım adım + kod uyum denetimi + hedef kitle kararı |
| `docs/DATA_SAFETY_FORM.md` | Data Safety formunun her sorusuna cevap |
| `docs/CONTENT_RATING.md` | IARC anketi cevapları ve beklenen derecelendirme |
| `docs/PRIVACY_POLICY_EN.md` / `_TR.md` | Gizlilik politikası kaynak metni |
| `docs/index.html` · `docs/tr/index.html` · `docs/app-ads.txt` | GitHub Pages'te yayınlanacak dosyalar |

Teknik madde (bu dosyanın kapsamında kalan):

- [ ] AAB üret: `./gradlew :app:bundleRelease`
      → `source/app/build/outputs/bundle/release/app-release.aab`
      **Not:** gerçek AdMob kimlikleri girildikten ve `appCategory="game"`
      eklendikten sonra versionCode artırılıp **yeniden** üretilmelidir.

## 6. Yayın sonrası izlenecekler

Crash oranı, ANR, gecis reklamı gösterim/tıklama oranı, ödüllü reklam
tamamlama oranı, 1./7. gün tutunma, ortalama koşu süresi, hangi bölümde
oyuncuların takıldığı (bölüm hedefleri fazla sıkıysa `LevelCatalog`'dan
ayarlanır).
