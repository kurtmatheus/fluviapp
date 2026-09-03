package dev.matheus.fluviapp.ui.screens.passagem.emissao

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.Agriculture
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
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import dev.matheus.fluviapp.domain.passagem.Acomodacao
import dev.matheus.fluviapp.domain.passagem.CategoriaPassagem
import dev.matheus.fluviapp.domain.passagem.ClasseVeiculo
import dev.matheus.fluviapp.domain.passagem.NaturezaVeiculo
import dev.matheus.fluviapp.domain.passagem.TipoGratuidade
import dev.matheus.fluviapp.domain.passagem.TipoPassagem
import dev.matheus.fluviapp.domain.viagem.TipoEmbarcacao
import dev.matheus.fluviapp.ui.components.passagem.EscolhaVisual
import dev.matheus.fluviapp.ui.components.passagem.GradeDeEscolhas
import dev.matheus.fluviapp.ui.components.passagem.ListaDeEscolhas

// --- As perguntas que se respondem com um toque (ADR-0029 D1) ---
//
// Todas saem do **domínio**, e não de texto de recurso: o rótulo é vocabulário de negócio, já mora no tipo,
// e é lá que o teste o alcança.
//
// O que muda entre elas é **de onde sai a lista**, e a régua é uma só: quem conhece a restrição é quem
// fornece as opções. Três já a seguem — o tipo tarifário vem da acomodação, e a categoria e a classe de
// veículo vêm do **casco** (desde 2026-09-03). O resto percorre `entries` porque não há restrição a aplicar.
//
// A alternativa que essa régua rejeita é sempre a mesma: listar tudo e desabilitar o que não vale. Mostrar
// escolha que não existe é pior do que não mostrá-la.

/**
 * A categoria — e a lista é **recortada pelo casco** ([ADR-0016] §8, ligado em 2026-09-03).
 *
 * *"Não se vende veículo para uma lancha."* Oferecer a categoria e descobrir no passo seguinte que não há
 * classe nenhuma seria fazer o operador percorrer um caminho que termina em parede.
 *
 * **Casco desconhecido oferece tudo.** Falhar em resolver a embarcação não impede vender (é o mesmo
 * princípio do cabeçalho), e recusar por não saber transformaria uma referência que não carregou numa
 * venda que não acontece.
 */
@Composable
fun EscolhaDeCategoria(
    tipoEmbarcacao: TipoEmbarcacao?,
    aoEscolher: (CategoriaPassagem) -> Unit,
    modifier: Modifier = Modifier,
) {
    GradeDeEscolhas(
        modifier = modifier,
        escolhas = CategoriaPassagem.entries
            .filter { it != CategoriaPassagem.VEICULO || tipoEmbarcacao?.levaVeiculo != false }
            .map { categoria ->
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

/**
 * A classe do veículo em **lista vertical** — é a pergunta mais longa do fluxo, e o rótulo é o que distingue.
 *
 * A lista sai de [TipoEmbarcacao.classesAdmitidas], e não de `ClasseVeiculo.entries`: **quem sabe o que
 * cabe é o casco**. É o mesmo desenho de [EscolhaDeTipo], que já lia a lista da acomodação em vez de listar
 * tudo e desabilitar o que não vale — mostrar escolhas que não existem é pior do que não mostrá-las.
 *
 * Foi este ponto que manteve `TipoEmbarcacao.admite()` sem chamador de produção desde 2026-08-03: a regra
 * existia, era testada, e ninguém a consultava.
 */
@Composable
fun EscolhaDeClasseDeVeiculo(
    tipoEmbarcacao: TipoEmbarcacao?,
    aoEscolher: (ClasseVeiculo) -> Unit,
    modifier: Modifier = Modifier,
) {
    ListaDeEscolhas(
        modifier = modifier,
        escolhas = (tipoEmbarcacao?.classesAdmitidas ?: ClasseVeiculo.entries).map { classe ->
            EscolhaVisual(rotulo = classe.rotulo, icone = classe.icone(), aoEscolher = { aoEscolher(classe) })
        },
    )
}

// --- Os ícones: apresentação, e por isso aqui e não no domínio ---
//
// Os `when` são exaustivos de propósito: quando a carga entrar em `CategoriaPassagem`, ou uma **natureza**
// nova em `NaturezaVeiculo`, o compilador para **aqui** e cobra a decisão de como ela se mostra — que é o
// que o tipo selado do ADR-0023 D1 comprou para o resto do app.
//
// Desde o ADR-0031 a cobrança mudou de altitude: uma **classe** nova não para mais aqui, porque ela herda o
// ícone da natureza. É troca consciente — dezessete ramos para quatro desenhos não seria decisão de
// produto, seria burocracia.

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

/**
 * **O ícone da classe é o da natureza dela** (ADR-0031).
 *
 * Quando eram seis classes, um ícone por valor era barato — e mesmo assim `CAMINHAO` e `CARRETA` já
 * dividiam o mesmo. Com dezessete, seriam dezessete ramos para talvez quatro desenhos distintos, e o que a
 * exaustividade cobraria não seria uma decisão de produto: seria burocracia.
 *
 * O `when` continua exaustivo, mas sobre a **natureza** — quatro valores. Uma classe nova não precisa de
 * ícone; uma **natureza** nova precisa, e essa é a decisão que vale a pena o compilador cobrar.
 */
private fun ClasseVeiculo.icone(): ImageVector = natureza.icone()

private fun NaturezaVeiculo.icone(): ImageVector = when (this) {
    NaturezaVeiculo.AUTOMOTOR -> Icons.Filled.DirectionsCar
    NaturezaVeiculo.MOTOCICLO -> Icons.Filled.TwoWheeler
    NaturezaVeiculo.MAQUINA -> Icons.Filled.Agriculture
    NaturezaVeiculo.REBOCADO -> Icons.Filled.LocalShipping
}