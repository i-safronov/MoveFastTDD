package com.mobile.finsolve.app.movefasttdd.data.repository

import com.mobile.finsolve.app.movefasttdd.data.local.WorkoutConfigDao
import com.mobile.finsolve.app.movefasttdd.data.local.WorkoutConfigEntity
import com.mobile.finsolve.app.movefasttdd.core.extensions.toDataResult
import com.mobile.finsolve.app.movefasttdd.domain.model.DataResult
import com.mobile.finsolve.app.movefasttdd.domain.model.WorkoutConfig
import com.mobile.finsolve.app.movefasttdd.domain.repository.WorkoutConfigRepository
import javax.inject.Inject

class WorkoutConfigRepositoryImpl @Inject constructor(
    private val dao: WorkoutConfigDao,
) : WorkoutConfigRepository {

    override suspend fun save(config: WorkoutConfig): DataResult<Unit> = runCatching {
        dao.save(config.toEntity())
    }.toDataResult()

    override suspend fun load(): DataResult<WorkoutConfig?> = runCatching {
        dao.load()?.toDomain()
    }.toDataResult()
}

private fun WorkoutConfig.toEntity() = WorkoutConfigEntity(
    reps = reps,
    repDuration = repDuration,
    restDuration = restDuration,
)

private fun WorkoutConfigEntity.toDomain() = WorkoutConfig(
    reps = reps,
    repDuration = repDuration,
    restDuration = restDuration,
)
