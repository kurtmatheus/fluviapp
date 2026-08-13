package dev.matheus.fluviapp.services.repository.passagem

import dev.matheus.fluviapp.domain.passagem.Passagem
import dev.matheus.fluviapp.domain.passagem.ResultadoEmbarque
import dev.matheus.fluviapp.domain.passagem.StatusPassagem
import dev.matheus.fluviapp.domain.viagem.OcorrenciaViagem

/**
 * **A porta da Passagem** ([ADR-0025] D1) — e ela se define tanto pelo que tem quanto pelo que **não** tem.
 *
 * Esta interface preenche o vazio mais antigo desta entidade: a passagem era a **única** sem porta, com a classe
 * concreta injetada em dez lugares — e era por isso que **não existia teste de ViewModel de passagem**, porque
 * sem porta não há fake.
 *
 * ### As três ausências, que são decisões tomando forma de código
 *
 * - **sem `editar`** — bilhete não se reescreve;
 * - **sem `deletar`** — remoção física **não é uma operação da Passagem em nenhuma camada** ([ADR-0024] D11), e
 *   o servidor a nega. O que existe é o cancelamento, que é [StatusPassagem.CANCELADA] via [transicionar];
 * - **sem `observarTodas`** — dado que cresce sem limite não se observa inteiro (ADR-0024 D9). Onde as sete
 *   entidades revitalizadas têm um `StateFlow` da coleção, aqui há [consultar].
 *
 * É o mesmo recurso que o `ViagemRepository` usa para dizer que a viagem é imutável: **a imutabilidade não é um
 * comentário, é um método que não existe**.
 *
 * ### O que a porta deliberadamente não carrega
 *
 * **Não pergunta "posso?"**. Quem pode cancelar, emitir ou ver é da política (`PermissoesUsuario`,
 * [ADR-0010]), consultada **antes** da chamada. Uma porta que opinasse sobre permissão criaria uma segunda
 * fonte de autorização, e o ADR-0010 existe para haver uma.
 */
interface PassagemRepository {

    /**
     * Emite: cria o documento e devolve o **id** (o que o QR carrega). Não existe "salvar por cima".
     *
     * A escrita é otimista porque o SDK já a faz — o `set` entra no cache e o bilhete já vale, com a ida ao
     * servidor observada depois ([ADR-0025] D5).
     */
    suspend fun emitir(passagem: Passagem): String

    suspend fun obterPorId(id: String): Passagem?

    /**
     * Ao vivo, **fora do cache**: o QR pode chegar num aparelho que nunca viu o bilhete — foi emitido noutra
     * bilheteria, e o embarque valida contra a fonte da verdade (ADR-0012).
     */
    suspend fun obterDoServidorPorId(id: String): Passagem?

    /** A consulta recortada (D2). [CriterioPassagem] é dado; a tradução é pura. */
    suspend fun consultar(criterio: CriterioPassagem): List<Passagem>

    /** Avança a FSM. **Cancelar é transição, não remoção.** Transição ilegal é recusada, não aplicada. */
    suspend fun transicionar(id: String, novo: StatusPassagem)

    /** Valida a aresta `EMITIDA → EMBARCADA` e carimba **o uid** de quem leu o QR. */
    suspend fun confirmarEmbarque(id: String, operadorId: String): ResultadoEmbarque

    /**
     * Reserva o **próximo número** daquela saída — atômico no servidor ([ADR-0024] D6).
     *
     * Quem chama não precisa saber que existe `viagens/{viagemId}/ocorrencias/{data}`: a subcoleção é do
     * repositório concreto, como as `atuacoes` são da Empresa.
     */
    suspend fun reservarNumero(ocorrencia: OcorrenciaViagem): Int
}