package dev.matheus.fluviapp.ui.states.contagem

import dev.matheus.fluviapp.extensions.formatarDataBarrasBr
import dev.matheus.fluviapp.model.screendata.DadosContagemPassagem
import java.time.LocalDate

data class ContagemPassagemUiState(
    val listaDadosContagemPassagens: List<DadosContagemPassagem> = emptyList(),

    val dataViagem: String = LocalDate.now().formatarDataBarrasBr(),
    val onDataViagemChange: (String) -> Unit = {},
    val isDataViagemError: Boolean = false,

    val jaFoiGerado: Boolean = false,

    val isProcessing: Boolean = false
)
