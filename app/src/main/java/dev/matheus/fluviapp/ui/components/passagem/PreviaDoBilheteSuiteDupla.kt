package dev.matheus.fluviapp.ui.components.passagem

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.matheus.fluviapp.extensions.formataParaMoedaBrasileira
import dev.matheus.fluviapp.ui.states.passagem.BilheteDigital
import dev.matheus.fluviapp.ui.states.passagem.PassageiroDoBilhete
import java.math.BigDecimal

/**
 * **Suíte para dois** — o bilhete que não é unitário (ADR-0028): uma acomodação, duas pessoas, um documento só.
 *
 * É onde se lê a hierarquia entre **titular** e **acompanhante**, que o mapper decide pela posição no
 * agregado — o titular é sempre o cliente 0. Na prévia principal ([bilheteDeRedeInteira]) não há hierarquia
 * a mostrar: com uma pessoa só, o papel é "Passageiro".
 */
internal val bilheteDeSuiteDupla = BilheteDigital(
    idPassagem = "previa-suite-dupla",
    numero = "#41",
    agencia = "NAVEG",
    trajeto = "Porto de Val-de-Cães · Belém/PA → Porto de Parintins · Parintins/AM",
    partida = "Terça-feira, 18/08 · 18:00",
    embarcacao = "F/B Modelo",
    bilhete = "Suíte · 2 pessoas",
    passageiros = listOf(
        PassageiroDoBilhete("Titular", "Ana Ribeiro", "CPF 529.982.247-25"),
        PassageiroDoBilhete("Acompanhante", "Bruno Costa", "CPF 111.444.777-35"),
    ),
    total = BigDecimal("300.00").formataParaMoedaBrasileira(),
)

@Preview(name = "Suíte · dois", widthDp = 412, showBackground = true)
@Composable
private fun PreviaDoBilheteSuiteDupla() {
    ConteudoDoBilhete(bilhete = bilheteDeSuiteDupla)
}