package com.clibeats.domain.provider

sealed class ProviderResult<out T> {
    data class Success<T>(val data: T) : ProviderResult<T>()

    data class Error(val message: String, val cause: Throwable? = null) : ProviderResult<Nothing>()

    data object Loading : ProviderResult<Nothing>()
}
