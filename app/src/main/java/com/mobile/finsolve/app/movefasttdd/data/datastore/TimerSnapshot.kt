package com.mobile.finsolve.app.movefasttdd.data.datastore

data class TimerSnapshot(
    val reps: Int,
    val repDuration: Int,
    val restDuration: Int,
    val phaseIndex: Int,
    val remainingSeconds: Int,
)
