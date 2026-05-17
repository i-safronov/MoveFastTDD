package com.mobile.finsolve.app.movefasttdd.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.mobile.finsolve.app.movefasttdd.MainActivity
import com.mobile.finsolve.app.movefasttdd.R
import com.mobile.finsolve.app.movefasttdd.domain.model.TimerPhase
import com.mobile.finsolve.app.movefasttdd.domain.model.TimerState

class WorkoutNotificationManager(private val context: Context) {

    fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun build(state: TimerState): Notification {
        val contentIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE,
        )

        val (title, text) = state.notificationContent()
        val progress = (state.progress * 100).toInt()

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, false)
            .apply { if (!state.isFinished) addTimerActions(state.isRunning) }
            .build()
    }

    private fun TimerState.notificationContent(): Pair<String, String> {
        val repText = context.getString(R.string.notification_rep_counter, currentRep, totalReps)
        val timeText = remainingSeconds.toTimeString()
        return when (currentPhase) {
            is TimerPhase.Work ->
                context.getString(R.string.notification_phase_work) to "$timeText  •  $repText"
            is TimerPhase.Rest ->
                context.getString(R.string.notification_phase_rest) to "$timeText  •  $repText"
            TimerPhase.Finished ->
                context.getString(R.string.notification_phase_finished) to
                        context.getString(R.string.notification_workout_done)
        }
    }

    private fun NotificationCompat.Builder.addTimerActions(isRunning: Boolean) {
        val pauseResumeIntent = if (isRunning) {
            context.getString(R.string.notification_action_pause) to actionIntent(ACTION_PAUSE, 1)
        } else {
            context.getString(R.string.notification_action_resume) to actionIntent(ACTION_RESUME, 2)
        }
        addAction(0, pauseResumeIntent.first, pauseResumeIntent.second)
        addAction(0, context.getString(R.string.notification_action_cancel), actionIntent(ACTION_CANCEL, 3))
    }

    private fun actionIntent(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context, requestCode,
            Intent(action).setPackage(context.packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    companion object {
        const val CHANNEL_ID = "workout_timer_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_PAUSE = "com.mobile.finsolve.app.movefasttdd.ACTION_PAUSE"
        const val ACTION_RESUME = "com.mobile.finsolve.app.movefasttdd.ACTION_RESUME"
        const val ACTION_CANCEL = "com.mobile.finsolve.app.movefasttdd.ACTION_CANCEL"
    }
}

private fun Int.toTimeString(): String {
    val m = this / 60
    val s = this % 60
    return "%02d:%02d".format(m, s)
}
