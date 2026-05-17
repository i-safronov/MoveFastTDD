package com.mobile.finsolve.app.movefasttdd.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mobile.finsolve.app.movefasttdd.domain.repository.WorkoutTimerRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WorkoutNotificationReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: WorkoutTimerRepository

    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    WorkoutNotificationManager.ACTION_PAUSE -> repository.pause()
                    WorkoutNotificationManager.ACTION_RESUME -> repository.resume()
                    WorkoutNotificationManager.ACTION_CANCEL -> repository.cancel()
                }
            } finally {
                result.finish()
            }
        }
    }
}
