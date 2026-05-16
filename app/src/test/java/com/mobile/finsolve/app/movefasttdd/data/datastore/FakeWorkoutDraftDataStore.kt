package com.mobile.finsolve.app.movefasttdd.data.datastore

class FakeWorkoutDraftDataStore : WorkoutDraftDataStore {

    var storedDraft: WorkoutDraft? = null
    var saveCallCount: Int = 0
    var clearCallCount: Int = 0

    override suspend fun load(): WorkoutDraft? = storedDraft

    override suspend fun save(draft: WorkoutDraft) {
        storedDraft = draft
        saveCallCount++
    }

    override suspend fun clear() {
        storedDraft = null
        clearCallCount++
    }
}
