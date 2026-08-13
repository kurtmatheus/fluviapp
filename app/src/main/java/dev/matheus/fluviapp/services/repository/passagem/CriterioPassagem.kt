package dev.matheus.fluviapp.services.repository.passagem

import dev.matheus.fluviapp.domain.operacoes.PermissoesUsuario.EscopoEmpresa
import dev.matheus.fluviapp.domain.passagem.CategoriaPassagem
import dev.matheus.fluviapp.domain.passagem.StatusPassagem
import dev.matheus.fluviapp.domain.viagem.OcorrenciaViagem
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * **O que se quer perguntar à coleção de passagens** — um objeto de critério ([ADR-0025] D2).
 *
 * A alternativa que este tipo rejeita não é teórica: era um método por combinação de filtros, e foi assim que
 * se chegou ao `obterTodasPorDataStatus(data, status, funcionario, agencia)` — quatro parâmetros posicionais em
 * que o quarto, **vazio, significava *ver tudo***. A outra alternativa, um lambda recebendo `Query`, seria a
 * mais flexível e a única que quebra a garantia pela qual o `DocumentoBruto` existe: **mapeamento e consulta
 * testáveis sem Firebase**.
 *
 * Critério é **dado**; por isso traduzi-lo em [PlanoDeConsulta] é função pura, verificável em JVM, e a porta
 * não vaza tipo do Firebase.
 *
 * ### "Ver tudo" deixou de ser uma string vazia
 *
 * O [escopo] é [EscopoEmpresa], e os três casos dele são diferentes de propósito — o terceiro é o perigoso:
 * *não filtrar nada* e *não ter empresa nenhuma* pareciam iguais como `""`, e abriam a listagem inteira para
 * quem não deveria ver nada. Aqui, [EscopoEmpresa.Nenhuma] vira [PlanoDeConsulta.SemResultado] — a consulta
 * **não acontece**, em vez de acontecer sem recorte.
 */
data class CriterioPassagem(
    /** Quando, e de que travessia. Ver [RecorteTemporal]. */
    val recorte: RecorteTemporal = RecorteTemporal.Qualquer,
    /** De quem se pode ver. Sem valor explícito, **nada** — fail-closed. */
    val escopo: EscopoEmpresa = EscopoEmpresa.Nenhuma,
    val status: StatusPassagem? = null,
    val categoria: CategoriaPassagem? = null,
    /** Quem emitiu. */
    val funcionarioId: String? = null,
    /** Em que passagens esta pessoa viajou — responde com **uma** consulta porque o titular está no array (D3). */
    val clienteId: String? = null,
)

/**
 * O recorte no tempo, como **um eixo só**.
 *
 * Ele é selado porque os casos se excluem, e a exclusão precisa ser impossível de violar: pedir *"o dia 18"* e
 * *"a semana toda"* na mesma consulta é contraditório, e no Firestore é também **ilegal** — uma consulta admite
 * desigualdade em um único campo. Com campos independentes (`data` + `periodo`), a contradição se escreveria e
 * só apareceria em tempo de execução, no servidor.
 */
sealed interface RecorteTemporal {
    /** A travessia concreta: `viagemId` **e** `data`. É o recorte da ocupação e da numeração. */
    data class Ocorrencia(val ocorrencia: OcorrenciaViagem) : RecorteTemporal

    /** Um dia de calendário, em todas as viagens. */
    data class Dia(val data: LocalDate) : RecorteTemporal

    /**
     * Uma faixa fechada — o que o balanço e a análise por período pedem.
     *
     * Funciona sem truque porque a data é **texto ISO**: `yyyy-MM-dd` ordena lexicograficamente na mesma ordem
     * em que ordena cronologicamente ([ADR-0024] D2).
     */
    data class Periodo(val de: LocalDate, val ate: LocalDate) : RecorteTemporal

    /** Todas as datas. Sozinho não abre nada: o [CriterioPassagem.escopo] continua valendo. */
    data object Qualquer : RecorteTemporal
}

/**
 * Um filtro **neutro** — sem tipo do Firebase, para que a tradução seja verificável em JVM.
 *
 * São só os três operadores que as perguntas desta coleção usam. Não há `orderBy` nem limite aqui, e a ausência
 * é decisão: ordenar por valor exigiria um total denormalizado, que o [ADR-0024] D4 recusa.
 */
sealed interface FiltroPassagem {
    data class Igual(val campo: String, val valor: String) : FiltroPassagem

    /** `array-contains` — o que responde *"em que passagens esta pessoa viajou"*. */
    data class ContemNoArray(val campo: String, val valor: String) : FiltroPassagem

    /** Faixa fechada sobre texto ISO. */
    data class NaFaixa(val campo: String, val de: String, val ate: String) : FiltroPassagem
}

/**
 * O que a tradução produz: ou uma consulta com filtros, ou **nenhuma consulta**.
 *
 * O segundo caso não é uma consulta que devolve vazio — é a decisão de **não perguntar**, e ela existe para que
 * *"não tenho empresa nenhuma"* jamais seja executado como *"tudo"*.
 */
sealed interface PlanoDeConsulta {
    data object SemResultado : PlanoDeConsulta

    data class Filtrada(val filtros: List<FiltroPassagem>) : PlanoDeConsulta
}

/**
 * Critério → plano de consulta. **Função pura** — é aqui que a testabilidade da consulta mora.
 *
 * A ordem dos filtros é estável (escopo, tempo, status, categoria, funcionário, cliente) para que o teste
 * compare listas sem depender de ordenação incidental.
 */
fun CriterioPassagem.traduzir(): PlanoDeConsulta {
    val agenciaId = when (val escopo = escopo) {
        is EscopoEmpresa.Nenhuma -> return PlanoDeConsulta.SemResultado
        is EscopoEmpresa.Todas -> null
        is EscopoEmpresa.Apenas -> escopo.empresaId
    }

    val filtros = buildList {
        agenciaId?.let { add(FiltroPassagem.Igual(CAMPO_AGENCIA, it)) }

        when (val recorte = recorte) {
            is RecorteTemporal.Ocorrencia -> {
                add(FiltroPassagem.Igual(CAMPO_VIAGEM, recorte.ocorrencia.viagemId))
                add(FiltroPassagem.Igual(CAMPO_DATA, recorte.ocorrencia.dataIso))
            }

            is RecorteTemporal.Dia -> add(FiltroPassagem.Igual(CAMPO_DATA, recorte.data.iso()))

            is RecorteTemporal.Periodo ->
                add(FiltroPassagem.NaFaixa(CAMPO_DATA, recorte.de.iso(), recorte.ate.iso()))

            RecorteTemporal.Qualquer -> Unit
        }

        status?.let { add(FiltroPassagem.Igual(CAMPO_STATUS, it.name)) }
        categoria?.let { add(FiltroPassagem.Igual(CAMPO_CATEGORIA, it.name)) }
        funcionarioId?.takeIf { it.isNotBlank() }?.let { add(FiltroPassagem.Igual(CAMPO_FUNCIONARIO, it)) }
        clienteId?.takeIf { it.isNotBlank() }?.let { add(FiltroPassagem.ContemNoArray(CAMPO_CLIENTES, it)) }
    }

    return PlanoDeConsulta.Filtrada(filtros)
}

private fun LocalDate.iso(): String = format(DateTimeFormatter.ISO_LOCAL_DATE)

const val CAMPO_AGENCIA = "agenciaId"
const val CAMPO_VIAGEM = "viagemId"
const val CAMPO_DATA = "data"
const val CAMPO_STATUS = "status"
const val CAMPO_CATEGORIA = "categoria"
const val CAMPO_FUNCIONARIO = "funcionarioId"
const val CAMPO_CLIENTES = "clientes"