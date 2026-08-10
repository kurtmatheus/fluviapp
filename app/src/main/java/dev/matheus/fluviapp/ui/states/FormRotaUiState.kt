package dev.matheus.fluviapp.ui.states

import androidx.annotation.StringRes
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.rota.Rota

/**
 * Estado do formulário de rota — puro (só dados + flags), no molde do ADR-0006.
 *
 * **Não existe modo de edição**, e a ausência é a decisão: rota é imutável (ADR-0016 §7.1) — corrige-se
 * criando outra e inativando a anterior, e o descartado fica como registro. Por isso o estado não tem
 * `titulo` variável nem id de rota: esta tela só cria.
 *
 * [rotasExistentes] vem no estado porque a validação é pura: ela não consulta nada, decide sobre o que
 * lhe deram — como no Porto.
 */
data class FormRotaUiState(
    val portoOrigem: String = "",
    val portoDestino: String = "",
    val erroPar: ErroParRota = ErroParRota.NENHUM,

    /** Milhas náuticas, só dígitos e um separador — o VM filtra. */
    val distanciaMn: String = "",
    val isDistanciaError: Boolean = false,

    /** Horas. */
    val tempoMedioH: String = "",
    val isTempoError: Boolean = false,

    val portos: List<PortoOpcao> = emptyList(),
    val rotasExistentes: List<Rota> = emptyList(),

    /**
     * Menos de dois portos concedidos — não há par a formar (F8.3). É o preço declarado de recortar o
     * painel pela atuação: provisionar virou pré-requisito, e a tela diz isso em vez de mostrar dois
     * dropdowns vazios que pareceriam defeito.
     */
    val semConcessao: Boolean = false,

    val isProcessing: Boolean = false,
)

/** Um porto **como opção de escolha**: o id que se grava e o rótulo que se lê. */
data class PortoOpcao(
    val id: String,
    val rotulo: String,
)

/**
 * O par de portos tem **três** maneiras de estar errado, e elas pedem frases diferentes: faltando,
 * repetido (não é travessia) e já existente (o pool não duplica).
 *
 * É o mesmo motivo do `ErroNomePorto`: uma flag booleana obrigaria a tela a escolher uma mensagem só, e
 * a errada manda corrigir o que já está certo.
 */
enum class ErroParRota(@StringRes val mensagem: Int) {
    NENHUM(R.string.error_camp_obrig),
    OBRIGATORIO(R.string.error_camp_obrig),
    MESMO_PORTO(R.string.error_rota_mesmo_porto),
    DUPLICADA(R.string.error_rota_duplicada);

    val existe: Boolean get() = this != NENHUM
}