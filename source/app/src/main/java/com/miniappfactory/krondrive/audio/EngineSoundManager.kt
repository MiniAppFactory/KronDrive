package com.miniappfactory.krondrive.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.Volatile
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/**
 * Motor ve nitro sesi — calisma aninda sentezlenir, projede tek bayt ses
 * dosyasi yoktur.
 *
 * Prototip tek bir testere dalgasini alcak geciren filtreden geciriyordu;
 * bu native surumun ilk halinde de oyle yapildi ve oyuncu sesin "cok kotu"
 * oldugunu bildirdi (2026-08-13) — hakliydi, tek testere dalgasi motordan cok
 * vizilti gibi duyuluyor. Simdiki model:
 *
 *  - **Silindir vurusu:** ana frekans + 2. ve 3. harmonikler (azalan agirlikla),
 *    yani sinus yiginindan olusan yumusak bir "brum".
 *  - **Puruz:** ana frekansin yarisinda hafif genlik modulasyonu — gercek bir
 *    motorun duzensiz vurus karakteri.
 *  - **Grit:** az miktarda testere dalgasi, sadece tini icin.
 *  - **Alcak geciren filtre:** hiza gore acilan tek kutuplu filtre.
 *
 * Nitro ise gurultu tabanlidir: tetiklendiginde kesme frekansi hizla acilan
 * bir "fisss" + yukari suzulen ince bir islik; boost basili kaldigi surece
 * altta sabit bir hafif tislama kalir.
 */
object EngineSoundManager {

    private const val SAMPLE_RATE = 22050
    private const val BUFFER_FRAMES = 512

    /** Hedeflere yaklasma hizi (ornek basina) — parametre atlarken tik olmaz. */
    private const val SMOOTHING = 0.0015f

    /** Nitro efektinin toplam suresi. */
    private const val NITRO_DURATION = 0.55f

    private var track: AudioTrack? = null
    private var thread: Thread? = null

    @Volatile
    private var running = false

    @Volatile
    private var enabled = true

    @Volatile
    private var targetFreq = 45f

    @Volatile
    private var targetGain = 0f

    @Volatile
    private var targetCutoff = 900f

    /** Boost basili mi — sabit tislama katmani icin. */
    @Volatile
    private var boostActive = false

    /** Nitro efektinde kalan sure (saniye). */
    @Volatile
    private var nitroRemaining = 0f

    fun setEnabled(value: Boolean) {
        enabled = value
        if (!value) {
            targetGain = 0f
            boostActive = false
            nitroRemaining = 0f
        }
    }

    fun isEnabled(): Boolean = enabled

    @Synchronized
    fun start() {
        if (running || !enabled) return
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
        nitroRemaining = 0f
        boostActive = false
    }

    /**
     * Her karede motorun hedef degerlerini gunceller.
     *
     * Frekans araligi bilincli olarak prototipten DUSUK tutuldu (45–150 Hz
     * civari): yuksek frekansli testere dalgasi telefon hoparlorunde ciyaklama
     * gibi duyuluyordu.
     */
    fun update(speed: Float, boosting: Boolean) {
        if (!enabled) return
        targetFreq = 45f + speed * 12f + if (boosting) 14f else 0f
        targetGain = 0.030f + min(0.045f, speed * 0.0055f) + if (boosting) 0.012f else 0f
        targetCutoff = 700f + speed * 130f + if (boosting) 500f else 0f
        boostActive = boosting
    }

    /** Kosu durunca motor sesi bosa duser. */
    fun idle() {
        targetGain = 0f
        boostActive = false
    }

    /** Boost'a basildigi an calan nitro efekti. */
    fun playNitro() {
        if (!enabled) return
        nitroRemaining = NITRO_DURATION
    }

    private fun renderLoop(audioTrack: AudioTrack) {
        val buffer = ShortArray(BUFFER_FRAMES)
        var phase = 0f
        var whistlePhase = 0f
        var freq = targetFreq
        var gain = 0f
        var cutoff = targetCutoff
        var lowpass = 0f
        var noiseLp = 0f
        var hissLevel = 0f
        var noiseState = 0x9E3779B9.toInt()
        val dtPerSample = 1f / SAMPLE_RATE
        val twoPi = 2f * PI.toFloat()

        fun noise(): Float {
            // Xorshift — java.util.Random'dan cok daha ucuz, kalitesi bu is icin fazlasiyla yeterli.
            noiseState = noiseState xor (noiseState shl 13)
            noiseState = noiseState xor (noiseState ushr 17)
            noiseState = noiseState xor (noiseState shl 5)
            return noiseState / Int.MAX_VALUE.toFloat()
        }

        while (running) {
            for (i in 0 until BUFFER_FRAMES) {
                freq += (targetFreq - freq) * SMOOTHING
                gain += (targetGain - gain) * SMOOTHING
                cutoff += (targetCutoff - cutoff) * SMOOTHING

                phase += freq * dtPerSample
                if (phase >= 1f) phase -= 1f

                val angle = phase * twoPi
                // Silindir vurusu: temel + azalan harmonikler + az miktar grit.
                var engine = sin(angle) +
                    0.5f * sin(2f * angle) +
                    0.25f * sin(3f * angle) +
                    0.12f * (phase * 2f - 1f)
                engine *= 0.55f
                // Puruz: yarim frekansta hafif genlik modulasyonu.
                engine *= 0.85f + 0.15f * sin(angle * 0.5f)

                val alpha = 1f - exp(-twoPi * cutoff * dtPerSample)
                lowpass += alpha * (engine - lowpass)

                var sample = lowpass * gain

                // --- Nitro ---
                val wantsHiss = boostActive
                val hissTarget = if (wantsHiss) 1f else 0f
                hissLevel += (hissTarget - hissLevel) * 0.0008f

                if (hissLevel > 0.001f || nitroRemaining > 0f) {
                    val raw = noise()
                    // Gurultuyu tek kutuplu filtreden gecirip yuksek bileseni
                    // ayirmak, duz beyaz gurultudense "hava kacisi" gibi duyulur.
                    val noiseCut = if (nitroRemaining > 0f) {
                        val t = 1f - (nitroRemaining / NITRO_DURATION)
                        400f + 5200f * t
                    } else {
                        2600f
                    }
                    val na = 1f - exp(-twoPi * noiseCut * dtPerSample)
                    noiseLp += na * (raw - noiseLp)
                    val hiss = raw - noiseLp

                    sample += hiss * 0.022f * hissLevel

                    if (nitroRemaining > 0f) {
                        val t = 1f - (nitroRemaining / NITRO_DURATION)
                        // Hizli acilip yavas sonen zarf.
                        val envelope = (t * 6f).coerceAtMost(1f) * (1f - t) * (1f - t)
                        sample += hiss * 0.16f * envelope

                        // Yukari suzulen ince islik.
                        whistlePhase += (700f + 1500f * t) * dtPerSample
                        if (whistlePhase >= 1f) whistlePhase -= 1f
                        sample += sin(whistlePhase * twoPi) * 0.035f * envelope

                        nitroRemaining -= dtPerSample
                        if (nitroRemaining < 0f) nitroRemaining = 0f
                    }
                }

                buffer[i] = (sample.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
            }
            val written = runCatching {
                audioTrack.write(buffer, 0, BUFFER_FRAMES)
            }.getOrDefault(-1)
            if (written < 0) break
        }
    }
}
