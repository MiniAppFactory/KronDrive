package com.miniappfactory.krondrive.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.Volatile

/**
 * Motor, nitro, korna, carpisma ve geri sayim sesi — calisma aninda
 * sentezlenir, projede tek bayt ses dosyasi yoktur (bkz. `PROVENANCE.md`,
 * "Ses varliklari"). Carpisma ve geri sayim da bu kuralin icinde: APK'ya
 * 0 bayt eklerler.
 *
 * Bu sinif **yalnizca Android koprusu**: bir `AudioTrack` acar, ayri bir
 * thread'de [EngineVoice]'u dondurur ve ciktiyi 16-bit PCM'e cevirir.
 * Sentezin tamami ve arac basina ses kimligi [EngineVoice] ile
 * [CarSoundProfile] icindedir; ikisi de saf Kotlin oldugu icin JVM
 * testleriyle dogrulanabiliyor (bu makinede hoparlor yok).
 *
 * Ses UI thread'inde uretilmez ve oyun dongusunu bekletmez: ekran yalnizca
 * [update]/[playNitro]/[playHorn] ile birkac `volatile` alan yazar, ornekleri
 * uretme isi tamamen ses thread'inde olur.
 */
object EngineSoundManager {

    private const val SAMPLE_RATE = EngineVoice.DEFAULT_SAMPLE_RATE
    private const val BUFFER_FRAMES = 512

    private val voice = EngineVoice(SAMPLE_RATE)

    private var track: AudioTrack? = null
    private var thread: Thread? = null

    @Volatile
    private var running = false

    /** Oyuncunun sectigi gövdeye gore ses kimligi. */
    fun setProfile(shapeId: String?) = voice.setProfile(shapeId)

    fun setEnabled(value: Boolean) = voice.setEnabled(value)

    fun isEnabled(): Boolean = voice.isEnabled()

    @Synchronized
    fun start() {
        if (running || !voice.isEnabled()) return
        val minBuffer = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) return

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(minBuffer, BUFFER_FRAMES * 4))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        if (audioTrack.state != AudioTrack.STATE_INITIALIZED) {
            audioTrack.release()
            return
        }

        track = audioTrack
        running = true
        audioTrack.play()
        thread = Thread({ renderLoop(audioTrack) }, "KronEngineSound").apply {
            priority = Thread.NORM_PRIORITY + 1
            start()
        }
    }

    @Synchronized
    fun stop() {
        running = false
        thread?.join(250)
        thread = null
        track?.let { t ->
            runCatching { t.stop() }
            t.release()
        }
        track = null
        voice.reset()
    }

    /** Her karede motorun hedef degerlerini gunceller. */
    fun update(speed: Float, boosting: Boolean) = voice.update(speed, boosting)

    /** Kosu durunca motor sesi bosa duser. */
    fun idle() = voice.idle()

    /** Boost'a basildigi an calan nitro efekti. */
    fun playNitro() = voice.playNitro()

    /**
     * Korna — oynanisa etkisi yok. Ses kapaliyken ya da bekleme suresi
     * dolmadan cagrilirsa `false` doner (bkz. [EngineVoice.playHorn]).
     */
    fun playHorn(): Boolean = voice.playHorn()

    /**
     * Carpisma sesi — oyuncunun oldugu an calinir.
     *
     * ⚠ **Bu cagri su an hicbir yerden yapilmiyor.** API bilerek hazir
     * birakildi; oyun ekranina baglama isi ayri bir degisiklik
     * (`ui/game/GameScreen.kt` bu degisiklikte ELLENMEDI).
     *
     * Bagladiktan sonra dikkat edilecek tek sey sudur: [stop] cagrildiginda
     * ses thread'i biter, yani carpismadan HEMEN sonra ses motoru
     * durdurulursa efekt duyulmadan kesilir. Sesin tamamlanmasi icin
     * [EngineVoice.CRASH_DURATION] kadar (0.46 sn) beklemek gerekir.
     */
    fun playCrash() = voice.playCrash()

    /**
     * Geri sayim bipi — kosu basindaki 3 → 2 → 1 → BASLA dizisi.
     *
     * `ui/game/GameScreen.kt` bunu **rakam degistiginde bir kez** cagirir
     * (kare basina degil); son cagri [final] = `true` ile, rakamin 0'a
     * dustugu — yani kosunun basladigi — karede yapilir.
     *
     * Ses ayari kapaliyken sessizdir: [setEnabled] zaten [EngineVoice]
     * uzerinde tetikleyicileri kapatiyor, cagiran tarafta ayrica kontrol
     * gerekmez (motorun kendisi de boyle calisiyor).
     *
     * ⚠ [stop] cagrildiginda ses thread'i biter, yani son bip
     * [EngineVoice.COUNTDOWN_GO_SECONDS] (0.5 sn) dolmadan ses motoru
     * durdurulursa efekt duyulmadan kesilir. Pratikte bu yalnizca oyuncunun
     * kosu baslar baslamaz uygulamayi arka plana almasi demektir.
     */
    fun playCountdownBeep(final: Boolean) = voice.playCountdownBeep(final)

    private fun renderLoop(audioTrack: AudioTrack) {
        val samples = FloatArray(BUFFER_FRAMES)
        val buffer = ShortArray(BUFFER_FRAMES)
        while (running) {
            voice.render(samples)
            for (i in 0 until BUFFER_FRAMES) {
                buffer[i] = (samples[i] * Short.MAX_VALUE).toInt().toShort()
            }
            val written = runCatching {
                audioTrack.write(buffer, 0, BUFFER_FRAMES)
            }.getOrDefault(-1)
            if (written < 0) break
        }
    }
}
