package dev.matheus.fluviapp.ui.components.forms.areas.empresa

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.operacoes.Atuacao
import dev.matheus.fluviapp.domain.viagem.Embarcacao
import dev.matheus.fluviapp.ui.components.forms.fields.FormTextFieldBrownNoIcon
import dev.matheus.fluviapp.ui.states.FormEmpresaUiState
import dev.matheus.fluviapp.ui.states.PortoOpcao
import dev.matheus.fluviapp.util.visualtransformation.CnpjVisualTransformation

@Composable
fun ContentEmpresaAreaForm(
    modifier: Modifier,
    state: FormEmpresaUiState,
    onNomeChange: (String) -> Unit,
    onRazaoSocialChange: (String) -> Unit,
    onCnpjChange: (String) -> Unit,
    onEnderecoChange: (String) -> Unit,
    onTelefone1Change: (String) -> Unit,
    onTelefone2Change: (String) -> Unit,
    onAtuacaoToggle: (Atuacao) -> Unit = {},
    onEmbarcacaoToggle: (String) -> Unit = {},
    onPortoToggle: (String) -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FormTextFieldBrownNoIcon(
            modifier = modifier.fillMaxWidth(),
            value = state.nome,
            label = R.string.label_nome_empresa,
            onValueChange = onNomeChange,
            isError = state.isNomeError,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Next,
            ),
        )

        FormTextFieldBrownNoIcon(
            modifier = modifier.fillMaxWidth(),
            value = state.razaoSocial,
            label = R.string.label_razao_social,
            onValueChange = onRazaoSocialChange,
            isError = state.isRazaoSocialError,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Next,
            ),
        )

        FormTextFieldBrownNoIcon(
            modifier = modifier.fillMaxWidth(),
            value = state.cnpj,
            label = R.string.label_cnpj,
            onValueChange = onCnpjChange,
            isError = state.isCnpjError,
            textoErro = R.string.error_cnpj_invalido,
            visualTransformation = CnpjVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
            ),
        )

        FormTextFieldBrownNoIcon(
            modifier = modifier.fillMaxWidth(),
            value = state.endereco,
            label = R.string.label_endereco,
            onValueChange = onEnderecoChange,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
        )

        FormTextFieldBrownNoIcon(
            modifier = modifier.fillMaxWidth(),
            value = state.telefone1,
            label = R.string.label_telefone1,
            onValueChange = onTelefone1Change,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next,
            ),
        )

        FormTextFieldBrownNoIcon(
            modifier = modifier.fillMaxWidth(),
            value = state.telefone2,
            label = R.string.label_telefone2,
            onValueChange = onTelefone2Change,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done,
            ),
        )

        AreaAtuacoes(
            selecionadas = state.atuacoes,
            isError = state.isAtuacoesError,
            onAtuacaoToggle = onAtuacaoToggle,
        )

        // A concessão só existe para quem agencia — e por isso vem DEPOIS das atuações: é a resposta a
        // uma pergunta que a de cima acabou de fazer.
        if (state.concedeEmbarcacoes) {
            AreaConcessoes(
                embarcacoes = state.embarcacoes,
                concedidas = state.embarcacoesConcedidas,
                onEmbarcacaoToggle = onEmbarcacaoToggle,
            )
        }

        // A outra metade da concessão (F7): **onde** ela pode operar. Vem depois de "em quê" porque é
        // a que a Rota consome — e a rota só existe depois de haver porto.
        if (state.concedePortos) {
            AreaPortosConcedidos(
                portos = state.portos,
                concedidos = state.portosConcedidos,
                onPortoToggle = onPortoToggle,
            )
        }
    }
}

/**
 * **O que a parte faz** (ADR-0016 §4). Caixas de seleção, e não escolha única, porque uma empresa
 * exerce várias atuações ao mesmo tempo — é justamente aí que "tipo de empresa" falharia.
 *
 * As dormentes aparecem desabilitadas em vez de sumirem: elas existem no modelo, e escondê-las faria
 * parecer que a plataforma não as conhece (ADR-0016 §5).
 */
@Composable
private fun AreaAtuacoes(
    selecionadas: Set<Atuacao>,
    isError: Boolean,
    onAtuacaoToggle: (Atuacao) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.label_atuacoes),
            style = MaterialTheme.typography.titleSmall,
            color = if (isError) MaterialTheme.colorScheme.error else Color.Unspecified,
        )

        if (isError) {
            Text(
                text = stringResource(R.string.error_selecione_opcao),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Atuacao.entries.forEach { atuacao ->
            // O toque é da LINHA, não da caixinha: `toggleable` no Row (com `onCheckedChange = null` no
            // Checkbox, que passa a delegar) funde caixa e rótulo num nó só. Sem isso, cada Checkbox é um
            // nó anônimo sem vínculo com o texto ao lado — o leitor de tela anuncia quatro "caixa de
            // seleção, não marcada" indistinguíveis, e o alvo de toque é do tamanho da caixinha.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = atuacao in selecionadas,
                        enabled = atuacao.operante,
                        role = Role.Checkbox,
                        onValueChange = { onAtuacaoToggle(atuacao) },
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = atuacao in selecionadas,
                    onCheckedChange = null,
                    enabled = atuacao.operante,
                )
                Text(
                    text = atuacao.rotulo,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/**
 * **O que esta parte pode vender** (ADR-0016 §7.1). A lista é a frota inteira da plataforma, porque
 * agenciar é vender o que é dos outros — concede-se a **embarcação**, não o armador (7ª rodada), e foi
 * essa mudança que tornou a checagem direta: `concedeu(id)`, sem ir perguntar de quem é o navio.
 *
 * **Não marcar nada é uma resposta legítima**, e por isso não há validação aqui: frota nova nasce
 * não-concedida, que é o fail-closed assumido no ADR — a agência que ainda não representa ninguém é um
 * estado normal do cadastro, não um formulário pela metade.
 *
 * Mesmo `toggleable` no `Row` das atuações, e pelo mesmo motivo: funde caixa e rótulo num nó só, para o
 * leitor de tela anunciar *qual* embarcação em vez de "caixa de seleção, não marcada" repetida.
 */
@Composable
private fun AreaConcessoes(
    embarcacoes: List<Embarcacao>,
    concedidas: Set<String>,
    onEmbarcacaoToggle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.label_concessoes),
            style = MaterialTheme.typography.titleSmall,
        )

        if (embarcacoes.isEmpty()) {
            // Sem frota cadastrada a área ficaria em branco, e branco não explica nada: quem chegou aqui
            // esperando escolher precisa saber que o que falta é cadastrar embarcação, não achar o botão.
            Text(
                text = stringResource(R.string.msg_sem_embarcacoes),
                style = MaterialTheme.typography.bodySmall,
            )
            return@Column
        }

        embarcacoes.forEach { embarcacao ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = embarcacao.id in concedidas,
                        role = Role.Checkbox,
                        onValueChange = { onEmbarcacaoToggle(embarcacao.id) },
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = embarcacao.id in concedidas,
                    onCheckedChange = null,
                )
                Text(
                    text = "${embarcacao.descricaoNome} · ${embarcacao.tipo.rotulo}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/**
 * **Onde esta parte pode operar** — a segunda dimensão da concessão (ADR-0016 §7.1, F7).
 *
 * A lista é a de todos os portos ativos da plataforma, pela mesma razão da frota: operar num porto não é
 * ser dono dele. E é daqui que sai a *linha ofertável*: quem tem os dois portos de uma rota pode
 * vendê-la — a rota em si é do pool compartilhado, quem a criou não importa.
 *
 * Os inativos ficam de fora, e essa é a diferença para as atuações dormentes logo acima: atuação
 * desabilitada é vocabulário que a plataforma conhece e ainda não usa; porto inativo é um registro
 * aposentado — mantê-lo aqui seria oferecer conceder onde ninguém mais opera.
 */
@Composable
private fun AreaPortosConcedidos(
    portos: List<PortoOpcao>,
    concedidos: Set<String>,
    onPortoToggle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.label_concessoes_portos),
            style = MaterialTheme.typography.titleSmall,
        )

        if (portos.isEmpty()) {
            Text(
                text = stringResource(R.string.msg_sem_portos),
                style = MaterialTheme.typography.bodySmall,
            )
            return@Column
        }

        portos.forEach { porto ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = porto.id in concedidos,
                        role = Role.Checkbox,
                        onValueChange = { onPortoToggle(porto.id) },
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = porto.id in concedidos,
                    onCheckedChange = null,
                )
                Text(
                    text = porto.rotulo,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
