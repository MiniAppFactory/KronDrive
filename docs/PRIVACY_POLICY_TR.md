# Gizlilik Politikası — Kron Drive: Retro Racer

**Yürürlük tarihi:** 14 Ağustos 2026
**Son güncelleme:** 14 Ağustos 2026

**Uygulama:** Kron Drive: Retro Racer
**Paket adı:** `com.miniappfactory.krondrive`
**Geliştirici:** MiniAppFactory
**İletişim:** whatsthisapp@proton.me
**Proje sayfası:** https://github.com/MiniAppFactory/KronDrive

---

## 1. Kısa özet

Kron Drive çevrimdışı oynanan tek kişilik bir yarış oyunudur. **Kullanıcı hesabı yok,
giriş yok, bize ait sunucu yok, analitik yok, çökme raporlama servisi yok, uygulama içi
satın alma yok.**

Biz — geliştirici — sizinle ilgili **hiçbir kişisel veriyi toplamıyor, almıyor, saklamıyor
veya satmıyoruz.** Oyunun sizinle ilgili hatırladığı her şey (coin, bölüm yıldızları,
yükseltmeler, araç seçimi, görev ilerlemesi, dil ve ses tercihi) **kendi cihazınızdaki**
depolamaya yazılır ve hiçbir yere gönderilmez.

Tek istisna reklamlardır. Oyun **Google AdMob** üzerinden reklam gösterir. Uygulamanın
içinde çalışan Google Mobile Ads SDK'sı; reklamcılık, analiz ve dolandırıcılık önleme
amacıyla cihazınızın reklam kimliği dâhil sınırlı bir veri kümesini toplar ve paylaşır. Bu
veri bize değil Google'a gider. Ayrıntısı 4. bölümdedir.

---

## 2. Oyunun kendisinin topladığı veri

**Cihazınızdan çıkan hiçbir veri yok.**

Oyun aşağıdakileri cihazınızda, uygulamanın özel depolama alanında saklar
(Android `DataStore`, dosya adı `kron_drive_progress`):

| Saklanan değer | Nedir |
|---|---|
| Coin, XP | Oyun içi para ve deneyim |
| Açılan en yüksek bölüm, bölüm başına yıldız | Kariyer ilerlemesi |
| Yükseltme seviyeleri (hız, ivme, fren, boost) | Garaj yükseltmeleri |
| Sahip olunan güçlendiriciler | Tüketilebilir eşya sayıları |
| Seçili araç gövdesi ve boyası, sahip olunan gövde ve boyalar | Araç özelleştirme |
| Sonsuz mod en iyi süre ve en iyi skor | Kişisel rekorlar |
| Günlük görevde ulaşılan kademe, gün kimliği | Günlük görev durumu |
| Haftalık görev ilerlemesi ve alınan ödüller, hafta kimliği | Haftalık görev durumu |
| O güne ait ödüllü reklam sayacı | Günlük ödül sınırını uygular |
| Geçiş reklamı sayaçları | Tam ekran reklamın sıklığını ayarlar |
| Ses açık/kapalı, dil (Türkçe/İngilizce), tanıtım görüldü bayrağı | Tercihleriniz |

Bu değerlerin hiçbiri sizi tanımlamaz. İsim, e-posta, telefon numarası, hesap, rehber,
fotoğraf, mikrofon veya kamera erişimi yoktur; hassas konum erişimi de yoktur. Uygulama
kendi adına yalnızca iki Android izni tanımlar — `INTERNET` ve `ACCESS_NETWORK_STATE` — ve
ikisi de sadece reklam SDK'sının ağa çıkabilmesi içindir. **Oyunun kendisi tamamen
çevrimdışı oynanabilir.**

### Android yedeklemesi

Uygulama Android'in standart Otomatik Yedekleme özelliğini kullanır. Cihaz ayarlarınızda
yedekleme açıksa ilerleme dosyanız **sizin kendi Google Drive yedek hesabınıza**
kopyalanabilir; bu sizin kontrolünüzde ve Google'ın koşullarına tabidir, bize gelmez.
Cihaz ayarlarından kapatabilirsiniz (Ayarlar → Google → Yedekleme).

---

## 3. Yapmadıklarımız

- Kullanıcı hesabı oluşturmuyoruz; tek tek oyuncuları tanımlayamayız.
- Bu oyun için hiçbir sunucu, veritabanı veya arka uç işletmiyoruz.
- Analitik SDK'sı kullanmıyoruz (Firebase Analytics yok, Google Analytics yok).
- Çökme raporlama servisi kullanmıyoruz.
- Uygulama içi satın alma sunmuyoruz; hiçbir ödeme bilgisi istemiyoruz.
- Sohbet, liderlik tablosu, arkadaş listesi veya kullanıcı üretimi içerik yoktur.
- Kripto para, token, cüzdan, gerçek para ödülü veya "oyna-kazan" mekaniği yoktur.
- 4. bölümde anlatılanın dışında, kişisel bilgiyi davranışsal reklamcılık için bilerek
  satmıyor veya paylaşmıyoruz.

---

## 4. Reklamlar (Google AdMob)

Uygulama; menü ekranlarında banner reklam, koşular arasında tam ekran geçiş reklamı ve
oyun içi ödül karşılığında **izlemeyi kendiniz seçtiğiniz** ödüllü video reklam gösterir.
Bunlar Google Mobile Ads SDK'sı aracılığıyla **Google AdMob** tarafından sunulur.

Google'ın Google Mobile Ads SDK için yayımladığı resmî açıklamaya göre SDK şu veri
kategorilerini toplar ve paylaşır:

| Veri | Amaç | Not |
|---|---|---|
| Android reklam kimliği (AAID) | Reklamcılık, analiz, dolandırıcılık önleme | Opsiyonel — cihaz ayarlarından sıfırlayabilir veya silebilirsiniz (aşağı bakın) |
| App set ID | Reklamcılık, analiz, dolandırıcılık önleme | Aynı geliştiricinin uygulamalarıyla sınırlı kimlik |
| Hesap tanımlayıcıları | Reklamcılık, analiz, dolandırıcılık önleme | Google'ın SDK'sı toplar, bizim görebildiğimiz bir şey değildir |
| Yaklaşık konum (IP adresinden türetilir) | Reklam sunumu için cihazın genel konumunu tahmin etme | GPS değildir; uygulama konum izni istemez |
| Uygulama etkileşimleri (reklam gösterimi, tıklama) | Reklamcılık, analiz, dolandırıcılık önleme | Yalnızca reklam olayları |
| Tanılama (diagnostics) | Uygulama ve SDK performans izleme | Teknik veri |

Google, Mobile Ads SDK'sının topladığı tüm kullanıcı verisinin aktarım sırasında TLS ile
şifrelendiğini beyan eder. Bu, Google'ın kendi SDK'sı hakkındaki beyanıdır; bizim sunucumuz
olmadığı için bu verinin bizde bir kopyası bulunmaz.

**Google, AdMob üzerinden topladığı veri bakımından bağımsız veri sorumlusudur.**
Google'ın bu veriyi nasıl kullandığını şuradan okuyabilirsiniz:

- Google Gizlilik Politikası — https://policies.google.com/privacy
- Google hizmetlerini kullanan site ve uygulamalardan gelen bilgiler —
  https://policies.google.com/technologies/partner-sites
- Google Reklam Politikaları — https://policies.google.com/technologies/ads

Yukarıdaki tablonun kaynağı:
https://developers.google.com/admob/android/privacy/play-data-disclosure

### Reklam kişiselleştirmesini kontrol etmek

- **Cihazınızda:** Ayarlar → Gizlilik → Reklamlar → *Reklam kimliğini sil* veya
  *Reklam kimliğini sıfırla*. Reklam kimliğini silmek uygulamaların bu kimliği almasını
  durdurur; reklamlar görünmeye devam eder ama kişiselleştirilmez.
- **Google hesap ayarlarınızda:** https://myadcenter.google.com

---

## 5. Onay / rıza (AEA, Birleşik Krallık ve İsviçre)

Avrupa Ekonomik Alanı, Birleşik Krallık ve İsviçre'deki kullanıcılar için uygulama,
kişiselleştirilmiş reklam verisi kullanılmadan önce bir onay formu göstermek üzere
**Google'ın User Messaging Platform (UMP)** bileşenini kullanır. Formdaki tercihiniz UMP
SDK'sı tarafından cihazınızda saklanır ve reklam SDK'sına iletilir.

Tercihinize göre kişiselleştirilmiş veya kişiselleştirilmemiş reklam görürsünüz.
Kişiselleştirilmemiş reklamlar da sunum, sıklık sınırlama ve dolandırıcılık önleme için
temel veriye (IP adresi ve reklam etkileşim sayıları gibi) ihtiyaç duyar.

Onayınızı istediğiniz zaman geri alabilir veya değiştirebilirsiniz. Bunu uygulama verisini
temizleyerek (Ayarlar → Uygulamalar → Kron Drive → Depolama → *Verileri temizle*; bu,
kayıtlı tercihi sıfırlar ve form bir sonraki açılışta yeniden gelir) veya
whatsthisapp@proton.me adresine yazarak yapabilirsiniz.

---

## 6. Çocukların gizliliği

Kron Drive **13 yaş altı çocuklara yönelik değildir.** Google Play mağaza sayfasında
uygulamanın hedef kitlesi **13 yaş ve üzeri** olarak beyan edilmiştir. Çocuklardan bilerek
kişisel veri toplamayız.

Bir çocuğun uygulamayı veri toplanmasına yol açacak şekilde kullandığını düşünüyorsanız
whatsthisapp@proton.me adresine yazın. Uygulamada hesap ve sunucu tarafında kayıt
bulunmadığı için pratik çözüm uygulamayı silmek ve cihazın reklam kimliğini sıfırlamaktır;
talep hâlinde adım adım anlatırız.

---

## 7. Verinin saklanması ve silinmesi

- **Oyun ilerlemesi:** yalnızca cihazınızda, uygulama kurulu kaldığı sürece saklanır.
  Uygulamayı kaldırmak bu veriyi siler. Kaldırmadan da silebilirsiniz:
  Ayarlar → Uygulamalar → Kron Drive → Depolama → *Verileri temizle*. Bizde silinecek bir
  kopya yoktur, çünkü hiç almadık.
- **Reklam verisi:** Google'ın elindedir ve Google'ın saklama politikalarına tabidir. Bu
  veri üzerindeki haklarınızı https://myadcenter.google.com üzerinden, cihazınızın reklam
  kimliği denetimleriyle veya doğrudan Google'a başvurarak kullanabilirsiniz.
- **Bize gönderdiğiniz e-posta:** whatsthisapp@proton.me adresine yazarsanız mesajınızı
  yalnızca yanıtlamak için gerekli süre boyunca tutar, sonra sileriz.

Uygulama hesap oluşturmayı desteklemediği için Google Play'in uygulama içi **hesap silme**
zorunluluğu bizde geçerli değildir.

---

## 8. Haklarınız (KVKK, GDPR, UK GDPR, CCPA/CPRA)

Hakkınızda hiçbir kişisel veri tutmadığımız için sizin adınıza erişilecek, düzeltilecek,
taşınacak veya silinecek bir kaydımız yoktur. Google'ın AdMob üzerinden topladığı veriye
ilişkin haklarınızı, o verinin sorumlusu olan Google'a yöneltmeniz gerekir (4. bölümdeki
bağlantılar).

6698 sayılı KVKK'nın 11. maddesi ve ilgili diğer mevzuat uyarınca genel olarak şu
haklarınız vardır: kişisel verinizin işlenip işlenmediğini öğrenme, işlenmişse bilgi talep
etme, işlenme amacını ve amaca uygun kullanılıp kullanılmadığını öğrenme, eksik veya yanlış
işlenmişse düzeltilmesini isteme, silinmesini veya yok edilmesini isteme, işlemeye itiraz
etme, açık rızayı geri alma, verinin aktarıldığı üçüncü kişileri bilme ve zararın
giderilmesini talep etme. Ayrıca Kişisel Verileri Koruma Kurumu'na (AB'de kendi ülkenizin
veri koruma otoritesine) şikâyette bulunma hakkınız saklıdır.

Her zaman önce **whatsthisapp@proton.me** adresinden bize ulaşabilirsiniz; 30 gün içinde
yanıt veririz.

---

## 9. Güvenlik

Burada rahatlatıcı olmak yerine **doğru** olmayı tercih ediyoruz:

- Bu oyun için sunucu işletmiyoruz; dolayısıyla **sızdırılabilecek merkezî bir oyuncu
  veri tabanı yoktur.**
- İlerleme dosyanız uygulamanın özel depolama dizinindedir ve Android'in standart uygulama
  kum havuzu (sandbox) tarafından korunur; başka uygulamalar okuyamaz. Bizim tarafımızdan
  ayrıca şifrelenmez ve kullanıcının kendine root yetkisi verdiği bir cihazda okunabilir.
  İçinde kişisel veya hassas hiçbir bilgi yoktur.
- Google Mobile Ads SDK'sının ilettiği veri, Google'ın yayımladığı beyana göre aktarım
  sırasında TLS ile şifrelenir.

Bunların dışında hiçbir güvenlik iddiasında bulunmuyoruz.

---

## 10. Politikadaki değişiklikler

Bu politika değişirse yeni sürüm aynı URL'de yayımlanır ve baştaki "Son güncelleme" tarihi
değişir. Veri toplamayı etkileyen esaslı değişiklikler ayrıca mağaza sayfasındaki sürüm
notlarında belirtilir.

---

## 11. İletişim

**E-posta:** whatsthisapp@proton.me
**Proje:** https://github.com/MiniAppFactory/KronDrive

---

<!--
========================================================================
YAYIMLANAN POLİTİKANIN PARÇASI DEĞİLDİR — İÇ NOTLAR, YAYINLAMADAN ÖNCE SİL
========================================================================

1. BU DOSYA KAYNAK METİNDİR, YAYIMLANAN SAYFA DEĞİLDİR.
   Yayımlanan sayfa: docs/tr/index.html →
   https://miniappfactory.github.io/KronDrive/tr/
   Bu metin değişirse docs/tr/index.html de güncellenmelidir. İngilizce eşi:
   docs/PRIVACY_POLICY_EN.md → docs/index.html.
   Yayına alma adımları: docs/STORE_SUBMISSION_CHECKLIST.md bölüm 2.

2. 5. bölüm, onayın "uygulama verisini temizleyerek" geri alınmasını anlatıyor;
   çünkü uygulamada henüz gizlilik seçenekleri giriş noktası yok (denetim
   bulgusu C-2a). O düğme eklendiğinde 5. bölüm HEM .md HEM .html tarafında
   güncellenmeli.

3. 6. bölüm ÖNERİLEN "13+" hedef kitle kararına göre yazılmıştır. Sahibi bunun
   yerine çocukları içeren bir hedef kitle seçerse 6. bölüm aşağıdaki metinle
   DEĞİŞTİRİLMELİ ve uygulamada TFCD/TFUA bayrakları set edilmelidir:

   ## 6. Çocukların gizliliği
   Kron Drive çocukların erişimine açıktır. Google Play Families Politikası ve
   COPPA gereğince uygulama tüm reklam isteklerini çocuğa yönelik
   (tagForChildDirectedTreatment) ve rıza yaşının altında (tagForUnderAgeOfConsent)
   olarak işaretler. Bunun sonucunda yalnızca KİŞİSELLEŞTİRİLMEMİŞ reklam
   gösterilir, reklam kimliği ilgi alanına dayalı reklamcılık veya yeniden
   pazarlama için kullanılmaz ve reklamlar yalnızca Google Play Families
   Self-Certified Ads SDK üzerinden sunulur. Çocuklardan bilerek kişisel bilgi
   toplamayız.

4. KVKK "veri sorumlusu" kimliği için tüzel kişi unvanı / adres gerekiyorsa
   Geliştirici satırının altına eklenmelidir. VERBİS kaydının gerekip
   gerekmediği tüzel kişilik türüne ve ciro eşiklerine bağlıdır — hukuki teyit
   alınmalı; bu belge hukuki görüş değildir.
-->
