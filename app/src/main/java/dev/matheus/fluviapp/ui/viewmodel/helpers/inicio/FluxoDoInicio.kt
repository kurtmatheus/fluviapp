package dev.matheus.fluviapp.ui.viewmodel.helpers.inicio

import dev.matheus.fluviapp.domain.viagem.EscopoDoPool
import dev.matheus.fluviapp.domain.viagem.inicioDoPainel
import dev.matheus.fluviapp.services.repository.cadastro.localidade.LocalidadeRepository
import dev.matheus.fluviapp.services.repository.cadastro.porto.PortoRepository
import dev.matheus.fluviapp.services.repository.cadastro.rota.RotaRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmbarcacaoRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.ViagemRepository
import dev.matheus.fluviapp.ui.states.InicioDaTela
import dev.matheus.fluviapp.util.Relogio
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * **O Início como assinatura, e não como leitura** (defeito medido em 2026-08-17).
 *
 * Uma viagem inativada pelo painel continuava no card da tela inicial até o app ser reaberto. A causa não era
 * o dado: `ColecaoFirestore` mantém um listener e um `StateFlow` sempre atuais, e o `obterTodas()` que o
 * Início usava apenas **liga** esse listener e devolve o primeiro snapshot. O que faltava era o Início
 * assinar a fonte em vez de tirar uma fotografia dela no `init` do ViewModel.
 *
 * O sintoma tinha uma explicação a mais: cada tela recarregava a si mesma depois da **própria** escrita
 * (`PesquisaViagemViewModel.onInativar` chama `carregar()` em seguida), e nenhuma sabia da escrita das
 * outras. Com o fluxo, ninguém precisa avisar ninguém: quem escreve escreve, e quem mostra recompõe.
 *
 * ### Por que aqui, e não no ViewModel
 *
 * Mesmo motivo do [paraTela] neste pacote: o `MainScreenViewModel` depende de `FirebaseAuth` e de
 * `DataStore` e não se constrói numa JVM. Com a montagem do fluxo fora dele, a reatividade passa a ter
 * teste — e era justamente ela que não tinha.
 *
 * ### As duas coisas que a ordem aqui garante
 *
 * **Espera-se o primeiro snapshot de cada coleção antes de emitir.** Os `StateFlow` nascem com lista vazia, e
 * combinar cinco deles crus faria a primeira emissão dizer *"não há saída esta semana"* antes de qualquer
 * leitura — o recado errado no pior momento, que é exatamente o que [InicioDaTela.Carregando] existe para
 * evitar. Os `obterTod*()` iniciais são a espera: eles ligam o listener (idempotente) e suspendem até o
 * snapshot chegar, então a primeira emissão do `combine` já é verdade.
 *
 * **O escopo entra por valor.** Ele vem da sessão, e a sessão não muda sem a tela ser refeita — trocar de
 * empresa passa pela escolha de contexto. Reler a concessão a cada snapshot custaria uma consulta por
 * emissão para responder sempre o mesmo.
 */
fun fluxoDoInicio(
    escopo: EscopoDoPool,
    viagemRepository: ViagemRepository,
    rotaRepository: RotaRepository,
    portoRepository: PortoRepository,
    localidadeRepository: LocalidadeRepository,
    embarcacaoRepository: EmbarcacaoRepository,
    relogio: Relogio,
): Flow<InicioDaTela> = flow {
    // Liga os cinco listeners e espera o primeiro snapshot de cada um: vazio depois disto significa
    // *vazio*, e não *ainda não chegou*.
    viagemRepository.obterTodas()
    rotaRepository.obterTodas()
    portoRepository.obterTodos()
    localidadeRepository.obterTodas()
    embarcacaoRepository.obterTodos()

    emitAll(
        combine(
            viagemRepository.observarTodas(),
            rotaRepository.observarTodas(),
            portoRepository.observarTodos(),
            localidadeRepository.observarTodas(),
            embarcacaoRepository.observarTodas(),
        ) { viagens, rotas, portos, localidades, embarcacoes ->
            val rotulosDeLocalidade = localidades.associate { it.id to it.rotulo }
            val rotasPorId = rotas.associateBy { it.id }
            val portosPorId = portos.associate { porto -> porto.id to porto.rotuloCom(rotulosDeLocalidade) }
            val nomesDeEmbarcacao = embarcacoes.associate { it.id to it.descricaoNome }

            // O relógio é lido a cada emissão, e não uma vez: a janela de sete dias é relativa a *agora*, e
            // um snapshot que chega depois da meia-noite tem de ser recortado pelo dia de hoje.
            inicioDoPainel(
                escopo = escopo,
                viagens = viagens,
                rotasPorId = rotasPorId,
                agora = relogio.agora(),
            ).paraTela(
                rotasPorId = rotasPorId,
                portosPorId = portosPorId,
                embarcacoes = nomesDeEmbarcacao,
            )
        },
    )
}