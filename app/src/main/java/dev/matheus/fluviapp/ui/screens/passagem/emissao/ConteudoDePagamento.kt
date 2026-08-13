package dev.matheus.fluviapp.ui.screens.passagem.emissao

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.passagem.FormaPagamento
import dev.matheus.fluviapp.extensions.formataParaMoedaBrasileira
import dev.matheus.fluviapp.ui.components.forms.fields.FormTextFieldBrownNoIcon
import dev.matheus.fluviapp.ui.components.texts.TextRegularBrownItalic
import dev.matheus.fluviapp.ui.components.texts.TextSubTitleBrownBold
import dev.matheus.fluviapp.ui.states.passagem.ErroDeEmissao
import dev.matheus.fluviapp.ui.states.passagem.LancamentoEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.PagamentoEmEdicao

/**
 * **O passo 5 — quanto entrou, por forma.**
 *
 * Marcar a forma acrescenta uma linha; desmarcar tira. É a tradução direta do modelo de **lançamentos**
 * ([ADR-0018] D11): as quatro colunas fixas (`valorPix`, `valorDinheiro`…) davam sempre quatro campos, com ou
 * sem uso, e somar por forma no momento da escrita descartava informação de forma irreversível.
 *
 * **O total é exibido, nunca digitado** ([ADR-0024] D4): ele é a soma das linhas. Um campo de total ao lado
 * da lista seria a chance permanente de os dois discordarem — e o que iria para o balanço seria o digitado.
 *
 * **A gratuidade não passa por aqui exigindo nada**: tarifa zero por lei não é pagamento de valor zero, então
 * o passo aceita seguir vazio quando o bilhete é gratuito. Registrar R$ 0,00 poluiria a análise por forma com
 * entradas que nunca existiram.
 */
@Composable
fun ConteudoDePagamento(
    pagamento: PagamentoEmEdicao,
    aoMudar: (PagamentoEmEdicao) -> Unit,
    erros: Set<ErroDeEmissao>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormaPagamento.entries.forEach { forma ->
                val marcada = pagamento.lancamentos.any { it.forma == forma }
                FilterChip(
                    selected = marcada,
                    onClick = {
                        val novas = if (marcada) {
                            pagamento.lancamentos.filterNot { it.forma == forma }
                        } else {
                            pagamento.lancamentos + LancamentoEmEdicao(forma)
                        }
                        aoMudar(pagamento.copy(lancamentos = novas))
                    },
                    label = { TextRegularBrownItalic(text = forma.rotulo) },
                    colors = FilterChipDefaults.filterChipColors(),
                )
            }
        }

        pagamento.lancamentos.forEach { lancamento ->
            FormTextFieldBrownNoIcon(
                modifier = Modifier.fillMaxWidth(),
                value = lancamento.valor,
                label = R.string.label_valor_recebido,
                onValueChange = { texto ->
                    val novas = pagamento.lancamentos.map {
                        if (it.forma == lancamento.forma) it.copy(valor = texto) else it
                    }
                    aoMudar(pagamento.copy(lancamentos = novas))
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = ErroDeEmissao.VALOR_INVALIDO in erros && lancamento.valorEmReais() == null,
                textoApoio = lancamento.forma.rotulo,
            )
        }

        if (ErroDeEmissao.SEM_PAGAMENTO in erros) {
            TextRegularBrownItalic(text = "Informe ao menos uma forma de pagamento")
        }

        // O **total não mora aqui** — mora na barra fixa, ao lado do botão de emitir. O teste em aparelho
        // mostrou por quê: com uma forma marcada e a observação abaixo, ele já saía da dobra num A56, e o
        // total é justamente o número que o operador confere contra o dinheiro na mão. Valor que se confere
        // não pode depender de rolagem.

        FormTextFieldBrownNoIcon(
            modifier = Modifier.fillMaxWidth(),
            value = pagamento.observacao,
            label = R.string.label_observacao,
            onValueChange = { aoMudar(pagamento.copy(observacao = it)) },
        )
    }
}