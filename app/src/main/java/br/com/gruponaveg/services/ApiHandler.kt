package br.com.gruponaveg.services

import br.com.gruponaveg.services.RequestResult.Error
import br.com.gruponaveg.services.RequestResult.Exception
import br.com.gruponaveg.services.RequestResult.Success
import retrofit2.HttpException
import retrofit2.Response

sealed class RequestResult<T : Any> {
    class Success<T : Any>(val data: T) : RequestResult<T>()
    class Error<T : Any>(val code: Int, val message: String) : RequestResult<T>()
    class Exception<T : Any>(val e: Throwable) : RequestResult<T>()
}

suspend fun <T : Any> handleApi(
    execute: suspend () -> Response<T>
): RequestResult<T> {
    return try {
        val response = execute()

        if (response.isSuccessful && response.body() != null) {
            Success(response.body()!!)
        } else {
            Error(code = response.code(), message = response.message())
        }
    } catch (e: HttpException) {
        Exception(e)
    } catch (e: Throwable) {
        Exception(e)
    }
}

suspend fun <T : Any> RequestResult<T>.onSuccess(
    function: suspend (T) -> Unit
): RequestResult<T> = apply {
    if (this is Success) {
        function(data)
    }
}

suspend fun <T : Any> RequestResult<T>.onError(
    function: suspend (code: Int, message: String) -> Unit
): RequestResult<T> = apply {
    if (this is Error) {
        function(code, message)
    }
}

suspend fun <T : Any> RequestResult<T>.onException(
    function: suspend (e: Throwable) -> Unit
): RequestResult<T> = apply {
    if (this is Exception) {
        function(e)
    }
}