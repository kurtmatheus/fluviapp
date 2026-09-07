# Estudos de design — índice

Os documentos desta pasta são **estudos**: mapeiam o código como ele está, expõem opções e preparam
decisão. Quem decide é o analista; o que ele decide vira **ADR** em [`docs/adr/`](../adr/). Um estudo não
tem autoridade — quando ele e um ADR discordarem, **o ADR vence**.

> O estado de vigência das **decisões** fica no [índice dos ADRs](../adr/README.md) — o que vale, o que caiu
> e por quem. Este índice cuida dos **estudos**. A porta de entrada dos dois é [`docs/README.md`](../README.md).
>
> **Revisado em 2026-08-01 e em 2026-09-03.** A segunda revisão foi de manutenção, e o que ela encontrou
> vale como aviso: **sete linhas estavam mentindo** — estudos marcados *aberto* que os ADRs 0021, 0023,
> 0024, 0026 e 0030 já haviam fechado, e um que dizia *bloqueia a F7* depois de a F7 e a F8 estarem feitas.
> Nenhuma delas estava errada quando foi escrita; todas envelheceram sozinhas. É exatamente o que o índice
> dos ADRs adverte — *"um índice que envelhece é pior que nenhum: ele mente com ar de autoridade"* — e a
> forma de não repetir é fechar o estudo **na leva do ADR que o fechou**, não depois.
>
> **Seis estudos saíram desta pasta na mesma revisão** e foram absorvidos por
> [`docs/historico/`](../historico/), em dois documentos que sintetizam o que eles acharam: o
> [domínio antigo](../historico/o-dominio-antigo.md) (catálogo, relação por nome e a data) e as
> [telas antes da revitalização](../historico/as-telas-antes-da-revitalizacao.md) (login, painel e o form de
> passagem). **Nada se perdeu** — o texto integral está no git —, e o que se ganhou foi parar de oferecer,
> num índice de referências vivas, documentos que descreviam um app que não existe mais.

Os estudos estão organizados em **cinco eixos**. O eixo diz de que o documento trata; o **estado** diz
quanto ele ainda vale:

| Estado | Significa |
|---|---|
| **base** | referência viva — consultar antes de mexer no assunto |
| **aberto** | mapeado, aguardando decisão do analista |
| **fechado** | virou ADR; fica como registro do caminho até a decisão |
| **respondido** | não virou ADR próprio: a pergunta dele foi respondida dentro de outro |
| **executado** | virou código sem passar por ADR — a decisão está no commit e no estudo |
| **superado** | envelheceu; ler só como história, com o substituto indicado |

---

## Eixo 1 — Domínio

O que o negócio é. **É a base de qualquer transformação:** mexer em dados, tela ou regra sem passar por
aqui é mexer no efeito, não na causa.

| Documento | Estado | Do que trata |
|---|---|---|
| [dominio-da-plataforma.md](dominio-da-plataforma.md) | **base** | **O catálogo completo**: os dois contextos, o mapa de coleções, todas as entidades com seus campos, todos os enums e as regras puras. Marca o que é `[hoje]`, `[alvo]` e `[morre]` |
| [e2-painel-e-fim-do-catalogo.md](e2-painel-e-fim-do-catalogo.md) | fechado → ADR-0020 (**F1 feita, F2 parcial**) | A E2 refeita: aplicando a régua do ADR-0016 §3 sem exceção, **nenhuma categoria do catálogo sobrevive** — máscara de documento é regra (LGPD), forma de pagamento é do lançamento, atuação é da empresa. O `Catalogo` não nasce, o domínio fecha em tipos e a primeira seção depois do Painel é **Empresa** |
| [usuario-e-funcionario.md](usuario-e-funcionario.md) | **executado** na F6.6 · sem ADR próprio | O ADM abria **Equipe** — o quadro de pessoal de uma empresa — como se a plataforma tivesse equipe. A plataforma tem **usuários**; a empresa tem **funcionários**. Propõe a seção **Usuários** (ADM-only) e o **convite** como o único lugar onde os dois contextos se encontram — o que também resolve quem cria o primeiro supervisor de uma empresa. Reabre o ADR-0021 D0 por outra razão: corrigir vocabulário que mente na tela |
| [acesso-e-identidade.md](acesso-e-identidade.md) | **aberto** · aguarda respostas → ADR | *Quem entra, quem é, e o que pode.* O eixo de acesso medido inteiro — autenticação, sessão, autorização, observabilidade e provisionamento —, complementando o estudo acima, que cuidou de **quem gere quem**. Registra duas peças que acertaram (a porta `SessaoUsuario`, com 12 consumidores, e a decisão da splash como função pura) e quatro pontos de acoplamento, dos quais **um tem razão**: o menu lê o DataStore cru porque `collect`a e reage, enquanto a porta só sabe responder uma vez — trocar hoje perderia reatividade. O achado central é outro: **12 das 22 funções da política não têm chamador de produção**, e entre elas está a cadeia inteira da Passagem — emitir e confirmar embarque **agem sem perguntar**, de modo que a única fronteira de autorização ativa é o servidor. Mede também um **gate que passa pulando o que mais importa** (`npm test` roda 103 casos de regra e pula **57**, justo os de passagem, cliente e veículo) e uma **telemetria que não diz quem** (3 pontos de emissão, nenhum com o operador; 23 `Log.e` fora dela). E derruba a premissa do pedido que o originou: **criar `ADM`/`GESTOR` pelo app já funciona** desde a F6.6, com caso de emulador provando — enquanto o ADR-0021 D0, este índice e um comentário dentro do `firestore.rules` seguem dizendo que é impossível |
| [empresa-com-duas-atuacoes.md](empresa-com-duas-atuacoes.md) | **fechado** · aguarda ADR · **só domínio** | *A atividade como concessão, o cargo como registro.* A matriz **agencia, arrenda o cais e navega** — uma parte, várias atuações —, e o caso que dá a régua é a **dona de porto**, que precisa gerir cargos e **delegar demandas intraportuárias**. Decisão do analista: **a atividade concede, o cargo registra** — e com isso o `Cargo` enum **morre** e vira **entidade da empresa** (ADR-0020: dado é entidade), enquanto `Atividade` herda a proteção que o cargo tinha (*"concessão cadastrável é escalonamento por cadastro"* — o argumento do ADR-0016 §6.1 não é contrariado, é **transferido**, e por isso a conclusão dele cai). O modelo fica em **três camadas** — atuação (universo) ∩ cargo (delegação) → atividades em vigor —, a política ganha **uma** entrada (`atividade in atividadesDe(contexto)`) e o anti-escalonamento muda de lugar: **ninguém delega o que a empresa não exerce, nem o que ele mesmo não tem**. O `Vinculo` fica com a forma de hoje (um por empresa, cargo por id) e o par `(empresa, atuação)` do §6.1 fica sem necessidade. **O eixo final é a gestão da informação**: a régua *registra-se o ato — quem e quando —, o resto se infere dos ids que ele ancora* explica por que nada de cargo vai no bilhete, e mede os **três lugares onde a inferência não alcança** — autoridade no passado (cargo mutável), o poder emergencial sem autor (`alteradoEm` **sem** `alteradoPorId`) e os dois espaços de id (emissão por `funcionarioId`, embarque por `uid`). **Os três fecharam** (D7–D9): **delegação imutável** (redelegar = criar + inativar + reapontar, regime da Rota/Viagem), **o poder emergencial registra autor** e **a autoria de negócio unifica no `funcionarioId`** — com o que o `uid` sai dos registros de negócio e ato de negócio passa a exigir registro na operação. Recortado a pedido: tela, formulário e regra de servidor ficam fora |
| [painel-da-empresa.md](painel-da-empresa.md) | fechado → **ADR-0022** | Registra que a **v0.0.4 fechou o painel da plataforma** (F4 e F5) e organiza o resto: o núcleo compartilhado (Início, Rotas, Viagens) × as duas seções exclusivas da empresa (Passagens, Equipe), e a redivisão das fases — **Equipe primeiro**, porque é ela que muda a política de `(papel, cargo)` para `(papel, atuação, cargo)`. As regras deixam de ser fase e viram definição de pronto |
| [painel-administrativo.md](painel-administrativo.md) | fechado → **ADR-0021** · **fora do MVP** | A seção **Usuários** (`ADM`-only) e o que ela cobra do que existe: `ADM` e `GESTOR` nunca se separaram na política, `users` é lido por qualquer autenticado, o `Usuario` não sabe ser desligado e **não há caminho in-app para a segunda conta**. Mapeia o caminho de entrada que passou a funcionar em 2026-08-04 e não pode regredir |
| [dominio-passagem.md](dominio-passagem.md) | **base** · §11 fechado → ADR-0018 · **§1/§2/§5 reescritos** → ADR-0023 | O agregado Passagem em detalhe. **Reformulado em 2026-08-11**: a raiz é a **categoria** (passageiro \| veículo, com carga previsto) e **nada é congelado no domínio** — onde o texto antigo diz *snapshot* ou *modo*, o aviso no topo traduz. O §11 fica como registro das rodadas de decisão |
| [classe-de-veiculo.md](classe-de-veiculo.md) | fechado → **ADR-0031** (§6.4 superado) | *A classe é só registro — e o enum é o registro.* Nasce da issue #4 (*"o Supervisor cadastrar novas classes"*) e encontra uma decisão, não um vazio: **2026-08-01** já fechara que *"a classe do veículo não pode ser texto de catálogo editável"*, porque o produto do modo veículo é a série **contagem × classe × preço**. A primeira versão foi **rejeitada no enquadramento** (punha a questão como *tipo ou entidade*, e toda saída custava coleção, CRUD, regra e menu); a pergunta certa é **onde mora o comportamento**, e assim a decisão antiga sai **confirmada por outro caminho**. Duas medições sustentam a direção: **`ehPesado` não tem leitor** e **`tarifaMotoBase` não tem chamador** — a classe já não carrega preço (I/O desde 2026-08-11) nem o recorte da balsa. A via do §6: **o enum continua sendo o registro** (é a chave da série, e série não convive com identidade que se renomeia) e o comportamento **sobe para a família** — o mecanismo do `Cargo` **sem** a segunda metade dele, no formato que a `Acomodacao` já usa. E a resposta de eficiência, com número: o custo do enum não cresce com o nº de valores, mas com **valores × comportamentos** — as seis classes de hoje já têm só **três** assinaturas, e com dezesseis seria quatro quintos de repetição. **A lista veio no mesmo dia e corrige o §6**: são **dezessete**, e o eixo não é um só — o jet-ski exige cilindrada *e* é rebocado, o que separa **porte** de **propulsão**. Ela traz ainda a colisão `Lancha` × `TipoEmbarcacao.LANCHA`, três nomes com o radical *carret-* na mesma lista, e a pergunta que ninguém tinha feito: ônibus e motorhome ocupam vaga nos **dois** eixos de capacidade. **As três foram respondidas no mesmo dia** e viraram o ADR-0031: a natureza é a propriedade (e existe pela **capacidade analítica**, não pela arrumação), o casco admite **por exclusão** (Balsa todas · Navio carro e moto · Lancha nenhuma), o **porte cai** por perder consumidor, o ônibus é irrelevante porque *todo embarque gera passagem*, e a emissão ganha **passo + subpasso**. **Duas leituras do estudo caíram em 2026-09-05**, e as duas eram sobre o mesmo valor: o jet-ski **não** exige cilindrada e **não** é motociclo — é rebocado, como a lancha, e só a moto exige o campo. Com isso some também o argumento que separava *porte* de *propulsão* aqui: era o jet-ski que o sustentava |
| [f9-passagens-terreno.md](f9-passagens-terreno.md) | fechado → **ADR-0023** · F9 executada | O terreno da **F9** medido antes do plano: 7.791 linhas de **código escuro** em 75 arquivos, 156 testes congelados que **passam** (provam o modelo antigo), 4 das 6 tabelas do Room e a `PASSAGEM` como única seção fora do andaime. O achado que dimensiona a fase: **hoje a emissão não fecha** — a tabela de tarifa morreu no ADR-0016 §7.2 e o método da inferência nunca foi decidido, então o form resolve `null` e a guarda bloqueia. Propõe oito fatias e o corte entre *revitalizar a emissão* × *trocar o agregado* (pools e abas) |

## Eixo 2 — Dados e persistência

Onde o dado vive, como chega e como se mantém.

| Documento | Estado | Do que trata |
|---|---|---|
| [eixo-de-storage-firestore-only.md](eixo-de-storage-firestore-only.md) | fechado → ADR-0017 | Aposentar o Room como datasource: inventário dos 11 DAOs, os dois caches em disco e o resíduo local |
| [camada-de-dados-passagem.md](camada-de-dados-passagem.md) | fechado → **ADR-0025** | O terceiro passo (**domínio → fronteira → camada**). Mede a Passagem contra a anatomia das sete revitalizadas — porta + codec privado + repositório compondo `ColecaoFirestore` — e encontra **cinco desvios**, o primeiro deles explicando um vazio antigo: ela é a **única entidade sem porta**, e a classe concreta está injetada em **dez lugares**, que é a razão pela qual **não existe teste de ViewModel de passagem**. Mais: o **login** conhece a passagem (o listener do contador), as leituras são consultas *ad hoc* com três formatos de retorno, os mappers **fazem I/O** enquanto a F8 provou o estilo puro, e a telemetria perde o desfecho `salvaLocal`, que media *"durável no Room"* |
| [fronteira-de-dados-passagem.md](fronteira-de-dados-passagem.md) | fechado → **ADR-0024** | A fronteira medida contra o agregado do ADR-0023. O contrato (`DocumentoBruto` + `CodecFirestore` + `ColecaoFirestore`) já foi atravessado por **sete entidades** — e a Passagem é a primeira a **não caber em três pontos**: o volume (a coleção inteira num `StateFlow` não serve para dado que cresce sem limite), o **polimorfismo** (primeiro agregado com sub-tipos → codec que despacha por discriminador) e o contador que **mora dentro da coleção que ele conta**. Traz a forma proposta do documento, o que morre na travessia e a boa notícia sobre o custo do D8: as entidades de referência **já estão em memória**, então a junção é lookup, não leitura |
| [dto-por-entidade-ou-caso-de-uso.md](dto-por-entidade-ou-caso-de-uso.md) | **respondido** → ADR-0024 D8 | O ponto 10 do ADR-0016 virado estudo: o app já tem as duas formas, e `DadosPassagem` (58 campos para 10 usados numa lista) é a que cobra o preço — em leitura, não em memória |
| [sincronizacao-firestore-room.md](sincronizacao-firestore-room.md) | fechado → ADR-0009 · **destino vencido** | O pipeline reativo, o ciclo de vida do listener e a porta `FonteSnapshots` — tudo de pé, menos o destino: não é mais o DAO, é um `StateFlow` (ADR-0017 D1) |
| [balanco-passagens-mapper.md](balanco-passagens-mapper.md) | aberto | O mapper de ocupação: estrutura, threading e o que ele de fato conta. **A ocupação passa a ter teto** — a capacidade vem do navio e barra a emissão (ADR-0018 D8) |

## Eixo 3 — Apresentação

Como o app se mostra e por onde o usuário anda.

| Documento | Estado | Do que trata |
|---|---|---|
| [orquestracao-passagem.md](orquestracao-passagem.md) | fechado → **ADR-0026** · executado na F9.4/F9.5 | Helpers e ViewModels da Passagem contra o molde e contra a trinca 0023/0024/0025. Seis desvios medidos, e o principal é de **posse**: o VM declara o estado e entrega o **handle mutável** a três helpers. Mais: o helper acumula **quatro papéis** com destinos diferentes, a orquestração da emissão mora no **navcomposable** (que muta estado por um `internal lateinit` exposto), `Context` no VM com toast como canal de erro, `scrollParaErro: Int` como one-shot feito à mão, e três `UiState` com booleano de veículo espelhando o modelo que o ADR-0023 desfez. **O contraexemplo vale mais que os desvios**: o `EmbarqueViewModel`, 67 linhas, está no molde — o problema é o tamanho do form, não o domínio |
| [apresentacao-passagem.md](apresentacao-passagem.md) | fechado → **ADR-0025 e 0026** · executado | Navegação e UI da Passagem. A rota exige argumento que não existe, e daí nasceu `isTextoNaoNulo` para desfazer o texto `"null"` — **a correção já está provada quatro vezes** nos cadastros revitalizados, e a sentinela atravessou até a escrita no Firestore. A tela tem **47 parâmetros** porque a assinatura espelha o modelo achatado. O bilhete digital é desenhado em Compose e **capturado como imagem** — o mecanismo fica, o destino vira a galeria. E a formatação **se inverte**: hoje um único arquivo de `ui/` formata; com o DTO tipado, formatar passa a ser da apresentação |
| [camada-de-apresentacao.md](camada-de-apresentacao.md) | **aberto** | A camada inteira: rotas por String, orquestração dentro da navegação, callback drilling, `@RequiresApi(S)` com `minSdk 26`, UiState com lambda. Ganha um consumidor: a **emissão por etapas** (ADR-0018 F7) é a primeira tela desenhada a partir de um eixo de domínio |
| [bilhete-digital.md](bilhete-digital.md) | fechado → **ADR-0030** · implementado | O documento que vai para a mão do passageiro. Mede as **quatro peças demolidas na F9.2** e separa o que volta do que não volta: a **captura em Compose** volta inteira (e com a espera de layout que já custou um defeito), o **índice local não volta** — ele só existia porque o arquivo tinha nome de *timestamp*, e o ADR-0017 D5 já resolveu isso com nome derivado do `idPassagem` + galeria. Registra que **`ColetorDeReferencias.completas` foi escrito na F9.4 e ainda não tem consumidor**: o bilhete é ele. E o pedido novo — **pré-visualizar no ato de emitir** — tem consequência técnica: se o desfecho já desenha o bilhete, captura-se **o que está na tela**, em vez de renderizar duas vezes como antes. Achado lateral: o `WRITE_EXTERNAL_STORAGE` do manifesto não vale desde a API 29 e é resíduo |
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