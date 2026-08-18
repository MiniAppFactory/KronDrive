package com.miniappfactory.krondrive.game

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MAGAZA METNI OYUNLA UYUSMALI.
 *
 * ## Neden
 *
 * Magaza aciklamasi oyunun somut sayilarini yaziyor ("son bolum 120 saniyede
 * N metre ister"). Denge degistiginde o cumle sessizce eskiyor ve kimse fark
 * etmiyor — 2026-08-19 denetiminde bunun **altinci tekrari** sayildi.
 *
 * Son ornek: 2026-08-18'de mesafe hedefleri x0.75 olceklendi
 * (`ReachDistance` 5000 -> 3800) ama iki magaza metni de "5.000 metre"
 * demeye devam etti. Yayina o hâliyle cikilsa magaza METADATA'SI yanlis
 * olurdu; Play tarafinda yaniltici aciklama sayilir.
 *
 * Bu test kod ile metni birbirine baglar: sayi degisirse test kirilir ve
 * metni guncellemek zorunda kalirsin.
 *
 * ## Dosyayi nasil buluyor
 *
 * Gradle'in calisma dizini modul dizini (`source/app`) olabiliyor; bu yuzden
 * dosya yukari dogru yuruyerek araniyor. Bulunamazsa test KIRILIR — sessizce
 * atlanan bir bekci, olmayan bekciden daha kotudur (yesil gorunur, korumaz).
 */
class StoreTextMatchesGameTest {

    private fun bul(gorecelYol: String): File {
        var dizin: File? = File("").absoluteFile
        repeat(6) {
            val aday = File(dizin, gorecelYol)
            if (aday.isFile) return aday
            dizin = dizin?.parentFile
        }
        error(
            "magaza metni bulunamadi: $gorecelYol (baslangic: ${File("").absolutePath})"
        )
    }

    private val sonBolum = LevelCatalog.levels.last()

    @Test
    fun `magaza metni son bolumun mesafesini dogru yaziyor`() {
        val hedef = sonBolum.goal
        assertTrue(
            "son bolumun hedefi ReachDistance degil (${hedef}); bu test o " +
                "varsayima dayaniyor, metin cumlesi de oyle",
            hedef is LevelGoal.ReachDistance
        )
        val mesafe = (hedef as LevelGoal.ReachDistance).meters
        val sure = hedef.timeLimitSec

        // TR: "3.800" — binlik ayirici nokta. EN: "3,800" — virgul.
        val tr = bul("docs/play_store_assets/store_long_description_tr.txt").readText()
        val en = bul("docs/play_store_assets/store_long_description_en.txt").readText()
        val trSayi = "%,d".format(mesafe).replace(",", ".")
        val enSayi = "%,d".format(mesafe)

        assertTrue(
            "TR magaza metni son bolum icin '$trSayi metre' demiyor " +
                "(kodda $mesafe m / $sure sn)",
            tr.contains("$trSayi metre")
        )
        assertTrue(
            "EN magaza metni son bolum icin '$enSayi metres' demiyor " +
                "(kodda $mesafe m / $sure sn)",
            en.contains("$enSayi metres")
        )
        assertTrue("TR metin sureyi yanlis yaziyor", tr.contains("$sure saniye"))
        assertTrue("EN metin sureyi yanlis yaziyor", en.contains("$sure seconds"))
    }
}
