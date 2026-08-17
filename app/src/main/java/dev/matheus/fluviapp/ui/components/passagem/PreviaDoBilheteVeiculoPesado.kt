package dev.matheus.fluviapp.ui.components.passagem

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.matheus.fluviapp.extensions.formataParaMoedaBrasileira
import dev.matheus.fluviapp.ui.states.passagem.BilheteDigital
import dev.matheus.fluviapp.ui.states.passagem.PassageiroDoBilhete
import dev.matheus.fluviapp.ui.states.passagem.VeiculoConferido
import java.math.BigDecimal

/**
 * **Carreta** — a passagem de veículo, e a única das quatro em que quem viaja não é o assunto do bilhete.
 *
 * O `modelo` vem **nulo de propósito**: carreta e caminhão *já são* o modelo (`ClasseVeiculo.exigeModelo`),
 * de modo que a linha do veículo aqui tem um campo a menos que a de um carro. É o que esta prévia serve para
 * olhar — se a linha ficar com separador solto ou espaço sobrando, é neste desenho que aparece.
 *
 * O responsável pela retirada é **opcional** no domínio; está preenchido para que a seção de pessoas não
 * apareça vazia. Apagar essa linha é uma edição legítima, e mostra o outro caso.
 */
internal val bilheteDeVeiculoPesado = BilheteDigital(
    idPassagem = "previa-veiculo-pesado",
    numero = "#07",
    agencia = "NAVEG",
    trajeto = "Porto de Val-de-Cães · Belém/PA → Porto de Parintins · Parintins/AM",
    partida = "Terça-feira, 18/08 · 18:00",
    embarcacao = "B/A Modelo",
    bilhete = "Veículo",
    passageiros = listOf(
        PassageiroDoBilhete("Responsável pela retirada", "Célio Nogueira", "CPF 111.444.777-35"),
    ),
    veiculo = VeiculoConferido(
        placa = "PAA3G47",
        classe = "Carreta",
        modelo = null,
        cor = "Branca",
        cilindrada = null,
    ),
    total = BigDecimal("1200.00").formataParaMoedaBrasileira(),
    observacao = "Embarque do veículo até 2h antes da partida.",
)

@Preview(name = "Veículo · carreta", widthDp = 412, showBackground = true)
@Composable
private fun PreviaDoBilheteVeiculoPesado() {
    ConteudoDoBilhete(bilhete = bilheteDeVeiculoPesado)
}