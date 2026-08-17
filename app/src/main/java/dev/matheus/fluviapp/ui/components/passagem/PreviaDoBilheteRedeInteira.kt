package dev.matheus.fluviapp.ui.components.passagem

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.matheus.fluviapp.extensions.formataParaMoedaBrasileira
import dev.matheus.fluviapp.ui.states.passagem.BilheteDigital
import dev.matheus.fluviapp.ui.states.passagem.PassageiroDoBilhete
import java.math.BigDecimal

/**
 * **A prévia principal** — uma pessoa, rede, inteira: o bilhete que o balcão emite o dia inteiro.
 *
 * É o exemplar de referência, e não por ser o mais completo, mas pelo contrário: é o mais **enxuto** que o
 * documento chega a ser. Sem gratuidade, sem veículo, sem acompanhante — só a travessia, quem viaja e o valor.
 * Se o desenho não se sustenta aqui, é porque estava sendo segurado por conteúdo que na maioria das emissões
 * não existe.
 *
 * A acomodação aparece **sozinha**, sem o tipo tarifário ao lado: o mapper só imprime o tipo quando ele não é
 * a inteira ([BilheteDigital] via `descricaoDoBilheteCompleta`), porque é o desvio que se fiscaliza — o
 * comum é o silêncio.
 *
 * É este exemplar que a prévia da [dev.matheus.fluviapp.ui.screens.passagem.BilheteScreen] monta, para a tela
 * e o desenho não divergirem.
 */
internal val bilheteDeRedeInteira = BilheteDigital(
    idPassagem = "previa-rede-inteira",
    numero = "#23",
    agencia = "NAVEG",
    trajeto = "Porto de Val-de-Cães · Belém/PA → Porto de Parintins · Parintins/AM",
    partida = "Terça-feira, 18/08 · 18:00",
    embarcacao = "F/B Modelo",
    bilhete = "Rede",
    passageiros = listOf(
        PassageiroDoBilhete(
            "Passageiro",
            "Ana Ribeiro",
            "CPF 529.982.247-25",
            dataNascimento = "10/01/1975"
        ),
    ),
    total = BigDecimal("150.00").formataParaMoedaBrasileira(),
)

@Preview(name = "Rede · inteira (principal)", widthDp = 412, showBackground = true)
@Composable
private fun PreviaDoBilheteRedeInteira() {
    ConteudoDoBilhete(bilhete = bilheteDeRedeInteira)
}