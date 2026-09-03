package dev.matheus.fluviapp.ui.screens.passagem.emissao

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.components.forms.fields.FormTextFieldBrownNoIcon
import dev.matheus.fluviapp.ui.states.passagem.ErroDeEmissao
import dev.matheus.fluviapp.ui.states.passagem.VeiculoEmEdicao
import dev.matheus.fluviapp.util.visualtransformation.PlacaVisualTransformation

/**
 * **O formulário do veículo — rearranjado pela natureza** ([ADR-0029] D2, [ADR-0031] D2).
 *
 * A classe já foi escolhida no passo anterior, e é a **natureza** dela que decide o que se pergunta:
 *
 * - **cilindrada** só na natureza `MOTOCICLO` — moto, quadriciclo e jet-ski. Nas outras o campo não é
 *   opcional, é **sem sentido**, e por isso ele não existe em vez de ficar em branco;
 * - **modelo, placa e cor** aparecem sempre. Destes, **só a placa é cobrada** — ela é a chave natural do
 *   pool. O modelo deixou de ser exigido em 2026-09-03 (ADR-0031): *não pedir* é diferente de *não deixar
 *   dizer*, e quem tem "Scania R450" no pátio precisa poder escrevê-lo.
 *
 * A régua que sobra é essa: **some o campo que não se aplica; fica o campo que se aplica e apenas não se
 * cobra.** O campo desabilitado não entra em nenhum dos dois casos — ele ocupa a tela e ainda faz o
 * operador se perguntar se deveria preenchê-lo.
 */
@Composable
fun FormularioDeVeiculo(
    veiculo: VeiculoEmEdicao,
    aoMudar: (VeiculoEmEdicao) -> Unit,
    erros: Set<ErroDeEmissao>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FormTextFieldBrownNoIcon(
            modifier = Modifier.fillMaxWidth(),
            value = veiculo.placa,
            label = R.string.label_placa_veículo,
            onValueChange = { aoMudar(veiculo.copy(placa = it.filter { c -> c.isLetterOrDigit() }.take(7))) },
            // A máscara existe porque a placa é **chave**: este pool não deveria acumular duplicata, e a
            // única fonte dela aqui é digitação errada (ADR-0018 D15).
            visualTransformation = PlacaVisualTransformation(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            isError = ErroDeEmissao.VEICULO_SEM_PLACA in erros,
        )

        // Oferecido em toda classe e cobrado em nenhuma (ADR-0031) — logo, sem estado de erro: não há
        // como estar errado o que não é exigido.
        FormTextFieldBrownNoIcon(
            modifier = Modifier.fillMaxWidth(),
            value = veiculo.modelo,
            label = R.string.label_modelo_veiculo,
            onValueChange = { aoMudar(veiculo.copy(modelo = it)) },
        )

        if (veiculo.classe?.exigeCilindrada == true) {
            FormTextFieldBrownNoIcon(
                modifier = Modifier.fillMaxWidth(),
                value = veiculo.cilindrada,
                label = R.string.label_cilindrada,
                onValueChange = { aoMudar(veiculo.copy(cilindrada = it.filter(Char::isDigit).take(4))) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = ErroDeEmissao.VEICULO_SEM_CILINDRADA in erros,
            )
        }

        // A cor não é exigida por classe nenhuma: ela ajuda a achar o veículo no pátio, não a cobrar tarifa.
        FormTextFieldBrownNoIcon(
            modifier = Modifier.fillMaxWidth(),
            value = veiculo.cor,
            label = R.string.label_cor_veículo,
            onValueChange = { aoMudar(veiculo.copy(cor = it)) },
        )
    }
}