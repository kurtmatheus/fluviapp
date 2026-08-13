package dev.matheus.fluviapp.ui.screens.passagem.emissao

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirportShuttle
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Elderly
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HotelClass
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import dev.matheus.fluviapp.domain.passagem.Acomodacao
import dev.matheus.fluviapp.domain.passagem.CategoriaPassagem
import dev.matheus.fluviapp.domain.passagem.ClasseVeiculo
import dev.matheus.fluviapp.domain.passagem.TipoGratuidade
import dev.matheus.fluviapp.domain.passagem.TipoPassagem
import dev.matheus.fluviapp.ui.components.passagem.EscolhaVisual
import dev.matheus.fluviapp.ui.components.passagem.GradeDeEscolhas
import dev.matheus.fluviapp.ui.components.passagem.ListaDeEscolhas

/**
 * **As cinco perguntas que se respondem com um toque** ([ADR-0029] D1).
 *
 * Todas elas percorrem `entries` do enum correspondente, e isso não é economia de digitação: é o que faz o
 * **domínio ser a fonte da tela**. Acrescentar `PassagemDeCarga` ou uma classe de veículo nova aparece aqui
 * sozinho — e o `when` do ícone, que é exaustivo, obriga alguém a decidir como ela se mostra.
 *
 * Cada função devolve as escolhas já com o **rótulo do domínio** (`Acomodacao.rotulo`, `ClasseVeiculo.rotulo`),
 * e não com texto de recurso: o rótulo é vocabulário de negócio e já mora no tipo, onde o teste o alcança.
 */
@Composable
fun EscolhaDeCategoria(aoEscolher: (CategoriaPassagem) -> Unit, modifier: Modifier = Modifier) {
    GradeDeEscolhas(
        modifier = modifier,
        escolhas = CategoriaPassagem.entries.map { categoria ->
            EscolhaVisual(
                rotulo = categoria.rotulo,
                icone = categoria.icone(),
                aoEscolher = { aoEscolher(categoria) },
            )
        },
    )
}

@Composable
fun EscolhaDeAcomodacao(aoEscolher: (Acomodacao) -> Unit, modifier: Modifier = Modifier) {
    GradeDeEscolhas(
        modifier = modifier,
        escolhas = Acomodacao.entries.map { acomodacao ->
            EscolhaVisual(
                rotulo = acomodacao.rotulo,
                icone = acomodacao.icone(),
                aoEscolher = { aoEscolher(acomodacao) },
                // Diz **quantos cabem**, que é o que distingue rede de suíte na hora de vender.
                descricao = if (acomodacao.ocupacaoMaxima > 1) "até ${acomodacao.ocupacaoMaxima}" else "1 pessoa",
            )
        },
    )
}

/**
 * O tipo tarifário — e a lista sai de [Acomodacao.tiposPermitidos], não de `TipoPassagem.entries`.
 *
 * A diferença é a regra: fora da rede o único tipo admitido é *inteira*, e quem sabe disso é a acomodação.
 * Listar todos e desabilitar dois seria mostrar ao operador escolhas que não existem.
 */
@Composable
fun EscolhaDeTipo(
    acomodacao: Acomodacao,
    aoEscolher: (TipoPassagem) -> Unit,
    modifier: Modifier = Modifier,
) {
    GradeDeEscolhas(
        modifier = modifier,
        escolhas = TipoPassagem.entries
            .filter { it in acomodacao.tiposPermitidos }
            .map { tipo ->
                EscolhaVisual(rotulo = tipo.rotulo(), icone = tipo.icone(), aoEscolher = { aoEscolher(tipo) })
            },
    )
}

@Composable
fun EscolhaDeGratuidade(aoEscolher: (TipoGratuidade) -> Unit, modifier: Modifier = Modifier) {
    GradeDeEscolhas(
        modifier = modifier,
        escolhas = TipoGratuidade.entries.map { gratuidade ->
            EscolhaVisual(
                rotulo = gratuidade.rotulo(),
                icone = gratuidade.icone(),
                aoEscolher = { aoEscolher(gratuidade) },
            )
        },
    )
}

/** Quantas pessoas — de 1 até o que a acomodação admite. Na rede este passo nem existe no roteiro. */
@Composable
fun EscolhaDeQuantidade(
    ocupacaoMaxima: Int,
    aoEscolher: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    GradeDeEscolhas(
        modifier = modifier,
        escolhas = (1..ocupacaoMaxima).map { quantidade ->
            EscolhaVisual(
                rotulo = if (quantidade == 1) "1 pessoa" else "$quantidade pessoas",
                icone = when (quantidade) {
                    1 -> Icons.Filled.Person
                    2 -> Icons.Filled.Group
                    else -> Icons.Filled.Groups
                },
                aoEscolher = { aoEscolher(quantidade) },
            )
        },
    )
}

/** A classe do veículo em **lista vertical** — é a pergunta mais longa do fluxo, e o rótulo é o que distingue. */
@Composable
fun EscolhaDeClasseDeVeiculo(aoEscolher: (ClasseVeiculo) -> Unit, modifier: Modifier = Modifier) {
    ListaDeEscolhas(
        modifier = modifier,
        escolhas = ClasseVeiculo.entries.map { classe ->
            EscolhaVisual(rotulo = classe.rotulo, icone = classe.icone(), aoEscolher = { aoEscolher(classe) })
        },
    )
}

// --- Os ícones: apresentação, e por isso aqui e não no domínio ---
//
// Os `when` são exaustivos de propósito. Quando a carga entrar em `CategoriaPassagem`, ou uma classe nova em
// `ClasseVeiculo`, o compilador vai parar **aqui** e cobrar a decisão de como ela se mostra — que é
// exatamente o que o tipo selado do ADR-0023 D1 comprou para o resto do app.

private fun CategoriaPassagem.icone(): ImageVector = when (this) {
    CategoriaPassagem.PASSAGEIRO -> Icons.Filled.Person
    CategoriaPassagem.VEICULO -> Icons.Filled.DirectionsCar
}

private fun Acomodacao.icone(): ImageVector = when (this) {
    Acomodacao.REDE -> Icons.Filled.Weekend
    Acomodacao.SUITE -> Icons.Filled.HotelClass
    Acomodacao.CAMAROTE -> Icons.Filled.MeetingRoom
}

private fun TipoPassagem.icone(): ImageVector = when (this) {
    TipoPassagem.INTEIRA -> Icons.Filled.ConfirmationNumber
    TipoPassagem.MEIA -> Icons.Filled.ChildCare
    TipoPassagem.GRATUIDADE -> Icons.Filled.VerifiedUser
}

private fun TipoGratuidade.icone(): ImageVector = when (this) {
    TipoGratuidade.IDOSO -> Icons.Filled.Elderly
    TipoGratuidade.PCD -> Icons.Filled.Accessible
    TipoGratuidade.CRIANCA_ATE_5 -> Icons.Filled.ChildCare
    TipoGratuidade.PASSE_FEDERAL -> Icons.Filled.VerifiedUser
}

private fun ClasseVeiculo.icone(): ImageVector = when (this) {
    ClasseVeiculo.CARRO -> Icons.Filled.DirectionsCar
    ClasseVeiculo.MOTO -> Icons.Filled.TwoWheeler
    ClasseVeiculo.VAN -> Icons.Filled.AirportShuttle
    ClasseVeiculo.SUV -> Icons.Filled.Terrain
    ClasseVeiculo.CAMINHAO -> Icons.Filled.LocalShipping
    ClasseVeiculo.CARRETA -> Icons.Filled.LocalShipping
}