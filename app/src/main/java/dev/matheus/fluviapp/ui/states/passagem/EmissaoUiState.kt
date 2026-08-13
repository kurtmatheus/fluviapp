package dev.matheus.fluviapp.ui.states.passagem

import dev.matheus.fluviapp.domain.cliente.Cliente
import dev.matheus.fluviapp.domain.documento.TipoDocumento
import dev.matheus.fluviapp.domain.passagem.Acomodacao
import dev.matheus.fluviapp.domain.passagem.CategoriaPassagem
import dev.matheus.fluviapp.domain.passagem.FormaPagamento
import dev.matheus.fluviapp.domain.passagem.TipoGratuidade
import dev.matheus.fluviapp.domain.passagem.TipoPassagem
import dev.matheus.fluviapp.domain.passagem.ClasseVeiculo
import dev.matheus.fluviapp.domain.veiculo.Veiculo
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * **O estado da emissão, um passo por vez** ([ADR-0028] D4, [ADR-0026] D2).
 *
 * O que ele substitui é um `UiState` só, com `isVeiculoChecked` decidindo quais dos seus campos valiam — e
 * com um `limparCamposPassageiroOuVeiculo` para apagar os que tinham deixado de valer. Aqui a troca de
 * categoria **troca o objeto** ([ParticipanteEmEdicao]), então não há o que limpar: o campo do outro
 * sub-domínio deixa de existir em vez de deixar de importar.
 */
data class EmissaoUiState(
    /** Onde se está **no roteiro** — que é derivado, não fixo ([ADR-0029] D3). */
    val indiceDoPasso: Int = 0,
    val cabecalho: CabecalhoDaViagem = CabecalhoDaViagem(),
    val bilhete: BilheteEmEdicao = BilheteEmEdicao(),
    val participante: ParticipanteEmEdicao = ParticipanteEmEdicao.DePassageiro(),
    val pagamento: PagamentoEmEdicao = PagamentoEmEdicao(),
    /** Erros do passo corrente, calculados na tentativa de avançar — nunca a cada tecla. */
    val erros: Set<ErroDeEmissao> = emptySet(),
    val emitindo: Boolean = false,
    /**
     * **O detalhamento aberto** — presente enquanto o operador confere, ausente no resto do tempo.
     *
     * Ele é estado e não passo ([ConfirmacaoDaEmissao]): não entra no roteiro, não mexe na trilha, e some
     * tanto ao voltar para corrigir quanto ao confirmar.
     */
    val confirmacao: ConfirmacaoDaEmissao? = null,
    /** O id do bilhete, depois que a emissão se resolveu — é o que o passo do desfecho usa. */
    val idEmitida: String? = null,
) {
    /**
     * O caminho que as escolhas desenharam. É **calculado**, e não guardado, porque guardá-lo criaria uma
     * segunda verdade a sincronizar a cada toque — que é como roteiro e estado passam a discordar.
     */
    val roteiro: List<PassoDaEmissao> get() = roteiroDe(bilhete, participante)

    val passo: PassoDaEmissao get() = roteiro.getOrElse(indiceDoPasso) { PassoDaEmissao.Categoria }

    /** "3 de 6" — os dois lados saem do roteiro, então ficam certos em qualquer fluxo. */
    val numeroDoPasso: Int get() = indiceDoPasso + 1

    val totalDePassos: Int get() = roteiro.size

    val podeVoltar: Boolean get() = indiceDoPasso > 0 && passo != PassoDaEmissao.Desfecho
}

/**
 * O **cabeçalho de guia** ([ADR-0028] D5): a saída escolhida, visível nos três passos.
 *
 * Tudo aqui é **leitura**. Data e hora eram campos editáveis no formulário antigo — o operador podia digitar
 * uma data que discorda da saída —, e é essa classe de erro que o cabeçalho elimina.
 */
data class CabecalhoDaViagem(
    val travessia: String = "",
    val partida: String = "",
    val embarcacao: String = "",
)

/**
 * **Passo 1 — o bilhete**: o que se está vendendo.
 *
 * A [categoria] é a raiz; a [acomodacao] só existe para passageiro, e é ela que decide quantos cabem
 * ([Acomodacao.ocupacaoMaxima]) e que tipos admite. O [tipo] só se pergunta onde há escolha —
 * [Acomodacao.temEscolhaDeTipo], hoje só a rede —, e a [gratuidade] só quando o tipo é `GRATUIDADE`.
 */
data class BilheteEmEdicao(
    val categoria: CategoriaPassagem = CategoriaPassagem.PASSAGEIRO,
    val acomodacao: Acomodacao? = null,
    val tipo: TipoPassagem = TipoPassagem.INTEIRA,
    val gratuidade: TipoGratuidade? = null,
) {
    /** Quantas pessoas este bilhete admite. Sem acomodação escolhida ainda, uma. */
    val ocupacaoMaxima: Int get() = acomodacao?.ocupacaoMaxima ?: 1

    val pedeTipo: Boolean get() = acomodacao?.temEscolhaDeTipo == true

    val pedeGratuidade: Boolean get() = pedeTipo && tipo == TipoPassagem.GRATUIDADE
}

/**
 * **Passo 2 — quem viaja, ou o que embarca.** Selado, e é isso que apaga a limpeza reativa.
 */
sealed interface ParticipanteEmEdicao {

    /**
     * Pessoas. O **primeiro é o titular** e é obrigatório; os demais são acompanhantes, e só existem onde a
     * acomodação admite mais de um (suíte e camarote).
     */
    data class DePassageiro(
        val pessoas: List<ClienteEmEdicao> = listOf(ClienteEmEdicao()),
    ) : ParticipanteEmEdicao

    /**
     * Um veículo, e **opcionalmente** quem o retira: bilhete de veículo sem ninguém nomeado é a forma normal
     * ([ADR-0028] D3) — quem retira costuma ser definido na hora, entre despachante e transportadora.
     */
    data class DeVeiculo(
        val veiculo: VeiculoEmEdicao = VeiculoEmEdicao(),
        val responsavel: ClienteEmEdicao? = null,
    ) : ParticipanteEmEdicao
}

/**
 * Uma pessoa **como a tela a digita**: texto em tudo, porque é isso que um campo produz.
 *
 * A conversão para [Cliente] é a **fronteira texto → tipo** e devolve `null` quando não fecha — a mesma régua
 * dos codecs. É aqui, e não na tela, que "30/01/1996" vira `LocalDate` e o CPF perde a pontuação.
 */
data class ClienteEmEdicao(
    val nome: String = "",
    val tipoDocumento: TipoDocumento? = null,
    val numeroDocumento: String = "",
    /** `dd/MM/yyyy`, como o campo de calendário entrega. */
    val dataNascimento: String = "",
    val telefone: String = "",
    /** Preenchido quando a pessoa veio do pool: evita recriar quem já existe. */
    val idExistente: String? = null,
) {
    val vazio: Boolean
        get() = nome.isBlank() && numeroDocumento.isBlank() && dataNascimento.isBlank() && tipoDocumento == null

    fun paraCliente(): Cliente? {
        val tipo = tipoDocumento ?: return null
        val numero = tipo.normalizar(numeroDocumento)
        if (numero.isBlank() || !tipo.validar(numero)) return null
        val nascimento = runCatching { LocalDate.parse(dataNascimento, FORMATO_BR) }.getOrNull() ?: return null

        return Cliente(
            id = idExistente.orEmpty(),
            nome = nome.trim(),
            tipoDocumento = tipo,
            numeroDocumento = numero,
            dataNascimento = nascimento,
            telefone = telefone.takeIf { it.isNotBlank() },
        )
    }

    companion object {
        /** Do domínio para a tela — o caminho de volta, quando a pessoa vem do pool. */
        fun de(cliente: Cliente) = ClienteEmEdicao(
            nome = cliente.nome,
            tipoDocumento = cliente.tipoDocumento,
            numeroDocumento = cliente.numeroDocumento,
            dataNascimento = cliente.dataNascimento.format(FORMATO_BR),
            telefone = cliente.telefone.orEmpty(),
            idExistente = cliente.id,
        )
    }
}

/** Um veículo como a tela o digita. O que se exige de cada campo é do [ClasseVeiculo], não do formulário. */
data class VeiculoEmEdicao(
    val placa: String = "",
    val classe: ClasseVeiculo? = null,
    val modelo: String = "",
    val cor: String = "",
    val cilindrada: String = "",
) {
    fun paraVeiculo(): Veiculo? {
        val classe = classe ?: return null
        if (placa.isBlank()) return null

        return Veiculo(
            placa = placa,
            tipo = classe,
            modelo = modelo.takeIf { it.isNotBlank() },
            cor = cor,
            cilindrada = cilindrada.filter { it.isDigit() }.toIntOrNull(),
        )
    }
}

/**
 * **Passo 3 — o pagamento**: quanto entrou, por forma.
 *
 * Uma linha por forma marcada, e o total é a **soma** — não há campo de total, porque total gravado ao lado
 * da lista é a chance permanente de os dois discordarem ([ADR-0024] D4).
 */
data class PagamentoEmEdicao(
    val lancamentos: List<LancamentoEmEdicao> = emptyList(),
    val observacao: String = "",
) {
    val total: BigDecimal
        get() = lancamentos.mapNotNull { it.valorEmReais() }.fold(BigDecimal.ZERO, BigDecimal::add)
}

/** Um lançamento em edição: a forma escolhida e o valor **como digitado**. */
data class LancamentoEmEdicao(
    val forma: FormaPagamento,
    val valor: String = "",
) {
    /** `null` quando o texto ainda não é um valor — o campo em branco não vale zero, vale *nada*. */
    fun valorEmReais(): BigDecimal? {
        val limpo = valor.replace(".", "").replace(",", ".").trim()
        return limpo.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO }
    }
}

/**
 * O que impede o avanço, **por campo**, para a tela apontar onde é.
 *
 * São valores e não mensagens: a tradução para texto é da apresentação (ADR-0024 D8), e um enum permite ao
 * teste afirmar *qual* regra falhou sem depender da redação.
 */
enum class ErroDeEmissao {
    ACOMODACAO_NAO_ESCOLHIDA,
    GRATUIDADE_NAO_ESCOLHIDA,
    TITULAR_INCOMPLETO,
    ACOMPANHANTE_INCOMPLETO,
    EXCEDE_OCUPACAO,
    PESSOA_REPETIDA,
    VEICULO_SEM_PLACA,
    VEICULO_SEM_CLASSE,
    VEICULO_SEM_MODELO,
    VEICULO_SEM_CILINDRADA,
    RESPONSAVEL_INCOMPLETO,
    SEM_PAGAMENTO,
    VALOR_INVALIDO,
}

private val FORMATO_BR: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")