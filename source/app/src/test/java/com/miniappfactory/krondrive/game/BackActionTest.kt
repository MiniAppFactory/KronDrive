package com.miniappfactory.krondrive.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Geri tusunun anlamini kilitler.
 *
 * Bu es lesme uc kez bozuldu ve ucunde de sahibi bildirdi. Testler o uc
 * hatanin her birine ayri ayri karsilik geliyor — biri geri gelirse burasi
 * kirmizi yanar.
 */
class BackActionTest {

    private fun act(
        exiting: Boolean = false,
        paused: Boolean = false,
        crash: Boolean = false,
        result: Boolean = false
    ) = backAction(
        exiting = exiting,
        paused = paused,
        crashDialogVisible = crash,
        resultVisible = result
    )

    /**
     * ⚠ CIFT POP (2026-08-19): sonuc ekraninda geri tusuna iki kez basmak iki
     * ayri `popBackStack()` uretiyordu; oyuncu kariyerde bolum haritasini
     * atlayip ana menude buluyordu kendini.
     */
    @Test
    fun `cikis basladiktan sonra geri tusu hicbir sey yapmaz`() {
        assertEquals(BackAction.IGNORE, act(exiting = true, result = true))
        assertEquals(BackAction.IGNORE, act(exiting = true, paused = true))
        assertEquals(BackAction.IGNORE, act(exiting = true, crash = true))
        assertEquals(BackAction.IGNORE, act(exiting = true))
    }

    /**
     * ⚠ GERI SAYIM (2026-08-19): faz kontrolu yuzunden "HAZIR OL" ekraninda
     * geri tusu [BackHandler]'i atliyor ve kosuyu — dolayisiyla ekran
     * acilirken dusulen guclendiricileri — dogrudan yakiyordu.
     */
    @Test
    fun `hicbir perde acik degilken geri tusu DURAKLATIR`() {
        assertEquals(BackAction.PAUSE, act())
    }

    /**
     * ⚠ SONSUZ DONGU (2026-08-18): ikinci basis "devam ettir"iyordu ve oyun
     * ekranindan geri tusuyla cikmanin hicbir yolu yoktu.
     */
    @Test
    fun `duraklatilmisken geri tusu KOSUDAN CIKAR`() {
        assertEquals(BackAction.QUIT_RUN, act(paused = true))
    }

    /**
     * ⚠ SESSIZ SILME (2026-08-19): carpma perdesi `result` daliyla
     * birlestirilmisti ve `finish()`/`publishResult()` hic cagrilmadan
     * cikiliyordu — coin, XP, gorev ilerlemesi ve rekor gidiyordu.
     */
    @Test
    fun `carpma perdesinde geri tusu once KOSUYU KAYDEDER`() {
        assertEquals(BackAction.FINISH_AND_SHOW_RESULT, act(crash = true))
        // Perde sonuc ekranindan ONCE gelir: ikisi birden goruluyorsa bile
        // once kaydetme dali kazanir.
        assertEquals(BackAction.FINISH_AND_SHOW_RESULT, act(crash = true, result = true))
    }

    @Test
    fun `sonuc ekraninda geri tusu reklam kuralindan gecerek cikar`() {
        assertEquals(BackAction.EXIT_WITH_RESULT, act(result = true))
    }

    /**
     * Duraklatma her seyin ustunde (cikis mandali haric): duraklatilmisken
     * arkada bir sonuc kalmis olsa bile geri tusu ÇIKIŞ butonuyla ayni yolu
     * izlemeli, yoksa yine "ayni niyetin iki yolu iki farkli sonuc" olur.
     */
    @Test
    fun `duraklatma sonuctan once gelir`() {
        assertEquals(BackAction.QUIT_RUN, act(paused = true, result = true))
    }

    /** Her kombinasyon bir dala duser — sessizce yutulan durum yok. */
    @Test
    fun `hicbir durum kararsiz kalmaz`() {
        val all = mutableSetOf<BackAction>()
        for (e in listOf(false, true)) {
            for (p in listOf(false, true)) {
                for (c in listOf(false, true)) {
                    for (r in listOf(false, true)) {
                        all += act(exiting = e, paused = p, crash = c, result = r)
                    }
                }
            }
        }
        assertEquals(BackAction.values().toSet(), all)
    }
}

/**
 * Geri sayim sirasinda duraklatma.
 *
 * ⚠ 2026-08-19'da cihazda bulundu: geri tusu ve duraklat tusu "HAZIR OL"
 * ekraninda hicbir sey yapmiyordu. Motor `COUNTDOWN`u duraklatabiliyordu ama
 * ekran kosulu `RUNNING` diye kopyalamisti. Bu test iki tarafi birbirine
 * bagliyor: `pause()` artik duraklattigini SOYLUYOR ve ekran ona bakiyor.
 */
class PauseDuringCountdownTest {

    private fun engine() = GameEngine(mode = RunMode.ENDLESS, random = Random(7)).also {
        it.setViewport(360f, 640f)
    }

    @Test
    fun `geri sayimda duraklatilabilir`() {
        val e = engine()
        assertEquals(RunPhase.COUNTDOWN, e.phase)
        assertTrue("geri sayimda duraklatma reddedildi", e.pause())
        assertEquals(RunPhase.PAUSED, e.phase)
    }

    @Test
    fun `geri sayimdan devam edilince yine geri sayim`() {
        val e = engine()
        e.pause()
        e.resume()
        assertEquals(
            "geri sayim duraklatilip devam edilince kosu ANIDEN baslamamali",
            RunPhase.COUNTDOWN,
            e.phase
        )
    }

    @Test
    fun `surerken duraklatilabilir ve devam edilince surus surer`() {
        val e = engine()
        repeat(300) { e.step(0.016f) }
        assertEquals(RunPhase.RUNNING, e.phase)
        assertTrue(e.pause())
        e.resume()
        assertEquals(RunPhase.RUNNING, e.phase)
    }

    @Test
    fun `zaten duraklatilmisken ikinci duraklatma false doner`() {
        val e = engine()
        assertTrue(e.pause())
        assertFalse("ikinci duraklatma duraklatti sanmamali", e.pause())
    }
}
