package com.example.core.result

/**
 * Standard Result wrapper for all domain and data operations in Hotel ERP Pro.
 * Provides unified error handling and state propagation.
 */
sealed class Resource<out T> {
    data object Idle : Resource<Nothing>()
    data object Loading : Resource<Nothing>()
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String, val cause: Throwable? = null) : Resource<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    fun getOrNull(): T? = (this as? Success)?.data
}

/**
 * Domain-specific exceptions for Hotel ERP Pro operations.
 */
sealed class HotelErpException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class AccountingException(message: String) : HotelErpException(message)
    class ValidationException(message: String) : HotelErpException(message)
    class DataIntegrityException(message: String) : HotelErpException(message)
    class UnauthorizedException(message: String) : HotelErpException(message)
    class RecordNotFoundException(message: String) : HotelErpException(message)
}
