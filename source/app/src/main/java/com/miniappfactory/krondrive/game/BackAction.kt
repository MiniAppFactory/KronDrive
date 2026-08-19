package com.miniappfactory.krondrive.game

/**
 * Oyun ekraninda sistem geri tusunun ne yapacagi.
 *
 * Kendi dosyasinda ve saf Kotlin: karar Compose'un icinde `when` olarak
 * yasadigi surece JVM testiyle kilitlenemiyordu ve ayni hata UC KEZ geri
 * geldi — 2026-08-16 (kosu sessizce siliniyordu), 2026-08-18 (duraklat/devam
 * sonsuz dongusu), 2026-08-19 (carpma perdesi sonucu yayimlamiyordu, sonra
 * cift pop ana menuye atiyordu). Artik es lesme burada ve testlerde.
 */
enum class BackAction {
    /** Hicbir sey yapma — cikis zaten baslamis, ikinci basis yutulur. */
    IGNORE,

    /** Kosuyu duraklat (geri sayim dahil). */
    PAUSE,

    /** Kosuyu bitir, sonucu yayimla ve sonuc ekranini goster. */
    FINISH_AND_SHOW_RESULT,

    /** Sonuc ekranindan cik: reklam kurali isler, sonra ekran kapanir. */
    EXIT_WITH_RESULT,

    /** Duraklatilmisken cikis: kosuyu bitirir, yayimlar ve ekrani kapatir. */
    QUIT_RUN
}

/**
 * Geri tusunun anlamini belirler.
 *
 * ⚠ SIRA ONEMLI. [exiting] en ustte: cikis bir kez baslayinca ekranin geri
 * kalan durumu artik onemsizdir. Bu sira olmadan sonuc ekraninda geri tusuna
 * iki kez basmak IKI ayri `popBackStack()` uretiyordu ve oyuncu kariyerde
 * bolum haritasini atlayip ana menude buluyordu kendini (sahibi bildirdi,
 * 2026-08-19).
 *
 * [phase] burada BILEREK yer almiyor: geri sayim, surus ve "cikamayan" her
 * durum ayni dala ([BackAction.PAUSE]) duser. Eskiden faz kontrolu vardi ve
 * `COUNTDOWN` hicbir dala girmedigi icin geri tusu ekrani dogrudan
 * kapatiyordu — secili guclendiriciler oyun hic baslamadan yaniyordu.
 */
fun backAction(
    exiting: Boolean,
    paused: Boolean,
    crashDialogVisible: Boolean,
    resultVisible: Boolean
): BackAction = when {
    exiting -> BackAction.IGNORE
    paused -> BackAction.QUIT_RUN
    // Carpma perdesi sonuc ekranindan ONCE gelir: VAZGEC ile ayni sey yapilir,
    // yani kosu KAYDEDILIR. Dogrudan cikilmaz — oyuncu once sonucunu gorur.
    crashDialogVisible -> BackAction.FINISH_AND_SHOW_RESULT
    resultVisible -> BackAction.EXIT_WITH_RESULT
    else -> BackAction.PAUSE
}
