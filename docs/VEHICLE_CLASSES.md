# Araç Sınıfları — tasarım

**Durum:** taslak / karar bekliyor. **Tarih:** 2026-08-16. **Kod yazılmadı.**
Bu belge yalnızca tasarımı ve gerekçesini anlatır; `source/` altında hiçbir
dosyaya dokunulmadı.

**Kaynak dosyalar:** `game/GameConfig.kt`, `game/CarCatalog.kt`,
`game/GameEngine.kt` (`updateObstacles`), `ui/common/CarSprites.kt`,
`ui/common/CarArtwork.kt`, `tools/build_car_sprites.py`,
`app/src/test/.../CarCatalogTest.kt`, `PROVENANCE.md` #11 ve #16.

---

## 0. Bugünkü durum — ölçülmüş sayılar

Bütün tartışma bu sayıların üzerinde döndüğü için önce onları sabitliyorum.
Hepsi `GameConfig`'ten türetildi, elle yazılmadı.

| Değer | Formül | Sonuç |
|---|---|---|
| Sanat ölçeği | `CAR_ART_SCALE` | 0.80 |
| Çizim kutusu (birim) | `x -20..20`, `y -2..74` | 40 × 76 |
| Görünür araç (dp) | `40·0.8`, `76·0.8` | 32.00 × 60.80 |
| Hitbox ölçeği | `HITBOX_SCALE` | 0.88 |
| Hitbox (dp) | görünürün %88'i | 28.16 × 53.50 |
| Yanal görsel payı | `(32 − 28.16)/2` | **1.92 dp / kenar** |
| Boyuna görsel payı | `(60.8 − 53.50)/2` | **3.65 dp / uç** |

Şerit genişliği ekrana bağlı: `min(290, W·0.56) / 3`.

| Ekran (dp) | Yol | Şerit | Aracın şeritteki payı |
|---|---|---|---|
| 320 | 179.2 | **59.73** | %53.6 |
| 360 | 201.6 | 67.20 | %47.6 |
| 412 | 230.7 | 76.90 | %41.6 |
| ≥518 (tablet) | 290.0 | 96.67 | %33.1 |

En dar destekleyeceğimiz şerit **59.73 dp**. Aşağıdaki bütün "sığıyor mu"
hesapları bu en kötü durumda yapıldı.

### Çarpışma nasıl oluşuyor (Minkowski — bu belgenin en önemli sayısı)

`GameEngine.updateObstacles` iki dikdörtgeni `rectHit` ile karşılaştırıyor.
İki eksenli AABB testinin anlamı şu:

```
yatay çarpma eşiği  |dx| < (genişlikA + genişlikB) / 2
dikey  örtüşme      |dy| < (boyA + boyB) / 2
```

Bugün her iki araç da aynı kutuyu kullandığı için eşik tam olarak
`28.16` ve `53.50`. **Kritik sonuç: oyuncunun kutusunu %45 daraltmak
çarpışma eşiğini %45 daraltmaz** — trafiğin kutusu değişmediği için oyuncunun
katkısı yarı yarıyadır. Motosiklet tartışmasının tamamı bu tek gerçeğe
dayanıyor ve dengeyi düşündüğümüzden çok daha az bozuyor (§2).

### Bugünkü sprite hattının sessiz kayması (ölçüldü, düzeltilmeli)

`build_car_sprites.py` her referansı `scale = min(240/w, 456/h)` ile 240×456
tuvale sığdırıp **ortalıyor**. Referansın oranı 40:76 = 0.526 değilse fark
sessizce şeffaf boşluğa dönüşüyor. Bugün depoda duran referansların gerçek
piksel ölçüleri:

| Gövde | Referans bbox | Oran | Sprite | Görsel dp | Hitbox'a pay/kenar |
|---|---|---|---|---|---|
| hatchback | 635×1226 | 0.518 | 236×456 | 31.47 | +1.65 |
| race_sedan | 612×1215 | 0.504 | 230×456 | 30.67 | +1.25 |
| supercar | 600×1220 | 0.492 | 224×456 | 29.87 | +0.85 |
| traffic | 601×1248 | 0.482 | 220×456 | 29.33 | +0.59 |
| kus_slx | 574×1196 | 0.480 | 219×456 | 29.20 | +0.52 |
| muscle | 560×1188 | 0.471 | 215×456 | 28.67 | +0.25 |
| mountain_goat | 560×1207 | 0.464 | 212×456 | 28.27 | **+0.05** |
| muscle_67 | 542×1191 | 0.455 | 208×456 | 27.73 | **−0.21** |
| *(aday)* 10_f1 | 578×1223 | 0.473 | 216×456 | 28.80 | +0.32 |
| *(aday)* 06_taksi | 586×1188 | 0.493 | 225×456 | 30.00 | +0.92 |
| *(aday)* 05_beyaz_sedan | 605×1235 | 0.490 | 223×456 | 29.73 | +0.79 |
| *(aday)* 11_station_wagon | 523×1205 | 0.434 | 198×456 | 26.40 | **−0.88** |
| *(aday)* 07_motosiklet | 460×1228 | 0.375 | 171×456 | 22.80 | **−2.68** |
| *(aday)* 09_tir | 412×1738 | 0.237 | 108×456 | 14.40 | **−6.88** |

Okunuşu: **Boğa 67 bugün, v1.0.9'da yayınlanmış hâliyle, hitbox'ı çizimden
0.21 dp geniş bir araç.** Dağ Keçisi'nde pay sıfıra oturmuş. Yani
2026-08-13'te düzeltilen "havaya çarptım" hatası **sprite hattı üzerinden
milimetrik olarak geri gelmiş durumda** — vektör tarafını koruyan
`CarCatalogTest` sprite'ları hiç görmüyor.

Sayı küçük (S8'de kenar başına ~0.65 px, oyuncunun fark etmesi beklenmez) ama
yön yanlış ve mekanizma sessiz. Sınıf çalışmasından **bağımsız olarak**
düzeltilmeli; sınıflar bunu hızlandıracağı için §6 Adım 0 bu.

> **Emin değilim:** referans bbox'ları alfa > 8 eşiğiyle ölçüldü. Referansta
> pişmiş bir gölge/hale varsa bbox aracın kendisinden büyüktür ve yukarıdaki
> paylar **olduğundan iyimserdir**. Alfa > 128 ile ikinci bir ölçüm alınıp
> fark raporlanmalı (§4).

---

## 1. Sınıf kavramı

### Kaç sınıf ve neden

**Üç sınıf öneriyorum: `MOTOSIKLET`, `BINEK`, `AGIR`.** F1 için dördüncü bir
sınıf (`FORMULA`) *mümkün ama gereksiz* — gerekçesi aşağıda.

Sınıf sayısını üçte tutmanın sebebi keyfi değil: her sınıf bir **sprite tuvali**,
bir **denge muhakemesi** ve bir **test kümesi** demek. Gövde başına serbest
ölçü (yani `CarShapeDef`'e iki float eklemek) ilk bakışta esnek görünüyor ama
altı ay sonra birbirinden %3 farklı on iki kutu bırakır ve "şeride sığıyor mu"
sorusunu kimse tek başına cevaplayamaz. Enum, kutuyu **sonlu ve
gerekçelendirilebilir** tutar.

### Ölçüler

Sınıfın oranı **referans çiziminin oranıyla eşleşmek zorunda** (§4'teki hattın
temel kuralı bu). Bu yüzden her sınıfta genişliği tasarım kararı olarak
seçtim, boyu referans oranından türedi.

| Sınıf | Birim (G×B) | Oran | Görsel dp | Hitbox dp | Referans |
|---|---|---|---|---|---|
| `MOTOSIKLET` | 22 × 59 | 0.373 | 17.60 × 47.20 | 15.49 × 41.54 | 07_motosiklet (0.375) |
| `BINEK` | **40 × 76** | 0.526 | 32.00 × 60.80 | 28.16 × 53.50 | mevcut — **değişmez** |
| `AGIR` | 48 × 202 | 0.238 | 38.40 × 161.60 | 33.79 × 142.21 | 09_tir (0.237) |

Gerekçeler:

- **`BINEK` çıpadır ve asla değişmez.** Bütün denge, bütün bölüm hedefleri,
  bütün yükseltme eğrileri bu kutunun davranışına göre ayarlandı. Mevcut yedi
  gövde ve trafik aracı bu sınıfta kalır; hiçbir mevcut test satırı değişmez.
- **Motosiklet genişliği 22 birim (aracın %55'i).** Gerçek oran daha da dar
  (0.8 m / 1.8 m = %44 → 17.8 birim) ama 17.8 birim = 14.2 dp, oyun ölçeğinde
  bir çubuk gibi okunuyor ve fabrika boyası hiç görünmüyor. %55, "belirgin
  şekilde dar" ile "hâlâ araç" arasındaki uzlaşma. Boy referans oranından:
  22 / 0.375 ≈ 59.
- **Tır genişliği 48 birim (aracın %120'si).** Gerçek oran %139 (2.5 m / 1.8 m)
  olurdu → 55.6 birim = 44.5 dp, yani en dar şeridin %74'ü. Sığıyor (§3) ama
  görsel olarak şeridi tıkıyor ve oyuncuya "burada geçilecek yer yok" diyor,
  oysa var. %120 bilinçli bir kısaltma: tır hâlâ gözle "diğerlerinden geniş"
  okunuyor, şeritte nefes payı kalıyor. Boy referans oranından: 48 / 0.237 ≈ 202.

### `CarShapeDef` bunu nasıl taşır

**Ayrı bir `VehicleClass` enum'u + `CarShapeDef`'te tek bir alan.** Serbest
float alan değil.

```
enum class VehicleClass(val widthUnits: Float, val lengthUnits: Float)
    MOTOSIKLET(22f, 59f) · BINEK(40f, 76f) · AGIR(48f, 202f)
```

Enum, birimden türetilmiş dp/hitbox değerlerini de kendi üstünde hesaplar
(`GameConfig.CAR_ART_SCALE` ve hitbox payları üzerinden) — `game/` paketi saf
Kotlin olduğu için buna engel yok.

`CarShapeDef` yalnızca `val vehicleClass: VehicleClass = VehicleClass.BINEK`
kazanır. Varsayılan değer olması şart: yedi mevcut gövde ve trafik gövdesi tek
satır bile değişmeden derlenir.

**Nerede yaşamalı:** `game/VehicleClass.kt` (yeni dosya) — `GameConfig` fizik
sabitlerinin evi, `CarCatalog` içerik katalogu; sınıf ikisinin arasında bir
şey ve ikisi de ona bakacak. `GameConfig.CAR_WIDTH_PX` ve kardeşleri
**silinmez**, `VehicleClass.BINEK`'in takma adı hâline gelir; böylece "denge
değerleri tek yerde" kuralı korunur ve mevcut çağrı noktaları kırılmaz.

`CarCatalog.ART_LEFT/RIGHT/TOP/BOTTOM` ise artık dört sabit değil, **sınıfa
göre sorulan dört fonksiyon** olur. Dosya başındaki "kutu büyümez" kuralı şuna
dönüşür:

> **Değişmez kural (yeni hâli): her şekil KENDİ SINIFININ kutusuna sığar ve
> onu doldurur. `BINEK` kutusu hiçbir zaman büyümez.**

---

## 2. Çarpışma sonuçları — oynanış nasıl değişir

### Motosiklet: avantaj gerçek ama sanılandan küçük

Minkowski toplamıyla (§0):

| | Otomobil oyuncu | Motosiklet oyuncu | Fark |
|---|---|---|---|
| Yatay çarpma eşiği (trafiğe karşı) | 28.16 dp | (15.49+28.16)/2 = **21.83** | **−%22.5** |
| Dikey örtüşme eşiği | 53.50 dp | (41.54+53.50)/2 = **47.52** | **−%11.2** |
| "Tehlike dikdörtgeni" alanı | 1.00 | 0.775 × 0.888 = **0.688** | **−%31** |

Ama bu −%31 kâğıt üzerindeki değer. Gerçekte daha küçük, çünkü:

1. **Aynı şeritteyken hiçbir şey değişmez.** Trafik şerit merkezine
   kilitli; oyuncu aynı şeritteyse `dx = 0` ve motosiklet de çarpar.
2. **Şerit değiştirme 0.06 saniye sürüyor** (`LANE_LERP_RATE = 16`). Dar
   kutunun para ettiği tek an "şeritler arasındayken bir araç geçiyor" anı ve
   o an oyunun küçük bir yüzdesi.

Yani motosikletin gerçek kazancı "kenardan sıyırma" senaryosuna sıkışmış
durumda. **Ne kadar olduğunu tahmin etmeyi reddediyorum** — ölçülmeli (§5,
başsız simülasyon testi). Bu belgede rakam uydurmak, dengeyi rakama göre
ayarlayıp sonra "dengeli" demek olurdu.

### Motosikleti neyle telafi ederiz — seçenekler, tercih sırasıyla

1. **Mevcut çarpan bandı (0.80–1.25).** Motosiklete katalogun en kötü freni
   (0.80) ve en kötü boost'u (0.88) verilir, son hızı referansın altında
   (0.96) kalır, karşılığında en yüksek ivme (1.18). *Avantajı:* yeni kavram
   yok, garaj çubukları çalışıyor, `hicbir carpan asiri degil` testi zaten
   var. *Riski:* bandın toplam ifade gücü ±%25; dar kutunun değeri bunun
   üstüne çıkarsa yetmez.
2. **Şerit içi kararsızlık.** Motosiklet şerit merkezine oturmaz; hedefin
   biraz ötesine sarkar ve geri toplanır (kontrollü aşım). *Avantajı:*
   avantajı tam olarak avantajın geçerli olduğu yerde (geçiş anı) geri alır —
   tasarım olarak en dürüst telafi budur. *Riski:* yeni mekanik, his ayarı
   ister, kontrolleri "bozuk" hissettirebilir. **Tasarım olarak tercihim bu,
   ama 1. seçenek ölçülüp yetersiz çıkmadan eklenmemeli.**
3. **Sınıf başına ekonomi çarpanı** (coin/skor < 1.0). *Avantajı:* istenen
   büyüklükte ayarlanabilir, bant sınırı yok. *Riski:* oyuncu bunu "cezalı
   araç" diye okur; ayrıca "hangi araç daha çok kazandırır" sorusu bütün
   garajı yeniden dengelemeyi gerektirir.
4. **Yalnızca fiyat/seviye kapısı.** **Yetmez** ve tek başına kullanılmamalı:
   tek seferlik bir bedel, kalıcı bir avantaj satın alır. Katalogdaki "hiçbir
   ücretli araç diğerini dört eksende birden geçmez" kuralının ruhuna aykırı.

### Kutu artık beşinci bir denge ekseni

`CarCatalogTest`'teki "dominasyon" testi bugün dört çarpana bakıyor. Sınıflar
geldikten sonra bu test **yanlış negatif** üretir: dört eksende eşit ama
kutusu küçük bir araç, diğerini fiilen her yönden geçer. Dominasyon testi
"çarpışma alanı" eksenini de saymalı — küçük kutu bir **güç** olarak sayılır
(§5).

### Perfect dodge penceresi bozulur — düzeltilmeli

Bugün: `perfectDodgeMaxDx = CAR_WIDTH_PX + (lane − CAR_WIDTH_PX)·0.5`.
360 dp'de = 32 + 35.2·0.5 = **49.6 dp**. Çarpma 28.16'da olduğu için pencere
`(28.16 , 49.6]`, yani **21.44 dp genişliğinde**.

Formül dokunulmadan bırakılırsa motosikletin penceresi `(21.83 , 49.6]` =
**27.77 dp** olur: **%30 daha geniş**. Yani dar araç hem daha az çarpar hem
daha çok combo yapar — bileşik ve istenmeyen bir avantaj.

Düzeltme, mevcut davranışı birebir koruyacak şekilde:

```
perfectDodgeMaxDx(laneWidth, ciftYariGenislik)
    = ciftYariGenislik + (laneWidth − CAR_WIDTH_PX) · PERFECT_DODGE_WINDOW_RATIO
```

`ciftYariGenislik = (görselA + görselB) / 2`. Otomobil–otomobil çiftinde
32 + 17.6 = 49.6 — **bugünkü değerin aynısı, sabit hiç değişmiyor.**
Motosiklette 24.8 + 17.6 = 42.4; çarpma 21.83 → pencere **20.57 dp**,
otomobilin 21.44'üne neredeyse eşit. Anlamı da doğru oluyor: *perfect dodge,
iki aracın görünen kenarları arasındaki sabit bir açıklıktır.*

### Tır: oyuncu aracı mı, sadece trafik mi?

**Öneri: yalnızca TRAFİK. Oyuncu aracı olmamalı.** Üç gerekçe, ağırlık
sırasıyla:

1. **Maruz kalma süresi oyunu kilitler.** Tırın hitbox boyu 142.21 dp. Trafik
   aracına karşı dikey örtüşme eşiği (142.21 + 53.50)/2 = **97.86 dp**. Taban
   hızda yaklaşma hızı ≈ (2.63 − 0.5·2.63) · 187.5 = 246 dp/s → örtüşme
   **0.80 saniye** sürer. Engel doğma aralığı **0.78 saniye**. Yani tırla
   oynayan oyuncu, pratikte **her an** en az bir araçla dikey olarak
   örtüşür ve şerit değiştirmek için temiz bir pencere hiç açılmaz. Bu bir
   denge sorunu değil, oynanabilirlik duvarıdır.
2. **Garaj vitrini taşımıyor.** Seçili araç kartı 96×118 dp. 1:4.2 oranında
   bir araç oraya 28 dp genişliğinde girer; yanında 32 dp'lik otomobil daha
   büyük görünür. "3000 coin verdim, kartta küçüldü" hissi.
3. **Sanat yönü.** Trafik ile oyuncu 60 Hz'de siluetle ayrılıyor
   (PROVENANCE #11). Tır hem oyuncuda hem trafikte olursa bu ayrım en belirgin
   silüette çöker.

Buna karşılık **trafikte tır çok değerli**: bugünkü engellerin hepsi
*tepkiyle* geçiliyor (0.22 s örtüşme). 0.80 s örtüşen bir gövde, oyuncuyu ilk
kez **planlamaya** zorlar — "şu şeride geçmeliyim, çünkü on saniye orada
kalacağım". Zorluk eğrisine yeni bir boyut ekler ve mevcut `ownSpeed`
modeline zaten oturuyor: yavaş tır = uzun sollama.

**Şart:** tır geldiği anda "çözülemez duvar" riski doğar (üç şeridi aynı y
bandında kapatan kombinasyon). Doğum kuralına bir güvence ve ona bir test
gerekiyor (§5).

---

## 3. Şerit uyumu — sayılarla

En kötü durum: 320 dp ekran, **şerit 59.73 dp**, şerit merkezleri arası
59.73 dp.

| Sınıf | Görsel dp | Şeridin %'si | Kenar boşluğu | Oyuncuya çarpma eşiği | Komşu şerit merkezi | Güvenli mi |
|---|---|---|---|---|---|---|
| `MOTOSIKLET` | 17.60 | %29.5 | 21.07 dp | 21.83 | 59.73 | ✔ (pay 37.9) |
| `BINEK` | 32.00 | %53.6 | 13.87 dp | 28.16 | 59.73 | ✔ (pay 31.6) |
| `AGIR` | 38.40 | %64.3 | **10.67 dp** | 30.98 | 59.73 | ✔ (pay 28.8) |

İki `AGIR` yan yana bile: (33.79+33.79)/2 = 33.79 < 59.73. Sığıyor.

**Cevap: tır şeride sığıyor — hem de rahatça.** Bu, belgedeki en önemli
düzeltmelerden biri: *tırın yalnızca trafik olması gerektiği sonucu şerit
genişliğinden gelmiyor.* Şerit sorun değil; sorun §2.1'deki maruz kalma
süresi ve §2.2-2.3'teki vitrin/siluet. Tırı şerit yüzünden reddedersek yanlış
gerekçeyle doğru sonuca varmış oluruz ve gelecekte biri "artık ekranlar geniş"
deyip kararı yanlış yerden bozar.

Genişliğin gerçek üst sınırı şudur ve teste yazılmalı:

```
(sinifHitboxGenisligi + BINEK.hitboxGenisligi) / 2  <  enDarSeritGenisligi
```

= sınıf hitbox'ı < 2·59.73 − 28.16 = **91.3 dp** (yani 114 birim). 48 birimlik
tır bu sınırın çok altında; darboğaz genişlik değil.

---

## 4. Sprite hattına etkisi

### Gizli sabit: 6 px / birim

Bugünkü tuval 240×456 ve kutu 40×76 → **240/40 = 6, 456/76 = 6**. Yani hattın
zaten örtük bir "birim başına 6 piksel" sabiti var, sadece yazılı değil.
Sınıflara geçerken bunu **açık bir sabit yapmak** her sınıfa aynı doku
yoğunluğunu verir ve tuval hesabını tek satıra indirir.

| Sınıf | Birim | Tuval (px) | Bugünkünün kaç katı alan |
|---|---|---|---|
| `MOTOSIKLET` | 22 × 59 | **132 × 354** | 0.43× |
| `BINEK` | 40 × 76 | **240 × 456** (değişmez) | 1.00× |
| `AGIR` | 48 × 202 | **288 × 1212** | 3.19× |

`AGIR` çözülmüş hâlde 288·1212·4 B ≈ **1.4 MB / katman**, iki katmanla
2.8 MB. PROVENANCE #16'daki olay (her `CarPreview` sprite'ları baştan
çözünce PSS 165 → 302 MB) tırla daha sert vurur; **tek kopya kuralı**
(`rememberCarSprites`) tır eklenirken ihlal edilmediği ayrıca doğrulanmalı.

APK maliyeti kaba tahmin: mevcut 8 gövde 552 KB. Tır çifti ~150–200 KB,
motosiklet çifti ~35 KB, diğer `BINEK` adayları (taksi, beyaz sedan, F1)
~70 KB/çift. Altısı birden ≈ **+450–500 KB**. *(Tahmin — üretilmeden
doğrulanamaz.)*

### Sessiz mektuplama biter: hat artık DOĞRULAR

Değişmesi gereken tek satır aslında `scale = min(...)`. Yerine geçecek
sözleşme:

1. `MAPPING` artık sınıfı da taşır: `"tir_traffic": ("09_tir", AGIR)`.
2. Referans bbox oranı ile sınıf oranı karşılaştırılır. Fark **%2'den
   büyükse betik HATA verir** — sessizce ortalayıp geçmez. Mesaj eyleme
   dönük olmalı: *"09_tir oranı 0.237, AGIR sınıfı 0.238; fark %0.4 — tamam"*
   / *"11_station_wagon oranı 0.434, BINEK 0.526; fark %17 — ya çizim
   düzeltilecek ya yeni sınıf gerekiyor."*
3. Eşleşiyorsa sprite tuvali **iki eksende birden doldurur**; şeffaf kenar
   payı kalmaz, hitbox ile görsel örtüşür.
4. Betik bir **manifest** yazar (`car_sprites.json`): gövde id → sınıf,
   içerik piksel ölçüsü, hesaplanan görsel dp, hitbox dp, pay. Bu manifest
   §5'teki testin girdisidir. Manifest olmadan JVM testi sprite'ları
   göremez ve §0'daki kayma bir daha sessizce olur.
5. Uzun gövdeler için **çift eşikli bbox kontrolü**: alfa > 8 ve alfa > 128
   ile ölçüp fark %3'ü aşarsa uyarı. Referansta pişmiş gölge/hale varsa
   hitbox aracın görünen arkasından taşar ve bu, "havaya çarptım"ın en
   görünür biçimidir (tırın arkasına çarpmak, oyuncunun ekranın ortasında
   izlediği bir olay).

### Bugünkü referansların sınıflara dağılımı

| Referans | Oran | Sınıf | Not |
|---|---|---|---|
| 06_taksi | 0.493 | `BINEK` | **Trafik olarak ideal**: obstacle paletinde zaten sarı (`FFD60A`) var, taksi o rengin gerekçesi oluyor. |
| 05_beyaz_sedan | 0.490 | `BINEK` | Trafik çeşitliliği. Oyuncuya verilirse `COLOR_GLACIER` çakışması gözden geçirilmeli. |
| 10_f1 | 0.473 | `BINEK` | §1'deki not; ayrı sınıf gerekmiyor. |
| 11_station_wagon | 0.434 | **hiçbiri** | `BINEK`'e sokulursa görsel 26.40 dp < hitbox 28.16 dp → kenar başına **−0.88 dp havaya çarpma**. Ya kendi sınıfı (34 × 78) ya çizim düzeltmesi. Olduğu gibi alınmamalı. |
| 07_motosiklet | 0.375 | `MOTOSIKLET` | — |
| 09_tir | 0.237 | `AGIR` | Referans tuvali zaten 887×1774 (2:1); tek dikey referans. |

### F1 neden yeni sınıf istemiyor — ölçüm sahibin beklentisiyle çelişiyor

Sahibin notu "F1 daha geniş". **Referans çizim bunu söylemiyor:** 10_f1 oranı
**0.473**, Şehir'in oranı 0.518. Yani elimizdeki F1 sanatı otomobilden
*daha dar* — ne kadar kanat çizilirse çizilsin ölçü bu.

Üç yol var:

- **(A) `BINEK`'te bırak.** Sprite 216×456'ya oturur, görsel 28.80 dp,
  hitbox'a pay **+0.32 dp** — bugün yayında olan Kas Arabası'ndan (+0.25) daha
  iyi. Sıfır risk, yeni sınıf yok, hiçbir test değişmiyor. **Önerim bu.**
- **(B) `FORMULA` sınıfı, 40 × 85 birim** (oran 0.471 ≈ referans). Genişlik
  aynı kalır, **boy uzar**: görsel 32 × 68 dp, hitbox 28.16 × 59.84.
  Trafiğe karşı dikey örtüşme 53.50 → 56.67 (**+%5.9**). Yani F1 kimliği bir
  **bedelle** gelir; "en hızlı ama en uzun kutu" hikâyesi katalogun "her
  aracın bir bedeli var" kuralına tam oturur. Sahibi "daha büyük olsun"da
  ısrar ederse doğru yol budur.
- **(C) Kutuyu genişlet (42+ birim).** **Reddediyorum.** Oyuncunun kutusunu
  yanlamasına büyütmek yalnızca zarar verir: hem çarpışma koridoru büyür hem
  de "kutu büyümez" kuralı, ki o kural 2026-08-13 adalet düzeltmesinin
  bekçisi. Ayrıca sanat zaten dar; geniş kutu doğrudan havaya çarpma üretir.

**F1 denge taslağı** (5000 coin, sahibi kararı): hız 1.18 · ivme 1.15 ·
fren **0.85** · boost **0.90**, seviye şartı 8. Süper Araba'yı (1.12 / 1.10 /
0.94 / 1.00) iki eksende geçer, ikisinde geride kalır → dominasyon testi
geçer. V10 sesi `game-audio` işidir, bu belgenin kapsamı dışında.

---

## 5. Test stratejisi

### Değişen test

`her sekil carpisma kutusuna sigar` → **`her sekil KENDI SINIFININ kutusuna
sigar`**. Gövdeye `vehicleClass` sorulur, sınırlar oradan alınır. Gövdelerin
hiçbiri sınıf değiştirmediği sürece test aynı şeyi doğrular ve yeşil kalır.

Aynı şekilde `yeni govdeler kutuyu birebir doldurur`,
`her sekil kutuyu makul olcude doldurur` ve
`ilk sekil cizim kutusunu tam olarak doldurur` sınıf sınırlarını kullanır;
sonuncusu `BINEK` çıpası olduğu için sayıları (−20/20/−2/74) **elle yazılı
kalmalı** — türetirsek çıpa olmaktan çıkar.

`kutu sinirlari GameConfig ile birebir ortusur` testi
`VehicleClass.BINEK ≡ GameConfig.CAR_*` eşitliğine dönüşür; birinin diğerinden
kayması hâlâ yakalanır.

### Yeni testler — önem sırasıyla

1. **`sprite hicbir govdede hitbox'tan dar degil`** *(en değerli test)*.
   Girdisi §4'teki manifest. Her gövde için
   `gorselGenislikDp ≥ hitboxGenislikDp` ve
   `gorselBoyDp ≥ hitboxBoyDp`. **Bu test bugün yazılsa Boğa 67'de kırmızı
   yanar** (§0) — yani sınıflardan bağımsız olarak şimdi gerekiyor.
2. **`her sinif en dar seride sigar`**: §3'teki eşitsizlik, `LANE_COUNT`,
   `ROAD_WIDTH_RATIO` ve desteklenen en dar ekran genişliğinden türetilir.
   Birinin `ROAD_MAX_WIDTH_PX`'i düşürmesi de böylece yakalanır.
3. **`AGIR sinif oyuncu araci degil`**: `CarCatalog.shapes` içinde
   `vehicleClass == AGIR` olan gövde bulunmamalı. §2'deki kararı veriden
   dondurur; sonraki bir oturumda "tırı da satalım" diyen için gerekçeye
   giden bir hata mesajı bırakır.
4. **`maruz kalma butcesi`**: her trafik sınıfı için
   `(sinifHitboxBoyu + BINEK.hitboxBoyu) / 2 / yaklasmaHizi < OBSTACLE_SPAWN_INTERVAL_SEC`.
   `AGIR` bu testi **0.80 s vs 0.78 s ile kıl payı geçemez** — bu bilinçli:
   tır trafiğe girecekse ya boyu kısalacak ya kendi doğum aralığı olacak ya
   da testin trafik sınıfına özel bir bütçesi olacak. Sayı, kararı yüzümüze
   söylemeli.
5. **`perfect dodge penceresi sinifa gore genislemez`**: otomobil çifti için
   eşik tam olarak bugünkü 49.6 dp (dondurulmuş sabit); her sınıf çifti için
   pencere genişliği (`maxDx − carpmaEsigi`) otomobil çiftininkinden ±%10
   içinde.
6. **`kucuk kutu bir GUC sayilir`**: dominasyon testine beşinci eksen olarak
   çarpışma alanı eklenir; hiçbir ücretli araç bir diğerini beş eksende
   birden geçemez (§2).
7. **`cozulebilirlik / gecilebilir serit`**: tohumlanmış RNG ile N=10 000
   doğum senaryosu üretilir; her y bandında en az bir şeridin
   `AGIR`+`BINEK` kombinasyonundan temiz kaldığı doğrulanır. `game/` saf
   Kotlin olduğu için bu testi JVM'de yazmak mümkün — cihaz gerektirmez.
8. **`kayitli kosu ozdesligi` (yeniden düzenleme kalkanı)**: sabit tohumlu,
   betimlenmiş girdili tam bir koşu simüle edilir ve nihai istatistikler
   (skor, mesafe, çarpma sayısı, combo) **bit bazında** beklenen değerlerle
   karşılaştırılır. §6'daki 1–4. adımlar davranışı değiştirmemeli; bu test o
   iddianın kanıtıdır ve ajan raporundan bağımsız bir delildir.
9. **`motosiklet carpma orani`** *(denge testi, ölçüm aracı)*: aynı senaryo,
   basit betimlenmiş bir "oyuncu" (önündeki şerit doluysa boş şeride geç) ile
   iki gövdeyle koşturulur; 1000 m başına çarpma sayısı raporlanır. İlk
   sürümde **eşik koymadan yalnızca rapor**, çünkü kabul edilebilir farkın ne
   olduğunu bilmiyoruz. Sayı elimize geldikten sonra eşik konur.

---

## 6. Aşamalı uygulama planı

Her adım kendi başına derlenir, testleri geçer ve **tek başına yayınlanabilir**.
Doğrulama her adımda aynı: `:app:testDebugUnitTest`, `:app:assembleDebug`,
`:app:assembleRelease` (+ cihazda duman testi, S8 bağlı).

| # | İş | Davranış değişir mi | Geri alınabilir mi | DataStore göçü |
|---|---|---|---|---|
| 0 | Sprite manifesti + "sprite ≥ hitbox" testi. Boğa 67 / Dağ Keçisi düzeltmesi. | Evet (0.2 dp, adalet yönünde) | Evet — betik + test geri alınır | Hayır |
| 1 | `VehicleClass` enum'u, yalnızca `BINEK`. Her şey ondan türer. | **Hayır** | Evet, saf refactor | Hayır |
| 2 | `Obstacle` sınıfını taşır; `updateObstacles` çift kutulu; doğum/temizlik payları sınıftan türer. | **Hayır** (tek sınıf var) | Evet | Hayır |
| 3 | `HITBOX_SCALE` → mutlak paylar (aşağıda). | **Hayır** (otomobil değerleri korunur) | Evet | Hayır |
| 4 | `perfectDodgeMaxDx` çift yarı-genişlik alır. | **Hayır** (otomobil çiftinde aynı sayı) | Evet | Hayır |
| 5 | Hat sınıf farkındalığı + `MOTOSIKLET`/`AGIR` tuvalleri; tır ve motosiklet sprite'ları üretilir, oyuna **girmez**. | Hayır | Evet | Hayır |
| 6 | `AGIR` trafiğe girer, doğum ağırlığı **0** (kapalı doğar) + çözülebilirlik testi. | Hayır (ağırlık 0) | **Evet, tek sayı** | Hayır |
| 7 | Tır ağırlığı bölüm bazında açılır (`LevelDef`). | Evet | Evet, bölüm verisi | Hayır |
| 8 | `MOTOSIKLET` oyuncu gövdesi; çarpanlar §5.9 ölçümüyle ayarlanır. | Evet | Evet (katalogdan çıkar) | **Hayır**, ama aşağıya bak |
| 9 | F1 (`BINEK`), 5000 coin, sv. 8; V10 sesi ayrı iş. | Evet | Evet | **Hayır**, ama aşağıya bak |

### 3. adımın detayı — `HITBOX_SCALE` neden mutlak paya dönmeli

`HITBOX_SCALE = 0.88` **oransal**. 202 birimlik tıra uygulanınca hitbox
boyundan 202·0.12 = 24 birim = **19.4 dp** kırpılır, yani tırın burnunda ve
arkasında ~9.7 dp'lik **görünür ama çarpmayan** bir bölge oluşur. Bu, "havaya
çarptım"ın tersi: *"bariz çarptım, hiçbir şey olmadı."* Otomobilde aynı oran
uç başına yalnızca 3.65 dp; sorun oran büyüdükçe değil, **gövde uzadıkça**
çıkıyor.

Çözüm, otomobil değerlerini birebir koruyarak: pay **mutlak** olur —
yanal 1.92 dp/kenar, boyuna 3.65 dp/uç. `BINEK`'te sonuç bugünküyle aynı
(28.16 × 53.50); `AGIR`'da hitbox 34.56 × 154.30 olur ve uçlardaki ölü bölge
kaybolur. §5.8'deki özdeşlik testi bu adımın davranışı değiştirmediğini
kanıtlar.

### Doğum/temizlik payları da sınıftan gelmeli (2. adım)

`OBSTACLE_SPAWN_Y_PX = −150` bir otomobil için doğru: araç ekranın üstünde,
görünmeden doğuyor. **161.6 dp uzunluğundaki tır −150'de doğarsa arkası
+11.6'da, yani ekranın İÇİNDE belirir.** Aynı şey `OBSTACLE_DESPAWN_TOP_MARGIN_PX
= 160` için de geçerli: tır o eşiği geçmeden temizlenmez. İkisi de
`−(sinifBoyu + pay)` olarak türetilmeli.

### Kayıt (DataStore) göçü

**Hiçbir adım göç gerektirmiyor.** Gerekçeler:

- Gövde/boya kimlikleri **dize** ve `CarCatalog.shape(id)` bilinmeyeni
  varsayılana düşürüyor. Yeni kimlik eklemek eski kaydı bozmaz.
- `vehicleClass` **varsayılan değerli** bir alan; kayıtta hiç durmuyor,
  katalogdan geliyor.
- `car_color_by_shape` yeni gövde için anahtar bulamazsa fabrika boyasına
  düşüyor (PROVENANCE #17) — yeni gövde ilk seçildiğinde doğru renkte gelir.
- Trafik/tır hiçbir şey kaydetmiyor.

**Göç olmayan ama sessiz olan tek risk:** 8. adımdan sonra motosikletin
çarpanları ayarlanırsa, o aracı almış oyuncunun aracı **haber verilmeden
değişir**. Bu bir veri göçü değil "denge göçü"; sürüm notunda yazılmalı ve
mümkünse coin iadesi değil, yalnızca şeffaflıkla çözülmeli.

**Gerçek göç yalnızca şunlarda gerekir** (hiçbiri planda yok): varsayılan
gövdenin değişmesi, mevcut bir kimliğin silinmesi/yeniden adlandırılması,
`colors` listesinden ücretli bir boyanın çıkarılması.

---

## 7. Riskler — "havaya çarptım" nereden geri gelir

2026-08-13'te düzeltilen hata şuydu: çarpışma kutusu görünür araçtan büyüktü,
oyuncu gözle boşluk görürken kaza oluyordu. Bu çalışmada o hatanın **altı
ayrı** dönüş yolu var. En tehlikelisi ilki, çünkü zaten olmuş.

1. **Sprite mektuplaması — ZATEN OLDU.** §0'daki tablo: Boğa 67'nin sprite'ı
   hitbox'tan 0.21 dp/kenar dar. Sınıflar bunu **hızlandırır**, çünkü her yeni
   sınıf yeni bir oran uyuşmazlığı fırsatıdır. **Azaltma:** §4'teki
   "eşleşmezse hata ver" kuralı + §5.1 testi. *Bu ikisi olmadan sınıf işine
   başlanmamalı.*
2. **Kutu birimden, sanat bbox'tan türeyip birbirinden ayrı sürüklenmesi.**
   Bugün iki dünya var: vektör parçalar (test ediliyor) ve sprite pikselleri
   (test edilmiyor). Sınıflar ikinciyi büyütür. **Azaltma:** manifest tek
   köprü olsun; sprite'ı olan gövdenin vektör parçaları da aynı sınıf
   kutusunu doldurmak zorunda kalsın.
3. **Uzun gövdenin arka ucu.** Tırın hitbox'ı görünen arkasından taşarsa
   oyuncu bunu **ekranın ortasında, saniyelerce izleyerek** görür — otomobilde
   0.2 saniyede olup biten şey tırda bir sahne olur. Referansta pişmiş gölge
   veya egzoz dumanı bbox'ı şişirir. **Azaltma:** §4.5 çift eşikli bbox
   kontrolü; ayrıca tırın arka yüzü için cihazda ekran görüntüsü + piksel
   ölçümü (S8 bağlı, `exec-out screencap`).
4. **Ters yön: "çarptım ama olmadı".** `HITBOX_SCALE` oransal kaldığı sürece
   tırın uçlarında ~9.7 dp ölü bölge. Görünürde farklı bir hata ama aynı
   kökten: kutu ile çizim arasındaki ilişkiyi sabit bir oranla tanımlamak.
   **Azaltma:** 3. adım (mutlak paylar).
5. **Motosiklette çizimden BÜYÜK hitbox.** 07_motosiklet bugünkü hatta
   sokulursa görsel 22.80 dp, `BINEK` hitbox'ı 28.16 dp → kenar başına
   **2.68 dp havaya çarpma**, otomobildekinin ~14 katı ve bu **görülür**.
   Motosiklet, kendi sınıfı olmadan oyuna kesinlikle girmemeli.
   Aynısı station wagon için −0.88 dp ile geçerli.
6. **Şerit merkezleri ile geniş gövde.** `setViewport` ekran değişiminde
   engelleri `laneCenter`'a yeniden oturtuyor. Tır ekran döndürme/çoklu
   pencere sırasında şerit ortasına ışınlanırsa oyuncunun üstüne binebilir.
   Bugün de teorik olarak var ama 32 dp'lik araçta fark 0; 38.4 dp'de
   büyür. **Azaltma:** viewport değişiminde oyuncuya çok yakın engellerin
   temizlenmesi (revive'daki mantık zaten var).

**Ek risk, adalet dışı:** çözülemez doğum (üç şeridin aynı anda kapanması).
Tır bunu ilk kez gerçekten mümkün kılar çünkü bir şeridi 0.8 saniye boyunca
tutar. §5.7 testi ve 6. adımdaki "kapalı doğ" yaklaşımı bunun içindir.

---

## Özet — karar için üç satır

1. **Üç sınıf**: `MOTOSIKLET` 22×59, `BINEK` 40×76 (dokunulmaz), `AGIR`
   48×202. Enum, `CarShapeDef`'te varsayılan değerli tek alan.
2. **Tır yalnızca trafik** — şeride sığdığı için değil, 0.80 saniyelik maruz
   kalma süresi oynanışı kilitlediği için. **F1 `BINEK`'te kalır**; referans
   çizim zaten otomobilden dar, geniş kutu yalnızca zarar verir.
3. **Önce güvenlik ağı**: sprite manifesti + "sprite ≥ hitbox" testi. Bu test
   bugün yazılsa Boğa 67'de kırmızı yanıyor — yani sınıf işi başlamadan da
   gerekiyor.

## Bu belgede emin olmadığım yerler

- Referans bbox'ları alfa > 8 ile ölçüldü; pişmiş gölge/hale varsa §0
  tablosundaki paylar iyimser.
- Motosikletin gerçek avantajı ölçülmedi; −%31 kâğıt üzerindeki tavan,
  gerçeği daha küçük (§2). Çarpan önerileri ölçümden sonra ayarlanmalı.
- Tırın 0.80 s maruziyeti "yeni bir zorluk boyutu" mu yoksa "sıkıcı bir
  duvar" mı — bu cihazda oynanmadan bilinemez.
- APK boyutu tahminleri (§4) üretilmiş dosyalarla doğrulanmadı.
- Bu oturumda **cihazda hiçbir şey denenmedi** (talimat gereği).
