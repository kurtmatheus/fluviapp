package dev.matheus.fluviapp.services.repository.cadastro.localidade

import dev.matheus.fluviapp.domain.localidade.Localidade
import kotlinx.coroutines.flow.StateFlow

/**
 * Porta do repositório de localidades (DIP) — capacidade da **plataforma**, na raiz do Firestore
 * (ADR-0016 §5). Os ViewModels dependem desta interface; produção usa `LocalidadeFirestoreRepository`.
 *
 * ### A regra do delete lógico
 *
 * [observarTodas] e [obterTodas] devolvem **tudo**, inativas incluídas, e a decisão de filtrar é de quem
 * lista. Parece o contrário do fail-closed, e é deliberado: a inativa **precisa** continuar chegando a
 * quem resolve um id — um porto histórico aponta para ela, e sumir com o registro trocaria "essa
 * localidade saiu de uso" por "essa localidade nunca existiu".
 *
 * Quem monta lista ou dropdown filtra por `ativo`; quem resolve por id não filtra. Está dito aqui, e não
 * em cada chamador, porque é a espécie de regra que se esquece uma vez e some do relatório para sempre.
 */
interface LocalidadeRepository {
    fun sincronizar()
    fun observarTodas(): StateFlow<List<Localidade>>

    /** Salva e devolve **o id** — necessário porque a criação o gera. */
    suspend fun salvar(localidade: Localidade): String

    suspend fun obterTodas(): List<Localidade>
    suspend fun obterPorId(id: String): Localidade?

    /**
     * **Inativa** — não apaga. O nome continua `deletar` porque é o gesto que a tela oferece (o mesmo
     * botão de lixeira das outras seções); o que muda é o que ele significa aqui, e isso é do repositório,
     * não da tela.
     */
    suspend fun deletar(id: String)
}