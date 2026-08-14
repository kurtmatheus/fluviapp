package dev.matheus.fluviapp.ui.states.passagem

import dev.matheus.fluviapp.domain.passagem.CategoriaPassagem
import dev.matheus.fluviapp.domain.passagem.StatusPassagem
import java.time.LocalDate

/**
 * **A busca de um bilhete** — a ação que dá sentido à seção Passagens no menu.
 *
 * O filtro nasce com **o dia de hoje**, e isso é a decisão de desenho desta tela: quem procura um bilhete no
 * balcão procura o de hoje em 99 dos 100 casos. Abrir a tela já respondendo à pergunta mais comum é o que
 * transforma a busca num toque em vez de num formulário.
 *
 * Os outros dois eixos são **opcionais e ficam em branco**: status e categoria estreitam quando a lista está
 * grande, e exigi-los de entrada seria cobrar decisão de quem só quer ver o que vendeu.
 */
data class PesquisaPassagemUiState(
    val data: LocalDate,
    val status: StatusPassagem? = null,
    val categoria: CategoriaPassagem? = null,
    val buscando: Boolean = false,
    val resultados: List<PassagemNaLista> = emptyList(),
    /** `true` depois da primeira busca — distingue "nada encontrado" de "ainda não procurou". */
    val buscou: Boolean = false,
    /** Sem vínculo não há o que listar: o recorte por agência é fail-closed (ADR-0025 D2). */
    val semEscopo: Boolean = false,
)

/**
 * Uma passagem **na lista** — a terceira projeção do agregado, e a mais enxuta das três.
 *
 * Ela responde só ao que uma lista precisa: *qual bilhete é este, de quem, e ele ainda vale*. Nome e placa
 * **não entram**, e por isso esta é a projeção que **não lê os pools** — uma lista de cinquenta bilhetes que
 * resolvesse cliente por cliente faria cinquenta leituras de dado pessoal para preencher uma coluna que o
 * operador usa para reconhecer, não para conferir.
 */
data class PassagemNaLista(
    val idPassagem: String,
    val numero: String,
    val bilhete: String,
    val partida: String,
    val status: String,
    /** `true` quando o bilhete não vale mais — a lista o mostra apagado, não o esconde. */
    val encerrada: Boolean,
)