package dev.matheus.fluviapp.ui.screens.forms.viagem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.viagem.DIAS_DA_SEMANA
import dev.matheus.fluviapp.domain.viagem.rotulo
import dev.matheus.fluviapp.ui.components.forms.areas.CommonAreaForm
import dev.matheus.fluviapp.ui.components.forms.buttons.CommonIconButton
import dev.matheus.fluviapp.ui.components.forms.dropdowns.DropDownFormField
import dev.matheus.fluviapp.ui.components.forms.fields.FormTextFieldBrownNoIcon
import dev.matheus.fluviapp.ui.components.texts.TextRegularBrown
import dev.matheus.fluviapp.ui.screens.forms.CommonScreenNoBottom
import dev.matheus.fluviapp.ui.states.EmbarcacaoOpcao
import dev.matheus.fluviapp.ui.states.FormViagemUiState
import dev.matheus.fluviapp.ui.states.RotaOpcao

@Composable
fun FormViagemScreen(
    uiState: FormViagemUiState,
    onRotaChange: (String) -> Unit = {},
    onEmbarcacaoChange: (String) -> Unit = {},
    onDiaSemanaChange: (String) -> Unit = {},
    onHoraChange: (String) -> Unit = {},
    onClickSalvar: () -> Unit = {},
    onClickVoltar: () -> Unit = {},
) {
    CommonScreenNoBottom(
        titleTopAppBar = R.string.title_top_viagens,
        titleTopContent = 0,
        isShowRightIcon = false,
        hasRefresh = false,
        isRefreshing = false,
        onClickVoltar = onClickVoltar,
    ) { modifier, _ ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CommonAreaForm(modifier = modifier, titleArea = R.string.subtitle_nova_viagem) {
                if (uiState.semConcessao) {
                    // Sem concessão não há o que oferecer, e dois dropdowns vazios pareceriam defeito.
                    // A mensagem manda a pessoa ao lugar certo: quem provisiona é a plataforma.
                    TextRegularBrown(text = stringResource(R.string.msg_viagem_sem_concessao))
                } else {
                    // A rota traz o par de portos; a embarcação, o em quê. As duas listas já chegam
                    // recortadas pela concessão — o que não se pode ofertar não aparece.
                    DropDownFormField(
                        modifier = it.fillMaxWidth(),
                        listaItens = uiState.rotas.map { rota -> rota.rotulo },
                        label = R.string.label_rota,
                        value = uiState.rota,
                        isError = uiState.isRotaError,
                        onValueChange = onRotaChange,
                    )
                    DropDownFormField(
                        modifier = it.fillMaxWidth(),
                        listaItens = uiState.embarcacoes.map { emb -> emb.rotulo },
                        label = R.string.label_embarcacao,
                        value = uiState.embarcacao,
                        isError = uiState.isEmbarcacaoError,
                        onValueChange = onEmbarcacaoChange,
                    )

                    // Dia e hora andam juntos porque juntos é que significam uma saída (§7.1).
                    DropDownFormField(
                        modifier = it.fillMaxWidth(),
                        listaItens = uiState.diasDaSemana,
                        label = R.string.label_dia_semana,
                        value = uiState.diaSemana,
                        isError = uiState.isDiaSemanaError,
                        onValueChange = onDiaSemanaChange,
                    )
                    FormTextFieldBrownNoIcon(
                        modifier = it.fillMaxWidth(),
                        value = uiState.hora,
                        label = R.string.label_hora_partida,
                        onValueChange = onHoraChange,
                        isError = uiState.erroHora.existe,
                        textoErro = uiState.erroHora.mensagem,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )

                    // A tela diz que não há edição — quem procurar por ela precisa saber o que fazer no
                    // lugar. Mesma frase da rota, e pela razão mais forte: o bilhete aponta para aqui.
                    TextRegularBrown(text = stringResource(R.string.msg_viagem_imutavel))
                }
            }

            if (!uiState.semConcessao) {
                Column(
                    modifier = modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CommonIconButton(
                        modifier = modifier,
                        onClick = onClickSalvar,
                        text = stringResource(id = R.string.btn_salvar),
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = stringResource(id = R.string.description_confirmacao),
                            )
                        },
                        color = MaterialTheme.colorScheme.primary,
                        isProcessing = uiState.isProcessing,
                    )
                }
            }
        }
    }
}

private val rotasDeExemplo = listOf(
    RotaOpcao("r1", "Porto de Val-de-Cães · Belém/PA → Porto de Parintins · Parintins/AM"),
)

@Preview(showBackground = true)
@Composable
private fun FormViagemScreenPreview() {
    FormViagemScreen(
        uiState = FormViagemUiState(
            rotas = rotasDeExemplo,
            rota = rotasDeExemplo.first().rotulo,
            embarcacoes = listOf(EmbarcacaoOpcao("e1", "F/B Modelo")),
            embarcacao = "F/B Modelo",
            diasDaSemana = DIAS_DA_SEMANA.map { it.rotulo },
            diaSemana = DIAS_DA_SEMANA[1].rotulo,
            hora = "18:00",
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun FormViagemScreenSemConcessaoPreview() {
    FormViagemScreen(uiState = FormViagemUiState(semConcessao = true))
}