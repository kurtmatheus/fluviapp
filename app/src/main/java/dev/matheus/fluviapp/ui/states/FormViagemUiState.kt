package dev.matheus.fluviapp.ui.states

import androidx.annotation.StringRes
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.viagem.Viagem

/**
 * Estado do formulário de viagem — puro (só dados + flags), no molde do ADR-0006.
 *
 * **Não existe modo de edição**, como na rota e pela razão elevada: a viagem é o que a passagem aponta, e
 * reescrever o horário mudaria a hora impressa em bilhete já emitido. Por isso não há `titulo` variável
 * nem id — esta tela só cria.
 *
 * [rotas] e [embarcacoes] chegam **já recortadas pela concessão** (decisão do analista, 2026-08-10): o
 * formulário não oferece o que a empresa não pode ofertar. É por isso que o recorte não aparece como
 * validação — o erro não chega a poder ser cometido.
 *
 * [viagensExistentes] vem no estado porque a validação é pura: ela não consulta nada, decide sobre o que
 * lhe deram — como no Porto e na Rota.
 */
data class FormViagemUiState(
    val rota: String = "",
    val isRotaError: Boolean = false,

    val embarcacao: String = "",
    val isEmbarcacaoError: Boolean = false,

    val diaSemana: String = "",
    val isDiaSemanaError: Boolean = false,

    /**
     * **Os dígitos**, no máximo quatro — `"1830"`, e não `"18:30"`.
     *
     * O nome diz o que ele guarda de propósito. O separador é **desenhado** pela
     * `HoraVisualTransformation`, e guardá-lo aqui foi o que quebrou o cursor: um caractere inserido no
     * meio do valor faz o Compose recalcular a seleção sobre o texto anterior. Por dentro, mais adiante,
     * a hora é minuto (ADR-0016 §7.1) — este campo é a fronteira de digitação, não a de domínio.
     */
    val horaDigitada: String = "",
    val erroHora: ErroHoraViagem = ErroHoraViagem.NENHUM,

    val rotas: List<RotaOpcao> = emptyList(),
    val embarcacoes: List<EmbarcacaoOpcao> = emptyList(),
    val diasDaSemana: List<String> = emptyList(),
    val viagensExistentes: List<Viagem> = emptyList(),

    /**
     * A empresa não tem concessão que alcance rota **ou** embarcação — então não há o que oferecer, e a
     * tela precisa dizer isso em vez de mostrar dois dropdowns vazios. É o preço declarado da decisão de
     * recortar por atuação: provisionar virou pré-requisito.
     */
    val semConcessao: Boolean = false,

    val isProcessing: Boolean = false,
)

/** Uma rota **como opção de escolha**: o id que se grava e o rótulo que se lê. */
data class RotaOpcao(
    val id: String,
    val rotulo: String,
)

/** Idem para a embarcação — só as **concedidas** chegam aqui. */
data class EmbarcacaoOpcao(
    val id: String,
    val rotulo: String,
)

/**
 * A hora tem **três** maneiras de estar errada, e elas pedem frases diferentes: faltando, ilegível
 * (`"18h"`, `"25:00"`) e já ocupada — esta saída já existe no pool.
 *
 * A terceira é do **par (rota, embarcação, dia, hora)** e não da hora sozinha, mas é no campo da hora que
 * ela se resolve: os outros três costumam estar certos, e é a hora que se muda. Marcar os quatro campos
 * mandaria corrigir o que já está certo.
 */
enum class ErroHoraViagem(@StringRes val mensagem: Int) {
    NENHUM(R.string.error_camp_obrig),
    OBRIGATORIA(R.string.error_camp_obrig),
    INVALIDA(R.string.error_hora_invalida),
    DUPLICADA(R.string.error_viagem_duplicada);

    val existe: Boolean get() = this != NENHUM
}