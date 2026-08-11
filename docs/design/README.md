# Estudos de design — índice

Os documentos desta pasta são **estudos**: mapeiam o código como ele está, expõem opções e preparam
decisão. Quem decide é o analista; o que ele decide vira **ADR** em [`docs/adr/`](../adr/). Um estudo não
tem autoridade — quando ele e um ADR discordarem, **o ADR vence**.

> O estado de vigência das **decisões** fica no [índice dos ADRs](../adr/README.md) — o que vale, o que caiu
> e por quem. Este índice cuida dos **estudos**. Ambos foram revisados em **2026-08-01**.

Os estudos estão organizados em **cinco eixos**. O eixo diz de que o documento trata; o **estado** diz
quanto ele ainda vale:

| Estado | Significa |
|---|---|
| **base** | referência viva — consultar antes de mexer no assunto |
| **aberto** | mapeado, aguardando decisão do analista |
| **fechado** | virou ADR; fica como registro do caminho até a decisão |
| **superado** | envelheceu; ler só como história, com o substituto indicado |

---

## Eixo 1 — Domínio

O que o negócio é. **É a base de qualquer transformação:** mexer em dados, tela ou regra sem passar por
aqui é mexer no efeito, não na causa.

| Documento | Estado | Do que trata |
|---|---|---|
| [dominio-da-plataforma.md](dominio-da-plataforma.md) | **base** | **O catálogo completo**: os dois contextos, o mapa de coleções, todas as entidades com seus campos, todos os enums e as regras puras. Marca o que é `[hoje]`, `[alvo]` e `[morre]` |
| [e2-painel-e-fim-do-catalogo.md](e2-painel-e-fim-do-catalogo.md) | fechado → ADR-0020 (**F1 feita, F2 parcial**) | A E2 refeita: aplicando a régua do ADR-0016 §3 sem exceção, **nenhuma categoria do catálogo sobrevive** — máscara de documento é regra (LGPD), forma de pagamento é do lançamento, atuação é da empresa. O `Catalogo` não nasce, o domínio fecha em tipos e a primeira seção depois do Painel é **Empresa** |
| [e3-catalogo.md](e3-catalogo.md) | **superado** | O mapa da frente E3: o que do catálogo já virou domínio (quase tudo), o resíduo REST do `IObjetoSimplificado` e o contador de bilhete defasado. **É a prova do doc acima** — que levou a mesma conta até o fim |
| [usuario-e-funcionario.md](usuario-e-funcionario.md) | **aberto** · bloqueia a F7 | O ADM abria **Equipe** — o quadro de pessoal de uma empresa — como se a plataforma tivesse equipe. A plataforma tem **usuários**; a empresa tem **funcionários**. Propõe a seção **Usuários** (ADM-only) e o **convite** como o único lugar onde os dois contextos se encontram — o que também resolve quem cria o primeiro supervisor de uma empresa. Reabre o ADR-0021 D0 por outra razão: corrigir vocabulário que mente na tela |
| [painel-da-empresa.md](painel-da-empresa.md) | fechado → **ADR-0022** | Registra que a **v0.0.4 fechou o painel da plataforma** (F4 e F5) e organiza o resto: o núcleo compartilhado (Início, Rotas, Viagens) × as duas seções exclusivas da empresa (Passagens, Equipe), e a redivisão das fases — **Equipe primeiro**, porque é ela que muda a política de `(papel, cargo)` para `(papel, atuação, cargo)`. As regras deixam de ser fase e viram definição de pronto |
| [painel-administrativo.md](painel-administrativo.md) | **aberto** | A seção **Usuários** (`ADM`-only) e o que ela cobra do que existe: `ADM` e `GESTOR` nunca se separaram na política, `users` é lido por qualquer autenticado, o `Usuario` não sabe ser desligado e **não há caminho in-app para a segunda conta**. Mapeia o caminho de entrada que passou a funcionar em 2026-08-04 e não pode regredir |
| [dominio-passagem.md](dominio-passagem.md) | **base** · §11 fechado → ADR-0018 · **§1/§2/§5 reescritos** → ADR-0023 | O agregado Passagem em detalhe. **Reformulado em 2026-08-11**: a raiz é a **categoria** (passageiro \| veículo, com carga previsto) e **nada é congelado no domínio** — onde o texto antigo diz *snapshot* ou *modo*, o aviso no topo traduz. O §11 fica como registro das rodadas de decisão |
| [f9-passagens-terreno.md](f9-passagens-terreno.md) | **aberto** · 3 perguntas | O terreno da **F9** medido antes do plano: 7.791 linhas de **código escuro** em 75 arquivos, 156 testes congelados que **passam** (provam o modelo antigo), 4 das 6 tabelas do Room e a `PASSAGEM` como única seção fora do andaime. O achado que dimensiona a fase: **hoje a emissão não fecha** — a tabela de tarifa morreu no ADR-0016 §7.2 e o método da inferência nunca foi decidido, então o form resolve `null` e a guarda bloqueia. Propõe oito fatias e o corte entre *revitalizar a emissão* × *trocar o agregado* (pools e abas) |
| [viagem-vs-trecho.md](viagem-vs-trecho.md) | fechado → ADR-0016 §7.1 | O insight sobre a data. **Vocabulário vencido:** o Trecho foi dissolvido; hoje são Rota, Viagem e ocorrência — o aviso no topo da nota traduz |
| [dominio-relacionamentos-e-camadas.md](dominio-relacionamentos-e-camadas.md) | **superado** | Visão geral da era do ADR-0008 (ainda fala em `Agente` e relação por nome). Substituído por `dominio-da-plataforma.md` |

## Eixo 2 — Dados e persistência

Onde o dado vive, como chega e como se mantém.

| Documento | Estado | Do que trata |
|---|---|---|
| [eixo-de-storage-firestore-only.md](eixo-de-storage-firestore-only.md) | fechado → ADR-0017 | Aposentar o Room como datasource: inventário dos 11 DAOs, os dois caches em disco e o resíduo local |
| [camada-de-dados-passagem.md](camada-de-dados-passagem.md) | fechado → **ADR-0025** | O terceiro passo (**domínio → fronteira → camada**). Mede a Passagem contra a anatomia das sete revitalizadas — porta + codec privado + repositório compondo `ColecaoFirestore` — e encontra **cinco desvios**, o primeiro deles explicando um vazio antigo: ela é a **única entidade sem porta**, e a classe concreta está injetada em **dez lugares**, que é a razão pela qual **não existe teste de ViewModel de passagem**. Mais: o **login** conhece a passagem (o listener do contador), as leituras são consultas *ad hoc* com três formatos de retorno, os mappers **fazem I/O** enquanto a F8 provou o estilo puro, e a telemetria perde o desfecho `salvaLocal`, que media *"durável no Room"* |
| [fronteira-de-dados-passagem.md](fronteira-de-dados-passagem.md) | fechado → **ADR-0024** | A fronteira medida contra o agregado do ADR-0023. O contrato (`DocumentoBruto` + `CodecFirestore` + `ColecaoFirestore`) já foi atravessado por **sete entidades** — e a Passagem é a primeira a **não caber em três pontos**: o volume (a coleção inteira num `StateFlow` não serve para dado que cresce sem limite), o **polimorfismo** (primeiro agregado com sub-tipos → codec que despacha por discriminador) e o contador que **mora dentro da coleção que ele conta**. Traz a forma proposta do documento, o que morre na travessia e a boa notícia sobre o custo do D8: as entidades de referência **já estão em memória**, então a junção é lookup, não leitura |
| [dto-por-entidade-ou-caso-de-uso.md](dto-por-entidade-ou-caso-de-uso.md) | **aberto** | O ponto 10 do ADR-0016 virado estudo: o app já tem as duas formas, e `DadosPassagem` (58 campos para 10 usados numa lista) é a que cobra o preço — em leitura, não em memória |
| [sincronizacao-firestore-room.md](sincronizacao-firestore-room.md) | fechado → ADR-0009 · **destino vencido** | O pipeline reativo, o ciclo de vida do listener e a porta `FonteSnapshots` — tudo de pé, menos o destino: não é mais o DAO, é um `StateFlow` (ADR-0017 D1) |
| [balanco-passagens-mapper.md](balanco-passagens-mapper.md) | aberto | O mapper de ocupação: estrutura, threading e o que ele de fato conta. **A ocupação passa a ter teto** — a capacidade vem do navio e barra a emissão (ADR-0018 D8) |

## Eixo 3 — Apresentação

Como o app se mostra e por onde o usuário anda.

| Documento | Estado | Do que trata |
|---|---|---|
| [orquestracao-passagem.md](orquestracao-passagem.md) | **aberto** · 4 perguntas | Helpers e ViewModels da Passagem contra o molde e contra a trinca 0023/0024/0025. Seis desvios medidos, e o principal é de **posse**: o VM declara o estado e entrega o **handle mutável** a três helpers. Mais: o helper acumula **quatro papéis** com destinos diferentes, a orquestração da emissão mora no **navcomposable** (que muta estado por um `internal lateinit` exposto), `Context` no VM com toast como canal de erro, `scrollParaErro: Int` como one-shot feito à mão, e três `UiState` com booleano de veículo espelhando o modelo que o ADR-0023 desfez. **O contraexemplo vale mais que os desvios**: o `EmbarqueViewModel`, 67 linhas, está no molde — o problema é o tamanho do form, não o domínio |
| [apresentacao-passagem.md](apresentacao-passagem.md) | **aberto** · 4 perguntas | Navegação e UI da Passagem. A rota exige argumento que não existe, e daí nasceu `isTextoNaoNulo` para desfazer o texto `"null"` — **a correção já está provada quatro vezes** nos cadastros revitalizados, e a sentinela atravessou até a escrita no Firestore. A tela tem **47 parâmetros** porque a assinatura espelha o modelo achatado. O bilhete digital é desenhado em Compose e **capturado como imagem** — o mecanismo fica, o destino vira a galeria. E a formatação **se inverte**: hoje um único arquivo de `ui/` formata; com o DTO tipado, formatar passa a ser da apresentação |
| [camada-de-apresentacao.md](camada-de-apresentacao.md) | **aberto** | A camada inteira: rotas por String, orquestração dentro da navegação, callback drilling, `@RequiresApi(S)` com `minSdk 26`, UiState com lambda. Ganha um consumidor: a **emissão por etapas** (ADR-0018 F7) é a primeira tela desenhada a partir de um eixo de domínio |
| [fluxo-login.md](fluxo-login.md) | base · **vocabulário vencido** | O fluxo de autenticação implementado. Cita cargos antigos (`DIRETOR`/`COLABORADOR_MASTER`) e o pacote `model/`; e o login ganha um passo novo — **a escolha do vínculo** (ADR-0016, 8ª rodada) |
| [fluxo-main-screen.md](fluxo-main-screen.md) | base · **vocabulário vencido** | A Main Screen: bottom bar reduzida e drawer com as seções. Cita `Agente` e cargos antigos; as seções passam a **derivar da atuação** (ADR-0016 §2) |
| [form-passagem-validacao-exibicao.md](form-passagem-validacao-exibicao.md) | **superado em boa parte** | Descreve o form **antes** do molde. Validação pura, UiState puro e eventos por parâmetro **já foram feitos**; sobrevivem os achados de regra, hoje listados no §9 do estudo do agregado |
| [cadastro-modulos.md](cadastro-modulos.md) | fechado → ADR-0006 | A análise que virou o molde de cadastro |
| [impressao-fisica-bluetooth.md](impressao-fisica-bluetooth.md) | **aberto** · fora do MVP | A **outra superfície de apresentação: o papel**. Mapeia os sete arquivos do `printerservice`, treze falhas (a maior: impressora fora do ar → toast de sucesso **e** passagem marcada como `EMITIDA`), o vocabulário do bilhete que ficou em 2024 e o desenho que resolve a raiz — um documento intermediário com dois renderizadores, um deles a **prévia em Compose** da bobina térmica |

## Eixo 4 — Regra de negócio e relatórios

| Documento | Estado | Do que trata |
|---|---|---|
| [balanco-financeiro.md](balanco-financeiro.md) | aberto · **régua vencida** | Esperada × real × déficit. O modelo de preço mudou: a base **não vem mais de tarifa cadastrada**, e sim de inferência (ADR-0016 §7.2); o eixo é a **ocorrência** e canceladas ficam de fora (ADR-0018 D9/D18) |

## Eixo 5 — Produto e entrega

| Documento | Estado | Do que trata |
|---|---|---|
| [mvp-roadmap.md](mvp-roadmap.md) | **base** | Os pilares do MVP, o que está fechado e o que falta |

---

## Como um estudo vira ADR

1. **Mapear** o estado atual no código, com arquivo e linha — sem proposta ainda.
2. **Expor as opções**, inclusive as que serão rejeitadas, com o custo de cada uma.
3. **Perguntar ao analista** no fim do documento, uma pergunta por decisão.
4. **Registrar as respostas** no próprio estudo, à medida que chegam.
5. **Escrever o ADR** com as decisões — e marcar o estudo como *fechado*, apontando para ele.