package com.mobile.finsolve.app.movefasttdd.domain.model

sealed class DataResult<out T> {
    data class Success<out T>(val value: T) : DataResult<T>()
    data object Error : DataResult<Nothing>()
}

inline fun <T, R> DataResult<T>.fold(
    onSuccess: (T) -> R,
    onError: () -> R,
): R = when (this) {
    is DataResult.Success -> onSuccess(value)
    DataResult.Error -> onError()
}
