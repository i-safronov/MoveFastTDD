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

interface WorkoutDraftDataStore {
    suspend fun load(): WorkoutDraft?
    suspend fun save(draft: WorkoutDraft)
    suspend fun clear()
}

private val Context.draftDataStore by preferencesDataStore(name = "workout_draft")

@Singleton
class WorkoutDraftDataStoreImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : WorkoutDraftDataStore {

    override suspend fun load(): WorkoutDraft? =
        context.draftDataStore.data.map { it.toDraft() }.first()

    override suspend fun save(draft: WorkoutDraft) {
        context.draftDataStore.edit { it.fromDraft(draft) }
    }

    override suspend fun clear() {
        context.draftDataStore.edit { it.clear() }
    }
}

// Ключи на уровне файла — доступны маперам, но скрыты снаружи
private object DraftKeys {
    val REPS = intPreferencesKey("reps")
    val REP_DURATION = intPreferencesKey("rep_duration")
    val REST_DURATION = intPreferencesKey("rest_duration")
}

private fun Preferences.toDraft(): WorkoutDraft? {
    val reps = this[DraftKeys.REPS] ?: return null
    val repDuration = this[DraftKeys.REP_DURATION] ?: return null
    val restDuration = this[DraftKeys.REST_DURATION] ?: return null
    return WorkoutDraft(reps, repDuration, restDuration)
}

private fun MutablePreferences.fromDraft(draft: WorkoutDraft) {
    this[DraftKeys.REPS] = draft.reps
    this[DraftKeys.REP_DURATION] = draft.repDuration
    this[DraftKeys.REST_DURATION] = draft.restDuration
}
