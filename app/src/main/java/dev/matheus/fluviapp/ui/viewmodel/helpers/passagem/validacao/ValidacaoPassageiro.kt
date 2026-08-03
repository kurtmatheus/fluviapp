package dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.validacao

import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.states.passagem.FormPassageiroUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Validação **pura** do sub-form de passageiro (molde ADR-0006, fatia 2): `(state, dataViagem) ->
 * ErrosPassageiro`, sem mutar estado. Substitui o `ValidacaoFormPassageiroHelper` impuro. Passageiros 2 e
 * 3 são acompanhantes **opcionais** (só validados quando marcados). A regra de idade da criança
 * (`CRIANCA_ATE_5`, ADR-0013) mora aqui e é JVM-testável.
 */
data class ErrosPassageiro(
    val acomodacao: Boolean = false,
    val tipoPassagem: Boolean = false,
    val tipoGratuidade: Boolean = false,
    val nomeP1: Boolean = false,
    val documentoP1: Boolean = false,
    /** @StringRes da mensagem do documento do titular: obrigatório × inválido (0 = sem erro). */
    val textDocumentoP1: Int = 0,
    val dataNascimentoP1: Boolean = false,
    /** @StringRes da mensagem do campo data de nascimento do titular (0 = sem erro). */
    val textDataNascimentoP1: Int = 0,
    val nomeP2: Boolean = false,
    val documentoP2: Boolean = false,
    val textDocumentoP2: Int = 0,
    val dataNascimentoP2: Boolean = false,
    val documentoP3: Boolean = false,
    val textDocumentoP3: Int = 0,
    val nomeP3: Boolean = false,
    val dataNascimentoP3: Boolean = false,
) {
    val valido: Boolean
        get() = !acomodacao && !tipoPassagem && !tipoGratuidade &&
            !nomeP1 && !documentoP1 && !dataNascimentoP1 &&
            !nomeP2 && !documentoP2 && !dataNascimentoP2 &&
            !documentoP3 && !nomeP3 && !dataNascimentoP3
}

fun validarPassageiro(state: FormPassageiroUiState, dataViagem: String): ErrosPassageiro {
    val gratuidadeCrianca = state.isGratuidade && state.tipoGratuidade.contains("CRIANCA", true)

    // Data de nascimento do titular: obrigatória; se gratuidade-criança, também limitada pela idade.
    val (dataNasc1Erro, textDataNasc1) = when {
        state.dataNascimentoPassageiro1.isBlank() -> true to R.string.error_camp_obrig
        gratuidadeCrianca && excedeIdadeCrianca(state.dataNascimentoPassageiro1, dataViagem) ->
            true to R.string.error_data_crianca
        else -> false to 0
    }

    // Documento: obrigatório quando um tipo foi escolhido e, desde o ADR-0020 D2, **válido para aquele
    // tipo** — CPF e CNPJ com dígito verificador conferido. Acompanhantes só valem quando marcados.
    val doc1 = validarDocumento(state.tipoDocumentoPassageiro1, state.documentoPassageiro1)
    val doc2 = if (state.isPassageiro2Checked) {
        validarDocumento(state.tipoDocumentoPassageiro2, state.documentoPassageiro2)
    } else {
        ErroDocumento()
    }
    val doc3 = if (state.isPassageiro3Checked) {
        validarDocumento(state.tipoDocumentoPassageiro3, state.documentoPassageiro3)
    } else {
        ErroDocumento()
    }

    return ErrosPassageiro(
        acomodacao = state.acomodacao.isBlank(),
        // Tipo tarifário só existe na REDE (ADR-0013): fora dela não se exige tipo nem gratuidade (inteira).
        tipoPassagem = state.ehAcomodacaoRede && state.tipoPassagem.isBlank(),
        tipoGratuidade = state.ehAcomodacaoRede && state.isGratuidade && state.tipoGratuidade.isBlank(),
        documentoP1 = doc1.erro,
        textDocumentoP1 = doc1.texto,
        nomeP1 = state.nomePassageiro1.isBlank(),
        dataNascimentoP1 = dataNasc1Erro,
        textDataNascimentoP1 = textDataNasc1,
        // Passageiro 2 (opcional): só valida quando marcado.
        nomeP2 = state.isPassageiro2Checked &&
            state.nomePassageiro2.isBlank() && state.documentoPassageiro2.isBlank(),
        documentoP2 = doc2.erro,
        textDocumentoP2 = doc2.texto,
        dataNascimentoP2 = state.isPassageiro2Checked && state.dataNascimentoPassageiro2.isBlank(),
        // Passageiro 3 (opcional): só valida quando marcado.
        documentoP3 = doc3.erro,
        textDocumentoP3 = doc3.texto,
        nomeP3 = state.isPassageiro3Checked && state.nomePassageiro3.isBlank(),
        dataNascimentoP3 = state.isPassageiro3Checked && state.dataNascimentoPassageiro3.isBlank(),
    )
}

private const val LIMITE_IDADE_CRIANCA_ANOS = 6L

/**
 * `CRIANCA_ATE_5` (ADR-0013): "até 5 anos inclusive" = nascido há **menos de 6 anos** na data da viagem
 * (limite exclusivo de 6). O número codifica exatamente essa faixa.
 */
private fun excedeIdadeCrianca(dataNascimento: String, dataViagem: String): Boolean {
    val formato = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val nascimento = LocalDate.parse(dataNascimento, formato)
    val viagem = LocalDate.parse(dataViagem, formato)
    return nascimento.isBefore(viagem.minusYears(LIMITE_IDADE_CRIANCA_ANOS).plusDays(1))
}