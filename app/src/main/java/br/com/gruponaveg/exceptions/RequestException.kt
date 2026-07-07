package br.com.gruponaveg.exceptions

class RequestException(
    val code: Int,
    override val message: String
) : RuntimeException()