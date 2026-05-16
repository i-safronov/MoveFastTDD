package com.mobile.finsolve.app.movefasttdd.core.extensions

import com.mobile.finsolve.app.movefasttdd.domain.model.DataResult

fun <T> Result<T>.toDataResult(): DataResult<T> =
    if (isSuccess) DataResult.Success(getOrThrow()) else DataResult.Error
