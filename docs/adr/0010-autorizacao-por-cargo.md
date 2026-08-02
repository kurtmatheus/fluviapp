# ADR-0010: Autorização por cargo — política única, dois eixos, posse por identidade

**Status:** Aceita e implementada. Fase 1 (política + correção de comportamento) e Fase 2 (posse por
identidade) implementadas; a Fase 3 virou o [ADR-0011](0011-regras-firestore-por-cargo.md) (regras no
servidor), também implementado.

> **Vigente em parte — o vocabulário e o número de eixos mudaram.** A tese central (política **única**,
> segurança por identidade, fail-closed) é a viga do sistema e continua inteira. Mudou o que ela recebe:
> - **os cargos foram renomeados** — `DIRETOR`/`COLABORADOR_MASTER`/`OPERADOR` deram lugar a **papel**
>   (`ADM`/`GESTOR`/`OPERADOR`, do sistema) × **cargo** (`SUPERVISOR`/`AGENTE`, do negócio)
>   ([ADR-0015](0015-rework-agente-equipe.md) §8);
> - **a atuação entrou como terceira coordenada**: `(papel, cargo)` → `(papel, atuação, cargo)`, e cada
>   atuação tem a sua lista de cargos ([ADR-0016](0016-dominio-da-plataforma.md), 8ª rodada). A política
>   segue **única** — cresce em coordenadas, não em cópias.

> **Estendido pelo [ADR-0015](0015-rework-agente-equipe.md):**
> - **Vocabulário — já implementado (P2.1).** Os cargos deste ADR foram renomeados: `DIRETOR` → **`GESTOR`**,
>   `COLABORADOR_MASTER` → **`SUPERVISOR`**, `OPERADOR` → **`AGENTE`**; e o predicado `ehGestor` virou
>   **`ehCargoPlataforma`** (`GESTOR` virou cargo concreto, então o predicado passou a se chamar pelo
>   *escopo*). Sem alias: valor antigo → fail-closed. **O texto abaixo preserva os nomes da época** — leia-o
>   como registro histórico da decisão, não como o vocabulário atual.
> - **Terceiro eixo — escopo (ainda sem código).** `ADM`/`GESTOR` são cargos de **plataforma** (atravessam
>   agências), `SUPERVISOR`/`AGENTE` são de **agência**; "todas as passagens" passa a significar "todas da
>   **minha agência**" para o `SUPERVISOR` (ADR-0015 §4.1, fase P2.6).

> Formaliza o [estudo de autorização](../design/autorizacao-por-cargo.md). Conversa com o
> [ADR-0005](0005-autenticacao-sessao-firebase-datastore.md) (sessão Firebase + DataStore guarda
> nome/cargo), o [ADR-0008](0008-relacionamentos-por-identidade.md) (relacionar por id, não por
> nome) e o [ADR-0003](0003-modelo-de-memoria-do-dado.md) (normalização em trânsito; stack dormente
> aditivo). Escopo declarado: **segurança por UI** (esconder menus/botões) — ver Consequências.

**Contexto**

Quatro cargos com acessos distintos, hoje só parcialmente modelados:

| Capacidade | ADM | Diretor | Colab. Master | Operador |
|---|:--:|:--:|:--:|:--:|
| Seções Viagem/Navio/Empresa/Agente | ✅ | ✅ | ❌ | ❌ |
| Passagem — criar | ✅ | ✅ | ✅ | ✅ |
| Passagem — editar/deletar **qualquer** | ✅ | ✅ | ✅ | ❌ |
| Passagem — editar/deletar **própria** | ✅ | ✅ | ✅ | ✅ |
| Pesquisa — ver **todas** | ✅ | ✅ | ✅ | ❌ (só próprias) |

Dois eixos de autorização vivem aqui e estavam **colapsados**:
- **(A) acesso a seção** (menu) — já resolvido por `PermissoesUsuario.secoesVisiveis` (drawer).
- **(B) ação sobre a Passagem, com posse** (criar / editar-própria / editar-qualquer) — **ausente**
  da política e disperso em dois pontos, **ambos quebrados**:

1. **Gate dos botões Editar/Deletar** (`DetalhesPassagemViewModel.atualizarIsAdminOuFuncResponsavel`):
   ```kotlin
   if (cargo != Usuario.Cargo.ADM.name ||     // qualquer NÃO-ADM já satisfaz o OR
       cargo == Usuario.Cargo.DIRETOR.name ||
       usuario == usuarioLogado!!.nome)        // compara o logado com ele mesmo — sempre true
   ```
   Sem `else`. Resultado: **todo usuário vê Editar/Deletar em toda passagem** — o oposto do
   requisito do Operador. E a checagem de "dono" nunca olhava o `funcionario` da passagem.

2. **Escopo da pesquisa** (`PesquisarPassagemViewModel` via `Usuario.temPermissaoEspecialPassagem()`):
   comparava `COLABORADOR_MASTER.obterDescricaoFormatada()` (`"COLABORADOR MASTER"`, com espaço)
   contra o valor gravado (`"COLABORADOR_MASTER"`, underscore). **Nunca casava** → o Colaborador
   Master jamais recebia o escopo ampliado.

Três problemas estruturais de fundo:
- **`cargo` é `String` solta e o enum estava incompleto**: `enum Cargo { ADM, DIRETOR,
  COLABORADOR_MASTER }` **não tinha `OPERADOR`**, mas o cadastro grava `cargo = "OPERADOR"`
  (`FirebaseAutenticacaoRepository`, `CadastroViewModel`). O usuário mais comum tinha um cargo que o
  tipo não conhecia — daí as comparações `.name` na mão que erravam.
- **Três idiomas de checagem** convivendo (`temPermissaoEspecialPassagem`, `ehGestor`, o `if` inline
  do detalhe) — contra a fonte única que `PermissoesUsuario` inaugurou.
- **Posse por NOME, não por identidade**: "própria" = `funcionarioResponsavel == usuario.nome`. É o
  anti-padrão do ADR-0008 (rótulo mutável como chave) reencarnado na autorização — homônimos colidem,
  renomear quebra a posse.

**Opções consideradas**

1. **Status quo** — consertar os `if` no lugar. Rápido, mas mantém a regra duplicada e a posse por
   nome; a próxima capacidade volta a divergir.
2. **Política única, cargo tipado, posse por identidade** — todo "pode X?" responde em
   `PermissoesUsuario`; a UI só pergunta. Corrige os dois bugs por construção (eles existem por
   estarem fora da política) e prepara as regras de servidor (Fase 3) lendo o mesmo modelo.

**Decisão**

Opção 2, faseada.

- **Cargo como tipo, String na fronteira.** Fechar o enum (`+ OPERADOR`) e converter a String uma
  vez com `Cargo.de(valor): Cargo?` — normalização em trânsito (ADR-0003). Regras casam por enum,
  nunca por `.name` solto. Cargo desconhecido → `null` → sem permissão (fail-closed).
- **Política única com dois eixos.** `PermissoesUsuario` mantém o eixo de seção (`podeAcessar`/
  `secoesVisiveis`) e ganha o eixo de ação sobre a Passagem: `podeCriarPassagem`,
  `podeEditarQualquerPassagem`, `podeEditarPassagem(cargo, ehDono)`, `podeDeletarPassagem`,
  `podeVerTodasPassagens`. A UI computa `ehDono` e pergunta; a política decide.
- **Mapa de cargos**: `ehGestor` = ADM ∨ DIRETOR; `editar/deletar qualquer` = gestor ∨
  COLABORADOR_MASTER; `criar` = qualquer cargo conhecido; `ver todas` acompanha `editar qualquer`.
- **Deletar segue editar** (decisão): quem edita aquela passagem pode deletá-la.
- **Posse por identidade** (ADR-0008, Fase 2): a Passagem ganha um id de dono estável (`uid`); o
  `funcionarioResponsavel` (nome) permanece como **snapshot** de exibição/impressão. "Própria"
  passa a comparar `uid`, não nome.

**Plano de migração (faseado, aditivo — "stack dormente" do ADR-0003)**

*Fase 1 — Política + correção (sem migração). **Feita.*** Enum fechado + `Cargo.de`; `PermissoesUsuario`
ganha o eixo de ação; os três pontos de UI passam a consumi-la; removido o `temPermissaoEspecialPassagem`
bugado. O gate do detalhe passa a computar `ehDono` contra o `funcionario` **da passagem**
(`DetalhesPassagemState.dadosPassagem.funcionario`), ainda por nome, temporariamente. Corrige o
comportamento já aqui.

*Fase 2 — Posse por `uid` (aditivo). **Feita.*** `Passagem`/`PassagemDocumento` ganham `funcionarioId`
(campo novo = schemaless no Firestore, `ALTER TABLE` v11→v12 no Room, ambos default `""`). A criação
carimba o `uid`; **a edição preserva** dono/responsável originais (autoria congelada na emissão,
ADR-0008 — um gestor editar não vira dono; corrige de passagem o overwrite que existia no
`montarPassagem`). `ehDono = passagem.funcionarioId == usuarioLogado.id`.

> **Correções ao plano original:** (a) **não** foi preciso tocar sessão/DataStore — o `uid` já chega
> via `Usuario.id` (doc id de `users/{uid}`) por `obterUltimoUsuarioLogado()`; (b) **não** há seed de
> passagens (o `SeedFirestore` só grava catálogos + o doc `contador`), então não há o que semear —
> passagens nascem em runtime já carimbadas. Sem backfill (portfólio): bilhetes anteriores ficam com
> `funcionarioId` vazio e são tratados como "sem dono" (só gestor/Colab editam). A **pesquisa**
> permanece filtrando por nome (é filtro de leitura/UX, não a fronteira de autorização — que é o gate
> de editar/deletar, agora por id).

*Fase 3 — Regras Firestore por cargo (ADR futuro).* Fora do escopo "UI": endurecer as regras
(`request.auth != null` hoje) lendo o **mesmo** modelo de cargo. A política é a costura que torna
isso um passo pequeno, não uma reescrita.

**Consequências**

- **Bugs corrigidos**: o Operador deixa de ver Editar/Deletar em passagens alheias; o Colaborador
  Master passa a enxergar o escopo ampliado (era comparação de string que nunca casava). Isto **não
  é só refactor — conserta comportamento** que contradizia o requisito.
- **Uma fonte de verdade**: acabam os três idiomas; toda decisão de acesso mora em
  `PermissoesUsuario`. Nova capacidade = nova função na política, consumida pela UI.
- **Segurança por UI, dita com todas as letras**: gate de UI é UX, não fronteira. Um cliente ainda
  consegue chamar o repositório/Firestore direto (regras hoje só exigem `auth != null`). Para um app
  de **portfólio** é escopo legítimo e consciente; a fronteira real vem na Fase 3, lendo o mesmo
  modelo — por isso a política já é desenhada como semente das regras de servidor.
- **API da política em String**: `PermissoesUsuario` recebe o `cargo: String?` cru (do DataStore/
  Room) e converte internamente com `Cargo.de` — zero ripple nos chamadores (ex.: `secoesVisiveis`),
  tipagem garantida dentro da política.
- **Posse por nome é dívida assumida na Fase 1**: só resolve de verdade na Fase 2 (identidade). Até
  lá, homônimo/rename podem confundir a posse — aceitável no portfólio, registrado como dívida.

**Alternativas futuras**

- **Posse por identidade** (Fase 2) — `funcionarioId` na Passagem + `uid` na sessão.
- **Regras Firestore por cargo** (Fase 3) — ADR próprio, com a política já como fonte.
- **Auditoria de ação** (quem editou/deletou) — casa com a observabilidade de exclusão do
  [ADR-0007](0007-observabilidade-cadastros.md), se e quando entrar.