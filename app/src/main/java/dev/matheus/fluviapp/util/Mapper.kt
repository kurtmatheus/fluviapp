package dev.matheus.fluviapp.util

interface Mapper<E, O> {
    fun map(entry: E): O
}