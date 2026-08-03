package dev.matheus.fluviapp.domain

interface IObjetoSimplificado {
    val id: String
    val descricaoNome: String
}

fun List<IObjetoSimplificado>.extrairPorId(id: String): IObjetoSimplificado {
    return first { it.id ==  id}
}

fun List<IObjetoSimplificado>.extrairPorDescricao(descricao: String): IObjetoSimplificado {
    return first { it.descricaoNome ==  descricao}
}

fun List<IObjetoSimplificado>.mapDescricao(): List<String> {
    return map { it.descricaoNome }
}