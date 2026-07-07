package dev.matheus.fluviapp.ui.states

import dev.matheus.fluviapp.model.screendata.DadosImpressora

data class ImpressaoState(
    val isPrinting: Boolean = false,

    val exibirDialogImpressoras: Boolean = false,
    val listaImpressorasPareadas: List<DadosImpressora> = emptyList(),

    val isShowDialogImpressaoViaNavio: Boolean = false,
) {
    companion object {
        var isPrinterSelected: Boolean = false
        var impressoraSelecionada: DadosImpressora = DadosImpressora()
    }
}
