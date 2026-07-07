package dev.matheus.fluviapp.exceptions

class RequestException(
    val code: Int,
    override val message: String
) : RuntimeException()