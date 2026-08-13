package dev.matheus.fluviapp.ui.screens.passagem.emissao

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.passagem.Acomodacao
import dev.matheus.fluviapp.extensions.formataParaMoedaBrasileira
import dev.matheus.fluviapp.ui.components.forms.buttons.CommonIconButton
import dev.matheus.fluviapp.ui.components.passagem.CabecalhoDaEmissao
import dev.matheus.fluviapp.ui.components.passagem.TrilhaDePassos
import dev.matheus.fluviapp.ui.components.texts.TextSubTitleBrownBold
import dev.matheus.fluviapp.ui.components.texts.TextTitleBrownRegular
import dev.matheus.fluviapp.ui.screens.forms.CommonScreenNoBottom
import dev.matheus.fluviapp.ui.states.passagem.ClienteEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.EmissaoUiState
import dev.matheus.fluviapp.ui.states.passagem.PagamentoEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.ParticipanteEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.PassoDaEmissao
import dev.matheus.fluviapp.ui.states.passagem.VeiculoEmEdicao

/**
 * **A emissão como totem** ([ADR-0029]): uma pergunta por tela, o cabeçalho da saída sempre visível, e a
 * trilha dizendo onde se está.
 *
 * ### Por que uma tela só, com um `when`, e não uma tela por passo na navegação
 *
 * Porque o roteiro é **derivado do estado** (D3): quantos passos existem depende do que se escolheu. Pôr cada
 * passo como destino de navegação obrigaria a navegação a saber dessa regra — e ela voltaria a orquestrar a
 * emissão, que é justamente o que o ADR-0026 D3 tirou dela. Aqui a navegação conhece **um** destino, e quem
 * decide o que mostrar é o estado.
 *
 * Este `when` é exaustivo sobre [PassoDaEmissao]: passo novo no roteiro não compila sem tela.
 */
@Composable
fun EmissaoScreen(
    state: EmissaoUiState,
    onEscolherCategoria: (dev.matheus.fluviapp.domain.passagem.CategoriaPassagem) -> Unit = {},
    onEscolherAcomodacao: (Acomodacao) -> Unit = {},
    onEscolherTipo: (dev.matheus.fluviapp.domain.passagem.TipoPassagem) -> Unit = {},
    onEscolherGratuidade: (dev.matheus.fluviapp.domain.passagem.TipoGratuidade) -> Unit = {},
    onEscolherQuantidade: (Int) -> Unit = {},
    onEscolherClasse: (dev.matheus.fluviapp.domain.passagem.ClasseVeiculo) -> Unit = {},
    onPreencherPessoa: (Int, ClienteEmEdicao) -> Unit = { _, _ -> },
    onPreencherVeiculo: (VeiculoEmEdicao) -> Unit = {},
    onPreencherResponsavel: (ClienteEmEdicao?) -> Unit = {},
    onPreencherPagamento: (PagamentoEmEdicao) -> Unit = {},
    onAvancar: () -> Unit = {},
    onVoltar: () -> Unit = {},
    onPular: () -> Unit = {},
    onConfirmarEmissao: () -> Unit = {},
    onRevisar: () -> Unit = {},
    onClickVoltarTela: () -> Unit = {},
    onVerBilhete: (String) -> Unit = {},
    onNovaEmissao: () -> Unit = {},
) {
    CommonScreenNoBottom(
        titleTopAppBar = R.string.title_top_passagem,
        titleTopContent = R.string.subtitle_nova_passagem,
        isShowRightIcon = false,
        hasRefresh = false,
        isRefreshing = false,
        onClickVoltar = onClickVoltarTela,
    ) { modifier, _ ->
        // A conferência **substitui** o passo enquanto está aberta, e não se soma a ele: ela não é uma
        // pergunta a mais, é o que já foi respondido, devolvido para leitura. Por isso não há trilha aqui —
        // "5 de 6" continuaria valendo quando ela fechar.
        val emConferencia = state.confirmacao
        if (emConferencia != null) {
            Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                CabecalhoDaEmissao(state.cabecalho)
                if (state.emitindo) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    DetalhamentoDaEmissao(
                        confirmacao = emConferencia,
                        onConfirmar = onConfirmarEmissao,
                        onRevisar = onRevisar,
                    )
                }
            }
            return@CommonScreenNoBottom
        }

        // `navigationBarsPadding` na **coluna inteira**, e não só na barra de ação: o rodapé desta tela é
        // fixo, então tudo o que fica abaixo da área rolável — o total e os botões — cai atrás da barra de
        // gestos do sistema sem ele. Foi o teste em aparelho que cobrou; numa prévia o layout parecia certo.
        Column(modifier = modifier.fillMaxSize().navigationBarsPadding()) {
            CabecalhoDaEmissao(state.cabecalho)
            TrilhaDePassos(numeroDoPasso = state.numeroDoPasso, totalDePassos = state.totalDePassos)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextTitleBrownRegular(text = perguntaDo(state.passo, state))

                when (val passo = state.passo) {
                    PassoDaEmissao.Categoria -> EscolhaDeCategoria(aoEscolher = onEscolherCategoria)

                    PassoDaEmissao.EscolhaDeAcomodacao -> EscolhaDeAcomodacao(aoEscolher = onEscolherAcomodacao)

                    PassoDaEmissao.EscolhaDeTipo -> EscolhaDeTipo(
                        acomodacao = state.bilhete.acomodacao ?: Acomodacao.REDE,
                        aoEscolher = onEscolherTipo,
                    )

                    PassoDaEmissao.EscolhaDeGratuidade -> EscolhaDeGratuidade(aoEscolher = onEscolherGratuidade)

                    PassoDaEmissao.QuantidadeDePessoas -> EscolhaDeQuantidade(
                        ocupacaoMaxima = state.bilhete.ocupacaoMaxima,
                        aoEscolher = onEscolherQuantidade,
                    )

                    PassoDaEmissao.ClasseDoVeiculo -> EscolhaDeClasseDeVeiculo(aoEscolher = onEscolherClasse)

                    PassoDaEmissao.DadosDoVeiculo -> FormularioDeVeiculo(
                        veiculo = (state.participante as? ParticipanteEmEdicao.DeVeiculo)?.veiculo
                            ?: VeiculoEmEdicao(),
                        aoMudar = onPreencherVeiculo,
                        erros = state.erros,
                    )

                    is PassoDaEmissao.DadosDoCliente -> FormularioDeCliente(
                        cliente = pessoaDo(passo, state),
                        aoMudar = { pessoa ->
                            if (passo.opcional) onPreencherResponsavel(pessoa)
                            else onPreencherPessoa(passo.indice, pessoa)
                        },
                        erros = state.erros,
                    )

                    PassoDaEmissao.Pagamento -> ConteudoDePagamento(
                        pagamento = state.pagamento,
                        aoMudar = onPreencherPagamento,
                        erros = state.erros,
                    )

                    PassoDaEmissao.Desfecho -> ConteudoDoDesfecho(
                        idPassagem = state.idEmitida,
                        onVerBilhete = onVerBilhete,
                        onNovaEmissao = onNovaEmissao,
                    )
                }

                // **Os botões moram com o formulário**, dentro da rolagem — e não numa barra fixa.
                //
                // Nos passos de escolha não há botão nenhum (o toque já responde), então uma barra fixa só
                // existiria para o formulário: ela ocupava altura em toda tela para servir a três delas, e
                // amarrava o arranjo dos botões ao rodapé. Aqui eles acompanham o campo que os habilita,
                // e o passo escolhe livremente como dispô-los.
                BotoesDoPasso(
                    state = state,
                    onAvancar = onAvancar,
                    onVoltar = onVoltar,
                    onPular = onPular,
                )
            }
        }
    }
}

/**
 * **Os botões do passo**, logo abaixo do formulário que eles fecham.
 *
 * Os passos de escolha **não têm botão nenhum**: o toque na opção já é a resposta e já anda ([ADR-0029] D1).
 * É por isso que eles não moram numa barra fixa — ela reservaria altura em toda tela para servir às três que
 * têm formulário, e amarraria o arranjo ao rodapé.
 *
 * O **total** vem junto, imediatamente acima do "Emitir": é o número que o operador confere contra o dinheiro
 * na mão, e o lugar dele é ao lado do gesto que cobra.
 */
@Composable
private fun BotoesDoPasso(
    state: EmissaoUiState,
    onAvancar: () -> Unit,
    onVoltar: () -> Unit,
    onPular: () -> Unit,
) {
    val passo = state.passo
    val ehEscolha = passo !is PassoDaEmissao.DadosDoCliente &&
        passo != PassoDaEmissao.DadosDoVeiculo &&
        passo != PassoDaEmissao.Pagamento

    if (state.emitindo) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    if (passo == PassoDaEmissao.Pagamento) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            TextSubTitleBrownBold(text = "Total: ${state.pagamento.total.formataParaMoedaBrasileira()}")
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.podeVoltar) {
            CommonIconButton(
                modifier = Modifier,
                onClick = onVoltar,
                text = "Voltar",
                width = 150,
                color = MaterialTheme.colorScheme.secondary,
                icon = { Icon(Icons.Filled.ArrowBack, contentDescription = null) },
            )
        }

        if (passo is PassoDaEmissao.DadosDoCliente && passo.opcional) {
            CommonIconButton(
                modifier = Modifier,
                onClick = onPular,
                text = "Pular",
                width = 150,
                color = MaterialTheme.colorScheme.secondary,
                icon = { Icon(Icons.Filled.SkipNext, contentDescription = null) },
            )
        }

        if (!ehEscolha) {
            CommonIconButton(
                modifier = Modifier,
                onClick = onAvancar,
                text = if (passo == PassoDaEmissao.Pagamento) "Emitir" else "Continuar",
                width = 170,
                icon = {
                    Icon(
                        if (passo == PassoDaEmissao.Pagamento) Icons.Filled.ConfirmationNumber
                        else Icons.Filled.Check,
                        contentDescription = null,
                    )
                },
            )
        }
    }
}

/** A pergunta do passo — e é ela que faz cada tela ter **uma** coisa a responder. */
private fun perguntaDo(passo: PassoDaEmissao, state: EmissaoUiState): String = when (passo) {
    PassoDaEmissao.Categoria -> "O que vai embarcar?"
    PassoDaEmissao.EscolhaDeAcomodacao -> "Onde o passageiro viaja?"
    PassoDaEmissao.EscolhaDeTipo -> "Qual o tipo do bilhete?"
    PassoDaEmissao.EscolhaDeGratuidade -> "Qual a gratuidade?"
    PassoDaEmissao.QuantidadeDePessoas -> "Quantas pessoas?"
    PassoDaEmissao.ClasseDoVeiculo -> "Qual o tipo do veículo?"
    PassoDaEmissao.DadosDoVeiculo -> "Dados do veículo"
    is PassoDaEmissao.DadosDoCliente -> when {
        passo.opcional -> "Quem retira o veículo? (opcional)"
        passo.indice == 0 && state.bilhete.ocupacaoMaxima > 1 -> "Quem é o titular?"
        passo.indice == 0 -> "Quem viaja?"
        else -> "Acompanhante ${passo.indice}"
    }

    PassoDaEmissao.Pagamento -> "Como foi pago?"
    PassoDaEmissao.Desfecho -> "Passagem emitida"
}

private fun pessoaDo(passo: PassoDaEmissao.DadosDoCliente, state: EmissaoUiState): ClienteEmEdicao =
    when (val participante = state.participante) {
        is ParticipanteEmEdicao.DePassageiro -> participante.pessoas.getOrElse(passo.indice) { ClienteEmEdicao() }
        is ParticipanteEmEdicao.DeVeiculo -> participante.responsavel ?: ClienteEmEdicao()
    }

/**
 * **O passo 6 — o desfecho** ([ADR-0029] D5).
 *
 * Ele nasce com **uma** saída, o bilhete digital, e com a forma preparada para ter mais de uma: a impressão
 * física traz surface própria (térmica, Bluetooth) e **vias** com destinatários diferentes — navio, agência,
 * cliente —, cada uma mostrando coisas distintas. Isso é estudo e ADR próprios; antecipá-los aqui seria
 * desenhar contra o que ainda não se sabe.
 */
@Composable
private fun ConteudoDoDesfecho(
    idPassagem: String?,
    onVerBilhete: (String) -> Unit,
    onNovaEmissao: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            Icons.Filled.ConfirmationNumber,
            contentDescription = null,
            modifier = Modifier.padding(8.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        TextSubTitleBrownBold(text = "A passagem foi emitida.")

        idPassagem?.let { id ->
            CommonIconButton(
                modifier = Modifier,
                onClick = { onVerBilhete(id) },
                text = "Bilhete digital",
                icon = { Icon(Icons.Filled.ConfirmationNumber, contentDescription = null) },
            )
        }

        CommonIconButton(
            modifier = Modifier,
            onClick = onNovaEmissao,
            text = "Nova passagem",
            color = MaterialTheme.colorScheme.secondary,
            icon = { Icon(Icons.Filled.Check, contentDescription = null) },
        )
    }
}