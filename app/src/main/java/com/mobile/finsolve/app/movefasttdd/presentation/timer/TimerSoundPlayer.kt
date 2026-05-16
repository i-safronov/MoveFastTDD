package com.mobile.finsolve.app.movefasttdd.presentation.timer

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.mobile.finsolve.app.movefasttdd.R
import com.mobile.finsolve.app.movefasttdd.presentation.timer.view_model.TimerContract

class TimerSoundPlayer(context: Context) {

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val startSoundId = soundPool.load(context, R.raw.start, 1)
    private val endSoundId = soundPool.load(context, R.raw.end, 1)

    fun play(type: TimerContract.SoundType) {
        val soundId = when (type) {
            TimerContract.SoundType.START -> startSoundId
            TimerContract.SoundType.END -> endSoundId
        }
        soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
    }

    fun release() = soundPool.release()
}
