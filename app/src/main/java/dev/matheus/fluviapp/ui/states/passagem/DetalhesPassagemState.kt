package dev.matheus.fluviapp.ui.states.passagem

import dev.matheus.fluviapp.domain.screendata.DadosPassagem

data class DetalhesPassagemState(
    val dadosPassagem: DadosPassagem = DadosPassagem(),
    val isShowAreaVeiculo: Boolean = false,
    val isShowConfirmReturnDialog: Boolean = false,
    val isShowConfirmDeleteDialog: Boolean = false,
    val isAdminOuFuncResposavel: Boolean = false,

    val isShowSheetEmissao: Boolean = false,
    val isShowDialogEmissaoPassagemDigital: Boolean = false
)
