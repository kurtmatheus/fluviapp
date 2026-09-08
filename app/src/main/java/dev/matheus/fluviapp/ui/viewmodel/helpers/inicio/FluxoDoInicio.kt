package dev.matheus.fluviapp.ui.viewmodel.helpers.inicio

import dev.matheus.fluviapp.domain.operacoes.metricasDeAcesso
import dev.matheus.fluviapp.domain.viagem.EscopoDoPool
import dev.matheus.fluviapp.domain.viagem.inicioDoPainel
import dev.matheus.fluviapp.services.repository.cadastro.localidade.LocalidadeRepository
import dev.matheus.fluviapp.services.repository.cadastro.porto.PortoRepository
import dev.matheus.fluviapp.services.repository.cadastro.rota.RotaRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmbarcacaoRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.ViagemRepository
import dev.matheus.fluviapp.services.repository.operacoes.ConviteRepository
import dev.matheus.fluviapp.services.repository.operacoes.UsuarioRepository
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
 * perfil reinicia esta assinatura ([ADR-0032] D5). Reler a concessão a cada snapshot custaria uma consulta
 * por emissão para responder sempre o mesmo.
 *
 * ### O escopo decide **antes** de ler, e não depois (2026-09-08)
 *
 * Até aqui o fluxo ligava os cinco listeners, esperava os cinco primeiros snapshots e só então perguntava
 * ao domínio o que mostrar — e para dois dos três escopos o domínio **descartava tudo**: `Todo` devolve o
 * painel da plataforma e `Nenhum` devolve "falta provisionar", nenhum dos dois olhando viagem nenhuma.
 * Cinco listeners e cinco esperas para desenhar uma tela que não depende deles.
 *
 * Com as métricas de acesso entrando no painel da plataforma (D6), isso deixou de ser só desperdício:
 * seriam sete coleções combinadas, e a linguagem só tem `combine` tipado até cinco. Ramificar por escopo
 * primeiro é o que faz cada painel ler **as coleções dele**.
 */
fun fluxoDoInicio(
    escopo: EscopoDoPool,
    viagemRepository: ViagemRepository,
    rotaRepository: RotaRepository,
    portoRepository: PortoRepository,
    localidadeRepository: LocalidadeRepository,
    embarcacaoRepository: EmbarcacaoRepository,
    relogio: Relogio,
    /** As duas coleções do painel da plataforma — `null` quando quem olha não é dela. Ver [fluxoDoAcesso]. */
    acesso: FontesDoAcesso? = null,
): Flow<InicioDaTela> = when (escopo) {
    // A plataforma não vende: o Início dela é o que ela administra (D6).
    EscopoDoPool.Todo -> fluxoDoAcesso(acesso, relogio)

    // "Falta provisionar" não depende de leitura nenhuma — é a ausência de concessão, e ela já é conhecida.
    EscopoDoPool.Nenhum -> flow { emit(InicioDaTela.SemConcessao) }

    is EscopoDoPool.Concedido -> fluxoDasSaidas(
        escopo, viagemRepository, rotaRepository, portoRepository,
        localidadeRepository, embarcacaoRepository, relogio,
    )
}

/**
 * As saídas da semana — o Início de quem vende, e o fluxo que existia antes de a plataforma ter o dela.
 */
private fun fluxoDasSaidas(
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
            //
            // Lido **uma vez por emissão**, e não duas: o recorte do domínio e o destaque da apresentação
            // têm de concordar sobre que dia é hoje. Duas leituras separadas por milissegundos discordam
            // uma vez por dia, à meia-noite — e o defeito só aparece nesse instante.
            val agora = relogio.agora()

            inicioDoPainel(
                escopo = escopo,
                viagens = viagens,
                rotasPorId = rotasPorId,
                agora = agora,
            ).paraTela(
                rotasPorId = rotasPorId,
                portosPorId = portosPorId,
                embarcacoes = nomesDeEmbarcacao,
                hoje = agora.toLocalDate(),
            )
        },
    )
}

/**
 * As duas coleções de onde as métricas de acesso saem, **juntas num parâmetro**.
 *
 * Elas andam em par por construção: *convite pendente* é a diferença entre `convites` e `users`, e uma sem
 * a outra não responde nada. Passá-las como um objeto só é o que impede o chamador de ligar uma e esquecer
 * a outra — e é também o que faz o `null` significar uma coisa clara: **quem olha não é da plataforma**.
 */
data class FontesDoAcesso(
    val usuarioRepository: UsuarioRepository,
    val conviteRepository: ConviteRepository,
)

/**
 * **O Início do painel da plataforma** ([ADR-0032] D6, decisão do analista em 2026-09-08).
 *
 * Ele era um recado fixo — *"o painel da plataforma monta o universo"* — e passa a ser o que a plataforma
 * de fato administra: o estado do acesso de quem entra no app.
 *
 * ### Por que o `null` corta a leitura, e não só a exibição
 *
 * [FontesDoAcesso] nulo é *"quem olha não é `ADM`"*, e nesse caso o fluxo emite o painel **sem métrica
 * nenhuma, sem ler nada**. A tentação seria ler sempre e esconder na tela; isso quebraria de um jeito
 * silencioso e feio: `allow list` de `convites` é `ehAdm()`, então o listener de um `GESTOR` receberia
 * *permission denied*, e a falha viraria não-fatal no Crashlytics — um alarme tocando no caso normal, que é
 * exatamente o que o KDoc da `Telemetry` chama de ruído com nome de sinal.
 *
 * Quem decide é a política (`podeGerirAcesso`), no ViewModel, uma vez.
 *
 * ### A espera, e o que ela evita
 *
 * Os dois `obterTodos()` iniciais ligam os listeners e suspendem até o primeiro snapshot, como no fluxo das
 * saídas. Sem eles, a primeira emissão diria *"nenhum acesso, nenhum convite"* — e zero é um número que se
 * lê como fato, não como ausência de resposta.
 */
private fun fluxoDoAcesso(fontes: FontesDoAcesso?, relogio: Relogio): Flow<InicioDaTela> = flow {
    if (fontes == null) {
        emit(InicioDaTela.DaPlataforma())
        return@flow
    }

    fontes.usuarioRepository.obterTodos()
    fontes.conviteRepository.obterTodos()

    emitAll(
        combine(
            fontes.usuarioRepository.observarTodos(),
            fontes.conviteRepository.observarTodos(),
        ) { usuarios, convites ->
            // O relógio é lido a cada emissão, e não uma vez: *expirado* e *a vencer* são relativos a
            // agora, e um snapshot que chega depois da meia-noite tem de ser recortado por hoje.
            InicioDaTela.DaPlataforma(metricasDeAcesso(usuarios, convites, relogio.millis()))
        },
    )
}