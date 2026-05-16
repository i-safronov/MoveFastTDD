package com.mobile.finsolve.app.movefasttdd.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface TimerStateDataStore {
    suspend fun load(): TimerSnapshot?
    suspend fun save(snapshot: TimerSnapshot)
    suspend fun clear()
}

private val Context.timerDataStore by preferencesDataStore(name = "timer_state")

@Singleton
class TimerStateDataStoreImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : TimerStateDataStore {

    override suspend fun load(): TimerSnapshot? =
        context.timerDataStore.data.map { it.toSnapshot() }.first()

    override suspend fun save(snapshot: TimerSnapshot) {
        context.timerDataStore.edit { it.fromSnapshot(snapshot) }
    }

    override suspend fun clear() {
        context.timerDataStore.edit { it.clear() }
    }
}

private object TimerKeys {
    val REPS = intPreferencesKey("reps")
    val REP_DURATION = intPreferencesKey("rep_duration")
    val REST_DURATION = intPreferencesKey("rest_duration")
    val PHASE_INDEX = intPreferencesKey("phase_index")
    val REMAINING_SECONDS = intPreferencesKey("remaining_seconds")
}

private fun Preferences.toSnapshot(): TimerSnapshot? {
    val reps = this[TimerKeys.REPS] ?: return null
    val repDuration = this[TimerKeys.REP_DURATION] ?: return null
    val restDuration = this[TimerKeys.REST_DURATION] ?: return null
    val phaseIndex = this[TimerKeys.PHASE_INDEX] ?: return null
    val remainingSeconds = this[TimerKeys.REMAINING_SECONDS] ?: return null
    return TimerSnapshot(reps, repDuration, restDuration, phaseIndex, remainingSeconds)
}

private fun MutablePreferences.fromSnapshot(snapshot: TimerSnapshot) {
    this[TimerKeys.REPS] = snapshot.reps
    this[TimerKeys.REP_DURATION] = snapshot.repDuration
    this[TimerKeys.REST_DURATION] = snapshot.restDuration
    this[TimerKeys.PHASE_INDEX] = snapshot.phaseIndex
    this[TimerKeys.REMAINING_SECONDS] = snapshot.remainingSeconds
}
