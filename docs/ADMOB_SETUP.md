# AdMob kurulumu — yayın öncesi yapılacaklar

Şu an projede **yalnızca Google'ın herkese açık test reklam kimlikleri** var.
Uygulama bu haliyle çalışır ve test reklamları gösterir; gerçek gelir için
aşağıdaki adımlar gerekir.

## 1. AdMob konsolunda oluşturulacaklar

- Uygulama: **Kron Drive** (Android, `com.miniappfactory.krondrive`)
- Reklam birimleri:
  - Banner (menü ekranları)
  - Interstitial (bölümler arası)
  - Rewarded (çarpışma sonrası devam)

## 2. Değiştirilecek TAM İKİ YER

### a) `app/src/main/AndroidManifest.xml`

```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-3940256099942544~3347511713" />   <!-- ← gerçek App ID -->
```

### b) `app/src/main/java/com/miniappfactory/krondrive/ads/AdIds.kt`

```kotlin
private const val PRODUCTION_BANNER_AD_UNIT_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX"
private const val PRODUCTION_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX"
private const val PRODUCTION_REWARDED_AD_UNIT_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX"

private const val USE_TEST_IDS_IN_RELEASE = true   // ← gerçek ID'ler girilince false yap
```

`USE_TEST_IDS_IN_RELEASE` bilerek `true` bırakıldı: gerçek ID girilmeden
release build alınırsa geçersiz kimlikle yayına çıkılmasın diye. Debug build
bu bayraktan bağımsız **her zaman** test reklamı gösterir.

Başka hiçbir dosyada reklam kimliği yoktur.

## 3. Kendi cihazını test cihazı olarak ekle

Release build'i kendi telefonunda test ederken kendi reklamına tıklamak AdMob
"geçersiz trafik" ihlalidir. İlk reklam isteğinde logcat'te `Ads` etiketiyle
şuna benzer bir satır çıkar:

```
Use RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList("XXXX")) ...
```

Oradaki kimliği `AdIds.developerTestDeviceIds` listesine ekle.

## 4. UMP (kullanıcı onayı)

Consent akışı `MainActivity` içinde kurulu. AdMob konsolunda
**Privacy & Messaging → GDPR/US states** mesajı yayınlanmalı; yayınlanmazsa
`requestConsentInfoUpdate` hata döner ve kod `canRequestAds()` doğruysa
reklamlara yine devam eder. Ayrıca 4 saniyelik bir güvenlik ağı vardır —
consent akışı hiç yanıt vermezse banner o oturum boyunca kaybolmasın diye.

## 5. Play Console veri güvenliği

Uygulama kendi başına hiçbir kişisel veri toplamaz (hesap yok, sunucu yok,
analytics yok). Ancak **Google Mobile Ads SDK** reklam kimliği ve cihaz
bilgisi toplar — Data Safety formunda bu beyan edilmelidir. `AD_ID` izni
SDK tarafından manifest birleşmesiyle eklenir.
