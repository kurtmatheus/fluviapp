# Estudo — por que dar capacidade ao `ADM` é caro, e o que as métricas podem medir

> **Estudo, não decisão.** Mede o código como ele está, expõe as tensões e prepara a decisão da
> [issue #19](https://github.com/kurtmatheus/fluviapp/issues/19). Onde este documento e um ADR
> discordarem, o ADR vence.
>
> Origem: pergunta do analista em 2026-09-08, depois de a gestão de acesso ficar de pé — *"ainda estou
> tentando entender por que é tão difícil dar capacidades administrativas para o papel `ADM`"*. A parte
> mais útil deste estudo é a §2, e ela responde essa pergunta com números; as métricas da #19 aparecem
> depois, na §3, porque **o obstáculo delas é um caso particular do obstáculo geral**.

---

## 1. O que já está de pé, e com quantas barreiras

Antes de medir a dificuldade: a capacidade que a [ADR-0032](../adr/0032-o-acesso-politica-sessao-e-ciclo-de-vida.md)
D6 pediu **existe**, e é `ADM`-only em **três** camadas independentes:

| onde | o que diz |
|---|---|
| `PermissoesUsuario.podeGerirAcesso` | `Papel.de(papel) == ADM` — desativar, reativar e definir prazo |
| `PermissoesUsuario.podeAcessar(USUARIOS, …)` | `Papel.de(papel) == ADM` — a seção inteira |
| `firestore.rules`, `users/{uid}` update | `ehAdm() && request.auth.uid != uid && soGestaoDeAcesso()` |

O `GESTOR` **vê** a lista (a leitura de `users` é de qualquer autenticado) e não escreve nada: o
`PesquisaUsuarioViewModelTest` tem o caso, e a regra o nega de novo do outro lado. Não há caminho em que
`GESTOR` desative alguém.

## 2. Por que a próxima capacidade vai custar o mesmo

Sete medidas. As três primeiras são **acoplamento** — dá para desfazer. A quarta e a quinta são **preço de
invariante** — não dá, e não se deve. As duas últimas são **molde**, e ficam no meio.

### 2.1 O `ADM` quase não existe como sujeito

`PermissoesUsuario` tem **26 funções**. Delas:

- **15** perguntam `ehPapelPlataforma(papel)` — isto é, **`ADM` ≡ `GESTOR`**;
- **3** distinguem o `ADM` (`podeGerirAcesso`, `podeConvidar`, e o ramo `USUARIOS` de `podeAcessar`).

Nas regras, a proporção é a mesma: `ehPapelPlataforma()` aparece em **21** pontos e `ehAdm()` em **7** —
e **6** desses 7 são a coleção `convites` mais o ramo novo de `users`.

**A consequência é a resposta curta da sua pergunta.** Quando se quer *"dar isto ao `ADM`"*, não existe um
lugar onde o `ADM` seja o sujeito da frase: existe um lugar onde **"papel de plataforma"** é o sujeito.
Toda capacidade nova de `ADM` começa, então, por um gesto que não é a capacidade — **separar `ADM` de
`GESTOR` num ponto onde os dois estavam fundidos** — e essa separação custa nos dois eixos ao mesmo tempo
(Kotlin *e* `firestore.rules`), cada um com caso de teste próprio, porque o [ADR-0011](../adr/0011-regras-firestore-por-cargo.md)
mantém os dois como espelhos.

O `painel-administrativo.md` §3.1 já tinha visto isto em 2026-08-04 e escrito a frase certa: a seção
Usuários é *"o primeiro lugar onde eles divergem"*. Um mês depois, ainda é quase o único.

### 2.2 Capacidade administrativa não tem casa no menu

O painel do `ADM` é `SECOES_DO_PAINEL`, e ele é uma lista de **cadastros**: Empresa, Embarcação,
Localidade, Porto, Rota, Viagem — mais Usuários. `SECOES_TRANSVERSAIS` é `emptySet()`: o lugar que existia
para o que não é de ninguém em particular **está vazio**.

E o molde presume o par. O KDoc do `SecaoMenu` diz: *"cada seção abre… seus cards de ação
(cadastrar/pesquisar)"*, e o `AcaoMenu` confirma — 9 seções, e cada uma com `_NOVA`/`_NOVO` +
`_PESQUISAR`. As duas exceções existentes provam a regra pela dificuldade: a `PASSAGEM` tem **só** pesquisar
(emitir começa no Início, ADR-0028 D5), e a contagem **saiu** do menu na F9.6 porque apontava para uma
tela apagada.

Métricas não cadastra nada e não pesquisa nada: é a primeira seção do painel que **não é um CRUD**. Isso
não é um impedimento — é a informação de que a #19 precisa inventar uma forma que o molde não tem, e que
essa forma vai ser copiada pelas próximas telas analíticas (balanço, faturamento, ocupação).

### 2.3 Cada capacidade atravessa oito camadas — medido

A gestão de acesso (issue #18) foram **13 arquivos e 958 linhas** para *duas escritas de dois campos*. As
cinco fatias do ADR-0032 juntas: 93 arquivos.

A lista de camadas é sempre a mesma, e nenhuma delas é dispensável no desenho atual:

```
domínio (o estado) → política (quem pode) → regra do servidor → caso de emulador
  → codec (fronteira) → porta (DIP) → impl Firestore
  → UiState → ViewModel → tela → navegação → strings → teste JVM
```

**É o preço do DIP mais o preço do espelho política↔regras.** Ele se paga em confiabilidade — nenhuma
dessas fatias quebrou outra —, mas significa que *"dar uma capacidade ao `ADM`"* nunca é uma mudança
local, e por isso nunca parece pequena.

### 2.4 A regra tem teto, e a cadeia de autorização é longa

A cadeia é `perfil() → papel()/funcionarioIdDoAutor() → cargoDoAutor() → vinculoDoAutor()`, e a linguagem
de regras **não memoriza resultado de função** — só o `get()` do documento. Cada predicado novo
*multiplica* a árvore de avaliação.

Isso não é teoria: ao acrescentar `acessoVigente()` em `papel()` e em `funcionarioIdDoAutor()` (#17), a
escrita de `funcionarios` — onde a cadeia é mais longa — **estourou o teto de 1000 expressões** e passou a
ser negada. A suíte de emulador pegou; sem ela, a regressão iria para produção como *permission denied* sem
causa aparente.

O conserto foi de forma, não de decisão: o limite do prazo passou a entrar **por parâmetro** em vez de
relido, e o `souSupervisorDe()` morreu porque os três chamadores de `daMinhaEmpresa` já perguntavam o cargo
do autor antes — a mesma pergunta era feita duas vezes por escrita.

**O que isso significa para a próxima capacidade:** cada condição nova encurta a margem de *todas* as
escritas, inclusive das que não são do `ADM`. O teto é um recurso compartilhado.

### 2.5 O anti-escalonamento é uma parede deliberada no meio do caminho

`users/{uid}` foi desenhado para que **ninguém escreva o documento de outro**. Toda capacidade
administrativa sobre pessoas colide com essa parede e precisa de uma exceção **nomeada e fechada** — a
lista `['ativo', 'expiraEm', 'funcionarioId']` da §Q1, com o `papel` fora dela para todo mundo.

E a #17 mostrou que a parede tem dois lados, não um: sem barrar as chaves de acesso **no ramo do dono**, o
desativado se religaria escrevendo no próprio documento, porque aquele ramo não pergunta o papel. A
desativação seria decorativa.

**Vale separar isto do resto do estudo:** aqui a dificuldade não é acoplamento a corrigir, é o preço da
proteção mais sensível do sistema. Confundir os dois levaria a "simplificar" exatamente o que não deve ser
simplificado.

### 2.6 O dado do `ADM` está permitido, e não está alcançável

As regras são generosas com ele. O que o app oferece, não:

| coleção | o servidor deixa o `ADM` ler? | o app tem porta? |
|---|---|---|
| `users` | sim (qualquer autenticado) | **sim, desde a #18** (`UsuarioRepository`) |
| `convites` | sim, e **só** ele (`list` é `ehAdm()`) | sim |
| `funcionarios`, `empresas`, `atuacoes`, `embarcacoes`, `localidades`, `portos`, `rotas`, `viagens` | sim | sim |
| `passagens` | sim (qualquer autenticado) | **parcial** — `consultar(criterio)`, desenhada para a emissão |
| `viagens/{id}/ocorrencias/{data}` | sim | **não** — a porta só *reserva número*, não lê o contador |
| `clientes`, `veiculos` | sim (`ehPapelPlataforma()`) | não, e é PII: melhor assim |

O padrão: **o dado existe, a permissão existe, e a pergunta não tem por onde entrar.** Cada pergunta
analítica do `ADM` precisa de um método novo numa porta — e é isso que a §3 mede para as métricas.

### 2.7 O andaime da revitalização ainda intercepta o menu

`secoesDoMenu` = política ∩ `SECOES_REVITALIZADAS`, e hoje o conjunto tem 9 de 9 — não corta nada. Mas
**qualquer seção nova nasce invisível** até ser acrescentada à lista, e o sintoma é o pior possível: a
política concede, o teste da política passa, e a seção não aparece.

---

## 3. O que a #19 pode medir

### 3.1 A premissa da issue não se sustenta, e é bom que não

A #19 diz *"o evento agregado (#12) é o que alimenta esta tela"* e *"sem o evento, não há o que mostrar"*.
Medido: **o evento não alimenta tela nenhuma, e não vai.**

`FirebaseTelemetry.evento()` chama `analytics.logEvent(...)`. O SDK do Analytics no Android é **só de
escrita** — não existe API de leitura. Os eventos da D4 vivem no console e, com export, no BigQuery. Uma
tela que os lesse precisaria de back-end próprio, que o [ADR-0017](../adr/0017-eixo-de-storage-firestore-only.md)
adiou por decisão.

**A boa notícia é que a régua da D4 sobrevive à troca de fonte.** *"O evento conta, o documento prova"*
vira, do lado da leitura, algo ainda mais forte: o Firestore sabe **contar sem devolver documento**
(`count()`, disponível no `firebase-bom:33.7.0` que o projeto usa, junto com `sum()` e `average()`). Uma
contagem não traz nome, não traz documento, não traz PII — **fica cega a PII por construção**, que era
exatamente o argumento da D4.

### 3.2 O que é respondível hoje, e a que custo

| pergunta | de onde | custo |
|---|---|---|
| acessos **ativos · desativados · expirados**, por papel | `users` (a #18 já lista) | zero — já está em memória |
| **convites pendentes** (convidado que não entrou) | `convites` × `users`, a junção que a #18 fez | zero |
| **acessos que expiram nos próximos N dias** | `users.expiraEm` | zero |
| funcionários por empresa · empresas por atuação | `funcionarios.vinculo.empresaId`, `atuacoes` | uma leitura de coleção pequena |
| **emissões por agência e período**, por status ou categoria | `passagens` + `CriterioPassagem` **que já existe** | `count()` — uma agregação, nenhum documento |
| **números emitidos** numa ocorrência | `ocorrencias/{data}.ultimoNumero` | O(1), e **conta números impressos**, não passagens vigentes (o contador é monotônico: cancelada não devolve número) |
| **valor** emitido por período | `sum()` sobre `passagens` | agregação — mas exige campo de total, e o [ADR-0024](../adr/0024-fronteira-de-dados-da-passagem.md) D4 **recusou** total denormalizado: o total é inferido dos lançamentos |

O achado que muda o tamanho da fatia: **a porta da Passagem já sabe recortar.** `CriterioPassagem` tem
`escopo: EscopoEmpresa` (Todas/Apenas/Nenhuma), `recorte: RecorteTemporal` (ocorrência · dia · período ·
qualquer), `status`, `categoria`, `gratuidade`, `funcionarioId`. A tradução para filtros é **pura e
testável em JVM** (`FiltroPassagem`, `ConsultaPassagem`).

Falta uma coisa só: **a pergunta "quantos" ao lado de "quais"** — um `contar(criterio): Int` que reusa a
mesma tradução e troca a operação terminal (`count()` no lugar de `get()`). É a diferença entre trazer a
lotação de uma balsa para descartá-la no cliente e não trazer nada.

### 3.3 O que **não** é respondível, e por quê

- **qualquer pergunta com pessoa no meio** — *"o que a Ana fez às 14h"*. A D4 já a proibiu, e a leitura por
  contagem a torna impossível em vez de proibida;
- **série histórica de uso do app** (sessões, telas abertas, erros por versão) — isso é Analytics, e mora
  no console. A tela não compete com ele;
- **valor** faturado, sem antes decidir se o total da passagem é inferido a cada leitura ou passa a ser
  campo — o que reabriria o ADR-0024 D4. **Não é decisão desta fatia.**

---

## 4. As perguntas que ficam para o analista

> **Respondidas em 2026-09-08**, na mesma sessão: *"as métricas de acesso no Início do painel da
> plataforma"*.
>
> - **Q1** → as **de acesso**, e só elas. As de operação (emissões por agência) ficam para quando alguém
>   pedir, e o `contar()` da Q4 fica com elas;
> - **Q2** → `ADM`-only, pelo mesmo argumento do ADR-0021 D1 e da decisão de 2026-08-04 (*"nenhum gestor
>   pode ver usuários"*). O `GESTOR` continua com o recado de antes, e o recorte corta a **leitura**, não a
>   exibição: `allow list` de `convites` é `ehAdm()`, então ligar o listener para ele produziria
>   *permission denied* e um não-fatal — alarme tocando no caso normal;
> - **Q3** → o **Início do painel da plataforma**, a opção (b);
> - **Q4** → depois, com as perguntas de operação.
>
> **O que a implementação acrescentou.** Ramificar o fluxo por escopo **antes** de ler achou um desperdício
> que estava à vista: `Todo` e `Nenhum` ligavam cinco listeners, esperavam cinco snapshots e descartavam
> tudo — o domínio não olha viagem nenhuma para responder *"o painel é da plataforma"* ou *"falta
> provisionar"*. Cada painel passou a ler as coleções dele, e a plataforma lê `users` + `convites`.
>
> E uma escolha de forma que o card carrega: **zero não vira linha**. Um card com quatro zeros ensina a não
> olhar o card; quando não há pendência, o rodapé diz isso em uma frase.

**Q1 — Quais perguntas a tela responde?** A §3.2 diz o que é barato. As três primeiras linhas (acesso,
convites pendentes, prazos a vencer) custam **zero leitura nova** e são as únicas que falam de *gestão de
usuários*, que é o título da issue. As emissões por agência custam uma porta nova e falam de *operação*.
São duas telas diferentes, e talvez duas fatias diferentes.

**Q2 — A métrica é do `ADM` ou do painel?** Contagem de acesso é `ADM`-only pela mesma razão que a seção
Usuários é. Emissões por agência não têm nada de sensível para o `GESTOR`, que administra o negócio da
plataforma — e é justamente o tipo de coisa que ele deveria ver. Se a resposta for *"os dois, com recortes
diferentes"*, esta fatia deixa de ser a exceção da §2.1 e passa a ser o **segundo** lugar onde `ADM` e
`GESTOR` divergem, o que é um argumento a favor de arrumar o eixo (§2.1) antes de empilhar a terceira.

**Q3 — Onde a tela mora?** As opções que o código admite hoje: (a) uma seção nova no menu, que exige
inventar a forma sem par cadastrar/pesquisar (§2.2) e entrar no andaime (§2.7); (b) o **Início do painel da
plataforma**, que hoje é `InicioDaTela.DaPlataforma` — um estado que não desenha nada e cujo sumário a F10
já prometia; (c) um cabeçalho na própria seção Usuários, restrito ao que fala de acesso.

A (b) tem uma propriedade que vale dizer: **é o único lugar onde a tela não precisa de nada novo** — nem
seção, nem ação de menu, nem rota, nem andaime. O painel do `ADM` abre hoje num estado vazio que existe
desde a F8.4 esperando conteúdo.

**Q4 — `contar()` entra agora ou depois?** Ele é pequeno (um método na porta, uma tradução já existente, um
fake) e destrava as perguntas de operação — mas só é necessário se a Q1 as incluir. Sem elas, a fatia
inteira é junção em memória do que a #18 já carrega.
