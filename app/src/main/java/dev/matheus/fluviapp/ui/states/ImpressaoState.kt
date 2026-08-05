package dev.matheus.fluviapp.ui.states

import dev.matheus.fluviapp.domain.screendata.DadosImpressora

data class ImpressaoState(
    val isPrinting: Boolean = false,

    val exibirDialogImpressoras: Boolean = false,
    val listaImpressorasPareadas: List<DadosImpressora> = emptyList(),

    val isShowDialogImpressaoViaEmbarcacao: Boolean = false,
) {
    companion object {
        var isPrinterSelected: Boolean = false
        var impressoraSelecionada: DadosImpressora = DadosImpressora()
    }
}
