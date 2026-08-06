package dev.matheus.fluviapp.services.repository.cadastro.porto

import dev.matheus.fluviapp.domain.porto.Porto
import kotlinx.coroutines.flow.StateFlow

/**
 * Porta do repositório de portos (DIP) — capacidade da **plataforma**, na raiz do Firestore
 * (ADR-0016 §5). Os ViewModels dependem desta interface; produção usa `PortoFirestoreRepository`.
 *
 * ### A mesma regra do delete lógico, um elo adiante
 *
 * [observarTodos] e [obterTodos] devolvem **tudo**, inativos incluídos, e quem lista é que filtra —
 * exatamente como na `LocalidadeRepository`. Aqui a razão fica ainda mais concreta: o porto inativo
 * precisa continuar resolvível porque a **rota** e a **concessão** guardam o id dele, e uma rota que
 * exibisse "porto: —" seria pior do que uma rota que exibe um porto fora de uso.
 *
 * Não há consulta "portos desta localidade" nesta porta, e é deliberado: a coleção é pequena e vem
 * inteira (ADR-0017 D1), então recortar por localidade é filtrar em memória — inventar uma query aqui
 * seria criar um segundo caminho de leitura para o mesmo dado que o listener já entregou.
 */
interface PortoRepository {
    fun sincronizar()
    fun observarTodos(): StateFlow<List<Porto>>

    /** Salva e devolve **o id** — necessário porque a criação o gera. */
    suspend fun salvar(porto: Porto): String

    suspend fun obterTodos(): List<Porto>
    suspend fun obterPorId(id: String): Porto?

    /**
     * **Inativa** — não apaga. O nome continua `deletar` porque é o gesto que a tela oferece; o que muda
     * é o que ele significa aqui, e isso é do repositório, não da tela.
     */
    suspend fun deletar(id: String)
}