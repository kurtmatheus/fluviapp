package br.com.gruponaveg.util

interface Mapper<E, O> {
    fun map(entry: E): O
}