package dev.matheus.fluviapp.exceptions

class QRCodeException(
    override val message: String,
    override val cause: Throwable?
) : RuntimeException(message , cause)
