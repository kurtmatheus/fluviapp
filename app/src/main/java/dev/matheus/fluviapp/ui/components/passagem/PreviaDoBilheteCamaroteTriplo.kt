package dev.matheus.fluviapp.ui.components.passagem

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.matheus.fluviapp.extensions.formataParaMoedaBrasileira
import dev.matheus.fluviapp.ui.states.passagem.BilheteDigital
import dev.matheus.fluviapp.ui.states.passagem.PassageiroDoBilhete
import java.math.BigDecimal

/**
 * **Camarote para três** — a ocupação máxima que uma acomodação admite, e por isso o bilhete mais alto.
 *
 * É o caso que mede a coluna: três blocos de pessoa entre os divisores, e ainda o total e o QR abaixo. Se o
 * rodapé precisar de rolagem ou a imagem gravada esticar demais, é aqui que se vê primeiro.
 */
internal val bilheteDeCamaroteTriplo = BilheteDigital(
    idPassagem = "previa-camarote-triplo",
    numero = "#58",
    agencia = "NAVEG",
    trajeto = "Porto de Val-de-Cães · Belém/PA → Porto de Manaus · Manaus/AM",
    partida = "Sábado, 22/08 · 19:30",
    embarcacao = "N/M Modelo",
    bilhete = "Camarote · 3 pessoas",
    passageiros = listOf(
        PassageiroDoBilhete("Titular", "Ana Ribeiro", "CPF 529.982.247-25"),
        PassageiroDoBilhete("Acompanhante", "Bruno Costa", "CPF 111.444.777-35"),
        PassageiroDoBilhete("Acompanhante", "Clara Menezes", "CPF 123.456.789-09"),
    ),
    total = BigDecimal("540.00").formataParaMoedaBrasileira(),
)

@Preview(name = "Camarote · três", widthDp = 412, showBackground = true)
@Composable
private fun PreviaDoBilheteCamaroteTriplo() {
    ConteudoDoBilhete(bilhete = bilheteDeCamaroteTriplo)
}