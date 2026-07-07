package br.com.gruponaveg.ui.states.faturamento

import br.com.gruponaveg.extensions.formatarDataBarrasBr
import br.com.gruponaveg.model.screendata.DadosBalancoPassagem
import java.time.LocalDate

data class BalancoState(
    val listaDadosBalancoPassagens: List<DadosBalancoPassagem> = emptyList(),

    val dataViagem: String = LocalDate.now().formatarDataBarrasBr(),
    val onDataViagemChange: (String) -> Unit = {},
    val isDataViagemError: Boolean = false,

    val jaFoiGerado: Boolean = false,

    val isProcessing: Boolean = false
)
