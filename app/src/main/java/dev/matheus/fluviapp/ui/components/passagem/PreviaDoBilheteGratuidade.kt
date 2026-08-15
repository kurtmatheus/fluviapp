package dev.matheus.fluviapp.ui.components.passagem

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.matheus.fluviapp.extensions.formataParaMoedaBrasileira
import dev.matheus.fluviapp.ui.states.passagem.BilheteDigital
import dev.matheus.fluviapp.ui.states.passagem.PassageiroDoBilhete
import java.math.BigDecimal

/**
 * **Rede, uma pessoa, gratuidade de idoso** — o bilhete mais curto que a emissão produz.
 *
 * Duas coisas só aparecem aqui: a **linha de gratuidade**, que é o que a fiscalização confere contra a
 * credencial, e o **total zerado**, que precisa continuar legível como valor e não sumir da coluna.
 *
 * A agência não tem marca própria, então quem assina o documento é o FluviApp — é a única das quatro prévias
 * que desenha o wordmark no topo e na marca d'água, e é por isso que ela existe assim.
 */
internal val bilheteDeGratuidade = BilheteDigital(
    idPassagem = "previa-gratuidade",
    numero = "#12",
    agencia = "Fluvi Belém",
    travessia = "Porto de Val-de-Cães · Belém/PA → Porto de Santarém · Santarém/PA",
    partida = "Quinta-feira, 20/08 · 11:00",
    embarcacao = "N/M Modelo",
    bilhete = "Rede · Gratuidade",
    passageiros = listOf(
        PassageiroDoBilhete("Passageiro", "Raimunda Alves", "CPF 529.982.247-25"),
    ),
    total = BigDecimal("0.00").formataParaMoedaBrasileira(),
    gratuidade = "Idoso",
)

@Preview(name = "Gratuidade · rede", widthDp = 412, showBackground = true)
@Composable
private fun PreviaDoBilheteGratuidade() {
    ConteudoDoBilhete(bilhete = bilheteDeGratuidade)
}