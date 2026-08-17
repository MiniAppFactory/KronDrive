package com.miniappfactory.krondrive.audio

import com.miniappfactory.krondrive.game.CarCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ses profili tablosunun dogrulanmasi.
 *
 * Bu makinede hoparlor yok — sesin "guzel" olup olmadigi burada
 * dogrulanamaz, o karar proje sahibinin. Burada dogrulanan sey tablonun
 * TUTARLILIGI: her gövde bir profil buluyor mu, bilinmeyen id cokuyor mu,
 * profiller gercekten birbirinden farkli mi ve sahibin acikca istedigi
 * karakter ("Boğa 67 en gürültülü") sayilara yansimis mi.
 */
class CarSoundProfilesTest {

    @Test
    fun `katalogdaki her govde bir ses profili bulur`() {
        // Kural 2026-08-16'da NETLESTIRILDI: her govdenin kendi profili
        // olmak zorunda DEGIL, ama sessizce genel sese dusmesi de yasak.
        // Bir govde ancak [CarSoundProfiles.ALIASES] icinde ACIKCA yaziliysa
        // baska bir govdenin sesini paylasabilir. Sahibi karari: Sehir,
        // Beety ile ayni sesi kullaniyor.
        CarCatalog.shapes.forEach { shape ->
            val profile = CarSoundProfiles.forShape(shape.id)
            val beklenen = CarSoundProfiles.ALIASES[shape.id] ?: shape.id
            assertEquals(
                "${shape.id} icin profil yok — ya kendi profilini yaz ya da " +
                    "ALIASES icine acikca ekle",
                beklenen,
                profile.id
            )
        }
        assertEquals(
            "profil sayisi = govde sayisi - bilerek paylasanlar",
            CarCatalog.shapes.size - CarSoundProfiles.ALIASES.size,
            CarSoundProfiles.all.size
        )
    }

    @Test
    fun `bilinmeyen bos ve null id varsayilana duser`() {
        assertSame(CarSoundProfiles.DEFAULT, CarSoundProfiles.forShape(null))
        assertSame(CarSoundProfiles.DEFAULT, CarSoundProfiles.forShape(""))
        assertSame(CarSoundProfiles.DEFAULT, CarSoundProfiles.forShape("tir_2027"))
        assertSame(CarSoundProfiles.DEFAULT, CarSoundProfiles.forShape("HATCHBACK"))
    }

    @Test
    fun `profiller birbirinden ayirt edilebilir`() {
        val profiles = CarSoundProfiles.all
        // Kimlikler benzersiz.
        assertEquals(profiles.size, profiles.map { it.id }.toSet().size)
        // Iki gövde ayni ses parametreleriyle gelmemeli — aksi halde oyuncu
        // arac degistirdiginde hicbir sey duymaz.
        val fingerprints = profiles.map {
            listOf(
                it.freqMul, it.harmonic2, it.harmonic3, it.harmonic4, it.harmonic5,
                it.grit, it.noiseAmount, it.lopeDepth, it.lopeRate,
                it.gainMul, it.cutoffMul
            )
        }
        assertEquals("iki gövde ayni motor sesine sahip", profiles.size, fingerprints.toSet().size)
        // Korna da araca gore degisir; en az bes farkli temel frekans olsun.
        assertTrue(
            "kornalar birbirine cok yakin",
            profiles.map { it.hornBaseHz }.toSet().size >= 5
        )
    }

    @Test
    fun `Boga 67 katalogun en gurultulu egzozu`() {
        val boga = CarSoundProfiles.forShape(CarCatalog.SHAPE_MUSCLE_67)
        val digerleri = CarSoundProfiles.all.filter { it.id != boga.id }

        assertTrue(
            "en yuksek genlik Boga 67'de olmali",
            digerleri.all { it.gainMul < boga.gainMul }
        )
        assertTrue(
            "en kalin motor Boga 67'de olmali",
            digerleri.all { it.freqMul > boga.freqMul }
        )
        assertTrue(
            "en derin lope Boga 67'de olmali",
            digerleri.all { it.lopeDepth < boga.lopeDepth }
        )
        assertTrue(
            "en kalin korna Boga 67'de olmali",
            digerleri.all { it.hornBaseHz > boga.hornBaseHz }
        )
        // V8 karakterinin sayisal tanimi: TEK sayili harmonikler cift
        // sayililardan baskin.
        assertTrue(
            "tek sayili harmonikler baskin olmali",
            boga.harmonic3 + boga.harmonic5 > 2f * (boga.harmonic2 + boga.harmonic4)
        )
    }

    @Test
    fun `dag kecisi tok super araba keskin`() {
        val keci = CarSoundProfiles.forShape(CarCatalog.SHAPE_MOUNTAIN_GOAT)
        val super_ = CarSoundProfiles.forShape(CarCatalog.SHAPE_SUPERCAR)
        val sehir = CarSoundProfiles.DEFAULT

        assertTrue("dag kecisi en kapali filtreye sahip olmali", keci.cutoffMul < sehir.cutoffMul)
        assertTrue("dizel dokusu en yuksek dag kecisinde olmali",
            CarSoundProfiles.all.filter { it.id != keci.id }.all { it.noiseAmount < keci.noiseAmount })
        assertTrue("super araba en tiz olmali",
            CarSoundProfiles.all.filter { it.id != super_.id }.all { it.freqMul < super_.freqMul })
        assertTrue("super araba en acik filtreye sahip olmali",
            CarSoundProfiles.all.filter { it.id != super_.id }.all { it.cutoffMul < super_.cutoffMul })
    }

    /**
     * Beety'nin kimligi Boga 67'nin AYNASI: boksor dortlu CIFT sayili
     * harmonikleri one cikarir, V8 tek sayililari.
     *
     * Bu test ayrica bir kurali korumak icin var: yeni bir govde, var olan bir
     * govdenin tek cumlelik tarifini elinden alarak kendine kimlik kuramaz.
     * Beety'nin ilk profili katalogun en pesi + en derin lope'u + en
     * gurultulusu yapilmisti ve Boga 67 ile Dag Kecisi'nin garantilerini
     * kiriyordu (2026-08-16).
     */
    @Test
    fun `Beety boksor imzasi tasir ve kimseden tac almaz`() {
        val beety = CarSoundProfiles.forShape(CarCatalog.SHAPE_BEETY)
        val boga = CarSoundProfiles.forShape(CarCatalog.SHAPE_MUSCLE_67)
        val keci = CarSoundProfiles.forShape(CarCatalog.SHAPE_MOUNTAIN_GOAT)

        assertTrue(
            "boksor imzasi: cift sayili harmonikler baskin olmali",
            beety.harmonic2 + beety.harmonic4 > 2f * (beety.harmonic3 + beety.harmonic5)
        )
        // Taclar sahibinde kalmali.
        assertTrue("Boga 67 daha pes kalmali", beety.freqMul > boga.freqMul)
        assertTrue("Boga 67'nin lope'u daha derin kalmali", beety.lopeDepth < boga.lopeDepth)
        assertTrue("Dag Kecisi daha gurultulu kalmali", beety.noiseAmount < keci.noiseAmount)
    }

    /**
     * F1'in kimligi ne Boga 67'nin (tek harmonikler) ne Beety'nin (cift
     * harmonikler) aynasi — UCUNCU bir yapi: **duz tarak**. Butun
     * harmonikler birbirine yakin ve hepsi yuksek.
     *
     * Bu, olcumden turedi: F1 referansinda en guclu 8 tepenin guc degisim
     * katsayisi 0.108, en zayif/en guclu orani 0.714 — mevcut yedi aracin
     * bandi (CV 0.999–1.901, oran 0.007–0.032) ile kiyaslanamaz. F1 tek bir
     * harmonige yiginmiyor, cok sayida ust harmonigi esit suruyor.
     *
     * Test ayrica F1'in HICBIR tac almadigini kilitliyor: tizlik ve filtre
     * Super Araba'da kaliyor. F1'in kendi taclari duzensizligin YOKLUGU
     * ekseninde (en sig lope, en az grit, en az gurultu) — bunlar bostu.
     */
    @Test
    fun `F1 duz harmonik tarak tasir ve kimseden tac almaz`() {
        val f1 = CarSoundProfiles.forShape(CarCatalog.SHAPE_F1)
        val digerleri = CarSoundProfiles.all.filter { it.id != f1.id }
        val superAraba = CarSoundProfiles.forShape(CarCatalog.SHAPE_SUPERCAR)

        // DUZ TARAK: en zayif harmonik, en guclusunun en az %80'i olmali.
        val harmonikler = listOf(f1.harmonic2, f1.harmonic3, f1.harmonic4, f1.harmonic5)
        assertTrue(
            "F1 imzasi: harmonikler birbirine yakin olmali (min/max >= 0.80), " +
                "olculen $harmonikler",
            harmonikler.min() >= 0.80f * harmonikler.max()
        )
        // ...ve hepsi GERCEKTEN yuksek olmali; dort tane 0.05 de "duz"dur.
        assertTrue(
            "F1 imzasi: harmonikler yuksek olmali (en zayifi >= 0.40)",
            harmonikler.min() >= 0.40f
        )
        // Ne V8 ne boksor imzasi — F1 ucuncu kategori.
        assertTrue(
            "F1 tek sayili harmonik imzasini (Boga 67) almamali",
            f1.harmonic3 + f1.harmonic5 <= 2f * (f1.harmonic2 + f1.harmonic4)
        )
        assertTrue(
            "F1 cift sayili harmonik imzasini (Beety) almamali",
            f1.harmonic2 + f1.harmonic4 <= 2f * (f1.harmonic3 + f1.harmonic5)
        )

        // Taclar sahibinde: tizlik ve filtre Super Araba'nin.
        assertTrue("Super Araba daha tiz kalmali", f1.freqMul < superAraba.freqMul)
        assertTrue("Super Araba'nin filtresi daha acik kalmali",
            f1.cutoffMul < superAraba.cutoffMul)

        // F1'in KENDI taclari: duzensizligin yoklugu.
        assertTrue("en sig lope F1'de olmali",
            digerleri.all { it.lopeDepth > f1.lopeDepth })
        assertTrue("en az metalik puruz F1'de olmali",
            digerleri.all { it.grit > f1.grit })
        assertTrue("en az motor gurultusu F1'de olmali",
            digerleri.all { it.noiseAmount > f1.noiseAmount })
        // Korna: F1'de korna yok — sifir yazilamadigi icin en ince ve en kisa.
        assertTrue("en ince korna F1'de olmali",
            digerleri.all { it.hornBaseHz < f1.hornBaseHz })
        assertTrue("en kisa korna F1'de olmali",
            digerleri.all { it.hornSeconds > f1.hornSeconds })
    }

    /**
     * Motosiklet F1 ile ayni ailede (yuksek devir, dengeli tarak) ama ondan
     * HAM. Olcumde ikisini ayiran sey: 500 Hz ustu/alti enerji orani
     * motosiklette 0.115, F1'de 1.348 — yani motosikletin agirligi hala
     * altta. Profilde bu, daha kapali filtre + daha dusuk frekans + belirgin
     * lope + metalik grit olarak karsiligini buluyor.
     */
    @Test
    fun `motosiklet F1den ham ama ayni ailede`() {
        val moto = CarSoundProfiles.forShape(CarCatalog.SHAPE_MOTOSIKLET)
        val f1 = CarSoundProfiles.forShape(CarCatalog.SHAPE_F1)
        val boga = CarSoundProfiles.forShape(CarCatalog.SHAPE_MUSCLE_67)
        val superAraba = CarSoundProfiles.forShape(CarCatalog.SHAPE_SUPERCAR)

        // Ayni aile: ikisi de katalogun tiz ucunda.
        assertTrue("motosiklet yuksek devirli olmali", moto.freqMul > 1.15f)
        // Ama F1'den ham ve daha az parlak.
        assertTrue("F1 daha tiz kalmali", moto.freqMul < f1.freqMul)
        assertTrue("F1'in filtresi daha acik kalmali", moto.cutoffMul < f1.cutoffMul)
        assertTrue("motosiklet F1'den daha belirgin vurmali",
            moto.lopeDepth > f1.lopeDepth)
        assertTrue("motosiklet F1'den daha puruzlu olmali", moto.grit > f1.grit)

        // Vurus gercekten belirgin olmali (tek/cift silindir imzasi).
        assertTrue("motosikletin lope'u belirgin olmali", moto.lopeDepth >= 0.20f)

        // Taclar sahibinde.
        assertTrue("Super Araba daha tiz kalmali", moto.freqMul < superAraba.freqMul)
        assertTrue("Boga 67'nin V8 imzasi exclusive kalmali",
            moto.harmonic3 + moto.harmonic5 <= 2f * (moto.harmonic2 + moto.harmonic4))
        assertTrue("Boga 67 daha gurultulu kalmali", moto.gainMul < boga.gainMul)
    }

    /**
     * Tir katalogun EN KAPALI FILTRELI motoru (0.44).
     *
     * Dikkat — burada "en koyu SES" IDDIA EDILMIYOR ve test de onu
     * kontrol etmiyor. Olculdu: sentezlenen tirin spektral agirlik merkezi
     * 173 Hz (tonal govde), Dag Kecisi'nin 160 Hz'inden yuksek. Sebebi
     * kasitli — kalinlik icin eklenen tek sayili harmonikler enerjiyi
     * yukari tasir ve dizel tikirtisi motor filtresini bypass eder.
     * Ayrinti [CarSoundProfiles.TIR] KDoc'unda.
     *
     * Kritik nokta: tirin kalinligi [CarSoundProfile.freqMul]'dan GELMIYOR
     * — gelemez, cunku 0.76 tabani (telefon hoparloru) ve Boga 67'nin
     * "en pes" taci o ekseni kapatiyor. Kalinlik iki yerden geliyor ve test
     * ikisini de kilitliyor: eksik temel etkisi (tek sayili harmonikler
     * ciftlerden agir) ve katalogun en kapali filtresi.
     */
    @Test
    fun `tir en kapali filtreli motor ama frekans tacini almaz`() {
        val tir = CarSoundProfiles.forShape(CarCatalog.SHAPE_TIR)
        val digerleri = CarSoundProfiles.all.filter { it.id != tir.id }
        val boga = CarSoundProfiles.forShape(CarCatalog.SHAPE_MUSCLE_67)
        val keci = CarSoundProfiles.forShape(CarCatalog.SHAPE_MOUNTAIN_GOAT)

        // 1) EN TOK: katalogun en kapali filtresi.
        assertTrue("en kapali filtre tirda olmali",
            digerleri.all { it.cutoffMul > tir.cutoffMul })
        // 2) Kalinlik tek sayili harmoniklerden (eksik temel etkisi).
        assertTrue(
            "tirin kalinligi tek sayili harmoniklerden gelmeli",
            tir.harmonic3 + tir.harmonic5 > tir.harmonic2 + tir.harmonic4
        )
        // 3) ...ama frekanstan GELMEMELI: 0.76 tabani ve Boga'nin taci.
        assertTrue("Boga 67 katalogun en pesi kalmali", tir.freqMul > boga.freqMul)
        assertTrue(
            "freqMul 0.76 tabaninin altina inemez (telefon hoparloru temeli " +
                "tasimiyor — KDoc'ta yazili)",
            tir.freqMul >= 0.76f
        )
        // 4) Boga'nin V8 imzasi yine de exclusive: tirinki olculu yatiklik.
        assertTrue(
            "Boga 67'nin V8 imzasi (tek > 2x cift) exclusive kalmali",
            tir.harmonic3 + tir.harmonic5 <= 2f * (tir.harmonic2 + tir.harmonic4)
        )
        // 5) Dizel tikirtisi yuksek ama Dag Kecisi'nin taci duruyor.
        assertTrue("tirda belirgin dizel tikirtisi olmali", tir.noiseAmount >= 0.06f)
        assertTrue("Dag Kecisi daha gurultulu kalmali", tir.noiseAmount < keci.noiseAmount)
        // 6) Agir rolanti, ama Boga'nin iki lope taci da yerinde.
        assertTrue("Boga 67'nin lope'u daha derin kalmali", tir.lopeDepth < boga.lopeDepth)
        assertTrue("Boga 67'nin lope'u daha yavas kalmali", tir.lopeRate > boga.lopeRate)
    }

    /**
     * Tirin HAVA KORNASI. Dosyanin basindaki "Tir hazirligi" bolumu ~110 Hz
     * onermisti; Boga 67'nin 250 Hz'lik "en kalin korna" taci buna izin
     * vermedi. Karakter bu yuzden bos duran uc eksene dagitildi — asil hava
     * kornasi hissini (basinc kurmasi, uzun uzun otmesi) tasiyanlar zaten
     * bunlar.
     */
    @Test
    fun `tirin hava kornasi en genis en uzun ve en yavas`() {
        val tir = CarSoundProfiles.forShape(CarCatalog.SHAPE_TIR)
        val digerleri = CarSoundProfiles.all.filter { it.id != tir.id }
        val boga = CarSoundProfiles.forShape(CarCatalog.SHAPE_MUSCLE_67)

        assertTrue("hava kornasi en genis araliga sahip olmali",
            digerleri.all { it.hornInterval < tir.hornInterval })
        assertTrue("hava kornasi en uzun otmeli",
            digerleri.all { it.hornSeconds < tir.hornSeconds })
        assertTrue("hava kornasi en yavas atakli olmali (basinc kurar)",
            digerleri.all { it.hornAttack < tir.hornAttack })
        // Kalinlikta ikinci: tac Boga 67'de kaliyor.
        assertTrue("Boga 67'nin kornasi en kalin kalmali", tir.hornBaseHz > boga.hornBaseHz)
        assertTrue("tir katalogun ikinci en kalin kornasi olmali",
            digerleri.filter { it.id != boga.id }.all { it.hornBaseHz > tir.hornBaseHz })
    }

    /**
     * Sahibin istegi (2026-08-17): *"yaris motoru sesi daha tok/bogus/basli
     * olmali baslangic hizlarinda, sonra tiz olmali."* Bu iki arac o istegin
     * hedefi.
     *
     * Test, karartmanin var oldugunu ve YARIS araclarina ozel kaldigini
     * kilitler: baska bir arac bu eksene girerse bilincli bir karar olmali,
     * sessizce degil.
     */
    @Test
    fun `yaris araclari once tok sonra tiz ekseninde ayarli`() {
        val f1 = CarSoundProfiles.forShape(CarCatalog.SHAPE_F1)
        val sedan = CarSoundProfiles.forShape(CarCatalog.SHAPE_RACE_SEDAN)

        assertTrue("F1 dusuk hizda karartilmali", f1.lowSpeedDarken > 0f)
        assertTrue("Yaris Sedan dusuk hizda karartilmali", sedan.lowSpeedDarken > 0f)

        val karartanlar = CarSoundProfiles.all.filter { it.lowSpeedDarken > 0f }
        assertEquals(
            "karartma su an yalnizca iki yaris aracinda — baskasi eklendiyse " +
                "bu testi ve gerekcesini birlikte guncelle",
            setOf(CarCatalog.SHAPE_F1, CarCatalog.SHAPE_RACE_SEDAN),
            karartanlar.map { it.id }.toSet()
        )
    }

    /**
     * Yaris Sedan'in ust harmonikleri 2026-08-17'de buyutuldu, cunku olcum
     * onun tepe hizda katalogun TIRINDAN bile donuk oldugunu gosterdi
     * (300 Hz ustu enerji payi %21.7'ye karsi %25.5). Buyutme sirasinda
     * baskasinin taci alinmadi — test bunu koruyor.
     */
    @Test
    fun `Yaris Sedan zenginlestirildi ama kimseden tac almaz`() {
        val sedan = CarSoundProfiles.forShape(CarCatalog.SHAPE_RACE_SEDAN)
        val f1 = CarSoundProfiles.forShape(CarCatalog.SHAPE_F1)
        val superAraba = CarSoundProfiles.forShape(CarCatalog.SHAPE_SUPERCAR)

        // Ust harmonikler gercekten guclu — "tiz"ligi tasiyan bu.
        assertTrue(
            "yaris sedaninin ust harmonikleri zayif kalmamali",
            sedan.harmonic3 + sedan.harmonic4 + sedan.harmonic5 >= 0.90f
        )
        // F1'in DUZ TARAK imzasi exclusive: sedanin combu belirgin inmeli.
        val h = listOf(sedan.harmonic2, sedan.harmonic3, sedan.harmonic4, sedan.harmonic5)
        assertTrue(
            "F1'in duz tarak imzasi (min/max >= 0.80) exclusive kalmali, olculen $h",
            h.min() < 0.80f * h.max()
        )
        // Ne Boga 67'nin V8 imzasi ne Beety'nin boksor imzasi.
        assertTrue(
            "Boga 67'nin V8 imzasi exclusive kalmali",
            sedan.harmonic3 + sedan.harmonic5 <= 2f * (sedan.harmonic2 + sedan.harmonic4)
        )
        assertTrue(
            "Beety'nin boksor imzasi exclusive kalmali",
            sedan.harmonic2 + sedan.harmonic4 <= 2f * (sedan.harmonic3 + sedan.harmonic5)
        )
        // F1 en az purüzlu kalmali (kendi taci), tizlik taci Super Araba'da.
        assertTrue("en az metalik puruz F1'de kalmali", sedan.grit > f1.grit)
        assertTrue("Super Araba daha tiz kalmali", sedan.freqMul < superAraba.freqMul)
    }

    /**
     * Carpisma rengi ([CarSoundProfile.crashTone]) turetilmis bir deger, elle
     * yazilmiyor — ama sahibin tarifini karsilamasi gerekiyor:
     * *"tirin carpmasi motosikletinkinden tok olmali"*.
     */
    @Test
    fun `carpisma rengi agir araclarda daha pes`() {
        val tir = CarSoundProfiles.forShape(CarCatalog.SHAPE_TIR)
        val moto = CarSoundProfiles.forShape(CarCatalog.SHAPE_MOTOSIKLET)

        assertTrue(
            "tirin carpmasi motosikletinkinden tok olmali " +
                "(${tir.crashTone} vs ${moto.crashTone})",
            tir.crashTone < moto.crashTone
        )
        // En tok carpma tirda: en dusuk freqMul+cutoffMul birlesimi onda.
        assertTrue(
            "en tok carpisma tirda olmali",
            CarSoundProfiles.all.filter { it.id != tir.id }.all { it.crashTone > tir.crashTone }
        )
        // Aralik dar tutulmali: carpisma RENKLENSIN, ayri bir ses olmasin.
        val tonlar = CarSoundProfiles.all.map { it.crashTone }
        assertTrue("carpisma rengi araligi cok genis (${tonlar.min()}..${tonlar.max()})",
            tonlar.max() / tonlar.min() <= 1.8f)
        assertTrue("carpisma rengi sinirlarin disina tasti",
            tonlar.all { it in 0.70f..1.35f })
    }

    @Test
    fun `normalizasyon ham dalgayi bir birimin altinda tutar`() {
        CarSoundProfiles.all.forEach { p ->
            // Tum bilesenler ayni anda tepe yapsa bile:
            val worstCase =
                (1f + p.harmonic2 + p.harmonic3 + p.harmonic4 + p.harmonic5 + p.grit) *
                    p.waveNormalize
            assertTrue(
                "${p.id}: ham motor dalgasi 1'i asiyor ($worstCase)",
                worstCase <= 1f + 1e-4f
            )
            val hornWorst = 2f * (1f + p.hornBuzz * 1.08f) * p.hornNormalize
            assertTrue("${p.id}: ham korna dalgasi 1'i asiyor ($hornWorst)", hornWorst <= 1f + 1e-4f)
        }
    }
}
