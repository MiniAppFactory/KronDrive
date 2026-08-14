# Kron Drive — Android

Şeritli trafik-atlatma oyunu. Kısa koşular, üç yıldızlı bölümler, Perfect Dodge
combo'ları, sonsuz mod ve araç yükseltmeleri. Tamamen çevrimdışı (reklamlar
hariç), sunucusuz, hesapsız.

- **Paket adı:** `com.miniappfactory.krondrive`
- **Sürüm:** 1.0.0 (versionCode 1)
- **minSdk / targetSdk:** 24 / 36
- **Teknoloji:** Kotlin, Jetpack Compose, DataStore, Google Mobile Ads
- **Android Studio projesi:** `source/`

## Hızlı başlangıç

```
cd source
./gradlew :app:assembleDebug        # debug APK
./gradlew :app:testDebugUnitTest    # motor birim testleri
```

Çıktı: `source/app/build/outputs/apk/debug/app-debug.apk`

Android Studio'da `source/` klasörünü aç, Gradle sync et, çalıştır.

## Neyi nerede bulurum

| Ne | Nerede |
|---|---|
| Oyun simülasyonu (fizik, çarpışma, dodge) | `source/app/src/main/java/.../game/GameEngine.kt` |
| **Tüm denge değerleri** | `source/app/src/main/java/.../game/GameConfig.kt` |
| **Bölüm tanımları** | `source/app/src/main/java/.../game/LevelCatalog.kt` |
| Yükseltme maliyet/etkileri | `source/app/src/main/java/.../game/UpgradeCatalog.kt` |
| Günlük / haftalık görevler | `source/app/src/main/java/.../data/*Generator.kt` |
| Yerel kayıt (DataStore) | `source/app/src/main/java/.../data/GameStateRepository.kt` |
| Çizim (Canvas) | `source/app/src/main/java/.../ui/game/GameRenderer.kt` |
| Oyun ekranı ve döngüsü | `source/app/src/main/java/.../ui/game/GameScreen.kt` |
| **AdMob kimlikleri** | `source/app/src/main/java/.../ads/AdIds.kt` + `AndroidManifest.xml` |
| Motor sesi (sentez) | `source/app/src/main/java/.../audio/EngineSoundManager.kt` |

## Yeni bölüm eklemek

`LevelCatalog.kt` içindeki listeye bir satır ekle — başka hiçbir yeri
değiştirmen gerekmez:

```kotlin
LevelDef(
    id = 31,
    goal = LevelGoal.SurviveTime(90),          // veya ReachDistance(metre, süreLimiti)
    stars = listOf(
        Objective.PerfectDodges(12),           // 1. yıldız
        Objective.ComboAtLeast(6),             // 2. yıldız
        Objective.ScoreAtLeast(6000)           // 3. yıldız
    )
)
```

Yeni bir hedef **türü** gerekiyorsa `GameModels.kt` içindeki `Objective`
sealed class'ına bir `data class` ekle; motor ve UI otomatik çalışır.

## Kayıt

Tek bir DataStore dosyası (`kron_drive_progress`): coin, XP, açılan bölümler,
bölüm başına en iyi yıldız, yükseltme seviyeleri, booster envanteri, sonsuz mod
rekoru, günlük/haftalık görev durumu, ses ve dil tercihi. Bulut yok, hesap yok.

## Reklamlar

Banner yalnızca menü ekranlarında; gecis reklamı her 2 bölümde bir, sadece
sonuç ekranından çıkarken; ödüllü reklam yalnızca "çarptın → devam et" için
(koşu başına 1). Reklam yüklenemezse oyun akışı hiç bloklanmaz. İnternet yoksa
oyun sorunsuz oynanır.

## Belgeler

- **`docs/HANDOVER_20260813.md` — devir belgesi. Projeye yeni başlıyorsan önce bunu oku.**
- `CLAUDE.md` — bu projede çalışırken uyulacak kurallar (Claude için)
- `PROVENANCE.md` — kodun kökeni, prototipten bilinçli sapmalar
- `docs/ARCHITECTURE.md` — katmanlar, veri akışı, kritik kararlar
- `docs/BALANCE.md` — her sayının nereden geldiği
- `docs/ADMOB_SETUP.md` — yayın öncesi reklam kimliği adımları
- `docs/RELEASE_CHECKLIST.md` — imzalama, cihaz testi, Play Console
- `docs/CHANGELOG.md`

## Durum

Derleniyor ve debug APK üretiyor. **Gerçek cihazda veya emülatörde
çalıştırılmadı** — bu makinede adb/emülatör yok. Release imzalama ve gerçek
AdMob kimlikleri henüz yapılmadı (bkz. `docs/RELEASE_CHECKLIST.md`).
