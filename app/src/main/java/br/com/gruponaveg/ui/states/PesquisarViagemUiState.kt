package br.com.gruponaveg.ui.states

import androidx.room.Ignore
import br.com.gruponaveg.R
import br.com.gruponaveg.model.cadastro.constantes.Constante
import br.com.gruponaveg.model.screendata.DadosViagemCard
import br.com.gruponaveg.model.viagem.Empresa
import br.com.gruponaveg.model.viagem.Navio

data class PesquisarViagemUiState(
    val listaEmpresas: List<Empresa> = emptyList(),

    val isCheckedEmpresa: Boolean = false,
    val onCheckEmpresa: (Boolean) -> Unit = {},
    val empresa: String = "",
    val onEmpresaChange: (String) -> Unit = {},
    val isEmpresaError: Boolean = false,

    val listaNavios: List<Navio> = emptyList(),

    val isCheckedNavio: Boolean = false,
    val onCheckNavio: (Boolean) -> Unit = {},
    val navio: String = "",
    val onNavioChange: (String) -> Unit = {},
    val isNavioError: Boolean = false,

    val listaMunicipios: List<Constante> = emptyList(),

    val isCheckedTrecho: Boolean = false,
    val onCheckTrecho: (Boolean) -> Unit = {},
    val isTrechoError: Boolean = false,
    val textTrechoError: Int = R.string.error_selecione_opcao,

    val origem: String = "",
    val onOrigemChange: (String) -> Unit = {},

    val destino: String = "",
    val onDestinoChange: (String) -> Unit = {},

    val listaResultadoViagens: List<DadosViagemCard> = emptyList(),
    val dadosViagemCard: DadosViagemCard = DadosViagemCard(),

    val isShowDeleteDialog: Boolean = false,
    val onExibirConfirmDeleteDialog: (Boolean) -> Unit = {},
) {
    @Ignore
    val filtrarPorOrigem = isCheckedTrecho && origem.isNotBlank() && destino.isBlank()
    val filtrarPorDestino = isCheckedTrecho && origem.isBlank() && destino.isNotBlank()
    val filtrarPorOrigemDestino = isCheckedTrecho && origem.isNotBlank() && destino.isNotBlank()
}