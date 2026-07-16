package dev.matheus.fluviapp.util

interface Mapper<E, O> {
    suspend fun map(entry: E): O
}