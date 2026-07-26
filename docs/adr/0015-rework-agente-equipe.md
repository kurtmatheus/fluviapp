# ADR-0015: Agente é o usuário — Equipe, agência/lotação como capacidades, agência transversal à emissão

**Status:** **Aceita** — todos os pontos fechados com o analista (ver *Decisões resolvidas*). **Em
implementação:** P2.0 e P2.1 feitos; P2.2 em diante pendentes. É o **Pilar 2** do
[`mvp-roadmap.md`](../design/mvp-roadmap.md) e responde o §6 do
[estudo do form de passagem](../design/form-passagem-validacao-exibicao.md).

> Conversa com o [ADR-0010](0010-autorizacao-por-cargo.md) (autorização por cargo; `funcionarioId = uid` já
> é o emissor), o [ADR-0008](0008-relacionamentos-por-identidade.md) (relacionar por id), o
> [ADR-0002](0002-capability-forma-pagamento.md) (capability `podeSelecionarFormaPagamento` — **superado**
> por este, §4a) e o [ADR-0003](0003-modelo-de-memoria-do-dado.md), o
> [ADR-0012](0012-ciclo-de-vida-passagem-e-embarque-qr.md) (o check-in virou QR), o
> [ADR-0011](0011-regras-firestore-por-cargo.md) (regras Firestore), a
> nota [Viagem × Trecho](../design/viagem-vs-trecho.md) (a **ocupação das embarcações** é a dimensão que
> cruza agências) e a [identidade visual](../design/) (branding por agência; `logo1/logo2.png` já no repo).

## Contexto

Duas entidades sobrepostas modelam "quem vende":

- **`Agente`** — entidade separada (nome + `agencia` + capability `podeSelecionarFormaPagamento`), com
  `AgenteRepository` (`obterTodasAgencias`/`obterTodosAgentes`/`obterAgentesPorAgencia`). As **agências são
  um enum** (`Agente.Agencia`, ex. `MATRIZ`).
- **`Usuario`** — o logado (uid, email, nome, **cargo**). É quem de fato emite: `funcionarioId = uid`
  congelado na Passagem (ADR-0010).

Rachaduras:

- **O agente do bilhete é texto livre.** `Passagem.agencia`/`agente` são `String` snapshot, preenchidos por
  uma **área de agência do form que está comentada** (`ContentAgenciaAreaPassagemForm`); os eventos
  `onAgenciaChange`/`onAgenteChange` existem no VM mas não são plugados; `atualizarListaAgente` usa
  `runBlocking`.
- **Capability por casamento de nome (frágil).** `PassagemDadosPassagemMapper` deriva
  `podeSelecionarFormaPagamento` buscando `obterAgentesPorAgencia(agencia).firstOrNull { descricaoNome ==
  agente }` — best-effort, quebra com homônimo/rename.
- **Meta multi-agência.** A plataforma vai atender **várias agências** e exigir cadastro. Cada agência opera
  **isolada** nas suas passagens; o único ponto compartilhado é a **ocupação das embarcações**. E a emissão
  deve refletir a **identidade visual da agência** emissora.

**Diagnóstico:** o agente **é** o usuário logado. Manter `Agente` como entidade paralela + agência/agente
como texto livre no bilhete duplica identidade e fragiliza a capability. Falta **agência como capacidade do
usuário**, derivada na emissão.

## Decisão

**O agente é o usuário; o módulo "Agentes" vira "Equipe"; agência e lotação são capacidades do usuário; a
agência é transversal à emissão (sempre a do logado) e governa a identidade visual.**

### 1. Agente = Usuário; módulo "Equipe"

O membro da **Equipe** é o `Usuario`. O conceito `Agente` como entidade separada é **aposentado**; suas
capacidades migram para `Usuario`. O menu "Agentes" vira **"Equipe"** — gestão de membros (usuários com
cargo + agência + lotação), no molde de cadastro (ADR-0006).

### 2. Agência e lotação como capacidades do usuário

`Usuario` ganha:
- **`agencia`** — por ora o valor do **conjunto fixo** (o enum `Agencia`, que sai de dentro do `Agente` para
  não morrer com ele em P2.5; evolui p/ coleção cadastrável depois — §Decisões). Relação por valor estável
  agora, por id quando virar coleção (ADR-0008). **Default `AUTONOMO`** — a *agência coringa* de quem não
  está vinculado a nenhuma. Não é estado inválido: é o padrão de quem ainda não foi alocado.
- **`lotacao`** — o **município** do membro. (Embarque/desembarque **não** são lotação: são competências do
  **Trecho** — origem/destino da rota — e serão integrados no rework Viagem→Trecho.)

A lotação é **dado de perfil, não de bilhete**: **não** entra no snapshot da Passagem (não existe "município
emissor" no bilhete). Quem é congelado na emissão continua sendo o **emissor** (`funcionarioId`, ADR-0010) e
a **agência** (§3) — a lotação se alcança pelo perfil do emissor quando alguém precisar dela.

Migração Room + espelho Firestore (ADR-0003). São **capacidades organizacionais** — dizem *onde* o membro
atua, não *o que* ele pode fazer; permissão continua sendo do cargo (§4). A capability
`podeSelecionarFormaPagamento` **não migra**: é removida (§4a).

### 2.1 Provisionamento: o membro é cadastrado pela gestão; o autocadastro sai

Decisão do analista: **o autocadastro é desabilitado.** Ele existia como vitrine de portfólio (estrutura de
login completa), não como necessidade do negócio — numa operação real ninguém se auto-inscreve na equipe de
uma agência. Quem cadastra é **ADM/GESTOR** ou **SUPERVISOR**, com formulários diferentes:

| Quem cadastra | Agência do novo membro | Lotação |
|---|---|---|
| **ADM / GESTOR** | **dropdown** (agências do conjunto fixo; cadastro de agência segue no console por ora) | campo do form |
| **SUPERVISOR** | **implícita** — a dele, auto-escrita, sem seletor | campo do form |

O cadastro grava o **e-mail** do membro. É ele que amarra o pré-cadastro à pessoa depois.

**A restrição que molda o desenho — é questão de validação, não de ordem impossível** (correção do
analista). **Conta no Firebase Auth continua obrigatória para o agente**; ela só nasce mais tarde, no
primeiro acesso. O que a ordem impede é uma coisa só: o perfil é `users/{uid}`, com o id **sendo** o uid do
Auth (`criarPerfil` escreve em `document(currentUser.uid)`, `perfilAutenticado` lê de lá, as regras
resolvem o cargo com `get(users/$(request.auth.uid))`, e a emissão congela `funcionarioId = uid` —
ADR-0010). Como a gestão cadastra **antes** de existir conta, não existe uid para usar de id naquele
momento. Logo o cadastro da gestão grava em outro lugar, chaveado pelo **e-mail**, e o primeiro acesso é o
ponto onde se **valida** e se promove aquilo a perfil.

**Desenho (proposta do Claude, aberta a veto):** separar *convite* de *perfil*.

- A gestão escreve um **pré-cadastro** numa coleção própria, com o **e-mail como id do documento** —
  `membrosConvidados/{email}` = `{ nome, email, cargo, agencia, lotacao }`. Não precisa de uid.
- **Primeiro acesso** (a funcionalidade nova no login): a pessoa informa o e-mail; havendo pré-cadastro e
  não havendo conta, o app abre **"criar senha + confirmar"**, cria a credencial no Auth e só então nasce o
  `users/{uid}` **com os dados do pré-cadastro**, que é consumido. Depois disso ela **loga de novo** com
  e-mail e senha — o passo de confirmação que você descreveu.
- **`users/{uid}` continua sendo criado pelo próprio dono** — a regra self-create do ADR-0011 permanece
  intacta. O que muda é que o conteúdo deixa de ser escolhido pelo cliente: a regra passa a exigir que cargo,
  agência e lotação **casem com o pré-cadastro** (`get(membrosConvidados/$(request.auth.token.email))`).
  O anti-escalonamento fica **mais forte**, não mais fraco: hoje o autocadastro dá `AGENTE` a qualquer um;
  depois, só existe quem a gestão convidou, com o cargo que ela definiu.
- O e-mail como id funciona porque o token do Auth carrega `request.auth.token.email` — dá para autorizar o
  convidado a ler/consumir **o próprio** convite sem query (regras não fazem query, só `get` por caminho).

> **Variante possível, se preferir uma coleção só:** o cliente *consegue* criar a conta do outro na hora do
> cadastro, inicializando uma **segunda instância de `FirebaseApp`** (o `createUserWithEmailAndPassword` da
> instância padrão trocaria o usuário logado; numa instância secundária, não). Aí a gestão já obtém o uid e
> escreve direto em `users/{uid}`, sem coleção de convite. O custo é ter de inventar uma senha temporária
> para essa conta — e aí o "primeiro acesso" deixa de ser *criar* senha e vira *redefinir* (link de e-mail),
> que é diferente do fluxo que você descreveu. Por isso o desenho acima é o default.

**Edição de perfil alheio** (decisão do analista): **ADM/GESTOR editam qualquer perfil**; o **SUPERVISOR**
edita os membros da **própria agência** (lotação; a agência dele é implícita). É a primeira exceção ao
"só o dono escreve o próprio perfil" do ADR-0011 — entra como regra nova, com casos no emulador.

**Google Sign-In sai** (decisão do analista). Era a outra porta do autocadastro: `autenticarComGoogle`
auto-provisiona `users/{uid}` com o cargo padrão quando o doc não existe — fechar o cadastro por e-mail e
deixar essa aberta não fecharia nada. Some inteiro em P2.2c, junto com o autocadastro: o acesso passa a ser
**e-mail + senha de quem foi pré-cadastrado**, e ponto. (Junto saem o botão, o fluxo de credencial e a
dependência de Google Sign-In.)

### 2.2 A lista da Equipe é recortada pelo cargo

Mesma tela, dois recortes (decisão do analista):

- **SUPERVISOR**: vê os agentes que trabalham para ele — a **agência é implícita** (a dele) e por isso
  **não** é filtro; o filtro disponível é **lotação**.
- **ADM/GESTOR**: veem a mesma lista, agora com **filtro por agência também** (é cargo administrativo,
  atravessa agências — §4.1), e com o **supervisor editável**.

Isto é a **antecipação do escopo por agência** (§4.1, fase P2.6) na primeira tela que precisa dele: a Equipe
já nasce isolada por agência para o supervisor, enquanto a listagem de passagens só ganha esse recorte
depois.

### 3. Agência transversal à emissão — sempre a do logado

A emissão **deriva a agência do usuário logado** e a congela na Passagem (snapshot, como `funcionarioId`).
Consequências:
- **Aposenta o campo manual** agente/agência: `ContentAgenciaAreaPassagemForm` sai de vez; os eventos
  `onAgenciaChange`/`onAgenteChange` e o `runBlocking` de `atualizarListaAgente` morrem.
- **Isolamento multi-tenant:** cada agência só vê/mexe nas **próprias passagens**; nenhuma interfere nas da
  outra. O emissor não escolhe agência — é a dele. **Exceção: os cargos de plataforma** (`ADM`/`GESTOR`)
  atravessam agências — ver §4.1.
- **Isolamento por UI no MVP** (decisão do analista): o filtro por agência vive na consulta/listagem, no mesmo
  espírito do ADR-0010 ("segurança por UI"). **Sem** regra Firestore por agência agora — fica como débito
  explícito, a ser promovido no ADR-0011 quando houver agências reais/mutuamente desconfiadas.

### 4. `podeSelecionarFormaPagamento` é **aposentada**; permissão continua sendo do **cargo**

Duas coisas, na ordem certa:

**(a) A capability morre — não migra para lugar nenhum.** Ela é resíduo da **proposta antiga** do app, em que
o que se emitia era o **bilhete de check-in**: aí fazia sentido restringir quem escolhia a forma de pagamento
(agências terceiras tinham faturamento fixo — [ADR-0002](0002-capability-forma-pagamento.md)). O app **mudou
de ponto no processo**: hoje emite literalmente a **passagem**, *antes* do bilhete, e o **check-in virou o QR
code** ([ADR-0012](0012-ciclo-de-vida-passagem-e-embarque-qr.md): `A_EMITIR → EMITIDA → EMBARCADA`). Quem emite a
passagem é quem cobra — o papel que a restrição protegia não existe mais. Migrá-la seria carregar para a
`Equipe` uma competência de um processo aposentado.

**(b) O eixo de permissão continua sendo o cargo** ([ADR-0010](0010-autorizacao-por-cargo.md),
`PermissoesUsuario`) — política única, nada de permissão individual por usuário. O `Usuario` ganha
`agencia`/`lotacao` como **capacidades organizacionais** (§2), não como permissões: quem pode o quê continua
saindo do cargo.

**O código já concordava com (a).** O gate está partido em dois, em direções opostas:

| Caminho | Hoje | Efeito real |
|---|---|---|
| **Emissão** (`FormPassagemUiState:83`) | `isFormaPagamentoEnabled = true` **constante** | o gate **já não existe** ao emitir |
| **Leitura** (`DadosPassagem:72` ← mapper) | derivada por nome, e `entry.agencia` é sempre `""` (a área do form está comentada) | resolve **`false` quase sempre** → o detalhamento **esconde** o breakdown de pix/dinheiro/débito/crédito de passagens que os têm |

Remover unifica os dois no comportamento que a emissão já pratica e, de quebra, conserta a exibição do
detalhamento (`DetalhamentoPassagemContent:290`).

**Superfície da remoção:** `DadosPassagem.podeSelecionarFormaPagamento` + `isFormaPagamentoEnabled`, o ramo do
mapper e sua dependência `agenteRepository` (`PassagemDadosPassagemMapper:39-47`), o `if` do
`DetalhamentoPassagemContent`, o `if` do `ContentPagamentoAreaForm:109` (vira incondicional), a constante do
`FormPassagemUiState`, a coluna em `Agente`/`AgenteDocumento`/`DocumentoBrutoMappers`, os `SampleData` e os
testes. O **[ADR-0002](0002-capability-forma-pagamento.md) fica superado** por este ponto.

**O "valor pago" avulso sai junto** (decisão do analista): não faz mais sentido no processo novo. Ele é o par
da capability — existia para o caso "não escolhe forma de pagamento, informa só o total". Detalhe de execução
importante:

- A **validação** dele (`ValidacaoDadosPassagem:60`, ramo `!isFormaPagamentoEnabled && !isGratuidade`) é
  **inalcançável**: quando o `else` roda é porque há gratuidade, e a condição pede `!isGratuidade`. Deleção
  sem efeito.
- O **campo**, esse, é alcançável — pela **gratuidade**: `ContentPagamentoAreaForm:157` renderiza o "valor
  pago" no `else` de `if (enabled && !isGratuidade)`, com `"0"` e `isValorPagoEnabled = false`
  (`FormPassageiroHelper:87-89`). Ou seja: hoje o bilhete gratuito exibe um campo desabilitado escrito `0`.
  Removê-lo faz a área de pagamento **não renderizar nada** na gratuidade — que é o correto (gratuidade não
  paga), mas é **mudança visível**, não deleção invisível.
- É campo **persistido**: `Passagem.valorPago: Double?` (Room + `PassagemDocumento` no Firestore), o
  `RascunhoPassagemSnapshot`, o state/evento do form e as duas somas que o incluem
  (`PassagemExtensions.obterValorTotalAPagar`, `valorCobrado` no mapper). Sai com migração Room (drop de
  coluna) e regeneração pelo seed. As somas perdem uma parcela que, no fluxo atual, é sempre `0` ou nula —
  então o **balanço financeiro** (ADR-0014) não muda de resultado.

### 4.1 Escopo por cargo: ADM/GESTOR são cargos **FluviApp**; os demais são **por agência**

O isolamento do §3 não é uniforme — depende do cargo:

- **`ADM` e `GESTOR` são cargos da plataforma (FluviApp), não de uma agência.** São *masters*: detêm todas
  as permissões e enxergam **todas as agências**. É o predicado que `PermissoesUsuario.ehCargoPlataforma` isola.
- **`SUPERVISOR` e `AGENTE` (ex-`COLABORADOR_MASTER`/`OPERADOR` — §4.2) são cargos de agência.** Controle e
  gestão da informação acontecem **dentro da própria agência**.

Isso acrescenta um **terceiro eixo** ao ADR-0010, que hoje tem dois (seção × ação-com-posse): o **escopo** —
plataforma × agência. Na prática a posse ganha um grau intermediário:

> minha passagem → passagens **da minha agência** → todas as agências

Consequência afiada, a implementar em P2.6: `podeVerTodasPassagens`/`podeEditarQualquerPassagem` hoje
respondem `true` para o `SUPERVISOR`. Com o escopo, "**todas**" passa a significar
"**todas da minha agência**" para ele, e "todas mesmo" só para os cargos de plataforma. A política ganha algo como
`podeVerTodasAgencias(cargo) = ehCargoPlataforma(cargo)`, e as consultas de passagem filtram por agência quando ele é
`false`.

### 4.2 Os cargos são renomeados: `GESTOR`, `SUPERVISOR` e `AGENTE`

A armadilha de nome do `COLABORADOR_MASTER` ("master" que não é master de plataforma) se resolve renomeando.
O enum `Usuario.Cargo` passa a ser:

| Antes | Depois | Escopo | Papel |
|---|---|---|---|
| `ADM` | `ADM` | **Plataforma (FluviApp)** | master: todas as permissões, todas as agências |
| `DIRETOR` | **`GESTOR`** | **Plataforma (FluviApp)** | idem — "gestor do sistema" é a terminologia correta; `DIRETOR` confundia com o diretor *de uma agência* |
| `COLABORADOR_MASTER` | **`SUPERVISOR`** | Agência | o **master da agência**; **cargos executivos de agência entram aqui automaticamente** (o diretor *de uma agência* é `SUPERVISOR`, não `GESTOR`) |
| `OPERADOR` | **`AGENTE`** | Agência | quem vende: operador = agente |

**O predicado acompanha:** `PermissoesUsuario.ehGestor` vira **`ehCargoPlataforma`** (e o mesmo em
`firestore.rules`). Com `GESTOR` virando cargo concreto, um `ehGestor(cargo)` que responde `true` também
para `ADM` recriaria a mesma ambiguidade que o rename está desfazendo; nomear pelo **escopo** (§4.1) resolve.

**Como alguém vira `SUPERVISOR`:** pela **gestão**, não pelo form. O cadastro (in-app ou autocadastro)
sempre nasce `AGENTE` — o menor privilégio — e a promoção é operação de console/backend, coerente com a
regra anti-escalonamento do ADR-0011 (o cliente nunca escreve o próprio cargo). Por isso o form de membro
**não tem seletor de cargo**.

O nome **"Agente" volta — como cargo, não como entidade.** Só é possível porque a entidade `Agente` morre
(§7): o token deixa de ser ambíguo e passa a nomear o **papel** de quem emite, casando com a tese do ADR
("o agente é o usuário"). Fecha o vocabulário: a **Equipe** é formada por membros com cargo, e o cargo de
quem vende chama-se **Agente**.

**Ordem importa — colisão de nome.** Existe `SecaoMenu.AGENTE` (a seção de menu), que P2.1 renomeia para
`EQUIPE`. O rename do cargo tem de vir **junto ou depois** disso, senão convivem dois `AGENTE` com
significados diferentes (seção × cargo). Por isso os dois renames vão na **mesma fase (P2.1)**.

**Superfície do rename** — o cargo é **String persistida**, então não é só o enum:
`Usuario.Cargo`/`PermissoesUsuario`, `firestore.rules` (a lista literal de `cargoConhecido()`, o
`ehCargoPlataforma()`, o `podeEditarQualquerPassagem()` e o cargo default do autocadastro em `users/{uid}`),
a suíte `firestore-tests/rules.test.js`, `CARGO_PADRAO`/`CARGO_PADRAO_AUTOCADASTRO`
(`CadastroViewModel`, `FirebaseAutenticacaoRepository`) e os testes.

> **Fail-closed morde na virada.** `Cargo.de` devolve `null` para valor desconhecido e a política nega tudo
> (ADR-0010). Um usuário com `cargo: "OPERADOR"` gravado em `users/{uid}` ou em cache na sessão (DataStore)
> vira **sem permissão** depois do rename — e as regras do servidor também passam a negá-lo. `rules` + app +
> suíte de emulador têm de ir no **mesmo commit**, senão o servidor nega o app inteiro.
>
> **Correção do que este ADR dizia antes:** a saída **não** é "regenerar pelo seed". O `SeedFirestore`
> **não semeia usuários** por decisão explícita — quem popula `users/{uid}` é o cadastro in-app. E a regra
> anti-escalonamento impede o próprio usuário de corrigir o cargo. Logo, para um perfil já gravado com o
> vocabulário antigo há duas saídas: **editar o cargo no console** (operação de backend, como a promoção
> sempre foi) ou **recadastrar** o usuário, que nasce `AGENTE`. Sem alias de compatibilidade —
> `PermissoesUsuarioTest` tem um lock explícito de que `DIRETOR`/`COLABORADOR_MASTER`/`OPERADOR` **não**
> resolvem.

### 5. Identidade visual por agência

O bilhete/impressão resolve o **logo pela agência do emissor**. Por ora, **bundle fixo** (drawable —
`logo1/logo2.png` já no repo) mapeado por chave de agência; **Storage por agência** fica como futuro. Casa
com o `FluviWordmark` (marca do app) × logo da agência (marca do emissor).

### 6. O que cruza agências: só a ocupação das embarcações

Implicação forte das respostas: agências são **isoladas nas passagens**, mas **compartilham a ocupação das
embarcações** (a capacidade do navio é finita e comum). Logo:
- A **Contagem de Passagem** (ocupação do navio) é naturalmente **cross-agência** — todos precisam saber
  quão cheio está o barco (coerente com "operador vê a contagem geral", MVP P1.3).
- O **Faturamento** e a **gestão de passagens** são **isolados por agência**.

**Recorte da contagem = a viagem, nunca a agência** (decisão do analista). A agência **não é eixo** da
contagem — não filtra, não fatia, não aparece no agrupamento; a unidade é a **viagem**, somando as passagens
de todas as agências que venderam nela. Isso vale **já no MVP**, não espera o Viagem→Trecho.

> Consequência técnica: hoje `BalancoPassagensMapper` agrupa por **`navioId`** congelado
> (`BalancoPassagensMapper.kt:33`) — um proxy do recorte certo enquanto uma "Viagem" ainda é o **trecho sem
> data** (`viagem-vs-trecho.md`). A Passagem já congela `viagemId`, então mover o `groupBy` para ele é
> mecânico; mas só com o rework Viagem→Trecho (viagem = trecho + data) o "por viagem" deixa de misturar dias.
> Enquanto isso, o agrupamento por navio **não conflita** com a decisão: ambos ignoram a agência.

### 7. `Agente` é removido de vez — sem período dormente

Nada de entidade zumbi: quando as capacidades estiverem no `Usuario` (P2.2) e a emissão derivar do logado
(P2.3), o `Agente` **sai inteiro** — entidade, DAO, repositórios, documento, form, telas, navegação, fakes e
testes (~40 arquivos citam `Agente` hoje). O que sustenta a remoção sem transição:

- **Não há dado real a preservar** — o app é portfólio, o conteúdo vem do `SeedFirestore`; migrar agente
  existente é problema inexistente (regenera-se pelo seed).
- **Ninguém depende do `Agente` depois de P2.0/P2.3** — a única leitura viva é o casamento-por-nome do
  `PassagemDadosPassagemMapper`, e ela morre com a capability (§4a), não com uma substituta.
- **Dormente custa mais que remover** — duas fontes de "quem vende" convivendo é exatamente a rachadura que
  este ADR fecha; manter a tabela "por via das dúvidas" reabre a duplicação de identidade.

O que **não** é removido: os campos **snapshot** `Passagem.agencia`/`agente` continuam existindo (são
histórico congelado, ADR-0008) — mudam só de **origem**, passando a ser preenchidos a partir do usuário
logado em vez de digitados.

## Plano de migração (faseado, aditivo)

- **P2.0 — Aposentar `podeSelecionarFormaPagamento` + o `valorPago` avulso. ✅ FEITO** (§4a) — em dois
  commits: a capability (leitura, cadastro e `MIGRATION_17_18`) e o `valorPago` (form, somas e
  `MIGRATION_18_19`). O `PassagemDadosPassagemMapper` perdeu o `AgenteRepository`; a gratuidade deixou de
  exibir o campo `0` desabilitado; `FormPassageiroHelper` deixou de depender do state da passagem (a
  visibilidade da área virou estado derivado de `isGratuidade`, não sincronização imperativa).
- **P2.1 — Vocabulário: "Equipe" + cargos novos. ✅ FEITO** (§4.2). Menu "Agentes" → "Equipe"
  (`SecaoMenu.AGENTE` → `EQUIPE`), `DIRETOR` → `GESTOR`, `COLABORADOR_MASTER` → `SUPERVISOR`,
  `OPERADOR` → `AGENTE`, e `ehGestor` → `ehCargoPlataforma` — tudo num commit só, com `firestore.rules`,
  a suíte de emulador (34 casos verdes) e os defaults de autocadastro.
  **Regra de tradução do vocabulário na UI** (decisão do analista): o *coletivo* é **Equipe** (menu, título
  da seção, pesquisa); o *indivíduo* continua **Agente** — "a equipe é formada por agentes". Os
  identificadores de código do CRUD (`FormAgenteScreen`, rotas, chaves de string) **não** foram renomeados:
  a entidade morre inteira em P2.5.
- **P2.2 — `Usuario` ganha `agencia` + `lotacao`.** Cresceu de "duas colunas" para uma mudança de
  **provisionamento de identidade** (§2.1), então vai em três passos:
  - **P2.2a — modelo.** `Usuario` ganha `agencia` (default `AUTONOMO`) + `lotacao`; o enum `Agencia` sai de
    dentro do `Agente` (que morre em P2.5); espelho `UsuarioDocumento` + migração Room. Não depende das
    decisões de tela — é a base de tudo.
  - **P2.2b — cadastro pela gestão.** Coleção de pré-cadastro (`membrosConvidados/{email}`), form da Equipe
    nos dois recortes (§2.1) e a lista recortada por cargo (§2.2). Regras novas no ADR-0011 (escrita de
    convite; edição de perfil alheio por plataforma/supervisor) + casos no emulador.
  - **P2.2c — primeiro acesso.** Detecção no login, tela de criar/confirmar senha, nascimento do
    `users/{uid}` a partir do convite, **desabilitar o autocadastro** e **remover o Google Sign-In** — que
    é a outra porta do mesmo provisionamento automático (§2.1).
- **P2.3 — Emissão deriva do logado.** Congela agência (do usuário) na Passagem; remove
  `ContentAgenciaAreaPassagemForm` + eventos/`runBlocking`, e com ele as validações órfãs de
  `agencia`/`agente` (`ValidacaoDadosPassagem:57-58` exige campos que a UI não mostra).
- **P2.4 — Identidade visual por agência.** Logo (bundle mapeado) no bilhete/impressão.
- **P2.5 — Aposentar `Agente`.** Remoção completa (§7): `Agente`, `AgenteDao`, `AgenteRepository`/
  `AgenteFirestoreRepository`, `AgenteDocumento`, `FormAgente*`/`ResultSearchAgente*`, `ValidacaoAgente`,
  rotas/destinos, `FakeAgenteRepository` e testes; migração Room que dropa a tabela; seed sem agentes.
  Só depois de P2.3 (a última leitura viva morre lá).
- **P2.6 — Escopo por agência na listagem.** Novo eixo em `PermissoesUsuario` (`podeVerTodasAgencias =
  ehCargoPlataforma`) + filtro por agência do logado nas consultas de passagem quando ele for `false` — isolamento por
  UI (§3, §4.1). Contagem de Passagem fica **fora** desse filtro por definição (§6).

## Consequências

- **Uma identidade só** — o agente é o usuário; some a duplicação `Agente` × `Usuario`.
- **Um eixo de permissão só** — tudo pelo cargo (ADR-0010); nenhuma permissão individual por usuário. Menos
  lugares para perguntar "quem pode".
- **Menos código, não mais** — a capability desaparece em vez de mudar de casa; o detalhamento passa a
  mostrar o breakdown de pagamento que hoje esconde.
- **ADR-0010 ganha um terceiro eixo** — escopo (plataforma × agência), com "todas as passagens" passando a
  significar "todas da minha agência" para o `SUPERVISOR` (§4.1). É mudança de comportamento, não só adição.
- **Vocabulário fechado** — plataforma (`ADM`/`GESTOR`) × agência (`SUPERVISOR`/`AGENTE`); "Agente" volta
  como **cargo** porque a entidade homônima morre. Cargo é String persistida: o rename é *breaking* para
  sessões e documentos existentes (fail-closed), resolvido por seed + re-login (§4.2).
- **Emissão mais simples** — sem campo de agência/agente; menos superfície, sem `runBlocking`.
- **Multi-tenant** — isolamento por agência nas passagens; ocupação de embarcação compartilhada.
- **Branding por agência** — o bilhete carrega a marca da agência emissora.
- **Débito de isolamento no servidor** — o isolamento por agência é **só de UI** por decisão consciente: um
  cliente adulterado ainda lê passagens de outra agência. Aceitável enquanto todas as agências são do mesmo
  operador/portfólio; vira regra Firestore (ADR-0011) quando houver agências mutuamente desconfiadas.
- **Sem "município emissor" no bilhete** — a lotação não viaja no snapshot; quem quiser o município do
  emissor resolve pelo perfil (`funcionarioId` → `Usuario.lotacao`), com o risco normal de dado vivo (o
  membro pode ter sido transferido depois da emissão).
- **Corte grande de superfície** — a remoção do `Agente` (§7) apaga um módulo CRUD inteiro; o diff de P2.5 é
  quase todo deleção.

## Alternativas consideradas

- **Manter `Agente` separado + texto livre (status quo)** — rejeitado: duplica identidade, capability frágil,
  não escala p/ multi-agência.
- **`Agente` dormente durante a transição** — rejeitado (§7): sem dado real a preservar e sem leitor após
  P2.3, a tabela zumbi só perpetuaria a duplicação de identidade.
- **Migrar `podeSelecionarFormaPagamento` p/ atributo do `Usuario`** — rejeitado: preservaria, como permissão
  individual, uma competência do processo antigo (bilhete de check-in). O que sobrevive vira cargo; o que é
  do processo aposentado, morre.
- **Manter a capability, só movendo-a para o cargo** — rejeitado pelo mesmo motivo: não há regra de negócio
  atual que ela expresse.
- **Contagem fatiada por agência** — rejeitado: a lotação de um navio é única; fatiar por agência daria a
  cada uma uma visão parcial de um recurso finito e compartilhado.
- **Regra Firestore de isolamento por agência já no MVP** — adiado: segurança por UI basta enquanto as
  agências não são mutuamente desconfiadas (mesma postura do ADR-0010 antes do ADR-0011).
- **Agência como coleção cadastrável já no MVP** — adiado: conjunto fixo (enum) basta agora; promover a
  coleção quando exigir cadastro de agência (não só de usuário).
- **Logos no Firebase Storage por agência** — adiado: bundle fixo cobre o MVP; Storage quando houver muitas
  agências cadastrando o próprio logo.

## Decisões resolvidas na conversa (analista, 2026-07-26) — 1ª rodada

- **Lotação = município** — embarque/desembarque são competências do Trecho (origem/destino), integração
  futura (Viagem→Trecho).
- **Agência do bilhete = sempre a do usuário logado** — sem override; agências isoladas nas passagens,
  compartilham só a **ocupação das embarcações**.
- **Agência = conjunto fixo (enum) por ora** — só migra a relação p/ o usuário; evolui p/ coleção
  cadastrável depois.
- **Logos = bundle fixo mapeado por agência por ora** — Storage quando evoluir.

## Decisões resolvidas na conversa (analista, 2026-07-26) — 2ª rodada

Fechamento dos cinco pontos que estavam abertos:

1. **Lotação não entra no bilhete** — não existe "município emissor" no snapshot; a lotação é só perfil do
   usuário (§2).
2. **Permissão continua no cargo** (ADR-0010) — e a capability `podeSelecionarFormaPagamento` **some**: era
   competência da proposta antiga (app emitia o **bilhete de check-in**); hoje o app emite a **passagem**,
   antes do bilhete, e o check-in virou o **QR code** (ADR-0012). Supera o ADR-0002 (§4).
   - **`ADM`/`DIRETOR` são cargos FluviApp** — masters, todas as permissões, atravessam agências;
     `COLABORADOR_MASTER`/`OPERADOR` gerem informação **dentro da própria agência** (§4.1).
3. **Isolamento por agência = por UI** — sem regra Firestore no MVP; débito registrado p/ o ADR-0011 (§3).
4. **`Agente` removido de vez** — sem período dormente; remoção completa em P2.5 (§7).
5. **Contagem cross-agência já no MVP, recortada por viagem** — a agência não é eixo da contagem (§6).

## Decisões resolvidas na conversa (analista, 2026-07-26) — 3ª rodada

- **O "valor pago" avulso sai junto** com a capability (P2.0) — não faz mais sentido no processo novo (§4a).
- **`COLABORADOR_MASTER` → `SUPERVISOR`** — o master **da agência**; cargos executivos de agência entram aí
  automaticamente (§4.2).
- **`OPERADOR` → `AGENTE`** — operador *é* o agente; o token fica livre porque a entidade `Agente` morre.
- **`DIRETOR` → `GESTOR`** — "gestor do sistema" é a terminologia correta para o cargo de plataforma, e
  desambigua do diretor *de uma agência* (que é `SUPERVISOR`). Arrasta o predicado: `ehGestor` →
  `ehCargoPlataforma` (§4.2).
- **Na UI, "Equipe" é o coletivo e "Agente" é o indivíduo** — "a equipe é formada por agentes".
- **`SUPERVISOR` é concedido pela gestão** (console/backend), nunca pelo form — o cadastro nasce `AGENTE`.
- **`ImpressaoPassagem` fica dormente** — é impressão **física** e volta com o **módulo de check-in**
  (novo status `CHECADA`, pré-embarque, tornando o bilhete de embarque um artefato *pós-passagem
  confirmada*). Por isso o rótulo `"Operador"` do bilhete **não** foi renomeado aqui; a direção está
  registrada nas *Alternativas futuras* do [ADR-0012](0012-ciclo-de-vida-passagem-e-embarque-qr.md).

## Decisões resolvidas na conversa (analista, 2026-07-26) — 4ª rodada (provisionamento)

- **Autocadastro é desabilitado** — era vitrine de portfólio (estrutura de login), não necessidade do
  negócio. Quem cadastra membro é **ADM/GESTOR/SUPERVISOR** (§2.1).
- **Agência não é escolha do membro**: para ADM/GESTOR é **dropdown**; para SUPERVISOR é **implícita** (a
  dele). Cadastro de *agência* segue no console por ora.
- **Sem agência → `AUTONOMO`**, agência coringa (default, não erro).
- **O cadastro grava o e-mail** — é a chave que liga o pré-cadastro à pessoa no primeiro acesso.
- **Primeiro acesso** vira funcionalidade do login: detecta, pede **criar senha + confirmar**, e a pessoa
  **loga de novo** para confirmar login/senha.
- **ADM/GESTOR editam perfil alheio**; o **SUPERVISOR** edita os da própria agência.
- **A lista da Equipe é recortada por cargo** (§2.2): supervisor filtra só por lotação (agência implícita);
  ADM/GESTOR filtram por agência também.
- **Conta no Firebase Auth é obrigatória para o agente** — não é ordem impossível, é **validação**: a conta
  nasce no primeiro acesso, e só nasce para quem tem pré-cadastro.
- **Google Sign-In sai** — some junto com o autocadastro (P2.2c); acesso passa a ser só e-mail + senha.
- **Cargo na tela de edição fica com o PO/analista** — decisão adiada; até lá, pré-cadastro nasce `AGENTE`
  e promoção segue no console.

## Pontos abertos

- **A tela de edição do ADM/GESTOR mexe no cargo?** — **reservado ao PO/analista**, decisão dele, ainda não
  tomada. Até lá o código segue o caminho conservador: o pré-cadastro nasce **`AGENTE`** e a promoção a
  `SUPERVISOR` continua sendo operação de **console**, com a regra anti-escalonamento intacta. Quando a
  decisão vier, o que muda é (a) um campo de cargo na tela do ADM/GESTOR e (b) a regra que hoje congela o
  cargo no update.

Duas coisas que este ADR só encosta, para depois: promover agência de enum a coleção cadastrável (quando
houver cadastro de agência) e mover o `groupBy` da contagem de `navioId` p/ `viagemId` (quando o
Viagem→Trecho der data à viagem).