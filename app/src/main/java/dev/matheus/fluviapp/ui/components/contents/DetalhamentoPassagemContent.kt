package dev.matheus.fluviapp.ui.components.contents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.sampledata.dadosPassagemSample
import dev.matheus.fluviapp.ui.components.texts.FluviWordmark
import dev.matheus.fluviapp.ui.components.texts.TextBoldNavyBlue
import dev.matheus.fluviapp.ui.theme.HeaderNavy
import dev.matheus.fluviapp.ui.theme.SteelTeal
import dev.matheus.fluviapp.ui.components.texts.TextRegularBrownItalic
import dev.matheus.fluviapp.ui.components.texts.TextTitleBrownItalic
import dev.matheus.fluviapp.ui.states.passagem.DetalhesPassagemState

@Composable
fun DetalhamentoPassagemContent(
    modifier: Modifier,
    state: DetalhesPassagemState,
) {
    Column(
        modifier = modifier
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FluviWordmark(
                modifier = modifier.height(65.dp),
                fontSize = 26.sp,
                // Superfície clara (papel): gradiente escuro navy/teal p/ legibilidade — o default
                // (teal→amarelo→ciano) é claro e some sobre fundo claro.
                fluviColor = SteelTeal,
                appGradient = listOf(SteelTeal, HeaderNavy, SteelTeal),
                strokeWidth = 3f,
            )

            TextRegularBrownItalic(
                text = stringResource(id = R.string.card_title_detalhes_passagem)
            )

            TextTitleBrownItalic(
                text = state.dadosPassagem.numero
            )

        }
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.onBackground
        )

        SecaoPassageiro(state, modifier)
        SecaoViagem(modifier, state)
        SecaoGeral(modifier, state)
    }
}

@Composable
private fun SecaoPassageiro(state: DetalhesPassagemState, modifier: Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        if (state.isShowAreaVeiculo) {
            if (state.dadosPassagem.temResponsavel) {
                CommonRowDetalhamento(
                    modifier = modifier,
                    label = R.string.label_nome_responsavel,
                    valor = state.dadosPassagem.nomeResponsavelRetirada
                )

                CommonRowDetalhamento(
                    modifier = modifier,
                    label = R.string.label_documento_responsavel,
                    valor = state.dadosPassagem.numeroDocumentoResponsavelRetirada,
                )
            }
            Spacer(modifier = modifier)

            CommonRowDetalhamento(
                modifier = modifier,
                label = R.string.label_tipo_veiculo,
                valor = state.dadosPassagem.tipoVeiculo
            )

            CommonRowDetalhamento(
                modifier = modifier,
                label = R.string.label_modelo_veiculo,
                valor = state.dadosPassagem.modeloVeiculo
            )

            CommonRowDetalhamento(
                modifier = modifier,
                label = R.string.label_placa_veículo,
                valor = state.dadosPassagem.placaVeiculo,
            )

            CommonRowDetalhamento(
                modifier = modifier,
                label = R.string.label_cor_veículo,
                valor = state.dadosPassagem.corVeiculo,
            )
        } else {
            CommonRowDetalhamento(
                modifier = modifier,
                label = R.string.label_nome_passageiro,
                valor = state.dadosPassagem.nomePassageiro1
            )

            CommonRowDetalhamento(
                modifier = modifier,
                label = R.string.label_numero_documento,
                valor = state.dadosPassagem.documentoPassageiro1,
            )
            CommonRowDetalhamento(
                modifier = modifier,
                label = R.string.label_data_nascimento,
                valor = state.dadosPassagem.dataNascimento1,
            )

            if (state.dadosPassagem.tem2Pessoas) {
                CommonRowDetalhamento(
                    modifier = modifier,
                    label = R.string.label_nome_passageiro,
                    valor = state.dadosPassagem.nomePassageiro2
                )

                CommonRowDetalhamento(
                    modifier = modifier,
                    label = R.string.label_numero_documento,
                    valor = state.dadosPassagem.documentoPassageiro2,
                )
                CommonRowDetalhamento(
                    modifier = modifier,
                    label = R.string.label_data_nascimento,
                    valor = state.dadosPassagem.dataNascimento2,
                )
            }

            if (state.dadosPassagem.tem3Pessoas) {
                CommonRowDetalhamento(
                    modifier = modifier,
                    label = R.string.label_nome_passageiro,
                    valor = state.dadosPassagem.nomePassageiro3
                )

                CommonRowDetalhamento(
                    modifier = modifier,
                    label = R.string.label_numero_documento,
                    valor = state.dadosPassagem.documentoPassageiro3,
                )
                CommonRowDetalhamento(
                    modifier = modifier,
                    label = R.string.label_data_nascimento,
                    valor = state.dadosPassagem.dataNascimento3,
                )
            }

            CommonRowDetalhamento(
                modifier = modifier,
                label = R.string.form_area_title_acomodacao,
                valor = state.dadosPassagem.acomodacao,
            )

            CommonRowDetalhamento(
                modifier = modifier,
                label = R.string.label_tipo_passagem,
                valor = state.dadosPassagem.tipoPassagem,
            )

            if (state.dadosPassagem.temGratuidade) {
                CommonRowDetalhamento(
                    modifier = modifier,
                    label = R.string.label_tipo_gratuidade,
                    valor = state.dadosPassagem.tipoGratuidade,
                )
            }

        }

        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun SecaoViagem(modifier: Modifier, state: DetalhesPassagemState) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CommonRowDetalhamento(
            modifier = modifier,
            label = R.string.label_empresa,
            valor = state.dadosPassagem.empresaNome
        )

        CommonRowDetalhamento(
            modifier = modifier,
            label = R.string.label_navio,
            valor = state.dadosPassagem.navio,
        )

        CommonRowDetalhamento(
            modifier = modifier,
            label = R.string.label_trecho_origem,
            valor = state.dadosPassagem.origem
        )

        CommonRowDetalhamento(
            modifier = modifier,
            label = R.string.label_trecho_destino,
            valor = state.dadosPassagem.destino,
        )

        CommonRowDetalhamento(
            modifier = modifier,
            label = R.string.label_data_viagem,
            valor = state.dadosPassagem.dataViagem,
        )

        CommonRowDetalhamento(
            modifier = modifier,
            label = R.string.label_hora_viagem,
            valor = state.dadosPassagem.horaViagem,
        )

        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun SecaoGeral(modifier: Modifier, state: DetalhesPassagemState) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

//        CommonRowDetalhamento(
//            modifier = modifier,
//            label = R.string.label_agencia,
//            dado = state.dadosPassagem.agencia,
//        )
//
//        CommonRowDetalhamento(
//            modifier = modifier,
//            label = R.string.label_agente,
//            dado = state.dadosPassagem.agente,
//        )

        CommonRowDetalhamento(
            modifier = modifier,
            label = R.string.label_valor_total,
            valor = state.dadosPassagem.valorTotal,
        )

        CommonRowDetalhamento(
            modifier = modifier,
            label = R.string.label_desconto,
            valor = state.dadosPassagem.desconto
        )


        CommonRowDetalhamento(
            modifier = modifier,
            label = R.string.label_valor_a_pagar,
            valor = state.dadosPassagem.valorAPagar
        )

        if (state.dadosPassagem.isFormaPagamentoEnabled) {
            Column(
                modifier = modifier.padding(5.dp, 0.dp)
            ) {
                if (state.dadosPassagem.valorPix.isNotEmpty()) {
                    CommonRowDetalhamento(
                        modifier = modifier,
                        label = R.string.label_valor_pix,
                        valor = state.dadosPassagem.valorPix,
                    )
                }

                if (state.dadosPassagem.valorDinheiro.isNotEmpty()) {
                    CommonRowDetalhamento(
                        modifier = modifier,
                        label = R.string.label_valor_dinheiro,
                        valor = state.dadosPassagem.valorDinheiro,
                    )
                }

                if (state.dadosPassagem.valorDebito.isNotEmpty()) {
                    CommonRowDetalhamento(
                        modifier = modifier,
                        label = R.string.label_valor_debito,
                        valor = state.dadosPassagem.valorDebito,
                    )
                }

                if (state.dadosPassagem.valorCredito.isNotEmpty()) {
                    CommonRowDetalhamento(
                        modifier = modifier,
                        label = R.string.label_valor_credito,
                        valor = state.dadosPassagem.valorCredito,
                    )
                }
            }
        }

        CommonRowDetalhamento(
            modifier = modifier,
            label = R.string.label_funcionario,
            valor = state.dadosPassagem.funcionario,
        )

        CommonRowDetalhamentoStatus(
            modifier = modifier,
            label = R.string.label_situacao,
            situacao = state.dadosPassagem.situacao,
        )

        CommonRowDetalhamento(
            modifier = modifier,
            label = R.string.label_obs,
            valor = "",
        )

        TextBoldNavyBlue(
            modifier = modifier.padding(0.10.dp),
            text = state.dadosPassagem.observacao
        )

        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DetalhamentoPassagemContentPreview() {
    DetalhamentoPassagemContent(
        modifier = Modifier,
        state = DetalhesPassagemState(
            dadosPassagem = dadosPassagemSample,
        )
    )
}
