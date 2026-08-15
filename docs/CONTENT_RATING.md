# IARC içerik derecelendirme anketi — cevaplar

**Uygulama:** Kron Drive: Retro Racer · `com.miniappfactory.krondrive`
**Hazırlanma tarihi:** 2026-08-14
**Play Console yolu:** *App content → Content rating*

Anket IARC (International Age Rating Coalition) tarafından yürütülür; tek bir anketi
doldurursun, sonuç ESRB / PEGI / USK / ClassInd / GRAC gibi kurulların derecelerine
otomatik çevrilir. **Yanlış beyan uygulamanın kaldırılma sebebidir**, bu yüzden aşağıdaki
cevaplar kod ve oyun içeriğine bakılarak verilmiştir.

---

## 1. Kategori seçimi

| Soru | Cevap |
|---|---|
| Uygulama kategorisi | **Game** (Oyun) |
| Alt tür (soruluyorsa) | Racing / Arcade |

---

## 2. Şiddet

| Soru | Cevap | Gerekçe |
|---|---|---|
| Gerçekçi görünen insan/hayvan karakterlere yönelik şiddet içeriyor mu? | **Hayır** | Oyunda karakter yok; yalnızca yukarıdan görünen soyut araç dikdörtgenleri var |
| Fantastik karakterlere yönelik şiddet? | **Hayır** | — |
| Kan veya kan sıçraması gösteriyor mu? | **Hayır** | Çarpışmada yalnızca renkli parçacık efekti çıkıyor (`GameEngine.addParticles`); yaralanma, enkaz veya kan çizimi yok |
| İnsan/hayvan yaralanması veya ölümü tasvir ediliyor mu? | **Hayır** | Çarpışma yalnızca koşuyu bitirir (`GameEvent.Crash`), görsel olarak araç yok olmaz, kimse zarar görmez |
| Cinsel şiddet, işkence, uzuv kopması vb. | **Hayır** | — |
| Silah kullanımı var mı? | **Hayır** | Silah, ateş etme, savaş mekaniği yok |
| Diğer araçlara **kasıtlı** çarparak puan kazanma var mı? | **Hayır** | Tam tersi: çarpışma koşuyu bitirir, "Perfect Dodge" (kaçınma) ödüllendirilir |

> Trafik kazası temalı yarış oyunlarında IARC bazen "araçlara zarar verme" alt sorusunu
> sorar. Kron Drive'da hasar modeli, enkaz görseli ve çarpışma ödülü yoktur — hepsine
> **Hayır**.

---

## 3. Cinsellik ve müstehcenlik

| Soru | Cevap |
|---|---|
| Cinsel içerik, çıplaklık, müstehcen davranış | **Hayır** |
| Kışkırtıcı kıyafet / cinsel çağrışım | **Hayır** |

Oyunda insan figürü hiç yoktur.

---

## 4. Dil

| Soru | Cevap |
|---|---|
| Küfür, argo, hakaret içeriyor mu? | **Hayır** |
| Kaba mizah (tuvalet mizahı vb.) | **Hayır** |

Tüm metinler TR/EN oyun arayüzü metinleridir (`AppLanguage.pick`); küfür içermez.

---

## 5. Kontrollü maddeler

| Soru | Cevap |
|---|---|
| Alkol, tütün veya uyuşturucu kullanımı/gösterimi | **Hayır** |
| Alkol/tütün/uyuşturucu referansı | **Hayır** |

---

## 6. Korku / tedirginlik

| Soru | Cevap |
|---|---|
| Küçük çocukları korkutabilecek görüntü veya ses | **Hayır** |
| Karanlık/gerilim teması | **Hayır** — "night" yol teması yalnızca bir renk paletidir |

---

## 7. Kumar ve şans

| Soru | Cevap | Gerekçe (kod kanıtı) |
|---|---|---|
| Gerçek parayla kumar oynanıyor mu? | **Hayır** | Uygulama içi satın alma yok, ödeme yok |
| Simüle kumar (slot, poker, rulet) var mı? | **Hayır** | Böyle bir mini oyun yok |
| Rastgele içerikli **ganimet sandığı / loot box** var mı? | **Hayır** | Haftalık sandık sabit ödül veriyor: `WeeklyMissionGenerator.WEEKLY_CHEST_COINS` + sabit bir `WEEKLY_CHEST_BOOSTER`. Rastgelelik yok, ödeme yok |
| Oyun içi para gerçek parayla alınabiliyor mu? | **Hayır** | Coin yalnızca oynayarak veya ödüllü reklam izleyerek kazanılır |
| Gerçek para veya ödül kazanma vaadi? | **Hayır** | Token/kripto/"kazan" mekaniği yok |

---

## 8. Kullanıcı etkileşimi (Interactive elements)

| Soru | Cevap | Gerekçe |
|---|---|---|
| Kullanıcılar birbiriyle etkileşebiliyor mu (sohbet, mesaj, forum)? | **Hayır** | Ağ üzerinden oyuncu etkileşimi yok; sunucu yok |
| Kullanıcı üretimi içerik paylaşılabiliyor mu? | **Hayır** | — |
| Kullanıcının fiziksel konumu diğer kullanıcılarla paylaşılıyor mu? | **Hayır** | Konum izni hiç yok |
| Kullanıcının kişisel bilgisi üçüncü taraflarla paylaşılıyor mu? | **Evet** | Reklam SDK'sı reklam kimliği ve reklam etkileşimi paylaşıyor — bkz. `docs/DATA_SAFETY_FORM.md`. **Bu satırda "Hayır" demek Data Safety beyanıyla çelişir** |
| Dijital satın alma var mı? | **Hayır** | IAP yok |
| Uygulama reklam gösteriyor mu? | **Evet** | Banner + geçiş + ödüllü video |

---

## 9. Diğer

| Soru | Cevap |
|---|---|
| Uygulama dış bağlantı (tarayıcı) açıyor mu? | **Evet — yalnızca reklam tıklamasıyla.** Oyunun kendi menülerinde harici bağlantı yok. *(Anket "uncontrolled web browsing" diye soruyorsa cevap Hayır: gömülü serbest tarayıcı yoktur.)* |
| Sosyal ağ bağlantısı, hesap girişi | **Hayır** |
| Kripto para / NFT / blockchain | **Hayır** |
| Sağlık/tıbbi bilgi | **Hayır** |
| Nefret söylemi, aşırılık, yasa dışı içerik | **Hayır** |

---

## 10. Beklenen derecelendirme

Yukarıdaki cevaplarla beklenen sonuç:

| Kurul | Beklenen | Not |
|---|---|---|
| **ESRB** (ABD/Kanada) | **Everyone** | "In-Game Purchases" etiketi **çıkmaz** (IAP yok); reklam ayrıca etiketlenmez |
| **PEGI** (Avrupa) | **PEGI 3** | Muhtemelen tek deskriptörsüz |
| **USK** (Almanya) | **USK 0** | — |
| **ClassInd** (Brezilya) | **Livre (L)** | — |
| **GRAC** (Kore) | **All** | — |
| **IARC genel** | **3+** | — |

> **Önemli ayrım:** IARC derecelendirmesi (3+ / Everyone) ile Play Console'daki
> **hedef kitle (target audience)** beyanı **farklı iki şeydir**. İçerik olarak oyun 3+
> çıkacaktır; buna rağmen hedef kitleyi **13+** seçmek mümkündür ve önerilmektedir
> (gerekçesi: `docs/STORE_SUBMISSION_CHECKLIST.md` → "Hedef kitle kararı"). Düşük IARC
> derecesi tek başına uygulamayı Families programına sokmaz.

---

## 10b. Kardeş uygulama (Kaboom Blocks) ile tutarlılık

Emsal: `Boom-Blocks/docs/PLAY_STORE_DATA_SAFETY.md` §4. Orada da şiddet / cinsel içerik /
küfür / madde referansı / kullanıcı içeriği / konum paylaşımı / kumar-benzeri mekanik
maddelerinin hepsine **Hayır** verilmiş ve beklenen sonuç **PEGI 3 / Everyone** olarak
yazılmış. **Kron Drive'ın cevapları bu çizgiyle birebir aynıdır**; farklı bir yanıt veren
tek satır yoktur.

Bu belge emsalden yalnızca **daha ayrıntılı** olmasıyla ayrılır: yarış oyununa özgü
soruları (araçlara kasıtlı çarpma, hasar/enkaz gösterimi) ve "kişisel bilgi üçüncü
taraflarla paylaşılıyor mu → **Evet**, reklam SDK'sı" satırını açıkça ele alır. Sonuncusu
önemlidir: Data Safety'de "paylaşılıyor" denip IARC anketinde "paylaşılmıyor" denmesi
kendi içinde çelişkili bir gönderim yaratır.

---

## 11. Anket sonrası

- Anket kaydedildiğinde derecelendirme birkaç dakika içinde uygulanır; kurul rozetleri
  mağaza sayfasında görünür.
- **İçerik değişirse anket yeniden doldurulmalıdır.** Bu oyunda anketi geçersiz kılacak
  değişiklikler: uygulama içi satın alma eklenmesi, rastgele ödüllü sandık eklenmesi,
  liderlik tablosu/sohbet gibi kullanıcı etkileşimi eklenmesi, gerçek para/token
  mekaniği eklenmesi.
- Anket cevaplarının bir kopyası Console'da saklanır; bu dosya "biz ne cevaplamıştık"
  sorusunun kaynağıdır — Console'da değişiklik yaparsan burayı da güncelle.
