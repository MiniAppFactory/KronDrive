# Kron Drive — çalışma kuralları

## Oturuma başlarken

`mem` skill'ini çağır ve şunları oku:

- `C:\Users\bhdre\.claude\memory\projects\kron-drive.md` (kalıcı proje hafızası)
- `docs/HANDOVER_20260816.md` (en son handover)

Bu iki dosya "nerede kalmıştık"ı anlatır. Hafıza yazıldığı andaki durumdur;
bir dosya/fonksiyon adı geçiyorsa iddia etmeden önce koda bakıp doğrula.

Oturum sonunda (veya kullanıcı "kaydet" dediğinde) `mem` skill'indeki
kaydetme yordamını uygula.

## Bu proje hakkında

Native Kotlin/Compose Android oyunu. WebView yok, sunucu yok, hesap yok.
Android Studio'da açılacak klasör: `source/`.
Paket: `com.miniappfactory.krondrive`.

HTML prototipi (`KRON_DRIVE_FINAL_BALANCED_80_3.html`) **oyun hissinin
doğruluk kaynağıdır**. Fizik sabitleri oradan birebir alındı; tuhaf görünen
sayılar kasıtlıdır. Bilinçli sapmaların tamamı `PROVENANCE.md`'de yazılı —
oraya yazmadan sapma yapma.

## Değişmez kurallar

1. **Denge değerleri tek yerde.** Yeni bir sabit gerekiyorsa
   `game/GameConfig.kt`, `game/LevelCatalog.kt` veya `game/UpgradeCatalog.kt`
   içine yaz. Ekran/motor dosyalarına sihirli sayı gömme.
2. **`game/` paketi saf Kotlin kalır.** Android importu girmez; JVM testleriyle
   doğrulanabilirliği bundan geliyor.
3. **Simülasyon durumu Compose durumu değildir.** `GameEngine` düz alanlar
   tutar, ekran her karede adım atıp yeniden çizer. Motor alanlarını
   `mutableStateOf` ile sarma — 60 Hz'de tüm ağaç yeniden bestelenir.
4. **Reklam akışı oyunu asla bloklamaz.** Ödül yalnızca SDK'nın gerçek
   "kazanıldı" geri çağrısında verilir. Oyun ekranında banner yok.
5. **Web3 kapalı.** Token, cüzdan, gerçek para, "kazan" vaadi yok; sahibi
   açıkça istemedikçe eklenmez.
6. **Cihaz doğrulaması artık MÜMKÜN (2026-08-15'ten beri).** Samsung S8
   (SM-G950F) USB ile bağlı. adb PATH'te değil, tam yolla çağrılır:
   `AppData\Local\Android\Sdk\platform-tools\adb.exe` (kullanıcı klasörü
   altında). `install -r`, `shell input tap`, `exec-out screencap`, logcat
   çalışıyor — bir davranışı iddia etmeden önce **cihazda dene**. Pause
   tuşunun çalışmadığı böyle bulundu.
   Kural değişmedi: yalnızca gerçekten doğrulanan yazılır; denenmediyse
   "denenmedi" denir. Emülatör hâlâ yok.
7. **Metinler `AppLanguage.pick(tr = …, en = …)` üzerinden.** Tek dilli
   sabit metin yazma.

## Ekip — hangi iş kime gider

**Ajan çağırmak için izin sorma (proje sahibi kararı, 2026-08-14).** İhtiyaç
duyduğunda doğrudan çağır; her seferinde "ajan çalıştırayım mı" diye sorma.
Bağımsız işleri **tek mesajda paralel** başlat (örn. store görselleri +
keystore + compliance aynı anda). Ajanların farklı zamanlarda dönmesi normal —
biri beklerken diğer işe devam et, gelen sonucu sen özetleyip aktar.

Ajan çağırmanın hâlâ **yanlış** olduğu yer: küçük ve tek dosyalık işler
(sabit değiştir, test ekle, APK çıkar). Orada bağlamı ajana anlatmak işi
yapmaktan uzun sürer — kendin yap.

Ajan ne yaparsa yapsın, kanıt standardı değişmez: build/test çıktısı
görülmeden "tamamlandı" denmez, ajanın raporu tek başına kanıt sayılmaz.

Orkestrasyon bu oturumda yürütülür; ajanlar `.claude/agents/` altında
(19 ajan, CEO seviyesindeki `APPDeveloper/.claude/agents/` ile birebir aynı).

| İş | Skill | Ajan |
|---|---|---|
| Görsel tasarım, ikon, store görselleri | `game-art` | `ui-ux-mobile-designer` |
| Derinlik/perspektif/paralaks hissi | `game-depth-3d` | `gameplay-developer` |
| Bölüm, zorluk, ekonomi, görev dengesi | `game-scenario` | `product-owner`, `gameplay-developer` |
| Ses efekti ve motor sesi | `game-audio` | `gameplay-developer` |
| Kotlin/Compose implementasyon | — | `kotlin-android-developer` |
| Test | — | `qa-test-engineer`, `regression-guardian` |
| Build/imzalama/AAB | — | `build-release-engineer` |
| Reklam | — | `admob-monetization-engineer` |
| Play Store uyum | — | `play-store-compliance-engineer` |
| Oturumlar arası hafıza | `mem` | — |

## Doğrulama

```
cd source
./gradlew :app:testDebugUnitTest --offline
./gradlew :app:assembleDebug --offline
./gradlew :app:assembleRelease --offline
```

Üçü de geçmeden "bitti" deme. Oyun mantığını değiştirdiysen `app/src/test/`
altındaki testleri de güncelle.

## En son handover

`docs/HANDOVER_20260816_2.md` (2026-08-16 akşam) — bir öncekini
(`HANDOVER_20260816.md`) **iptal etmez, üzerine ekler**. Yarım kalan
performans ölçümü, onaylanmış ama uygulanmamış ekonomi değişiklikleri ve
sahibinin verdiği yeni "Beety" aracı orada.
