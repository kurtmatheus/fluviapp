package dev.matheus.fluviapp.ui.states.faturamento

import dev.matheus.fluviapp.extensions.formatarDataBarrasBr
import dev.matheus.fluviapp.model.screendata.DadosBalancoPassagem
import java.time.LocalDate

data class BalancoState(
    val listaDadosBalancoPassagens: List<DadosBalancoPassagem> = emptyList(),

    val dataViagem: String = LocalDate.now().formatarDataBarrasBr(),
    val onDataViagemChange: (String) -> Unit = {},
    val isDataViagemError: Boolean = false,

    val jaFoiGerado: Boolean = false,

    val isProcessing: Boolean = false
)
