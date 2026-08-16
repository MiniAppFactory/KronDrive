# Ekran görüntüsü planı — Kron Drive v1.0.9

Hazırlayan: play-store-compliance-engineer · 2026-08-16
Durum: **kareler henüz çekilmedi** — çekimi sahibi yapacak (cihaz başka ajanda).

Bu plan v1.0.9'a göre yazıldı. v1.0.9'un iki görsel yeniliği var ve **ikisi de
mağaza görsellerinde görünmek zorunda**, çünkü ikon ve feature graphic zaten
yeni araç sprite'larıyla üretildi — ekran görüntüleri eski görünürse mağaza
sayfası kendi içinde tutarsız olur:

1. **Yeni araç sprite'ları** (7 gövde: Şehir, Yarış Sedan, Kuş SLX, Dağ Keçisi,
   Kas Arabası, Boğa 67, Süper Araba) + fabrika boyası
2. **Yenilenen kontrol ikonları** (sol/sağ üstte, fren/boost altta, korna)

---

## 1. Teknik hedef

| Özellik | Değer | Not |
|---|---|---|
| Adet | **8** (Play'in telefon üst sınırı) | En az 2 zorunlu; 4'ün altına düşme |
| Yön | Dikey (portre) | Oyun portre kilitli (`AndroidManifest.xml`) |
| **Hedef çözünürlük** | **1080 × 1920 (tam 9:16)** | Güvenli hedef: hem genel sınırların hem de tanıtım yerleşimi eşiğinin içinde |
| Biçim | 24-bit PNG (alfa kanalı YOK) veya JPEG | Şeffaflık kabul edilmez |
| Sıra | Aşağıdaki 1→8 sırası | İlk 3 kare listede önizlemede görünür; en güçlüleri başa koy |

### Dikkat — Samsung S8'in ham çıktısı doğrudan yüklenemez

Cihazın ham `screencap` çıktısı **1440 × 2960** (≈2.06:1). Bu oran 9:16
(1.78:1) değildir ve 2:1 sınırının da üstündedir. **Ham kare doğrudan
yüklenmemeli.** İki yoldan biri uygulanmalı:

- **Tercih edilen:** kareyi 1080 genişliğe ölçekle, üst/alt taraftan
  **1080 × 1920 olacak biçimde kırp**. Kırparken HUD (skor/coin, üst şerit) ve
  kontrol tuşları kadraj dışında kalmamalı — bu ikisi karenin satış argümanı.
- Alternatif: 1080 × 1920 tuvale oyunun karesini oturt, boşluğu oyunun arka
  plan rengiyle doldur (letterbox). Yalnızca kırpma HUD'ı yiyorsa kullan.

Mevcut `tools/caption_screenshot.py` girdinin boyutunu korur; **kırpma/ölçekleme
bu betiğin işi değil, ondan ÖNCE yapılmalı.** `screenshot_caption_strip_1080x2400.png`
şablonu 1080×2400 (2.22:1) — bu oran hedef dışı, **şablon 1080×1920'ye
güncellenmeli veya kullanılmamalı.**

### Çekim yordamı (cihaz serbest kaldığında)

```
adb exec-out screencap -p > kare_01.png
```

`adb` PATH'te değil, tam yolla çağrılır:
`AppData\Local\Android\Sdk\platform-tools\adb.exe`

**Çekim release APK ile yapılmalı** (`builds/KronDrive_release_2026-08-15_2351_v1.0.9.apk`),
debug ile değil — debug build test reklamı gösterir ve "Test Ad" etiketi bir
kareye girerse mağaza görseli yanıltıcı olur.

---

## 2. Sekiz kare

Her kare için: ne görünecek, nasıl o duruma gelinir, hangi yeniliği kanıtlar.

### Kare 1 — Oynanış: Perfect Dodge anı (EN GÜÇLÜ KARE, başa)
- **Ne:** Koşu sırasında, oyuncu aracı bir trafik aracının **yanından kıl payı
  geçerken**; ekranda Perfect Dodge geri bildirimi ve combo çarpanı görünür.
- **Nasıl:** Bölüm 3-5 civarı, orta hız. Combo en az 1.5x olsun.
- **Kanıtladığı:** Yeni araç sprite'ları + oyunun çekirdek mekaniği + HUD.
- **Şart:** Alt kontrol tuşları (fren/boost) ve üst yön tuşları kadrajda olacak.

### Kare 2 — Oynanış: yüksek combo + boost
- **Ne:** Boost basılıyken, boost şeridi üstte dolu/azalıyor, combo 2x veya 3x.
- **Nasıl:** Boost barı doluyken uzun bas.
- **Kanıtladığı:** Hız hissi, yenilenen kontrol ikonları (boost tuşu basılı hâlde).

### Kare 3 — Garaj: araç seçimi
- **Ne:** Garaj ekranı, **yeni sprite'larıyla birden fazla gövde** aynı anda
  görünür durumda; seçili araç büyük gösterimde.
- **Nasıl:** Ana menü → Garaj. Mümkünse en az 2-3 aracın açık olduğu bir kayıt
  kullan (hepsi kilitli görünürse kare "hiçbir şeyim yok" hissi verir).
- **Kanıtladığı:** 7 gövde iddiası (uzun açıklamada yazıyor) — **bu kare o
  iddianın görsel kanıtı.**

### Kare 4 — Garaj: boya / özelleştirme
- **Ne:** Boya paleti açık, birkaç renk seçilebilir hâlde, araç boyalı önizlemede.
- **Kanıtladığı:** 10 boya + fabrika boyası yeniliği.

### Kare 5 — Bölüm haritası (30 bölüm, yıldızlar)
- **Ne:** Bölüm haritası; bir kısmı açık ve **yıldız kazanılmış**, ilerisi kilitli.
- **Nasıl:** En az 5-6 bölüm bitmiş bir kayıt.
- **Kanıtladığı:** "30 bölüm, 90 yıldız" iddiası. İlerleme hissi.

### Kare 6 — Yükseltmeler (HIZ / İVME / FREN / BOOST)
- **Ne:** Yükseltme ekranı, dört dal ve seviye çubukları; en az bir dal
  kısmen yükseltilmiş.
- **Kanıtladığı:** Uzun vadeli ilerleme ve coin'in bir işe yaradığı.

### Kare 7 — Görevler (günlük + haftalık)
- **Ne:** Görev ekranı; günlük görevin kademeleri ve haftalık görevler,
  en az biri kısmen tamamlanmış.
- **Kanıtladığı:** Geri dönme sebebi (retention) — mağaza sayfasında
  "her gün yeni bir şey var" mesajı.

### Kare 8 — Sonsuz mod / koşu sonu skoru
- **Ne:** Sonsuz mod koşu sonu ekranı; skor, mesafe, kişisel rekor.
- **Nasıl:** Sonsuz modda makul bir skor yap.
- **Kanıtladığı:** Kariyer dışında da içerik olduğu.

---

## 3. Kare seçiminde uyulacak kurallar

- **Reklam görünen kare YOK.** Ne banner ne geçiş reklamı hiçbir karede
  olmayacak. Banner menü ekranlarında çıkıyor — Kare 3-7 çekilirken banner
  kadraja girerse o bölge kırpılmalı ya da kare banner yüklenmeden çekilmeli.
- **"Test Ad" etiketi hiçbir karede olmayacak** (release APK ile çek).
- **Sistem çubukları:** saat/pil/sinyal karede kalabilir, sorun değil; ama
  bildirim ikonları (WhatsApp vb.) kadrajda olmasın — çekimden önce
  bildirimleri temizle.
- **Çocuk kitlesine yönelik öge yok** — hedef kitle beyanı 13+ olacaksa
  görseller de o çizgide olmalı (bkz. `STORE_SUBMISSION_CHECKLIST.md` §6).
- **Karede yazı/rozet varsa** ("#1", "En iyi", "Ücretsiz") **kullanılmaz** —
  Play metadata politikası bunları mağaza görsellerinde de yasaklıyor.
- **Türkçe mağaza sayfası için ayrı set gerekir mi?** Zorunlu değil ama
  önerilir: TR sayfasında oyunun dili Türkçe olan kareler dönüşümü artırır.
  Oyun dili ayarlardan tek dokunuşla değişiyor — **aynı 8 kare Türkçe arayüzle
  bir kez daha çekilirse** TR sayfası tam lokalize olur. Zaman yoksa EN set
  her iki dilde de kullanılabilir.

## 4. Alt yazı (caption) şeridi — isteğe bağlı

`tools/caption_screenshot.py` karenin üstüne başlık şeridi basıyor
(`screenshot_caption_example_{tr,en}.png` örnekleri var). Kullanılacaksa:

- Yalnızca ilk 3-4 karede kullan; hepsinde kullanmak kalabalık yapar.
- Şerit yüksekliği karenin oyun alanını yemesin — kırpma **şeritten önce** yapılır.
- Önerilen başlıklar:

| Kare | EN | TR |
|---|---|---|
| 1 | PERFECT DODGE | KIL PAYI GEÇ |
| 2 | CHAIN THE COMBO | COMBO'YU ZİNCİRLE |
| 3 | SEVEN CARS | YEDİ ARAÇ |
| 5 | 30 LEVELS, 90 STARS | 30 BÖLÜM, 90 YILDIZ |

## 5. Kontrol listesi (çekim bitince işaretle)

- [ ] 8 kare çekildi, hepsi release APK ile
- [ ] Hepsi 1080 × 1920 PNG'ye dönüştürüldü (alfa kanalı yok)
- [ ] Hiçbirinde reklam / "Test Ad" etiketi yok
- [ ] Kare 3 ve 4'te yeni araç sprite'ları ve boya net görünüyor
- [ ] Kare 1 ve 2'de yenilenen kontrol ikonları kadrajda
- [ ] Bildirim ikonu / kişisel bilgi içeren kare yok
- [ ] Dosyalar `docs/play_store_assets/screenshots/` altına `01_..._08_` sırasıyla kondu
- [ ] (İsteğe bağlı) Türkçe arayüzle ikinci set çekildi
