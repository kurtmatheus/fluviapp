package br.com.gruponaveg.services.printerservice.printer

interface Printer {
    fun open()

    fun write(command: ByteArray?)

    fun close()
}
