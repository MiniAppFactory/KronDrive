package com.miniappfactory.krondrive.data

import com.miniappfactory.krondrive.game.CarCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Kayit kodlamasinin dogrulugu ve KIMLIK SOZLUGU.
 *
 * 2026-08-19'a kadar bu mantik [GameStateRepository]'nin icinde `private`
 * oldugu ve o sinif `Context` istedigi icin **hic test edilemiyordu** — yani
 * oyuncunun kalici verisini yazip okuyan kod tamamen kapsam disindaydi.
 * [SaveCodec]'e tasindiktan sonra yazildi.
 *
 * En kritik madde asagidaki `kimliklerde ayirici karakter yok` testi:
 * format `anahtar:deger,anahtar:deger` ve **kacis karakteri yok**. Iceren tek
 * bir arac/boya/booster kimligi o girdiyi sessizce dusurur; oyuncu bir daha
 * acildiginda sahip oldugu iceriklerin bir kismini kaybeder ve hicbir hata
 * gorunmez.
 */
class SaveCodecTest {

    // --- Gidis-donus ---

    @Test
    fun `string set gidis donus`() {
        val set = setOf("a", "b", "c")
        assertEquals(set, SaveCodec.decodeStringSet(set.joinToString(",")))
    }

    @Test
    fun `string int map gidis donus`() {
        val map = mapOf("gorev_a" to 3, "gorev_b" to 0, "gorev_c" to 120)
        assertEquals(map, SaveCodec.decodeStringIntMap(SaveCodec.encodeStringIntMap(map)))
    }

    @Test
    fun `booster map gidis donus ve sifirlari atma`() {
        val map = mapOf(BoosterType.entries.first() to 2)
        assertEquals(map, SaveCodec.decodeBoosterMap(SaveCodec.encodeBoosterMap(map)))
        // Sifir ve negatif YAZILMAZ: bos satir saklamanin anlami yok.
        val sifirli = BoosterType.entries.associateWith { 0 }
        assertEquals("", SaveCodec.encodeBoosterMap(sifirli))
        assertEquals(emptyMap<BoosterType, Int>(), SaveCodec.decodeBoosterMap(""))
    }

    @Test
    fun `int map ve string map gidis donus`() {
        val i = mapOf(1 to 3, 7 to 0)
        assertEquals(i, SaveCodec.decodeIntMap(SaveCodec.encodeIntMap(i)))
        val s = mapOf("govde" to "renk", "x" to "y")
        assertEquals(s, SaveCodec.decodeStringMap(SaveCodec.encodeStringMap(s)))
    }

    // --- Bozuk girdi oyunu kirmamali ---

    @Test
    fun `bozuk girdi cokmez ve saglam girdileri korur`() {
        assertEquals(emptyMap<String, Int>(), SaveCodec.decodeStringIntMap(null))
        assertEquals(emptySet<String>(), SaveCodec.decodeStringSet("   "))
        // Yarim/bozuk girdiler ATILIR, saglamlar KALIR — kayit bozulmasi
        // oyunu kirmasin diye. "gorev_b" sayiya cevrilemedigi icin duser.
        assertEquals(
            mapOf("gorev_a" to 3),
            SaveCodec.decodeStringIntMap("gorev_a:3,gorev_b:abc,bozuk,:,")
        )
        assertEquals(
            emptyMap<BoosterType, Int>(),
            SaveCodec.decodeBoosterMap("YOK_BOYLE_BIR_TUR:5")
        )
    }

    @Test
    fun `negatif ve buyuk sayilar korunur`() {
        // Negatif deger formatin kendisi icin gecerli; anlam kontrolu
        // cagiranin isi (bkz. GameStateRepository guard'lari).
        assertEquals(mapOf("a" to -5), SaveCodec.decodeStringIntMap("a:-5"))
        assertEquals(mapOf("b" to Int.MAX_VALUE), SaveCodec.decodeStringIntMap("b:${Int.MAX_VALUE}"))
        // Tasan sayi cozumlenemez -> girdi duser, cokme yok.
        assertEquals(emptyMap<String, Int>(), SaveCodec.decodeStringIntMap("c:99999999999999"))
    }

    // --- ASIL KORUMA: kimlik sozlugu ---

    @Test
    fun `kimliklerde ayirici karakter yok`() {
        val yasak = listOf(",", ":")
        val kimlikler = buildList {
            CarCatalog.shapes.forEach { add("govde ${it.id}" to it.id) }
            CarCatalog.colors.forEach { add("boya ${it.id}" to it.id) }
            BoosterType.entries.forEach { add("booster ${it.name}" to it.name) }
        }
        kimlikler.forEach { (etiket, id) ->
            yasak.forEach { k ->
                assertTrue(
                    "$etiket kimliginde '$k' var — kayit formatinda kacis karakteri yok, " +
                        "bu kimlik cozumlenirken SESSIZCE dusulur ve oyuncu o icerigi kaybeder",
                    !id.contains(k)
                )
            }
            assertTrue("$etiket kimligi bos", id.isNotBlank())
        }
    }

    /**
     * Bir gövde kimliği bir digerinin ONEKI olmamali degil — format tam
     * esleme yapiyor — ama kimlikler TEKIL olmali; ayni kimlikten iki icerik
     * varsa biri otekinin sahipligini devralir.
     */
    @Test
    fun `kimlikler tekil`() {
        val govde = CarCatalog.shapes.map { it.id }
        assertEquals("tekrarli govde kimligi var", govde.size, govde.toSet().size)
        val boya = CarCatalog.colors.map { it.id }
        assertEquals("tekrarli boya kimligi var", boya.size, boya.toSet().size)
    }
}
