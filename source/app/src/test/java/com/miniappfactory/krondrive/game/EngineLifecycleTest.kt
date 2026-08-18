package com.miniappfactory.krondrive.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * MOTORUN SINIR DURUMLARI (2026-08-19, QA bosluk taramasi).
 *
 * `GameEngineTest` oynanis kurallarini (hiz, carpisma, hedef, yildiz)
 * dogruluyor. Burada dogrulanan sey oynanis degil, **motorun kotu bir dis
 * dunyaya dayanikliligi**: dev bir kare suresi, arka plandan donus, yanlis
 * fazda cagrilan `revive`/`finish`, geri sayimda duraklatma.
 *
 * Bunlarin ortak ozelligi hicbirinin "build gecti" ile gorunmemesi — hepsi
 * cihazda, oyuncunun elinde ortaya cikar.
 */
class EngineLifecycleTest {

    private val dt = 0.016f
    private val genislik = 1080f
    private val yukseklik = 1920f

    private fun motor(mode: RunMode = RunMode.ENDLESS, seed: Int = 42): GameEngine {
        val e = GameEngine(mode = mode, random = Random(seed))
        e.setViewport(genislik, yukseklik)
        return e
    }

    private fun GameEngine.isit(kare: Int) {
        var guard = 0
        while (phase == RunPhase.COUNTDOWN && guard++ < 1000) step(dt)
        repeat(kare) { step(dt) }
    }

    // -----------------------------------------------------------------
    // Kare suresi
    // -----------------------------------------------------------------

    /**
     * ARKA PLANDAN DONUS.
     *
     * Uygulama arka plana alinip geri gelince `withFrameNanos` iki kare
     * arasinda saniyeler gecmis olarak doner. O kare oldugu gibi islenseydi
     * arac bir karede ekranlar boyu yol alir, trafigin icinden gecer ve
     * gecmisteki bir konumda "carpismadan" cikardi.
     *
     * [GameConfig.MAX_FRAME_DT] tam olarak bunun icin var. Test iki ayni
     * motoru ayni tohumla surup birine dev bir kare, digerine tavan kare
     * veriyor: ikisi BIREBIR ayni sonucu vermeli.
     */
    @Test
    fun `arka plandan donen dev kare tavan kare kadar islenir`() {
        val dev = motor(seed = 7).apply { isit(200) }
        val tavan = motor(seed = 7).apply { isit(200) }

        dev.step(10f)
        tavan.step(GameConfig.MAX_FRAME_DT)

        assertEquals(
            "10 saniyelik kare tavan kareden farkli mesafe uretti — kirpma calismiyor",
            tavan.distanceMeters(), dev.distanceMeters()
        )
        assertEquals(
            "10 saniyelik kare tavan kareden farkli hiz uretti",
            tavan.speed, dev.speed, 1e-4f
        )
    }

    /**
     * Negatif kare suresi dunyayi GERI SARMAMALI. Sistem saati geri
     * alindiginda ya da bir olcum hatasi oldugunda negatif fark gelebilir;
     * o kare islenmemis sayilmali.
     */
    @Test
    fun `negatif kare suresi dunyayi geri sarmaz`() {
        val e = motor(seed = 11).apply { isit(200) }
        val mesafe = e.distanceMeters()
        val hiz = e.speed

        e.step(-1f)

        assertEquals("negatif kare mesafeyi degistirdi", mesafe, e.distanceMeters())
        assertEquals("negatif kare hizi degistirdi", hiz, e.speed, 1e-4f)
        assertEquals("negatif kare fazi degistirdi", RunPhase.RUNNING, e.phase)
    }

    /**
     * TUNEL ETKISI — EN AGIR DEGISMEZ.
     *
     * Carpisma tespiti surekli degil, kare kare yapiliyor: bir engel her
     * karede `(oyuncuHizi - engelHizi) * WORLD_PX_PER_SPEED_UNIT * dt` kadar
     * asagi atliyor ve carpisma yalnizca DIKEY ORTUSME oldugu kare test
     * ediliyor. Adim, ortusme penceresinden buyurse arac oyuncunun ICINDEN
     * GECER ve hicbir sey olmaz.
     *
     * Pencere = oyuncunun kutu boyu + engelin kutu boyu.
     *
     * Neden simdi onemli: iki sabit son gunlerde ayri ayri buyudu —
     * `MAX_FRAME_DT` 0.032'den 0.050'ye cikti (kaybolan hareket duzeltmesi)
     * ve arac hiz carpanlari 2026-08-18'de %11'den %108'e acildi. Ikisi de
     * tek basina makul, CARPIMLARI ise tam olarak bu penceredir ve hicbir
     * test ikisini bir arada olcmuyordu.
     *
     * Olculen en kotu durum (2026-08-19): en hizli arac (F1, tam yukseltme,
     * sonsuz mod tavan carpani, boost basili) en yavas trafigi gecerken
     * pencerenin **%57'si** kadar yol aliyor. Yani pay var ama sinirsiz
     * degil; bu test payin tukendigi gun kirilir.
     */
    @Test
    fun `en hizli arac tek karede carpisma penceresini atlayamaz`() {
        val enHizliGovde = CarCatalog.shapes.maxByOrNull { it.topSpeedMul }!!

        // GameEngine.updateSpeed ile ayni hesap: taban + rampa * tavan *
        // sonsuz mod carpani, uzerine boost katkisi.
        val enYuksekHiz = GameConfig.BASE_SPEED +
            UpgradeCatalog.scoreSpeedCap(UpgradeCatalog.MAX_LEVEL, enHizliGovde) *
            GameConfig.ENDLESS_SPEED_MAX_MULTIPLIER +
            UpgradeCatalog.boostSpeedBonus(UpgradeCatalog.MAX_LEVEL)

        // En buyuk yaklasma hizi = en hizli oyuncu, en yavas trafik.
        val enYavasTrafik = GameConfig.BASE_SPEED * GameConfig.TRAFFIC_SPEED_RATIO_MIN
        val karedekiYol = (enYuksekHiz - enYavasTrafik) *
            GameConfig.WORLD_PX_PER_SPEED_UNIT * GameConfig.MAX_FRAME_DT

        VehicleClass.entries.forEach { oyuncuSinifi ->
            VehicleClass.entries.forEach { engelSinifi ->
                val pencere = oyuncuSinifi.hitboxHeightPx + engelSinifi.hitboxHeightPx
                assertTrue(
                    "$oyuncuSinifi oyuncu / $engelSinifi engel: tek karede $karedekiYol px " +
                        "yol aliniyor ama carpisma penceresi $pencere px — arac icinden " +
                        "gecebilir. MAX_FRAME_DT ya da hiz tavani fazla buyudu.",
                    karedekiYol < pencere
                )
            }
        }
    }

    // -----------------------------------------------------------------
    // Faz gecisleri
    // -----------------------------------------------------------------

    /**
     * GERI SAYIMDA DURAKLATMA.
     *
     * `pause()` hem RUNNING hem COUNTDOWN fazindan calisiyor ve `resume()`
     * **kaldigi faza** donmeli. Geri sayimda duraklatip devam eden oyuncu
     * dogrudan yola cikarsa, hazir olmadan ve elini surmeden hareket eden bir
     * araba bulur. `resumePhase` tam bunun icin tutuluyor; bu test onu
     * kilitliyor.
     */
    @Test
    fun `geri sayimda duraklatip devam etmek kosuyu erken baslatmaz`() {
        val e = motor(seed = 5)
        assertEquals("motor geri sayimla baslamali", RunPhase.COUNTDOWN, e.phase)

        e.pause()
        assertEquals(RunPhase.PAUSED, e.phase)

        e.resume()
        assertEquals(
            "geri sayimda duraklatilan kosu devam edince YINE geri sayimda olmali",
            RunPhase.COUNTDOWN, e.phase
        )
    }

    /**
     * Duraklatilmis kosuda fizik islememeli. `pause()` ayrica gaz ve fren
     * girdilerini de birakiyor — parmak ekrandayken duraklatan oyuncu geri
     * dondugunde boost basili kalmis olmamali.
     */
    @Test
    fun `duraklatilmis kosuda dunya ilerlemez`() {
        val e = motor(seed = 13).apply { isit(200) }
        e.setBoost(true)
        e.step(dt)

        e.pause()
        val mesafe = e.distanceMeters()
        val hiz = e.speed
        repeat(100) { e.step(dt) }

        assertEquals("duraklatilmis kosuda mesafe ilerledi", mesafe, e.distanceMeters())
        assertEquals("duraklatilmis kosuda hiz degisti", hiz, e.speed, 1e-4f)
    }

    /**
     * `revive()` YALNIZCA carpisma fazinda calismali. Yanlis fazda cagrilirsa
     * (arayuzde bir yaris durumu, cift dokunus, geciken reklam geri cagrisi)
     * sessizce yok sayilmali — yoksa oyuncu carpmadan yolu temizletip
     * dokunulmazlik kazanirdi.
     */
    @Test
    fun `revive yalnizca carpisma fazinda calisir`() {
        val e = motor(seed = 17).apply { isit(200) }
        assertEquals(RunPhase.RUNNING, e.phase)
        assertFalse("carpmadan revive teklif edilmemeli", e.canRevive())

        val obstaculSayisi = e.obstacles.size
        e.revive()

        assertEquals("RUNNING fazinda revive fazi degistirdi", RunPhase.RUNNING, e.phase)
        assertFalse("RUNNING fazinda revive dokunulmazlik verdi", e.isInvulnerable())
        assertEquals(
            "RUNNING fazinda revive yolu temizledi — bedava tehlike temizligi",
            obstaculSayisi, e.obstacles.size
        )
    }

    // -----------------------------------------------------------------
    // Bitis
    // -----------------------------------------------------------------

    /**
     * BITEN KOSU IKINCI KEZ ODUL VERMEZ.
     *
     * `finish()` disaridan da cagrilabiliyor (oyuncu "devam" teklifini
     * reddedince arayuz cagiriyor). Iki kez cagrilirsa ikinci cagri yeni bir
     * [RunResult] uretmemeli: `KronViewModel.onRunFinished` her sonuc icin
     * coin/XP yaziyor, yani ikinci bir sonuc dogrudan ikinci bir odeme olurdu.
     *
     * Kosu, odeme esigini ([GameConfig.MIN_PAID_RUN_SECONDS]) gececek kadar
     * uzun surduruluyor — aksi halde odul zaten 0 olur ve test hicbir sey
     * kanitlamazdi.
     */
    @Test
    fun `finish iki kez cagrilirsa odul iki kez verilmez`() {
        val e = motor(seed = 23)
        e.isit(kare = (GameConfig.MIN_PAID_RUN_SECONDS / dt).toInt() + 200)

        e.finish(completed = false)
        val ilkSonuc = e.lastResult
        assertNotNull("ilk finish sonuc uretmeliydi", ilkSonuc)
        assertTrue(
            "olcum anlamli olsun diye kosu odul esigini gecmeliydi",
            ilkSonuc!!.stats.timeSurvivedSec >= GameConfig.MIN_PAID_RUN_SECONDS
        )

        // Ilk finish'in urettigi olay bir sonraki step'te bosaltilir.
        val ilkOlaylar = e.step(dt)
        assertEquals(
            "ilk finish tam bir Finished olayi uretmeli",
            1, ilkOlaylar.count { it is GameEvent.Finished }
        )

        e.finish(completed = true)
        val ikinciOlaylar = e.step(dt)

        assertSame("ikinci finish yeni bir sonuc uretti", ilkSonuc, e.lastResult)
        assertEquals(
            "ikinci finish yeni bir Finished olayi uretti — odul iki kez yazilirdi",
            0, ikinciOlaylar.count { it is GameEvent.Finished }
        )
    }

    /** Biten kosuda `step` cagrilmaya devam etse bile dunya ilerlemez. */
    @Test
    fun `biten kosuda step dunyayi ilerletmez`() {
        val e = motor(seed = 29).apply { isit(300) }
        e.finish(completed = false)

        val mesafe = e.distanceMeters()
        repeat(200) { e.step(dt) }

        assertEquals("biten kosuda mesafe ilerledi", mesafe, e.distanceMeters())
        assertEquals(RunPhase.FINISHED, e.phase)
    }

    // -----------------------------------------------------------------
    // Hiz kilidi
    // -----------------------------------------------------------------

    /**
     * HIZ KILIDI "DONDURMA" DEGIL.
     *
     * Kilidin belgelenmis anlami: *"hizini sen yonet"* — skordan gelen
     * hizlanma ve sonsuz mod rampasi durur ama **boost ve fren calismaya
     * devam eder**. Mevcut testler kilidin hizi sabitledigini dogruluyor;
     * hicbiri boost/frenin hala calistigini dogrulamiyordu.
     *
     * Bu bosluk onemli cunku "kilit hizi sabitlesin" diye yapilacak bir
     * sadelestirme sonsuz modu duz bir yola cevirir ve butun testler yesil
     * kalirdi: oyuncu kilit acikken bir engelden kacamaz hale gelir.
     */
    @Test
    fun `hiz kilidi acikken boost ve fren hala calisir`() {
        val e = motor(seed = 31).apply { isit(400) }
        e.toggleSpeedLock()
        repeat(60) { e.step(dt) }
        val kilitliHiz = e.speed

        e.setBoost(true)
        repeat(30) { e.step(dt) }
        val boostluHiz = e.speed
        e.setBoost(false)
        repeat(150) { e.step(dt) }

        e.setBrake(true)
        repeat(60) { e.step(dt) }
        val frenliHiz = e.speed

        assertTrue(
            "kilit acikken boost hizi artirmadi ($kilitliHiz -> $boostluHiz) — " +
                "kilit 'dondurma'ya donusmus",
            boostluHiz > kilitliHiz + 0.1f
        )
        assertTrue(
            "kilit acikken fren hizi dusurmedi ($kilitliHiz -> $frenliHiz)",
            frenliHiz < kilitliHiz - 0.1f
        )
        assertTrue(
            "fren hizi MIN_SPEED altina dusurdu",
            frenliHiz >= GameConfig.MIN_SPEED - 1e-3f
        )
    }
}
