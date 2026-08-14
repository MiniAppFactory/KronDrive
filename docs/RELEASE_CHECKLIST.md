# Yayın kontrol listesi

## Şu anki durum (2026-08-13)

| Adım | Durum |
|---|---|
| Debug APK derleniyor | ✅ `:app:assembleDebug` BUILD SUCCESSFUL |
| Motor birim testleri | testler yazıldı, sonuç için CHANGELOG'a bak |
| Gerçek cihaz/emülatör testi | ❌ **YAPILMADI** — bu makinede adb/emülatör yok |
| Release imzalama | ❌ keystore yok |
| Gerçek AdMob kimlikleri | ❌ test kimlikleri kullanılıyor |
| Play Store listesi | ❌ hazırlanmadı |

Aşağıdaki hiçbir madde "yapıldı" sayılmamalı — hiçbiri cihazda doğrulanmadı.

## 1. Keystore oluştur (bir kez)

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

- [ ] Uygulama ikonu 512×512 (`docs/play_store_assets/play-store-icon-512.png` hazır)
- [ ] Özellik grafiği 1024×500 — **yok, üretilmeli**
- [ ] En az 2 telefon ekran görüntüsü — **yok, cihazdan alınmalı**
- [ ] Kısa/uzun açıklama (TR + EN)
- [ ] Gizlilik politikası URL'si — **yok, gerekli** (reklam SDK'sı veri topluyor)
- [ ] Data Safety formu: reklam kimliği + cihaz bilgisi (Mobile Ads SDK)
- [ ] İçerik derecelendirme anketi
- [ ] Hedef kitle: 13+ önerilir (reklam içeriği nedeniyle)
- [ ] AAB üret: `./gradlew :app:bundleRelease`

## 6. Yayın sonrası izlenecekler

Crash oranı, ANR, gecis reklamı gösterim/tıklama oranı, ödüllü reklam
tamamlama oranı, 1./7. gün tutunma, ortalama koşu süresi, hangi bölümde
oyuncuların takıldığı (bölüm hedefleri fazla sıkıysa `LevelCatalog`'dan
ayarlanır).
