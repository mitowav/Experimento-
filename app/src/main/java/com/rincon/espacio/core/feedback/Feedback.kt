package com.rincon.espacio.core.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RawRes
import androidx.compose.runtime.staticCompositionLocalOf
import com.rincon.espacio.R

/** Intensidad global de los efectos de sonido. */
enum class SoundIntensity(val label: String, val gain: Float) {
    Low("Suave", 0.28f),
    Medium("Media", 0.55f),
    High("Alta", 0.9f);

    companion object {
        fun fromKey(key: String?): SoundIntensity = entries.firstOrNull { it.name == key } ?: Low
    }
}

/** Fuerza del retorno háptico. */
enum class HapticStrength { Light, Medium, Strong }

/**
 * Un único punto de entrada para "cómo se siente" tocar la app.
 *
 * La UI nunca habla con [Vibrator] ni con [SoundPool] directamente: pide un
 * gesto semántico (`tap`, `pop`, `settle`...) y esta capa decide qué sonido y
 * qué vibración corresponde, respetando los ajustes de la persona usuaria.
 */
interface Feedback {
    fun tap()
    fun pop()
    fun paper()
    fun settle()
    fun complete()
    fun goalReached()
    fun chime()
    fun toggle()
    fun warn()
    /** Elegir una opción: un "toc" seco y corto. */
    fun click()
    /** Romper algo: papel rasgándose. */
    fun tear()
    /** Escuchar un tono de aviso antes de elegirlo. */
    fun previewTone(rawRes: Int)
}

/** Implementación inerte: útil en previews y tests. */
object NoFeedback : Feedback {
    override fun tap() = Unit
    override fun pop() = Unit
    override fun paper() = Unit
    override fun settle() = Unit
    override fun complete() = Unit
    override fun goalReached() = Unit
    override fun chime() = Unit
    override fun toggle() = Unit
    override fun warn() = Unit
    override fun click() = Unit
    override fun tear() = Unit
    override fun previewTone(rawRes: Int) = Unit
}

val LocalFeedback = staticCompositionLocalOf<Feedback> { NoFeedback }

class FeedbackController(context: Context) : Feedback {

    private val appContext = context.applicationContext

    @Volatile var soundEnabled: Boolean = true
    @Volatile var hapticsEnabled: Boolean = true
    @Volatile var intensity: SoundIntensity = SoundIntensity.Low

    private val vibrator: Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = appContext.getSystemService(VibratorManager::class.java)
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }.getOrNull()

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val loaded = HashMap<Int, Int>()
    private var ready = false

    init {
        soundPool.setOnLoadCompleteListener { _, _, _ -> ready = true }
        listOf(
            R.raw.sfx_tap, R.raw.sfx_pop, R.raw.sfx_paper,
            R.raw.sfx_success, R.raw.sfx_reminder, R.raw.sfx_goal,
            R.raw.sfx_click, R.raw.sfx_tear,
        ).forEach { res -> loaded[res] = soundPool.load(appContext, res, 1) }
    }

    private fun play(@RawRes res: Int, volumeScale: Float = 1f, rate: Float = 1f) {
        if (!soundEnabled || !ready) return
        val id = loaded[res] ?: return
        val v = (intensity.gain * volumeScale).coerceIn(0f, 1f)
        if (v <= 0.01f) return
        soundPool.play(id, v, v, 0, 0, rate)
    }

    private fun vibrate(strength: HapticStrength) {
        if (!hapticsEnabled) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        val (ms, amp) = when (strength) {
            HapticStrength.Light -> 8L to 55
            HapticStrength.Medium -> 14L to 110
            HapticStrength.Strong -> 20L to 175
        }
        runCatching { v.vibrate(VibrationEffect.createOneShot(ms, amp)) }
    }

    private fun vibratePattern(timings: LongArray, amplitudes: IntArray) {
        if (!hapticsEnabled) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        runCatching { v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1)) }
    }

    override fun tap() {
        play(R.raw.sfx_tap, 0.7f)
        vibrate(HapticStrength.Light)
    }

    override fun pop() {
        play(R.raw.sfx_pop)
        vibrate(HapticStrength.Light)
    }

    override fun paper() {
        play(R.raw.sfx_paper, 0.75f, rate = 0.95f + Math.random().toFloat() * 0.1f)
        vibrate(HapticStrength.Light)
    }

    override fun settle() {
        play(R.raw.sfx_paper, 0.55f, rate = 0.85f)
        vibrate(HapticStrength.Light)
    }

    override fun complete() {
        play(R.raw.sfx_success)
        vibratePattern(longArrayOf(0, 10, 40, 16), intArrayOf(0, 80, 0, 130))
    }

    override fun goalReached() {
        play(R.raw.sfx_goal)
        vibratePattern(longArrayOf(0, 12, 50, 12, 50, 22), intArrayOf(0, 90, 0, 110, 0, 170))
    }

    override fun chime() {
        play(R.raw.sfx_reminder, 0.8f)
        vibrate(HapticStrength.Medium)
    }

    override fun toggle() {
        play(R.raw.sfx_tap, 0.5f, rate = 1.14f)
        vibrate(HapticStrength.Light)
    }

    override fun warn() {
        play(R.raw.sfx_tap, 0.6f, rate = 0.8f)
        vibrate(HapticStrength.Medium)
    }

    override fun click() {
        // Ligera variación de tono para que repetir la acción no suene a bucle.
        play(R.raw.sfx_click, 0.95f, rate = 0.97f + Math.random().toFloat() * 0.07f)
        vibrate(HapticStrength.Light)
    }

    override fun tear() {
        play(R.raw.sfx_tear, 0.9f, rate = 0.95f + Math.random().toFloat() * 0.1f)
        vibratePattern(longArrayOf(0, 18, 30, 26), intArrayOf(0, 120, 0, 90))
    }

    /**
     * Los tonos de aviso se cargan la primera vez que se escuchan: son los
     * ficheros más pesados y no tiene sentido tenerlos en memoria si nadie
     * abre los ajustes. Suenan al volumen real del aviso, no al de la
     * interfaz: hay que poder juzgarlos.
     */
    override fun previewTone(rawRes: Int) {
        val id = loaded[rawRes] ?: soundPool.load(appContext, rawRes, 1).also {
            loaded[rawRes] = it
            // Se reproduce en cuanto termine de cargar.
            soundPool.setOnLoadCompleteListener { pool, sampleId, status ->
                ready = true
                if (sampleId == it && status == 0) pool.play(sampleId, 0.85f, 0.85f, 1, 0, 1f)
            }
            return
        }
        soundPool.play(id, 0.85f, 0.85f, 1, 0, 1f)
    }

    fun release() {
        soundPool.release()
    }
}
