package com.mobile.finsolve.app.movefasttdd.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class WorkoutConfig(
    val reps: Int,
    val repDuration: Int,
    val restDuration: Int,
) : Parcelable
