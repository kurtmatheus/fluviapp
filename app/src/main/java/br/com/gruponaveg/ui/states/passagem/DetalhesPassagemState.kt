package br.com.gruponaveg.ui.states.passagem

import br.com.gruponaveg.model.screendata.DadosPassagem

data class DetalhesPassagemState(
    val dadosPassagem: DadosPassagem = DadosPassagem(),
    val isShowAreaVeiculo: Boolean = false,
    val isShowConfirmReturnDialog: Boolean = false,
    val isShowConfirmDeleteDialog: Boolean = false,
    val isAdminOuFuncResposavel: Boolean = false,

    val isShowSheetEmissao: Boolean = false,
    val isShowDialogImpressaoDigital: Boolean = false
)
