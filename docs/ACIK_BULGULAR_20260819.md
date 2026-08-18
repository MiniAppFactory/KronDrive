# Açık bulgular — otonom oturum, 2026-08-19

Bu belge, otonom oturumda **bulunan ama kapatılmayan** maddeleri tutar.
Kapatılanlar `docs/CHANGELOG.md`'de (2026-08-19 (3) başlığı).

Her madde: ne bulundu, nasıl ölçüldü, neden kapatılmadı, ne yapılmalı.

---

## 1. Tır dengelenemiyor — dört eksende de kötü, kütle mekaniği yok

**Bulgu.** Tır 3.600 coin ve 1.800 coinlik Kas Arabası'ndan hız, ivme ve
frenin **üçünde de** geride. Üstelik tek `VehicleClass.AGIR` üyesi olduğu için
çarpışma kutusu 3,2 kat büyük (48×202'ye karşı 40×76) — yani pahalı, yavaş ve
oynaması zor. İki ajan bunu bağımsız buldu, kendi hesabımla da doğruladım:
katalog **67–77 coin/km-h** bandındayken tır **200**.

**Neden kapatılmadı.** Denedim ve çıkmaza girdim:

- Fiyatı 1.500'e indirince Kas Arabası tırı **dört eksende birden** ezdi
  (mevcut `hicbir ucretli arac digerini dort eksende birden gecmiyor` testi
  kırmızı yandı).
- Boost'unu yükseltmek çözerdi ama sahibinin 2026-08-17 kararına aykırı:
  *"tırın boostu çok yüksek, tır boostlanamaz ki"*.
- Hızını yükseltmek çözerdi ama tırın Kas Arabası'ndan hızlı olması gerekirdi.

**Kök sebep.** Oyunda **kütle mekaniği yok**. Tırın tek ayırt edici özelliği
(büyüklük) sadece dezavantaj olarak var. Dört sürüş ekseninde de kötü olan bir
araç, tanımı gereği bu eksenlerle dengelenemez.

**Sonradan gelen ölçüm (2026-08-19, bağımsız).** Tez doğrulandı ve
sayısallaştırıldı. BINEK trafiğe karşı Minkowski tehlike alanı:

| Sınıf | Kutu (dp) | Tehlike alanı | Orana |
|---|---|---|---|
| MOTOSİKLET | 15,49 × 41,54 | 4.148 dp² | **0,69×** |
| BINEK | 28,16 × 53,50 | 6.027 dp² | 1,00× |
| AĞIR (tır) | 33,79 × 142,21 | 12.124 dp² | **2,01×** |

Yani **ölçü sınıfı 2,9× yayılım üretiyor** — kataloğun en geniş hız
yayılımından (1,00 → 2,08) bile geniş, ve fiyatta karşılığı **sıfır**.

İki ek bulgu:
- **Asıl dezavantaj enine değil boyuna.** Tırın eni +%20, boyu **+%166**.
  Otopilot şerit penceresini binek boyuna göre hesaplarken tır 40 koşuda 40
  kaza yapıyor; gerçek kutu boyuna göre hesaplayınca 320 saniye hiç
  çarpmıyor. Yani tır **imkânsız değil, işaretsiz** — oyun oyuncuya
  dorsesinin arkasında olduğunu hiçbir yerde söylemiyor.
- **Eşit hayatta kalmada tır 29.806 skor** üretiyor; 1.500 coinlik Kuş SLX
  29.544. Yani 3.600 coine 1.500 coinlik performans satılıyor.

**Kendi hatamın düzeltmesi.** Yukarıda "fiyatı 1.500'e indirince dominasyon
testi kırıldı" yazmıştım — **yanlış teşhis**. O test `priceCoins`'e hiç
bakmıyor; kırılmasının sebebi aynı anda boost'u 1,20'den 0,94'e geri
almamdı. Fiyat değişikliği yalnızca iki liste-sırası testini kırar, ikisi de
tek satırlık.

**Ne yapılmalı (sahibinin kararı).** Üç yol:
1. **Mekanik ver.** Tır çarpışmada yavaşlamasın / trafiği itsin. Kimliğini
   gerçek yapar, ama yeni oynanış kodu demek.
2. **Ucuzlat + dominasyon testine belgeli istisna.** "Tır bilinçli handikap"
   diye kabul et; en dürüst hâli ama testin koruduğu kural delinir.
3. **Katalogdan çıkar.** Sprite ve sınıf kodu kalsın, satılmasın.

⚠ Ayrıca: **HEAD'de tırın boostu 1.20** (bugün baskınlığı çözmek için ben
yükseltmiştim) ve bu sahibinin kararıyla çelişiyor. Hangi yol seçilirse
seçilsin bu değer gözden geçirilmeli.

---

## 2. Araç seviyesi şartı ölü bir kapı

**Ölçüm (kendi ölçümüm, kariyeri sırayla oynatarak).** On aracın onunda da
seviye şartı, coin şartından **çok önce** doluyor:

| Araç | Gereken sv | Seviye yeter (bölüm) | Coin yeter (bölüm) | Gerçek kapı |
|---|---|---|---|---|
| Şehir | 1 | 1 | 5 | coin |
| Yarış Sedan | 2 | 4 | 12 | coin |
| Kas Arabası | 4 | 8 | 20 | coin |
| Motosiklet | 5 | 10 | 29 | coin |
| Süper/Tır/Formula | 6–8 | 12–14 | hiç yetmiyor | coin |

Kariyer sonunda oyuncu **seviye 23**, en yüksek şart 8.

**Sonucu.** `requiredCarLevel` hiçbir şeyi kapatmıyor. Dolayısıyla bugün
eklenen **seviye atlama bedeli hiç kullanılmayacak** — kimse seviyeden kilitli
kalmıyor. Sahibinin "hem coin hem seviye istiyoruz" isteği pratikte
karşılanmıyor.

**Ne yapılmalı.** Seviye gerçekten kapı olacaksa `XP_PER_CAR_LEVEL` 500'den
~2.200'e çıkmalı (ekonomi ajanının önerisi, ben doğrulamadım). Olmayacaksa
`requiredCarLevel` ve atlama bedeli kaldırılmalı — ikisi de ölü kod.

---

## 3. Bölüm 10, 20 ve 30 mekanik olarak birebir aynı

**Ölçüm (kendim, antrenman modu kapalı).** Oyuncunun önündeki şeritlerin
doluluk dağılımı:

| bölüm | 0 dolu | 1 dolu | 2 dolu | 3 dolu |
|---|---|---|---|---|
| 1 | %47,0 | %53,0 | %0,0 | %0,00 |
| 5 | %36,0 | %28,0 | %30,5 | %5,49 |
| **10 / 20 / 30** | **%36,8** | **%29,3** | **%28,9** | **%5,00** |

Son yirmi bölümde oynanış **hiç değişmiyor**; zorluk artışının tamamı hedef
rakamlarından geliyor. Hedef ajanı da bağımsız olarak aynı sonuca vardı
(L11/13, L14/16, L17/19, L20/22/23, L25/26/28/29 satır satır aynı sayılar).

**Neden kapatılmadı.** Bu bir ayar değil, **bölüm tasarımı** işi: 30 bölüm için
gerçekten farklı trafik desenleri gerekiyor. Tek sabit değiştirerek çözülmez.

**Öneri (ölçülmedi).** Araçlar tek tek değil **desen** hâlinde doğsun — iki
şeridi aynı anda kapatan doğuş olasılığı bölüm ilerledikçe 0'dan ~0,35'e
rampalansın. Doğuş aralığını kısaltmak bunu çözmez, sadece ekranı
kalabalıklaştırır.

---

## 4. Günlük görevin `combo` şablonu temkinli oyuncuya kapalı

**Ölçüm.** 180 saniyelik günlük koşuda, beş tohumun beşinde `bestCombo = 0`.
Şablonun **ilk kademesi** bile `ComboAtLeast(2)`, yani 6 saniye içinde iki
mükemmel dodge zinciri.

**Emin olmadığım yer.** Otopilot yakın geçiş kovalamıyor; insan oyuncu
kovalayabilir. Yani bu sayı otopilot verisidir, oyuncu verisi değil.

**Ama güçlü bir emsal var.** `dodge` şablonu 2026-08-17'de tam bu gerekçeyle
kaldırılmıştı (*"temkinli oyun otuz bölümün hiçbirinde tek bir dodge
yapmıyor"*). `combo`, `dodge`'dan **kesinlikle daha zor** ve gözden kaçmış.
Kariyer bunu kural olarak engelliyor (combo yalnızca 3. yıldız olabilir);
günlükte ise 1. kademede duruyor — yani o gün oyuncu 500 coin'in **sıfırını**
alıyor.

**Ne yapılmalı.** `dodge` ile aynı muamele: şablonu kaldır ya da combo'yu
yalnızca üst kademeye taşı. Kod değişikliği küçük, karar sahibinin.

---

## 5. `spendCoins(-100)` coin **ekler**

`GameStateRepository.spendCoins` (satır ~265) `amount <= 0` kontrolü yapmıyor,
oysa kardeşi `addCoins` yapıyor. Bugün **hiçbir çağıranı yok**, yani şu an
zararsız — ama ilk kullanan için hazır bir tuzak. Ya guard eklenmeli ya da
fonksiyon silinmeli.

---

## 6. Ölçüm hijyeni — bir daha düşülmemesi gereken tuzaklar

1. **Antrenman modu ölçümü geçersiz kılar.** Çözüldü (`sideLanesOnly`
   parametresi), ama `LevelCurveTest` dışındaki her yeni denge ölçümü de
   bunu açıkça `false` geçmeli.
2. **`checkGoalReached` koşuyu erken bitirir.** Bir hedefin tavanını ölçerken
   hedefler ulaşılamaz yapılmalı, yoksa ölçüm kendi ölçtüğü şeye bağımlı olur.
3. **Tema rastgele ve çizim yükü 4 kat değişir.** Performans A/B'sinden önce
   sabitle — ama commit'e sızdırma (bugün bir kez daha oldu).
4. **Paralel ajanlar aynı ağaçta çalışırken ölçüm kirlenir.** Bu oturumda bir
   ajan üç commit'li denge sabitini çalışma ağacında geri aldı
   (`SCORE_SPEED_CAP_BASE`, `WORLD_SPEED_SCALE`, `OBSTACLE_SPAWN_INTERVAL_SEC`)
   ve benim bir ölçümüm yanlış sabitlerle koştu. Ajanlara ölçüm yaptırılacaksa
   `isolation: worktree` kullanılmalı.

---

## 7. Doğrulanmayan ajan iddiaları (kanıt sayılmadı)

- *"Yol hiç tıkanmıyor, üç şerit birden asla kapanmıyor (%0,00)"* — kendi
  ölçümümde bölüm 5+ için **%5,0**.
- *"Günlük görevde `PassVehicles(90)` artık ulaşılamaz (70 ölçüldü)"* — kendi
  ölçümümde geçiş tavanı **135**.

İkisi de "oyun çok kolay" yönünde doğru sezgiler ama rakamları tutmadı.
Rapor ≠ kanıt.
