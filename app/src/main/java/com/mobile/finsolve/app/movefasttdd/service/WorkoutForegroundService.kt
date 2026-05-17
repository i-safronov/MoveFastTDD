package com.mobile.finsolve.app.movefasttdd.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.mobile.finsolve.app.movefasttdd.R
import com.mobile.finsolve.app.movefasttdd.domain.model.TimerState
import com.mobile.finsolve.app.movefasttdd.domain.repository.WorkoutTimerRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WorkoutForegroundService : LifecycleService() {

    @Inject lateinit var repository: WorkoutTimerRepository

    private val notificationManager by lazy { WorkoutNotificationManager(this) }
    private val systemNotificationManager by lazy {
        getSystemService(NotificationManager::class.java)
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager.createChannel()
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val currentState = repository.state.value
        ServiceCompat.startForeground(
            this,
            WorkoutNotificationManager.NOTIFICATION_ID,
            currentState?.let { notificationManager.build(it) } ?: buildPlaceholderNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )

        lifecycleScope.launch {
            repository.state
                .takeWhile { it != null && !it.isFinished }
                .collect { state -> updateNotification(state!!) }
            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    private fun updateNotification(state: TimerState) {
        systemNotificationManager.notify(
            WorkoutNotificationManager.NOTIFICATION_ID,
            notificationManager.build(state),
        )
    }

    // Используется когда сервис стартует раньше чем репозиторий успел
    // установить состояние — крайне редкий кейс, но startForeground()
    // обязан получить корректный Notification немедленно.
    private fun buildPlaceholderNotification(): Notification =
        NotificationCompat.Builder(this, WorkoutNotificationManager.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.app_name))
            .setOngoing(true)
            .build()

    companion object {
        fun intent(context: android.content.Context) =
            Intent(context, WorkoutForegroundService::class.java)
    }
}
