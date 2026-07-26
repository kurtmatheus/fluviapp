# ADR-0011: Regras do Firestore por cargo — a fronteira de autorização no servidor

**Status:** Aceita (direção). Regras escritas (`firestore.rules` + `firebase.json`) e cobertas por
suíte de emulador verde (`firestore-tests/`, 24 casos); deploy em produção é passo de operação (ver
Deploy). Fecha a **Fase 3** prevista no [ADR-0010](0010-autorizacao-por-cargo.md).

> **A ajustar quando o [ADR-0015](0015-rework-agente-equipe.md) for codado:** o rename dos cargos
> (`COLABORADOR_MASTER` → `SUPERVISOR`, `OPERADOR` → `AGENTE`) toca a lista literal de `cargoConhecido()`, o
> `podeEditarQualquerPassagem()` e o cargo default do autocadastro — `rules` + app + suíte de emulador têm de
> ir no **mesmo commit** (fail-closed nega o app inteiro se saírem dessincronizados). O isolamento por
> agência **não** entra nas regras no MVP (fica por UI) — é o débito de servidor registrado no ADR-0015 §3.

> Conversa com o [ADR-0010](0010-autorizacao-por-cargo.md) (política de cargo, dois eixos), o
> [ADR-0005](0005-autenticacao-sessao-firebase-datastore.md) (`users/{uid}` com `cargo`) e o
> [ADR-0008](0008-relacionamentos-por-identidade.md) / Fase 2 (o `funcionarioId` na Passagem, que
> torna a posse verificável no servidor).

**Contexto**

O ADR-0010 disse com todas as letras: **gate de UI é UX, não fronteira**. As Fases 1-2 esconderam
menus/botões e acertaram a posse por identidade no cliente — mas as regras do Firestore continuavam em
`request.auth != null` (qualquer usuário autenticado lê/escreve tudo). Consequência: um cliente
adulterado ou um script com o token do usuário **contorna toda a autorização** — edita passagem
alheia, cadastra navio sendo operador, e — o pior — **promove o próprio cargo** gravando
`users/{uid}.cargo = "ADM"`. Sem regras, o modelo de papéis é cosmético.

Dois habilitadores tornam a fronteira barata agora:
- **`funcionarioId` na Passagem** (Fase 2) → posse verificável no servidor sem heurística de nome.
- **`users/{uid}.cargo`** (ADR-0005) → a regra lê o cargo do próprio autor via `get()`.

**Decisão**

Escrever `firestore.rules` que **espelham `PermissoesUsuario`** — a mesma matriz, agora imposta no
servidor. O cargo é lido do documento do autor; a posse, do `funcionarioId`.

- **Cargo lido no servidor**: `get(/databases/$(db)/documents/users/$(uid)).data.cargo`. Funções
  `ehGestor()` (ADM/DIRETOR), `podeEditarQualquerPassagem()` (gestor ∨ COLABORADOR_MASTER),
  `cargoConhecido()` (os quatro), espelhando a política Kotlin.
- **Cargo imutável pelo cliente (anti-escalonamento)** — o ponto mais crítico:
  - *create* em `users/{uid}`: só o próprio dono, e **só com o cargo default `OPERADOR`** (o
    provisionamento de auto-cadastro/Google; ninguém nasce ADM pelo cliente).
  - *update* em `users/{uid}`: só o próprio dono e **com `cargo` inalterado** (edita nome, nunca o
    papel). Promoção/rebaixamento é operação **de console/backend** (Admin SDK ignora regras).
- **Catálogos** (`constants`, `empresas`, `navios`, `agents`, `viagens`): **leitura para todo
  autenticado** (o Operador precisa ler viagens/navios/agentes para vender passagem — dropdowns e a
  Home listam viagens), **escrita só para gestor**. É a tradução fiel de "Operador/Colab não têm
  acesso (CRUD) a viagens/agente", sem quebrar a venda.
- **Passagens** (`passagens/{id}`):
  - *create*: `cargoConhecido()` **e** `funcionarioId == uid` — não dá para **forjar dono** na
    emissão.
  - *update/delete*: `podeEditarQualquerPassagem() || ehDono` (posse por `funcionarioId`), **e** no
    update o `funcionarioId` é **imutável** (não se reatribui dono editando).
  - *read*: todo autenticado (ver Consequências — read-scoping fica como refino futuro).
  - **`passagens/contador`** (doc especial do número de bilhete): escrita para quem pode criar
    passagem (`cargoConhecido()`), pois a emissão incrementa o contador — mas **endurecida**, por ser
    infra compartilhada: o *update* é **monotônico** (`numeroBilhete` só cresce → sem bilhete duplicado
    por rewind) e **não há *delete*** (não concedido → default-deny). *Create* segue liberado para
    bootstrap/seed. A corrida de dois clientes lendo o mesmo N **não** é resolvível por regra (exige
    transação/`FieldValue.increment` no app) — fica como observação, não a fronteira deste ADR.
- **Default-deny**: coleções não listadas ficam negadas por omissão (sem catch-all `allow false`
  redundante).

**Consequências**

- **A autorização passa a ser real**: operador não edita passagem alheia nem cadastra master data;
  ninguém se auto-promove. O que a UI esconde, o servidor agora **recusa**.
- **Dever de paridade (tensão de DRY)**: a matriz existe em **dois lugares** — `PermissoesUsuario.kt`
  e `firestore.rules` — em linguagens que não compartilham código. É duplicação inevitável (a regra
  roda no servidor do Google). Mitigação: a matriz canônica do ADR-0010 é a fonte; qualquer mudança de
  papel altera **os dois** de propósito, e o teste de emulador (abaixo) trava a paridade.
- **Cargo é gerido por console/backend**: não há fluxo in-app de promoção (nem havia). Coerente com o
  portfólio; um painel de admin seria trabalho à parte.
- **Leituras abertas por ora** (users, passagens, catálogos para autenticado): endurecer a leitura de
  passagem para "só as próprias do Operador" exigiria regras casadas com *query constraints* e
  reescrever o listener `UsuarioRepository.carregarUsuarios` (que hoje espelha a coleção `users`
  inteira). Fica como refino; a fronteira que importa (escrita) está fechada.
- **Custo de `get()`**: cada avaliação de regra que checa cargo faz 1 leitura de `users/{uid}` —
  barato e cacheado por request; aceitável.
- **Ordenação do 1º provisionamento**: o *create* de `users/{uid}` usa `request.resource.data`, não
  `get()`, então não depende do doc já existir; as demais regras (catálogos/passagens) só são
  exercidas após o perfil existir.

**Alternativas consideradas**

- **Custom claims no token** (cargo como claim do Firebase Auth) em vez de `get(users/{uid})`: elimina
  a leitura por request e é o padrão "robusto"; mas exige Cloud Function para setar o claim a cada
  mudança de cargo e re-emissão de token. Rejeitado por ora (sem backend/Functions no portfólio);
  registrado como evolução se o custo do `get()` ou a latência incomodarem.
- **Manter só a UI** (status quo pré-0011): rejeitado — é exatamente o teatro que este ADR fecha.

**Deploy e teste (passo de operação)**

- Arquivos: `firestore.rules` + `firebase.json`. O `.firebaserc`/projeto é específico de ambiente e
  **não** entra no repo; alvo via `firebase use <project>`.
- Publicar: `firebase deploy --only firestore:rules` (ou colar no console → Firestore → Regras).
 - **Ordem importa (mudança de comportamento no deploy)**: catálogo saiu de `auth != null` para
  `ehGestor()`. Pior: `seedFirestore.semearSeVazio()` roda no `LoginViewModel.init`
  (`LoginViewModel.kt:62`) — **antes do login**, com `request.auth == null`. Num projeto novo, após
  publicar as regras, o seed seria **negado por completo** (nem autenticado está, quanto mais gestor).
  Semear **antes** de travar as regras (test-mode), ou popular pelo console/Admin SDK, ou mover o seed
  para rodar pós-login como gestor. (Não é bug de regra; é sequência de operação — e um bom argumento
  para o seed sair do caminho pré-auth quando o módulo evoluir.)
- **Validação automatizada** (não roda no build Gradle — regras não são Kotlin): projeto Node isolado
  em `firestore-tests/` com `@firebase/rules-unit-testing` + Jest, rodado via
  `firebase emulators:exec` (`cd firestore-tests && npm install && npm test`). Cobre a matriz —
  operador não edita alheia; colab edita alheia; operador não vira ADM; catálogo só gestor escreve;
  contador não retrocede nem some. **24 casos, todos verdes.** É a rede que trava a paridade com
  `PermissoesUsuario`: mudou um cargo? o teste correspondente quebra até os dois lados casarem.

**Alternativas futuras**

- **Read-scoping de passagens** por posse (Operador só as próprias) com query constraints.
- **Custom claims** + Cloud Function para cargo (tira o `get()` por request).
- **Suíte de testes de regras no CI** — a suíte já existe (`firestore-tests/`); falta pendurá-la num
  workflow (rodar `npm test` no push que toca `firestore.rules`).
