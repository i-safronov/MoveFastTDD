package com.mobile.finsolve.app.movefasttdd.data.datastore

class FakeTimerStateDataStore : TimerStateDataStore {

    var snapshot: TimerSnapshot? = null
    var saveCallCount: Int = 0
    var clearCallCount: Int = 0

    override suspend fun load(): TimerSnapshot? = snapshot

    override suspend fun save(snapshot: TimerSnapshot) {
        this.snapshot = snapshot
        saveCallCount++
    }

    override suspend fun clear() {
        snapshot = null
        clearCallCount++
    }
}
